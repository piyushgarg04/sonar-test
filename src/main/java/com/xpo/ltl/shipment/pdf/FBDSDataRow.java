package com.xpo.ltl.shipment.pdf;

public class FBDSDataRow {

	private String piecesNum;
	private String hazardousInd;
	private String description;
	private String additionalDescription;
	private String weight;
	private String rate;
	private String charges;
	public String getPiecesNum() {
		return piecesNum;
	}
	public void setPiecesNum(String piecesNum) {
		this.piecesNum = piecesNum;
	}
	public String getHazardousInd() {
		return hazardousInd;
	}
	public void setHazardousInd(String hazardousInd) {
		this.hazardousInd = hazardousInd;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getAdditionalDescription() {
		return additionalDescription;
	}
	public void setAdditionalDescription(String additionalDescription) {
		this.additionalDescription = additionalDescription;
	}
	public String getWeight() {
		return weight;
	}
	public void setWeight(String weight) {
		this.weight = weight;
	}
	public String getRate() {
		return rate;
	}
	public void setRate(String rate) {
		this.rate = rate;
	}
	public String getCharges() {
		return charges;
	}
	public void setCharges(String charges) {
		this.charges = charges;
	}
}
