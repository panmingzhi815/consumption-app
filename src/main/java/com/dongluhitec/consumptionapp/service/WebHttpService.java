package com.dongluhitec.consumptionapp.service;

import com.dongluhitec.card.blservice.DatabaseServiceProvider;
import com.dongluhitec.card.blservice.SystemLogServiceI;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** http + json
 * Created by xiaopan on 2016-07-21.
 */
public class WebHttpService extends AbstractIdleService {

    private final Logger LOGGER = LoggerFactory.getLogger(WebHttpService.class);

    private final int Port = 10999;
    private final int MaxSession = 100;

    private Map<String, Object[]> actionMapping = new HashMap<>();
    private DatabaseServiceProvider serviceProvider;
    private ConsumptionController consumptionController;
    private HttpServer httpServer;

    @Inject
    public WebHttpService(ConsumptionController consumptionController,DatabaseServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        this.consumptionController = consumptionController;
    }

    @Override
    protected void startUp() throws Exception {
        LOGGER.info("开始启动硬件对外开放接口服务,端口号:{}",Port);
        HttpServerProvider provider = HttpServerProvider.provider();
        this.httpServer = provider.createHttpServer(new InetSocketAddress(Port), MaxSession);
        this.httpServer.createContext("/", httpExchange -> {
            try {
                String path = httpExchange.getRequestURI().getPath();
                LOGGER.info("收到请求:{}",path);
                Optional.ofNullable(actionMapping.get(path)).ifPresent(p ->{
                    try {
                        Map map = AbstractHandler.parsePostToMap(httpExchange);
                        LOGGER.debug("请求参数：{}",map);
                        SystemLogServiceI systemLogService = serviceProvider.getSystemLogService();
                        systemLogService.setLoginUser((String)map.get("username"));

                        Method m = (Method) p[1];
                        RequestMsg requestMsg = (RequestMsg)m.invoke(p[0],map);
                        AbstractHandler.writeObject(httpExchange, requestMsg);
                    } catch (Exception e) {
                        AbstractHandler.writeObject(httpExchange,RequestMsg.request_error());
                    }
                });
            } catch (Throwable e) {
                LOGGER.error("处理硬件接口调用时发生错误",e);
            }finally {
                httpExchange.close();
            }
        });
        registerController(consumptionController);
        httpServer.setExecutor(null);
        httpServer.start();
        LOGGER.info("启动硬件对外开放接口服务成功");
    }

    @Override
    protected void shutDown() throws Exception {
        LOGGER.info("开始停止硬件对外开放接口服务,端口号:{}",Port);
        if (httpServer != null) {
            httpServer.stop(5);
        }
        LOGGER.info("停止硬件对外开放接口服务成功");
    }

    /**
     * 注册所有的controller
     * @param objects
     */
    public void registerController(Object... objects){
        for (Object o : objects) {
            Method[] declaredMethods = o.getClass().getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                WebMethod annotation = declaredMethod.getAnnotation(WebMethod.class);
                Optional.ofNullable(annotation).ifPresent(p->actionMapping.put(annotation.action(),new Object[]{o,declaredMethod}));
            }
        }
    }
}
