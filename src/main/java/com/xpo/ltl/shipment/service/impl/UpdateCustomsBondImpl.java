package com.xpo.ltl.shipment.service.impl;


import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBondPK;
import com.xpo.ltl.api.shipment.transformer.v2.ArchiveControlCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BondReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BondStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BondTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.CrcAuditCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.CrcStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentDirectionCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.ArchiveControlCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BondStatusCd;
import com.xpo.ltl.api.shipment.v2.BondTypeCd;
import com.xpo.ltl.api.shipment.v2.CrcAuditCd;
import com.xpo.ltl.api.shipment.v2.CustomsBond;
import com.xpo.ltl.api.shipment.v2.UpsertCustomsBondRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmCustomsBondSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;

import javax.enterprise.context.RequestScoped;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import static com.google.common.base.Preconditions.checkNotNull;

@RequestScoped
public class UpdateCustomsBondImpl {

	private static final String LOCATION = "UpsertCustomsBondImpl";

	private ActionCd allowedAction = ActionCd.NO_ACTION;
	private ShmCustomsBond existingEntity = null;
	
	/*
	 * index <-------> field (mappings for validation of mandatory fields during insert)
	 * 0 - createDate
	 * 1 - statusCd
	 * 2 - typeCd
	 * 3 - city
	 * 4 - stateCd
	 * 5 - shipmentDirectionCd
	 * 6 - bondedSic
	 */
	private Boolean[] validationList = {
		Boolean.FALSE, Boolean.FALSE,
		Boolean.FALSE, Boolean.FALSE,
		Boolean.FALSE, Boolean.FALSE,
		Boolean.FALSE};

	private Map<Integer, String> fieldMap = new HashMap<>();

	@Inject
	ShmCustomsBondSubDAO shmCustomsBondSubDAO;

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;
	
