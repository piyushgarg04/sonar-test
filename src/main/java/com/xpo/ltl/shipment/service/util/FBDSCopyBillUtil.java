package com.xpo.ltl.shipment.service.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.api.client.util.Lists;
import com.xpo.ltl.api.appointment.v1.DeliveryNotification;
import com.xpo.ltl.api.dockoperations.v1.GetProLoadDetailsResp;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.CashOnDelivery;
import com.xpo.ltl.api.shipment.v2.ChargeToCd;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.DocumentFormTypeCd;
import com.xpo.ltl.api.shipment.v2.FBDSDocument;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.api.shipment.v2.Movement;
import com.xpo.ltl.api.shipment.v2.MovementException;
import com.xpo.ltl.api.shipment.v2.MovementExceptionTypeCd;
import com.xpo.ltl.api.shipment.v2.RateAndCharge;
import com.xpo.ltl.api.shipment.v2.RatedAsWeight;
import com.xpo.ltl.api.shipment.v2.ReducedCharge;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.ShipmentCharge;
import com.xpo.ltl.api.shipment.v2.ShipmentCreditStatusCd;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.api.shipment.v2.SuppRefNbr;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.enums.SuppRefNbrTypeEnum;

/**
 * Utility for FBDS/CopyBill shipment document generation.-
 *
 * @author skapcitzky
 *
 */
public class FBDSCopyBillUtil {

	private static final String QUEBEC_REMARK = "QUEBEC QST #1018347012 TQ0001 -0- RATED";
	private static final String CANADIAN_REMARK = "CANADIAN GST #R139842884 -0- RATED";
	private static final String CANADA_CD = "CA";
	private static final String USA_CD = "US";
	private static final String REMARK_INITIALS_PREFIX = "******";
	private static final double ZERO_PERCENTAGE = 0d;
	private static final double HUNDRED_PERCENTAGE = 100d;
	private static final String MOVEMENT_DELIVERY_TYPE_CODE = "7";
	public static final String CLEARANCE_BILL = "**** CLEARANCE BILL ****";
	public static final String PIECES_SHORT = " PIECES SHORT.";
	public static final String ORIGINAL_BILL_WAS = "*ORIGINAL BILL WAS ";
	private static final String HARTE_HANKS_CIS_NBR_1 = "5855377";
	private static final String HARTE_HANKS_CIS_NBR_2 = "940814628";
	private static final String HARTE_HANKS_CIS_NBR_3 = "654647374";
	private static final String RE_WEIGHED_PER_WEIGHT_CERTIFICATE = "RE-WEIGHED PER WEIGHT CERTIFICATE";
	private static final String TRUE_LTL_PRICING_LINE = "YOU HAVE RECEIVED TRUE LTL PRICING - CAPPED PRICING FOR LARGE LTL SHIPMENTS WITH SAME DAY PICKUP AND GUARANTEED DELIVERY.";
	private static final String TRUE_LTL_PRICING_SAVES_YOU = "TRUE LTL PRICING SAVES YOU";
	private static final String VOLUME_SHIPMENT_PRICING_LINE = "YOU HAVE RECEIVED VOLUME SHIPMENT PRICING - CAPPED PRICING FOR LARGE LTL SHIPMENTS WITH SAME DAY PICKUP AND GUARANTEED DELIVERY.";
	private static final String VOLUME_SHIPMENT_PRICING_SAVES_YOU = "VOLUME SHIPMENT PRICING SAVES YOU";
	private static final String PQ_STATE_CODE = "PQ";
	private static final String DRIVER_COLLECT_CASH = "*** DRIVER COLLECT/CASH ***";
	private static final String DRIVER_COLLECT = "*** DRIVER COLLECT ***";
	private static final String DEFICIT_WEIGHT_DESC_PREFIX = "DEF WGT: ";
	private static final String TARIFF_RATE_M = "M";
	private static final String RC_CREDIT = "CREDIT ";
	private static final String PART_PREPAID = "PartPrepaid";
	private static final String PART_COLLECT = "PartCollect";
	private static final String CASH_PREPAID = "CashPrepaid";
	private static final String FREIGHT_CHARGE = "FreightCharge";
	private static final String PPD = "PPD";
	private static final String COL = "COL";
	private static final String COD_AMOUNT = "C. O. D. AMOUNT";
	private static final String FREIGHT_CHARGE_DESC = "FREIGHT CHARGE";
	private static final String COLL_MONEY_CREDIT_PP = "N";
	private static final String COLL_MONEY_NOT_CREDIT_PP = "F";
	private static final String COLL_MONEY_BOTH_BILL = "B";
	private static final String COLL_MONEY_COD = "C";
	private static final BigDecimal SET_TO_ZEROS = new BigDecimal("0.00");
	private static final String PART_SHORT = "K";
	private static final String EXCP_TYPE_SHORT = "2";
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static final String NOTIFICATION_TIME_FORMAT = "HH:mm";
	private static final DateTimeFormatter NOTIFICATION_TIME_FORMATTER = DateTimeFormatter.ofPattern(NOTIFICATION_TIME_FORMAT);

	private static final String WAF_ACCS_CODE = "WAF";
	private static final String WHF_ACCS_CODE = "WHF";
	private static final String OHF_ACCS_CODE = "OHF";
	private static final String FSC_ACCS_CODE = "FSC";
	private static final String FUEL_ACCS_CODE = "FUEL";
	private static final String BLIND_SHIPMENT_CODE = "BLS";
	private static final String UNRATED = "UNRATED";
	private static final String TOTAL = "TOTAL";
	private static final String WARRANTY_TEXT = "W!";
	private static final String FREEZABLE_TEXT = "**** FREEZABLE SHIPMENT ****";
	private static final String APPOINTMENT_TIME_ZEROES = 	"00:00:00";
	private static final String PCS_TO_APPLY_TO_SHORTAGE = "PCS TO APPLY TO SHORTAGE";
	private static final String FREIGHT_AMT = "FREIGHT_AMT";
	private static final String COD_AMT = "COD_AMT";
	private static final String FREIGHT_COD_AMT = "FREIGHT_COD_AMT";
	private static final String CLSS_TYPE = "Clss";
	private static final String COMMODITY_CLASS_TYPE = "Class ";

	private static final Log LOGGER = LogFactory.getLog(FBDSCopyBillUtil.class);


	/**
	 * Retrieves the most recently created movement exception for current shipment where movement type code = 7,
	 * delivery qualifier code = K and exception type = 2.-
	 * @param movementExceptionsByShmInstId 
	 *
	 * @param shipment
	 *            the shipment to retrieve exceptions from
	 * @return a list of {@link MovementException} for the current shipment.-
	 */
	public MovementException getMovementExceptionForShipment(final List<Movement> movements, Map<Long, List<MovementException>> movementExceptionsByShmInstId) {

		if(CollectionUtils.isEmpty(movements)) {
			return null;
		}
		
		List<MovementException> movementExceptionList = null;
		MovementException movementExcp = new MovementException();

		final Comparator<MovementException> createTimestampComparator = (
				e1,
				e2) -> e1.getAuditInfo().getCreatedTimestamp().compare(e2.getAuditInfo().getCreatedTimestamp());

		for (Movement sm: movements) {
			if (MOVEMENT_DELIVERY_TYPE_CODE.equals(sm.getTypeCd()) && PART_SHORT.equals(sm.getDeliveryQualifierCd())) {
				List<MovementException> exceptions = movementExceptionsByShmInstId.get(sm.getShipmentInstId());
				movementExceptionList = CollectionUtils.emptyIfNull(exceptions)
					.stream()
					.filter(excp -> excp.getTypeCd() == MovementExceptionTypeCd.SHORT)
					.collect(Collectors.toList());
				}
			}

		if (CollectionUtils.isNotEmpty(movementExceptionList)) {
			movementExcp = movementExceptionList.stream().max(createTimestampComparator).get();
		}

		return movementExcp;
	}

	public class FBDSDerivedAttributes {
		Boolean collectMoneyAtDlvry = false;
		Boolean cashOnly = false;
		String collectMoneyAtDeliveryCd = "";
		boolean accessoriesExistsOnBothBill = false;
		ChargeToCd chargeToCdFound;
		List<AccessorialService> accessorialServiceList;
		boolean splitMatchFound = false;

	}

