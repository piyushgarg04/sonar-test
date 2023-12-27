package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequestNote;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.ListSalvageRequestNotesResp;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestNoteSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ListSalvageRequestNoteImpl {

	@Inject
	ShmSalvageRequestNoteSubDAO shmSalvageRequestNoteSubDAO;

	public ListSalvageRequestNotesResp listSalvageRequestNotes(Long salvageRequestId, TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		checkNotNull(salvageRequestId, "SalvageRequestId is required");

		ListSalvageRequestNotesResp listSalvageRequestNotesResp = new ListSalvageRequestNotesResp();

		List<ShmSalvageRequestNote> shmSalvageRequestNotesList = shmSalvageRequestNoteSubDAO
				.getShmSalvageReqNotesBySalvageReqId(salvageRequestId, entityManager);

		if (CollectionUtils.isNotEmpty(shmSalvageRequestNotesList)) {
			listSalvageRequestNotesResp
					.setSalvageRequestNotes(EntityTransformer.toSalvageRequestNote(shmSalvageRequestNotesList));
		}
		return listSalvageRequestNotesResp;
	}
}
