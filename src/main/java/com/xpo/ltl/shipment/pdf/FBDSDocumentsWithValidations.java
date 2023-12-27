package com.xpo.ltl.shipment.pdf;

import java.util.List;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.FBDSDocument;

public class FBDSDocumentsWithValidations {

	private List<FBDSDocument> fbdsDocuments;
	private List<DataValidationError> validationErrors;
	
	public List<FBDSDocument> getFbdsDocuments() {
		return fbdsDocuments;
	}
	public void setFbdsDocuments(List<FBDSDocument> fbdsDocuments) {
		this.fbdsDocuments = fbdsDocuments;
	}
	public List<DataValidationError> getValidationErrors() {
		return validationErrors;
	}
	public void setValidationErrors(List<DataValidationError> validationErrors) {
		this.validationErrors = validationErrors;
	}
}
