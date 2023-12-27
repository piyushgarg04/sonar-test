package com.xpo.ltl.shipment.service.dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ProFrtBillIndexDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ProFrtBillIndex_;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillIndex_;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ProFrtBillIndexSubDAO extends ProFrtBillIndexDAO<ProFrtBillIndex>{


	/**
	 * Updates the DB2 entity with values from Oracle entity.
	 *
	 * @param shmShipment
	 * @param exadataLstUpdtTmst
	 * @param txnContext
	 * @param db2EntityManager
	 * @throws ServiceException
	 */
	public void updateDB2ProNbrFrtBillIndex(final ProFrtBillIndex proFrtBillIndex, final Timestamp exadataLstUpdtTmst,
		final TransactionContext txnContext, final EntityManager db2EntityManager) throws ServiceException {
		final Function<DB2ProFrtBillIndex, Boolean> checkVersionFunction = getCheckVersionFunction(exadataLstUpdtTmst);

		updateDB2ProFrtBillIndex(proFrtBillIndex, checkVersionFunction, db2EntityManager, txnContext);

	}

    // specific case used only for update shipment skeketon.
    public void updateDB2ProNbrFrtBillIndexForUpdSkeleton(ProFrtBillIndex proFrtBillIndex, EntityManager db2EntityManager) {

        CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        CriteriaUpdate<DB2ProFrtBillIndex> criteriaUpdate = cb.createCriteriaUpdate(DB2ProFrtBillIndex.class);
        Root<DB2ProFrtBillIndex> root = criteriaUpdate.from(DB2ProFrtBillIndex.class);

        criteriaUpdate.set(root.get(DB2ProFrtBillIndex_.shpInstId), proFrtBillIndex.getShpInstId());
        criteriaUpdate.set(root.get(DB2ProFrtBillIndex_.statCd), proFrtBillIndex.getStatCd());
        criteriaUpdate.set(root.get(DB2ProFrtBillIndex_.mvmtUnitSeqNbr), BasicTransformer.toShort(proFrtBillIndex.getMvmtUnitSeqNbr()));
        criteriaUpdate.set(root.get(DB2ProFrtBillIndex_.proPfxOvrdInd), proFrtBillIndex.getProPfxOvrdInd());
        criteriaUpdate.set(root.get(DB2ProFrtBillIndex_.billSicCd), proFrtBillIndex.getBillSicCd());
        criteriaUpdate.set(root.get(DB2ProFrtBillIndex_.lstUpdtTranCd), proFrtBillIndex.getLstUpdtTranCd());
        criteriaUpdate.set(root.get(DB2ProFrtBillIndex_.lstUpdtTmst), proFrtBillIndex.getLstUpdtTmst());

        Path<String> proNbrTxtPath = root.get(DB2ProFrtBillIndex_.proNbrTxt);
        criteriaUpdate.where(cb.equal(proNbrTxtPath, proFrtBillIndex.getProNbrTxt()));

        db2EntityManager.createQuery(criteriaUpdate).executeUpdate();

    }

	private Function<DB2ProFrtBillIndex, Boolean> getCheckVersionFunction(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

    /**
     * <b> DO NOT send more than 1000 proNbrs</b>
     * <i>In case you need to send more that 1k proNumbers, we recommend to improve this query using parallelStream for
     * each query with less
     * than 1k pros and then aggregate/merge the result.</i>
     *
     * @param proNbrList
     * @param entityManager
     * @return
     */
    public List<ProFrtBillIndex> findAllByProNbrList(List<String> proNbrList, EntityManager entityManager) {

        if (CollectionUtils.isEmpty(proNbrList)) {
            return new ArrayList<>();
        }

        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ProFrtBillIndex> query = builder.createQuery(ProFrtBillIndex.class);
        final Root<ProFrtBillIndex> rootEntity = query.from(ProFrtBillIndex.class);

        query.select(rootEntity).where(rootEntity.get(ProFrtBillIndex_.proNbrTxt).in(proNbrList));

        final List<ProFrtBillIndex> result = getResultList(query, entityManager);

        return result;
    }

    @Override
    public DB2ProFrtBillIndex findDb2ById(@NotNull final String proNbrTxt, @NotNull final EntityManager entityManager) {
        return super.findDb2ById(proNbrTxt, entityManager);
    }

}