	public void upsertCustomsBond(UpsertCustomsBondRqst upsertCustomsBondRqst, TransactionContext txnContext, EntityManager entityManager) 
			throws ServiceException, ParseException{

		ActionCd possibleAction;

		checkNotNull(upsertCustomsBondRqst, "The request is required.");
		checkNotNull(upsertCustomsBondRqst.getCustomsBond(),"The Customs Bond entity is required.");
		checkNotNull(upsertCustomsBondRqst.getCustomsBond().getShipmentInstId(),"ShipmentInstId is required.");
		
		populateFieldMap();
		if(validateCustomsBondRqst(upsertCustomsBondRqst)) {

			if(upsertCustomsBondRqst.getCustomsBond().getListActionCd() == ActionCd.ADD){

				List<Long> shpInstIdList = Arrays.asList(upsertCustomsBondRqst.getCustomsBond().getShipmentInstId());

				Optional<ShmCustomsBond> inbond = 
						CollectionUtils.emptyIfNull(
								shmCustomsBondSubDAO.findByShpInstIds(shpInstIdList, entityManager))
						.stream()
						.filter(Objects::nonNull)
						.sorted((o1, o2) -> 
								BasicTransformer.toInt(o2.getId().getSeqNbr()) - BasicTransformer.toInt(o1.getId().getSeqNbr()))
						.findFirst();

				existingEntity = inbond.orElse(null);

			}
			else if(upsertCustomsBondRqst.getCustomsBond().getListActionCd() == ActionCd.UPDATE){
				if(upsertCustomsBondRqst.getCustomsBond().getSequenceNbr() == null){
					throw ExceptionBuilder
						.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
						.moreInfo(LOCATION, "Entered information is not sufficient to uniquely identify an inbond record, please provide sequence number of the record to update.")
						.build();
				}
				if(upsertCustomsBondRqst.getCustomsBond().getBondStatusCd() == BondStatusCd.CLEARED
					&& StringUtils.isBlank(upsertCustomsBondRqst.getCustomsBond().getBondClearDate())){
						throw ExceptionBuilder
						.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
						.moreInfo(LOCATION, "Please provide the BondClearDate while clearing a bond.")
						.build();
				}

				ShmCustomsBondPK pk = new ShmCustomsBondPK();
				pk.setShpInstId(upsertCustomsBondRqst.getCustomsBond().getShipmentInstId());
				pk.setSeqNbr(BasicTransformer.toLong(upsertCustomsBondRqst.getCustomsBond().getSequenceNbr()));
				existingEntity = shmCustomsBondSubDAO.findById(pk, entityManager);
				//throw errors when these fields are being updated for an inbond record and existing entity was not null
				if(upsertCustomsBondRqst.getCustomsBond().getShipmentDirectionCd() != null
					&& StringUtils.isNotBlank(existingEntity.getShipmentDirectionCd())){
					throw ExceptionBuilder
						.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
						.moreInfo(LOCATION, "ShipmentDirectionCd cannot be updated once the bond has been created.")
						.build();
				}
			}
			if(existingEntity != null) {

				if (upsertCustomsBondRqst.getCustomsBond().getListActionCd() == ActionCd.ADD) {

					throw ExceptionBuilder
							.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
							.moreInfo(LOCATION, "Bond can't be created as a bond already exists on this shipment.")
							.build();

				}

				//If the bond is already Cleared, do not allow any updates
				if(BondStatusCdTransformer.toEnum(existingEntity.getBondStatusCd()) == BondStatusCd.CLEARED 
						&& upsertCustomsBondRqst.getCustomsBond().getListActionCd() != ActionCd.ADD
						&& upsertCustomsBondRqst.getCustomsBond().getCrcAuditCd() == null) {
					throw ExceptionBuilder
					.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
					.moreInfo(LOCATION, "Bond on this shipment has been cleared and cannot be updated.")
					.build();
				}
				if(BondStatusCdTransformer.toEnum(existingEntity.getBondStatusCd()) == BondStatusCd.CLEARED 
						&& upsertCustomsBondRqst.getCustomsBond().getListActionCd() == ActionCd.UPDATE
						&& upsertCustomsBondRqst.getCustomsBond().getCrcAuditCd() != null){
					//audits generally happen after a bond is cleared, allow this update
					possibleAction = ActionCd.UPDATE;
				}
				else {
					if(allowedAction == ActionCd.UPDATE)
						possibleAction = ActionCd.UPDATE;
					else {
						checkMandatoryFields(txnContext);
						possibleAction = ActionCd.ADD;
					}		
				}
			}
			else {
				checkMandatoryFields(txnContext);
				possibleAction = ActionCd.ADD;
			}
		}
		else {
			checkMandatoryFields(txnContext);
			throw ExceptionBuilder
				.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
				.moreInfo(LOCATION, "Upsert operation not allowed.")
				.build();
		}
		if(ObjectUtils.equals(possibleAction, allowedAction)) {
			AuditInfo auditInfo;
			auditInfo = AuditInfoHelper.getAuditInfo(txnContext);
			auditInfo.setUpdatedTimestamp(AuditInfoHelper.getTransactionTimestamp(txnContext));
			
			ShmCustomsBond record = new ShmCustomsBond();
			
			if(possibleAction == ActionCd.ADD)
				record = createInsertEntity(upsertCustomsBondRqst.getCustomsBond(), entityManager, txnContext);
			else if(possibleAction == ActionCd.UPDATE)
				record = createUpdateEntity(upsertCustomsBondRqst.getCustomsBond(), txnContext);

			DtoTransformer.setAuditInfo(record, auditInfo);
			try {
				shmCustomsBondSubDAO.persist(record, entityManager);
				//two phase commit - exadata and dB2

				populateDefaultValuesForNull(record);
				if(possibleAction == ActionCd.ADD){
					shmCustomsBondSubDAO.createDB2ShmCustomsBond(record, db2EntityManager);
				}
				else{
					shmCustomsBondSubDAO.updateDB2ShmCustomsBond(record, db2EntityManager, txnContext);
				}
			}
			catch(final ServiceException e) {
				throw ExceptionBuilder
				.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
				.log()
				.moreInfo("SQL", "Unable to persist the entity for Customs Bond")
				.build();
			}
		}
		else {
			checkMandatoryFields(txnContext);
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
			.moreInfo(LOCATION, "Upsert operation not allowed.")
			.build();
		}
	}

