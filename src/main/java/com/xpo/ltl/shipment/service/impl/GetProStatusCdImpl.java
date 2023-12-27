package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.itextpdf.styledxmlparser.jsoup.helper.StringUtil;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillRange;
import com.xpo.ltl.api.shipment.transformer.v2.ProStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.GetProStatusResp;
import com.xpo.ltl.api.shipment.v2.ProStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.ProNumber;
import com.xpo.ltl.shipment.service.dao.ProFrtBillIndexSubDAO;
import com.xpo.ltl.shipment.service.dao.ProFrtBillRangeSubDAO;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@RequestScoped
public class GetProStatusCdImpl {

	private static final Log logger = LogFactory.getLog(GetProStatusCdImpl.class);


	@Inject
	ProFrtBillIndexSubDAO proFrtBillIndexSubDAO;

	@Inject
	ProFrtBillRangeSubDAO proFrtBillRangeSubDAO;

	public GetProStatusResp getProStatus(final String proNbr,
			final TransactionContext txnContext, final EntityManager entityManager)
			throws ServiceException {

		final Stopwatch sw = Stopwatch.createStarted();
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		GetProStatusResp response = new GetProStatusResp();

		validateRequest(proNbr, txnContext);
		String formattedPRO = ProNumberHelper.validateProNumber(proNbr, txnContext);
		ProFrtBillIndex proFrtBillIndex = proFrtBillIndexSubDAO.findById(formattedPRO, entityManager);
		if (proFrtBillIndex != null) {

			response.setBillSicCd(proFrtBillIndex.getBillSicCd());
			response.setMovementUnitSequenceNbr(proFrtBillIndex.getMvmtUnitSeqNbr().toBigInteger());
			response.setProNbr(proFrtBillIndex.getProNbrTxt());
			response.setShipmentInstId(proFrtBillIndex.getShpInstId().longValue());
			response.setProStatusCd(ProStatusCdTransformer.toEnum(proFrtBillIndex.getStatCd()));

		} else {

			ProNumber proNumber = ProNumber.from(proNbr);
			String prefix = getProPfxTxt(proNumber);
			String suffix = getProSfxTxt(proNumber);

			ProFrtBillRange proFrtBillRange = proFrtBillRangeSubDAO.findByPfxAndSfx(
			    prefix,
			    suffix, 
			    suffix,
				entityManager);
			if (proFrtBillRange != null) {
				response.setBillSicCd(proFrtBillRange.getOrigSicCd());
				response.setMovementUnitSequenceNbr(BigInteger.ZERO);
				response.setProNbr(proNbr);
				response.setShipmentInstId(0l);
				response.setProStatusCd(ProStatusCd.AVAILABLE);
			} else {
				ProFrtBillRange proFrtBilRnge = proFrtBillRangeSubDAO.findByPfxTxt(prefix, entityManager);
				if (proFrtBilRnge != null) {
					throw addMoreInfo(
							ExceptionBuilder.exception(ValidationErrorMessage.PRO_NBR_RANGE_INVALID, txnContext),
							proNbr).log().build();
				} else {
					throw addMoreInfo(
							ExceptionBuilder.exception(ValidationErrorMessage.PRO_NBR_PREFIX_INVALID, txnContext),
							proNbr).log().build();
				}

			}
		}
		return response;
	}
	
	public GetProStatusResp getProStatusDB2(final String proNbr,
			final TransactionContext txnContext, final EntityManager entityManager)
			throws ServiceException {

		final Stopwatch sw = Stopwatch.createStarted();
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		GetProStatusResp response = new GetProStatusResp();

		validateRequest(proNbr, txnContext);
		String formattedPRO = ProNumberHelper.validateProNumber(proNbr, txnContext);
		DB2ProFrtBillIndex proFrtBillIndexDB2 = proFrtBillIndexSubDAO.findDb2ById(formattedPRO, entityManager);

		if (proFrtBillIndexDB2 != null) {

			response.setBillSicCd(proFrtBillIndexDB2.getBillSicCd());
			response.setMovementUnitSequenceNbr(BasicTransformer.toBigInteger(proFrtBillIndexDB2.getMvmtUnitSeqNbr()));
			response.setProNbr(proFrtBillIndexDB2.getProNbrTxt());
			response.setShipmentInstId(proFrtBillIndexDB2.getShpInstId().longValue());
			response.setProStatusCd(ProStatusCdTransformer.toEnum(proFrtBillIndexDB2.getStatCd()));

		} else {
			response.setBillSicCd(StringUtils.EMPTY);
			response.setMovementUnitSequenceNbr(BigInteger.ZERO);
			response.setProNbr(proNbr);
			response.setShipmentInstId(0l);
			response.setProStatusCd(ProStatusCd.AVAILABLE);
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
	 * Validate the data on the request. Pro Number is required
	 * 
	 * @param moreInfo
	 * @param moreInfo
	 * @param getShipmentPartiesResp
	 */
	protected void validateRequest(final String proNbr,
			final TransactionContext txnContext) throws ValidationException, ServiceException {

		// We need a PRO .
		if (proNbr == null) {
			throw addMoreInfo(ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_NBR_RQ, txnContext), proNbr).log().build();

			// If there is a PRO and a shipment instance ID, throw an error -- you can give
			// one or the other, but not both.
		} else if (txnContext == null) {
			throw new NullPointerException("The TransactionContext is required.");
		}
	}


	private ExceptionBuilder<? extends ServiceException> addMoreInfo(
			final ExceptionBuilder<? extends ServiceException> builder, final String proNbr) {
		if (builder == null) {
			throw new NullPointerException("The ExceptionBuilder is required.");
		}
		builder.moreInfo("proNbr", proNbr);

		return builder;
	}
}
