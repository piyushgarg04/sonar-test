package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentCustomsSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentTdcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AcSvcUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AdvBydCarrAllTxUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CommodityTransactions;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CustomsBondUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.MiscLineItemsUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.RemarkAllTxUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.UpdateShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.ShipmentUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAcSvcFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAdvBydCarrFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateCommodityFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateCustomsBondFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateMiscLineItemFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateRemarkFactory;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@TransactionManagement(TransactionManagementType.BEAN)
@RequestScoped
public class UpdateCorrectionImpl extends AbstractUpdate implements UpdateShipment {

	private static final Logger logger = LogManager.getLogger(UpdateCorrectionImpl.class);

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShipmentRemarkSubDAO shipmentRemarkSubDAO;
	@Inject
	private ShipmentAcSvcSubDAO shipmentAcSvcSubDAO;
	@Inject
	private ShmCommoditySubDAO shmCommoditySubDAO;
	@Inject
	private ShipmentAdvBydSubDAO shipmentAdvBydSubDAO;
	@Inject
	private ShipmentCustomsSubDAO shipmentCustomsSubDAO;
	@Inject
	private ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;
	@Inject
	private ShipmentTdcSubDAO shipmentTdcSubDAO;
	@Inject
	private UpdateMiscLineItemFactory updateMiscLineItemFactory;
	@Inject
	private UpdateAdvBydCarrFactory updateAdvBydCarrFactory;
	@Inject
	private UpdateAcSvcFactory updateAcSvcFactory;
	@Inject
	private UpdateCommodityFactory updateCommodityFactory;
	@Inject
	private UpdateRemarkFactory updateRemarkFactory;
	@Inject
	private ShipmentUpdateCommonImpl updateShipment;
	@Inject
	private UpdateCustomsBondFactory updateCustomsBondFactory;
	@Inject
	private CorrectionAsMatchedPartyUpdateImpl correctionAsMatchedPartyUpdate;
	@Inject
	private CorrectionTimeDateCriticalUpdateImpl timeDateCriticalUpdateCommon;


