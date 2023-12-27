package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javatuples.Quartet;

import com.google.common.base.Joiner;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentManagementRemarkTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.ListShipmentManagementRemarksResp;
import com.xpo.ltl.api.shipment.v2.ListShipmentManagementRemarksRqst;
import com.xpo.ltl.api.shipment.v2.ManagementRemark;
import com.xpo.ltl.api.shipment.v2.ShipmentManagementRemarkTypeCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentManagementRemarkSubDAO;

@RequestScoped
public class ListShipmentManagementRemarkImpl {
	private static final Log log = LogFactory.getLog(ListShipmentManagementRemarkImpl.class);

	@Inject
	private ShipmentManagementRemarkSubDAO shipmentManagementRemarkDAO;

	public ListShipmentManagementRemarksResp listShipmentManagementRemark(
		ListShipmentManagementRemarksRqst rqst,
		TransactionContext txnContext,
		EntityManager entityManager) {
		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		
		CollectionUtils.emptyIfNull(rqst.getManagementRemarks()).stream()
				.filter(MgmtRemark -> MgmtRemark.getParentShipmentInstId() == null)
				.forEach(MgmtRemark -> MgmtRemark.setParentShipmentInstId(MgmtRemark.getShipmentInstId()));

		List<Long> shpIdsToDisplay = CollectionUtils.emptyIfNull(rqst.getManagementRemarks()).stream()
				.map(ManagementRemark::getParentShipmentInstId).collect(Collectors.toList());

		List<Long> osdIdsToDisplay = CollectionUtils.emptyIfNull(rqst.getManagementRemarks()).stream()
        .map(ManagementRemark::getParentOsdId).collect(Collectors.toList());
		if(!CollectionUtils.isNotEmpty(shpIdsToDisplay))
            log.info(String.format("Retrieving management remarks for shipmentInstIds: %s", Joiner.on(",").join(shpIdsToDisplay)));
        if(!CollectionUtils.isNotEmpty(osdIdsToDisplay))
            log.info(String.format("Retrieving management remarks for osdIds: %s", Joiner.on(",").join(osdIdsToDisplay)));
		final ListShipmentManagementRemarksResp resp = new ListShipmentManagementRemarksResp();	
		List<ManagementRemark> allRemarksList = new ArrayList<>();

		//Group similar remarks
		Map<Quartet<BigInteger,BigInteger, Boolean, ShipmentManagementRemarkTypeCd>, List<ManagementRemark>> groupedRemarkPairs = 
				CollectionUtils.emptyIfNull(rqst.getManagementRemarks()).stream()
				  .collect(Collectors.groupingBy(remark -> new Quartet<BigInteger, BigInteger, Boolean, ShipmentManagementRemarkTypeCd>(
					  remark.getMovementSequenceNbr(), 
					  remark.getMovementExceptionSequenceNbr(),
					  remark.getShowToCustomerInd(),
					  remark.getTypeCd())));
		
		groupedRemarkPairs.forEach((k,v) -> {
			Integer movementSequenceNbr = k.getValue0() != null ? k.getValue0().intValue() : null;
			Integer movementExceptionSequenceNbr = k.getValue1() != null ? k.getValue1().intValue() : null;
			Boolean showToCustomerInd = k.getValue2();
			ShipmentManagementRemarkTypeCd typeCd = k.getValue3();
			List<Long> shipmentInstIds = v.stream().filter(rmk -> rmk.getParentShipmentInstId() != null).map(ManagementRemark::getParentShipmentInstId).collect(Collectors.toList());
			List<Long> osdIds = v.stream().filter(rmk -> rmk.getParentOsdId() != null).map(ManagementRemark::getParentOsdId).collect(Collectors.toList());
            
            if(CollectionUtils.isNotEmpty(shipmentInstIds) 
			|| CollectionUtils.isNotEmpty(osdIds)){

			    final List<Object []> mgmtRemarks = shipmentManagementRemarkDAO.listShipmentManagementRemarks(
                    shipmentInstIds,
                    osdIds,
                    movementSequenceNbr,
                    movementExceptionSequenceNbr,
                    typeCd,
                    showToCustomerInd,
                    entityManager);
				
				final List<ManagementRemark> shmMgmtRemarks = typeCast(mgmtRemarks);
				if (shmMgmtRemarks != null) {
					Map<Long, List<ManagementRemark>> shipInstIdMgmtRemarksMap = shmMgmtRemarks.stream()
							.collect(Collectors.groupingBy(mgmt -> null != mgmt.getShipmentInstId() ? mgmt.getShipmentInstId() : mgmt.getParentOsdId(),
									Collectors.mapping(Function.identity(), Collectors.toList())));

					for (Map.Entry<Long, List<ManagementRemark>> entry : shipInstIdMgmtRemarksMap.entrySet()) {

						entry.getValue().stream().sorted(Comparator.comparing(mgmtRemark -> mgmtRemark.getRemarkId()))
								.collect(Collectors.toList());

						Integer sequenceNbr = 1;
						for (ManagementRemark managementRemark : entry.getValue()) {
							managementRemark.setSequenceNbr(EntityTransformer.toBigInteger(sequenceNbr++));
						}
					}

					CollectionUtils.addAll(allRemarksList, shmMgmtRemarks);
				}
            }

		});
		
		allRemarksList.sort((ManagementRemark a, ManagementRemark b) -> a.getAuditInfo().getCreatedTimestamp().compare(b.getAuditInfo().getCreatedTimestamp()));
		resp.setManagementRemarks(allRemarksList);
		
        if(!CollectionUtils.isNotEmpty(shpIdsToDisplay))
            log.info(String.format("Finished retrieving management remarks for shipmentInstIds: %s", Joiner.on(",").join(shpIdsToDisplay)));
        if(!CollectionUtils.isNotEmpty(osdIdsToDisplay))
            log.info(String.format("Finished retrieving management remarks for osdIds: %s", Joiner.on(",").join(osdIdsToDisplay)));
		
		return resp;

	}

