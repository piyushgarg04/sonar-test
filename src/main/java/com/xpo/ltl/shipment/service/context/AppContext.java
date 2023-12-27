package com.xpo.ltl.shipment.service.context;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.cache.CacheBuilderSpec;
import com.xpo.ltl.shipment.service.enums.ShipmentPropertyEnum;
import com.xpo.ltl.util.thread.ExecutorServiceFactory;
import com.xpo.ltl.util.thread.ThreadPoolDefinition;

@ApplicationScoped
public class AppContext {

    private static final Log LOGGER = LogFactory.getLog(AppContext.class);

    @Resource(lookup="concurrent/threadFactory")
    private ManagedThreadFactory threadFactory;

    @Resource(lookup="concurrent/executorServiceShipment")
    @Deprecated
    private ExecutorService legacyExecutor;
    
    private String restHost;
    private String restBasePathForShipmentV1;
    private String restBasePathForShipmentV2;
    private String restBasePathForLocation;
    private String restBasePathForAppointment;
    private String restBasePathForDms;
    private String restBasePathForCarrierManagement;
    private String restBasePathForCustomer;
    private String restBasePathForCustomerV2;
    private String restBasePathForDockOperations;
    private String restBasePathForProNumberReengineering;
    private String restBasePathForFreightFlow;
    private String restBasePathForFreightFlowV2;
    private String restBasePathForHumanResource;
    private String restBasePathForHumanResourceV2;
    private String restBasePathForCityOperations;
    private String restClientUserName;
    private String restClientPassword;
    private String restClientAuthToken;
    private String restClientUserNameOnPrem;
	private String restClientPasswordOnPrem;
	private String restClientAuthTokenOnPrem;
	private String restHostOnPrem;
    private String comBridgeConfiguration;
    private String printFbdsComBridgeConfiguration;
    private String printFbdsAuthorizedUsers;
    private String dmsCorpCode;
    private String docClass;
    private String confLevelPctPredTrueDebtor;
    private String fontsResourcePath;
    private String imagesResourcePath;
    private int maxCountForInClause;
    private int totalShipmentCountPredTrueDebtor;
    private CacheBuilderSpec restClientLocReferenceDetailsCacheSpec;
    private CacheBuilderSpec restClientLocReferenceDetailsBySicCacheSpec;
    private CacheBuilderSpec restClientZoneAndSatelliteBySicCacheSpec;
    private CacheBuilderSpec restClientListProBolPrefixMasterCacheSpec;
    private CacheBuilderSpec restClientCarrierManagementCacheSpec;
    private CacheBuilderSpec restClientHostSicDetailsCacheSpec;
    private CacheBuilderSpec restClientDetermineOperationalServiceDateCacheSpec;
    private CacheBuilderSpec restClientCalculateTransitTimeCacheSpec;
    private CacheBuilderSpec restClientLocFeaturesBySettingCacheSpec;
    private CacheBuilderSpec restSicForPostalCodesCacheSpec;
    private CacheBuilderSpec restClientOperationalServiceCentersCacheSpec;
    private int shipmentSkeletonLegacyUpdateMaxConcurrent;
    private int shipmentSkeletonLegacyUpdateMaxAttempts;
    private long shipmentSkeletonLegacyUpdateRetryDelayMillis;
    private long shipmentSkeletonLegacyUpdateTimeoutMillis;
    private int locationReferenceDetailsMaxConcurrent;
    private int locationReferenceDetailsMaxAttempts;
    private long locationReferenceDetailsRetryDelayMillis;
    private long locationReferenceDetailsTimeoutMillis;
    private int bulkCreateShipmentSkeletonMaxConcurrent;
    private int bulkCreateShipmentSkeletonMaxAttempts;
    private long bulkCreateShipmentSkeletonRetryDelayMillis;
    private long bulkCreateShipmentSkeletonTimeoutMillis;
    private String restBasePathForInfrastructure;
    private String salvageToEmail;
    private String salvageApproverEmail;
	private Boolean isProd;

	private Boolean applyDb2TwoPhaseCommit;
    private String comCfgDistGen;
	private Boolean db2CommitEnabledForUpdateShipment;
    private ExecutorService executor;
    
