package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AcSvcUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AdvBydCarrUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.MiscLineItemsUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.RemarkUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.UpdateShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.ShipmentUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAcSvcFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAdvBydCarrFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateMiscLineItemFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateRemarkFactory;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@TransactionManagement(TransactionManagementType.BEAN)
@RequestScoped
public class UpdateManRateImpl extends AbstractUpdate implements UpdateShipment {

	private static final Logger logger = LogManager.getLogger(UpdateManRateImpl.class);
	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShipmentAdvBydSubDAO shipmentAdvBydSubDAO;

	@Inject
	private ShipmentRemarkSubDAO shipmentRemarkSubDAO;

	@Inject
	private ShipmentAcSvcSubDAO shipmentAcSvcSubDAO;

	@Inject
	private UpdateMiscLineItemFactory updateMiscLineItemFactory;
	@Inject
	private UpdateAcSvcFactory updateAcSvcFactory;

	@Inject
	private UpdateAdvBydCarrFactory updateAdvBydCarrFactory;

	@Inject
	private ShmCommoditySubDAO shmCommoditySubDAO;

	@Inject
	private UpdateRemarkFactory updateRemarkFactory;

	@Inject
	private ShipmentUpdateCommonImpl updateShipment;

	@Inject
	private ManRateCommodityUpdImpl commodityUpdate;

	@Inject
	private ShmEventLogSubDAO shmEventLogSubDAO;
	@LogExecutionTime
	@Transactional(Transactional.TxType.MANDATORY)
	@Override
	public void update(
			UpdateShipmentRqst updateShipmentRqst,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			ShmShipment shipment,
			List<ShmMiscLineItem> shmMiscLineItems,
			EntityManager entityManager,
			TransactionContext transactionContext) throws ServiceException {

		logger.info("{} {}{}{}", "INIT", this.getClass().getSimpleName(), ":", "update");
		String userId = getUserFromContext(transactionContext);

		List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds = new ArrayList<>();
		shipmentRemarkTypeCds.add(ShipmentRemarkTypeCd.AUTHORITY_LN_TXT_RMK);
		shipmentRemarkTypeCds.add(ShipmentRemarkTypeCd.SHIPPING_RMK);

		List<ShmCommodity> shmCommodity = shipment.getShmCommodities();

		List<ShmAcSvc> shmAcSvcs = shipment.getShmAcSvcs();

		List<ShmAdvBydCarr> advBydCarrs = shipment.getShmAdvBydCarrs();

		List<ShmRemark> shmRemarkOriginals = shipment.getShmRemarks();

		AcSvcUpdate acSvcUpdate = updateAcSvcFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE);

		MiscLineItemsUpdate miscLineItemsUpdate = updateMiscLineItemFactory.getUpdateImplementation(
				ShipmentUpdateActionCd.MANUAL_RATE);



