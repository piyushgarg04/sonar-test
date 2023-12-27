package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AcSvcUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CommodityTransactions;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.MiscLineItemsUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.UpdateShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.ShipmentUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAcSvcFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateCommodityFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateMiscLineItemFactory;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@TransactionManagement(TransactionManagementType.BEAN)
@RequestScoped
public class UpdateAutoRateImpl extends AbstractUpdate implements UpdateShipment {

	private static final Logger logger = LogManager.getLogger(UpdateAutoRateImpl.class);

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;
	@Inject
	private ShipmentAcSvcSubDAO shipmentAcSvcSubDAO;
	@Inject
	private ShmCommoditySubDAO shmCommoditySubDAO;
	@Inject
	private UpdateMiscLineItemFactory updateMiscLineItemFactory;
	@Inject
	private UpdateAcSvcFactory updateAcSvcFactory;
	@Inject
	private UpdateCommodityFactory updateCommodityFactory;
	@Inject
	private ShipmentUpdateCommonImpl updateShipment;

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

		String userId = getUserForAutoRate(updateShipmentRqst,transactionContext);
		boolean rateAuditIncompleteInd = Objects.nonNull(updateShipmentRqst.getRateAuditIncompleteInd()) ? updateShipmentRqst.getRateAuditIncompleteInd() : false;
		EventLogSubTypeCd eventLogSubTypeCd = null;

		if (updateShipmentRqst.getRateAuditIncompleteInd() != null) {
			eventLogSubTypeCd = updateShipmentRqst.getRateAuditIncompleteInd() ?
					EventLogSubTypeCd.RATE_AUDITOR_UNRATED :
					EventLogSubTypeCd.RATE_AUDITOR_RATED;
		} else {
			eventLogSubTypeCd = EventLogSubTypeCd.RATE_AUDITOR_RATED;
		}
		if (rateAuditIncompleteInd){

			updateShipment.update(updateShipmentRqst,
					entityManager,
					db2EntityManager,
					transactionContext,
					shipment,
					AUTORATE_TRAN_CD,
					EventLogTypeCd.RATING,
					eventLogSubTypeCd,
					shipmentUpdateActionCd);
		}else{

			List<ShmCommodity> shmCommodity = shipment.getShmCommodities();
			List<ShmAcSvc> shmAcSvcs = shipment.getShmAcSvcs();

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

			MiscLineItemsUpdate miscLineItemsUpdate = updateMiscLineItemFactory.getUpdateImplementation(
					shipmentUpdateActionCd);
			AcSvcUpdate acSvcUpdate = updateAcSvcFactory.getUpdateImplementation(shipmentUpdateActionCd);
			CommodityTransactions commodityTransactions = updateCommodityFactory.getUpdateImplementation(
					shipmentUpdateActionCd);

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
					AUTORATE_TRAN_CD,
					transactionContext,
					shipmentUpdateActionCd);


			CompletableFuture<Map<ActionCd, List<ShmAcSvc>>> cfActionCdListAcSvcMap = acSvcUpdate.cfGetItemsForTransactions(
					updateShipmentRqst.getShipment().getShipmentInstId(),
					updateShipmentRqst.getAccessorialServices(),
					shmAcSvcs,
					shipment,
					userId,
					shipmentUpdateActionCd,
					AUTORATE_TRAN_CD,
					transactionContext);

			CompletableFuture.allOf(
					cfActionCdListMap,
					cfMapCommodities,
					cfActionCdListAcSvcMap).join();


			Map<ActionCd, List<ShmMiscLineItem>> actionCdListMap = retrieveData(cfActionCdListMap, transactionContext);
			Map<ActionCd, List<ShmCommodity>> mapCommodities = retrieveData(cfMapCommodities, transactionContext);
			Map<ActionCd, List<ShmAcSvc>> actionCdListAcSvcMap = retrieveData(cfActionCdListAcSvcMap, transactionContext);

			try {


				updateShipment.update(updateShipmentRqst,
						entityManager,
						db2EntityManager,
						transactionContext,
						shipment,
						AUTORATE_TRAN_CD,
						EventLogTypeCd.RATING,
						eventLogSubTypeCd,
						shipmentUpdateActionCd);

				commodityTransactions.delete(entityManager,
						db2EntityManager,
						transactionContext,
						mapCommodities.get(ActionCd.DELETE));
				commodityTransactions.insert(entityManager,
						db2EntityManager,
						transactionContext,
						mapCommodities.get(ActionCd.ADD),
						AUTORATE_TRAN_CD,
						userId);
				commodityTransactions.update(entityManager,
						db2EntityManager,
						transactionContext,
						mapCommodities.get(ActionCd.UPDATE),
						AUTORATE_TRAN_CD,
						userId);

				miscLineItemsUpdate.delete(entityManager,
						db2EntityManager,
						transactionContext,
						actionCdListMap.get(ActionCd.DELETE));
				miscLineItemsUpdate.insert(entityManager,
						db2EntityManager,
						transactionContext,
						actionCdListMap.get(ActionCd.ADD),
						AUTORATE_TRAN_CD,
						userId);
				miscLineItemsUpdate.update(entityManager,
						db2EntityManager,
						transactionContext,
						actionCdListMap.get(ActionCd.UPDATE),
						AUTORATE_TRAN_CD,
						userId);

				acSvcUpdate.delete(entityManager,
						db2EntityManager,
						transactionContext,
						actionCdListAcSvcMap.get(ActionCd.DELETE));
				acSvcUpdate.insert(entityManager,
						db2EntityManager,
						transactionContext,
						actionCdListAcSvcMap.get(ActionCd.ADD),
						AUTORATE_TRAN_CD,
						userId);

			} catch (ServiceException e) {
				getException(ServiceErrorMessage.UNHANDLED_SERVICE_EXCEPTION,
						this.getClass().getSimpleName(),
						e,
						transactionContext);

			}
		}


		logger.info("{} {}{}{}", "END", this.getClass().getSimpleName(), ":", "update");
	}

}
