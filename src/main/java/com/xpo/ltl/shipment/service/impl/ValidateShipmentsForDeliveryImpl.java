package com.xpo.ltl.shipment.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.shipment.v2.ValidateShipmentsForDeliveryResp;
import com.xpo.ltl.api.shipment.v2.ValidateShipmentsForDeliveryRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.FormatHelper;
import com.xpo.ltl.shipment.service.util.ShipmentUtil;
import com.xpo.ltl.shipment.service.validators.ShipmentsDeliveryValidator;

@RequestScoped
public class ValidateShipmentsForDeliveryImpl {
	private static final Log log = LogFactory.getLog(ValidateShipmentsForDeliveryImpl.class);

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject
	private ShipmentMovementExceptionSubDAO shipmentMovementExceptionSubDAO;

	@Inject
	private ShipmentsDeliveryValidator shipmentsDeliveryValidator;

	List<ShipmentId> eligibleDeliveryShipments = new ArrayList<>();
	List<ShipmentId> ineligibleDeliveryShipments = new ArrayList<>();
	List<Shipment> movrShipments = new ArrayList<>();
	List<Shipment> partAndAllShortShpmtInfoList = new ArrayList<>();
	List<Shipment> finalDlvryShpmtInfoList = new ArrayList<>();
	List<Shipment> otherShpmtInfoList = new ArrayList<>();

	public static String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

	public ValidateShipmentsForDeliveryResp validateShipmentsForDeliveryImpl(ValidateShipmentsForDeliveryRqst request,
			TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

		shipmentsDeliveryValidator.validateValidateShipmentsForDeliveryRqst(request, txnContext);

		List<String> proNumbersList = ShipmentUtil.getProNumbers(request.getShipmentIds());
		List<String> proNumbers = formatProNumbers(proNumbersList,txnContext);
		
		ValidateShipmentsForDeliveryResp response = new ValidateShipmentsForDeliveryResp();

		// step1 : get shipment info
		List<Shipment> shipmentInfoList = shmShipmentSubDAO.listShipmentsForValidation(proNumbers, entityManager);
		// if shipmentinfo list empty add pro number to ineligible

		if (CollectionUtils.isEmpty(shipmentInfoList)) {
			request.getShipmentIds().stream().forEach(shm -> {
				ineligibleDeliveryShipments.add(shm);
			});
		}
		// Step2: filter the shipment for MOVR, Short/PartShort, finalDeliveryones and
		// rest of them
		populateAllShipmentsForValidation(shipmentInfoList);

		// Add finalDeliveryones to ineligible
		finalDlvryShpmtInfoList.stream().forEach(shm -> {
			ineligibleDeliveryShipments.add(getShipmentId(shm));
		});

		// Add other Delivery ones to eligible
		otherShpmtInfoList.stream().forEach(shmt -> {
			eligibleDeliveryShipments.add(getShipmentId(shmt));
		});

		// step 3 : get Parent shipment details for PART/ALL SHORT shipments for now

		List<ShmShipment> moverFromPartShorts = shmShipmentSubDAO.listMovrShipmentsByShipmentInstId(
				ShipmentUtil.convertToLongSetShipmentInstId(partAndAllShortShpmtInfoList), entityManager);

		// if moverFromPartShorts empty then partAndAllShortShpmtInfoList ineligible
		// list
		if (CollectionUtils.isEmpty(moverFromPartShorts)) {
			partAndAllShortShpmtInfoList.stream().forEach(shm -> {
				eligibleDeliveryShipments.add(getShipmentId(shm));
			});
		} else {
			List<ShmShipment> missedMovrShipments = getMissedProMovrShipments(moverFromPartShorts);
			List<ShmShipment> activeMovrProShipments = getActiveMovrProShipments(moverFromPartShorts);

			// non matched should be into eligible
			missedMovrShipments.stream().forEach(shm -> {
				eligibleDeliveryShipments.add(getShipmentId(shm));
			});
			// matched should be into non eligible
			activeMovrProShipments.stream().forEach(shm -> {
				ineligibleDeliveryShipments.add(getShipmentId(shm));
			});
		}

		// Step 4 :
		proNumbers = ShipmentUtil.getProNumbersFromShipment(movrShipments);
		if (CollectionUtils.isNotEmpty(proNumbers)) {
			List<ShmShipment> shmShipments = shmShipmentSubDAO.findMovrShipmentsByProNbr(proNumbers, entityManager);

			if (CollectionUtils.isNotEmpty(shmShipments)) {
				List<Long> parentShipmentIds = ShipmentUtil.getParentIds(shmShipments);
				List<ShmMovementExcp> shipmentExceptions = shipmentMovementExceptionSubDAO
						.findByShpInstIdForShortType(parentShipmentIds, entityManager);

				for (ShmShipment shipment : shmShipments) {
					ShmShipment parentShipment = null;
					if (ShipmentUtil.isValidParentShipment(shipment.getShmShipment()))
						parentShipment = shipment.getShmShipment();

					if (Objects.nonNull(parentShipment)) {
						// if total MOVR PRO piece count is greater than parent piece count then add
						// shipment to ineligible queue.
						if (Objects.nonNull(shipment.getTotPcsCnt()) && Objects.nonNull(parentShipment.getTotPcsCnt())
								&& shipment.getTotPcsCnt().compareTo(parentShipment.getTotPcsCnt()) > 0) {
							ineligibleDeliveryShipments.add(getShipmentId(shipment));
							continue;
						}

						ShmMovementExcp shmMovementExcp = ShipmentUtil.getExceptionByShipmentId(shipmentExceptions,
								parentShipment.getShpInstId());
						if (Objects.isNull(shmMovementExcp) || (Objects.nonNull(shmMovementExcp.getPcsCnt())
								&& shipment.getTotPcsCnt().compareTo(shmMovementExcp.getPcsCnt()) > 0)) {
							ineligibleDeliveryShipments.add(getShipmentId(shipment));
						} else {
							eligibleDeliveryShipments.add(getShipmentId(shipment));
						}

					} else {
						ineligibleDeliveryShipments.add(getShipmentId(shipment));
					}
				}

				// If there is a pro number with no shipment will be added here
				List<String> proNbrShipments = ShipmentUtil.getProNbr(shmShipments);
				proNumbers.removeAll(proNbrShipments);
				if (CollectionUtils.isNotEmpty(proNumbers)) {
					List<ShipmentId> proWithNoShipments = proNumbers.stream().map(proNumber -> {
						ShipmentId shipmentId = new ShipmentId();
						shipmentId.setProNumber(proNumber);
						return shipmentId;
					}).collect(Collectors.toList());
					ineligibleDeliveryShipments.addAll(proWithNoShipments);
				}

			} else {
				ineligibleDeliveryShipments.addAll(request.getShipmentIds());
			}
		}

		response.setEligibleDeliveryShipments(eligibleDeliveryShipments);
		response.setIneligibleDeliveryShipments(ineligibleDeliveryShipments);
		return response;
	}