	@Transactional(Transactional.TxType.MANDATORY)
	@Override
	public void update(
			UpdateShipmentRqst updateShipmentRqst,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			ShmShipment shipment,
			List<ShmMiscLineItem> shmMiscLineItems,
			EntityManager entityManager,
			TransactionContext transactionContext) throws ServiceException {
		logger.info("{} {}{}{} {}", "INIT", this.getClass().getSimpleName(), ":", "update", shipmentUpdateActionCd);
		List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds = new ArrayList<>();

		shipmentRemarkTypeCds.add(ShipmentRemarkTypeCd.SHIPPING_RMK);

		String userId = getUserFromContext(transactionContext);

		List<ShmRemark> shmRemarkOriginals = shipment.getShmRemarks();
		List<ShmCommodity> shmCommodity = shipment.getShmCommodities();
		List<ShmAcSvc> shmAcSvcs = shipment.getShmAcSvcs();
		List<ShmAdvBydCarr> advBydCarrs = shipment.getShmAdvBydCarrs();
		List<ShmCustomsBond> shmCustomsBondOriginal = shipment.getShmCustomsBonds();
		List<ShmAsEntdCust> shmAsEntdCusts = shipmentAsEnteredCustomerDAO.findByShpInstIds(Collections.singletonList(
				shipment.getShpInstId()), entityManager);
		ShmTmDtCritical shmTmDtCritical = shipmentTdcSubDAO.findById(shipment.getShpInstId(), entityManager);

		ObjectMapper objectMapper = new ObjectMapper();
		// Configure ObjectMapper
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		RemarkAllTxUpdate remarkAllTxUpdate = (RemarkAllTxUpdate) updateRemarkFactory.getUpdateImplementation(
				shipmentUpdateActionCd);
		MiscLineItemsUpdate miscLineItemsUpdate = updateMiscLineItemFactory.getUpdateImplementation(
				shipmentUpdateActionCd);
		AcSvcUpdate acSvcUpdate = updateAcSvcFactory.getUpdateImplementation(shipmentUpdateActionCd);
		CommodityTransactions commodityTransactions = (CommodityTransactions) updateCommodityFactory.getUpdateImplementation(
				shipmentUpdateActionCd);
		AdvBydCarrAllTxUpdate advBydCarrAllTxUpdate = (AdvBydCarrAllTxUpdate) updateAdvBydCarrFactory.getUpdateImplementation(
				shipmentUpdateActionCd);
		CustomsBondUpdate customsBondUpdate = updateCustomsBondFactory.getUpdateImplementation(shipmentUpdateActionCd);
		CompletableFuture<Map<ActionCd, List<ShmTmDtCritical>>> cfDateCriticalsForTransactions = timeDateCriticalUpdateCommon.cfGetTimeDateCriticalsForTransactions(
				updateShipmentRqst.getTimeDateCritical(),
				shmTmDtCritical,
				userId,
				shipment,
				CORRECTION_TRAN_CD,
				transactionContext);

		CompletableFuture<Map<ActionCd, List<ShmAsEntdCust>>> cfAsMatchedPartiesForTransactions = correctionAsMatchedPartyUpdate.cfGetAsMatchedPartiesForTransactions(
				updateShipmentRqst.getAsMatchedParties(),
				shmAsEntdCusts,
				userId,
				shipment,
				CORRECTION_TRAN_CD,
				transactionContext);
		CompletableFuture<Map<ActionCd, List<ShmCustomsBond>>> cfCustomsBondForTransactions = customsBondUpdate.cfGetCustomsBondForTransactions(
				shipment.getShpInstId(),
				updateShipmentRqst,
				shmCustomsBondOriginal,
				shipment,
				CORRECTION_TRAN_CD,
				transactionContext);

		CompletableFuture<Map<ActionCd, List<ShmRemark>>> cfRemarksForTransactions = remarkAllTxUpdate.cfGetRemarksForTransactions(
				updateShipmentRqst.getShipmentRemarks(),
				shmRemarkOriginals,
				userId,
				shipmentRemarkTypeCds,
				shipment,
				CORRECTION_TRAN_CD,
				transactionContext);

		CompletableFuture<Map<ActionCd, List<ShmMiscLineItem>>> cfActionCdListMap = miscLineItemsUpdate.cfGetItemsForTransactions(
				updateShipmentRqst.getShipment().getShipmentInstId(),
				updateShipmentRqst.getMiscLineItems(),
				shmMiscLineItems,
				shipment,
				userId,
				transactionContext,
				shipmentUpdateActionCd);

		CompletableFuture<Map<ActionCd, List<ShmCommodity>>> cfMapCommodities = commodityTransactions.cfGetAllTx(shmCommodity,
				shipment,
				updateShipmentRqst,
				userId,
				CORRECTION_TRAN_CD,
				transactionContext,
				shipmentUpdateActionCd);

		CompletableFuture<Map<ActionCd, List<ShmAdvBydCarr>>> cfActionCdListAdvBydCarrMap = advBydCarrAllTxUpdate.cfGetAllTx(shipment,
				updateShipmentRqst,
				userId,
				advBydCarrs,
				CORRECTION_TRAN_CD,
				transactionContext);

		CompletableFuture<Map<ActionCd, List<ShmAcSvc>>> cfActionCdListAcSvcMap = acSvcUpdate.cfGetItemsForTransactions(
				updateShipmentRqst.getShipment().getShipmentInstId(),
				updateShipmentRqst.getAccessorialServices(),
				shmAcSvcs,
				shipment,
				userId,
				shipmentUpdateActionCd,
				CORRECTION_TRAN_CD,
				transactionContext);

		CompletableFuture.allOf(cfAsMatchedPartiesForTransactions,
				cfCustomsBondForTransactions,
				cfRemarksForTransactions,
				cfActionCdListMap,
				cfMapCommodities,
				cfActionCdListAdvBydCarrMap,
				cfActionCdListAcSvcMap,
				cfDateCriticalsForTransactions).join();

		Map<ActionCd, List<ShmAsEntdCust>> asMatchedPartiesForTransactions = retrieveData(cfAsMatchedPartiesForTransactions,
				transactionContext);
		Map<ActionCd, List<ShmCustomsBond>> customsBondForTransactions = retrieveData(cfCustomsBondForTransactions,
				transactionContext);
		Map<ActionCd, List<ShmRemark>> remarksForTransactions = retrieveData(cfRemarksForTransactions,
				transactionContext);
		Map<ActionCd, List<ShmMiscLineItem>> actionCdListMap = retrieveData(cfActionCdListMap, transactionContext);
		Map<ActionCd, List<ShmCommodity>> mapCommodities = retrieveData(cfMapCommodities, transactionContext);
		Map<ActionCd, List<ShmAdvBydCarr>> actionCdListAdvBydCarrMap = retrieveData(cfActionCdListAdvBydCarrMap,
				transactionContext);
		Map<ActionCd, List<ShmAcSvc>> actionCdListAcSvcMap = retrieveData(cfActionCdListAcSvcMap, transactionContext);
		Map<ActionCd, List<ShmTmDtCritical>> dateCriticalsForTransactions = retrieveData(cfDateCriticalsForTransactions,
				transactionContext);

		try {

			updateShipment.update(updateShipmentRqst,
					entityManager,
					db2EntityManager,
					transactionContext,
					shipment,
					CORRECTION_TRAN_CD,
					EventLogTypeCd.CORRECTIONS,
					EventLogSubTypeCd.RATE_AUDITOR_RATED,
					shipmentUpdateActionCd);

			commodityTransactions.delete(entityManager,
					db2EntityManager,
					transactionContext,
					mapCommodities.get(ActionCd.DELETE));
			commodityTransactions.insert(entityManager,
					db2EntityManager,
					transactionContext,
					mapCommodities.get(ActionCd.ADD),
					CORRECTION_TRAN_CD,
					userId);
			commodityTransactions.update(entityManager,
					db2EntityManager,
					transactionContext,
					mapCommodities.get(ActionCd.UPDATE),
					CORRECTION_TRAN_CD,
					userId);

			miscLineItemsUpdate.delete(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListMap.get(ActionCd.DELETE));
			miscLineItemsUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListMap.get(ActionCd.ADD),
					CORRECTION_TRAN_CD,
					userId);
			miscLineItemsUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListMap.get(ActionCd.UPDATE),
					CORRECTION_TRAN_CD,
					userId);
			acSvcUpdate.delete(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAcSvcMap.get(ActionCd.DELETE));
			acSvcUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAcSvcMap.get(ActionCd.ADD),
					CORRECTION_TRAN_CD,
					userId);
			acSvcUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAcSvcMap.get(ActionCd.UPDATE),
					CORRECTION_TRAN_CD,
					userId);

			advBydCarrAllTxUpdate.delete(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAdvBydCarrMap.get(ActionCd.DELETE));
			advBydCarrAllTxUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAdvBydCarrMap.get(ActionCd.ADD),
					CORRECTION_TRAN_CD);
			advBydCarrAllTxUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					actionCdListAdvBydCarrMap.get(ActionCd.UPDATE),
					CORRECTION_TRAN_CD);

			remarkAllTxUpdate.delete(entityManager,
					db2EntityManager,
					transactionContext,
					remarksForTransactions.get(ActionCd.DELETE));
			remarkAllTxUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					remarksForTransactions.get(ActionCd.ADD),
					CORRECTION_TRAN_CD);
			remarkAllTxUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					remarksForTransactions.get(ActionCd.UPDATE),
					CORRECTION_TRAN_CD);

			customsBondUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					customsBondForTransactions.get(ActionCd.UPDATE),
					CORRECTION_TRAN_CD);
			customsBondUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					customsBondForTransactions.get(ActionCd.ADD),
					CORRECTION_TRAN_CD);

			correctionAsMatchedPartyUpdate.delete(entityManager,
					db2EntityManager,
					transactionContext,
					asMatchedPartiesForTransactions.get(ActionCd.DELETE));
			correctionAsMatchedPartyUpdate.insert(entityManager,
					db2EntityManager,
					transactionContext,
					asMatchedPartiesForTransactions.get(ActionCd.ADD),
					CORRECTION_TRAN_CD);
			correctionAsMatchedPartyUpdate.update(entityManager,
					db2EntityManager,
					transactionContext,
					asMatchedPartiesForTransactions.get(ActionCd.UPDATE),
					CORRECTION_TRAN_CD);
			timeDateCriticalUpdateCommon.delete(entityManager,
					db2EntityManager,
					transactionContext,
					dateCriticalsForTransactions.get(ActionCd.DELETE));
			timeDateCriticalUpdateCommon.insert(entityManager,
					db2EntityManager,
					transactionContext,
					dateCriticalsForTransactions.get(ActionCd.ADD),
					CORRECTION_TRAN_CD);
			timeDateCriticalUpdateCommon.update(entityManager,
					db2EntityManager,
					transactionContext,
					dateCriticalsForTransactions.get(ActionCd.UPDATE),
					CORRECTION_TRAN_CD);


		} catch (ServiceException e) {
			getException(ServiceErrorMessage.UNHANDLED_SERVICE_EXCEPTION,
					this.getClass().getSimpleName(),
					e,
					transactionContext);

		}
		logger.info("{} {}{}{}", "END", this.getClass().getSimpleName(), ":", "update");
	}

}
