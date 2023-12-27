package com.xpo.ltl.shipment.service.dao;


import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmSalvageRequestDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequest;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequest_;
import com.xpo.ltl.api.shipment.transformer.v2.SalvageRequestStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.SalvageRequestTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.ListInfo;
import com.xpo.ltl.api.shipment.v2.ListSalvageRequestsRqst;
import com.xpo.ltl.api.shipment.v2.SalvageRequest;
import com.xpo.ltl.api.shipment.v2.SalvageRequestStatusCd;
import com.xpo.ltl.api.shipment.v2.SalvageRequestTypeCd;
import com.xpo.ltl.api.shipment.v2.SalvageRequest_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.DAOUtil;

@ApplicationScoped
@LogExecutionTime
public class ShmSalvageRequestSubDAO extends ShmSalvageRequestDAO<ShmSalvageRequest> {

    public ShmSalvageRequest findBySalvageProNbr(
        final String proNbr,
        final EntityManager entityManager) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmSalvageRequest> criteriaQuery = cb.createQuery(ShmSalvageRequest.class);
        Root<ShmSalvageRequest> from = criteriaQuery.from(ShmSalvageRequest.class);

        Predicate proNumberPredicate = cb.equal(from.get(ShmSalvageRequest_.proNbrTxt), proNbr);
        criteriaQuery.select(from).where(proNumberPredicate);
        
