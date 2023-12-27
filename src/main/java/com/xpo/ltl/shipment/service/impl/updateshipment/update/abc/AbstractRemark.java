package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmRemarkPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.Comparator;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.ComparatorFactory;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.xpo.ltl.shipment.service.impl.updateshipment.comparator.EntityComparer.findDifferences;

public abstract class AbstractRemark extends AbstractUpdate {

	private static final String REMARKS = "Remarks";
	@Inject
	private ShipmentRemarkSubDAO shipmentRemarkSubDAO;

	@LogExecutionTime
	public void updateRemarks(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkList,
			String transactionCd) throws ValidationException {
		if (CollectionUtils.isNotEmpty(shmRemarkList)) {

			shmRemarkList.forEach(shmRemark -> {
				shmRemark.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmRemark.setLstUpdtTranCd(transactionCd);
				shmRemark.setLstUpdtUid(getUserFromContext(transactionContext));

			});
			shipmentRemarkSubDAO.persist(shmRemarkList, entityManager);

			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				shmRemarkList.forEach(shmRemark -> {
					try {
						final Function<DB2ShmRemark, Boolean> checkVersionFunctionShmRemark = getCheckVersionFunctionShmRemark(
								new Timestamp(new Date().getTime()));
						shipmentRemarkSubDAO.updateDB2ShmRemark(shmRemark,
								checkVersionFunctionShmRemark,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.REMARK_UPDATE_FAILED, REMARKS, e, transactionContext);
					}
				});
			}

		}
	}

	@LogExecutionTime
	public void addRemarks(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkListToAdd,
			String transactionCd) throws ValidationException {
		if (CollectionUtils.isNotEmpty(shmRemarkListToAdd)) {
			shmRemarkListToAdd.forEach(shmRemark -> {
				shmRemark.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmRemark.setLstUpdtTranCd(transactionCd);
				shmRemark.setLstUpdtUid(getUserFromContext(transactionContext));

			});
			try {
				shipmentRemarkSubDAO.persist(shmRemarkListToAdd, entityManager);
				if (appContext.getDb2CommitEnabledForUpdateShipment()) {
					shmRemarkListToAdd.forEach(shmRemark -> shipmentRemarkSubDAO.createDB2ShmRemark(shmRemark,
							db2EntityManager));
				}
			} catch (Exception e) {
				getException(ServiceErrorMessage.REMARK_CREATE_FAILED, REMARKS, e, transactionContext);
			}
		}
	}

	@LogExecutionTime
	public void deleteRemarks(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkListToDelete) {
		if (CollectionUtils.isNotEmpty(shmRemarkListToDelete)) {

			shipmentRemarkSubDAO.remove(shmRemarkListToDelete, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				final Function<DB2ShmRemark, Boolean> checkVersionFunctionShmRemark = getCheckVersionFunctionShmRemark(new Timestamp(
						new Date().getTime()));
				shmRemarkListToDelete.forEach(shmRemark -> {
					try {
						shipmentRemarkSubDAO.deleteDB2ShmRemark(shmRemark.getId(),
								checkVersionFunctionShmRemark,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.REMARK_DELETE_FAILED, REMARKS, e, transactionContext);
					}

				});
			}
		}
	}

	public Function<DB2ShmRemark, Boolean> getCheckVersionFunctionShmRemark(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	public void setRemarkValues(
			Remark remark, ShmRemark toShmRemark) {

		ShmRemarkPK shmRemarkPK = new ShmRemarkPK();

		shmRemarkPK.setTypCd(getShipmentRemarkTypeCd(remark.getTypeCd().value()));
		toShmRemark.setId(shmRemarkPK);
		if (StringUtils.isEmpty(toShmRemark.getArchiveCntlCd())) {
			toShmRemark.setArchiveCntlCd(StringUtils.SPACE);
		}

	}

	@LogExecutionTime
	public List<ShmRemark> getAddRemarks(
			List<Remark> shipmentRemarks,
			List<ShmRemark> shmRemarks,
			List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds,
			ShmShipment shmShipment,
			String userId,
			TransactionContext transactionContext,
			String transactionCd) {
		List<ShmRemark> result = new ArrayList<>();
		Timestamp timestamp = new Timestamp(new Date().getTime());
		CollectionUtils.emptyIfNull(shipmentRemarks).forEach(remark -> {
			if (shipmentRemarkTypeCds.contains(remark.getTypeCd())) {

				Optional<ShmRemark> optionalShmRemark = shmRemarks
						.stream()
						.filter(shmRemark ->
								shmRemark.getId().getTypCd().equals(getShipmentRemarkTypeCd(remark.getTypeCd().value()))
										&& shmRemark.getId().getShpInstId() == remark.getShipmentInstId())
						.findAny();

				if (!optionalShmRemark.isPresent()) {

					try {
						ShmRemark toShmRemark = new ShmRemark();
						remark.setListActionCd(ActionCd.ADD);
						DtoTransformer.toShmRemark(remark, toShmRemark);

						setRemarkValues(remark, toShmRemark);
						toShmRemark.setLstUpdtTmst(timestamp);
						toShmRemark.setLstUpdtTranCd(transactionCd);
						toShmRemark.setLstUpdtUid(userId);
						toShmRemark.getId().setShpInstId(shmShipment.getShpInstId());

						result.add(toShmRemark);
					} catch (Exception e) {
						getException(ServiceErrorMessage.UNEXPECTED_EXCEPTION, "getAddRemarks", e, transactionContext);
					}
				}
			}
		});
		return result;
	}

	@LogExecutionTime
	public List<ShmRemark> getUpdateRemark(
			List<Remark> shipmentRemarks,
			List<ShmRemark> shmRemarks,
			String userId,
			String transactionCd,
			List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds) {
		List<ShmRemark> result = new ArrayList<>();

		if (CollectionUtils.isNotEmpty(shipmentRemarks) && CollectionUtils.isNotEmpty(shmRemarks)) {
			shipmentRemarks.forEach(remark -> {

				if (shipmentRemarkTypeCds
						.stream()
						.anyMatch(shipmentRemarkTypeCd -> shipmentRemarkTypeCd.equals(remark.getTypeCd()))) {

					Optional<ShmRemark> optionalShmRemark = shmRemarks
							.stream()
							.filter(shmRemark -> shmRemark
									.getId()
									.getTypCd()
									.equals(getShipmentRemarkTypeCd(remark.getTypeCd().value()))
									&& shmRemark.getId().getShpInstId() == remark.getShipmentInstId())
							.findAny();

					if (optionalShmRemark.isPresent()) {
						ShmRemark shmRemarkToCheck = new ShmRemark();

						ShmRemark shmRemark = optionalShmRemark.get();
						copyFields(shmRemark, shmRemarkToCheck);

						shmRemark.setRmkTxt(remark.getRemark());
						List<String> diff = this.compareRemarks(shmRemark, shmRemarkToCheck);

						if (CollectionUtils.isNotEmpty(diff)) {
							shmRemark.setLstUpdtTranCd(transactionCd);
							shmRemark.setLstUpdtTmst(new Timestamp(new Date().getTime()));
							shmRemark.setLstUpdtUid(userId);
							result.add(shmRemark);
						}
					}
				}

			});

		}

		return result;
	}

	@LogExecutionTime
	public List<ShmRemark> getDeleteRemarks(
			List<Remark> shipmentRemarks,
			List<ShmRemark> shmRemarks,
			List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds) {
		List<ShmRemark> result = new ArrayList<>();

		CollectionUtils.emptyIfNull(shmRemarks).forEach(shmRemark -> {

			if (shipmentRemarkTypeCds.contains(ShipmentRemarkTypeCd.fromValue(getShipmentRemarkTypeCd(shmRemark
					.getId()
					.getTypCd())))) {

				if (CollectionUtils.isNotEmpty(shipmentRemarks)) {
					Optional<Remark> optionalShmRemark = shipmentRemarks
							.stream()
							.filter(remark -> shmRemark
									.getId()
									.getTypCd()
									.equals(getShipmentRemarkTypeCd(remark.getTypeCd().value()))
									&& shmRemark.getId().getShpInstId() == remark.getShipmentInstId())
							.findAny();

					if (!optionalShmRemark.isPresent()) {

						result.add(shmRemark);
					}
				} else {
					result.addAll(shmRemarks
							.stream()
							.filter(shmRemark1 -> shipmentRemarkTypeCds.contains(ShipmentRemarkTypeCd.fromValue(
									getShipmentRemarkTypeCd(shmRemark1.getId().getTypCd()))))
							.collect(Collectors.toList()));
				}
			}
		});

		return result.stream().distinct().collect(Collectors.toList());
	}

	public String getShipmentRemarkTypeCd(String name) {
		Field[] statusDeclaredFields = ShipmentRemarkTypeCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}

	protected List<String> compareRemarks(ShmRemark source, ShmRemark target) {
		Comparator<ShmRemark> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmRemark.class.getName(), differences, source.getId().getShpInstId());
		return differences;
	}
}
