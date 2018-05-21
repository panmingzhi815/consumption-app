package com.dongluhitec.consumptionapp.service;

import com.caucho.hessian.client.HessianProxyFactory;
import com.dongluhitec.card.blservice.*;
import com.dongluhitec.card.domain.security.LocalSecurityManager;
import com.dongluhitec.card.service.impl.AbstractServiceProvider;
import com.dongluhitec.core.crypto.SystemModuleService;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: wudong
 * Date: 05/08/13
 * Time: 22:35
 * To change this template use File | Settings | File Templates.
 */
public class HessianServiceProvider extends AbstractServiceProvider {

    static Logger LOGGER = LoggerFactory.getLogger(com.dongluhitec.card.service.impl.HessianServiceProvider.class);

    public static final String DATABASE_REMOTE_URL_KEY = "Database_remote_url_key";

    final private String defaultUrl = "http://127.0.0.1:8889/db/";

    @Inject
    public HessianServiceProvider() {

    }

    @Override
    protected void initService() throws MalformedURLException {
        LOGGER.info("开始初始化客户端数据服务");
        if(Strings.isNullOrEmpty(System.getProperty(DATABASE_REMOTE_URL_KEY))){
            LOGGER.error("未设备数据服务器地址,不能启动客户端数据服务");
            return;
        }
        Injector injector = Guice.createInjector(new Model());
        this.setCardService(injector.getInstance(CardService.class));
        this.setDeviceService(injector.getInstance(DeviceService.class));
        this.setSecurityService(injector.getInstance(SecurityService.class));
        this.setSystemLogService(injector.getInstance(SystemLogServiceI.class));
        this.setConsumptionService(injector.getInstance(ConsumptionService.class));
    }

    @Override
    public boolean isDatabaseConnected() {
        if (System.getProperty(DATABASE_REMOTE_URL_KEY) == null){
            System.setProperty(DATABASE_REMOTE_URL_KEY,defaultUrl);
        }
        String url = System.getProperty(DATABASE_REMOTE_URL_KEY);
        String substring = url.substring(7, url.length() - 4);
        String[] split = substring.split(":");

        try (Socket socket = new Socket(split[0], Integer.valueOf(split[1]))){
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public class Model extends AbstractModule {

        @Override
        protected void configure() {
            String url = System.getProperty(DATABASE_REMOTE_URL_KEY);
            LOGGER.info("客户端远程数据底层地址:{}",url);

            HessianProxyFactory factory = new HessianProxyFactory();
            factory.setOverloadEnabled(true);
            factory.setUser(System.getProperty(LocalSecurityManager.user_key));
            factory.setPassword(System.getProperty(LocalSecurityManager.pass_key));
            try {
                this.bind(DeviceService.class).toInstance((DeviceService) factory.create(DeviceService.class, url));
                this.bind(SecurityService.class).toInstance((SecurityService) factory.create(SecurityService.class, url));
                //将刷卡记录数据业务单独分离出来
                this.bind(CardService.class).toInstance((CardService) factory.create(CardService.class, url.replace("db", "cardService")));
                this.bind(ConsumptionService.class).toInstance((ConsumptionService) factory.create(ConsumptionService.class, url.replace("db", "consumptionService")));
                this.bind(SystemLogServiceI.class).toInstance((SystemLogServiceI) factory.create(SystemLogServiceI.class, url.replace("db", "systemLog")));
            } catch (Exception e) {
                throw new DongluServiceException("初始化远程数据库连接是发生严重错误！！！", e);
            }
        }
    }

    @Override
    protected void stopServices() {
        LOGGER.info("关闭客户端数据服务");
    }

}
