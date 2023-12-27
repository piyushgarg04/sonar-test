package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ExceptionBuilder;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationErrorMessage;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.ChildProNbrReplacement;
import com.xpo.ltl.api.shipment.v2.ChildShipmentId;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.ReplaceChildProsResp;
import com.xpo.ltl.api.shipment.v2.ReplaceChildProsRqst;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

/**
 * @author Sanjay Kamble
 *
 */
@RequestScoped
public class ReplaceChildProsImpl {

    private static final String PGM_ID = "REPLCP";
    private static final String HU_MVMT_STATUS_CD_FINAL_DLVD = "5";
    private static final String HU_MVMT_TYPE_CD_OUT_FOR_DLVRY = "3";

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
	private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;


    /**
     * Method to replace current child pros with new child pros
     * 
     * @param replaceChildProsRqst
     * @param txnContext
     * @param entityManager
     * @return ReplaceChildProsResp
     * @throws ServiceException
     */
    public ReplaceChildProsResp replaceChildProsWithNewPros(
        ReplaceChildProsRqst replaceChildProsRqst, TransactionContext txnContext,
        EntityManager entityManager) throws ServiceException {
        
        checkNotNull(replaceChildProsRqst, "ReplaceChildPros Request is required.");
        checkNotNull(txnContext, "The TransactionContext is required.");
        checkNotNull(entityManager, "The EntityManager is required.");
        
        ReplaceChildProsResp replaceChildProsResp = new ReplaceChildProsResp();
        
        LinkedHashMap<String, List<String>> errorMsgsMap = new LinkedHashMap<>();
        
        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext);

        validateInputRqstAndPros(replaceChildProsRqst, errorMsgsMap, txnContext);
        
        Map<String, ChildProNbrReplacement> reqNewProsMap = replaceChildProsRqst
                .getChildProNbrReplacements()
                .stream()
                .filter(hu -> StringUtils.isNotBlank(hu.getNewChildProNbr()) && !errorMsgsMap.containsKey(hu.getNewChildProNbr()))
                .collect(Collectors.toMap(ChildProNbrReplacement::getNewChildProNbr, hu -> hu));
       
        List<String> childPros = replaceChildProsRqst
                .getChildProNbrReplacements()
                .stream()
                .filter(hu -> StringUtils.isNotBlank(hu.getCurrentChildProNbr()) && !errorMsgsMap.containsKey(hu.getCurrentChildProNbr()))
                .map(ChildProNbrReplacement::getCurrentChildProNbr)
                .distinct()
                .collect(Collectors.toList());
        
        //Find Shipment Handling Unit from DB by Child Pro Number list
        List<ShmHandlingUnit> shmHandlingUnitsDB = shmHandlingUnitSubDAO
            .findByChildProNumberList(new HashSet<>(childPros), entityManager);
        
        if (shmHandlingUnitsDB.isEmpty()) {
            addErrorMessageToMap("ReplaceChildProsImpl", "Child PRO is not found.", errorMsgsMap);
        }

        Map<String, List<ShmHandlingUnit>> childProAndShmHUsMapFromDB = shmHandlingUnitsDB
            .stream()
            .collect(Collectors.groupingBy(ShmHandlingUnit::getChildProNbrTxt, Collectors.toList()));
        
        //Get all Shipment Handling Units from DB
        List<ShmHandlingUnit> allShmHandlingUnitsDB = shmHandlingUnitSubDAO
                .getAllShmHandlingUnit(entityManager);
        
        validateNewChildProsAgainstDB(reqNewProsMap, allShmHandlingUnitsDB, errorMsgsMap, txnContext);

        validateHuMvmtCd(shmHandlingUnitsDB, errorMsgsMap);
       
        throwExceptionIfAnyValidationError(errorMsgsMap, txnContext);

