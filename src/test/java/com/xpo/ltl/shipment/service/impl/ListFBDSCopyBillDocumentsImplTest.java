package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.appointment.v1.AppointmentNotificationStatCd;
import com.xpo.ltl.api.appointment.v1.AuditInfo;
import com.xpo.ltl.api.appointment.v1.DeliveryNotification;
import com.xpo.ltl.api.appointment.v1.ListAppointmentNotificationsForShipmentsResp;
import com.xpo.ltl.api.customer.v1.DetermineRestrictedBillToResp;
import com.xpo.ltl.api.location.v2.GetLocOperationsServiceCenterProfitabilityBySicResp;
import com.xpo.ltl.api.location.v2.GetLocReferenceDetailsBySicResp;
import com.xpo.ltl.api.location.v2.LocOperationsSvccProfitability;
import com.xpo.ltl.api.location.v2.LocationReference;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.DocumentFormTypeCd;
import com.xpo.ltl.api.shipment.v2.ListFBDSDocumentsResp;
import com.xpo.ltl.api.shipment.v2.MovementException;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentSupRefSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCustomsBondSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.FBDSCopyBillUtil;

public class ListFBDSCopyBillDocumentsImplTest {

    private static final String PRO_NBR_TXT = "09860881755";
    private static final Long SHP_INST_ID = 1111L;

	@Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

	@Mock
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Mock
	private ShipmentAsEnteredCustomerDAO shmAsEntdCustsSubDAO;
	
	@Mock
	private ShmMovementSubDAO shmMovementSubDAO;
	
	@Mock
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;
	
	@Mock
	private ShipmentAcSvcSubDAO shmAcSvcSubDAO;
	
	@Mock
	private ShipmentAdvBydSubDAO shmAdvBydSubDAO;
	
	@Mock
	private ShmCommoditySubDAO shmCommoditySubDAO;

	@Mock
	private ShipmentSupRefSubDAO shmSrNbrSubDAO;
	
	@Mock
	private ShipmentMiscLineItemSubDAO shmMiscLineItemSubDAO;
	
	@Mock
	private ShmCustomsBondSubDAO shmCustomsBondSubDAO;

	@Mock
	private ShipmentRemarkSubDAO shmRemarkSubDAO;
	
	@Mock
	private ShipmentMovementExceptionSubDAO shmMovementExceptionSubDAO;
	
	@Mock
	private ExternalRestClient restClient;
    
    @Mock
    FBDSCopyBillUtil util;
    @InjectMocks
    private ListFBDSCopyBillDocumentsImpl listFBDSCopyBillDocumentsImpl;

    
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
    public void testListFBDSDocuments() throws Exception {
        String[] proNbrs = { PRO_NBR_TXT };
        DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.FBDS;
        when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(getShipmentData());
        when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(getShipmentData());
        when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(getShipment());
        when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
        when(util.getMovementExceptionForShipment(Mockito.anyList(), Mockito.anyMap())).thenReturn(getMovementException());
        ListFBDSDocumentsResp response =
            listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                (proNbrs,
                 null,
                 documentRequestType,
                 false,
                 false,
                 txnContext,
                 entityManager);
        Assert.assertTrue(response != null);
    }

    @Test
    public void testListFBDSDocumentsWhenshipmentsEmpty() throws Exception {
        String[] proNbrs = { PRO_NBR_TXT };
        DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.FBDS;
        when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(new ArrayList<>());
        when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(getShipmentData());
        when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(getShipment());
        when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
        when(util.getMovementExceptionForShipment(Mockito.anyList(), Mockito.anyMap())).thenReturn(getMovementException());
        ListFBDSDocumentsResp response =
            listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                (proNbrs,
                 null,
                 documentRequestType,
                 false,
                 false,
                 txnContext,
                 entityManager);
        Assert.assertTrue(response != null);
    }

