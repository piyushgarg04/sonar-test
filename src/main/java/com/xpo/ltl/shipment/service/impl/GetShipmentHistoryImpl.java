package com.xpo.ltl.shipment.service.impl;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvcHist;
import com.xpo.ltl.api.shipment.service.entity.ShmMscLnItmHst;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AccessorialServiceHist;
import com.xpo.ltl.api.shipment.v2.GetShipmentHistoryResp;
import com.xpo.ltl.api.shipment.v2.MiscLineItemHist;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.ShipmentHistory;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcHistSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMscLnItmHstSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.ShipmentRequestsValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetShipmentHistoryImpl {

	private static final Logger logger = LogManager.getLogger(GetShipmentHistoryImpl.class);

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject
	private ShipmentAcSvcHistSubDAO shipmentAcSvcHistSubDAO;

	@Inject
	private ShmMscLnItmHstSubDAO shmMscLnItmHstSubDAO;

	@Inject
	private ShipmentRequestsValidator getShipmentsRequestValidator;

	private static final String SEARCH_METHOD = "getShipmentHistory";
	private static final String INIT = "INIT";
	private static final String END = "END";
	private static final String COLON = "::";

	@LogExecutionTime
	public GetShipmentHistoryResp getShipmentHistory(
			final String proNbr,
			final String pickupDate,
			final Long shipmentInstId,
			final XMLGregorianCalendar[] xmlGregorianCalendars,
			final ShipmentDetailCd[] shipmentDetailCds,
			final TransactionContext transactionContext,
			final EntityManager entityManager) throws ServiceException {

		logger.info("{} {}{}{}", INIT, this.getClass().getSimpleName(), COLON, SEARCH_METHOD);
		String proNumber = "";
		getShipmentsRequestValidator.validateRequest(proNbr, pickupDate, shipmentInstId, shipmentDetailCds, transactionContext);

		if(org.apache.commons.lang3.StringUtils.isNotBlank(proNbr)) {
			proNumber = ProNumberHelper.validateProNumber(proNbr, transactionContext);
		}

		Long shpInstId = shmShipmentSubDAO.getIdByIdOrProNumber(proNumber, shipmentInstId,pickupDate, entityManager);

		if (Objects.isNull(shpInstId)) {
			logger.error(
					"{} {}{}{} {}{}",
					NotFoundErrorMessage.SHIPMENT_NOT_FOUND,
					this.getClass().getSimpleName(),
					COLON,
					SEARCH_METHOD,
					COLON,
					proNbr);
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, transactionContext).build();
		}

		List<ShipmentDetailCd> shipmentDetailCdslist = Arrays.stream(shipmentDetailCds).filter(Objects::nonNull).collect(
				Collectors.toList());
		GetShipmentHistoryResp result = new GetShipmentHistoryResp();
		List<Long> shipmentIdList = new ArrayList<>();
		List<ShmMscLnItmHst> shmMscLnItmHsts = new ArrayList<>();
		boolean miscLineItem = true;
		boolean accessorial = true;
		List<ShmAcSvcHist> shmAcSvcHists = null;

		result.setProNbr(proNbr);
		result.setShipmentHistories(new ArrayList<>());
		shipmentIdList.add(shpInstId);

		if (CollectionUtils.isNotEmpty(shipmentDetailCdslist)) {

			accessorial = shipmentDetailCdslist
					.stream()
					.anyMatch(shipmentDetailCd -> shipmentDetailCd
							.value()
							.equals(ShipmentDetailCd.ACCESSORIAL.value()));

			miscLineItem = shipmentDetailCdslist
					.stream()
					.anyMatch(shipmentDetailCd -> shipmentDetailCd
							.value()
							.equals(ShipmentDetailCd.MISC_LINE_ITEM.value()));

		}

		if (accessorial) {
			shmAcSvcHists = shipmentAcSvcHistSubDAO.getAccessorialsForShipmentId(shpInstId, entityManager);
		}

		if (miscLineItem) {
			shmMscLnItmHsts = shmMscLnItmHstSubDAO.getMiscellaneousForShipmentId(shpInstId, entityManager);
		}

		if (Objects.nonNull(xmlGregorianCalendars) && CollectionUtils.isNotEmpty(Arrays.asList(xmlGregorianCalendars))){
			result = formatShipmentHistoryData(xmlGregorianCalendars,  shmMscLnItmHsts, miscLineItem, accessorial, shmAcSvcHists);
		}else{
			ShipmentHistory shipmentHistory = new ShipmentHistory();

			if (accessorial) {
				shipmentHistory.setAccessorialServicesHist(EntityTransformer.toAccessorialServiceHist(shmAcSvcHists));
			}

			if (miscLineItem) {
				shipmentHistory.setMiscLineItemsHist(EntityTransformer.toMiscLineItemHist(shmMscLnItmHsts));
			}
			result.getShipmentHistories().add(shipmentHistory);
		}

		logger.info("{} {}{}{}", END, this.getClass().getSimpleName(), COLON, SEARCH_METHOD);
		result.setProNbr(proNbr);
		return result;
	}

	private GetShipmentHistoryResp formatShipmentHistoryData(
			XMLGregorianCalendar[] effectiveDates,
			List<ShmMscLnItmHst> shmMscLnItmHsts,
			boolean miscLineItem,
			boolean accessorial,
			List<ShmAcSvcHist> shmAcSvcHists) {

		List<XMLGregorianCalendar> effectiveDatesList;
		GetShipmentHistoryResp result = new GetShipmentHistoryResp();
		result.setShipmentHistories(new ArrayList<>());

		effectiveDatesList = Arrays.stream(effectiveDates).collect(
				Collectors.toList());
		effectiveDatesList = effectiveDatesList.stream().sorted(XMLGregorianCalendar::compare).collect(Collectors.toList());

		List<ShmMscLnItmHst> finalShmMscLnItmHsts = shmMscLnItmHsts;

		effectiveDatesList.forEach(initDate -> {

			ShipmentHistory shipmentHistory = new ShipmentHistory();
			shipmentHistory.setEffectiveDateTime(initDate);

			if (accessorial) {
				shipmentHistory.setAccessorialServicesHist(filterAccesorialsByDate(shmAcSvcHists, initDate));
			}

			if (miscLineItem) {
				shipmentHistory.setMiscLineItemsHist(filterMiscellaneousByDate(finalShmMscLnItmHsts, initDate));
			}
			result.getShipmentHistories().add(shipmentHistory);
		});

		return result;
	}

	private List<AccessorialServiceHist> filterAccesorialsByDate(
			List<ShmAcSvcHist> shmAcSvcHists,
			XMLGregorianCalendar effectiveDate) {

		if (CollectionUtils.isEmpty(shmAcSvcHists)) {
			return Collections.emptyList();
		}

		List<ShmAcSvcHist> tmpEntityList = new ArrayList<>();

		tmpEntityList.addAll(shmAcSvcHists.stream().filter(shmAcSvcHist -> {

			XMLGregorianCalendar gcEffTmst = BasicTransformer.toXMLGregorianCalendar(shmAcSvcHist.getId().getEffTmst());
			XMLGregorianCalendar gcExprTmst = BasicTransformer.toXMLGregorianCalendar(shmAcSvcHist.getExprTmst());

			return (gcEffTmst.compare(effectiveDate) <= DatatypeConstants.EQUAL) && (gcExprTmst.compare(effectiveDate)
					>= DatatypeConstants.EQUAL);
		}).collect(Collectors.toList()));

		return EntityTransformer.toAccessorialServiceHist(tmpEntityList);
	}

	private List<MiscLineItemHist> filterMiscellaneousByDate(
			List<ShmMscLnItmHst> shmMscLnItmHsts,
			XMLGregorianCalendar initDate) {

		if (CollectionUtils.isEmpty(shmMscLnItmHsts)) {
			return Collections.emptyList();
		}

		List<ShmMscLnItmHst> mscLnItmHstsTmp = new ArrayList<>();
		mscLnItmHstsTmp.addAll(shmMscLnItmHsts.stream().filter(shmMscLnItmHst ->{
			XMLGregorianCalendar gcEffTmst = BasicTransformer.toXMLGregorianCalendar(shmMscLnItmHst.getId().getEffTmst());

			XMLGregorianCalendar gcExpireDate = BasicTransformer.toXMLGregorianCalendar(shmMscLnItmHst.getExprTmst());

			return gcEffTmst.compare(initDate)<= DatatypeConstants.EQUAL && gcExpireDate.compare(initDate) >= DatatypeConstants.EQUAL;

		}).collect(Collectors.toList()));

		return EntityTransformer.toMiscLineItemHist(mscLnItmHstsTmp);

	}


}
