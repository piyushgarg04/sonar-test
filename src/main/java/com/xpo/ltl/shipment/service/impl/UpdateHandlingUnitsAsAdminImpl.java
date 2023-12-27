package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementExceptionTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.HandlingUnitTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementExceptionTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitAsAdminRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmHandlingUnitDelegate;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;
import com.xpo.ltl.shipment.service.validators.UpdateHandlingUnitDimensionsValidator;

@RequestScoped
public class UpdateHandlingUnitsAsAdminImpl {

    private static final List<String> DIMENSION_TYPE_CDS = Arrays.asList("ACCURACY", "DOCK", "PICKUP", "PICKUP_DIMENSIONER");
    protected static final String NORMAL_MOVEMENT_CD = "NORMAL";
    protected static final String MISSING_MOVEMENT_CD = "MISSING";
    protected static final String ASTRAY_MOVEMENT_CD = "ASTRAY";
    private static final List<String> HU_MVMT_CDS = Arrays.asList(NORMAL_MOVEMENT_CD, MISSING_MOVEMENT_CD, ASTRAY_MOVEMENT_CD);

    @Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

    @Inject
    private UpdateHandlingUnitDimensionsValidator validator;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

    @Inject
    private DeleteHandlingUnitImpl deleteHandingUnitImpl;

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    public void updateHandlingUnitAsAdmin(UpdateHandlingUnitAsAdminRqst updateHandlingUnitAsAdminRqst, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        checkNotNull(updateHandlingUnitAsAdminRqst, "updateHandlingUnitAsAdminRqst Request is required.");
        checkNotNull(txnContext, "The TransactionContext is required.");
        checkNotNull(entityManager, "The EntityManager is required.");

        ShmShipment shipment = validateRequestReturnShipment(updateHandlingUnitAsAdminRqst, txnContext, entityManager);

        ShmHandlingUnit huOnDB = shmHandlingUnitSubDAO.findByTrackingProNumber(updateHandlingUnitAsAdminRqst.getHandlingUnit().getChildProNbr(), entityManager);

        boolean assignToNewParent = BooleanUtils.isTrue(updateHandlingUnitAsAdminRqst.getAssignToNewParentInd());
        String requestingSic = updateHandlingUnitAsAdminRqst.getRequestingSicCd();

        if (assignToNewParent) {

            // Transfer handling unit
            transferHandlingUnit(updateHandlingUnitAsAdminRqst, huOnDB, shipment, entityManager, txnContext);

        } else if (huOnDB != null) {
            AuditInfo auditInfo = AuditInfoHelper.getAuditInfo(txnContext);
            createShmHUMovement(updateHandlingUnitAsAdminRqst.getHandlingUnit(), updateHandlingUnitAsAdminRqst.getUserId(), huOnDB, requestingSic, auditInfo, entityManager, txnContext);
            // Update handling unit
            updateHandlingUnit(updateHandlingUnitAsAdminRqst, huOnDB, shipment, auditInfo, entityManager, txnContext);

        } else {

            // Create handling unit
            ShmHandlingUnit  shmHandlingUnit = createHandlingUnit(updateHandlingUnitAsAdminRqst, shipment, entityManager, txnContext);

            AuditInfo auditInfo = AuditInfoHelper.getAuditInfo(txnContext);
            createShmHUMovementForNewHandlingUnit(updateHandlingUnitAsAdminRqst.getHandlingUnit(), updateHandlingUnitAsAdminRqst.getUserId(), shmHandlingUnit, requestingSic, auditInfo, entityManager, txnContext);
        }


    }