    @Test
    public void testListFBDSDocumentsForNonPartialSegment() throws Exception {
        String[] proNbrs = { PRO_NBR_TXT };
        DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.FBDS;
        ShmShipment shipment = getShipment();
        shipment.setDlvryQalfrCd("K");
        shipment.setBillClassCd("C");
        when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
        when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
        when(util.getMovementExceptionForShipment(Mockito.anyList(), Mockito.anyMap())).thenReturn(getMovementException());
        ListFBDSDocumentsResp response =
            listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                (proNbrs,
                 null,
                 documentRequestType,
                 false,
                 false,
                 txnContext,
                 entityManager);
        Assert.assertTrue(response != null);
    }

    @Test
    public void testListFBDSDocumentsForAstray() throws Exception {
        String[] proNbrs = { PRO_NBR_TXT };
        DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.FBDS;
        ShmShipment shipment = getShipment();
        shipment.setDlvryQalfrCd("K");
        shipment.setBillClassCd("I");
        when(util.displayRatesAndCharges(Mockito.anyBoolean(),Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyBoolean(),Mockito.anyMap())).thenReturn(Boolean.TRUE);
        when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
        when(restClient.listShipmentAppointmentNotifications(Mockito.any(),Mockito.any())).thenReturn(getListShipmentAppointmentNotificationsResp());
        when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
        when(util.getMovementExceptionForShipment(Mockito.anyList(), Mockito.anyMap())).thenReturn(getMovementException());
        ListFBDSDocumentsResp response =
            listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                (proNbrs,
                 null,
                 documentRequestType,
                 false,
                 false,
                 txnContext,
                 entityManager);
        Assert.assertTrue(response!=null);
    }

    @Test
    public void testListFBDSDocumentsForPartialSegment() throws Exception {
        String[] proNbrs = { PRO_NBR_TXT };
        DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.FBDS;
        ShmShipment shipment = getShipment();
        shipment.setDlvryQalfrCd("J");
        shipment.setBillClassCd("E");
        shipment.setChrgToCd("B");
        when(util.displayRatesAndCharges(Mockito.anyBoolean(),Mockito.any(),Mockito.any(),Mockito.any(),Mockito.anyBoolean(),Mockito.anyMap())).thenReturn(Boolean.TRUE);
        when(restClient.getCustomerRestrictedInfo(Mockito.any(),Mockito.any())).thenReturn(getDetermineRestrictedBillToResp());
        when(restClient.getLocReferenceDetailsBySic(Mockito.any(),Mockito.any())).thenReturn(getGetLocReferenceDetailsBySicResp());
        when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
        when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
        when(util.getMovementExceptionForShipment(Mockito.anyList(), Mockito.anyMap())).thenReturn(getMovementException());
        ListFBDSDocumentsResp response =
            listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                (proNbrs,
                 null,
                 documentRequestType,
                 false,
                 false,
                 txnContext,
                 entityManager);
        Assert.assertTrue(response != null);
    }

    @Test
    public void testListFBDSDocumentsForCopyBill() throws Exception {
        String[] proNbrs = { PRO_NBR_TXT };
        DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.COPY_BILL;
        ShmShipment shipment = getShipment();
        shipment.setBillStatCd("3");
        shipment.setBillClassCd("D");
        when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
        when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
        ListFBDSDocumentsResp response =
            listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                (proNbrs,
                 null,
                 documentRequestType,
                 false,
                 false,
                 txnContext,
                 entityManager);
        Assert.assertTrue(response!=null);
    }

    @Test
    public void testListFBDSDocumentsForCopyBillAndAstrayBillClass() throws Exception {
        String[] proNbrs = { PRO_NBR_TXT };
        DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.COPY_BILL;
        ShmShipment shipment = getShipment();
        shipment.setBillStatCd("3");
        shipment.setBillClassCd("I");
        when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
        when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
        when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
        ListFBDSDocumentsResp response =
            listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                (proNbrs,
                 null,
                 documentRequestType,
                 false,
                 false,
                 txnContext,
                 entityManager);
        Assert.assertTrue(response != null);
    }

