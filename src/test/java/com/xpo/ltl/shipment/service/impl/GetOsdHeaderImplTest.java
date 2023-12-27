package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.humanresource.v2.InterfaceEmployee;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.OverageApprovalStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.RefusedReasonCdTransformer;
import com.xpo.ltl.api.shipment.v2.ConeColorCd;
import com.xpo.ltl.api.shipment.v2.GetOsdResp;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdPayloadTypeCd;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.validators.OsdHeaderValidator;

import junit.framework.Assert;
import junit.framework.TestCase;

public class GetOsdHeaderImplTest extends TestCase {

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private OsdHeaderValidator osdHeaderValidator;

	@Mock
	private ShmOsdHdrSubDAO shmOsdHdrSubDAO;

	@Mock
	private ExternalRestClient externalRestClient;

	@Mock
	private ShmShipmentSubDAO shipmentSubDAO;

	@Mock
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Mock
	private ShipmentMovementExceptionSubDAO shipmentMovementExceptionSubDAO;

	@InjectMocks
	private GetOsdHeaderImpl osdHeaderImpl;

	private static final long shipmentIntsId = 7500000005105L;
	private static final String PRO_NBR = "06480210986";
	private static final String CHILD_PRO_NBR = "06481457861";
	private static final Long OSD_ID = 1L;
	private static final String REPORTING_SIC_CD = "UPO";
	private static final String OSD_CATEGORY_CD_DAMAGED = "DAMAGED";
	private static final String OSD_CATEGORY_CD_OVERAGE = "OVERAGE";
	private static final String OSD_CATEGORY_CD_REFUSED = "REFUSED";

	@Override
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetOsdWithOsdIdForDamagedCategoryCd() throws ServiceException {
		List<ShmShipment> shipments = getShipment();
		ShmShipment shmShipment = shipments.get(0);
		ShmOsdHeader shmOsdHeader = getShmOsdHeader(OSD_CATEGORY_CD_DAMAGED);
		when(shmOsdHdrSubDAO.getByOsdIdOrProNumber(any(), any(), any(), any(), any(), anyBoolean(), any()))
				.thenReturn(shmOsdHeader);
		when(shipmentSubDAO.findByProNumber(any(), any())).thenReturn(shipments);
		when(shipmentMovementExceptionSubDAO.findByShpInstIds(any(), any())).thenReturn(getShmMovementExcpsList());
		when(externalRestClient.getEmployeeDetailsMap(any(), any())).thenReturn(getEmployeeDetailsMap());
		
		GetOsdResp resp = osdHeaderImpl.getOsd(OSD_ID, null, OsdCategoryCd.DAMAGED, REPORTING_SIC_CD, OsdStatusCd.D_ON_HAND_NOTICE,
				OsdPayloadTypeCd.HH, txnContext, entityManager);

		assertEquals(resp.getOsdParentShipment().getShipmentInstId().longValue(), shmShipment.getShpInstId());
		assertEquals(resp.getOsdParentShipment().getOsdId().longValue(), shmOsdHeader.getOsdId());
		assertEquals(resp.getOsdParentShipment().getParentProNbr(),
				shmShipment.getShmHandlingUnits().get(0).getParentProNbrTxt());
		assertEquals(resp.getOsdParentShipment().getConeColorCd().toString().toUpperCase(),
				shmOsdHeader.getConeColorCd().toUpperCase());
		assertEquals(resp.getOsdParentShipment().getConeNbr().longValue(), shmOsdHeader.getConeNbr().longValue());
		assertEquals(resp.getOsdParentShipment().getOsdCategoryCd().toString().toUpperCase(),
				OSD_CATEGORY_CD_DAMAGED.toUpperCase());

	}

