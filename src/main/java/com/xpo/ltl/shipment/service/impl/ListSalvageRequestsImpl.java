package com.xpo.ltl.shipment.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.humanresource.v1.Employee;
import com.xpo.ltl.api.humanresource.v1.EmployeeName;
import com.xpo.ltl.api.humanresource.v1.ListEmployeesByEmpIdRqst;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequest;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequestNote;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.ListSalvageRequestsResp;
import com.xpo.ltl.api.shipment.v2.ListSalvageRequestsRqst;
import com.xpo.ltl.api.shipment.v2.SalvageRequest;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmOsdImageSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestNoteSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@ApplicationScoped
@LogExecutionTime
public class ListSalvageRequestsImpl {
	@Inject
	private ShmSalvageRequestSubDAO shmSalvageRequestSubDAO;
	
	@Inject 
	private ShmSalvageRequestNoteSubDAO shmSalvageRequestNoteSubDAO;
	
	@PersistenceContext(unitName = "ltl-java-shipment-rpt-jaxrs")
	private EntityManager rptEntityManager;
	
	@Inject
	private ShmOsdImageSubDAO shmOsdImageDAO;
	
	@Inject
	private ExternalRestClient externalRestClient;
	Long openSalvageRequestCount = new Long(0);
	

	public ListSalvageRequestsResp listSalvageRequests(ListSalvageRequestsRqst listSalvageRequestsRqst,
		final TransactionContext txnContext,
		final EntityManager entityManager
	) throws ServiceException {
		ListSalvageRequestsResp listSalvageRequestsResp = new ListSalvageRequestsResp();
		List<ShmSalvageRequest> shmSalvageRequests = new ArrayList();
		
		if(CollectionUtils.isNotEmpty(listSalvageRequestsRqst.getSalvageRequestIds())) { 
			shmSalvageRequests =  shmSalvageRequestSubDAO.getShmSalvageRequests(listSalvageRequestsRqst.getSalvageRequestIds(), listSalvageRequestsRqst.getListInfo() ,entityManager);
			openSalvageRequestCount = (long) listSalvageRequestsRqst.getSalvageRequestIds().size();
		} else {
			if(listSalvageRequestsRqst.getSalvageRequest() != null && StringUtils.isNotEmpty(listSalvageRequestsRqst.getSalvageRequest().getProNbr())) {
				listSalvageRequestsRqst.getSalvageRequest().setProNbr(handleProNum(listSalvageRequestsRqst.getSalvageRequest().getProNbr(), txnContext));
			} else if(listSalvageRequestsRqst != null && StringUtils.isNotEmpty(listSalvageRequestsRqst.getSearchQuery())) {
				listSalvageRequestsRqst.setSearchQuery(handleProNum(listSalvageRequestsRqst.getSearchQuery(), txnContext));
			}
			shmSalvageRequests =  shmSalvageRequestSubDAO.getShmSalvageRequestApprover(listSalvageRequestsRqst, entityManager);
			openSalvageRequestCount = shmSalvageRequestSubDAO.getShmSalvageRequestCountForApprover(listSalvageRequestsRqst, entityManager);
		}	
		List<SalvageRequest> salvageRequests = EntityTransformer.toSalvageRequest(shmSalvageRequests);
		Iterator<SalvageRequest> iterator = salvageRequests.iterator();
		Map<Long, List<ShmOsdImage>> shmOsdImageMap = new ConcurrentHashMap<>();
		if(displayImageHeader(listSalvageRequestsRqst)) {
			List<Long> salvageRequestIds = shmSalvageRequests.stream().map(ShmSalvageRequest :: getSalvageRequestId).collect(Collectors.toList());
			shmOsdImageMap =  getShmOsdImageMap(salvageRequestIds, entityManager);
		}
		
		List<String> createdByEmployeeids = shmSalvageRequests.stream().map(ShmSalvageRequest :: getCrteBy).collect(Collectors.toList());
		List<String> updatedByEmployeeids = shmSalvageRequests.stream().map(ShmSalvageRequest :: getLstUpdtBy).collect(Collectors.toList());
		ListEmployeesByEmpIdRqst listEmployeesByEmpIdRqst = new ListEmployeesByEmpIdRqst();
		createdByEmployeeids.addAll(updatedByEmployeeids);
		List<String> uniqueEmployeeidList = createdByEmployeeids.stream().distinct().collect(Collectors.toList());
		listEmployeesByEmpIdRqst.setEmployeeId(uniqueEmployeeidList);
		Map<String, String> employeeMap = getEmployeeNameMap(externalRestClient.listEmployeesByEmpIds(listEmployeesByEmpIdRqst, txnContext), entityManager);
		
		
	    while(iterator.hasNext()) {
        	SalvageRequest salvageRequest = iterator.next();
        	AuditInfo auditInfo = salvageRequest.getAuditInfo();
        	String requestorID = salvageRequest.getAuditInfo().getCreatedById();
			auditInfo.setCreatedById(StringUtils.isNotEmpty(employeeMap.get(salvageRequest.getAuditInfo().getCreatedById())) ? employeeMap.get(salvageRequest.getAuditInfo().getCreatedById()) : "SYSTEM");
			auditInfo.setUpdateById(StringUtils.isNotEmpty(employeeMap.get(salvageRequest.getAuditInfo().getUpdateById())) ? employeeMap.get(salvageRequest.getAuditInfo().getUpdateById()) : "SYSTEM");
			if(CollectionUtils.isNotEmpty(listSalvageRequestsRqst.getSalvageRequestIds())) {
	        	Employee createdBy = externalRestClient.getEmployeeDetailsByEmployeeId(requestorID, txnContext);
				auditInfo.setCreateByPgmId(getEmployeeEmail(createdBy));
			}
			salvageRequest.setAuditInfo(auditInfo);
			if(listSalvageRequestsRqst.getRequestType() != null && (listSalvageRequestsRqst.getRequestType().equals("APPROVER_SCREEN"))) {
				List<ShmSalvageRequestNote> shmSalvageRequestNotesList = 
						shmSalvageRequestNoteSubDAO.getShmSalvageReqNotesBySalvageReqId(salvageRequest.getSalvageRequestId(), entityManager);
				salvageRequest.setSalvageRequestNote(EntityTransformer.toSalvageRequestNote(shmSalvageRequestNotesList));	
			}
			if(displayImageHeader(listSalvageRequestsRqst)) {
				salvageRequest.setOsdImage(EntityTransformer.toOsdImage(shmOsdImageMap.get(salvageRequest.getSalvageRequestId())));
			}
        }
	    listSalvageRequestsResp.setFeedbackSalvageRequestCount(String.valueOf(shmSalvageRequestSubDAO.getFeedbackRequiredShmSalvageRequestCount(entityManager)));
		listSalvageRequestsResp.setOpenSalvageRequestCount(String.valueOf(openSalvageRequestCount));
		listSalvageRequestsResp.setSalvageRequests(salvageRequests);
		return listSalvageRequestsResp;
	}