		AdvBydCarrUpdate advBydCarrUpdate = updateAdvBydCarrFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE);

		CompletableFuture<List<ShmCommodity>> commoditiesCf = commodityUpdate.getShmCommoditiesToUpdateCf(updateShipmentRqst,
				userId,
				shmCommodity,
				MANRATE_TRAN_CD);

		CompletableFuture<Map<ActionCd, List<ShmMiscLineItem>>> cfActionCdListMap = miscLineItemsUpdate.cfGetItemsForTransactions(
				updateShipmentRqst.getShipment().getShipmentInstId(),
				updateShipmentRqst.getMiscLineItems(),
				shmMiscLineItems,
				shipment,
				userId,
				transactionContext,
				shipmentUpdateActionCd);

		CompletableFuture<Map<ActionCd, List<ShmAcSvc>>> cfActionCdListAcSvcMap = acSvcUpdate.cfGetItemsForTransactions(
				updateShipmentRqst.getShipment().getShipmentInstId(),
				updateShipmentRqst.getAccessorialServices(),
				shmAcSvcs,
				shipment,
				userId,
				ShipmentUpdateActionCd.MANUAL_RATE,
				MANRATE_TRAN_CD,
				transactionContext);

		CompletableFuture<List<ShmAdvBydCarr>> cfShmAdvBydCarrs = advBydCarrUpdate.cfGetShmAdvBydCarrsToUpdate(updateShipmentRqst,
				userId,
				advBydCarrs,
				MANRATE_TRAN_CD);

		RemarkUpdate remarkUpdate = updateRemarkFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE);
		CompletableFuture<Map<ActionCd, List<ShmRemark>>> cfRemarksForTransactions = remarkUpdate.cfGetRemarksForTransactions(
				updateShipmentRqst.getShipmentRemarks(),
				shmRemarkOriginals,
				userId,
				shipmentRemarkTypeCds,
				shipment,
				MANRATE_TRAN_CD,
				transactionContext);

		CompletableFuture
				.allOf(cfActionCdListMap,
						cfActionCdListAcSvcMap,
						cfShmAdvBydCarrs,
						cfRemarksForTransactions,
						commoditiesCf)
				.join();

		List<ShmCommodity> commodities = retrieveData(commoditiesCf, transactionContext);
		Map<ActionCd, List<ShmMiscLineItem>> actionCdListMap = retrieveData(cfActionCdListMap, transactionContext);
		Map<ActionCd, List<ShmAcSvc>> actionCdListAcSvcMap = retrieveData(cfActionCdListAcSvcMap, transactionContext);
		List<ShmAdvBydCarr> shmAdvBydCarrs = retrieveData(cfShmAdvBydCarrs, transactionContext);
		Map<ActionCd, List<ShmRemark>> remarksForTransactions = retrieveData(cfRemarksForTransactions,
				transactionContext);
		EventLogSubTypeCd eventLogSubTypeCd = getEventLogSubTypeCd(shipment.getShpInstId(), entityManager);
		try {

			updateShipment.update(updateShipmentRqst,
					entityManager,
					db2EntityManager,
					transactionContext,
					shipment,
					MANRATE_TRAN_CD,
					EventLogTypeCd.RATING,
					eventLogSubTypeCd,
					ShipmentUpdateActionCd.MANUAL_RATE);

			commodityUpdate.update(entityManager, db2EntityManager, transactionContext, commodities, MANRATE_TRAN_CD,userId);

			miscLineItemsUpdate.delete(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListMap.get(ActionCd.DELETE));
			miscLineItemsUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListMap.get(ActionCd.ADD),
					MANRATE_TRAN_CD,
					userId);
			miscLineItemsUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListMap.get(ActionCd.UPDATE),
					MANRATE_TRAN_CD,
					userId);

			acSvcUpdate.delete(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAcSvcMap.get(ActionCd.DELETE));
			acSvcUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAcSvcMap.get(ActionCd.ADD),
					MANRATE_TRAN_CD,
					userId);
			acSvcUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAcSvcMap.get(ActionCd.UPDATE),
					MANRATE_TRAN_CD,
					userId);

			advBydCarrUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					shmAdvBydCarrs,
					MANRATE_TRAN_CD);

			remarkUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					remarksForTransactions.get(ActionCd.UPDATE),
					MANRATE_TRAN_CD);
			remarkUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					remarksForTransactions.get(ActionCd.ADD),
					MANRATE_TRAN_CD);

		} catch (ServiceException e) {
			getException(ServiceErrorMessage.UNEXPECTED_EXCEPTION, "MANRATE-UPDATE", e, transactionContext);
		}

		logger.info("{} {}{}{}", "END", this.getClass().getSimpleName(), ":", "update");

	}

	private EventLogSubTypeCd getEventLogSubTypeCd(long shpInstId, EntityManager entityManager) {
		EventLogSubTypeCd result;
		List<ShmEventLog> shmEventLogs = shmEventLogSubDAO.findByShmInstId(shpInstId, entityManager);
		if (CollectionUtils.isEmpty(shmEventLogs)){
			return EventLogSubTypeCd.AUTO_RATED;
		}else{
			result = shmEventLogs.stream().anyMatch(shmEventLog -> shmEventLog.getSubTypCd().equals(getEventLogSubTypeCdCdAlt(EventLogSubTypeCd.AUTO_RATED.value())) && shmEventLog.getTypCd().equals(getEventLogTypeCdAlt(EventLogTypeCd.RATING.value()))) ? EventLogSubTypeCd.RATE_AUDITOR_RATED : EventLogSubTypeCd.AUTO_RATED;
		}
		return result;
	}

}