	/**
	 * Generates the collect money method and info on shipment delivery.-
	 *
	 * @param codAmount
	 *            the Shipment's MiscLine amount for COD type
	 * @param shipment
	 *            the actual shipment
	 * @param accesorialServiceList
	 *            the accessorial service list for the current shipment
	 * @param matchedPartyList
	 *            the matched party (cust entd) list for the current shipment
	 * @param miscLineItems
	 *            misc line items list for the current shipment
	 * @param frtChargeAmount
	 * @param cashOnly
	 * @param collectMoneyAtDeliveryBoth
	 */
	public FBDSDerivedAttributes retrieveCollectMoneyAtDeliveryMethod(
		final double codAmount,
		final Shipment shipment,
		final List<AccessorialService> accesorialServiceList,
		final List<AsMatchedParty> matchedPartyList,
		final List<MiscLineItem> miscLineItems,
		Boolean collectMoneyAtDeliveryBoth,
		boolean cashOnly,
		double frtChargeAmount) {

		FBDSDerivedAttributes fbdsAttributes = new FBDSDerivedAttributes();
		boolean accessoriesExistsOnBothBill = false;
		final ChargeToCd chargeToCdFound = null;
		AsMatchedParty matchedPartyFound = null;
		double collectAmount = 0.0d;

		if (ChargeToCd.BOTH.equals(shipment.getChargeToCd())) {

				final Optional<MiscLineItem> collectAmountOptional = CollectionUtils.emptyIfNull(miscLineItems)
						.stream()
						.filter(miscLine -> MiscLineItemCd.PART_COLL_LN.equals(miscLine.getLineTypeCd()))
						.findFirst();
				collectAmount = collectAmountOptional.isPresent() ? collectAmountOptional.get().getAmount() :
					ZERO_PERCENTAGE;

				setAccesorialData(
					shipment,
					codAmount,
					collectAmount,
					miscLineItems,
					accesorialServiceList,
					chargeToCdFound,
					fbdsAttributes);

				if (CollectionUtils.emptyIfNull(accesorialServiceList)
						.stream()
						.filter(accs -> accs.getPrepaidPercentage() != HUNDRED_PERCENTAGE)
						.count() > 0) {
					accessoriesExistsOnBothBill = true;
				}
		}

		if (!(ChargeToCd.PPD.equals(shipment.getChargeToCd())
				|| ChargeToCd.VALID_ONLY_FOR_NON_REV_BILLS.equals(shipment.getChargeToCd())
				|| (ChargeToCd.PPD.equals(chargeToCdFound) && accessoriesExistsOnBothBill)
				|| shipment.getGovtBolTrafficInd())) {
			matchedPartyFound = CollectionUtils.emptyIfNull(matchedPartyList)
					.stream()
					.filter(party -> (party.getDebtorInd()
							&& ((ChargeToCd.COLL.equals(shipment.getChargeToCd())
									&& MatchedPartyTypeCd.CONS.equals(party.getTypeCd())))))
					.findFirst()
					.orElseGet(() -> getBillToAsMatchedParty(matchedPartyList, shipment));
		}


		if (!Objects.isNull(matchedPartyFound)) {
				if ((!ShipmentCreditStatusCd.CREDIT.equals(matchedPartyFound.getCreditStatusCd())
						&& !ShipmentCreditStatusCd.POST_PETITION.equals(matchedPartyFound.getCreditStatusCd()))) {
					collectMoneyAtDeliveryBoth = true;
					fbdsAttributes.collectMoneyAtDeliveryCd = COLL_MONEY_NOT_CREDIT_PP;
					if (ShipmentCreditStatusCd.CASH_ONLY.equals(matchedPartyFound.getCreditStatusCd())) {
						cashOnly = true;
					}
				} else {
					collectMoneyAtDeliveryBoth = false;
					fbdsAttributes.collectMoneyAtDeliveryCd = COLL_MONEY_CREDIT_PP;
				}
		}

		frtChargeAmount = (collectMoneyAtDeliveryBoth ? (collectAmount - codAmount) :
			(shipment.getTotalChargeAmount() - codAmount));

		if (codAmount > 0) {
			if (collectMoneyAtDeliveryBoth) {
				fbdsAttributes.collectMoneyAtDeliveryCd = COLL_MONEY_BOTH_BILL;
			} else {
				fbdsAttributes.collectMoneyAtDeliveryCd = COLL_MONEY_COD;
			}
		}


		fbdsAttributes.cashOnly = cashOnly;
		fbdsAttributes.collectMoneyAtDlvry = collectMoneyAtDeliveryBoth;
		fbdsAttributes.accessoriesExistsOnBothBill = accessoriesExistsOnBothBill;
		if (fbdsAttributes.chargeToCdFound == null)
			fbdsAttributes.chargeToCdFound = chargeToCdFound;

		return fbdsAttributes;
	}

	private static AsMatchedParty getBillToAsMatchedParty(List<AsMatchedParty> matchedPartyList, Shipment shipment) {

		Optional<AsMatchedParty> result = CollectionUtils.emptyIfNull(matchedPartyList)
				.stream()
				.filter(party -> (party.getDebtorInd()
						&& ((ChargeToCd.COLL.equals(shipment.getChargeToCd())
								&& (MatchedPartyTypeCd.BILL_TO_INB.equals(party.getTypeCd())
								|| MatchedPartyTypeCd.BILL_TO_OTB.equals(party.getTypeCd()))))))
				.findFirst();

		if (result.isPresent())
			return result.get();

		return null;
	}

	/**
	 * Evaluates accessorial services data an set percentages for zero and positive amounts.-
	 *
	 * @param shipment
	 *            the shipment to take the info from
	 * @param codAmount
	 *            the Shipment's MiscLine amount for COD type
	 * @param collectAmount
	 *            the Shipment's MiscLine amount for Collect type
	 * @param miscLineItems
	 *            the list of miscLineItems
	 * @param accessorialServiceList
	 *            the accessorial service to evaluate and set data
	 * @param matchFound
	 *            the a boolean to determine if a prepaid is found for later analysis
	 * @param chargeToCdFound
	 *            the cargeTo code to be found
	 */
	private void setAccesorialData(
		final Shipment shipment,
		final double codAmount,
		final double collectAmount,
		final List<MiscLineItem> miscLineItems,
		final List<AccessorialService> accessorialServiceList,
		ChargeToCd chargeToCdFound,
		FBDSDerivedAttributes fbdsAttributes) {

		Double percentageToSet = null;

		final Optional<MiscLineItem> prepaidAmountOptional = CollectionUtils.emptyIfNull(miscLineItems)
				.stream()
				.filter(miscLine -> MiscLineItemCd.PART_PPD_LN.equals(miscLine.getLineTypeCd()))
				.findFirst();

		final double prepaidAmount = new Double(df2.format(prepaidAmountOptional.isPresent() ? prepaidAmountOptional.get().getAmount() :
			ZERO_PERCENTAGE));

		final Map<Boolean, List<AccessorialService>> accesorialServiceAmountMap = CollectionUtils.emptyIfNull(accessorialServiceList)
				.stream()
				.collect(Collectors.partitioningBy(accs -> accs.getAmount() > 0));

		final double positiveAccesTotalAmount = new Double(df2.format(accesorialServiceAmountMap
				.get(true)
				.stream()
				.collect(Collectors.summarizingDouble(AccessorialService::getAmount))
				.getSum()));

		final double frtAmount = new Double(df2.format(shipment.getTotalChargeAmount().doubleValue() - positiveAccesTotalAmount));

		final double frtCodAmount = new Double(df2.format(frtAmount - codAmount));

		if (collectAmount == 0 || codAmount == collectAmount
				|| (positiveAccesTotalAmount == 0 && frtAmount == prepaidAmount)) {
			chargeToCdFound = ChargeToCd.PPD;
			percentageToSet = setAccessorialPrepaidPercentage(accessorialServiceList, HUNDRED_PERCENTAGE);
		} else if (prepaidAmount == 0) {
			chargeToCdFound = ChargeToCd.COLL;
			percentageToSet = setAccessorialPrepaidPercentage(accessorialServiceList, ZERO_PERCENTAGE);
		}

		if (percentageToSet == null && accessorialServiceList.size() > 3) {

			if (positiveAccesTotalAmount == prepaidAmount) {
				chargeToCdFound = ChargeToCd.COLL;
				percentageToSet = HUNDRED_PERCENTAGE;
			} else if ((frtCodAmount + positiveAccesTotalAmount) == prepaidAmount) {
				chargeToCdFound = ChargeToCd.PPD;
				percentageToSet = HUNDRED_PERCENTAGE;
			} else if ((codAmount + positiveAccesTotalAmount) == collectAmount) {
				chargeToCdFound = ChargeToCd.PPD;
				percentageToSet = ZERO_PERCENTAGE;
			} else if ((frtAmount + positiveAccesTotalAmount) == collectAmount) {
				chargeToCdFound = ChargeToCd.COLL;
				percentageToSet = ZERO_PERCENTAGE;
			}

			for (final AccessorialService accessorialService : accessorialServiceList) {
				if (percentageToSet != null)
					accessorialService.setPrepaidPercentage(percentageToSet);
			}
		}

		if (percentageToSet == null) {
			//If percentageToSet was not set above, check single accessorial record
			for (AccessorialService acc: accessorialServiceList) {
				if (acc.getAmount() == prepaidAmount) {
					chargeToCdFound = ChargeToCd.COLL;
					acc.setPrepaidPercentage(HUNDRED_PERCENTAGE);
					//set all other accessorial prepaid % to 0
					fbdsAttributes.splitMatchFound = true;
					fbdsAttributes = setOtherAccessorials(
							accessorialServiceList, Arrays.asList(acc.getAccessorialCd()), ZERO_PERCENTAGE, fbdsAttributes);
					break;
				} else if (!fbdsAttributes.splitMatchFound && (frtAmount + acc.getAmount()) == prepaidAmount) {
					chargeToCdFound = ChargeToCd.PPD;
					acc.setPrepaidPercentage(HUNDRED_PERCENTAGE);
					//set all other accessorial prepaid % to 0
					fbdsAttributes.splitMatchFound = true;
					fbdsAttributes = setOtherAccessorials(
							accessorialServiceList, Arrays.asList(acc.getAccessorialCd()), ZERO_PERCENTAGE, fbdsAttributes);
					break;
				} else if (!fbdsAttributes.splitMatchFound && (codAmount + acc.getAmount()) == collectAmount) {
					chargeToCdFound = ChargeToCd.PPD;
					acc.setPrepaidPercentage(ZERO_PERCENTAGE);
					//set all other accessorial prepaid % to 100
					fbdsAttributes.splitMatchFound = true;
					fbdsAttributes = setOtherAccessorials(
							accessorialServiceList, Arrays.asList(acc.getAccessorialCd()), HUNDRED_PERCENTAGE, fbdsAttributes);
					break;
				} else if (!fbdsAttributes.splitMatchFound && (frtCodAmount + acc.getAmount()) == collectAmount) {
					chargeToCdFound = ChargeToCd.COLL;
					acc.setPrepaidPercentage(ZERO_PERCENTAGE);
					//set all other accessorial prepaid % to 100
					fbdsAttributes.splitMatchFound = true;
					fbdsAttributes = setOtherAccessorials(
							accessorialServiceList, Arrays.asList(acc.getAccessorialCd()), HUNDRED_PERCENTAGE, fbdsAttributes);
					break;
				}
			}
		}

		fbdsAttributes.chargeToCdFound = chargeToCdFound;

		if (!fbdsAttributes.splitMatchFound) {
			//If prepaid percentage could not be set above, perform the following -
			//Need to loop through every 2 accessorial to check if any combo of 2 can equal the prepaid or collect amounts
			//in order to set the prepaid percentage
			fbdsAttributes = getAccessorialSplits(accessorialServiceList, 2, prepaidAmount,
					frtCodAmount, frtAmount, codAmount, null, fbdsAttributes);
			if (!fbdsAttributes.splitMatchFound)
				fbdsAttributes = getAccessorialSplits(accessorialServiceList, 2, prepaidAmount,
						frtCodAmount, frtAmount, codAmount, FREIGHT_AMT, fbdsAttributes);
			if (!fbdsAttributes.splitMatchFound)
				fbdsAttributes = getAccessorialSplits(accessorialServiceList, 2, collectAmount,
						frtCodAmount, frtAmount, codAmount, COD_AMT, fbdsAttributes);
			if (!fbdsAttributes.splitMatchFound)
				fbdsAttributes = getAccessorialSplits(accessorialServiceList, 2, collectAmount,
						frtCodAmount, frtAmount, codAmount, FREIGHT_COD_AMT, fbdsAttributes);

			//If no matchFound Need to loop through every 3 accessorial to check if any combo of 3 can equal the prepaid or collect amounts
			if (!fbdsAttributes.splitMatchFound)
				fbdsAttributes = getAccessorialSplits(accessorialServiceList, 3, prepaidAmount,
						frtCodAmount, frtAmount, codAmount, null, fbdsAttributes);
			if (!fbdsAttributes.splitMatchFound)
				fbdsAttributes = getAccessorialSplits(accessorialServiceList, 3, prepaidAmount,
						frtCodAmount, frtAmount, codAmount, FREIGHT_AMT, fbdsAttributes);
			if (!fbdsAttributes.splitMatchFound)
				fbdsAttributes = getAccessorialSplits(accessorialServiceList, 3, collectAmount,
						frtCodAmount, frtAmount, codAmount, COD_AMT, fbdsAttributes);
			if (!fbdsAttributes.splitMatchFound)
				fbdsAttributes = getAccessorialSplits(accessorialServiceList, 3, collectAmount,
						frtCodAmount, frtAmount, codAmount, FREIGHT_COD_AMT, fbdsAttributes);
		}

	}

