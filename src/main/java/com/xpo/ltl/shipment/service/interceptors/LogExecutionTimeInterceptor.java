package com.xpo.ltl.shipment.service.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ClassUtils.Interfaces;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@LogExecutionTime
@Interceptor
public class LogExecutionTimeInterceptor {

    private static final Logger LOGGER = LogManager.getLogger(LogExecutionTimeInterceptor.class);

    @AroundInvoke
    public Object logExecutionTime(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        Class<?> declaringClass = method.getDeclaringClass();

        LogExecutionTime annotation = getAnnotation(context, LogExecutionTime.class);
        if (annotation == null) {
            LOGGER.debug("logExecutionTime: Failed to lookup LogExecutionTime annotation for " + method);
            return context.proceed();
        }

        String levelName = annotation.value();
        Level level = Level.toLevel(levelName, null);
        if (level == null) {
            LOGGER.debug("logExecutionTime: Failed to lookup logging level \"" + levelName + "\" for " + method);
            return context.proceed();
        }

        String className = declaringClass.getName();
        Logger logger = LogManager.getLogger(className);
        if (!logger.isEnabled(level))
            return context.proceed();

        String methodName = method.getName();
        logger.log(level, methodName + ": Entering method");

        StopWatch watch = new StopWatch();
        watch.start();

        boolean completed = false;
        try {
            Object result = context.proceed();
            completed = true;
            return result;
        }
        finally {
            watch.stop();
            if (completed)
                logger.log(level, methodName + ": Returned after " + watch.getTime() + " ms");
            else
                logger.log(level, methodName + ": Raised exception after " + watch.getTime() + " ms");
        }
    }

    public <T extends Annotation> T getAnnotation(InvocationContext context, Class<T> annotationClass) {
        T annotation = null;

        Method method = context.getMethod();
        annotation = MethodUtils.getAnnotation(method, annotationClass, true, false);
        if (annotation != null)
            return annotation;

        Class<?> declaringClass = method.getDeclaringClass();
        annotation = declaringClass.getAnnotation(annotationClass);
        if (annotation != null)
            return annotation;

        Object target = context.getTarget();
        for (Class<?> type : ClassUtils.hierarchy(target.getClass(), Interfaces.INCLUDE)) {
            annotation = type.getAnnotation(annotationClass);
            if (annotation != null)
                return annotation;
        }

        return annotation;
    }

}