	private List<ManagementRemark> typeCast(List<Object[]> mgmtRemarks) {
		List<ManagementRemark> remarksList = new ArrayList<ManagementRemark>();
		for(Object [] obj: mgmtRemarks){
			ManagementRemark rmk = new ManagementRemark();
			rmk.setRemarkId(BasicTransformer.toLong((BigDecimal)obj[0]));
			rmk.setParentShipmentInstId(BasicTransformer.toLong((BigDecimal)obj[1]));
			rmk.setShipmentInstId(BasicTransformer.toLong((BigDecimal)obj[1]));
			rmk.setParentOsdId(BasicTransformer.toLong((BigDecimal)obj[2]));
			rmk.setMovementSequenceNbr(BasicTransformer.toBigInteger((BigDecimal)obj[3]));
			rmk.setMovementExceptionSequenceNbr(BasicTransformer.toBigInteger((BigDecimal)obj[4]));
			rmk.setRemark((String)obj[5]);
			rmk.setTypeCd(ShipmentManagementRemarkTypeCdTransformer.toEnum((String)obj[6]));
			rmk.setShowToCustomerInd((String)obj[7]=="Y"?true:false);
			AuditInfo auditInfo = new AuditInfo();
			auditInfo.setCreatedById((String)obj[8]);
			auditInfo.setCreatedTimestamp(BasicTransformer.toXMLGregorianCalendar((Timestamp)obj[9]));
			auditInfo.setCreateByPgmId((String)obj[10]);
			auditInfo.setUpdateById((String)obj[11]);
			auditInfo.setUpdatedTimestamp(BasicTransformer.toXMLGregorianCalendar((Timestamp)obj[12]));
			auditInfo.setUpdateByPgmId((String)obj[13]);
			auditInfo.setCorrelationId((String)obj[14]);
			rmk.setAuditInfo(auditInfo);
			remarksList.add(rmk);
		}
		return remarksList;
	}
}