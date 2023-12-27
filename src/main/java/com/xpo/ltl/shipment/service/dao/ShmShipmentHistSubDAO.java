package com.xpo.ltl.shipment.service.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.shipment.service.entity.ShmShipmentHist;
import com.xpo.ltl.api.shipment.service.entity.ShmShipmentHistPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipmentHistPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmShipmentHist_;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.DAOUtil;

@ApplicationScoped
@LogExecutionTime
public class ShmShipmentHistSubDAO {

    @Inject
    private AppContext appContext;

    public List<ShmShipmentHist> findMostRecentByShpInstIds
            (Collection<Long> shpInstIds,
             boolean dlvryQalfrCdChange,
             EntityManager entityManager) {
        if (CollectionUtils.isEmpty(shpInstIds))
            return Collections.emptyList();

        CriteriaBuilder criteriaBuilder =
            entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmShipmentHist> criteriaQuery =
            criteriaBuilder.createQuery(ShmShipmentHist.class);
        Root<ShmShipmentHist> shmShipmentHistRoot =
            criteriaQuery.from(ShmShipmentHist.class);
        Path<ShmShipmentHistPK> idPath =
            shmShipmentHistRoot.get(ShmShipmentHist_.id);
        Path<Long> shpInstIdPath =
            idPath.get(ShmShipmentHistPK_.shpInstId);

        List<ShmShipmentHist> results =
            DAOUtil.executeQueryWithChunkedInClause
                (entityManager,
                 criteriaQuery,
                 shpInstIdPath,
                 shpInstIds,
                 appContext.getMaxCountForInClause());

        return filterMostRecent(results, dlvryQalfrCdChange);
    }

    public List<ShmShipmentHist> findMostRecentByProNbrs
            (Collection<String> proNbrs,
             boolean dlvryQalfrCdChange,
             EntityManager entityManager) {
        CriteriaBuilder criteriaBuilder =
            entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmShipmentHist> criteriaQuery =
            criteriaBuilder.createQuery(ShmShipmentHist.class);
        Root<ShmShipmentHist> shmShipmentHistRoot =
            criteriaQuery.from(ShmShipmentHist.class);
        Path<String> proNbrTxtPath =
            shmShipmentHistRoot.get(ShmShipmentHist_.proNbrTxt);

        List<ShmShipmentHist> results =
            DAOUtil.executeQueryWithChunkedInClause
                (entityManager,
                 criteriaQuery,
                 proNbrTxtPath,
                 proNbrs,
                 appContext.getMaxCountForInClause());

        return filterMostRecent(results, dlvryQalfrCdChange);
    }

    private List<ShmShipmentHist> filterMostRecent
            (List<ShmShipmentHist> results,
             boolean dlvryQalfrCdChange) {
        if (CollectionUtils.isEmpty(results))
            return Collections.emptyList();

        Map<Long, List<ShmShipmentHist>> reverseOrderShmShipmentHistsByShpInstId =
            results.stream()
                .sorted
                    (Comparator.comparing
                         (shmShipmentHist ->
                              shmShipmentHist.getId().getEffTmst(),
                          Comparator.reverseOrder()))
                .collect
                    (Collectors.groupingBy
                         (shmShipmentHist ->
                              shmShipmentHist.getId().getShpInstId()));

        List<ShmShipmentHist> mostRecentShmShipmentHists = new ArrayList<>();

        for (List<ShmShipmentHist> reverseOrderShmShipmentHists
                 : reverseOrderShmShipmentHistsByShpInstId.values()) {
            if (dlvryQalfrCdChange) {
                ShmShipmentHist previousShmShipmentHist = null;
                for (ShmShipmentHist shmShipmentHist
                         : reverseOrderShmShipmentHists) {
                    if (previousShmShipmentHist != null
                        && !StringUtils.equals
                               (shmShipmentHist.getDlvryQalfrCd(),
                                previousShmShipmentHist.getDlvryQalfrCd())) {
                        mostRecentShmShipmentHists.add(shmShipmentHist);
                        break;
                    }

                    previousShmShipmentHist = shmShipmentHist;
                }
            }
            else {
                ShmShipmentHist shmShipmentHist =
                    reverseOrderShmShipmentHists.iterator().next();
                mostRecentShmShipmentHists.add(shmShipmentHist);
            }
        }

        return mostRecentShmShipmentHists;
    }

}
