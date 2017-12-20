package com.dongluhitec.consumptionapp.aop;

import com.dongluhitec.consumptionapp.controller.RequestMsg;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;

@Aspect
@Component
public class SessionInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionInterceptor.class);

    @Around("execution(* com.dongluhitec.consumptionapp.controller..*(..))")
    public static Object Interceptor(ProceedingJoinPoint pjp){
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            NoSecurity annotation = method.getAnnotation(NoSecurity.class);
            if (annotation == null) {
                RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                HttpSession session = (HttpSession) requestAttributes.resolveReference(RequestAttributes.REFERENCE_SESSION);
                if (session.getAttribute("username") == null && method.getReturnType() == String.class) {
                    LOGGER.warn("用户未登录，访问页面被拒绝：{}",method.getName());
                    return "/login";
                }
                if (session.getAttribute("username") == null && method.getReturnType() == RequestMsg.class) {
                    LOGGER.warn("用户未登录，访问业务被拒绝：{}",method.getName());
                    return RequestMsg.not_logged_in();
                }
            }

            return pjp.proceed();
        } catch (Throwable throwable) {
            return RequestMsg.request_error();
        }
    }
}