    /**
     * <ul>
     * <li>if not on a dock name and dockName is not blank</li>
     * <ul>
     * <li>if it was on a trailer -> create the UNLOAD movement</li>
     * <li>else if split_ind flip from N and Y -> create the STAGE movement first, then the SPLIT</li>
     * <li>else if Change split_ind to N -> create the UNSPLIT movement first, then the STAGE</li>
     * <li>else -> create a STAGE movement</li>
     * </ul>
     * <li>else if it was on a dock and dockName is not blank and they are not the same-</li>
     * <ul>
     * <li>if split_ind flip from N and Y -> create the STAGE movement first, then the SPLIT</li>
     * <li>else if Change split_ind to N -> create the UNSPLIT movement first, then the STAGE</li>
     * <li>else -> create a STAGE movement</li>
     * </ul>
     * <li>else if was on a trailer and trailer is supplied and they are not the same-</li>
     * <ul>
     * <li>if split_ind flip from N and Y -> create the SPLIT movement</li>
     * <li>else if Change split_ind to N -> create the UNSPLIT movement</li>
     * <li>create the UNLOAD movement, then the LOAD movement</li>
     * <li>if movement status is OnTrailer, create CLOSE movement</li>
     * </ul>
     * <li>else if was not on a trailer and trailer is supplied-</li>
     * <ul>
     * <li>if split_ind flip from N and Y -> create the SPLIT movement</li>
     * <li>else if Change split_ind to N -> create the UNSPLIT movement</li>
     * <li>create LOAD movement</li>
     * <li>if movement status is OnTrailer, create CLOSE movement</li>
     * </ul>
     * <li>else if was on a trailer and trailer is not supplied-</li>
     * <ul>
     * <li>if split_ind flip from N and Y -> create the SPLIT movement</li>
     * <li>else if Change split_ind to N -> create the UNSPLIT movement</li>
     * <li>create the UNLOAD movement</li>
     * </ul>
     * <li>else if Change mvmt_stat_cd to 5 - create a DELIVER movement -> no need to create other movement (even if
     * handling_mvmt_cd changes)</li>
     * <li>else if Change handling_mvmt_cd from NORMAL to MISSING and mvmt_stat_cd not 4 -> create a DOP_EXCP movement</li>
     * <li>else if Change handling_mvmt_cd from NORMAL to MISSING and mvmt_stat_cd is 4 -> create a DELIVER movement with
     * exception</li>
     * </ul>
     *
     * @throws ValidationException
     */
    protected void createShmHUMovement(HandlingUnit handlingUnit, String authUserId, ShmHandlingUnit huOnDB, String requestingSic, AuditInfo auditInfo, EntityManager entityManager,
        TransactionContext txnContext) throws ValidationException {
        MovementStatusCd oldMvmtStatusCd = MovementStatusCdTransformer.toEnum(huOnDB.getMvmtStatCd());
        MovementStatusCd newMvmtStatusCd = MovementStatusCdTransformer.toEnum(handlingUnit.getMovementStatusCd());
        String oldHuMvmtCd = huOnDB.getHandlingMvmtCd();
        String newHuMvmtCd = handlingUnit.getHandlingMovementCd();
        Boolean oldSplitInd = DtoTransformer.toBoolean(huOnDB.getSplitInd());
        Boolean newSplitInd = handlingUnit.getSplitInd();
        Boolean newDockNameIsBlank = StringUtils.isBlank(handlingUnit.getCurrentDockLocation());
        Boolean oldDockNameIsBlank = StringUtils.isBlank(huOnDB.getCurrentDockLocTxt());
        Boolean dockChanged = !newDockNameIsBlank
                && !oldDockNameIsBlank && !handlingUnit.getCurrentDockLocation().equalsIgnoreCase(huOnDB.getCurrentDockLocTxt());
        Boolean newTrailerIdAvailable = handlingUnit.getCurrentTrailerInstanceId() != null ? NumberUtils.compare(handlingUnit.getCurrentTrailerInstanceId(), 0) > 0 : false;
        Boolean oldTrailerIdAvailable = huOnDB.getCurrentTrlrInstId() != null ? NumberUtils.compare(BasicTransformer.toLong(huOnDB.getCurrentTrlrInstId()), 0) > 0 : false;
        Boolean trailerChanged = newTrailerIdAvailable
                && oldTrailerIdAvailable && NumberUtils.compare(BasicTransformer.toLong(huOnDB.getCurrentTrlrInstId()), handlingUnit.getCurrentTrailerInstanceId()) != 0;

        Boolean hasSplitIndChangedToY = !oldSplitInd && newSplitInd;
        Boolean hasSplitIndChangedToN = oldSplitInd && !newSplitInd;

        long nextMvmtSeqNbr = shmHandlingUnitMvmtSubDAO
            .getNextMvmtBySeqNbrAndShpInstId(huOnDB.getId().getShpInstId(), huOnDB.getId().getSeqNbr(), entityManager);

        if(oldDockNameIsBlank && !newDockNameIsBlank) {
            if (oldTrailerIdAvailable && !newTrailerIdAvailable) {
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNLOAD, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
            if (hasSplitIndChangedToY) {
                // create the STAGE movement first, then the SPLIT
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.STAGE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.SPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo, entityManager);
            } else if (hasSplitIndChangedToN) {
                // create the UNSPLIT movement first, then the STAGE
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNSPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.STAGE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo, entityManager);
            } else {
                // create a STAGE movement 
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.STAGE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
            if(!MovementStatusCd.ON_DOCK.equals(newMvmtStatusCd)) {
                // override any input movement status when staging
                handlingUnit.setMovementStatusCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
            }
        } else if(dockChanged) {
            if (hasSplitIndChangedToY) {
                // create the STAGE movement first, then the SPLIT
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.STAGE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo,
                    entityManager);
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.SPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo, entityManager);
            } else if (hasSplitIndChangedToN) {
                // create the UNSPLIT movement first, then the STAGE
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNSPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.STAGE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo, entityManager);
            } else {
                // create a STAGE movement 
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.STAGE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
            if(!MovementStatusCd.ON_DOCK.equals(newMvmtStatusCd)) {
                // override any input movement status when staging
                handlingUnit.setMovementStatusCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
            }
        } else if (oldTrailerIdAvailable && newTrailerIdAvailable) {
            if (hasSplitIndChangedToY) {
                // create the SPLIT
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.SPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo, entityManager);
            } else if (hasSplitIndChangedToN) {
                // create the UNSPLIT movement first
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNSPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
            if (trailerChanged) {
                // create the UNLOAD movement first,
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNLOAD, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
                // then create a LOAD movement
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.LOAD, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
            if(MovementStatusCd.ON_TRAILER.equals(newMvmtStatusCd)) {
                // create the CLOSE movement
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.CLOSE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            } else if(!MovementStatusCd.ON_DOCK.equals(newMvmtStatusCd) && !MovementStatusCd.OUT_FOR_DLVRY.equals(newMvmtStatusCd)) {
                // override any input movement status when unloading
                handlingUnit.setMovementStatusCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
            }
        } else if (!oldTrailerIdAvailable && newTrailerIdAvailable) {
            if (hasSplitIndChangedToY) {
                // create the SPLIT
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.SPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo, entityManager);
            } else if (hasSplitIndChangedToN) {
                // create the UNSPLIT movement first, then the STAGE
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNSPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
            // create a LOAD movement
            persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.LOAD, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                entityManager);
            if(MovementStatusCd.ON_TRAILER.equals(newMvmtStatusCd)) {
                // create the CLOSE movement
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.CLOSE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            } else if(!MovementStatusCd.ON_DOCK.equals(newMvmtStatusCd) && !MovementStatusCd.OUT_FOR_DLVRY.equals(newMvmtStatusCd)) {
                // override any input movement status when unloading
                handlingUnit.setMovementStatusCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
            }
        } else if (oldDockNameIsBlank && newDockNameIsBlank && oldTrailerIdAvailable && !newTrailerIdAvailable) {
            if (hasSplitIndChangedToY) {
                // create the UNLOAD movement first, then the SPLIT
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNLOAD, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.SPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo, entityManager);
            } else if (hasSplitIndChangedToN) {
                // create the UNSPLIT movement first, then the UNLOAD
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNSPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo,
                    entityManager);
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNLOAD, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            } else {
                // create a UNLOAD movement 
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNLOAD, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
            if(!MovementStatusCd.ON_DOCK.equals(newMvmtStatusCd)) {
                // override any input movement status when unloading
                handlingUnit.setMovementStatusCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
            }
        } else if (MovementStatusCd.FINAL_DLVD != oldMvmtStatusCd && MovementStatusCd.FINAL_DLVD == newMvmtStatusCd) {
            // create a DELIVER movement - no need to create other movement (even if handling_mvmt_cd changes)
            persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.DELIVER, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo, entityManager);
        } else if (NORMAL_MOVEMENT_CD.equals(oldHuMvmtCd) && MISSING_MOVEMENT_CD.equals(newHuMvmtCd)) {
            if (MovementStatusCd.INTERIM_DLVRY != newMvmtStatusCd) {
                // create a DOP_EXCP mvmt
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.DOCK_OPERATIONS_EXCEPTION, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic,
                    null, auditInfo,
                    entityManager);
            } else {
                // create a DELIVER mvmt with exception
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.DELIVER, huOnDB, nextMvmtSeqNbr++,
                    Optional.of(MovementExceptionTypeCd.SHORT), requestingSic, null, auditInfo, entityManager);
            }
        } else {
            if (hasSplitIndChangedToY) {
                // create the SPLIT movement
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.SPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo, entityManager);
            } else if (hasSplitIndChangedToN) {
                // create the UNSPLIT movement
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.UNSPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
        }

    }

    protected void createShmHUMovementForNewHandlingUnit(HandlingUnit handlingUnit, String authUserId, ShmHandlingUnit huOnDB, String requestingSic, AuditInfo auditInfo, EntityManager entityManager,
        TransactionContext txnContext) throws ValidationException {

        Boolean dockNameIsBlank = StringUtils.isBlank(handlingUnit.getCurrentDockLocation());
        Boolean trailerIdAvailable = handlingUnit.getCurrentTrailerInstanceId() != null ? NumberUtils.compare(handlingUnit.getCurrentTrailerInstanceId(), 0) > 0 : false;

        long nextMvmtSeqNbr = 1L;

        if(!dockNameIsBlank) {
            // create the STAGE movement first
            persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.STAGE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                entityManager);
            if(BooleanUtils.isTrue(handlingUnit.getSplitInd())) {
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.SPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo, entityManager);
            }
        } else if(trailerIdAvailable) {
            persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.LOAD, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                entityManager);
            if(BooleanUtils.isTrue(handlingUnit.getSplitInd())) {
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.SPLIT, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, authUserId, auditInfo, entityManager);
            }
            if(MovementStatusCd.ON_TRAILER.equals(MovementStatusCdTransformer.toEnum(handlingUnit.getMovementStatusCd()))) {
                // create the CLOSE movement
                persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd.CLOSE, huOnDB, nextMvmtSeqNbr++, Optional.empty(), requestingSic, null, auditInfo,
                    entityManager);
            }
        }
    }

    protected void persistShipmentHandlingUnitMvmt(HandlingUnitMovementTypeCd huMvmtTypeCd, ShmHandlingUnit shmHandlingUnit, long nextMvmSeqNbr,
        Optional<MovementExceptionTypeCd> mvmtExpTypCdOpt, String requestingSic, String authUserId, AuditInfo auditInfo, EntityManager entityManager)
            throws ValidationException {

        ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
        id.setShpInstId(shmHandlingUnit.getId().getShpInstId());
        id.setSeqNbr(shmHandlingUnit.getId().getSeqNbr());
        id.setMvmtSeqNbr(nextMvmSeqNbr);

        ShmHandlingUnitMvmt shmHandlingUnitMvmt = new ShmHandlingUnitMvmt();
        DtoTransformer.setAuditInfo(shmHandlingUnitMvmt, auditInfo);
        shmHandlingUnitMvmt.setId(id);
        shmHandlingUnitMvmt.setShmHandlingUnit(shmHandlingUnit);
        shmHandlingUnitMvmt.setSplitAuthorizeBy(StringUtils.isNotBlank(authUserId) ? authUserId : StringUtils.SPACE);
        shmHandlingUnitMvmt.setSplitAuthorizeTmst(StringUtils.isNotBlank(authUserId) ? shmHandlingUnitMvmt.getCrteTmst() : DB2DefaultValueUtil.LOW_TMST);
        shmHandlingUnitMvmt.setMvmtTmst(shmHandlingUnitMvmt.getCrteTmst());
        shmHandlingUnitMvmt.setMvmtTypCd(HandlingUnitMovementTypeCdTransformer.toCode(huMvmtTypeCd));
        shmHandlingUnitMvmt.setArchiveCntlCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanInd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanReason(StringUtils.SPACE);
        shmHandlingUnitMvmt.setDmgdCatgCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setDockInstId(BigDecimal.ZERO);
        shmHandlingUnitMvmt
            .setExcpTypCd(mvmtExpTypCdOpt.isPresent() ? MovementExceptionTypeCdTransformer.toCode(mvmtExpTypCdOpt.get()) : StringUtils.SPACE);

        if (StringUtils.isNotBlank(requestingSic)) {
            shmHandlingUnitMvmt.setMvmtRptgSicCd(requestingSic);
        } else {
            shmHandlingUnitMvmt.setMvmtRptgSicCd(StringUtils.SPACE);
        }

        shmHandlingUnitMvmt.setRfsdRsnCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setRmrkTxt(StringUtils.SPACE);
        shmHandlingUnitMvmt.setScanTmst(DB2DefaultValueUtil.LOW_TMST);
        shmHandlingUnitMvmt.setTrlrInstId(BigDecimal.ZERO);
        shmHandlingUnitMvmt.setUndlvdRsnCd(StringUtils.SPACE);

        shmHandlingUnitMvmtSubDAO.save(shmHandlingUnitMvmt, entityManager);
        shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(shmHandlingUnitMvmt, db2EntityManager);
    }

    private ShmHandlingUnit createHandlingUnit(UpdateHandlingUnitAsAdminRqst updateHandlingUnitAsAdminRqst, ShmShipment shipment, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        HandlingUnit handlingUnit = updateHandlingUnitAsAdminRqst.getHandlingUnit();
        if (handlingUnit.getDimensionTypeCd() != null && !DIMENSION_TYPE_CDS.contains(handlingUnit.getDimensionTypeCd())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", String.format("DimensionTypeCd required for %s.", updateHandlingUnitAsAdminRqst.getHandlingUnit().getChildProNbr()))
                .log()
                .build();
        }

        if (handlingUnit.getHandlingMovementCd() != null && !HU_MVMT_CDS.contains(handlingUnit.getHandlingMovementCd())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", String.format("HandlingMovementCd required or invalid for %s.", updateHandlingUnitAsAdminRqst.getHandlingUnit().getChildProNbr()))
                .log()
                .build();

        }

        if (ObjectUtils.anyNull(handlingUnit.getLengthNbr(), handlingUnit.getWidthNbr(), handlingUnit.getHeightNbr())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.HU_DIMENSIONS_REQUIRED, txnContext).moreInfo("childProNbr", handlingUnit.getChildProNbr()).build();
        }
        validator.validateDimension(handlingUnit.getLengthNbr(), handlingUnit.getWidthNbr(), handlingUnit.getHeightNbr(), txnContext);

        if (handlingUnit.getVolumeCubicFeet() == null || handlingUnit.getVolumeCubicFeet().compareTo(0D) < 0) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.TOT_VOL_CFT_IS_REQUIRED, txnContext).moreInfo("childProNbr", handlingUnit.getChildProNbr()).build();
        }

        if (handlingUnit.getPupVolumePercentage() == null || handlingUnit.getPupVolumePercentage().compareTo(0D) <= 0) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.PUP_VOL_PCT_NOT_CALCULATED, txnContext).moreInfo("childProNbr", handlingUnit.getChildProNbr()).build();
        }

        if (handlingUnit.getCurrentSicCd() == null) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.CURRENT_SIC_RQ, txnContext).moreInfo("childProNbr", handlingUnit.getChildProNbr()).build();
        }

        if (handlingUnit.getCurrentTrailerInstanceId() == null && handlingUnit.getCurrentDockLocation() == null) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.TRAILER_INST_ID_RQ, txnContext).moreInfo("childProNbr", handlingUnit.getChildProNbr()).build();
        }

        // set default values for not required fields

        if (handlingUnit.getHandlingMovementCd() == null) {
            handlingUnit.setHandlingMovementCd(NORMAL_MOVEMENT_CD);
        }

        if (handlingUnit.getSplitInd() == null) {
            handlingUnit.setSplitInd(false);
        }

        shmShipmentSubDAO.findById(handlingUnit.getShipmentInstanceId(), entityManager);

        //        List<ShmHandlingUnit> allShmHandlingUnitsDB = shmHandlingUnitSubDAO
        //                .findByParentShipmentInstanceId(shipment.getShpInstId(), entityManager);
        List<ShmHandlingUnit> allShmHandlingUnitsDB = shipment.getShmHandlingUnits();

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithUserId(updateHandlingUnitAsAdminRqst.getUserId(), txnContext);

        long nextHUSeqNbr = shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(shipment.getShpInstId(), entityManager);

        Timestamp lastMvmtTimestamp = Timestamp.from(Instant.now());
        XMLGregorianCalendar lastMvmtDateTime = TimestampUtil.toXmlGregorianCalendar(lastMvmtTimestamp);

        ShmHandlingUnit  shmHandlingUnit = shmHandlingUnitDelegate.createShmHandlingUnit(handlingUnit, shipment.getShpInstId(), nextHUSeqNbr, shipment.getPkupDt(), handlingUnit.getCurrentSicCd(), lastMvmtDateTime, auditInfo, entityManager);

        entityManager.flush();
        db2EntityManager.flush();