	private void checkMandatoryFields(TransactionContext txnContext) throws ValidationException {
		int[] position = {-1};
		final Optional<Boolean> record = Arrays.stream(validationList)
		.peek(x -> position[0]++)
		.filter(Boolean.FALSE::equals)
		.findFirst();
		if(record.isPresent()){
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
			.moreInfo(LOCATION, String.format("Mandatory field %s is missing from the payload", fieldMap.get(position[0])))
			.build();
		}
	}

	private void populateFieldMap() {
		fieldMap.put(0, "bondCreateDate");
		fieldMap.put(1, "bondStatusCd");
		fieldMap.put(2, "bondTypeCd");
		fieldMap.put(3, "city");
		fieldMap.put(4, "state");
		fieldMap.put(5, "shipmentDirectionCd");
		fieldMap.put(6, "bondedSic");
	}

	private ShmCustomsBond createUpdateEntity(CustomsBond bond, TransactionContext txnContext) throws ParseException, ValidationException {
		if(bond.getBondStatusCd() == BondStatusCd.CLEARED) {
			existingEntity.setBondStatusCd(BondStatusCdTransformer.toCode(BondStatusCd.CLEARED));
			if(bond.getCrcStatusCd() != null) {
				existingEntity.setCrcStatusCd(CrcStatusCdTransformer.toCode(bond.getCrcStatusCd()));
			}
			try{
				Date dateCleared = BasicTransformer.toDate(bond.getBondClearDate());
				if(dateCleared.compareTo(existingEntity.getBondCreateDt()) < 0){
					throw ExceptionBuilder
					.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
					.moreInfo(LOCATION, "BondClearDate cannot be before BondCreateDate")
					.build();
				}
				existingEntity.setBondClearDt(dateCleared);
			}
			catch(final RuntimeException e){
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
				.moreInfo(LOCATION, "Please provide a valid date when the bond was cleared in the format: MM/DD/YYYY.")
				.build();
			}
		}
		else if(BondStatusCdTransformer.toEnum(existingEntity.getBondStatusCd()) != BondStatusCd.CLEARED
				&& bond.getBondClearDate() != null){
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
			.moreInfo(LOCATION, "Bond clear date cannot be set unless the bond has been cleared.")
			.build();
		}
		else if(BondStatusCdTransformer.toEnum(existingEntity.getBondStatusCd()) == BondStatusCd.CLEARED
				&& bond.getCrcAuditCd() != null){
			//update CrcAuditCd details for a cleared shipment - only update allowed on a cleared shipment
			existingEntity.setCrcAuditCd(CrcAuditCdTransformer.toCode(bond.getCrcAuditCd()));
			if(bond.getCrcStatusCd() != null) {
				existingEntity.setCrcStatusCd(CrcStatusCdTransformer.toCode(bond.getCrcStatusCd()));
			}
		}
		else if(BondStatusCdTransformer.toEnum(existingEntity.getBondStatusCd()) != BondStatusCd.CLEARED){
			if(existingEntity.getBondNbrTxt() != bond.getBondNbr()
					&& StringUtils.isNotBlank(bond.getBondNbr())) {
				existingEntity.setBondNbrTxt(bond.getBondNbr());
			}
			if(!StringUtils.equals(existingEntity.getBondTypeCd(),BondTypeCdTransformer.toString(bond.getBondTypeCd()))
					&& bond.getBondTypeCd() != null) {
				if((BondTypeCdTransformer.toEnum(existingEntity.getBondTypeCd()) == BondTypeCd.PLANNED
					|| StringUtils.isBlank(existingEntity.getBondTypeCd()))
					&& bond.getBondTypeCd() == BondTypeCd.UNPLANNED){
						if(bond.getBondReasonCd() != null){
								existingEntity.setBondTypeCd(BondTypeCdTransformer.toCode(bond.getBondTypeCd()));
						}
						else{
							throw ExceptionBuilder
							.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
							.moreInfo(LOCATION, "Unplanned bonds need to have a valid bondReasonCd to go through with this operation.")
							.build();
						}
				}
				else if(BondTypeCdTransformer.toEnum(existingEntity.getBondTypeCd()) == BondTypeCd.UNPLANNED
						|| StringUtils.isBlank(existingEntity.getBondTypeCd())){
					existingEntity.setBondTypeCd(BondTypeCdTransformer.toCode(bond.getBondTypeCd()));
				}
			}
			if(existingEntity.getCtyTxt() != bond.getCity()
					&& StringUtils.isNotBlank(bond.getCity())) {
				existingEntity.setCtyTxt(bond.getCity());
			}
			if(existingEntity.getStCd() != bond.getStateCd()
					&& StringUtils.isNotBlank(bond.getStateCd())) {
				existingEntity.setStCd(bond.getStateCd());
			}
			if(bond.getBondStatusCd() != null) {
				existingEntity.setBondStatusCd(BondStatusCdTransformer.toCode(bond.getBondStatusCd()));
			}
			if(bond.getBondReasonCd() != null
				&& BondReasonCdTransformer.toEnum(existingEntity.getBondReasonCd()) != bond.getBondReasonCd()){
				existingEntity.setBondReasonCd(BondReasonCdTransformer.toCode(bond.getBondReasonCd()));
			}
			if(bond.getBondReasonCd() == null && 
					(bond.getBondTypeCd() != null && bond.getBondTypeCd() == BondTypeCd.PLANNED)) {
					existingEntity.setBondReasonCd(null);
			}
			if(bond.getBondValueAmount() != null
			   && existingEntity.getBondValueAmt() != BasicTransformer.toBigDecimal(bond.getBondValueAmount())
				){
				existingEntity.setBondValueAmt(BasicTransformer.toBigDecimal(bond.getBondValueAmount()));
			}
			if(bond.getBondedPort() != null 
				&& existingEntity.getBondedPort() != bond.getBondedPort()
				){
				existingEntity.setBondedPort(bond.getBondedPort());
			}
			if(bond.getBroker() != null 
				&& existingEntity.getBroker() != bond.getBroker()){
				existingEntity.setBroker(bond.getBroker());
			}
			if(bond.getBondedSicCd() != null
				&& existingEntity.getBondedSicCode() != bond.getBondedSicCd()){
				existingEntity.setBondedSicCode(bond.getBondedSicCd());
			}
			if(bond.getBondCreateDate() != null){
				existingEntity.setBondCreateDt(BasicTransformer.toDate(bond.getBondCreateDate()));
			}
			if(bond.getCrcAuditCd() != null 
				&& existingEntity.getCrcAuditCd() != CrcAuditCdTransformer.toCode(bond.getCrcAuditCd())){
				existingEntity.setCrcAuditCd(CrcAuditCdTransformer.toCode(bond.getCrcAuditCd()));
			}
			if(bond.getShipmentDirectionCd() != null){
				//in case bond was created through order-entry: CCS-7163
				existingEntity.setShipmentDirectionCd(ShipmentDirectionCdTransformer.toCode(bond.getShipmentDirectionCd()));
			}
			if(bond.getCrcStatusCd() != null) {
				existingEntity.setCrcStatusCd(CrcStatusCdTransformer.toCode(bond.getCrcStatusCd()));
			}
			existingEntity.setArchiveCntlCd(ArchiveControlCdTransformer.toCode(ArchiveControlCd.ACTIVE_PRO));
		}
		return existingEntity;
	}

