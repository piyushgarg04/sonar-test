package com.xpo.ltl.shipment.service.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.service.dao.DmtShmShiplifyDAO;
import com.xpo.ltl.api.shipment.service.entity.DmtShmShiplify;
import com.xpo.ltl.api.shipment.service.entity.DmtShmShiplify_;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class DmtShmShiplifySubDAO extends DmtShmShiplifyDAO<DmtShmShiplify>{

    private static final String UPDATE_SHIPLIFY_QUERY = "update DMT_SHM_SHIPLIFY shp set shp.PROCESSED_IND = 'Y' where shp.SHP_INST_ID in :processedSet \r\n";

    public List<DmtShmShiplify> listUnprocessedshipments(
		final EntityManager entityManager) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DmtShmShiplify> cq = cb.createQuery(DmtShmShiplify.class);
        Root<DmtShmShiplify> from = cq.from(DmtShmShiplify.class);
        final Path<String> processedIndPath = from.get(DmtShmShiplify_.processedInd);
        // final Path<String> consigneeDockAccess = from.get(DmtShmShiplify_.consigneeDockAccessInd);
        // final Path<String> consigneeDockAccessCnfdnc = from.get(DmtShmShiplify_.consigneeDockAccessCnfdnc);
        // final Path<String> consigneeForkliftAccess = from.get(DmtShmShiplify_.consigneeForkliftInd);
        // final Path<String> consigneeForkliftAccessCnfdnc = from.get(DmtShmShiplify_.consigneeForkliftCnfdnc); 
		List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(processedIndPath, "N"));
        // predicates.add(cb.equal(consigneeForkliftAccess, "N"));
        // predicates.add(cb.equal(consigneeForkliftAccessCnfdnc, "A1"));
        // predicates.add(cb.equal(consigneeDockAccess, "N"));
        // predicates.add(cb.equal(consigneeDockAccessCnfdnc, "A1"));
		cq.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<DmtShmShiplify> resultRemarks = entityManager.createQuery(cq).setMaxResults(10000).getResultList();
		return resultRemarks;
	}

	public void updateProcessedInd(final Set<Long> processedSet, final EntityManager entityManager) {

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(UPDATE_SHIPLIFY_QUERY);
		final String finishedQuery = queryBuilder.toString();
		Query query = entityManager.createNativeQuery(finishedQuery);
		List<Long> processedList = new ArrayList<Long>(processedSet);
		query.setParameter("processedSet", processedList);
		query.executeUpdate();
	}
}
