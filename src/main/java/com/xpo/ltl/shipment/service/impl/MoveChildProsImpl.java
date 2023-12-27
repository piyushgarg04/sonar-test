package com.xpo.ltl.shipment.service.impl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.javatuples.Pair;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.ChildShipmentId;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.MoveChildProsResp;
import com.xpo.ltl.api.shipment.v2.MoveChildProsRqst;
import com.xpo.ltl.api.shipment.v2.ParentProNbrReplacement;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;
import com.xpo.ltl.shipment.service.util.HandlingUnitHelper;
import com.xpo.ltl.shipment.service.util.HandlingUnitMovementHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * author: @sarthaksingh
 * Parent ticket: CCS-6747
 * Move an existing PLT pro to a new parent pro with supporting logic
 */
@RequestScoped
public class MoveChildProsImpl {
    
    protected static List<ShmHandlingUnit> shmHandlingUnitsList = null;
    protected static List<ShmShipment> shmShipmentList = null;
    protected static List<ShmHandlingUnitMvmt> shmHandlingUnitMvmtList = null;
    protected static Map<String, String> originalParentProMap = null;
    
    private static final String OUT_FOR_DELIVERY = "3";
    private static final String FINAL_DLVD = "5";
    private static final String TYPE_CD_LOOSE = "LOOSE";
    private static final String TYPE_CD_MOTOR = "MOTOR";

    /*
     * childProNbr -> ShmHandlingUnit, ShmShipment(for new parent) and ShmShipment(for old parent) mapping
     */
    private Map<String, Pair<ShmHandlingUnit, Pair<ShmShipment, ShmShipment>>> childProDetailsMap = new HashMap<>();

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

    @Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ReplaceChildProsImpl replaceChildProsImpl;


    public MoveChildProsResp moveChildPros(MoveChildProsRqst rqst, TransactionContext txnContext,
            EntityManager entityManager) throws ServiceException, ValidationException{
        checkNotNull(rqst, "The request is required.");
        checkNotNull(rqst.getParentProNbrReplacements(), "Atleast one parent pro replacement request is required.");
        checkNotNull(entityManager, "The EntityManager is required." );
        validateProNumbers(rqst, txnContext);
        getDbDetailsForChildProAndNewParentPro(rqst, entityManager);
        MoveChildProsResp resp = new MoveChildProsResp();
        resp.setChildShipmentIds(processReplacement(rqst.getParentProNbrReplacements(), entityManager, txnContext));
        return resp;
    }