//        if (handlingUnit.getWeightLbs() != null) {
//
//            shmHandlingUnitDelegate.updateHandlingUnitsWeight(shipment.getTotWgtLbs(), BasicTransformer.toBigDecimal(handlingUnit.getWeightLbs()), allShmHandlingUnitsDB, auditInfo, entityManager, txnContext);
//
//        }

        boolean shipmentUpdated = false;

        if (BooleanUtils.isTrue(handlingUnit.getSplitInd()) && !BasicTransformer.toString(true).equals(shipment.getHandlingUnitSplitInd())) {
            shipment.setHandlingUnitSplitInd(BasicTransformer.toString(true));
            shipmentUpdated = true;
        }

        String handlingUnitPartialInd = shmHandlingUnitDelegate.calculateHUPartialInd(Lists.newArrayList(handlingUnit), shmHandlingUnitDelegate.buildChildProHUMap(allShmHandlingUnitsDB));

        if (!handlingUnitPartialInd.equals(shipment.getHandlingUnitPartialInd())) {
            shipment.setHandlingUnitPartialInd(handlingUnitPartialInd);
            shipmentUpdated = true;
        }

        if (shipmentUpdated) {
            DtoTransformer.setLstUpdateAuditInfo(shipment, auditInfo);
            shmShipmentSubDAO.save(shipment, entityManager);
            shmShipmentSubDAO.updateDB2ShmShipment(shipment, shipment.getLstUpdtTmst(), txnContext, entityManager);
        }

        return shmHandlingUnit;
    }


    private void updateHandlingUnit(UpdateHandlingUnitAsAdminRqst updateHandlingUnitAsAdminRqst, ShmHandlingUnit huOnDB, ShmShipment shipment,
        AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext)
            throws ServiceException {

        HandlingUnit handlingUnit = updateHandlingUnitAsAdminRqst.getHandlingUnit();

        if (ObjectUtils.allNull(handlingUnit.getTypeCd(), handlingUnit.getWeightLbs(), handlingUnit.getDimensionTypeCd(), handlingUnit.getLengthNbr(), handlingUnit.getWidthNbr(),
            handlingUnit.getHeightNbr(), handlingUnit.getPupVolumePercentage(), handlingUnit.getVolumeCubicFeet(), handlingUnit.getCurrentSicCd(), handlingUnit.getCurrentDockLocation(),
            handlingUnit.getCurrentTrailerInstanceId(), handlingUnit.getHandlingMovementCd(), handlingUnit.getSplitInd())) {
            // if we don't receive any of the field, we can assume that they didn't get updated
            return;
        }

        if (handlingUnit.getTypeCd() != null) {
            huOnDB.setTypeCd(HandlingUnitTypeCdTransformer.toCode(handlingUnit.getTypeCd()));
        }

//        boolean reweight = false;

        if (handlingUnit.getWeightLbs() != null) {
            huOnDB.setWgtLbs(BasicTransformer.toBigDecimal(handlingUnit.getWeightLbs()));
//            reweight = true;
        }

        if (handlingUnit.getDimensionTypeCd() != null) {
            huOnDB.setDimensionTypeCd(handlingUnit.getDimensionTypeCd());
        }

        if (handlingUnit.getLengthNbr() != null) {
            huOnDB.setLengthNbr(BasicTransformer.toBigDecimal(handlingUnit.getLengthNbr()));
        }

        if (handlingUnit.getWidthNbr() != null) {
            huOnDB.setWidthNbr(BasicTransformer.toBigDecimal(handlingUnit.getWidthNbr()));
        }

        if (handlingUnit.getHeightNbr() != null) {
            huOnDB.setHeightNbr(BasicTransformer.toBigDecimal(handlingUnit.getHeightNbr()));
        }


//        BigDecimal previousPupVolumePercentage = null;

        if (handlingUnit.getPupVolumePercentage() != null) {
//            previousPupVolumePercentage = huOnDB.getPupVolPct();
            huOnDB.setPupVolPct(BasicTransformer.toBigDecimal(handlingUnit.getPupVolumePercentage()));
        }

//        BigDecimal previousVolumeCubicFeet = null;
        if (handlingUnit.getVolumeCubicFeet() != null) {
//            previousVolumeCubicFeet = huOnDB.getVolCft();
            huOnDB.setVolCft(BasicTransformer.toBigDecimal(handlingUnit.getVolumeCubicFeet()));
        }

        if (handlingUnit.getCurrentSicCd() != null) {
            huOnDB.setCurrentSicCd(handlingUnit.getCurrentSicCd());
        }

        if (handlingUnit.getMovementStatusCd() != null) {
            huOnDB.setMvmtStatCd(handlingUnit.getMovementStatusCd());
        }

        if (handlingUnit.getCurrentDockLocation() != null) {
            huOnDB.setCurrentDockLocTxt(handlingUnit.getCurrentDockLocation());
        }

        if (handlingUnit.getCurrentTrailerInstanceId() != null) {
            huOnDB.setCurrentTrlrInstId(BasicTransformer.toBigDecimal(handlingUnit.getCurrentTrailerInstanceId()));
        }

        boolean checkSplit = false;
        String previousMovementCd = huOnDB.getHandlingMvmtCd();
        if (handlingUnit.getHandlingMovementCd() != null) {
            huOnDB.setHandlingMvmtCd(handlingUnit.getHandlingMovementCd());
            checkSplit = true;
        }

        if (handlingUnit.getSplitInd() != null) {
            huOnDB.setSplitInd(BasicTransformer.toString(handlingUnit.getSplitInd()));
            checkSplit = true;
        }

        DtoTransformer.setLstUpdateAuditInfo(huOnDB, auditInfo);
        shmHandlingUnitSubDAO.save(huOnDB, entityManager);
        shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(huOnDB, huOnDB.getLstUpdtTmst(), txnContext, db2EntityManager);

        entityManager.flush();
        db2EntityManager.flush();

        List<ShmHandlingUnit> allShmHandlingUnitsDB = shmHandlingUnitSubDAO
                .findByParentShipmentInstanceId(shipment.getShpInstId(), entityManager);

//        List<ShmHandlingUnit> allOtherShmHandlingUnitsDB =
//                CollectionUtils.emptyIfNull(allShmHandlingUnitsDB)
//                .stream()
//                .filter(hu -> !hu.getChildProNbrTxt().equals(huOnDB.getChildProNbrTxt()))
//                .collect(Collectors.toList());
//
//        if (reweight) {
//
//            shmHandlingUnitDelegate.updateHandlingUnitsWeight(shipment.getTotWgtLbs(), huOnDB.getWgtLbs(), allOtherShmHandlingUnitsDB, auditInfo, entityManager, txnContext);
//
//        }

        boolean shipmentUpdated = false;

//        if (handlingUnit.getVolumeCubicFeet() != null && (previousVolumeCubicFeet == null || handlingUnit.getVolumeCubicFeet().compareTo(BasicTransformer.toDouble(previousVolumeCubicFeet)) != 0)) {
//
//            double totalVolumeCubicFeet = allShmHandlingUnitsDB.stream().filter(hu -> hu.getVolCft() != null).mapToDouble(hu -> BasicTransformer.toDouble(hu.getVolCft())).sum();
//            shipment.setTotVolCft(BasicTransformer.toBigDecimal(totalVolumeCubicFeet));
//            shipmentUpdated = true;
//
//        }
//
//        if (handlingUnit.getPupVolumePercentage() != null && (previousPupVolumePercentage == null || handlingUnit.getPupVolumePercentage().compareTo(BasicTransformer.toDouble(previousPupVolumePercentage)) != 0)) {
//
//            double totalVolumePercentage = allShmHandlingUnitsDB.stream().filter(hu -> hu.getPupVolPct() != null).mapToDouble(hu -> BasicTransformer.toDouble(hu.getPupVolPct())).sum();
//            shipment.setPupVolPct(BasicTransformer.toBigDecimal(totalVolumePercentage));
//            shipmentUpdated = true;
//
//        }

        if (handlingUnit.getHandlingMovementCd() != null) {
            String handlingUnitPartialInd = shmHandlingUnitDelegate.calculateHUPartialInd(Lists.newArrayList(handlingUnit), shmHandlingUnitDelegate.buildChildProHUMap(allShmHandlingUnitsDB));

            if (!handlingUnitPartialInd.equals(shipment.getHandlingUnitPartialInd())) {
                shipment.setHandlingUnitPartialInd(handlingUnitPartialInd);
                shipmentUpdated = true;
            }
        }

        if (checkSplit) {
            if (BooleanUtils.isTrue(handlingUnit.getSplitInd()) && !BasicTransformer.toString(true).equals(shipment.getHandlingUnitSplitInd())) {
                shipment.setHandlingUnitSplitInd(BasicTransformer.toString(true));
                shipmentUpdated = true;
            }

        }

        if (ASTRAY_MOVEMENT_CD.equals(previousMovementCd) && NORMAL_MOVEMENT_CD.equals(handlingUnit.getHandlingMovementCd())) {
            if (HandlingUnitTypeCd.MOTOR == handlingUnit.getTypeCd()) {
                shipment.setMtrzdPcsCnt(shipment.getMtrzdPcsCnt().add(BigDecimal.ONE));
                shipmentUpdated = true;
            }
            if (HandlingUnitTypeCd.LOOSE == handlingUnit.getTypeCd()) {
                shipment.setLoosePcsCnt(shipment.getLoosePcsCnt().add(BigDecimal.ONE));
                shipmentUpdated = true;
            }
        }

        if (shipmentUpdated) {
            DtoTransformer.setLstUpdateAuditInfo(shipment, auditInfo);
            shmShipmentSubDAO.save(shipment, entityManager);
            shmShipmentSubDAO.updateDB2ShmShipment(shipment, shipment.getLstUpdtTmst(), txnContext, db2EntityManager);
        }
    }

    private void transferHandlingUnit(UpdateHandlingUnitAsAdminRqst updateHandlingUnitAsAdminRqst, ShmHandlingUnit huOnDB, ShmShipment shipment, EntityManager entityManager,
        TransactionContext txnContext)
            throws ServiceException {

        HandlingUnit handlingUnit = updateHandlingUnitAsAdminRqst.getHandlingUnit();

        deleteHandingUnitImpl.deleteHandlingUnit(handlingUnit.getChildProNbr(), txnContext, entityManager);

        entityManager.flush();
        db2EntityManager.flush();

        createHandlingUnit(updateHandlingUnitAsAdminRqst, shipment, entityManager, txnContext);

    }

    private ShmShipment validateRequestReturnShipment(UpdateHandlingUnitAsAdminRqst updateHandlingUnitAsAdminRqst, TransactionContext txnContext, @NotNull EntityManager entityManager)
            throws ValidationException, NotFoundException {

        if (updateHandlingUnitAsAdminRqst.getHandlingUnit() == null) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo("N/A", "handlingUnit is required.").log().build();
        }

        if (BooleanUtils.isTrue(updateHandlingUnitAsAdminRqst.getHandlingUnit().getSplitInd())
                && StringUtils.isBlank(updateHandlingUnitAsAdminRqst.getUserId())) {
                throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo("N/A", "userId is required when requesting to split.").log().build();
        }

        if (StringUtils.isBlank(updateHandlingUnitAsAdminRqst.getHandlingUnit().getChildProNbr())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo("N/A", "Child PRO is required.").log().build();
        }

        try {
            updateHandlingUnitAsAdminRqst.getHandlingUnit().setChildProNbr(ProNumberHelper.validateProNumber(updateHandlingUnitAsAdminRqst.getHandlingUnit().getChildProNbr(), txnContext));

        } catch (ServiceException se) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", String.format("Child PRO %s is invalid.", updateHandlingUnitAsAdminRqst.getHandlingUnit().getChildProNbr()))
                .log()
                .build();
        }

        if (StringUtils.isBlank(updateHandlingUnitAsAdminRqst.getHandlingUnit().getParentProNbr())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo("N/A", "Parent PRO is required.").log().build();
        }

        try {
            updateHandlingUnitAsAdminRqst.getHandlingUnit().setParentProNbr(ProNumberHelper.validateProNumber(updateHandlingUnitAsAdminRqst.getHandlingUnit().getParentProNbr(), txnContext));
        } catch (ServiceException se) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", String.format("Parent PRO %s is invalid.", updateHandlingUnitAsAdminRqst.getHandlingUnit().getParentProNbr()))
                .log()
                .build();
        }

        if (updateHandlingUnitAsAdminRqst.getHandlingUnit().getCurrentTrailerInstanceId() != null && updateHandlingUnitAsAdminRqst.getHandlingUnit().getCurrentTrailerInstanceId() > 0
                && StringUtils.isNotBlank(updateHandlingUnitAsAdminRqst.getHandlingUnit().getCurrentDockLocation())) {
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
            .moreInfo("N/A", String.format("Child pro can be only at one location at any point in time.", updateHandlingUnitAsAdminRqst.getHandlingUnit().getParentProNbr()))
            .log()
            .build();
        }

        if (updateHandlingUnitAsAdminRqst.getHandlingUnit().getMovementStatusCd() == null) {
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.MVMT_STAT_CD_RQ, txnContext)
            .log()
            .build();
        } else {
            try {
                MovementStatusCdTransformer.toEnum(updateHandlingUnitAsAdminRqst.getHandlingUnit().getMovementStatusCd());
            } catch (Exception e) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.MVMT_STATUS_CD_INVALID, txnContext)
                .moreInfo("MovementStatusCd", updateHandlingUnitAsAdminRqst.getHandlingUnit().getMovementStatusCd())
                .log()
                .build();

            }
        }

        ShmShipment shipment = shmShipmentSubDAO.findByIdOrProNumber(updateHandlingUnitAsAdminRqst.getHandlingUnit().getParentProNbr(), null, entityManager);

        if (shipment == null) {
            throw ExceptionBuilder
                .exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext)
            .moreInfo("parentProNbr", updateHandlingUnitAsAdminRqst.getHandlingUnit().getParentProNbr()).build();
        }


       return shipment;

    }


}
