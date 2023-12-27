package com.xpo.ltl.shipment.service.transformers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPass;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPassPK;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.RatingInformationPass;
import com.xpo.ltl.api.transformer.AbstractEntityBeanUtils;
import com.xpo.ltl.api.transformer.BasicTransformer;

public class RatingInformationPassTransformer {

	private static final DB2ShmRtgInfoPassValidator VALIDATOR = new DB2ShmRtgInfoPassValidator();

	public static DB2ShmRtgInfoPass toDb2Entity(final RatingInformationPass pass) throws ValidationException {
		if (pass == null) {
			return null;
		}
		final DB2ShmRtgInfoPass entity = new DB2ShmRtgInfoPass();

		updateDb2Entity(pass, entity);

		return entity;
	}

	public static DB2ShmRtgInfoPass updateDb2Entity(
		final RatingInformationPass pass,
		final DB2ShmRtgInfoPass entity) throws ValidationException {
		if (pass == null) {
			return entity;
		}
        if (pass.getAbsoluteMinChargeInd() != null) {
            entity.setAbsMinChgInd(BasicTransformer.toString(pass.getAbsoluteMinChargeInd()));
		}
		if (pass.getFscTariffName() != null) {
			entity.setFscTrfNmTxt(pass.getFscTariffName());
		}
		if (pass.getOffshoreTotalAccessorialAmount() != null) {
			entity.setOfshrTotAcAmt(BasicTransformer.toBigDecimal(pass.getOffshoreTotalAccessorialAmount()));
		}
		if (pass.getOffshoreTotalFscAmount() != null) {
			entity.setOfshrTotFscAmt(BasicTransformer.toBigDecimal(pass.getOffshoreTotalFscAmount()));
		}
		if (pass.getOffshoreTotalLinehaulChargeAmount() != null) {
			entity.setOfshrTotLnhlChrgAmt(BasicTransformer.toBigDecimal(pass.getOffshoreTotalLinehaulChargeAmount()));
		}
		if (pass.getPriceAgreementId() != null) {
			entity.setPrcAgrmtId(BasicTransformer.toBigDecimal(pass.getPriceAgreementId()));
		}
		if (pass.getPriceRulesetNbr() != null) {
            entity.setPrcRulesetNbr(BasicTransformer.toShort(pass.getPriceRulesetNbr()));
		}
		if (pass.getRatingTariffName() != null) {
			entity.setRtgTrfNmTxt(pass.getRatingTariffName());
		}
		if (pass.getRatingTariffVersion() != null) {
			entity.setRtgTrfVer(BasicTransformer.toInt(pass.getRatingTariffVersion()));
		}
		if (pass.getRulesTariffName() != null) {
			entity.setRulesTrfNmTxt(pass.getRulesTariffName());
		}
		if (pass.getRulesTariffVersion() != null) {
			entity.setRulesTrfVer(BasicTransformer.toInt(pass.getRulesTariffVersion()));
		}
		if (pass.getTotalAccessorialAmount() != null) {
			entity.setTotAcAmt(BasicTransformer.toBigDecimal(pass.getTotalAccessorialAmount()));
		}
		if (pass.getTotalChargeAmount() != null) {
			entity.setTotChrgAmt(BasicTransformer.toBigDecimal(pass.getTotalChargeAmount()));
		}
		if (pass.getTotalDiscountAmount() != null) {
			entity.setTotDscntAmt(BasicTransformer.toBigDecimal(pass.getTotalDiscountAmount()));
		}
		if (pass.getTotalFscAmount() != null) {
			entity.setTotFscAmt(BasicTransformer.toBigDecimal(pass.getTotalFscAmount()));
		}
		if (pass.getTotalLinehaulChargeAmount() != null) {
			entity.setTotLnhlChrgAmt(BasicTransformer.toBigDecimal(pass.getTotalLinehaulChargeAmount()));
		}
		if (pass.getTotalTaxAmount() != null) {
			entity.setTotTaxAmt(BasicTransformer.toBigDecimal(pass.getTotalTaxAmount()));
		}

		final DB2ShmRtgInfoPassPK id = new DB2ShmRtgInfoPassPK();
		id.setPassTypCd(pass.getPassTypeCd());
		if (pass.getShipmentInstId() != null) {
			id.setShpInstId(pass.getShipmentInstId());
		}
		entity.setId(id);

		// audit information
		if (pass.getAuditInfo() != null) {
			entity.setLstUpdtBy(pass.getAuditInfo().getUpdateById());
			entity.setLstUpdtTmst(BasicTransformer.toTimestamp(pass.getAuditInfo().getUpdatedTimestamp()));
			entity.setLstUpdtTranCd(pass.getAuditInfo().getUpdateByPgmId());
		}

		VALIDATOR.validate(entity);
		return entity;
	}

