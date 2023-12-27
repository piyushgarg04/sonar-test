package com.xpo.ltl.shipment.service.transformers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPass;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPassPK;
import com.xpo.ltl.api.shipment.v2.RatingInformationPass;
import com.xpo.ltl.api.transformer.BasicTransformer;

public class RatingInformationPassTransformerTest {

	@Test
	public void toDb2EntityNull() throws Exception {
		assertThat(RatingInformationPassTransformer.toDb2Entity(null), Matchers.nullValue());
	}

	@Test
	public void toDtoNull() throws Exception {
		assertThat(RatingInformationPassTransformer.toDto(null), Matchers.nullValue());
	}

	@Test
	public void toEntityAllNull() {
		final RatingInformationPass dto = new RatingInformationPass();
		try {
			RatingInformationPassTransformer.toDb2Entity(dto);
			fail("Transformer should have failed on null fields");
		} catch (final ValidationException e) {
			final List<MoreInfo> moreInfo = e.getFault().getMoreInfo();
			assertThat(moreInfo.size(), Matchers.equalTo(18));
		}
	}

	@Test
	public void toDtoAllNull() {
		final DB2ShmRtgInfoPass pass = new DB2ShmRtgInfoPass();
		final RatingInformationPass dto = RatingInformationPassTransformer.toDto(pass);
		assertEquals(pass, dto);

	}

	@Test
	public void toDtoAndViceversa() throws Exception {
		DB2ShmRtgInfoPass pass = new DB2ShmRtgInfoPass();
		pass.setAbsMinChgInd("Y");
		pass.setFscTrfNmTxt("fsc");
		final DB2ShmRtgInfoPassPK id = new DB2ShmRtgInfoPassPK();
		id.setPassTypCd("T");
		id.setShpInstId(1l);
		pass.setId(id);
		pass.setLstUpdtBy("usr");
		pass.setLstUpdtTmst(new Timestamp(System.currentTimeMillis()));
		pass.setLstUpdtTranCd("tst");
		pass.setOfshrTotAcAmt(BigDecimal.valueOf(2.1));
		pass.setOfshrTotFscAmt(BigDecimal.valueOf(3.1));
		pass.setOfshrTotLnhlChrgAmt(BigDecimal.valueOf(4.1));
		pass.setPrcAgrmtId(BigDecimal.TEN);
		pass.setPrcRulesetNbr((short) 9);
		pass.setRtgTrfNmTxt("rtgtrf");
		pass.setRtgTrfVer(12);
		pass.setRulesTrfNmTxt("ruletrf");
		pass.setRulesTrfVer(13);
		pass.setTotAcAmt(BigDecimal.valueOf(5.1));
		pass.setTotChrgAmt(BigDecimal.valueOf(6.2));
		pass.setTotLnhlChrgAmt(BigDecimal.valueOf(7.1));
		pass.setTotTaxAmt(BigDecimal.valueOf(8.1));
		pass.setTotDscntAmt(BigDecimal.valueOf(9.1));
		pass.setTotFscAmt(BigDecimal.valueOf(10.1));

		final RatingInformationPass dto = RatingInformationPassTransformer.toDto(pass);
		assertEquals(pass, dto);

		pass = RatingInformationPassTransformer.toDb2Entity(dto);
		assertEquals(pass, dto);
	}

	private void assertEquals(final DB2ShmRtgInfoPass pass, final RatingInformationPass dto) {
		assertThat(
            dto.getAbsoluteMinChargeInd(),
			Matchers.equalTo(BasicTransformer.toBoolean(pass.getAbsMinChgInd())));
		assertThat(dto.getAuditInfo().getUpdateById(), Matchers.equalTo(pass.getLstUpdtBy()));
		if (pass.getLstUpdtTmst() != null) {
			assertThat(dto.getAuditInfo().getUpdatedTimestamp(), Matchers.notNullValue());
		} else {
			assertThat(dto.getAuditInfo().getUpdatedTimestamp(), Matchers.nullValue());
		}
		assertThat(dto.getAuditInfo().getUpdateByPgmId(), Matchers.equalTo(pass.getLstUpdtTranCd()));
		assertThat(dto.getFscTariffName(), Matchers.equalTo(pass.getFscTrfNmTxt()));
		assertThat(
			dto.getOffshoreTotalAccessorialAmount(),
			Matchers.equalTo(BasicTransformer.toDouble(pass.getOfshrTotAcAmt())));
		assertThat(
			dto.getOffshoreTotalFscAmount(),
			Matchers.equalTo(BasicTransformer.toDouble(pass.getOfshrTotFscAmt())));
		assertThat(
			dto.getOffshoreTotalLinehaulChargeAmount(),
			Matchers.equalTo(BasicTransformer.toDouble(pass.getOfshrTotLnhlChrgAmt())));
		if (pass.getId() == null) {
			assertThat(dto.getPassTypeCd(), Matchers.nullValue());
			assertThat(dto.getShipmentInstId(), Matchers.nullValue());
		} else {
			assertThat(dto.getPassTypeCd(), Matchers.equalTo(pass.getId().getPassTypCd()));
			assertThat(dto.getShipmentInstId(), Matchers.equalTo(pass.getId().getShpInstId()));
		}
		assertThat(dto.getPriceAgreementId(), Matchers.equalTo(BasicTransformer.toLong(pass.getPrcAgrmtId())));
        assertThat(dto.getPriceRulesetNbr(), Matchers.equalTo(BasicTransformer.toBigInteger(pass.getPrcRulesetNbr())));
		assertThat(dto.getRatingTariffName(), Matchers.equalTo(pass.getRtgTrfNmTxt()));
		assertThat(dto.getRatingTariffVersion().intValue(), Matchers.equalTo(pass.getRtgTrfVer()));
		assertThat(dto.getRulesTariffName(), Matchers.equalTo(pass.getRulesTrfNmTxt()));
		assertThat(dto.getRulesTariffVersion().intValue(), Matchers.equalTo(pass.getRulesTrfVer()));
		assertThat(dto.getTotalAccessorialAmount(), Matchers.equalTo(BasicTransformer.toDouble(pass.getTotAcAmt())));
		assertThat(dto.getTotalChargeAmount(), Matchers.equalTo(BasicTransformer.toDouble(pass.getTotChrgAmt())));
		assertThat(dto.getTotalDiscountAmount(), Matchers.equalTo(BasicTransformer.toDouble(pass.getTotDscntAmt())));
		assertThat(dto.getTotalFscAmount(), Matchers.equalTo(BasicTransformer.toDouble(pass.getTotFscAmt())));
		assertThat(
			dto.getTotalLinehaulChargeAmount(),
			Matchers.equalTo(BasicTransformer.toDouble(pass.getTotLnhlChrgAmt())));
		assertThat(dto.getTotalTaxAmount(), Matchers.equalTo(BasicTransformer.toDouble(pass.getTotTaxAmt())));
	}
}
