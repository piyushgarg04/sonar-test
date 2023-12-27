package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import com.xpo.ltl.api.shipment.service.dao.ShmMgmtRemarkDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark_;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentManagementRemarkTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.ShipmentManagementRemarkTypeCd;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentManagementRemarkSubDAO extends ShmMgmtRemarkDAO<ShmMgmtRemark> {

	private static final String SHM_MGMT_REMARK_QUERY = "select * from SHM_MGMT_REMARK rmk \r\n";
	/**
	 * Find a {@link ShmMgmtRemark} shipment movement exception remark (using all ids) or shipment note (using
	 * shipmentInstId and seqNbr)
	 *
	 * @param shipmentInstId
	 * @param movementSequenceNbr
	 * @param movementExceptionSequenceNbr
	 * @param seqNbr
	 * @param entityManager
	 * @return
	 */
	public ShmMgmtRemark findByIds(
		final Long remarkId,
		final Integer movementSeqNbr,
		final Integer movementExcpSeqNbr,
		final EntityManager entityManager) {
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<ShmMgmtRemark> cq = cb.createQuery(ShmMgmtRemark.class);
		final Root<ShmMgmtRemark> from = cq.from(ShmMgmtRemark.class);

		final Expression<Long> shmMgmtRemarkIdPath = from.get(ShmMgmtRemark_.remarkId);
		final Expression<BigDecimal> shmMgmtRemarkMovementSeqNbrPath = from.get(ShmMgmtRemark_.mvmtSeqNbr);
		final Expression<BigDecimal> shmMgmtRemarkMovementExcpSeqNbrPath = from.get(ShmMgmtRemark_.mvmtExcpSeqNbr);

		final List<Predicate> predicates = new ArrayList<>();
		final Predicate remarkIdPred = cb.equal(shmMgmtRemarkIdPath, remarkId);
		final Predicate mvmtExcpSeqNbrPred = cb.equal(shmMgmtRemarkMovementSeqNbrPath, movementSeqNbr);
		final Predicate mvmtSeqNbrPred = cb.equal(shmMgmtRemarkMovementExcpSeqNbrPath, movementExcpSeqNbr);

		predicates.add(remarkIdPred);
		predicates.add(mvmtExcpSeqNbrPred);
		predicates.add(mvmtSeqNbrPred);

		cq.select(from).where(predicates.toArray(new Predicate[predicates.size()]));

		final List<ShmMgmtRemark> resultRemarks = entityManager.createQuery(cq).getResultList();

		if (CollectionUtils.isNotEmpty(resultRemarks))
			return resultRemarks.get(0);

		return null;
	}

	public List<Object []> listShipmentManagementRemarks(
		final List<Long> shipmentInstIds,
		final List<Long> osdIds,
		final Integer movementSequenceNbr,
		final Integer movementExceptionSequenceNbr,
		final ShipmentManagementRemarkTypeCd typeCd,
		final Boolean showToCustomerInd,
		final EntityManager entityManager) {


		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(SHM_MGMT_REMARK_QUERY);
		if(CollectionUtils.isNotEmpty(shipmentInstIds)){
			queryBuilder.append("where rmk.SHP_INST_ID in :listFK");
		}
		else if(CollectionUtils.isNotEmpty(osdIds)){
			queryBuilder.append("where rmk.OSD_ID in :listFK");
		}
		queryBuilder = getFilterFields(queryBuilder, movementSequenceNbr, movementExceptionSequenceNbr, typeCd, showToCustomerInd);
		final String finishedQuery = queryBuilder.toString();
		Query query = entityManager.createNativeQuery(finishedQuery);
		if(CollectionUtils.isNotEmpty(shipmentInstIds)){
			query.setParameter("listFK", shipmentInstIds);
		}
		else if(CollectionUtils.isNotEmpty(osdIds)){
			query.setParameter("listFK", osdIds);
		}
		List<Object []> resultRemarks = query.getResultList();
		return resultRemarks;

	}

	private StringBuilder getFilterFields(StringBuilder queryBuilder, Integer movementSequenceNbr, Integer movementExceptionSequenceNbr,
			ShipmentManagementRemarkTypeCd typeCd, Boolean showToCustomerInd) {
		
		if(movementSequenceNbr != null){
			queryBuilder.append(String.format(" and rmk.MVMT_SEQ_NBR = %s", movementSequenceNbr));
		}
		if(movementExceptionSequenceNbr != null){
			queryBuilder.append(String.format(" and rmk.MVMT_EXCP_SEQ_NBR = %s", movementExceptionSequenceNbr));
		}
		if(typeCd != null){
			queryBuilder.append(String.format(" and rmk.TYPE_CD = \'%s\'", ShipmentManagementRemarkTypeCdTransformer.toCode(typeCd)));
		}
		if(showToCustomerInd != null){
			queryBuilder.append(String.format(" and rmk.SHOW_TO_CUST_IND = \'%s\'", showToCustomerInd?"Y":"N"));
		}
		return queryBuilder;

	}
}

