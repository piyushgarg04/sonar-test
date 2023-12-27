package com.xpo.ltl.shipment.service.ejb.v1;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.ws.rs.ServiceUnavailableException;
import javax.xml.datatype.XMLGregorianCalendar;

import com.xpo.ltl.shipment.service.impl.updateshipment.UpdateShipmentImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.ListMetadata;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.v2.ShipmentServiceIF;
import com.xpo.ltl.api.shipment.v2.*;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dto.InternalCreateNonRevenueShipmentResponseDTO;
import com.xpo.ltl.shipment.service.impl.BulkUpsertHandlingUnitsImpl;
import com.xpo.ltl.shipment.service.impl.CalculateServiceStandardForShipmentsImpl;
import com.xpo.ltl.shipment.service.impl.CreateAndArchiveCopyBillDocumentImpl;
import com.xpo.ltl.shipment.service.impl.CreateFbdsDocumentsImpl;
import com.xpo.ltl.shipment.service.impl.CreateHandlingUnitMovementImpl;
import com.xpo.ltl.shipment.service.impl.CreateMoverShipmentImpl;
import com.xpo.ltl.shipment.service.impl.CreateNonRevenueShipmentImpl;
import com.xpo.ltl.shipment.service.impl.CreateShipmentDockExceptionImpl;
import com.xpo.ltl.shipment.service.impl.DeleteHandlingUnitImpl;
import com.xpo.ltl.shipment.service.impl.DeleteShipmentMovementImpl;
import com.xpo.ltl.shipment.service.impl.DetermineAccessorialServiceConflictImpl;
import com.xpo.ltl.shipment.service.impl.GetMoverShipmentsImpl;
import com.xpo.ltl.shipment.service.impl.GetOsdHeaderImpl;
import com.xpo.ltl.shipment.service.impl.GetPredictedTrueDebtorImpl;
import com.xpo.ltl.shipment.service.impl.GetProStatusCdImpl;
import com.xpo.ltl.shipment.service.impl.GetRatingInformationImpl;
import com.xpo.ltl.shipment.service.impl.GetServiceOverrideImpl;
import com.xpo.ltl.shipment.service.impl.GetShipmentAppointmentImpl;
import com.xpo.ltl.shipment.service.impl.GetShipmentCountImpl;
import com.xpo.ltl.shipment.service.impl.GetShipmentHistoryImpl;
import com.xpo.ltl.shipment.service.impl.GetShipmentImpl;
import com.xpo.ltl.shipment.service.impl.GetShipmentsAggregationImpl;
import com.xpo.ltl.shipment.service.impl.ListDeliveryCollectionInstructionForShipmentsImpl;
import com.xpo.ltl.shipment.service.impl.ListEventRefrencesImpl;
import com.xpo.ltl.shipment.service.impl.ListFBDSCopyBillDocumentsImpl;
import com.xpo.ltl.shipment.service.impl.ListHandlingUnitsImpl;
import com.xpo.ltl.shipment.service.impl.ListShipmentManagementRemarkImpl;
import com.xpo.ltl.shipment.service.impl.ListShipmentsFromHistoryImpl;
import com.xpo.ltl.shipment.service.impl.ListShipmentsImpl;
import com.xpo.ltl.shipment.service.impl.ListStatusForProsImpl;
import com.xpo.ltl.shipment.service.impl.MaintainAdHocMovementExceptionImpl;
import com.xpo.ltl.shipment.service.impl.MaintainShipmentManagementRemarkImpl;
import com.xpo.ltl.shipment.service.impl.MaintainShipmentMovementExceptionActionImpl;
import com.xpo.ltl.shipment.service.impl.MoveChildProsImpl;
import com.xpo.ltl.shipment.service.impl.OperationsShipmentImpl;
import com.xpo.ltl.shipment.service.impl.OperationsShipmentImplPilot;
import com.xpo.ltl.shipment.service.impl.PrintProLabelsImpl;
import com.xpo.ltl.shipment.service.impl.RatingInfoPassImpl;
import com.xpo.ltl.shipment.service.impl.ReplaceChildProsImpl;
import com.xpo.ltl.shipment.service.impl.SplitNonLoadedHandlingUnitsImpl;
import com.xpo.ltl.shipment.service.impl.SynchronizeShiplifyAccessorialsImpl;
import com.xpo.ltl.shipment.service.impl.UpdateCustomsBondImpl;
import com.xpo.ltl.shipment.service.impl.UpdateHandlingUnitWeightImpl;
import com.xpo.ltl.shipment.service.impl.UpdateHandlingUnitsAsAdminImpl;
import com.xpo.ltl.shipment.service.impl.UpdateHandlingUnitsImpl;
import com.xpo.ltl.shipment.service.impl.UpdateMovementForShipmentsImpl;
import com.xpo.ltl.shipment.service.impl.UpdateShipmentAsHandlingUnitExemptImpl;
import com.xpo.ltl.shipment.service.impl.UpdateShipmentDimensionsImpl;
import com.xpo.ltl.shipment.service.impl.UpdateShipmentFoodAndMotorMovesImpl;
import com.xpo.ltl.shipment.service.impl.UpdateShipmentHazMatImpl;
import com.xpo.ltl.shipment.service.impl.UpdateShipmentsUponTripCompletionImpl;
import com.xpo.ltl.shipment.service.impl.UpdateShipmentsWithRouteDtlImpl;
import com.xpo.ltl.shipment.service.impl.UpsertCarrierShipmentStatusImpl;
import com.xpo.ltl.shipment.service.impl.UpsertOsdImpl;
import com.xpo.ltl.shipment.service.impl.UpsertShipmentNotificationsImpl;
import com.xpo.ltl.shipment.service.impl.SynchronizeShiplifyAccessorialsImpl;
import com.xpo.ltl.shipment.service.impl.ValidateShipmentsForDeliveryImpl;
import com.xpo.ltl.shipment.service.impl.interim.PrintFbdsDocumentsImpl;
import com.xpo.ltl.shipment.service.impl.interim.UpdateAppointmentRequiredIndImpl;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@Stateless
@Local(ShipmentServiceIF.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
@LogExecutionTime
public class ShipmentServiceBean implements ShipmentServiceIF {

	private static final Log LOG = LogFactory.getLog(ShipmentServiceBean.class);

	@Inject
	private InternalServiceBean internalServiceBean;

	@Inject
	private MaintainShipmentManagementRemarkImpl maintainShipmentManagementRemarkImpl;

	@Inject
	private ListShipmentManagementRemarkImpl listShipmentMovementRemarkImpl;

	@Inject
	private UpdateShipmentsWithRouteDtlImpl updateShipmentsWithRouteDtlImpl;

	@Inject
	private CreateAndArchiveCopyBillDocumentImpl createAndArchiveCopyBillDocumentImpl;

	@Inject
	private GetShipmentImpl getShipmentImpl;

	@Inject
	private GetShipmentCountImpl getShipmentCountImpl;

	@Inject
	private GetShipmentsAggregationImpl getShipmentsAggregationImpl;

	@Inject
	private ListShipmentsImpl listShipmentsImpl;

	@Inject
	private ListShipmentsFromHistoryImpl listShipmentsFromHistoryImpl;

	@Inject
	private ListHandlingUnitsImpl listHandlingUnitsImpl;

	@Inject
	private MaintainShipmentMovementExceptionActionImpl maintainShipmentMovementExceptionActionImpl;

	@Inject
	private MaintainAdHocMovementExceptionImpl maintainAdHocMovementExceptionImpl;

	@Inject
	private OperationsShipmentImpl operationsShipmentImpl;

	@Inject
	private GetPredictedTrueDebtorImpl getPredictedTrueDebtorImpl;

	@Inject
	private OperationsShipmentImplPilot operationsShipmentImplPilot;

    @Inject
    private ValidateShipmentsForDeliveryImpl validateShipmentsForDelivery;

    @Inject
    private UpdateShipmentFoodAndMotorMovesImpl updateShipmentFoodAndMotorMovesImpl;

	@Inject
	private	ListFBDSCopyBillDocumentsImpl listFBDSCopyBillDocumentsImpl;

	@Inject
	private UpdateShipmentsUponTripCompletionImpl updateShipmentsUponTripCompletionImpl;

	@Inject
	private UpdateShipmentHazMatImpl updateShipmentHazMatImpl;

	@Inject
	private CreateHandlingUnitMovementImpl createHandlingUnitMovementImpl;

	@Inject
	private PrintFbdsDocumentsImpl printFbdsImpl;

	@Inject
	private CreateFbdsDocumentsImpl listGeneratedFbdsDocumentsImpl;

	@Inject
	private DeleteShipmentMovementImpl deleteShipmentMovementImpl;

	@Inject
	private CreateShipmentDockExceptionImpl createShipmentDockExceptionImpl;

	@Inject
	private ListDeliveryCollectionInstructionForShipmentsImpl listDeliveryCollectionInstructionForShipmentsImpl;

	@Inject
	private UpdateMovementForShipmentsImpl updateMovementForShipmentsImpl;

    @Inject
    private GetServiceOverrideImpl getServiceOverrideImpl;

	@Inject
	private UpdateShipmentDimensionsImpl updateShipmentDimensionsImpl;


    @Inject
    private DetermineAccessorialServiceConflictImpl determineAccessorialServiceConflictImpl;

    @Inject
	private UpsertCarrierShipmentStatusImpl upsertCarrierShipmentStatusImpl;

    @Inject
	private UpdateAppointmentRequiredIndImpl updateAppointmentRequiredIndImpl;

    @Inject
    private RatingInfoPassImpl ratingInfoPassImpl;

    @Inject
    private GetRatingInformationImpl getRatingInformationImpl;

	@Inject
	private GetProStatusCdImpl getProStatusCdImpl;

    @Inject
    private UpdateHandlingUnitWeightImpl updateHandlingUnitWeightImpl;

    @Inject
    private SplitNonLoadedHandlingUnitsImpl splitNonLoadedHandlingUnitsImpl;

    @Inject
    private UpdateHandlingUnitsImpl updateHandlingUnitsImpl;

    @Inject
    private BulkUpsertHandlingUnitsImpl bulkUpsertHandlingUnitsImpl;

    @Inject
    private ListEventRefrencesImpl listEventRefrencesImpl;

    @Inject
    private DeleteHandlingUnitImpl deleteHandingUnitImpl;

	@Inject
	private CreateMoverShipmentImpl createMoverShipmentImpl;

    @Inject
    private UpdateHandlingUnitsAsAdminImpl updateHandlingUnitsAsAdminImpl;
    
    @Inject
	private UpdateCustomsBondImpl updateCustomsBondImpl;

    @Inject
    private GetMoverShipmentsImpl getMoverShipmentsImpl;

    @Inject
    private CreateNonRevenueShipmentImpl createNonRevenueShipmentImpl;

    @Inject
    private UpdateShipmentAsHandlingUnitExemptImpl updateShipmentAsHandlingUnitExemptImpl;

    @Inject
    private ListStatusForProsImpl listStatusForProsImpl;

    @Inject
    private ReplaceChildProsImpl replaceChildProsImpl;

	@Inject
	private MoveChildProsImpl moveChildProsImpl;
	
    @Inject
    private CalculateServiceStandardForShipmentsImpl calculateServiceStandardForShipmentsImpl;

	@Inject
	private SynchronizeShiplifyAccessorialsImpl synchronizeShiplifyAccessorialsImpl;

    @Resource
	private EJBContext ejbContext;

	@PersistenceContext(unitName = "ltl-java-shipment-jaxrs")
	private EntityManager entityManager;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;
	
	@Inject
	private GetShipmentHistoryImpl getShipmentHistory;
	
	@Inject
	private UpsertOsdImpl upsertOsdImpl;
	
	@Inject
	private GetOsdHeaderImpl getOsdHeaderImpl;
	
	@Inject
	private GetShipmentAppointmentImpl getShipmentAppointmentImpl;
	
	@Inject
	private UpsertShipmentNotificationsImpl upsertShipmentNotificationsImpl;
	
	@Inject
	private PrintProLabelsImpl printProLabelsImpl;
	@Inject
	private UpdateShipmentImpl updateShipment;

	@Override
	public CreateShipmentManagementRemarkResp createShipmentManagementRemark(
		final CreateShipmentManagementRemarkRqst createShipmentManagementRemarkRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException {

		try {

			return maintainShipmentManagementRemarkImpl.createShipmentManagementRemark(
				createShipmentManagementRemarkRqst,
				txnContext,entityManager);

		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();

			checkNotNull(createShipmentManagementRemarkRqst, "The request is required.");
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.moreInfo(e.getFault())
			.log(e)
			.build();
		} catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();

			checkNotNull(createShipmentManagementRemarkRqst, "The request is required.");
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.log(e)
			.build();
		}

	}

	@Override
	public ListShipmentManagementRemarksResp listShipmentManagementRemarks(
		final ListShipmentManagementRemarksRqst listShipmentManagementRemarksRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException {

		return listShipmentMovementRemarkImpl.listShipmentManagementRemark(
			listShipmentManagementRemarksRqst,
			txnContext,
			entityManager);
	}

	@Override
	public UpdateShipmentManagementRemarkResp updateShipmentManagementRemark(
		final UpdateShipmentManagementRemarkRqst updateShipmentManagementRemarkRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		try {
			return maintainShipmentManagementRemarkImpl.updateShipmentManagementRemark(
				updateShipmentManagementRemarkRqst,
				txnContext, entityManager);

		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();

			checkNotNull(updateShipmentManagementRemarkRqst, "The request is required.");
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.contextValues(
				String.format(
					"update ShipmentManagementRemark for remarkId %s",
					updateShipmentManagementRemarkRqst.getManagementRemark().getRemarkId()))
			.moreInfo(e.getFault())
			.log(e)
			.build();
		} catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();
			checkNotNull(updateShipmentManagementRemarkRqst, "The request is required.");
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.contextValues(
				String.format(
					"update ShipmentManagementRemark for remarkId %s",
					updateShipmentManagementRemarkRqst.getManagementRemark().getRemarkId()))
			.log(e)
			.build();
		}
	}

	@Override
	public void updateMovementForShipments(
			final UpdateMovementForShipmentsRqst updateMovementForShipmentsRqst,
			final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		try {
			updateMovementForShipmentsImpl.updateMovementForShipments
					(updateMovementForShipmentsRqst, txnContext, entityManager);
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			throw e;
		} catch (final Exception e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.moreInfo("updateMovementForShipments",
					"An unexpected error occurred while trying to update the shipments: " + e.getMessage())
					.log(e).build();
		}
	}

	@Override
	public void deleteShipmentMovement(final Long shipmentInstId, final Integer movementSeqNbr, final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		deleteShipmentMovementImpl.deleteShipmentMovement(shipmentInstId, movementSeqNbr, txnContext, entityManager);
	}

	@Override
	public void updateShipmentsWithRouteDtl(
		final UpdateShipmentsWithRouteDtlRqst updateShipmentsWithRouteDtlRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		try {
			updateShipmentsWithRouteDtlImpl.updateShipmentsWithRouteDtl(updateShipmentsWithRouteDtlRqst, txnContext, entityManager);
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			throw e;
		} catch (final Exception e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).moreInfo("updateShipmentsWithRouteDtl",
				"An unexpected error occurred while trying to update the shipments with route details: " + e.getMessage()).log(e).build();
		}
	}

	@Override
	public CreateShipmentRemarkResp createShipmentRemark(
		final CreateShipmentRemarkRqst createShipmentRemarkRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException {

		throw new ServiceUnavailableException("Not yet implemented");
	}

	@Override
	public GetShipmentRemarkResp getShipmentRemark(
		final Long shipmentInstId,
		final ShipmentRemarkTypeCd typeCd,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		throw new ServiceUnavailableException("Not yet implemented");
	}

	@Override
	public GetShipmentAsMatchedPartiesResp getShipmentAsMatchedParties(final String proNbr, final XMLGregorianCalendar pickupDate,
		final Long shipmentInstId, final TransactionContext txnContext)
				throws ServiceException, ValidationException, NotFoundException {
		throw new ServiceUnavailableException("Not yet implemented");
	}

	@Override
	public CreateAndArchiveCopyBillDocumentResp createAndArchiveCopyBillDocument(
		final CreateAndArchiveCopyBillDocumentRqst createAndArchiveCopyBillDocumentRqst, final TransactionContext txnContext)
				throws ServiceException, ValidationException, NotFoundException {

		CreateAndArchiveCopyBillDocumentResp resp = null;
		try {
			resp = createAndArchiveCopyBillDocumentImpl.createAndArchiveCopyBillDocument(createAndArchiveCopyBillDocumentRqst, txnContext, entityManager);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return resp;
	}

	@Override
	public CreateHandlingUnitMovementResp createHandlingUnitMovement(
			final CreateHandlingUnitMovementRqst createHandlingUnitMovementRqst, final String childProNbr,
			final TransactionContext txnContext) throws ServiceException, ValidationException {
		try {
			return createHandlingUnitMovementImpl.createHandlingUnitMovement(createHandlingUnitMovementRqst,
					childProNbr, txnContext, entityManager);
		} catch (final OptimisticLockException e) {
			throw ExceptionBuilder
					.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext)
					.contextValues(
							String.format(
									"createHandlingUnitMovement for PRO Number %s",
									childProNbr))
					.log(e)
					.build();
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		} catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
	}

	@Override
	public GetShipmentResp getShipment(
		String proNbr, 
		String pickupDate, 
		Long shipmentInstId,
		ShipmentDetailCd[] shipmentDetailCd, 
		String[] suppRefNbrTypes, 
		Boolean handlingUnitMovementsForSplitOnlyInd,
		TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {

		return getShipmentImpl.getShipment(
			proNbr,
			pickupDate,
			shipmentInstId,
			shipmentDetailCd,
			suppRefNbrTypes,
			handlingUnitMovementsForSplitOnlyInd,
			txnContext,
			entityManager);
	}

	@Override
	public ListShipmentsResp listShipments(final ListShipmentsRqst listShipmentsRqst, final TransactionContext txnContext)
			throws ServiceException, ValidationException {

		return listShipmentsImpl.listShipments(listShipmentsRqst, txnContext, entityManager);
	}

	@Override
	public UpsertMovementExceptionActionResp upsertMovementExceptionAction(
		final UpsertMovementExceptionActionRqst upsertMovementExceptionActionRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		return maintainShipmentMovementExceptionActionImpl
				.upsertMovementExceptionAction(upsertMovementExceptionActionRqst, txnContext, entityManager);
	}

	@Override
	public void deleteMovementExceptionAction(
		final Long shipmentInstId,
		final Integer sequenceNbr,
		final Integer movementSequenceNbr,
		final Integer movementExceptionSequenceNbr,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		maintainShipmentMovementExceptionActionImpl.deleteMovementExceptionAction(
			shipmentInstId,
			sequenceNbr,
			movementSequenceNbr,
			movementExceptionSequenceNbr,
			txnContext,
			entityManager);
	}

	@Override
	public void deleteAdHocMovementException(
		final Long shipmentInstId,
		final Integer movementSequenceNbr,
		final Integer sequenceNbr,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		maintainAdHocMovementExceptionImpl.deleteAdHocMovementException(
			shipmentInstId,
			movementSequenceNbr,
			sequenceNbr,
			txnContext,
			entityManager);

	}

	@Override
	public UpsertAdHocMovementExceptionResp UpsertAdHocMovementException(
		final UpsertAdHocMovementExceptionRqst upsertAdHocMovementExceptionRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		return maintainAdHocMovementExceptionImpl.upsertAdHocMovementException(
			upsertAdHocMovementExceptionRqst,
			txnContext,
			entityManager);
	}

	@Override
	public void upsertCarrierShipmentStatus(
			final UpsertCarrierShipmentStatusRqst upsertCarrierShipmentStatusRqst,
			final Long carrierId,
			final TransactionContext txnContext)
			throws ServiceException, ValidationException {

		try {
			upsertCarrierShipmentStatusImpl.upsertCarrierShipmentStatus(
					upsertCarrierShipmentStatusRqst, carrierId, txnContext);
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			throw e;
		} catch (final Exception e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).moreInfo("upsertCarrierShipmentStatus",
					"An unexpected error occurred while trying to update the carrier shipment status: " + e.getMessage()).log(e).build();
		}
	}

	@Override
	public GetShipmentsAggregationResp getShipmentsAggregation(
		final String beginDateStr,
		final String endDateStr,
		final Long[] pricingAgreementIdsArr,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {

		Date beginDate;
		try {
			beginDate = BasicTransformer.toDate(beginDateStr);
		} catch (final RuntimeException rte) {
			throw ExceptionBuilder
				.exception(ValidationErrorMessage.GENERIC_DATE_INVALID, txnContext)
				.contextValues(String.format("invalid beginDate %s", beginDateStr))
				.log(rte)
				.build();
		}
		Date endDate;
		try {
			endDate = BasicTransformer.toDate(endDateStr);
		} catch (final RuntimeException rte) {
			throw ExceptionBuilder
				.exception(ValidationErrorMessage.GENERIC_DATE_INVALID, txnContext)
				.contextValues(String.format("invalid endDate %s", endDateStr))
				.log(rte)
				.build();
		}

		final List<BigDecimal> pricingAgreementIds = Stream
			.of(pricingAgreementIdsArr)
			.map(BigDecimal::valueOf)
			.collect(Collectors.toList());

		return getShipmentsAggregationImpl
			.getShipmentsAggregation(beginDate, endDate, pricingAgreementIds, txnContext, entityManager);
	}

	@Override
	public UpsertOperationsShipmentResp upsertOperationsShipment(
		final UpsertOperationsShipmentRqst upsertOperationsShipmentRqst, final TransactionContext txnContext)
				throws ServiceException, ValidationException, NotFoundException {
		try {
			return operationsShipmentImpl.upsertOperationShipment(upsertOperationsShipmentRqst,
				txnContext,
				entityManager);
		}
		catch (final OptimisticLockException e) {
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext)
			.contextValues(
				String.format(
					"upsertOperationsShipmentRqst for shipmentInstId %s",
					upsertOperationsShipmentRqst.getOperationsShipment().getShipmentInstId()))
			.log(e)
			.build();
		}
		catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
		catch (final RuntimeException e) {
			LOG.error(e);
			throw e;
		}
	}

	@Override
	public GetShipmentCountByShipperResp getShipmentCountByShipper(final Integer shipperCisCustomerNbr,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		return getShipmentCountImpl.getShipmentCountByShipper(shipperCisCustomerNbr,
			txnContext,
			entityManager);
	}

	@Override
	public GetPredictedTrueDebtorResp getPredictedTrueDebtor(final String shipperCustomerId, final String consigneeZipCd,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		// TODO Auto-generated method stub
		return getPredictedTrueDebtorImpl.getPredictedTrueDebtor(shipperCustomerId, consigneeZipCd,
			txnContext,
			entityManager);
	}

	@Override
	public CreateShipmentDockExceptionResp createShipmentDockException(
			final CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst,
			final TransactionContext txnContext) throws ServiceException {
		try {
			return createShipmentDockExceptionImpl.createShipmentDockException(
					createShipmentDockExceptionRqst, txnContext, entityManager);
		} catch (final OptimisticLockException e) {
			throw ExceptionBuilder
					.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext).log(e)
					.build();
		} catch (ServiceException | RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
	public CreateShipmentSkeletonResp createShipmentSkeleton(final CreateShipmentSkeletonRqst createShipmentSkeletonRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException {

        try {
            final ShipmentSkeletonResponse shipmentSkeletonResponse = internalServiceBean.createShipmentSkeleton(createShipmentSkeletonRqst,
                txnContext, entityManager);
            if (BooleanUtils.isTrue(shipmentSkeletonResponse.isSkeletonBasedOnPickup())) {
                internalServiceBean.performLegacyUpdates(shipmentSkeletonResponse, txnContext);
            }
            final CreateShipmentSkeletonResp createShipmentSkeletonResp = new CreateShipmentSkeletonResp();
            createShipmentSkeletonResp.setShipmentSkeletonResponse(shipmentSkeletonResponse);
            return createShipmentSkeletonResp;
        } catch (final RuntimeException e) {
            LOG.error(e);
            throw e;
        }
	}

    @Override
    @Deprecated
	public UpdateHandlingUnitDimensionsResp updateHandlingUnitDimensions(
		final UpdateHandlingUnitDimensionsRqst updateHandlingUnitDimensionsRqst, final String trackingProNbr,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {

        throw new ServiceUnavailableException("This Service was deprecated, use updateShipmentDimensions service.");

	}

	@Override
	public void updateShipmentFoodAndMotorMoves(final UpdateShipmentFoodAndMotorMovesRqst updateShipmentFoodAndMotorMovesRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		try {
			updateShipmentFoodAndMotorMovesImpl.updateShipmentFoodAndMotorMoves(updateShipmentFoodAndMotorMovesRqst,
					txnContext,
					entityManager);
		}
		catch (final OptimisticLockException e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder
					.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext)
					.contextValues(
							String.format(
									"updateShipmentFoodAndMotorMovesRqst for shipmentInstId %s - "
											+ "proNbr %s",
									updateShipmentFoodAndMotorMovesRqst.getShipmentInstId(),
									updateShipmentFoodAndMotorMovesRqst.getProNbr()))
					.log(e)
					.build();
		}
		catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
		catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
	}

    @Override
    public ListFBDSDocumentsResp listFBDSDocuments
            (final String[] proNbrs,
             final Long[] shipmentInstIds,
             final DocumentFormTypeCd documentRequestType,
             final Boolean allowWarningsInd,
             final TransactionContext txnContext)
            throws ServiceException {
        return listFBDSCopyBillDocumentsImpl.listFBDSDocuments
            (proNbrs,
             shipmentInstIds,
             documentRequestType,
             allowWarningsInd,
             false,
             txnContext,
             entityManager);
    }

	@Override
	public UpdateShipmentHazMatResp updateShipmentHazMat(
			final UpdateShipmentHazMatRqst updateShipmentHazMatRqst,
			final TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		try {
			return updateShipmentHazMatImpl.updateShipmentHazMat(updateShipmentHazMatRqst,
					txnContext, entityManager);
		} catch (final OptimisticLockException e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder
					.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext)
					.contextValues(
							String.format("updateShipmentHazMat for shipmentInstId %s - proNbr %s",
									updateShipmentHazMatRqst.getShipmentInstId(),
									updateShipmentHazMatRqst.getProNbr()))
					.log(e).build();
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		} catch (final RuntimeException e) {
			LOG.error(e);
			throw e;
		}
	}

	@Override
	public UpsertOperationsShipmentPilotResp upsertOperationsShipmentPilot(
		final UpsertOperationsShipmentPilotRqst upsertOperationsShipmentPilotRqst, final TransactionContext txnContext)
				throws ServiceException, ValidationException, NotFoundException {
		try {
			return operationsShipmentImplPilot.upsertOperationShipmentPilot(upsertOperationsShipmentPilotRqst,
				txnContext,
				entityManager);
		}
		catch (final OptimisticLockException e) {
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext)
			.contextValues(
				String.format(
					"upsertOperationsShipmentPilot for shipmentInstId %s",
					upsertOperationsShipmentPilotRqst.getOperationsShipmentPilot().getShipmentInstId()))
			.log(e)
			.build();
		}
		catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
		catch (final RuntimeException e) {
			LOG.error(e);
			throw e;
		}
	}

    @Override
    public ValidateShipmentsForDeliveryResp validateShipmentsForDelivery(final ValidateShipmentsForDeliveryRqst validateShipmentsForDeliveryRqst, final TransactionContext transactionContext) throws ServiceException, ValidationException {
        return validateShipmentsForDelivery.validateShipmentsForDeliveryImpl(validateShipmentsForDeliveryRqst, transactionContext, entityManager);
    }

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void updateShipmentsUponTripCompletion(
			final UpdateShipmentsUponTripCompletionRqst updateShipmentsUponTripCompletionRqst, final TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		try {
			updateShipmentsUponTripCompletionImpl.updateShipmentsUponTripCompletion(updateShipmentsUponTripCompletionRqst, entityManager, txnContext);
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			throw e;
		} catch (final Exception e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).moreInfo("updateShipmentsUponTripCompletion",
					"An unexpected error occurred while trying to update the shipments with route details: " + e.getMessage()).log(e).build();
		}
	}

	@Override
	public PrintFBDSDocumentsResp printFBDSDocuments(
		final PrintFBDSDocumentsRqst printFBDSDocumentsRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException {

		return printFbdsImpl.printFbdsDocuments(printFBDSDocumentsRqst, txnContext, entityManager);
	}

	@Override
	public ListDeliveryCollectionInstructionForShipmentsResp listDeliveryCollectionInstructionForShipments(
			final ListDeliveryCollectionInstructionForShipmentsRqst listDeliveryCollectionInstructionForShipmentsRqst,
			final TransactionContext txnContext) throws ServiceException, ValidationException {

		return listDeliveryCollectionInstructionForShipmentsImpl.listDeliveryCollectionInstructionForShipments(listDeliveryCollectionInstructionForShipmentsRqst, txnContext, entityManager);
	}

	@Override
	public ListHandlingUnitsResp listHandlingUnits(final ListHandlingUnitsRqst listHandlingUnitsRqst,
			final TransactionContext txnContext) throws ServiceException, ValidationException {

		return listHandlingUnitsImpl.listHandlingUnits(listHandlingUnitsRqst, txnContext, entityManager);
	}

    @Override
    @Deprecated
    public void updateShipmentLinehaulDimensions(
        final UpdateShipmentLinehaulDimensionsRqst updateShipmentLinehaulDimensionsRqst, final TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
        throw new ServiceUnavailableException("This Service was deprecated, use updateShipmentDimensions service.");
    }

	@Override
	public GetServiceOverrideResp getServiceOverride(final Long shipmentInstId, final TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		return getServiceOverrideImpl.getServiceOverride(shipmentInstId, txnContext, entityManager);
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public UpdateHandlingUnitsResp updateHandlingUnits(final UpdateHandlingUnitsRqst updateHandlingUnitsRqst,
			final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
	    UpdateHandlingUnitsResp resp = null;
        try {
            resp = updateHandlingUnitsImpl.updateHandlingUnits(updateHandlingUnitsRqst, entityManager, txnContext);
        } catch (final OptimisticLockException e) {
            ejbContext.setRollbackOnly();
            throw ExceptionBuilder
                .exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext)
                .log(e)
                .build();
        } catch (ServiceException | RuntimeException e) {
            ejbContext.setRollbackOnly();
            LOG.error(e);
            throw e;
        }
        return resp;

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void updateShipmentDimensions(
		final UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst,
		final String proNbr,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {

		try {
			updateShipmentDimensionsImpl.updateShipmentDimensions(updateShipmentDimensionsRqst, proNbr, entityManager, txnContext);
		} catch (final OptimisticLockException e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext).contextValues(String.format("proNbr  %s", proNbr)).log(e).build();
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		} catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
	}

    @Override
    public DetermineAccessorialServiceConflictResp determineAccessorialServiceConflict(
        final DetermineAccessorialServiceConflictRqst determineAccessorialServiceConflictRqst, final TransactionContext txnContext)
            throws ServiceException, ValidationException {
        try {
            return determineAccessorialServiceConflictImpl
                .determineAccessorialServiceConflict(determineAccessorialServiceConflictRqst, txnContext);
        } catch (ServiceException | RuntimeException e) {
            ejbContext.setRollbackOnly();
            LOG.error(e);
            throw e;
        }
    }

//	@Override
//	public void upsertCarrierShipmentStatus(UpsertCarrierShipmentStatusRqst upsertCarrierShipmentStatusRqst,
//			Long carrierId, TransactionContext txnContext) throws ServiceException, ValidationException {
//
//		upsertCarrierShipmentStatusImpl.upsertCarrierShipmentStatus(
//				upsertCarrierShipmentStatusRqst, carrierId, txnContext);
//
//	}

	@Override
	public CreateFBDSDocumentsResp createFBDSDocuments(
		final CreateFBDSDocumentsRqst createFBDSDocumentsRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException {
		return listGeneratedFbdsDocumentsImpl.createFbdsDocuments(createFBDSDocumentsRqst, txnContext, entityManager);
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public BulkCreateShipmentSkeletonResp bulkCreateShipmentSkeleton(final BulkCreateShipmentSkeletonRqst bulkCreateShipmentSkeletonRqst,
        final TransactionContext txnContext) throws ServiceException, ValidationException {

        try {
            final List<ShipmentSkeletonResponse> shipmentSkeletonResponseList = internalServiceBean
                .bulkCreateShipmentSkeleton(bulkCreateShipmentSkeletonRqst, txnContext, entityManager);
            internalServiceBean.bulkPerformLegacyUpdates(CollectionUtils
                .emptyIfNull(shipmentSkeletonResponseList)
                .stream()
                .filter(shipmentSkeletonResponseDTO -> BooleanUtils
                    .isTrue(shipmentSkeletonResponseDTO.isSkeletonBasedOnPickup()))
                .collect(Collectors.toList()), txnContext);
            final BulkCreateShipmentSkeletonResp bulkCreateShipmentSkeletonResp = new BulkCreateShipmentSkeletonResp();
            bulkCreateShipmentSkeletonResp.setShipmentSkeletonsResponses(shipmentSkeletonResponseList);
            return bulkCreateShipmentSkeletonResp;
        } catch (final RuntimeException e) {
            LOG.error(e);
            throw e;
        }
    }

	@Override
	public ListShipmentHistTraceEventsResp listShipmentHistTraceEvents(
		final String proNbr,
		final Boolean movementEventsInd,
		final Boolean adminEventsInd,
		final ListMetadata listMetadata,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateAppointmentRequiredInd(
		final UpdateAppointmentRequiredIndRqst updateAppointmentRequiredIndRqst,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		try {
            updateAppointmentRequiredIndImpl.updateAppointmentRequiredInd(updateAppointmentRequiredIndRqst, txnContext);
		} catch (final Exception e) {
            throw ExceptionBuilder
            .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.moreInfo("ShipmentServiceBean.updateAppointmentRequiredInd",
					"An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
            .log(e)
            .build();
		}

	}

	@Override
	public UpsertRatingInformationPassResp upsertRatingInformationPass(
		final UpsertRatingInformationPassRqst upsertRatingInformationPassRqst,
		final TransactionContext txnContext) throws ServiceException {
		try {
			return ratingInfoPassImpl.upsertRatingInformationPass(upsertRatingInformationPassRqst, txnContext);
		} catch (ServiceException | RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
	}

	@Override
	public GetRatingInformationResp getRatingInformation(
		final Long shipmentInstId,
		final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		try {
			return getRatingInformationImpl.getRatingInformation(shipmentInstId, txnContext, entityManager);
		} catch (ServiceException | RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
	}

    @Override
    public GetProStatusResp getProStatus(final String proNbr, final TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
		try {
			return getProStatusCdImpl.getProStatus(proNbr, txnContext, entityManager);
		} catch (ServiceException | RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
	}

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateHandlingUnitWeight(final UpdateHandlingUnitWeightRqst updateHandlingUnitWeightRqst, final String childProNbr,
        final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
        try {
            updateHandlingUnitWeightImpl
                .updateHandlingUnitWeight(updateHandlingUnitWeightRqst, childProNbr, txnContext, entityManager);
        } catch (ServiceException | RuntimeException e) {
            ejbContext.setRollbackOnly();
            LOG.error(e);
            throw e;
        }
    }

    @Override
    public void bulkCreateShipmentManagementRemarks(
        final BulkCreateShipmentManagementRemarksRqst bulkCreateShipmentManagementRemarksRqst, final TransactionContext txnContext)
            throws ServiceException, ValidationException {
    	try {
            maintainShipmentManagementRemarkImpl
                .bulkCreateShipmentManagementRemarks(bulkCreateShipmentManagementRemarksRqst, txnContext,
                entityManager);
        } catch (final ServiceException e) {
            ejbContext.setRollbackOnly();
            throw e;
        } catch (final RuntimeException e) {
            ejbContext.setRollbackOnly();
            throw ExceptionBuilder
                .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                .moreInfo("InternalServiceBean.bulkCreateShipmentSkeleton",
                    "An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
                .log(e)
                .build();
        }
    }

	@Override
	public GetOperationsShipmentPilotResp getOperationsShipmentPilot(final Long shipmentInstId, final TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		return operationsShipmentImplPilot.getOperationsShipmentPilot(shipmentInstId, txnContext, entityManager);
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public SplitNonLoadedHandlingUnitsResp splitNonLoadedHandlingUnits(
			final SplitNonLoadedHandlingUnitsRqst splitNonLoadedHandlingUnitsRqst, final TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
        try {
            final SplitNonLoadedHandlingUnitsResp splitNonLoadedHandlingUnitsResp = splitNonLoadedHandlingUnitsImpl
                .splitNonLoadedHandlingUnits(splitNonLoadedHandlingUnitsRqst, txnContext, entityManager);

            return splitNonLoadedHandlingUnitsResp;
        } catch (final ServiceException e) {
            ejbContext.setRollbackOnly();
            throw e;
        } catch (final RuntimeException e) {
            ejbContext.setRollbackOnly();
            LOG.error(e);
            throw e;
        }
	}

	@Override
	public void deleteOperationsShipment(final Long shipmentInstId, final TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		operationsShipmentImpl.deleteOperationsShipment(shipmentInstId, txnContext, entityManager);
	}

	@Override
	public void deleteOperationsShipmentPilot(final Long shipmentInstId, final TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		operationsShipmentImplPilot.deleteOperationsShipmentPilot(shipmentInstId, txnContext, entityManager);
	}


	@Override
	public ListShipmentsFromHistoryResp listShipmentsFromHistory(final ListShipmentsFromHistoryRqst listShipmentsFromHistoryRqst, final TransactionContext txnContext)
			throws ServiceException, ValidationException {
		return listShipmentsFromHistoryImpl.listShipmentsFromHistory(listShipmentsFromHistoryRqst, txnContext, entityManager);
	}

   @Override
   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   public void bulkCreateHandlingUnits(final BulkCreateHandlingUnitsRqst bulkCreateHandlingUnitsRqst, final TransactionContext txnContext) throws ServiceException, ValidationException {
       throw new ServiceUnavailableException("This Service was deprecated, use bulkUpsertHandlingUnits service.");


   }

   @Override
   @TransactionAttribute(TransactionAttributeType.REQUIRED)
   public void bulkUpsertHandlingUnits(final BulkUpsertHandlingUnitsRqst bulkUpsertHandlingUnitsRqst,
       final TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
       try {
           bulkUpsertHandlingUnitsImpl.bulkUpsertHandlingUnits(bulkUpsertHandlingUnitsRqst, txnContext, entityManager);
       } catch (final RuntimeException e) {
           LOG.error(e);
           throw e;
       }
   }

   @Override
   public ListEventReferencesResp listEventReferences(final ListMetadata listMetadata, final TransactionContext txnContext)
        throws ServiceException, ValidationException, NotFoundException {
    return listEventRefrencesImpl.listEventReferences(listMetadata, txnContext, entityManager);
   }

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteHandlingUnit(final String childPro, final TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
        try {
            deleteHandingUnitImpl.deleteHandlingUnit(childPro, txnContext, entityManager);
        } catch (final RuntimeException e) {
            LOG.error(e);
            throw e;
        }
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateHandlingUnitAsAdmin(UpdateHandlingUnitAsAdminRqst updateHandlingUnitAsAdminRqst, TransactionContext transactionContext) throws ServiceException, ValidationException, NotFoundException {
        try {
            updateHandlingUnitsAsAdminImpl.updateHandlingUnitAsAdmin(updateHandlingUnitAsAdminRqst, entityManager, transactionContext);
        } catch (RuntimeException e) {
            LOG.error(e);
            throw e;
        }
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public CreateMoverShipmentResp createMoverShipment(CreateMoverShipmentRqst createMoverShipmentRqst, TransactionContext txnContext)
			throws ServiceException, ValidationException {

	       try {
			   CreateMoverShipmentResp moverResp =  internalServiceBean.createMoverShipment(createMoverShipmentRqst, txnContext, entityManager);

			   CreateAndArchiveCopyBillDocumentRqst createAndArchiveCopyBillDocumentRqst = new CreateAndArchiveCopyBillDocumentRqst();

			   ShipmentId shipmentId = new ShipmentId();
			   shipmentId.setShipmentInstId(String.valueOf(moverResp.getMoverShipmentInstId()));
			   createAndArchiveCopyBillDocumentRqst.setShipmentId(shipmentId);

			   try{
				   internalServiceBean.createAndArchiveCopyBillDocument(createAndArchiveCopyBillDocumentRqst, txnContext, entityManager);
			   }catch (ServiceException e) {
			   		LOG.error(e);
			   		DataValidationError error = new DataValidationError();
			   		error.setMessage(e.getMessage());
			   		moverResp.setWarning(error);
			   }

			   return moverResp;
	        } catch (ServiceException e) {
	            ejbContext.setRollbackOnly();
	            throw e;
	        } catch (final RuntimeException e) {
	            LOG.error(e);
	            throw e;
	       }
	}

	@Override
	public void upsertCustomsBond(UpsertCustomsBondRqst upsertCustomsBondRqst, TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		try {
			updateCustomsBondImpl.upsertCustomsBond(upsertCustomsBondRqst, txnContext, entityManager);
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			throw e;
		} catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		} catch (java.text.ParseException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
		} 
	}

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public GetMoverShipmentsResp getMoverShipments(String proNbr, TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
        return getMoverShipmentsImpl.getMoverShipments(proNbr, txnContext, entityManager);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public CreateNonRevenueShipmentResp createNonRevenueShipment(CreateNonRevenueShipmentRqst createNonRevenueShipmentRqst,
        TransactionContext txnContext) throws ServiceException, ValidationException {

        try {
            CreateNonRevenueShipmentResp createNonRevenueShipmentResp = new CreateNonRevenueShipmentResp();

            InternalCreateNonRevenueShipmentResponseDTO internalResponse = internalServiceBean.createNonRevenueShipmentImpl(createNonRevenueShipmentRqst, txnContext, entityManager);

            if (internalResponse.getDataValidationError().isPresent()) {
                createNonRevenueShipmentResp.setWarning(internalResponse.getDataValidationError().get());
            } else {
                CreateAndArchiveCopyBillDocumentRqst createAndArchiveCopyBillDocumentRqst = new CreateAndArchiveCopyBillDocumentRqst();

                ShipmentId shipmentId = new ShipmentId();
                shipmentId.setShipmentInstId(String.valueOf(internalResponse.getNonRevenueShipmentInstId()));
                createAndArchiveCopyBillDocumentRqst.setShipmentId(shipmentId);

                try {
                    internalServiceBean.createAndArchiveCopyBillDocument(createAndArchiveCopyBillDocumentRqst, txnContext, entityManager);
                } catch (ServiceException e) {
                    LOG.error(e);
                    DataValidationError error = new DataValidationError();
                    error.setMessage(e.getMessage());
                    createNonRevenueShipmentResp.setWarning(error);
                }
            }

            return createNonRevenueShipmentResp;
        } catch (ServiceException e) {
            ejbContext.setRollbackOnly();
            throw e;
        } catch (final RuntimeException e) {
            LOG.error(e);
            throw e;
        }

    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public UpdateShipmentsAsHandlingUnitExemptResp updateShipmentsAsHandlingUnitExempt(
        UpdateShipmentsAsHandlingUnitExemptRqst updateShipmentsAsHandlingUnitExemptRqst, TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
        try {
            UpdateShipmentsAsHandlingUnitExemptResp shipmentAsHandlingUnitExempt = updateShipmentAsHandlingUnitExemptImpl
                .updateShipmentAsHandlingUnitExempt(updateShipmentsAsHandlingUnitExemptRqst, txnContext, entityManager);

            if (CollectionUtils.isNotEmpty(shipmentAsHandlingUnitExempt.getWarnings())) {
                ejbContext.setRollbackOnly();
            }

            return shipmentAsHandlingUnitExempt;
        } catch (ServiceException e) {
            ejbContext.setRollbackOnly();
            throw e;
        } catch (final RuntimeException e) {
            LOG.error(e);
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public ListStatusForProsResp listStatusForPros(ListStatusForProsRqst listStatusForProsRqst, TransactionContext txnContext)
            throws ServiceException, ValidationException {
        return listStatusForProsImpl.listStatusForPros(listStatusForProsRqst, txnContext, entityManager);
    }

    @Override
    public ReplaceChildProsResp replaceChildPros(ReplaceChildProsRqst replaceChildProsRqst,
        TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
        return replaceChildProsImpl.replaceChildProsWithNewPros(replaceChildProsRqst, txnContext, entityManager);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CalculateServiceStandardForShipmentsResp calculateServiceStandardForShipments(
        CalculateServiceStandardForShipmentsRqst calculateServiceStandardForShipmentsRqst, TransactionContext txnContext)
            throws ServiceException, ValidationException {
        return calculateServiceStandardForShipmentsImpl
            .calculateServiceStandardForShipments(calculateServiceStandardForShipmentsRqst, txnContext, entityManager);
    }

    @Override
	public MoveChildProsResp moveChildPros(MoveChildProsRqst rqst, TransactionContext txnContext)
	throws ServiceException, ValidationException, NotFoundException {
		try {
			return moveChildProsImpl.moveChildPros(rqst, txnContext, entityManager);
		} catch (ServiceException | RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}
	}


	@Override
	public GetShipmentHistoryResp getShipmentHistory(
			String proNbr,
			String pickupDate,
			Long shipmentInstId,
			XMLGregorianCalendar[] xmlGregorianCalendars,
			ShipmentDetailCd[] shipmentDetailCds,
			TransactionContext transactionContext) throws ServiceException, ValidationException, NotFoundException {
		return getShipmentHistory.getShipmentHistory(proNbr,pickupDate, shipmentInstId,xmlGregorianCalendars,shipmentDetailCds,transactionContext,entityManager);

	}
	@Override
	public ListShipmentEventLogsResp listShipmentEventLogs(
			Long aLong,
			TransactionContext transactionContext) throws ServiceException, ValidationException, NotFoundException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void deleteShipmentManagementRemark(Long remarkId, Integer movementSequenceNbr,
			Integer movementExceptionSequenceNbr, TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		try{
			maintainShipmentManagementRemarkImpl.deleteShipmentManagementRemark(
				remarkId,
				movementSequenceNbr,
				movementExceptionSequenceNbr,
				txnContext,
				entityManager);
		} catch (final RuntimeException e) {
				LOG.error(e);
				throw e;
		}
	}

	@Override
	public GetShipmentManagementRemarkResp getShipmentManagementRemark(Long remarkId, Integer movementSequenceNbr,
			Integer movementExceptionSequenceNbr, TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		return maintainShipmentManagementRemarkImpl.getShipmentManagementRemark(
			remarkId,
			movementSequenceNbr,
			movementExceptionSequenceNbr,
			txnContext,
			entityManager);
	}

	@Override
	public GetOsdResp getOsd(Long osdId, String proNbr, OsdCategoryCd osdCategoryCd, String reportingSicCd,
			OsdStatusCd osdStatusCd, OsdPayloadTypeCd osdPayloadTypeCd,
			TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		return getOsdHeaderImpl.getOsd(osdId, proNbr, osdCategoryCd, reportingSicCd, osdStatusCd, osdPayloadTypeCd, txnContext, entityManager);
	}

	@Override
	public UpsertOsdResp upsertOsd(UpsertOsdRqst upsertOsdRqst, TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		try {
			return upsertOsdImpl.upsertOsd(upsertOsdRqst, txnContext, entityManager);
		}
		catch (ServiceException | RuntimeException e) {
			ejbContext.setRollbackOnly();
			LOG.error(e);
			throw e;
		}

	}

	@Override
	public UpdateShipmentResp updateShipment(Long shipmentInstId, ShipmentUpdateActionCd actionCd,
			UpdateShipmentRqst updateShipmentRqst, TransactionContext txnContext)
			throws ServiceException {
		return updateShipment.updateShipment(shipmentInstId, actionCd, updateShipmentRqst, txnContext, entityManager);
	}

    @Override
    public GetShipmentAppointmentResp getShipmentAppointment(String proNbr, Long shipmentInstId,
        TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
        return getShipmentAppointmentImpl.getShipmentAppointment(proNbr, shipmentInstId, txnContext, entityManager);
    }

    @Override
    public UpsertShipmentNotificationsResp upsertShipmentNotifications(
        UpsertShipmentNotificationsRqst upsertShipmentNotificationsRqst, TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
        return upsertShipmentNotificationsImpl.upsertShipmentNotifications(
                    upsertShipmentNotificationsRqst, 
                    txnContext, 
                    entityManager, 
                    db2EntityManager);
    }

  @Override
	public void updatePartyMatchReview(UpdatePartyMatchReviewRqst updatePartyMatchReviewRqst,
			TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'updatePartyMatchReview'");
	}

    public UpdateProStatusResp updateProStatus(
            UpdateProStatusRqst updateProStatusRqst, String proNbr, TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
        throw new UnsupportedOperationException("Unimplemented method 'updateProStatus'");
    }

    @Override
    public AcorShipmentResp acorShipment(String proNbr, AcorTypeCd acorTypeCd, AcorShipmentRqst acorShipmentRqst,
        TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
        throw new UnsupportedOperationException("This service is not yet implemented.");
    }

    @Override
    public VoidShipmentResp voidShipment(String proNbr, VoidShipmentRqst voidShipmentRqst,
        TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
        throw new UnsupportedOperationException("This service is not yet implemented.");
    }

	@Override
	public PrintProLabelsResp printProLabels(PrintProLabelsRqst printProLabelsRqst, TransactionContext txnContext)
	throws ServiceException, ValidationException {
		return printProLabelsImpl.printProLabels(printProLabelsRqst, txnContext);
	}
	
	public ListUnprocessedShiplifyRecordsResp listUnprocessedShiplifyShipments(TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		return synchronizeShiplifyAccessorialsImpl.listAccessorials(txnContext, entityManager);
	}

	@Override
	public SynchronizeShiplifyAccessorialsResp synchronizeShiplifyAccessorials(
			SynchronizeShiplifyAccessorialsRqst synchronizeShiplifyAccessorialsRqst, TransactionContext txnContext)
			throws ServiceException, ValidationException {
		//this microservice is implemented for batch job operator
		return synchronizeShiplifyAccessorialsImpl.synchronizeAccessorials(synchronizeShiplifyAccessorialsRqst, txnContext, entityManager);
	}
	
}