	private Double setAccessorialPrepaidPercentage(final List<AccessorialService> accessorialServiceList,
			Double percentage) {
		for (final AccessorialService accessorialService : accessorialServiceList) {
			accessorialService.setPrepaidPercentage(percentage);
		}
		return percentage;
	}


	private FBDSDerivedAttributes setOtherAccessorials(List<AccessorialService> accessorialServiceList, List<String> accCds,
			Double prepaidPercentage, FBDSDerivedAttributes fbdsAttributes) {

		fbdsAttributes.accessorialServiceList = accessorialServiceList.stream().filter(acc -> !accCds.contains(acc.getAccessorialCd()))
		.peek(x-> x.setPrepaidPercentage(prepaidPercentage))
		.collect(Collectors.toList());

		return fbdsAttributes;

	}

	private FBDSDerivedAttributes getAccessorialSplits(List<AccessorialService> accessorialServiceList, int combi, double amtToCompare,
			double frtCodAmount, double frtAmount, double codAmount, String type, FBDSDerivedAttributes fbdsAttributes) {

		Double amt = 0.0;
		List<String> accCds = new ArrayList<>();
		int n=0;

		for (int i=0; i<accessorialServiceList.size(); i++) {
			n++;
			amt += accessorialServiceList.get(i).getAmount();
			accCds.add(accessorialServiceList.get(i).getAccessorialCd());
			if (n % combi == 0) {
				if ((StringUtils.isBlank(type) || FREIGHT_AMT.equals(type))
						&& (amt == amtToCompare || (frtAmount+amt) == amtToCompare)) {
					fbdsAttributes = setPrepaidPercentage(
								accessorialServiceList, accCds, fbdsAttributes, i, combi, HUNDRED_PERCENTAGE, ZERO_PERCENTAGE);
					if (amt == amtToCompare)
						fbdsAttributes.chargeToCdFound = ChargeToCd.COLL;
					if ((frtAmount+amt) == amtToCompare)
						fbdsAttributes.chargeToCdFound = ChargeToCd.PPD;

				} else if ((COD_AMT.equals(type) || FREIGHT_COD_AMT.equals(type))
						&& ((codAmount+amt) == amtToCompare || (frtCodAmount+amt) == amtToCompare)) {
					fbdsAttributes = setPrepaidPercentage(
								accessorialServiceList, accCds, fbdsAttributes, i, combi, ZERO_PERCENTAGE, HUNDRED_PERCENTAGE);
					if ((codAmount+amt) == amtToCompare)
						fbdsAttributes.chargeToCdFound = ChargeToCd.PPD;
					if ((frtCodAmount+amt) == amtToCompare)
						fbdsAttributes.chargeToCdFound = ChargeToCd.COLL;
				}
				amt = 0.0;
				accCds = new ArrayList<>();
			}
		}
		return fbdsAttributes;
	}

	private FBDSDerivedAttributes setPrepaidPercentage(List<AccessorialService> accessorialServiceList, List<String> accCds,
			FBDSDerivedAttributes fbdsAttributes, int index, int combi, double percentage, double otherAccPercentage) {

		accessorialServiceList.get(index).setPrepaidPercentage(percentage);
		accessorialServiceList.get(index-1).setPrepaidPercentage(percentage);
		if (combi > 2) {
			accessorialServiceList.get(index-2).setPrepaidPercentage(percentage);
		}
		fbdsAttributes.splitMatchFound = true;
		fbdsAttributes = setOtherAccessorials(
				accessorialServiceList, accCds, otherAccPercentage, fbdsAttributes);

		return fbdsAttributes;
	}

