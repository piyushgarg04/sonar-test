package com.xpo.ltl.shipment.service.enums;

public enum ShipmentPropertyEnum {

	PROPERTY_FILE("externalShipment"),
	REST_HOST("restHost"),
    REST_BASE_PATH_FOR_SHIPMENT_V1("restBasePathForShipmentV1"),
    REST_BASE_PATH_FOR_SHIPMENT_V2("restBasePathForShipmentV2"),
	REST_BASE_PATH_FOR_LOCATION("restBasePathForLocation"),
	REST_BASE_PATH_FOR_APPOINTMENT("restBasePathForAppointment"),
	REST_BASE_PATH_FOR_DMS("restBasePathForDms"),
	REST_BASE_PATH_FOR_CARRIER_MANAGEMENT("restBasePathForCarrierManagement"),
	REST_BASE_PATH_FOR_CUSTOMER("restBasePathForCustomer"),
    REST_BASE_PATH_FOR_CUSTOMERV2("restBasePathForCustomerV2"),
	REST_BASE_PATH_FOR_DOCK_OPERATIONS("restBasePathForDockOperations"),
	REST_BASE_PATH_FOR_PRO_NUMBER_REENGINEERING("restBasePathForProNumberReengineering"),
	REST_BASE_PATH_FOR_FREIGHT_FLOW("restBasePathForFreightFlow"),
    REST_BASE_PATH_FOR_FREIGHT_FLOW_V2("restBasePathForFreightFlowV2"),
	REST_BASE_PATH_FOR_HUMAN_RESOURCE("restBasePathForHumanResource"),
	REST_BASE_PATH_FOR_HUMAN_RESOURCE_V2("restBasePathForHumanResourceV2"),
	REST_BASE_PATH_FOR_CITY_OPERATIONS("restBasePathForCityOperations"),
	REST_BASE_PATH_FOR_INFRASTRUCTURE("restBasePathForInfrastructure"),
	SALVAGE_TO_MAIL("salvageToEmail"),
	SALVAGE_APPROVER_MAIL("salvageApproverEmail"),
	REST_CLIENT_USERNAME("restClientUserName"),
	REST_CLIENT_PASSWORD("restClientPassword"),
	REST_CLIENT_AUTH_TOKEN("restClientAuthToken"),
	COMBRIDGECONFIGURATION("comBridgeConfiguration"),
	PRINT_FBDS_COM_BRIDGE_CONFIGURATION("printFbdsComBridgeConfiguration"),
	DMS_CORPS_CODE("dmsCorpCode"),
	DOC_CLASS("docClass"),
	CONFLEVEL_PCT_PRED_TRUEDEBTOR("confLevelPctPredTrueDebtor"),
	MAX_COUNT_FOR_IN_CLAUSE("maxCountForInClause"),
    TOTAL_SHIPMENT_COUNT_PRED_TRUEDEBTOR("totalShipmentCountPredTrueDebtor"),
    REST_CLIENT_CARRIERMANAGEMENT_CACHE_SPEC
    	("restClient.carrierManagement.cacheSpec",
    	ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_LOCREFERENCEDETAILSBYSIC_CACHE_SPEC
        ("restClient.locReferenceDetailsBySic.cacheSpec",
         ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_LOCREFERENCEDETAILS_CACHE_SPEC
        ("restClient.locReferenceDetails.cacheSpec",
        ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_ZONEANDSATELLITEBYSIC_CACHE_SPEC
        ("restClient.zoneAndSatelliteBySic.cacheSpec",
         ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_PROBOLPREFIXMASTER_CACHE_SPEC
        ("restClient.proBolPrefixMaster.cacheSpec",
         ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_HOSTSICDETAILS_CACHE_SPEC
        ("restClient.hostSicDetails.cacheSpec",
         ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
     REST_CLIENT_DETERMINE_OPERATIONAL_SERVICE_DATE_CACHE_SPEC
        ("restClient.determineOperationalServiceDate.cacheSpec",
         ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_CALCULATE_TRANSIT_TIME_CACHE_SPEC
    ("restClient.calculateTransitTime.cacheSpec",
     ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_LOC_FEATURE_SETTING_CACHE_SPEC("restClient.locFeatureSetting.cacheSpec", ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_SIC_FOR_POSTAL_CODES_CACHE_SPEC
    ("restClient.sicForPostalCodes.cacheSpec",
     ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_OPERATIONALSERVICECENTERS_CACHE_SPEC
	("restClient.operationalServiceCenters.cacheSpec",
	 ShipmentPropertyEnum.DEFAULT_CACHE_SPEC),
    REST_CLIENT_ON_PREM_USERNAME("restClientOnPremUserName"),
	REST_CLIENT_ON_PREM_PASSWORD("restClientOnPremPassword"),
	REST_HOST_ON_PREM("restHostOnPrem"),
	REST_CLIENT_ON_PREM_AUTH_TOKEN("restClientOnPremAuthToken"),
	PRINT_FBDS_AUTHORIZED_USERS("printFbdsAuthorizedUsers"),
	FONTS_RESOURCE_PATH("fontsResourcePath"),
	IMAGES_RESOURCE_PATH("imagesResourcePath"),
	SHIPMENT_SKELETON_LEGACY_UPDATE_TASK_PROCESSOR_MAX_CONCURRENT
    ("shipmentSkeletonLegacy.taskProcessor.maxConcurrent", "30"),
    SHIPMENT_SKELETON_LEGACY_UPDATE_TASK_PROCESSOR_MAX_ATTEMPTS
    ("shipmentSkeletonLegacy.taskProcessor.maxAttempts", "1"),
    SHIPMENT_SKELETON_LEGACY_UPDATE_TASK_PROCESSOR_RETRY_DELAY_MILLIS
    ("shipmentSkeletonLegacy.taskProcessor.retryDelayMillis", "5000"),
    SHIPMENT_SKELETON_LEGACY_UPDATE_TASK_PROCESSOR_TIMEOUT_MILLIS
    ("shipmentSkeletonLegacy.taskProcessor.timeoutMillis", "60000"),
    BULK_CREATE_SHIPMENT_SKELETON_TASK_PROCESSOR_MAX_CONCURRENT
    ("bulkCreateShipmentSkeleton.taskProcessor.maxConcurrent", "30"),
    BULK_CREATE_SHIPMENT_SKELETON_TASK_PROCESSOR_MAX_ATTEMPTS
    ("bulkCreateShipmentSkeleton.taskProcessor.maxAttempts", "1"),
    BULK_CREATE_SHIPMENT_SKELETON_TASK_PROCESSOR_RETRY_DELAY_MILLIS
    ("bulkCreateShipmentSkeleton.taskProcessor.retryDelayMillis", "5000"),
    BULK_CREATE_SHIPMENT_SKELETON_TASK_PROCESSOR_TIMEOUT_MILLIS
    ("bulkCreateShipmentSkeleton.taskProcessor.timeoutMillis", "60000"),
    LOCATION_REFERENCE_DETAILS_TASK_PROCESSOR_MAX_CONCURRENT
    ("locationReferenceDetails.taskProcessor.maxConcurrent", "30"),
    LOCATION_REFERENCE_DETAILS_TASK_PROCESSOR_MAX_ATTEMPTS
    ("locationReferenceDetails.taskProcessor.maxAttempts", "1"),
    LOCATION_REFERENCE_DETAILS_TASK_PROCESSOR_RETRY_DELAY_MILLIS
    ("locationReferenceDetails.taskProcessor.retryDelayMillis", "5000"),
    LOCATION_REFERENCE_DETAILS_TASK_PROCESSOR_TIMEOUT_MILLIS
    ("locationReferenceDetails.taskProcessor.timeoutMillis", "60000"),
    IS_PROD("isProd"),
	APPLY_DB2_TWO_PHASE_COMMIT("2PhaseCommitFlag", "Y"),
    @Deprecated
    THREAD_POOL_ENABLED("threadPool.enabled", "false"),

    THREAD_POOL_CORE_THREADS("threadPool.coreThreads", "10"),
    THREAD_POOL_MAX_THREADS("threadPool.maxThreads", "25"),
    THREAD_POOL_KEEP_ALIVE_TIME_IN_MINUTES("threadPool.keepAliveTimeInMinutes", "60"),
    THREAD_POOL_QUEUE_SIZE("threadPool.queueSize", "100"),
    COM_CONFIG_DIST_GEN("comCfgDistGen"),
	DB2_COMMIT_ENABLED_FOR_UPDATE_SHIPMENT("db2CommitEnabledForUpdateShipmentFlag", "Y");

    private static final String DEFAULT_CACHE_SPEC =
        "initialCapacity=300,maximumSize=3000,expireAfterWrite=3d,refreshAfterWrite=1h";

    private final String name;
    private final String defaultValue;

    ShipmentPropertyEnum(String name) {
        this(name, null);
    }

    ShipmentPropertyEnum(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

}
