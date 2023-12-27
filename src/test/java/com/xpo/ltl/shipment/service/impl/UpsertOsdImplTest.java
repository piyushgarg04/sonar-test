package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;

import org.hibernate.Query;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.api.client.util.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.CloseReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.IdentifiedLocationTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CloseReasonCd;
import com.xpo.ltl.api.shipment.v2.ConeColorCd;
import com.xpo.ltl.api.shipment.v2.IdentifiedLocationTypeCd;
import com.xpo.ltl.api.shipment.v2.ManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdDocumentImage;
import com.xpo.ltl.api.shipment.v2.OsdHeader;
import com.xpo.ltl.api.shipment.v2.OsdImage;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdResp;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShipmentManagementRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdImageSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.transformers.OsdEntityOtherCategoryTransformer;
import com.xpo.ltl.shipment.service.transformers.OsdEntityTransformer;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.validators.OsdHeaderValidator;
import com.xpo.ltl.shipment.service.validators.OsdOtherCategoryValidator;

import junit.framework.TestCase;

public class UpsertOsdImplTest extends TestCase {

	private static final String CREATE_PGM_ID = "CRTEOSD";

	@Mock
	private AuditInfo auditInfo;

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShmOsdHdrSubDAO shmOsdHdrSubDAO;

	@Mock
	private ShmOsdImageSubDAO shmOsdImageSubDAO;

	@Mock
	private UpsertOsdRqst upsertOsdRqst;

	@Mock
	private UpsertOsdResp upsertOsdResp;

	@Mock
	private OsdDocumentImage osdDocumentImage;

	@Mock
	private ShmOsdHeader osdHeaderEntity;

	@Mock
	private OsdHeader headerRqst;

	@Mock
	private OsdHeaderValidator osdHeaderValidator;

	@Mock
	private OsdEntityTransformer osdEntityTransformer;

	@Mock
	private ShipmentManagementRemarkSubDAO shipmentManagementRemarkDAO;

	@Mock
	private ExternalRestClient externalRestClient;

	@Mock
	private ShmShipmentSubDAO shipmentSubDAO;

	@Mock
	private MaintainShipmentManagementRemarkImpl maintainShipmentManagementRemarkImpl;

	@Mock
	private OsdOtherCategoryValidator osdOtherCategoryValidator;

	@Mock
	private OsdEntityOtherCategoryTransformer osdEntityOtherCategoryTransformer;

	@InjectMocks
	private UpsertOsdImpl upsertOsdImpl;

