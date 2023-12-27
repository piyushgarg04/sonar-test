package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import com.xpo.ltl.api.client.common.Attachment;
import com.xpo.ltl.api.client.common.JsonAttachment;
import com.xpo.ltl.api.documentmanagement.v1.ArchiveDocumentResp;
import com.xpo.ltl.api.documentmanagement.v1.DmsArchiveRequest;
import com.xpo.ltl.api.documentmanagement.v1.DmsIndex;
import com.xpo.ltl.api.exception.ExceptionBuilder;
import com.xpo.ltl.api.exception.ServiceErrorMessage;
import com.xpo.ltl.api.invoice.v1.InvoiceShipmentParty;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.CreateAndArchiveCopyBillDocumentResp;
import com.xpo.ltl.api.shipment.v2.CreateAndArchiveCopyBillDocumentRqst;
import com.xpo.ltl.api.shipment.v2.GetShipmentResp;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.SuppRefNbr;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.util.CopyBillPdfHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

/**
 * Service for Creating and Archiving a Copy Bill document
 *
 */
public class CreateAndArchiveCopyBillDocumentImpl {
    
	@Inject
	private GetShipmentImpl getShipmentImpl;
	
	@Inject
	private ExternalRestClient externalRestClient;

	@Inject
	private CreateCopyBillPdf createCopyBillPdf;

	private Attachment inputFile;
	
	private static final Log log = LogFactory
			.getLog(CreateAndArchiveCopyBillDocumentImpl.class);
	
