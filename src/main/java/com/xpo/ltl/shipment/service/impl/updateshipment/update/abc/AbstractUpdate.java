package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.google.gson.annotations.SerializedName;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.v2.ChargeToCd;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@RequestScoped
public abstract class AbstractUpdate {

	public static final String MANRATE_TRAN_CD = "RT81";
	public static final String CORRECTION_TRAN_CD = "BC07";
	public static final String AUTORATE_TRAN_CD = "RTS1";
	private static final Logger logger = LogManager.getLogger(AbstractUpdate.class);
	private static final String NO_DIFF = "There is no differences between ";
	private static final String DIFF_FOUND = " Has differences on the following fields -> ";
	public static final String DATE_PATTERN = "yyyy-MM-dd";
	public static final String TIME_PATTERN = "HH:mm:ss";

	@Inject
	protected AppContext appContext;

	protected AbstractUpdate() {
	}

	@LogExecutionTime
	public static Long getMaxSequenceNumberLong(List<Long> collect) {
		long result;

		result = collect.stream().max(Long::compareTo).orElse(0L);
		return result;
	}

	public static <T> T retrieveData(CompletableFuture<T> completableFuture, TransactionContext transactionContext) {
		try {
			return completableFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			getException(ServiceErrorMessage.UNHANDLED_SERVICE_EXCEPTION, "retrieveData", e, transactionContext);
		}
		return null;
	}

	protected static String getAlternateValueByVal(String name, Field[] statusDeclaredFields) {
		if (StringUtils.isNotBlank(name)) {
			List<SerializedName> annotationsList = Arrays
					.stream(statusDeclaredFields)
					.map(field -> field.getAnnotation(SerializedName.class))
					.collect(Collectors.toList());
			Optional<SerializedName> optAnn = annotationsList
					.stream()
					.filter(serializedName -> Objects.nonNull(serializedName)
							&& Objects.equals(serializedName.alternate()[0], name))
					.findAny();
			if (optAnn.isPresent()) {
				SerializedName serializedName = optAnn.get();
				return serializedName.value();
			} else {
				Optional<SerializedName> optAn = annotationsList
						.stream()
						.filter(serializedName -> Objects.nonNull(serializedName)
								&& serializedName.alternate().length > 1
								&& Objects.equals(serializedName.alternate()[1], name))
						.findAny();
				if (optAn.isPresent()) {
					SerializedName serializedName = optAn.get();
					return serializedName.alternate()[0];
				} else {
					Optional<SerializedName> optAnnByVal = annotationsList
							.stream()
							.filter(serializedName -> Objects.nonNull(serializedName)
									&& Objects.equals(serializedName.value(), name))
							.findAny();
					if (optAnnByVal.isPresent()) {
						SerializedName serializedName = optAnnByVal.get();
						return serializedName.alternate()[0];
					}
				}
			}
		}
		return StringUtils.EMPTY;
	}

	static String getAlternateValue(String name, Field[] statusDeclaredFields) {
		String foundAlternate = null;
		Map<String, String> map = new HashMap<>();
		for (Field statusDeclaredField : statusDeclaredFields) {
			SerializedName annotations = statusDeclaredField.getAnnotation(SerializedName.class);
			if (annotations != null) {
				map.put(annotations.value(), annotations.alternate()[0]);
				for (Map.Entry<String, String> entry : map.entrySet()) {
					if (entry.getKey().equals(name)) {
						foundAlternate = entry.getValue();
						break;
					}
				}
			}
		}
		return foundAlternate;
	}

	public static String getChargeToCdAlt(String value) {
		Field[] statusDeclaredFields = ChargeToCd.class.getDeclaredFields();
		return getAlternateValue(value, statusDeclaredFields);
	}
	public static String getEventLogSubTypeCdCdAlt(String value) {
		Field[] statusDeclaredFields = EventLogSubTypeCd.class.getDeclaredFields();
		return getAlternateValue(value, statusDeclaredFields);
	}
	public static String getEventLogTypeCdAlt(String value) {
		Field[] statusDeclaredFields = EventLogTypeCd.class.getDeclaredFields();
		return getAlternateValue(value, statusDeclaredFields);
	}

	public static String getFlag(final Boolean value) {
		if (value == null) {
			return StringUtils.EMPTY;
		} else if (value) {
			return "Y";
		}
		return "N";
	}

	public static void logMsg(String objectName, List<String> differences, Long shipmentInstId) {
		if (CollectionUtils.isEmpty(differences)) {
			logger.info("{}{}", NO_DIFF, objectName);
		} else {
			String diff = String.join(", ", differences);
			logger.info("{}{}{} shipmentInstId-> {}", objectName, DIFF_FOUND, diff, shipmentInstId);
		}
	}

	@LogExecutionTime
	protected static <T> void copyFields(T source, T target) {
		Field[] fields = source.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (!isStatic(field)) {
				copyField(source, target, field);
			}
		}
	}

	@LogExecutionTime
	private static <T> void copyField(T source, T target, Field field) {
		try {
			String fieldName = field.getName();
			String setterName = "set" + capitalizeFirstLetter(fieldName);
			Method getter = source.getClass().getMethod("get" + capitalizeFirstLetter(fieldName));
			Method setter = target.getClass().getMethod(setterName, field.getType());
			Object value = getter.invoke(source);
			setter.invoke(target, value);
		} catch (NoSuchMethodException e) {
			logger.error("Failed to copy field  {}", field.getName());
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException("Failed to copy fields", e);
		}
	}

	public static String capitalizeFirstLetter(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	private static boolean isStatic(Field field) {
		return (field.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0;
	}

	public static void getException(
			ServiceErrorMessage serviceErrorMessage,
			String location,
			Exception e,
			TransactionContext transactionContext) {
		try {
			throw ExceptionBuilder
					.exception(serviceErrorMessage, transactionContext)
					.moreInfo(location, e.getMessage())
					.build();
		} catch (ServiceException ex) {
			throw new UnsupportedOperationException(ex);
		}
	}

	public static <T, R> void sortListByLongField(
			List<T> list, Function<T, R> childField, ToLongFunction<R> longField) {
		list.sort(java.util.Comparator.comparingLong((T t) -> {
			R r = childField.apply(t);
			return longField.applyAsLong(r);
		}));
	}

	public static String getUserFromContext(TransactionContext transactionContext) {

		if (Objects.nonNull(transactionContext) && Objects.nonNull(transactionContext.getUser()) && Objects.nonNull(
				transactionContext.getUser().getEmployeeId())) {
			return transactionContext.getUser().getEmployeeId();

		} else {
			return "LTLAPP_USER";
		}

	}
	public static String getUserForAutoRate(UpdateShipmentRqst updateShipmentRqst, TransactionContext transactionContext){
		String userId;
		if (updateShipmentRqst.getAuditInfo() != null && org.apache.commons.lang.StringUtils.isNotEmpty(updateShipmentRqst.getAuditInfo().getUpdateById())){
			userId = updateShipmentRqst.getAuditInfo().getUpdateById();
		}else{
			userId = getUserFromContext(transactionContext);
		}
		return userId;
	}

}
