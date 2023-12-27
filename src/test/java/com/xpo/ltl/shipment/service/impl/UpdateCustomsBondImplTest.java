package com.xpo.ltl.shipment.service.impl;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBondPK;
import com.xpo.ltl.api.shipment.v2.*;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmCustomsBondSubDAO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

public class UpdateCustomsBondImplTest {
	
	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@InjectMocks
    private UpdateCustomsBondImpl updateCustomsBondImpl;
	
	@InjectMocks
	private ShmCustomsBond shmCustomsBond;
	
	@Mock
	private ShmCustomsBondSubDAO shmCustomsBondSubDAO;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

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
	public void testUpsert_RequestRequired() throws Exception {
		try {
			updateCustomsBondImpl.upsertCustomsBond(null, txnContext, entityManager);
			Assert.fail("Expected an exception.");
		} catch (final Exception e) {
			Assert.assertEquals("The request is required.", e.getMessage());
		}
	}
	
	@Test
	public void testUpsert_CustomsBond_Required() throws Exception {
		try {
			final UpsertCustomsBondRqst request = new UpsertCustomsBondRqst();
			request.setCustomsBond(null);
			updateCustomsBondImpl.upsertCustomsBond(request, txnContext, entityManager);
			Assert.fail("Expected an exception.");
		} catch (final Exception e) {
			Assert.assertEquals("The Customs Bond entity is required.", e.getMessage());
		}
	}
	
	@Test
	public void testUpsert_ShipmentID_Required() throws Exception {
		try {
			final UpsertCustomsBondRqst request = new UpsertCustomsBondRqst();
			CustomsBond bond = new CustomsBond();
			bond.setShipmentInstId(null);
			request.setCustomsBond(bond);
			updateCustomsBondImpl.upsertCustomsBond(request, txnContext, entityManager);
			Assert.fail("Expected an exception.");
		} catch (final Exception e) {
			Assert.assertEquals("ShipmentInstId is required.", e.getMessage());
		}
	}
	
	
	@Test
	public void testUpsert_insertCustomsBondSuccess() throws Exception {
			final UpsertCustomsBondRqst request = new UpsertCustomsBondRqst();
			CustomsBond bond = new CustomsBond();
			bond.setShipmentInstId(1234L);
			bond.setSequenceNbr(BasicTransformer.toBigInteger(1));
			bond.setCity("DETROIT");
			bond.setStateCd("MI");
			bond.setBondNbr("IT 42069");
			bond.setBondCreateDate("10/12/1998");
			bond.setShipmentDirectionCd(ShipmentDirectionCd.NORTHBOUND);
			bond.setBondedSicCd("UPO");
			bond.setBondTypeCd(BondTypeCd.PLANNED);
			bond.setBondStatusCd(BondStatusCd.UNCLEARED);
			bond.setListActionCd(ActionCd.ADD);
			bond.setCrcAuditCd(CrcAuditCd.COMPLETED);
			request.setCustomsBond(bond);
			List<Long> shpInstIdList = Arrays.asList(1234L);
			when(shmCustomsBondSubDAO.findByShpInstIds(shpInstIdList, entityManager))
            		.thenReturn(null);

			Assertions.assertDoesNotThrow(() -> 
					updateCustomsBondImpl.upsertCustomsBond(request, txnContext, entityManager));

	}
	
	@Test
	public void testUpsert_updateCustomsBondSuccess() throws Exception {
			final UpsertCustomsBondRqst request = new UpsertCustomsBondRqst();
			CustomsBond bond = new CustomsBond();
			bond.setShipmentInstId(1234L);
			bond.setCity("DETROIT");
			bond.setStateCd("MI");
			bond.setBondNbr("IT 42069");
			bond.setSequenceNbr(BasicTransformer.toBigInteger(1));
			bond.setBondTypeCd(BondTypeCd.PLANNED);
			bond.setBondStatusCd(BondStatusCd.UNCLEARED);
			bond.setListActionCd(ActionCd.UPDATE);
			request.setCustomsBond(bond);
			ShmCustomsBond bondReturned = new ShmCustomsBond();
			ShmCustomsBondPK pk = new ShmCustomsBondPK();
			pk.setShpInstId(1234L);
			pk.setSeqNbr(1);
			bondReturned.setId(pk);
			bondReturned.setCtyTxt("LAREDO");
			bondReturned.setStCd("TX");
			bondReturned.setBondNbrTxt("IT 42069");
			bondReturned.setBondTypeCd("Unplanned");
			bondReturned.setBondStatusCd("Uncleared");
			bondReturned.setBondCreateDt(new SimpleDateFormat("MM/dd/yyyy").parse("10/12/1998"));

			when(shmCustomsBondSubDAO.findById(pk, entityManager))
            		.thenReturn(shmCustomsBond);
			
			Assertions.assertDoesNotThrow(() -> 
					updateCustomsBondImpl.upsertCustomsBond(request, txnContext, entityManager));

	}

	@Test
	public void testUpsertCustomsBondAddDuplicatedCustomsBond() throws Exception {

		Long shipmentInstId = 1234L;

		final UpsertCustomsBondRqst upsertCustomsBondRqst = new UpsertCustomsBondRqst();

		CustomsBond customsBond = new CustomsBond();
		customsBond.setListActionCd(ActionCd.ADD);
		customsBond.setShipmentInstId(shipmentInstId);
		customsBond.setCity("DETROIT");
		customsBond.setStateCd("ID");
		customsBond.setBondNbr("IT 42069");
		customsBond.setBondCreateDate("10/12/1998");
		customsBond.setShipmentDirectionCd(ShipmentDirectionCd.NORTHBOUND);
		customsBond.setBondedSicCd("UPO");
		customsBond.setBondTypeCd(BondTypeCd.PLANNED);
		customsBond.setBondStatusCd(BondStatusCd.UNCLEARED);
		customsBond.setBondValueAmount(Double.valueOf(0));
		customsBond.setCrcAuditCd(CrcAuditCd.COMPLETED);

		upsertCustomsBondRqst.setCustomsBond(customsBond);

		List<Long> shpInstIdList = Collections.singletonList(shipmentInstId);

		ShmCustomsBond shmCustomsBond = new ShmCustomsBond();
		List<ShmCustomsBond> shmCustomsBonds = Collections.singletonList(shmCustomsBond);

		when(shmCustomsBondSubDAO.findByShpInstIds(shpInstIdList, entityManager))
				.thenReturn(shmCustomsBonds);

		try {

			updateCustomsBondImpl.upsertCustomsBond(upsertCustomsBondRqst, txnContext, entityManager);
			
			Assert.fail("Expected an exception.");

		} catch (final ServiceException e) {

			Assert.assertEquals("Bond can't be created as a bond already exists on this shipment.", e.getFault().getMoreInfo().get(0).getMessage());

		} catch (final Exception e) {

			Assert.fail("Expected a ValidationException.");

		}

	}
	
}