	private boolean displayImageHeader(ListSalvageRequestsRqst listSalvageRequestsRqst) {
		return listSalvageRequestsRqst.getRequestType() != null && (listSalvageRequestsRqst.getRequestType().equals("REQUESTOR_FEEDBACK_SCREEN") 
				|| listSalvageRequestsRqst.getRequestType().equals("APPROVER_SCREEN")
				|| listSalvageRequestsRqst.getRequestType().equals("REQUESTOR_SCREEN"));
	}
	
	private Map<Long, List<ShmOsdImage>>getShmOsdImageMap(List<Long> salvageRequestIds, EntityManager entityManager) {
		ConcurrentHashMap<Long, List<ShmOsdImage>> overageImageMap = new ConcurrentHashMap<>();
		if(CollectionUtils.isEmpty(salvageRequestIds)) {
			return overageImageMap;
		}
		List<ShmOsdImage> listOverageImage = shmOsdImageDAO.getBySalvageRequestIds(salvageRequestIds, entityManager);
		for(ShmOsdImage shmOvrgImgHdr : listOverageImage) {
			List<ShmOsdImage> overage =  new ArrayList<>();
			long salvageId = shmOvrgImgHdr.getShmSalvageRequest().getSalvageRequestId();
			if(overageImageMap.get(salvageId) == null) {
				overage.add(shmOvrgImgHdr);
				overageImageMap.put(salvageId, overage);
			} else {
				overageImageMap.get(salvageId).add(shmOvrgImgHdr);
			}
		}
		return overageImageMap;
	}
	
	private Map<String, String>getEmployeeNameMap(List<EmployeeName> employeeNames, EntityManager entityManager) {
		ConcurrentHashMap<String, String> employeeMap = new ConcurrentHashMap<>();
		if(CollectionUtils.isEmpty(employeeNames)) {
			return employeeMap;
		}
		for(EmployeeName employee : employeeNames) {
			String employeeId = employee.getEmployeeId();
			if(StringUtils.isNotEmpty(employeeId) && employeeMap.get(employeeId) == null) {
				employeeMap.put(employeeId, employee != null ? employee.getFirstName() + " "+employee.getLastName() : "SYSTEM");
			}
		}
		return employeeMap;
	}

	
	private String getEmployeeName(Employee employee) {
		String employeeName =  employee != null ? employee.getBasicInfo().getFirstName() +" "+ employee.getBasicInfo().getLastName() : "SYSTEM";
		return employeeName;
	} 
	
	private String handleProNum(String proNumber, TransactionContext txnContext) throws ServiceException {
		try {
			if(StringUtils.EMPTY.equals(proNumber)) {
				return StringUtils.EMPTY;
			}
			String elevenDigitProNum = ProNumberHelper.validateProNumber(proNumber, txnContext);
			if(StringUtils.isNotBlank(elevenDigitProNum)) {
				if(ProNumberHelper.isYellowPro(elevenDigitProNum)) {
					return ProNumberHelper.isValidChildProNum(elevenDigitProNum);
				}
				if(ProNumberHelper.isBluePro(elevenDigitProNum)) {
					return ProNumberHelper.formatProNineDigit(elevenDigitProNum);
				}
			}
		} catch (ServiceException ex) {
			return proNumber;
		}
		return proNumber;
	}
	
	private String getEmployeeEmail(Employee employee) {
		String employeeEmail = employee != null ? employee.getEmailAddress() : "overage@xpo.com";
		return employeeEmail;
	}

}