    @Test
    public void testListFBDSDocumentsForCopyBillAndNotAstrayBillClass() throws Exception {
        try {
            String[] proNbrs = { PRO_NBR_TXT };
            DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.COPY_BILL;
            ShmShipment shipment = getShipment();
            shipment.setBillStatCd("1");
            shipment.setBillClassCd("G");
            shipment.setDlvryQalfrCd("A");
            shipment.setRevBillInd("Y");
            when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
            when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
            when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
            when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
            ListFBDSDocumentsResp response =
                listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                    (proNbrs,
                     null,
                     documentRequestType,
                     false,
                     false,
                     txnContext,
                     entityManager);
            Assert.assertTrue(response != null);
        }
        catch (Exception e) {
            assertThat(e.getMessage(), containsString("PRO not billed"));
        }
    }

    @Test
    public void testListFBDSDocumentsForUnbilledStatus() throws Exception {
        try {
            String[] proNbrs = { PRO_NBR_TXT };
            DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.FBDS;
            ShmShipment shipment = getShipment();
            shipment.setBillStatCd("1");
            shipment.setBillClassCd("D");
            shipment.setRevBillInd("Y");
            when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
            when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
            when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
            when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
            ListFBDSDocumentsResp response =
                listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                    (proNbrs,
                     null,
                     documentRequestType,
                     false,
                     false,
                     txnContext,
                     entityManager);
            Assert.assertTrue(response != null);
        }
        catch (Exception e) {
            assertThat(e.getMessage(), containsString("PRO not billed"));
        }
    }

    @Test
    public void testListFBDSDocumentsForEXPEDITEBilltatus() throws Exception {
        try {
            String[] proNbrs = { PRO_NBR_TXT };
            DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.FBDS;
            ShmShipment shipment = getShipment();
            shipment.setBillStatCd("1");
            shipment.setBillClassCd("E");
            when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
            when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
            when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
            when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
            ListFBDSDocumentsResp response =
                listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                    (proNbrs,
                     null,
                     documentRequestType,
                     false,
                     false,
                     txnContext,
                     entityManager);
            Assert.assertTrue(response != null);
        }
        catch (Exception e) {
            assertThat(e.getMessage(), containsString("PRO not billed"));
        }
    }

    @Test
    public void testListFBDSDocumentsForClaimsOvrgRptgBillBilltatus() throws Exception {
        try {
            String[] proNbrs = { PRO_NBR_TXT };
            DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.COPY_BILL;
            ShmShipment shipment = getShipment();
            shipment.setBillStatCd("1");
            shipment.setBillClassCd("G");
            when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(Arrays.asList(shipment));
            when(shmShipmentSubDAO.getRelatedShipments(Mockito.any(),Mockito.any())).thenReturn(Arrays.asList(shipment));
            when(shmShipmentSubDAO.findById(Mockito.anyLong(),Mockito.any())).thenReturn(shipment);
            when(util.retrieveCollectMoneyAtDeliveryMethod(Mockito.anyDouble(),Mockito.any(),Mockito.anyList(),Mockito.anyList(),Mockito.anyList(),Mockito.anyBoolean(),Mockito.anyBoolean(),Mockito.anyDouble())).thenReturn( new FBDSCopyBillUtil().new FBDSDerivedAttributes());
            ListFBDSDocumentsResp response =
                listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                    (proNbrs,
                     null,
                     documentRequestType,
                     false,
                     false,
                     txnContext,
                     entityManager);
            Assert.assertTrue(response != null);
        }
        catch (Exception e) {
            assertThat(e.getMessage(), containsString("PRO not billed"));
        }
    }

    @Test
    public void testListFBDSDocumentsWhenPronumbersEmpty() {
        try {
        	String[] proNbrs = null;
            DocumentFormTypeCd documentRequestType = DocumentFormTypeCd.FBDS;
        	  when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(null);
            ListFBDSDocumentsResp response =
                listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                    (proNbrs,
                     null,
                     documentRequestType,
                     false,
                     false,
                     txnContext,
                     entityManager);
        	Assert.assertTrue(response!=null);
        } catch (Exception e) {
        	 Assert.assertTrue(e.getMessage()!=null);
        }
    }

