package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmTmDtCritical;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.TimeDateCritical;
import com.xpo.ltl.java.util.cityoperations.enums.TimeDateCriticalDateTypeCd;
import com.xpo.ltl.java.util.cityoperations.enums.TimeDateCriticalTimeTypeCd;
import com.xpo.ltl.shipment.service.dao.ShipmentTdcSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.Comparator;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.ComparatorFactory;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.xpo.ltl.shipment.service.impl.updateshipment.comparator.EntityComparer.findDifferences;
import static com.xpo.ltl.shipment.service.util.TimestampUtil.getDate;

public abstract class AbstractTimeDateCritical extends AbstractUpdate {

	/*
	* Date Format: MM/DD/YYYY
	Time Format: HH24:MM
	* */
	private static final String DEFAULT_DATE = "0001-01-01";
	private static final String DEFAUL_TIME = "00:00:00";
	protected static final String TMDTCRITICAL = "TmDtCritical";
	private static final Logger logger = LogManager.getLogger(AbstractTimeDateCritical.class);
	@Inject
	protected ShipmentTdcSubDAO shipmentTdcSubDAO;
	@LogExecutionTime
	public void updateTimeDateCriticals(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmTmDtCritical> shmTmDtCriticals,
			String transactionCd) throws ValidationException {
		if (CollectionUtils.isNotEmpty(shmTmDtCriticals)) {

			shmTmDtCriticals.forEach(shmTmDtCritical -> {
				shmTmDtCritical.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmTmDtCritical.setLstUpdtTranCd(transactionCd);
				shmTmDtCritical.setLstUpdtUid(getUserFromContext(transactionContext));

			});
			shipmentTdcSubDAO.persist(shmTmDtCriticals, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				shmTmDtCriticals.forEach(shmTmDtCritical -> {
					try {
						final Function<DB2ShmTmDtCritical, Boolean> checkVersionFunctionShmRemark = getCheckVersionFunctionShmTmDtCritical(
								new Timestamp(new Date().getTime()));
						shipmentTdcSubDAO.updateDB2ShmTmDtCritical(shmTmDtCritical,
								checkVersionFunctionShmRemark,
								db2EntityManager,
								transactionContext);
					}  catch (Exception e) {
						logger.error(e);
						getException(ServiceErrorMessage.TDC_UPDT_FAILED, TMDTCRITICAL, e, transactionContext);
					}
				});
			}
		}
	}
	@LogExecutionTime
	public void addTimeDateCriticals(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmTmDtCritical> shmTmDtCriticalsToAdd,
			String transactionCd) {
		if (CollectionUtils.isNotEmpty(shmTmDtCriticalsToAdd)) {
			shmTmDtCriticalsToAdd.forEach(shmTmDtCritical -> {
				shmTmDtCritical.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmTmDtCritical.setLstUpdtTranCd(transactionCd);
				shmTmDtCritical.setLstUpdtUid(getUserFromContext(transactionContext));
			});
			try {
				shipmentTdcSubDAO.persist(shmTmDtCriticalsToAdd, entityManager);
				if (appContext.getDb2CommitEnabledForUpdateShipment()) {
					shmTmDtCriticalsToAdd.forEach(shmRemark -> {
						try {
							shipmentTdcSubDAO.createDB2ShmTmDtCritical(setDefaultShmTmDtCriticalValues(shmRemark),
									db2EntityManager);
						} catch (ParseException e) {
							throw new IllegalStateException(e);
						}
					});
				}
			} catch (Exception e) {
				logger.error(e);
				getException(ServiceErrorMessage.TDC_CREATE_FAILED, TMDTCRITICAL, e, transactionContext);
			}
		}
	}
	public Function<DB2ShmTmDtCritical, Boolean> getCheckVersionFunctionShmTmDtCritical(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}
	protected List<ShmTmDtCritical> getAbcShmTmDtCriticalForInsert(
			Long shipmentInstId,
			TimeDateCritical timeDateCritical,
			ShmTmDtCritical shmTmDtCritical,
			TransactionContext transactionContext) {
		List<ShmTmDtCritical> result = new ArrayList<>();

		if (Objects.nonNull(timeDateCritical) && Objects.isNull(shmTmDtCritical)) {

			try {
				ShmTmDtCritical tmDtCritical = new ShmTmDtCritical();
				DtoTransformer.toShmTmDtCritical(timeDateCritical, tmDtCritical);
				tmDtCritical.setTdcDtTypCd(timeDateCritical.getTdcDateTypeCd());
				tmDtCritical.setTdcTmTypCd(timeDateCritical.getTdcTimeTypeCd());

				setDateCritical(tmDtCritical, timeDateCritical);
				setTimeCritical(tmDtCritical, timeDateCritical);
				tmDtCritical.setShpInstId(shipmentInstId);
				setDefaultShmTmDtCriticalValues(tmDtCritical);
				result.add(tmDtCritical);
			} catch (ServiceException | ParseException e) {
				logger.error(e);
				getException(ServiceErrorMessage.TDC_CREATE_FAILED, TMDTCRITICAL, e, transactionContext);
			}

		}

		return result;
	}