        //Replace the existing child PRO with the new Child PRO and Create SHM Handling Unit Mvmt record
        replaceChildProsAndCreateShmHandlingUnitMvmts(shmHandlingUnitsDB, childProAndShmHUsMapFromDB,
            replaceChildProsRqst, replaceChildProsResp, entityManager, auditInfo, txnContext);

        return replaceChildProsResp;
    }

    /**
     * Method to replace Child Pros and create SHM Handling Unit Mvmt
     * 
     * @param allShmHandlingUnitsDB
     * @param childProAndShmHUsMapFromDB
     * @param replaceChildProsRqst
     * @param replaceChildProsResp
     * @param entityManager
     * @param auditInfo
     * @param txnContext
     * @throws ServiceException
     */
    private void replaceChildProsAndCreateShmHandlingUnitMvmts(List<ShmHandlingUnit> allShmHandlingUnitsDB,
        Map<String, List<ShmHandlingUnit>> childProAndShmHUsMapFromDB,
        ReplaceChildProsRqst replaceChildProsRqst, ReplaceChildProsResp replaceChildProsResp, EntityManager entityManager,
        AuditInfo auditInfo, TransactionContext txnContext) throws ServiceException {
        
        List<ChildShipmentId> childShipmentIds = new ArrayList<>();

        for (ChildProNbrReplacement childProNbrReplacement : replaceChildProsRqst
            .getChildProNbrReplacements()) {

            List<ShmHandlingUnit> shmHandlingUnitListForChildPro= childProAndShmHUsMapFromDB
                .get(childProNbrReplacement.getCurrentChildProNbr());
            
            ChildShipmentId childShipmentId = new ChildShipmentId();
            
            for (ShmHandlingUnit handlingUnit : shmHandlingUnitListForChildPro) {
                
                DtoTransformer.setLstUpdateAuditInfo(handlingUnit, auditInfo);
                handlingUnit.setChildProNbrTxt(childProNbrReplacement.getNewChildProNbr());
                
                shmHandlingUnitSubDAO.persist(handlingUnit, entityManager);
               
                //CCS-7257: Populate Default Values For Null for DB2
                populateDefaultValuesForNull(handlingUnit);
                
                shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(handlingUnit, handlingUnit.getLstUpdtTmst(), txnContext, db2EntityManager);
                
                Optional<Long> mvmtSeqNbr = CollectionUtils.emptyIfNull(handlingUnit.getShmHandlingUnitMvmts())
                        .stream()
                        .map(mvmt -> (Long) mvmt.getId().getMvmtSeqNbr())
                        .max(Long::compare);
                Long nextMvmtSeqNbr = mvmtSeqNbr.isPresent() ?  mvmtSeqNbr.get() + 1L : 1L;

                persistShipmentHandlingUnitMvmt(true, handlingUnit, nextMvmtSeqNbr, handlingUnit.getCurrentSicCd(), 
                    auditInfo, entityManager);
                
                childShipmentId.setChildProNbr(childProNbrReplacement.getNewChildProNbr());
                childShipmentId.setShipmentInstId(handlingUnit.getId().getShpInstId());
                ShipmentId id = new ShipmentId();
                id.setProNumber(handlingUnit.getParentProNbrTxt());
                childShipmentId.setParentShipmentId(id);
                
                childShipmentIds.add(childShipmentId);
            }
       }
        replaceChildProsResp.setChildShipmentIds(childShipmentIds);
    }

    
    /**
     * Method to Persist SHM Handling Unit Mvmt
     * 
     * @param markAsSplit
     * @param shmHandlingUnit
     * @param nextMvmSeqNbr
     * @param requestingSicCd
     * @param auditInfo
     * @param entityManager
     * @throws ValidationException
     */
    protected void persistShipmentHandlingUnitMvmt(Boolean markAsSplit, ShmHandlingUnit shmHandlingUnit, long nextMvmSeqNbr,
        String requestingSicCd, AuditInfo auditInfo, EntityManager entityManager)
            throws ValidationException {

        ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
        id.setShpInstId(shmHandlingUnit.getId().getShpInstId());
        id.setSeqNbr(shmHandlingUnit.getId().getSeqNbr());
        id.setMvmtSeqNbr(nextMvmSeqNbr);
        
        ShmHandlingUnitMvmt mvmt = shmHandlingUnit.getShmHandlingUnitMvmts().stream().max(Comparator.comparingLong(obj -> obj.getId().getMvmtSeqNbr())).orElse(new ShmHandlingUnitMvmt());

        ShmHandlingUnitMvmt shmHandlingUnitMvmt = new ShmHandlingUnitMvmt();
        DtoTransformer.setAuditInfo(shmHandlingUnitMvmt, auditInfo);
        shmHandlingUnitMvmt.setId(id);
        shmHandlingUnitMvmt.setShmHandlingUnit(shmHandlingUnit);
        shmHandlingUnitMvmt.setCrteUid(auditInfo.getCreatedById());
        shmHandlingUnitMvmt.setSplitAuthorizeBy(StringUtils.isNotBlank(mvmt.getSplitAuthorizeBy()) ? mvmt.getSplitAuthorizeBy() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setSplitAuthorizeTmst(
                 DB2DefaultValueUtil.LOW_TMST);
        Timestamp currentTmst = new Timestamp(System.currentTimeMillis());
        shmHandlingUnitMvmt.setCrteTmst(currentTmst);
        shmHandlingUnitMvmt.setMvmtTmst(currentTmst);
        shmHandlingUnitMvmt
            .setMvmtTypCd(markAsSplit 
                ? HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.REPLACE)
                    : HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.REPLACE));
        shmHandlingUnitMvmt.setArchiveCntlCd(StringUtils.isNotBlank(mvmt.getArchiveCntlCd()) ? mvmt.getArchiveCntlCd() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanInd(StringUtils.isNotBlank(mvmt.getBypassScanInd()) ? mvmt.getBypassScanInd() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanReason(StringUtils.isNotBlank(mvmt.getBypassScanReason()) ? mvmt.getBypassScanReason() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setDmgdCatgCd(StringUtils.isNotBlank(mvmt.getDmgdCatgCd()) ? mvmt.getDmgdCatgCd() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setDockInstId(mvmt.getDockInstId() == null ? BigDecimal.ZERO : mvmt.getDockInstId());
        shmHandlingUnitMvmt.setExcpTypCd(StringUtils.isNotBlank(mvmt.getExcpTypCd()) ? mvmt.getExcpTypCd() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setMvmtRptgSicCd(StringUtils.isNotBlank(requestingSicCd) ? requestingSicCd : StringUtils.SPACE);
        shmHandlingUnitMvmt.setRfsdRsnCd(StringUtils.isNotBlank(mvmt.getRfsdRsnCd()) ? mvmt.getRfsdRsnCd() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setRmrkTxt(StringUtils.isNotBlank(mvmt.getRmrkTxt()) ? mvmt.getRmrkTxt() : StringUtils.SPACE);
        shmHandlingUnitMvmt.setScanTmst(DB2DefaultValueUtil.LOW_TMST);
        shmHandlingUnitMvmt.setTrlrInstId(mvmt.getTrlrInstId() == null ? BigDecimal.ZERO : mvmt.getTrlrInstId());
        shmHandlingUnitMvmt.setUndlvdRsnCd(StringUtils.isNotBlank(mvmt.getUndlvdRsnCd()) ? mvmt.getUndlvdRsnCd() : StringUtils.SPACE);

        shmHandlingUnitMvmtSubDAO.save(shmHandlingUnitMvmt, entityManager);
        shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(shmHandlingUnitMvmt, db2EntityManager);
    }

    /**
     * Method to Validate input Request and Pros
     * @param replaceChildProsRqst
     * @param errorMsgsMap
     * @param txnContext
     * @throws ValidationException
     */
    protected void validateInputRqstAndPros(ReplaceChildProsRqst replaceChildProsRqst,
        LinkedHashMap<String, List<String>> errorMsgsMap,
        TransactionContext txnContext)
            throws ValidationException {
        
        if (CollectionUtils.isEmpty(replaceChildProsRqst.getChildProNbrReplacements())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "At least one ChildProNbrReplacements is required.")
                .log()
                .build();
        }

        if (CollectionUtils.isEmpty(replaceChildProsRqst.getChildProNbrReplacements())
                || replaceChildProsRqst.getChildProNbrReplacements().stream().allMatch(hu -> hu.getCurrentChildProNbr() == null)) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "At least one CurrentChildProNbr is required.")
                .log()
                .build();
        }
        
        if (CollectionUtils.isEmpty(replaceChildProsRqst.getChildProNbrReplacements())
                || replaceChildProsRqst.getChildProNbrReplacements().stream().allMatch(hu -> hu.getNewChildProNbr() == null)) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "At least one NewChildProNbr is required.")
                .log()
                .build();
        }

        Iterator<ChildProNbrReplacement> it = replaceChildProsRqst.getChildProNbrReplacements().iterator();
        while (it.hasNext()) {
            ChildProNbrReplacement childProNbrReplacement = it.next();          
            validateCurrentChildPro(childProNbrReplacement, errorMsgsMap, txnContext);
            validateNewChildPro(childProNbrReplacement, errorMsgsMap, txnContext);
        }
    }

    private void addErrorMessageToMap(String ChildProNbrKey, String newMessage,
        LinkedHashMap<String, List<String>> errorMsgsMap) {
        List<String> errors = errorMsgsMap.getOrDefault(ChildProNbrKey, new ArrayList<String>());
        errors.add(newMessage);
        errorMsgsMap.put(ChildProNbrKey, errors);
    }
    
    /**
     * Method to Validate Current Child PRO
     * 
     * @param childProNbrReplacement
     * @param errorMsgsMap
     * @param txnContext
     */
    private void validateCurrentChildPro(ChildProNbrReplacement childProNbrReplacement, LinkedHashMap<String, List<String>> errorMsgsMap,
        TransactionContext txnContext) {
        if (StringUtils.isBlank(childProNbrReplacement.getCurrentChildProNbr())) {
            addErrorMessageToMap(childProNbrReplacement.getCurrentChildProNbr(), "Current Child PRO is required.", errorMsgsMap);
            return;
        }

        try {
            childProNbrReplacement.setCurrentChildProNbr(ProNumberHelper.validateProNumber(childProNbrReplacement.getCurrentChildProNbr(), txnContext));
        } catch (ServiceException se) {
            addErrorMessageToMap(childProNbrReplacement.getCurrentChildProNbr(), String.format("Current Child PRO %s is invalid.", childProNbrReplacement.getCurrentChildProNbr()),
                errorMsgsMap);
        }

    }
    
    /**
     * Method to Validate New Child Pro Number 
     * 
     * @param childProNbrReplacement
     * @param errorMsgsMap
     * @param txnContext
     */
    private void validateNewChildPro(ChildProNbrReplacement childProNbrReplacement, LinkedHashMap<String, List<String>> errorMsgsMap,
        TransactionContext txnContext) {
        if (StringUtils.isBlank(childProNbrReplacement.getNewChildProNbr())) {
            addErrorMessageToMap(childProNbrReplacement.getNewChildProNbr(), "New Child PRO is required.", errorMsgsMap);
            return;
        }
        
        if (childProNbrReplacement.getNewChildProNbr().equals(childProNbrReplacement.getCurrentChildProNbr())) {
            addErrorMessageToMap(childProNbrReplacement.getNewChildProNbr(), String.format("Child PRO number and New Child PRO number cannot not be the same", childProNbrReplacement.getCurrentChildProNbr(), childProNbrReplacement.getNewChildProNbr()),errorMsgsMap);
            return;
        }
        
        try {
            childProNbrReplacement.setNewChildProNbr(ProNumberHelper.validateProNumber(childProNbrReplacement.getNewChildProNbr(), txnContext));
        } catch (ServiceException se) {
            addErrorMessageToMap(childProNbrReplacement.getNewChildProNbr(), String.format("New Child PRO %s is invalid.", childProNbrReplacement.getNewChildProNbr()),
                errorMsgsMap);
        }
        
    }
    
    /**
     * Method to Validate handling mvmt code
     * 
     * @param shmHandlingUnitsDB
     * @param errorMsgsMap
     */
    private void validateHuMvmtCd(List<ShmHandlingUnit> shmHandlingUnitsDB, LinkedHashMap<String, List<String>> errorMsgsMap) {
        for (ShmHandlingUnit handlingUnit : shmHandlingUnitsDB) {
            // Validate handling mvmt cd is present and should not be 3 (Out for Delivery).
            if (StringUtils.isBlank(handlingUnit.getMvmtStatCd()) || HU_MVMT_TYPE_CD_OUT_FOR_DLVRY.equals(handlingUnit.getMvmtStatCd())) {
                addErrorMessageToMap(handlingUnit.getChildProNbrTxt(), String.format("Child PRO %s is out for delivery and cannot be replaced.", handlingUnit.getChildProNbrTxt()),
                    errorMsgsMap);
            }
            
            // Validate handling mvmt cd is present and should not be 5 (Final Delivered).
            if (StringUtils.isBlank(handlingUnit.getMvmtStatCd()) || HU_MVMT_STATUS_CD_FINAL_DLVD.equals(handlingUnit.getMvmtStatCd())) {
                addErrorMessageToMap(handlingUnit.getChildProNbrTxt(), String.format("Child PRO %s is final delivered and cannot be replaced.", handlingUnit.getChildProNbrTxt()),
                    errorMsgsMap);
            }
        }
       
    }
    
    /**
     * Method to Validate New Child Pros against database record
     * 
     * @param reqNewProsMap
     * @param allShmHandlingUnitsDB
     * @param errorMsgsMap
     * @param txnContext
     */
    private void validateNewChildProsAgainstDB(Map<String, ChildProNbrReplacement> reqNewProsMap,
        List<ShmHandlingUnit> allShmHandlingUnitsDB, LinkedHashMap<String, List<String>> errorMsgsMap,
        TransactionContext txnContext) {

        // Validate New child Pros
        for (String NewChildPro : reqNewProsMap.keySet()) {
            boolean existsInDB = allShmHandlingUnitsDB
                .stream()
                .anyMatch(huDB -> huDB.getChildProNbrTxt().equals(NewChildPro));
            if (existsInDB) {
                addErrorMessageToMap(NewChildPro, String.format("The PRO %s already exists and cannot be used.", NewChildPro),
                    errorMsgsMap);
            }
        }
    }

    private void throwExceptionIfAnyValidationError(LinkedHashMap<String, List<String>> errorMsgsMap,
        TransactionContext txnContext) throws ValidationException {
        if (!errorMsgsMap.isEmpty()) {
            List<MoreInfo> moreInfoList = new ArrayList<MoreInfo>();

            for (String childPro : errorMsgsMap.keySet()) {
                MoreInfo moreInfo = new MoreInfo();
                moreInfo.setLocation(childPro);
                moreInfo.setMessage(String.join(" ", errorMsgsMap.get(childPro)));
                moreInfoList.add(moreInfo);
            }
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo(moreInfoList)
                .log()
                .build();
        }
    }
    
    //CCS-7257: Populate Default Values for Null in DB2 two phase commit 
    public void populateDefaultValuesForNull(ShmHandlingUnit shmHandlingUnit) {
        if (Objects.isNull(shmHandlingUnit)) {
            return;
        }
        shmHandlingUnit.setArchiveInd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getArchiveInd()));
        shmHandlingUnit.setChildProNbrTxt(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getChildProNbrTxt()));
        shmHandlingUnit.setCrteTmst(DB2DefaultValueUtil.getValueOrLowTmst(shmHandlingUnit.getCrteTmst()));
        shmHandlingUnit.setCrteTranCd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getCrteTranCd()));
        shmHandlingUnit.setCrteUid(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getCrteUid()));
        shmHandlingUnit.setCurrentDockLocTxt(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getCurrentDockLocTxt()));
        shmHandlingUnit.setCurrentSicCd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getCurrentSicCd()));
        shmHandlingUnit.setCurrentTrlrInstId(DB2DefaultValueUtil.getValueOr0(shmHandlingUnit.getCurrentTrlrInstId()));
        shmHandlingUnit.setDimensionTypeCd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getDimensionTypeCd()));
        shmHandlingUnit.setDmlTmst(DB2DefaultValueUtil.getValueOrLowTmst(shmHandlingUnit.getDmlTmst()));
        shmHandlingUnit.setDtlCapxtimestamp(DB2DefaultValueUtil.getValueOrLowTmst(shmHandlingUnit.getDtlCapxtimestamp()));
        shmHandlingUnit.setHandlingMvmtCd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getHandlingMvmtCd()));
        shmHandlingUnit.setLstMvmtTmst(DB2DefaultValueUtil.getValueOrLowTmst(shmHandlingUnit.getLstMvmtTmst()));
        shmHandlingUnit.setLstUpdtTmst(DB2DefaultValueUtil.getValueOrLowTmst(shmHandlingUnit.getLstUpdtTmst()));
        shmHandlingUnit.setHeightNbr(DB2DefaultValueUtil.getValueOr0(shmHandlingUnit.getHeightNbr()));
        shmHandlingUnit.setLengthNbr(DB2DefaultValueUtil.getValueOr0(shmHandlingUnit.getLengthNbr()));
        shmHandlingUnit.setLstUpdtTranCd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getLstUpdtTranCd()));
        shmHandlingUnit.setLstUpdtUid(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getLstUpdtUid()));
        shmHandlingUnit.setMovrProNbrTxt(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getMovrProNbrTxt()));
        shmHandlingUnit.setMovrSuffix(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getMovrSuffix()));
        shmHandlingUnit.setMvmtStatCd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getMvmtStatCd()));
        shmHandlingUnit.setParentProNbrTxt(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getParentProNbrTxt()));
        shmHandlingUnit.setPkupDt(DB2DefaultValueUtil.getValueOrLowTmst(shmHandlingUnit.getPkupDt()));
        shmHandlingUnit.setPoorlyPackagedInd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getPoorlyPackagedInd()));
        shmHandlingUnit.setPupVolPct(DB2DefaultValueUtil.getValueOr0(shmHandlingUnit.getPupVolPct()));
        shmHandlingUnit.setPoorlyPackagedInd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getPoorlyPackagedInd()));
        shmHandlingUnit.setReplLstUpdtTmst(DB2DefaultValueUtil.getValueOrLowTmst(shmHandlingUnit.getReplLstUpdtTmst()));
        shmHandlingUnit.setReweighInd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getReweighInd()));
        shmHandlingUnit.setSplitInd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getSplitInd()));
        shmHandlingUnit.setStackableInd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getStackableInd()));
        shmHandlingUnit.setTypeCd(DB2DefaultValueUtil.getValueOrSpace(shmHandlingUnit.getTypeCd()));
        shmHandlingUnit.setVolCft(DB2DefaultValueUtil.getValueOr0(shmHandlingUnit.getVolCft()));
        shmHandlingUnit.setWgtLbs(DB2DefaultValueUtil.getValueOr0(shmHandlingUnit.getWgtLbs()));
        shmHandlingUnit.setWidthNbr(DB2DefaultValueUtil.getValueOr0(shmHandlingUnit.getWidthNbr()));
    }
}
