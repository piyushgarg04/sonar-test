package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;

import javax.persistence.EntityManager;
import java.util.List;

public interface RemarkAllTxUpdate extends RemarkUpdate {
	void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkList) throws ValidationException;
}
