package com.xpo.ltl.shipment.service.validators;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Triplet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OtherOsdReasonCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CloseReasonCd;
import com.xpo.ltl.api.shipment.v2.ConeColorCd;
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

public class OsdOtherCategoryValidatorTest extends TestCase {

	@Mock
	private TransactionContext txnContext;

	@Mock
	private OsdCommonValidator osdCommonValidator;

	@InjectMocks
	private OsdOtherCategoryValidator osdOtherCategoryValidator;

	@Override
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testValidate_Mandatory_coneNbr() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			osdOtherCategoryValidator.validate(upsertOsdRqst, txnContext);
		} catch (Exception e) {
			assertEquals("SHMN020-994E:Mandatory fields are required(location:coneNbr, message:coneNbr is required.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Mandatory_coneColorCd() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			osdOtherCategoryValidator.validate(upsertOsdRqst, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:coneColorCd, message:coneColorCd is required.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Valid_coneColorCd() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.GREEN);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			osdOtherCategoryValidator.validate(upsertOsdRqst, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:coneColorCd, message:Cone Color Cd should be Yellow.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Mandatory_osdDocumentImagesdmsUrl() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			osdOtherCategoryValidator.validate(upsertOsdRqst, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:osdDocumentImages.dmsUrl, message:At least 1 photo is required.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Mandatory_otherReasonCd_OnlyIf_newParentProNbrPassed() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
			osdParentShipment.setNewParentProNbr("06480112234");
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			List<OsdDocumentImage> osdDocumentImages = new ArrayList<>();
			OsdDocumentImage osdDocumentImage = new OsdDocumentImage();
			osdDocumentImage.setDmsUrl("23232323");
			osdDocumentImages.add(osdDocumentImage);
			upsertOsdRqst.setOsdDocumentImages(osdDocumentImages);

			osdOtherCategoryValidator.validate(upsertOsdRqst, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:otherOsdReasonCd, message:otherOsdReasonCd is required.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Valid_newParentProNbr() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.ADD);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
			osdParentShipment.setNewParentProNbr("06481112234");
			osdParentShipment.setOtherOsdReasonCd(OtherOsdReasonCd.FNL_IN_ERROR);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			List<OsdDocumentImage> osdDocumentImages = new ArrayList<>();
			OsdDocumentImage osdDocumentImage = new OsdDocumentImage();
			osdDocumentImage.setDmsUrl("23232323");
			osdDocumentImages.add(osdDocumentImage);
			upsertOsdRqst.setOsdDocumentImages(osdDocumentImages);

			osdOtherCategoryValidator.validate(upsertOsdRqst, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:newParentProNbr, message:The new PRO number entered does not have a valid format.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Valid_cannotUpdate_ColorCdToRed() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.RED);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			List<OsdDocumentImage> osdDocumentImages = new ArrayList<>();
			OsdDocumentImage osdDocumentImage = new OsdDocumentImage();
			osdDocumentImage.setDmsUrl("23232323");
			osdDocumentImages.add(osdDocumentImage);
			upsertOsdRqst.setOsdDocumentImages(osdDocumentImages);

			osdOtherCategoryValidator.validate(upsertOsdRqst, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:coneColorCd, message:Cone Color Cd cannot be Red.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Valid_cannotUpdate_categoryCd() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.REFUSED);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));

			Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails = new Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>, ShmOsdHeader>(
					null, null, null, null, null);

			osdOtherCategoryValidator.validateOtherCategoryPayloadwithDB(upsertOsdRqst, shipmentAndOsdDetails,
					shmOsdHeader, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:osdCategoryCd, message:Cannot update the category code for Other entries.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Valid_cannotUpdate_newParentProNbr() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
			osdParentShipment.setNewParentProNbr("06480112234");
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));
			shmOsdHeader.setNewParentProNbrTxt("06480112235");

			Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails = new Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>, ShmOsdHeader>(
					null, null, null, null, null);

			osdOtherCategoryValidator.validateOtherCategoryPayloadwithDB(upsertOsdRqst, shipmentAndOsdDetails,
					shmOsdHeader, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:newParentProNbr, message:Cannot update the New Parent Pro Nbr.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Valid_newParentProNbr_ShouldExist() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
			osdParentShipment.setNewParentProNbr("06480112234");
			osdParentShipment.setOtherOsdReasonCd(OtherOsdReasonCd.CROSSED);
			osdParentShipment.setParentProNbr("06480112234");
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));

			Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails = new Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>, ShmOsdHeader>(
					null, null, null, null, null);

			osdOtherCategoryValidator.validateOtherCategoryPayloadwithDB(upsertOsdRqst, shipmentAndOsdDetails,
					shmOsdHeader, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:parentProNbr, message:Parent PRO# 06480112234 does not exist.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Mandatory_osdPiecesCount_ForLegacyPro() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));

			Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails = new Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>, ShmOsdHeader>(
					null, null, Boolean.TRUE, null, null);

			osdOtherCategoryValidator.validateOtherCategoryPayloadwithDB(upsertOsdRqst, shipmentAndOsdDetails,
					shmOsdHeader, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:osdPiecesCount, message:OSD Pieces Count is required for Legacy Pro.)",
					e.getMessage());
		}
	}

	@Test
	public void testValidate_Valid_OtherReasonCd_CloseReasonCd_() throws ServiceException {
		try {
			UpsertOsdRqst upsertOsdRqst = new UpsertOsdRqst();
			upsertOsdRqst.setActionCd(ActionCd.UPDATE);

			OsdParentShipment osdParentShipment = new OsdParentShipment();
			osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			osdParentShipment.setConeNbr(BigInteger.ONE);
			osdParentShipment.setConeColorCd(ConeColorCd.YELLOW);
			osdParentShipment.setStatusCd(OsdStatusCd.OT_CLOSED);
			osdParentShipment.setCloseReasonCd(CloseReasonCd.RETURNED_TO_SHIPPER);
			osdParentShipment.setParentProNbr("06480112234");
			upsertOsdRqst.setOsdParentShipment(osdParentShipment);

			ShmOsdHeader shmOsdHeader = new ShmOsdHeader();
			shmOsdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER));
			shmOsdHeader.setReportingSicCd("UPO");
			shmOsdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));
			shmOsdHeader.setOtherReasonCd(OtherOsdReasonCdTransformer.toCode(OtherOsdReasonCd.FNL_IN_ERROR));

			Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails = new Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>, ShmOsdHeader>(
					new ShmShipment(), null, Boolean.FALSE, null, null);

			osdOtherCategoryValidator.validateOtherCategoryPayloadwithDB(upsertOsdRqst, shipmentAndOsdDetails,
					shmOsdHeader, txnContext, null);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:closeReasonCd, message:For OtherOsdReasonCd fnl_in_error, closeReason can be [made_gcbz_new_bill, dup_entry_cncl_entry])",
					e.getMessage());
		}
	}

}