	/**
	 * Evaluates reference numbers and returns data formatted. Populates
	 * customerReferenceNbrs, poNbrs, and otherReferenceNbrs, as needed.
	 *
	 * @param shipment the shipment to get the info from
	 * @param customerReferenceNbrs
	 * @param poNbrs
	 * @param otherReferenceNbrs
	 * @return a list of formatted reference numbers
	 */
	public List<String> retrieveSrnInfo(final ShmShipment shipment, List<String> customerReferenceNbrs,
			List<String> poNbrs, List<String> otherReferenceNbrs) {

		List<String> returnList = new ArrayList<>();
		List<String> poList = new ArrayList<>();
		List<String> snList = new ArrayList<>();
		List<String> gblList = new ArrayList<>();
		List<String> hhList = new ArrayList<>();

		Map<MatchedPartyTypeCd, List<AsMatchedParty>> parentAsMatchedPartyMap = shipment
				.getShmAsEntdCusts()
				.stream()
				.filter(
					cust -> MatchedPartyTypeCd.SHPR.value().equals(cust.getTypCd())
					|| MatchedPartyTypeCd.CONS.value().equals(cust.getTypCd())
					|| MatchedPartyTypeCd.BILL_TO_INB.value().equals(cust.getTypCd())
					|| MatchedPartyTypeCd.BILL_TO_OTB.value().equals(cust.getTypCd()))
				.map(EntityTransformer::toAsMatchedParty)
				.collect(Collectors.groupingBy(AsMatchedParty::getTypeCd));

		Map<String, List<SuppRefNbr>> srNumberMap = EntityTransformer
				.toSuppRefNbr(shipment.getShmSrNbrs())
				.stream()
				.collect(Collectors.groupingBy(SuppRefNbr::getTypeCd));

		if (Objects.nonNull(customerReferenceNbrs)) {
			customerReferenceNbrs.addAll(buildSrnInfoList(SuppRefNbrTypeEnum.SN_NUMBER, srNumberMap));
		}

		if (Objects.nonNull(poNbrs)) {
			poNbrs.addAll(buildSrnInfoList(SuppRefNbrTypeEnum.PO_NUMBER, srNumberMap));
		}

		if (Objects.nonNull(otherReferenceNbrs)) {
			otherReferenceNbrs.addAll(buildSrnInfoList(SuppRefNbrTypeEnum.OTHER, srNumberMap));
		}

		for (Entry<String, List<SuppRefNbr>> entry : srNumberMap.entrySet()) {
			if (SuppRefNbrTypeEnum.PO_NUMBER.getName().equals(entry.getKey())) {
				poList.add(entry.getKey() + " " + entry.getValue().get(0).getRefNbr());
			} else if(SuppRefNbrTypeEnum.GBL.getName().equals(entry.getKey())) {
				gblList.add(entry.getKey() + " " + entry.getValue().get(0).getRefNbr());
			} else if(SuppRefNbrTypeEnum.SN_NUMBER.getName().equals(entry.getKey())) {
				snList.add(entry.getKey() + " " + entry.getValue().get(0).getRefNbr());
			}

			AsMatchedParty asMatchedParty = null;
			if (parentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.BILL_TO_INB)) {
				asMatchedParty = parentAsMatchedPartyMap.get(MatchedPartyTypeCd.BILL_TO_INB).get(0);
			} else if (parentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.BILL_TO_OTB)) {
				asMatchedParty = parentAsMatchedPartyMap.get(MatchedPartyTypeCd.BILL_TO_OTB).get(0);
			}
			// Harte Hanks client
			if (!Objects.isNull(asMatchedParty)
					&& (HARTE_HANKS_CIS_NBR_1.equals(asMatchedParty.getCisCustNbr().toString())
							|| HARTE_HANKS_CIS_NBR_2.equals(asMatchedParty.getCisCustNbr().toString())
							|| HARTE_HANKS_CIS_NBR_3.equals(asMatchedParty.getCisCustNbr().toString()))) {
				hhList.add(
					SuppRefNbrTypeEnum.valueOf(entry.getKey()).getFormatValue() + " "
							+ entry.getValue().get(0).getRefNbr());
			}

		}

		returnList.addAll(snList);
		returnList.addAll(gblList);
		returnList.addAll(poList);
		returnList.addAll(hhList);

		return returnList;
	}

	private static List<String> buildSrnInfoList(SuppRefNbrTypeEnum suppRefNbrType,
			Map<String, List<SuppRefNbr>> srNumberMap) {

		if (Objects.isNull(suppRefNbrType) || MapUtils.isEmpty(srNumberMap)) {
			return Collections.emptyList();
		}

		final List<SuppRefNbr> suppRefNbrs = new ArrayList<>();
		List<SuppRefNbr> certainTypeOfSuppRefNbrs = null;

		switch (suppRefNbrType) {
		case SN_NUMBER:
			certainTypeOfSuppRefNbrs = getSnNumbers(srNumberMap);
			break;
		case PO_NUMBER:
			certainTypeOfSuppRefNbrs = getPoNumbers(srNumberMap);
			break;
		default:
			return getOtherReferenceNumbers(srNumberMap);
		}

		if (CollectionUtils.isNotEmpty(certainTypeOfSuppRefNbrs)) {
			suppRefNbrs.addAll(certainTypeOfSuppRefNbrs);
		}

		return suppRefNbrs.size() > 1
				? suppRefNbrs.stream()
						.map(SuppRefNbr::getRefNbr)
						.collect(Collectors.toList())
				: Collections.emptyList();
	}

	private static List<SuppRefNbr> getSnNumbers(Map<String, List<SuppRefNbr>> srNumberMap) {
		if (MapUtils.isEmpty(srNumberMap)) {
			return Collections.emptyList();
		}

		final List<SuppRefNbr> suppRefNbrs = new ArrayList<>();

		final List<SuppRefNbr> snNumbers = srNumberMap.get(SuppRefNbrTypeEnum.SN_NUMBER.getName());
		if (CollectionUtils.isNotEmpty(snNumbers)) {
			suppRefNbrs.addAll(snNumbers);
		}

		return suppRefNbrs;
	}

	private static List<SuppRefNbr> getPoNumbers(Map<String, List<SuppRefNbr>> srNumberMap) {
		if (MapUtils.isEmpty(srNumberMap)) {
			return Collections.emptyList();
		}

		final List<SuppRefNbr> suppRefNbrs = new ArrayList<>();

		final List<SuppRefNbr> poNumbers = srNumberMap.get(SuppRefNbrTypeEnum.PO_NUMBER.getName());
		if (CollectionUtils.isNotEmpty(poNumbers)) {
			suppRefNbrs.addAll(poNumbers);
		}

		final List<SuppRefNbr> pos = srNumberMap.get(SuppRefNbrTypeEnum.PO.getName());
		if (CollectionUtils.isNotEmpty(pos)) {
			suppRefNbrs.addAll(pos);
		}

		return suppRefNbrs;
	}

	private static List<String> getOtherReferenceNumbers(Map<String, List<SuppRefNbr>> srNumberMap) {
		if (MapUtils.isEmpty(srNumberMap)) {
			return Collections.emptyList();
		}

		return srNumberMap.entrySet().stream()
				.filter(entry -> !SuppRefNbrTypeEnum.SN_NUMBER.getName().equals(entry.getKey())
						&& !SuppRefNbrTypeEnum.PO_NUMBER.getName().equals(entry.getKey())
						&& !SuppRefNbrTypeEnum.GBL.getName().equals(entry.getKey()))
				.map(entry -> entry.getValue().stream().map(suppRefNbr -> entry.getKey() + " " + suppRefNbr.getRefNbr())
						.collect(Collectors.toList()))
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	/**
	 * Format remark text according to the type code. Returns a map of edited remarks and the billers initials (just for
	 * type code = 2).
	 */
    public void setRemarks(DocumentFormTypeCd documentFormTypeCd,
                           Shipment shipment,
                           Map<MatchedPartyTypeCd, List<AsMatchedParty>> shipmentAsMatchedPartyMap,
                           FBDSDocument document,
                           List<Remark> remarkList,
                           boolean displayRatesCharges,
                           FBDSDerivedAttributes fbdsAttributes) {
		Map<ShipmentRemarkTypeCd, List<Remark>> remarkByTypeMap = CollectionUtils.emptyIfNull(remarkList)
				.stream()
				.collect(Collectors.groupingBy(Remark::getTypeCd));

		Remark remark = null;

		if (remarkByTypeMap.containsKey(ShipmentRemarkTypeCd.ADHOC_HZ_MATL_RMK)) {
			document.setHazmatRemark(remarkByTypeMap.get(ShipmentRemarkTypeCd.ADHOC_HZ_MATL_RMK).get(0).getRemark());
		}

		//Adding shiplify accessorials remarks
		if (remarkByTypeMap.containsKey(ShipmentRemarkTypeCd.SHIPLIFY_RMK)) {
			document.setShiplifyRemark(remarkByTypeMap.get(ShipmentRemarkTypeCd.SHIPLIFY_RMK).get(0).getRemark());
		}

		if (remarkByTypeMap.containsKey(ShipmentRemarkTypeCd.SHIPPING_RMK)) {

			String billerInitials = StringUtils.EMPTY;
			String formattedRemark = StringUtils.EMPTY;

			remark = remarkByTypeMap.get(ShipmentRemarkTypeCd.SHIPPING_RMK).get(0);

			if (StringUtils.isNotBlank(remark.getRemark()))
				formattedRemark = remark.getRemark();

			if (formattedRemark.contains(RE_WEIGHED_PER_WEIGHT_CERTIFICATE)) {
				formattedRemark = StringUtils.remove(formattedRemark, RE_WEIGHED_PER_WEIGHT_CERTIFICATE);
			}
			if (formattedRemark.contains(TRUE_LTL_PRICING_LINE)) {
				//get last index position of this constant -
				int lastIndexPos = formattedRemark.indexOf(
						TRUE_LTL_PRICING_LINE) + TRUE_LTL_PRICING_LINE.length();
				formattedRemark = formatShipmentRemarks(formattedRemark, TRUE_LTL_PRICING_LINE, lastIndexPos, 47);
			}
			if (formattedRemark.contains(TRUE_LTL_PRICING_SAVES_YOU)) {
				int lastIndexPos = formattedRemark.indexOf(
						TRUE_LTL_PRICING_SAVES_YOU) + TRUE_LTL_PRICING_SAVES_YOU.length();
				formattedRemark = formatShipmentRemarks(formattedRemark, TRUE_LTL_PRICING_SAVES_YOU, lastIndexPos, 11);
			}
			if (formattedRemark.contains(VOLUME_SHIPMENT_PRICING_LINE)) {
				int lastIndexPos = formattedRemark.indexOf(
						VOLUME_SHIPMENT_PRICING_LINE) + VOLUME_SHIPMENT_PRICING_LINE.length();
				formattedRemark = formatShipmentRemarks(formattedRemark, VOLUME_SHIPMENT_PRICING_LINE, lastIndexPos, 54);
			}
			if (formattedRemark.contains(VOLUME_SHIPMENT_PRICING_SAVES_YOU)) {
				int lastIndexPos = formattedRemark.indexOf(
						VOLUME_SHIPMENT_PRICING_SAVES_YOU) + VOLUME_SHIPMENT_PRICING_SAVES_YOU.length();
				formattedRemark = formatShipmentRemarks(formattedRemark, VOLUME_SHIPMENT_PRICING_SAVES_YOU, lastIndexPos, 11);
			}

			//get the last 9 characters containing ****** prefix and biller initials, if any
			if (formattedRemark.length() > 9 && formattedRemark.contains(REMARK_INITIALS_PREFIX)) {
				String lastNine = formattedRemark.substring(formattedRemark.length()-9);
				if (lastNine.startsWith(REMARK_INITIALS_PREFIX)) {
					billerInitials = lastNine;
					document.setBillerInitials(billerInitials);
				} else {
					document.setShipmentRemark(formattedRemark);
				}
			} else if (!StringUtils.EMPTY.equals(formattedRemark)) {
				document.setShipmentRemark(formattedRemark);
			} else if (StringUtils.isNotBlank(remark.getRemark())) {
				document.setShipmentRemark(remark.getRemark());
			}
		}

		if (remarkByTypeMap.containsKey(ShipmentRemarkTypeCd.AUTHORITY_LN_TXT_RMK)) {

			remark = remarkByTypeMap.get(ShipmentRemarkTypeCd.AUTHORITY_LN_TXT_RMK).get(0);
            if (documentFormTypeCd == DocumentFormTypeCd.COPY_BILL) {
				document.setAuthorityLineRemark(remark.getRemark());
            }
            else if (documentFormTypeCd == DocumentFormTypeCd.FBDS) {
				if ((BillStatusCd.UNBILLED.equals(shipment.getBillStatusCd())
						|| BillStatusCd.IN_FBES_SUSPENSE_QUEUE.equals(shipment.getBillStatusCd())
						|| BillStatusCd.BILLED.equals(shipment.getBillStatusCd()))
						&& shipment.getRevenueBillInd() && displayRatesCharges) {
					document.setAuthorityLineRemark(remark.getRemark());
				}
			}
		}

         if (document.getAuthorityLineRemark() == null
             && documentFormTypeCd == DocumentFormTypeCd.FBDS
             && shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.BILL_TO_INB)
             && COLL_MONEY_CREDIT_PP.equals(fbdsAttributes.collectMoneyAtDeliveryCd)) {
			String custName = shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.BILL_TO_INB).get(0).getName1();
			document.setAuthorityLineRemark(
					String.format("%s%s", "COLLECT CHARGES ARE BEING PAID BY THE FOLLOWING BILL TO: ", custName));
		}

		if (remarkByTypeMap.containsKey(ShipmentRemarkTypeCd.DLVRY_ATTM_RMK)) {
			remark = remarkByTypeMap.get(ShipmentRemarkTypeCd.DLVRY_ATTM_RMK).get(0);
			document.setDeliveryAttachmentRemark(String.format("%s%s", "ATTD:", remark.getRemark()));
		}

		if (shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.SHPR)
				&& CollectionUtils.isNotEmpty(shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.SHPR))
				&& shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.CONS)
				&& CollectionUtils.isNotEmpty(shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.CONS))) {

			AsMatchedParty shipper = shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.SHPR).get(0);
			AsMatchedParty consignee = shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.CONS).get(0);
			
			if (((CANADA_CD.equals(
				shipper.getCountryCd()))
					&& (StringUtils.SPACE.equals(consignee.getCountryCd())
							|| USA_CD.equals(consignee.getCountryCd())))
					||
					((USA_CD.equals(shipper.getCountryCd()))
							&& (StringUtils
									.SPACE.equals(consignee.getCountryCd())
									|| CANADA_CD.equals(consignee.getCountryCd())))) {
				document.setCanadianGoodsServicesRemark(CANADIAN_REMARK);
			} else {
				if (PQ_STATE_CODE.equals(shipper.getStateCd())
						|| PQ_STATE_CODE.equals(
								consignee.getStateCd())) {
					document.setQuebecGstRemark(QUEBEC_REMARK);
				}
			}
		}

	}

	private static String formatShipmentRemarks(String remarks, String constant, int index, int length) {

		if (index != -1) {
			//Add given chars (length) from the lastIndex, to remove
			String trailingChars = remarks.substring(index, index+length);
			remarks = StringUtils.remove(remarks, constant.concat(trailingChars));
			return remarks;
		}
		return remarks;
	}

	/**
	 * Method to get formatted equipment number for FBDS
	 * @param shipment
	 * @return equipmentNbr
	 */
	public String getFormattedEquipNbr(ShmShipment shipment) {

		String formattedEquipNbr = StringUtils.SPACE; //if no record found, return spaces

		List<ShmMovement> movements = shipment.getShmMovements();
		String trailerIdPfxTxt = null;
		BigDecimal trailerIdSfxNbr = null;
		List<ShmMovement> sortedMovements = Lists.newArrayList();

		//Since there could be multiple ShmMovements for a Shipment, get the latest based on MvmtTmst
		if (CollectionUtils.isNotEmpty(movements)) {
			sortedMovements = movements.stream()
					.sorted(Comparator.comparing(ShmMovement::getMvmtTmst).reversed())
					.collect(Collectors.toList());
		}

		if (CollectionUtils.isNotEmpty(sortedMovements)) {
			trailerIdPfxTxt = sortedMovements.get(0).getTrlrIdPfxTxt();
			trailerIdSfxNbr = sortedMovements.get(0).getTrlrIdSfxNbr();
			if (StringUtils.isNotBlank(trailerIdPfxTxt) && Objects.nonNull(trailerIdSfxNbr)) {
				trailerIdPfxTxt = StringUtils.stripStart(trailerIdPfxTxt, "0");
				formattedEquipNbr = String.format("%s-%s", trailerIdPfxTxt,trailerIdSfxNbr.toString());
			}
		}

		return formattedEquipNbr;
	}
	

	public String getFormattedEquipNbr(final String trailerIdPfxTxt, final String trailerIdSfxNbr) {
		String result = null;
		if(trailerIdPfxTxt != null && trailerIdSfxNbr != null) {
			if (StringUtils.isNotBlank(trailerIdPfxTxt) && Objects.nonNull(trailerIdSfxNbr)) {
				String modifiedTrailerIdPfxTxt = StringUtils.stripStart(trailerIdPfxTxt, "0");
				result = String.format("%s-%s", modifiedTrailerIdPfxTxt, trailerIdSfxNbr.toString());
			}
		}

		return result;
	}

    /**
     * Method to set Bill-To info
     */
    public FBDSDocument setBillToCustomer(List<AsMatchedParty> asMatchedParties,
                                          Shipment shipmentDto,
                                          FBDSDocument document,
                                          DocumentFormTypeCd documentFormTypeCd) {
    	
    	if(CollectionUtils.isNotEmpty(asMatchedParties)) {
    		for (AsMatchedParty asMatchedParty: asMatchedParties) {
        		//checking for type 3 or 4. Only one of them should be present
        		if(asMatchedParty.getTypeCd() == MatchedPartyTypeCd.BILL_TO_INB
        				|| asMatchedParty.getTypeCd() == MatchedPartyTypeCd.BILL_TO_OTB) {

                    if (documentFormTypeCd == DocumentFormTypeCd.COPY_BILL) {
        				document.setBillTo(asMatchedParty);
        				break;
        			}

        			if (!((StringUtils.SPACE.equals(asMatchedParty.getName1())
        					|| ChargeToCd.PPD.equals(shipmentDto.getChargeToCd())
        					|| ChargeToCd.COLL.equals(shipmentDto.getChargeToCd()))
        				&& (!BillClassCd.PARTIAL_SEGMENT.equals(shipmentDto.getBillClassCd())))) {

        				//display BillTo in FBDS doc
        				document.setBillTo(asMatchedParty);
        				break;
        			}
        		}
        	}

    	}
    	
    	return document;
    }

    /**
     * Method to set Rates and charges on FBDS/CopyBill document.
     */
    public FBDSDocument setRatesAndCharges(List<MiscLineItem> miscLineItems,
                                           Shipment shipmentDto,
                                           FBDSDocument document,
                                           DocumentFormTypeCd documentFormTypeCd,
                                           FBDSDerivedAttributes fbdsAttributes,
                                           boolean supprRatesCharges,
                                           boolean restrictedCustomer,
                                           boolean displayRatesCharges) {
		
		// Credit status line
		if (documentFormTypeCd == DocumentFormTypeCd.FBDS
			&& StringUtils.isEmpty(document.getMovrClearanceBillText())) {
			if(fbdsAttributes != null && StringUtils.isNotBlank(fbdsAttributes.collectMoneyAtDeliveryCd)
					&& !fbdsAttributes.collectMoneyAtDeliveryCd.equals(COLL_MONEY_CREDIT_PP)) {
				//set Driver collect (credit status) on the document
				if (fbdsAttributes.cashOnly) {
					document.setDriverCollectDescription(DRIVER_COLLECT_CASH);
				} else {
					document.setDriverCollectDescription(DRIVER_COLLECT);
				}
			}
		}

		if(CollectionUtils.isEmpty(miscLineItems)) {
    		return document;
    	}
    	
    	RateAndCharge rc = new RateAndCharge();
    	List<ShipmentCharge> scList = Lists.newArrayList();

    	if (displayRatesCharges) {

    		for (MiscLineItem lineItem : miscLineItems) {

        		switch(lineItem.getLineTypeCd()) {

    	    		case AS_WGT:
    	    			setAsWgtDeficitWgt(lineItem, MiscLineItemCd.AS_WGT, document, rc);
    	    			break;
    	    		case DEFICIT_WGT:
    	    			setAsWgtDeficitWgt(lineItem, MiscLineItemCd.DEFICIT_WGT, document, rc);
    	    			break;
    	    		case DISC_LN:
    	    			setReducedCharge(lineItem, document, rc);
    	    			break;

    	    		default:
        		}

        	}

    	}

    	for (MiscLineItem lineItem : miscLineItems) {

    		switch(lineItem.getLineTypeCd()) {

    			case PART_PPD_LN:
                    if (documentFormTypeCd == DocumentFormTypeCd.COPY_BILL)
                        setPartPrepaidCharges(lineItem, document, scList, restrictedCustomer);
	    			break;
	    		case CASH_PPD_LN:
                    if (documentFormTypeCd == DocumentFormTypeCd.COPY_BILL)
	    				setCashPrepaidCharges(lineItem, document, scList, restrictedCustomer);
	    			break;
	    		case PART_COLL_LN:
				setPartCollectCharges(lineItem, shipmentDto, document, scList, fbdsAttributes, restrictedCustomer);
	    			break;
	    		case COD_AMT:
	    			setCodFreightCharges(lineItem, miscLineItems, shipmentDto, document, rc,
	    					scList, displayRatesCharges, fbdsAttributes, restrictedCustomer);
	    			break;

	    		default:
    		}
    	}

    	if (CollectionUtils.isNotEmpty(scList)) {
			rc.setShipmentCharges(scList);
		}

    	if (rc != null) {
			document.setRatesAndCharges(rc);
		}


    	return document;
    }


    private static void setAsWgtDeficitWgt(MiscLineItem lineItem, MiscLineItemCd itemCd, FBDSDocument document, RateAndCharge rc) {

    	RatedAsWeight rw = new RatedAsWeight();
    	//set description on document
    	if (itemCd.equals(MiscLineItemCd.AS_WGT)) {
    		rw.setRatedAsWeightDescription(lineItem.getDescription());
    	} else {
    		rw.setRatedAsWeightDescription(String.format("%s%s", DEFICIT_WEIGHT_DESC_PREFIX, lineItem.getDescription()));
    	}

    	//set quantity
    	rw.setQuantity(new BigDecimal(lineItem.getQuantity()));

    	//set tariff rate
    	if (lineItem.getMinimumChargeInd()) {
    		//set tariff txt to 'M'
    		rw.setTariffRateTxt(TARIFF_RATE_M);
    	} else {
        	//set tariff txt with bdRate
    		rw.setTariffRateTxt(lineItem.getTariffsRate().toString());
    	}

    	//set amount
    	BigDecimal bdAmt = getAmount(lineItem.getAmount(), 2);
    	rw.setAmount(bdAmt);

    	rc.setRatedAsWeight(rw);
    }

    private static void setReducedCharge(MiscLineItem lineItem, FBDSDocument document, RateAndCharge rc) {

    	ReducedCharge rcge = new ReducedCharge();
    	//set reduced charge text on desc
    	rcge.setReducedChargeText(String.format("%s%s", RC_CREDIT, lineItem.getDescription()));
    	//set amount
    	if (lineItem.getAmount() > 0) {
    		BigDecimal bdAmt = getAmount(lineItem.getAmount(), 2);
    		rcge.setAmount(bdAmt);
    	} else {
    		//set to 0.00
    		rcge.setAmount(SET_TO_ZEROS);
    	}

    	rc.setReducedCharge(rcge);
    }

    private static void setPartPrepaidCharges(
			MiscLineItem lineItem, FBDSDocument document, List<ShipmentCharge> scList, boolean restrictedCustomer) {

		if (!restrictedCustomer && (lineItem.getAmount() > 0 || !StringUtils.SPACE.equals(lineItem.getDescription()))) {

    		ShipmentCharge sc = new ShipmentCharge();
    		sc.setChargeType(PART_PREPAID);
    		//set type_cd to PPD
    		sc.setPaymentType(PPD);
    		//set desc
    		sc.setDescription(lineItem.getDescription());
    		//set amt
        	BigDecimal bdAmt = getAmount(lineItem.getAmount(), 2);
        	sc.setAmount(bdAmt);
        	scList.add(sc);
    	}
    }

    private static void setCashPrepaidCharges(
    		MiscLineItem lineItem, FBDSDocument document, List<ShipmentCharge> scList, boolean restrictedCustomer) {

    	if (lineItem.getAmount() > 0 && !restrictedCustomer) {
    		ShipmentCharge sc = new ShipmentCharge();
    		sc.setChargeType(CASH_PREPAID);
    		//set desc
    		sc.setDescription(lineItem.getDescription());
    		//set amt
    		BigDecimal bdAmt = getAmount(lineItem.getAmount(), 2);
    		sc.setAmount(bdAmt);
    		scList.add(sc);
    	}
    }

    private static void setPartCollectCharges(
			MiscLineItem lineItem, Shipment shipmentDto, FBDSDocument document, List<ShipmentCharge> scList,
			FBDSDerivedAttributes fbdsAttributes, boolean restrictedCustomer) {

		if (!isSetPartCollectDisplay(fbdsAttributes, restrictedCustomer)
				&& (lineItem.getAmount() > 0 || !StringUtils.SPACE.equals(lineItem.getDescription()))) {
    		ShipmentCharge sc = new ShipmentCharge();
    		sc.setChargeType(PART_COLLECT);
    		//set type_cd to COL
    		sc.setPaymentType(COL);
    		//set desc
    		sc.setDescription(lineItem.getDescription());

    		if (lineItem.getAmount() > 0) {
    			//set amt
            	BigDecimal bdAmt = getAmount(lineItem.getAmount(), 2);
            	sc.setAmount(bdAmt);
    		} else if(shipmentDto.getGuaranteedInd()) {
    			//set amt to 0.00
    			sc.setAmount(SET_TO_ZEROS);
    		}
    		scList.add(sc);
    	}
    }

	private static boolean isSetPartCollectDisplay(FBDSDerivedAttributes fbdsAttributes, boolean restrictedCustomer) {
		return restrictedCustomer && (fbdsAttributes.collectMoneyAtDeliveryCd == COLL_MONEY_CREDIT_PP
				|| fbdsAttributes.collectMoneyAtDeliveryCd == COLL_MONEY_COD);
	}

    private void setCodFreightCharges(MiscLineItem lineItem, List<MiscLineItem> miscLineItems,
    		Shipment shipmentDto, FBDSDocument document, RateAndCharge rc, List<ShipmentCharge> scList,
    		boolean displayRatesCharges, FBDSDerivedAttributes fbdsAttributes, boolean restrictedCustomer) {

    	if (shipmentDto.getCodInd()) {

    		ShipmentCharge sc = new ShipmentCharge();
    		BigDecimal bdAmt = null;
    		CashOnDelivery cod = new CashOnDelivery();

    		//set description1. COD description of payment method
    		cod.setDescription1(String.format("%s%s%s", "*** ",lineItem.getDescription()," ***"));

    		//set description2. COD amount text line description
    		cod.setDescription2(COD_AMOUNT);

    		if (lineItem.getAmount() > 0) {
    			//set amt
        		bdAmt = getAmount(lineItem.getAmount(), 2);
        		cod.setAmount(bdAmt);
    		} else if(shipmentDto.getGuaranteedInd()) {
    			//set amt to 0.00
    			cod.setAmount(SET_TO_ZEROS);
    		}

    		rc.setCashOnDelivery(cod);

    		//Freight charges :
    		sc.setChargeType(FREIGHT_CHARGE);
    		//set description to "FREIGHT CHARGE", for freight charge amount text line
    		sc.setDescription(FREIGHT_CHARGE_DESC);

    		//get part collect amount from the relevant line item

    		Double d = CollectionUtils.emptyIfNull(miscLineItems)
    		.stream()
    		.filter(line -> MiscLineItemCd.PART_COLL_LN.equals(line.getLineTypeCd()))
    		.filter(Objects::nonNull)
    		.map(MiscLineItem::getAmount)
    		.findFirst()
    		.orElse(null);

    		BigDecimal partCollectAmt = null;
    		if (d != null) {
    			partCollectAmt = new BigDecimal(d);
    			partCollectAmt = getAmount(partCollectAmt.doubleValue(), 2);
    		}

    		//If ChargeToCd is 'C' set payment type to COL
    		if (ChargeToCd.COLL.equals(shipmentDto.getChargeToCd())) {
    			sc.setPaymentType(COL);
    		} else {
    			if (Objects.nonNull(partCollectAmt) && (partCollectAmt.subtract(bdAmt).compareTo(new BigDecimal("0")) == 0)) {
    				sc.setPaymentType(PPD);
    			} else {
    				sc.setPaymentType(COL);
    			}
    		}

    		//set freight charge amount
    		if (displayRatesCharges || (!displayRatesCharges &&
    									(fbdsAttributes.collectMoneyAtDeliveryCd.equals(COLL_MONEY_NOT_CREDIT_PP)
    											|| (fbdsAttributes.collectMoneyAtDeliveryCd.equals(COLL_MONEY_COD)
    													&& !restrictedCustomer
    													)
    											|| fbdsAttributes.collectMoneyAtDeliveryCd.equals(COLL_MONEY_BOTH_BILL))
    									&& (fbdsAttributes.accessoriesExistsOnBothBill || COL.equals(sc.getPaymentType())))) {

    			if (ChargeToCd.BOTH.equals(shipmentDto.getChargeToCd())) {
    				if (Objects.nonNull(partCollectAmt)) {
    					sc.setAmount(partCollectAmt.subtract(cod.getAmount()).setScale(2, RoundingMode.HALF_UP));
    				}
    			} else {
    				BigDecimal totalChrgeAmt = new BigDecimal(shipmentDto.getTotalChargeAmount());
    				sc.setAmount(totalChrgeAmt.subtract(cod.getAmount()).setScale(2, RoundingMode.HALF_UP));
    			}

    		} else {
    			sc.setAmount(SET_TO_ZEROS);
    		}

    		scList.add(sc);

    	}
    }

    private static BigDecimal getAmount(Double d, int scale) {
    	return new BigDecimal(d).setScale(scale, RoundingMode.HALF_UP);
    }

    public static String formatFBDSPro(String proNbr) {
    	if (StringUtils.isNotBlank(proNbr) && proNbr.startsWith("0") && proNbr.length() == 11) {
    		proNbr = proNbr.substring(1);
    		proNbr = String.format("%s%s%s", proNbr.substring(0, 3), "-", proNbr.substring(4));
		}
    	return proNbr;
    }
    
    public static String formatFBDSProElevenDigit(String proNbr) {
    	if (StringUtils.isNotBlank(proNbr) && proNbr.length() >= 4 && proNbr.charAt(3) == '-') {
    		proNbr = String.format("0%s0%s", proNbr.substring(0, 3), proNbr.substring(4));
		}
    	return proNbr;
    }

    /**
     * Method to determine if Rates and Charges can be displayed on FBDS document.
     */
    public boolean displayRatesAndCharges
            (boolean isRestricted,
             Shipment shipmentDto,
             DocumentFormTypeCd documentFormTypeCd,
             FBDSDerivedAttributes fbdsAttributes,
             boolean suppressRatesCharges,
             Map<MatchedPartyTypeCd, List<AsMatchedParty>> shipmentAsMatchedPartyMap) {
    	boolean display = false;
    	String typeCd = null;

        if (documentFormTypeCd == DocumentFormTypeCd.FBDS && !shipmentAsMatchedPartyMap.isEmpty()) {
    		if (!(BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
        			|| DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd()))) {

        		if (!isRestricted) {

        			switch(shipmentDto.getChargeToCd()) {

        			case BOTH:
        				if(fbdsAttributes.chargeToCdFound != null
        				&& ChargeToCd.PPD.equals(fbdsAttributes.chargeToCdFound)) {
        					display = false;
        				} else if ((fbdsAttributes.chargeToCdFound != null
        						&& !ChargeToCd.PPD.equals(fbdsAttributes.chargeToCdFound))
        						&& shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.BILL_TO_INB)
        						&& (StringUtils.isNotBlank(fbdsAttributes.collectMoneyAtDeliveryCd)
        								&& COLL_MONEY_CREDIT_PP.equals(fbdsAttributes.collectMoneyAtDeliveryCd))
        						&& !suppressRatesCharges) {
        					display = true;
        				}
        				break;
        			case COLL:
        				if (shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.BILL_TO_INB)
        						|| shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.BILL_TO_OTB)) {
        					
        					if (shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.BILL_TO_INB)
        							&& StringUtils.isNotBlank(shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.BILL_TO_INB).get(0).getTypeCd().value()))
        						typeCd = shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.BILL_TO_INB).get(0).getTypeCd().value();
        					else if(shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.BILL_TO_OTB)
        							&& StringUtils.isNotBlank(shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.BILL_TO_OTB).get(0).getTypeCd().value()))
        						typeCd = shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.BILL_TO_OTB).get(0).getTypeCd().value();

        					if (StringUtils.isNotBlank(typeCd) && (StringUtils.isNotBlank(fbdsAttributes.collectMoneyAtDeliveryCd)
    								&& COLL_MONEY_CREDIT_PP.equals(fbdsAttributes.collectMoneyAtDeliveryCd))) {
        						display = !suppressRatesCharges ? true: false;
        					} else {
        						//if collectMoneyAtDlvry is not 'N'
        						display = true;
        					}

        				} else {
        					//If not Bill-to
        					display = true;
        				}
        				break;
        			case PPD:
        				display = false;
        				break;

        			default:
        			}

        		}

    		}

    	} else {
			//If CopyBill, always display Rates&Charges
			display = true;
		}


    	return display;
    }


    public static boolean isAppointment(DeliveryNotification notification) {
		return Objects.nonNull(notification) && StringUtils.equals(notification.getNotificationCategoryCd(), "A");
	}

    public static String buildNotificationDate(DeliveryNotification mostRecentNotification, boolean isAppointment) {
		if (Objects.isNull(mostRecentNotification)) {
			return null;
		}

		Date notificationDate = null;

		if (isAppointment) {
			if (StringUtils.isNotBlank(mostRecentNotification.getScheduledDeliveryFromTime())) {
				notificationDate = BasicTransformer.toDate(mostRecentNotification.getScheduledDeliveryDate());
			}
		} else {
			if (Objects.nonNull(mostRecentNotification.getCallDateTime())) {
				notificationDate = BasicTransformer.toDate(mostRecentNotification.getCallDateTime());
			}
		}

		return Objects.nonNull(notificationDate) ? DateFormatUtils.format(notificationDate, "M/dd") : null;
	}

	public static String buildNotificationTime(DeliveryNotification mostRecentNotification, boolean isAppointment) {
		if (Objects.isNull(mostRecentNotification)) {
			return null;
		}

		String notificationTime = null;

		if (isAppointment) {
			final String fromTime = mostRecentNotification.getScheduledDeliveryFromTime();
			if (hasAppointmentTime(fromTime)) {
				notificationTime = LocalTime.parse(fromTime)
						.format(NOTIFICATION_TIME_FORMATTER);
			}

			final String toTime = mostRecentNotification.getScheduledDeliveryToTime();
			if (hasAppointmentTime(toTime)) {
				final String formattedToTime = LocalTime.parse(toTime).format(NOTIFICATION_TIME_FORMATTER);
				notificationTime = StringUtils.isNotBlank(notificationTime)
						? notificationTime + "-" + formattedToTime
						: formattedToTime;
			}
		} else {
			if (Objects.nonNull(mostRecentNotification.getCallDateTime())) {
				notificationTime = DateFormatUtils.format(
						BasicTransformer.toDate(mostRecentNotification.getCallDateTime()), NOTIFICATION_TIME_FORMAT);
			}
		}

		return notificationTime;
	}

	private static boolean hasAppointmentTime(String time) {
		return StringUtils.isNotBlank(time) && !StringUtils.equals(time, APPOINTMENT_TIME_ZEROES);
	}

	 /**
     * Method to determine if Accessorials need to be suppressed on FBDS/CopyBill document.
     */
    public boolean suppressAccessorialDisplay(AccessorialService accessorial,
                                              Shipment shipmentDto,
                                              boolean isCustomerRestricted,
                                              boolean displayRatesCharges,
                                              DocumentFormTypeCd documentFormTypeCd) {
        if (documentFormTypeCd == DocumentFormTypeCd.FBDS) {
    		if (BLIND_SHIPMENT_CODE.equals(accessorial.getAccessorialCd())) {
    			return true;
    		}

    		if (displayRatesCharges
    				|| (ChargeToCd.BOTH.equals(shipmentDto.getChargeToCd())
    						&& !isCustomerRestricted)) {

    			if (StringUtils
						.containsAny(
								accessorial.getAccessorialCd(),
								FSC_ACCS_CODE,
								OHF_ACCS_CODE,
								WHF_ACCS_CODE,
								WAF_ACCS_CODE)
						&& (accessorial.getPrepaidPercentage() == HUNDRED_PERCENTAGE)) {
    				return true;
    			}

    			if (StringUtils
						.containsAny(
								accessorial.getDescription(),
								FSC_ACCS_CODE,
								FUEL_ACCS_CODE)
						&& (accessorial.getPrepaidPercentage() == HUNDRED_PERCENTAGE)) {
    				return true;
    			}

    		} else {

    			if (StringUtils
						.containsAny(
								accessorial.getAccessorialCd(),
								FSC_ACCS_CODE,
								OHF_ACCS_CODE,
								WHF_ACCS_CODE,
								WAF_ACCS_CODE)) {
    				return true;
    			}

    			if (StringUtils
						.containsAny(
								accessorial.getDescription(),
								FSC_ACCS_CODE,
								FUEL_ACCS_CODE)) {
    				return true;
    			}

    		}
    	}

    	//If CopyBill, accessorials are not suppressed
    	return false;
    }

    /**
     * Method to set FBDS Total counts.
     */
    public static void setTotalCounts(boolean printClearanceBill,
                                      Shipment shipmentDto,
                                      FBDSDocument documentDetails,
                                      DocumentFormTypeCd documentFormTypeCd,
                                      boolean displayRatesCharges,
                                      boolean accessorialsSuppressed,
                                      FBDSDerivedAttributes fbdsAttributes) {
		if (!printClearanceBill
				&& (!BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
				|| !DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd()))) {

			documentDetails.setTotalChargeAmountTextLine1(TOTAL);
			if (ChargeToCd.PPD.equals(shipmentDto.getChargeToCd())) {
				documentDetails.setTotalChargeAmountTextLine2(ChargeToCd.PPD.value().toUpperCase());
			} else if (ChargeToCd.COLL.equals(shipmentDto.getChargeToCd())) {
				documentDetails.setTotalChargeAmountTextLine2(ChargeToCd.COLL.value().toUpperCase());
			} else if (ChargeToCd.BOTH.equals(shipmentDto.getChargeToCd())) {
				documentDetails.setTotalChargeAmountTextLine2(ChargeToCd.BOTH.value().toUpperCase());
			} else {
				documentDetails.setTotalChargeAmountTextLine2(StringUtils.SPACE);
			}

			if ((BillStatusCd.UNBILLED.equals(shipmentDto.getBillStatusCd())
					|| BillStatusCd.BILLED.equals(shipmentDto.getBillStatusCd())
					|| BillStatusCd.IN_FBES_SUSPENSE_QUEUE.equals(shipmentDto.getBillStatusCd()))
					&& shipmentDto.getRevenueBillInd()) {

                if (documentFormTypeCd == DocumentFormTypeCd.COPY_BILL)
					documentDetails.setTotalAmount(UNRATED);

			} else if ((displayRatesCharges && !accessorialsSuppressed)
					|| (COLL_MONEY_NOT_CREDIT_PP.equals(fbdsAttributes.collectMoneyAtDeliveryCd)
							|| (COLL_MONEY_BOTH_BILL.equals(fbdsAttributes.collectMoneyAtDeliveryCd)
									&& ChargeToCd.COLL.equals(shipmentDto.getChargeToCd())))) {
				if (shipmentDto.getTotalChargeAmount() != 0) {
					documentDetails.setTotalAmount(String.valueOf(shipmentDto.getTotalChargeAmount()));
				} else {
					if (shipmentDto.getGuaranteedInd())
						documentDetails.setTotalAmount(String.valueOf(SET_TO_ZEROS));
				}

			}

			if (!shipmentDto.getTotalPiecesCount().equals(BigInteger.ZERO))
				documentDetails.setTotalPieces(shipmentDto.getTotalPiecesCount());

			if (shipmentDto.getTotalWeightLbs() != 0)
				documentDetails.setTotalWeight(shipmentDto.getTotalWeightLbs());
		}

        if (documentFormTypeCd == DocumentFormTypeCd.FBDS
            && ((BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
                 && printClearanceBill)
                || DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd()))) {

			documentDetails.setTotalAmount(StringUtils.SPACE);
			documentDetails.setTotalChargeAmountTextLine2(StringUtils.SPACE);
			documentDetails.setTotalChargeAmountTextLine1(StringUtils.SPACE);
			documentDetails.setTotalPieces(null);
			documentDetails.setTotalWeight(null);

		}
    }

    public static String getWarrantyText(Shipment shipmentDto) {
		return Objects.nonNull(shipmentDto) && BooleanUtils.isTrue(shipmentDto.getWarrantyInd())
				? WARRANTY_TEXT
				: null;
    }

    public static String getFreezableText(Shipment shipmentDto) {
		return Objects.nonNull(shipmentDto) && BooleanUtils.isTrue(shipmentDto.getFreezableInd())
				? FREEZABLE_TEXT
				: null;
    }

    /**
     * Method to set Commodities on FBDS doc.
     */
    public static void setCommodities(FBDSDocument document,
                                      List<Commodity> commodities,
                                      Shipment shipmentDto,
                                      DocumentFormTypeCd documentFormTypeCd,
                                      Long excpPiecesCount,
                                      boolean printClearanceBill,
                                      boolean displayRatesCharges) {
        if (documentFormTypeCd == DocumentFormTypeCd.FBDS) {
    		if (DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd())) {
    			document.setTotalPieces(BasicTransformer.toBigInteger(excpPiecesCount));
    			document.setTotalChargeAmountTextLine1(PCS_TO_APPLY_TO_SHORTAGE);
    			//If FBDS and Pro is PART_SHORT, then should not display Commodities
    			return;
    		}

    		if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())) {

    			if (printClearanceBill) {
    				document.setTotalPieces(BasicTransformer.toBigInteger(excpPiecesCount));
        			document.setTotalChargeAmountTextLine1(PCS_TO_APPLY_TO_SHORTAGE);
    			}
    			setCommodityPiecesWeightDesc(commodities, displayRatesCharges, shipmentDto);
        	} else {
        		setCommodityPiecesWeightDesc(commodities, displayRatesCharges, shipmentDto);
        	}

    	} else {
    		//for CopyBill
    		setCommodityPiecesWeightDesc(commodities, displayRatesCharges, shipmentDto);

    	}
    	document.setCommodities(commodities);
    }

   private static List<Commodity> setCommodityPiecesWeightDesc(List<Commodity> commodities, boolean displayRatesCharges, Shipment shipmentDto) {

	   //whether request is FBDS or CopyBill
	   if(CollectionUtils.isNotEmpty(commodities)) {
		   for (Commodity cmdty : commodities) {
			   //display only if pieces count is > 0
			   if (cmdty.getPiecesCount().compareTo(BigInteger.ZERO) != 1)
				   cmdty.setPiecesCount(null);
			   //display only if WeightLbs is > 0
			   if (cmdty.getWeightLbs() <= 0)
				   cmdty.setWeightLbs(null);

			   setCommoditiesDescAmounts(cmdty, displayRatesCharges, shipmentDto);
		   }
	   }

	   return commodities;
   }

   private static Commodity setCommoditiesDescAmounts(Commodity cmdty, boolean displayRatesCharges, Shipment shipmentDto) {

	   cmdty.setOriginalDescription(null);

	   if (StringUtils.isNotBlank(cmdty.getNmfcItemCd())) {
		   cmdty.setDescription(String.format(
					"%s %s %s %s", cmdty.getPackageCd(), cmdty.getDescription(), cmdty.getNmfcItemCd(),
					getCommodityClassType(cmdty.getClassType().value())).toUpperCase());
	   } else {
		   cmdty.setDescription(String.format(
					"%s %s %s", cmdty.getPackageCd(), cmdty.getDescription(),
					getCommodityClassType(cmdty.getClassType().value())).toUpperCase());
	   }

		if (displayRatesCharges) {

            if (cmdty.getMinimumChargeInd()) {

                if (cmdty.getTariffsRate() <= 0) {
                    cmdty.setTariffsRate(SET_TO_ZEROS.doubleValue());
                } else {
                    cmdty.setTariffsRate(cmdty.getTariffsRate());
                }
			}

			if (cmdty.getAmount() != 0)
				cmdty.setAmount(cmdty.getAmount());

			if (cmdty.getAmount() == 0 && shipmentDto.getGuaranteedInd())
				cmdty.setAmount(SET_TO_ZEROS.doubleValue());

		} else {
			cmdty.setTariffsRate(null);
			cmdty.setAmount(null);
		}

		return cmdty;
   }

	private static String getCommodityClassType(String classType) {
		classType = classType.replace(CLSS_TYPE, COMMODITY_CLASS_TYPE);
		return classType;
	}

	/**
	 * Add {@link DataValidationError} about the issue encountered for a specific pro number
	 * @param proNbr
	 * @param message
	 * @return
	 */
	public static DataValidationError createDataValidationError(String proNbr, String message) {
		DataValidationError error = new DataValidationError();
		error.setFieldName("proNbr");
		error.setFieldValue(proNbr);
		error.setMessage(message);
		
		return error;
	}
}