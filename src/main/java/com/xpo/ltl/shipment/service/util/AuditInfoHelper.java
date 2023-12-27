package com.xpo.ltl.shipment.service.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Timestamp;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;

/**
 * A utility class for retrieving information from or setting information on {@link AuditInfo}s.
 */
public class AuditInfoHelper
{
	private static final long serialVersionUID = 1L;

	/**
	 * The default user ID to use if none is set.
	 */
	static final String DEFAULT_USER_ID = "LTLAPP_USER";

	/**
	 * The default program ID to use if none is set.
	 */
	static final String DEFAULT_PGM_ID = "LTLGENR";
	
	/**
	 * The default correlation ID to use if none is set.
	 */
	static final String DEFAULT_CORRELATION_ID = "TEST";

	/**
	 * The maximum length an application ID can be.
	 */
	static final int MAX_APPLICATION_ID_LENGTH = 8;

	/**
	 * The maximum length of a user ID.
	 */
	static final int MAX_USER_ID_LENGTH = 15;

	/**
	 * Returns the timestamp on the {@link TransactionContext} or the {@link #getCurrentTimestamp()}
	 * if it is {@code null}.
	 *
	 * @param txnContext The {@link TransactionContext} to use.
	 * @return The timestamp from the {@link TransactionContext} or the
	 *         {@link #getCurrentTimestamp()} if none is available on the {@link TransactionContext}
	 *         or if {@code txnContext} is {@code null}.
	 */
	public static XMLGregorianCalendar getTransactionTimestamp(final TransactionContext txnContext)
	{
		if (txnContext == null || txnContext.getTransactionTimestamp() == null) {
			return getCurrentTimestamp();
		}
		else {
			return txnContext.getTransactionTimestamp();
		}
	}

	/**
	 * Returns an {@link XMLGregorianCalendar} initialized to the current date and time.
	 *
	 * @return A {@link XMLGregorianCalendar} with the current date and time.
	 */
	private static XMLGregorianCalendar getCurrentTimestamp()
	{
		return DtoTransformer.toXMLGregorianCalendar(new Timestamp(System.currentTimeMillis()));
	}

	/**
	 * Returns the source application ID from the {@link TransactionContext}.
	 *
	 * @param txnContext The {@link TransactionContext} to pull the source application ID from.
	 * @return The value of the {@link TransactionContext#getSrcApplicationId()} if it is not
	 *         {@code null}, if it is {@code null} or if {@code txnContext} is {@code null} then the
	 *         default {@link #DEFAULT_PGM_ID} is returned instead. The maximum length of the string
	 *         returned is defined by {@link #MAX_APPLICATION_ID_LENGTH}.
	 */
	public static String getTransactionApplicationId(final TransactionContext txnContext)
	{
		final String applicationId = txnContext != null && txnContext.getSrcApplicationId() != null
			? txnContext.getSrcApplicationId()
			: DEFAULT_PGM_ID;

		return StringUtils.substring(applicationId, 0, MAX_APPLICATION_ID_LENGTH);
	}

	/**
	 * Returns the user ID from the {@link TransactionContext}.
	 *
	 * @param txnContext The {@link TransactionContext} to retrieve the user ID from.
	 * @return Returns the value of the {@link User#getEmployeeId()} or
	 *         {@link User#getProfileInstId()} from the {@link TransactionContext} or returns
	 *         {@link #DEFAULT_USER_ID} if {@code txnContext}, {@code txnContext.getUser()} or
	 *         {@code txnContext.getUser().getEmployeeId()} or
	 *         {@code txnContext.getUser().getProfileInstId()} is {@code null}. The maximum length
	 *         of the string returned is defined by {@link #MAX_USER_ID_LENGTH}.
	 */
	public static String getTransactionUserId(final TransactionContext txnContext)
	{
		String userId = DEFAULT_USER_ID;
		if (txnContext != null && txnContext.getUser() != null) {
			if (txnContext.getUser().getEmployeeId() != null) {
				userId = txnContext.getUser().getEmployeeId();
			}
			else if (txnContext.getUser().getProfileInstId() != null) {
				userId = txnContext.getUser().getProfileInstId();
			}
		}

		return StringUtils.substring(userId, 0, MAX_USER_ID_LENGTH);
	}

	public static String getTransactionUserEmailAddress(final TransactionContext txnContext)
	{
		if (txnContext == null || txnContext.getUser() == null) {
			return null;
		}

		return txnContext.getUser().getEmailAddress();
	}
	
	public static String getTransactionCorrelationId(final TransactionContext txnContext) {
		return txnContext != null && txnContext.getCorrelationId() != null ? txnContext.getCorrelationId()
				: DEFAULT_CORRELATION_ID;
	}

