package com.xpo.ltl.shipment.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmNotification;
import com.xpo.ltl.api.shipment.service.entity.ShmNotificationPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DataStoreUseCd;
import com.xpo.ltl.api.shipment.v2.ShmNotification;
import com.xpo.ltl.api.shipment.v2.UpsertShipmentNotificationsResp;
import com.xpo.ltl.api.shipment.v2.UpsertShipmentNotificationsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.NumberUtil;
import com.xpo.ltl.shipment.service.dao.ShmNotificationSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.enums.ShipmentAppointmentStatusEnum;
import com.xpo.ltl.shipment.service.enums.ShmNotificationStatusCdEnum;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ShipmentUtil;
import com.xpo.ltl.shipment.service.util.ShmNotificationUtil;
import com.xpo.ltl.shipment.service.util.TimestampUtil;
import com.xpo.ltl.shipment.service.validators.UpsertShipmentNotificationsValidator;

@RequestScoped
@LogExecutionTime
public class UpsertShipmentNotificationsImpl {

    private static final String TRAN_NAME = "USRTNTFN";
    private static final String TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String LOW_DATE_TIME = "0001-01-01T00:00:00.000";

    @Inject private UpsertShipmentNotificationsValidator upsertShipmentNotificationsValidator;
    
    @Inject private ShmShipmentSubDAO shmShipmentSubDAO;
    
    @Inject private ShmNotificationSubDAO shmNotificationSubDAO;
    
    public UpsertShipmentNotificationsResp upsertShipmentNotifications(
            UpsertShipmentNotificationsRqst request, 
            TransactionContext txnContext, 
            EntityManager entityManager,
            EntityManager db2EntityManager) 
                    throws ServiceException {
        
        upsertShipmentNotificationsValidator.validate(request, txnContext);

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(TRAN_NAME, txnContext);

        List<Long> shpInstIds = request.getShipmentNotifications().
                stream()
                .map(ShmNotification::getShipmentInstId)
                .collect(Collectors.toList());

        List<ShmNotification> updatedShmNotifications = new ArrayList<>();
        
        // Get all the data
        List<ShmShipment> shmShipmentEntities = shmShipmentSubDAO.listShipmentsByShipmentIds(shpInstIds, entityManager);
        List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> shmNotificationsForAllShps = shmNotificationSubDAO.listNotificationsForShipmentIdList(shpInstIds, entityManager);

        // Performs the updates
        if (CollectionUtils.isNotEmpty(shmShipmentEntities)) {
            for (com.xpo.ltl.api.shipment.v2.ShmNotification requestShmNotification : request.getShipmentNotifications()) {         
                List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> notificationsThisShipment = 
                        ShmNotificationUtil.listNotificationsByShpId(requestShmNotification.getShipmentInstId(), shmNotificationsForAllShps);
                
                if (request.getDataStoreUseCd() == DataStoreUseCd.LEGACY_ONLY) {
                    backsyncOracleToDB2(requestShmNotification, notificationsThisShipment, updatedShmNotifications, shmShipmentEntities, auditInfo, txnContext, db2EntityManager);                    
                } else {
                    if (NumberUtil.isNullOrZero(requestShmNotification.getNotificationSequenceNbr())) {
                        createShmNotification(
                            requestShmNotification, 
                            notificationsThisShipment, 
                            updatedShmNotifications, 
                            request.getDataStoreUseCd(), 
                            auditInfo, 
                            txnContext, 
                            entityManager, 
                            db2EntityManager);
                        ShmShipment shmShipment = getShipment(requestShmNotification.getShipmentInstId(), shmShipmentEntities, txnContext);
                        shmShipment.setNtfictnCd(requestShmNotification.getCategoryCd());
                        updateShmShipment(shmShipment, request.getDataStoreUseCd(), auditInfo, txnContext, entityManager, db2EntityManager);
                    } else {
                        updateShmNotification(
                            requestShmNotification, 
                            notificationsThisShipment, 
                            updatedShmNotifications, 
                            shmShipmentEntities,
                            request.getDataStoreUseCd(), 
                            auditInfo, 
                            txnContext, 
                            entityManager, 
                            db2EntityManager);
                    }
                }
            }
        } else {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("No shipments were found in the system for the request.").build();
        }
        
        UpsertShipmentNotificationsResp upsertShipmentNotificationsResp = new UpsertShipmentNotificationsResp();
        upsertShipmentNotificationsResp.setShipmentNotifications(updatedShmNotifications);
        return upsertShipmentNotificationsResp;
    }
    
