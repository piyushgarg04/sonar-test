package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.ListMetadata;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmEventReference;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.ListEventReferencesResp;
import com.xpo.ltl.api.shipment.v2.ListInfo;
import com.xpo.ltl.shipment.service.dao.ShmEventReferenceSubDAO;

@RequestScoped
public class ListEventRefrencesImpl {

	@Inject
	private ShmEventReferenceSubDAO shmEventReferenceSubDAO;
	
	/**
	 * Method to lists the Event References for Shipment.
	 * 
	 * @param listMetadata
	 * @param txnContext
	 * @param entityManager
	 * @return list of eventReference
	 * @throws ServiceException
	 */
	public ListEventReferencesResp listEventReferences(
		final ListMetadata listMetadata,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {
	    
        checkNotNull(txnContext, "Transaction Context is required");
        checkNotNull(entityManager, "EntityManager is required");

		Integer start = 0;
		Integer totalRows = 50;
		if (listMetadata != null) {
			if (listMetadata.getStartAt() != null) {
				start = Math.max(0, listMetadata.getStartAt().intValue());
			}
			if (listMetadata.getNumberOfRows() != null) {
				totalRows = Math.min(50, listMetadata.getNumberOfRows().intValue());
			}
		}

		List<ShmEventReference> entities = shmEventReferenceSubDAO.listEventReferences(totalRows, start, entityManager);

        if (entities == null) {
            throw ExceptionBuilder
            .exception(NotFoundErrorMessage.SHP_EVENT_REFERENCE_NF, txnContext)
            .build();
        }
        
		ListEventReferencesResp eventReferencesResp = new ListEventReferencesResp();
		ListInfo listInfo = new ListInfo();
		listInfo.setStartAt(BigInteger.valueOf(start));
		listInfo.setNumberOfRows(BigInteger.valueOf(entities.size()));
		listInfo.setTotalRowCount(BigInteger.valueOf(entities.size()));
		eventReferencesResp.setListInfo(listInfo);
		
		eventReferencesResp.setEventReferences(EntityTransformer.toEventReference(entities));

		return eventReferencesResp;
	}
}