    @Test
    public void testListFBDSDocumentsWhenDocumentTypeisEmpty() {
        try {
        	String[] proNbrs = { PRO_NBR_TXT };
            DocumentFormTypeCd documentRequestType = null;
        	  when(shmShipmentSubDAO.findNonArchivedByProNbrs(Mockito.anyList(),Mockito.any())).thenReturn(null);
            ListFBDSDocumentsResp response =
                listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                    (proNbrs,
                     null,
                     documentRequestType,
                     false,
                     false,
                     txnContext,
                     entityManager);
        	Assert.assertTrue(response!=null);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Validation Errors found."));
        }
    }
    
    
    private ListAppointmentNotificationsForShipmentsResp getListShipmentAppointmentNotificationsResp() {
    	ListAppointmentNotificationsForShipmentsResp resp = new ListAppointmentNotificationsForShipmentsResp();
    	DeliveryNotification deliveryNotification  = new DeliveryNotification();
    	deliveryNotification.setCallDateTime(stringToXmlGregorianCalendar("2020-03-11T13:14:15", "yyyy-MM-dd'T'HH:mm:ss"));
    	deliveryNotification.setStatusCd(AppointmentNotificationStatCd.ACTIVE);
    	deliveryNotification.setScheduledDeliveryDate("2020-21-10T13:14:15");
    	deliveryNotification.setShipmentInstId(SHP_INST_ID);
    	AuditInfo auditInfo = new AuditInfo();
    	auditInfo.setUpdatedTimestamp(stringToXmlGregorianCalendar("2020-03-11T13:14:15", "yyyy-MM-dd'T'HH:mm:ss"));
    	deliveryNotification.setAuditInfo(auditInfo);
    	resp.setShipmentAppointmentNotifications(Arrays.asList(deliveryNotification));
    	return resp;
    }
    
   
    private DetermineRestrictedBillToResp getDetermineRestrictedBillToResp() {
    	DetermineRestrictedBillToResp determineRestrictedBillToResp = new DetermineRestrictedBillToResp();
    	determineRestrictedBillToResp.setBillToIsRestrictedInd(false);
    	return determineRestrictedBillToResp;
    }
    private MovementException getMovementException() {
    	MovementException movementException = new MovementException();
    	movementException.setPiecesCount(2l);
    	return movementException;
    }

    private List<ShmShipment> getShipmentData() {
    	List<ShmShipment> shmShipments = new ArrayList<>();
        ShmShipment shmShipment = getShipment();
        shmShipments.add(shmShipment);
        return shmShipments;
    }