	@Override
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		auditInfo = new AuditInfo();
		auditInfo.setCreateByPgmId("PgmId");
		auditInfo.setUpdateByPgmId("PgmId");
		auditInfo.setCreatedById("Id");
		auditInfo.setUpdateById("Id");
		auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));

		when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");

		final User user = new User();
		user.setUserId("JUNIT");
		user.setEmployeeId("JUNIT");
		when(txnContext.getUser()).thenReturn(user);

		when(txnContext.getTransactionTimestamp())
				.thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));

		when(txnContext.getCorrelationId()).thenReturn("0");
	}

	@Test
	public void testUpsert_UpsertOsdRqst() throws Exception {
		try {
			upsertOsdImpl.upsertOsd(null, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The OSD header request is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_TxnContextRequired() throws Exception {
		try {
			upsertOsdImpl.upsertOsd(new UpsertOsdRqst(), null, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The TransactionContext is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_EntityManagerRequired() throws Exception {
		try {
			upsertOsdImpl.upsertOsd(new UpsertOsdRqst(), txnContext, null);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The EntityManager is required.", e.getMessage());
		}
	}

	@Test
	private OsdParentShipment getOsdParentShipment() {

		com.xpo.ltl.api.shipment.v2.AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(CREATE_PGM_ID,
				txnContext);

		OsdParentShipment osdParentShipment = new OsdParentShipment();

		List<String> dmsUrls = new ArrayList<>();
		String dmsUrl = "212547988579363625";
		dmsUrls.add(dmsUrl);

		// osdParentShipment.set("2023-04-14T12:44:58.598Z");
		osdParentShipment.setAssignedUserId("Test1");
		osdParentShipment.setAssignedUserName("TestName");
		osdParentShipment.setParentProNbr("06480193173");
		osdParentShipment.setOsdId(3L);
		osdParentShipment.setConeColorCd(ConeColorCd.RED);
		osdParentShipment.setOsdCategoryCd(OsdCategoryCd.REFUSED);
		osdParentShipment.setReportingSicCd("UPO");
		osdParentShipment.setStatusCd(OsdStatusCd.R_NOT_STARTED);

		osdParentShipment.setDmsUrls(dmsUrls);

		return osdParentShipment;

	}

	@Test
	public void testCreateOsdRecord() throws ValidationException, ServiceException, NotFoundException {
		List<MoreInfo> moreInfos = Lists.newArrayList();

		UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
		upsertOsdRqst.setOsdId(712L);
		OsdParentShipment osdParentShipmentRqst = getOsdParentShipment();

		upsertOsdRqst.setOsdChildShipments(Arrays.asList(getOsdChilShipment()));
		upsertOsdRqst.setOsdManagementRemarks(Arrays.asList(getOsdManagementRemark()));
		upsertOsdRqst.setActionCd(ActionCd.ADD);
		upsertOsdRqst.setOsdParentShipment(osdParentShipmentRqst);
		upsertOsdRqst.setOsdDocumentImages(getOsdDocumentImages());

		OsdManagementRemark osdManagementRemark = new OsdManagementRemark();

		auditInfo = new AuditInfo();
		auditInfo.setCreateByPgmId("PgmId");
		auditInfo.setUpdateByPgmId("PgmId");
		auditInfo.setCreatedById("Id");
		auditInfo.setUpdateById("Id");
		auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));

		osdManagementRemark.setRemark("Testing the upsert API");
		osdManagementRemark.setCreatedById(auditInfo.getCreatedById());
		osdManagementRemark.setCreatedByUserName(auditInfo.getCreateByPgmId());
		osdManagementRemark.setCreatedTimestamp(auditInfo.getCreatedTimestamp());
		ManagementRemark mgmtRemarkRqst = new ManagementRemark();
		mgmtRemarkRqst.setParentOsdId(osdParentShipmentRqst.getOsdId());
		mgmtRemarkRqst.setMovementExceptionSequenceNbr(BigInteger.ZERO);
		mgmtRemarkRqst.setMovementSequenceNbr(BigInteger.ZERO);
		mgmtRemarkRqst.setRemark(osdManagementRemark.getRemark());

		ShmMgmtRemark remarkEntity = new ShmMgmtRemark();
		remarkEntity.setRemarkTxt("Test");

		List<OsdDocumentImage> osdDocumentImage = upsertOsdRqst.getOsdDocumentImages();

		ShmOsdHeader osdHeaderEntity = getOsdHeaderEntity();
		OsdHeader osdHeader = new OsdHeader();
		osdHeader.setHandlingUnitCount(BigInteger.valueOf(1L));

		OsdImage osdImage = new OsdImage();
		osdImage.setOverPairedWithShortInd(false);
		String dmsUrl = "212547988579363625";
		ShmOsdImage osdImageEntity = new ShmOsdImage();
		osdImageEntity.setProNbrTxt("06480233611");

		Boolean isLegacyPro = Boolean.FALSE;

		ShmShipment shipmentEntity = new ShmShipment();

		// Set up mock objects
		when(shmOsdHdrSubDAO.findById(any(Long.class), any())).thenReturn(osdHeaderEntity);
		when(shmOsdImageSubDAO.save(any(), any())).thenReturn(osdImageEntity);
		when(osdEntityTransformer.buildImageRqstForParentPros(any(), any(), any(), any(), any())).thenReturn(osdImage);
		when(osdEntityTransformer.buildImageRqstForChildPros(any(), any(), any(), any(), any())).thenReturn(osdImage);
		when(osdEntityTransformer.buildImageRqstForOveragePros(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(osdImage);
		when(shmOsdHdrSubDAO.save(any(), any())).thenReturn(osdHeaderEntity);
		when(osdEntityTransformer.buildOsdHeaderRqst(osdParentShipmentRqst, txnContext)).thenReturn(osdHeader);

		// Call the method to be tested
		upsertOsdImpl.createOsdRecord(upsertOsdRqst, isLegacyPro, txnContext, entityManager, upsertOsdResp,
				osdDocumentImage, osdHeaderEntity, moreInfos);
	}

	@Test
	public void testCreateOsdRecordWithInvalidStatusCd()
			throws ValidationException, ServiceException, NotFoundException {
		List<MoreInfo> moreInfos = Lists.newArrayList();

		UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
		upsertOsdRqst.setOsdId(712L);
		OsdParentShipment osdParentShipmentRqst = getOsdParentShipment();
		osdParentShipmentRqst.setStatusCd(OsdStatusCd.R_DISPO_RECEIVED);

		upsertOsdRqst.setOsdChildShipments(Arrays.asList(getOsdChilShipment()));
		upsertOsdRqst.setOsdManagementRemarks(Arrays.asList(getOsdManagementRemark()));
		upsertOsdRqst.setActionCd(ActionCd.ADD);
		upsertOsdRqst.setOsdParentShipment(osdParentShipmentRqst);
		upsertOsdRqst.setOsdDocumentImages(getOsdDocumentImages());

		OsdManagementRemark osdManagementRemark = new OsdManagementRemark();

		auditInfo = new AuditInfo();
		auditInfo.setCreateByPgmId("PgmId");
		auditInfo.setUpdateByPgmId("PgmId");
		auditInfo.setCreatedById("Id");
		auditInfo.setUpdateById("Id");
		auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));

		osdManagementRemark.setRemark("Testing the upsert API");
		osdManagementRemark.setCreatedById(auditInfo.getCreatedById());
		osdManagementRemark.setCreatedByUserName(auditInfo.getCreateByPgmId());
		osdManagementRemark.setCreatedTimestamp(auditInfo.getCreatedTimestamp());
		ManagementRemark mgmtRemarkRqst = new ManagementRemark();
		mgmtRemarkRqst.setParentOsdId(osdParentShipmentRqst.getOsdId());
		mgmtRemarkRqst.setMovementExceptionSequenceNbr(BigInteger.ZERO);
		mgmtRemarkRqst.setMovementSequenceNbr(BigInteger.ZERO);
		mgmtRemarkRqst.setRemark(osdManagementRemark.getRemark());

		ShmMgmtRemark remarkEntity = new ShmMgmtRemark();
		remarkEntity.setRemarkTxt("Test");

		List<OsdDocumentImage> osdDocumentImage = upsertOsdRqst.getOsdDocumentImages();

		ShmOsdHeader osdHeaderEntity = getOsdHeaderEntity();
		OsdHeader osdHeader = new OsdHeader();
		osdHeader.setHandlingUnitCount(BigInteger.valueOf(1L));

		OsdImage osdImage = new OsdImage();
		osdImage.setOverPairedWithShortInd(false);
		String dmsUrl = "212547988579363625";
		ShmOsdImage osdImageEntity = new ShmOsdImage();
		osdImageEntity.setProNbrTxt("06480233611");

		Boolean isLegacyPro = Boolean.FALSE;

		ShmShipment shipmentEntity = new ShmShipment();

		// Set up mock objects
		when(shmOsdHdrSubDAO.findById(any(Long.class), any())).thenReturn(osdHeaderEntity);
		when(shmOsdImageSubDAO.save(any(), any())).thenReturn(osdImageEntity);
		when(osdEntityTransformer.buildImageRqstForParentPros(any(), any(), any(), any(), any())).thenReturn(osdImage);
		when(osdEntityTransformer.buildImageRqstForChildPros(any(), any(), any(), any(), any())).thenReturn(osdImage);
		when(osdEntityTransformer.buildImageRqstForOveragePros(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(osdImage);
		when(shmOsdHdrSubDAO.save(any(), any())).thenReturn(osdHeaderEntity);
		when(osdEntityTransformer.buildOsdHeaderRqst(osdParentShipmentRqst, txnContext)).thenReturn(osdHeader);

		// Call the method to be tested
		try {
			upsertOsdImpl.createOsdRecord(upsertOsdRqst, isLegacyPro, txnContext, entityManager, upsertOsdResp,
					osdDocumentImage, osdHeaderEntity, moreInfos);
			fail("Expected an exception.");
		} catch (final ValidationException e) {
			assertEquals(
					"Invalid OsdStatusCd provided for create OS&D operation.(Please remove this or provide a *not_started status.",
					e.getFault().getMoreInfo().get(0).getMessage());
		}
	}

	@Test
	public void testCreateOsdRecordWithNullStatusCd() throws ValidationException, ServiceException, NotFoundException {
		List<MoreInfo> moreInfos = Lists.newArrayList();

		UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
		upsertOsdRqst.setOsdId(712L);
		OsdParentShipment osdParentShipmentRqst = getOsdParentShipment();
		osdParentShipmentRqst.setStatusCd(null);

		upsertOsdRqst.setOsdChildShipments(Arrays.asList(getOsdChilShipment()));
		upsertOsdRqst.setOsdManagementRemarks(Arrays.asList(getOsdManagementRemark()));
		upsertOsdRqst.setActionCd(ActionCd.ADD);
		upsertOsdRqst.setOsdParentShipment(osdParentShipmentRqst);
		upsertOsdRqst.setOsdDocumentImages(getOsdDocumentImages());

		OsdManagementRemark osdManagementRemark = new OsdManagementRemark();

		auditInfo = new AuditInfo();
		auditInfo.setCreateByPgmId("PgmId");
		auditInfo.setUpdateByPgmId("PgmId");
		auditInfo.setCreatedById("Id");
		auditInfo.setUpdateById("Id");
		auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));

		osdManagementRemark.setRemark("Testing the upsert API");
		osdManagementRemark.setCreatedById(auditInfo.getCreatedById());
		osdManagementRemark.setCreatedByUserName(auditInfo.getCreateByPgmId());
		osdManagementRemark.setCreatedTimestamp(auditInfo.getCreatedTimestamp());
		ManagementRemark mgmtRemarkRqst = new ManagementRemark();
		mgmtRemarkRqst.setParentOsdId(osdParentShipmentRqst.getOsdId());
		mgmtRemarkRqst.setMovementExceptionSequenceNbr(BigInteger.ZERO);
		mgmtRemarkRqst.setMovementSequenceNbr(BigInteger.ZERO);
		mgmtRemarkRqst.setRemark(osdManagementRemark.getRemark());

		ShmMgmtRemark remarkEntity = new ShmMgmtRemark();
		remarkEntity.setRemarkTxt("Test");

		List<OsdDocumentImage> osdDocumentImage = upsertOsdRqst.getOsdDocumentImages();

		ShmOsdHeader osdHeaderEntity = getOsdHeaderEntity();
		OsdHeader osdHeader = new OsdHeader();
		osdHeader.setHandlingUnitCount(BigInteger.valueOf(1L));

		OsdImage osdImage = new OsdImage();
		osdImage.setOverPairedWithShortInd(false);
		String dmsUrl = "212547988579363625";
		ShmOsdImage osdImageEntity = new ShmOsdImage();
		osdImageEntity.setProNbrTxt("06480233611");

		Boolean isLegacyPro = Boolean.FALSE;

		ShmShipment shipmentEntity = new ShmShipment();

		// Set up mock objects
		when(shmOsdHdrSubDAO.findById(any(Long.class), any())).thenReturn(osdHeaderEntity);
		when(shmOsdImageSubDAO.save(any(), any())).thenReturn(osdImageEntity);
		when(osdEntityTransformer.buildImageRqstForParentPros(any(), any(), any(), any(), any())).thenReturn(osdImage);
		when(osdEntityTransformer.buildImageRqstForChildPros(any(), any(), any(), any(), any())).thenReturn(osdImage);
		when(osdEntityTransformer.buildImageRqstForOveragePros(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(osdImage);
		when(shmOsdHdrSubDAO.save(any(), any())).thenReturn(osdHeaderEntity);
		when(osdEntityTransformer.buildOsdHeaderRqst(osdParentShipmentRqst, txnContext)).thenReturn(osdHeader);

		upsertOsdImpl.createOsdRecord(upsertOsdRqst, isLegacyPro, txnContext, entityManager, upsertOsdResp,
				osdDocumentImage, osdHeaderEntity, moreInfos);

	}

	private ShmOsdHeader getOsdHeaderEntity() {
		ShmOsdHeader shmOsdHeader = new ShmOsdHeader();

		shmOsdHeader.setArriveAtOsdTmst(
				new Timestamp(auditInfo.getCreatedTimestamp().toGregorianCalendar().getTimeInMillis()));
		shmOsdHeader.setAssignedUser("Test");
		shmOsdHeader.setOsdId(709);

		return shmOsdHeader;
	}

	private List<OsdDocumentImage> getOsdDocumentImages() {

		List<OsdDocumentImage> osdDocumentImages = new ArrayList<>();

		OsdDocumentImage osdDocumentImage = new OsdDocumentImage();

		osdDocumentImage.setDmsUrl("212547988579363625");
		osdDocumentImage.setThumbnailImage("data:image/png;base64,<document data>");

		osdDocumentImages.add(osdDocumentImage);

		return osdDocumentImages;
	}

	private OsdManagementRemark getOsdManagementRemark() {
		OsdManagementRemark osdManagementRemark = new OsdManagementRemark();

		auditInfo = new AuditInfo();
		auditInfo.setCreateByPgmId("PgmId");
		auditInfo.setUpdateByPgmId("PgmId");
		auditInfo.setCreatedById("Id");
		auditInfo.setUpdateById("Id");
		auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));

		osdManagementRemark.setRemark("Testing the upsert API");
		osdManagementRemark.setCreatedById(auditInfo.getCreatedById());
		osdManagementRemark.setCreatedByUserName(auditInfo.getCreateByPgmId());
		osdManagementRemark.setCreatedTimestamp(auditInfo.getCreatedTimestamp());

		return osdManagementRemark;
	}

	private OsdChildShipment getOsdChilShipment() {
		OsdChildShipment osdChildShipment = new OsdChildShipment();

		osdChildShipment.setChildProNbr("06481494401");
		osdChildShipment.setStatusCd("Refused");
		osdChildShipment.setActionCd(ActionCd.ADD);

		return osdChildShipment;
	}

	@Test
	public void testUpsertOsdImages() throws ServiceException {
		// Create mock objects for dependencies
		List<OsdChildShipment> childShipments = new ArrayList<>();
		OsdParentShipment parentShipment = new OsdParentShipment();
		String dmsUrl = "http://localhost:8080";
		long rqstOsdId = 1234L;
		ActionCd actionCd = ActionCd.ADD;
		String thumbnailImage = "data:image/png;base64,<document data>";
		ShmOsdHeader osdHeader = new ShmOsdHeader();
		TransactionContext txnContext = new TransactionContext();
		List<MoreInfo> moreInfos = new ArrayList<>();
		AuditInfo auditInfo = new AuditInfo();
		EntityManager entityManager = mock(EntityManager.class);

		// Mock the behavior of entityManager to return a mock Query object
		Query query = mock(Query.class);
		when(entityManager.createNamedQuery(anyString())).thenReturn(query);

		// Call the method under test
		upsertOsdImpl.createOsdImages(childShipments, parentShipment, dmsUrl, thumbnailImage, osdHeader, rqstOsdId,
				actionCd, txnContext, moreInfos, auditInfo, entityManager);

	}

	@Test
	public void testUpdateOsdImage() throws ServiceException {
		// Create mock objects for dependencies
		ShmOsdImage osdImageEntity = new ShmOsdImage();
		OsdImage osdImageRqst = new OsdImage();
		EntityManager entityManager = mock(EntityManager.class);
		AuditInfo auditInfo = new AuditInfo();

		// Call the method under test
		upsertOsdImpl.updateOsdImage(osdImageEntity, osdImageRqst, entityManager, auditInfo);
	}

	@Test
	public void testCreateOsdImagesForParentPro() throws Exception {
		// given
		OsdParentShipment osdParentShipment = new OsdParentShipment();
		String dmsUrl = "http://example.com";
		String thumbnailImage = "data:image/png;base64,<document data>";
		ShmOsdHeader shmOsdHeaderEntity = new ShmOsdHeader();
		TransactionContext txnContext = new TransactionContext();
		List<MoreInfo> moreInfos = new ArrayList<>();

		ShmOsdImage osdImageEntity = new ShmOsdImage();
		osdImageEntity.setProNbrTxt("06480233611");

		AuditInfo auditInfo = new AuditInfo();

		auditInfo = new AuditInfo();
		auditInfo.setCreateByPgmId("PgmId");
		auditInfo.setUpdateByPgmId("PgmId");
		auditInfo.setCreatedById("Id");
		auditInfo.setUpdateById("Id");
		auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));

		OsdImage osdImageRqst = new OsdImage();

		when(osdEntityTransformer.buildImageRqstForParentPros(osdParentShipment, dmsUrl, thumbnailImage, auditInfo,
				txnContext)).thenReturn(osdImageRqst);
		when(shmOsdImageSubDAO.save(any(), any())).thenReturn(osdImageEntity);
		Mockito.doNothing().when(osdHeaderValidator).validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment,
				auditInfo, moreInfos, ActionCd.ADD, txnContext, entityManager);

		Mockito.doNothing().when(entityManager).persist(any(OsdImage.class));

		// when
		upsertOsdImpl.createOsdImagesForParentPro(osdParentShipment, dmsUrl, thumbnailImage, shmOsdHeaderEntity,
				txnContext, moreInfos, ActionCd.ADD, auditInfo, entityManager);

		// then
		Mockito.verify(osdEntityTransformer).buildImageRqstForParentPros(osdParentShipment, dmsUrl, thumbnailImage,
				auditInfo, txnContext);
		Mockito.verify(osdHeaderValidator).validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo,
				moreInfos, ActionCd.ADD, txnContext, entityManager);
	}

	@Test
	public void testCreateOsdImagesForChildPros() throws Exception {
		// given
		OsdParentShipment osdParentShipment = new OsdParentShipment();
		String dmsUrl = "http://example.com";
		String thumbnailImage = "data:image/png;base64,<document data>";
		ShmOsdHeader osdHeaderEntity = new ShmOsdHeader();
		TransactionContext txnContext = new TransactionContext();
		List<MoreInfo> moreInfos = new ArrayList<>();
		AuditInfo auditInfo = new AuditInfo();
		auditInfo = new AuditInfo();
		auditInfo.setCreateByPgmId("PgmId");
		auditInfo.setUpdateByPgmId("PgmId");
		auditInfo.setCreatedById("Id");
		auditInfo.setUpdateById("Id");
		auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date()));

		List<OsdChildShipment> osdChildShipments = new ArrayList<>();
		OsdChildShipment osdChildShipment = new OsdChildShipment();
		osdChildShipment.setActionCd(ActionCd.ADD);
		osdChildShipments.add(osdChildShipment);

		OsdImage osdImageRqst = new OsdImage();

		OsdHeader osdHeader = new OsdHeader();

		ShmOsdImage osdImageEntity = new ShmOsdImage();
		osdImageEntity.setProNbrTxt("06480233611");

		Mockito.when(osdEntityTransformer.buildImageRqstForChildPros(osdChildShipment, osdHeaderEntity,
				osdParentShipment, auditInfo, txnContext)).thenReturn(osdImageRqst);
		when(osdEntityTransformer.buildOsdHeaderRqst(any(), any())).thenReturn(osdHeader);
		when(shmOsdImageSubDAO.save(any(), any())).thenReturn(osdImageEntity);

		Mockito.doNothing().when(osdHeaderValidator).validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment,
				auditInfo, moreInfos, ActionCd.ADD, txnContext, entityManager);

		Mockito.doNothing().when(entityManager).persist(Mockito.any(OsdImage.class));

		// when
		upsertOsdImpl.createOsdImagesForChildPros(osdChildShipment, osdParentShipment, dmsUrl, thumbnailImage,
				osdHeaderEntity, txnContext, moreInfos, ActionCd.ADD, auditInfo, entityManager);

	}

	@Test
	public void testCreateMgmtRemarkForShipment() throws Exception {
		// Create test input data
		final Long parentShipmentInstId = 123L;
		final ManagementRemark managementRemark = new ManagementRemark();
		managementRemark.setParentShipmentInstId(parentShipmentInstId);
		managementRemark.setRemark("Test remark text");

		final TransactionContext txnContext = new TransactionContext();
		final EntityManager entityManager = null;

		final ShmShipment shipmentEntity = new ShmShipment();
		shipmentEntity.setShpInstId(parentShipmentInstId);

		final AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);

		// Mock external dependencies
		when(shipmentSubDAO.findById(eq(parentShipmentInstId), any())).thenReturn(shipmentEntity);
		when(shipmentManagementRemarkDAO.save(any(), any())).thenReturn(new ShmMgmtRemark());

		// Call the method under test
		final ShmMgmtRemark result = maintainShipmentManagementRemarkImpl.createMgmtRemark(managementRemark, null, null,
				txnContext, entityManager);
	}

	@Test
	public void testCreateMgmtRemarkForOSD() throws Exception {
		// Create test input data
		final Long parentOsdId = 456L;
		final ManagementRemark managementRemark = new ManagementRemark();
		managementRemark.setParentOsdId(parentOsdId);
		managementRemark.setRemark("Test remark text");

		final TransactionContext txnContext = new TransactionContext();
		final EntityManager entityManager = null;

		final ShmOsdHeader osdHeaderEntity = new ShmOsdHeader();
		osdHeaderEntity.setOsdId(parentOsdId);

		final AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);

		// Mock external dependencies
		when(shmOsdHdrSubDAO.findById(eq(parentOsdId), any())).thenReturn(osdHeaderEntity);
		when(shipmentManagementRemarkDAO.save(any(), any())).thenReturn(new ShmMgmtRemark());

		// Call the method under test
		final ShmMgmtRemark result = maintainShipmentManagementRemarkImpl.createMgmtRemark(managementRemark, null, null,
				txnContext, entityManager);
	}

	@Test
	public void testCreateOsdImagesForOveragePros() throws ServiceException {
		List<MoreInfo> moreInfos = new ArrayList<>();
		OsdParentShipment osdParentShipment = new OsdParentShipment();
		String dmsUrl = "https://example.com";
		String thumbnailImage = "data:image/png;base64,<document data>";
		ShmOsdImage osdImageEntity = new ShmOsdImage();
		osdImageEntity.setProNbrTxt("06480233611");
		ActionCd actionCd = ActionCd.ADD;

		OsdImage osdImageRqst = new OsdImage();
		when(osdEntityTransformer.buildImageRqstForOveragePros(osdParentShipment, osdImageEntity, dmsUrl,
				thumbnailImage, actionCd, auditInfo, txnContext)).thenReturn(osdImageRqst);

		when(shmOsdImageSubDAO.save(any(), any())).thenReturn(osdImageEntity);

		osdHeaderValidator.validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo, moreInfos,
				ActionCd.ADD, txnContext, entityManager);
		verify(osdHeaderValidator).validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo, moreInfos,
				ActionCd.ADD, txnContext, entityManager);

		upsertOsdImpl.saveOsdImage(osdImageRqst, osdHeaderEntity, entityManager, auditInfo);
	}

	@Test
	public void testCreateOsdForOtherCategory() throws ValidationException, ServiceException, NotFoundException {

		UpsertOsdRqst upsertOsd = getUpsertOsd();
		ShmOsdHeader shmOsdHeader = getShmOsdHeader();
		List<ShmShipment> shmShipments = getShmShipments();

		when(osdEntityOtherCategoryTransformer.buildShmOsdHeader(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(shmOsdHeader);

		when(shipmentSubDAO.findByProNumber(Mockito.anyList(), any())).thenReturn(shmShipments);

		upsertOsdImpl.upsertOsdForOtherCategory(upsertOsd, txnContext, null, entityManager);

		verify(shmOsdHdrSubDAO, times(1)).save(any(), any());
		verify(shmOsdImageSubDAO, times(1)).save(any(), any());
		verify(shipmentManagementRemarkDAO, times(1)).save(any(), any());
		verify(entityManager, times(1)).flush();
		verify(shipmentSubDAO, times(2)).findByProNumber(any(), any());

	}

	@Test
	public void testUpdateOsdForOtherCategory() throws ValidationException, ServiceException, NotFoundException {

		UpsertOsdRqst upsertOsd = getUpsertOsd();
		upsertOsd.setActionCd(ActionCd.UPDATE);
		upsertOsd.setOsdId(1L);
		ShmOsdHeader shmOsdHeader = getShmOsdHeader();
		List<ShmShipment> shmShipments = getShmShipments();

		when(osdEntityOtherCategoryTransformer.buildShmOsdHeader(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(shmOsdHeader);

		when(shipmentSubDAO.findByProNumber(Mockito.anyList(), any())).thenReturn(shmShipments);

		upsertOsdImpl.upsertOsdForOtherCategory(upsertOsd, txnContext, shmOsdHeader, entityManager);
		
		verify(shmOsdHdrSubDAO, times(1)).save(any(), any());
		verify(shmOsdImageSubDAO, times(1)).save(any(), any());
		verify(shipmentManagementRemarkDAO, times(1)).save(any(), any());
		verify(entityManager, times(1)).flush();
		verify(shipmentSubDAO, times(2)).findByProNumber(any(), any());

	}

	private UpsertOsdRqst getUpsertOsd() {
		UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
		upsertOsdRqst.setActionCd(ActionCd.ADD);

		OsdParentShipment osdParentShipment = new OsdParentShipment();
		osdParentShipment.setReportingSicCd("UPO");
		osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
		osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
		osdParentShipment.setNewParentProNbr("06480112235");
		osdParentShipment.setParentProNbr("06480112235");
		osdParentShipment.setArriveAtOsdDateTime(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
		osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
		osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
		osdParentShipment.setConeNbr(BigInteger.ONE);
		osdParentShipment.setIdentifiedLocationTypeCd(IdentifiedLocationTypeCd.DOCK);
		osdParentShipment.setIdentifiedLocationId("1");
		osdParentShipment.setAssignedUserId("B0872");
		osdParentShipment.setDockWorkerUserId("B0872");
		osdParentShipment.setOsdPiecesCount(BigInteger.ONE);
		osdParentShipment.setOsdDescription("Desc");
		osdParentShipment.setSelectedTags("Tags");

		upsertOsdRqst.setOsdParentShipment(osdParentShipment);

		List<OsdChildShipment> osdChildShipments = new ArrayList<>();
		OsdChildShipment osdChildShipment = new OsdChildShipment();
		osdChildShipment.setActionCd(ActionCd.ADD);
		osdChildShipment.setChildProNbr("06481112234");
		osdChildShipments.add(osdChildShipment);

		upsertOsdRqst.setOsdChildShipments(osdChildShipments);

		List<OsdDocumentImage> osdDocumentImages = new ArrayList<>();
		OsdDocumentImage osdDocumentImage = new OsdDocumentImage();
		osdDocumentImage.setDmsUrl("23232323");
		osdDocumentImages.add(osdDocumentImage);
		upsertOsdRqst.setOsdDocumentImages(osdDocumentImages);

		List<OsdManagementRemark> osdManagementRemarks = new ArrayList<>();
		OsdManagementRemark osdManagementRemark = new OsdManagementRemark();
		osdManagementRemark.setRemark("Remark");
		osdManagementRemarks.add(osdManagementRemark);

		upsertOsdRqst.setOsdManagementRemarks(osdManagementRemarks);

		return upsertOsdRqst;
	}

	private ShmOsdHeader getShmOsdHeader() {
		ShmOsdHeader shmOsdHeader = new ShmOsdHeader();

		shmOsdHeader.setArriveAtOsdTmst(
				BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance())));
		shmOsdHeader.setCrteTmst(
				BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance())));
		shmOsdHeader.setCrteBy("B0872");
		shmOsdHeader.setCrtePgmId("B0872");
		shmOsdHeader.setLstUpdtPgmId("B0872");
		shmOsdHeader.setProNbrTxt("06480112234");
		shmOsdHeader.setReportingSicCd("UPO");
		shmOsdHeader.setShpInstId(BigDecimal.ONE);
		shmOsdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
		shmOsdHeader.setConeColorCd(ConeColorCdTransformer.toCode(ConeColorCd.YELLOW));
		shmOsdHeader.setConeNbr(BasicTransformer.toBigDecimal(BigInteger.ONE));
		shmOsdHeader.setOsdDescription("Desc");
		shmOsdHeader.setIdentifiedLocTypeCd(IdentifiedLocationTypeCdTransformer.toCode(IdentifiedLocationTypeCd.DOCK));
		shmOsdHeader.setIdentifiedLocId("1");
		shmOsdHeader.setPhotoCnt(BigDecimal.ONE);
		shmOsdHeader.setHuCnt(BasicTransformer.toBigDecimal(BigInteger.ONE));
		shmOsdHeader.setAssignedUser("B0872");
		shmOsdHeader.setDockWorkerUserid("B0872");
		shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
		shmOsdHeader.setCloseReasonCd(CloseReasonCdTransformer.toCode(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY));

		List<ShmOsdImage> shmOsdImages = new ArrayList<>();
		shmOsdImages.add(getShmOsdImage());
		shmOsdHeader.setShmOsdImages(shmOsdImages);

		List<ShmMgmtRemark> shmMgmtRemarks = new ArrayList<>();
		shmMgmtRemarks.add(getShmMgmtRemark());
		shmOsdHeader.setShmMgmtRemarks(shmMgmtRemarks);

		return shmOsdHeader;
	}

	private ShmMgmtRemark getShmMgmtRemark() {
		ShmMgmtRemark shmMgmtRemark = new ShmMgmtRemark();
		shmMgmtRemark.setRemarkTxt("Remark");
		return shmMgmtRemark;
	}

	private ShmOsdImage getShmOsdImage() {
		ShmOsdImage shmOsdImage = new ShmOsdImage();

		shmOsdImage.setCrteTmst(
				BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance())));
		shmOsdImage.setProNbrTxt("06480112234");
		shmOsdImage.setRptgSicCd("UPO");
		shmOsdImage.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
		shmOsdImage.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
		shmOsdImage.setDmsUrl("123123123");

		return shmOsdImage;
	}

	private List<ShmShipment> getShmShipments() {
		List<ShmShipment> shmShipments = new ArrayList<>();
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setShpInstId(1);
		shmShipment.setShmHandlingUnits(new ArrayList<>());
		shmShipments.add(shmShipment);
		return shmShipments;
	}
}