	@Test
	public void testGetOsdWithOsdIdForOverageCategoryCd() throws ServiceException {
		List<ShmShipment> shipments = getShipment();
		ShmShipment shmShipment = shipments.get(0);
		ShmOsdHeader shmOsdHeader = getShmOsdHeader(OSD_CATEGORY_CD_OVERAGE);
		when(shmOsdHdrSubDAO.getByOsdIdOrProNumber(any(), any(), any(), any(), any(), anyBoolean(), any()))
				.thenReturn(shmOsdHeader);
		when(shipmentSubDAO.findByProNumber(any(), any())).thenReturn(shipments);
		when(shipmentMovementExceptionSubDAO.findByShpInstIds(any(), any())).thenReturn(getShmMovementExcpsList());
		when(externalRestClient.getEmployeeDetailsMap(any(), any())).thenReturn(getEmployeeDetailsMap());

		GetOsdResp resp = osdHeaderImpl.getOsd(null, PRO_NBR, OsdCategoryCd.OVERAGE, REPORTING_SIC_CD, OsdStatusCd.O_READY_TO_SHIP,
				OsdPayloadTypeCd.EDGE_OSD, txnContext, entityManager);

		assertEquals(resp.getOsdParentShipment().getShipmentInstId().longValue(), shmShipment.getShpInstId());
		assertEquals(resp.getOsdParentShipment().getOsdId().longValue(), shmOsdHeader.getOsdId());
		assertEquals(resp.getOsdParentShipment().getParentProNbr(),
				shmShipment.getShmHandlingUnits().get(0).getParentProNbrTxt());
		assertEquals(resp.getOsdParentShipment().getConeColorCd().toString().toUpperCase(),
				shmOsdHeader.getConeColorCd().toUpperCase());
		assertEquals(resp.getOsdParentShipment().getConeNbr().longValue(), shmOsdHeader.getConeNbr().longValue());
		assertEquals(resp.getOsdParentShipment().getOsdCategoryCd().toString().toUpperCase(),
				OSD_CATEGORY_CD_OVERAGE.toUpperCase());
		assertEquals(resp.getOsdParentShipment().getOverageProNbr(), CHILD_PRO_NBR);
		assertEquals(resp.getOsdParentShipment().getBrand(), shmOsdHeader.getShmOsdImages().get(0).getBrand());
		assertEquals(resp.getOsdParentShipment().getPackageCd(), shmOsdHeader.getShmOsdImages().get(0).getPkgCd());
		assertEquals(resp.getOsdParentShipment().getApprovalStatusCd(), OverageApprovalStatusCdTransformer.toEnum(shmOsdHeader.getShmOsdImages().get(0).getApprovalStatusCd()));
	}

	@Test
	public void testGetOsdWithOsdIdForRefusedCategoryCd() throws ServiceException {
		List<ShmShipment> shipments = getShipment();
		ShmShipment shmShipment = shipments.get(0);
		ShmOsdHeader shmOsdHeader = getShmOsdHeader(OSD_CATEGORY_CD_REFUSED);
		when(shmOsdHdrSubDAO.getByOsdIdOrProNumber(any(), any(), any(), any(), any(), anyBoolean(), any()))
				.thenReturn(shmOsdHeader);
		when(shipmentSubDAO.findByProNumber(any(), any())).thenReturn(shipments);
		when(shipmentMovementExceptionSubDAO.findByShpInstIds(any(), any())).thenReturn(getShmMovementExcpsList());
		when(externalRestClient.getEmployeeDetailsMap(any(), any())).thenReturn(getEmployeeDetailsMap());

		GetOsdResp resp = osdHeaderImpl.getOsd(null, CHILD_PRO_NBR, OsdCategoryCd.REFUSED, REPORTING_SIC_CD, OsdStatusCd.R_ON_HAND_NOTICE,
				OsdPayloadTypeCd.EDGE_OSD, txnContext, entityManager);

		assertEquals(resp.getOsdParentShipment().getShipmentInstId().longValue(), shmShipment.getShpInstId());
		assertEquals(resp.getOsdParentShipment().getOsdId().longValue(), shmOsdHeader.getOsdId());
		assertEquals(resp.getOsdParentShipment().getParentProNbr(),
				shmShipment.getShmHandlingUnits().get(0).getParentProNbrTxt());
		assertEquals(resp.getOsdParentShipment().getConeColorCd().toString().toUpperCase(),
				shmOsdHeader.getConeColorCd().toUpperCase());
		assertEquals(resp.getOsdParentShipment().getConeNbr().longValue(), shmOsdHeader.getConeNbr().longValue());
		assertEquals(resp.getOsdParentShipment().getOsdCategoryCd().toString().toUpperCase(),
				OSD_CATEGORY_CD_REFUSED.toUpperCase());
		assertEquals(resp.getOsdParentShipment().getRefusedReasonCd(),
				RefusedReasonCdTransformer.toEnum(shmOsdHeader.getRefusedReasonCd()));

	}

