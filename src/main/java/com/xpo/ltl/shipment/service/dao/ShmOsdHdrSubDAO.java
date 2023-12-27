package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.jpa.QueryHints;

import com.xpo.ltl.api.shipment.service.dao.ShmOsdHeaderDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader_;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage_;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;

@Dependent
public class ShmOsdHdrSubDAO extends ShmOsdHeaderDAO<ShmOsdHeader> {
	
	private static final String CLOSED = "CLOSED";

	private static final String GET_TRAILER_LAST_CLOSE_QUERY = "SELECT * FROM PKG_OPS_TRAILER_CLOSE.GET_TRAILER_LAST_CLOSE_SIC(:eqpId_pfx, :eqpId_sfx, :dateTime) \r\n";

	public long getOsdId(String seqName, EntityManager entityManager) {
		return getNextSeq(seqName, entityManager);
	}

	public ShmOsdHeader getByProNumber(@NotNull final String proNumber, @NotNull final EntityManager entityManager) {
		checkNotNull(proNumber, "proNumber is required");
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmOsdHeader> cq = cb.createQuery(ShmOsdHeader.class);
		Root<ShmOsdHeader> from = cq.from(ShmOsdHeader.class);

		Predicate proNumberPredicate = cb.equal(from.get(ShmOsdHeader_.proNbrTxt), proNumber);

		cq.select(from).where(proNumberPredicate);

		return getSingleResultOrNull(cq, entityManager);
	}
	
	public List<ShmOsdHeader> getByProNumberOrReportingSic(@NotNull final String proNumber, final String reportingSicCd, @NotNull final EntityManager entityManager) {
		checkNotNull(proNumber, "proNumber is required");
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmOsdHeader> cq = cb.createQuery(ShmOsdHeader.class);
		Root<ShmOsdHeader> from = cq.from(ShmOsdHeader.class);
		
		final Path<String> proNbrPath = from.get(ShmOsdHeader_.proNbrTxt);
		final Path<String> reportingSicCdPath = from.get(ShmOsdHeader_.reportingSicCd);
		
		List<Predicate> predicates = new ArrayList<>();
		
		if (StringUtils.isNotBlank(proNumber)) {
			predicates.add(cb.equal(proNbrPath, proNumber));
		}
		if (StringUtils.isNotBlank(reportingSicCd)) {
			predicates.add(cb.equal(reportingSicCdPath, reportingSicCd));
		}
		
		cq.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmOsdHeader> shmOsdHeaders = entityManager.createQuery(cq).getResultList();

		if (CollectionUtils.isEmpty(shmOsdHeaders))
			return new ArrayList<>();

		return shmOsdHeaders;
	}
	
	public long getNextSequence(String seqName, EntityManager em) {
		return getNextSeq(seqName, em);
	}
	
	public ShmOsdHeader getByOsdIdOrProNumber(final Long osdId, final String proNumber, String reportingSicCd,
			OsdStatusCd osdStatusCd, OsdCategoryCd osdCategoryCd, Boolean isChildPro, EntityManager entityManager) {
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();

		final CriteriaQuery<ShmOsdHeader> cq = cb.createQuery(ShmOsdHeader.class);
		final Root<ShmOsdHeader> from = cq.from(ShmOsdHeader.class);
		final Path<Long> osdIdPath = from.get(ShmOsdHeader_.osdId);
		final Path<String> proNbrPath = from.get(ShmOsdHeader_.proNbrTxt);
		final Path<String> reportingSicCdPath = from.get(ShmOsdHeader_.reportingSicCd);
		final Path<String> osdCategoryCdPath = from.get(ShmOsdHeader_.osdCategoryCd);
		final Path<String> statusCdPath = from.get(ShmOsdHeader_.statusCd);
		final Path<Timestamp> crteTmstPath = from.get(ShmOsdHeader_.crteTmst);

		List<Predicate> predicates = new ArrayList<>();

		if (Objects.nonNull(osdId) && osdId > 0L) {
			predicates.add(cb.equal(osdIdPath, osdId));
		}

		if (isChildPro) {
			predicates.add(cb.equal(osdIdPath, getOsdIdForChildPro(proNumber, reportingSicCd, entityManager)));
		} else if (StringUtils.isNotBlank(proNumber) && !isChildPro) {
			predicates.add(cb.equal(proNbrPath, proNumber));
		}

		if (StringUtils.isNotBlank(reportingSicCd)) {
			predicates.add(cb.equal(reportingSicCdPath, reportingSicCd));
		}
		if (null != osdCategoryCd) {
			predicates.add(cb.equal(osdCategoryCdPath, OsdCategoryCdTransformer.toCode(osdCategoryCd)));
		}
		if (null != osdStatusCd) {
			predicates.add(cb.equal(statusCdPath, OsdStatusCdTransformer.toCode(osdStatusCd)));
		}
		
		cq.select(from).where(predicates.toArray(new Predicate[predicates.size()])).orderBy(cb.desc(crteTmstPath));;

		TypedQuery<ShmOsdHeader> typedQuery = entityManager.createQuery(cq);
		EntityGraph<ShmOsdHeader> entityGraph = entityManager.createEntityGraph(ShmOsdHeader.class);
		entityGraph.addSubgraph("shmMgmtRemarks");
		entityGraph.addSubgraph("shmOsdImages");
		typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);

