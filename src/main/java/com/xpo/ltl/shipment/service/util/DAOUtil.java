package com.xpo.ltl.shipment.service.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.jpa.QueryHints;

import com.google.api.client.util.Lists;
import com.xpo.ltl.api.shipment.v2.ListInfo;
import com.xpo.ltl.api.shipment.v2.SortField;
import com.xpo.ltl.api.shipment.v2.SortOrderCd;

public class DAOUtil {

   private DAOUtil() {
    }
    

    @SafeVarargs
    public static <T> List<T> executeQueryWithGraphs
            (TypedQuery<T> typedQuery,
             EntityGraph<T>... entityGraphs) {
        List<T> results = null;

        if (ArrayUtils.isEmpty(entityGraphs)) {
            results = typedQuery.getResultList();
        }
        else {
            for (EntityGraph<T> entityGraph : entityGraphs) {
                typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
                results = typedQuery.getResultList();
                if (results.isEmpty())
                    break;
            }
        }

        return results;
    }

    @SafeVarargs
    public static <T> List<T> executeQueryWithGraphs
            (EntityManager entityManager,
             CriteriaQuery<T> criteriaQuery,
             EntityGraph<T>... entityGraphs) {
        TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);

        return executeQueryWithGraphs(typedQuery, entityGraphs);
    }

    @SafeVarargs
    public static <T> List<T> executeQueryWithGraphs
            (EntityManager entityManager,
             Class<T> rootEntityType,
             CriteriaQuery<T> criteriaQuery,
             ListAttribute<T, ?>... subgraphAttributes) {
        EntityGraph<T>[] entityGraphs =
            toEntityGraphs(entityManager, rootEntityType, subgraphAttributes);

        return executeQueryWithGraphs
            (entityManager,
             criteriaQuery,
             entityGraphs);
    }

    private static <T, V> List<T> executeQueryWithChunkedInClause
            (Function<Collection<V>, List<T>> executeFunction,
             Collection<V> inClauseValues,
             int initialChunkSize,
             int maxChunkSize) {
        if (CollectionUtils.isEmpty(inClauseValues))
            return Collections.emptyList();

        List<List<V>> chunks = new ArrayList<>();
        chunks.add(new ArrayList<>());

        int valueIndex = 0;
        for (V value : inClauseValues) {
            if (valueIndex < initialChunkSize) {
                chunks.get(0).add(value);
            }
            else {
                int chunkIndex =
                    (valueIndex - initialChunkSize) / maxChunkSize + 1;
                if (chunks.size() < chunkIndex + 1) {
                    chunks.add(new ArrayList<>());
                }
                chunks.get(chunkIndex).add(value);
            }
            valueIndex++;
        }

        List<V> lastChunk = chunks.get(chunks.size() - 1);
        int padCount = initialChunkSize - lastChunk.size();
        if (padCount > 0) {
            for (int i = 0; i < padCount; i++)
                lastChunk.add(null);
        }

        List<T> results = new ArrayList<>();

        for (List<V> chunk : chunks) {
            List<T> chunkResults = executeFunction.apply(chunk);
            results.addAll(chunkResults);
        }

        return results;
    }

    @SafeVarargs
    public static <T, V> List<T> executeQueryWithChunkedInClause
            (EntityManager entityManager,
             CriteriaQuery<T> criteriaQuery,
             Collection<Predicate> predicates,
             Path<V> inClausePath,
             Collection<V> inClauseValues,
             int initialChunkSize,
             int maxChunkSize,
             EntityGraph<T>... entityGraphs) {
        if (CollectionUtils.isEmpty(inClauseValues))
            return Collections.emptyList();

        int predicatesSize = CollectionUtils.size(predicates) + 1;
        Predicate[] predicatesAsArray = new Predicate[predicatesSize];
        int predicateIndex = 0;
        for (Predicate predicate : CollectionUtils.emptyIfNull(predicates)) {
            predicatesAsArray[predicateIndex] = predicate;
            predicateIndex++;
        }

        final int inClausePredicateIndex = predicateIndex;

        Function<Collection<V>, List<T>> executeFunction =
            (chunk) -> {
                predicatesAsArray[inClausePredicateIndex] =
                    inClausePath.in(chunk);

                criteriaQuery.where(predicatesAsArray);

                return executeQueryWithGraphs
                    (entityManager, criteriaQuery, entityGraphs);
            };

        return executeQueryWithChunkedInClause
            (executeFunction,
             inClauseValues,
             initialChunkSize,
             maxChunkSize);
    }

    @SafeVarargs
    public static <T, V> List<T> executeQueryWithChunkedInClause
            (EntityManager entityManager,
             Class<T> rootEntityType,
             CriteriaQuery<T> criteriaQuery,
             Collection<Predicate> predicates,
             Path<V> inClausePath,
             Collection<V> inClauseValues,
             int initialChunkSize,
             int maxChunkSize,
             ListAttribute<T, ?>... subgraphAttributes) {
        EntityGraph<T>[] entityGraphs =
            toEntityGraphs(entityManager, rootEntityType, subgraphAttributes);

        return executeQueryWithChunkedInClause
                (entityManager,
                 criteriaQuery,
                 predicates,
                 inClausePath,
                 inClauseValues,
                 initialChunkSize,
                 maxChunkSize,
                 entityGraphs);
    }

    public static <T, V> List<T> executeQueryWithChunkedInClause
            (EntityManager entityManager,
             CriteriaQuery<T> criteriaQuery,
             Collection<Predicate> predicates,
             Path<V> inClausePath,
             Collection<V> inClauseValues,
             int initialChunkSize,
             int maxChunkSize) {
        return executeQueryWithChunkedInClause
                (entityManager,
                 criteriaQuery,
                 predicates,
                 inClausePath,
                 inClauseValues,
                 initialChunkSize,
                 maxChunkSize,
                 (EntityGraph<T>[]) null);
    }

    public static <T, V> List<T> executeQueryWithChunkedInClause
            (EntityManager entityManager,
             CriteriaQuery<T> criteriaQuery,
             Path<V> inClausePath,
             Collection<V> inClauseValues,
             int chunkSize) {
        return executeQueryWithChunkedInClause
                (entityManager,
                 criteriaQuery,
                 null,
                 inClausePath,
                 inClauseValues,
                 chunkSize,
                 chunkSize);
    }

    public static <T, V> List<T> executeQueryWithChunkedInClause
            (TypedQuery<T> typedQuery,
             String inClauseParameter,
             Collection<V> inClauseValues,
             int initialChunkSize,
             int maxChunkSize) {
        Function<Collection<V>, List<T>> executeFunction =
            (chunk) -> {
                typedQuery.setParameter(inClauseParameter, chunk);
                return typedQuery.getResultList();
            };

        return executeQueryWithChunkedInClause
            (executeFunction,
             inClauseValues,
             initialChunkSize,
             maxChunkSize);
    }

    public static <T, V> List<T> executeQueryWithChunkedInClause
            (TypedQuery<T> typedQuery,
             String inClauseParameter,
             Collection<V> inClauseValues,
             int chunkSize) {
        return executeQueryWithChunkedInClause
            (typedQuery,
             inClauseParameter,
             inClauseValues,
             chunkSize,
             chunkSize);
    }

    @SafeVarargs
    private static <T> EntityGraph<T>[] toEntityGraphs
            (EntityManager entityManager,
             Class<T> rootEntityType,
             ListAttribute<T, ?>... subgraphAttributes) {
        List<EntityGraph<T>> entityGraphs = new ArrayList<>();

        if (subgraphAttributes != null) {
            for (ListAttribute<T, ?> subgraph : subgraphAttributes) {
                EntityGraph<T> entityGraph =
                    entityManager.createEntityGraph(rootEntityType);
                entityGraph.addSubgraph(subgraph);
                entityGraphs.add(entityGraph);
            }
        }

        @SuppressWarnings("unchecked")
        EntityGraph<T>[] entityGraphsAsArray =
            entityGraphs.toArray(new EntityGraph[entityGraphs.size()]);
        return entityGraphsAsArray;
    }

    private static Root<?> findRoot(final CriteriaQuery<?> criteriaQuery, final Class<?> criteriaReturnType) {
        for (final Root<?> root : criteriaQuery.getRoots()) {
            if (criteriaReturnType.equals(root.getJavaType())) {
                return root;
            }
        }

        return null;
    }

    private static Join<?, ?> findJoin(final CriteriaQuery<?> criteriaQuery, final Class<?> criteriaReturnType) {
        for (final Root<?> root : criteriaQuery.getRoots()) {
            if (CollectionUtils.isNotEmpty(root.getJoins())) {
                for (final Join<?, ?> join : root.getJoins()) {
                    if (criteriaReturnType.equals(join.getJavaType())) {
                        return join;
                    }
                }
            }
        }

        for (final Root<?> root : criteriaQuery.getRoots()) {
            if (CollectionUtils.isNotEmpty(root.getJoins())) {
                for (final Join<?, ?> join : root.getJoins()) {
                    if (CollectionUtils.isNotEmpty(join.getJoins())) {
                        for (final Join<?, ?> subJoin : join.getJoins()) {
                            if (criteriaReturnType.equals(subJoin.getJavaType())) {
                                return subJoin;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    public static <T> List<T> searchWithSortAndPagination(
         CriteriaQuery<T> criteriaQuery,
         Map<String, List<Pair<SingularAttribute, Class>>> singularAttributeMap,
         ListInfo listInfo,  EntityManager em) {

        if (CollectionUtils.isNotEmpty(listInfo.getSortFields())) {
            addSortingData(criteriaQuery, singularAttributeMap, listInfo, em);
        }

        int startAt = listInfo.getStartAt() == null ? 0 : listInfo.getStartAt().intValue();
        try {
            if (listInfo.getNumberOfRows() == null ) {
                return em.createQuery(criteriaQuery).setFirstResult(startAt).getResultList();
            } else {
                return em.createQuery(criteriaQuery).setMaxResults(listInfo.getNumberOfRows().intValue()).setFirstResult(startAt).getResultList();
            }
        } catch (final NoResultException nre) {
            return Lists.newArrayList();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void addSortingData(
        final CriteriaQuery<?> criteriaQuery,
        final Map<String, List<Pair<SingularAttribute, Class>>> singularAttributeMap,
                                final ListInfo listInfo, final EntityManager em) {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final List<Order> orderList = new ArrayList<>();

        for (final SortField sortField : CollectionUtils.emptyIfNull(listInfo.getSortFields())) {
            final List<Pair<SingularAttribute, Class>> singularAttributeSet = singularAttributeMap
                .get(sortField.getSortByFieldName());

            if (singularAttributeSet == null || singularAttributeSet.isEmpty()) {
                throw new IllegalArgumentException(String.format("The sortFieldName %s is not supported", sortField.getSortByFieldName()));
            }

            for (final Pair<SingularAttribute, Class> singularAttribute : singularAttributeSet) {

                if (singularAttribute == null) {
                    throw new IllegalArgumentException(
                        String.format(
                            "The sortFieldName %s contains a wrong definition.",
                            sortField.getSortByFieldName()));
                }

                if (StringUtils.isNotBlank(sortField.getSortOrder())) {
                    final Root<?> root = findRoot(criteriaQuery, singularAttribute.getRight());
                    Join<?, ?> join = null;
                    if (Objects.isNull(root)) {
                        join = findJoin(criteriaQuery, singularAttribute.getRight());
                    }

                    if (Objects.isNull(root) && Objects.isNull(join)) {
                        throw new IllegalStateException("Unable to find appropriate root");
                    }

                    if (StringUtils.equalsIgnoreCase(SortOrderCd.ASCENDING.value(), sortField.getSortOrder()) 
                            || StringUtils.equalsIgnoreCase("asc", sortField.getSortOrder())) {
                        orderList.add(
                            cb.asc(
                                Objects.nonNull(root) ? root.get(singularAttribute.getLeft()) :
                                    join.get(singularAttribute.getLeft())));
                    } else if (StringUtils.equalsIgnoreCase(SortOrderCd.DESCENDING.value(), sortField.getSortOrder())
                            || StringUtils.equalsIgnoreCase("desc", sortField.getSortOrder())) {
                        orderList.add(
                            cb.desc(
                                Objects.nonNull(root) ? root.get(singularAttribute.getLeft()) :
                                    join.get(singularAttribute.getLeft())));
                    } else {
                        throw new IllegalArgumentException(String.format("Sort order value [%s] not supported"));
                    }
                }
            }

        }

        if (CollectionUtils.isNotEmpty(orderList)) {
            criteriaQuery.orderBy(orderList);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List<Pair<SingularAttribute, Class>> getAttributesFieldAsList(
        final Pair<SingularAttribute, Class>... args) {

        final List<Pair<SingularAttribute, Class>> list = new ArrayList<>(Arrays.asList(args));

        return list;
    }
}
