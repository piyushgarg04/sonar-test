package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.InvoiceCurrencyCd;
import com.xpo.ltl.api.shipment.v2.RatingCurrencyCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.api.shipment.v2.WarrantyStatusCd;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.Comparator;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.ComparatorFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValuesFactory;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.xpo.ltl.shipment.service.impl.updateshipment.comparator.EntityComparer.findDifferences;

public abstract class AbstractShipment extends AbstractUpdate {

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject
	private LoadValFactory loadValFactory;

	@Inject
	private ShmEventDelegate shmEventDelegate;

	public static String getBillStatusCd(String name) {
		Field[] statusDeclaredFields = BillStatusCd.class.getDeclaredFields();
		return getAlternateValue(name, statusDeclaredFields);
	}

	public static String getWarrantyStatusCd(String name) {
		Field[] statusDeclaredFields = WarrantyStatusCd.class.getDeclaredFields();
		return getAlternateValue(name, statusDeclaredFields);
	}

	@LogExecutionTime
	public void updateShipment(
			UpdateShipmentRqst updateShipmentRqst,
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			ShmShipment shmShipment,
			String transactionCd,
			EventLogTypeCd eventLogTypeCd,
			EventLogSubTypeCd eventLogSubTypeCd,
			ShipmentUpdateActionCd shipmentUpdateActionCd) throws ServiceException {

		LoadValuesFactory loadValuesFactory = loadValFactory.getFactoryImplementation(shipmentUpdateActionCd);
		LoadValues<UpdateShipmentRqst, ShmShipment> shmipmentLoadValues = loadValuesFactory.getFactoryImplementation(
				ShmShipment.class);

		shmipmentLoadValues.loadtValues(updateShipmentRqst, shmShipment);

		Timestamp timestamp = new Timestamp(new Date().getTime());
		shmShipment.setLstUpdtTmst(timestamp);
		String userId;
		if (ShipmentUpdateActionCd.AUTO_RATE.equals(shipmentUpdateActionCd)) {
			userId = getUserForAutoRate(updateShipmentRqst,transactionContext);
		} else {
			userId = getUserFromContext(transactionContext);
		}
		shmShipment.setLstUpdtUid(userId);
		shmShipment.setLstUpdtTranCd(transactionCd);
		AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmAndUserId(transactionCd,
				userId,
				transactionContext);

		try {
			shmShipmentSubDAO.persist(shmShipment, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				setDefaultShipmentValues(shmShipment);
				shmShipmentSubDAO.updateDB2ShmShipment(shmShipment, timestamp, transactionContext, db2EntityManager);
			}
		} catch (Exception e) {
			getException(ServiceErrorMessage.SHIPMENT_UPDATE_FAILED, "Shipment", e, transactionContext);
		}

		try {
			this.createShipmentEvent(eventLogTypeCd,
					eventLogSubTypeCd,
					shmShipment,
					"ARE",
					transactionCd,
					entityManager,
					auditInfo);
		} catch (Exception e) {
			getException(ServiceErrorMessage.SHIPMENT_CREATE_FAILED, "ShipmentEvent", e, transactionContext);
		}
	}

