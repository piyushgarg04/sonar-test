package com.xpo.ltl.shipment.service.client;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.inject.Inject;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.xpo.ltl.api.appointment.client.v1.AppointmentClient;
import com.xpo.ltl.api.appointment.v1.GetActiveApptNotificationForShipmentResp;
import com.xpo.ltl.api.appointment.v1.ListAppointmentNotificationsForShipmentsResp;
import com.xpo.ltl.api.appointment.v1.ListAppointmentNotificationsForShipmentsRqst;
import com.xpo.ltl.api.carriermanagement.client.v1.CarrierManagementClient;
import com.xpo.ltl.api.carriermanagement.v1.GetCarrierDetailsResp;
import com.xpo.ltl.api.cityoperations.client.v1.CityOperationsClient;
import com.xpo.ltl.api.cityoperations.v1.CustomerOperationsNote;
import com.xpo.ltl.api.cityoperations.v1.CustomerProfileDetailCd;
import com.xpo.ltl.api.cityoperations.v1.DeliveryShipmentSearchFilter;
import com.xpo.ltl.api.cityoperations.v1.DeliveryShipmentSearchRecord;
import com.xpo.ltl.api.cityoperations.v1.GetDeliveryNotificationResp;
import com.xpo.ltl.api.cityoperations.v1.GetOperationsCustomerProfileResp;
import com.xpo.ltl.api.cityoperations.v1.GetServiceCenterPrintPreferencesResp;
import com.xpo.ltl.api.cityoperations.v1.PrintOption;
import com.xpo.ltl.api.cityoperations.v1.SearchDeliveryShipmentsResp;
import com.xpo.ltl.api.cityoperations.v1.SearchDeliveryShipmentsRqst;
import com.xpo.ltl.api.cityoperations.v1.XrtAttributeFilter;
import com.xpo.ltl.api.cityoperations.v1.XrtSortExpression;
import com.xpo.ltl.api.client.common.Attachment;
import com.xpo.ltl.api.client.common.ClientCredentials;
import com.xpo.ltl.api.client.common.JsonAttachment;
import com.xpo.ltl.api.client.common.Response;
import com.xpo.ltl.api.customer.client.v1.CustomerClient;
import com.xpo.ltl.api.customer.v1.DetermineRestrictedBillToResp;
import com.xpo.ltl.api.customer.v1.GetShipperProfileFreightCubeResp;
import com.xpo.ltl.api.customer.v1.GetShipperProfileFreightCubeRqst;
import com.xpo.ltl.api.customer.v2.CustomerContact;
import com.xpo.ltl.api.customer.v2.CustomerIdTypeCd;
import com.xpo.ltl.api.customer.v2.ListCustomerContactsResp;
import com.xpo.ltl.api.dockoperations.client.v1.DockOperationsClient;
import com.xpo.ltl.api.dockoperations.v1.GetProLoadDetailsResp;
import com.xpo.ltl.api.dockoperations.v1.GetTrailerLoadByEquipmentIdResp;
import com.xpo.ltl.api.dockoperations.v1.GetTrailerSpecificationResp;
import com.xpo.ltl.api.documentmanagement.client.v1.DocumentManagementClient;
import com.xpo.ltl.api.documentmanagement.v1.ArchiveDocumentResp;
import com.xpo.ltl.api.documentmanagement.v1.DmsArchiveRequest;
import com.xpo.ltl.api.documentmanagement.v1.RetrieveDmsAuthTokenResp;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.freightflow.client.v2.FreightFlowClient;
import com.xpo.ltl.api.freightflow.v2.CalculateTransitTimeResp;
import com.xpo.ltl.api.freightflow.v2.CalculateTransitTimeRqst;
import com.xpo.ltl.api.freightflow.v2.TransitTimeBasic;
import com.xpo.ltl.api.humanresource.client.v1.HumanResourceClient;
import com.xpo.ltl.api.humanresource.v1.Employee;
import com.xpo.ltl.api.humanresource.v1.EmployeeName;
import com.xpo.ltl.api.humanresource.v1.GetEmployeeDetailsByEmpIdResp;
import com.xpo.ltl.api.humanresource.v1.ListEmployeesByEmpIdResp;
import com.xpo.ltl.api.humanresource.v1.ListEmployeesByEmpIdRqst;
import com.xpo.ltl.api.humanresource.v2.InterfaceEmployee;
import com.xpo.ltl.api.humanresource.v2.ListEmployeesResp;
import com.xpo.ltl.api.humanresource.v2.ListEmployeesRqst;
import com.xpo.ltl.api.infrastructure.client.v2.InfrastructureClient;
import com.xpo.ltl.api.location.client.v2.LocationClient;
import com.xpo.ltl.api.location.v2.CompanyOperations;
import com.xpo.ltl.api.location.v2.DetermineOperationalServiceDateResp;
import com.xpo.ltl.api.location.v2.GetHostSicDetailsResp;
import com.xpo.ltl.api.location.v2.GetLocOperationsServiceCenterProfitabilityBySicResp;
import com.xpo.ltl.api.location.v2.GetLocReferenceDetailsBySicResp;
import com.xpo.ltl.api.location.v2.GetLocationReferenceDetailsResp;
import com.xpo.ltl.api.location.v2.GetOperationalServiceDaysCountResp;
import com.xpo.ltl.api.location.v2.GetRefSicAddressResp;
import com.xpo.ltl.api.location.v2.GetSicForPostalCodesResp;
import com.xpo.ltl.api.location.v2.GetZoneAndSatelliteBySicResp;
import com.xpo.ltl.api.location.v2.ListLocationFeatureValuesByFeatureResp;
import com.xpo.ltl.api.location.v2.ListLocationFeaturesResp;
import com.xpo.ltl.api.location.v2.LocationReference;
import com.xpo.ltl.api.location.v2.ServiceCenter;
import com.xpo.ltl.api.location.v2.ServiceTypeCd;
import com.xpo.ltl.api.pronumberreengineering.client.v1.ProNumberReengineeringClient;
import com.xpo.ltl.api.pronumberreengineering.v1.ListProBolPrefixMasterResp;
import com.xpo.ltl.api.pronumberreengineering.v1.ListProBolPrefixMasterRqst;
import com.xpo.ltl.api.pronumberreengineering.v1.ProInUseCd;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.client.v2.ShipmentClient;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v1.GetShipmentResp;
import com.xpo.ltl.api.shipment.v1.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.ApprovedSalvagesPayload;
import com.xpo.ltl.api.shipment.v2.BaseLog;
import com.xpo.ltl.api.shipment.v2.CreateBaseLogPayload;
import com.xpo.ltl.api.shipment.v2.CreateNonRevenueShipmentRqst;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeletonResponse;
import com.xpo.ltl.api.shipment.v2.StartApprovedSalvagesChEnsembleRqst;
import com.xpo.ltl.api.shipment.v2.StartCreateBaseLogChEnsembleRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.NumberUtil;
import com.xpo.ltl.java.util.cityoperations.executors.TaskProcessor;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.enums.BaseLogTypeEnum;
import com.xpo.ltl.shipment.service.impl.interim.MaintainBolDocImpl;
import com.xpo.ltl.shipment.service.util.ServiceExceptionUtil;
import com.xpo.ltl.util.cache.carriermanagement.CarrierManagementCaches;
import com.xpo.ltl.util.cache.carriermanagement.api.CarrierManagementClientProvider;
import com.xpo.ltl.util.cache.carriermanagement.key.CarrierDetailsCacheKey;
import com.xpo.ltl.util.cache.core.key.SetCacheKey;
import com.xpo.ltl.util.cache.core.key.StringCacheKey;
import com.xpo.ltl.util.cache.core.key.VoidCacheKey;
import com.xpo.ltl.util.cache.freightflow.FreightFlowCaches;
import com.xpo.ltl.util.cache.freightflow.api.FreightFlowClientProvider;
import com.xpo.ltl.util.cache.freightflow.key.CalculateTransitTimeCacheKey;
import com.xpo.ltl.util.cache.location.LocationCaches;
import com.xpo.ltl.util.cache.location.api.LocationClientProvider;
import com.xpo.ltl.util.cache.location.key.DetermineOperationalServiceDateCacheKey;
import com.xpo.ltl.util.cache.location.key.LocationFeaturesBySettingValueCacheKey;
import com.xpo.ltl.util.cache.location.key.ZoneAndSatelliteBySicCacheKey;
import com.xpo.ltl.util.cache.pronumberreengineering.ProNumberReengineeringCaches;
import com.xpo.ltl.util.cache.pronumberreengineering.api.ProNumberReengineeringClientProvider;
import com.xpo.ltl.util.cache.pronumberreengineering.key.ProBolPrefixMasterCacheKey;

