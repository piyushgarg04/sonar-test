package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBondPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.CustomsBond;
import com.xpo.ltl.shipment.service.dao.ShipmentCustomsSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.Comparator;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.ComparatorFactory;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
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
import static com.xpo.ltl.shipment.service.util.TimestampUtil.getStringDate;

public abstract class AbstractCustomsBond extends AbstractUpdate {

	private static final String CUSTOM_BONDS = "CustomsBond";
	@Inject
	private ShipmentCustomsSubDAO shipmentCustomsSubDAO;

	public static List<ShmCustomsBond> resetSeqNumberCustomsBond(
			Long shipmentInstId, List<ShmCustomsBond> shmCustomsBonds) {
		List<ShmCustomsBond> newList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmCustomsBonds)) {

			sortListByLongField(shmCustomsBonds, ShmCustomsBond::getId, ShmCustomsBondPK::getSeqNbr);
			AtomicReference<Long> seq = new AtomicReference<>(1L);
			shmCustomsBonds.forEach(shmCustomsBond -> {
				ShmCustomsBond shmCustomsBondNew = new ShmCustomsBond();
				copyFields(shmCustomsBond, shmCustomsBond);

				ShmCustomsBondPK shmCustomsBondPK = new ShmCustomsBondPK();
				shmCustomsBondPK.setShpInstId(shipmentInstId);
				shmCustomsBondPK.setSeqNbr(seq.getAndSet(seq.get() + 1));
				shmCustomsBondNew.setId(shmCustomsBondPK);
				newList.add(shmCustomsBondNew);

			});
		}
		return newList;
	}

	@LogExecutionTime
	public void updateCustomsBond(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> customsBondList,
			String transactionCd) throws ValidationException {

		if (CollectionUtils.isNotEmpty(customsBondList)) {

			customsBondList.forEach(customsBond -> {
				customsBond.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				customsBond.setLstUpdtTranCd(transactionCd);
				customsBond.setLstUpdtUid(getUserFromContext(transactionContext));
			});

			shipmentCustomsSubDAO.persist(customsBondList, entityManager);

			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				customsBondList.forEach(customsBond -> {
					try {
						final Function<DB2ShmCustomsBond, Boolean> checkVersionFunctionShmCustomsBond = getCheckVersionFunctionShmCustomsBond(
								new Timestamp(new Date().getTime()));
						setCustomBondsDefaultValues(customsBond);
						shipmentCustomsSubDAO.updateDB2ShmCustomsBond(customsBond,
								checkVersionFunctionShmCustomsBond,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.CUSTOMS_BOND_UPDATE_FAILED, CUSTOM_BONDS, e, transactionContext);
					}

				});
			}
		}
	}

	@LogExecutionTime
	public void deleteCustomsBond(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> customsBondList) {

		if (CollectionUtils.isNotEmpty(customsBondList)) {
			shipmentCustomsSubDAO.remove(customsBondList, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				customsBondList.forEach(customsBond -> {
					try {
						final Function<DB2ShmCustomsBond, Boolean> checkVersionFunctionShmCustomsBond = getCheckVersionFunctionShmCustomsBond(
								new Timestamp(new Date().getTime()));
						shipmentCustomsSubDAO.deleteDB2ShmCustomsBond(customsBond.getId(),
								checkVersionFunctionShmCustomsBond,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.CUSTOMS_BOND_DELETE_FAILED, CUSTOM_BONDS, e, transactionContext);
					}

				});
			}
		}
	}

	@LogExecutionTime
	public void insertCustomsBond(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> customsBondList,
			String transactionCd) {
		if (CollectionUtils.isNotEmpty(customsBondList)) {
			customsBondList.forEach(shmCustomsBond -> {
				shmCustomsBond.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmCustomsBond.setLstUpdtTranCd(transactionCd);
				shmCustomsBond.setLstUpdtUid(getUserFromContext(transactionContext));

			});
			try {
				shipmentCustomsSubDAO.persist(customsBondList, entityManager);
				if (appContext.getDb2CommitEnabledForUpdateShipment()) {
					customsBondList.forEach(shmCustomsBond -> {
						setCustomBondsDefaultValues(shmCustomsBond);
						shipmentCustomsSubDAO.createDB2ShmCustomsBond(shmCustomsBond,
								db2EntityManager);
					});
				}
			} catch (Exception e) {
				getException(ServiceErrorMessage.CUSTOMS_BOND_CREATE_FAILED, CUSTOM_BONDS, e, transactionContext);
			}
		}
	}

	public Function<DB2ShmCustomsBond, Boolean> getCheckVersionFunctionShmCustomsBond(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	public List<ShmCustomsBond> getAbcCustomsBondListToDelete(
			final List<CustomsBond> customsBonds, final List<ShmCustomsBond> shmCustomsBonds) {
		List<ShmCustomsBond> result = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(customsBonds) && CollectionUtils.isNotEmpty(shmCustomsBonds)) {
			shmCustomsBonds.forEach(shmCustomsBond -> {

				Optional<CustomsBond> optionalCustomsBond = customsBonds
						.stream()
						.filter(customsBond -> shmCustomsBond.getId().getShpInstId() == customsBond.getShipmentInstId())
						.findAny();

				if (!optionalCustomsBond.isPresent()) {
					result.add(shmCustomsBond);

				}
			});

		}

		return result;
	}

	protected List<ShmCustomsBond> getAbcCustomsBondForInsert(
			ShmShipment shmShipment,
			List<CustomsBond> customsBonds,
			List<ShmCustomsBond> shmCustomsBonds,
			TransactionContext transactionContext) {
		List<ShmCustomsBond> result = new ArrayList<>();

		if (CollectionUtils.isNotEmpty(customsBonds) ) {
			customsBonds.forEach(customsBond -> {
				Optional<ShmCustomsBond> optionalShmCustomsBond = Optional.empty();
				if (CollectionUtils.isNotEmpty(shmCustomsBonds)) {
					optionalShmCustomsBond = shmCustomsBonds
							.stream()
							.filter(shmCustomsBond -> shmCustomsBond.getId().getShpInstId()
									== customsBond.getShipmentInstId())
							.findAny();
				}

				if (!optionalShmCustomsBond.isPresent()) {
					try {

						ShmCustomsBond shmCustomsBond = new ShmCustomsBond();
						if (StringUtils.isEmpty(customsBond.getBondCreateDate())) {
							customsBond.setBondCreateDate(getStringDate(new Date(), DATE_PATTERN));
						}
						DtoTransformer.toShmCustomsBond(customsBond, shmCustomsBond);
						ShmCustomsBondPK shmCustomsBondPK = new ShmCustomsBondPK();
						shmCustomsBondPK.setShpInstId(shmShipment.getShpInstId());
						shmCustomsBondPK.setSeqNbr(1L);
						shmCustomsBond.setId(shmCustomsBondPK);
						shmCustomsBond.setShmShipment(shmShipment);
						result.add(shmCustomsBond);
					} catch (Exception e) {
						getException(ServiceErrorMessage.CUSTOMS_BOND_CREATE_FAILED,
								CUSTOM_BONDS,
								e,
								transactionContext);
					}

				}
			});
		}

		return result;
	}

	@LogExecutionTime
	protected List<String> compareCustomsBond(ShmCustomsBond source, ShmCustomsBond target) {
		Comparator<ShmCustomsBond> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmCustomsBond.class.getName(), differences, source.getId().getShpInstId());
		return differences;
	}
	 public void setCustomBondsDefaultValues(ShmCustomsBond shmCustomsBond){

		 if (Objects.isNull(shmCustomsBond.getArchiveCntlCd())) {
			 shmCustomsBond.setArchiveCntlCd(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getBondNbrTxt())) {
			 shmCustomsBond.setBondNbrTxt(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getBondReasonCd())) {
			 shmCustomsBond.setBondReasonCd(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getBondStatusCd())) {
			 shmCustomsBond.setBondStatusCd(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getBondTypeCd())) {
			 shmCustomsBond.setBondTypeCd(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getBondValueAmt())) {
			 shmCustomsBond.setBondValueAmt(BigDecimal.ZERO);
		 }
		 if (Objects.isNull(shmCustomsBond.getBondedPort())) {
			 shmCustomsBond.setBondedPort(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getBondedSicCode())) {
			 shmCustomsBond.setBondedSicCode(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getBroker())) {
			 shmCustomsBond.setBroker(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getCrcAuditCd())) {
			 shmCustomsBond.setCrcAuditCd(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getCtyTxt())) {
			 shmCustomsBond.setCtyTxt(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getShipmentDirectionCd())) {
			 shmCustomsBond.setShipmentDirectionCd(StringUtils.SPACE);
		 }
		 if (Objects.isNull(shmCustomsBond.getStCd())) {
			 shmCustomsBond.setStCd(StringUtils.SPACE);
		 }


	 }

}
