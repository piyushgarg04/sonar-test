package com.xpo.ltl.shipment.service.dao;

import javax.persistence.EntityManager;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.service.dao.ShmGuaranteedFailRerateDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmGuaranteedFailRerate;

public class ShmGuaranteedFailRerateSubDAO extends ShmGuaranteedFailRerateDAO<ShmGuaranteedFailRerate> {
	public void createShmGuaranteedFailRerate(final ShmGuaranteedFailRerate shmGuaranteedFailRerate,
			final EntityManager entityManager) throws ValidationException {
		persist(shmGuaranteedFailRerate, entityManager);
	}

}