	@Test
	public void testGetOsdWithOsdIdForDamagedCategoryCd_LegacyPro() throws ServiceException {
		List<ShmShipment> shipments = getShipment();
		ShmShipment shmShipment = shipments.get(0);
		shmShipment.setShmHandlingUnits(new ArrayList<>());
		ShmOsdHeader shmOsdHeader = getShmOsdHeader(OSD_CATEGORY_CD_DAMAGED);
		when(shmOsdHdrSubDAO.getByOsdIdOrProNumber(any(), any(), any(), any(), any(), anyBoolean(), any()))
				.thenReturn(shmOsdHeader);
		when(shipmentSubDAO.findByProNumber(any(), any())).thenReturn(shipments);
		when(shipmentMovementExceptionSubDAO.findByShpInstIds(any(), any())).thenReturn(getShmMovementExcpsList());
		when(externalRestClient.getEmployeeDetailsMap(any(), any())).thenReturn(getEmployeeDetailsMap());

		GetOsdResp resp = osdHeaderImpl.getOsd(OSD_ID, null, OsdCategoryCd.DAMAGED, REPORTING_SIC_CD, OsdStatusCd.D_ON_HAND_NOTICE,
				OsdPayloadTypeCd.HH, txnContext, entityManager);

		assertEquals(resp.getOsdParentShipment().getShipmentInstId().longValue(), shmShipment.getShpInstId());
		assertEquals(resp.getOsdParentShipment().getOsdId().longValue(), shmOsdHeader.getOsdId());
		assertEquals(resp.getOsdParentShipment().getConeColorCd().toString().toUpperCase(),
				shmOsdHeader.getConeColorCd().toUpperCase());
		assertEquals(resp.getOsdParentShipment().getConeNbr().longValue(), shmOsdHeader.getConeNbr().longValue());
		assertEquals(resp.getOsdParentShipment().getOsdCategoryCd().toString().toUpperCase(),
				OSD_CATEGORY_CD_DAMAGED.toUpperCase());
	}

	@Test
	public void testGetOsdWithOsdIdForOverageCategoryCd_LegacyPro() throws ServiceException {
		List<ShmShipment> shipments = getShipment();
		ShmShipment shmShipment = shipments.get(0);
		shmShipment.setShmHandlingUnits(new ArrayList<>());
		ShmOsdHeader shmOsdHeader = getShmOsdHeader(OSD_CATEGORY_CD_OVERAGE);
		when(shmOsdHdrSubDAO.getByOsdIdOrProNumber(any(), any(), any(), any(), any(), anyBoolean(), any()))
				.thenReturn(shmOsdHeader);
		when(shipmentSubDAO.findByProNumber(any(), any())).thenReturn(shipments);
		when(shipmentMovementExceptionSubDAO.findByShpInstIds(any(), any())).thenReturn(getShmMovementExcpsList());
		when(externalRestClient.getEmployeeDetailsMap(any(), any())).thenReturn(getEmployeeDetailsMap());

		GetOsdResp resp = osdHeaderImpl.getOsd(null, PRO_NBR, OsdCategoryCd.OVERAGE, REPORTING_SIC_CD, OsdStatusCd.O_SHPR_FOUND,
				OsdPayloadTypeCd.EDGE_OSD, txnContext, entityManager);

		assertEquals(resp.getOsdParentShipment().getShipmentInstId().longValue(), shmShipment.getShpInstId());
		assertEquals(resp.getOsdParentShipment().getOsdId().longValue(), shmOsdHeader.getOsdId());
		assertEquals(resp.getOsdParentShipment().getConeColorCd().toString().toUpperCase(),
				shmOsdHeader.getConeColorCd().toUpperCase());
		assertEquals(resp.getOsdParentShipment().getConeNbr().longValue(), shmOsdHeader.getConeNbr().longValue());
		assertEquals(resp.getOsdParentShipment().getOsdCategoryCd().toString().toUpperCase(),
				OSD_CATEGORY_CD_OVERAGE.toUpperCase());
	}