    private ShmAdvBydCarr getShmAdvBydCarr() {
    	ShmAdvBydCarr shmAdvBydCarr = new ShmAdvBydCarr();
    	shmAdvBydCarr.setTypCd("1");
    	shmAdvBydCarr.setChgAmt(new BigDecimal(223));
    	return shmAdvBydCarr;
    }
	private ShmShipment getShipment() {
		ShmShipment shmShipment = new ShmShipment();
        shmShipment.setProNbrTxt(PRO_NBR_TXT);
        shmShipment.setShpInstId(123456L);
        shmShipment.setPrcAgrmtId(BigDecimal.ONE);
        shmShipment.setDestTrmnlSicCd("UPO");
        shmShipment.setOrigTrmnlSicCd("UPW");
        shmShipment.setShmAsEntdCusts(getShmAsEntdCust());
        shmShipment.setShmMovements(buildShmMovementsList());
        shmShipment.setShmMiscLineItems(new ArrayList<>());
        shmShipment.setChrgToCd("C");
        shmShipment.setFbdsPrintCnt(new BigDecimal(1));
        shmShipment.setBillClassCd("I");
        shmShipment.setShmAcSvcs(Arrays.asList(getShmAcSvc()));
        shmShipment.setTotPcsCnt(new BigDecimal(2));
        shmShipment.setTotWgtLbs(new BigDecimal(2891));
        shmShipment.setShmCommodities(Arrays.asList(getShmCommodity()));
        shmShipment.setParentInstId(new BigDecimal(495026));
        shmShipment.setHazmatInd("N");
        shmShipment.setDlvryQalfrCd("K");
        shmShipment.setHazmatInd("Y");
        shmShipment.setRtePfxTxt("STHA");
        shmShipment.setRteSfxTxt("26");
        shmShipment.setShmCustomsBonds(Arrays.asList(getShmCustomsBond()));
        shmShipment.setShmAdvBydCarrs(Arrays.asList(getShmAdvBydCarr()));
        shmShipment.setTotChrgAmt(new BigDecimal(888.10));
        
		return shmShipment;
	}
	private ShmCustomsBond getShmCustomsBond() {
		ShmCustomsBond shmCustomsBond= new ShmCustomsBond();
		shmCustomsBond.setBondNbrTxt("Bond Text");
		shmCustomsBond.setCtyTxt("City Txt");
		shmCustomsBond.setStCd("UP");
		
		return shmCustomsBond;
	}
	
	private List<ShmAsEntdCust> getShmAsEntdCust() {
		List<ShmAsEntdCust> shmAsEntdCusts = new ArrayList<>();
		ShmAsEntdCust shmAsEntdCust = new ShmAsEntdCust();
		shmAsEntdCust.setAddrTxt("");
		shmAsEntdCust.setAllShpmtPpdInd("");
		shmAsEntdCust.setTypCd("1");
		shmAsEntdCust.setName1Txt("SMH Text");
		
		ShmAsEntdCust shmAsEntdCust1 = new ShmAsEntdCust();
		shmAsEntdCust1.setAddrTxt("");
		shmAsEntdCust1.setAllShpmtPpdInd("");
		shmAsEntdCust1.setTypCd("2");
		shmAsEntdCust1.setName1Txt("SMH Text");
		shmAsEntdCust1.setBiltoRelCd("B");
		
		ShmAsEntdCust shmAsEntdCust2 = new ShmAsEntdCust();
		shmAsEntdCust2.setAddrTxt("");
		shmAsEntdCust2.setAllShpmtPpdInd("");
		shmAsEntdCust2.setTypCd("3");
		shmAsEntdCust2.setName1Txt("SMH Text");
		shmAsEntdCust2.setCisCustNbr(new BigDecimal(43));
		shmAsEntdCust2.setBiltoRelCd("C");
		shmAsEntdCusts.add(shmAsEntdCust);
		shmAsEntdCusts.add(shmAsEntdCust1);
		shmAsEntdCusts.add(shmAsEntdCust2);
		return shmAsEntdCusts;
	}
	