@ApplicationScoped
public class ExternalRestClient
        implements LocationClientProvider,
        ProNumberReengineeringClientProvider, CarrierManagementClientProvider, FreightFlowClientProvider {

    private static final Logger LOGGER = LogManager.getLogger(ExternalRestClient.class);

    private static final List<ProInUseCd> NON_LEGACY_IN_USE_CD_LIST = 
            Lists.newArrayList(ProInUseCd.ELECTRONIC, ProInUseCd.LABEL, ProInUseCd.NOT_USED, ProInUseCd.WEB, ProInUseCd.GENERAL_CLAIMS_BUSINESS);
    
    private static final List<String> FIELDS = Arrays.asList("shipmentInstId" ,"proNbr", "equipmentIdPrefix","equipmentIdSuffix","equipmentInstId");
	@Inject
	private AppContext appContext;

	private LoadingCache<StringCacheKey, GetLocationReferenceDetailsResp> locationReferenceDetailsCache;
    private LoadingCache<StringCacheKey, GetLocReferenceDetailsBySicResp> locReferenceDetailsBySicCache;
    private LoadingCache<ZoneAndSatelliteBySicCacheKey, GetZoneAndSatelliteBySicResp> zoneAndSatelliteBySicCache;
    private LoadingCache<ProBolPrefixMasterCacheKey, ListProBolPrefixMasterResp> proBolPrefixMasterCache;
    private LoadingCache<CarrierDetailsCacheKey, GetCarrierDetailsResp> carrierDetailsCache;
    private LoadingCache<StringCacheKey, GetHostSicDetailsResp> hostSicDetailsCache;
    private LoadingCache<DetermineOperationalServiceDateCacheKey, DetermineOperationalServiceDateResp> DetermineOperationalServiceDateLoaderCache;
    private LoadingCache<SetCacheKey<CalculateTransitTimeCacheKey>, CalculateTransitTimeResp> newCalculateTransitTimesCache;
    private LoadingCache<CalculateTransitTimeCacheKey, CalculateTransitTimeResp> newCalculateTransitTimeCache;
    private LoadingCache<SetCacheKey<String>, GetSicForPostalCodesResp> sicForPostalCodesCache;
    private LoadingCache<LocationFeaturesBySettingValueCacheKey, ListLocationFeaturesResp> locationFeatureSettingCache;
    private LoadingCache<VoidCacheKey, Map<String, ServiceCenter>> operationalServiceCentersCache;

    @PostConstruct
    protected void init() {
    	
		locationReferenceDetailsCache =
		LocationCaches.newLocationReferenceDetailsCache
			(this,
			 appContext.getRestClientLocReferenceDetailsCacheSpec(),
			 appContext.getExecutor());
    	carrierDetailsCache = 
    			CarrierManagementCaches.newCarrierDetailsCache
    				(this,
    				appContext.getRestClientCarrierManagementCacheSpec(),
    				appContext.getExecutor());
    	
        locReferenceDetailsBySicCache =
            LocationCaches.newLocReferenceDetailsBySicCache
                (this,
                 appContext.getRestClientLocReferenceDetailsBySicCacheSpec(),
                 appContext.getExecutor());

        zoneAndSatelliteBySicCache =
            LocationCaches.newZoneAndSatelliteBySicCache
                (this,
                 appContext.getRestClientZoneAndSatelliteBySicCacheSpec(),
                 appContext.getExecutor());

        proBolPrefixMasterCache =
            ProNumberReengineeringCaches.newProBolPrefixMasterCache
                (this,
                 appContext.getRestClientListProBolPrefixMasterCacheSpec(),
                 appContext.getExecutor());

        hostSicDetailsCache =
                LocationCaches.newHostSicDetailsCache
                    (this,
                     appContext.getRestClientHostSicDetailsCacheSpec(),
                     appContext.getExecutor());

        DetermineOperationalServiceDateLoaderCache =
        		LocationCaches.newDetermineOperationalServiceDateLoaderCache
        		(this,
        		 appContext.getRestClientDetermineOperationalServiceDateCacheSpec(),
        		 appContext.getExecutor());
        
        newCalculateTransitTimeCache =
                FreightFlowCaches.newCalculateTransitTimeCache
                (this,
                 appContext.getRestClientCalculateTransitTimeSpec(),
                 appContext.getExecutor());

        newCalculateTransitTimesCache =
                FreightFlowCaches
                    .newCalculateTransitTimesCache
                (this,
                 appContext.getRestClientCalculateTransitTimeSpec(),
                 appContext.getExecutor(),
                 newCalculateTransitTimeCache);
        
        sicForPostalCodesCache = 
        		LocationCaches.newSicForPostalCodesCache
                (this,
                 appContext.getRestSicForPostalCodesCacheSpec(),
                 appContext.getExecutor());

        locationFeatureSettingCache = LocationCaches
            .newLocationFeaturesBySettingCache(this, appContext.getRestClientLocFeaturesBySettingCacheSpec(), appContext.getExecutor());
        
        operationalServiceCentersCache =
                LocationCaches.newOperationServiceCentersCache
                    (this,
                     false,
                     appContext.getRestClientOperationalServiceCentersCacheSpec(),
                     appContext.getExecutor());

    }

    private com.xpo.ltl.api.shipment.client.v1.ShipmentClient getShipmentV1Client
            (TransactionContext txnContext)
            throws ServiceException {
        try {
            if (txnContext != null
                && StringUtils.isNotBlank(txnContext.getAuthorization())) {
                return com.xpo.ltl.api.shipment.client.v1.ShipmentClient.newInstance
                    (appContext.getRestHost(),
                     appContext.getRestBasePathForShipmentV1(),
                     txnContext.getAuthorization());
            }
            else {
                // Default to use SHIPMENT_REST Application Credentials for Local Testing
                return com.xpo.ltl.api.shipment.client.v1.ShipmentClient.newInstance
                    (appContext.getRestHost(),
                     appContext.getRestBasePathForShipmentV1(),
                     ClientCredentials.newInstance
                         (appContext.getRestClientAuthToken(),
                          appContext.getRestClientUserName(),
                          appContext.getRestClientPassword()));
            }
        }
        catch (Exception e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION);
        }
    }

    private ShipmentClient getShipmentV2Client(TransactionContext txnContext)
            throws ServiceException {
        try {
            if (txnContext != null
                && StringUtils.isNotBlank(txnContext.getAuthorization())) {
                return ShipmentClient.newInstance
                    (appContext.getRestHost(),
                     appContext.getRestBasePathForShipmentV2(),
                     txnContext.getAuthorization());
            }
            else {
                // Default to use SHIPMENT_REST Application Credentials for Local Testing
                return ShipmentClient.newInstance
                    (appContext.getRestHost(),
                     appContext.getRestBasePathForShipmentV2(),
                     ClientCredentials.newInstance
                         (appContext.getRestClientAuthToken(),
                          appContext.getRestClientUserName(),
                          appContext.getRestClientPassword()));
            }
        }
        catch (Exception e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION);
        }
    }
    
    private CityOperationsClient getCityOperationsClient(TransactionContext txnContext)
            throws ServiceException {
        try {
            if (txnContext != null
                && StringUtils.isNotBlank(txnContext.getAuthorization())) {
                return CityOperationsClient.newInstance
                    (appContext.getRestHost(),
                     appContext.getRestBasePathForCityOperations(),
                     txnContext.getAuthorization());
            }
            else {
                // Default to use SHIPMENT_REST Application Credentials for Local Testing
                return CityOperationsClient.newInstance
                    (appContext.getRestHost(),
                     appContext.getRestBasePathForCityOperations(),
                     ClientCredentials.newInstance
                         (appContext.getRestClientAuthToken(),
                          appContext.getRestClientUserName(),
                          appContext.getRestClientPassword()));
            }
        }
        catch (Exception e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION);
        }
    }
    
    public InfrastructureClient getInfrastructureClient(TransactionContext txnContext)
            throws ServiceException {
        try {
            if (txnContext != null
                && StringUtils.isNotBlank(txnContext.getAuthorization())) {
                return InfrastructureClient.newInstance
                    (appContext.getRestHost(),
                     appContext.getRestBasePathForInfrastructure(),
                     txnContext.getAuthorization());
            }
            else {
                // Default to use SHIPMENT_REST Application Credentials for Local Testing
                return InfrastructureClient.newInstance
                    (appContext.getRestHost(),
                     appContext.getRestBasePathForInfrastructure(),
                     ClientCredentials.newInstance
                         (appContext.getRestClientAuthToken(),
                          appContext.getRestClientUserName(),
                          appContext.getRestClientPassword()));
            }
        }
        catch (Exception e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION);
        }
    }

	private DocumentManagementClient getDocumentManagementClient(final TransactionContext txnContext) throws ServiceException {
		try {
			if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
				return DocumentManagementClient.newInstance(
					appContext.getRestHost(),
					appContext.getRestBasePathForDms(),
					txnContext.getAuthorization());
			} else {
				//Default to use SHIPMENT_REST Application Credentials for Local Testing
				return DocumentManagementClient.newInstance(
					appContext.getRestHost(),
					appContext.getRestBasePathForDms(),
					ClientCredentials.newInstance(
						appContext.getRestClientAuthToken(),
						appContext.getRestClientUserName(),
						appContext.getRestClientPassword()));
			}
		}
		catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}

	private AppointmentClient getAppointmentClient(final TransactionContext txnContext) throws ServiceException {
		try {
			if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
				return AppointmentClient.newInstance(
					appContext.getRestHost(),
					appContext.getRestBasePathForAppointment(),
					txnContext.getAuthorization());
			} else {
				// Default to use SHIPMENT_REST Application Credentials for Local Testing
				return AppointmentClient.newInstance(
					appContext.getRestHost(),
					appContext.getRestBasePathForAppointment(),
					ClientCredentials.newInstance(
						appContext.getRestClientAuthToken(),
						appContext.getRestClientUserName(),
						appContext.getRestClientPassword()));
			}
		} catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}

    @Override
    public LocationClient getLocationClient(TransactionContext txnContext) throws ServiceException {
		try {
			if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
				return LocationClient.newInstance(
					appContext.getRestHost(),
					appContext.getRestBasePathForLocation(),
					txnContext.getAuthorization());
			} else {
				// Default to use SHIPMENT_REST Application Credentials for Local Testing
				return LocationClient.newInstance(
					appContext.getRestHost(),
					appContext.getRestBasePathForLocation(),
					ClientCredentials.newInstance(
						appContext.getRestClientAuthToken(),
						appContext.getRestClientUserName(),
						appContext.getRestClientPassword()));
			}
		} catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}

	public CustomerClient getCustomerClient(TransactionContext txnContext) throws ServiceException {
		// On-Prem call to Customer using the Auth token, not the Access token
		// on the request
		try {
			return CustomerClient
				.newInstance(appContext.getRestHostOnPrem(),
							 appContext.getRestBasePathForCustomer(),
							 ClientCredentials
								.newInstance(appContext.getRestClientAuthTokenOnPrem(),
											 appContext.getRestClientUserNameOnPrem(),
											 appContext.getRestClientPasswordOnPrem()));
		} catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}

    public com.xpo.ltl.api.customer.client.v2.CustomerClient getCustomerV2Client(TransactionContext txnContext) throws ServiceException {
        try {
            if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
                return com.xpo.ltl.api.customer.client.v2.CustomerClient.newInstance(
                    appContext.getRestHost(),
                    appContext.getRestBasePathForCustomerV2(),
                    txnContext.getAuthorization());
            } else {
                // Default to use SHIPMENT_REST Application Credentials for Local Testing
                return com.xpo.ltl.api.customer.client.v2.CustomerClient.newInstance(
                    appContext.getRestHost(),
                    appContext.getRestBasePathForCustomerV2(),
                    ClientCredentials.newInstance(
                        appContext.getRestClientAuthToken(),
                        appContext.getRestClientUserName(),
                        appContext.getRestClientPassword()));
            }
        } catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
            throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
        }
    }
	
	@Override
    public CarrierManagementClient getCarrierManagementClient(TransactionContext txnContext) throws ServiceException{

		try {
			if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
				return CarrierManagementClient.newInstance(
						appContext.getRestHost(), 
						appContext.getRestBasePathForCarrierManagement(), 
						txnContext.getAuthorization());
				
			} else {
				return CarrierManagementClient.newInstance(appContext.getRestHost(), 
						appContext.getRestBasePathForCarrierManagement(), 
						ClientCredentials.newInstance(
								appContext.getRestClientAuthToken(), 
								appContext.getRestClientUserName(), 
								appContext.getRestClientPassword()));
			}
		}
		catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
		
	}

	private DockOperationsClient getDockOperationsClient(final TransactionContext txnContext) throws ServiceException {
		try {
			if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
				return DockOperationsClient.newInstance(
						appContext.getRestHost(),
						appContext.getRestBasePathForDockOperations(),
						txnContext.getAuthorization());
			} else {
				//Default to use SHIPMENT_REST Application Credentials for Local Testing
				return DockOperationsClient.newInstance(
						appContext.getRestHost(),
						appContext.getRestBasePathForDockOperations(),
						ClientCredentials.newInstance(
								appContext.getRestClientAuthToken(),
								appContext.getRestClientUserName(),
								appContext.getRestClientPassword()));
			}
		}
		catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}

    @Override
	public ProNumberReengineeringClient getProNumberReengineeringClient(final TransactionContext txnContext) throws ServiceException {
		try {
			if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
				return ProNumberReengineeringClient.newInstance(
					appContext.getRestHost(),
					appContext.getRestBasePathForProNumberReengineering(),
					txnContext.getAuthorization());
			} else {
				//Default to use SHIPMENT_REST Application Credentials for Local Testing
				return ProNumberReengineeringClient.newInstance(
					appContext.getRestHost(),
					appContext.getRestBasePathForProNumberReengineering(),
					ClientCredentials.newInstance(
						appContext.getRestClientAuthToken(),
						appContext.getRestClientUserName(),
						appContext.getRestClientPassword()));
			}
		}
		catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}

    public com.xpo.ltl.api.freightflow.client.v1.FreightFlowClient getFreightFlowClientV1(final TransactionContext txnContext)
            throws ServiceException {
        try {
            if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
                return com.xpo.ltl.api.freightflow.client.v1.FreightFlowClient
                    .newInstance(appContext.getRestHost(), appContext.getRestBasePathForFreightFlow(), txnContext.getAuthorization());
            } else {
                // Default to use SHIPMENT_REST Application Credentials for Local Testing
                return com.xpo.ltl.api.freightflow.client.v1.FreightFlowClient
                    .newInstance(appContext.getRestHost(), appContext.getRestBasePathForFreightFlow(), ClientCredentials
                        .newInstance(appContext.getRestClientAuthToken(), appContext.getRestClientUserName(), appContext.getRestClientPassword()));
            }
        } catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
            throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
        }
    }

    @Override
    public FreightFlowClient getFreightFlowClient(final TransactionContext txnContext) throws ServiceException {
        try {
            if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
                return FreightFlowClient.newInstance(
                    appContext.getRestHost(),
                    appContext.getRestBasePathForFreightFlowV2(),
                    txnContext.getAuthorization());
            } else {
                //Default to use SHIPMENT_REST Application Credentials for Local Testing
                return FreightFlowClient.newInstance(
                    appContext.getRestHost(),
                    appContext.getRestBasePathForFreightFlowV2(),
                    ClientCredentials.newInstance(
                        appContext.getRestClientAuthToken(),
                        appContext.getRestClientUserName(),
                        appContext.getRestClientPassword()));
            }
        }
        catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
            throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
        }
    }
    
    private com.xpo.ltl.api.humanresource.client.v2.HumanResourceClient getHumanResourceClientV2(final TransactionContext txnContext)
			throws ServiceException {
		try {
			if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
				return com.xpo.ltl.api.humanresource.client.v2.HumanResourceClient.newInstance(appContext.getRestHost(),
						appContext.getRestBasePathForHumanResourceV2(),
						txnContext.getAuthorization());
			} else {
				//Default to use SHIPMENT_REST Application Credentials for Local Testing
				return com.xpo.ltl.api.humanresource.client.v2.HumanResourceClient.newInstance(
						appContext.getRestHost(),
						appContext.getRestBasePathForHumanResourceV2(),
						ClientCredentials.newInstance(
								appContext.getRestClientAuthToken(),
								appContext.getRestClientUserName(),
								appContext.getRestClientPassword()));
			}
		}
		catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}

	public GetShipmentResp getShipment(final String proNbr, final XMLGregorianCalendar pickupDate, final String shipmentInstId, final TransactionContext txnContext)
			throws ServiceException {

		Response<GetShipmentResp> getShipment;

		Long shipmentInstIdAsLong = null;

		if (shipmentInstId != null) {
			shipmentInstIdAsLong = Long.parseLong(shipmentInstId);
		}

		//TODO find how to get this
		ShipmentDetailCd[] shipmentDetailCds = null;

		try {
            com.xpo.ltl.api.shipment.client.v1.ShipmentClient shipmentClient =
                getShipmentV1Client(txnContext);
			getShipment = shipmentClient.getShipment(proNbr, pickupDate, shipmentInstIdAsLong, shipmentDetailCds);
		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}

		if (getShipment != null) {
			return getShipment.getData();
		}

		else return null;
	}

    public void startCreateBaseLogChEnsemble(BaseLogTypeEnum logId,
                                             List<BaseLog> baseLogs,
                                             TransactionContext txnContext)
            throws ServiceException {
        try {
            StartCreateBaseLogChEnsembleRqst request =
                new StartCreateBaseLogChEnsembleRqst();
            request.setEnsembleName("CreateBaseLog");
            CreateBaseLogPayload payload = new CreateBaseLogPayload();
            request.setPayload(payload);
            payload.setLogId(logId.getCode());
            payload.setBaseLogs(baseLogs);

            ShipmentClient client = getShipmentV2Client(txnContext);
            client.startCreateBaseLogChEnsemble(request);
        }
        catch (Throwable e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION);
        }
    }

	public String retrieveDmsAuthToken(final TransactionContext txnContext) throws ServiceException {

		Response<RetrieveDmsAuthTokenResp> dmsAuthToken;

		try {
			final DocumentManagementClient documentManagementClient = getDocumentManagementClient(txnContext);
			dmsAuthToken = documentManagementClient.retrieveDmsAuthToken();
		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
		return dmsAuthToken.getData().getAccessToken();
	}

	public ArchiveDocumentResp archiveDocument(final Attachment inputFile, final JsonAttachment<DmsArchiveRequest> archiveRequest,
		final String dmsAuthToken, final TransactionContext txnContext) throws ServiceException {

		Response<ArchiveDocumentResp> archiveDocument;
		String corpCode = appContext.getDmsCorpCode();
		String docClass = appContext.getDocClass();

		try {

			final DocumentManagementClient documentManagementClient = getDocumentManagementClient(txnContext);
			archiveDocument = documentManagementClient.archiveDocument(corpCode, docClass, inputFile, archiveRequest, dmsAuthToken);
		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
		return archiveDocument.getData();
	}

	/**
	 * Retrieves notification information for a Shipment.-
	 *
	 * @param shipmentInstId
	 *            the shipment ID to retrieve the notifications from
	 * @param txnContext
	 *            the current transaction context
	 * @return a {@link GetActiveApptNotificationForShipmentResp} with notification information
	 * @throws ServiceException
	 */
	public GetActiveApptNotificationForShipmentResp getNotification(
		final Long shipmentInstId,
		final TransactionContext txnContext) throws ServiceException {

		Response<GetActiveApptNotificationForShipmentResp> notificationForShipment = null;
		try {
			final AppointmentClient appointmentClient = getAppointmentClient(txnContext);

			notificationForShipment = appointmentClient.getActiveApptNotificationForShipment(shipmentInstId);

		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
		return notificationForShipment.getData();
	}

	public ListAppointmentNotificationsForShipmentsResp listShipmentAppointmentNotifications(final List<Long> shipmentInstIds,
			final TransactionContext txnContext) throws ServiceException {

		Response<ListAppointmentNotificationsForShipmentsResp> shipmentAppointmentNotifications = null;
		try {
			ListAppointmentNotificationsForShipmentsRqst rqst = new ListAppointmentNotificationsForShipmentsRqst();
			rqst.setShipmentInstIds(shipmentInstIds);
			final AppointmentClient appointmentClient = getAppointmentClient(txnContext);
			shipmentAppointmentNotifications = appointmentClient.listAppointmentNotificationsForShipments(rqst);
		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
		return shipmentAppointmentNotifications.getData();
	}

    public GetLocReferenceDetailsBySicResp getLocReferenceDetailsBySic(String sicCd, TransactionContext txnContext)
            throws ServiceException {
        try {
            StringCacheKey key = StringCacheKey.of(sicCd, txnContext);
            GetLocReferenceDetailsBySicResp details =
                locReferenceDetailsBySicCache.get(key);
            return details;
        }
        catch (Throwable e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "LocationClient.getLocReferenceDetailsBySic");
        }
        finally {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("getLocReferenceDetailsBySic: " + locReferenceDetailsBySicCache.stats());
        }
    }

    public ListLocationFeaturesResp getLocFeatureSetting(String featureCd, TransactionContext txnContext) throws ServiceException {
        try {
            LocationFeaturesBySettingValueCacheKey key = LocationFeaturesBySettingValueCacheKey.of(featureCd, null, txnContext);
            ListLocationFeaturesResp details = locationFeatureSettingCache.get(key);
            return details;
        } catch (Throwable e) {
            throw ServiceExceptionUtil
                .createException(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "LocationClient.getLocFeatureSetting");
        } finally {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("getLocFeatureSetting: " + locationFeatureSettingCache.stats());
        }
    }

	public GetLocationReferenceDetailsResp getLocationReferenceDetails
            (String sicCd,
             TransactionContext txnContext)
            throws ServiceException {
        try {
            StringCacheKey key = StringCacheKey.of(sicCd, txnContext);
            GetLocationReferenceDetailsResp details =
			locationReferenceDetailsCache.get(key);
            return details;
        }
        catch (Throwable e) {
        	throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, e, txnContext)
				.contextValues("LocationClient.getLocationReferenceDetails").build();
        }
        finally {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("getLocationReferenceDetails: " + locationReferenceDetailsCache.stats());
        }
    }
	
    public GetLocReferenceDetailsBySicResp getLocReferenceDetails(String sicCd, TransactionContext txnContext)
            throws ServiceException {
    	Response<GetLocReferenceDetailsBySicResp> response = null;
		try {
			LocationClient locationClient = getLocationClient(txnContext);
			response = locationClient.getLocReferenceDetailsBySic(sicCd, null);

		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
		return response.getData();
    }

    /**
     * This operation retrieves the Loc Address record for the requested SIC.
     * <br/>
     * <br/>
     * Pre-condition:
     * <br/>
     * 1. A valid reference SIC is provided.
     * <br/>
     * <br/>
     * Post-conditions:
     * <br/>
     * 1. If successful, then the corresponding Loc Address record is returned.
     * <br/>
     * 2. Otherwise an appropriate error message is returned.
     */
    public GetRefSicAddressResp getRefSicAddress(String sicCd, TransactionContext txnContext)
            throws ServiceException {

        Response<GetRefSicAddressResp> response;
        try {
            LocationClient locationClient = getLocationClient(txnContext);

            response = locationClient.getRefSicAddress(sicCd);

            if (Objects.nonNull(response)) {
                return response.getData();
            } else {
                return null;
            }

        } catch (final Exception e) {
            throw ServiceExceptionUtil
                .createException(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION,
                    "LocationClient" + ".getRefSicAddress");
        }

    }

    public GetZoneAndSatelliteBySicResp getZoneAndSatelliteBySic(String sicCd,
                                                                 Boolean zoneInd,
                                                                 Boolean satelliteInd,
                                                                 TransactionContext txnContext)
            throws ServiceException {
        try {
            ZoneAndSatelliteBySicCacheKey key =
                ZoneAndSatelliteBySicCacheKey.of
                    (sicCd, zoneInd, satelliteInd, txnContext);
            return zoneAndSatelliteBySicCache.get(key);
        }
        catch (Throwable e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "LocationClient.getZoneAndSatelliteBySic");
        }
        finally {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("getZoneAndSatelliteBySic: " + zoneAndSatelliteBySicCache.stats());
        }
    }

	/**
	 * Retrieves Location profitability for a Sic Code.-
	 *
	 * @param sicCode
	 *            the SIC code to retrieve the location profitability info from
	 * @param txnContext
	 *            the current transaction context
	 * @return a {@link GetLocOperationsServiceCenterProfitabilityBySicResp} with location profitability
	 * @throws ServiceException
	 */
	public GetLocOperationsServiceCenterProfitabilityBySicResp getLocationProfitabilityBySic(
		final String sicCode,
		final TransactionContext txnContext) throws ServiceException {

		Response<GetLocOperationsServiceCenterProfitabilityBySicResp> response = null;
		try {
			LocationClient locationClient = getLocationClient(txnContext);

			response = locationClient
					.getLocOperationsServiceCenterProfitabilityBySic(sicCode);

		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
		return response.getData();
	}

	/**
	 * Get the SIC for a list of US or CA postal Codes.-
	 *
	 * @param postalCds
	 *            The list of US or CA postal codes for which the SIC will be retrieved
	 * @param txnContext
	 *            the current transaction context
	 * @return a {@link GetSicForPostalCodesResp} with the SIC, state, country, and other details
	 * for the postal codes.
	 * @throws ServiceException
	 */
	public GetSicForPostalCodesResp getSicForPostalCodes(String[] postalCds, TransactionContext txnContext)
			throws ServiceException {

		try {
            SetCacheKey<String> key = SetCacheKey.of(txnContext, postalCds);
            return sicForPostalCodesCache.get(key);
        }
        catch (Throwable e) {
            throw ExceptionBuilder
                .exception
                    (ServiceErrorMessage.UNEXPECTED_EXCEPTION,
                     ExceptionUtils.getRootCause(e),
                     txnContext)
                .contextValues("Location.getSicForPostalCodes")
                .build();
        }

	}

	public DetermineRestrictedBillToResp getCustomerRestrictedInfo(Long cisCustNbr,
			TransactionContext txnContext) throws ServiceException {

		Response<DetermineRestrictedBillToResp> resp = null;
		DetermineRestrictedBillToResp custRestrictedResp = null;

		try {
			CustomerClient customerClient = getCustomerClient(txnContext);

			resp = customerClient
					.determineRestrictedBillTo(cisCustNbr);
			if (resp != null) {
				custRestrictedResp = resp.getData();
			}

		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}

		return custRestrictedResp;
	}

	/**
	 * Get a trailer specification.-
	 *
	 * @param trailerTypeCd valid trailer type Code
	 * @param trailerSubTypeCd valid trailer sub type code
	 * @param effectiveDt (optional) The string representation of the trailer specification's
	 *                       effective date
	 * @param txnContext
	 *            the current transaction context
	 * @return a {@link GetTrailerSpecificationResp} with the trailer specification
	 * @throws ServiceException
	 */
	public GetTrailerSpecificationResp getTrailerSpecification(String trailerTypeCd,
			String trailerSubTypeCd, String effectiveDt, TransactionContext txnContext)
			throws ServiceException {

		Response<GetTrailerSpecificationResp> response;
		try {
			DockOperationsClient dockOperationsClient = getDockOperationsClient(txnContext);

			response = dockOperationsClient
					.getTrailerSpecification(trailerTypeCd, trailerSubTypeCd, effectiveDt);

			if (Objects.nonNull(response)) {
				return response.getData();
			} else {
				return null;
			}


		} catch (final Exception e) {
			throw ServiceExceptionUtil.createException
					(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "DockOperationsClient"
							+ ".getTrailerSpecification");
		}

	}
	
	public GetProLoadDetailsResp getProLoadDetails(String proNbr,
		String sicCd, String shiftCd, TransactionContext txnContext)
		throws ServiceException {

	Response<GetProLoadDetailsResp> response;
	try {
		DockOperationsClient dockOperationsClient = getDockOperationsClient(txnContext);

		response = dockOperationsClient
				.getProLoadDetails(sicCd, shiftCd, proNbr, null, null, null, null, null);

		if (Objects.nonNull(response)) {
			return response.getData();
		} else {
			return null;
		}


	} catch (final Exception e) {
		throw ServiceExceptionUtil.createException
				(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "DockOperationsClient"
						+ ".getTrailerSpecification");
	}

}

	public GetTrailerLoadByEquipmentIdResp getTrailerLoadDetails(ShmShipment shipment,  TransactionContext txnContext)
			throws ServiceException {
		Response<GetTrailerLoadByEquipmentIdResp> response = null;
		try {
			DockOperationsClient dockOperationsClient = getDockOperationsClient(txnContext);
			
			ShmMovement mvmt = 
				CollectionUtils.emptyIfNull(shipment.getShmMovements())
				.stream()
				.sorted((o1, o2) -> o2.getMvmtTmst().compareTo(o1.getMvmtTmst()) )
				.filter(shmMvmt -> shmMvmt.getTrlrInstId() != null)
	            .findFirst()
	            .orElse(null);
			
			if(mvmt != null 
			&& mvmt.getTrlrInstId() != null
			&& BasicTransformer.toInt(mvmt.getTrlrInstId()) != 0) {
				response = dockOperationsClient
						.getTrailerLoadByEquipmentId(BasicTransformer.toLong(mvmt.getTrlrInstId()));
			}
			else if (Objects.nonNull(shipment.getShpInstId())) {
				Stream<Long> shipmentInstId = Stream.of(shipment.getShpInstId());
				List<String> shipmentInstIdStrings = shipmentInstId.map(shmInstId -> shmInstId.toString())
						.collect(Collectors.toList());
				List<DeliveryShipmentSearchRecord> elasticRecords = getDeliveryShipmentElasticRecords(
						shipmentInstIdStrings, txnContext);
				DeliveryShipmentSearchRecord elasticRecord = CollectionUtils.emptyIfNull(elasticRecords).stream()
						.findFirst().orElse(null);
				if (Objects.nonNull(elasticRecord)) {
					Long equipmentInstId = elasticRecord.getEquipmentInstId();
					if (Objects.nonNull(equipmentInstId) && NumberUtil.isNonZero(equipmentInstId)) {
						response = dockOperationsClient.getTrailerLoadByEquipmentId(equipmentInstId);
					}
				}
			}
			if (Objects.nonNull(response)) {
				return response.getData();
			} else {
				return null;
			}


		} catch (final Exception e) {
			throw ServiceExceptionUtil.createException
					(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "DockOperationsClient"
							+ ".getTrailerSpecification");
		}
		
	}
	private boolean isActiveSicByLocReference(LocationReference locationReference) {

		boolean result = false;
		Date today = new Date();

		if (locationReference != null && locationReference.getEffDate() != null && BasicTransformer
				.toDate(locationReference.getEffDate()).before(today) && (
				locationReference.getExpiryDate() == null || BasicTransformer
						.toDate(locationReference.getExpiryDate()).after(today))) {
			result = true;
		}

		return result;
	}

	private boolean isLinehaulSicByCompanyOperations(CompanyOperations companyOperations) {

		boolean result = false;

		if (companyOperations != null && companyOperations.getFreightLtlMovementFlag() != null
				&& companyOperations.getFreightLtlMovementFlag().equals(BigInteger.ONE)) {
			result = true;
		}

		return result;
	}

	public boolean isActiveSic(String sicCd, TransactionContext txnContext)
			throws ServiceException {

		GetLocReferenceDetailsBySicResp locReferenceDetails = this
				.getLocReferenceDetailsBySic(sicCd, txnContext);
		if (locReferenceDetails == null) {
			return false;
		}
		return isActiveSicByLocReference(locReferenceDetails.getLocReference());
	}

	public boolean isActiveSicAndLinehaul(String sicCd, TransactionContext txnContext)
			throws ServiceException {

		GetLocReferenceDetailsBySicResp locReferenceDetails = this
				.getLocReferenceDetailsBySic(sicCd, txnContext);
		if (locReferenceDetails == null) {
			return false;
		}
		return isActiveSicByLocReference(locReferenceDetails.getLocReference())
				&& isLinehaulSicByCompanyOperations(locReferenceDetails.getLocCompanyOperations());
	}

	public boolean isNotActiveSicAndLinehaul(String sicCd, TransactionContext txnContext)
			throws ServiceException {
		return !isActiveSicAndLinehaul(sicCd, txnContext);
	}

	/**
	 * List BOL Pro Prefixes
	 *
	 * @param inUseCdList List of ProInUseCd
	 * @param bolProPrefix (optional) valid BOL Pro prefix to match
	 * @param lastInUseChangeDt (optional) The string representation of the last date the inUseCd changed
	 * @param txnContext
	 *            the current transaction context
	 * @return a {@link ListProBolPrefixMasterResp} with BOL Pro prefix/es satisfying the criteria
	 * @throws ServiceException
	 */
	public ListProBolPrefixMasterResp listProBolPrefixMaster(List<ProInUseCd> inUseCdList,
			String bolProPrefix, String lastInUseChangeDt, TransactionContext txnContext)
			throws ServiceException {

		Response<ListProBolPrefixMasterResp> response;
		try {
			ProNumberReengineeringClient proNumberReengineeringClient = getProNumberReengineeringClient(txnContext);

			final ListProBolPrefixMasterRqst listProBolPrefixMasterRqst = new ListProBolPrefixMasterRqst();
			listProBolPrefixMasterRqst.setInUseCd(inUseCdList);

			response = proNumberReengineeringClient
					.listProBolPrefixMaster(listProBolPrefixMasterRqst, bolProPrefix, lastInUseChangeDt);

			if (Objects.nonNull(response)) {
				return response.getData();
			} else {
				return null;
			}


		} catch (final Exception e) {
			throw ServiceExceptionUtil.createException
					(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "ProNumberReengineeringClient"
							+ ".listProBolPrefixMaster");
		}

	}

    public ListProBolPrefixMasterResp listNonLegacyProBolPrefixMaster(TransactionContext txnContext)
            throws ServiceException {
        try {
            ProBolPrefixMasterCacheKey key =
                    ProBolPrefixMasterCacheKey.of(NON_LEGACY_IN_USE_CD_LIST, null, null, txnContext);
            ListProBolPrefixMasterResp details =
                proBolPrefixMasterCache.get(key);
            return details;
        }
        catch (Throwable e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "ProNumberReengineeringClient.listLegacyProBolPrefixMaster");
        }
        finally {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("listLegacyProBolPrefixMaster: " + proBolPrefixMasterCache.stats());
        }
    }

    public GetShipperProfileFreightCubeResp getShipperProfileFreightCube(
        GetShipperProfileFreightCubeRqst getShipperProfileFreightCubeRqst, Long shipperId,
        final TransactionContext txnContext) throws com.xpo.ltl.api.exception.ServiceException {
        try {
            LOGGER.info("Calling getShipperProfileFreightCube customer.");
            CustomerClient customerClient = getCustomerClient(txnContext);

            final Response<GetShipperProfileFreightCubeResp> response = customerClient
                .getShipperProfileFreightCube(getShipperProfileFreightCubeRqst, shipperId);

            return Objects.nonNull(response) ? response.getData() : null;

        } catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
            throw ServiceExceptionUtil.createException(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION,
                "CustomerClient" + ".getShipperProfileFreightCube");
        }
    }

    public GetOperationalServiceDaysCountResp getOperationalServiceDaysCount(String originSic, String destSic,
        String pickupDate, String endDate, ServiceTypeCd serviceTypeCd, TransactionContext txnContext)
            throws ServiceException {
        try {
            LocationClient client = getLocationClient(txnContext);
            Response<GetOperationalServiceDaysCountResp> response = client.getOperationalServiceDaysCount(originSic,
                destSic, pickupDate, endDate, serviceTypeCd);
            return response.getData();
        } catch (Throwable e) {
            throw ServiceExceptionUtil.createException(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION,
                "LocationClient.getOperationalServiceDaysCount");
        }
    }

    public DetermineOperationalServiceDateResp getOperationalServiceDate(String originSic, String destSic,
        String pickupDate, ServiceTypeCd serviceTypeCd, int offsetDaysCount, boolean findNextServiceDateInd,
        TransactionContext txnContext) throws ServiceException {
        try {
            LocationClient client = getLocationClient(txnContext);
            Response<DetermineOperationalServiceDateResp> response = client.determineOperationalServiceDate(originSic,
                destSic, pickupDate, serviceTypeCd, offsetDaysCount, findNextServiceDateInd);
            return response.getData();
        } catch (Throwable e) {
            throw ServiceExceptionUtil.createException(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION,
                "LocationClient.getOperationalServiceDate");
        }
    }
    
    public DetermineOperationalServiceDateResp getOperationalServiceDates(String originSic, String destSic,
            String pickupDate, ServiceTypeCd serviceTypeCd, int offsetDaysCount, boolean findNextServiceDateInd,
            TransactionContext txnContext)
            throws ServiceException {
        try {
        	LocalDate pkupDate = LocalDate.parse(pickupDate);
            DetermineOperationalServiceDateCacheKey key = DetermineOperationalServiceDateCacheKey.of(originSic,
                    destSic, pkupDate, serviceTypeCd, offsetDaysCount, findNextServiceDateInd, txnContext);
            return DetermineOperationalServiceDateLoaderCache.get(key);
        }
        catch (Throwable e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "LocationClient.getOperationalServiceDate");
        }
    }

    public GetHostSicDetailsResp getHostSicDetails(String sicCd, TransactionContext txnContext)
            throws ServiceException {
        try {
            StringCacheKey key = StringCacheKey.of(sicCd, txnContext);
            return hostSicDetailsCache.get(key);
        }
        catch (Throwable e) {
            throw ServiceExceptionUtil.createException
                (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "LocationClient.getHostSicDetails");
        }
        finally {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("getHostSicDetails: " + hostSicDetailsCache.stats());
        }
    }

    /**
	 * Get Client to interact with Human Resource SDK
	 *
	 * @param txnContext
	 * @return
	 * @throws com.xpo.ltl.api.exception.ServiceException
	 */
	private HumanResourceClient getHumanResourceClient(final TransactionContext txnContext)
			throws ServiceException {
		try {
			if (txnContext != null && StringUtils.isNotBlank(txnContext.getAuthorization())) {
				return HumanResourceClient.newInstance(appContext.getRestHost(),
						appContext.getRestBasePathForHumanResource(),
						txnContext.getAuthorization());
			} else {
				//Default to use SHIPMENT_REST Application Credentials for Local Testing
				return HumanResourceClient.newInstance(
						appContext.getRestHost(),
						appContext.getRestBasePathForHumanResource(),
						ClientCredentials.newInstance(
								appContext.getRestClientAuthToken(),
								appContext.getRestClientUserName(),
								appContext.getRestClientPassword()));
			}
		}
		catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}

	/**
	 *
	 * @param employeeId
	 * @param txnContext
	 * @return
	 * @throws ServiceException
	 */
	public Employee getEmployeeDetailsByEmployeeId(String employeeId,
			TransactionContext txnContext) throws ServiceException {

		HumanResourceClient client = getHumanResourceClient(txnContext);
		Response<GetEmployeeDetailsByEmpIdResp> resp = null;
		Employee result = null;

		try {
			XMLGregorianCalendar effectiveDate = BasicTransformer
					.toXMLGregorianCalendar(new Date());
			resp = client.getEmployeeDetailsByEmpId(employeeId, effectiveDate, null);

			if ( resp!= null && resp.getData() != null && resp.getData().getEmployee() != null
					&& resp.getData().getEmployee().getBasicInfo() != null) {
				result = resp.getData().getEmployee();
			}
			return result;

		} catch (final Exception e) {
			throw ServiceExceptionUtil.createException(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION,
					"HumanResourceClient.getEmployeeDetailsByEmployeeId");
		}
	}

    public Map<String, InterfaceEmployee> listEmployeesByRacfId(List<String> racfIds, TransactionContext txnContext) throws ServiceException {

        Map<String, InterfaceEmployee> racfIdEmployeeDetailsMap = new HashMap<>();
        
        try {
            com.xpo.ltl.api.humanresource.client.v2.HumanResourceClient client = getHumanResourceClientV2(txnContext);
            ListEmployeesRqst request = new ListEmployeesRqst();
            request.setRacfIds(racfIds);
            Response<ListEmployeesResp> resp = client.listEmployees(request);
            if (null != resp) {
                ListEmployeesResp result = resp.getData();
                racfIdEmployeeDetailsMap =  CollectionUtils.emptyIfNull(result.getInterfaceEmployee()).stream()
                        .collect( Collectors.toMap(InterfaceEmployee::getRacfId, 
                                Function.identity()) );
            }
            return racfIdEmployeeDetailsMap;
        } catch (Throwable e) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("listEmployeesByRacfId: " + e);
            return null;
        }
    }
	
	/**
	 *
	 * @param listEmployeesByEmpIdRqst
	 * @param txnContext
	 * @return
	 * @throws ServiceException
	 */
	public List<EmployeeName> listEmployeesByEmpIds(ListEmployeesByEmpIdRqst listEmployeesByEmpIdRqst,
			TransactionContext txnContext) throws ServiceException {

		HumanResourceClient client = getHumanResourceClient(txnContext);
		Response<ListEmployeesByEmpIdResp> resp = null;
		List<EmployeeName> result = null;

		try {
			XMLGregorianCalendar effectiveDate = BasicTransformer
					.toXMLGregorianCalendar(new Date());
			resp = client.listEmployeesByEmpId(listEmployeesByEmpIdRqst);

			if ( resp!= null && resp.getData() != null && resp.getData().getEmployeeNames() != null
					) {
				result = resp.getData().getEmployeeNames();
			}
			return result;

		} catch (final Exception e) {
			throw ServiceExceptionUtil.createException(txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION,
					"HumanResourceClient.listEmployeesByEmpId");
		}
	}
    /**
     * calculateTransitTime
     *
     * @param calculateTransitTimeRqst
     * @param txnContext
     * @return
     * @throws com.xpo.ltl.api.exception.ServiceException
     */ 
	public CalculateTransitTimeResp calculateTransitTime(CalculateTransitTimeRqst calculateTransitTimeRqst, final TransactionContext txnContext)
            throws ServiceException, com.xpo.ltl.api.exception.ServiceException {

        try {
            List<TransitTimeBasic> transitTimeBasics = calculateTransitTimeRqst.getTransitTime();
            List<CalculateTransitTimeCacheKey> calculateTransitTimeCacheKeys = new ArrayList<>();
            for(TransitTimeBasic transitTimeBasic : transitTimeBasics) {
                calculateTransitTimeCacheKeys.add(
                    CalculateTransitTimeCacheKey.of(transitTimeBasic.getOrigPostalCd(), transitTimeBasic.getDestPostalCd(),
                    		transitTimeBasic.getRequestedPkupDate(), txnContext));
            }
            SetCacheKey<CalculateTransitTimeCacheKey> key = 
                    SetCacheKey.of(calculateTransitTimeCacheKeys, txnContext);
            return newCalculateTransitTimesCache.get(key);
            
        } catch (ExecutionException e) {
            throw ExceptionBuilder
                .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                .moreInfo("calculateTransitTime", e.getMessage())
                .build();
        }

    }
    
    public GetCarrierDetailsResp getCarrierDetails(long carrierId,String scacCd,
		  	   TransactionContext txnContext) throws ServiceException {
		
    	try {
    		CarrierDetailsCacheKey key = CarrierDetailsCacheKey.of(carrierId, scacCd, txnContext); 
    		GetCarrierDetailsResp resp = 
        		carrierDetailsCache.get(key);
    		return resp;
    	} catch(Throwable e) {
            throw ServiceExceptionUtil.createException
            (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION, "CarrierClient.getCarrierDetails");  
    	}
    	finally {
    		if (LOGGER.isDebugEnabled())
    			LOGGER.debug("getCarrierDetails: " + carrierDetailsCache.stats());
    	}
    	
	}
    
	public ArchiveDocumentResp archiveOverageImageDocument(String documentClass, final Attachment inputFile, final JsonAttachment<DmsArchiveRequest> archiveRequest,
			final String dmsAuthToken, final TransactionContext txnContext) throws ServiceException {

			Response<ArchiveDocumentResp> archiveDocument;
			String corpCode = appContext.getDmsCorpCode();
			String docClass = documentClass;

			try {

				final DocumentManagementClient documentManagementClient = getDocumentManagementClient(txnContext);
				archiveDocument = documentManagementClient.archiveDocument(corpCode, docClass, inputFile, archiveRequest, dmsAuthToken);
			} catch (final Exception e) {
				throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
			}
			return archiveDocument.getData();
		}


	public List<DeliveryShipmentSearchRecord> getDeliveryShipmentElasticRecords(final List<String> shipmentInstIds,
		final TransactionContext txnContext) throws ServiceException {
		CityOperationsClient cityOperationsClient = null;
		try {
			cityOperationsClient = getCityOperationsClient(txnContext);
		} catch (final ServiceException e) {
			throw e;
		}
		try {
			SearchDeliveryShipmentsRqst rqst = new SearchDeliveryShipmentsRqst();
			DeliveryShipmentSearchFilter filter = new DeliveryShipmentSearchFilter();
			filter.setShipmentInstId(buildXrtAttributeFilterCityOpsMultiple(shipmentInstIds));
			rqst.setFilter(filter);
			rqst.setFields(FIELDS);
			rqst.setSortExpressions(buildSortExpressionCityOps("pickupDt.keyword"));
			rqst.setPageSize(BigInteger.valueOf(1000)); 
			final Response<SearchDeliveryShipmentsResp> resp = cityOperationsClient.searchDeliveryShipments(rqst);
			if(resp != null && resp.getData() != null && CollectionUtils.isNotEmpty(resp.getData().getResult())) {
				return resp.getData().getResult();
			}
			return null;
		} catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
			
			String searchIds = String.join(", ", shipmentInstIds);
			throw ExceptionBuilder
				.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
				.contextValues(String.format("Search input: %s. Error: %s", searchIds, e.getMessage()))
				.moreInfo(searchIds, "getUnassignedShipmentFromCityOpsElastic")
				.log(e)
				.build();
		}
	}

    public SearchDeliveryShipmentsResp searchDeliveryShipment(SearchDeliveryShipmentsRqst  request, TransactionContext txnContext) throws ServiceException {
        
        try {
            CityOperationsClient client = getCityOperationsClient(txnContext);
            Response<SearchDeliveryShipmentsResp> response =  client.searchDeliveryShipments(request);
            return response.getData();
        }
        catch (Throwable e) {
            throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, e, txnContext)
                .contextValues("CityOperationsClient.searchDeliveryShipments").build();
        }
    }
	
	private List<XrtSortExpression> buildSortExpressionCityOps(final String sortColumnName) {
		final XrtSortExpression xrtSortExpression = new XrtSortExpression();
		xrtSortExpression.setColumn(sortColumnName);
		xrtSortExpression.setIsDescendingSort(true);
		return Arrays.asList(xrtSortExpression);
	}
	
	private XrtAttributeFilter buildXrtAttributeFilterCityOps(final String searchTxt) {
		final XrtAttributeFilter xrtAttributeFilter = new XrtAttributeFilter();
		xrtAttributeFilter.setEquals(searchTxt);
		return xrtAttributeFilter;
	}
	
	private XrtAttributeFilter buildXrtAttributeFilterCityOpsMultiple(final List<String> searchTxts) {
		final XrtAttributeFilter xrtAttributeFilter = new XrtAttributeFilter();
		xrtAttributeFilter.setValues(searchTxts);
		return xrtAttributeFilter;
	}
	
	public void bulkPerformLegacyUpdates(List<ShipmentSkeletonResponse> shipmentSkeletonResponseList,
			 MaintainBolDocImpl maintainBolDocImpl,
				ExternalRestClient client,TransactionContext txnContext) throws ServiceException {
		
		try{
			Collection<ShipmentSkeletonLegacyUpdatesTask> tasks = new ArrayList<>();
		
			 for (ShipmentSkeletonResponse shipmentSkeletonResponse : CollectionUtils
			            .emptyIfNull(shipmentSkeletonResponseList)) {
	        	ShipmentSkeletonLegacyUpdatesTask task =
	                new ShipmentSkeletonLegacyUpdatesTask(shipmentSkeletonResponse, maintainBolDocImpl, client, txnContext);
	            tasks.add(task);
	        }

	        Collection<ShipmentSkeletonLegacyUpdatesResult> shipmentSkeletonLegacyUpdatesResult = 
	        		TaskProcessor.process(tasks, 
	        				appContext.getShipmentSkeletonLegacyUpdateMaxConcurrent(), 
	        				appContext.getShipmentSkeletonLegacyUpdateMaxAttempts(), 
	        				appContext.getShipmentSkeletonLegacyUpdateRetryDelayMillis(), 
	        				appContext.getExecutor(), 
	        				appContext.getShipmentSkeletonLegacyUpdateTimeoutMillis());
	        
	        String creatErrorMsg = CollectionUtils.emptyIfNull(shipmentSkeletonLegacyUpdatesResult).stream().filter(result -> (null != result.getMessage()) &&(!"Success".equalsIgnoreCase(result.getMessage()))).map(result->result.getMessage()).collect(Collectors.joining(","));
	           if(StringUtils.isNotEmpty(creatErrorMsg)) {
	        	   throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.moreInfo("bulkPerformLegacyUpdates ", creatErrorMsg)
					.build();
	           }
	 }
    catch (Throwable e) {
		throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
		.contextValues("bulkPerformLegacyUpdates ").build();
	     }
	
}
	
	public PrintOption getServiceCenterPrintPreferences(final String sicCd,
			final TransactionContext txnContext) throws ServiceException {
		CityOperationsClient cityOperationsClient = null;
			try {
				cityOperationsClient = getCityOperationsClient(txnContext);
			} catch (final ServiceException e) {
				if (LOGGER.isErrorEnabled())
	                LOGGER.error("getServiceCenterPrintPreferences: " + e);
				return null;
			}
			try {
				final Response<GetServiceCenterPrintPreferencesResp> resp = cityOperationsClient.getServiceCenterPrintPreferences(sicCd);
				if(resp != null && resp.getData() != null && null != resp.getData().getPrintPreferences()) {
					return resp.getData().getPrintPreferences();
				}
				return null;
			} catch (final com.xpo.ltl.api.client.exception.ServiceException e) {
				if (LOGGER.isErrorEnabled())
	                LOGGER.error("getServiceCenterPrintPreferences: " + e);
				return null;
			}
		}
	
	public Collection<CalculateTransitTimeResult> getCalculateTransitTime(Collection<CalculateTransitTimeTask> tasks, TransactionContext txnContext) throws ServiceException {
    	try{
	        Collection<CalculateTransitTimeResult> getCalculateTransitTimeResp = 
	        		TaskProcessor.process(tasks, 
	        				appContext.getBulkCreateShipmentSkeletonMaxConcurrent(), 
	        				appContext.getBulkCreateShipmentSkeletonMaxAttempts(), 
	        				appContext.getBulkCreateShipmentSkeletonRetryDelayMillis(), 
	        				appContext.getExecutor(), 
	        				appContext.getBulkCreateShipmentSkeletonTimeoutMillis());

	        String creatErrorMsg = CollectionUtils.emptyIfNull(getCalculateTransitTimeResp).stream().filter(result -> (null != result.getMessage()) 
	        		&&(!"Success".equalsIgnoreCase(result.getMessage()))).map(result->result.getMessage()).collect(Collectors.joining(","));
	           if(StringUtils.isNotEmpty(creatErrorMsg)) {
	        	   throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                       .moreInfo("getCalculateTransitTime ", creatErrorMsg)
					.build();
	           }
	           return getCalculateTransitTimeResp;
	 } 
    catch (Throwable e) {
		throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION,txnContext)
            .moreInfo("getCalculateTransitTime ", e.getMessage())
            .build();
	     }
	}

    public Collection<CalculateTransitTimesResult> getCalculateTransitTimes(Collection<CalculateTransitTimesTask> tasks, TransactionContext txnContext) throws ServiceException {
        try{
            Collection<CalculateTransitTimesResult> getCalculateTransitTimeResp = 
                    TaskProcessor.process(tasks, 
                            appContext.getBulkCreateShipmentSkeletonMaxConcurrent(), 
                            appContext.getBulkCreateShipmentSkeletonMaxAttempts(), 
                            appContext.getBulkCreateShipmentSkeletonRetryDelayMillis(), 
                            appContext.getExecutor(), 
                            appContext.getBulkCreateShipmentSkeletonTimeoutMillis());

            String creatErrorMsg = CollectionUtils.emptyIfNull(getCalculateTransitTimeResp).stream().filter(result -> (null != result.getMessage()) 
                    &&(!"Success".equalsIgnoreCase(result.getMessage()))).map(result->result.getMessage()).collect(Collectors.joining(","));
               if(StringUtils.isNotEmpty(creatErrorMsg)) {
                   throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                       .moreInfo("getCalculateTransitTime ", creatErrorMsg)
                    .build();
               }
               return getCalculateTransitTimeResp;
     } 
    catch (Throwable e) {
        throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION,txnContext)
            .moreInfo("getCalculateTransitTime ", e.getMessage())
            .build();
         }
    }

	public ListLocationFeatureValuesByFeatureResp getListLocationFeatureValuesByFeatureResp(final String flagCode, final TransactionContext txnContext) throws ServiceException {
		Response<ListLocationFeatureValuesByFeatureResp> response = null;
		try {
			LocationClient locationClient = getLocationClient(txnContext);
			response = locationClient.listLocationFeatureValuesByFeature(flagCode, null);
		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
		return response.getData();
	}
      
	public void deleteHandlingUnit(final String childProNbr, final TransactionContext txnContext) throws ServiceException {
		try {
			java.util.Date today = new java.util.Date();
			ShipmentClient shipmentClient =  getShipmentV2Client(txnContext);
			shipmentClient.deleteHandlingUnit(childProNbr, today.getTime());
		} catch (final Exception e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e).build();
		}
	}
	
	public void createNonRevenueShipment(CreateNonRevenueShipmentRqst request,
	    TransactionContext txnContext)
	            throws ServiceException {
	    try {
	        ShipmentClient client = getShipmentV2Client(txnContext);
	        client.createNonRevenueShipment(request);
	    }
	    catch (Throwable e) {
	        throw ServiceExceptionUtil.createException
	        (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION);
	    }
	}
	
	public void startApprovedSalvagesChEnsemble(Long salvageId, TransactionContext txnContext)
            throws ServiceException {
    try {
        ShipmentClient client = getShipmentV2Client(txnContext);
        StartApprovedSalvagesChEnsembleRqst request = new StartApprovedSalvagesChEnsembleRqst();
        request.setEnsembleName("ApprovedSalvages");
        request.setBusinessKey1(String.valueOf(salvageId));
        ApprovedSalvagesPayload payload = new ApprovedSalvagesPayload();
        payload.setSalvageRequestId(salvageId);
        request.setPayload(payload);
        client.startApprovedSalvagesChEnsemble(request);
    }
    catch (Throwable e) {
        throw ServiceExceptionUtil.createException
        (txnContext, e, ServiceErrorMessage.UNEXPECTED_EXCEPTION);
    }
}
	
	public Map<String,InterfaceEmployee> getEmployeeDetailsMap(List<String> employeeIds, TransactionContext txnContext) {
		Map<String,InterfaceEmployee> employeeIdEmployeeDetailsMap = null;
		try {
			com.xpo.ltl.api.humanresource.client.v2.HumanResourceClient client = getHumanResourceClientV2(txnContext);
			ListEmployeesRqst request = new ListEmployeesRqst();
			request.setEmployeeIds(employeeIds);
			Response<ListEmployeesResp> resp = client.listEmployees(request);
			if (null != resp) {
				ListEmployeesResp result = resp.getData();
				employeeIdEmployeeDetailsMap =  CollectionUtils.emptyIfNull(result.getInterfaceEmployee()).stream()
                        .collect( Collectors.toMap(InterfaceEmployee::getEmployeeId, 
                                Function.identity()) );
			}
			return employeeIdEmployeeDetailsMap;
		} catch (final Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error("getEmployeeDetailsMap: " + e);
			return null;
		}
	}
	
	public boolean isValidOperationalSic(String sicCd, TransactionContext txnContext) throws ServiceException {
		boolean sicIsOperational = false;
		try {
			Map<String, ServiceCenter> serviceCenters = getOperationalServiceCenters(txnContext);
			ServiceCenter serviceCenter = serviceCenters.get(sicCd);
			if (serviceCenter != null) {
				sicIsOperational = true;
			}
		} catch (Throwable t) {
			throw t;
		}
		return sicIsOperational;
	}
	
	protected Map<String,ServiceCenter> getOperationalServiceCenters(TransactionContext txnContext)
			throws ServiceException {
		try {
            VoidCacheKey key = VoidCacheKey.of(txnContext);
            return operationalServiceCentersCache.get(key);
		}
		catch (Throwable e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, e, txnContext)
				.contextValues("LocationClient.getOperationalServiceCenters").build();
		}
		finally {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("getOperationalServiceCenters: " + operationalServiceCentersCache.stats());
		}
	}
	
    public List<CustomerContact> listCustomerContacts(String custInstId, TransactionContext txnContext) {

        List<CustomerContact> customerContacts = new ArrayList<>();
        try {
            com.xpo.ltl.api.customer.client.v2.CustomerClient client = getCustomerV2Client(txnContext);
            Response<ListCustomerContactsResp> resp = client.listCustomerContacts(custInstId, CustomerIdTypeCd.LEGACY_CIS_CUSTOMER_NUMBER);
            if (null != resp) {
                ListCustomerContactsResp result = resp.getData();
                customerContacts.addAll(result.getCustomerContacts());
            }
            return customerContacts;
        } catch (Throwable e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("listCustomerContacts: " + e);
            }
            return null;
        }
    }

    public List<CustomerOperationsNote> listCustomerOperationsNotes(Long custInstId, TransactionContext txnContext) {

        List<CustomerOperationsNote> customerOperationsNote = new ArrayList<>();

        try {
            com.xpo.ltl.api.cityoperations.client.v1.CityOperationsClient client = getCityOperationsClient(txnContext);
            CustomerProfileDetailCd[] custProfileDetailCds = new CustomerProfileDetailCd[1];
            custProfileDetailCds[0] = CustomerProfileDetailCd.NOTE;
            Response<GetOperationsCustomerProfileResp> resp = client.getOperationsCustomerProfile(custInstId, custProfileDetailCds);
            if (null != resp) {
                GetOperationsCustomerProfileResp result = resp.getData();
                customerOperationsNote.addAll(result.getCustomerOperationsNotes());
            }
            return customerOperationsNote;
        } catch (Throwable e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("listCustomerOperationsNotes: " + e);
            }
            return null;
        }        
    }
    
    /**
     * Returns the SCO notification and any notification logs for a notification instance ID.
     */
    public GetDeliveryNotificationResp getDeliveryNotification(Long notificationInstId, TransactionContext txnContext) {
        
        try {
            com.xpo.ltl.api.cityoperations.client.v1.CityOperationsClient client = getCityOperationsClient(txnContext);
            GetDeliveryNotificationResp result = null;
            Response<GetDeliveryNotificationResp> resp = client.getDeliveryNotification(notificationInstId);
            if (null != resp) {
                result = resp.getData();
            }
            return result;
        } catch (Throwable e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("getDeliveryNotification: " + e);
            }
            return null;
        }        
    }
}

