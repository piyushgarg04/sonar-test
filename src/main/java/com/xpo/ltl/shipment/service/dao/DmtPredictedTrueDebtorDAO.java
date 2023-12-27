package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.service.dao.DmtTrueDebtorPredictDAO;
import com.xpo.ltl.api.shipment.service.entity.DmtTrueDebtorPredict;
import com.xpo.ltl.api.shipment.service.entity.DmtTrueDebtorPredictPK_;
import com.xpo.ltl.api.shipment.service.entity.DmtTrueDebtorPredict_;

@ApplicationScoped
public class DmtPredictedTrueDebtorDAO extends DmtTrueDebtorPredictDAO<DmtTrueDebtorPredict> {


	public DmtTrueDebtorPredict getPredictedDebror(long shipperCustomerId, String consigneeZipCd,EntityManager entityManager,BigDecimal confidenceLevelPct,BigDecimal totalShipmentCount)
	{
		DmtTrueDebtorPredict dmtTrueDebtorPredict = null;
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<DmtTrueDebtorPredict> cq = cb.createQuery(DmtTrueDebtorPredict.class);
		final Root<	DmtTrueDebtorPredict> from = cq.from(DmtTrueDebtorPredict.class);

		final Expression<Long> shipperCustomerIdPath = from.get(DmtTrueDebtorPredict_.id).get(DmtTrueDebtorPredictPK_.shipperCustId);
		final Expression<BigDecimal> confidenceLevelPctPath = from.get(DmtTrueDebtorPredict_.confidenceLevelPct);
		final Expression<BigDecimal> totalShipmentCountPath = from.get(DmtTrueDebtorPredict_.totShipmentCnt);


		final List<Predicate> predicates = new ArrayList<>();
		final Predicate shipperCustomerIdPred = cb.equal(shipperCustomerIdPath, shipperCustomerId);
		final Predicate confidenceLevelPctPred = cb.greaterThanOrEqualTo(confidenceLevelPctPath, confidenceLevelPct);
		final Predicate totalShipmentCountPred = cb.greaterThanOrEqualTo(totalShipmentCountPath, totalShipmentCount);


		predicates.add(shipperCustomerIdPred);
		predicates.add(confidenceLevelPctPred);
		predicates.add(totalShipmentCountPred);


		cq.select(from).where(predicates.toArray(new Predicate[predicates.size()]));

		cq.orderBy(cb.desc(from.get(DmtTrueDebtorPredict_.confidenceLevelPct)),cb.desc(from.get(DmtTrueDebtorPredict_.totShipmentCnt)));
		
		final List<DmtTrueDebtorPredict> resp = entityManager.createQuery(cq).getResultList();

		if (CollectionUtils.isNotEmpty(resp)) {
		
			if(consigneeZipCd == null) {
				return resp.get(0);}
			/*Iterate to get the record that matches the consignee zip code from the request */
			for(DmtTrueDebtorPredict predictRec: resp)
			{
				if(predictRec.getId().getConsigneeZipCd() != null && predictRec.getId().getConsigneeZipCd().trim().equalsIgnoreCase(consigneeZipCd)) {
						dmtTrueDebtorPredict =predictRec;
					}
			}
			/*if no record matches the consignee, return the first record from the list */
			if(dmtTrueDebtorPredict == null)
			{
				dmtTrueDebtorPredict = resp.get(0);
			}
			
		}	
		return dmtTrueDebtorPredict;
	}
}