		final List<ShmOsdHeader> shmOsdHeaders = createQuery(cq, entityManager).getResultList();
		ShmOsdHeader shmOsdHeader = null;
		if (CollectionUtils.isNotEmpty(shmOsdHeaders)) {
			Set<String> statusCds = new HashSet<>();
			for (ShmOsdHeader osdHeader : shmOsdHeaders) {
				String statusCd = (String) osdHeader.getStatusCd();
				if(StringUtils.isNotEmpty(statusCd)) {
					statusCds.add(statusCd);
				}
			}
			
			if (shmOsdHeaders.size() == 1 || statusCds.size() == 1) {
				shmOsdHeader = shmOsdHeaders.get(0);
			} else {
				for (ShmOsdHeader osdHeader : shmOsdHeaders) {
					String statusCd = (String) osdHeader.getStatusCd();
					if (StringUtils.isNotEmpty(statusCd) && !statusCd.contains(CLOSED)) {
						shmOsdHeader = osdHeader;
						break;
					}
				}
			}
		}
		return shmOsdHeader;
	}

	public Long getOsdIdForChildPro(final String childProNbr,  String reportingSicCd, EntityManager entityManager) {
		Long osdId = 0L;
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();

		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<ShmOsdImage> from = cq.from(ShmOsdImage.class);
		final Path<String> proNbrPath = from.get(ShmOsdImage_.proNbrTxt);
		final Path<Timestamp> crteTmstPath = from.get(ShmOsdImage_.crteTmst);
		final Path<String> reportingSicCdPath = from.get(ShmOsdImage_.rptgSicCd);

		List<Predicate> predicates = new ArrayList<>();

		if (StringUtils.isNotBlank(childProNbr)) {
			predicates.add(cb.equal(proNbrPath, childProNbr));
		}
		
		if (StringUtils.isNotBlank(reportingSicCd)) {
			predicates.add(cb.equal(reportingSicCdPath, reportingSicCd));
		}

		cq.multiselect(from.get(ShmOsdImage_.shmOsdHeader).get(ShmOsdHeader_.osdId), from.get(ShmOsdImage_.statusCd))
				.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(cb.desc(crteTmstPath));

		List<Tuple> tupleResult = entityManager.createQuery(cq).getResultList();

		if (CollectionUtils.isNotEmpty(tupleResult)) {
			Set<String> statusCds = new HashSet<>();
			for (Tuple t : tupleResult) {
				String statusCd = (String) t.get(1);
				if(StringUtils.isNotEmpty(statusCd)) {
					statusCds.add(statusCd);
				}
			}
			
			if (tupleResult.size() == 1 || statusCds.size() == 1) {
				osdId = (Long) tupleResult.get(0).get(0);
			} else {
				for (Tuple t : tupleResult) {
					String statusCd = (String) t.get(1);
					if (StringUtils.isNotEmpty(statusCd) && !statusCd.contains(CLOSED)) {
						osdId = (Long) t.get(0);
						break;
					}
				}
			}
		}
		return osdId;
	}
	
	public ShmOsdHeader getByConeAndSicDetails(final String coneColorCd, BigInteger coneNbr, 
			final String reportingSicCd, @NotNull final EntityManager entityManager) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmOsdHeader> cq = cb.createQuery(ShmOsdHeader.class);
		Root<ShmOsdHeader> from = cq.from(ShmOsdHeader.class);
		
		final Path<String> coneColorCdPath = from.get(ShmOsdHeader_.coneColorCd);
		final Path<BigDecimal> coneNbrPath = from.get(ShmOsdHeader_.coneNbr);
		final Path<String> reportingSicCdPath = from.get(ShmOsdHeader_.reportingSicCd);
		final Path<String> statusCdPath = from.get(ShmOsdHeader_.statusCd);
		
		List<Predicate> predicates = new ArrayList<>();
		
		if (StringUtils.isNotBlank(coneColorCd)) {
			predicates.add(cb.equal(coneColorCdPath, coneColorCd));
		}
		if (Objects.nonNull(coneNbr)) {
			predicates.add(cb.equal(coneNbrPath, coneNbr));
		}
		if (StringUtils.isNotBlank(reportingSicCd)) {
			predicates.add(cb.equal(reportingSicCdPath, reportingSicCd));
		}
		
		predicates.add(cb.notLike(statusCdPath, "%_CLOSED"));
		
		cq.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmOsdHeader> shmOsdHeaders = entityManager.createQuery(cq).getResultList();

		if (CollectionUtils.isEmpty(shmOsdHeaders))
			return null;

		return shmOsdHeaders.get(0);
	}

	public String getLastCloseSic(String[] trailerTokens, XMLGregorianCalendar dateTime, EntityManager entityManager) {
		
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(GET_TRAILER_LAST_CLOSE_QUERY);
		final String finishedQuery = queryBuilder.toString();
		Query query = entityManager.createNativeQuery(finishedQuery);
		if(trailerTokens != null 
			&& trailerTokens[0] != null
			&& trailerTokens[0].length() == 3 
			&& StringUtils.isNumericSpace(trailerTokens[0])){
			trailerTokens[0] = trailerTokens[0].replaceAll("\\s","");
			trailerTokens[0] = "0"+trailerTokens[0];
		}
		query.setParameter("eqpId_pfx", trailerTokens[0]);
		query.setParameter("eqpId_sfx", BasicTransformer.toLong(trailerTokens[1]));
		query.setParameter("dateTime", BasicTransformer.toTimestamp(dateTime));
		List<Object[]> obj = query.getResultList();
		String lastSic = "";
		if(!obj.isEmpty()){
			Object[] res = obj.get(0);
			lastSic = (String)res[1];
		}
		return lastSic;
	}
}