	private List<String> formatProNumbers(List<String> proNumbersList, TransactionContext txnContext) throws ServiceException {
		List<String> proNumbers = new ArrayList<String>();
		for(String pro:CollectionUtils.emptyIfNull(proNumbersList)) {
			proNumbers.add(FormatHelper.formatProNbrNumber(pro, txnContext));
		}
		return proNumbers;
	}

	private List<ShmShipment> getActiveMovrProShipments(List<ShmShipment> moverFromPartShorts) {
		return CollectionUtils.emptyIfNull(moverFromPartShorts).stream()
				.filter(o1 -> partAndAllShortShpmtInfoList.stream()
						.anyMatch(o2 -> o2.getShipmentInstId().compareTo(getParentInstId(o1)) == 0))
				.collect(Collectors.toList());
	}

	private long getParentInstId(ShmShipment o1) {
		return o1.getParentInstId() == null ? 0L : o1.getParentInstId().longValue();
	}

	private List<ShmShipment> getMissedProMovrShipments(List<ShmShipment> moverFromPartShorts) {
		return CollectionUtils.emptyIfNull(moverFromPartShorts).stream()
				.filter(o1 -> partAndAllShortShpmtInfoList.stream()
						.noneMatch(o2 -> o2.getShipmentInstId().compareTo(getParentInstId(o1)) == 0))
				.collect(Collectors.toList());
	}

	private void populateAllShipmentsForValidation(List<Shipment> shipmentInfoList) {
		for (Shipment shmpt : shipmentInfoList) {
			if (BillClassCd.ASTRAY_FRT_SEGMENT == shmpt.getBillClassCd()) {
				movrShipments.add(shmpt);
			} else if (DeliveryQualifierCd.ALL_SHORT == shmpt.getDeliveryQualifierCd()
					|| DeliveryQualifierCd.PARTIAL_SHORT == shmpt.getDeliveryQualifierCd()) {
				partAndAllShortShpmtInfoList.add(shmpt);
			} else if (DeliveryQualifierCd.FINAL == shmpt.getDeliveryQualifierCd()) {
				finalDlvryShpmtInfoList.add(shmpt);
			} else {
				otherShpmtInfoList.add(shmpt);
			}
		}
	}

	private ShipmentId getShipmentId(ShmShipment shipment) {
		ShipmentId shipmentId = new ShipmentId();
		shipmentId.setShipmentInstId(BasicTransformer.toString(shipment.getShpInstId()));
		shipmentId.setProNumber(shipment.getProNbrTxt());
		shipmentId.setPickupDate(BasicTransformer.toXMLGregorianCalendar(shipment.getPkupDt()));
		return shipmentId;
	}

	private ShipmentId getShipmentId(Shipment shipment) {
		ShipmentId shipmentId = new ShipmentId();
		shipmentId.setShipmentInstId(BasicTransformer.toString(shipment.getShipmentInstId()));
		shipmentId.setProNumber(shipment.getProNbr());
		shipmentId.setPickupDate(
				BasicTransformer.toXMLGregorianCalendar(BasicTransformer.toDate(shipment.getPickupDate())));
		return shipmentId;
	}
}