	private ShmCustomsBond createInsertEntity(CustomsBond bond, EntityManager entityManager, TransactionContext txnContext) throws ValidationException {
		ShmCustomsBond newRecord = new ShmCustomsBond();
		ShmCustomsBondPK pk = new ShmCustomsBondPK();
		pk.setShpInstId(bond.getShipmentInstId());
		newRecord.setId(pk);
		if(existingEntity != null)
			newRecord.getId().setSeqNbr(existingEntity.getId().getSeqNbr()+1);
		else
			newRecord.getId().setSeqNbr(1L);
		

		// Bond create date logic and error handling
		try{
			if(bond.getBondCreateDate() == null){
				throw ExceptionBuilder
						.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
						.moreInfo(LOCATION, "Please provide the date when the bond was created.")
						.build();
			}
			newRecord.setBondCreateDt(BasicTransformer.toDate(bond.getBondCreateDate()));
		}
		catch(final RuntimeException e){
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
					.moreInfo(LOCATION, "Please provide a valid date when the bond was created in the format: MM/DD/YYYY.")
					.build();
		}
		

		newRecord.setBondNbrTxt(bond.getBondNbr());
		newRecord.setBondStatusCd(BondStatusCdTransformer.toCode((bond.getBondStatusCd() == null)?BondStatusCd.UNCLEARED:bond.getBondStatusCd()));
		newRecord.setCtyTxt(bond.getCity());
		newRecord.setStCd(bond.getStateCd());
		newRecord.setShipmentDirectionCd(ShipmentDirectionCdTransformer.toCode(bond.getShipmentDirectionCd()));
		newRecord.setBondedSicCode(bond.getBondedSicCd());
		newRecord.setArchiveCntlCd(ArchiveControlCdTransformer.toCode(ArchiveControlCd.ACTIVE_PRO));

		//if there's an unplanned bond, we need a reasonCd associated else the API should fail to upsert
		if(bond.getBondTypeCd() == BondTypeCd.UNPLANNED 
			&& bond.getBondReasonCd() != null){
			newRecord.setBondTypeCd(BondTypeCdTransformer.toCode(BondTypeCd.UNPLANNED));
			newRecord.setBondReasonCd(BondReasonCdTransformer.toCode(bond.getBondReasonCd()));
		}
		else if(bond.getBondTypeCd() != BondTypeCd.UNPLANNED){
			newRecord.setBondTypeCd(BondTypeCdTransformer.toCode(bond.getBondTypeCd()));
		}
		else{
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.CUSTOMS_BOND_UPSERT_INVALID, txnContext)
					.moreInfo(LOCATION, "Unplanned bonds need to have a valid bondReasonCd to go through with this operation.")
					.build();
		}
		// add the fields that might or might not be present in the insert object
		// adding extra null checks - removable but can be helpful for scalability
		if(bond.getBondReasonCd() != null){
			newRecord.setBondReasonCd(BondReasonCdTransformer.toCode(bond.getBondReasonCd()));
		}
		if(bond.getBondValueAmount() != null){
			newRecord.setBondValueAmt(BasicTransformer.toBigDecimal(bond.getBondValueAmount()));
		}
		if(bond.getBondedPort() != null){
			newRecord.setBondedPort(bond.getBondedPort());
		}
		if(bond.getBroker() != null){
			newRecord.setBroker(bond.getBroker());
		}
		//default crcAuditCd should be set to not_available, until it has been audited
		newRecord.setCrcAuditCd(CrcAuditCdTransformer.toCode(CrcAuditCd.NOT_AVAILABLE));
		if(bond.getCrcStatusCd() != null) {
			newRecord.setCrcStatusCd(CrcStatusCdTransformer.toCode(bond.getCrcStatusCd()));
		}
		return newRecord;
	}

	private boolean validateCustomsBondRqst(UpsertCustomsBondRqst rqst) {
		if(validInsertRqst(rqst))
			allowedAction = ActionCd.ADD;
		else if(validUpdateRqst(rqst))
			allowedAction = ActionCd.UPDATE;
		return (allowedAction != ActionCd.NO_ACTION);
	}

	private boolean validUpdateRqst(UpsertCustomsBondRqst rqst) {
		CustomsBond bond = rqst.getCustomsBond();
		return
			(bond.getShipmentInstId() 		  != null
				&& bond.getSequenceNbr() 	  != null
				&& 	(( bond.getBondNbr()      != null
					|| bond.getBondTypeCd()   != null
					|| bond.getCity()         != null
					|| bond.getStateCd()      != null
					|| bond.getBondReasonCd() != null
					|| bond.getBondTypeCd()   != null
					||((bond.getBondTypeCd() == BondTypeCd.UNPLANNED)?
					   bond.getBondReasonCd() != null:false)
					|| bond.getBondValueAmount() != null
					|| bond.getBondedPort()   != null
					|| bond.getBroker() 	  != null
					|| bond.getCrcAuditCd()   != null
					|| bond.getBondedSicCd()  != null
					|| bond.getBondCreateDate() != null
					|| bond.getShipmentDirectionCd() != null
					||(bond.getBondStatusCd() != null
					&& bond.getBondStatusCd() != BondStatusCd.CLEARED)
					)^ (bond.getBondStatusCd() == BondStatusCd.CLEARED
					&&	bond.getCrcAuditCd() == null
					&& 	bond.getBondClearDate() != null
					))
					&& bond.getListActionCd() == ActionCd.UPDATE)?
		true:false;
	}

	private boolean validInsertRqst(UpsertCustomsBondRqst rqst) {
		CustomsBond bond = rqst.getCustomsBond();
		return
			bond.getShipmentInstId() 		!= null
				&& checkNullHelper(bond.getBondCreateDate(), 0)
				&& checkNullHelper(bond.getBondStatusCd(), 1)
				&& checkNullHelper(bond.getBondTypeCd(), 2)
				&& checkNullHelper(bond.getCity(), 3)
				&& checkNullHelper(bond.getStateCd(), 4)
				&& checkNullHelper(bond.getShipmentDirectionCd(), 5)
				&& checkNullHelper(bond.getBondedSicCd(), 6)
				&& bond.getListActionCd() 	== ActionCd.ADD;
	}

	private Boolean checkNullHelper(Object obj, int index){
		validationList[index] = (obj != null);
		return validationList[index];
	}

	//CCS-7014: To fix the BatchUpdateException for Null in DB2 two phase commit 
	private void populateDefaultValuesForNull(ShmCustomsBond shmCustomsBondRecord) {

        if (Objects.isNull(shmCustomsBondRecord)) {
            return;
        }
        shmCustomsBondRecord.setArchiveCntlCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getArchiveCntlCd()));
        shmCustomsBondRecord.setBondClearDt(DB2DefaultValueUtil.getValueOrLowTmst(shmCustomsBondRecord.getBondClearDt()));
        shmCustomsBondRecord.setBondCreateDt(DB2DefaultValueUtil.getValueOrLowTmst(shmCustomsBondRecord.getBondCreateDt()));
        shmCustomsBondRecord.setBondNbrTxt(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getBondNbrTxt()));
        shmCustomsBondRecord.setBondReasonCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getBondReasonCd()));
        shmCustomsBondRecord.setBondStatusCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getBondStatusCd()));
        shmCustomsBondRecord.setBondTypeCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getBondTypeCd()));
        shmCustomsBondRecord.setBondValueAmt(DB2DefaultValueUtil.getValueOr0(shmCustomsBondRecord.getBondValueAmt()));
        shmCustomsBondRecord.setBondedPort(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getBondedPort()));
        shmCustomsBondRecord.setBondedSicCode(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getBondedSicCode()));
        shmCustomsBondRecord.setBroker(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getBroker()));
        shmCustomsBondRecord.setCrcAuditCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getCrcAuditCd()));
        shmCustomsBondRecord.setCrcStatusCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getCrcStatusCd()));
        shmCustomsBondRecord.setCtyTxt(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getCtyTxt()));
        shmCustomsBondRecord.setDmlTmst(DB2DefaultValueUtil.getValueOrLowTmst(shmCustomsBondRecord.getDmlTmst()));
        shmCustomsBondRecord.setDtlCapxtimestamp(DB2DefaultValueUtil.getValueOrLowTmst(shmCustomsBondRecord.getDtlCapxtimestamp()));
        shmCustomsBondRecord.setLstUpdtTmst(DB2DefaultValueUtil.getValueOrLowTmst(shmCustomsBondRecord.getLstUpdtTmst()));
        shmCustomsBondRecord.setLstUpdtTranCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getLstUpdtTranCd()));
        shmCustomsBondRecord.setLstUpdtUid(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getLstUpdtUid()));
        shmCustomsBondRecord.setReplLstUpdtTmst(DB2DefaultValueUtil.getValueOrLowTmst(shmCustomsBondRecord.getReplLstUpdtTmst()));
        shmCustomsBondRecord.setShipmentDirectionCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getShipmentDirectionCd()));
        shmCustomsBondRecord.setStCd(DB2DefaultValueUtil.getValueOrSpace(shmCustomsBondRecord.getStCd()));
    }

	
}
