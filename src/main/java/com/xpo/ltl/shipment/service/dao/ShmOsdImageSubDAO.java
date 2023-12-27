package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import com.xpo.ltl.api.shipment.service.dao.ShmOsdImageDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader_;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage_;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequest_;


@Dependent
public class ShmOsdImageSubDAO extends ShmOsdImageDAO<ShmOsdImage> {


	/**
	 * Method to find Osd Image record by Pro Number
	 * @param proNumber
	 * @param entityManager
	 * @return
	 */
	public ShmOsdImage findByProNumber(@NotNull final String proNumber, @NotNull final EntityManager entityManager) {
		checkNotNull(proNumber, "proNumber is required");
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmOsdImage> cq = cb.createQuery(ShmOsdImage.class);
		Root<ShmOsdImage> from = cq.from(ShmOsdImage.class);

		Predicate proNumberPredicate = cb.equal(from.get(ShmOsdImage_.proNbrTxt),
				proNumber);

		cq.select(from).where(proNumberPredicate);

		return getSingleResultOrNull(cq, entityManager);
	}

	/**
	 * Method to find Osd Image by Inst Id
	 * @param instId
	 * @param entityManager
	 * @return
	 */
	public List<ShmOsdImage> findByOsdId(@NotNull final Long osdId, @NotNull final EntityManager entityManager) {
		checkNotNull(osdId, "`osdRequestId is required");
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmOsdImage> cq = cb.createQuery(ShmOsdImage.class);
		Root<ShmOsdImage> from = cq.from(ShmOsdImage.class);
		Predicate osdIdPredicate = cb.equal(from.get(ShmOsdImage_.shmOsdHeader).get(ShmOsdHeader_.osdId), osdId);

		cq.select(from).where(osdIdPredicate);

		return getResultList(cq, entityManager);
	}

	public long getNextSequence(String seqName, EntityManager em) {
		return getNextSeq(seqName, em);
	}
	
	public List<ShmOsdImage> getBySalvageRequestIds(@NotNull final List <Long> salvageRequestIds, @NotNull final EntityManager entityManager) {
		checkNotNull(salvageRequestIds, "salvageRequestId is required");
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmOsdImage> cq = cb.createQuery(ShmOsdImage.class);
		Root<ShmOsdImage> from = cq.from(ShmOsdImage.class);
		Predicate proNumberPredicate = from.get(ShmOsdImage_.shmSalvageRequest).get(ShmSalvageRequest_.salvageRequestId).in(salvageRequestIds);

		cq.select(from).where(proNumberPredicate);

		return getResultList(cq, entityManager);
	}
	
	public List<ShmOsdImage> getBySalvageRequestId(@NotNull final Long salvageRequestId, @NotNull final EntityManager entityManager) {
		checkNotNull(salvageRequestId, "`salvageRequestId is required");
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmOsdImage> cq = cb.createQuery(ShmOsdImage.class);
		Root<ShmOsdImage> from = cq.from(ShmOsdImage.class);
		Predicate proNumberPredicate = cb.equal(from.get(ShmOsdImage_.shmSalvageRequest).get(ShmSalvageRequest_.salvageRequestId), salvageRequestId);

		cq.select(from).where(proNumberPredicate);

		return getResultList(cq, entityManager);
	}
}