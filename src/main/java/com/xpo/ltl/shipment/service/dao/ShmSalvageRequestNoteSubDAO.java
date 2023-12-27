package com.xpo.ltl.shipment.service.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections.CollectionUtils;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.service.dao.ShmSalvageRequestNoteDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequestNote;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequestNotePK_;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequestNote_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmSalvageRequestNoteSubDAO extends ShmSalvageRequestNoteDAO<ShmSalvageRequestNote> {

	public void createShmSalvageRequestNote(final ShmSalvageRequestNote shmSalvageRequestNote,
			final EntityManager entityManager) throws ValidationException {
		persist(shmSalvageRequestNote, entityManager);
	}

	public Long getNextSeqNbrForSalvageRequestNote(final Long salvageRequestId, final EntityManager entityManager) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Long> query = cb.createQuery(Long.class);
		final Root<ShmSalvageRequestNote> from = query.from(ShmSalvageRequestNote.class);
		final Predicate predicate = cb.equal(
				from.get(ShmSalvageRequestNote_.id).get(ShmSalvageRequestNotePK_.salvageRequestId), salvageRequestId);
		query.select(cb.max(from.get(ShmSalvageRequestNote_.id).get(ShmSalvageRequestNotePK_.seqNbr))).where(predicate);
		Long result = entityManager.createQuery(query).getSingleResult();
		result = (result == null ? 1 : ++result);
		return result;
	}

	public List<ShmSalvageRequestNote> getShmSalvageReqNotesBySalvageReqId(Long salvageRequestId,
			final EntityManager entityManager) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmSalvageRequestNote> query = criteriaBuilder.createQuery(ShmSalvageRequestNote.class);
		Root<ShmSalvageRequestNote> from = query.from(ShmSalvageRequestNote.class);

		Path<Long> salvageRequestIdPath = from.get(ShmSalvageRequestNote_.id)
				.get(ShmSalvageRequestNotePK_.salvageRequestId);

		query.select(from).where(criteriaBuilder.equal(salvageRequestIdPath, salvageRequestId))
				.orderBy(criteriaBuilder.asc(salvageRequestIdPath));

		List<ShmSalvageRequestNote> shmSalvageRequestNotesList = entityManager.createQuery(query).getResultList();
		return CollectionUtils.isEmpty(shmSalvageRequestNotesList) ? null : shmSalvageRequestNotesList;
	}
}
