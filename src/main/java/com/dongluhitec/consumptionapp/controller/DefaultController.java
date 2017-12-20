package com.dongluhitec.consumptionapp.controller;

import com.alibaba.fastjson.JSON;
import com.dongluhitec.consumptionapp.aop.NoSecurity;
import lombok.Setter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@ConfigurationProperties(prefix = "default.config")
public class DefaultController {

    private static Logger LOGGER = LoggerFactory.getLogger(DefaultController.class);

    @Setter
    private String host;

    @RequestMapping(path = {"/app/index"})
    public String index() {
        return "index";
    }

    @RequestMapping(path = {"/app/record"})
    public String record() {
        return "record";
    }

    @NoSecurity
    @RequestMapping({"/", "/app/login"})
    public String login() {
        return "login";
    }

    @NoSecurity
    @ResponseBody
    @RequestMapping("/doLogin")
    public RequestMsg doLogin(@RequestBody Map<String, String> map, HttpSession session) {
        RequestMsg requestMsg = baseRequest("doLogin", map);
        if (requestMsg.getCode() == 200) {
            session.setAttribute("username", map.get("username"));
        }
        return requestMsg;
    }

    @ResponseBody
    @RequestMapping("/request/{method}")
    public RequestMsg request(@PathVariable String method, @RequestBody Map<String, String> map, HttpSession session) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put("username", (String)session.getAttribute("username"));
        return baseRequest(method, map);
    }

    private RequestMsg baseRequest(String method, Map<String, String> map) {
        HttpClient httpclient = new HttpClient();
        PostMethod postMethod = null;
        try {
            postMethod = new PostMethod(host + "/" + method);
            postMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET,"utf-8");

            LOGGER.info("发送请求：{}",postMethod.getPath());

            List<NameValuePair> collect = map.entrySet().stream().map(m -> new NameValuePair(m.getKey(), m.getValue())).collect(Collectors.toList());
            NameValuePair[] nameValuePairs = collect.toArray(new NameValuePair[collect.size()]);
            postMethod.setRequestBody(nameValuePairs);

            httpclient.executeMethod(postMethod);
            String toString = IOUtils.toString(postMethod.getResponseBodyAsStream(), "UTF-8");
            LOGGER.debug("返回数据：{}",toString);
            return JSON.parseObject(toString, RequestMsg.class);
        }catch (NoHttpResponseException nre){
            LOGGER.error("请求服务处理失败",nre);
            return RequestMsg.request_timeout();
        }catch (Exception e) {
            LOGGER.error("请求服务处理失败",e);
            return RequestMsg.request_error();
        } finally {
            Optional.ofNullable(postMethod).ifPresent(HttpMethodBase::releaseConnection);
        }
    }

}