	@Test
	public void testGetOsdWithOsdIdForRefusedCategoryCd_ChildExistsInHandlingUnit() throws ServiceException {
		List<ShmShipment> shipments = getShipment();
		ShmShipment shmShipment = shipments.get(0);
		ShmOsdHeader shmOsdHeader = getShmOsdHeader(OSD_CATEGORY_CD_REFUSED);
		when(shmOsdHdrSubDAO.getByOsdIdOrProNumber(any(), any(), any(), any(), any(), anyBoolean(), any()))
				.thenReturn(shmOsdHeader);
		when(shipmentSubDAO.findByProNumber(any(), any())).thenReturn(shipments);
		when(shipmentMovementExceptionSubDAO.findByShpInstIds(any(), any())).thenReturn(getShmMovementExcpsList());
		when(shmHandlingUnitSubDAO.findByChildProNumberList(any(), any()))
				.thenReturn(shmShipment.getShmHandlingUnits());
		when(externalRestClient.getEmployeeDetailsMap(any(), any())).thenReturn(getEmployeeDetailsMap());

		GetOsdResp resp = osdHeaderImpl.getOsd(null, CHILD_PRO_NBR, OsdCategoryCd.REFUSED, REPORTING_SIC_CD, OsdStatusCd.R_ON_HAND_NOTICE,
				OsdPayloadTypeCd.EDGE_OSD, txnContext, entityManager);

		assertEquals(resp.getOsdParentShipment().getShipmentInstId().longValue(), shmShipment.getShpInstId());
		assertEquals(resp.getOsdParentShipment().getOsdId().longValue(), shmOsdHeader.getOsdId());
		assertEquals(resp.getOsdParentShipment().getParentProNbr(),
				shmShipment.getShmHandlingUnits().get(0).getParentProNbrTxt());
		assertEquals(resp.getOsdParentShipment().getConeColorCd().toString().toUpperCase(),
				shmOsdHeader.getConeColorCd().toUpperCase());
		assertEquals(resp.getOsdParentShipment().getConeNbr().longValue(), shmOsdHeader.getConeNbr().longValue());
		assertEquals(resp.getOsdParentShipment().getOsdCategoryCd().toString().toUpperCase(),
				OSD_CATEGORY_CD_REFUSED.toUpperCase());
		assertEquals(resp.getOsdParentShipment().getCreatedByUserName(), "FirstName LastName");
		assertEquals(resp.getOsdParentShipment().getDockWorkerUserName(), "FirstName LastName");
		assertEquals(resp.getOsdParentShipment().getAssignedUserName(), "FirstName LastName");
		assertEquals(resp.getOsdParentShipment().getCreatedByUserId(), "Test");
		assertEquals(resp.getOsdParentShipment().getAssignedUserId(), "Test");
		assertEquals(resp.getOsdParentShipment().getDockWorkerUserId(), "Test");
		
	}

	@Test
	public void testGetOsdWithOsdIdForDamagedCategoryCd_WhenOsdEntryNotExist() throws ServiceException {
		List<ShmShipment> shipments = getShipment();
		ShmShipment shmShipment = shipments.get(0);
		shmShipment.setDlvryQalfrCd("M");
		when(shipmentSubDAO.findByProNumber(any(), any())).thenReturn(shipments);
		when(shipmentMovementExceptionSubDAO.findByShpInstIds(any(), any())).thenReturn(getShmMovementExcpsList());
		when(shmHandlingUnitSubDAO.findByChildProNumberList(any(), any()))
				.thenReturn(shmShipment.getShmHandlingUnits());
		when(externalRestClient.getEmployeeDetailsMap(any(), any())).thenReturn(getEmployeeDetailsMap());

		GetOsdResp resp = osdHeaderImpl.getOsd(null, PRO_NBR, OsdCategoryCd.DAMAGED, REPORTING_SIC_CD, OsdStatusCd.D_ON_HAND_NOTICE,
				OsdPayloadTypeCd.EDGE_OSD, txnContext, entityManager);

		assertEquals(resp.getOsdParentShipment().getShipmentInstId().longValue(), shmShipment.getShpInstId());
		assertEquals(resp.getOsdParentShipment().getOsdCategoryCd().toString().toUpperCase(),
				OSD_CATEGORY_CD_DAMAGED.toUpperCase());
		assertEquals(resp.getOsdParentShipment().getParentProNbr(),
				shmShipment.getShmHandlingUnits().get(0).getParentProNbrTxt());
	}

	private List<ShmShipment> getShipment() {
		List<ShmShipment> shmShipments = new ArrayList<>();
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setShpInstId(shipmentIntsId);
		shmShipment.setShmHandlingUnits(getShmHandlingUnits());
		shmShipment.setProNbrTxt(PRO_NBR);
		shmShipments.add(shmShipment);
		return shmShipments;
	}

