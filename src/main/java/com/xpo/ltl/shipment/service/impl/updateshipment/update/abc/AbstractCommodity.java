package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodityPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.CommodityClassCd;
import com.xpo.ltl.api.shipment.v2.CommodityPackageCd;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
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

public abstract class AbstractCommodity extends AbstractUpdate {

	private static final String COMMODITIES = "Commodities";
	@Inject
	private ShmCommoditySubDAO shmCommoditySubDAO;



	public static List<ShmCommodity> resetSeqNumberCommodities(Long shipmentInstId, List<ShmCommodity> commodities,ShmShipment shmShipment) {
		List<ShmCommodity> newList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(commodities)) {

			sortListByLongField(commodities, ShmCommodity::getId, ShmCommodityPK::getSeqNbr);
			AtomicReference<Long> seq = new AtomicReference<>(1L);
			commodities.forEach(shmCommodity -> {
				ShmCommodity shmCommodityNew = new ShmCommodity();
				copyFields(shmCommodity, shmCommodityNew);

				ShmCommodityPK shmCommodityPK = new ShmCommodityPK();
				shmCommodityPK.setShpInstId(shipmentInstId);
				shmCommodityPK.setSeqNbr(seq.getAndSet(seq.get() + 1));
				shmCommodityNew.setId(shmCommodityPK);
				shmCommodityNew.setShmShipment(shmShipment);
				newList.add(shmCommodityNew);

			});
		}
		return newList;
	}

	@LogExecutionTime
	public void updateCommodities(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCommodity> commodities,
			String transactionCd,
			String userId) throws ValidationException {

		if (CollectionUtils.isNotEmpty(commodities)) {

			commodities.forEach(commodity -> {
				commodity.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				commodity.setLstUpdtTranCd(transactionCd);
				commodity.setLstUpdtUid(userId);
			});

			shmCommoditySubDAO.persist(commodities, entityManager);

			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				commodities.forEach(commodity -> {
					try {
						final Function<DB2ShmCommodity, Boolean> checkVersionFunctionShmCommodity = getCheckVersionFunctionShmCommodity(
								new Timestamp(new Date().getTime()));
						setCommodityDefaultValues(commodity);

						shmCommoditySubDAO.updateDB2ShmCommodity(commodity,
								checkVersionFunctionShmCommodity,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.COMMODITY_UPDATE_FAILED, COMMODITIES, e, transactionContext);
					}

				});
			}
		}
	}

	@LogExecutionTime
	public void deleteCommodities(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCommodity> commodities) {

		if (CollectionUtils.isNotEmpty(commodities)) {


			shmCommoditySubDAO.remove(commodities, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				commodities.forEach(commodity -> {
					try {
						final Function<DB2ShmCommodity, Boolean> checkVersionFunctionShmCommodity = getCheckVersionFunctionShmCommodity(
								new Timestamp(new Date().getTime()));
						shmCommoditySubDAO.deleteDB2ShmCommodity(commodity.getId(),
								checkVersionFunctionShmCommodity,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						getException(ServiceErrorMessage.COMMODITY_DELETE_FAILED, COMMODITIES, e, transactionContext);
					}
				});
			}
		}
	}

	@LogExecutionTime
	public void insertCommodities(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCommodity> shmCommodities,
			String transactionCd,
			String userId)  {
		if (CollectionUtils.isNotEmpty(shmCommodities)) {
			shmCommodities.forEach(shmCommodity -> {
				shmCommodity.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmCommodity.setLstUpdtTranCd(transactionCd);
				shmCommodity.setLstUpdtUid(userId);

			});
			try {

				shmCommodities.forEach(shmCommodity -> {
					entityManager.merge(shmCommodity);
					if (appContext.getDb2CommitEnabledForUpdateShipment()) {
						setCommodityDefaultValues(shmCommodity);
						shmCommodity.setLstUpdtTranCd(transactionCd);
						shmCommoditySubDAO.createDB2ShmCommodity(shmCommodity, db2EntityManager);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				getException(ServiceErrorMessage.COMMODITY_CREATE_FAILED, COMMODITIES, e, transactionContext);
			}
		}
	}

	public Function<DB2ShmCommodity, Boolean> getCheckVersionFunctionShmCommodity(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	protected List<ShmCommodity> getAbcCommoditiesForDelete(
			List<Commodity> commodities, List<ShmCommodity> shmCommodities) {
		List<ShmCommodity> result = new ArrayList<>();

		shmCommodities.forEach(shmCommodity -> {

			Optional<Commodity> optionalCommodity = commodities
					.stream()
					.filter(commodity -> Objects.nonNull(commodity.getSequenceNbr())
							&& commodity.getSequenceNbr().longValue() == shmCommodity.getId().getSeqNbr()
							&& shmCommodity.getId().getShpInstId() == commodity.getShipmentInstId())
					.findAny();

			if (!optionalCommodity.isPresent()) {
				result.add(shmCommodity);

			}
		});

		return result;
	}

	protected List<ShmCommodity> getAbcCommoditiesForInsert(ShmShipment shmShipment, List<Commodity> commodities) throws ServiceException {
		List<ShmCommodity> result = new ArrayList<>();

				commodities.forEach(commodity -> {
			if (Objects.isNull(commodity.getSequenceNbr())) {
				ShmCommodityPK shmCommodityPK = new ShmCommodityPK();
				shmCommodityPK.setShpInstId(shmShipment.getShpInstId());
				ShmCommodity shmCommodity = new ShmCommodity();
				shmCommodity.setId(shmCommodityPK);
				if (Objects.nonNull(commodity.getAmount())) {
					shmCommodity.setAmt(BigDecimal.valueOf(commodity.getAmount()));
				}
				shmCommodity.setArchiveCntlCd(commodity.getArchiveControlCd());
				if (Objects.nonNull(commodity.getAsRatedClassCd())) {
					shmCommodity.setAsRatedClassCd(getCommodityClassCd(commodity.getAsRatedClassCd().value()));
				}
				if (Objects.nonNull(commodity.getChrgToCd())) {
					shmCommodity.setChrgToCd(getChargeToCdAlt(commodity.getChrgToCd().value()));
				}
				if (Objects.nonNull(commodity.getClassType())) {
					shmCommodity.setClassTyp(getCommodityClassCd(commodity.getClassType().value()));
				}
				shmCommodity.setDescTxt(commodity.getDescription());
				if (Objects.nonNull(commodity.getDefaultClassSelectedInd())) {
					shmCommodity.setDfltClassSlctInd(getFlag(commodity.getDefaultClassSelectedInd()));
				}

				if (Objects.nonNull(commodity.getFreezableInd())) {
					shmCommodity.setFrzbleInd(getFlag(commodity.getFreezableInd()));
				}
				if (Objects.nonNull(commodity.getHazardousMtInd())) {
					shmCommodity.setHzMtInd(getFlag(commodity.getHazardousMtInd()));
				}

				if (Objects.nonNull(commodity.getMinimumChargeInd())) {
					shmCommodity.setMinChrgInd(getFlag(commodity.getMinimumChargeInd()));
				}
				if (Objects.nonNull(commodity.getMixClassCommodityInd())) {
					shmCommodity.setMixClssCmdyInd(getFlag(commodity.getMixClassCommodityInd()));
				}

				shmCommodity.setNmfcItmCd(commodity.getNmfcItemCd());
				shmCommodity.setOriglDescTxt(commodity.getOriginalDescription());

				if (Objects.nonNull(commodity.getPiecesCount())) {
					shmCommodity.setPcsCnt(BigDecimal.valueOf(commodity.getPiecesCount().doubleValue()));
				}
				if (Objects.nonNull(commodity.getPackageCd())) {
					shmCommodity.setPkgCd(getCommodityPackageCd(commodity.getPackageCd().value()));
				}
				if (Objects.nonNull(commodity.getPrepaidPercentage())) {
					shmCommodity.setPpdPct(BigDecimal.valueOf(commodity.getPrepaidPercentage()));
				}
				if (Objects.nonNull(commodity.getReducedWeight())) {
					shmCommodity.setRdcdWgt(BigDecimal.valueOf(commodity.getReducedWeight()));
				}

				if (Objects.nonNull(commodity.getRatingQuantity())) {
					shmCommodity.setRtgQty(BigDecimal.valueOf(commodity.getRatingQuantity()));
				}
				shmCommodity.setRtgUom(commodity.getRatingUnitOfMeasure());
				shmCommodity.setShmShipment(shmShipment);
				shmCommodity.setSrceCd(commodity.getSourceCd());

				if (Objects.nonNull(commodity.getTariffsRate())) {
					shmCommodity.setTrfRt(BigDecimal.valueOf(commodity.getTariffsRate()));
				}
				if (Objects.nonNull(commodity.getVolumeCubicFeet())) {
					shmCommodity.setVolCft(BigDecimal.valueOf(commodity.getVolumeCubicFeet()));
				}
				if (Objects.nonNull(commodity.getWeightLbs())) {
					shmCommodity.setWgtLbs(BigDecimal.valueOf(commodity.getWeightLbs()));
				}
				shmCommodity.setShmShpCmdyAssns(null);
				result.add(shmCommodity);
			}
		});

		return result;
	}

	protected void setCommodityDefaultValues(ShmCommodity shmCommodity) {
		if (Objects.isNull(shmCommodity.getAmt())) {
			shmCommodity.setAmt(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmCommodity.getPcsCnt())) {
			shmCommodity.setPcsCnt(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmCommodity.getPpdPct())) {
			shmCommodity.setPpdPct(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmCommodity.getRdcdWgt())) {
			shmCommodity.setRdcdWgt(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmCommodity.getRtgQty())) {
			shmCommodity.setRtgQty(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmCommodity.getTrfRt())) {
			shmCommodity.setTrfRt(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmCommodity.getVolCft())) {
			shmCommodity.setVolCft(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmCommodity.getWgtLbs())) {
			shmCommodity.setWgtLbs(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmCommodity.getArchiveCntlCd())) {
			shmCommodity.setArchiveCntlCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getAsRatedClassCd())) {
			shmCommodity.setAsRatedClassCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getChrgToCd())) {
			shmCommodity.setChrgToCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getClassTyp())) {
			shmCommodity.setClassTyp(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getDescTxt())) {
			shmCommodity.setDescTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getDfltClassSlctInd())) {
			shmCommodity.setDfltClassSlctInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getFrzbleInd())) {
			shmCommodity.setFrzbleInd(getFlag(false));
		}

		if (Objects.isNull(shmCommodity.getHzMtInd())) {
			shmCommodity.setHzMtInd(getFlag(false));
		}
		if (Objects.isNull(shmCommodity.getMinChrgInd())) {
			shmCommodity.setMinChrgInd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getMixClssCmdyInd())) {
			shmCommodity.setMixClssCmdyInd(getFlag(false));
		}
		if (Objects.isNull(shmCommodity.getNmfcItmCd())) {
			shmCommodity.setNmfcItmCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getOriglDescTxt())) {
			shmCommodity.setOriglDescTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getRtgUom())) {
			shmCommodity.setRtgUom(StringUtils.SPACE);
		}
		if (Objects.isNull(shmCommodity.getSrceCd())) {
			shmCommodity.setSrceCd(StringUtils.SPACE);
		}

	}

	public String getCommodityClassCd(String name) {
		Field[] statusDeclaredFields = CommodityClassCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}

	public String getCommodityPackageCd(String name) {
		Field[] statusDeclaredFields = CommodityPackageCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}

	@LogExecutionTime
	protected List<String> compareCommodity(ShmCommodity source, ShmCommodity target) {
		Comparator<ShmCommodity> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmCommodity.class.getName(), differences, source.getId().getShpInstId());
		return differences;
	}

}