	public static RatingInformationPass toDto(final DB2ShmRtgInfoPass pass) {
		if (pass == null) {
			return null;
		}
		final RatingInformationPass dto = new RatingInformationPass();

        dto.setAbsoluteMinChargeInd(BasicTransformer.toBoolean(pass.getAbsMinChgInd()));
		final AuditInfo auditInfo = new AuditInfo();
		auditInfo.setUpdateById(pass.getLstUpdtBy());
		auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar(pass.getLstUpdtTmst()));
		auditInfo.setUpdateByPgmId(pass.getLstUpdtTranCd());
		dto.setAuditInfo(auditInfo);
		dto.setFscTariffName(pass.getFscTrfNmTxt());
		dto.setOffshoreTotalAccessorialAmount(BasicTransformer.toDouble(pass.getOfshrTotAcAmt()));
		dto.setOffshoreTotalFscAmount(BasicTransformer.toDouble(pass.getOfshrTotFscAmt()));
		dto.setOffshoreTotalLinehaulChargeAmount(BasicTransformer.toDouble(pass.getOfshrTotLnhlChrgAmt()));
		if (pass.getId() != null) {
			dto.setPassTypeCd(pass.getId().getPassTypCd());
			dto.setShipmentInstId(pass.getId().getShpInstId());
		}
		dto.setPriceAgreementId(BasicTransformer.toLong(pass.getPrcAgrmtId()));
        dto.setPriceRulesetNbr(BasicTransformer.toBigInteger(pass.getPrcRulesetNbr()));
		dto.setRatingTariffName(pass.getRtgTrfNmTxt());
        dto.setRatingTariffVersion(BasicTransformer.toLong(pass.getRtgTrfVer()));
		dto.setRulesTariffName(pass.getRulesTrfNmTxt());
        dto.setRulesTariffVersion(BasicTransformer.toLong(pass.getRulesTrfVer()));
		dto.setTotalAccessorialAmount(BasicTransformer.toDouble(pass.getTotAcAmt()));
		dto.setTotalChargeAmount(BasicTransformer.toDouble(pass.getTotChrgAmt()));
		dto.setTotalDiscountAmount(BasicTransformer.toDouble(pass.getTotDscntAmt()));
		dto.setTotalFscAmount(BasicTransformer.toDouble(pass.getTotFscAmt()));
		dto.setTotalLinehaulChargeAmount(BasicTransformer.toDouble(pass.getTotLnhlChrgAmt()));
		dto.setTotalTaxAmount(BasicTransformer.toDouble(pass.getTotTaxAmt()));
		return dto;
	}

	private static class DB2ShmRtgInfoPassValidator extends AbstractEntityBeanUtils<DB2ShmRtgInfoPass> {

		@Override
		protected DB2ShmRtgInfoPass newEntity() {
			return new DB2ShmRtgInfoPass();
		}

		@Override
		protected List<MoreInfo> validate(final DB2ShmRtgInfoPass entity, final Integer itemNbr) {
			final List<MoreInfo> errors = new ArrayList<>();
			if (entity != null) {
				final String entityName = getEntityName();
				DB2ShmRtgInfoPassPK id = entity.getId();
				if (id == null) {
					id = new DB2ShmRtgInfoPassPK();
				}
				validate(
					errors,
					entityName,
					"shpInstId",
					itemNbr,
					id.getShpInstId(),
					-999999999999999L,
					999999999999999L,
					false);
				validate(errors, entityName, "passTypCd", itemNbr, id.getPassTypCd(), 1, false);
				validate(errors, entityName, "absMinChgInd", itemNbr, entity.getAbsMinChgInd(), 1, false);
				validate(errors, entityName, "fscTrfNmTxt", itemNbr, entity.getFscTrfNmTxt(), 7, false);
				validate(errors, entityName, "lstUpdtBy", itemNbr, entity.getLstUpdtBy(), 15, false);
				isNull(errors, entityName + ".lstUpdtTmst", itemNbr, entity.getLstUpdtTmst(), false);
				validate(errors, entityName, "lstUpdtTranCd", itemNbr, entity.getLstUpdtTranCd(), 8, false);
				validate(errors, entityName, "rtgTrfNmTxt", itemNbr, entity.getRtgTrfNmTxt(), 6, false);
				validate(errors, entityName, "rulesTrfNmTxt", itemNbr, entity.getRulesTrfNmTxt(), 7, false);
				validate(
					errors,
					entityName,
					"ofshrTotAcAmt",
					itemNbr,
					entity.getOfshrTotAcAmt(),
					-99999999999.99,
					99999999999.99,
					false);
				validate(
					errors,
					entityName,
					"ofshrTotFscAmt",
					itemNbr,
					entity.getOfshrTotFscAmt(),
					-99999.99,
					99999.99,
					false);
				validate(
					errors,
					entityName,
					"ofshrTotLnhlChrgAmt",
					itemNbr,
					entity.getOfshrTotLnhlChrgAmt(),
					-99999999999.99,
					99999999999.99,
					false);
				validate(
					errors,
					entityName,
					"prcAgrmtId",
					itemNbr,
					entity.getPrcAgrmtId(),
					-999999999999999L,
					999999999999999L,
					false);
				validate(errors, entityName, "prcRulesetNbr", itemNbr, entity.getPrcRulesetNbr(), 0, 9999, false);
				validate(
					errors,
					entityName,
					"rtgTrfVer",
					itemNbr,
					entity.getRtgTrfVer(),
					-9999999999999L,
					9999999999999L,
					false);
				validate(
					errors,
					entityName,
					"rulesTrfVer",
					itemNbr,
					entity.getRulesTrfVer(),
					-9999999999999L,
					9999999999999L,
					false);
				validate(
					errors,
					entityName,
					"totAcAmt",
					itemNbr,
					entity.getTotAcAmt(),
					-99999999999.99,
					99999999999.99,
					false);
				validate(
					errors,
					entityName,
					"totChrgAmt",
					itemNbr,
					entity.getTotChrgAmt(),
					-99999999999.99,
					99999999999.99,
					false);
				validate(
					errors,
					entityName,
					"totDscntAmt",
					itemNbr,
					entity.getTotDscntAmt(),
					-999999999.99,
					999999999.99,
					false);
				validate(
					errors,
					entityName,
					"totFscAmt",
					itemNbr,
					entity.getTotFscAmt(),
					-99999999999.99,
					99999999999.99,
					false);
				validate(
					errors,
					entityName,
					"totLnhlChrgAmt",
					itemNbr,
					entity.getTotLnhlChrgAmt(),
					-99999999999.99,
					99999999999.99,
					false);
				validate(
					errors,
					entityName,
					"totTaxAmt",
					itemNbr,
					entity.getTotTaxAmt(),
					-99999999999.99,
					99999999999.99,
					false);
			}
			return errors;
		}

		@Override
		protected String getEntityName() {
			return DB2ShmRtgInfoPass.class.getSimpleName();
		}

		@Override
		protected int[] getHashCodeParts(final DB2ShmRtgInfoPass entity) {
			if (entity == null)
				return new int[] { 0 };
			return new int[] { getHashCodePart(entity.getId().getShpInstId()),
					getHashCodePart(entity.getId().getPassTypCd()) };
		}

		@Override
		public Comparator<DB2ShmRtgInfoPass> getComparator() {
			return new Comparator<DB2ShmRtgInfoPass>() {
				@Override
				public int compare(final DB2ShmRtgInfoPass object1, final DB2ShmRtgInfoPass object2) {
					return Integer.valueOf(getHashCode(object1)).compareTo(getHashCode(object2));
				}
			};
		}
	};
}
