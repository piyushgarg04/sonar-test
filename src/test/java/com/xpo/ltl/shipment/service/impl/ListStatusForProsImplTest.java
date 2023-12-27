package com.xpo.ltl.shipment.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.google.api.client.util.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillRange;
import com.xpo.ltl.api.shipment.transformer.v2.ProStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.ListStatusForProsResp;
import com.xpo.ltl.api.shipment.v2.ListStatusForProsRqst;
import com.xpo.ltl.api.shipment.v2.ProStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ProFrtBillIndexSubDAO;
import com.xpo.ltl.shipment.service.dao.ProFrtBillRangeSubDAO;


public class ListStatusForProsImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ProFrtBillIndexSubDAO proFrtBillIndexSubDAO;

    @Mock
    private ProFrtBillRangeSubDAO proFrtBillRangeSubDAO;

    @InjectMocks
    private ListStatusForProsImpl listStatusForProsImpl;

    @Before
    public void setUp() {
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
    public void testTrnContextRequired() {
        try {
            listStatusForProsImpl.listStatusForPros(null, null, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("TransactionContext is required", e.getMessage());
        }
    }

    @Test
    public void testListProStatus_EntityManagerRequired() {
        try {
            listStatusForProsImpl.listStatusForPros(null, txnContext, null);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("EntityManager is required", e.getMessage());
        }
    }

    @Test
    public void testListProStatus_InvalidProFormat() throws ServiceException {
        ListStatusForProsRqst request = new ListStatusForProsRqst();
        request.setProNbrs(Arrays.asList("1111ABCD"));
        ListStatusForProsResp listProStatus = listStatusForProsImpl.listStatusForPros(request, txnContext, entityManager);

        assertNotNull(listProStatus);
        assertEquals(1, listProStatus.getWarnings().size());
        assertEquals(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.errorCode(), listProStatus.getWarnings().get(0).getErrorCd());
        assertEquals(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.message(), listProStatus.getWarnings().get(0).getMessage());
        assertEquals("ProNbr", listProStatus.getWarnings().get(0).getFieldName());
        assertEquals("1111ABCD", listProStatus.getWarnings().get(0).getFieldValue());
        assertEquals(0, listProStatus.getProStatuses().size());
    }

    @Test
    public void testListProStatus_ProPrefixInvalid() throws ServiceException {
        
        when(proFrtBillIndexSubDAO.findAllByProNbrList(anyListOf(String.class), eq(entityManager))).thenReturn(Lists.newArrayList());
        when(proFrtBillRangeSubDAO.findByPfxAndSfx(eq("111"), eq("12345"), eq("12345"), eq(entityManager))).thenReturn(null);

        ListStatusForProsRqst request = new ListStatusForProsRqst();
        request.setProNbrs(Arrays.asList("111123456"));
        ListStatusForProsResp listProStatus = listStatusForProsImpl.listStatusForPros(request, txnContext, entityManager);

        assertNotNull(listProStatus);
        assertEquals(1, listProStatus.getWarnings().size());
        assertEquals(ValidationErrorMessage.PRO_NBR_PREFIX_INVALID.errorCode(), listProStatus.getWarnings().get(0).getErrorCd());
        assertEquals(ValidationErrorMessage.PRO_NBR_PREFIX_INVALID.message(), listProStatus.getWarnings().get(0).getMessage());
        assertEquals("ProNbr", listProStatus.getWarnings().get(0).getFieldName());
        assertEquals("01110123456", listProStatus.getWarnings().get(0).getFieldValue());
        assertEquals(0, listProStatus.getProStatuses().size());
    }

    public void testListProStatus_ProRangeInvalid() throws ServiceException {
        
        when(proFrtBillIndexSubDAO.findAllByProNbrList(anyListOf(String.class), eq(entityManager))).thenReturn(Lists.newArrayList());
        when(proFrtBillRangeSubDAO.findByPfxAndSfx(eq("111"), eq("12345"), eq("12345"), eq(entityManager))).thenReturn(null);
        when(proFrtBillRangeSubDAO.findByPfxTxt(eq("111"), eq(entityManager))).thenReturn(mockedProFrtBillRange());

        ListStatusForProsRqst request = new ListStatusForProsRqst();
        request.setProNbrs(Arrays.asList("111123456"));
        ListStatusForProsResp listProStatus = listStatusForProsImpl.listStatusForPros(request, txnContext, entityManager);

        assertNotNull(listProStatus);
        assertEquals(1, listProStatus.getWarnings().size());
        assertEquals(ValidationErrorMessage.PRO_NBR_RANGE_INVALID.errorCode(), listProStatus.getWarnings().get(0).getErrorCd());
        assertEquals(ValidationErrorMessage.PRO_NBR_RANGE_INVALID.message(), listProStatus.getWarnings().get(0).getMessage());
        assertEquals("ProNbr", listProStatus.getWarnings().get(0).getFieldName());
        assertEquals("01110123456", listProStatus.getWarnings().get(0).getFieldValue());
        assertEquals(0, listProStatus.getProStatuses().size());
    }

    @Test
    public void testListProStatus_OneInProFrtBillIx() throws ServiceException {
        ListStatusForProsRqst request = new ListStatusForProsRqst();
        request.setProNbrs(Arrays.asList("111-123456", "222-123451", "333-2222"));

        when(proFrtBillIndexSubDAO.findAllByProNbrList(anyListOf(String.class), eq(entityManager))).thenReturn(mockedProFrtBillIxList());
        when(proFrtBillRangeSubDAO.findByPfxAndSfx(eq("222"), eq("12345"), eq("12345"), eq(entityManager))).thenReturn(mockedProFrtBillRange());

        ListStatusForProsResp listProStatus = listStatusForProsImpl.listStatusForPros(request, txnContext, entityManager);

        assertNotNull(listProStatus);
        assertEquals(1, listProStatus.getWarnings().size());
        assertEquals(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.errorCode(), listProStatus.getWarnings().get(0).getErrorCd());
        assertEquals(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.message(""), listProStatus.getWarnings().get(0).getMessage());
        assertEquals("ProNbr", listProStatus.getWarnings().get(0).getFieldName());
        assertEquals("333-2222", listProStatus.getWarnings().get(0).getFieldValue());
        assertEquals(2, listProStatus.getProStatuses().size());
        assertEquals("UCO", listProStatus.getProStatuses().get(0).getBillSicCd());
        assertEquals("01110123456", listProStatus.getProStatuses().get(0).getProNbr());
        assertEquals(23231L, listProStatus.getProStatuses().get(0).getShipmentInstId());
        assertEquals("10", listProStatus.getProStatuses().get(0).getMovementUnitSequenceNbr());
        assertEquals(ProStatusCd.VOIDED, listProStatus.getProStatuses().get(0).getProStatusCd());
        assertEquals("UFO", listProStatus.getProStatuses().get(1).getBillSicCd());
        assertEquals("02220123451", listProStatus.getProStatuses().get(1).getProNbr());
        assertEquals(0L, listProStatus.getProStatuses().get(1).getShipmentInstId());
        assertEquals("0", listProStatus.getProStatuses().get(1).getMovementUnitSequenceNbr());
        assertEquals(ProStatusCd.AVAILABLE, listProStatus.getProStatuses().get(1).getProStatusCd());

    }

    private List<ProFrtBillIndex> mockedProFrtBillIxList() {
        ProFrtBillIndex p1 = new ProFrtBillIndex();
        p1.setBillSicCd("UCO");
        p1.setProNbrTxt("01110123456");
        p1.setShpInstId(BigDecimal.valueOf(23231));
        p1.setMvmtUnitSeqNbr(BigDecimal.TEN);
        p1.setStatCd(ProStatusCdTransformer.toCode(ProStatusCd.VOIDED));

        return Arrays.asList(p1);
    }

    private ProFrtBillRange mockedProFrtBillRange() {
        ProFrtBillRange p1 = new ProFrtBillRange();
        p1.setOrigSicCd("UFO");
        return p1;
    }
}