	private void setDefaultShipmentValues(ShmShipment shmShipment) {

		if (Objects.isNull(shmShipment.getAbsMinChgInd())) {
			shmShipment.setAbsMinChgInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getArchiveCntlCd())) {
			shmShipment.setArchiveCntlCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getAutoRateableInd())) {
			shmShipment.setAutoRateableInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getCftPrflMthdCd())) {
			shmShipment.setCftPrflMthdCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getCftPrflTypeCd())) {
			shmShipment.setCftPrflTypeCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getConsUnldTrlrCd())) {
			shmShipment.setConsUnldTrlrCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getCurrSicCd())) {
			shmShipment.setCurrSicCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getDfltTrfId())) {
			shmShipment.setDfltTrfId(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getDlvrInfoRqdCd())) {
			shmShipment.setDlvrInfoRqdCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getDlvrSigNmTxt())) {
			shmShipment.setDlvrSigNmTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getDlvryQalfrCd())) {
			shmShipment.setDlvryQalfrCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getExemptRsnCd())) {
			shmShipment.setExemptRsnCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getFoodPoisonCd())) {
			shmShipment.setFoodPoisonCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getFromPortCd())) {
			shmShipment.setFromPortCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getLstMovrProTxt())) {
			shmShipment.setLstMovrProTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getLstMvRptgSicCd())) {
			shmShipment.setLstMvRptgSicCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getMovrSuffix())) {
			shmShipment.setMovrSuffix(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getNtfictnCd())) {
			shmShipment.setNtfictnCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getObcPkpDlvRteCd())) {
			shmShipment.setObcPkpDlvRteCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getParentInstId())) {
			shmShipment.setParentInstId(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmShipment.getPkupBackdateInd())) {
			shmShipment.setPkupBackdateInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getReweighWgtLbs())) {
			shmShipment.setReweighWgtLbs(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmShipment.getRtAudtrInit())) {
			shmShipment.setRtAudtrInit(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getRtePfxTxt())) {
			shmShipment.setRtePfxTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getRteSfxTxt())) {
			shmShipment.setRteSfxTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getRteTypCd())) {
			shmShipment.setRteTypCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getRtgOvrdCd())) {
			shmShipment.setRtgOvrdCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getShpSvcStatCd())) {
			shmShipment.setShpSvcStatCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getShpmtAcqrTypCd())) {
			shmShipment.setShpmtAcqrTypCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getShprLdTrlrCd())) {
			shmShipment.setShprLdTrlrCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getSvcCalcStatCd())) {
			shmShipment.setSvcCalcStatCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getToPortCd())) {
			shmShipment.setToPortCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getTransactionId())) {
			shmShipment.setTransactionId(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getWarrantyStatCd())) {
			shmShipment.setWarrantyStatCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getCallForApptInd())) {
			shmShipment.setCallForApptInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getDebtorTermFlipInd())) {
			shmShipment.setDebtorTermFlipInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getExcessiveValueInd())) {
			shmShipment.setExcessiveValueInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getInspectedInd())) {
			shmShipment.setInspectedInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getInvcCrncd())) {
			shmShipment.setInvcCrncd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getMxDoorToDoorInd())) {
			shmShipment.setMxDoorToDoorInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getRtgCrncd())) {
			shmShipment.setRtgCrncd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getRtgTrfId())) {
			shmShipment.setRtgTrfId(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getRtOrRtAudtqNm())) {
			shmShipment.setRtOrRtAudtqNm(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getChrgToCd())) {
			shmShipment.setChrgToCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getSingleShpmtAcqrInd())) {
			shmShipment.setSingleShpmtAcqrInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmShipment.getBulkLqdInd())) {
			shmShipment.setBulkLqdInd(StringUtils.SPACE);
		}

	}

	@LogExecutionTime
	public void createShipmentEvent(
			EventLogTypeCd eventLogTypeCd,
			EventLogSubTypeCd eventLogSubTypeCd,
			ShmShipment shmShipment,
			String rptgSicCd,
			String transactionCd,
			EntityManager entityManager,
			AuditInfo auditInfo) {
		shmEventDelegate.createEvent(0L,
				eventLogTypeCd,
				eventLogSubTypeCd,
				shmShipment,
				null,
				rptgSicCd,
				Optional.empty(),
				transactionCd,
				entityManager,
				auditInfo);
	}

	public String getDeliveryQualifierCd(String name) {
		Field[] statusDeclaredFields = DeliveryQualifierCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);

	}

	public String getInvoiceCurrencyCd(String name) {
		Field[] statusDeclaredFields = InvoiceCurrencyCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}

	public String getRatingCurrencyCd(String name) {
		Field[] statusDeclaredFields = RatingCurrencyCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}

	protected List<String> compareShmShipment(ShmShipment source, ShmShipment target) {
		Comparator<ShmShipment> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmShipment.class.getName(), differences, source.getShpInstId());
		return differences;
	}

	public String getShipmentDiscCdFromDiscPct(BigDecimal discPct) {
		if (Objects.nonNull(discPct)) {
			String strDiscCd = discPct.toString();
			strDiscCd = strDiscCd.replace(".", StringUtils.EMPTY);
			if (strDiscCd.length() > 3) {
				return strDiscCd.substring(0, 3);
			} else {
				return strDiscCd;
			}
		}
		return StringUtils.EMPTY;
	}

}
