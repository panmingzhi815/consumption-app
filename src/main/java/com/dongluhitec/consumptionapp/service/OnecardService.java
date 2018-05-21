package com.dongluhitec.consumptionapp.service;
import com.dongluhitec.card.blservice.DatabaseServiceProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class OnecardService implements Filter {

    private Logger LOGGER = LoggerFactory.getLogger(OnecardService.class);

    private static WebHttpService instance;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.setProperty(HessianServiceProvider.DATABASE_REMOTE_URL_KEY,"http://127.0.0.1:8889/db/");
        final HessianServiceProvider hessianServiceProvider = new HessianServiceProvider();
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DatabaseServiceProvider.class).toInstance(hessianServiceProvider);
            }
        });
        instance = injector.getInstance(WebHttpService.class);
        try {
            hessianServiceProvider.start();
            instance.startUp();
        } catch (Exception e) {
            LOGGER.error("启动一卡通手机app业务异常",e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        LOGGER.info("请求：{}",httpServletRequest.getRequestURI());
        filterChain.doFilter(servletRequest,servletResponse);
    }

    @Override
    public void destroy() {
        try {
            instance.shutDown();
        } catch (Exception e) {
            LOGGER.error("停止一卡通手机app业务异常",e);
        }
    }
}