	/**
	 * Sets the created and updated audit information. This is a composite of
	 * {@link #setCreatedInfo(AuditInfo, TransactionContext)} and
	 * {@link #setUpdatedInfo(AuditInfo, TransactionContext)}.
	 *
	 * @param auditInfo The audit info object on which to set the information.
	 * @param txnContext The transaction context to pull the information from.
	 * @throws NullPointerException if {@code auditInfo} or {@code txnContext} is {@code null}.
	 */
	public static void setAuditInfo(final AuditInfo auditInfo, final TransactionContext txnContext)
	{
		setCreatedInfo(auditInfo, txnContext);
		setUpdatedInfo(auditInfo, txnContext);
	}

	/**
	 * Sets the {@link AuditInfo#getCreatedTimestamp()}, {@link AuditInfo#getCreateByPgmId()} and
	 * {@link AuditInfo#getCreatedById()} based on the information within the
	 * {@link TransactionContext} or a default if not supplied.
	 *
	 * @param auditInfo The audit info on which to set the information.
	 * @param txnContext The transaction context to pull the information from.
	 * @throws NullPointerException if {@code auditInfo} or {@code txnContext} is {@code null}.
	 */
	public static void setCreatedInfo(final AuditInfo auditInfo, final TransactionContext txnContext)
	{
		checkNotNull(auditInfo, "The AuditInfo is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");

		auditInfo.setCreatedTimestamp(getTransactionTimestamp(txnContext));
		auditInfo.setCreateByPgmId(getTransactionApplicationId(txnContext));
		auditInfo.setCreatedById(getTransactionUserId(txnContext));
	}

	/**
	 * Sets the {@link AuditInfo#getUpdatedTimestamp()}, {@link AuditInfo#getUpdateByPgmId()} and
	 * {@link AuditInfo#getUpdateById()} based on the information within the
	 * {@link TransactionContext} or a default if not supplied.
	 *
	 * @param auditInfo The audit info on which to set the information.
	 * @param txnContext The transaction context to pull the information from.
	 * @throws NullPointerException if {@code auditInfo} or {@code txnContext} is {@code null}.
	 */
	public static void setUpdatedInfo(final AuditInfo auditInfo, final TransactionContext txnContext)
	{
		checkNotNull(auditInfo, "The AuditInfo is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");

		auditInfo.setUpdatedTimestamp(getTransactionTimestamp(txnContext));
		auditInfo.setUpdateByPgmId(getTransactionApplicationId(txnContext));
		auditInfo.setUpdateById(getTransactionUserId(txnContext));
	}

	/**
	 * Builds and returns an {@link AuditInfo} based on the {@link TransactionContext}.
	 *
	 * @param txnContext The transaction context to pull the information from.
	 * @return The {@link AuditInfo} with the created and last updated by information being
	 *         populated.
	 * @throws NullPointerException if {@code txnContext} is {@code null}.
	 */
	public static AuditInfo getAuditInfo(final TransactionContext txnContext)
	{
		final AuditInfo auditInfo = new AuditInfo();
		setAuditInfo(auditInfo, txnContext);

		return auditInfo;
	}

	public static String getDefaultUserId()
	{
		return DEFAULT_USER_ID;
	}

    public static AuditInfo getAuditInfoWithPgmId(final String pgmId, final TransactionContext txnContext) {
        final AuditInfo auditInfo = getAuditInfo(txnContext);
        if (StringUtils.isNotBlank(pgmId)) {
            auditInfo.setCreateByPgmId(pgmId);
            auditInfo.setUpdateByPgmId(pgmId);
        }

        return auditInfo;
    }

    public static AuditInfo getAuditInfoWithUserId(final String userId, final TransactionContext txnContext) {
        final AuditInfo auditInfo = getAuditInfo(txnContext);
        if (StringUtils.isNotBlank(userId)) {
            auditInfo.setCreatedById(userId);
            auditInfo.setUpdateById(userId);
        }

        return auditInfo;
    }

    public static AuditInfo getAuditInfoWithPgmAndUserId(final String pgmId, final String userId,
        final TransactionContext txnContext) {
        final AuditInfo auditInfo = getAuditInfoWithUserId(userId, txnContext);
        if (StringUtils.isNotBlank(pgmId)) {
            auditInfo.setCreateByPgmId(pgmId);
            auditInfo.setUpdateByPgmId(pgmId);
        }

        return auditInfo;
    }

    /**
     * @param pgmId
     *            optional pgmId to use, will be used it if it's present.
     * @param defaultPgmId
     *            if the pgmId is not present, this one will be used.
     * @param userId
     * @param txnContext
     * @return
     */
    public static AuditInfo getAuditInfoWithPgmAndUserId(final Optional<String> pgmId, final String defaultPgmId,
        final String userId, TransactionContext txnContext) {

        return AuditInfoHelper.getAuditInfoWithPgmAndUserId(pgmId.orElse(defaultPgmId), userId, txnContext);
    }

}
