package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.infrastructure.client.v2.InfrastructureClient;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequest;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequestNote;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.SalvageRequestStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestNoteResp;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestNoteRqst;
import com.xpo.ltl.api.shipment.v2.SalvageRequest;
import com.xpo.ltl.api.shipment.v2.SalvageRequestNote;
import com.xpo.ltl.api.shipment.v2.SalvageRequestStatusCd;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestNoteSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@ApplicationScoped
@LogExecutionTime
public class CreateSalvageRequestNoteImpl {

	@Inject
	ShmSalvageRequestNoteSubDAO shmSalvageRequestNoteSubDAO;
	
	@Inject
	ShmSalvageRequestSubDAO shmSalvageRequestSubDAO; 

	@Inject
	private ExternalRestClient externalRestClient;

	@Inject
	private AppContext appContext;
	
    private static final Log logger = LogFactory.getLog(CreateSalvageRequestNoteImpl.class);

	private static final String EMAIL_FROM = "overage@xpo.com";
	private static final String EMAIL_FROM_NAME = "Overage";
	private static final String EMAIL_SUBJECT = "[OS&D] : Status update for Salvage ID:  ";

	public CreateSalvageRequestNoteResp createSalvageRequestNote(
			CreateSalvageRequestNoteRqst createSalvageRequestNoteRqst, final TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		checkNotNull(createSalvageRequestNoteRqst, "CreateSalvageRequestNoteRqst is required");
		checkNotNull(createSalvageRequestNoteRqst.getSalvageRequestNote(), "SalvageRequestNote is required");

		CreateSalvageRequestNoteResp createSalvageRequestNoteResp = new CreateSalvageRequestNoteResp();

		Long seqNbr = shmSalvageRequestNoteSubDAO.getNextSeqNbrForSalvageRequestNote(
				createSalvageRequestNoteRqst.getSalvageRequestNote().getSalvageRequestId(), entityManager);
		createSalvageRequestNoteRqst.getSalvageRequestNote().setSequenceNbr(BigInteger.valueOf(seqNbr));
		
		ShmSalvageRequestNote shmSalvageRequestNote = new ShmSalvageRequestNote();
		DtoTransformer.setAuditInfo(shmSalvageRequestNote, getAuditInfo(txnContext));
		shmSalvageRequestNote = DtoTransformer.toShmSalvageRequestNote(
				createSalvageRequestNoteRqst.getSalvageRequestNote(), shmSalvageRequestNote);

		shmSalvageRequestNoteSubDAO.createShmSalvageRequestNote(shmSalvageRequestNote, entityManager);
		SalvageRequest salvageRequest = new SalvageRequest();

		if(createSalvageRequestNoteRqst.getSalvageRequestNote().getNoteTypeCd().equals("REQUESTOR")) {
			ShmSalvageRequest shmSalvageRequest =  shmSalvageRequestSubDAO.findById(createSalvageRequestNoteRqst.getSalvageRequestNote().getSalvageRequestId(), entityManager);
			shmSalvageRequest.setStatusCd(SalvageRequestStatusCdTransformer.toCode(SalvageRequestStatusCd.FEEDBACK_SUBMITTED));
			shmSalvageRequestSubDAO.save(shmSalvageRequest, entityManager);
			salvageRequest = EntityTransformer.toSalvageRequest(shmSalvageRequest);
			sendStatusUpdateMail(txnContext, shmSalvageRequest, createSalvageRequestNoteRqst.getSalvageRequestNote());
		}
		List<SalvageRequestNote> salvageRequestNotes = new ArrayList<>();
		salvageRequestNotes.add(createSalvageRequestNoteRqst.getSalvageRequestNote());
		salvageRequest.setSalvageRequestNote(salvageRequestNotes);
		createSalvageRequestNoteResp.setSalvageRequest(salvageRequest);
		return createSalvageRequestNoteResp;
	}

	private static AuditInfo getAuditInfo(final TransactionContext txnContext) {
		AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		return auditInfo;
	}
	
	private void sendStatusUpdateMail(TransactionContext txnContext,
			ShmSalvageRequest shmSalvageRequest, 
			SalvageRequestNote salvageRequestNote) {
		
		try {
			
			InfrastructureClient infrastructureClient = externalRestClient.getInfrastructureClient(txnContext);
			infrastructureClient.sendEmail(null, 
					null, 
					EMAIL_FROM, 
					EMAIL_FROM_NAME, 
					salvageRequestNote.getNote(),
					null, 
					EMAIL_SUBJECT + String.valueOf(shmSalvageRequest.getSalvageRequestId()) ,
					appContext.getSalvageToEmail(),
					null);
		} catch (com.xpo.ltl.api.client.exception.ServiceException | ServiceException e1) {
            logger.warn(e1.getMessage());		
           }
	}

}
