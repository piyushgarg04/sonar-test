package com.xpo.ltl.shipment.service.util;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovement;
import com.xpo.ltl.api.transformer.BasicTransformer;

/**
 * Handling Unit Movement Helper.-
 *
 * @author msanguinetti
 *
 */
public class HandlingUnitMovementHelper {

	public static void setDefaultValues(HandlingUnitMovement handlingUnitMvmt) {



		if (Objects.isNull(handlingUnitMvmt.getArchiveControlCd())){
			handlingUnitMvmt.setArchiveControlCd(StringUtils.SPACE);
		}

		if (Objects.isNull(handlingUnitMvmt.getExceptionTypeCd())) {
			handlingUnitMvmt.setExceptionTypeCd(StringUtils.SPACE);
		}
		if (Objects.isNull(handlingUnitMvmt.getDamagedCategoryCd())){
			handlingUnitMvmt.setDamagedCategoryCd(StringUtils.SPACE);
		}
		if (Objects.isNull(handlingUnitMvmt.getRefusedReasonCd())) {
			handlingUnitMvmt.setRefusedReasonCd(StringUtils.SPACE);
		}
		if (Objects.isNull(handlingUnitMvmt.getUndeliveredReasonCd())) {
			handlingUnitMvmt.setUndeliveredReasonCd(StringUtils.SPACE);
		}

		if (Objects.isNull(handlingUnitMvmt.getDockInstanceId())) {
			handlingUnitMvmt.setDockInstanceId(0L);
		}

		if (Objects.isNull(handlingUnitMvmt.getTrailerInstanceId())) {
			handlingUnitMvmt.setTrailerInstanceId(0L);
		}

        if (Objects.isNull(handlingUnitMvmt.getBypassScanInd())) {
            handlingUnitMvmt.setBypassScanInd(false);
        }

        if (Objects.isNull(handlingUnitMvmt.getBypassScanReason())) {
            handlingUnitMvmt.setBypassScanReason(StringUtils.SPACE);
        }

        if (Objects.isNull(handlingUnitMvmt.getScanDateTime())) {
            handlingUnitMvmt.setScanDateTime(BasicTransformer.toXMLGregorianCalendar(TimestampUtil.getLowTimestamp()));
        }

        if (Objects.isNull(handlingUnitMvmt.getSplitAuthorizeBy())) {
            handlingUnitMvmt.setSplitAuthorizeBy(StringUtils.SPACE);
        }

        if (Objects.isNull(handlingUnitMvmt.getSplitAuthorizeDateTime())) {
            handlingUnitMvmt
                .setSplitAuthorizeDateTime(BasicTransformer.toXMLGregorianCalendar(TimestampUtil.getLowTimestamp()));
        }
	}

    public static ShmHandlingUnitMvmt cloneHuMvmt(ShmHandlingUnitMvmt newHandlingUnitMvmt) {
        if(newHandlingUnitMvmt == null){
			return null;
		}
		ShmHandlingUnitMvmt cloneMvmt = new ShmHandlingUnitMvmt();
		cloneMvmt.setArchiveCntlCd(newHandlingUnitMvmt.getArchiveCntlCd());
		cloneMvmt.setBypassScanInd(newHandlingUnitMvmt.getBypassScanInd());
		cloneMvmt.setBypassScanReason(newHandlingUnitMvmt.getBypassScanReason());
		cloneMvmt.setCrteTmst(newHandlingUnitMvmt.getCrteTmst());
		cloneMvmt.setCrteTranCd(newHandlingUnitMvmt.getCrteTranCd());
		cloneMvmt.setCrteUid(newHandlingUnitMvmt.getCrteUid());
		cloneMvmt.setDmgdCatgCd(newHandlingUnitMvmt.getDmgdCatgCd());
		cloneMvmt.setDmlTmst(newHandlingUnitMvmt.getDmlTmst());
		cloneMvmt.setDockInstId(newHandlingUnitMvmt.getDockInstId());
		cloneMvmt.setDtlCapxtimestamp(newHandlingUnitMvmt.getDtlCapxtimestamp());
		cloneMvmt.setExcpTypCd(newHandlingUnitMvmt.getExcpTypCd());
		cloneMvmt.setId(newHandlingUnitMvmt.getId());
		cloneMvmt.setMvmtRptgSicCd(newHandlingUnitMvmt.getMvmtRptgSicCd());
		cloneMvmt.setMvmtTmst(newHandlingUnitMvmt.getMvmtTmst());
		cloneMvmt.setMvmtTypCd(newHandlingUnitMvmt.getMvmtTypCd());
		cloneMvmt.setReplLstUpdtTmst(newHandlingUnitMvmt.getReplLstUpdtTmst());
		cloneMvmt.setRfsdRsnCd(newHandlingUnitMvmt.getRfsdRsnCd());
		cloneMvmt.setRmrkTxt(newHandlingUnitMvmt.getRmrkTxt());
		cloneMvmt.setScanTmst(newHandlingUnitMvmt.getScanTmst());
		cloneMvmt.setSplitAuthorizeBy(newHandlingUnitMvmt.getSplitAuthorizeBy());
		cloneMvmt.setSplitAuthorizeTmst(newHandlingUnitMvmt.getSplitAuthorizeTmst());
		cloneMvmt.setTrlrInstId(newHandlingUnitMvmt.getTrlrInstId());
		cloneMvmt.setUndlvdRsnCd(newHandlingUnitMvmt.getUndlvdRsnCd());

		return cloneMvmt;
    }


}