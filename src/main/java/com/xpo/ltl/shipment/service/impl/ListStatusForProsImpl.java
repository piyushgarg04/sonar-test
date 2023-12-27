package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Strings;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillRange;
import com.xpo.ltl.api.shipment.transformer.v2.ProStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.ListStatusForProsResp;
import com.xpo.ltl.api.shipment.v2.ListStatusForProsRqst;
import com.xpo.ltl.api.shipment.v2.ProStatus;
import com.xpo.ltl.api.shipment.v2.ProStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.ProNumber;
import com.xpo.ltl.shipment.service.dao.ProFrtBillIndexSubDAO;
import com.xpo.ltl.shipment.service.dao.ProFrtBillRangeSubDAO;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@RequestScoped
public class ListStatusForProsImpl {

    private static final Log LOGGER = LogFactory.getLog(ListStatusForProsImpl.class);

	@Inject
	ProFrtBillIndexSubDAO proFrtBillIndexSubDAO;

	@Inject
	ProFrtBillRangeSubDAO proFrtBillRangeSubDAO;


    public ListStatusForProsResp listStatusForPros(final ListStatusForProsRqst listProStatusRqst,
			final TransactionContext txnContext, final EntityManager entityManager)
			throws ServiceException {

		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");

        ListStatusForProsResp response = new ListStatusForProsResp();
        response.setProStatuses(new ArrayList<ProStatus>());
        response.setWarnings(new ArrayList<DataValidationError>());

        List<String> formattedProNbrList = validateRequestAndGetFormattedProNbrs(listProStatusRqst, response, txnContext);
        if (formattedProNbrList.isEmpty())
            return response;

        List<ProFrtBillIndex> proFrtBillIndexDBList = proFrtBillIndexSubDAO.findAllByProNbrList(formattedProNbrList, entityManager);

        for(String formattedProNbr : formattedProNbrList) {
            Optional<ProFrtBillIndex> proFrtBillIndexOpt = proFrtBillIndexDBList
                .stream()
                .filter(proFrtBillIx -> formattedProNbr.equals(proFrtBillIx.getProNbrTxt()))
                .findFirst();

            if (proFrtBillIndexOpt.isPresent()) {
                ProFrtBillIndex proFrtBillIndex = proFrtBillIndexOpt.get();
                ProStatus proStatus = new ProStatus();
                proStatus.setBillSicCd(proFrtBillIndex.getBillSicCd());
                proStatus.setProNbr(proFrtBillIndex.getProNbrTxt());
                proStatus.setMovementUnitSequenceNbr(proFrtBillIndex.getMvmtUnitSeqNbr().toBigInteger().toString());
                proStatus.setShipmentInstId(proFrtBillIndex.getShpInstId().longValue());
                proStatus.setProStatusCd(ProStatusCdTransformer.toEnum(proFrtBillIndex.getStatCd()));
                response.getProStatuses().add(proStatus);
            } else {
                ProNumber proNumber = ProNumber.from(formattedProNbr);
                String prefix = getProPfxTxt(proNumber);
                String suffix = getProSfxTxt(proNumber);

                ProFrtBillRange proFrtBillRange = proFrtBillRangeSubDAO.findByPfxAndSfx(prefix, suffix, suffix, entityManager);

                if (proFrtBillRange != null) {
                    ProStatus proStatus = new ProStatus();
                    proStatus.setBillSicCd(proFrtBillRange.getOrigSicCd());
                    proStatus.setMovementUnitSequenceNbr(BigInteger.ZERO.toString());
                    proStatus.setProNbr(formattedProNbr);
                    proStatus.setShipmentInstId(0l);
                    proStatus.setProStatusCd(ProStatusCd.AVAILABLE);
                    response.getProStatuses().add(proStatus);
                } else {
                    ProFrtBillRange proFrtBilRnge = proFrtBillRangeSubDAO.findByPfxTxt(prefix, entityManager);
                    ValidationErrorMessage valErrorMsg = (proFrtBilRnge != null) ? ValidationErrorMessage.PRO_NBR_RANGE_INVALID :
                        ValidationErrorMessage.PRO_NBR_PREFIX_INVALID;
                    addDataValidationErrorToResponse(valErrorMsg, "ProNbr", formattedProNbr, response);
                }
            }
        }

		return response;
	}

	private String getProPfxTxt(ProNumber proNumber) {
		return Strings.padStart(BasicTransformer.toString(proNumber.getPrefix()), 3, '0');
	}

	private String getProSfxTxt(ProNumber proNumber) {
	    return Strings.padStart(BasicTransformer.toString(proNumber.getSerial()), 5, '0');
	}

	/**
     * Validate the data on the request.
     */
    protected List<String> validateRequestAndGetFormattedProNbrs(final ListStatusForProsRqst listProStatusRqst,
        final ListStatusForProsResp listProStatusResp,
			final TransactionContext txnContext) throws ValidationException, ServiceException {

        if (listProStatusRqst == null || CollectionUtils.isEmpty(listProStatusRqst.getProNbrs())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext).build();
        }

        return listProStatusRqst.getProNbrs().stream().map(pro -> {
            try {
                return ProNumberHelper.validateProNumber(pro, txnContext);
            } catch (ServiceException e) {
                addDataValidationErrorToResponse(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, "ProNbr", pro, listProStatusResp);
            }
            return StringUtils.EMPTY;
        }).filter(pro -> StringUtils.isNotBlank(pro)).collect(Collectors.toList());
	}

    private void addDataValidationErrorToResponse(ValidationErrorMessage validationErrorMsg, String fieldName, String fieldValue,
        ListStatusForProsResp listProStatusResp) {
        DataValidationError dataValError = new DataValidationError();
        dataValError.setErrorCd(validationErrorMsg.errorCode());
        dataValError.setMessage(validationErrorMsg.message()); //get actual message to return to user
        dataValError.setFieldName(fieldName);
        dataValError.setFieldValue(fieldValue);
        listProStatusResp.getWarnings().add(dataValError);
    }

}
