package com.xpo.ltl.shipment.service.dao;


import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;

import com.xpo.ltl.api.shipment.service.dao.ShmSalvageRqstDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRqst;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmSalvageRqstSubDAO extends ShmSalvageRqstDAO<ShmSalvageRqst> {

	public long getSalvageRequestId(String seqName, EntityManager entityManager) {
        return getNextSeq(seqName, entityManager);
    }
	
}
