package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCommodity;

import java.math.BigDecimal;
import java.util.Objects;

public class CorrectionLoadCommodityValuesImpl  extends AbstractCommodity implements LoadValues<Commodity, ShmCommodity> {
	@Override
	public void loadtValues(Commodity commodity, ShmCommodity shmCommodity) {
		if (Objects.nonNull(commodity.getAmount())) { //AMT
			shmCommodity.setAmt(BigDecimal.valueOf(commodity.getAmount()));
		}

		if (Objects.nonNull(commodity.getTariffsRate())) {//TRF_RT
			shmCommodity.setTrfRt(BigDecimal.valueOf(commodity.getTariffsRate()));
		}
		if (Objects.nonNull(commodity.getPiecesCount())) {//PCS
			shmCommodity.setPcsCnt(BigDecimal.valueOf(commodity.getPiecesCount().longValue()));
		}
		if (Objects.nonNull(commodity.getPackageCd())) {//PKG_CD
			shmCommodity.setPkgCd(getCommodityPackageCd(commodity.getPackageCd().value()));
		}
		if (Objects.nonNull(commodity.getHazardousMtInd())) {//HZ_MT_IND
			shmCommodity.setHzMtInd(getFlag(commodity.getHazardousMtInd()));
		}

		shmCommodity.setDescTxt(commodity.getDescription());//DESC_TXT
		shmCommodity.setNmfcItmCd(commodity.getNmfcItemCd());//NMFC_ITM_CD

		if (Objects.nonNull(commodity.getClassType())) {//CLASS_TYP
			shmCommodity.setClassTyp(getCommodityClassCd(commodity.getClassType().value()));
		}

		if (Objects.nonNull(commodity.getWeightLbs())) {//WGT_LBS
			shmCommodity.setWgtLbs(BigDecimal.valueOf(commodity.getWeightLbs()));
		}
	}
}