    @PostConstruct
    protected void init() {
        try {
            ResourceBundle bundle =
                ResourceBundle.getBundle(ShipmentPropertyEnum.PROPERTY_FILE.getName());
            restHost =
                getProperty(bundle, ShipmentPropertyEnum.REST_HOST);
            restBasePathForShipmentV1 =
                getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_SHIPMENT_V1);
            restBasePathForShipmentV2 =
                getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_SHIPMENT_V2);
            restBasePathForLocation =
                getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_LOCATION);
            restBasePathForAppointment =
                getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_APPOINTMENT);
            restBasePathForDms =
                getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_DMS);
            restBasePathForCarrierManagement = 
            		getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_CARRIER_MANAGEMENT);
            restBasePathForCustomer =
            	getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_CUSTOMER);
            restBasePathForCustomerV2 =
                    getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_CUSTOMERV2);
            restBasePathForDockOperations =
                    getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_DOCK_OPERATIONS);
            restBasePathForProNumberReengineering = 
            		getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_PRO_NUMBER_REENGINEERING);
            restBasePathForHumanResource = getProperty(bundle,
					ShipmentPropertyEnum.REST_BASE_PATH_FOR_HUMAN_RESOURCE);
            restBasePathForHumanResourceV2 = getProperty(bundle,
					ShipmentPropertyEnum.REST_BASE_PATH_FOR_HUMAN_RESOURCE_V2);
            restBasePathForFreightFlow = 
                    getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_FREIGHT_FLOW);
            restBasePathForFreightFlowV2 = getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_FREIGHT_FLOW_V2);
            restBasePathForCityOperations =
                    getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_CITY_OPERATIONS);
            restBasePathForInfrastructure = 
                    getProperty(bundle, ShipmentPropertyEnum.REST_BASE_PATH_FOR_INFRASTRUCTURE);
            salvageToEmail = 
                    getProperty(bundle, ShipmentPropertyEnum.SALVAGE_TO_MAIL);
            salvageApproverEmail = 
                    getProperty(bundle, ShipmentPropertyEnum.SALVAGE_APPROVER_MAIL);
            restClientUserName =
                getProperty(bundle, ShipmentPropertyEnum.REST_CLIENT_USERNAME);
            restClientPassword =
                getProperty(bundle, ShipmentPropertyEnum.REST_CLIENT_PASSWORD);
            restClientAuthToken =
                getProperty(bundle, ShipmentPropertyEnum.REST_CLIENT_AUTH_TOKEN);
            restHostOnPrem =
            	getProperty(bundle, ShipmentPropertyEnum.REST_HOST_ON_PREM);
            restClientUserNameOnPrem =
            	getProperty(bundle, ShipmentPropertyEnum.REST_CLIENT_ON_PREM_USERNAME);
			restClientPasswordOnPrem =
				getProperty(bundle, ShipmentPropertyEnum.REST_CLIENT_ON_PREM_PASSWORD);
			restClientAuthTokenOnPrem =
				getProperty(bundle, ShipmentPropertyEnum.REST_CLIENT_ON_PREM_AUTH_TOKEN);
            comBridgeConfiguration =
                getProperty(bundle, ShipmentPropertyEnum.COMBRIDGECONFIGURATION);
            dmsCorpCode =
                getProperty(bundle, ShipmentPropertyEnum.DMS_CORPS_CODE);
            docClass =
                getProperty(bundle, ShipmentPropertyEnum.DOC_CLASS);
            confLevelPctPredTrueDebtor =
                getProperty(bundle, ShipmentPropertyEnum.CONFLEVEL_PCT_PRED_TRUEDEBTOR);
            maxCountForInClause =
                Integer.parseInt(getProperty(bundle, ShipmentPropertyEnum.MAX_COUNT_FOR_IN_CLAUSE));
            totalShipmentCountPredTrueDebtor =
                Integer.parseInt(getProperty(bundle, ShipmentPropertyEnum.TOTAL_SHIPMENT_COUNT_PRED_TRUEDEBTOR));
            restClientCarrierManagementCacheSpec =
            		getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_CARRIERMANAGEMENT_CACHE_SPEC);
            restClientLocReferenceDetailsBySicCacheSpec =
                getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_LOCREFERENCEDETAILSBYSIC_CACHE_SPEC);
            restClientLocReferenceDetailsCacheSpec = 
                getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_LOCREFERENCEDETAILS_CACHE_SPEC);
            restClientZoneAndSatelliteBySicCacheSpec =
                getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_ZONEANDSATELLITEBYSIC_CACHE_SPEC);
            restClientListProBolPrefixMasterCacheSpec =
                    getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_PROBOLPREFIXMASTER_CACHE_SPEC);
            restClientHostSicDetailsCacheSpec = getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_HOSTSICDETAILS_CACHE_SPEC);
            restClientDetermineOperationalServiceDateCacheSpec = getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_DETERMINE_OPERATIONAL_SERVICE_DATE_CACHE_SPEC);
            restClientCalculateTransitTimeCacheSpec = getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_CALCULATE_TRANSIT_TIME_CACHE_SPEC);
            restClientLocFeaturesBySettingCacheSpec = getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_LOC_FEATURE_SETTING_CACHE_SPEC);
            restSicForPostalCodesCacheSpec = getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_SIC_FOR_POSTAL_CODES_CACHE_SPEC);
			restClientOperationalServiceCentersCacheSpec = getPropertyAsCacheSpec(bundle, ShipmentPropertyEnum.REST_CLIENT_OPERATIONALSERVICECENTERS_CACHE_SPEC);
            printFbdsComBridgeConfiguration =
            	getProperty(bundle, ShipmentPropertyEnum.PRINT_FBDS_COM_BRIDGE_CONFIGURATION);
            printFbdsAuthorizedUsers = 
                	getProperty(bundle, ShipmentPropertyEnum.PRINT_FBDS_AUTHORIZED_USERS);
            fontsResourcePath = 
            		getProperty(bundle, ShipmentPropertyEnum.FONTS_RESOURCE_PATH);
            imagesResourcePath = 
                	getProperty(bundle, ShipmentPropertyEnum.IMAGES_RESOURCE_PATH);
            shipmentSkeletonLegacyUpdateMaxConcurrent = Integer.parseInt(getProperty(bundle, ShipmentPropertyEnum.SHIPMENT_SKELETON_LEGACY_UPDATE_TASK_PROCESSOR_MAX_CONCURRENT));
            shipmentSkeletonLegacyUpdateMaxAttempts = Integer.parseInt(getProperty(bundle, ShipmentPropertyEnum.SHIPMENT_SKELETON_LEGACY_UPDATE_TASK_PROCESSOR_MAX_ATTEMPTS));
            shipmentSkeletonLegacyUpdateRetryDelayMillis = Long.parseLong(getProperty(bundle, ShipmentPropertyEnum.SHIPMENT_SKELETON_LEGACY_UPDATE_TASK_PROCESSOR_RETRY_DELAY_MILLIS));
            shipmentSkeletonLegacyUpdateTimeoutMillis = Long.parseLong(getProperty(bundle, ShipmentPropertyEnum.SHIPMENT_SKELETON_LEGACY_UPDATE_TASK_PROCESSOR_TIMEOUT_MILLIS));
            bulkCreateShipmentSkeletonMaxConcurrent = Integer.parseInt(getProperty(bundle, ShipmentPropertyEnum.BULK_CREATE_SHIPMENT_SKELETON_TASK_PROCESSOR_MAX_CONCURRENT));
            bulkCreateShipmentSkeletonMaxAttempts = Integer.parseInt(getProperty(bundle, ShipmentPropertyEnum.BULK_CREATE_SHIPMENT_SKELETON_TASK_PROCESSOR_MAX_ATTEMPTS));
            bulkCreateShipmentSkeletonRetryDelayMillis = Long.parseLong(getProperty(bundle, ShipmentPropertyEnum.BULK_CREATE_SHIPMENT_SKELETON_TASK_PROCESSOR_RETRY_DELAY_MILLIS));
            bulkCreateShipmentSkeletonTimeoutMillis = Long.parseLong(getProperty(bundle, ShipmentPropertyEnum.BULK_CREATE_SHIPMENT_SKELETON_TASK_PROCESSOR_TIMEOUT_MILLIS));
            locationReferenceDetailsMaxConcurrent = Integer.parseInt(getProperty(bundle, ShipmentPropertyEnum.LOCATION_REFERENCE_DETAILS_TASK_PROCESSOR_MAX_CONCURRENT));
            locationReferenceDetailsMaxAttempts = Integer.parseInt(getProperty(bundle, ShipmentPropertyEnum.LOCATION_REFERENCE_DETAILS_TASK_PROCESSOR_MAX_ATTEMPTS));
            locationReferenceDetailsRetryDelayMillis = Long.parseLong(getProperty(bundle, ShipmentPropertyEnum.LOCATION_REFERENCE_DETAILS_TASK_PROCESSOR_RETRY_DELAY_MILLIS));
            locationReferenceDetailsTimeoutMillis = Long.parseLong(getProperty(bundle, ShipmentPropertyEnum.LOCATION_REFERENCE_DETAILS_TASK_PROCESSOR_TIMEOUT_MILLIS));
            isProd = getPropertyAsBoolean(bundle, ShipmentPropertyEnum.IS_PROD);
			applyDb2TwoPhaseCommit = getPropertyAsBoolean(bundle, ShipmentPropertyEnum.APPLY_DB2_TWO_PHASE_COMMIT);
            comCfgDistGen = getProperty(bundle, ShipmentPropertyEnum.COM_CONFIG_DIST_GEN);
			db2CommitEnabledForUpdateShipment = getPropertyAsBoolean(bundle, ShipmentPropertyEnum.DB2_COMMIT_ENABLED_FOR_UPDATE_SHIPMENT);
            boolean threadPoolEnabled =
                    getPropertyAsBoolean
                    (bundle,
                        ShipmentPropertyEnum.THREAD_POOL_ENABLED);
            if (threadPoolEnabled) {
                ThreadPoolDefinition threadPoolDefinition =
                        new ThreadPoolDefinition();
                threadPoolDefinition.setCoreThreads
                (getPropertyAsInt
                    (bundle,
                        ShipmentPropertyEnum.THREAD_POOL_CORE_THREADS));
                threadPoolDefinition.setMaxThreads
                (getPropertyAsInt
                    (bundle,
                        ShipmentPropertyEnum.THREAD_POOL_MAX_THREADS));
                threadPoolDefinition.setKeepAliveTimeInMinutes
                (getPropertyAsInt
                    (bundle,
                        ShipmentPropertyEnum.THREAD_POOL_KEEP_ALIVE_TIME_IN_MINUTES));
                threadPoolDefinition.setQueueSize
                (getPropertyAsInt
                    (bundle,
                        ShipmentPropertyEnum.THREAD_POOL_QUEUE_SIZE));
                executor =
                        ExecutorServiceFactory.newExecutorService
                        ("default", threadPoolDefinition, threadFactory);
            }
            else {
                executor = legacyExecutor;
            }
        }
        catch (RuntimeException e) {
            LOGGER.error("init: Failed to initialize AppContext: " + e.getMessage(), e);
            throw e;
        }
    }

	protected void start(@SuppressWarnings("unused")
                         @Observes
                         @Initialized(ApplicationScoped.class)
                         ServletContext servletContext) {
        // Observer method that instantiates AppContext bean immediately after
        // deployment
    }

    @PreDestroy
    protected void stop() {
        executor.shutdownNow();
    }
    
    private String getProperty(ResourceBundle bundle, ShipmentPropertyEnum property) {
        String value = null;
        try {
            value = bundle.getString(property.getName());
        }
        catch (MissingResourceException e) {
            if (property.getDefaultValue() == null)
                throw e;
        }

        if (value == null)
            value = property.getDefaultValue();

        return value;
    }
    
    private int getPropertyAsInt(ResourceBundle bundle,
        ShipmentPropertyEnum property) {
        String value = null;
        try {
            value = bundle.getString(property.getName());
        }
        catch (MissingResourceException e) {
            if (property.getDefaultValue() == null)
                throw e;
        }

        if (StringUtils.isNotBlank(value)) {
            try {
                return Integer.parseInt(value);
            }
            catch (Throwable e) {
                LOGGER.error("getPropertyAsInt: Failed to parse " + value + " for property " + property.getName() + ": " + e.getMessage(), e);
            }
        }

        LOGGER.warn("getPropertyAsInt: Using default value " + property.getDefaultValue() + " for property " + property.getName());
        return Integer.parseInt(property.getDefaultValue());
    }

    private boolean getPropertyAsBoolean(ResourceBundle bundle,
        ShipmentPropertyEnum property) {
        String value = null;
        try {
            value = bundle.getString(property.getName());
        }
        catch (MissingResourceException e) {
            if (property.getDefaultValue() == null)
                throw e;
        }

        if (StringUtils.isNotBlank(value)) {
            try {
                return BooleanUtils.toBooleanObject(value);
            }
            catch (Throwable e) {
                LOGGER.error("getPropertyAsInt: Failed to parse " + value + " for property " + property.getName() + ": " + e.getMessage(), e);
            }
        }

        LOGGER.warn("getPropertyAsInt: Using default value " + property.getDefaultValue() + " for property " + property.getName());
        return BooleanUtils.toBoolean(property.getDefaultValue());
    }

    private CacheBuilderSpec getPropertyAsCacheSpec(ResourceBundle bundle, ShipmentPropertyEnum property) {
        String value = null;
        try {
            value = bundle.getString(property.getName());
        }
        catch (MissingResourceException e) {
            if (property.getDefaultValue() == null)
                throw e;
        }

        if (StringUtils.isNotBlank(value)) {
            try {
                return CacheBuilderSpec.parse(value);
            }
            catch (Throwable e) {
                LOGGER.error("getPropertyAsCacheSpec: Failed to parse " + value + " for property " + property.getName() + ": " + e.getMessage(), e);
            }
        }

        LOGGER.warn("getPropertyAsCacheSpec: Using default value " + property.getDefaultValue() + " for property " + property.getName());
        return CacheBuilderSpec.parse(property.getDefaultValue());
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }

    public String getRestHost() {
        return restHost;
    }

    public String getRestBasePathForShipmentV1() {
        return restBasePathForShipmentV1;
    }

    public String getRestBasePathForShipmentV2() {
        return restBasePathForShipmentV2;
    }

    public String getRestBasePathForLocation() {
        return restBasePathForLocation;
    }

    public String getRestBasePathForAppointment() {
        return restBasePathForAppointment;
    }

    public String getRestBasePathForDms() {
        return restBasePathForDms;
    }

    public String getRestClientUserName() {
        return restClientUserName;
    }

    public String getRestClientPassword() {
        return restClientPassword;
    }

    public String getRestClientAuthToken() {
        return restClientAuthToken;
    }

    public String getRestClientUserNameOnPrem() {
		return restClientUserNameOnPrem;
	}

	public String getRestClientPasswordOnPrem() {
		return restClientPasswordOnPrem;
	}

	public String getRestClientAuthTokenOnPrem() {
		return restClientAuthTokenOnPrem;
	}

	public String getRestHostOnPrem() {
		return restHostOnPrem;
	}

	public String getRestBasePathForCustomer() {
		return restBasePathForCustomer;
	}
	
	public String getRestBasePathForCarrierManagement() {
		return restBasePathForCarrierManagement;
	}

    public String getRestBasePathForDockOperations() {
        return restBasePathForDockOperations;
    }

    public String getRestBasePathForProNumberReengineering() {
        return restBasePathForProNumberReengineering;
    }

    public String getRestBasePathForFreightFlow() {
        return restBasePathForFreightFlow;
    }

    public String getRestBasePathForFreightFlowV2() {
        return restBasePathForFreightFlowV2;
    }

    public String getRestBasePathForHumanResource() { return restBasePathForHumanResource; }
    
    public String getRestBasePathForHumanResourceV2() { 
    	return restBasePathForHumanResourceV2; 
    }

    public String getComBridgeConfiguration() {
        return comBridgeConfiguration;
    }

    public String getDmsCorpCode() {
        return dmsCorpCode;
    }

    public String getDocClass() {
        return docClass;
    }

    public String getConfLevelPctPredTrueDebtor() {
        return confLevelPctPredTrueDebtor;
    }

    public int getMaxCountForInClause() {
        return maxCountForInClause;
    }

    public int getTotalShipmentCountPredTrueDebtor() {
        return totalShipmentCountPredTrueDebtor;
    }

    public CacheBuilderSpec getRestClientCarrierManagementCacheSpec() {
		return restClientCarrierManagementCacheSpec;
	}
    
    public CacheBuilderSpec getRestClientLocReferenceDetailsCacheSpec() {
		return restClientLocReferenceDetailsCacheSpec;
	}

    public CacheBuilderSpec getRestClientLocReferenceDetailsBySicCacheSpec() {
        return restClientLocReferenceDetailsBySicCacheSpec;
    }

    public CacheBuilderSpec getRestClientZoneAndSatelliteBySicCacheSpec() {
        return restClientZoneAndSatelliteBySicCacheSpec;
    }

    public CacheBuilderSpec getRestClientListProBolPrefixMasterCacheSpec() {
        return restClientListProBolPrefixMasterCacheSpec;
    }

    public CacheBuilderSpec getRestClientHostSicDetailsCacheSpec() {
        return restClientHostSicDetailsCacheSpec;
    }

    public String getPrintFbdsComBridgeConfiguration() {
		return printFbdsComBridgeConfiguration;
	}
    
    public CacheBuilderSpec getRestClientDetermineOperationalServiceDateCacheSpec() {
        return restClientDetermineOperationalServiceDateCacheSpec;
    }
    
    public CacheBuilderSpec getRestClientCalculateTransitTimeSpec() {
        return restClientCalculateTransitTimeCacheSpec;
    }

	public void setPrintFbdsComBridgeConfiguration(String printFbdsComBridgeConfiguration) {
		this.printFbdsComBridgeConfiguration = printFbdsComBridgeConfiguration;
	}

	public String getPrintFbdsAuthorizedUsers() {
		return printFbdsAuthorizedUsers;
	}

	public void setPrintFbdsAuthorizedUsers(String printFbdsAuthorizedUsers) {
		this.printFbdsAuthorizedUsers = printFbdsAuthorizedUsers;
	}

	public String getImagesResourcePath() {
		return imagesResourcePath;
	}

	public void setImagesResourcePath(String imagesResourcePath) {
		this.imagesResourcePath = imagesResourcePath;
	}

	public String getFontsResourcePath() {
		return fontsResourcePath;
	}

	public void setFontsResourcePath(String fontsResourcePath) {
		this.fontsResourcePath = fontsResourcePath;
	}

	public String getRestBasePathForCityOperations() {
		return restBasePathForCityOperations;
	}

	public void setRestBasePathForCItyOperations(String restBasePathForCItyOperations) {
		this.restBasePathForCityOperations = restBasePathForCItyOperations;
	}

	public int getShipmentSkeletonLegacyUpdateMaxConcurrent() {
		return shipmentSkeletonLegacyUpdateMaxConcurrent;
	}

	public int getShipmentSkeletonLegacyUpdateMaxAttempts() {
		return shipmentSkeletonLegacyUpdateMaxAttempts;
	}

	public long getShipmentSkeletonLegacyUpdateRetryDelayMillis() {
		return shipmentSkeletonLegacyUpdateRetryDelayMillis;
	}

	public long getShipmentSkeletonLegacyUpdateTimeoutMillis() {
		return shipmentSkeletonLegacyUpdateTimeoutMillis;
	}

    public int getLocationReferenceDetailsMaxConcurrent() {
		return locationReferenceDetailsMaxConcurrent;
	}

	public int getLocationReferenceDetailsMaxAttempts() {
		return locationReferenceDetailsMaxAttempts;
	}

	public long getLocationReferenceDetailsRetryDelayMillis() {
		return locationReferenceDetailsRetryDelayMillis;
	}

	public long getLocationReferenceDetailsTimeoutMillis() {
		return locationReferenceDetailsTimeoutMillis;
	}
	
	public int getBulkCreateShipmentSkeletonMaxConcurrent() {
		return bulkCreateShipmentSkeletonMaxConcurrent;
	}

	public int getBulkCreateShipmentSkeletonMaxAttempts() {
		return bulkCreateShipmentSkeletonMaxAttempts;
	}

	public long getBulkCreateShipmentSkeletonRetryDelayMillis() {
		return bulkCreateShipmentSkeletonRetryDelayMillis;
	}

	public long getBulkCreateShipmentSkeletonTimeoutMillis() {
		return bulkCreateShipmentSkeletonTimeoutMillis;
	}
	
	 public CacheBuilderSpec getRestSicForPostalCodesCacheSpec() {
		return restSicForPostalCodesCacheSpec;
	}

    public CacheBuilderSpec getRestClientLocFeaturesBySettingCacheSpec() {
        return restClientLocFeaturesBySettingCacheSpec;
    }
    
    public CacheBuilderSpec getRestClientOperationalServiceCentersCacheSpec() {
		return restClientOperationalServiceCentersCacheSpec;
	}

	public String getRestBasePathForInfrastructure() {
		 return restBasePathForInfrastructure;
	}

	public String getSalvageToEmail() {
		return salvageToEmail;
	}

    public Boolean isProd() {
        return isProd;
    }

	public Boolean getApplyDb2TwoPhaseCommit() {
		return applyDb2TwoPhaseCommit;
	}

	public String getSalvageApproverEmail() {
		return salvageApproverEmail;
	}

    
    public String getRestBasePathForCustomerV2() {
        return restBasePathForCustomerV2;
    }

    
    public void setRestBasePathForCustomerV2(String restBasePathForCustomerV2) {
        this.restBasePathForCustomerV2 = restBasePathForCustomerV2;
    }

    public String getComCfgDistGen() {
        return comCfgDistGen;
    }

	public Boolean getDb2CommitEnabledForUpdateShipment() {
		return db2CommitEnabledForUpdateShipment;
	}
}