	public ShmTmDtCritical setDefaultShmTmDtCriticalValues(ShmTmDtCritical shmTmDtCritical) throws ParseException {

		if (Objects.isNull(shmTmDtCritical.getTdcSrceCd())) {
			shmTmDtCritical.setTdcSrceCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmTmDtCritical.getTdcTmTypCd())) {
			shmTmDtCritical.setTdcTmTypCd(StringUtils.SPACE);
		}
		if (Objects.isNull(shmTmDtCritical.getTdcDt2())) {
			shmTmDtCritical.setTdcDt2(getDate(DEFAULT_DATE, DATE_PATTERN));
		}
		if (Objects.isNull(shmTmDtCritical.getTdcTm1())) {
			shmTmDtCritical.setTdcTm1(getDate(DEFAUL_TIME, TIME_PATTERN));
		}
		if (Objects.isNull(shmTmDtCritical.getTdcTm2())) {
			shmTmDtCritical.setTdcTm2(getDate(DEFAUL_TIME, TIME_PATTERN));
		}
		return shmTmDtCritical;
	}

	protected List<String> compareShmTmDtCritical(ShmTmDtCritical source, ShmTmDtCritical target) {
		Comparator<ShmTmDtCritical> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmRemark.class.getName(), differences, source.getShpInstId());
		return differences;
	}

	@LogExecutionTime
	public List<ShmTmDtCritical> getAbcShmTmDtCriticalForUpdate(
			TimeDateCritical timeDateCritical,
			ShmTmDtCritical shmTmDtCritical,
			String userId,
			String transactionCd,
			TransactionContext transactionContext) throws ParseException {
		List<ShmTmDtCritical> result = new ArrayList<>();

		if (Objects.nonNull(timeDateCritical) && Objects.nonNull(shmTmDtCritical)
				&& shmTmDtCritical.getShpInstId() == timeDateCritical.getShipmentInstId()) {
			ShmTmDtCritical shmTmDtCriticalToCheck = new ShmTmDtCritical();
			copyFields(shmTmDtCritical, shmTmDtCriticalToCheck);

			try {
				shmTmDtCritical.setTdcDtTypCd(timeDateCritical.getTdcDateTypeCd());
				setDateCritical(shmTmDtCritical, timeDateCritical);

				shmTmDtCritical.setTdcTmTypCd(timeDateCritical.getTdcTimeTypeCd());
				setTimeCritical(shmTmDtCritical, timeDateCritical);

			} catch (ParseException e) {
				logger.error(e);
				getException(ServiceErrorMessage.TDC_UPDT_FAILED, TMDTCRITICAL, e, transactionContext);
			}

			List<String> diff = this.compareShmTmDtCritical(shmTmDtCritical, shmTmDtCriticalToCheck);

			if (CollectionUtils.isNotEmpty(diff)) {
				shmTmDtCritical.setLstUpdtTranCd(transactionCd);
				shmTmDtCritical.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmTmDtCritical.setLstUpdtUid(userId);
				setDefaultShmTmDtCriticalValues(shmTmDtCritical);
				result.add(shmTmDtCritical);
			}

		}

		return result;
	}

	/**
	 * Sets the critical date information on the provided ShmTmDtCritical object
	 * based on the provided TimeDateCritical data.
	 * This method performs the following operations:
	 * - Sets tdcDt1 in ShmTmDtCritical using the tdcDate1 from TimeDateCritical.
	 * - If the date type code tdcDateTypeCd from TimeDateCritical is RANGE, sets
	 *   tdcDt2 in ShmTmDtCritical using tdcDate2 from TimeDateCritical.
	 * - If the date type code tdcDateTypeCd from TimeDateCritical is BY or ON,
	 *   sets tdcDt2 in ShmTmDtCritical using a default date.
	 *
	 * @param shmTmDtCritical     the ShmTmDtCritical object to be modified
	 * @param timeDateCritical    the TimeDateCritical object containing the
	 *                            critical time and date data
	 * @throws ParseException     if parsing the date string fails
	 * @see ShmTmDtCritical
	 * @see TimeDateCritical
	 * @see TimeDateCriticalDateTypeCd
	 */

	private void setDateCritical(
			ShmTmDtCritical shmTmDtCritical,
			TimeDateCritical timeDateCritical) throws ParseException {

		shmTmDtCritical.setTdcDt1(getDate(timeDateCritical.getTdcDate1(), DATE_PATTERN));
		if (TimeDateCriticalDateTypeCd.RANGE.getCode().equals(timeDateCritical.getTdcDateTypeCd())) {
			shmTmDtCritical.setTdcDt2(getDate(timeDateCritical.getTdcDate2(), DATE_PATTERN));
		} else if (TimeDateCriticalDateTypeCd.BY.getCode().equals(timeDateCritical.getTdcDateTypeCd())
				|| TimeDateCriticalDateTypeCd.ON.getCode().equals(timeDateCritical.getTdcDateTypeCd())) {
			shmTmDtCritical.setTdcDt2(getDate(DEFAULT_DATE, DATE_PATTERN));
		}
	}

	private void setTimeCritical(
			ShmTmDtCritical shmTmDtCritical, TimeDateCritical timeDateCritical) throws ParseException {

		Date defaultTime = getDate(DEFAUL_TIME, TIME_PATTERN);
		if (TimeDateCriticalTimeTypeCd.BEFORE.getCode().equals(timeDateCritical.getTdcTimeTypeCd())) {
			shmTmDtCritical.setTdcTm1(getDate(timeDateCritical.getTdcTime1(), TIME_PATTERN));
			shmTmDtCritical.setTdcTm2(defaultTime);
		} else if (TimeDateCriticalTimeTypeCd.RANGE.getCode().equals(timeDateCritical.getTdcTimeTypeCd())
				|| TimeDateCriticalTimeTypeCd.AT.getCode().equals(timeDateCritical.getTdcTimeTypeCd())) {
			shmTmDtCritical.setTdcTm1(getDate(timeDateCritical.getTdcTime1(), TIME_PATTERN));
			shmTmDtCritical.setTdcTm2(getDate(timeDateCritical.getTdcTime2(), TIME_PATTERN));
		} else if (TimeDateCriticalTimeTypeCd.AFTER.getCode().equals(timeDateCritical.getTdcTimeTypeCd()) && (
				TimeDateCriticalDateTypeCd.ON.getCode().equals(timeDateCritical.getTdcDateTypeCd())
						|| TimeDateCriticalDateTypeCd.RANGE.getCode().equals(timeDateCritical.getTdcDateTypeCd()))) {
			shmTmDtCritical.setTdcTm1(getDate(timeDateCritical.getTdcTime1(), TIME_PATTERN));
			shmTmDtCritical.setTdcTm2(defaultTime);
		} else {
			shmTmDtCritical.setTdcTm1(defaultTime);
			shmTmDtCritical.setTdcTm2(defaultTime);
		}
	}
	protected List<ShmTmDtCritical> getAbcShmTmDtCriticalForDelete(TimeDateCritical timeDateCritical, ShmTmDtCritical shmTmDtCritical) {
		List<ShmTmDtCritical> result = new ArrayList<>();
		if (Objects.isNull(timeDateCritical) && Objects.nonNull(shmTmDtCritical)){
			result.add(shmTmDtCritical);
		}
		return result;
	}
}
