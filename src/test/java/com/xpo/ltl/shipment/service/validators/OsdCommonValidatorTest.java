package com.xpo.ltl.shipment.service.validators;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CloseReasonCd;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;

import junit.framework.TestCase;

public class OsdCommonValidatorTest extends TestCase {

	@Mock
	private TransactionContext txnContext;
	
	@Mock
	private OsdHeaderValidator osdHeaderValidator;

	@InjectMocks
	private OsdCommonValidator osdCommonValidator;

	@Override
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testValidate_Mandatory_actionCd() throws ServiceException {
		try {
			osdCommonValidator.validate(new UpsertOsdRqst(),txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:actionCd, message:actionCd is required)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Valid_actionCd() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.DELETE);
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:actionCd, message:Please provide a valid actionCd (Add or Update) to perform a create/update operation for OS&D.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Mandatory_osdCategoryCd() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:osdCategoryCd, message:osdCategoryCd is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Mandatory_reportingSicCd() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:reportingSicCd, message:reportingSicCd is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Valid_statusCdIfPassed() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:statusCd, message:Invalid OsdStatusCd provided for create OS&D operation.(Please remove this or provide a *not_started status.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Valid_statusCdAndosdCategoryCdIfPassed() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.REFUSED);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_NOT_STARTED);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:statusCd & osdCategoryCd, message:OSD Status and OSD Category code are not matching.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Valid_Remark() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_NOT_STARTED);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			List<OsdManagementRemark> osdManagementRemarks = new ArrayList<>();
			osdManagementRemarks.add(new OsdManagementRemark());
			
			upsertOsdRqst.setOsdManagementRemarks(osdManagementRemarks);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:osdManagementRemarks.remark, message:remark is mandatory.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Mandatory_osdChildShipmentActionCd() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_NOT_STARTED);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			List<OsdChildShipment> osdChildShipments = new ArrayList<>();
			osdChildShipments.add(new OsdChildShipment());
			
			upsertOsdRqst.setOsdChildShipments(osdChildShipments);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:osdChildShipments.actionCd, message:actionCd is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Mandatory_osdChildShipmentChildProNbr() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_NOT_STARTED);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			List<OsdChildShipment> osdChildShipments = new ArrayList<>();
			OsdChildShipment osdChildShipment = new OsdChildShipment();
			osdChildShipment.setActionCd(ActionCd.ADD);
			osdChildShipments.add(osdChildShipment);
			
			upsertOsdRqst.setOsdChildShipments(osdChildShipments);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:osdChildShipments.childProNbr, message:Child Pro Number is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Valid_osdChildShipmentChildProNbr() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_NOT_STARTED);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			List<OsdChildShipment> osdChildShipments = new ArrayList<>();
			OsdChildShipment osdChildShipment = new OsdChildShipment();
			osdChildShipment.setActionCd(ActionCd.ADD);
			osdChildShipment.setChildProNbr("06480112234");
			osdChildShipments.add(osdChildShipment);
			
			upsertOsdRqst.setOsdChildShipments(osdChildShipments);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:osdChildShipments.childProNbr, message:Cannot accept parent PRO format for child pro numbers in OsdChildShipments.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Mandatory_closeReasonCd() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:closeReasonCd, message:Closed Reason Code is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Valid_closeReasonCd_OnlyIfstatusCdisClosed() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_NOT_STARTED);
			osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:closeReasonCd, message:Closed Due To reason is required only for close OS&D entry.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Valid_closeReasonCd_statusCdNotPassed() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:closeReasonCd, message:Closed Due To reason is required only for close OS&D entry.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testValidate_Valid_parentProNbr() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
			osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
			osdParentShipment.setParentProNbr("06480112234");
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			osdCommonValidator.validate(upsertOsdRqst,txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:parentProNbr, message:The PRO number entered does not have a valid format.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateOsdPayloadWithDB_Valid_parentProNbr() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
			osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
			osdParentShipment.setParentProNbr("06480112234");
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			
			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setReportingSicCd("UPH");
			
			osdCommonValidator.validateOsdPayloadWithDB(upsertOsdRqst, shmOsdHeader, null, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:reportingSicCd, message:Reporting SIC cannot be updated.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateOsdPayloadWithDB_Valid_CannotUpdate_OsdWhenClosed() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
			osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
			osdParentShipment.setParentProNbr("06480112234");
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			
			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_CLOSED));
			
			osdCommonValidator.validateOsdPayloadWithDB(upsertOsdRqst, shmOsdHeader, null, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:statusCd, message:Cannot update an OS&D once closed.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateOsdPayloadWithDB_Valid_CannotUpdate_arriveAtOsdTimestamp() throws ServiceException {
		try {
			
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
			osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
			osdParentShipment.setParentProNbr("06480112234");
			osdParentShipment.setArriveAtOsdDateTime(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));
			shmOsdHeader.setArriveAtOsdTmst(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(calendar)));
			
			osdCommonValidator.validateOsdPayloadWithDB(upsertOsdRqst, shmOsdHeader, null, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:arriveAtOsdDateTime, message:Cannot update arriveAtOsdDateTime after creation.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateOsdPayloadWithDB_Valid_statusCd_CategotyCd_NotMatching() throws ServiceException {
		try {
			
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.R_CLOSED);
			osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
			osdParentShipment.setParentProNbr("06480112234");
			osdParentShipment.setArriveAtOsdDateTime(BasicTransformer.toXMLGregorianCalendar(calendar));
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));
			shmOsdHeader.setArriveAtOsdTmst(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(calendar)));
			
			osdCommonValidator.validateOsdPayloadWithDB(upsertOsdRqst, shmOsdHeader, null, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:statusCd & osdCategoryCd, message:OSD Status and OSD Category code are not matching)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateOsdPayloadWithDB_Valid_parentPro_NotExist() throws ServiceException {
		try {
			
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);
			
			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setReportingSicCd("UPO");
			osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
			osdParentShipment.setCloseReasonCd(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);
			osdParentShipment.setParentProNbr("06480112234");
			osdParentShipment.setArriveAtOsdDateTime(BasicTransformer.toXMLGregorianCalendar(calendar));
			
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);
			
			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));
			shmOsdHeader.setArriveAtOsdTmst(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(calendar)));
			
			osdCommonValidator.validateOsdPayloadWithDB(upsertOsdRqst, shmOsdHeader, null, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:parentProNbr, message:Parent PRO# 06480112234 does not exist.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateAuditInfo_Mandatory_auditInfoCreatedById() throws ServiceException {
		try {	
			osdCommonValidator.validateAuditInfo(new AuditInfo(), txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:auditInfo.createdById, message:createdById is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateAuditInfo_Mandatory_auditInfoCreatedByTimeStamp() throws ServiceException {
		try {
			AuditInfo auditInfo = new AuditInfo();
			auditInfo.setCreatedById("B0872");
			osdCommonValidator.validateAuditInfo(auditInfo, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:auditInfo.createdTimestamp, message:createdTimestamp is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateAuditInfo_Mandatory_auditInfoCreatedByPgmId() throws ServiceException {
		try {
			AuditInfo auditInfo = new AuditInfo();
			auditInfo.setCreatedById("B0872");
			auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
			osdCommonValidator.validateAuditInfo(auditInfo, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:auditInfo.createByPgmId, message:createByPgmId is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateAuditInfo_Mandatory_auditInfoUpdatedTimestamp() throws ServiceException {
		try {	
			AuditInfo auditInfo = new AuditInfo();
			auditInfo.setCreatedById("B0872");
			auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
			auditInfo.setCreateByPgmId("B0872");
			osdCommonValidator.validateAuditInfo(auditInfo, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:auditInfo.updatedTimestamp, message:updatedTimestamp is required.)",
					e.getMessage());
		}
	}
	
	@Test
	public void testvalidateAuditInfo_Mandatory_auditInfoUpdateByPgmId() throws ServiceException {
		try {
			AuditInfo auditInfo = new AuditInfo();
			auditInfo.setCreatedById("B0872");
			auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
			auditInfo.setCreateByPgmId("B0872");
			auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
			osdCommonValidator.validateAuditInfo(auditInfo, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:auditInfo.updateByPgmId, message:updateByPgmId is required.)",
					e.getMessage());
		}
	}

}