	private ShmCommodity getShmCommodity() {
		
		ShmCommodity commodity = new ShmCommodity();
		commodity.setAmt(new BigDecimal(0));
		commodity.setArchiveCntlCd("");
		commodity.setAsRatedClassCd("");
		commodity.setChrgToCd("P");
		commodity.setClassTyp("100");
		commodity.setDescTxt("BLUE BERRY PUREE SEEDLESS");
		commodity.setDfltClassSlctInd("");
		commodity.setDmlTmst(new Timestamp(System.currentTimeMillis()));
		commodity.setFrzbleInd("N");
		commodity.setHzMtInd("N");
		commodity.setLstUpdtTmst(new Timestamp(System.currentTimeMillis()));
		commodity.setLstUpdtTranCd("RTS1");
		commodity.setLstUpdtUid("CMBF0251");
		commodity.setMinChrgInd("N");
		commodity.setMixClssCmdyInd("N");
		commodity.setNmfcItmCd("N");
		commodity.setOriglDescTxt("BLUE BERRY PUREE SEEDLESS");
		commodity.setPcsCnt(new BigDecimal(1));
		commodity.setPkgCd("PLT");
		commodity.setPpdPct(new BigDecimal(1));
		commodity.setRdcdWgt(new BigDecimal(2891));
		commodity.setReplLstUpdtTmst(new Timestamp(System.currentTimeMillis()));
		commodity.setRtgQty(new BigDecimal(1));
		commodity.setRtgUom("");
		commodity.setSrceCd("2");
		commodity.setWgtLbs(new BigDecimal(2891));
		return commodity;
		
	}
	private ShmAcSvc getShmAcSvc()
	{
		ShmAcSvc shmAcSvc = new ShmAcSvc();
		shmAcSvc.setAcCd("GUR");
		shmAcSvc.setAcQty(new BigDecimal(1));
		shmAcSvc.setAcUom("6");
		shmAcSvc.setAmt(new BigDecimal(555.00));
		shmAcSvc.setArchiveCntlCd("");
		shmAcSvc.setChrgToCd("C");
		shmAcSvc.setDescTxt("TLF VOLUME SHIPMENT FUEL SURCHARGE");
		shmAcSvc.setDmlTmst(new Timestamp(System.currentTimeMillis()));
		shmAcSvc.setLstUpdtTranCd("RTS1");
      shmAcSvc.setDtlCapxtimestamp(new Timestamp(System.currentTimeMillis()));
      shmAcSvc.setLstUpdtTmst(new Timestamp(System.currentTimeMillis()));
      shmAcSvc.setLstUpdtUid("CMBF2051");
      shmAcSvc.setMinChrgInd("Y");
      shmAcSvc.setPpdPct(new BigDecimal(5));
       return shmAcSvc;
	}
    private List<ShmMovement> buildShmMovementsList() {
        List<ShmMovement> shmMovementList = Lists.newArrayList();
        ShmMovement shmMovement1 = buildShmMovement(2L);
        ShmMovement shmMovement2 = buildShmMovement(1L);
        ShmMovement shmMovement3 = buildShmMovement(3L);

        shmMovementList.add(shmMovement1);
        shmMovementList.add(shmMovement2);
        shmMovementList.add(shmMovement3);
        return shmMovementList;
    }

    private ShmMovement buildShmMovement(long sequenceNumber) {
        ShmMovement shmMovement = new ShmMovement();
        ShmMovementPK shmMovementPK = new ShmMovementPK();
        shmMovementPK.setSeqNbr(sequenceNumber);
        shmMovementPK.setShpInstId(123456L);
        shmMovement.setId(shmMovementPK);
        shmMovement.setShmMovementExcps(buildShmMovementExcpsList());
        shmMovement.setCrteTmst(new Timestamp(new Date().getTime() + sequenceNumber));
        return shmMovement;
    }

    private List<ShmMovementExcp> buildShmMovementExcpsList() {
        List<ShmMovementExcp> shmMovementList = Lists.newArrayList();
        ShmMovementExcp shmMovementExcp1 = buildShmMovementExcp(2L);
        ShmMovementExcp shmMovementExcp2 = buildShmMovementExcp(1L);
        ShmMovementExcp shmMovementExcp3 = buildShmMovementExcp(3L);

        shmMovementList.add(shmMovementExcp1);
        shmMovementList.add(shmMovementExcp2);
        shmMovementList.add(shmMovementExcp3);
        return shmMovementList;
    }

    private ShmMovementExcp buildShmMovementExcp(long sequenceNumber) {
        ShmMovementExcp shmMovementExcp = new ShmMovementExcp();
        ShmMovementExcpPK shmMovementExcpPK = new ShmMovementExcpPK();
        shmMovementExcpPK.setSeqNbr(sequenceNumber);
        shmMovementExcpPK.setShpInstId(123456L);
        shmMovementExcp.setId(shmMovementExcpPK);
        return shmMovementExcp;
    }
    
