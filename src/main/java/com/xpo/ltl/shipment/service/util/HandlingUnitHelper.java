package com.xpo.ltl.shipment.service.util;

import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.api.client.util.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;

/**
 * Handling Unit Helper.-
 *
 * @author msanguinetti
 *
 */
public class HandlingUnitHelper {

	private static final Log logger = LogFactory.getLog(HandlingUnitHelper.class);

	public static double MAX_HANDLING_UNIT_LENGTH = 636;
	public static double MAX_HANDLING_UNIT_WIDTH_HEIGHT = 103;
	public static double CUBIC_INCHES_CUBIC_FEET_FACTOR = 1728;
    public final static double MAX_VOL_CUB_FT = 99999.0;


	/**
	 * Validates handling unit length
	 *
	 * @param lengthNbr
	 *
	 */
	public static void validateHandlingUnitLength(Double lengthNbr, List<MoreInfo> moreInfoList) {

		if (Objects.isNull(moreInfoList)){
			moreInfoList = Lists.newArrayList();
		}

		if (Objects.isNull(lengthNbr)){
			moreInfoList.add(createMoreInfo("lengthNbr",
					ValidationErrorMessage.HU_LENGTH_REQUIRED.message()));
		} else {

			if (lengthNbr <= 0) {
				moreInfoList.add(createMoreInfo("lengthNbr",
						ValidationErrorMessage.FRIEHGT_DIMENSIONS_INVALID.message()));
			}

			if (lengthNbr > MAX_HANDLING_UNIT_LENGTH) {
				moreInfoList.add(createMoreInfo("lengthNbr",
						ValidationErrorMessage.LENGTH_GREATER_THAN_636.message()));
			}
		}
	}

	/**
	 * Validates handling unit width or height
	 *
	 * @param dimNbr
	 *
	 */
	public static void validateHandlingUnitWidthHeight(Double dimNbr, String location,
			List<MoreInfo> moreInfoList) {

		if (Objects.isNull(moreInfoList)){
			moreInfoList = Lists.newArrayList();
		}

		if (Objects.isNull(dimNbr)){
			moreInfoList.add(createMoreInfo("dimNbr",
					ValidationErrorMessage.HU_DIMENSIONS_REQUIRED.message()));
		} else {
			if (dimNbr <= 0) {
				moreInfoList.add(createMoreInfo(location,
						ValidationErrorMessage.FRIEHGT_DIMENSIONS_INVALID.message()));
			}

			if (dimNbr > MAX_HANDLING_UNIT_WIDTH_HEIGHT) {
				moreInfoList.add(createMoreInfo(location,
						ValidationErrorMessage.WIDTH_HEIGHT_GREATER_THAN_103.message()));
			}
		}

	}

    /**
     * Calculates volume of handling unit and convert cubic inches to cubic Feet.<br/>
     * If vol cubic in feet is greater than {@code HandlingUnitHelper#MAX_SIZE_INCHES}, the value will be limited to
     * that max.
     *
     * @param length
     *            in inches
     * @param width
     *            in inches
     * @param height
     *            in inches
     * @return volume of handling unit in cubic feet
     */
    public static Double calculateVolCubFt(Double length, Double width, Double height) {
        if (length == null || width == null || height == null) {
            return 0.0;
        }

        double result = (length * width * height) / CUBIC_INCHES_CUBIC_FEET_FACTOR;

        if (result > MAX_VOL_CUB_FT)
            result = MAX_VOL_CUB_FT;

        return result;
    }

	public static MoreInfo createMoreInfo(String location, String message) {
		MoreInfo moreInfo = new MoreInfo();
		moreInfo.setItemNbr(null);
		moreInfo.setMessage(message);
		moreInfo.setLocation(location);
		return moreInfo;
	}

    public static ShmHandlingUnit clone(ShmHandlingUnit handlingUnitOriginal) {
		if(handlingUnitOriginal == null){
			return null;
		}
        ShmHandlingUnit cloneHu = new ShmHandlingUnit();
		cloneHu.setArchiveInd(handlingUnitOriginal.getArchiveInd());
		cloneHu.setChildProNbrTxt(handlingUnitOriginal.getChildProNbrTxt());
		cloneHu.setCrteTmst(handlingUnitOriginal.getCrteTmst());
		cloneHu.setCrteTranCd(handlingUnitOriginal.getCrteTranCd());
		cloneHu.setCrteUid(handlingUnitOriginal.getCrteUid());
		cloneHu.setCurrentDockLocTxt(handlingUnitOriginal.getCurrentDockLocTxt());
		cloneHu.setCurrentSicCd(handlingUnitOriginal.getCurrentSicCd());
		cloneHu.setCurrentTrlrInstId(handlingUnitOriginal.getCurrentTrlrInstId());
		cloneHu.setDimensionTypeCd(handlingUnitOriginal.getDimensionTypeCd());
		cloneHu.setDmlTmst(handlingUnitOriginal.getDmlTmst());
		cloneHu.setDtlCapxtimestamp(handlingUnitOriginal.getDtlCapxtimestamp());
		cloneHu.setHandlingMvmtCd(handlingUnitOriginal.getHandlingMvmtCd());
		cloneHu.setHeightNbr(handlingUnitOriginal.getHeightNbr());
		cloneHu.setId(handlingUnitOriginal.getId());
		cloneHu.setLengthNbr(handlingUnitOriginal.getLengthNbr());
		cloneHu.setLstMvmtTmst(handlingUnitOriginal.getLstMvmtTmst());
		cloneHu.setLstUpdtTmst(handlingUnitOriginal.getLstUpdtTmst());
		cloneHu.setLstUpdtTranCd(handlingUnitOriginal.getLstUpdtTranCd());
		cloneHu.setLstUpdtUid(handlingUnitOriginal.getLstUpdtUid());
		cloneHu.setMovrProNbrTxt(handlingUnitOriginal.getMovrProNbrTxt());
		cloneHu.setMovrSuffix(handlingUnitOriginal.getMovrSuffix());
        cloneHu.setMvmtStatCd(handlingUnitOriginal.getMvmtStatCd());
        cloneHu.setParentProNbrTxt(handlingUnitOriginal.getParentProNbrTxt());
        cloneHu.setPkupDt(handlingUnitOriginal.getPkupDt());
        cloneHu.setPoorlyPackagedInd(handlingUnitOriginal.getPoorlyPackagedInd());
        cloneHu.setPupVolPct(handlingUnitOriginal.getPupVolPct());
        cloneHu.setReplLstUpdtTmst(handlingUnitOriginal.getReplLstUpdtTmst());
        cloneHu.setReweighInd(handlingUnitOriginal.getReweighInd());
        cloneHu.setSplitInd(handlingUnitOriginal.getSplitInd());
        cloneHu.setStackableInd(handlingUnitOriginal.getStackableInd());
        cloneHu.setTypeCd(handlingUnitOriginal.getTypeCd());
        cloneHu.setVolCft(handlingUnitOriginal.getVolCft());
        cloneHu.setWgtLbs(handlingUnitOriginal.getWgtLbs());
        cloneHu.setWidthNbr(handlingUnitOriginal.getWidthNbr());

		return cloneHu;
		
    }

}