    private void updateShmShipment(
            ShmShipment shmShipment,
            DataStoreUseCd dataStoreUseCd,
            AuditInfo auditInfo, 
            TransactionContext txnContext, 
            EntityManager entityManager,
            EntityManager db2EntityManager) 
                    throws ServiceException {
        
        DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);
        
        if (dataStoreUseCd == DataStoreUseCd.OPERATIONAL_ONLY || dataStoreUseCd == DataStoreUseCd.OPERATIONAL_AND_LEGACY) {
            shmShipmentSubDAO.persist(shmShipment, entityManager);
        }
        
        if (dataStoreUseCd == DataStoreUseCd.LEGACY_ONLY || dataStoreUseCd == DataStoreUseCd.OPERATIONAL_AND_LEGACY) {
            shmShipmentSubDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(), txnContext, db2EntityManager);
        }
    }
    
    private ShmShipment getShipment(Long shpInstId, List<ShmShipment> shmShipmentEntities, TransactionContext txnContext) throws ValidationException {
        
        ShmShipment shmShipment = ShipmentUtil.findShipmentByShpInstId(shpInstId, shmShipmentEntities);
        if (null == shmShipment) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("Shipment was not found in the system for shipment instance ID " + shpInstId).build();
        }
        return shmShipment;
    }
    
    private void backsyncOracleToDB2(
            ShmNotification requestShmNotification, 
            List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> notificationsThisShipment,
            List<ShmNotification> updatedShmNotifications,
            List<ShmShipment> shmShipmentEntities,
            AuditInfo auditInfo, 
            TransactionContext txnContext, 
            EntityManager db2EntityManager) throws ServiceException {

        Long shpInstId = requestShmNotification.getShipmentInstId();
        Long seqNbr = requestShmNotification.getNotificationSequenceNbr().longValue();
        com.xpo.ltl.api.shipment.service.entity.ShmNotification matchingNotification = ShmNotificationUtil.getNotificationBySeqNbrAndShpInstId(shpInstId, seqNbr, notificationsThisShipment);
        
        if (null == matchingNotification) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("Shipment was not found in the system for shipment instance ID " + requestShmNotification.getShipmentInstId()).build();
        } else {
            DB2ShmNotification dB2ShmNotification = shmNotificationSubDAO.findDB2ByShipmentIdAndSeqNbr(shpInstId, seqNbr, db2EntityManager);
            if (null == dB2ShmNotification) {
                DtoTransformer.setLstUpdateAuditInfo(matchingNotification, auditInfo);
                shmNotificationSubDAO.createDB2ShmNotification(matchingNotification, db2EntityManager);
            } else {
                DtoTransformer.setAuditInfo(matchingNotification, auditInfo);
                shmNotificationSubDAO.updateDB2ShmNotification(matchingNotification, db2EntityManager, txnContext);
            }
            updatedShmNotifications.add(EntityTransformer.toShmNotification(matchingNotification));

            ShmShipment shmShipment = getShipment(requestShmNotification.getShipmentInstId(), shmShipmentEntities, txnContext);
            DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);
            shmShipmentSubDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(), txnContext, db2EntityManager);
        }
    }

    private void createShmNotification(
            ShmNotification requestShmNotification, 
            List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> notificationsThisShipment,
            List<ShmNotification> updatedShmNotifications,
            DataStoreUseCd dataStoreUseCd,
            AuditInfo auditInfo, 
            TransactionContext txnContext, 
            EntityManager entityManager,
            EntityManager db2EntityManager) 
                    throws ValidationException {
        
        Long nextSeqNbr = ShmNotificationUtil.findLastSeqNbrForThisShipment(notificationsThisShipment);
        nextSeqNbr = nextSeqNbr.longValue() + 1;
        com.xpo.ltl.api.shipment.service.entity.ShmNotification newShmNotification = new com.xpo.ltl.api.shipment.service.entity.ShmNotification();
        ShmNotificationPK shmNotificationPk = new ShmNotificationPK();
        shmNotificationPk.setShpInstId(requestShmNotification.getShipmentInstId());
        shmNotificationPk.setNtfictnSeqNbr(nextSeqNbr);
        newShmNotification.setId(shmNotificationPk);

        if (null == requestShmNotification.getAppointmentNote()) {
            newShmNotification.setApptNote(StringUtils.SPACE);
        } else {
            newShmNotification.setApptNote(requestShmNotification.getAppointmentNote());
        }
        newShmNotification.setCallerRacfId(requestShmNotification.getCallerRacfId());
        newShmNotification.setCallTmst(BasicTransformer.toTimestamp(requestShmNotification.getCallDateTime()));
        newShmNotification.setCatgCd(requestShmNotification.getCategoryCd().toUpperCase());
        if (null == requestShmNotification.getConsigneeRefNbr()) {
            newShmNotification.setConsRefNbrTxt(StringUtils.SPACE);
        } else {
            newShmNotification.setConsRefNbrTxt(requestShmNotification.getConsigneeRefNbr());
        }
        if (null == requestShmNotification.getFbdsNote()) {
            newShmNotification.setFbdsNote(StringUtils.SPACE);
        } else {
            newShmNotification.setFbdsNote(requestShmNotification.getFbdsNote());
        }
        if (null == requestShmNotification.getReschdReasonCd()) {
            newShmNotification.setReschdRsnCd(StringUtils.SPACE);
        } else {
            newShmNotification.setReschdRsnCd(requestShmNotification.getReschdReasonCd());
        }
        try {
            newShmNotification.setSchdDlvyDt(BasicTransformer.toDate(TimestampUtil.stringToXmlGregorianCalendar(requestShmNotification.getScheduledDeliveryDate(), TIME_PATTERN)));
        } catch (Throwable t) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("The scheduled delivery date for shipment instance ID " + requestShmNotification.getShipmentInstId() + " was invalid.").log().build();
        }
        try {
            Date scheduledFromDateTime = BasicTransformer.toDate(TimestampUtil.stringToXmlGregorianCalendar(requestShmNotification.getScheduledDeliveryFromTime(), TIME_PATTERN));
            newShmNotification.setSchdDlvyFromTm(scheduledFromDateTime);
        } catch (Throwable t) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("The scheduled delivery from-time for shipment instance ID " + requestShmNotification.getShipmentInstId() + " was invalid.").log().build();
        }
        try {
            Date scheduledToDateTime = BasicTransformer.toDate(TimestampUtil.stringToXmlGregorianCalendar(requestShmNotification.getScheduledDeliveryToTime(), TIME_PATTERN));
            newShmNotification.setSchdDlvyToTm(scheduledToDateTime);
        } catch (Throwable t) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("The scheduled delivery to-time for shipment instance ID " + requestShmNotification.getShipmentInstId() + " was invalid.").log().build();
        }
        newShmNotification.setScoNtficnInstId(BasicTransformer.toBigDecimal(requestShmNotification.getScoNtficnInstId()));
        newShmNotification.setSicCd(requestShmNotification.getSicCd().toUpperCase());
        newShmNotification.setStatCd(requestShmNotification.getStatusCd());
        if (null == requestShmNotification.getTypeCd()) {
            newShmNotification.setTypCd(StringUtils.SPACE);
        } else {
            newShmNotification.setTypCd(requestShmNotification.getTypeCd());
        }
        updatedShmNotifications.add(EntityTransformer.toShmNotification(newShmNotification));
        DtoTransformer.setAuditInfo(newShmNotification, auditInfo);
        if (dataStoreUseCd == DataStoreUseCd.OPERATIONAL_ONLY || dataStoreUseCd == DataStoreUseCd.OPERATIONAL_AND_LEGACY) {
            shmNotificationSubDAO.save(newShmNotification, entityManager);
        }
        if (dataStoreUseCd == DataStoreUseCd.LEGACY_ONLY || dataStoreUseCd == DataStoreUseCd.OPERATIONAL_AND_LEGACY) {
            shmNotificationSubDAO.createDB2ShmNotification(newShmNotification, db2EntityManager);
        }
    }

    private void updateShmNotification(
            ShmNotification requestShmNotification, 
            List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> notificationsThisShipment,
            List<ShmNotification> updatedShmNotifications,
            List<ShmShipment> shmShipmentEntities,
            DataStoreUseCd dataStoreUseCd,
            AuditInfo auditInfo, 
            TransactionContext txnContext, 
            EntityManager entityManager,
            EntityManager db2EntityManager) 
                    throws ServiceException {
        
        if (isRemoveShipmentFromAppointment(requestShmNotification)) {
            removeShipmentFromAppointment(
                requestShmNotification, 
                updatedShmNotifications, 
                shmShipmentEntities, 
                notificationsThisShipment,
                dataStoreUseCd, 
                auditInfo, 
                txnContext, 
                entityManager, 
                db2EntityManager);
        } else {
            updateNotficationForShipment(
                requestShmNotification, 
                notificationsThisShipment, 
                updatedShmNotifications, 
                shmShipmentEntities,
                dataStoreUseCd, 
                auditInfo, 
                txnContext, 
                entityManager, 
                db2EntityManager);
        }
    }
    
    private boolean isRemoveShipmentFromAppointment(ShmNotification requestShmNotification) {
        
        boolean isRemoveShipmentFromAppointment = false;
        
        if (StringUtils.equals(requestShmNotification.getStatusCd(), ShipmentAppointmentStatusEnum.REMOVED_FROM_APPOINTMENT.getCode())) {
            isRemoveShipmentFromAppointment = true;
        }
        return isRemoveShipmentFromAppointment;
    }
    
    /**
     * Removes the shipment from an existing appointment.  To remove all the appointment notifications for a shipment, the consumer needs 
     * send all notifications for that shipment related to the Operations notification appointment instance ID (scoNtficnInstId) are on this 
     * request with the correct status of Removed From Appointment.
     */
    private void removeShipmentFromAppointment(
            ShmNotification requestShmNotification, 
            List<ShmNotification> updatedShmNotifications, 
            List<ShmShipment> shmShipmentEntities,
            List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> notificationsThisShipment,            
            DataStoreUseCd dataStoreUseCd, 
            AuditInfo auditInfo, 
            TransactionContext txnContext, 
            EntityManager entityManager, 
            EntityManager db2EntityManager) throws ServiceException {
        
        ShmShipment shmShipment = getShipment(requestShmNotification.getShipmentInstId(), shmShipmentEntities, txnContext);
        shmShipment.setNtfictnCd(StringUtils.SPACE);
        updateShmShipment(shmShipment, dataStoreUseCd, auditInfo, txnContext, entityManager, db2EntityManager);
        
        com.xpo.ltl.api.shipment.service.entity.ShmNotification shmNotificationForUpdate = getNotificationToUpdate(requestShmNotification, notificationsThisShipment, txnContext);
        shmNotificationForUpdate.setStatCd(ShipmentAppointmentStatusEnum.REMOVED_FROM_APPOINTMENT.getCode());
        DtoTransformer.setLstUpdateAuditInfo(shmNotificationForUpdate, auditInfo);
        
        if (dataStoreUseCd == DataStoreUseCd.OPERATIONAL_ONLY || dataStoreUseCd == DataStoreUseCd.OPERATIONAL_AND_LEGACY) {
            shmNotificationSubDAO.persist(shmNotificationForUpdate, entityManager);
        }
        
        if (dataStoreUseCd == DataStoreUseCd.LEGACY_ONLY || dataStoreUseCd == DataStoreUseCd.OPERATIONAL_AND_LEGACY) {
            shmNotificationSubDAO.updateDB2ShmNotification(shmNotificationForUpdate, db2EntityManager, txnContext);
        }
        
        updatedShmNotifications.add(EntityTransformer.toShmNotification(shmNotificationForUpdate));
    }
    
    private void updateNotficationForShipment(
            ShmNotification requestShmNotification, 
            List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> notificationsThisShipment,
            List<ShmNotification> updatedShmNotifications,
            List<ShmShipment> shmShipmentEntities,
            DataStoreUseCd dataStoreUseCd,
            AuditInfo auditInfo, 
            TransactionContext txnContext, 
            EntityManager entityManager,
            EntityManager db2EntityManager) 
                    throws ServiceException {

        com.xpo.ltl.api.shipment.service.entity.ShmNotification shmNotificationForUpdate = getNotificationToUpdate(requestShmNotification, notificationsThisShipment, txnContext);

        boolean hasRecordChanged = false;
        if (null != requestShmNotification.getAppointmentNote()) {
            String storedApptNote = (StringUtils.isEmpty(shmNotificationForUpdate.getApptNote()) ? StringUtils.SPACE : shmNotificationForUpdate.getApptNote());
            String requestApptNote = (StringUtils.isEmpty(requestShmNotification.getAppointmentNote()) ? StringUtils.SPACE : requestShmNotification.getAppointmentNote());
            if (!StringUtils.equals(requestApptNote, storedApptNote)) {
                shmNotificationForUpdate.setApptNote(requestApptNote);
                hasRecordChanged = true;
            }
        }
        if (!BasicTransformer.toTimestamp(requestShmNotification.getCallDateTime()).equals(shmNotificationForUpdate.getCallTmst())) {
            shmNotificationForUpdate.setCallTmst(BasicTransformer.toTimestamp(requestShmNotification.getCallDateTime()));
            hasRecordChanged = true;
        }
        if (!StringUtils.equals(requestShmNotification.getCallerRacfId(), shmNotificationForUpdate.getCallerRacfId())) {
            shmNotificationForUpdate.setCallerRacfId(requestShmNotification.getCallerRacfId());
            hasRecordChanged = true;
        }
        if (!StringUtils.equals(requestShmNotification.getCategoryCd(), shmNotificationForUpdate.getCatgCd())) {
            shmNotificationForUpdate.setCatgCd(requestShmNotification.getCategoryCd().toUpperCase());
            hasRecordChanged = true;
        }
        if (null != requestShmNotification.getConsigneeRefNbr()) {
            String storedConsRefNbr = (StringUtils.isEmpty(shmNotificationForUpdate.getConsRefNbrTxt()) ? StringUtils.SPACE : shmNotificationForUpdate.getConsRefNbrTxt());
            String requestConsRefNbr = (StringUtils.isEmpty(requestShmNotification.getConsigneeRefNbr()) ? StringUtils.SPACE : requestShmNotification.getConsigneeRefNbr());
            if (!StringUtils.equals(requestConsRefNbr, storedConsRefNbr)) {
                shmNotificationForUpdate.setConsRefNbrTxt(requestConsRefNbr);
                hasRecordChanged = true;
            }
        }
        if (null != requestShmNotification.getFbdsNote()) {
            String storedFbdsNote = (StringUtils.isEmpty(shmNotificationForUpdate.getFbdsNote()) ? StringUtils.SPACE : shmNotificationForUpdate.getFbdsNote());
            String requestFbdsNote = (StringUtils.isEmpty(requestShmNotification.getFbdsNote()) ? StringUtils.SPACE : requestShmNotification.getFbdsNote());
            if (!StringUtils.equals(requestFbdsNote, storedFbdsNote)) {
                shmNotificationForUpdate.setFbdsNote(requestFbdsNote);
                hasRecordChanged = true;
            }
        }
        if (null != requestShmNotification.getReschdReasonCd()) {
            String storedReschedRsnCd = (StringUtils.isEmpty(shmNotificationForUpdate.getReschdRsnCd()) ? StringUtils.SPACE : shmNotificationForUpdate.getReschdRsnCd());
            String requestReschedRsnCd = (StringUtils.isEmpty(requestShmNotification.getReschdReasonCd()) ? StringUtils.SPACE : requestShmNotification.getReschdReasonCd());
            if (!StringUtils.equals(requestReschedRsnCd, storedReschedRsnCd)) {
                shmNotificationForUpdate.setReschdRsnCd(requestReschedRsnCd);
                hasRecordChanged = true;
            }
        }
        try {
            if (isNotificationBeingSetToPending(requestShmNotification, shmNotificationForUpdate)) {
                shmNotificationForUpdate.setSchdDlvyDt(BasicTransformer.toDate(LOW_DATE_TIME));
                hasRecordChanged = true;
            } else {
                if (!BasicTransformer.toDate(requestShmNotification.getScheduledDeliveryDate()).equals(shmNotificationForUpdate.getSchdDlvyDt())) {
                    shmNotificationForUpdate.setSchdDlvyDt(BasicTransformer.toDate(TimestampUtil.stringToXmlGregorianCalendar(requestShmNotification.getScheduledDeliveryDate(), TIME_PATTERN)));
                    hasRecordChanged = true;
                }
            }
        } catch (Throwable t) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("The scheduled delivery date for shipment instance ID " + requestShmNotification.getShipmentInstId() + " was invalid.").log().build();
        }
        try {
            if (isNotificationBeingSetToPending(requestShmNotification, shmNotificationForUpdate)) {
                shmNotificationForUpdate.setSchdDlvyFromTm(BasicTransformer.toDate(LOW_DATE_TIME));
                hasRecordChanged = true;
            } else {
                Date scheduledFromDateTime = BasicTransformer.toDate(TimestampUtil.stringToXmlGregorianCalendar(requestShmNotification.getScheduledDeliveryFromTime(), TIME_PATTERN));
                if (!scheduledFromDateTime.equals(shmNotificationForUpdate.getSchdDlvyFromTm())) {
                    shmNotificationForUpdate.setSchdDlvyFromTm(scheduledFromDateTime);
                    hasRecordChanged = true;
                }
            }
        } catch (Throwable t) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("The scheduled delivery from-time for shipment instance ID " + requestShmNotification.getShipmentInstId() + " was invalid.").log().build();
        }
        try {
            if (isNotificationBeingSetToPending(requestShmNotification, shmNotificationForUpdate)) {
                shmNotificationForUpdate.setSchdDlvyToTm(BasicTransformer.toDate(LOW_DATE_TIME));
                hasRecordChanged = true;
            } else {
                Date scheduledToDateTime = BasicTransformer.toDate(TimestampUtil.stringToXmlGregorianCalendar(requestShmNotification.getScheduledDeliveryToTime(), TIME_PATTERN));
                if (!scheduledToDateTime.equals(shmNotificationForUpdate.getSchdDlvyToTm())) {
                    shmNotificationForUpdate.setSchdDlvyToTm(scheduledToDateTime);
                    hasRecordChanged = true;
                }
            }
        } catch (Throwable t) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("The scheduled delivery to-time for shipment instance ID " + requestShmNotification.getShipmentInstId() + " was invalid.").log().build();
        }
        if (requestShmNotification.getScoNtficnInstId().longValue() != shmNotificationForUpdate.getScoNtficnInstId().longValue()) {
            shmNotificationForUpdate.setScoNtficnInstId(BasicTransformer.toBigDecimal(requestShmNotification.getScoNtficnInstId()));
            hasRecordChanged = true;
        }
        if (!StringUtils.equals(requestShmNotification.getSicCd(), shmNotificationForUpdate.getSicCd())) {
            shmNotificationForUpdate.setSicCd(requestShmNotification.getSicCd().toUpperCase());
            hasRecordChanged = true;
        }
        if (!StringUtils.equals(requestShmNotification.getStatusCd(), shmNotificationForUpdate.getStatCd())) {
            shmNotificationForUpdate.setStatCd(requestShmNotification.getStatusCd());
            hasRecordChanged = true;
        }
        if (null != requestShmNotification.getTypeCd()) {
            String storedTypeCd = (StringUtils.isEmpty(shmNotificationForUpdate.getTypCd()) ? StringUtils.SPACE : shmNotificationForUpdate.getTypCd());
            String requestTypeCd = (StringUtils.isEmpty(requestShmNotification.getTypeCd()) ? StringUtils.SPACE : requestShmNotification.getTypeCd());
            if (!StringUtils.equals(requestTypeCd, storedTypeCd)) {
                shmNotificationForUpdate.setTypCd(requestTypeCd);
                hasRecordChanged = true;
            }
        }
        if (hasRecordChanged) {
            DtoTransformer.setLstUpdateAuditInfo(shmNotificationForUpdate, auditInfo);
            if (dataStoreUseCd == DataStoreUseCd.OPERATIONAL_ONLY || dataStoreUseCd == DataStoreUseCd.OPERATIONAL_AND_LEGACY) {
                shmNotificationSubDAO.persist(shmNotificationForUpdate, entityManager);
            }
            if (dataStoreUseCd == DataStoreUseCd.LEGACY_ONLY || dataStoreUseCd == DataStoreUseCd.OPERATIONAL_AND_LEGACY) {
                shmNotificationSubDAO.updateDB2ShmNotification(shmNotificationForUpdate, db2EntityManager, txnContext);
            }
            updatedShmNotifications.add(EntityTransformer.toShmNotification(shmNotificationForUpdate));
        }

        ShmShipment shmShipment = getShipment(requestShmNotification.getShipmentInstId(), shmShipmentEntities, txnContext);
        if (StringUtils.equals(requestShmNotification.getStatusCd(), ShipmentAppointmentStatusEnum.CANCELLED.getCode())) {
            shmShipment.setNtfictnCd(StringUtils.SPACE);
        } else {
            shmShipment.setNtfictnCd(requestShmNotification.getCategoryCd());
        }
        updateShmShipment(shmShipment, dataStoreUseCd, auditInfo, txnContext, entityManager, db2EntityManager);
    }
    
    private boolean isNotificationBeingSetToPending(ShmNotification requestShmNotification, com.xpo.ltl.api.shipment.service.entity.ShmNotification shmNotificationForUpdate) {
        
        boolean isNotificationBeingSetToPending = false;
        
        if (!StringUtils.equals(requestShmNotification.getStatusCd(), shmNotificationForUpdate.getStatCd())
                && StringUtils.equals(requestShmNotification.getStatusCd(), ShmNotificationStatusCdEnum.PENDING.getCode())) {
            isNotificationBeingSetToPending = true;
        }
        return isNotificationBeingSetToPending;
    }

    private com.xpo.ltl.api.shipment.service.entity.ShmNotification getNotificationToUpdate(
            ShmNotification requestShmNotification, 
            List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> notificationsThisShipment,
            TransactionContext txnContext) throws ValidationException {
        
        com.xpo.ltl.api.shipment.service.entity.ShmNotification shmNotificationForUpdate = 
                ShmNotificationUtil.getNotificationBySeqNbrAndShpInstId(
                        requestShmNotification.getShipmentInstId(), 
                        BasicTransformer.toLong(requestShmNotification.getNotificationSequenceNbr()), 
                        notificationsThisShipment);

        if (null == shmNotificationForUpdate) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.GENERIC_VAL_ERR, txnContext)
                .contextValues("Shipment Notification is not found for shipment instance ID " + BasicTransformer.toString(requestShmNotification.getShipmentInstId())).build();
        }
        return shmNotificationForUpdate;
    }
}
