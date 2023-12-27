package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCustPK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.BillToRelationCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyStatusCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentCreditStatusCd;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.Comparator;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.ComparatorFactory;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.xpo.ltl.shipment.service.impl.updateshipment.comparator.EntityComparer.findDifferences;

public abstract class AbstractAsMatchedParty extends AbstractUpdate {

	private static final Logger logger = LogManager.getLogger(AbstractAsMatchedParty.class);
	private static final String SHM_AS_ENTD_CUST = "ShmAsEntdCust";

	@Inject
	private ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;

	public static List<ShmAsEntdCust> resetSeqNumberShmAsEntdCust(
			Long shipmentInstId, List<ShmAsEntdCust> shmAsEntdCusts) {
		List<ShmAsEntdCust> newList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmAsEntdCusts)) {

			sortListByLongField(shmAsEntdCusts, ShmAsEntdCust::getId, ShmAsEntdCustPK::getSeqNbr);
			AtomicReference<Long> seq = new AtomicReference<>(1L);
			shmAsEntdCusts.forEach(shmAsEntdCust -> {
				ShmAsEntdCust shmAsEntdCustNew = new ShmAsEntdCust();
				copyFields(shmAsEntdCust, shmAsEntdCustNew);

				ShmAsEntdCustPK shmAsEntdCustPK = new ShmAsEntdCustPK();
				shmAsEntdCustPK.setShpInstId(shipmentInstId);
				shmAsEntdCustPK.setSeqNbr(seq.getAndSet(seq.get() + 1));
				shmAsEntdCustNew.setId(shmAsEntdCustPK);
				newList.add(shmAsEntdCustNew);

			});
		}
		return newList;
	}

	@LogExecutionTime
	public void updateAsMatchedParties(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCusts,
			String transactionCd) throws ValidationException {

		if (CollectionUtils.isNotEmpty(shmAsEntdCusts)) {

			setTxInfo(transactionContext, shmAsEntdCusts, transactionCd);

			shipmentAsEnteredCustomerDAO.persist(shmAsEntdCusts, entityManager);

			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				shmAsEntdCusts.forEach(shmAsEntdCust -> {
					try {
						final Function<DB2ShmAsEntdCust, Boolean> checkVersionFunctionShmAsEntdCust = getCheckVersionFunctionShmAsEntdCust(
								new Timestamp(new Date().getTime()));
						setShmAsEntdCustDefaultValues(shmAsEntdCust);
						shipmentAsEnteredCustomerDAO.updateDB2ShmAsEntdCust(shmAsEntdCust,
								checkVersionFunctionShmAsEntdCust,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.AS_ENTD_CUST_UPDATE_FAILED,
								SHM_AS_ENTD_CUST,
								e,
								transactionContext);
					}

				});
			}
		}
	}

	@LogExecutionTime
	public void deleteAsMatchedParties(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCusts) {

		if (CollectionUtils.isNotEmpty(shmAsEntdCusts)) {
			shipmentAsEnteredCustomerDAO.remove(shmAsEntdCusts, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				shmAsEntdCusts.forEach(commodity -> {
					try {
						final Function<DB2ShmAsEntdCust, Boolean> checkVersionFunctionShmAsEntdCust = getCheckVersionFunctionShmAsEntdCust(
								new Timestamp(new Date().getTime()));
						shipmentAsEnteredCustomerDAO.deleteDB2ShmAsEntdCust(commodity.getId(),
								checkVersionFunctionShmAsEntdCust,
								db2EntityManager,
								transactionContext);
					} catch (ValidationException | NotFoundException e) {
						getException(ServiceErrorMessage.AS_ENTD_CUST_DELETE_FAILED,
								SHM_AS_ENTD_CUST,
								e,
								transactionContext);
					}

				});
			}
		}
	}

	@LogExecutionTime
	public void insertAsMatchedParties(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCusts,
			String transactionCd) throws ValidationException {
		if (CollectionUtils.isNotEmpty(shmAsEntdCusts)) {
			setTxInfo(transactionContext, shmAsEntdCusts, transactionCd);
			try {
				shipmentAsEnteredCustomerDAO.persist(shmAsEntdCusts, entityManager);
				if (appContext.getDb2CommitEnabledForUpdateShipment()) {
					shmAsEntdCusts.forEach(shmAsEntdCust -> {
						setShmAsEntdCustDefaultValues(shmAsEntdCust);
						shipmentAsEnteredCustomerDAO.createDB2ShmAsEntdCust(shmAsEntdCust, db2EntityManager);
					});
				}
			} catch (Exception e) {
				getException(ServiceErrorMessage.AS_ENTD_CUST_CREATE_FAILED, SHM_AS_ENTD_CUST, e, transactionContext);
			}
		}
	}

	private void setTxInfo(
			TransactionContext transactionContext, List<ShmAsEntdCust> shmAsEntdCusts, String transactionCd) {
		shmAsEntdCusts.forEach(shmAsEntdCust -> {
			shmAsEntdCust.setLstUpdtTmst(new Timestamp(new Date().getTime()));
			shmAsEntdCust.setLstUpdtTranCd(transactionCd);
			shmAsEntdCust.setLstUpdtUid(getUserFromContext(transactionContext));

		});
	}

	public Function<DB2ShmAsEntdCust, Boolean> getCheckVersionFunctionShmAsEntdCust(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	protected List<ShmAsEntdCust> getAbcAsMatchedPartiesForDelete(
			List<AsMatchedParty> asMatchedParties, List<ShmAsEntdCust> shmAsEntdCusts) {
		List<ShmAsEntdCust> result = new ArrayList<>();

		AsMatchedParty shipper = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.SHPR))
				.findAny()
				.orElse(null);
		AsMatchedParty consignee = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.CONS))
				.findAny()
				.orElse(null);
		AsMatchedParty billToInb = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.BILL_TO_INB))
				.findAny()
				.orElse(null);
		AsMatchedParty billToOtb = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.BILL_TO_OTB))
				.findAny()
				.orElse(null);

		ShmAsEntdCust shmShipper = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.SHPR.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmConsignee = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.CONS.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmBillToInb = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.BILL_TO_INB.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmBillToOtb = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.BILL_TO_OTB.value())))
				.findAny()
				.orElse(null);

		if (Objects.isNull(shipper) || (Objects.nonNull(shmShipper) && !(Objects.nonNull(shipper.getCisCustNbr())
				&& shipper.getCisCustNbr().compareTo(shmShipper.getCisCustNbr().toBigInteger()) == 0
				&& (Objects.nonNull(shmShipper.getAsMchMadCd()) ?
				shmShipper.getAsMchMadCd().trim() :
				StringUtils.EMPTY).equals(Objects.nonNull(shipper.getAsMatchedMadCd()) ?
				shipper.getAsMatchedMadCd().trim() :
				StringUtils.EMPTY)))) {

			result.add(shmShipper);
		}

		if (Objects.isNull(consignee) || (Objects.nonNull(shmConsignee) && !(Objects.nonNull(consignee.getCisCustNbr())
				&& consignee.getCisCustNbr().compareTo(shmConsignee.getCisCustNbr().toBigInteger()) == 0
				&& (Objects.nonNull(shmConsignee.getAsMchMadCd()) ?
				shmConsignee.getAsMchMadCd().trim() :
				StringUtils.EMPTY).equals(Objects.nonNull(consignee.getAsMatchedMadCd()) ?
				consignee.getAsMatchedMadCd().trim() :
				StringUtils.EMPTY)))) {

			result.add(shmConsignee);
		}
		if (Objects.isNull(billToInb) || (Objects.nonNull(shmBillToInb) && !(Objects.nonNull(billToInb.getCisCustNbr())
				&& billToInb.getCisCustNbr().compareTo(shmBillToInb.getCisCustNbr().toBigInteger()) == 0
				&& (Objects.nonNull(shmBillToInb.getAsMchMadCd()) ?
				shmBillToInb.getAsMchMadCd().trim() :
				StringUtils.EMPTY).equals(Objects.nonNull(billToInb.getAsMatchedMadCd()) ?
				billToInb.getAsMatchedMadCd().trim() :
				StringUtils.EMPTY)))) {

			result.add(shmBillToInb);
		}
		if (Objects.isNull(billToOtb) || (Objects.nonNull(shmBillToOtb) && !(Objects.nonNull(billToOtb.getCisCustNbr())
				&& billToOtb.getCisCustNbr().compareTo(shmBillToOtb.getCisCustNbr().toBigInteger()) == 0
				&& (Objects.nonNull(shmBillToOtb.getAsMchMadCd()) ?
				shmBillToOtb.getAsMchMadCd().trim() :
				StringUtils.EMPTY).equals(Objects.nonNull(billToOtb.getAsMatchedMadCd()) ?
				billToOtb.getAsMatchedMadCd().trim() :
				StringUtils.EMPTY)))) {

			result.add(shmBillToOtb);
		}

		return result.stream().filter(Objects::nonNull).collect(Collectors.toList());
	}

	protected List<ShmAsEntdCust> getAbcAsMatchedPartiesForInsert(
			Long shpInstId,
			List<AsMatchedParty> asMatchedParties,
			List<ShmAsEntdCust> shmAsEntdCusts,
			TransactionContext transactionContext) {
		List<ShmAsEntdCust> result = new ArrayList<>();

		AsMatchedParty shipper = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.SHPR))
				.findAny()
				.orElse(null);
		AsMatchedParty consignee = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.CONS))
				.findAny()
				.orElse(null);
		AsMatchedParty billToInb = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.BILL_TO_INB))
				.findAny()
				.orElse(null);
		AsMatchedParty billToOtb = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.BILL_TO_OTB))
				.findAny()
				.orElse(null);

		ShmAsEntdCust shmShipper = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.SHPR.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmConsignee = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.CONS.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmBillToInb = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.BILL_TO_INB.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmBillToOtb = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.BILL_TO_OTB.value())))
				.findAny()
				.orElse(null);

		try {

			if (Objects.isNull(shmShipper) || (Objects.nonNull(shipper) && !(Objects.nonNull(shipper.getCisCustNbr())
					&& shipper.getCisCustNbr().compareTo(shmShipper.getCisCustNbr().toBigInteger()) == 0
					&& (Objects.nonNull(shmShipper.getAsMchMadCd()) ?
					shmShipper.getAsMchMadCd().trim() :
					StringUtils.EMPTY).equals(Objects.nonNull(shipper.getAsMatchedMadCd()) ?
					shipper.getAsMatchedMadCd().trim() :
					StringUtils.EMPTY)))) {
				ShmAsEntdCust shmAsEntdCust = new ShmAsEntdCust();
				ShmAsEntdCustPK id = new ShmAsEntdCustPK();
				id.setShpInstId(shpInstId);
				DtoTransformer.toShmAsEntdCust(shipper, shmAsEntdCust);
				shmAsEntdCust.setId(id);
				if (Objects.nonNull(shmShipper)) {
					shmAsEntdCust.getId().setSeqNbr(shmShipper.getId().getSeqNbr());
				}
				result.add(shmAsEntdCust);
			}
			if (Objects.isNull(shmConsignee) || (Objects.nonNull(consignee) && !(
					Objects.nonNull(consignee.getCisCustNbr())
							&& consignee.getCisCustNbr().compareTo(shmConsignee.getCisCustNbr().toBigInteger()) == 0
							&& (Objects.nonNull(shmConsignee.getAsMchMadCd()) ?
							shmConsignee.getAsMchMadCd().trim() :
							StringUtils.EMPTY).equals(Objects.nonNull(consignee.getAsMatchedMadCd()) ?
							consignee.getAsMatchedMadCd().trim() :
							StringUtils.EMPTY)))) {
				ShmAsEntdCust shmAsEntdCust = new ShmAsEntdCust();
				ShmAsEntdCustPK id = new ShmAsEntdCustPK();
				id.setShpInstId(shpInstId);
				DtoTransformer.toShmAsEntdCust(consignee, shmAsEntdCust);
				shmAsEntdCust.setId(id);
				if (Objects.nonNull(shmConsignee)) {
					shmAsEntdCust.getId().setSeqNbr(shmConsignee.getId().getSeqNbr());
				}
				result.add(shmAsEntdCust);

			}
			if ((Objects.isNull(shmBillToInb) && Objects.nonNull(billToInb)) || (Objects.nonNull(billToInb) && !(
					Objects.nonNull(billToInb.getCisCustNbr())
							&& billToInb.getCisCustNbr().compareTo(shmBillToInb.getCisCustNbr().toBigInteger()) == 0
							&& (Objects.nonNull(shmBillToInb.getAsMchMadCd()) ?
							shmBillToInb.getAsMchMadCd().trim() :
							StringUtils.EMPTY).equals(Objects.nonNull(billToInb.getAsMatchedMadCd()) ?
							billToInb.getAsMatchedMadCd().trim() :
							StringUtils.EMPTY)))) {

				ShmAsEntdCust shmAsEntdCust = new ShmAsEntdCust();
				ShmAsEntdCustPK id = new ShmAsEntdCustPK();
				id.setShpInstId(shpInstId);
				DtoTransformer.toShmAsEntdCust(billToInb, shmAsEntdCust);
				shmAsEntdCust.setId(id);
				if (Objects.nonNull(shmBillToInb)) {
					shmAsEntdCust.getId().setSeqNbr(shmBillToInb.getId().getSeqNbr());
				}
				result.add(shmAsEntdCust);

			}
			if ((Objects.isNull(shmBillToOtb) && Objects.nonNull(billToOtb)) || (Objects.nonNull(billToOtb) && !(
					Objects.nonNull(billToOtb.getCisCustNbr())
							&& billToOtb.getCisCustNbr().compareTo(shmBillToOtb.getCisCustNbr().toBigInteger()) == 0
							&& (Objects.nonNull(shmBillToOtb.getAsMchMadCd()) ?
							shmBillToOtb.getAsMchMadCd().trim() :
							StringUtils.EMPTY).equals(Objects.nonNull(billToOtb.getAsMatchedMadCd()) ?
							billToOtb.getAsMatchedMadCd().trim() :
							StringUtils.EMPTY)))) {

				ShmAsEntdCust shmAsEntdCust = new ShmAsEntdCust();
				ShmAsEntdCustPK id = new ShmAsEntdCustPK();
				id.setShpInstId(shpInstId);
				DtoTransformer.toShmAsEntdCust(billToOtb, shmAsEntdCust);
				shmAsEntdCust.setId(id);
				if (Objects.nonNull(shmBillToOtb)) {
					shmAsEntdCust.getId().setSeqNbr(shmBillToOtb.getId().getSeqNbr());
				}
				result.add(shmAsEntdCust);
			}
		} catch (Exception e) {
			getException(ServiceErrorMessage.AS_ENTD_CUST_CREATE_FAILED, "AdvBydCarr", e, transactionContext);
		}

		return result;
	}

	protected List<ShmAsEntdCust> getAbcAsMatchedPartiesForUpdate(
			List<AsMatchedParty> asMatchedParties, List<ShmAsEntdCust> shmAsEntdCusts) {
		List<ShmAsEntdCust> result = new ArrayList<>();

		AsMatchedParty shipper = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.SHPR))
				.findAny()
				.orElse(null);
		AsMatchedParty consignee = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.CONS))
				.findAny()
				.orElse(null);
		AsMatchedParty billToInb = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.BILL_TO_INB))
				.findAny()
				.orElse(null);
		AsMatchedParty billToOtb = asMatchedParties
				.stream()
				.filter(asMatchedParty -> asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.BILL_TO_OTB))
				.findAny()
				.orElse(null);

		ShmAsEntdCust shmShipper = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.SHPR.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmConsignee = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.CONS.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmBillToInb = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.BILL_TO_INB.value())))
				.findAny()
				.orElse(null);
		ShmAsEntdCust shmBillToOtb = shmAsEntdCusts
				.stream()
				.filter(shmAsEntdCust -> Objects.nonNull(shmAsEntdCust.getTypCd()) && shmAsEntdCust
						.getTypCd()
						.equals(getMatchedPartyTypeCd(MatchedPartyTypeCd.BILL_TO_OTB.value())))
				.findAny()
				.orElse(null);

		if (Objects.nonNull(shipper) && Objects.nonNull(shmShipper) && (Objects.nonNull(shipper.getCisCustNbr())
				&& shipper.getCisCustNbr().compareTo(shmShipper.getCisCustNbr().toBigInteger()) == 0
				&& (Objects.nonNull(shmShipper.getAsMchMadCd()) ?
				shmShipper.getAsMchMadCd().trim() :
				StringUtils.EMPTY).equals(Objects.nonNull(shipper.getAsMatchedMadCd()) ?
				shipper.getAsMatchedMadCd().trim() :
				StringUtils.EMPTY))) {
			ShmAsEntdCust shmAsEntdCustShippToCheck = new ShmAsEntdCust();
			copyFields(shmShipper, shmAsEntdCustShippToCheck);
			setUpdateValues(shipper, shmShipper);
			List<String> differences = compareAsMatchedParty(shmShipper, shmAsEntdCustShippToCheck);
			if (CollectionUtils.isNotEmpty(differences)) {
				result.add(shmShipper);
			}

		}
		if (Objects.nonNull(consignee) && Objects.nonNull(shmConsignee) && (Objects.nonNull(consignee.getCisCustNbr())
				&& consignee.getCisCustNbr().compareTo(shmConsignee.getCisCustNbr().toBigInteger()) == 0
				&& (Objects.nonNull(shmConsignee.getAsMchMadCd()) ?
				shmConsignee.getAsMchMadCd().trim() :
				StringUtils.EMPTY).equals(Objects.nonNull(consignee.getAsMatchedMadCd()) ?
				consignee.getAsMatchedMadCd().trim() :
				StringUtils.EMPTY))) {
			ShmAsEntdCust shmAsEntdCustConsToCheck = new ShmAsEntdCust();
			copyFields(shmConsignee, shmAsEntdCustConsToCheck);
			setUpdateValues(consignee, shmConsignee);
			List<String> differences = compareAsMatchedParty(shmConsignee, shmAsEntdCustConsToCheck);
			if (CollectionUtils.isNotEmpty(differences)) {
				result.add(shmConsignee);
			}
		}
		if (Objects.nonNull(billToInb) && Objects.nonNull(shmBillToInb) && (Objects.nonNull(billToInb.getCisCustNbr())
				&& billToInb.getCisCustNbr().compareTo(shmBillToInb.getCisCustNbr().toBigInteger()) == 0
				&& (Objects.nonNull(shmBillToInb.getAsMchMadCd()) ?
				shmBillToInb.getAsMchMadCd().trim() :
				StringUtils.EMPTY).equals(Objects.nonNull(billToInb.getAsMatchedMadCd()) ?
				billToInb.getAsMatchedMadCd().trim() :
				StringUtils.EMPTY))) {

			ShmAsEntdCust shmAsEntdCustBillToInbToCheck = new ShmAsEntdCust();
			copyFields(shmBillToInb, shmAsEntdCustBillToInbToCheck);
			setUpdateValues(billToInb, shmBillToInb);
			List<String> differences = compareAsMatchedParty(shmBillToInb, shmAsEntdCustBillToInbToCheck);
			if (CollectionUtils.isNotEmpty(differences)) {
				result.add(shmBillToInb);
			}
		}
		if (Objects.nonNull(billToOtb) && Objects.nonNull(shmBillToOtb) && (Objects.nonNull(billToOtb.getCisCustNbr())
				&& billToOtb.getCisCustNbr().compareTo(shmBillToOtb.getCisCustNbr().toBigInteger()) == 0
				&& (Objects.nonNull(shmBillToOtb.getAsMchMadCd()) ?
				shmBillToOtb.getAsMchMadCd().trim() :
				StringUtils.EMPTY).equals(Objects.nonNull(billToOtb.getAsMatchedMadCd()) ?
				billToOtb.getAsMatchedMadCd().trim() :
				StringUtils.EMPTY))) {

			ShmAsEntdCust shmAsEntdCustBillToOtbToCheck = new ShmAsEntdCust();
			copyFields(shmBillToOtb, shmAsEntdCustBillToOtbToCheck);
			setUpdateValues(billToOtb, shmBillToOtb);
			List<String> differences = compareAsMatchedParty(shmBillToOtb, shmAsEntdCustBillToOtbToCheck);
			if (CollectionUtils.isNotEmpty(differences)) {
				result.add(shmBillToOtb);
			}
		}

		return result;
	}

	@LogExecutionTime
	protected List<String> compareAsMatchedParty(ShmAsEntdCust source, ShmAsEntdCust target) {
		Comparator<ShmAsEntdCust> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmAsEntdCust.class.getName(), differences, source.getId().getShpInstId());
		return differences;
	}

	private void setUpdateValues(AsMatchedParty asMatchedParty, ShmAsEntdCust shmAsEntdCust) {

		if (Objects.nonNull(asMatchedParty.getAsMatchedMadCd()) && StringUtils.isNotEmpty(asMatchedParty.getAsMatchedMadCd())) {
			shmAsEntdCust.setAsMchMadCd(asMatchedParty.getAsMatchedMadCd());
		}

		if (Objects.nonNull(asMatchedParty.getName1()) && StringUtils.isNotEmpty(asMatchedParty.getName1())) {
			shmAsEntdCust.setName1Txt(asMatchedParty.getName1());
		}

		if (Objects.nonNull(asMatchedParty.getName2()) && StringUtils.isNotEmpty(asMatchedParty.getName2())) {
			shmAsEntdCust.setName2Txt(asMatchedParty.getName2());
		}

		if (Objects.nonNull(asMatchedParty.getAddress()) && StringUtils.isNotEmpty(asMatchedParty.getAddress())) {
			shmAsEntdCust.setAddrTxt(asMatchedParty.getAddress());
		}

		if (Objects.nonNull(asMatchedParty.getCity()) && StringUtils.isNotEmpty(asMatchedParty.getCity())) {
			shmAsEntdCust.setCtyTxt(asMatchedParty.getCity());
		}

		if (Objects.nonNull(asMatchedParty.getStateCd()) && StringUtils.isNotEmpty(asMatchedParty.getStateCd())) {
			shmAsEntdCust.setStCd(asMatchedParty.getStateCd());
		}

		if (Objects.nonNull(asMatchedParty.getZip6()) && StringUtils.isNotEmpty(asMatchedParty.getZip6())) {
			shmAsEntdCust.setZip6Txt(asMatchedParty.getZip6());
		}

		if (Objects.nonNull(asMatchedParty.getCountryCd()) && StringUtils.isNotEmpty(asMatchedParty.getCountryCd())) {
			shmAsEntdCust.setCntryCd(asMatchedParty.getCountryCd());
		}
		if (Objects.nonNull(asMatchedParty.getCreditStatusCd()) ) {
			shmAsEntdCust.setCredStatCd(getShipmentCreditStatusCd(asMatchedParty.getCreditStatusCd().value()));
		}

		if (Objects.nonNull(asMatchedParty.getBillToRelationshipCd()) && Objects.nonNull(asMatchedParty.getTypeCd()) &&  (asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.BILL_TO_OTB) || asMatchedParty.getTypeCd().equals(MatchedPartyTypeCd.BILL_TO_INB))  ) {
			shmAsEntdCust.setBiltoRelCd(getBillToRelationCd(asMatchedParty.getBillToRelationshipCd().value()));
		}
		if (Objects.nonNull(asMatchedParty.getMatchedStatusCd())) {
			shmAsEntdCust.setMchStatCd(getMatchedPartyStatusCd(asMatchedParty.getMatchedStatusCd().value()));
		}
	}

	public String getMatchedPartyStatusCd(String name) {
		Field[] statusDeclaredFields = MatchedPartyStatusCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}
	public String getBillToRelationCd(String name) {
		Field[] statusDeclaredFields = BillToRelationCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}
	public String getMatchedPartyTypeCd(String name) {
		Field[] statusDeclaredFields = MatchedPartyTypeCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}
	public String getShipmentCreditStatusCd(String name) {
		Field[] statusDeclaredFields = ShipmentCreditStatusCd.class.getDeclaredFields();
		return getAlternateValue(name, statusDeclaredFields);
	}

	private void setShmAsEntdCustDefaultValues(ShmAsEntdCust shmAsEntdCust) {

		if (Objects.isNull(shmAsEntdCust.getAlternateCustNbr())) {
			shmAsEntdCust.setAlternateCustNbr(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmAsEntdCust.getBrkrCustKeyNbr())) {
			shmAsEntdCust.setBrkrCustKeyNbr(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmAsEntdCust.getCisCustNbr())) {
			shmAsEntdCust.setCisCustNbr(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmAsEntdCust.getLstMchTmst())) {
			shmAsEntdCust.setLstMchTmst(getDefaultTimestamp());
		}
		if (Objects.isNull(shmAsEntdCust.getLstUpdtTmst())) {
			shmAsEntdCust.setLstUpdtTmst(getDefaultTimestamp());
		}
		if (Objects.isNull(shmAsEntdCust.getAddrTxt())) {
			shmAsEntdCust.setAddrTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getAllShpmtPpdInd())) {
			shmAsEntdCust.setAllShpmtPpdInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getArchiveCntlCd())) {
			shmAsEntdCust.setArchiveCntlCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getAsMchMadCd())) {
			shmAsEntdCust.setAsMchMadCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getBiltoRelCd())) {
			shmAsEntdCust.setBiltoRelCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getCntryCd())) {
			shmAsEntdCust.setCntryCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getCredStatCd())) {
			shmAsEntdCust.setCredStatCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getCtyTxt())) {
			shmAsEntdCust.setCtyTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getDebtorInd())) {
			shmAsEntdCust.setDebtorInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getDirCd())) {
			shmAsEntdCust.setDirCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getLstUpdtUid())) {
			shmAsEntdCust.setLstUpdtUid(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getMchInitTxt())) {
			shmAsEntdCust.setMchInitTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getMchSourceCd())) {
			shmAsEntdCust.setMchSourceCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getMchStatCd())) {
			shmAsEntdCust.setMchStatCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getName1Txt())) {
			shmAsEntdCust.setName1Txt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getName2Txt())) {
			shmAsEntdCust.setName2Txt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getPacdNbr())) {
			shmAsEntdCust.setPacdNbr(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getPccdNbr())) {
			shmAsEntdCust.setPccdNbr(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getPextNbr())) {
			shmAsEntdCust.setPextNbr(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getPhonNbr())) {
			shmAsEntdCust.setPhonNbr(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getPodImgInd())) {
			shmAsEntdCust.setPodImgInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getPodRqrdInd())) {
			shmAsEntdCust.setPodRqrdInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getPrefPmtCrncyCd())) {
			shmAsEntdCust.setPrefPmtCrncyCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getSelfInvcInd())) {
			shmAsEntdCust.setSelfInvcInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getStCd())) {
			shmAsEntdCust.setStCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getTypCd())) {
			shmAsEntdCust.setTypCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getUseAsEntrdInd())) {
			shmAsEntdCust.setUseAsEntrdInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getZip4RestUsTxt())) {
			shmAsEntdCust.setZip4RestUsTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getZip6Txt())) {
			shmAsEntdCust.setZip6Txt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAsEntdCust.getEMailId())) {
			shmAsEntdCust.setEMailId(StringUtils.SPACE);
		}
	}

	private Timestamp getDefaultTimestamp() {
		String dateString = "0001-12-30T04:00:00.000Z";

		try {

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			Date date = sdf.parse(dateString);
			return new Timestamp(date.getTime());
		} catch (ParseException e) {
			logger.error(e);
		}
		return null;
	}
}
