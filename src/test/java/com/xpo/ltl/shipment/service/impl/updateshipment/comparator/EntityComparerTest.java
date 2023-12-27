package com.xpo.ltl.shipment.service.impl.updateshipment.comparator;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityComparerTest {

	@Test
	void findDifferences() {
		Comparator<ShmAcSvc> comparator = ComparatorFactory.createStrictComparator();
		Timestamp timestamp = new Timestamp(new Date().getTime());
		ShmAcSvc shmAcSvc = new ShmAcSvc();
		ShmAcSvc shmAcSvcNew = new ShmAcSvc();

		shmAcSvc.setAcQty(BigDecimal.ZERO);
		shmAcSvc.setAmt(BigDecimal.ZERO);
		shmAcSvc.setPpdPct(BigDecimal.ZERO);
		shmAcSvc.setTrfRt(BigDecimal.ZERO);
		shmAcSvc.setId(null);
		shmAcSvc.setShmShipment(null);
		shmAcSvc.setAcCd(StringUtils.SPACE);
		shmAcSvc.setAcUom(StringUtils.SPACE);
		shmAcSvc.setArchiveCntlCd(StringUtils.SPACE);
		shmAcSvc.setChrgToCd(StringUtils.SPACE);
		shmAcSvc.setDescTxt(StringUtils.SPACE);
		shmAcSvc.setLstUpdtTranCd(StringUtils.SPACE);
		shmAcSvc.setLstUpdtUid(StringUtils.SPACE);
		shmAcSvc.setMinChrgInd(StringUtils.SPACE);
		shmAcSvc.setDmlTmst(timestamp);
		shmAcSvc.setDtlCapxtimestamp(timestamp);
		shmAcSvc.setLstUpdtTmst(timestamp);
		shmAcSvc.setReplLstUpdtTmst(timestamp);

		shmAcSvcNew.setAcQty(BigDecimal.ZERO);
		shmAcSvcNew.setAmt(BigDecimal.ZERO);
		shmAcSvcNew.setPpdPct(BigDecimal.ZERO);
		shmAcSvcNew.setTrfRt(BigDecimal.ZERO);
		shmAcSvcNew.setId(null);
		shmAcSvcNew.setShmShipment(null);
		shmAcSvcNew.setAcCd(StringUtils.SPACE);
		shmAcSvcNew.setAcUom(StringUtils.SPACE);
		shmAcSvcNew.setArchiveCntlCd(StringUtils.SPACE);
		shmAcSvcNew.setChrgToCd(StringUtils.SPACE);
		shmAcSvcNew.setDescTxt(StringUtils.SPACE);
		shmAcSvcNew.setLstUpdtTranCd(StringUtils.SPACE);
		shmAcSvcNew.setLstUpdtUid(StringUtils.SPACE);
		shmAcSvcNew.setMinChrgInd(StringUtils.SPACE);
		shmAcSvcNew.setDmlTmst(timestamp);
		shmAcSvcNew.setDtlCapxtimestamp(timestamp);
		shmAcSvcNew.setLstUpdtTmst(timestamp);
		shmAcSvcNew.setReplLstUpdtTmst(timestamp);
		List<String> differences = EntityComparer.findDifferences(shmAcSvc, shmAcSvcNew, comparator);
		assertEquals(0, differences.size());

	}

	@Test
	void findDifferences2() {
		Comparator<ShmAcSvc> comparator = ComparatorFactory.createStrictComparator();
		Timestamp timestamp = new Timestamp(new Date().getTime());
		ShmAcSvc shmAcSvc = new ShmAcSvc();
		ShmAcSvc shmAcSvcNew = new ShmAcSvc();

		shmAcSvc.setAcQty(BigDecimal.ZERO);
		shmAcSvc.setAmt(BigDecimal.ZERO);
		shmAcSvc.setPpdPct(BigDecimal.ZERO);
		shmAcSvc.setTrfRt(BigDecimal.ZERO);
		shmAcSvc.setId(null);
		shmAcSvc.setShmShipment(null);
		shmAcSvc.setAcCd(StringUtils.SPACE);
		shmAcSvc.setAcUom(StringUtils.SPACE);
		shmAcSvc.setArchiveCntlCd(StringUtils.SPACE);
		shmAcSvc.setChrgToCd(StringUtils.SPACE);
		shmAcSvc.setDescTxt(StringUtils.SPACE);
		shmAcSvc.setLstUpdtTranCd(StringUtils.SPACE);
		shmAcSvc.setLstUpdtUid(StringUtils.SPACE);
		shmAcSvc.setMinChrgInd(StringUtils.SPACE);
		shmAcSvc.setDmlTmst(timestamp);
		shmAcSvc.setDtlCapxtimestamp(timestamp);
		shmAcSvc.setLstUpdtTmst(timestamp);
		shmAcSvc.setReplLstUpdtTmst(timestamp);

		shmAcSvcNew.setAcQty(BigDecimal.ONE);
		shmAcSvcNew.setAmt(BigDecimal.ZERO);
		shmAcSvcNew.setPpdPct(BigDecimal.ZERO);
		shmAcSvcNew.setTrfRt(BigDecimal.ZERO);
		shmAcSvcNew.setId(null);
		shmAcSvcNew.setShmShipment(null);
		shmAcSvcNew.setAcCd(StringUtils.SPACE);
		shmAcSvcNew.setAcUom(StringUtils.SPACE);
		shmAcSvcNew.setArchiveCntlCd(StringUtils.SPACE);
		shmAcSvcNew.setChrgToCd(StringUtils.SPACE);
		shmAcSvcNew.setDescTxt("A");
		shmAcSvcNew.setLstUpdtTranCd(StringUtils.SPACE);
		shmAcSvcNew.setLstUpdtUid(StringUtils.SPACE);
		shmAcSvcNew.setMinChrgInd(StringUtils.SPACE);
		shmAcSvcNew.setDmlTmst(timestamp);
		shmAcSvcNew.setDtlCapxtimestamp(timestamp);
		shmAcSvcNew.setLstUpdtTmst(timestamp);
		shmAcSvcNew.setReplLstUpdtTmst(timestamp);

		List<String> differences = EntityComparer.findDifferences(shmAcSvc, shmAcSvcNew, comparator);
		assertEquals(2, differences.size());

	}
}