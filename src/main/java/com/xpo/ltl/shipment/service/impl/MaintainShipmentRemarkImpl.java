
package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmRemarkPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentRemarkTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CreateShipmentRemarkResp;
import com.xpo.ltl.api.shipment.v2.CreateShipmentRemarkRqst;
import com.xpo.ltl.api.shipment.v2.GetShipmentRemarkResp;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@RequestScoped
public class MaintainShipmentRemarkImpl {

	private static final Log log = LogFactory.getLog(MaintainShipmentRemarkImpl.class);

	@Inject
	private ShipmentRemarkSubDAO shipmentRemarkDAO;

	@Inject
	private ShmShipmentSubDAO shipmentDAO;

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	public CreateShipmentRemarkResp createShipmentRemark(
		final CreateShipmentRemarkRqst rqst,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {

		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(rqst.getShipmentRemark(), "The shipment remark is required.");

		log.info(
			String.format(
				"Creating shipment remark for shipmentInstId %s and typeCd %s",
				rqst.getShipmentRemark().getShipmentInstId(),
				rqst.getShipmentRemark().getTypeCd().value()));

		final AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);

		final ShmShipment shipmentEntity = shipmentDAO
			.findById(rqst.getShipmentRemark().getShipmentInstId(), entityManager);

		if (shipmentEntity == null) {
			throw ExceptionBuilder
				.exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
				.contextValues(
					rqst.getShipmentRemark().getShipmentInstId().toString(),
					rqst.getShipmentRemark().getTypeCd().value())
				.log()
				.build();
		}

		final ShmRemark remarkEntity = createNewRemarkEntity(rqst.getShipmentRemark(), auditInfo, entityManager);
		remarkEntity.setShmShipment(shipmentEntity);
		DtoTransformer.setAuditInfo(remarkEntity, auditInfo);

		final ShmRemarkPK id = new ShmRemarkPK();
		id.setShpInstId(rqst.getShipmentRemark().getShipmentInstId());
		id.setTypCd(ShipmentRemarkTypeCdTransformer.toCode(rqst.getShipmentRemark().getTypeCd()));
		shipmentRemarkDAO.createDB2ShmRemark(remarkEntity, db2EntityManager);
		entityManager.persist(remarkEntity);
		db2EntityManager.flush();
		entityManager.flush();

		log.info(
			String.format(
				"Created new Remark for a shipment with shpInstId %s and typeCd %s.",
				remarkEntity.getId().getShpInstId(),
				remarkEntity.getId().getTypCd()));

		final CreateShipmentRemarkResp resp = new CreateShipmentRemarkResp();
		resp.setShipmentRemark(EntityTransformer.toRemark(remarkEntity));
		return resp;
	}

	private ShmRemark createNewRemarkEntity(
		final Remark shipmentRemark,
		final AuditInfo auditInfo,
		final EntityManager entityManager) throws ServiceException {
		ShmRemark remarkEntity = new ShmRemark();
		final ShmRemarkPK id = new ShmRemarkPK();
		id.setShpInstId(shipmentRemark.getShipmentInstId());
		id.setTypCd(ShipmentRemarkTypeCdTransformer.toCode(shipmentRemark.getTypeCd()));
		remarkEntity.setId(id);
		remarkEntity = DtoTransformer.toShmRemark(shipmentRemark, remarkEntity);
		return remarkEntity;
	}

	public GetShipmentRemarkResp getShipmentRemark(
		final Long shipmentInstId,
		final ShipmentRemarkTypeCd typeCd,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {

		checkNotNull(shipmentInstId, "Shipment Instance Id is required.");
		checkNotNull(typeCd, "Shipment Remark Type code is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		log.info(
			String.format("Retrieving shipment remark for shipmentInstId %s and typeCd %s", shipmentInstId, typeCd));

		final ShmRemarkPK id = new ShmRemarkPK();
		id.setShpInstId(shipmentInstId.longValue());
		id.setTypCd(ShipmentRemarkTypeCdTransformer.toCode(typeCd));
		final Remark shipmentRemark = EntityTransformer.toRemark(shipmentRemarkDAO.findById(id, entityManager));

		if (shipmentRemark == null) {
			throw ExceptionBuilder
				.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
				.log()
				.moreInfo("getShipmentRemark", "remark not found")
				.build();
		}

		log.info(
			String.format("Retrieved shipment remark for shipmentInstId %s and typeCd %s", shipmentInstId, typeCd));

		final GetShipmentRemarkResp resp = new GetShipmentRemarkResp();
		resp.setShipmentRemark(shipmentRemark);

		return resp;
	}

}