	private List<ShmHandlingUnit> getShmHandlingUnits() {
		List<ShmHandlingUnit> shmHandlingUnits = Lists.newArrayList();
		ShmHandlingUnit shmHandlingUnit = new ShmHandlingUnit();
		shmHandlingUnit.setParentProNbrTxt(PRO_NBR);
		shmHandlingUnit.setChildProNbrTxt(CHILD_PRO_NBR);
		ShmHandlingUnitPK key = new ShmHandlingUnitPK();
		key.setShpInstId(shipmentIntsId);
		key.setSeqNbr(1);
		shmHandlingUnit.setId(key);
		shmHandlingUnit.setShmHandlingUnitMvmts(getShmHandlingUnitMvmts());
		shmHandlingUnits.add(shmHandlingUnit);

		return shmHandlingUnits;
	}

	private List<ShmMovementExcp> getShmMovementExcpsList() {
		List<ShmMovementExcp> shmMovementList = Lists.newArrayList();
		ShmMovementExcp shmMovementExcp = getShmMovementExcp();
		shmMovementList.add(shmMovementExcp);
		return shmMovementList;
	}

	private ShmMovementExcp getShmMovementExcp() {
		ShmMovementExcp shmMovementExcp = new ShmMovementExcp();
		ShmMovementExcpPK shmMovementExcpPK = new ShmMovementExcpPK();
		shmMovementExcpPK.setShpInstId(shipmentIntsId);
		shmMovementExcpPK.setSeqNbr(1L);
		shmMovementExcpPK.setMvmtSeqNbr(1L);
		shmMovementExcp.setId(shmMovementExcpPK);
		shmMovementExcp.setCrteTmst(new Timestamp(System.currentTimeMillis()));
		shmMovementExcp.setPcsCnt(new BigDecimal(1));
		shmMovementExcp.setTypCd("3");
		return shmMovementExcp;
	}

	private ShmOsdHeader getShmOsdHeader(String osdCategoryCd) {
		ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
		shmOsdHeader.setOsdId(OSD_ID);
		shmOsdHeader.setReportingSicCd(REPORTING_SIC_CD);
		shmOsdHeader.setOsdNumberTxt(REPORTING_SIC_CD + "-" + OSD_ID);
		shmOsdHeader.setShpInstId(BigDecimal.valueOf(shipmentIntsId));
		shmOsdHeader.setOsdCategoryCd(osdCategoryCd);
		shmOsdHeader.setConeColorCd(ConeColorCd.RED.toString().toUpperCase());
		shmOsdHeader.setConeNbr(BigDecimal.valueOf(15L));
		shmOsdHeader.setOsdDescription(osdCategoryCd);
		shmOsdHeader.setStatusCd("D_NOT_STARTED");
		shmOsdHeader.setIdentifiedLocTypeCd("TRAILER");
		shmOsdHeader.setIdentifiedLocId("3132121");
		shmOsdHeader.setHuCnt(BigDecimal.valueOf(1L));
		shmOsdHeader.setAssignedUser("Test");
		shmOsdHeader.setDockWorkerUserid("Test");
		shmOsdHeader.setProNbrTxt(PRO_NBR);
		shmOsdHeader.setRefusedReasonCd("A");
		shmOsdHeader.setCrteBy("Test");
		shmOsdHeader.setShmMgmtRemarks(getShmMgmtRemarks());
		shmOsdHeader.setShmOsdImages(getShmOsdImages(osdCategoryCd));
		return shmOsdHeader;
	}

	private List<ShmMgmtRemark> getShmMgmtRemarks() {
		List<ShmMgmtRemark> shmMgmtRemarks = new ArrayList<>();
		ShmMgmtRemark shmMgmtRemark = new ShmMgmtRemark();
		shmMgmtRemark.setCrteBy("Test");
		shmMgmtRemark.setRemarkTxt("Remark");
		shmMgmtRemarks.add(shmMgmtRemark);
		return shmMgmtRemarks;
	}

	private List<ShmOsdImage> getShmOsdImages(String osdCategoryCd) {
		List<ShmOsdImage> shmOsdImages = new ArrayList<>();

		if (osdCategoryCd.equalsIgnoreCase(OSD_CATEGORY_CD_OVERAGE.toString())) {
			shmOsdImages.add(getShmOsdImageForOvrageMatched());
		} else {
			shmOsdImages.add(getShmOsdImageWithImageInformation());
			shmOsdImages.add(getShmOsdImage());
		}

		return shmOsdImages;
	}

