package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmTmDtCritical;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.TimeDateCritical;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.TimeDateCriticalUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractTimeDateCritical;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CorrectionTimeDateCriticalUpdateImpl extends AbstractTimeDateCritical implements TimeDateCriticalUpdate {
	private static final Logger logger = LogManager.getLogger(CorrectionTimeDateCriticalUpdateImpl.class);
	@Override
	public void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmTmDtCritical> shmTmDtCriticals,
			String transactionCd) throws ValidationException {

		this.updateTimeDateCriticals(entityManager,
				db2EntityManager,
				transactionContext,
				shmTmDtCriticals,
				transactionCd);

	}
	@Override
	public void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmTmDtCritical> tdcToDeleteList) throws ValidationException {

		if (CollectionUtils.isNotEmpty(tdcToDeleteList)) {
			shipmentTdcSubDAO.remove(tdcToDeleteList, entityManager);
			if (appContext.getApplyDb2TwoPhaseCommit()) {
				tdcToDeleteList.forEach(shmTmDtCritical -> {
					try {

						final Timestamp exadataLstUpdtTmst = new Timestamp(new Date().getTime());
						final Function<DB2ShmTmDtCritical, Boolean> checkVersionFunction = getCheckVersionFunctionShmTmDtCritical(
								exadataLstUpdtTmst);
						shipmentTdcSubDAO.deleteDB2(shmTmDtCritical.getShpInstId(),
								checkVersionFunction,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						logger.error(e);
						getException(ServiceErrorMessage.SHM_TM_DT_CRITICAL_DE, TMDTCRITICAL, e, transactionContext);
					}

				});
			}
		}

	}
	@Override
	public void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmTmDtCritical> shmTmDtCriticals,
			String transactionCd) throws ValidationException {

		this.addTimeDateCriticals(entityManager,
				db2EntityManager,
				transactionContext, shmTmDtCriticals,
				transactionCd);

	}

	@Override
	public CompletableFuture<Map<ActionCd, List<ShmTmDtCritical>>> cfGetTimeDateCriticalsForTransactions(
			TimeDateCritical timeDateCritical,
			ShmTmDtCritical shmTmDtCritical,
			String userId,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return getTimeDateCriticalsForTransactions(timeDateCritical,
						shmTmDtCritical,
						userId,
						shmShipment,
						transactionCd,
						transactionContext);
			} catch (ParseException e) {
				throw new IllegalStateException(e);
			}
		});
	}

	@Override
	public Map<ActionCd, List<ShmTmDtCritical>> getTimeDateCriticalsForTransactions(
			TimeDateCritical timeDateCritical,
			ShmTmDtCritical shmTmDtCritical,
			String userId,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) throws ParseException {

		Map<ActionCd, List<ShmTmDtCritical>> result = new HashMap<>();

		result.put(ActionCd.ADD,
				this.getAbcShmTmDtCriticalForInsert(shmShipment.getShpInstId(),
						timeDateCritical,
						shmTmDtCritical,
						transactionContext));
		result.put(ActionCd.UPDATE,
				this.getAbcShmTmDtCriticalForUpdate(timeDateCritical,
						shmTmDtCritical,
						userId,
						transactionCd,
						transactionContext));
		result.put(ActionCd.DELETE,
				this.getAbcShmTmDtCriticalForDelete(timeDateCritical,
						shmTmDtCritical));
		return result;
	}
}
