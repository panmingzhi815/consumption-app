package com.dongluhitec.consumptionapp.aop;

import com.dongluhitec.consumptionapp.controller.RequestMsg;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class LogInterceptor{

    private static final Logger LOGGER = LoggerFactory.getLogger(LogInterceptor.class);

    @Around("execution(* com.dongluhitec.consumptionapp.controller..*(..))")
    public Object Interceptor(ProceedingJoinPoint pjp){
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            LOGGER.debug("开始请求action : {}",request.getRequestURI());

            Signature signature = pjp.getSignature();
            LOGGER.debug("开始请求method : {}",signature.toShortString());
            Object proceed = pjp.proceed();
            LOGGER.debug("结束请求method : {}",signature.toShortString());
            return proceed;
        } catch (Throwable throwable) {
            return RequestMsg.request_error();
        }
    }
}
