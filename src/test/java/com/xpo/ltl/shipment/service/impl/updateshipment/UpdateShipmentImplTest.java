package com.xpo.ltl.shipment.service.impl.updateshipment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmLnhDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.UpdateManRateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

class UpdateShipmentImplTest extends MockParent {

	@InjectMocks
	private UpdateShipmentImpl updateShipmentImpl;

	@Mock
	private UpdateManRateImpl updateManRate;

	@Mock
	private ShmShipmentSubDAO mockShmShipmentSubDAO;

	@Mock
	private ShmEventDelegate mockShmEventDelegate;

	@Mock
	private ShmLnhDimensionSubDAO mockShmLnhDimensionSubDAO;

	@Mock
	private ShipmentAcSvcSubDAO shipmentAcSvcSubDAO;

	@Mock
	private ShipmentAdvBydSubDAO shipmentAdvBydSubDAO;

	@Mock
	private UpdateFactory mockUpdateFactory;

	@Mock
	private ShmMiscLineItemSubDAO mockShmMiscLineItemSubDAO;

	@Mock
	private EntityManager mockEntityManager;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testUpdateShipment() throws ServiceException {
		String pro = "06420172510";
		ShipmentUpdateActionCd shipmentUpdateActionCd = ShipmentUpdateActionCd.MANUAL_RATE;
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});

		List<ShmMiscLineItem> shmMiscLineItems = new ArrayList<>();
		shmMiscLineItems = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItems.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
		shmMiscLineItems = objectMapper.convertValue(shmMiscLineItems, new TypeReference<List<ShmMiscLineItem>>() {
		});

		List<ShmAcSvc> shmAcSvcs = new ArrayList<>();
		shmAcSvcs = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcs.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		shmAcSvcs = objectMapper.convertValue(shmAcSvcs, new TypeReference<List<ShmAcSvc>>() {
		});
		shmShipment.setShmAcSvcs(shmAcSvcs);
		List<ShmAdvBydCarr> advBydCarrs = new ArrayList<>();
		advBydCarrs = (List<ShmAdvBydCarr>) jsonStringToObject(advBydCarrs.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAdvBydCarr"));
		advBydCarrs = objectMapper.convertValue(advBydCarrs, new TypeReference<List<ShmAdvBydCarr>>() {
		});
		shmShipment.setShmAdvBydCarrs(advBydCarrs);
		shmShipment.setShmMiscLineItems(shmMiscLineItems);

		TransactionContext txnContext = new TransactionContext();

		final List<ShipmentDetailCd> shipmentDetailCdsList = getShipmentDetails(ShipmentUpdateActionCd.MANUAL_RATE);
		ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan = ShmShipmentEagerLoadPlan.from(shipmentDetailCdsList, false);

		when(mockUpdateFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(updateManRate);

		Mockito.doNothing().when(updateManRate).update(updateShipmentRqst,
				ShipmentUpdateActionCd.MANUAL_RATE,
				shmShipment,
				shmMiscLineItems,
				mockEntityManager,
				txnContext);

		updateManRate.update(updateShipmentRqst,
				ShipmentUpdateActionCd.MANUAL_RATE,
				shmShipment,
				shmShipment.getShmMiscLineItems(),
				mockEntityManager,
				txnContext);

	}

	private List<ShipmentDetailCd> getShipmentDetails(ShipmentUpdateActionCd shipmentUpdateActionCd) {
		List<ShipmentDetailCd> result = new ArrayList<>();
		result.add(ShipmentDetailCd.COMMODITY);
		result.add(ShipmentDetailCd.ACCESSORIAL);
		result.add(ShipmentDetailCd.ADVANCE_BEYOND);
		result.add(ShipmentDetailCd.REMARKS);
		result.add(ShipmentDetailCd.MISC_LINE_ITEM);
		if (!ShipmentUpdateActionCd.MANUAL_RATE.equals(shipmentUpdateActionCd)) {
			result.add(ShipmentDetailCd.CUSTOMS_BOND);

		}

		return result;
	}
}