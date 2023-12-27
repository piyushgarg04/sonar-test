package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.jpa.QueryHints;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.dao.ShmShipmentDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmShipment;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmShipment_;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit_;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage_;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp_;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement_;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment_;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ChargeToCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.DateUtils;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmShipmentSubDAO extends ShmShipmentDAO<ShmShipment> {

	private static final String ARCHIVE_INDICATOR = "Y";
	private static final String ON_TRAILER_CD = "2";
	private static final String BILL_CLASS_FRT_SEG = BillClassCdTransformer.toCode(BillClassCd.ASTRAY_FRT_SEGMENT);
	private static final String MATCH_SCORE_FULL_MATCH = "70";
	private static final String SHM_SHIPMENT_SEQ = "SHM_SHIPMENT_SEQ";
	private static final String OUT_FOR_DELIVERY = MovementStatusCdTransformer.toCode(MovementStatusCd.OUT_FOR_DLVRY);
	private static final int BULK_SAVE_LIMIT = 200;

	private static String SHIPMENTS_BY_SHP_ID_SQL  = "SELECT shipment FROM ShmShipment shipment WHERE shipment.shpInstId in :shpIdList ";
	private static String DB2_SHIPMENTS_WITH_UR__BY_SHP_ID_SQL  = "SELECT * FROM SHM_SHIPMENT WHERE SHP_INST_ID in :shpIdList WITH UR";
	private static String SHIPMENTS_BY_PRO_NBR_SQL = "SELECT shipment FROM ShmShipment shipment WHERE shipment.proNbrTxt in :proList ORDER BY pkupDt DESC";
	private static String SHIPMENTS_BY_NON_ARCHIVED_PRO_NBR_SQL = "SELECT shipment FROM ShmShipment shipment WHERE "
			+ "shipment.proNbrTxt in :proList "
			+ "AND shipment.archiveInd = :archiveInd";
	private static final String SHIPMENT_ID_BY_PRO_NBR_SQL = "SELECT SHP_INST_ID FROM SHM_SHIPMENT";

	// Outstanding shipment lists for an appointment
    private static final String OUTSTANDING_SHP_APPT_FOR_CONSIGNEE =
                " SELECT shm.shpInstId, shm.proNbrTxt, shm.totPcsCnt, shm.totWgtLbs, shm.estimatedDlvrDt " +
                " FROM ShmShipment shm " +
                " JOIN ShmAsEntdCust cst ON shm.shpInstId = cst.id.shpInstId " +
                " WHERE (cst.cisCustNbr = :custInstId OR cst.alternateCustNbr = :custInstId) " +
                " AND cst.typCd = " + "'" + MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.CONS) + "'" +
                " AND cst.mchStatCd = " + "'" + MATCH_SCORE_FULL_MATCH + "'" +
                " AND shm.destTrmnlSicCd = :destSicCd " +
                " AND shm.mvmtStatCd <= " + "'" + MovementStatusCdTransformer.toCode(MovementStatusCd.INTERIM_DLVRY) + "'" +
                " AND (shm.ntfictnCd is null OR shm.ntfictnCd = ' ' ) " +
                " AND shm.billClassCd <> " + "'" + BillClassCdTransformer.toCode(BillClassCd.MASTER_SHPMT) + "'" + 
                " AND shm.billClassCd <> " + "'" + BillClassCdTransformer.toCode(BillClassCd.SPLIT_SEGMENT) + "'" + 
                " AND shm.billClassCd <> " + "'" + BillClassCdTransformer.toCode(BillClassCd.CLAIMS_OVRG_RPTG_BILL) + "'" +
                " AND shm.dlvryQalfrCd <> " + "'" + DeliveryQualifierCdTransformer.toCode(DeliveryQualifierCd.PARTIAL_SHORT) + "'" +
                " AND shm.dlvryQalfrCd <> " + "'" + DeliveryQualifierCdTransformer.toCode(DeliveryQualifierCd.SPOTTED) + "'" +
                " AND shm.pkupDt >= :pickupDtStart" + " AND shm.pkupDt <= :pickupDtEnd " +
                " AND shm.estimatedDlvrDt >= :estDeliveryDtStart AND shm.estimatedDlvrDt <= :estDeliveryDtEnd ";
    
    // Index variables
    private static final int SHP_INST_ID = 0;
    private static final int PRO_NBR = 1;
    private static final int TOT_PCT = 2;
    private static final int TOT_WGT = 3;
    private static final int EST_DLVY_DT = 4;    
    
    // END Outstanding shipment lists for an appointment
    
    // List shipments for appointment variables
    private static final String SHIPMENTS_FOR_APPOINTMENT =
            " SELECT shm.shpInstId,"
            + " shm.proNbrTxt,"
            + " shm.estimatedDlvrDt,"
            + " shm.totPcsCnt,"
            + " shm.totWgtLbs,"
            + " shm.totChrgAmt,"
            + " shm.billClassCd,"
            + " shm.chrgToCd,"
            + " shm.parentInstId,"
            + " tdc.tdcDtTypCd,"
            + " tdc.tdcDt1,"
            + " tdc.tdcDt2,"
            + " tdc.tdcTmTypCd,"
            + " tdc.tdcTm1,"
            + " tdc.tdcTm2";          

    private static final String MISC_SELECTION_FOR_APPTS =
            " , misc.lnTypCd,"
            + " misc.amt";

    private static final String SHIPMENTS_FOR_APPOINTMENT2 = 
              " FROM ShmShipment shm "
            + " LEFT JOIN ShmTmDtCritical tdc ON tdc.shpInstId = shm.shpInstId";
    
    private static final String SHIPMENTS_FOR_APPT_MISC_LN_ITM_JOIN = " LEFT JOIN ShmMiscLineItem misc ON misc.id.shpInstId = shm.shpInstId";
    
    private static final String SHIPMENTS_FOR_APPT_WHERE = " WHERE shm.shpInstId IN :shpInstIds";
    
    // END List shipments for appointment variables
    
    
	@Inject
	private AppContext appContext;

	/**
	 * Updates the DB2 entities with values from Oracle entities.
	 *
	 * @param shmShipments
	 * @param exadataLstUpdtTmst
	 * @param txnContext
	 * @param entityManager
	 * @throws ServiceException
	 */
	public void updateDB2ShmShipment(final List<ShmShipment> shmShipments, final Timestamp exadataLstUpdtTmst,
		final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
		for (final ShmShipment shmShipment : shmShipments) {
			updateDB2ShmShipment(shmShipment, exadataLstUpdtTmst, txnContext, entityManager);
		}
	}

	/**
	 * Updates the DB2 entity with values from Oracle entity.
	 *
	 * @param shmShipment
	 * @param exadataLstUpdtTmst
	 * @param txnContext
	 * @param db2EntityManager
	 * @throws ServiceException
	 */
	public void updateDB2ShmShipment(final ShmShipment shmShipment, final Timestamp exadataLstUpdtTmst,
		final TransactionContext txnContext, final EntityManager db2EntityManager) throws ServiceException {
		final Function<DB2ShmShipment, Boolean> checkVersionFunction = getCheckVersionFunction(exadataLstUpdtTmst);
		final BigDecimal parentInstId = shmShipment.getShmShipment() != null
				? BasicTransformer.toBigDecimal(shmShipment.getShmShipment().getShpInstId())
					: null;
				updateDB2ShmShipment(shmShipment, parentInstId, checkVersionFunction, db2EntityManager, txnContext);
	}

    // specific case used only for update handling unit weight.
    public void updateDb2ShmShipmentForUpdHUWeight(final ShmShipment shmShipment, final EntityManager db2EntityManager) {

        final CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(DB2ShmShipment.class);
        final Root<DB2ShmShipment> root = criteriaUpdate.from(DB2ShmShipment.class);

        criteriaUpdate.set(root.get(DB2ShmShipment_.reweighWgtLbs), shmShipment.getReweighWgtLbs());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTranCd), shmShipment.getLstUpdtTranCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTmst), shmShipment.getLstUpdtTmst());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtUid), shmShipment.getLstUpdtUid());

        final Path<Long> shpInstIdPath = root.get(DB2ShmShipment_.shpInstId);
        criteriaUpdate.where(cb.equal(shpInstIdPath, shmShipment.getShpInstId()));

        db2EntityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    // specific case used only for update handling unit.
    public void updateDb2ShmShipmentForUpdHU(final ShmShipment shmShipment, final EntityManager db2EntityManager) {

        final CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(DB2ShmShipment.class);
        final Root<DB2ShmShipment> root = criteriaUpdate.from(DB2ShmShipment.class);

        criteriaUpdate.set(root.get(DB2ShmShipment_.currSicCd), shmShipment.getCurrSicCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.mvmtStatCd), shmShipment.getMvmtStatCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.dlvryQalfrCd), shmShipment.getDlvryQalfrCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.poorlyPackagedInd), shmShipment.getPoorlyPackagedInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitSplitInd), shmShipment.getHandlingUnitSplitInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitPartialInd), shmShipment.getHandlingUnitPartialInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTranCd), shmShipment.getLstUpdtTranCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTmst), shmShipment.getLstUpdtTmst());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtUid), shmShipment.getLstUpdtUid());

        final Path<Long> shpInstIdPath = root.get(DB2ShmShipment_.shpInstId);
        criteriaUpdate.where(cb.equal(shpInstIdPath, shmShipment.getShpInstId()));

        db2EntityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    // specific case used only for update service standard info.
    public void updateDb2ShmShipmentForUpdServiceStdInfo(final ShmShipment shmShipment, final EntityManager db2EntityManager) {

        final CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(DB2ShmShipment.class);
        final Root<DB2ShmShipment> root = criteriaUpdate.from(DB2ShmShipment.class);

        criteriaUpdate.set(root.get(DB2ShmShipment_.stdTrnstDays), shmShipment.getStdTrnstDays());
        criteriaUpdate.set(root.get(DB2ShmShipment_.estTrnstDays), shmShipment.getEstTrnstDays());
        criteriaUpdate.set(root.get(DB2ShmShipment_.estimatedDlvrDt), shmShipment.getEstimatedDlvrDt());
        criteriaUpdate.set(root.get(DB2ShmShipment_.svcStrtDt), shmShipment.getSvcStrtDt());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTranCd), shmShipment.getLstUpdtTranCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTmst), shmShipment.getLstUpdtTmst());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtUid), shmShipment.getLstUpdtUid());

        final Path<Long> shpInstIdPath = root.get(DB2ShmShipment_.shpInstId);
        criteriaUpdate.where(cb.equal(shpInstIdPath, shmShipment.getShpInstId()));

        db2EntityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    // specific case used only for bulk upsert handling units.
    public void updateDb2ShmShipmentForBulkUpsertHUs(final ShmShipment shmShipment, final EntityManager db2EntityManager) {

        final CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(DB2ShmShipment.class);
        final Root<DB2ShmShipment> root = criteriaUpdate.from(DB2ShmShipment.class);

        criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitPartialInd), shmShipment.getHandlingUnitPartialInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.loosePcsCnt), shmShipment.getLoosePcsCnt());
        criteriaUpdate.set(root.get(DB2ShmShipment_.mtrzdPcsCnt), shmShipment.getMtrzdPcsCnt());
        criteriaUpdate.set(root.get(DB2ShmShipment_.totVolCft), shmShipment.getTotVolCft());
        criteriaUpdate.set(root.get(DB2ShmShipment_.pupVolPct), shmShipment.getPupVolPct());
        criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitExemptionInd), shmShipment.getHandlingUnitExemptionInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitExemptionRsn), shmShipment.getHandlingUnitExemptionRsn());

        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTranCd), shmShipment.getLstUpdtTranCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTmst), shmShipment.getLstUpdtTmst());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtUid), shmShipment.getLstUpdtUid());

        final Path<Long> shpInstIdPath = root.get(DB2ShmShipment_.shpInstId);
        criteriaUpdate.where(cb.equal(shpInstIdPath, shmShipment.getShpInstId()));

        db2EntityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    // specific case used only for update shipment skeketon.
    public void updateDb2ShmShipmentForUpdSkeketon(final ShmShipment shmShipment, final EntityManager db2EntityManager) {

        final CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(DB2ShmShipment.class);
        final Root<DB2ShmShipment> root = criteriaUpdate.from(DB2ShmShipment.class);

        criteriaUpdate.set(root.get(DB2ShmShipment_.lateTenderCd), shmShipment.getLateTenderCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.shpmtAcqrTypCd), shmShipment.getShpmtAcqrTypCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.shprLdTrlrCd), shmShipment.getShprLdTrlrCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.singleShpmtAcqrInd), shmShipment.getSingleShpmtAcqrInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.svcTypCd), shmShipment.getSvcTypCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.mvmtStatCd), shmShipment.getMvmtStatCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.origTrmnlSicCd), shmShipment.getOrigTrmnlSicCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.destTrmnlSicCd), shmShipment.getDestTrmnlSicCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.currSicCd), shmShipment.getCurrSicCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.totPcsCnt), shmShipment.getTotPcsCnt());
        criteriaUpdate.set(root.get(DB2ShmShipment_.totPlltCnt), shmShipment.getTotPlltCnt());
        criteriaUpdate.set(root.get(DB2ShmShipment_.totWgtLbs), shmShipment.getTotWgtLbs());
        criteriaUpdate.set(root.get(DB2ShmShipment_.mtrzdPcsCnt), shmShipment.getMtrzdPcsCnt());
        criteriaUpdate.set(root.get(DB2ShmShipment_.loosePcsCnt), shmShipment.getLoosePcsCnt());
        criteriaUpdate.set(root.get(DB2ShmShipment_.hazmatInd), shmShipment.getHazmatInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.frzbleInd), shmShipment.getFrzbleInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.garntdInd), shmShipment.getGarntdInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.foodPoisonCd), shmShipment.getFoodPoisonCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstMvRptgSicCd), shmShipment.getLstMvRptgSicCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstMvmtTmst), shmShipment.getLstMvmtTmst());
        criteriaUpdate.set(root.get(DB2ShmShipment_.totVolCft), shmShipment.getTotVolCft());
        criteriaUpdate.set(root.get(DB2ShmShipment_.pupVolPct), shmShipment.getPupVolPct());
        criteriaUpdate.set(root.get(DB2ShmShipment_.cftPrflMthdCd), shmShipment.getCftPrflMthdCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.cftPrflTypeCd), shmShipment.getCftPrflTypeCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.bulkLqdInd), shmShipment.getBulkLqdInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.mtrzdPcsKnwnInd), shmShipment.getMtrzdPcsKnwnInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitExemptionInd), shmShipment.getHandlingUnitExemptionInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitExemptionRsn), shmShipment.getHandlingUnitExemptionRsn());
        criteriaUpdate.set(root.get(DB2ShmShipment_.poorlyPackagedInd), shmShipment.getPoorlyPackagedInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.debtorTermFlipInd), shmShipment.getDebtorTermFlipInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.apptRqrdInd), shmShipment.getApptRqrdInd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTranCd), shmShipment.getLstUpdtTranCd());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTmst), shmShipment.getLstUpdtTmst());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtUid), shmShipment.getLstUpdtUid());

        final Path<Long> shpInstIdPath = root.get(DB2ShmShipment_.shpInstId);
        criteriaUpdate.where(cb.equal(shpInstIdPath, shmShipment.getShpInstId()));

        db2EntityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    public int bulkUpdateHandlingUnitExempByProNbrList(final List<String> proNbrList, final String exemptionInd,
        final String exemptReason, final AuditInfo auditInfo,
        final EntityManager entityManager) {

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(ShmShipment.class);
        final Root<ShmShipment> root = criteriaUpdate.from(ShmShipment.class);

        final Path<String> proNbrPath = root.get(ShmShipment_.proNbrTxt);
        criteriaUpdate.where(proNbrPath.in(proNbrList));

        if (StringUtils.isNotBlank(exemptReason)) {
            criteriaUpdate.set(root.get(ShmShipment_.handlingUnitExemptionRsn), exemptReason);
        }
        criteriaUpdate.set(root.get(ShmShipment_.handlingUnitExemptionInd), exemptionInd);
        criteriaUpdate.set(root.get(ShmShipment_.lstUpdtTranCd), auditInfo.getUpdateByPgmId());
        criteriaUpdate.set(root.get(ShmShipment_.lstUpdtTmst), BasicTransformer.toTimestamp(auditInfo.getUpdatedTimestamp()));
        criteriaUpdate.set(root.get(ShmShipment_.lstUpdtUid), auditInfo.getUpdateById());

        return entityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    public int db2BulkUpdateHandlingUnitExempByProNbrList(final List<String> proNbrList, final String exemptionInd,
        final String exemptReason, final AuditInfo auditInfo,
        final EntityManager db2EntityManager) {

        final CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(DB2ShmShipment.class);
        final Root<DB2ShmShipment> root = criteriaUpdate.from(DB2ShmShipment.class);

        final Path<String> proNbrPath = root.get(DB2ShmShipment_.proNbrTxt);
        criteriaUpdate.where(proNbrPath.in(proNbrList));

        if (StringUtils.isNotBlank(exemptReason)) {
            criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitExemptionRsn), exemptReason);
        }
        criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitExemptionInd), exemptionInd);
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTranCd), auditInfo.getUpdateByPgmId());
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTmst), BasicTransformer.toTimestamp(auditInfo.getUpdatedTimestamp()));
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtUid), auditInfo.getUpdateById());

        return db2EntityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    public int updateDB2ShmShipmentDimensionCaptureInfo(final Long shpInstId, final Optional<BigDecimal> pupVolPct,
        final Optional<BigDecimal> totVolCft,
        final Optional<String> cftPrflMthdCd, final Optional<String> cftPrflTypeCd, final Optional<String> huExemptionRsn,
        final String lstUpdtTranCd, final Timestamp lstUpdtTmst, final String lstUpdtUid,
        final EntityManager db2EntityManager) {

        final CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(DB2ShmShipment.class);
        final Root<DB2ShmShipment> root = criteriaUpdate.from(DB2ShmShipment.class);

        if (pupVolPct.isPresent()) {
            criteriaUpdate.set(root.get(DB2ShmShipment_.pupVolPct), pupVolPct.get());
        }
        if (totVolCft.isPresent()) {
            criteriaUpdate.set(root.get(DB2ShmShipment_.totVolCft), totVolCft.get());
        }
        if (cftPrflMthdCd.isPresent()) {
            criteriaUpdate.set(root.get(DB2ShmShipment_.cftPrflMthdCd), cftPrflMthdCd.get());
        }
        if (cftPrflTypeCd.isPresent()) {
            criteriaUpdate.set(root.get(DB2ShmShipment_.cftPrflTypeCd), cftPrflTypeCd.get());
        }
        if (huExemptionRsn.isPresent()) {
            criteriaUpdate.set(root.get(DB2ShmShipment_.handlingUnitExemptionRsn), huExemptionRsn.get());
        }

        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTranCd), lstUpdtTranCd);
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtTmst), lstUpdtTmst);
        criteriaUpdate.set(root.get(DB2ShmShipment_.lstUpdtUid), lstUpdtUid);

        final Path<Long> shpInstIdPath = root.get(DB2ShmShipment_.shpInstId);

        criteriaUpdate.where(cb.equal(shpInstIdPath, shpInstId));

        return db2EntityManager.createQuery(criteriaUpdate).executeUpdate();
    }

	private Function<DB2ShmShipment, Boolean> getCheckVersionFunction(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	/**
	 * Returns a list of shipments given a list of ids
	 */
	public List<ShmShipment> listShipmentsByShipmentIds(
			final Collection<? extends Number> shipmentInstIds,
			final EntityManager entityManager) {
		// For performance reasons DO NOT change this from a native query to use critiera builder.
		final TypedQuery<Tuple> query = entityManager.createQuery(SHIPMENTS_BY_SHP_ID_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		final List<Tuple> results = query.getResultList();
		final List<ShmShipment> shipmentList = new ArrayList<>();
		for (final Tuple oneResult : results) {
			final ShmShipment oneShipment = (ShmShipment)oneResult.get(0);
			shipmentList.add(oneShipment);
		}
		return shipmentList;
	}

	/**
	 * Returns a list of shipments given a list of ids
	 */
	public List<ShmShipment> findByShpInstIdsOrParentInstIdsWithCustomsBonds(
			final Collection<? extends Number>  shipmentInstIds, final List<BigDecimal> parentInstIds,
			final EntityManager entityManager) {
		
		if (CollectionUtils.isEmpty(shipmentInstIds) && CollectionUtils.isEmpty(parentInstIds))
		    return Collections.emptyList();
		
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> query = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = query.from(ShmShipment.class);
		final Path<Long> shpInstIdPath = from.get(ShmShipment_.shpInstId);
		final Path<BigDecimal> parentInstPath = from.get(ShmShipment_.parentInstId);
		final List<Predicate> predicates = new ArrayList<>();
		Predicate predicate = null;

		if (CollectionUtils.isNotEmpty(shipmentInstIds)) {
		    predicate = shpInstIdPath.in(shipmentInstIds);
		}

		if (CollectionUtils.isNotEmpty(parentInstIds)) {
		    Predicate parentPredicate = parentInstPath.in(parentInstIds);
		    if (predicate != null)
		        predicate = cb.or(predicate, parentPredicate);
		    else
		        predicate = parentPredicate;

			//search if any parent is inbond CCS-6647, CCS-6942
			parentPredicate = shpInstIdPath.in(CollectionUtils.emptyIfNull(parentInstIds).stream().map(BigDecimal::longValue).collect(Collectors.toList()));
			if(parentPredicate != null)
				predicate = cb.or(predicate, parentPredicate);
		}
		predicates.add(predicate);
		query.select(from).where(cb.or(predicates.toArray(new Predicate[CollectionUtils.size(predicates)])));
		
		final EntityGraph<ShmShipment> entityGraph = entityManager.createEntityGraph(ShmShipment.class);
		entityGraph.addSubgraph("shmCustomsBonds");
		final TypedQuery<ShmShipment> typedQuery = entityManager.createQuery(query);
		typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
		return typedQuery.getResultList();
	}
	
	
	
	public List<ShmShipment> findShipmentOnTrailerWithMovementsByIds(final Set<Long> shipmentIds, final EntityManager entityManager) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> query = cb.createQuery(ShmShipment.class);

		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final Path<Long> shpInstIdPath = from.get(ShmShipment_.shpInstId);
		final Path<String> mvmtStatCdPath = from.get(ShmShipment_.mvmtStatCd);

		final List<ShmShipment> result = new ArrayList<>();
		for (final List<Long> partitionedList : Lists.partition(Lists.newArrayList(shipmentIds), appContext.getMaxCountForInClause())) {
			final List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.and(shpInstIdPath.in(partitionedList)));
			predicates.add(cb.equal(mvmtStatCdPath, ON_TRAILER_CD));
			query.select(from).where(cb.and(predicates.toArray(new Predicate[CollectionUtils.size(predicates)])));

			final EntityGraph<ShmShipment> entityGraph = entityManager.createEntityGraph(ShmShipment.class);
			entityGraph.addAttributeNodes("shmMovements");
			final TypedQuery<ShmShipment> typedQuery = entityManager.createQuery(query);
			typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
			result.addAll(typedQuery.getResultList());
		}

		return result;
	}

	public List<ShmShipment> findShipmentOnTrailerWithMovementsByProNbrs(final List<String> proNbrs, final EntityManager entityManager) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> query = cb.createQuery(ShmShipment.class);

		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final Path<String> proNbrTxtath = from.get(ShmShipment_.proNbrTxt);
		final Path<String> mvmtStatCdPath = from.get(ShmShipment_.mvmtStatCd);

		final List<ShmShipment> result = new ArrayList<>();
		for (final List<String> partitionedList : Lists.partition(proNbrs, appContext.getMaxCountForInClause())) {
			final List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.and(proNbrTxtath.in(partitionedList)));
			predicates.add(cb.equal(mvmtStatCdPath, ON_TRAILER_CD));
			query.select(from).where(cb.and(predicates.toArray(new Predicate[CollectionUtils.size(predicates)])));

			final EntityGraph<ShmShipment> entityGraph = entityManager.createEntityGraph(ShmShipment.class);
			entityGraph.addAttributeNodes("shmMovements");
			final TypedQuery<ShmShipment> typedQuery = entityManager.createQuery(query);
			typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
			result.addAll(typedQuery.getResultList());
		}

		return result;
	}

	public List<DB2ShmShipment> listDb2ShipmentsByShipmentIds(
			final List<Long> shipmentInstIds,
			final EntityManager entityManager) {

		final Query query = entityManager.createNativeQuery(DB2_SHIPMENTS_WITH_UR__BY_SHP_ID_SQL, DB2ShmShipment.class);
		query.setParameter("shpIdList", shipmentInstIds);
		@SuppressWarnings("unchecked")
		final
		List<DB2ShmShipment> results = query.getResultList();
		return results;
	}

	public List<ShmShipment> findMovrShipmentsByProNbr(final List<String> proNbrs, final EntityManager entityManager) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> query = cb.createQuery(ShmShipment.class);

		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final Path<String> proNbrTxtPath = from.get(ShmShipment_.proNbrTxt);
		final Path<String> billClassPath = from.get(ShmShipment_.billClassCd);

		final List<ShmShipment> result = new ArrayList<>();
		for (final List<String> partitionedList : Lists.partition(proNbrs, appContext.getMaxCountForInClause())) {
			final List<Predicate> predicates = Lists.newArrayList();
			predicates.add(cb.or(proNbrTxtPath.in(partitionedList)));
			query.select(from).where(cb.and(cb.equal(billClassPath, BILL_CLASS_FRT_SEG),cb.or(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]))));

			final EntityGraph<ShmShipment> entityGraph = entityManager.createEntityGraph(ShmShipment.class);
			entityGraph.addAttributeNodes("shmShipment");
			final TypedQuery<ShmShipment> typedQuery = entityManager.createQuery(query);
			typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
			result.addAll(typedQuery.getResultList());
		}

		return result;
	}

    public ShmShipment findByProOrShipmentId(String proNumber,
                                             Date pickupDate,
                                             Long shipInstanceId,
                                             boolean yellowProInd,
                                             ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                             EntityManager entityManager) {
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();

		//Code copied from RAD
		final CriteriaQuery<ShmShipment> cq = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = cq.from(ShmShipment.class);
		final Path<Long> shmInstIdPath = from.get(ShmShipment_.shpInstId);
		final Path<Date> pkupDtPath = from.get(ShmShipment_.pkupDt);
		final Path<String> archiveInd = from.get(ShmShipment_.archiveInd);

		List<Predicate> predicates = new ArrayList<>();

		if(shipInstanceId != null && shipInstanceId > 0L) {
			predicates.add(cb.equal(shmInstIdPath, shipInstanceId));
		}else if (pickupDate == null ) {
			if(yellowProInd) {
				predicates.add(cb.equal(shmInstIdPath, getShipmentInstIdForHandlingUnitPro(cb, cq, proNumber)));
			} else {
				predicates.add(cb.and(cb.equal(from.get(ShmShipment_.proNbrTxt), proNumber),
						cb.notEqual(archiveInd, ARCHIVE_INDICATOR)));
				cq.orderBy(cb.desc(pkupDtPath));
			}
		}else {
			predicates.add(cb.and(cb.equal(from.get(ShmShipment_.proNbrTxt), proNumber),
					cb.equal(pkupDtPath, pickupDate)));
		}

		cq.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
        List<ShmShipment> shmShipments =
            eagerLoad(cq, shmShipmentEagerLoadPlan, entityManager);
		return CollectionUtils.isNotEmpty(shmShipments) ? shmShipments.get(0) : null;
	}

	public void update(final List<Long> shmShipmentInstIds,
					   final String rtePfxTxt,
					   final String rteSfxTxt,
					   final String rteTypCd,
					   final AuditInfo auditInfo,
					   final EntityManager entityManager) {
		final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		final CriteriaUpdate<ShmShipment> criteriaUpdate = builder.createCriteriaUpdate(ShmShipment.class);
		final Root<ShmShipment> root = criteriaUpdate.from(ShmShipment.class);

		criteriaUpdate
		.set(root.get("rtePfxTxt"), rtePfxTxt)
		.set(root.get("rteSfxTxt"), rteSfxTxt)
		.set(root.get("rteTypCd"), rteTypCd)
		.set(root.get("lstUpdtTranCd"), auditInfo.getUpdateByPgmId())
		.set(root.get("lstUpdtTmst"), BasicTransformer.toTimestamp(auditInfo.getUpdatedTimestamp()))
		.set(root.get("lstUpdtUid"), auditInfo.getUpdateById())
		.where(builder.and(root.get("shpInstId").in(shmShipmentInstIds)));

		entityManager
		.createQuery(criteriaUpdate)
		.executeUpdate();
	}

	public void updateDb2(final List<Long> shmShipmentInstIds,
						  final String rtePfxTxt,
						  final String rteSfxTxt,
						  final String rteTypCd,
						  final AuditInfo auditInfo,
						  final TransactionContext txnContext,
						  final EntityManager db2EntityManager) throws ValidationException {

		final CriteriaBuilder builder = db2EntityManager.getCriteriaBuilder();
		final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = builder.createCriteriaUpdate(DB2ShmShipment.class);
		final Root<DB2ShmShipment> root = criteriaUpdate.from(DB2ShmShipment.class);
		final Timestamp lstUpdtTmst = BasicTransformer.toTimestamp(auditInfo.getUpdatedTimestamp());
		criteriaUpdate
		.set(root.get(DB2ShmShipment_.rtePfxTxt), rtePfxTxt)
		.set(root.get(DB2ShmShipment_.rteSfxTxt), rteSfxTxt)
		.set(root.get(DB2ShmShipment_.rteTypCd), rteTypCd)
		.set(root.get(DB2ShmShipment_.lstUpdtTranCd), auditInfo.getUpdateByPgmId())
		.set(root.get(DB2ShmShipment_.lstUpdtTmst), lstUpdtTmst)
		.set(root.get(DB2ShmShipment_.lstUpdtUid), auditInfo.getUpdateById())
		.where(builder.and(root.get(DB2ShmShipment_.shpInstId).in(shmShipmentInstIds),
			builder.lessThanOrEqualTo
			(root.get(DB2ShmShipment_.lstUpdtTmst), lstUpdtTmst)));
		final int count =
				db2EntityManager
				.createQuery(criteriaUpdate)
				.executeUpdate();
		if (count != shmShipmentInstIds.size()) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.VERSION_CHECK_TIMESTAMP, txnContext)
			.moreInfo("expectedUpdateCount", String.valueOf(shmShipmentInstIds.size()))
			.moreInfo("actualUpdateCount", String.valueOf(count))
			.moreInfo("shpInstIds", StringUtils.join(shmShipmentInstIds, ", "))
			.build();
		}
	}

	public List<ShmShipment> listShipmentsByProNumbers(final List<String> proNbrList,final EntityManager entityManager) {
		// For performance reasons DO NOT change this from a native query to use critiera builder.
		if(CollectionUtils.isEmpty(proNbrList)) {
			return Collections.emptyList();
		}
		final TypedQuery<Tuple> query = entityManager.createQuery(SHIPMENTS_BY_PRO_NBR_SQL, Tuple.class);
		query.setParameter("proList", proNbrList);
		final List<Tuple> results = query.getResultList();
		final List<ShmShipment> shipmentList = new ArrayList<>();
		for (final Tuple oneResult : results) {
			final ShmShipment oneShipment = (ShmShipment)oneResult.get(0);
			shipmentList.add(oneShipment);
		}
		return shipmentList;
	}

	public List<ShmShipment> getRelatedShipments(final Long shipmentInstId, final EntityManager entityManager) {
		checkNotNull(shipmentInstId, "shipmentInstId");

		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> query = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<BigDecimal> parentShipmentInstIdPath = from.get(ShmShipment_.parentInstId);
		predicates
		.add(cb.equal(parentShipmentInstIdPath, new BigDecimal(shipmentInstId)));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmShipment> shipments = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(shipments))
			return new ArrayList<>();

		return shipments;

	}

	public List<ShmShipment> getRelatedShipmentsByShpmntInstIds(final List<BigDecimal> shipmentInstIds, final EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> query = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<BigDecimal> parentShipmentInstIdPath = from.get(ShmShipment_.parentInstId);
		predicates
		.add(parentShipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmShipment> shipments = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(shipments))
			return new ArrayList<>();

		return shipments;

	}

	public List<ShmShipment> findByProNbrs(
			final List<String> proNbrs,
			final EntityManager entityManager) {

		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> query = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = query.from(ShmShipment.class);
		final Expression<String> proNbrPath = from.get(ShmShipment_.proNbrTxt);
		final Path<Date> pkupDatePath = from.get(ShmShipment_.pkupDt);

		query.select(from).where(proNbrPath.in(proNbrs)).orderBy(cb.desc(pkupDatePath));
		final List<ShmShipment> shipments = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(shipments))
			return new ArrayList<>();
		return shipments;
	}

    public List<ShmShipment> listShipmentsByShpInstIds
            (List<Long> shipmentInstIds,
             ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
             EntityManager entityManager) {
        CriteriaBuilder criteriaBuilder =
            entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmShipment> criteriaQuery =
            criteriaBuilder.createQuery(ShmShipment.class);

        Root<ShmShipment> shmShipmentRoot =
            criteriaQuery.from(ShmShipment.class);
        Path<Long> shpInstIdPath =
            shmShipmentRoot.get(ShmShipment_.shpInstId);

        criteriaQuery.where(shpInstIdPath.in(shipmentInstIds));

        return eagerLoad(criteriaQuery,
                         shmShipmentEagerLoadPlan,
                         entityManager);
    }

    public List<ShmShipment> listShipmentsByParentShpInstIds
            (List<Long> shipmentInstIds,
             ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
             EntityManager entityManager) {
        CriteriaBuilder criteriaBuilder =
            entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmShipment> criteriaQuery =
            criteriaBuilder.createQuery(ShmShipment.class);

        Root<ShmShipment> shmShipmentRoot =
            criteriaQuery.from(ShmShipment.class);
        Path<BigDecimal> parentShpInstIdPath =
            shmShipmentRoot.get(ShmShipment_.parentInstId);

        criteriaQuery.where(parentShpInstIdPath.in(shipmentInstIds));

        return eagerLoad(criteriaQuery,
                         shmShipmentEagerLoadPlan,
                         entityManager);
    }

    public List<ShmShipment> listShipmentsByProNbrs
            (List<String> proNbrs,
             ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
             EntityManager entityManager) {
        CriteriaBuilder criteriaBuilder =
            entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmShipment> criteriaQuery =
            criteriaBuilder.createQuery(ShmShipment.class);

        Root<ShmShipment> shmShipmentRoot =
            criteriaQuery.from(ShmShipment.class);
        Path<String> proNbrTxtPath =
            shmShipmentRoot.get(ShmShipment_.proNbrTxt);
        Path<Date> pkupDtPath =
            shmShipmentRoot.get(ShmShipment_.pkupDt);

        criteriaQuery
            .where(proNbrTxtPath.in(proNbrs))
            .orderBy(criteriaBuilder.desc(pkupDtPath));

        return eagerLoad(criteriaQuery,
                         shmShipmentEagerLoadPlan,
                         entityManager);
    }

    private List<ShmShipment> eagerLoad
            (CriteriaQuery<ShmShipment> criteriaQuery,
             ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
             EntityManager entityManager) {
        ShmShipmentEagerLoadPlan plan = shmShipmentEagerLoadPlan;
        if (plan == null)
            plan = new ShmShipmentEagerLoadPlan();

        List<String> firstGraphs = new ArrayList<>();

        if (plan.isShmShipment())
            firstGraphs.add("shmShipment");

        if (plan.isShmHazMats())
            firstGraphs.add("shmHazMats");

        if (plan.isShmOpsShipment())
            firstGraphs.add("shmOpsShipment");

        List<String[]> allGraphs = new ArrayList<>();

        if (!firstGraphs.isEmpty())
            allGraphs.add(firstGraphs.toArray(new String[firstGraphs.size()]));

        if (plan.isShmAcSvcs())
            allGraphs.add(new String[] { "shmAcSvcs" });

        if (plan.isShmAdvBydCarrs())
            allGraphs.add(new String[] { "shmAdvBydCarrs" });

        if (plan.isShmAsEntdCusts())
            allGraphs.add(new String[] { "shmAsEntdCusts" });

        if (plan.isShmCommodities())
            allGraphs.add(new String[] { "shmCommodities" });

        if (plan.isShmCustomsBonds())
            allGraphs.add(new String[] { "shmCustomsBonds" });

        if (plan.isShmCustomsCntrls())
            allGraphs.add(new String[] { "shmCustomsCntrls" });

        if (plan.isShmEventLogs())
            allGraphs.add(new String[] { "shmEventLogs" });

        if (plan.isShmHandlingUnits())
            allGraphs.add(new String[] { "shmHandlingUnits" });

        if (plan.isShmLnhDimensions())
            allGraphs.add(new String[] { "shmLnhDimensions" });

        if (plan.isShmMgmtRemarks())
            allGraphs.add(new String[] { "shmMgmtRemarks" });

        if (plan.isShmMiscLineItems())
            allGraphs.add(new String[] { "shmMiscLineItems" });

        if (plan.isShmMovements())
            allGraphs.add(new String[] { "shmMovements" });

        if (plan.isShmRemarks())
            allGraphs.add(new String[] { "shmRemarks" });

        if (plan.isShmBillEntryStats())
            allGraphs.add(new String[] { "shmBillEntryStats" });

        if (plan.isShmShipments())
            allGraphs.add(new String[] { "shmShipments" });

        if (plan.isShmSrNbrs())
            allGraphs.add(new String[] { "shmSrNbrs" });

        List<ShmShipment> results = null;
        if (allGraphs.isEmpty()) {
            TypedQuery<ShmShipment> query =
                entityManager.createQuery(criteriaQuery);
            results = query.getResultList();
        }
        else {
            for (String[] graphs : allGraphs) {
                TypedQuery<ShmShipment> query =
                    entityManager.createQuery(criteriaQuery);
                EntityGraph<ShmShipment> entityGraph =
                    entityManager.createEntityGraph(ShmShipment.class);
                for (String graph : graphs)
                    entityGraph.addSubgraph(graph);
                query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
                results = query.getResultList();
                if (results.isEmpty())
                    return results;
            }
        }

        ShmHandlingUnitEagerLoadPlan shmHandlingUnitPlan =
            plan.getShmHandlingUnitEagerLoadPlan();
        if (plan.isShmHandlingUnits()
            && shmHandlingUnitPlan != null
            && shmHandlingUnitPlan.isShmHandlingUnitMvmts()) {
            @SuppressWarnings("null")
            List<ShmHandlingUnitPK> shmHandlingUnitIds =
                results.stream()
                    .map(ShmShipment::getShmHandlingUnits)
                    .flatMap(Collection::stream)
                    .filter(shmHandlingUnit ->
                                BooleanUtils.toBoolean
                                    (shmHandlingUnit.getSplitInd()))
                    .map(ShmHandlingUnit::getId)
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(shmHandlingUnitIds)) {
                CriteriaBuilder criteriaBuilder =
                    entityManager.getCriteriaBuilder();
                CriteriaQuery<ShmHandlingUnit> shmHandlingUnitCriteriaQuery =
                    criteriaBuilder.createQuery(ShmHandlingUnit.class);

                Root<ShmHandlingUnit> shmHandlingUnitRoot =
                    shmHandlingUnitCriteriaQuery.from(ShmHandlingUnit.class);
                Path<ShmHandlingUnitPK> idPath =
                    shmHandlingUnitRoot.get(ShmHandlingUnit_.id);

                shmHandlingUnitCriteriaQuery.where
                    (idPath.in(shmHandlingUnitIds));

                TypedQuery<ShmHandlingUnit> shmHandlingUnitQuery =
                    entityManager.createQuery(shmHandlingUnitCriteriaQuery);
                EntityGraph<ShmHandlingUnit> shmHandlingUnitEntityGraph =
                    entityManager.createEntityGraph(ShmHandlingUnit.class);
                shmHandlingUnitEntityGraph.addSubgraph
                    (ShmHandlingUnit_.shmHandlingUnitMvmts);
                shmHandlingUnitQuery.setHint(QueryHints.HINT_LOADGRAPH,
                                             shmHandlingUnitEntityGraph);
                shmHandlingUnitQuery.getResultList();
            }
        }

        return results;
    }
    
    public List<Shipment> listShipmentsNeedingAppointmentForConsignee(
            Long custInstId, 
            String destSicCd, 
            Date pickupDtStart, 
            Date pickupDtEnd,
            Date estDeliveryDtStart, 
            Date estDeliveryDtEnd,
            EntityManager entityManager) {
        
        
        TypedQuery<Tuple> query = entityManager.createQuery(OUTSTANDING_SHP_APPT_FOR_CONSIGNEE, Tuple.class);
        query.setParameter("custInstId", BasicTransformer.toBigDecimal(custInstId));
        query.setParameter("destSicCd", destSicCd);
        query.setParameter("pickupDtStart", pickupDtStart);
        query.setParameter("pickupDtEnd", pickupDtEnd);
        query.setParameter("estDeliveryDtStart", estDeliveryDtStart);
        query.setParameter("estDeliveryDtEnd", estDeliveryDtEnd);
        List<Tuple> results = query.getResultList();
        List<Shipment> shipmentList = new ArrayList<>();
        for (Tuple oneResult : results) {
            Shipment oneShipment = new Shipment(); 
            oneShipment.setShipmentInstId((Long)oneResult.get(SHP_INST_ID));
            oneShipment.setProNbr((String)oneResult.get(PRO_NBR));
            oneShipment.setTotalPiecesCount(BasicTransformer.toBigInteger((BigDecimal)oneResult.get(TOT_PCT)));
            oneShipment.setTotalWeightLbs(BasicTransformer.toDouble((BigDecimal)oneResult.get(TOT_WGT)));
            oneShipment.setEstimatedDeliveryDate(new SimpleDateFormat(DateUtils.DATE_FORMAT_SHORT).format((Date)oneResult.get(EST_DLVY_DT)));
            shipmentList.add(oneShipment);
        }
        return shipmentList;
    }

	/**
	 * Returns a list of non archived Shipments by Pro number.-
	 *
	 * @param proNbrs
	 *            the list of Pro numbers to retrieve shipments
	 * @param entityManager
	 *            the current shipment
	 * @return a list of shipment for the pro numbers
	 */
	public List<ShmShipment> findNonArchivedByProNbrs(
	    final List<String> proNbrs,
	    final EntityManager entityManager) {
	    // For performance reasons DO NOT change this from a native query to use critiera builder.
	    final TypedQuery<Tuple> query = entityManager.createQuery(SHIPMENTS_BY_NON_ARCHIVED_PRO_NBR_SQL, Tuple.class);
	    query.setParameter("proList", proNbrs);
	    query.setParameter("archiveInd", BasicTransformer.toString(false));
	    final List<Tuple> results = query.getResultList();
	    final List<ShmShipment> shipmentList = new ArrayList<>();
	    for (final Tuple oneResult : results) {
	        final ShmShipment oneShipment = (ShmShipment)oneResult.get(0);
	        shipmentList.add(oneShipment);
	    }

	    return shipmentList.stream()
	            .sorted(Comparator.comparingInt
                    (shmShipment
                         -> proNbrs.indexOf(shmShipment.getProNbrTxt())))
	            .collect(Collectors.toList());
	}

	/**
	 * Returns a Shipment by Pro number or shipment instance id. One of the two params are
	 * required in order to be able to find the shipment-
	 *
	 * @param proNumber
	 *            Pro number to retrieve shipment
	 * @param shipInstanceId
	 * 	          Pro number to retrieve shipment
	 * @param entityManager
	 *            the current shipment
	 * @return a list of shipment for the pro numbers
	 */
	public ShmShipment findByIdOrProNumber(
			final String proNumber,
			final Long shipInstanceId,
			final EntityManager entityManager) {

		final CriteriaBuilder cb = checkNotNull(entityManager,
				ValidationErrorMessage.ENTITY_MANAGER_REQUIRED.message()).getCriteriaBuilder();

		final CriteriaQuery<ShmShipment> cq = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = cq.from(ShmShipment.class);
		final Path<Long> shmInstIdPath = from.get(ShmShipment_.shpInstId);
		final Path<String> proNbrTxtPath = from.get(ShmShipment_.proNbrTxt);

		if (shipInstanceId != null && shipInstanceId > 0L) {
			cq.select(from).where(cb.equal(shmInstIdPath, shipInstanceId));
		} else if (StringUtils.isNotBlank(proNumber)) {
			cq.select(from).where(cb.equal(proNbrTxtPath, proNumber));
		}
		final List<ShmShipment> shmShipmentList =  entityManager.createQuery(cq).getResultList();
		return (CollectionUtils.isNotEmpty(shmShipmentList) ? shmShipmentList.get(0) : null);
	}

	public ShmShipment create(final ShmShipment entity, final EntityManager entityManager) throws ValidationException {
		setShpInstId(entity,SHM_SHIPMENT_SEQ,entityManager);
		return super.save(entity, entityManager);
	}

    public List<ShmShipment> findByShpInstIdsWithMovements(final Collection<Long> shpInstIds,
                                                           final EntityManager entityManager) {
        if (CollectionUtils.isEmpty(shpInstIds))
            return Collections.emptyList();
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ShmShipment> criteriaQuery = criteriaBuilder.createQuery(ShmShipment.class);
        final Root<ShmShipment> shmShipmentRoot = criteriaQuery.from(ShmShipment.class);
        final Path<Long> shpInstIdPath = shmShipmentRoot.get(ShmShipment_.shpInstId);
        final Path<String> archiveIndPath = shmShipmentRoot.get(ShmShipment_.archiveInd);
        criteriaQuery
            .select(shmShipmentRoot)
            .where(shpInstIdPath.in(shpInstIds),
                   criteriaBuilder.equal(archiveIndPath, "N"));
        final TypedQuery<ShmShipment> typedQuery = entityManager.createQuery(criteriaQuery);
        final EntityGraph<ShmShipment> entityGraph = entityManager.createEntityGraph(ShmShipment.class);
        entityGraph.addSubgraph("shmMovements");
        typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
        return typedQuery.getResultList();
    }

    public List<ShmShipment> findByProNbrsWithMovements(final Collection<String> proNbrs,
                                                        final EntityManager entityManager) {
        if (CollectionUtils.isEmpty(proNbrs))
            return Collections.emptyList();
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ShmShipment> criteriaQuery = criteriaBuilder.createQuery(ShmShipment.class);
        final Root<ShmShipment> shmShipmentRoot = criteriaQuery.from(ShmShipment.class);
        final Path<String> proNbrTxtPath = shmShipmentRoot.get(ShmShipment_.proNbrTxt);
        final Path<String> archiveIndPath = shmShipmentRoot.get(ShmShipment_.archiveInd);
        criteriaQuery
            .select(shmShipmentRoot)
            .where(proNbrTxtPath.in(proNbrs),
                   criteriaBuilder.equal(archiveIndPath, "N"));
        final TypedQuery<ShmShipment> typedQuery = entityManager.createQuery(criteriaQuery);
        final EntityGraph<ShmShipment> entityGraph = entityManager.createEntityGraph(ShmShipment.class);
        entityGraph.addSubgraph("shmMovements");
        typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
        return typedQuery.getResultList();
    }

	public void bulkUpdateShipmentStatusAndSicCd(
			final Set<Long> shpInsIds,
			final MovementStatusCd movementStatusCd,
			final String currentSicCd,
			final AuditInfo auditInfo,
			final TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException {

		if (CollectionUtils.isEmpty(shpInsIds))
			return;

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaUpdate<ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(ShmShipment.class);
		final Root<ShmShipment> from = criteriaUpdate.from(ShmShipment.class);

		final Path<Long> shpInstIdPath = from.get(ShmShipment_.shpInstId);
		final Path<String> mvmtStatCdPath = from.get(ShmShipment_.mvmtStatCd);
		final Path<String> currSicCdPath = from.get(ShmShipment_.currSicCd);
		final Path<String> lstMvRptgSicCdPath = from.get(ShmShipment_.lstMvRptgSicCd);

		final Path<Timestamp> lstUpdtTmstPath = from.get(ShmShipment_.lstUpdtTmst);
		final Path<String> lstUpdtUserIdPath = from.get(ShmShipment_.lstUpdtUid);
		final Path<String> lstUpdtTranCdPath = from.get(ShmShipment_.lstUpdtTranCd);

		final List<Predicate> predicates = getPredicateForBulkUpdateBasedOnShipmentId
				(shpInsIds, cb, shpInstIdPath, mvmtStatCdPath);

		criteriaUpdate
				.set(mvmtStatCdPath, MovementStatusCdTransformer.toCode(movementStatusCd))
				.set(currSicCdPath, currentSicCd)
				.set(lstMvRptgSicCdPath, currentSicCd)
				.set(lstUpdtTmstPath, BasicTransformer.toTimestamp(auditInfo.getUpdatedTimestamp()))
				.set(lstUpdtUserIdPath, auditInfo.getUpdateById())
				.set(lstUpdtTranCdPath, auditInfo.getUpdateByPgmId())
				.where(cb.and(predicates.toArray(new Predicate[0])));

		final int count = entityManager.createQuery(criteriaUpdate).executeUpdate();
		checkUpdatedEntitiesNumberForShipmentInstIds(
				shpInsIds,
				count,
				txnContext);
	}

	public void bulkUpdateShipmentStatusAndSicCdFromDB2(
			final Set<Long> shpInsIds,
			final MovementStatusCd movementStatusCd,
			final String currentSicCd,
			final AuditInfo auditInfo,
			final TransactionContext txnContext,
			final EntityManager db2EntityManager) throws ServiceException {

		if (CollectionUtils.isEmpty(shpInsIds))
			return;

		final CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
		final CriteriaUpdate<DB2ShmShipment> criteriaUpdate = cb.createCriteriaUpdate(DB2ShmShipment.class);
		final Root<DB2ShmShipment> from = criteriaUpdate.from(DB2ShmShipment.class);

		final Path<Long> shpInstIdPath = from.get(DB2ShmShipment_.shpInstId);
		final Path<String> mvmtStatCdPath = from.get(DB2ShmShipment_.mvmtStatCd);
		final Path<String> currSicCdPath = from.get(DB2ShmShipment_.currSicCd);
		final Path<String> lstMvRptgSicCdPath = from.get(DB2ShmShipment_.lstMvRptgSicCd);

		final Path<Timestamp> lstUpdtTmstPath = from.get(DB2ShmShipment_.lstUpdtTmst);
		final Path<String> lstUpdtUserIdPath = from.get(DB2ShmShipment_.lstUpdtUid);
		final Path<String> lstUpdtTranCdPath = from.get(DB2ShmShipment_.lstUpdtTranCd);

		final List<Predicate> predicates = getPredicateForBulkUpdateBasedOnShipmentId
				(shpInsIds, cb, shpInstIdPath, mvmtStatCdPath);

		criteriaUpdate
				.set(mvmtStatCdPath, MovementStatusCdTransformer.toCode(movementStatusCd))
				.set(currSicCdPath, currentSicCd)
				.set(lstMvRptgSicCdPath, currentSicCd)
				.set(lstUpdtTmstPath, BasicTransformer.toTimestamp(auditInfo.getUpdatedTimestamp()))
				.set(lstUpdtUserIdPath, auditInfo.getUpdateById())
				.set(lstUpdtTranCdPath, auditInfo.getUpdateByPgmId())
				.where(cb.and(predicates.toArray(new Predicate[0])));

		final int count = db2EntityManager.createQuery(criteriaUpdate).executeUpdate();
		checkUpdatedEntitiesNumberForShipmentInstIds(
				shpInsIds,
				count,
				txnContext);
	}

	private List<Predicate> getPredicateForBulkUpdateBasedOnShipmentId(
			final Set<Long> shpInsIds,
			final CriteriaBuilder cb,
			final Path<Long> shpInstIdPath,
			final Path<String> mvmtStatCdPath) {
		final List<Predicate> predicates = new ArrayList<>();
		predicates.add(shpInstIdPath.in(shpInsIds));
		predicates.add(cb.equal(mvmtStatCdPath, OUT_FOR_DELIVERY));
		return predicates;
	}

	private void checkUpdatedEntitiesNumberForShipmentInstIds(
			final Set<Long> shpInsIds,
			final int updateCount,
			final TransactionContext txnContext) throws ServiceException {
		final int size = CollectionUtils.size(shpInsIds);
		if(updateCount != size) {
			final String shpInsIdsAsString = StringUtils.join
					(shpInsIds
							.stream()
							.map(shipmentInstId -> String.format("(%s)", shipmentInstId))
							.collect(Collectors.toList()), ", ");

			throw ExceptionBuilder
					.exception(ServiceErrorMessage.UNEXPECTED_DB_ERROR, txnContext)
					.contextValues(
							String.format("Expected update count: %s, but actual was: %s", size, updateCount),
							String.format("shpInsIds: %s", shpInsIdsAsString))
					.log()
					.build();
		}
	}

	public List<Shipment> listShipmentsByShipmentIdsAndMovementStatusOnlyWithEventLogAttributes(
			final Set<Long> shpInsIds,
			final MovementStatusCd movementStatusCd,
			final EntityManager entityManager) {

		if (CollectionUtils.isEmpty(shpInsIds))
			return new ArrayList<>();

		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final Path<Long> shpInstIdPath = from.get(ShmShipment_.shpInstId);
		final Path<String> mvmtStatCdPath = from.get(ShmShipment_.mvmtStatCd);

		final List<Predicate> predicates = Lists.newArrayList();
		predicates.add(shpInstIdPath.in(shpInsIds));
		predicates.add(criteriaBuilder.equal(mvmtStatCdPath, MovementStatusCdTransformer.toCode(movementStatusCd)));

		// Event Log attributes
		final Path<BigDecimal> totPcsCntPath = from.get(ShmShipment_.totPcsCnt);
		final Path<BigDecimal> totWgtLbsPath = from.get(ShmShipment_.totWgtLbs);
		final Path<BigDecimal> totChrgAmtPath = from.get(ShmShipment_.totChrgAmt);
		final Path<String> origTrmnlSicCdPath = from.get(ShmShipment_.origTrmnlSicCd);
		final Path<String> destTrmnlSicCdPath = from.get(ShmShipment_.destTrmnlSicCd);
		final Path<Date> pkupDtPath = from.get(ShmShipment_.pkupDt);
		final Path<Date> estimatedDlvrDtPath = from.get(ShmShipment_.estimatedDlvrDt);
		final Path<String> chrgToCdPath = from.get(ShmShipment_.chrgToCd);
		final Path<String> billStatCdPath = from.get(ShmShipment_.billStatCd);
		final Path<String> garntdIndPath = from.get(ShmShipment_.garntdInd);
		final Path<String> hazmatIndPath = from.get(ShmShipment_.hazmatInd);
		final Path<String> billClassCdPath = from.get(ShmShipment_.billClassCd);

		query.multiselect(
				shpInstIdPath,
				totPcsCntPath,
				totWgtLbsPath,
				totChrgAmtPath,
				origTrmnlSicCdPath,
				destTrmnlSicCdPath,
				pkupDtPath,
				estimatedDlvrDtPath,
				chrgToCdPath,
				billStatCdPath,
				garntdIndPath,
				hazmatIndPath,
				billClassCdPath)
			.where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]));

		final List<Tuple> resultList = entityManager.createQuery(query).getResultList();
		final List<Shipment> shipmentsResult = CollectionUtils.emptyIfNull(resultList)
				.stream()
				.map(tuple -> {
					final Shipment shipment = new Shipment();
					shipment.setShipmentInstId(tuple.get(shpInstIdPath));
					shipment.setTotalPiecesCount(BasicTransformer.toBigInteger(tuple.get(totPcsCntPath)));
					shipment.setTotalWeightLbs(BasicTransformer.toDouble(tuple.get(totWgtLbsPath)));
					shipment.setTotalChargeAmount(BasicTransformer.toDouble(tuple.get(totChrgAmtPath)));
					shipment.setOriginTerminalSicCd(tuple.get(origTrmnlSicCdPath));
					shipment.setDestinationTerminalSicCd(tuple.get(destTrmnlSicCdPath));
					shipment.setPickupDate(BasicTransformer.toString(tuple.get(pkupDtPath)));
					shipment.setEstimatedDeliveryDate(BasicTransformer.toString(tuple.get(estimatedDlvrDtPath)));
					shipment.setChargeToCd(ChargeToCdTransformer.toEnum(tuple.get(chrgToCdPath)));
					shipment.setBillStatusCd(BillStatusCdTransformer.toEnum(tuple.get(billStatCdPath)));
					shipment.setBillClassCd(BillClassCdTransformer.toEnum(tuple.get(billClassCdPath)));
					shipment.setGuaranteedInd(BasicTransformer.toBoolean(tuple.get(garntdIndPath)));
					shipment.setHazmatInd(BasicTransformer.toBoolean(tuple.get(hazmatIndPath)));
					return shipment;
				})
				.collect(Collectors.toList());
		return CollectionUtils.isEmpty(shipmentsResult) ? new ArrayList<>() : shipmentsResult;
	}

	/**
	 * To get shipmentIndt, Pro , bill class and deliverQulaifierCd
	 *
	 * @param proNbrs
	 * @param entityManager
	 * @return
	 */
	public List<Shipment> listShipmentsForValidation(final List<String> proNbrs, final EntityManager entityManager) {

		if (CollectionUtils.isEmpty(proNbrs))
			return new ArrayList<>();

		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final Path<String> proNbrTxtPath = from.get(ShmShipment_.proNbrTxt);

		final List<Predicate> predicates = Lists.newArrayList();
		predicates.add(proNbrTxtPath.in(proNbrs));

		final Path<Long> shpInstIdPath = from.get(ShmShipment_.shpInstId);
		final Path<String> proNumberTxtPath = from.get(ShmShipment_.proNbrTxt);
		final Path<Date> pkupDtPath = from.get(ShmShipment_.pkupDt);
		final Path<String> dlvryQalfrCdPath = from.get(ShmShipment_.dlvryQalfrCd);
		final Path<String> billClassCdPath = from.get(ShmShipment_.billClassCd);

		query.multiselect(shpInstIdPath, proNumberTxtPath, pkupDtPath, dlvryQalfrCdPath, billClassCdPath)
				.where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]));

		final List<Tuple> resultList = entityManager.createQuery(query).getResultList();
		final List<Shipment> shipmentsResult = CollectionUtils.emptyIfNull(resultList).stream().map(tuple -> {
			final Shipment shipment = new Shipment();
			shipment.setShipmentInstId(tuple.get(shpInstIdPath));
			shipment.setProNbr(tuple.get(proNumberTxtPath));
			shipment.setPickupDate(tuple.get(pkupDtPath) == null ? null : tuple.get(pkupDtPath).toString());
			shipment.setDeliveryQualifierCd(DeliveryQualifierCdTransformer.toEnum(tuple.get(dlvryQalfrCdPath)));
			shipment.setBillClassCd(BillClassCdTransformer.toEnum(tuple.get(billClassCdPath)));

			return shipment;
		}).collect(Collectors.toList());
		return CollectionUtils.isEmpty(shipmentsResult) ? new ArrayList<>() : shipmentsResult;

	}

	/**
	 * To get parentshipment id, pro and pickup dates
	 *
	 * @param shpInstIds
	 * @param entityManager
	 * @return
	 */
	public List<ShmShipment> listMovrShipmentsByShipmentInstId(final Set<Long> shpInstIds,
			final EntityManager entityManager) {
		if (CollectionUtils.isEmpty(shpInstIds))
			return new ArrayList<>();

		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final Path<BigDecimal> parentShmInstPath = from.get(ShmShipment_.parentInstId);

		final Path<Long> shpInstIdPath = from.get(ShmShipment_.shpInstId);
		final Path<String> proNumberTxtPath = from.get(ShmShipment_.proNbrTxt);
		final Path<Date> pkupDtPath = from.get(ShmShipment_.pkupDt);
		final Path<String> billClassCdPath = from.get(ShmShipment_.billClassCd);
		final Path<String> dlvrQqlfrCdPath = from.get(ShmShipment_.dlvryQalfrCd);

		final List<Predicate> predicates = Lists.newArrayList();
		predicates.add(parentShmInstPath.in(shpInstIds));
		predicates.add(criteriaBuilder.equal(billClassCdPath, BILL_CLASS_FRT_SEG));
		predicates.add(criteriaBuilder.notEqual(dlvrQqlfrCdPath,
				DeliveryQualifierCdTransformer.toCode(DeliveryQualifierCd.FINAL)));

		query.multiselect(shpInstIdPath, proNumberTxtPath, pkupDtPath, parentShmInstPath)
				.where(criteriaBuilder.and(predicates.toArray(new Predicate[CollectionUtils.size(predicates)])));

		final List<Tuple> resultList = entityManager.createQuery(query).getResultList();
		final List<ShmShipment> shipmentsResult = CollectionUtils.emptyIfNull(resultList).stream().map(tuple -> {
			final ShmShipment shipment = new ShmShipment();
			shipment.setParentInstId(tuple.get(parentShmInstPath));
			shipment.setShpInstId(tuple.get(shpInstIdPath));
			shipment.setProNbrTxt(tuple.get(proNumberTxtPath));
			shipment.setPkupDt(
					BasicTransformer.toDate(tuple.get(pkupDtPath) == null ? null : tuple.get(pkupDtPath).toString()));

			return shipment;
		}).collect(Collectors.toList());
		return CollectionUtils.isEmpty(shipmentsResult) ? new ArrayList<>() : shipmentsResult;

	}
	
	public List<Tuple> listShpDetailsWithTdcForAppointmentByShpId(Set<Long> shpInstIds, boolean isParent, EntityManager entityManager) {
	    
        if (CollectionUtils.isEmpty(shpInstIds)) {
            return new ArrayList<>();
        }

        StringBuffer apptSb =  new StringBuffer();
        apptSb.append(SHIPMENTS_FOR_APPOINTMENT);
        
        if (!isParent) {
            apptSb.append(MISC_SELECTION_FOR_APPTS);
        }
        apptSb.append(SHIPMENTS_FOR_APPOINTMENT2);
        if (!isParent) {
            apptSb.append(SHIPMENTS_FOR_APPT_MISC_LN_ITM_JOIN);
        }
        apptSb.append(SHIPMENTS_FOR_APPT_WHERE);
        
        TypedQuery<Tuple> query = entityManager.createQuery(apptSb.toString(), Tuple.class);
        query.setParameter("shpInstIds", shpInstIds);
        return query.getResultList();
	}

	public void saveBulk(final List<ShmShipment> shipmentEntity, final EntityManager entityManager) throws ValidationException {
		int i = 0;
		for (final ShmShipment shmShipment : shipmentEntity) {
			if(i == BULK_SAVE_LIMIT) {
				entityManager.flush();
				i = 0;
			}

			persist(shipmentEntity, entityManager);
			++i;
		}

		entityManager.flush();
		return;
	}

	public void updateDB2ShmShipmentsBulk(
		final List<ShmShipment> shipmentEntitiesToUpdate,
		final TransactionContext txnContext,
		final EntityManager db2EntityManager) throws ServiceException {
		Objects.requireNonNull(shipmentEntitiesToUpdate, "Required parameter ShmShipments from is null.");
		Objects.requireNonNull(db2EntityManager, "Required parameter EntityManager db2EntityManager is null.");

		final Map<Long, List<ShmShipment>> shmShipmentsByShpInstId = CollectionUtils.emptyIfNull(shipmentEntitiesToUpdate).stream()
				.collect(Collectors.groupingBy(ShmShipment::getShpInstId));
		final List<Long> shpInstIds = shmShipmentsByShpInstId.keySet().stream().collect(Collectors.toList());

		final List<DB2ShmShipment> db2shmShipmentEntities = findDb2ByIds(shpInstIds, db2EntityManager);
		int i = 0;

		if(CollectionUtils.isNotEmpty(db2shmShipmentEntities)) {
			for (final DB2ShmShipment db2ShmShipment : db2shmShipmentEntities) {
				if(i == BULK_SAVE_LIMIT) {
					db2EntityManager.flush();
					i = 0;
				}
				final ShmShipment shmShipment = shmShipmentsByShpInstId.get(db2ShmShipment.getShpInstId()).get(0);

				final Function<DB2ShmShipment, Boolean> checkVersionFunction = getCheckVersionFunction(shmShipment.getLstUpdtTmst());

				Objects.requireNonNull(checkVersionFunction, "Required parameter Function<DB2ShmShipment, Boolean> checkVersionFunction is null.");

				if(checkVersionFunction.apply(db2ShmShipment)) {
					copy(shmShipment, db2ShmShipment);
				} else {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VERSION_CHECK_TIMESTAMP, txnContext)
						.moreInfo("Action", "Update DB2ShmShipment")
						.moreInfo("shpInstId", BasicTransformer.toString(shmShipment.getShpInstId()))
						.moreInfo("lstUpdtTranCd-ShmShipment:DB2ShmShipment", shmShipment.getLstUpdtTranCd() + ":" + db2ShmShipment.getLstUpdtTranCd())
						.moreInfo("lstUpdtTmst-ShmShipment:DB2ShmShipment", BasicTransformer.toString(shmShipment.getLstUpdtTmst()) +
							":" + BasicTransformer.toString(db2ShmShipment.getLstUpdtTmst()))
						.moreInfo("lstUpdtUid-ShmShipment:DB2ShmShipment", shmShipment.getLstUpdtUid() + ":" + db2ShmShipment.getLstUpdtUid())
						.log()
						.build();
				}

				++i;
			}
			db2EntityManager.flush();
		}
	}

	private List<DB2ShmShipment> findDb2ByIds(final List<Long> shpInstIds, final EntityManager db2EntityManager) {
		final CriteriaBuilder criteriaBuilder = db2EntityManager.getCriteriaBuilder();
		final CriteriaQuery<DB2ShmShipment> query = criteriaBuilder.createQuery(DB2ShmShipment.class);
		final Root<DB2ShmShipment> from = query.from(DB2ShmShipment.class);
		final Path<Long> shPathInstIdPath = from.get(DB2ShmShipment_.shpInstId);

		query.select(from).where(criteriaBuilder.and(shPathInstIdPath.in(shpInstIds)));

		final List<DB2ShmShipment> resultList = db2EntityManager.createQuery(query).getResultList();

		return resultList;
	}

	public static class ShipmentsAggregation {

		private final BigInteger count;
		private final double totalRevenueAmount;
		private final Double totalWeightLbs;

		public ShipmentsAggregation(
			final BigInteger count,
			final double totalRevenueAmount,
			final Double totalWeightLbs) {
			super();
			this.count = count;
			this.totalRevenueAmount = totalRevenueAmount;
			this.totalWeightLbs = totalWeightLbs;
		}

		public BigInteger getCount() {
			return count;
		}

		public double getTotalRevenueAmount() {
			return totalRevenueAmount;
		}

		public Double getTotalWeightLbs() {
			return totalWeightLbs;
		}
	}

	public ShipmentsAggregation getShipmentAggregation(
		final Date beginDate,
		final Date endDate,
		final List<BigDecimal> pricingAgreementIds,
		final TransactionContext txnContext,
		final EntityManager entityManager) {

		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
		final Root<ShmShipment> from = query.from(ShmShipment.class);

		query
			.multiselect(
				criteriaBuilder.count(from).alias("count"),
				criteriaBuilder.sum(from.get(ShmShipment_.totChrgAmt)).alias("totChrgAmt"),
				criteriaBuilder.sum(from.get(ShmShipment_.totWgtLbs)).alias("totWgtLbs"))
			.where(
				from.get(ShmShipment_.prcAgrmtId).in(pricingAgreementIds),
				criteriaBuilder
					.between(
						from.get(ShmShipment_.pkupDt),
						criteriaBuilder.literal(beginDate),
						criteriaBuilder.literal(endDate)),
				from.get(ShmShipment_.billStatCd).in("4", "5"));

		final Tuple resultTuple = entityManager.createQuery(query).getSingleResult();

		return new ShipmentsAggregation(
			BigInteger.valueOf((Long) resultTuple.get("count")),
			(resultTuple.get("totChrgAmt") == null) ? 0.0 : ((BigDecimal) resultTuple.get("totChrgAmt")).doubleValue(),
			(resultTuple.get("totWgtLbs") == null) ? 0.0 : ((BigDecimal) resultTuple.get("totWgtLbs")).doubleValue());
	}
	/**
	 * Returns a ShipmentId by Pro number or shipment instance id. One of the two params are
	 * required in order to be able to find the shipment-
	 *
	 * @param proNumber
	 *            Pro number to retrieve shipmentId
	 * @param shipInstanceId
	 * 	          shipInstanceId to retrieve shipmentId
	 * @param pickupDate
	 * 	          pickupDate to retrieve shipmentId
	 * @param entityManager
	 *            the current shipment
	 * @return shipmentId for the pro numbers
	 */
	@LogExecutionTime
	public Long getIdByIdOrProNumber(
			final String proNumber,
			final Long shipInstanceId,
			final String pickupDate,
			final EntityManager entityManager) {

		final String strQuery = SHIPMENT_ID_BY_PRO_NBR_SQL+" WHERE ";
		Query query = null;

		if (shipInstanceId != null && shipInstanceId > 0L) {
			query = entityManager.createNativeQuery(strQuery+"SHP_INST_ID = :shpInstId");
			query.setParameter("shpInstId", shipInstanceId);
		} else if (StringUtils.isNotBlank(proNumber) && StringUtils.isNotBlank(pickupDate)) {
			query = entityManager.createNativeQuery(strQuery+"PRO_NBR_TXT = :proNbr AND PKUP_DT = :pkupDt");
			query.setParameter("proNbr", proNumber);
			query.setParameter("pkupDt", BasicTransformer.toDate(pickupDate));

		}

		if (Objects.nonNull(query)) {
			BigDecimal shmId = (BigDecimal) query.getSingleResult();
			if (Objects.nonNull(shmId)){
				return shmId.longValue();
			}
		}
		return null;
	}
	private Subquery getShipmentInstIdForHandlingUnitPro(CriteriaBuilder cb, CriteriaQuery query, String childProNbr) {
		final Subquery<Long> subQuery = query.subquery(Long.class);
		final Root<ShmHandlingUnit> subQueryFrom = subQuery.from(ShmHandlingUnit.class);
		final Path<Long> subQueryShpInstIdPath = subQueryFrom.get(ShmHandlingUnit_.id).get(ShmHandlingUnitPK_.shpInstId);
		Path<String> childProNbrPath = subQueryFrom.get(ShmHandlingUnit_.childProNbrTxt);

		final List<Predicate> subQueryPredicates = new ArrayList<>();
		subQueryPredicates.add(cb.equal(childProNbrPath, childProNbr));

		return subQuery.select(subQueryShpInstIdPath)
				.where(subQueryPredicates.toArray(new Predicate[CollectionUtils.size(subQueryPredicates)]));
	}
	
	public List<ShmShipment> findByProNumber(List<String> proNumbers, EntityManager entityManager) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> query = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = query.from(ShmShipment.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<String> proNbrPath = from.get(ShmShipment_.proNbrTxt);
		predicates
		.add(proNbrPath.in(proNumbers));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		
		final EntityGraph<ShmShipment> entityGraph = entityManager.createEntityGraph(ShmShipment.class);
		entityGraph.addSubgraph("shmHandlingUnits");
		final TypedQuery<ShmShipment> typedQuery = entityManager.createQuery(query);
		typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
		
		final List<ShmShipment> records = typedQuery.getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}
}
