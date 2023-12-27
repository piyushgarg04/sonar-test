package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.humanresource.v1.Employee;
import com.xpo.ltl.api.humanresource.v1.EmployeeBasic;
import com.xpo.ltl.api.location.v2.FeatureSetting;
import com.xpo.ltl.api.location.v2.ListLocationFeaturesResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentsAsHandlingUnitExemptResp;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentsAsHandlingUnitExemptRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;

public class UpdateShipmentAsHandlingUnitExemptImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private AppContext appContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManager db2EntityManager;

    @Mock
    private ExternalRestClient externalRestClient;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @InjectMocks
    private UpdateShipmentAsHandlingUnitExemptImpl updateShipmentAsHandlingUnitExemptImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");

        final User user = new User();
        user.setUserId("JUNIT");
        user.setEmployeeId("JUNIT");
        List<String> roles = Arrays.asList("TST_LTL_PLT_EXEMPTION");
        user.setRoles(roles);

        when(txnContext.getUser()).thenReturn(user);
        when(txnContext.getTransactionTimestamp()).thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
        when(txnContext.getCorrelationId()).thenReturn("0");

        when(appContext.isProd()).thenReturn(false);
    }

    @Test
    public void testUpdateShipmentAsHandlingUnitExempt_OneInvalid() throws ServiceException {

        UpdateShipmentsAsHandlingUnitExemptRqst req = new UpdateShipmentsAsHandlingUnitExemptRqst();
        req.setProNbrs(Arrays.asList("1111ABCD"));

        UpdateShipmentsAsHandlingUnitExemptResp resp = updateShipmentAsHandlingUnitExemptImpl
            .updateShipmentAsHandlingUnitExempt(req, txnContext, entityManager);

        Assert.assertNotNull(resp.getWarnings());
        Assert.assertEquals(1, resp.getWarnings().size());
        Assert.assertEquals("1111ABCD", resp.getWarnings().get(0).getFieldValue());
        Assert.assertEquals("ProNbr", resp.getWarnings().get(0).getFieldName());
        Assert.assertEquals(ValidationErrorMessage.PRO_NUMBER_FORMAT.errorCode(), resp.getWarnings().get(0).getErrorCd());
        Assert.assertEquals(ValidationErrorMessage.PRO_NUMBER_FORMAT.message(), resp.getWarnings().get(0).getMessage());
    }

    @Test
    public void testUpdateShipmentAsHandlingUnitExempt_OneOk_OneInvalid_And_OneWithHUs() throws ServiceException {

        UpdateShipmentsAsHandlingUnitExemptRqst req = new UpdateShipmentsAsHandlingUnitExemptRqst();
        req.setProNbrs(Arrays.asList("111-123456", "222-123451", "1111ABCD"));
        req.setReason("this is my reason");

        when(shmShipmentSubDAO.bulkUpdateHandlingUnitExempByProNbrList(anyList(), eq("Y"), any(), any(), eq(entityManager)))
            .thenReturn(2);
        when(shmShipmentSubDAO.db2BulkUpdateHandlingUnitExempByProNbrList(anyList(), eq("Y"), any(), any(), eq(db2EntityManager)))
        .thenReturn(2);

        when(shmShipmentSubDAO.listShipmentsByProNbrs
                 (any(), any(), eq(entityManager)))
            .thenReturn(shmListMocked());

        when(externalRestClient.getEmployeeDetailsByEmployeeId(any(), eq(txnContext))).thenReturn(employeeMocked());

        when(externalRestClient.getLocFeatureSetting(eq("HU_EXEMPT_LANE"), eq(txnContext))).thenReturn(featureSettingMocked());

        UpdateShipmentsAsHandlingUnitExemptResp resp = updateShipmentAsHandlingUnitExemptImpl
            .updateShipmentAsHandlingUnitExempt(req, txnContext, entityManager);

        Assert.assertNotNull(resp.getWarnings());
        Assert.assertEquals(2, resp.getWarnings().size());
        Assert.assertEquals("1111ABCD", resp.getWarnings().get(0).getFieldValue());
        Assert.assertEquals("ProNbr", resp.getWarnings().get(0).getFieldName());
        Assert.assertEquals(ValidationErrorMessage.PRO_NUMBER_FORMAT.errorCode(), resp.getWarnings().get(0).getErrorCd());
        Assert.assertEquals(ValidationErrorMessage.PRO_NUMBER_FORMAT.message(), resp.getWarnings().get(0).getMessage());
        Assert.assertEquals("01110123456", resp.getWarnings().get(1).getFieldValue());
        Assert.assertEquals("ProNbr", resp.getWarnings().get(1).getFieldName());
        Assert.assertEquals(ValidationErrorMessage.HANDLING_UNIT_EXIST.errorCode(), resp.getWarnings().get(1).getErrorCd());
        Assert.assertEquals(ValidationErrorMessage.HANDLING_UNIT_EXIST.message("01110123456"), resp.getWarnings().get(1).getMessage());

    }

    private ListLocationFeaturesResp featureSettingMocked() {
        ListLocationFeaturesResp resp = new ListLocationFeaturesResp();
        FeatureSetting fs1 = new FeatureSetting();
        fs1.setSicCd("UCO");
        fs1.setSettingValue("UPO");
        FeatureSetting fs2 = new FeatureSetting();
        fs2.setSicCd("UCO");
        fs2.setSettingValue("XMN");

        resp.setLocationFeatures(Arrays.asList(fs1, fs2));
        return resp;
    }

    private List<ShmShipment> shmListMocked() {
        ShmShipment s1 = new ShmShipment();
        s1.setProNbrTxt("02220123451");
        s1.setOrigTrmnlSicCd("UCO");
        s1.setDestTrmnlSicCd("XMN");
        s1.setCurrSicCd("UCO");

        ShmShipment s2 = new ShmShipment();
        s2.setProNbrTxt("01110123456");
        s2.setOrigTrmnlSicCd("UCO");
        s2.setCurrSicCd("UCO");
        s2.setDestTrmnlSicCd("XMN");
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        s2.setShmHandlingUnits(Arrays.asList(hu1));
        return Arrays.asList(s1, s2);
    }

    private Employee employeeMocked() {
        Employee e = new Employee();
        EmployeeBasic basicInfo = new EmployeeBasic();
        basicInfo.setDeptSic("UCO");
        e.setBasicInfo(basicInfo);
        return e;
    }
}
