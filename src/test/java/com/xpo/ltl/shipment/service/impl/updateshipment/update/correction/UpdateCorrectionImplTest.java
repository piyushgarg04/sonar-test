package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.TimeDateCritical;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentCustomsSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentTdcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAcSvc;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAdvBydCarr;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCommodity;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCustomsBond;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractMiscLineItems;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractRemark;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractTimeDateCritical;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.AcSvcUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.CommodityCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.MiscLineItemsUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.ShipmentUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAcSvcFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateAdvBydCarrFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateCommodityFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateCustomsBondFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateMiscLineItemFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateRemarkFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateCorrectionImplTest extends MockParent {

	@InjectMocks
	CorrectionsLoadValuesFactory correctionsLoadValuesFactory = mock(CorrectionsLoadValuesFactory.class);
	LoadValues<AccessorialService, ShmAcSvc> acSvcLoadValues = mock(LoadValues.class);
	LoadValues<UpdateShipmentRqst, ShmShipment> shipmentLoadValues = mock(LoadValues.class);
	LoadValues<MiscLineItem, ShmMiscLineItem> shmMiscLineItemLoadValues  = mock(LoadValues.class);
	LoadValues<Commodity, ShmCommodity> commodityLoadValues =  mock(LoadValues.class);

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
	private ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;
	@Mock
	private ShmMiscLineItemSubDAO shmMiscLineItemSubDAO;
	@Mock
	private ShipmentCustomsSubDAO shipmentCustomsSubDAO;
	@Mock
	private ShipmentTdcSubDAO shipmentTdcSubDAO;

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
	private List<ShmCustomsBond> shmCustomsBondOriginal = new ArrayList();
	private List<ShmAsEntdCust> shmAsEntdCusts = new ArrayList();
	private ShmTmDtCritical shmTmDtCritical = new ShmTmDtCritical();
	private List<ShmMiscLineItem> shmMiscLineItems = new ArrayList();
	@Mock
	private TransactionContext txnContext;
	@InjectMocks
	private MiscLineItemsUpdateCommonImpl miscLineItemsUpdateCorr;
	@InjectMocks
	private CommodityCommonImpl correctionCommodityImpl;
	@InjectMocks
	private CorrectionAdvBydCarrUpdateImpl correctionAdvBydCarrUpdateImpl;
	@InjectMocks
	private ShipmentUpdateCommonImpl updateShipment = mock(ShipmentUpdateCommonImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private CorrectionRemarkUpdateImpl correctionRemarkUpdate;
	@InjectMocks
	private CorrectionCustomsBondUpdateImpl correctionCustomsBondUpdate;
	@InjectMocks
	private CorrectionTimeDateCriticalUpdateImpl timeDateCriticalUpdateCommom = mock(
			CorrectionTimeDateCriticalUpdateImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private CorrectionAsMatchedPartyUpdateImpl correctionAsMatchedPartyUpdate = mock(CorrectionAsMatchedPartyUpdateImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private CorrectionLoadAccessorialValuesImpl mockImplementation = mock(CorrectionLoadAccessorialValuesImpl.class,
			Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private AcSvcUpdateCommonImpl acSvcUpdateCommon;
	@Mock
	private SimpleDateFormat mockDateFormat = mock(SimpleDateFormat.class);
	@Mock
	private AbstractUpdate abstractUpdate = mock(AbstractUpdate.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private AbstractShipment abstractShipment = mock(AbstractShipment.class, Answers.CALLS_REAL_METHODS.get());
	@Mock
	private AbstractAcSvc abstractAcSvc = mock(AbstractAcSvc.class, Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private AbstractTimeDateCritical abstractTimeDateCritical = mock(AbstractTimeDateCritical.class,
			Answers.CALLS_REAL_METHODS.get());
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
	private AbstractCustomsBond abstractCustomsBond = mock(AbstractCustomsBond.class, Answers.CALLS_REAL_METHODS.get());
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
	private UpdateCustomsBondFactory updateCustomsBondFactory = mock(UpdateCustomsBondFactory.class,
			Answers.CALLS_REAL_METHODS.get());
	@Mock
	private LoadValFactory loadValFactory = mock(LoadValFactory.class, Answers.CALLS_REAL_METHODS.get());
	@InjectMocks
	private UpdateCorrectionImpl updateCorrection;

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

			shmCustomsBondOriginal = (List<ShmCustomsBond>) jsonStringToObject(shmCustomsBondOriginal.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmCustomsBond"));
			shmCustomsBondOriginal = objectMapper.convertValue(shmCustomsBondOriginal,
					new TypeReference<List<ShmCustomsBond>>() {
					});

			shmAsEntdCusts = (List<ShmAsEntdCust>) jsonStringToObject(shmAsEntdCusts.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmAsEntdCust"));
			shmAsEntdCusts = objectMapper.convertValue(shmAsEntdCusts, new TypeReference<List<ShmAsEntdCust>>() {
			});

			shmTmDtCritical = (ShmTmDtCritical) jsonStringToObject(shmTmDtCritical.getClass(),
					this.getJsonFromProperty(pro + ".jsonShmTmDtCritical"));
			shmTmDtCritical = objectMapper.convertValue(shmTmDtCritical, new TypeReference<ShmTmDtCritical>() {
			});

			shmShipment.setShmRemarks(shmRemarks);
			shmShipment.setShmCommodities(shmCommodity);
			shmShipment.setShmAcSvcs(shmAcSvcs);
			shmShipment.setShmAdvBydCarrs(advBydCarrs);
			when(shipmentCustomsSubDAO.listCustomsBondByShipmentIdList(Collections.singletonList(shmShipment.getShpInstId()),
					entityManager)).thenReturn(shmCustomsBondOriginal);
			when(shipmentAsEnteredCustomerDAO.findByShpInstIds(Collections.singletonList(shmShipment.getShpInstId()),
					entityManager)).thenReturn(shmAsEntdCusts);
			when(shipmentTdcSubDAO.findById(shmShipment.getShpInstId(), entityManager)).thenReturn(shmTmDtCritical);
			when(updateRemarkFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
					correctionRemarkUpdate);
			when(updateMiscLineItemFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
					miscLineItemsUpdateCorr);
			when(updateCustomsBondFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
					correctionCustomsBondUpdate);
			when(updateAcSvcFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
					acSvcUpdateCommon);
			when(updateAdvBydCarrFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
					correctionAdvBydCarrUpdateImpl);
			when(updateCommodityFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
					correctionCommodityImpl);
			when(loadValFactory.getFactoryImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
					correctionsLoadValuesFactory);
			doReturn(shmMiscLineItemLoadValues)
					.when(correctionsLoadValuesFactory)
					.getFactoryImplementation(ShmMiscLineItem.class);
			doReturn(commodityLoadValues)
					.when(correctionsLoadValuesFactory)
					.getFactoryImplementation(ShmCommodity.class);


			doNothing().when(shmMiscLineItemSubDAO).persist(any(Collection.class), any());
//			doNothing().when(shmMiscLineItemSubDAO).createDB2ShmMiscLineItem(any(), any());
			doNothing().when(shmMiscLineItemSubDAO).persist(any(ShmMiscLineItem.class), any());
//			doNothing().when(shmMiscLineItemSubDAO).createDB2ShmMiscLineItem(any(ShmMiscLineItem.class), any());
			doNothing().when(shmMiscLineItemSubDAO).updateDB2ShmMiscLineItem(any(ShmMiscLineItem.class), any(), any(), any());

			doNothing().when(shmMiscLineItemSubDAO).remove(any(Collection.class), any());
			doNothing().when(shmMiscLineItemSubDAO).deleteDB2ShmMiscLineItem(any(), any(), any(), any());

			doReturn(acSvcLoadValues).when(correctionsLoadValuesFactory).getFactoryImplementation(ShmAcSvc.class);
			doReturn(shipmentLoadValues).when(correctionsLoadValuesFactory).getFactoryImplementation(ShmShipment.class);

			SimpleDateFormat sdfDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			when(mockDateFormat.parse(updateShipmentRqst
					.getTimeDateCritical()
					.getTdcDate1())).thenReturn(sdfDateFormat.parse(updateShipmentRqst
					.getTimeDateCritical()
					.getTdcDate1()));
			when(mockDateFormat.parse(updateShipmentRqst
					.getTimeDateCritical()
					.getTdcDate2())).thenReturn(sdfDateFormat.parse(updateShipmentRqst
					.getTimeDateCritical()
					.getTdcDate2()));

			updateCorrection.update(updateShipmentRqst,
					ShipmentUpdateActionCd.CORRECTION,
					shmShipment,
					shmMiscLineItems,
					entityManager,
					txnContext);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	void testUpdate() {

		List<String> pros = Arrays.asList(getJsonFromProperty("correctionsPros").split(","));
		try {
			pros.forEach(pro -> {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
						this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
				updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst,
						new TypeReference<UpdateShipmentRqst>() {
						});

				ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
						this.getJsonFromProperty(pro + ".jsonShmShipment"));
				shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
				});

				shmMiscLineItems = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItems.getClass(),
						this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
				shmMiscLineItems = objectMapper.convertValue(shmMiscLineItems,
						new TypeReference<List<ShmMiscLineItem>>() {
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

				shmCustomsBondOriginal = (List<ShmCustomsBond>) jsonStringToObject(shmCustomsBondOriginal.getClass(),
						this.getJsonFromProperty(pro + ".jsonShmCustomsBond"));
				shmCustomsBondOriginal = objectMapper.convertValue(shmCustomsBondOriginal,
						new TypeReference<List<ShmCustomsBond>>() {
						});

				shmAsEntdCusts = (List<ShmAsEntdCust>) jsonStringToObject(shmAsEntdCusts.getClass(),
						this.getJsonFromProperty(pro + ".jsonShmAsEntdCust"));
				shmAsEntdCusts = objectMapper.convertValue(shmAsEntdCusts, new TypeReference<List<ShmAsEntdCust>>() {
				});

				try {
					shmTmDtCritical = (ShmTmDtCritical) jsonStringToObject(shmTmDtCritical.getClass(),
							this.getJsonFromProperty(pro + ".jsonShmTmDtCritical"));
					shmTmDtCritical = objectMapper.convertValue(shmTmDtCritical, new TypeReference<ShmTmDtCritical>() {
					});
				} catch (Exception e) {

				}

				shmShipment.setShmRemarks(shmRemarks);
				shmShipment.setShmCommodities(shmCommodity);
				shmShipment.setShmAcSvcs(shmAcSvcs);
				shmShipment.setShmAdvBydCarrs(advBydCarrs);
				when(shipmentCustomsSubDAO.listCustomsBondByShipmentIdList(Collections.singletonList(shmShipment.getShpInstId()),
						entityManager)).thenReturn(shmCustomsBondOriginal);
				when(shipmentAsEnteredCustomerDAO.findByShpInstIds(Collections.singletonList(shmShipment.getShpInstId()),
						entityManager)).thenReturn(shmAsEntdCusts);
				when(shipmentTdcSubDAO.findById(shmShipment.getShpInstId(), entityManager)).thenReturn(shmTmDtCritical);
				when(updateRemarkFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
						correctionRemarkUpdate);
				when(updateMiscLineItemFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
						miscLineItemsUpdateCorr);
				when(updateCustomsBondFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
						correctionCustomsBondUpdate);
				when(updateAcSvcFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
						acSvcUpdateCommon);
				when(updateAdvBydCarrFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
						correctionAdvBydCarrUpdateImpl);
				when(updateCommodityFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
						correctionCommodityImpl);
				when(loadValFactory.getFactoryImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
						correctionsLoadValuesFactory);
				//			when(correctionsLoadValuesFactory.getFactoryImplementation(ShmAcSvc.class)).thenReturn(mockAccessorialValues);
				doReturn(acSvcLoadValues).when(correctionsLoadValuesFactory).getFactoryImplementation(ShmAcSvc.class);
				doReturn(shipmentLoadValues)
						.when(correctionsLoadValuesFactory)
						.getFactoryImplementation(ShmShipment.class);
				doReturn(shmMiscLineItemLoadValues)
						.when(correctionsLoadValuesFactory)
						.getFactoryImplementation(ShmMiscLineItem.class);
				doReturn(commodityLoadValues)
						.when(correctionsLoadValuesFactory)
						.getFactoryImplementation(ShmCommodity.class);
				SimpleDateFormat sdfDateFormat = new SimpleDateFormat("yyyy-MM-dd");
				try {
					when(mockDateFormat.parse(updateShipmentRqst.getTimeDateCritical().getTdcDate1())).thenReturn(
							sdfDateFormat.parse(updateShipmentRqst.getTimeDateCritical().getTdcDate1()));
				} catch (Exception e) {

				}
				try {
					when(mockDateFormat.parse(updateShipmentRqst.getTimeDateCritical().getTdcDate2())).thenReturn(
							sdfDateFormat.parse(updateShipmentRqst.getTimeDateCritical().getTdcDate2()));
				} catch (Exception e) {

				}

				try {
					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);

					if (Objects.isNull(updateShipmentRqst.getTimeDateCritical())) {
						TimeDateCritical timeDateCritical = new TimeDateCritical();
						updateShipmentRqst.setTimeDateCritical(timeDateCritical);
						updateShipmentRqst.getTimeDateCritical().setTdcDateTypeCd("ON");
						updateShipmentRqst.getTimeDateCritical().setTdcDate2(null);
						updateShipmentRqst.getTimeDateCritical().setTdcTimeTypeCd("ON");
					}

					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);
					updateShipmentRqst.getTimeDateCritical().setTdcDate1("2023-12-12");
					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);
					when(shipmentTdcSubDAO.findById(shmShipment.getShpInstId(), entityManager)).thenReturn(null);
					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);
					updateShipmentRqst.getTimeDateCritical().setTdcDateTypeCd("RNG");
					updateShipmentRqst.getTimeDateCritical().setTdcTimeTypeCd("RNG");
					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);
					updateShipmentRqst.getTimeDateCritical().setTdcTimeTypeCd("BEF");
					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);

					updateShipmentRqst.getTimeDateCritical().setTdcDateTypeCd("ON");
					updateShipmentRqst.getTimeDateCritical().setTdcTimeTypeCd("AFT");
					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);
					updateShipmentRqst.getTimeDateCritical().setTdcDateTypeCd("RNG");
					updateShipmentRqst.getTimeDateCritical().setTdcTimeTypeCd("AFT");
					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);

					updateShipmentRqst.setTimeDateCritical(null);
					when(shipmentTdcSubDAO.findById(shmShipment.getShpInstId(), entityManager)).thenReturn(shmTmDtCritical);
					updateCorrection.update(updateShipmentRqst,
							ShipmentUpdateActionCd.CORRECTION,
							shmShipment,
							shmMiscLineItems,
							entityManager,
							txnContext);



				} catch (ServiceException e) {
					System.out.println("ERROR: " + pro);
				}

			});

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}