        return getSingleResultOrNull(criteriaQuery, entityManager);
    }
    
	public List<ShmSalvageRequest> getShmSalvageRequests(
		final List salvageRequestIds,
		final ListInfo listInfo,
		final EntityManager entityManager) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmSalvageRequest> criteriaQuery = cb.createQuery(ShmSalvageRequest.class);
		Root<ShmSalvageRequest> from = criteriaQuery.from(ShmSalvageRequest.class);

		Predicate proNumberPredicate = from.get(ShmSalvageRequest_.salvageRequestId).in(salvageRequestIds);
		criteriaQuery.select(from).where(proNumberPredicate);
		
		ListInfo listInfoData = new ListInfo();
		if (listInfo != null ) {
		    listInfoData = listInfo;
		}
		List<ShmSalvageRequest> results = DAOUtil
                .searchWithSortAndPagination(criteriaQuery, getSingularAttributeFieldMap(), listInfoData, entityManager); 
		
		return results;
	}
	
	public List<ShmSalvageRequest> getShmSalvageRequestBySearchText(
			final ListSalvageRequestsRqst listSalvageRequestsRqst,
			final EntityManager entityManager) {
			
	    ListInfo listInfo = listSalvageRequestsRqst.getListInfo() != null ? listSalvageRequestsRqst.getListInfo() : new ListInfo();
	    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	    CriteriaQuery<ShmSalvageRequest> cq = cb.createQuery(ShmSalvageRequest.class);
	    Root<ShmSalvageRequest> from = cq.from(ShmSalvageRequest.class);
	    Predicate finalPredicate = getShmSalvageRequestBySearchTextPridicates(listSalvageRequestsRqst, cb, from);
	    cq.select(from).where(finalPredicate);	      

	    List<ShmSalvageRequest> results = DAOUtil
	            .searchWithSortAndPagination(cq, getSingularAttributeFieldMap(), listInfo, entityManager); 

	    return results; 
	}

	private Predicate getShmSalvageRequestBySearchTextPridicates(final ListSalvageRequestsRqst listSalvageRequestsRqst,
			CriteriaBuilder cb, Root<ShmSalvageRequest> from) {
		String searchText = listSalvageRequestsRqst.getSearchQuery();
		final List<Predicate> predicates = new ArrayList<>();
		final List<Predicate> andPredicates = new ArrayList<>();
		if(StringUtils.isNotEmpty(searchText) && isNumeric(searchText)) {
			predicates.add(cb.equal(from.get(ShmSalvageRequest_.salvageRequestId),Long.parseLong(searchText)));	
		}
		predicates.add(cb.like(from.get(ShmSalvageRequest_.salvageRequestName),searchText));
		predicates.add(cb.like(from.get(ShmSalvageRequest_.proNbrTxt),searchText));
		predicates.add(cb.like(from.get(ShmSalvageRequest_.sicCode),searchText));
		predicates.add(cb.like(from.get(ShmSalvageRequest_.statusCd),searchText));
		predicates.add(cb.like(from.get(ShmSalvageRequest_.brand),searchText));
		if(listSalvageRequestsRqst.getRequestType() != null) {
			predicates.add(cb.equal(from.get(ShmSalvageRequest_.statusCd),listSalvageRequestsRqst.getRequestType()));	
		}
		if(listSalvageRequestsRqst.getFromDate() != null && listSalvageRequestsRqst.getToDate() != null) {
			andPredicates.add(cb.between(from.get(ShmSalvageRequest_.crteTmst), localToTimeStamp(listSalvageRequestsRqst.getFromDate(), false), localToTimeStamp(listSalvageRequestsRqst.getToDate(), true)));
		}
		final Predicate predicateOr = cb.or(predicates.toArray(new Predicate[] {}));
		final Predicate predicateAnd = cb.and(andPredicates.toArray(new Predicate[] {}));
		Predicate finalPredicate  = cb.and(predicateOr, predicateAnd);
		return finalPredicate;
	}
	
	public Long getShmSalvageRequestBySearchTextCount(final ListSalvageRequestsRqst listSalvageRequestsRqst,
			final EntityManager entityManager) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ShmSalvageRequest> from = cq.from(ShmSalvageRequest.class);
		
		Predicate finalPredicate  = getShmSalvageRequestBySearchTextPridicates(listSalvageRequestsRqst, cb, from);
		cq.select(cb.count(from));
		cq.where(finalPredicate);
		return entityManager.createQuery(cq).getSingleResult();
	}
	
	public List<ShmSalvageRequest> getShmSalvageRequestBySalvageRequest(
			final ListSalvageRequestsRqst listSalvageRequestsRqst,
			final EntityManager entityManager) {
			ListInfo listInfo = listSalvageRequestsRqst.getListInfo() != null ? listSalvageRequestsRqst.getListInfo() : new ListInfo();
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<ShmSalvageRequest> cq = cb.createQuery(ShmSalvageRequest.class);
			Root<ShmSalvageRequest> from = cq.from(ShmSalvageRequest.class);
			final Predicate predicateOr = getShmSalvageRequestBySalvageRequestPridicates(listSalvageRequestsRqst, cb, from);
	        cq.select(from).where(cb.and(predicateOr));
	        
	        List<ShmSalvageRequest> results = DAOUtil
	                    .searchWithSortAndPagination(cq, getSingularAttributeFieldMap(), listInfo, entityManager); 
	        
	        return results;
	}

	private Predicate getShmSalvageRequestBySalvageRequestPridicates(
			final ListSalvageRequestsRqst listSalvageRequestsRqst, CriteriaBuilder cb,
			Root<ShmSalvageRequest> from) {
		SalvageRequest salvageRequest =  listSalvageRequestsRqst.getSalvageRequest();
		final List<Predicate> predicates = new ArrayList<>();
		if(salvageRequest.getSalvageRequestId() != null) {
			predicates.add(cb.equal(from.get(ShmSalvageRequest_.salvageRequestId),salvageRequest.getSalvageRequestId()));	
		}
		if (salvageRequest.getSalvageRequestName() != null) {
			predicates.add(cb.equal(from.get(ShmSalvageRequest_.salvageRequestName),salvageRequest.getSalvageRequestName()));	
		}
		if(salvageRequest.getProNbr() != null) {
		    predicates.add(cb.equal(from.get(ShmSalvageRequest_.proNbrTxt),salvageRequest.getProNbr()));
		}
		if(salvageRequest.getSicCd() != null) {
			predicates.add(cb.equal(from.get(ShmSalvageRequest_.sicCode),salvageRequest.getSicCd()));	
		}
		if(salvageRequest.getStatusCd() != null) {
			predicates.add(cb.equal(from.get(ShmSalvageRequest_.statusCd),SalvageRequestStatusCdTransformer.toCode(salvageRequest.getStatusCd())));	
		}
		if(salvageRequest.getBrand() != null) {
			predicates.add(cb.equal(from.get(ShmSalvageRequest_.brand),salvageRequest.getBrand()));	
		}
		if(listSalvageRequestsRqst.getRequestType() != null) {
			predicates.add(cb.equal(from.get(ShmSalvageRequest_.statusCd), listSalvageRequestsRqst.getRequestType()));	
		}
		
		final Predicate predicateOr = cb.and(predicates.toArray(new Predicate[] {}));
		return predicateOr;
	}
	
	public Long getShmSalvageRequestBySalvageRequestCount(final ListSalvageRequestsRqst listSalvageRequestsRqst,
			final EntityManager entityManager) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ShmSalvageRequest> from = cq.from(ShmSalvageRequest.class);
		
		Predicate finalPredicate  = getShmSalvageRequestBySalvageRequestPridicates(listSalvageRequestsRqst, cb, from);
		cq.select(cb.count(from));
		cq.where(finalPredicate);
		return entityManager.createQuery(cq).getSingleResult();
	}
	
	public ShmSalvageRequest createShmSalvageRequest(ShmSalvageRequest shmSalvageRequest, final EntityManager entityManager) throws ValidationException {
		setSalvageRequestId(shmSalvageRequest, "SHM_SALVAGE_REQ_SEQ", entityManager);
		save(shmSalvageRequest, entityManager);
		return shmSalvageRequest;
	}
	
	public Long getShmSalvageRequestCountBySalvageRequestType(String Type, final EntityManager entityManager) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Long> query = cb.createQuery(Long.class);
		final Root<ShmSalvageRequest> shmSalvageRequest = query.from(ShmSalvageRequest.class);
	
		List<Predicate> conditions = new ArrayList<>();
		conditions.add(cb.equal(shmSalvageRequest.get(ShmSalvageRequest_.statusCd),
				Type));
		query.select(cb.count(shmSalvageRequest));
		query.where(conditions.toArray(new Predicate[CollectionUtils.size(conditions)]));
		return entityManager.createQuery(query).getSingleResult();
	}
	
	public static boolean isNumeric(String str) {
		  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
	
	public List<ShmSalvageRequest> getShmSalvageRequestApprover(
			final ListSalvageRequestsRqst listSalvageRequestsRqst,
			final EntityManager entityManager) {
			ListInfo listInfo = listSalvageRequestsRqst.getListInfo() != null ? listSalvageRequestsRqst.getListInfo() : new ListInfo();
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<ShmSalvageRequest> cq = cb.createQuery(ShmSalvageRequest.class);
			Root<ShmSalvageRequest> from = cq.from(ShmSalvageRequest.class);
			
			Predicate finalPredicate  = getShmSalvageRequestApproverPridicate(listSalvageRequestsRqst, cb, from,
					entityManager);
			cq.select(from).where(finalPredicate);	

	        List<ShmSalvageRequest> results = DAOUtil
	                .searchWithSortAndPagination(cq, getSingularAttributeFieldMap(), listInfo, entityManager); 
	        
	        return results;
	}
	
	public Long getShmSalvageRequestCountForApprover(final ListSalvageRequestsRqst listSalvageRequestsRqst,
			final EntityManager entityManager) throws ValidationException {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ShmSalvageRequest> from = cq.from(ShmSalvageRequest.class);
		
		Predicate finalPredicate  = getShmSalvageRequestApproverPridicate(listSalvageRequestsRqst, cb, from,
				entityManager);
		cq.select(cb.count(from));
		cq.where(finalPredicate);
		return entityManager.createQuery(cq).getSingleResult();
	}
	
	public Long getFeedbackRequiredShmSalvageRequestCount(
			final EntityManager entityManager) throws ValidationException {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ShmSalvageRequest> from = cq.from(ShmSalvageRequest.class);
		
		Predicate finalPredicate  =  cb.equal(from.get(ShmSalvageRequest_.statusCd), SalvageRequestStatusCdTransformer.toCode(SalvageRequestStatusCd.REQUEST_FOR_FEEDBACK));

		cq.select(cb.count(from));
		cq.where(finalPredicate);
		return entityManager.createQuery(cq).getSingleResult();
	}

	private Predicate getShmSalvageRequestApproverPridicate(
			final ListSalvageRequestsRqst listSalvageRequestsRqst, CriteriaBuilder cb, Root<ShmSalvageRequest> from, final EntityManager entityManager) {
		String searchText = listSalvageRequestsRqst.getSearchQuery();
		SalvageRequest salvageRequest = listSalvageRequestsRqst.getSalvageRequest();
		
		final List<Predicate> orPredicates = new ArrayList<>();
		final List<Predicate> andPredicates = new ArrayList<>();

		if(StringUtils.isNotEmpty(searchText)) {
			searchText = searchText.toUpperCase();
			if(isNumeric(searchText)) {
				orPredicates.add(cb.equal(from.get(ShmSalvageRequest_.salvageRequestId),Long.parseLong(searchText)));	
			}
			orPredicates.add(cb.equal(from.get(ShmSalvageRequest_.salvageRequestName),searchText));
			orPredicates.add(cb.equal(from.get(ShmSalvageRequest_.proNbrTxt),searchText));
			orPredicates.add(cb.equal(from.get(ShmSalvageRequest_.sicCode),searchText));
			orPredicates.add(cb.equal(from.get(ShmSalvageRequest_.brand),searchText));
		}
		
		if(isRequestorFeedbackCall(listSalvageRequestsRqst)) {
			andPredicates.add(from.get(ShmSalvageRequest_.statusCd).in(SalvageRequestStatusCdTransformer.toCode(SalvageRequestStatusCd.REQUEST_FOR_FEEDBACK), 
					SalvageRequestStatusCdTransformer.toCode(SalvageRequestStatusCd.REQUEST_FOR_PHOTOS)));			
		} else if(isSubmittedCall(listSalvageRequestsRqst)) {
			andPredicates.add(cb.equal(from.get(ShmSalvageRequest_.statusCd), SalvageRequestStatusCdTransformer.toCode(SalvageRequestStatusCd.OPEN)));
		} if(salvageRequest != null && salvageRequest.getStatusCd() != null) {
			andPredicates.add(cb.equal(from.get(ShmSalvageRequest_.statusCd), SalvageRequestStatusCdTransformer.toCode(salvageRequest.getStatusCd())));
		} 
		if(salvageRequest != null ) {
			if(salvageRequest.getQualifyTypeCd() != null) {
		    	andPredicates.add(cb.equal(from.get(ShmSalvageRequest_.qualifyTypeCd),salvageRequest.getQualifyTypeCd()));	
		    }
		    if(salvageRequest.getSalvageRequestTypeCd() != null) {
		    	andPredicates.add(cb.equal(from.get(ShmSalvageRequest_.salvageRequestTypeCd), SalvageRequestTypeCdTransformer.toCode(salvageRequest.getSalvageRequestTypeCd())));	
		    }
		    if(salvageRequest.getEstimatedValueCd() != null) {
		    	andPredicates.add(cb.equal(from.get(ShmSalvageRequest_.estimatedValueCd),salvageRequest.getEstimatedValueCd()));	
		    }
			if(salvageRequest.getProNbr() != null) {
				andPredicates.add(cb.equal(from.get(ShmSalvageRequest_.proNbrTxt),salvageRequest.getProNbr()));
			}
			if(salvageRequest.getSicCd() != null) {
				andPredicates.add(cb.equal(from.get(ShmSalvageRequest_.sicCode),salvageRequest.getSicCd().toUpperCase()));
			} 
			if(salvageRequest.getBrand() != null) {
				andPredicates.add(cb.equal(from.get(ShmSalvageRequest_.brand),salvageRequest.getBrand()));	
			}
		}
		if(listSalvageRequestsRqst.getFromDate() != null && listSalvageRequestsRqst.getToDate() != null) {
			andPredicates.add(cb.between(from.get(ShmSalvageRequest_.crteTmst), localToTimeStamp(listSalvageRequestsRqst.getFromDate(), false), localToTimeStamp(listSalvageRequestsRqst.getToDate(), true)));
		}
		final Predicate predicateOr = cb.or(orPredicates.toArray(new Predicate[] {}));
		final Predicate predicateAnd = cb.and(andPredicates.toArray(new Predicate[] {}));
		 
		if(CollectionUtils.isEmpty(orPredicates)) {
		    return cb.and(predicateAnd);
		} else {
			return cb.and(predicateOr, predicateAnd);
		}
	}

	private boolean isRequestorFeedbackCall(final ListSalvageRequestsRqst listSalvageRequestsRqst) {
		return listSalvageRequestsRqst.getRequestType() != null && listSalvageRequestsRqst.getRequestType().equals("REQUESTOR_FEEDBACK_SCREEN");
	}
	
	private boolean isSubmittedCall(final ListSalvageRequestsRqst listSalvageRequestsRqst) {
		return listSalvageRequestsRqst.getRequestType() != null && listSalvageRequestsRqst.getRequestType().equals("SUBMITTED");
	}
	
	 private static Timestamp localToTimeStamp(String dateString, boolean isTo) {
		 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
		 LocalDate localDate = LocalDate.parse(dateString, formatter);
		 if(isTo) {
			 localDate = localDate.plusDays(1);	 
		 }
		 return Timestamp.from(localDate.atStartOfDay().toInstant(ZoneOffset.UTC));
	 }

	    @SuppressWarnings({ "rawtypes", "unchecked" })
	    private static Map<String, List<Pair<SingularAttribute, Class>>> getSingularAttributeFieldMap() {
	        final Map<String, List<Pair<SingularAttribute, Class>>> fieldsMap = new HashMap<>();
	        fieldsMap.put(
	            SalvageRequest_.salvageRequestId.name(),
	            DAOUtil
	                .getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.salvageRequestId, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.salvageRequestName.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.salvageRequestName, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.salvageRequestTypeCd.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.salvageRequestTypeCd, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.proNbr.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.proNbrTxt, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.sicCd.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.sicCode, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.freightDescription.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.freightDescription, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.packagingCd.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.packagingCd, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.piecesCount.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.pcsCnt, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.weightLbs.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.weightLbs, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.statusCd.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.statusCd, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.freightConditionCd.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.freightConditionCd, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.estimatedValueCd.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.estimatedValueCd, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.specialHandlingInd.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.specialHandlingInd, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.lengthNbr.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.lengthNbr, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.widthNbr.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.widthNbr, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.heightNbr.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.heightNbr, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.cubeNbr.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.cubeNbr, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.brand.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.brand, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.statusCd.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.statusCd, ShmSalvageRequest.class)));
	        fieldsMap.put(
	            SalvageRequest_.createdById.name(),
	            DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.crteBy, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.createdTimestamp.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.crteTmst, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.createByPgmId.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.crtePgmId, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.updateById.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.lstUpdtBy, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.updatedTimestamp.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.lstUpdtTmst, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.updateByPgmId.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.lstUpdtPgmId, ShmSalvageRequest.class)));
            fieldsMap.put(
                "auditInfo.createdById",
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.crteBy, ShmSalvageRequest.class)));
            fieldsMap.put(
                "auditInfo.createdTimestamp",
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.crteTmst, ShmSalvageRequest.class)));
            fieldsMap.put(
                "auditInfo.createByPgmId",
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.crtePgmId, ShmSalvageRequest.class)));
            fieldsMap.put(
                "auditInfo.updateById",
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.lstUpdtBy, ShmSalvageRequest.class)));
            fieldsMap.put(
                "auditInfo.updatedTimestamp",
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.lstUpdtTmst, ShmSalvageRequest.class)));
            fieldsMap.put(
                "auditInfo.updateByPgmId",
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.lstUpdtPgmId, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.visibleInd.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.visibleInd, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.searchInd.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.searchInd, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.damageInformationInd.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.damageInfoInd, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.qualifyTypeCd.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.qualifyTypeCd, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.holdForVendorInd.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.holdForVendorInd, ShmSalvageRequest.class)));
            fieldsMap.put(
                SalvageRequest_.abeyanceInd.name(),
                DAOUtil.getAttributesFieldAsList(Pair.of(ShmSalvageRequest_.abeyanceInd, ShmSalvageRequest.class)));
            return fieldsMap;
	    }
}
