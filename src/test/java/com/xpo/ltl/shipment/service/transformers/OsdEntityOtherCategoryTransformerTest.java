package com.xpo.ltl.shipment.service.transformers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Triplet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.CloseReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.IdentifiedLocationTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OtherOsdReasonCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CloseReasonCd;
import com.xpo.ltl.api.shipment.v2.ConeColorCd;
import com.xpo.ltl.api.shipment.v2.IdentifiedLocationTypeCd;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdDocumentImage;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.OtherOsdReasonCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;

import junit.framework.TestCase;

public class OsdEntityOtherCategoryTransformerTest extends TestCase {

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private OsdEntityCommonTransformer osdEntityCommonTransformer;

	@InjectMocks
	private OsdEntityOtherCategoryTransformer osdEntityOtherCategoryTransformer;

	@Override
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testbuildShmOsdHeader_actionCd_ADD() throws ServiceException {

		UpsertOsdRqst upsertOsdRqst = getUpsertOsd();

		AuditInfo auditInfo = getAuditInfo();

		ShmShipment shmShipment = getShmShipment();

		ShmOsdHeader shmOsdHeader = getShmOsdHeader();

		ShmOsdImage shmOsdImage = getShmOsdImage();

		Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails = new Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>, ShmOsdHeader>(
				shmShipment, null, Boolean.TRUE, null, null);

		when(osdEntityCommonTransformer.buildShmOsdHeader(any(), any(), Mockito.anyLong(), any(), any(), any(), any()))
				.thenReturn(shmOsdHeader);

		when(osdEntityCommonTransformer.buildShmOsdImage(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(shmOsdImage);

		shmOsdHeader = osdEntityOtherCategoryTransformer.buildShmOsdHeader(upsertOsdRqst, shipmentAndOsdDetails,
				shmOsdHeader, auditInfo, new ArrayList<>(), txnContext, entityManager);

		assertEquals(shmOsdHeader.getNewParentProNbrTxt(), "06480112235");
		assertEquals(shmOsdHeader.getShpInstId(), BigDecimal.ONE);
		assertEquals(shmOsdHeader.getHuCnt(), BasicTransformer.toBigDecimal(BigInteger.ONE));
		assertEquals(shmOsdHeader.getOtherReasonCd(),
				OtherOsdReasonCdTransformer.toCode(OtherOsdReasonCd.ONLY_CHILD_PRO));
		assertEquals(shmOsdHeader.getStatusCd(), OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
		assertEquals(shmOsdHeader.getShmOsdImages().size(), 2);

		for (ShmOsdImage osdImage : shmOsdHeader.getShmOsdImages()) {
			assertEquals(osdImage.getStatusCd(), OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
			assertEquals(osdImage.getRptgSicCd(), "UPO");
			assertEquals(osdImage.getOsdCategoryCd(), OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
		}

	}

	@Test
	public void testbuildShmOsdHeader_actionCd_UPDATE() throws ServiceException {

		UpsertOsdRqst upsertOsdRqst = getUpsertOsd();
		upsertOsdRqst.setActionCd(ActionCd.UPDATE);

		AuditInfo auditInfo = getAuditInfo();

		ShmShipment shmShipment = getShmShipment();

		ShmOsdHeader shmOsdHeader = getShmOsdHeader();

		ShmOsdImage shmOsdImage = getShmOsdImage();

		Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails = new Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>, ShmOsdHeader>(
				shmShipment, null, Boolean.TRUE, null, null);

		when(osdEntityCommonTransformer.buildShmOsdHeader(any(), any(), Mockito.anyLong(), any(), any(), any(), any()))
				.thenReturn(shmOsdHeader);

		when(osdEntityCommonTransformer.buildShmOsdImage(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(shmOsdImage);

		shmOsdHeader = osdEntityOtherCategoryTransformer.buildShmOsdHeader(upsertOsdRqst, shipmentAndOsdDetails,
				shmOsdHeader, auditInfo, new ArrayList<>(), txnContext, entityManager);

		assertEquals(shmOsdHeader.getNewParentProNbrTxt(), "06480112235");
		assertEquals(shmOsdHeader.getShpInstId(), BigDecimal.ONE);
		assertEquals(shmOsdHeader.getHuCnt(), BasicTransformer.toBigDecimal(BigInteger.ONE));
				assertEquals(shmOsdHeader.getStatusCd(), OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
		assertEquals(shmOsdHeader.getShmOsdImages().size(), 2);

		for (ShmOsdImage osdImage : shmOsdHeader.getShmOsdImages()) {
			assertEquals(osdImage.getStatusCd(), OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
			assertEquals(osdImage.getRptgSicCd(), "UPO");
			assertEquals(osdImage.getOsdCategoryCd(), OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
		}

	}

	private ShmShipment getShmShipment() {
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setShpInstId(1);
		return shmShipment;
	}

	private AuditInfo getAuditInfo() {
		AuditInfo auditInfo = new AuditInfo();
		auditInfo.setCreatedById("B0872");
		auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
		auditInfo.setCreateByPgmId("B0872");
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
		auditInfo.setUpdateByPgmId("B0872");
		auditInfo.setUpdateById("B0872");
		return auditInfo;
	}

	private UpsertOsdRqst getUpsertOsd() {
		UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
		upsertOsdRqst.setActionCd(ActionCd.ADD);

		OsdParentShipment osdParentShipment = new OsdParentShipment();
		osdParentShipment.setReportingSicCd("UPO");
		osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
		osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
		osdParentShipment.setNewParentProNbr("06480112235");
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

		return shmOsdHeader;
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
}