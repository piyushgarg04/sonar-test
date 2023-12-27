package com.xpo.ltl.shipment.service.util;


import java.util.Calendar;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.xpo.ltl.api.invoice.v1.InvoiceShipmentParty;
import com.xpo.ltl.api.invoice.v1.MonetaryAmount;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.SuppRefNbr;


public class CopyBillPdfHelper {

	public CopyBillPdfHelper() {
    }

    protected Shipment shipment;
    protected InvoiceShipmentParty consignee;
    protected InvoiceShipmentParty shipper;
    protected List<SuppRefNbr> suppRefNbrs;
    protected List<SuppRefNbr> snRefNbrs;
    protected List<SuppRefNbr> poRefNbrs;
    protected List<Commodity> commodities;
    protected List<Remark> remarks;
    protected Double totalWeightLbs;
    protected Double totalChargeAmount;
    protected MonetaryAmount usdTotalAmount;
    protected XMLGregorianCalendar pickupDate;
    protected String proNbr;
    protected String parentProNbr;
    protected String refNbrSN;
    protected String originTerminalSicCd;
    protected String destinationTerminalSicCd;

    public String getOriginTerminalSicCd() {
        return originTerminalSicCd;
    }

    public void setOriginTerminalSicCd(String originTerminalSicCd) {
        this.originTerminalSicCd = originTerminalSicCd;
    }

    public String getDestinationTerminalSicCd() {
        return destinationTerminalSicCd;
    }

    public void setDestinationTerminalSicCd(String destinationTerminalSicCd) {
        this.destinationTerminalSicCd = destinationTerminalSicCd;
    }

    public List<Remark> getRemarks() {
        return remarks;
    }
    
    public void setRemarks(List<Remark> remarks) {
        this.remarks = remarks;
    }
    
    public InvoiceShipmentParty getConsignee() {
        return consignee;
    }
    
    public void setConsignee(InvoiceShipmentParty consignee) {
        this.consignee = consignee;
    }
    
    public InvoiceShipmentParty getShipper() {
        return shipper;

    }
    
    public void setShipper(InvoiceShipmentParty shipper) {
        this.shipper = shipper;
    }
    
    public List<SuppRefNbr> getSuppRefNbrs() {
        return suppRefNbrs;
    }
    
    public void setSuppRefNbrs(List<SuppRefNbr> suppRefNbrs) {
        this.suppRefNbrs = suppRefNbrs;
    }
    
    public List<Commodity> getCommodities() {
        return commodities;
    }
    
    public void setCommodities(List<Commodity> commodities) {
        this.commodities = commodities;
    }
    
    public Shipment getShipment() {
		return shipment;
	}
    
	public void setShipment(Shipment shipment) {
		this.shipment = shipment;
	}
	
	public List<SuppRefNbr> getSnRefNbrs() {
		return snRefNbrs;
	}
	
	public void setSnRefNbrs(List<SuppRefNbr> snRefNbrs) {
		this.snRefNbrs = snRefNbrs;
	}
	
	public List<SuppRefNbr> getPoRefNbrs() {
		return poRefNbrs;
	}
	
	public void setPoRefNbrs(List<SuppRefNbr> poRefNbrs) {
		this.poRefNbrs = poRefNbrs;
	}

    public Remark transformRemarkForInvoice(Remark remark) {
        Remark invoiceRemark = new Remark();

        invoiceRemark.setRemark(remark.getRemark());
        invoiceRemark.setShipmentInstId(remark.getShipmentInstId());

        return invoiceRemark;
    }

	

	public CharSequence getRefNbrPO() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public String getProNbr() {
		return proNbr;
	}
	public void setProNbr(String value) {
        this.proNbr = value;
    }
	
	
	public XMLGregorianCalendar getPickupDate() {
     	return pickupDate;
	}
	
	/**
     * Sets the value of the pickupDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
	public void setPickupDate(XMLGregorianCalendar value) {
        this.pickupDate = value;
 	}

	 
    public Double getTotalWeightLbs() {
        return totalWeightLbs;
    }

    
    public void setTotalWeightLbs(Double value) {
        this.totalWeightLbs = value;
    }

	
	public MonetaryAmount getusdTotalAmount() {
		return usdTotalAmount;
    }

	  /**
     * Gets the value of the totalChargeAmount property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getTotalChargeAmount() {
        return totalChargeAmount;
    }

    /**
     * Sets the value of the totalChargeAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setTotalChargeAmount(Double value) {
        this.totalChargeAmount = value;
    }

    public String getRefNbrSN() {
		return refNbrSN;
	}
    
	public void setRefNbrSN() {
		if (!snRefNbrs.isEmpty()) {
			this.refNbrSN = snRefNbrs.get(0).getRefNbr();
		} else {
			this.refNbrSN = "";
		}
		
	}

	public CharSequence getShipperRemark() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getAuthRemark() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getParentProNbr() {
		return parentProNbr;
	}
	
	public void setParentProNbr(String value) {
		this.parentProNbr = value;
	}

}
    
   