    private GetLocReferenceDetailsBySicResp getGetLocReferenceDetailsBySicResp() {
    	GetLocReferenceDetailsBySicResp response = new GetLocReferenceDetailsBySicResp();
    	response.setSatelliteParentSicCd("UFV");
    	response.setRegionSicCd("CWA");
    	response.setAreaSicCd("CWA");
    	response.setLinehaulHostSicCd("UFV");
    	response.setLinehaulHostSicNm("SAN FERNANDO VALLEY");
    	response.setSatelliteParentSicNm("SAN FERNANDO VALLEY");
    	LocationReference locationRef = new LocationReference();
    	locationRef.setSltCountryCd("US");
    	response.setLocReference(locationRef);
    	return response;
    }

	
	private GetLocOperationsServiceCenterProfitabilityBySicResp getGetLocOperationsServiceCenterProfitabilityBySicResp() {
		GetLocOperationsServiceCenterProfitabilityBySicResp response = new GetLocOperationsServiceCenterProfitabilityBySicResp();
		LocOperationsSvccProfitability locOperationsSvccProfitability  = new LocOperationsSvccProfitability();
		locOperationsSvccProfitability.setSicCd("XAY");
		locOperationsSvccProfitability.setCityOperationsInd(Boolean.TRUE);
		locOperationsSvccProfitability.setAutoprtPdManifestCd("1");
		locOperationsSvccProfitability.setOutboundAvgLoadedWeightLbs(new Double(13100l));
		locOperationsSvccProfitability.setOutboundManPowerLbs(new Double(7800l));
		locOperationsSvccProfitability.setOutboundPpldLbs(new Double(10000l));
		locOperationsSvccProfitability.setSmart4RolloutDate(stringToXmlGregorianCalendar("2020-03-11T13:14:15", "yyyy-MM-dd'T'HH:mm:ss"));
	
		locOperationsSvccProfitability.setSsrInboundInd(true);
		locOperationsSvccProfitability.setSsrInboundRolloutDate(stringToXmlGregorianCalendar("2020-03-11T13:14:15", "yyyy-MM-dd'T'HH:mm:ss"));
		
		locOperationsSvccProfitability.setAutoptDsrTripCd("1");
		locOperationsSvccProfitability.setMimsOperationsInd(true);
		locOperationsSvccProfitability.setMimsRolloutDate(stringToXmlGregorianCalendar("2020-03-11T13:14:15", "yyyy-MM-dd'T'HH:mm:ss"));

		locOperationsSvccProfitability.setDockOperationsInd(false);
		locOperationsSvccProfitability.setDockOperationsRolloutDate(stringToXmlGregorianCalendar("2020-03-11T13:14:15", "yyyy-MM-dd'T'HH:mm:ss"));

		locOperationsSvccProfitability.setInboundArrInd(false);
		locOperationsSvccProfitability.setInboundArrRolloutDate(stringToXmlGregorianCalendar("2020-03-11T13:14:15", "yyyy-MM-dd'T'HH:mm:ss"));
	
		locOperationsSvccProfitability.setAvgOutboundDensity(new Double(0l));
		locOperationsSvccProfitability.setAvgOutboundPceVolumeCubicFeet(0l);
		locOperationsSvccProfitability.setDmlDateTime(stringToXmlGregorianCalendar("2020-03-11T13:14:15", "yyyy-MM-dd'T'HH:mm:ss"));
		
		response.setLocOperationsServiceCentersProfitability(locOperationsSvccProfitability);
		
		return response;
	}

	
	
 public static XMLGregorianCalendar stringToXmlGregorianCalendar(final String strDate, final String inPattern) {
		XMLGregorianCalendar calendar = null;
		try {
			if (strDate == null)
				return null;

			final GregorianCalendar cal = new GregorianCalendar();
		    cal.setTime(new SimpleDateFormat(inPattern).parse(strDate));
		    calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		} catch (final ParseException e) {
			ExceptionUtils.getStackTrace(e);
		} catch (final DatatypeConfigurationException e) {
			ExceptionUtils.getStackTrace(e);
		}

		return calendar;
	}

}
