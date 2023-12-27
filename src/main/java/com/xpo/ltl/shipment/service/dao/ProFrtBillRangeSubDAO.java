package com.xpo.ltl.shipment.service.dao;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.service.dao.ProFrtBillRangeDAO;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillRange;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillRange_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ProFrtBillRangeSubDAO extends ProFrtBillRangeDAO<ProFrtBillRange> {


	public ProFrtBillRange findByPfxAndSfx(String pfx, String sfx, String end, EntityManager entityManager) {

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ProFrtBillRange> query = criteriaBuilder.createQuery(ProFrtBillRange.class);

		Root<ProFrtBillRange> from = query.from(ProFrtBillRange.class);
		Path<String> proPfxTxtPath = from.get(ProFrtBillRange_.proPfxTxt);
		Path<String> proSfxStrtPath = from.get(ProFrtBillRange_.proSfxStrt);
		Path<String> proSfxEndPath = from.get(ProFrtBillRange_.proSfxEnd);

		List<Predicate> predicates = new ArrayList<>();
		predicates.add(criteriaBuilder.equal(proPfxTxtPath, pfx));
		predicates.add(criteriaBuilder.lessThanOrEqualTo(proSfxStrtPath, sfx));
		predicates.add(criteriaBuilder.greaterThanOrEqualTo(proSfxEndPath, end));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));

		List<ProFrtBillRange> result = entityManager.createQuery(query).getResultList();
		return CollectionUtils.isNotEmpty(result) ? result.get(0) : null;
	}
	
	public ProFrtBillRange findByPfxTxt(String pfx, EntityManager entityManager) {

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ProFrtBillRange> query = criteriaBuilder.createQuery(ProFrtBillRange.class);

		Root<ProFrtBillRange> from = query.from(ProFrtBillRange.class);
		Path<String> proPfxTxtPath = from.get(ProFrtBillRange_.proPfxTxt);

		List<Predicate> predicates = new ArrayList<>();
		predicates.add(criteriaBuilder.equal(proPfxTxtPath, pfx));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));

		List<ProFrtBillRange> result = entityManager.createQuery(query).getResultList();
		return CollectionUtils.isEmpty(result) ? null : result.get(0);
	}

}