	public CreateAndArchiveCopyBillDocumentResp createAndArchiveCopyBillDocument(CreateAndArchiveCopyBillDocumentRqst createAndArchiveCopyBillDocumentRqst, TransactionContext txnContext, EntityManager entityManager)
			throws Exception {
		checkNotNull(createAndArchiveCopyBillDocumentRqst, "The request is required.");
		checkNotNull(txnContext, "TransactionContext is required");
		if (createAndArchiveCopyBillDocumentRqst.getShipmentId().getProNumber() == null && createAndArchiveCopyBillDocumentRqst.getShipmentId().getShipmentInstId() == null) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.fromValue("A ProNumber or InstanceID is required"), txnContext).build();
		}
		checkNotNull(createAndArchiveCopyBillDocumentRqst, "The shipmentInstIds is required.");
		log.info("createAndArchiveCopyBillDocument begin");
		String proNumber = createAndArchiveCopyBillDocumentRqst.getShipmentId().getProNumber();
		String pickUpDate = BasicTransformer.toDateString(createAndArchiveCopyBillDocumentRqst.getShipmentId().getPickupDate());
		long shipmentInstId = BasicTransformer.toLong(createAndArchiveCopyBillDocumentRqst.getShipmentId().getShipmentInstId());
		
		ShipmentDetailCd[] shipmentDetailCds = {};
		GetShipmentResp shipmentResp = getShipmentImpl.getShipment(proNumber, pickUpDate, shipmentInstId, shipmentDetailCds, null, null, txnContext, entityManager);
		log.info("Get shipment End");

		CopyBillPdfHelper copyBillPdf = null;
		
		if (shipmentResp != null) {
			copyBillPdf = createCopyBillPdf(shipmentResp);
			
			log.info("create pdf begin");
		} else throw ExceptionBuilder.exception(ServiceErrorMessage.fromValue("Shipment not found"), txnContext).build();
		
		//Build PDF
		final byte[] aPdf = createCopyBillPdf.createPdf(copyBillPdf);
		String fileName = "copyBillPdf";
		
		//check  if PDF or Image needs to be sent o DMS
		if (pdfCrossBorderCheck(copyBillPdf)) {
			log.info("Domestic - Archive as Pdf");
			String fileType = "application/pdf";
			inputFile = Attachment.fromBytes(fileName, fileType, aPdf);
		} else {
			log.info("Cross border - Archive as Image");
			PDDocument document = new PDDocument();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
				try {
		            String fileType = "image/png";
				    // Create image from pdf byte array
				    InputStream in = new ByteArrayInputStream(aPdf);
				    document = PDDocument.load(in);
                    log.info("temporary PDDocument allPages: " + document.getDocumentCatalog().getAllPages());
				    List<PDPage> list = document.getDocumentCatalog().getAllPages();
				    BufferedImage image = list.get(0).convertToImage();

				    // Convert image to byte array
				    ImageIO.write(image, "png", baos);
				    baos.flush();
				    byte[] imageInByte = baos.toByteArray();
				    inputFile = Attachment.fromBytes(fileName, fileType, imageInByte);
				} catch (Exception e) {
                    log.warn("Pdf to Png conversion failed: " + e.getMessage());
		            log.info("Cross border - revert to archive as Pdf");
		            String fileType = "application/pdf";
		            inputFile = Attachment.fromBytes(fileName, fileType, aPdf);
				} finally {
				    try{
				        document.close();
				        baos.close(); 
                    } catch (Exception e) {
                        //ignore
                    }
				    
				}
		}
	
		//Send to DMS
		log.info("DMS - get token");
		String accessToken = externalRestClient.retrieveDmsAuthToken(txnContext);

		// TODO add the mover pro number for mover as a tag
        Boolean isAMover = shipmentResp.getParentShipmentId() != null && StringUtils.isNotBlank(shipmentResp.getShipment().getMoverSuffix());
		
		DmsArchiveRequest dmsArchiveRqst = new DmsArchiveRequest();
		
        DmsIndex i1 = new DmsIndex();
        DmsIndex i2 = new DmsIndex();
        if (isAMover) {
            dmsArchiveRqst.setDocNumber(copyBillPdf.getProNbr());

            i1.setTag("PRO");
            i1.setValue(ProNumberHelper.formatMvrProNineDigit(copyBillPdf.getProNbr()));

            i2.setTag("MOVR");
            i2.setValue(shipmentResp.getShipment().getProNbr());

            dmsArchiveRqst.setIndices(Arrays.asList(i1, i2));
        } else {
            dmsArchiveRqst.setDocNumber(PdfCopyBillUtils.formatProNineDigit(copyBillPdf.getProNbr()));

            i1.setTag("PRO");
            i1.setValue(PdfCopyBillUtils.formatProNineDigit(copyBillPdf.getProNbr()));
            
            dmsArchiveRqst.setIndices(Arrays.asList(i1));
      }

        JsonAttachment<DmsArchiveRequest> archiveRequest = Attachment.fromObject(fileName, dmsArchiveRqst);

		log.info("DMS - archive begin");
		ArchiveDocumentResp archiveDocument = externalRestClient.archiveDocument(inputFile, archiveRequest, accessToken, txnContext);

		log.info("DMS - archive getresponse");
		final CreateAndArchiveCopyBillDocumentResp resp = new CreateAndArchiveCopyBillDocumentResp();

		if (archiveDocument.getDocumentInfo() != null) {
			resp.setDmsDocumentId(archiveDocument.getDocumentInfo().getDocArchiveTimestamp());
		} else {
			resp.setDmsDocumentId(null);
		}

		log.info("DMS - archive end");

		return resp;
		
	}

	private boolean pdfCrossBorderCheck(CopyBillPdfHelper shipdata) {
		return (shipdata.getShipper().getCountryCd().equals(shipdata.getConsignee().getCountryCd()));
		
	}
	
	private CopyBillPdfHelper createCopyBillPdf(GetShipmentResp shipmentResp) {
		CopyBillPdfHelper copyBillPdf = new CopyBillPdfHelper();
		
		Boolean isAMover = shipmentResp.getParentShipmentId() != null && StringUtils.isNotBlank(shipmentResp.getShipment().getMoverSuffix());
		
		//Set Party info
		List<AsMatchedParty> partyList = shipmentResp.getAsMatchedParty();
		
		for (AsMatchedParty party : partyList) {
			log.info("DMS - archive end");
			InvoiceShipmentParty invoiceShipmentParty = new InvoiceShipmentParty();
			invoiceShipmentParty.setName1(party.getName1());
			invoiceShipmentParty.setName2(party.getName2());
			invoiceShipmentParty.setAddressLine1(party.getAddress());
			invoiceShipmentParty.setCity(party.getCity());
			invoiceShipmentParty.setStateCd(party.getStateCd());
			invoiceShipmentParty.setPostalCd(party.getZip6());
			invoiceShipmentParty.setZip4RestUs(party.getZip4RestUs());
			invoiceShipmentParty.setCountryCd(party.getCountryCd());
			
			if (party.getTypeCd().toString().equalsIgnoreCase("SHPR")) {
				copyBillPdf.setShipper(invoiceShipmentParty);
			}
			else if (party.getTypeCd().toString().equalsIgnoreCase("CONS")) {
				copyBillPdf.setConsignee(invoiceShipmentParty);
			}
		}

		List<SuppRefNbr> sNRefNbrs = new ArrayList<SuppRefNbr>();
		List<SuppRefNbr> pORefNbrs = new ArrayList<SuppRefNbr>();
				
		for (SuppRefNbr suppRefNbr : shipmentResp.getSuppRefNbr()) {
			if (suppRefNbr.getTypeCd().equals("SN#")) {
				sNRefNbrs.add(suppRefNbr);
			}
			else if (suppRefNbr.getTypeCd().equals("PO#")) {
				pORefNbrs.add(suppRefNbr);
			 
			}
		}

		List<SuppRefNbr> suppRefNbrs = new ArrayList<>(shipmentResp.getSuppRefNbr());
		
		if(isAMover) {
			SuppRefNbr moverRefNbr = new SuppRefNbr();
			moverRefNbr.setRefNbr(shipmentResp.getShipment().getProNbr());
			moverRefNbr.setTypeCd("MOVR");
			suppRefNbrs.add(moverRefNbr);
		}
		
		copyBillPdf.setSuppRefNbrs(suppRefNbrs);
		copyBillPdf.setSnRefNbrs(sNRefNbrs);
		copyBillPdf.setPoRefNbrs(pORefNbrs);
		copyBillPdf.setRemarks(shipmentResp.getRemark());
		copyBillPdf.setCommodities(shipmentResp.getCommodity());
		copyBillPdf.setTotalWeightLbs(shipmentResp.getShipment().getTotalWeightLbs());
	 
	    copyBillPdf.setRefNbrSN();
        copyBillPdf.setOriginTerminalSicCd(shipmentResp.getShipment().getOriginTerminalSicCd());
        copyBillPdf.setDestinationTerminalSicCd(shipmentResp.getShipment().getDestinationTerminalSicCd());

	    if(isAMover){
	    	copyBillPdf.setProNbr(shipmentResp.getParentShipmentId().getProNumber() + shipmentResp.getShipment().getMoverSuffix());
			copyBillPdf.setParentProNbr(shipmentResp.getParentShipmentId().getProNumber());
		}else{
			copyBillPdf.setProNbr(shipmentResp.getShipment().getProNbr());
			copyBillPdf.setParentProNbr(null);
		}
		
		try {
			if (shipmentResp.getShipment().getPickupDate() != null) {
				copyBillPdf.setPickupDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(shipmentResp.getShipment().getPickupDate()));
			}else {
				copyBillPdf.setPickupDate(DatatypeFactory.newInstance().newXMLGregorianCalendar((new GregorianCalendar())));
			}
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
	
		if (shipmentResp.getShipment().getTotalChargeAmount() != null) {
			copyBillPdf.setTotalChargeAmount(shipmentResp.getShipment().getTotalChargeAmount());
		} else {
			copyBillPdf.setTotalChargeAmount((double) 0);
		}

		return copyBillPdf;
	}
}
