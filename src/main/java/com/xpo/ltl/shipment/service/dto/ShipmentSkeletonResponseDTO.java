package com.xpo.ltl.shipment.service.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ShipmentSkeletonResponseDTO {

    private Long shpInstId;
    private String reportingSicCd;
    private String destSicCd;
    private Timestamp lastMovementDateTime;
    private BigDecimal calcMvmtSeqNbr;
    private BigDecimal totalPcsCount;
    private Boolean hazmatInd;
    private BigDecimal totalWeightLbs;
    private BigDecimal bolInstId;
    private String proNbr;
    private Boolean lateTender;
    private Boolean skeletonBasedOnPickup;
    private BigDecimal shprCustNbr;

    public Long getShpInstId() {
        return shpInstId;
    }

    public void setShpInstId(Long shpInstId) {
        this.shpInstId = shpInstId;
    }

    public String getReportingSicCd() {
        return reportingSicCd;
    }

    public void setReportingSicCd(String reportingSicCd) {
        this.reportingSicCd = reportingSicCd;
    }

    public String getDestSicCd() {
        return destSicCd;
    }

    public void setDestSicCd(String destSicCd) {
        this.destSicCd = destSicCd;
    }

    public Timestamp getLastMovementDateTime() {
        return lastMovementDateTime;
    }

    public void setLastMovementDateTime(Timestamp lastMovementDateTime) {
        this.lastMovementDateTime = lastMovementDateTime;
    }

    public BigDecimal getCalcMvmtSeqNbr() {
        return calcMvmtSeqNbr;
    }

    public void setCalcMvmtSeqNbr(BigDecimal calcMvmtSeqNbr) {
        this.calcMvmtSeqNbr = calcMvmtSeqNbr;
    }

    public BigDecimal getTotalPcsCount() {
        return totalPcsCount;
    }

    public void setTotalPcsCount(BigDecimal totalPcsCount) {
        this.totalPcsCount = totalPcsCount;
    }

    public Boolean getHazmatInd() {
        return hazmatInd;
    }

    public void setHazmatInd(Boolean hazmatInd) {
        this.hazmatInd = hazmatInd;
    }

    public BigDecimal getTotalWeightLbs() {
        return totalWeightLbs;
    }

    public void setTotalWeightLbs(BigDecimal totalWeightLbs) {
        this.totalWeightLbs = totalWeightLbs;
    }

    public BigDecimal getBolInstId() {
        return bolInstId;
    }

    public void setBolInstId(BigDecimal bolInstId) {
        this.bolInstId = bolInstId;
    }

    public String getProNbr() {
        return proNbr;
    }

    public void setProNbr(String proNbr) {
        this.proNbr = proNbr;
    }

    public Boolean isLateTender() {
        return lateTender;
    }

    public void setLateTender(Boolean lateTender) {
        this.lateTender = lateTender;
    }

    public Boolean isSkeletonBasedOnPickup() {
        return skeletonBasedOnPickup;
    }

    public void setSkeletonBasedOnPickup(Boolean skeletonBasedOnPickup) {
        this.skeletonBasedOnPickup = skeletonBasedOnPickup;
    }

    public BigDecimal getShprCustNbr() {
        return shprCustNbr;
    }

    public void setShprCustNbr(BigDecimal shprCustNbr) {
        this.shprCustNbr = shprCustNbr;
    }

}