    private void validateProNumbers(MoveChildProsRqst replaceRqst, TransactionContext txnContext) throws ValidationException {
        for(ParentProNbrReplacement rqst: replaceRqst.getParentProNbrReplacements()){
            String childProTxt = StringUtils.EMPTY;
            String parentProTxt = StringUtils.EMPTY;
            try {
                childProTxt = ProNumberHelper.validateProNumber(rqst.getChildProNbr(), txnContext);
                parentProTxt = ProNumberHelper.validateProNumber(rqst.getNewParentProNbr(), txnContext);
                rqst.setChildProNbr(childProTxt);
                rqst.setNewParentProNbr(parentProTxt);
            } catch (ServiceException e) {
                if(StringUtils.isEmpty(childProTxt)){
                    throw ExceptionBuilder
                    .exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext)
                    .log()
                    .moreInfo("MoveChildPros.validateProNumbers" ,String.format("Child PRO %s is invalid", rqst.getChildProNbr()))
                    .build();  
                }
                else{
                    throw ExceptionBuilder
                    .exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext)
                    .log()
                    .moreInfo("MoveChildPros.validateProNumbers" ,String.format("New Parent PRO %s is invalid", rqst.getNewParentProNbr()))
                    .build(); 
                }
            }
        }
    }

    private void getDbDetailsForChildProAndNewParentPro(MoveChildProsRqst rqst, EntityManager entityManager){   
        final List<String> childProNumberList = CollectionUtils.emptyIfNull(rqst.getParentProNbrReplacements())
                                         .stream()
                                         .filter(r -> r.getChildProNbr() != null && r.getNewParentProNbr() != null)
                                         .map(r -> r.getChildProNbr())
                                         .collect(Collectors.toList());

        final List<String> newParentProNumberList = CollectionUtils.emptyIfNull(rqst.getParentProNbrReplacements())
                                        .stream()
                                        .filter(r -> r.getChildProNbr() != null && r.getNewParentProNbr() != null)
                                        .map(r -> r.getNewParentProNbr())
                                        .collect(Collectors.toList());
        /*
         * Returns a list of handling units for a given parent shipment instance Id including the handling unit movements
         */
        shmHandlingUnitsList = shmHandlingUnitSubDAO.listByChildProNumbers(childProNumberList, entityManager);
        originalParentProMap = CollectionUtils.emptyIfNull(shmHandlingUnitsList)
                                             .stream()
                                             .filter(hu -> hu != null)
                                             .collect(Collectors.toMap(ShmHandlingUnit::getChildProNbrTxt, ShmHandlingUnit::getParentProNbrTxt));

        for(ShmHandlingUnit hu: shmHandlingUnitsList){
            if(!originalParentProMap.containsKey(hu.getChildProNbrTxt())){
                originalParentProMap.put(hu.getChildProNbrTxt(), hu.getParentProNbrTxt());
            }
        }
        final List<String> originalParentProNumberList = new ArrayList<String>(originalParentProMap.values());
        final List<String> parentProNumberList = new ArrayList<String>(newParentProNumberList);
        parentProNumberList.addAll(originalParentProNumberList);
        shmShipmentList = shmShipmentSubDAO.findByProNbrs(parentProNumberList, entityManager);

    }
    private List<ChildShipmentId> processReplacement(List<ParentProNbrReplacement> replaceRqsts, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {
        List<ChildShipmentId> respList = new ArrayList<ChildShipmentId>();
        for(ParentProNbrReplacement replaceRqst: replaceRqsts){
            ChildShipmentId id = new ChildShipmentId();
            populateChildDetailsMap(replaceRqst);
            if(childProDetailsMap.get(replaceRqst.getChildProNbr()) != null){
                ShmHandlingUnit handlingUnit = childProDetailsMap.get(replaceRqst.getChildProNbr()).getValue0();
                if(handlingUnit == null){
                    throw ExceptionBuilder
                    .exception(ValidationErrorMessage.MOVE_CHILD_PROS_RQST_INVALID, txnContext)
                    .moreInfo("MoveChildProsImpl", String.format("No handling unit found for the child pro#: %s", replaceRqst.getChildProNbr()))
                    .build();
                }
                
                ShmShipment newParentShipment = childProDetailsMap.get(replaceRqst.getChildProNbr()).getValue1().getValue0();
                if(newParentShipment == null){
                    throw ExceptionBuilder
                    .exception(ValidationErrorMessage.MOVE_CHILD_PROS_RQST_INVALID, txnContext)
                    .moreInfo("MoveChildProsImpl", String.format("The new parent pro# %s does not exist and cannot be used.", replaceRqst.getNewParentProNbr()))
                    .build();
                }
                
                if(replaceRqst.getSplitInd() == null){
                    /*
                     * CCS-6747 (new requirement)
                     * when we have both parent pro and child pro not loaded to either trailer or dock location.
                     * In this case if the CURRENT_SIC are the same in SHM_SHIPMENT and SHM_HANDLING_UNIT, 
                     * then they are not split. if they are different then they are split.
                     */
                    if(StringUtils.equalsIgnoreCase(newParentShipment.getCurrSicCd(), handlingUnit.getCurrentSicCd()))
                        replaceRqst.setSplitInd(Boolean.FALSE);
                    else
                        replaceRqst.setSplitInd(Boolean.TRUE);
                }
                ShmShipment oldParentShipment = childProDetailsMap.get(replaceRqst.getChildProNbr()).getValue1().getValue1();
                Optional<ShmHandlingUnitMvmt> handlingUnitMvmt =  CollectionUtils.emptyIfNull(handlingUnit.getShmHandlingUnitMvmts())
                                                        .stream()
                                                        .filter(mvmt -> mvmt.getId().getShpInstId() == handlingUnit.getId().getShpInstId()
                                                                     && mvmt.getId().getSeqNbr() == handlingUnit.getId().getSeqNbr())
                                                        .sorted((o1,o2) ->  BasicTransformer.toInt(o2.getId().getMvmtSeqNbr())
                                                                          - BasicTransformer.toInt(o1.getId().getMvmtSeqNbr()))
                                                        .findFirst();
                                                        
                List<ShmHandlingUnit> oldSiblingHandlingUnits = CollectionUtils.emptyIfNull(oldParentShipment.getShmHandlingUnits())
                                                        .stream()
                                                        .filter(hu -> hu != null && StringUtils.equals(hu.getParentProNbrTxt(), originalParentProMap.get(replaceRqst.getChildProNbr())))
                                                        .collect(Collectors.toList());

                List<ShmHandlingUnit> newSiblingHandlingUnits = CollectionUtils.emptyIfNull(newParentShipment.getShmHandlingUnits())
                                                        .stream()
                                                        .filter(hu -> StringUtils.equals(hu.getParentProNbrTxt(), replaceRqst.getNewParentProNbr()))
                                                        .sorted((o1,o2) -> BasicTransformer.toInt(o2.getId().getSeqNbr())
                                                                         - BasicTransformer.toInt(o1.getId().getSeqNbr()))
                                                        .collect(Collectors.toList());

                Long seqNbrHu = CollectionUtils.isEmpty(newSiblingHandlingUnits)?1L:newSiblingHandlingUnits.get(0).getId().getSeqNbr()+1;                                  
                Long oldSeqNbr = handlingUnit.getId().getSeqNbr();                                    
                requestValidation(replaceRqst, handlingUnit, newParentShipment, txnContext);
                ShmHandlingUnit newHandlingUnit = updateHandlingUnitEntity(handlingUnit, seqNbrHu, replaceRqst, newParentShipment, oldParentShipment.getShpInstId(), entityManager, txnContext);
                createHandlingUnitMvmt(newHandlingUnit, seqNbrHu, handlingUnitMvmt, replaceRqst, txnContext, entityManager);
                updateOldAndNewProDetails(newHandlingUnit, newParentShipment, newSiblingHandlingUnits, oldParentShipment, oldSiblingHandlingUnits, oldSeqNbr, entityManager, txnContext);
                

                id.setChildProNbr(replaceRqst.getChildProNbr());
                ShipmentId parentId = new ShipmentId();
                parentId.setProNumber(replaceRqst.getNewParentProNbr());
                id.setParentShipmentId(parentId);
                id.setShipmentInstId(newParentShipment.getShpInstId());
            }
            respList.add(id);
        }
        return respList;
    }
    
    private void updateOldAndNewProDetails(ShmHandlingUnit handlingUnit, ShmShipment newParentShipment, List<ShmHandlingUnit> newSiblingHandlingUnits, ShmShipment oldParentShipment, List<ShmHandlingUnit> oldSiblingHandlingUnits,
    Long oldSeqNbr, EntityManager entityManager, TransactionContext txnContext) throws ServiceException{
        updateOriginalParentDetails(handlingUnit, oldParentShipment, entityManager, oldSiblingHandlingUnits, txnContext);
        updateOriginalParentChildDetails(oldParentShipment, handlingUnit, oldSiblingHandlingUnits, oldSeqNbr, entityManager, txnContext);
        updateNewParentDetails(handlingUnit, newParentShipment, entityManager, newSiblingHandlingUnits, txnContext);
        updateNewParentChildDetails(newParentShipment, handlingUnit, newSiblingHandlingUnits, entityManager, txnContext);
    }

    /*
     * For the other child PROs on the new parent PRO,
     * if SHM_HANDLING_UNIT.REWEIGH_IND = 'Y', no processing.
     * if SHM_HANDLING_UNIT.REWEIGH_IND = 'N' for some, 
     * subtract the weight (SHM_HANDLING_UNIT.WGT_LBS) of the child which are reweighed (SHM_HANDLING_UNIT.REWEIGH_IND = 'Y') from the total weight (SHM_SHIPMENT.TOT_WGT_LBS) of the new parent PRO
     * divide the remainder weight of the new parent between the child PROs which are not reweighed ( SHM_HANDLING_UNIT.REWEIGH_IND = 'N')
     * the weight of any child PRO should not be less than 0 under any condition. Default weight = 1lb
     */
    private void updateNewParentChildDetails(ShmShipment newParentShipment, ShmHandlingUnit shmHandlingUnit, List<ShmHandlingUnit> newSiblingHandlingUnits, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {
        newSiblingHandlingUnits.add(shmHandlingUnit);
        BigDecimal totWgt = newParentShipment.getTotWgtLbs();
        BigDecimal divisor = BigDecimal.ZERO;
        BigDecimal subtrahend = BigDecimal.ZERO;

        for(ShmHandlingUnit hu: newSiblingHandlingUnits){
            if(hu.getReweighInd() == "Y")
                subtrahend = subtrahend.add(hu.getWgtLbs());
            else
                divisor = divisor.add(BigDecimal.ONE);
        }
        if(divisor.compareTo(BigDecimal.ZERO) != 0){
            totWgt = totWgt.subtract(subtrahend);
            BigDecimal newWgt = totWgt.divide(divisor, 2, RoundingMode.HALF_UP);
            if(newWgt.compareTo(BigDecimal.ZERO) < 0)
                totWgt = BigDecimal.ONE;
            else
                totWgt = newWgt;
        }
        for(ShmHandlingUnit hu: newSiblingHandlingUnits){
            if(hu.getReweighInd().equals("N") 
            && divisor.compareTo(BigDecimal.ZERO) != 0){
                ShmHandlingUnit newHu = HandlingUnitHelper.clone(hu);
                hu.setWgtLbs(totWgt);
                try{
                    replaceChildProsImpl.populateDefaultValuesForNull(newHu);
                    hu = shmHandlingUnitSubDAO.save(hu, entityManager);
                    shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(hu, hu.getLstUpdtTmst(), txnContext, db2EntityManager);
                }
                catch(final Exception e){
                    throw ExceptionBuilder
                    .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                    .log()
                    .moreInfo("SQL", "Unable to persist the entity for sibling handling units for the new parent shipment")
                    .build();  
                }
            }
        }
    }

    /*
     * if SHM_HANDLING_UNIT.TYPE_CD = ‘LOOSE’ for child PRO, increment SHM_SHIPMENT.LOOSE_PCS_CNT by 1. 
     * if SHM_HANDLING_UNIT.TYPE_CD = ‘MOTOR’ for child PRO, increment SHM_SHIPMENT.MTRZD_PCS_CNT by 1.
     * update SHM_SHIPMENT.TOT_VOL_CFT by adding to it SHM_HANDLING_UNIT.VOL_CFT
     */
    private void updateNewParentDetails(ShmHandlingUnit handlingUnit, ShmShipment parentShipment, EntityManager entityManager, List<ShmHandlingUnit> newSiblingHandlingUnits, TransactionContext txnContext) throws ServiceException {
        /*
        * CCS-7881
        * If there is a child PRO exemption DO NOT update loose/mtrzd pcs count
        * set exemption ind to N (or fail)
        */
        if(parentShipment.getHandlingUnitExemptionInd().equals("Y")){
            parentShipment.setHandlingUnitExemptionInd("N");
        }
        
        long loosePiecesCnt = 0;
        long mtrzdPiecesCnt = 0;

        if(handlingUnit.getTypeCd().equals(TYPE_CD_LOOSE)){
            loosePiecesCnt++;
        }
        else if(handlingUnit.getTypeCd().equals(TYPE_CD_MOTOR)){
            mtrzdPiecesCnt++;
        }

        for(ShmHandlingUnit hu: newSiblingHandlingUnits) {
            if(hu.getTypeCd().equals(TYPE_CD_LOOSE)){
                loosePiecesCnt++;
            }
            else if(hu.getTypeCd().equals(TYPE_CD_MOTOR)){
                mtrzdPiecesCnt++;
            }
        }

        parentShipment.setLoosePcsCnt(BasicTransformer.toBigDecimal(loosePiecesCnt));
        parentShipment.setMtrzdPcsCnt(BasicTransformer.toBigDecimal(mtrzdPiecesCnt));

        

        BigDecimal totVolCft = parentShipment.getTotVolCft().add(handlingUnit.getVolCft());
        BigDecimal pupVolPct = parentShipment.getPupVolPct().add(handlingUnit.getPupVolPct());
        parentShipment.setTotVolCft(totVolCft);
        parentShipment.setPupVolPct(pupVolPct);
        try{
            parentShipment = shmShipmentSubDAO.save(parentShipment, entityManager);
            shmShipmentSubDAO.updateDB2ShmShipment(parentShipment, parentShipment.getLstUpdtTmst(), txnContext, db2EntityManager);
        }
        catch(final Exception e){
            throw ExceptionBuilder
            .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
            .log()
            .moreInfo("SQL", "Unable to persist the entity for new parent ShmShipment")
            .build();  
        }
    }

    /*
     * For the other child PROs on the original parent PRO,
     * if SHM_HANDLING_UNIT.REWEIGH_IND = 'Y', no processing.
     * if SHM_HANDLING_UNIT.REWEIGH_IND = 'N' for some, 
     * subtract the weight (SHM_HANDLING_UNIT.WGT_LBS) of the child which are reweighed (SHM_HANDLING_UNIT.REWEIGH_IND = 'Y') from the total weight (SHM_SHIPMENT.TOT_WGT_LBS) of the original parent PRO
     * divide the remainder weight of the original parent between the child PROs which are not reweighed ( SHM_HANDLING_UNIT.REWEIGH_IND = 'N')
     * the weight of any child PRO should not be less than 0 under any condition. Default weight = 1lb
     */
    private void updateOriginalParentChildDetails(ShmShipment originalParentShipment, ShmHandlingUnit shmHandlingUnit, List<ShmHandlingUnit> oldSiblingHandlingUnits, Long oldSeqNbr, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {
        BigDecimal totWgt = originalParentShipment.getTotWgtLbs();
        BigDecimal divisor = BigDecimal.ZERO;
        BigDecimal subtrahend = BigDecimal.ZERO;

        for(ShmHandlingUnit hu: oldSiblingHandlingUnits){
            if(hu.getId().getSeqNbr() != oldSeqNbr){
                if(hu.getReweighInd() == "Y")
                    subtrahend = subtrahend.add(hu.getWgtLbs());
                else
                    divisor = divisor.add(BigDecimal.ONE);
            }
        }
        if(divisor.compareTo(BigDecimal.ZERO) != 0){
            totWgt = totWgt.subtract(subtrahend);
            BigDecimal newWgt = totWgt.divide(divisor, 2, RoundingMode.HALF_UP);
            if(newWgt.compareTo(BigDecimal.ZERO) < 0)
                totWgt = BigDecimal.ONE;
            else
                totWgt = newWgt;
        }
        for(ShmHandlingUnit hu: oldSiblingHandlingUnits){
            if(hu.getReweighInd().equals("N")
                && divisor.compareTo(BigDecimal.ZERO) != 0
                && hu.getId().getSeqNbr() != oldSeqNbr){
                hu.setWgtLbs(totWgt);
                try{
                    replaceChildProsImpl.populateDefaultValuesForNull(hu);
                    hu = shmHandlingUnitSubDAO.save(hu, entityManager);
                    shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(hu, hu.getLstUpdtTmst(), txnContext, db2EntityManager);
                }
                catch(final Exception e){
                    throw ExceptionBuilder
                    .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                    .log()
                    .moreInfo("SQL", "Unable to persist the entity for sibling handling units for the original parent shipment")
                    .build();  
                }
                
            }
        }
    }

    /*
     * if SHM_HANDLING_UNIT.TYPE_CD = ‘LOOSE’ for child PRO, reduce SHM_SHIPMENT.LOOSE_PCS_CNT by 1. 
     * if SHM_HANDLING_UNIT.TYPE_CD = ‘MOTOR’ for child PRO, reduce SHM_SHIPMENT.MTRZD_PCS_CNT by 1.
     * SHM_SHIPMENT.LOOSE_PCS_CNT and SHM_SHIPMENT.MTRZD_PCS_CNT should always be equal or greaten than 0.
     * update SHM_SHIPMENT.TOT_VOL_CFT by subtracting from it SHM_HANDLING_UNIT.VOL_CFT
     */
    private void updateOriginalParentDetails(ShmHandlingUnit handlingUnit, ShmShipment parentShipment, EntityManager entityManager, List<ShmHandlingUnit> oldSiblingHandlingUnits, TransactionContext txnContext) throws ServiceException {
        long loosePiecesCnt = 0;
        long mtrzdPiecesCnt = 0;

        if(handlingUnit.getTypeCd().equals(TYPE_CD_LOOSE)){
            loosePiecesCnt--;
        }
        else if(handlingUnit.getTypeCd().equals(TYPE_CD_MOTOR)){
            mtrzdPiecesCnt--;
        }

        for(ShmHandlingUnit hu: oldSiblingHandlingUnits) {
            if(hu.getTypeCd().equals(TYPE_CD_LOOSE)){
                loosePiecesCnt++;
            }
            else if(hu.getTypeCd().equals(TYPE_CD_MOTOR)){
                mtrzdPiecesCnt++;
            }
        }

        if(loosePiecesCnt <= 0L && mtrzdPiecesCnt <= 0L) {
            if(handlingUnit.getTypeCd().equals(TYPE_CD_LOOSE)){
                loosePiecesCnt=1L;
            }
            else if(handlingUnit.getTypeCd().equals(TYPE_CD_MOTOR)){
                mtrzdPiecesCnt=1L;
            }
        }

        parentShipment.setLoosePcsCnt(BasicTransformer.toBigDecimal(loosePiecesCnt).max(BigDecimal.ZERO));
        parentShipment.setMtrzdPcsCnt(BasicTransformer.toBigDecimal(mtrzdPiecesCnt).max(BigDecimal.ZERO));
        

        BigDecimal totVolCft = parentShipment.getTotVolCft().subtract(handlingUnit.getVolCft());
        BigDecimal pupVolPct = parentShipment.getPupVolPct().subtract(handlingUnit.getPupVolPct());
        parentShipment.setTotVolCft(totVolCft.compareTo(BigDecimal.ZERO)<=0?BigDecimal.ZERO:totVolCft);
        parentShipment.setPupVolPct(pupVolPct.compareTo(BigDecimal.ZERO)<=0?BigDecimal.ZERO:pupVolPct);
        try{
            parentShipment = shmShipmentSubDAO.save(parentShipment, entityManager);
            shmShipmentSubDAO.updateDB2ShmShipment(parentShipment, parentShipment.getLstUpdtTmst(), txnContext, db2EntityManager);
        }
        catch(final Exception e){
            throw ExceptionBuilder
            .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
            .log()
            .moreInfo("SQL", "Unable to persist the entity for original parent ShmShipment")
            .build();  
        }
    }

    private void populateChildDetailsMap(ParentProNbrReplacement replaceRqst) {
        if(replaceRqst.getChildProNbr() != null && replaceRqst.getNewParentProNbr() != null){
            //populating Map for all necessary dB details needed for the operation
            ShmHandlingUnit handlingUnit = CollectionUtils.emptyIfNull(shmHandlingUnitsList)
                                           .stream()
                                           .filter(unit -> StringUtils.equals(unit.getChildProNbrTxt(),replaceRqst.getChildProNbr()))
                                           .findFirst()
                                           .orElse(null);
            ShmShipment newParentShipment = CollectionUtils.emptyIfNull(shmShipmentList)
                                         .stream()
                                         .filter(shpmt -> StringUtils.equals(shpmt.getProNbrTxt(),replaceRqst.getNewParentProNbr()))
                                         .findFirst()
                                         .orElse(null);
            ShmShipment oldParentShipment = CollectionUtils.emptyIfNull(shmShipmentList)
                                         .stream()
                                         .filter(shpmt -> StringUtils.equals(shpmt.getProNbrTxt(), originalParentProMap.get(replaceRqst.getChildProNbr())))
                                         .findFirst()
                                         .orElse(null);
            

            if(!childProDetailsMap.containsKey(replaceRqst.getChildProNbr()))
                childProDetailsMap.put(replaceRqst.getChildProNbr(), new Pair<ShmHandlingUnit,Pair<ShmShipment,ShmShipment>>(handlingUnit, new Pair<ShmShipment,ShmShipment>(newParentShipment, oldParentShipment)));
            }
    }

    private void createHandlingUnitMvmt(ShmHandlingUnit handlingUnit, Long seqNbrHu, Optional<ShmHandlingUnitMvmt> latestHandlingUnitMvmt, ParentProNbrReplacement replaceRqst, TransactionContext txnContext, EntityManager entityManager) throws ServiceException{
        ShmHandlingUnitMvmt newHandlingUnitMvmt = new ShmHandlingUnitMvmt();
        ShmHandlingUnitMvmtPK pk = new ShmHandlingUnitMvmtPK();
        pk.setShpInstId(handlingUnit.getId().getShpInstId());
        pk.setSeqNbr(seqNbrHu);
        pk.setMvmtSeqNbr(1L);
        
        AuditInfo auditInfo = new AuditInfo();
        auditInfo = AuditInfoHelper.getAuditInfo(txnContext);
        DtoTransformer.setAuditInfo(newHandlingUnitMvmt, auditInfo);
        newHandlingUnitMvmt.setId(pk);
        newHandlingUnitMvmt.setMvmtTypCd("MOVE");
        newHandlingUnitMvmt.setCrteUid(auditInfo.getCreatedById());
        newHandlingUnitMvmt.setShmHandlingUnit(handlingUnit);
        newHandlingUnitMvmt.setMvmtRptgSicCd(handlingUnit.getCurrentSicCd());
        newHandlingUnitMvmt.setMvmtTmst(new Timestamp(System.currentTimeMillis()));
        newHandlingUnitMvmt.setCrteTmst(new Timestamp(System.currentTimeMillis()));
        ShmHandlingUnitMvmt mvmt = new ShmHandlingUnitMvmt();
        if(latestHandlingUnitMvmt.isPresent()){
            mvmt = latestHandlingUnitMvmt.get();
        } 
        newHandlingUnitMvmt.setSplitAuthorizeTmst(
                 DB2DefaultValueUtil.LOW_TMST);
        newHandlingUnitMvmt.setArchiveCntlCd(StringUtils.isNotBlank(mvmt.getArchiveCntlCd()) ? mvmt.getArchiveCntlCd() : StringUtils.SPACE);
        newHandlingUnitMvmt.setBypassScanInd(StringUtils.isNotBlank(mvmt.getBypassScanInd()) ? mvmt.getBypassScanInd() : StringUtils.SPACE);
        newHandlingUnitMvmt.setBypassScanReason(StringUtils.isNotBlank(mvmt.getBypassScanReason()) ? mvmt.getBypassScanReason() : StringUtils.SPACE);
        newHandlingUnitMvmt.setDmgdCatgCd(StringUtils.isNotBlank(mvmt.getDmgdCatgCd()) ? mvmt.getDmgdCatgCd() : StringUtils.SPACE);
        newHandlingUnitMvmt.setDockInstId(mvmt.getDockInstId() == null ? BigDecimal.ZERO : mvmt.getDockInstId());
        newHandlingUnitMvmt.setExcpTypCd(StringUtils.isNotBlank(mvmt.getExcpTypCd()) ? mvmt.getExcpTypCd() : StringUtils.SPACE);
        newHandlingUnitMvmt.setMvmtRptgSicCd(StringUtils.isNotBlank(mvmt.getMvmtRptgSicCd()) ? mvmt.getMvmtRptgSicCd() : StringUtils.SPACE);
        newHandlingUnitMvmt.setRfsdRsnCd(StringUtils.isNotBlank(mvmt.getRfsdRsnCd()) ? mvmt.getRfsdRsnCd() : StringUtils.SPACE);
        newHandlingUnitMvmt.setRmrkTxt(StringUtils.isNotBlank(mvmt.getRmrkTxt()) ? mvmt.getRmrkTxt() : StringUtils.SPACE);
        newHandlingUnitMvmt.setScanTmst(DB2DefaultValueUtil.LOW_TMST);
        newHandlingUnitMvmt.setTrlrInstId(mvmt.getTrlrInstId() == null ? BigDecimal.ZERO : mvmt.getTrlrInstId());
        newHandlingUnitMvmt.setUndlvdRsnCd(StringUtils.isNotBlank(mvmt.getUndlvdRsnCd()) ? mvmt.getUndlvdRsnCd() : StringUtils.SPACE);
        newHandlingUnitMvmt.setSplitAuthorizeBy(StringUtils.isNotBlank(mvmt.getSplitAuthorizeBy()) ? mvmt.getSplitAuthorizeBy() : StringUtils.SPACE);
        try{
            shmHandlingUnitMvmtSubDAO.save(newHandlingUnitMvmt, entityManager);
            shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(newHandlingUnitMvmt, db2EntityManager);
        }
        catch(final Exception e){
            throw ExceptionBuilder
            .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
            .log()
            .moreInfo("SQL", String.format("Unable to persist the entity for ShmHandlingUnitMvmt::: %s", e.getMessage()))
            .build();  
        }
        if(replaceRqst.getSplitInd() == Boolean.TRUE){
            ShmHandlingUnitMvmt newHandlingUnitMvmtSplit = HandlingUnitMovementHelper.cloneHuMvmt(newHandlingUnitMvmt);
            newHandlingUnitMvmtSplit.getId().setMvmtSeqNbr(newHandlingUnitMvmtSplit.getId().getMvmtSeqNbr() + 1);
            newHandlingUnitMvmtSplit.setMvmtTypCd("SPLIT");
            newHandlingUnitMvmtSplit.setSplitAuthorizeBy("SYSTEM");
            newHandlingUnitMvmtSplit.setSplitAuthorizeTmst(new Timestamp(System.currentTimeMillis()));
            try{
                shmHandlingUnitMvmtSubDAO.save(newHandlingUnitMvmtSplit, entityManager);
                shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(newHandlingUnitMvmtSplit, db2EntityManager);
            }
            catch(final Exception e){
                throw ExceptionBuilder
                .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                .log()
                .moreInfo("SQL", String.format("Unable to persist the entity for ShmHandlingUnitMvmt split ::: %s", e.getMessage()))
                .build();  
            }
        }
    }


    private ShmHandlingUnit updateHandlingUnitEntity(ShmHandlingUnit handlingUnitOriginal, Long seqNbrHu, ParentProNbrReplacement replaceRqst,
            ShmShipment parentShipment, Long oldShpInstId, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {
        ShmHandlingUnit handlingUnit = HandlingUnitHelper.clone(handlingUnitOriginal);
        ShmHandlingUnitPK id = new ShmHandlingUnitPK();
        ShmHandlingUnitPK idDeleteDB2 = new ShmHandlingUnitPK();
        idDeleteDB2.setShpInstId(oldShpInstId);
        idDeleteDB2.setSeqNbr(handlingUnit.getId().getSeqNbr());
        id.setSeqNbr(seqNbrHu);
        id.setShpInstId(parentShipment.getShpInstId());
        handlingUnit.setId(id);
        handlingUnit.setParentProNbrTxt(replaceRqst.getNewParentProNbr());
        if(replaceRqst.getSplitInd() == Boolean.TRUE){
            handlingUnit.setSplitInd("Y");
            parentShipment.setHandlingUnitSplitInd("Y");
        }
        handlingUnit.setShmShipment(parentShipment);
        AuditInfo auditInfo = new AuditInfo();
        auditInfo = AuditInfoHelper.getAuditInfo(txnContext);
        DtoTransformer.setLstUpdateAuditInfo(handlingUnit, auditInfo);
        try{
            replaceChildProsImpl.populateDefaultValuesForNull(handlingUnit);
            handlingUnit = shmHandlingUnitSubDAO.updateHandlingUnitRecords(handlingUnitOriginal, handlingUnit, idDeleteDB2, entityManager, db2EntityManager, txnContext);
        }
        catch(final ServiceException e){
            throw ExceptionBuilder
            .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
            .log()
            .moreInfo("SQL", String.format("Unable to persist the entity for ShmHandlingUnit: %s", e.getMessage()))
            .build();  
        }
        return handlingUnit;
    }

    /*
     * The child pro cannot be final delivered or out for delivery
     * The child pro cannot be loaded on a trailer. 
     * The “New” parent pro has to exist in the SHM_SHIPMENT table and must  not be qualified as “Final Delivered” 
     */
    private void requestValidation(ParentProNbrReplacement replaceRqst, ShmHandlingUnit handlingUnit, ShmShipment parentShipment, TransactionContext txnContext) throws ValidationException{

        final String huMvmtStatCd = handlingUnit.getMvmtStatCd();
        final BigDecimal currTrailerInstId = handlingUnit.getCurrentTrlrInstId();

        if(StringUtils.equals(huMvmtStatCd, OUT_FOR_DELIVERY)){
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.MOVE_CHILD_PROS_RQST_INVALID, txnContext)
            .moreInfo("MoveChildProsImpl", String.format("Child PRO# %s is out for delivery and cannot be moved to a new parent.", replaceRqst.getChildProNbr()))
            .build();
        }
        if(StringUtils.equals(huMvmtStatCd, FINAL_DLVD)){
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.MOVE_CHILD_PROS_RQST_INVALID, txnContext)
            .moreInfo("MoveChildProsImpl", String.format("Child PRO# %s is final delivered and cannot be moved to a new parent.", replaceRqst.getChildProNbr()))
            .build();
        }
        if(BasicTransformer.toLong(currTrailerInstId) != 0L){
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.MOVE_CHILD_PROS_RQST_INVALID, txnContext)
            .moreInfo("MoveChildProsImpl", String.format("Child PRO# %s is loaded on a trailer and cannot be moved to a new parent.", replaceRqst.getChildProNbr()))
            .build();
        }
        if(StringUtils.equals(parentShipment.getDlvryQalfrCd(), DeliveryQualifierCdTransformer.toCode(DeliveryQualifierCd.FINAL))){
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.MOVE_CHILD_PROS_RQST_INVALID, txnContext)
            .moreInfo("MoveChildProsImpl", String.format("The new parent pro# %s is final delivered and cannot be used.", replaceRqst.getNewParentProNbr()))
            .build();
        }
    }
}
