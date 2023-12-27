package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ChargeToCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MiscLineItemCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentCreditStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.ChargeToCd;
import com.xpo.ltl.api.shipment.v2.CollectMoneyAtDeliveryCd;
import com.xpo.ltl.api.shipment.v2.DeliveryCollectionInstruction;
import com.xpo.ltl.api.shipment.v2.ListDeliveryCollectionInstructionForShipmentsResp;
import com.xpo.ltl.api.shipment.v2.ListDeliveryCollectionInstructionForShipmentsRqst;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.api.shipment.v2.ShipmentCreditStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ListDeliveryCollectionInstructionForShipmentsImpl {

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject
	private AppContext appContext;
	
	
	public ListDeliveryCollectionInstructionForShipmentsResp listDeliveryCollectionInstructionForShipments(
		final ListDeliveryCollectionInstructionForShipmentsRqst listDeliveryCollectionInstructionForShipmentsRqst,
		final TransactionContext txnContext,
		final EntityManager entityManager
	) throws ServiceException {
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		checkNotNull(listDeliveryCollectionInstructionForShipmentsRqst.getShipmentIds(), "ShipmentIds are required");

		List<ShmShipment> shipments = Lists.newArrayList();
//		final List<ShipmentDetailCd> shipmentDetailCds = Optional.ofNullable(listShipmentsRqst.getShipmentDetailCd()).orElse(new ArrayList<>());

		List<Long> shipmentInstIds = listDeliveryCollectionInstructionForShipmentsRqst.getShipmentIds().stream()
				.filter(shipmentId -> !StringUtils.isBlank(shipmentId.getShipmentInstId()))
				.map(shipmentId -> new Long(shipmentId.getShipmentInstId())).collect(Collectors.toList());


		final List<String> proNumbers = listDeliveryCollectionInstructionForShipmentsRqst.getShipmentIds()
			.stream()
			.filter(shipmentId -> !StringUtils.isBlank(shipmentId.getProNumber()))
			.map(shipmentId -> shipmentId.getProNumber())
			.collect(Collectors.toList());

        ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan =
            new ShmShipmentEagerLoadPlan();
        shmShipmentEagerLoadPlan.setShmAcSvcs(true);
        shmShipmentEagerLoadPlan.setShmMiscLineItems(true);
        shmShipmentEagerLoadPlan.setShmAsEntdCusts(true);

		validateRequest(proNumbers, shipmentInstIds, txnContext);

		if (CollectionUtils.isNotEmpty(shipmentInstIds)) {
			for (List<Long> shipmentInstIdsPartition : Lists.partition(shipmentInstIds, appContext.getMaxCountForInClause())) {
                shipments.addAll
                    (shmShipmentSubDAO.listShipmentsByShpInstIds
                         (shipmentInstIdsPartition,
                          shmShipmentEagerLoadPlan,
                          entityManager));
			}
		}
		else {
			for (List<String> proNumbersPartition : Lists.partition(proNumbers, appContext.getMaxCountForInClause())) {
                shipments.addAll
                    (shmShipmentSubDAO.listShipmentsByProNbrs
                         (proNumbersPartition,
                          shmShipmentEagerLoadPlan,
                          entityManager));
			}
			// populate the list of ids for later use
			shipmentInstIds = shipments.stream().map(ShmShipment::getShpInstId).collect(Collectors.toList());
		}

		//any shipments with parent, we must find the top parent shipment
		List<Long> parentShipmentInstIds = shipments.stream()
//				.filter(shipment -> !shipment.getParentInstId().equals(0))
				.filter(shipment -> shipment.getParentInstId() != BigDecimal.ZERO && shipment.getParentInstId() != null)
				.map(shipment -> BasicTransformer.toLong(shipment.getParentInstId()))
				.collect(Collectors.toList());
		
		if (!parentShipmentInstIds.isEmpty()){
			//if it has a parent, we don't want the current shipment record
			shipments.removeAll(shipments.stream().filter(shipment -> shipment.getParentInstId() != BigDecimal.ZERO).collect(Collectors.toSet()));
			boolean hasMoreParents = true;
			while(hasMoreParents) {
				for (List<Long> shipmentInstIdsPartition : Lists.partition(parentShipmentInstIds, appContext.getMaxCountForInClause())) {
                    shipments.addAll
                        (shmShipmentSubDAO.listShipmentsByShpInstIds
                             (shipmentInstIdsPartition,
                              shmShipmentEagerLoadPlan,
                              entityManager));
				}
				parentShipmentInstIds = shipments.stream()
						.filter(shipment -> shipment.getParentInstId() != BigDecimal.ZERO)
						.map(shipment -> BasicTransformer.toLong(shipment.getParentInstId()))
						.collect(Collectors.toList());
				hasMoreParents = !parentShipmentInstIds.isEmpty();
			}
		}
		
		//exclude if not rated or paid
		shipments.removeAll(shipments.stream()
				.filter(shipment -> !shipment.getBillStatCd().equalsIgnoreCase(BillStatusCdTransformer.toCode(BillStatusCd.RATED)) && !shipment.getBillStatCd().equalsIgnoreCase(BillStatusCdTransformer.toCode(BillStatusCd.PAID)))
				.collect(Collectors.toSet()));

		// do I want to throw actual error? TODO
		final Set<Long> shipmentsNotFound = new HashSet<>(shipmentInstIds);
		shipmentsNotFound.removeAll(shipments.stream().filter(shipment -> shipment != null).map(shipment -> shipment.getShpInstId()).collect(Collectors.toSet()));

		if (CollectionUtils.isNotEmpty(shipmentsNotFound)) {
			throw ExceptionBuilder
				.exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
				.contextValues(
					String.format(
						"ShipmentInstIds: %s",
						String.join(",", shipmentsNotFound.stream().filter(shipmentNotFound -> shipmentNotFound != null).map(shipmentNotFound -> shipmentNotFound.toString()).collect(Collectors.toList()))
					), ""
				)
				.build();
		}

        return buildResponse(shipments);
	}

	/**
	 * Validate the data on the request
	 *
	 * @param proNumbers
	 * @param shipmentInstIds
	 * @param shipmentDetailCds
	 * @param txnContext
	 * @throws ValidationException
	 * @throws ServiceException
	 */
	protected void validateRequest(
		final List<String> proNumbers,
		final List<Long> shipmentInstIds,
		final TransactionContext txnContext
	) throws ValidationException, ServiceException {

		// If there is a PRO and a shipment instance ID, throw an error -- you can give
		// one or the other, but not both.
		if (CollectionUtils.isNotEmpty(proNumbers) && CollectionUtils.isNotEmpty(shipmentInstIds)) {
			throw addMoreInfo(ExceptionBuilder.exception(ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext),
					shipmentInstIds).log().build();
			// We need a PRO or shipment instance ID.
		} else if (CollectionUtils.isEmpty(shipmentInstIds) && CollectionUtils.isEmpty(proNumbers)) {
			throw addMoreInfo(
				ExceptionBuilder.exception(
					ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext
				),
				shipmentInstIds
			).log().build();
		}
	}

    private ListDeliveryCollectionInstructionForShipmentsResp buildResponse(List<ShmShipment> shipments) {
		final ListDeliveryCollectionInstructionForShipmentsResp response = new ListDeliveryCollectionInstructionForShipmentsResp();
		final List<DeliveryCollectionInstruction> deliveryCollectionInstructions = new ArrayList<>();

		DeliveryCollectionInstruction deliveryCollectionInstruction = null;
		for (ShmShipment shipment: shipments) {

			try {

				deliveryCollectionInstruction = new DeliveryCollectionInstruction();

				deliveryCollectionInstruction.setShipmentInstId(shipment.getShpInstId());

				BigDecimal codAmount = new BigDecimal(0);
				BigDecimal ppdAmount = new BigDecimal(0);
				BigDecimal colAmount = new BigDecimal(0);	
				if (shipment.getChrgToCd().equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.BOTH))
						|| BasicTransformer.toBoolean(shipment.getCodInd())){
					for (ShmMiscLineItem miscLineItem: shipment.getShmMiscLineItems()) {
						if(miscLineItem.getLnTypCd().equalsIgnoreCase(MiscLineItemCdTransformer.toCode(MiscLineItemCd.COD_AMT))) {
							codAmount = miscLineItem.getAmt();
							deliveryCollectionInstruction.setCodAmount(BasicTransformer.toDouble(codAmount));
						}
						if(miscLineItem.getLnTypCd().equalsIgnoreCase(MiscLineItemCdTransformer.toCode(MiscLineItemCd.PART_PPD_LN))) {
							ppdAmount = miscLineItem.getAmt();
						}
						if(miscLineItem.getLnTypCd().equalsIgnoreCase(MiscLineItemCdTransformer.toCode(MiscLineItemCd.PART_COLL_LN))) {
							colAmount = miscLineItem.getAmt();
						}
					}
				}
				String frtChargesTo = "";
				boolean allPpdAc = false;
				boolean unableToDetermineSplit = false;
				boolean collectAcFound = false;
				if (shipment.getChrgToCd().equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.BOTH))) {

					Optional<BigDecimal> totalAcCharges = shipment.getShmAcSvcs().stream()
							.map(acSvc -> acSvc.getAmt())
							.reduce((a,b) -> a.add(b)); 

					long totalAcWithCharges = shipment.getShmAcSvcs().stream()
							.filter(acSvc -> BasicTransformer.toDouble(acSvc.getAmt()) != 0)
							.count(); 

					double freightCharges = BasicTransformer.toDouble(shipment.getTotChrgAmt()) - BasicTransformer.toDouble(codAmount) - BasicTransformer.toDouble(totalAcCharges.get());

					if (freightCharges < 0) {
						//TODO validation that freight charges is less than 0
					}

					if (ppdAmount.equals(BigDecimal.ZERO) || colAmount.equals(BigDecimal.ZERO)) {
						if (colAmount.equals(BigDecimal.ZERO)) {
							frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
							allPpdAc = true;
						} else {
							frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
						}
					} else {
						if (colAmount.equals(codAmount)) {
							frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
							allPpdAc = true;
						} else {
							if (totalAcWithCharges == 0) {
								if (freightCharges == BasicTransformer.toDouble(ppdAmount)) {
									frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
									allPpdAc = true;
								} else {
									unableToDetermineSplit = true;
								}
							}
							if (totalAcWithCharges > 3) {
								if (totalAcCharges.get().equals(ppdAmount)){
									frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
									allPpdAc = true;
								} else if (BasicTransformer.toDouble(totalAcCharges.get()) + freightCharges == BasicTransformer.toDouble(ppdAmount)) {
									frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
									allPpdAc = true;
								} else if (BasicTransformer.toDouble(totalAcCharges.get()) + BasicTransformer.toDouble(codAmount) == BasicTransformer.toDouble(colAmount)) {
									frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
								} else if (BasicTransformer.toDouble(totalAcCharges.get()) + BasicTransformer.toDouble(codAmount) + freightCharges == BasicTransformer.toDouble(colAmount)) {
									frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
								}
							} 
							for (ShmAcSvc acSvc: shipment.getShmAcSvcs()) {
								if (!acSvc.getAmt().equals(BigDecimal.ZERO)) {
									if (acSvc.getAmt().equals(ppdAmount)) {
										if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.PPD))) {
											unableToDetermineSplit = true;
											break;
										} else {
											frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
										}
									} else if (ppdAmount.equals(acSvc.getAmt().add(BasicTransformer.toBigDecimal(freightCharges)))) {
										if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.COLL))) {
											unableToDetermineSplit = true;
											break;
										} else {
											frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
										}
									}
									if (colAmount.equals(acSvc.getAmt().add(codAmount))) {
										if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.COLL))) {
											unableToDetermineSplit = true;
											break;
										} else {
											frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
											collectAcFound = true;
										}
									} else if (colAmount.equals(codAmount.add(acSvc.getAmt().add(BasicTransformer.toBigDecimal(freightCharges))))) {
										if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.PPD))) {
											unableToDetermineSplit = true;
											break;
										} else {
											frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
											collectAcFound = true;
										}
									}
									for (ShmAcSvc acSvc2: shipment.getShmAcSvcs()) {
										if (!acSvc2.getAmt().equals(BigDecimal.ZERO) && !acSvc.getAcCd().equalsIgnoreCase(acSvc2.getAcCd())) {
											if (acSvc2.getAmt().add(acSvc.getAmt()).equals(ppdAmount)) {
												if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.PPD))) {
													unableToDetermineSplit = true;
													break;
												} else {
													frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
												}
											} else if (ppdAmount.equals(acSvc2.getAmt().add(acSvc.getAmt()).add(BasicTransformer.toBigDecimal(freightCharges)))) {
												if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.COLL))) {
													unableToDetermineSplit = true;
													break;
												} else {
													frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
												}
											}
											if (colAmount.equals(acSvc2.getAmt().add(acSvc.getAmt()).add(codAmount))) {
												if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.COLL))) {
													unableToDetermineSplit = true;
													break;
												} else {
													frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
													collectAcFound = true;
												}
											} else if (colAmount.equals(codAmount.add(acSvc2.getAmt().add(acSvc.getAmt()).add(BasicTransformer.toBigDecimal(freightCharges))))) {
												if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.PPD))) {
													unableToDetermineSplit = true;
													break;
												} else {
													frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
													collectAcFound = true;
												}
											}
											for (ShmAcSvc acSvc3: shipment.getShmAcSvcs()) {
												if (!acSvc3.getAmt().equals(BigDecimal.ZERO) 
														&& !acSvc.getAcCd().equalsIgnoreCase(acSvc2.getAcCd())
														&& !acSvc.getAcCd().equalsIgnoreCase(acSvc3.getAcCd())
														&& !acSvc2.getAcCd().equalsIgnoreCase(acSvc3.getAcCd())) {
													if (acSvc3.getAmt().add(acSvc2.getAmt().add(acSvc.getAmt())).equals(ppdAmount)) {
														if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.PPD))) {
															unableToDetermineSplit = true;
															break;
														} else {
															frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
														}
													} else if (ppdAmount.equals(acSvc3.getAmt().add(acSvc2.getAmt().add(acSvc.getAmt()).add(BasicTransformer.toBigDecimal(freightCharges))))) {
														if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.COLL))) {
															unableToDetermineSplit = true;
															break;
														} else {
															frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
														}
													}
													if (colAmount.equals(acSvc3.getAmt().add(acSvc2.getAmt().add(acSvc.getAmt()).add(codAmount)))) {
														if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.COLL))) {
															unableToDetermineSplit = true;
															break;
														} else {
															frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.PPD);
															collectAcFound = true;
														}
													} else if (colAmount.equals(codAmount.add(acSvc3.getAmt().add(acSvc2.getAmt().add(acSvc.getAmt()).add(BasicTransformer.toBigDecimal(freightCharges)))))) {
														if (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.PPD))) {
															unableToDetermineSplit = true;
															break;
														} else {
															frtChargesTo = ChargeToCdTransformer.toCode(ChargeToCd.COLL);
															collectAcFound = true;
														}
													}
												}
											}
										}	
									}
								}							
							}
							if (!unableToDetermineSplit && !collectAcFound) {
								allPpdAc = true;							
							}
						}
					}
				}

				if (shipment.getChrgToCd().equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.VALID_ONLY_FOR_NON_REV_BILLS))
						|| BasicTransformer.toBoolean(shipment.getGblTrfcInd())
						|| shipment.getChrgToCd().equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.PPD))
						|| (frtChargesTo.equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.PPD)) 
								&& allPpdAc)) {
					//For readability
					deliveryCollectionInstruction.setCollectMoneyAtDeliveryCd(CollectMoneyAtDeliveryCd.NONE);
					deliveryCollectionInstruction.setAcceptCashOnlyInd(false);
				} else {
					//check debtor to see how payment must be collected
					List<ShmAsEntdCust> debtors = shipment.getShmAsEntdCusts().stream()
							.filter(party -> BasicTransformer.toBoolean(party.getDebtorInd()))
							.filter(party -> party.getTypCd().equalsIgnoreCase(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.CONS)))
							.collect(Collectors.toList());

					if (debtors.isEmpty()) {
						debtors = shipment.getShmAsEntdCusts().stream()
								.filter(party -> BasicTransformer.toBoolean(party.getDebtorInd()))
								.filter(party -> party.getTypCd().equalsIgnoreCase(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.BILL_TO_INB)) 
										|| party.getTypCd().equalsIgnoreCase(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.BILL_TO_OTB)))
								.collect(Collectors.toList());
					}

					//we only want to use 1 debtor, if multiple debtors exists, use consignee. If consignee is not a debtor, use the bill-to party.
					if (!debtors.isEmpty()) {
						if (!debtors.get(0).getCredStatCd().equalsIgnoreCase(ShipmentCreditStatusCdTransformer.toCode(ShipmentCreditStatusCd.CREDIT))
								&& !debtors.get(0).getCredStatCd().equalsIgnoreCase(ShipmentCreditStatusCdTransformer.toCode(ShipmentCreditStatusCd.POST_PETITION))) {
							deliveryCollectionInstruction.setCollectMoneyAtDeliveryCd(CollectMoneyAtDeliveryCd.FREIGHT);
							deliveryCollectionInstruction.setAcceptCashOnlyInd(
									debtors.get(0).getCredStatCd().equalsIgnoreCase(ShipmentCreditStatusCdTransformer.toCode(ShipmentCreditStatusCd.CASH_ONLY)));
						}
					}

					if (shipment.getChrgToCd().equalsIgnoreCase(ChargeToCdTransformer.toCode(ChargeToCd.BOTH))){ //nullable checks TODO
						deliveryCollectionInstruction.setFreightChargesAmount(BasicTransformer.toDouble(colAmount) - BasicTransformer.toDouble(codAmount));
					} else {
						deliveryCollectionInstruction.setFreightChargesAmount(BasicTransformer.toDouble(shipment.getTotChrgAmt()) - BasicTransformer.toDouble(codAmount));
					}

					if (BasicTransformer.toDouble(codAmount) > 0) {
						if (deliveryCollectionInstruction.getCollectMoneyAtDeliveryCd().equals(CollectMoneyAtDeliveryCd.FREIGHT)) {
							deliveryCollectionInstruction.setCollectMoneyAtDeliveryCd(CollectMoneyAtDeliveryCd.BOTH);	
						} else if (deliveryCollectionInstruction.getCollectMoneyAtDeliveryCd().equals(CollectMoneyAtDeliveryCd.NONE)) {
							deliveryCollectionInstruction.setCollectMoneyAtDeliveryCd(CollectMoneyAtDeliveryCd.COD);
						}
					}

					//check for any payment received during interim delivery and override the need to collect
					if (!deliveryCollectionInstruction.getCollectMoneyAtDeliveryCd().equals(CollectMoneyAtDeliveryCd.NONE)) {
						boolean cashCollected = shipment.getShmMiscLineItems().stream()
								.anyMatch(miscLineItem -> miscLineItem.getLnTypCd().equalsIgnoreCase(MiscLineItemCdTransformer.toCode(MiscLineItemCd.CASH_COLLECTED_LN))
										&& BasicTransformer.toDouble(miscLineItem.getAmt()) > 0);
						boolean codCollected = shipment.getShmMiscLineItems().stream()
								.anyMatch(miscLineItem -> miscLineItem.getLnTypCd().equalsIgnoreCase(MiscLineItemCdTransformer.toCode(MiscLineItemCd.COD_PMT))
										&& BasicTransformer.toDouble(miscLineItem.getAmt()) > 0);

						if ((deliveryCollectionInstruction.getCollectMoneyAtDeliveryCd().equals(CollectMoneyAtDeliveryCd.BOTH) && cashCollected && codCollected)
								|| (deliveryCollectionInstruction.getCollectMoneyAtDeliveryCd().equals(CollectMoneyAtDeliveryCd.COD) && codCollected)
								|| (deliveryCollectionInstruction.getCollectMoneyAtDeliveryCd().equals(CollectMoneyAtDeliveryCd.FREIGHT) && cashCollected))	{
							deliveryCollectionInstruction.setCollectMoneyAtDeliveryCd(CollectMoneyAtDeliveryCd.NONE);						
						}
					}

				}
			
				deliveryCollectionInstructions.add(deliveryCollectionInstruction);
			} catch (Exception e) {
				//error on any shipment being process means we return no delivery collection information
				deliveryCollectionInstruction = new DeliveryCollectionInstruction();
				deliveryCollectionInstruction.setShipmentInstId(shipment.getShpInstId());
				deliveryCollectionInstruction.setCollectMoneyAtDeliveryCd(CollectMoneyAtDeliveryCd.NONE);
				deliveryCollectionInstruction.setAcceptCashOnlyInd(false);

				deliveryCollectionInstructions.add(deliveryCollectionInstruction);
			}
		}
		response.setDeliveryCollectionInstructions(deliveryCollectionInstructions);

		return response;
	}

	/**
     * Facade for shipment exception creation
     */
	private ExceptionBuilder<? extends ServiceException> addMoreInfo(ExceptionBuilder<? extends ServiceException> builder,
																	 List<Long> shipmentInstIds) {
		builder.moreInfo("shipmentInstIds",
			CollectionUtils.isNotEmpty(shipmentInstIds)
				? String.join(",", shipmentInstIds.stream().map(shipmentInstId -> shipmentInstId.toString()).collect(Collectors.toList()))
				: null
		);

        return builder;
    }


}
