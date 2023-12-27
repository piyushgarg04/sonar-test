package com.xpo.ltl.shipment.service.dao;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPass;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPassPK;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmRtgInfoPassSubDAO {

	public DB2ShmRtgInfoPass createShmRtgInfoPass(
		@NotNull final DB2ShmRtgInfoPass pass,
		@NotNull final EntityManager db2EntityManager) {
		Objects.requireNonNull(pass, "Required parameter DB2ShmRtgInfoPass pass is null.");
		Objects.requireNonNull(db2EntityManager, "Required parameter EntityManager db2EntityManager is null.");

		db2EntityManager.persist(pass);
		db2EntityManager.flush();

		return pass;
	}

	public DB2ShmRtgInfoPass findById(
		@NotNull final DB2ShmRtgInfoPassPK id,
		@NotNull final EntityManager db2EntityManager) {
		Objects.requireNonNull(id, "Required parameter DB2ShmRtgInfoPassPK id is null.");
		Objects.requireNonNull(db2EntityManager, "Required parameter EntityManager db2EntityManager is null.");

		return db2EntityManager.find(DB2ShmRtgInfoPass.class, id);
	}
}
