package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.dao.ShmOsdHeaderDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BulkCreateShipmentManagementRemarksRqst;
import com.xpo.ltl.api.shipment.v2.CreateShipmentManagementRemarkResp;
import com.xpo.ltl.api.shipment.v2.CreateShipmentManagementRemarkRqst;
import com.xpo.ltl.api.shipment.v2.GetShipmentManagementRemarkResp;
import com.xpo.ltl.api.shipment.v2.ManagementRemark;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentManagementRemarkResp;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentManagementRemarkRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentManagementRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@RequestScoped
public class MaintainShipmentManagementRemarkImpl {
	private static final Log log = LogFactory.getLog(MaintainShipmentManagementRemarkImpl.class);
	private static final String SHM_MGMT_REMARK_SEQ = "SHM_MGMT_REMARK_SEQ";

	@Inject
	private ShipmentManagementRemarkSubDAO shipmentManagementRemarkDAO;

	@Inject
	private ShmShipmentSubDAO shipmentDAO;

    @Inject
    private ShmOsdHdrSubDAO osdDAO;


	/**
	 * Create a shipment movement exception remark or shipment note.
	 *
	 * @param rqst
	 *            The {@link CreateShipmentManagementRemarkRqst} containing the remark to create.
	 * @param txnContext
	 *            The {@link TransactionContext} for the request.
	 * @param entityManager
	 * @return A {@link CreateShipmentManagementRemarkResp} if successful.
	 * @throws NullPointerException
	 *             if {@code CreateShipmentManagementRemarkRqst} is {@code null}.
	 * @throws ServiceException
	 *             if an error occurs while creating the remark, such as missing required fields.
	 */
	public CreateShipmentManagementRemarkResp createShipmentManagementRemark(
		final CreateShipmentManagementRemarkRqst rqst,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {

		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(rqst.getManagementRemark(), "The shipment movement exception remark is required.");
        
		if(null == rqst.getManagementRemark().getParentShipmentInstId()) {
			rqst.getManagementRemark().setParentShipmentInstId(rqst.getManagementRemark().getShipmentInstId());
		}
		
        if(rqst.getManagementRemark().getParentShipmentInstId() != null){
            log.info(
                String.format(
                    "Creating Movement Exception Remark for shipmentInstId %s",
                    rqst.getManagementRemark().getParentShipmentInstId()));
        }
        if(rqst.getManagementRemark().getParentOsdId() != null){
            log.info(
                String.format(
                    "Creating Movement Exception Remark for osdId %s",
                    rqst.getManagementRemark().getParentOsdId()));
        }

		final ManagementRemark managementRemark = rqst.getManagementRemark();

		
		final ShmMgmtRemark resultEntity = createMgmtRemark(managementRemark, null, null, txnContext, entityManager);

		List<Long> shipmentInstIds = null;
		if (null != rqst.getManagementRemark().getParentShipmentInstId()) {
			shipmentInstIds = new ArrayList<Long>();
			shipmentInstIds.add(rqst.getManagementRemark().getParentShipmentInstId());
		}

		List<Long> osdIds = null;
		if (null != rqst.getManagementRemark().getParentOsdId()) {
			osdIds = new ArrayList<Long>();
			osdIds.add(rqst.getManagementRemark().getParentOsdId());
		}
		
		final List<Object[]> mgmtRemarks = shipmentManagementRemarkDAO.listShipmentManagementRemarks(shipmentInstIds,
				osdIds, null, null, null, null, entityManager);
		Integer sequenceNumber = 1;
		if (CollectionUtils.isNotEmpty(mgmtRemarks)) {
			sequenceNumber = mgmtRemarks.size();
		}
		
		final CreateShipmentManagementRemarkResp resp = new CreateShipmentManagementRemarkResp();
		resp.setManagementRemark(EntityTransformer.toManagementRemark(resultEntity));
		
		if (null != resp.getManagementRemark()) {
			resp.getManagementRemark().setShipmentInstId(rqst.getManagementRemark().getParentShipmentInstId());
			resp.getManagementRemark().setParentShipmentInstId(rqst.getManagementRemark().getParentShipmentInstId());
			resp.getManagementRemark().setSequenceNbr(BasicTransformer.toBigInteger(sequenceNumber));
		}	
		
		return resp;
	}

	/**
	 * Create a bulk of shipment movement exception remarks or shipment notes.
	 *
	 * @param rqst
	 *            The {@link BulkCreateShipmentManagementRemarksRqst} containing the remark to create.
	 * @param txnContext
	 *            The {@link TransactionContext} for the request.
	 * @param entityManager
	 * @throws ServiceException
	 *             if an error occurs while creating the remark, such as missing required fields.
	 */
	public void bulkCreateShipmentManagementRemarks(
		final BulkCreateShipmentManagementRemarksRqst rqst,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {
		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(rqst.getManagementRemarks(), "The managementRemarks is required.");

		List<ManagementRemark> managementRemarksRqst = rqst.getManagementRemarks();
		
		if (CollectionUtils.isEmpty(managementRemarksRqst)) {
			throw ExceptionBuilder
				.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
				.moreInfo("bulkCreateShipmentManagementRemarks", "managementRemarks cannot be empty.")
				.log()
				.build();
		}
		
		CollectionUtils.emptyIfNull(managementRemarksRqst).stream()
				.filter(MgmtRemark -> MgmtRemark.getParentShipmentInstId() == null)
				.forEach(MgmtRemark -> MgmtRemark.setParentShipmentInstId(MgmtRemark.getShipmentInstId()));

		log.info(
			String.format(
				"Started bulk creating Shipment Management Remarks"));
		
        Map<Long, List<ManagementRemark>> managementRemarksByShipInstId = managementRemarksRqst.stream()
                                                                            .filter(request -> request.getParentShipmentInstId() != null)
                                                                            .collect(Collectors.groupingBy(ManagementRemark::getParentShipmentInstId));

        Map<Long, List<ManagementRemark>> managementRemarksByOsdId = managementRemarksRqst.stream()
                                                                    .filter(request -> request.getParentOsdId() != null)
                                                                    .collect(Collectors.groupingBy(ManagementRemark::getParentOsdId));
        for(Long shpInstId: managementRemarksByShipInstId.keySet()){
			ShmShipment shipmentEntity = shipmentDAO
					.findById(shpInstId, entityManager);
			List<ManagementRemark> managementRemarks = managementRemarksByShipInstId.get(shpInstId);
			for (ManagementRemark managementRemark : managementRemarks) {
				createMgmtRemark(managementRemark, shipmentEntity, null, txnContext, entityManager);
			}
		}

		for(Long osdId: managementRemarksByOsdId.keySet()){
			ShmOsdHeader osdEntity = osdDAO.findById(osdId, entityManager);
			List<ManagementRemark> managementRemarks = managementRemarksByOsdId.get(osdId);
			for (ManagementRemark managementRemark : managementRemarks) {
				createMgmtRemark(managementRemark, null, osdEntity, txnContext, entityManager);
			}
		}
		
		log.info(
			String.format(
				"Finished bulk creating Shipment Management Remarks"));		
		return;
	}

	/**
	 * Main method to be used for creating singular management remark
	 * @param rqst
	 * @param txnContext
	 * @param entityManager
	 * @param managementRemark
	 * @param shipmentEntity 
	 * @return
	 * @throws NotFoundException
	 * @throws ServiceException
	 * @throws ValidationException
	 */
	public ShmMgmtRemark createMgmtRemark(
		final ManagementRemark managementRemark,
		ShmShipment shipmentEntity,
		ShmOsdHeader osdEntity,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws NotFoundException, ServiceException, ValidationException {
        

        ShmMgmtRemark remarkEntity = new ShmMgmtRemark();
        
		final AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		
    
        if(managementRemark.getParentShipmentInstId() != null){
			if(shipmentEntity == null)
				shipmentEntity = shipmentDAO.findById(managementRemark.getParentShipmentInstId(), entityManager);

            if (shipmentEntity == null) {
                throw ExceptionBuilder
                    .exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
                    .contextValues(managementRemark.getParentShipmentInstId().toString())
                    .log()
                    .build();
            }

            remarkEntity.setShmShipment(shipmentEntity);
        }
        if(managementRemark.getParentOsdId() != null){
			if(osdEntity == null)
            	osdEntity = osdDAO.findById(managementRemark.getParentOsdId(), entityManager);

            if(osdEntity == null){
                throw ExceptionBuilder
                    .exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
                    .contextValues(managementRemark.getParentOsdId().toString())
                    .log()
                    .build();
            }
            remarkEntity.setShmOsdHeader(osdEntity);
        }

		DtoTransformer.setAuditInfo(remarkEntity, auditInfo);
		remarkEntity = DtoTransformer.toShmMgmtRemark(managementRemark, remarkEntity);
		remarkEntity.setRecordVersionNbr(0L);
		shipmentManagementRemarkDAO.setRemarkId(remarkEntity, SHM_MGMT_REMARK_SEQ, entityManager);
		final ShmMgmtRemark resultEntity = shipmentManagementRemarkDAO.save(remarkEntity, entityManager);
		return resultEntity;
	}

	/**
	 * Update a shipment movement exception remark.
	 *
	 * @param rqst
	 *            The {@link UpdateShipmentManagementRemarkRqst} containing the remark to update.
	 * @param txnContext
	 *            The {@link TransactionContext} for the request.
	 * @param entityManager
	 * @return A {@link CreateClaimResp} if successful.
	 * @throws NullPointerException
	 *             if {@code UpdateShipmentManagementRemarkRqst} is {@code null}.
	 * @throws ServiceException
	 *             if an error occurs while updating the remark, such as missing required fields.
	 */
	public UpdateShipmentManagementRemarkResp updateShipmentManagementRemark(
		final UpdateShipmentManagementRemarkRqst rqst,
		final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {

		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(
			rqst.getManagementRemark(),
			"The shipment movement exception remark is required.");

		log.info(String.format("Updating shipment movement exception remark for remarkId %s",
				rqst.getManagementRemark().getRemarkId()));

		final AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);

		final ManagementRemark updateManagementRemark = rqst.getManagementRemark();

		ShmMgmtRemark remarkEntity = shipmentManagementRemarkDAO.findByIds(
			updateManagementRemark.getRemarkId(),
			updateManagementRemark.getMovementSequenceNbr().intValue(),
			updateManagementRemark.getMovementExceptionSequenceNbr().intValue(),
			entityManager);

		if (remarkEntity == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.REMARK_NF, txnContext).log().build();
		}

		remarkEntity = DtoTransformer.toShmMgmtRemark(updateManagementRemark, remarkEntity);
		DtoTransformer.setLstUpdateAuditInfo(remarkEntity, auditInfo);

		final ShmMgmtRemark resultEntity = shipmentManagementRemarkDAO.save(remarkEntity, entityManager);

		entityManager.flush();

		final UpdateShipmentManagementRemarkResp resp = new UpdateShipmentManagementRemarkResp();
		resp.setManagementRemark(EntityTransformer.toManagementRemark(resultEntity));

		return resp;
	}

	/**
	 * Retrieve management remark by provided identifiers
	 * @param remarkId
	 * @param movementSequenceNbr
	 * @param movementExceptionSequenceNbr
	 * @param txnContext
	 * @param entityManager
	 * @return
	 * @throws ServiceException
	 */
	public GetShipmentManagementRemarkResp getShipmentManagementRemark(

		final Long remarkId,
		final Integer movementSequenceNbr,
		final Integer movementExceptionSequenceNbr,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {

		checkNotNull(remarkId, "The remark ID is required.");
		checkNotNull(movementSequenceNbr, "The movement sequence number is required.");
		checkNotNull(movementExceptionSequenceNbr, "The movement exception sequence number is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		log.info(
			String.format(
				"Retrieving shipment movement exception remark for remarkId %s",
				remarkId));

		final ShmMgmtRemark shmMgmtRemark = shipmentManagementRemarkDAO
			.findByIds(remarkId, movementSequenceNbr, movementExceptionSequenceNbr, entityManager);

		if (shmMgmtRemark == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.REMARK_NF, txnContext).log().build();
		}

		final GetShipmentManagementRemarkResp resp = new GetShipmentManagementRemarkResp();
		resp.setManagementRemark(EntityTransformer.toManagementRemark(shmMgmtRemark));

		return resp;
	}

	/**
	 * Delete shipment inst id by provided identifiers
	 * @param remarkId
	 * @param movementSequenceNbr
	 * @param movementExceptionSequenceNbr
	 * @param txnContext
	 * @param entityManager
	 * @throws NotFoundException
	 */
	public void deleteShipmentManagementRemark(
		final Long remarkId,
		final Integer movementSequenceNbr,
		final Integer movementExceptionSequenceNbr,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws NotFoundException {

		checkNotNull(remarkId, "The remark ID is required.");
		checkNotNull(movementSequenceNbr, "The movement sequence number is required.");
		checkNotNull(movementExceptionSequenceNbr, "The movement exception sequence number is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		log.info(String.format("Deleting shipment movement exception remark for remarkId %s", remarkId));

		final ShmMgmtRemark shmMgmtRemark = shipmentManagementRemarkDAO
			.findByIds(remarkId, movementSequenceNbr, movementExceptionSequenceNbr, entityManager);

		if (shmMgmtRemark == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.REMARK_NF, txnContext).log().build();
		}

		shipmentManagementRemarkDAO.remove(shmMgmtRemark, entityManager);

		entityManager.flush();
	}
}
