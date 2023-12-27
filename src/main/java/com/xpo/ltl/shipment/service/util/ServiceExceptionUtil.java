package com.xpo.ltl.shipment.service.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dozer.Mapper;

import com.xpo.ltl.api.exception.AbstractApiExceptionBuilder;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;

public class ServiceExceptionUtil {

    private ServiceExceptionUtil() {
    }

    public static ServiceException createException(TransactionContext txnContext,
                                                   Throwable e,
                                                   ServiceErrorMessage errorMessage,
                                                   String... contextValues) {
        ListOrderedSet<MoreInfo> infos = ListOrderedSet.listOrderedSet(new TreeSet<>(new MoreInfoComparator()));
        for (Throwable cause : ExceptionUtils.getThrowableList(e)) {
            if (cause instanceof ServiceException)
                infos.addAll(extractMoreInfos((ServiceException) cause));
            if (cause instanceof com.xpo.ltl.api.client.exception.ServiceException)
                infos.addAll(convertMoreInfos((com.xpo.ltl.api.client.exception.ServiceException) cause));
        }

        AbstractApiExceptionBuilder<ServiceException> builder =
            ExceptionBuilder
                .exception(errorMessage, txnContext)
                .contextValues(contextValues);

        if (!CollectionUtils.isEmpty(infos))
            builder = builder.moreInfo(infos.asList());

        if (!infos.stream().anyMatch(i -> StringUtils.equals(i.getLocation(), "rootCause")))
            builder = builder.log(e);

        ServiceException exception = builder.build();

        if (e != null)
            try {
                exception.initCause(e);
            }
            catch (@SuppressWarnings("unused") IllegalStateException ignore) {
                // Newer version of ExceptionBuilder from xpo-ltl-util-rest
                // dependency may have already chained the exception
            }

        return exception;
    }

    public static List<MoreInfo> extractMoreInfos(ServiceException e) {
        if (e == null
            || e.getFault() == null
            || CollectionUtils.isEmpty(e.getFault().getMoreInfo()))
            return new ArrayList<>();
        return e.getFault().getMoreInfo();
    }

    public static List<com.xpo.ltl.api.client.exception.MoreInfo> extractMoreInfos(com.xpo.ltl.api.client.exception.ServiceException e) {
        if (e == null
            || e.getFault() == null
            || CollectionUtils.isEmpty(e.getFault().getMoreInfo()))
            return new ArrayList<>();
        return e.getFault().getMoreInfo();
    }

    public static List<MoreInfo> convertMoreInfos(com.xpo.ltl.api.client.exception.ServiceException e) {
        return map(DozerMapper.getInstance(),
                   extractMoreInfos(e),
                   MoreInfo.class);
    }

    /**
     * Maps the source {@linkplain List} instance to target class {@code destType}
     * using dozer mapper.
     */
    private static <S, D> List<D> map(Mapper mapper, List<S> source, Class<D> destType) {
        if (mapper == null || CollectionUtils.isEmpty(source))
            return new ArrayList<>();
        return source
            .stream()
            .map(s -> mapper.map(s, destType))
            .collect(Collectors.toList());
    }

    private static class MoreInfoComparator implements Comparator<MoreInfo> {

        public MoreInfoComparator() {
        }

        @Override
        public int compare(MoreInfo o1, MoreInfo o2) {
            return CompareToBuilder.reflectionCompare(o1, o2);
        }

    }

}
