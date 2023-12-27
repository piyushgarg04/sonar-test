package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcpPK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CreateShipmentDockExceptionResp;
import com.xpo.ltl.api.shipment.v2.CreateShipmentDockExceptionRqst;
import com.xpo.ltl.api.shipment.v2.XdockException;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmXdockExcpSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import org.apache.commons.lang3.BooleanUtils;

public class CreateShipmentDockExceptionImpl {

	@Inject
	private ExternalRestClient externalRestClient;
	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;
	@Inject
	private ShmXdockExcpSubDAO shmXdockExcpSubDAO;

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	private static final String PGM_ID = "UPDATEHU";

	public CreateShipmentDockExceptionResp createShipmentDockException(
			final CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst,
			final TransactionContext txnContext, EntityManager entityManager)
			throws ServiceException {

		checkNotNull(createShipmentDockExceptionRqst, "The request is required");
		checkNotNull(entityManager, "The entityManager is required");
		checkNotNull(txnContext, "The transanction context is required");
		checkNotNull(createShipmentDockExceptionRqst.getDockException(),
				"The DockException is required");

		if (createShipmentDockExceptionRqst.getParentProNbr() == null
				&& createShipmentDockExceptionRqst.getShipmentInstId() == null) {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext).build();
		}

		XdockException xdockException = createShipmentDockExceptionRqst.getDockException();
		checkNotNull(xdockException.getReportingSicCd(), "The Reporting SIC is required");

		if (externalRestClient.isNotActiveSicAndLinehaul(xdockException.getReportingSicCd(),
				txnContext)) {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("createShipmentDockException",
							"The reportingSicCd must be a valid and active Linehaul Sic")
					.build();
		}

		Long overPiecesCount = xdockException.getOverPiecesCount();
		Long shortPiecesCount = xdockException.getShortPiecesCount();

		if (overPiecesCount > 0 && shortPiecesCount > 0) {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("createShipmentDockException",
							"OverPicesCount and ShortPiecesCount cannot be both > 0")
					.build();
		}

		AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext);
		
		String formatProNumber = createShipmentDockExceptionRqst.getParentProNbr() != null
				? ProNumberHelper.validateProNumber(
						createShipmentDockExceptionRqst.getParentProNbr(), txnContext)
				: null;
		Long shipmentInstId = createShipmentDockExceptionRqst.getShipmentInstId();

		ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(formatProNumber,
				shipmentInstId, entityManager);

		if (shmShipment == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
					.contextValues(formatProNumber,
							(shipmentInstId != null ? shipmentInstId.toString() : null))
					.build();
		}

		long seqNbr = shmXdockExcpSubDAO.getNextSeqNbrByShpInstId(shmShipment.getShpInstId(),
				entityManager);

		if (Boolean.TRUE.equals(xdockException.getAllShrtInd()) && shortPiecesCount == 0) {
			shortPiecesCount = BasicTransformer.toLong(shmShipment.getTotPcsCnt());
		}

		if (xdockException.getDestinationTerminalSicCd() == null) {
			xdockException.setDestinationTerminalSicCd(xdockException.getReportingSicCd());
		}
		if (xdockException.getOriginTerminalSicCd() == null) {
			xdockException.setOriginTerminalSicCd(xdockException.getReportingSicCd());
		}
		if (xdockException.getPoorlyPackagedInd() == null) {
			xdockException.setPoorlyPackagedInd(Boolean.FALSE);
		}

		xdockException.setShortPiecesCount(shortPiecesCount);

		ShmXdockExcp shmXdockExcp = createShmXdockExcp(xdockException, seqNbr,
				shmShipment.getShpInstId(), txnContext, auditInfo);

		shmXdockExcpSubDAO.save(shmXdockExcp, entityManager);
		shmXdockExcpSubDAO.createDB2ShmXdockExcp(shmXdockExcp, db2EntityManager);

		updateShipmentPoorlyPackagedIndicator(xdockException, shmShipment, txnContext, entityManager);

		CreateShipmentDockExceptionResp resp = new CreateShipmentDockExceptionResp();
		xdockException.setSequenceNbr(BigInteger.valueOf(shmXdockExcp.getId().getSeqNbr()));
		xdockException.setShipmentInstId(shmXdockExcp.getId().getShpInstId());
		resp.setDockException(xdockException);

		return resp;
	}

	/**
	 * Set the SHM_SHIPMENT poorlyPackagedInd to true, when 'Y' input, and the input PRO_NBR is for a legacy or parent PRO.
	 *
	 * @param xdockException
	 * @param shmShipment
	 * @param entityManager
	 * @throws ValidationException
	 */
	private void updateShipmentPoorlyPackagedIndicator(XdockException xdockException,
		   ShmShipment shmShipment, TransactionContext transactionContext, EntityManager entityManager) throws ServiceException {

		// Once this is set to true it can't be set back to false by subsequent exceptions
		if (!xdockException.isPoorlyPackagedInd()) {
			return;
		}
		// No need to update if its already flagged
		if (BooleanUtils.toBoolean(shmShipment.getPoorlyPackagedInd())) {
			return;
		}

		shmShipment.setPoorlyPackagedInd(BasicTransformer.toString(Boolean.TRUE));
		shmShipmentSubDAO.save(shmShipment, entityManager);
		shmShipmentSubDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(), transactionContext, db2EntityManager);
	}

	private ShmXdockExcp createShmXdockExcp(XdockException xdockException, long seqNbr,
			long shpInstId, TransactionContext txnContext, AuditInfo auditInfo) throws ServiceException {

		ShmXdockExcp shmXdockExcp = new ShmXdockExcp();

		DtoTransformer.toShmXdockExcp(xdockException, shmXdockExcp);

		ShmXdockExcpPK id = new ShmXdockExcpPK();

		id.setSeqNbr(seqNbr);
		id.setShpInstId(shpInstId);
		shmXdockExcp.setId(id);

		DtoTransformer.setAuditInfo(shmXdockExcp, auditInfo);

		return shmXdockExcp;
	}
}
