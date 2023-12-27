package com.xpo.ltl.shipment.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.dockoperations.v1.GetTrailerSpecificationResp;
import com.xpo.ltl.api.dockoperations.v1.TrailerSpecification;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.humanresource.v1.Employee;
import com.xpo.ltl.api.humanresource.v1.EmployeeBasic;
import com.xpo.ltl.api.humanresource.v1.EmployeeRole;
import com.xpo.ltl.api.humanresource.v1.Role;
import com.xpo.ltl.api.location.v2.DetermineOperationalServiceDateResp;
import com.xpo.ltl.api.location.v2.GetOperationalServiceDaysCountResp;
import com.xpo.ltl.api.location.v2.GetSicForPostalCodesResp;
import com.xpo.ltl.api.location.v2.PostalSicAssignment;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMat;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.CreateShipmentSkeletonRqst;
import com.xpo.ltl.api.shipment.v2.FoodPoisonCd;
import com.xpo.ltl.api.shipment.v2.GetProStatusResp;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HazMat;
import com.xpo.ltl.api.shipment.v2.ProStatusCd;
import com.xpo.ltl.api.shipment.v2.ServiceTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentAcquiredTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeleton;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ProFrtBillIndexSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHazMatSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmLnhDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.helper.ShipmentSkeletonHelper;

import junit.framework.TestCase;

public class CreateShipmentSkeletonImplTestCase extends TestCase {

	private static final Long SHP_INST_ID = new Long(1234);
	private static final String INVALID_PARENT_PRO = "0083658910";
	private static final String INVALID_CHECK_DIGIT_PRO = "625534373";
	private static final String PRO_NUMBER = "02080966063";
	private static final String TRACKING_PRO_1 = "3541210282";
	private static final String TRACKING_PRO_2 = "6611396761";
	private static final String ZIP_CODE = "75374";
	private static final String TRAILER_TYPE_CD = "T";
	private static final String TRAILER_SUB_TYPE_CD = "TTC28";
	public static final String SUPRV_ROLE_ID = "165";


	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShmShipmentSubDAO shipmentDAO;

	@Mock
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Mock
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

	@Mock
	private ShmEventLogSubDAO shmEventLogSubDAO;

	@Mock
	private ShmHazMatSubDAO shmHazMatSubDAO;

	@Mock
	private ExternalRestClient externalRestClient;

    @Mock
    private ShmLnhDimensionSubDAO shmLnhDimensionSubDAO;

    @Mock
    private ShipmentRemarkSubDAO shipmentRemarkSubDAO;

    @Mock
    private UpdateShipmentLinehaulDimensionsImpl updateShipmentLinehaulDimensionsImpl;

    @Mock
    private ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;

    @Mock
    private ProFrtBillIndexSubDAO proNbrFrtBillIndexDAO;

    @Mock
    private ShmMovementSubDAO shmMovementSubDAO;
    
    @Mock
    private ShipmentSkeletonHelper shipmentSkeletonHelper;

    @Mock
    private GetProStatusCdImpl getProStatusCdImpl;

	@InjectMocks
	private CreateShipmentSkeletonImpl createShipmentSkeletonImpl;

	@Override
	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");

		final User user = new User();
		user.setUserId("JUNIT");
		user.setEmployeeId("JUNIT");
		when(txnContext.getUser()).thenReturn(user);

