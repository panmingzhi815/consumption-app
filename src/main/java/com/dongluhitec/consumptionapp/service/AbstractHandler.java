package com.dongluhitec.consumptionapp.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dongluhitec.card.domain.util.StrUtil;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiaopan on 2016-07-21.
 */
public abstract class AbstractHandler {

    private final static Logger LOOGER = LoggerFactory.getLogger(AbstractHandler.class);

    public void handler(HttpExchange httpExchange) throws IOException {
        String requestMethod = httpExchange.getRequestMethod();
        if (requestMethod.equals("GET")) {
            get(httpExchange);
        }else if (requestMethod.equals("POST")) {
            post(httpExchange);
        }
        service(httpExchange);
    }

    public void get(HttpExchange httpExchange){}

    public void post(HttpExchange httpExchange) throws IOException {}

    public void service(HttpExchange httpExchange) throws IOException {}

    public Map parseQueryToMap(HttpExchange httpExchange){
        String query = httpExchange.getRequestURI().getQuery();
        return getMap(query);
    }

    public static Map parsePostToMap(HttpExchange exchange){
        try {
            InputStream in = exchange.getRequestBody();
            if (in.available() == 0){
                return new HashMap();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
            String text = IOUtils.toString(reader);
            String decode = URLDecoder.decode(text, "UTF-8");
            return getMap(decode);
        } catch (IOException e) {
            return new HashMap();
        }
    }

    public static Map getMap(String query) {
        Map<String,String> map = new HashMap<>();
        if (StrUtil.isEmpty(query)) {
            return map;
        }
        String[] split = query.split("&");
        for (String s : split) {
            String[] split1 = s.split("=");
            if (split1.length == 2) {
                map.put(split1[0],split1[1]);
            }
        }
        return map;
    }

    protected void writeCodeAndMsg(HttpExchange httpExchange, ResultCode resultCode) throws IOException {
        writeCodeAndMsg(httpExchange,resultCode.code,resultCode.describe);
    }

    protected void writeCodeAndMsg(HttpExchange httpExchange, int value, Object msg) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", value);
        jsonObject.put("msg", msg);
        WriteToJson(httpExchange, jsonObject);
    }

    protected void WriteToJson(HttpExchange httpExchange, JSONObject jsonObject) throws IOException {
        String toJSONString = jsonObject.toJSONString();
        byte[] bytes = toJSONString.getBytes("UTF-8");
        write(httpExchange, bytes);

        LOOGER.info("http请求返回消息:{}",toJSONString);
    }

    public static void writeObject(HttpExchange httpExchange, RequestMsg object) {
        LOOGER.info("返回code:{} 返回msg:{}",object.getCode(),object.getCodeMsg());
        byte[] bytes = JSON.toJSONBytes(object);
        try {
            write(httpExchange,bytes);
        } catch (IOException e) {
            LOOGER.error("http响应时发生错误",e);
        }
    }

	/**
	 * @param httpExchange
	 * @param bytes
	 * @throws IOException
	 */
	public static void write(HttpExchange httpExchange, byte[] bytes) throws IOException {
		httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
        httpExchange.getResponseHeaders().set("Content-Type", "text/json;charset=UTF-8");
        OutputStream responseBody = httpExchange.getResponseBody();
        responseBody.write(bytes);
        responseBody.flush();
	}
}
