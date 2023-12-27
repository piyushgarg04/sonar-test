package com.xpo.ltl.shipment.service.transformers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.CloseReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.IdentifiedLocationTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentManagementRemarkTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CloseReasonCd;
import com.xpo.ltl.api.shipment.v2.ConeColorCd;
import com.xpo.ltl.api.shipment.v2.IdentifiedLocationTypeCd;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.ShipmentManagementRemarkTypeCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentManagementRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdImageSubDAO;

import junit.framework.TestCase;

public class OsdEntityCommonTransformerTest extends TestCase {

	@Mock
	private ShipmentManagementRemarkSubDAO shipmentManagementRemarkDAO;

	@Mock
	private ShmOsdImageSubDAO shmOsdImageSubDAO;

	@Mock
	private ShmOsdHdrSubDAO shmOsdHdrSubDAO;

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private OsdEntityCommonTransformer osdEntityCommonTransformer;

	@Override
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testbuildShmOsdHeader() throws ServiceException {

		UpsertOsdRqst upsertOsdRqst = getUpsertOsd();

		AuditInfo auditInfo = getAuditInfo();

		ShmShipment shmShipment = getShmShipment();

		ShmOsdHeader shmOsdHeader = osdEntityCommonTransformer.buildShmOsdHeader(upsertOsdRqst, null, 1, auditInfo,
				shmShipment, txnContext, entityManager);

		assertNotNull(shmOsdHeader.getArriveAtOsdTmst());
		assertNotNull(shmOsdHeader.getCrteTmst());
		assertNotNull(shmOsdHeader.getLstUpdtBy());
		assertNotNull(shmOsdHeader.getOsdNumberTxt());
		assertNotNull(shmOsdHeader.getOsdCloseTmst());

		assertEquals(shmOsdHeader.getCrteBy(), "B0872");
		assertEquals(shmOsdHeader.getCrtePgmId(), "B0872");
		assertEquals(shmOsdHeader.getLstUpdtPgmId(), "B0872");
		assertEquals(shmOsdHeader.getProNbrTxt(), "06480112234");
		assertEquals(shmOsdHeader.getReportingSicCd(), "UPO");
		assertEquals(shmOsdHeader.getShpInstId(), BigDecimal.ONE);
		assertEquals(shmOsdHeader.getOsdCategoryCd(), OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
		assertEquals(shmOsdHeader.getConeColorCd(), ConeColorCdTransformer.toCode(ConeColorCd.YELLOW));
		assertEquals(shmOsdHeader.getConeNbr(), BasicTransformer.toBigDecimal(BigInteger.ONE));
		assertEquals(shmOsdHeader.getOsdDescription(), "Desc");
		assertEquals(shmOsdHeader.getIdentifiedLocTypeCd(),
				IdentifiedLocationTypeCdTransformer.toCode(IdentifiedLocationTypeCd.DOCK));
		assertEquals(shmOsdHeader.getIdentifiedLocId(), "1");
		assertEquals(shmOsdHeader.getPhotoCnt(), BigDecimal.ONE);
		assertEquals(shmOsdHeader.getHuCnt(), BasicTransformer.toBigDecimal(BigInteger.ONE));
		assertEquals(shmOsdHeader.getAssignedUser(), "B0872");
		assertEquals(shmOsdHeader.getDockWorkerUserid(), "B0872");
		assertEquals(shmOsdHeader.getStatusCd(), OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
		assertEquals(shmOsdHeader.getCloseReasonCd(),
				CloseReasonCdTransformer.toCode(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY));

	}

	@Test
	public void testbuildShmMgmtRemark() throws ServiceException {

		UpsertOsdRqst upsertOsdRqst = getUpsertOsd();

		AuditInfo auditInfo = getAuditInfo();

		ShmShipment shmShipment = getShmShipment();

		ShmOsdHeader shmOsdHeader = osdEntityCommonTransformer.buildShmOsdHeader(upsertOsdRqst, null, 1, auditInfo,
				shmShipment, txnContext, entityManager);

		OsdManagementRemark osdManagementRemark = getOsdManagementRemark();

		ShmMgmtRemark shmMgmtRemark = osdEntityCommonTransformer.buildShmMgmtRemark(shmOsdHeader, shmShipment,
				osdManagementRemark, auditInfo, entityManager);

		assertNotNull(shmMgmtRemark.getCrteTmst());
		assertNotNull(shmMgmtRemark.getLstUpdtBy());
		assertNotNull(shmMgmtRemark.getShmShipment());
		assertNotNull(shmMgmtRemark.getShmOsdHeader());

		assertEquals(shmMgmtRemark.getRemarkTxt(), "Remark");
		assertEquals(shmMgmtRemark.getRecordVersionNbr(), 0L);
		assertEquals(shmMgmtRemark.getMvmtSeqNbr(), BasicTransformer.toBigDecimal(0L));
		assertEquals(shmMgmtRemark.getMvmtExcpSeqNbr(), BasicTransformer.toBigDecimal(0L));
		assertEquals(shmMgmtRemark.getTypeCd(),
				ShipmentManagementRemarkTypeCdTransformer.toCode(ShipmentManagementRemarkTypeCd.OSD_REMARKS));

	}

	@Test
	public void testbuildShmOsdImage() throws ServiceException {

		UpsertOsdRqst upsertOsdRqst = getUpsertOsd();

		AuditInfo auditInfo = getAuditInfo();

		ShmShipment shmShipment = getShmShipment();

		ShmOsdHeader shmOsdHeader = osdEntityCommonTransformer.buildShmOsdHeader(upsertOsdRqst, null, 1, auditInfo,
				shmShipment, txnContext, entityManager);

		ShmOsdImage shmOsdImage = osdEntityCommonTransformer.buildShmOsdImage(ActionCd.ADD, shmOsdHeader,
				upsertOsdRqst.getOsdParentShipment(), auditInfo, null, txnContext, entityManager);

		assertNotNull(shmOsdImage.getCrteTmst());
		assertNotNull(shmOsdImage.getLstUpdtBy());
		assertNotNull(shmOsdImage.getShmOsdHeader());

		assertEquals(shmOsdImage.getOsdCategoryCd(), OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
		assertEquals(shmOsdImage.getRptgSicCd(), "UPO");
		assertEquals(shmOsdImage.getStatusCd(), OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
		assertEquals(shmOsdImage.getSelectedTags(), "Tags");
		assertEquals(shmOsdImage.getOrigProNbrTxt(), "06480112234");
		assertEquals(shmOsdImage.getOverPairedWithShortInd(), "N");

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
		osdParentShipment.setParentProNbr("06480112234");
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
		return upsertOsdRqst;
	}

	private OsdManagementRemark getOsdManagementRemark() {
		OsdManagementRemark osdManagementRemark = new OsdManagementRemark();
		osdManagementRemark.setRemark("Remark");
		return osdManagementRemark;
	}

}