	private ShmOsdImage getShmOsdImageWithImageInformation() {
		ShmOsdImage shmOsdImage = new ShmOsdImage();
		shmOsdImage.setDmsUrl("dmsUrl");
		shmOsdImage.setThumbnailImg("thumbNailImg");
		shmOsdImage.setOrigProNbrTxt(PRO_NBR);
		return shmOsdImage;
	}

	private ShmOsdImage getShmOsdImage() {
		ShmOsdImage shmOsdImage = new ShmOsdImage();
		shmOsdImage.setLenNbr(BigDecimal.valueOf(1L));
		shmOsdImage.setWdthNbr(BigDecimal.valueOf(2L));
		shmOsdImage.setHghtNbr(BigDecimal.valueOf(3L));
		shmOsdImage.setWgtLbs(BigDecimal.valueOf(6L));
		shmOsdImage.setProNbrTxt(CHILD_PRO_NBR);
		return shmOsdImage;
	}

	private ShmOsdImage getShmOsdImageForOvrageMatched() {
		ShmOsdImage shmOsdImage = new ShmOsdImage();
		shmOsdImage.setBrand("brand");
		shmOsdImage.setPkgCd("pkgCd");
		shmOsdImage.setReferenceNbrTxt("referenceNbrTxt");
		shmOsdImage.setSelectedTags("selectedTags");
		shmOsdImage.setDmsUrl("dmsUrl");
		shmOsdImage.setLenNbr(BigDecimal.valueOf(1L));
		shmOsdImage.setWdthNbr(BigDecimal.valueOf(2L));
		shmOsdImage.setHghtNbr(BigDecimal.valueOf(3L));
		shmOsdImage.setWgtLbs(BigDecimal.valueOf(6L));
		shmOsdImage.setOrigProNbrTxt(PRO_NBR);
		shmOsdImage.setProNbrTxt(CHILD_PRO_NBR);
		shmOsdImage.setThumbnailImg("thumbNailImg");
		shmOsdImage.setApprovalStatusCd("Y");
		return shmOsdImage;
	}

	private List<ShmHandlingUnitMvmt> getShmHandlingUnitMvmts() {
		List<ShmHandlingUnitMvmt> handlingUnitMvmts = Lists.newArrayList();
		ShmHandlingUnitMvmt handlingUnitMvmt = new ShmHandlingUnitMvmt();
		ShmHandlingUnitMvmtPK key = new ShmHandlingUnitMvmtPK();
		key.setShpInstId(shipmentIntsId);
		key.setSeqNbr(1L);
		key.setMvmtSeqNbr(1L);
		handlingUnitMvmt.setId(key);
		handlingUnitMvmt.setExcpTypCd("3");
		handlingUnitMvmt.setMvmtTypCd("DELIVER");
		handlingUnitMvmt.setCrteTmst(new Timestamp(System.currentTimeMillis()));
		handlingUnitMvmts.add(handlingUnitMvmt);

		ShmHandlingUnitMvmt handlingUnitMvmt2 = new ShmHandlingUnitMvmt();
		ShmHandlingUnitMvmtPK key2 = new ShmHandlingUnitMvmtPK();
		key2.setShpInstId(shipmentIntsId);
		key2.setSeqNbr(2L);
		key2.setMvmtSeqNbr(2L);
		handlingUnitMvmt2.setId(key);
		handlingUnitMvmt2.setExcpTypCd("3");
		handlingUnitMvmt2.setMvmtTypCd("DELIVER");
		handlingUnitMvmt2.setCrteTmst(new Timestamp(System.currentTimeMillis() + 1000));
		handlingUnitMvmts.add(handlingUnitMvmt2);
		return handlingUnitMvmts;
	}

	private Map<String, InterfaceEmployee> getEmployeeDetailsMap() {
		Map<String, InterfaceEmployee> employeeIdEmployeeDetailsMap = new HashMap<String, InterfaceEmployee>();
		InterfaceEmployee interfaceEmployee = new InterfaceEmployee();
		interfaceEmployee.setFirstName("FirstName");
		interfaceEmployee.setLastName("LastName");
		interfaceEmployee.setPrimaryRoleDescription("Role");
		interfaceEmployee.setEmployeeId("Test");
		employeeIdEmployeeDetailsMap.put("Test", interfaceEmployee);
		return employeeIdEmployeeDetailsMap;
	}

}