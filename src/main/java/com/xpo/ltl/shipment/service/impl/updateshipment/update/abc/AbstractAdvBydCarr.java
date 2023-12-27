package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarrPK;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondCarrier;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondTypeCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.Comparator;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.ComparatorFactory;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.xpo.ltl.shipment.service.impl.updateshipment.comparator.EntityComparer.findDifferences;

public abstract class AbstractAdvBydCarr extends AbstractUpdate {

	private static final String ADV_BYD_CARR = "AdvBydCarr";
	@Inject
	private ShipmentAdvBydSubDAO shipmentAdvBydSubDAO;

	public static List<ShmAdvBydCarr> resetSeqNumberAdvBydCarr(
			Long shipmentInstId, List<ShmAdvBydCarr> shmAdvBydCarrs) {
		List<ShmAdvBydCarr> newList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmAdvBydCarrs)) {

			sortListByLongField(shmAdvBydCarrs, ShmAdvBydCarr::getId, ShmAdvBydCarrPK::getSeqNbr);
			AtomicReference<Long> seq = new AtomicReference<>(1L);
			shmAdvBydCarrs.forEach(shmAdvBydCarr -> {
				ShmAdvBydCarr shmAdvBydCarrNew = new ShmAdvBydCarr();
				copyFields(shmAdvBydCarr, shmAdvBydCarrNew);

				ShmAdvBydCarrPK shmAdvBydCarrPK = new ShmAdvBydCarrPK();
				shmAdvBydCarrPK.setShpInstId(shipmentInstId);
				shmAdvBydCarrPK.setSeqNbr(seq.getAndSet(seq.get() + 1));
				shmAdvBydCarrNew.setId(shmAdvBydCarrPK);
				newList.add(shmAdvBydCarrNew);

			});
		}
		return newList;
	}

	@LogExecutionTime
	public void updateShmAdvBydCarr(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) throws ValidationException {
		if (CollectionUtils.isNotEmpty(shmAdvBydCarrs)) {

			shmAdvBydCarrs.forEach(shmAdvBydCarr -> {
				shmAdvBydCarr.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmAdvBydCarr.setLstUpdtTranCd(transactionCd);
				shmAdvBydCarr.setLstUpdtUid(getUserFromContext(transactionContext));

			});
			shipmentAdvBydSubDAO.persist(shmAdvBydCarrs, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				shmAdvBydCarrs.forEach(shmAdvBydCarr -> {

					try {
						final Function<DB2ShmAdvBydCarr, Boolean> checkVersionFunctionAdvBydCarr = getCheckVersionFunctionAdvBydCarr(
								new Timestamp(new Date().getTime()));

						shipmentAdvBydSubDAO.updateDB2ShmAdvBydCarr(setDefaultAdvBydValues(shmAdvBydCarr),
								checkVersionFunctionAdvBydCarr,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.ADV_BYD_CARR_UPDATE_FAILED,
								"AdvanceBeyond",
								e,
								transactionContext);
					}
				});
			}

		}
	}

	public Function<DB2ShmAdvBydCarr, Boolean> getCheckVersionFunctionAdvBydCarr(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	@LogExecutionTime
	public void deleteAdvBydCarr(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs) {

		if (CollectionUtils.isNotEmpty(shmAdvBydCarrs)) {
			shipmentAdvBydSubDAO.remove(shmAdvBydCarrs, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				shmAdvBydCarrs.forEach(shmAdvBydCarr -> {
					try {
						final Function<DB2ShmAdvBydCarr, Boolean> checkVersionFunctionShmAdvBydCarr = getCheckVersionFunctionAdvBydCarr(
								new Timestamp(new Date().getTime()));
						shipmentAdvBydSubDAO.deleteDB2ShmAdvBydCarr(shmAdvBydCarr.getId(),
								checkVersionFunctionShmAdvBydCarr,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.ADV_BYD_CARR_DELETE_FAILED, ADV_BYD_CARR, e, transactionContext);
					}

				});
			}
		}
	}

	@LogExecutionTime
	public void insertAdvBydCarr(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) throws ValidationException {
		if (CollectionUtils.isNotEmpty(shmAdvBydCarrs)) {
			shmAdvBydCarrs.forEach(shmCommodity -> {
				shmCommodity.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmCommodity.setLstUpdtTranCd(transactionCd);
				shmCommodity.setLstUpdtUid(getUserFromContext(transactionContext));

			});
			try {
				shipmentAdvBydSubDAO.persist(shmAdvBydCarrs, entityManager);
				if (appContext.getDb2CommitEnabledForUpdateShipment()) {
					shmAdvBydCarrs.forEach(shmAdvBydCarr -> shipmentAdvBydSubDAO.createDB2ShmAdvBydCarr(setDefaultAdvBydValues(shmAdvBydCarr),
							db2EntityManager));
				}
			} catch (Exception e) {
				getException(ServiceErrorMessage.ADV_BYD_CARR_CREATE_FAILED, ADV_BYD_CARR, e, transactionContext);
			}
		}
	}

	public List<ShmAdvBydCarr> getAbcShmAdvBydCarrsToDelete(
			UpdateShipmentRqst updateShipmentRqst, List<ShmAdvBydCarr> shmAdvBydCarrs) {
		List<ShmAdvBydCarr> result = new ArrayList<>();

		shmAdvBydCarrs.forEach(shmAdvBydCarr -> {

			Optional<AdvanceBeyondCarrier> optionalAdvanceBeyondCarrier = updateShipmentRqst
					.getAdvanceBeyondCarriers()
					.stream()
					.filter(advanceBeyondCarrier -> getAdvanceBeyondTypeCd(advanceBeyondCarrier
							.getTypeCd()
							.value()).equals(shmAdvBydCarr.getTypCd())
							&& shmAdvBydCarr.getId().getShpInstId() == advanceBeyondCarrier.getShipmentInstId())
					.findAny();

			if (!optionalAdvanceBeyondCarrier.isPresent()) {
				result.add(shmAdvBydCarr);

			}
		});

		return result;

	}

	public List<ShmAdvBydCarr> getAbcShmAdvBydCarrsToInsert(
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) {
		List<ShmAdvBydCarr> result = new ArrayList<>();

		if (CollectionUtils.isNotEmpty(updateShipmentRqst.getAdvanceBeyondCarriers()) ) {
			String userId = getUserFromContext(transactionContext);
			Timestamp timestamp = new Timestamp(new Date().getTime());
			updateShipmentRqst.getAdvanceBeyondCarriers().forEach(advanceBeyondCarrier -> {

				Optional<ShmAdvBydCarr> optionalShmAdvBydCarr = shmAdvBydCarrs
						.stream()
						.filter(shmAdvBydCarr -> shmAdvBydCarr
								.getTypCd()
								.equals(getAdvanceBeyondTypeCd(advanceBeyondCarrier.getTypeCd().value()))
								&& shmAdvBydCarr.getId().getShpInstId() == advanceBeyondCarrier.getShipmentInstId())
						.findAny();

				if (!optionalShmAdvBydCarr.isPresent()) {

					try {
						ShmAdvBydCarr toShmAdvBydCarr = new ShmAdvBydCarr();
						advanceBeyondCarrier.setListActionCd(ActionCd.ADD);
						DtoTransformer.toShmAdvBydCarr(advanceBeyondCarrier, toShmAdvBydCarr);

						toShmAdvBydCarr.setLstUpdtTmst(timestamp);
						toShmAdvBydCarr.setLstUpdtTranCd(transactionCd);
						toShmAdvBydCarr.setLstUpdtUid(userId);
						toShmAdvBydCarr.getId().setShpInstId(shmShipment.getShpInstId());

						result.add(toShmAdvBydCarr);
					} catch (Exception e) {

						getException(ServiceErrorMessage.ADV_BYD_CARR_CREATE_FAILED,
								ADV_BYD_CARR,
								e,
								transactionContext);
					}

				}

			});

		}

		return result;
	}

	public String getAdvanceBeyondTypeCd(String name) {
		Field[] statusDeclaredFields = AdvanceBeyondTypeCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}

	@LogExecutionTime
	protected List<String> compareShmAdvBydCarr(ShmAdvBydCarr source, ShmAdvBydCarr target) {
		Comparator<ShmAdvBydCarr> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmRemark.class.getName(), differences, source.getId().getShpInstId());
		return differences;
	}

	public ShmAdvBydCarr setDefaultAdvBydValues(ShmAdvBydCarr shmAdvBydCarr) {

		if (Objects.isNull(shmAdvBydCarr.getCarrPkupDt())) {
			shmAdvBydCarr.setCarrPkupDt(BasicTransformer.toDate("0001-01-01"));
		}
		if (Objects.isNull(shmAdvBydCarr.getFromTrmnlSicCd())) {
			shmAdvBydCarr.setFromTrmnlSicCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAdvBydCarr.getArchiveCntlCd())) {
			shmAdvBydCarr.setArchiveCntlCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAdvBydCarr.getCarrScacCd())) {
			shmAdvBydCarr.setCarrScacCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAdvBydCarr.getCarrProNbrTxt())) {
			shmAdvBydCarr.setCarrProNbrTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAdvBydCarr.getChgAmt())) {
			shmAdvBydCarr.setChgAmt(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmAdvBydCarr.getToTrmnlSicCd())) {
			shmAdvBydCarr.setToTrmnlSicCd(StringUtils.SPACE);
		}
		return shmAdvBydCarr;
	}

}

