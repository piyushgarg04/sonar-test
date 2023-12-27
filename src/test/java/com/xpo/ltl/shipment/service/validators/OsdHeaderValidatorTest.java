package com.xpo.ltl.shipment.service.validators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.location.v2.FeatureSetting;
import com.xpo.ltl.api.location.v2.ListLocationFeaturesResp;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.OsdPayloadTypeCd;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class OsdHeaderValidatorTest extends TestCase {

	@Mock
	private TransactionContext txnContext;

	@Mock
	private ExternalRestClient externalRestClient;

	@InjectMocks
	private OsdHeaderValidator osdHeaderValidator;

	private static final String PRO_NBR = "06480210986";
	private static final Long OSD_ID = 1L;
	private static final String REPORTING_SIC_CD = "UPO";

	@Override
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetOsdForMandaotryValidation_OsdId_ProNbr() throws ServiceException {
		try {
			osdHeaderValidator.validateGetOsdParameters(null, null, REPORTING_SIC_CD, OsdPayloadTypeCd.EDGE_OSD,
					txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:osdId|proNbr, message:Either (osdId and osdPayloadTypeCd) OR (proNbr and reportingSicCd and osdPayloadTypeCd) are required)",
					e.getMessage());
		}
	}

	@Test
	public void testGetOsdForMandaotryValidation_OsdId_OsdPayloadTypeCd() throws ServiceException {
		try {
			osdHeaderValidator.validateGetOsdParameters(OSD_ID, null, REPORTING_SIC_CD, null,
					txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:osdPayloadTypeCd, message:osdId and osdPayloadTypeCd are required)",
					e.getMessage());
		}
	}

	@Test
	public void testGetOsdForMandaotryValidation_ProNbr_ReportingSicCd() throws ServiceException {
		try {
			osdHeaderValidator.validateGetOsdParameters(null, PRO_NBR, null, null, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN020-994E:Mandatory fields are required(location:reportingSicCd|osdPayloadTypeCd, message:proNbr and reportingSicCd and osdPayloadTypeCd are required)",
					e.getMessage());
		}
	}

	@Test
	public void testGetOsdForMandaotryValidation_ReportingSic_Invalid() throws ServiceException {

		ListLocationFeaturesResp locationFeaturesResp = createValidListLocationFeaturesResp();

		try {
			when(externalRestClient.getLocFeatureSetting(any(), any())).thenReturn(locationFeaturesResp);
			when(externalRestClient.isValidOperationalSic(any(), any())).thenReturn(false);
			osdHeaderValidator.validateGetOsdParameters(null, PRO_NBR, "UPO", OsdPayloadTypeCd.EDGE_OSD, txnContext);
		} catch (Exception e) {
			assertEquals(
					"SHMN021-021E:Reporting SIC is not valid.",
					e.getMessage());
		}
	}

    public void testOriginReportingSicValidation() {

        ListLocationFeaturesResp locationFeaturesResp = createValidListLocationFeaturesResp();

        try {

            when(externalRestClient.getLocFeatureSetting(any(), any())).thenReturn(locationFeaturesResp);

            osdHeaderValidator.validateGetOsdParameters(
                    null,
                    PRO_NBR,
                    "PPP",
                    OsdPayloadTypeCd.EDGE_OSD,
                    txnContext);

        } catch (Exception e) {

            assert e instanceof ValidationException;
            ValidationException validationException = ((ValidationException) e);

            assertEquals(
                    "This service is unavailable for SIC: PPP",
                    validationException.getFault().getMoreInfo().get(0).getMessage());

        }

    }

    private ListLocationFeaturesResp createValidListLocationFeaturesResp() {

        ListLocationFeaturesResp locationFeaturesResp = new ListLocationFeaturesResp();
        List<FeatureSetting> locationFeatures = new ArrayList<>();

        FeatureSetting featureSetting;

        featureSetting = new FeatureSetting();
        featureSetting.setSicCd("UPO");
        featureSetting.setSettingValue("Y");

        locationFeatures.add(featureSetting);

        featureSetting = new FeatureSetting();
        featureSetting.setSicCd("LGT");
        featureSetting.setSettingValue("Y");

        locationFeatures.add(featureSetting);

        locationFeaturesResp.setLocationFeatures(locationFeatures);

        return locationFeaturesResp;

    }

}