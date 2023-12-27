package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAcSvc;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAdvBydCarr;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCommodity;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractMiscLineItems;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractRemark;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.AcSvcUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.ShipmentUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAcSvcFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAdvBydCarrFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateCommodityFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateMiscLineItemFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateRemarkFactory;
import org.apache.commons.collections.ArrayStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateManRateImplTest extends MockParent {

	private final LoadValues<AccessorialService, ShmAcSvc> acSvcLoadValues = mock(LoadValues.class);
	private final LoadValues<UpdateShipmentRqst, ShmShipment> shipmentLoadValues = mock(LoadValues.class);
	@InjectMocks
	ManRateLoadValuesFactory manRateLoadValuesFactory = mock(ManRateLoadValuesFactory.class);
	@Mock
	private EntityManager entityManager;
	@Mock
	private ShipmentRemarkSubDAO shipmentRemarkSubDAO;
	@Mock
	private ShipmentAcSvcSubDAO shipmentAcSvcSubDAO;
	@Mock
	private ShmCommoditySubDAO shmCommoditySubDAO;
	@Mock
	private ShipmentAdvBydSubDAO shipmentAdvBydSubDAO;
	@Mock
	private ShmEventLogSubDAO shmEventLogSubDAO;
	@Mock
	private ShmShipmentSubDAO shmShipmentSubDAO;
	@Mock
	private ShmEventDelegate shmEventDelegate;
	@Mock
	protected AppContext appContext;
	private List<ShmRemark> shmRemarks = new ArrayList();
	private List<ShmCommodity> shmCommodity = new ArrayList();
	private List<ShmAcSvc> shmAcSvcs = new ArrayList();
	private List<ShmAdvBydCarr> advBydCarrs = new ArrayList();
	private List<ShmMiscLineItem> shmMiscLineItems = new ArrayStack();
	@Mock
	private TransactionContext txnContext;
	@InjectMocks
	private MiscLineItemsUpdateManRateImpl miscLineItemsUpdateManRate= mock(MiscLineItemsUpdateManRateImpl.class,
			Answers.CALLS_REAL_METHODS.get());

	@InjectMocks
	private ManRateCommodityUpdImpl commodityUpdate= mock(ManRateCommodityUpdImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private ManRateAdvBydCarrUpdateImpl manRateAdvBydCarrUpdate= mock(ManRateAdvBydCarrUpdateImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private ShipmentUpdateCommonImpl updateShipment = mock(ShipmentUpdateCommonImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private ManRateRemarkUpdateImpl manRateRemarkUpdate= mock(ManRateRemarkUpdateImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private ManRateLoadAccessorialValuesImpl mockImplementation = mock(ManRateLoadAccessorialValuesImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private AcSvcUpdateCommonImpl acSvcUpdateCommon;
	@Mock
	private AbstractUpdate abstractUpdate = mock(AbstractUpdate.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private AbstractShipment abstractShipment = mock(AbstractShipment.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private AbstractAcSvc abstractAcSvc = mock(AbstractAcSvc.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private AbstractRemark abstractRemark = mock(AbstractRemark.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private AbstractMiscLineItems abstractMiscLineItems = mock(AbstractMiscLineItems.class,
			Answers.CALLS_REAL_METHODS.get());
	@Mock
	private AbstractAdvBydCarr abstractAdvBydCarr = mock(AbstractAdvBydCarr.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private AbstractCommodity abstractCommodity = mock(AbstractCommodity.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private UpdateRemarkFactory updateRemarkFactory = mock(UpdateRemarkFactory.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private UpdateAdvBydCarrFactory updateAdvBydCarrFactory = mock(UpdateAdvBydCarrFactory.class,
			Answers.CALLS_REAL_METHODS.get());
	@Mock
	private UpdateAcSvcFactory updateAcSvcFactory = mock(UpdateAcSvcFactory.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private UpdateCommodityFactory updateCommodityFactory = mock(UpdateCommodityFactory.class,
			Answers.CALLS_REAL_METHODS.get());
	@Mock
	private UpdateMiscLineItemFactory updateMiscLineItemFactory = mock(UpdateMiscLineItemFactory.class,
			Answers.CALLS_REAL_METHODS.get());
	@Mock
	private LoadValFactory loadValFactory = mock(LoadValFactory.class, Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private UpdateManRateImpl updateManRate;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		final User user = new User();
		user.setEmployeeId("EmployeeId");

		txnContext.setUser(user);

	}

	@Test
	void update() {

		String pro = "06420172510";

		try {
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

			shmMiscLineItems = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItems.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
			shmMiscLineItems = objectMapper.convertValue(shmMiscLineItems, new TypeReference<List<ShmMiscLineItem>>() {
			});

			shmRemarks = (List<ShmRemark>) jsonStringToObject(shmRemarks.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmRemark"));
			shmRemarks = objectMapper.convertValue(shmRemarks, new TypeReference<List<ShmRemark>>() {
			});

			shmCommodity = (List<ShmCommodity>) jsonStringToObject(shmCommodity.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmCommodity"));
			shmCommodity = objectMapper.convertValue(shmCommodity, new TypeReference<List<ShmCommodity>>() {
			});

			shmAcSvcs = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcs.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
			shmAcSvcs = objectMapper.convertValue(shmAcSvcs, new TypeReference<List<ShmAcSvc>>() {
			});

			advBydCarrs = (List<ShmAdvBydCarr>) jsonStringToObject(advBydCarrs.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmAdvBydCarr"));
			advBydCarrs = objectMapper.convertValue(advBydCarrs, new TypeReference<List<ShmAdvBydCarr>>() {
			});

			shmShipment.setShmRemarks(shmRemarks);
			shmShipment.setShmCommodities(shmCommodity);
			shmShipment.setShmAcSvcs(shmAcSvcs);
			shmShipment.setShmAdvBydCarrs(advBydCarrs);
			when(updateRemarkFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateRemarkUpdate);
			when(updateMiscLineItemFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					miscLineItemsUpdateManRate);

			when(updateAcSvcFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					acSvcUpdateCommon);
			when(updateAdvBydCarrFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateAdvBydCarrUpdate);
//			when(updateCommodityFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
//					manRateCommodityUpd);
			when(loadValFactory.getFactoryImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateLoadValuesFactory);
			//			when(correctionsLoadValuesFactory.getFactoryImplementation(ShmAcSvc.class)).thenReturn(mockAccessorialValues);
			doReturn(acSvcLoadValues).when(manRateLoadValuesFactory).getFactoryImplementation(ShmAcSvc.class);
			doReturn(shipmentLoadValues).when(manRateLoadValuesFactory).getFactoryImplementation(ShmShipment.class);

			updateManRate.update(updateShipmentRqst,
					ShipmentUpdateActionCd.MANUAL_RATE,
					shmShipment,
					shmMiscLineItems,
					entityManager,
					txnContext);

		} catch (Exception e) {

		}

	}

	@Test
	void updateAdvByd() {

		String pro = "06340056883";

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
					this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
			updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
			});

			updateShipmentRqst
					.getAdvanceBeyondCarriers()
					.forEach(advanceBeyondCarrier -> advanceBeyondCarrier.setChargeAmount(150D));
			ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
					this.getJsonFromProperty(pro + ".jsonShmShipment"));
			shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
			});

			shmMiscLineItems = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItems.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
			shmMiscLineItems = objectMapper.convertValue(shmMiscLineItems, new TypeReference<List<ShmMiscLineItem>>() {
			});

			shmRemarks = (List<ShmRemark>) jsonStringToObject(shmRemarks.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmRemark"));
			shmRemarks = objectMapper.convertValue(shmRemarks, new TypeReference<List<ShmRemark>>() {
			});

			shmCommodity = (List<ShmCommodity>) jsonStringToObject(shmCommodity.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmCommodity"));
			shmCommodity = objectMapper.convertValue(shmCommodity, new TypeReference<List<ShmCommodity>>() {
			});

			shmAcSvcs = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcs.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
			shmAcSvcs = objectMapper.convertValue(shmAcSvcs, new TypeReference<List<ShmAcSvc>>() {
			});

			advBydCarrs = objectMapper.convertValue(advBydCarrs, new TypeReference<List<ShmAdvBydCarr>>() {
			});
			advBydCarrs.forEach(shmAdvBydCarr -> {
				shmAdvBydCarr.setChgAmt(BigDecimal.TEN);
			});
			shmShipment.setShmRemarks(shmRemarks);
			shmShipment.setShmCommodities(shmCommodity);
			shmShipment.setShmAcSvcs(shmAcSvcs);
			shmShipment.setShmAdvBydCarrs(advBydCarrs);
			when(updateRemarkFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateRemarkUpdate);
			when(updateMiscLineItemFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					miscLineItemsUpdateManRate);

			when(updateAcSvcFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					acSvcUpdateCommon);
			when(updateAdvBydCarrFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateAdvBydCarrUpdate);
//			when(updateCommodityFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
//					manRateCommodityUpd);
			when(loadValFactory.getFactoryImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateLoadValuesFactory);
			//			when(correctionsLoadValuesFactory.getFactoryImplementation(ShmAcSvc.class)).thenReturn(mockAccessorialValues);
			doReturn(acSvcLoadValues).when(manRateLoadValuesFactory).getFactoryImplementation(ShmAcSvc.class);
			doReturn(shipmentLoadValues).when(manRateLoadValuesFactory).getFactoryImplementation(ShmShipment.class);

			updateManRate.update(updateShipmentRqst,
					ShipmentUpdateActionCd.MANUAL_RATE,
					shmShipment,
					shmMiscLineItems,
					entityManager,
					txnContext);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	void updateRemarks() {

		String pro = "06340056883";

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
					this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
			updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
			});

			updateShipmentRqst.getShipmentRemarks().forEach(remark -> remark.setRemark(remark.getRemark() + " Test"));
			ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
					this.getJsonFromProperty(pro + ".jsonShmShipment"));
			shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
			});

			shmMiscLineItems = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItems.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
			shmMiscLineItems = objectMapper.convertValue(shmMiscLineItems, new TypeReference<List<ShmMiscLineItem>>() {
			});

			shmRemarks = (List<ShmRemark>) jsonStringToObject(shmRemarks.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmRemark"));
			shmRemarks = objectMapper.convertValue(shmRemarks, new TypeReference<List<ShmRemark>>() {
			});

			shmCommodity = (List<ShmCommodity>) jsonStringToObject(shmCommodity.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmCommodity"));
			shmCommodity = objectMapper.convertValue(shmCommodity, new TypeReference<List<ShmCommodity>>() {
			});

			shmAcSvcs = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcs.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
			shmAcSvcs = objectMapper.convertValue(shmAcSvcs, new TypeReference<List<ShmAcSvc>>() {
			});

			advBydCarrs = objectMapper.convertValue(advBydCarrs, new TypeReference<List<ShmAdvBydCarr>>() {
			});
			advBydCarrs.forEach(shmAdvBydCarr -> {
				shmAdvBydCarr.setChgAmt(BigDecimal.TEN);
			});
			shmShipment.setShmRemarks(shmRemarks);
			shmShipment.setShmCommodities(shmCommodity);
			shmShipment.setShmAcSvcs(shmAcSvcs);
			shmShipment.setShmAdvBydCarrs(advBydCarrs);

			when(updateRemarkFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateRemarkUpdate);
			when(updateMiscLineItemFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					miscLineItemsUpdateManRate);

			when(updateAcSvcFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					acSvcUpdateCommon);
			when(updateAdvBydCarrFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateAdvBydCarrUpdate);

			when(loadValFactory.getFactoryImplementation(ShipmentUpdateActionCd.MANUAL_RATE)).thenReturn(
					manRateLoadValuesFactory);
			//			when(correctionsLoadValuesFactory.getFactoryImplementation(ShmAcSvc.class)).thenReturn(mockAccessorialValues);
			doReturn(acSvcLoadValues).when(manRateLoadValuesFactory).getFactoryImplementation(ShmAcSvc.class);
			doReturn(shipmentLoadValues).when(manRateLoadValuesFactory).getFactoryImplementation(ShmShipment.class);

			updateManRate.update(updateShipmentRqst,
					ShipmentUpdateActionCd.MANUAL_RATE,
					shmShipment,
					shmMiscLineItems,
					entityManager,
					txnContext);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}