		when(txnContext.getTransactionTimestamp()).thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));

		when(txnContext.getCorrelationId()).thenReturn("0");
	}

	@Test
	public void testCreateShipmentSkeleton_RequestRequired() throws Exception
	{
		try {
			createShipmentSkeletonImpl.createShipmentSkeleton(null, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("The request is required."));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_ShipmentSkeletonRequired() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("The shipment skeleton is required."));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_ParentProNumberRequired() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr("");
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.PRO_NBR_RQ.message()));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_InvalidParentProNumberFormat() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(INVALID_PARENT_PRO);
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.PRO_NBR_CHK_DIGIT_ERROR.message()));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_InvalidCheckDigitPro() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(INVALID_CHECK_DIGIT_PRO);
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.PRO_NBR_CHK_DIGIT_ERROR.message()));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_InvalidTotalWeight() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(PRO_NUMBER);
			shipmentSkeleton.setTotalWeightLbs(new Double(-1));
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("Shipment total weight must be greater than zero."));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_InvalidTotalPiecesCount() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(PRO_NUMBER);
			shipmentSkeleton.setTotalWeightLbs(new Double(100));
			shipmentSkeleton.setTotalPiecesCount(new BigInteger("-1"));
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("Shipment total pieces count must be greater than zero."));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_InvalidLoosePiecesCount() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(PRO_NUMBER);
			shipmentSkeleton.setTotalWeightLbs(new Double(100));
			shipmentSkeleton.setTotalPiecesCount(BigInteger.ONE);
			shipmentSkeleton.setLoosePiecesCount(new BigInteger("-1"));
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage()
					.contains(ValidationErrorMessage.LPIECES_COUNT_GREATER_ZERO.message()));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_InvalidTotalPalletsCount() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(PRO_NUMBER);
			shipmentSkeleton.setTotalWeightLbs(new Double(100));
			shipmentSkeleton.setTotalPiecesCount(BigInteger.ONE);
			shipmentSkeleton.setMotorizedPiecesCount(BigInteger.ONE);
			shipmentSkeleton.setTotalPalletsCount(new BigInteger("-1"));
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("Shipment total pallet count must be greater than or equal to zero."));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_HazMatIndRequired() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(PRO_NUMBER);
			shipmentSkeleton.setTotalWeightLbs(new Double(100));
			shipmentSkeleton.setTotalPiecesCount(BigInteger.ONE);
			shipmentSkeleton.setLoosePiecesCount(BigInteger.ONE);
			shipmentSkeleton.setLastMovementDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("Shipment hazmat indicator must be supplied."));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_reqSicRequired() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(PRO_NUMBER);
			shipmentSkeleton.setTotalWeightLbs(new Double(100));
			shipmentSkeleton.setTotalPiecesCount(BigInteger.ONE);
			shipmentSkeleton.setLoosePiecesCount(BigInteger.ONE);
			shipmentSkeleton.setLastMovementDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));
			shipmentSkeleton.setFreezableInd(false);
			shipmentSkeleton.setFoodPoisonCd(FoodPoisonCd.FOOD);
			shipmentSkeleton.setGuaranteedInd(false);
			shipmentSkeleton.setBulkLiquidInd(false);
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.CURRENT_SIC_RQ.message()));
		}
	}

    @Test
    public void testCreateShipmentSkeleton_HandlingUnitReq() throws Exception {
        try {
            CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
            ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
            shipmentSkeleton.setParentProNbr(PRO_NUMBER);
            shipmentSkeleton.setTotalWeightLbs(new Double(100));
            shipmentSkeleton.setTotalPiecesCount(BigInteger.ONE);
            shipmentSkeleton.setLoosePiecesCount(BigInteger.ONE);
            shipmentSkeleton.setLastMovementDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));
            shipmentSkeleton.setFreezableInd(false);
            shipmentSkeleton.setFoodPoisonCd(FoodPoisonCd.FOOD);
            shipmentSkeleton.setGuaranteedInd(false);
            shipmentSkeleton.setBulkLiquidInd(false);
            request.setShipmentSkeleton(shipmentSkeleton);
            createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

            fail("Expected an exception.");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains("At least one handling unit is required or must use dimensions."));
        }
    }

	@Test
	public void testCreateShipmentSkeleton_InvalidHandlingUnitPro() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(PRO_NUMBER);
			shipmentSkeleton.setTotalWeightLbs(new Double(100));
			shipmentSkeleton.setTotalPiecesCount(BigInteger.ONE);
			shipmentSkeleton.setLoosePiecesCount(BigInteger.ONE);
			shipmentSkeleton.setMotorizedPiecesKnownInd(false);
			shipmentSkeleton.setMotorizedPiecesCount(BigInteger.ZERO);
			shipmentSkeleton.setLastMovementDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));
			shipmentSkeleton.setFreezableInd(false);
			shipmentSkeleton.setFoodPoisonCd(FoodPoisonCd.FOOD);
			shipmentSkeleton.setGuaranteedInd(false);
			shipmentSkeleton.setBulkLiquidInd(false);
			HandlingUnit handlingUnit = new HandlingUnit();
			handlingUnit.setChildProNbr(PRO_NUMBER);
			List<HandlingUnit> handlingUnits = new ArrayList<>();
			handlingUnits.add(handlingUnit);
			shipmentSkeleton.setHandlingUnits(handlingUnits);
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.message() +
					"Expected a valid Yellow Pro Number format for childProNbr "));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_InvalidHandlingUnitNum() throws Exception
	{
		try {
			CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
			ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
			shipmentSkeleton.setParentProNbr(PRO_NUMBER);
			shipmentSkeleton.setTotalWeightLbs(new Double(100));
			shipmentSkeleton.setTotalPiecesCount(BigInteger.ONE);
			shipmentSkeleton.setLoosePiecesCount(BigInteger.ONE);
			shipmentSkeleton.setMotorizedPiecesKnownInd(false);
			shipmentSkeleton.setMotorizedPiecesCount(BigInteger.ZERO);
			shipmentSkeleton.setLastMovementDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));
			shipmentSkeleton.setFreezableInd(false);
			shipmentSkeleton.setFoodPoisonCd(FoodPoisonCd.FOOD);
			shipmentSkeleton.setGuaranteedInd(false);
			shipmentSkeleton.setBulkLiquidInd(false);
			HandlingUnit handlingUnit = new HandlingUnit();
			handlingUnit.setChildProNbr(TRACKING_PRO_1);
			HandlingUnit handlingUnit1 = new HandlingUnit();
			handlingUnit1.setChildProNbr(TRACKING_PRO_2);
			List<HandlingUnit> handlingUnits = new ArrayList<>();
			handlingUnits.add(handlingUnit);
			handlingUnits.add(handlingUnit1);
			shipmentSkeleton.setHandlingUnits(handlingUnits);
			request.setShipmentSkeleton(shipmentSkeleton);
			createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("Total number of handling units for the shipment must be equal to the "
					+ "number of loose pieces count plus the number of motorized "
					+ "pieces count."));
		}
	}

	@Test
	public void testCreateShipmentSkeleton_ExistentShipmentOnlyUpdateHandlingUnits() throws Exception
	{

		CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
		ShipmentSkeleton shipmentSkeleton = createSkeleton(null);
		shipmentSkeleton.setUserId("32193");
        shipmentSkeleton.setPupVolumePercentage(10.2);
        shipmentSkeleton.setTotalVolumeCubicFeet(20.4);
		request.setShipmentSkeleton(shipmentSkeleton);

		List<HandlingUnit> handlingUnits = new ArrayList<>();
        HandlingUnit handlingUnit = new HandlingUnit();
        handlingUnit.setSplitInd(false);
        handlingUnit.setPoorlyPackagedInd(false);
        handlingUnit.setParentProNbr(PRO_NUMBER);
        handlingUnit.setChildProNbr("06481101351");
        handlingUnit.setHandlingMovementCd("NORMAL");
        handlingUnit.setReweighInd(false);
        handlingUnits.add(handlingUnit);
        shipmentSkeleton.setHandlingUnits(handlingUnits);

		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setTotPcsCnt(BigDecimal.TEN);
		shmShipment.setProNbrTxt(PRO_NUMBER);
		shmShipment.setShpInstId(SHP_INST_ID);
		shmShipment.setPupVolPct(new BigDecimal(10));
		String [] postalCodes = {shipmentSkeleton.getDestPostalCd()};
		GetSicForPostalCodesResp sicRespForPostalCodes = getSicForPostalCodes();
		Long seqNbr = 1L;
		
		List<String> proNbrList = new ArrayList<>();
		proNbrList.add(PRO_NUMBER);
		List<ShmShipment> shipmentList = new ArrayList<>();
		shipmentList.add(shmShipment);

		GetTrailerSpecificationResp getTrailerSpecificationResp = getTrailerSpecification();

		Employee employee = new Employee();
		EmployeeRole employeeRole = new EmployeeRole();
		Role role = new Role();
		role.setRoleId(SUPRV_ROLE_ID);
		employeeRole.setRole(role);
		employeeRole.setStartDate(BasicTransformer.toXMLGregorianCalendar(BasicTransformer.toDate("01/01/2020")));
		List<EmployeeRole> employeeRoles = new ArrayList<>();
		employeeRoles.add(employeeRole);
		EmployeeBasic basicInfo = new EmployeeBasic();
		basicInfo.setEmployeeId("32193");
		basicInfo.setOriginalHireDate(
				BasicTransformer.toXMLGregorianCalendar(BasicTransformer.toDate("01/01/2020")));
		employee.setBasicInfo(basicInfo);
		employee.setRoles(employeeRoles);

		when(shipmentDAO.listShipmentsByProNumbers(proNbrList, entityManager)).thenReturn(shipmentList);
        when(externalRestClient.isActiveSicAndLinehaul(shipmentSkeleton.getRequestingSicCd(),
				txnContext)).thenReturn(true);
		when(externalRestClient.getSicForPostalCodes(postalCodes,txnContext)).thenReturn(sicRespForPostalCodes);
		when(externalRestClient.getTrailerSpecification(TRAILER_TYPE_CD, TRAILER_SUB_TYPE_CD, null,
				txnContext)).thenReturn(getTrailerSpecificationResp);
		when(shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(anyLong(), any())).thenReturn(seqNbr);
		when(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(anyLong(), anyLong(), any())).thenReturn(seqNbr);
        when(externalRestClient.getEmployeeDetailsByEmployeeId(any(), any()))
				.thenReturn(employee);


		createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

		verify(shipmentDAO, times(1)).save(any(), any());
        verify(shipmentDAO, times(1)).updateDb2ShmShipmentForUpdSkeketon(any(ShmShipment.class), any());
		verify(shmHandlingUnitSubDAO, times(1)).save(any(), any());
		verify(shmHandlingUnitSubDAO, times(1)).createDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any());
	}

	@Test
	public void testCreateShipmentSkeleton_CreateShipmentForDockDrop() throws Exception
	{
		CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
		ShipmentSkeleton shipmentSkeleton = createSkeleton(createHazMatList());
		shipmentSkeleton.setParentProNbr(PRO_NUMBER);
        shipmentSkeleton.setShipmentAcquiredTypeCd(ShipmentAcquiredTypeCd.DOCK_DROP);
        shipmentSkeleton.setPupVolumePercentage(10.2);
        shipmentSkeleton.setTotalVolumeCubicFeet(20.2);
        shipmentSkeleton.setHandlingUnitExemptionInd(false);
        shipmentSkeleton.setHandlingUnitExemptionReason("Y");
        List<HandlingUnit> handlingUnits = new ArrayList<>();
        HandlingUnit handlingUnit = new HandlingUnit();
        handlingUnit.setSplitInd(false);
        handlingUnit.setPoorlyPackagedInd(false);
        handlingUnit.setParentProNbr(PRO_NUMBER);
        handlingUnit.setChildProNbr("06481101351");
        handlingUnit.setHandlingMovementCd("NORMAL");
        handlingUnit.setReweighInd(false);
        handlingUnits.add(handlingUnit);
        shipmentSkeleton.setHandlingUnits(handlingUnits);
        request.setShipmentSkeleton(shipmentSkeleton);

		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setTotPcsCnt(BigDecimal.TEN);
		shmShipment.setProNbrTxt(PRO_NUMBER);
		shmShipment.setShpInstId(SHP_INST_ID);
		shmShipment.setHandlingUnitPartialInd("Y");
		shmShipment.setHandlingUnitSplitInd("Y");
		shmShipment.setPoorlyPackagedInd("Y");
		String [] postalCodes = {shipmentSkeleton.getDestPostalCd()};
		GetSicForPostalCodesResp sicForPostalCodesResp = getSicForPostalCodes();
		Long seqNbr = 1L;

		GetTrailerSpecificationResp getTrailerSpecificationResp = getTrailerSpecification();

        GetOperationalServiceDaysCountResp opSrvcDaysCntResp = new GetOperationalServiceDaysCountResp();
        opSrvcDaysCntResp.setServiceDaysCount(BigInteger.ONE);
        DetermineOperationalServiceDateResp svcDtResp = new DetermineOperationalServiceDateResp();
        svcDtResp.setServiceDate("01/01/2020");

		when(shipmentDAO.findByIdOrProNumber(PRO_NUMBER, null, entityManager)).thenReturn(null);
        when(externalRestClient.isActiveSicAndLinehaul(shipmentSkeleton.getRequestingSicCd(),
				txnContext)).thenReturn(true);
		when(externalRestClient.getSicForPostalCodes(postalCodes,txnContext)).thenReturn(sicForPostalCodesResp);
		when(externalRestClient.getTrailerSpecification(TRAILER_TYPE_CD, TRAILER_SUB_TYPE_CD, null,
				txnContext)).thenReturn(getTrailerSpecificationResp);
		when(shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(anyLong(), any())).thenReturn(seqNbr);
		when(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(anyLong(), anyLong(), any())).thenReturn(seqNbr);
        when(externalRestClient.getOperationalServiceDates(any(), any(), any(),
            any(com.xpo.ltl.api.location.v2.ServiceTypeCd.class), anyInt(), anyBoolean(), eq(txnContext)))
            .thenReturn(svcDtResp);
        when(getProStatusCdImpl.getProStatus(any(), eq(txnContext), any())).thenReturn(mockGetProStatusCdResp());
        when(getProStatusCdImpl.getProStatusDB2(any(), eq(txnContext), any())).thenReturn(mockGetProStatusCdResp());

		createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

		verify(shipmentDAO, times(1)).create(any(), any());
		verify(shipmentDAO, times(1)).createDB2ShmShipment(any(ShmShipment.class), any(), any());

		verify(shmHandlingUnitSubDAO, times(1)).save(any(), any());
		verify(shmHandlingUnitSubDAO, times(1)).createDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any());

		verify(shmHazMatSubDAO, times(1)).save(any(), any());
		verify(shmHazMatSubDAO, times(1)).insertDB2ShmHazMat(any(ShmHazMat.class), any());

		verify(shmEventLogSubDAO, times(1)).create(any(), any());
		verify(shmEventLogSubDAO, times(1)).createDB2ShmEventLog(any(ShmEventLog.class), any());
        verify(externalRestClient, times(0)).getOperationalServiceDate(any(), any(), any(),
            any(com.xpo.ltl.api.location.v2.ServiceTypeCd.class), anyInt(), anyBoolean(), eq(txnContext));


	}

    private GetProStatusResp mockGetProStatusCdResp() {
        GetProStatusResp mockedResp = new GetProStatusResp();
        mockedResp.setProStatusCd(ProStatusCd.AVAILABLE);
        return mockedResp;
    }

    @Test
    public void createShipmentSkeletonForPickupTest() throws Exception {
        CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
        ShipmentSkeleton shipmentSkeleton = createSkeleton(createHazMatList());
        shipmentSkeleton.setShipmentAcquiredTypeCd(ShipmentAcquiredTypeCd.REGULAR_PKUP);
        request.setShipmentSkeleton(shipmentSkeleton);

        ShmShipment shmShipment = new ShmShipment();
        shmShipment.setTotPcsCnt(BigDecimal.TEN);
        shmShipment.setProNbrTxt(PRO_NUMBER);
        shmShipment.setShpInstId(SHP_INST_ID);
        String[] postalCodes = { shipmentSkeleton.getDestPostalCd() };
        GetSicForPostalCodesResp sicForPostalCodesResp = getSicForPostalCodes();
        Long seqNbr = 1L;

        GetTrailerSpecificationResp getTrailerSpecificationResp = getTrailerSpecification();

        GetOperationalServiceDaysCountResp opSrvcDaysCntResp = new GetOperationalServiceDaysCountResp();
        opSrvcDaysCntResp.setServiceDaysCount(BigInteger.ONE);
        DetermineOperationalServiceDateResp svcDtResp = new DetermineOperationalServiceDateResp();
        svcDtResp.setServiceDate("01/01/2020");

        when(shipmentDAO.findByIdOrProNumber(PRO_NUMBER, null, entityManager)).thenReturn(null);
        when(externalRestClient.isActiveSicAndLinehaul(shipmentSkeleton.getRequestingSicCd(), txnContext))
            .thenReturn(true);
        when(externalRestClient.getSicForPostalCodes(postalCodes, txnContext)).thenReturn(sicForPostalCodesResp);
        when(externalRestClient.getTrailerSpecification(TRAILER_TYPE_CD, TRAILER_SUB_TYPE_CD, null, txnContext))
            .thenReturn(getTrailerSpecificationResp);
        when(shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(anyLong(), any())).thenReturn(seqNbr);
        when(externalRestClient.getOperationalServiceDate(any(), any(), any(),
            any(com.xpo.ltl.api.location.v2.ServiceTypeCd.class), anyInt(), anyBoolean(), eq(txnContext)))
                .thenReturn(svcDtResp);

        createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

        verify(shipmentDAO, times(1)).create(any(), any());
        verify(shipmentDAO, times(1)).createDB2ShmShipment(any(ShmShipment.class), any(), any());

        verify(shmHandlingUnitSubDAO, times(1)).save(any(), any());
        verify(shmHandlingUnitSubDAO, times(1)).createDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any());

        verify(shmHazMatSubDAO, times(1)).save(any(), any());
        verify(shmHazMatSubDAO, times(1)).insertDB2ShmHazMat(any(ShmHazMat.class), any());

        verify(shmEventLogSubDAO, times(1)).create(any(), any());
        verify(shmEventLogSubDAO, times(1)).createDB2ShmEventLog(any(ShmEventLog.class), any());
        verify(externalRestClient, times(1)).getOperationalServiceDate(any(), any(), any(),
            any(com.xpo.ltl.api.location.v2.ServiceTypeCd.class), anyInt(), anyBoolean(), eq(txnContext));

    }

	@Test
	public void testGetDestSic_IsNotUpdateNullPostal_RetunsMoreInfoException() throws Exception {
		List<MoreInfo> moreInfoList = Lists.newArrayList();

		String destSicCode = shipmentSkeletonHelper.getDestSicCodeForPostalCode(null,
            moreInfoList, null, externalRestClient, null);

		assertNull(destSicCode);
		assertEquals(1, moreInfoList.size());
		assertTrue(StringUtils.contains(moreInfoList.get(0).getMessage(),
				ValidationErrorMessage.DEST_POSTAL_CODE_REQUIRED.message()));
	}

	@Test
	public void testGetDestSic_IsNotUpdateWithPostal_RetunsDestSic() throws Exception {
		String[] postalCodes = { "97045" };
		GetSicForPostalCodesResp sicForPostalCodesResp = getSicForPostalCodes();

		when(externalRestClient.getSicForPostalCodes(eq(postalCodes), any()))
				.thenReturn(sicForPostalCodesResp);

		String destSicCode = shipmentSkeletonHelper.getDestSicCodeForPostalCode("97045", null,
            null, externalRestClient, null);

		assertEquals("LDA", destSicCode);
	}

	@Test
	public void testGetDestSic_IsNotUpdateWithPostal_Exception() throws Exception {
		List<MoreInfo> moreInfoList = Lists.newArrayList();
		String[] postalCodes = { "97045" };
		GetSicForPostalCodesResp sicForPostalCodesResp = new GetSicForPostalCodesResp();

		when(externalRestClient.getSicForPostalCodes(eq(postalCodes), any()))
				.thenReturn(sicForPostalCodesResp);

		String destSicCode = shipmentSkeletonHelper.getDestSicCodeForPostalCode("97045",
            moreInfoList, null, externalRestClient, null);

		assertNull(destSicCode);
		assertEquals(1, moreInfoList.size());
		assertTrue(StringUtils.contains(moreInfoList.get(0).getMessage(),
				ValidationErrorMessage.CANT_GET_DEST_SIC.message()));
	}

	private GetTrailerSpecificationResp getTrailerSpecification() {
		GetTrailerSpecificationResp getTrailerSpecificationResp = new GetTrailerSpecificationResp();
		TrailerSpecification trailerSpecification = new TrailerSpecification();
		trailerSpecification.setFullCapacityVolume(new Double("1540"));
		getTrailerSpecificationResp.setTrailerSpecification(trailerSpecification);
		return getTrailerSpecificationResp;
	}

	private GetSicForPostalCodesResp getSicForPostalCodes() {
		GetSicForPostalCodesResp sicRespForPostalCodes = new GetSicForPostalCodesResp();
		List<PostalSicAssignment> postalSicAssignments = new ArrayList<>();
		PostalSicAssignment postalSicAssignment = new PostalSicAssignment();
		postalSicAssignment.setSicCd("LDA");
		postalSicAssignments.add(postalSicAssignment);
		sicRespForPostalCodes.setPostalSicAssignments(postalSicAssignments);
		return sicRespForPostalCodes;
	}

	private ShipmentSkeleton createSkeleton (List<HazMat> hazMats) {
		ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
		shipmentSkeleton.setParentProNbr(PRO_NUMBER);
		shipmentSkeleton.setTotalWeightLbs(new Double(100));
		shipmentSkeleton.setTotalPiecesCount(BigInteger.ONE);
		shipmentSkeleton.setLoosePiecesCount(BigInteger.ONE);
		shipmentSkeleton.setMotorizedPiecesKnownInd(false);
		shipmentSkeleton.setMotorizedPiecesCount(BigInteger.ZERO);
		shipmentSkeleton.setTotalPalletsCount(BigInteger.ZERO);
		shipmentSkeleton.setServiceTypeCd(ServiceTypeCd.GUARANTEED_BY_12_NOON);
		shipmentSkeleton.setRequestingSicCd("UPO");
		shipmentSkeleton.setLastMovementDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));
		shipmentSkeleton.setFreezableInd(false);
		shipmentSkeleton.setFoodPoisonCd(FoodPoisonCd.FOOD);
		shipmentSkeleton.setGuaranteedInd(false);
		shipmentSkeleton.setBulkLiquidInd(false);
		shipmentSkeleton.setDestPostalCd(ZIP_CODE);
		HandlingUnit handlingUnit = new HandlingUnit();
		handlingUnit.setChildProNbr(TRACKING_PRO_1);
		handlingUnit.setLengthNbr(new Double("15"));
		handlingUnit.setWeightLbs(new Double("15"));
		handlingUnit.setHeightNbr(new Double("15"));
		handlingUnit.setWidthNbr(new Double("15"));
		handlingUnit.setReweighInd(Boolean.FALSE);
		List<HandlingUnit> handlingUnits = new ArrayList<>();
		handlingUnits.add(handlingUnit);
		shipmentSkeleton.setHandlingUnits(handlingUnits);
		if (CollectionUtils.isNotEmpty(hazMats)){
			shipmentSkeleton.setHazmatInd(true);
		} else {
			shipmentSkeleton.setHazmatInd(false);
		}
		shipmentSkeleton.setHazmatGroups(hazMats);
		return shipmentSkeleton;
	}

	private List<HazMat> createHazMatList() {
		List<HazMat> hazMats = new ArrayList<>();
		HazMat hazMat = new HazMat();
		hazMat.setHazmatUnna("UN1002");
		hazMat.setHazmatClassCd("2");
		hazMat.setHazmatWeightLbs(500L);
		hazMat.setHazmatBulkQuantityCd("L");
		hazMat.setHazmatResidueInd(true);
		hazMats.add(hazMat);
		return hazMats;
	}

    @Test
    public void testCreateShipmentSkeletonWithZeroTotalVolumeCubicFeetValid() throws Exception {

        CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
        ShipmentSkeleton shipmentSkeleton = createSkeleton(null);
        shipmentSkeleton.setUserId("32193");
        shipmentSkeleton.setPupVolumePercentage(10.2);
        shipmentSkeleton.setTotalVolumeCubicFeet(0.0);
        request.setShipmentSkeleton(shipmentSkeleton);

        List<HandlingUnit> handlingUnits = new ArrayList<>();
        HandlingUnit handlingUnit = new HandlingUnit();
        handlingUnit.setSplitInd(false);
        handlingUnit.setPoorlyPackagedInd(false);
        handlingUnit.setParentProNbr(PRO_NUMBER);
        handlingUnit.setChildProNbr("06481101351");
        handlingUnit.setHandlingMovementCd("NORMAL");
        handlingUnit.setReweighInd(false);
        handlingUnits.add(handlingUnit);
        shipmentSkeleton.setHandlingUnits(handlingUnits);

        ShmShipment shmShipment = new ShmShipment();
        shmShipment.setTotPcsCnt(BigDecimal.TEN);
        shmShipment.setProNbrTxt(PRO_NUMBER);
        shmShipment.setShpInstId(SHP_INST_ID);
        shmShipment.setPupVolPct(new BigDecimal(10));
        String[] postalCodes = { shipmentSkeleton.getDestPostalCd() };
        GetSicForPostalCodesResp sicRespForPostalCodes = getSicForPostalCodes();
        Long seqNbr = 1L;
        
        List<String> proNbrList = new ArrayList<>();
		proNbrList.add(PRO_NUMBER);
		List<ShmShipment> shipmentList = new ArrayList<>();
		shipmentList.add(shmShipment);

        GetTrailerSpecificationResp getTrailerSpecificationResp = getTrailerSpecification();

        Employee employee = new Employee();
        EmployeeRole employeeRole = new EmployeeRole();
        Role role = new Role();
        role.setRoleId(SUPRV_ROLE_ID);
        employeeRole.setRole(role);
        employeeRole.setStartDate(BasicTransformer.toXMLGregorianCalendar(BasicTransformer.toDate("01/01/2020")));
        List<EmployeeRole> employeeRoles = new ArrayList<>();
        employeeRoles.add(employeeRole);
        EmployeeBasic basicInfo = new EmployeeBasic();
        basicInfo.setEmployeeId("32193");
        basicInfo.setOriginalHireDate(BasicTransformer.toXMLGregorianCalendar(BasicTransformer.toDate("01/01/2020")));
        employee.setBasicInfo(basicInfo);
        employee.setRoles(employeeRoles);

        when(shipmentDAO.listShipmentsByProNumbers(proNbrList, entityManager)).thenReturn(shipmentList);
        when(externalRestClient.isActiveSicAndLinehaul(shipmentSkeleton.getRequestingSicCd(), txnContext))
            .thenReturn(true);
        when(externalRestClient.getSicForPostalCodes(postalCodes, txnContext)).thenReturn(sicRespForPostalCodes);
        when(externalRestClient.getTrailerSpecification(TRAILER_TYPE_CD, TRAILER_SUB_TYPE_CD, null, txnContext))
            .thenReturn(getTrailerSpecificationResp);
        when(shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(anyLong(), any())).thenReturn(seqNbr);
        when(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(anyLong(), anyLong(), any())).thenReturn(seqNbr);
        when(externalRestClient.getEmployeeDetailsByEmployeeId(any(), any())).thenReturn(employee);
        doNothing().when(shmHandlingUnitSubDAO).remove(any(Collection.class), any());
        doNothing().when(shmHandlingUnitSubDAO).deleteDB2(any(), any(), any(), any());

        createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

        verify(shipmentDAO, times(1)).save(any(), any());
        verify(shipmentDAO, times(1)).updateDb2ShmShipmentForUpdSkeketon(any(ShmShipment.class), any());
        verify(shmHandlingUnitSubDAO, times(1)).save(any(), any());
        verify(shmHandlingUnitSubDAO, times(1)).createDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any());
    }

    @Test
    public void testReweightHandlingUnitsEvenly_Ok() throws Exception {
        HandlingUnit hu1 = new HandlingUnit();
        HandlingUnit hu2 = new HandlingUnit();
        HandlingUnit hu3 = new HandlingUnit();
        HandlingUnit hu4 = new HandlingUnit();
        hu1.setWeightLbs(10D);
        hu2.setWeightLbs(15D);
        hu3.setWeightLbs(20D);
        hu4.setWeightLbs(25D);
        hu1.setChildProNbr("01110111111");
        hu2.setChildProNbr("01110111111");
        hu3.setChildProNbr("01110111111");
        hu4.setChildProNbr("01230123456");
        hu1.setHandlingMovementCd("NORMAL");
        hu2.setHandlingMovementCd("NORMAL");
        hu3.setHandlingMovementCd("ASTRAY");
        hu4.setHandlingMovementCd("NORMAL");

        List<HandlingUnit> handlingUnits = Arrays.asList(hu1, hu2, hu3, hu4);
        BigDecimal totalWeight = BigDecimal.valueOf(100);

        ShmHandlingUnit huDB1 = new ShmHandlingUnit();
        huDB1.setReweighInd(BasicTransformer.toString(true));
        huDB1.setChildProNbrTxt("01230123456");
        List<ShmHandlingUnit> huListDB = Arrays.asList(huDB1);

        createShipmentSkeletonImpl.reweightHandlingUnitsEvenly(handlingUnits, huListDB , totalWeight);

        // 100 - 25 (rwght=1) = 75
        // 75 / 2 (rwght=0 and nonAstray) = 37.5
        assertEquals(4, handlingUnits.size());
        assertEquals(37.5, handlingUnits.get(0).getWeightLbs());
        assertEquals(37.5, handlingUnits.get(1).getWeightLbs());
        assertEquals(20D, handlingUnits.get(2).getWeightLbs());
        assertEquals(25D, handlingUnits.get(3).getWeightLbs());
    }

    @Test
    public void testCreateShipmentSkeletonWithZeroTotalVolumeCubicFeetInvalid() throws Exception {

        CreateShipmentSkeletonRqst request = new CreateShipmentSkeletonRqst();
        ShipmentSkeleton shipmentSkeleton = createSkeleton(null);
        shipmentSkeleton.setUserId("32193");
        shipmentSkeleton.setPupVolumePercentage(0.0);
        shipmentSkeleton.setTotalVolumeCubicFeet(0.0);
        request.setShipmentSkeleton(shipmentSkeleton);

        ShmShipment shmShipment = new ShmShipment();
        shmShipment.setTotPcsCnt(BigDecimal.TEN);
        shmShipment.setProNbrTxt(PRO_NUMBER);
        shmShipment.setShpInstId(SHP_INST_ID);
        String[] postalCodes = { shipmentSkeleton.getDestPostalCd() };
        GetSicForPostalCodesResp sicRespForPostalCodes = getSicForPostalCodes();
        Long seqNbr = 1L;

        GetTrailerSpecificationResp getTrailerSpecificationResp = getTrailerSpecification();

        Employee employee = new Employee();
        EmployeeRole employeeRole = new EmployeeRole();
        Role role = new Role();
        role.setRoleId(SUPRV_ROLE_ID);
        employeeRole.setRole(role);
        employeeRole.setStartDate(BasicTransformer.toXMLGregorianCalendar(BasicTransformer.toDate("01/01/2020")));
        List<EmployeeRole> employeeRoles = new ArrayList<>();
        employeeRoles.add(employeeRole);
        EmployeeBasic basicInfo = new EmployeeBasic();
        basicInfo.setEmployeeId("32193");
        basicInfo.setOriginalHireDate(BasicTransformer.toXMLGregorianCalendar(BasicTransformer.toDate("01/01/2020")));
        employee.setBasicInfo(basicInfo);
        employee.setRoles(employeeRoles);

        when(shipmentDAO.findByIdOrProNumber(PRO_NUMBER, null, entityManager)).thenReturn(shmShipment);
        when(externalRestClient.isActiveSicAndLinehaul(shipmentSkeleton.getRequestingSicCd(), txnContext))
            .thenReturn(true);
        when(externalRestClient.getSicForPostalCodes(postalCodes, txnContext)).thenReturn(sicRespForPostalCodes);
        when(externalRestClient.getTrailerSpecification(TRAILER_TYPE_CD, TRAILER_SUB_TYPE_CD, null, txnContext))
            .thenReturn(getTrailerSpecificationResp);
        when(shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(anyLong(), any())).thenReturn(seqNbr);
        when(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(anyLong(), anyLong(), any())).thenReturn(seqNbr);
        when(externalRestClient.getEmployeeDetailsByEmployeeId(any(), any())).thenReturn(employee);

        final Exception e = assertThrows(ValidationException.class, () -> {

            createShipmentSkeletonImpl.createShipmentSkeleton(request, txnContext, entityManager);

        });

        assertTrue(e.getMessage().contains("Pup volume percentage has not been calculated."));

        verify(shipmentDAO, times(0)).save(any(), any());
        verify(shipmentDAO, times(0)).updateDB2ShmShipment(any(ShmShipment.class), any(), any(), any());
        verify(shmHandlingUnitSubDAO, times(0)).save(any(), any());
        verify(shmHandlingUnitSubDAO, times(0)).createDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any());
        verify(shmEventLogSubDAO, times(0)).create(any(), any());
        verify(shmEventLogSubDAO, times(0)).createDB2ShmEventLog(any(ShmEventLog.class), any());
    }


}
