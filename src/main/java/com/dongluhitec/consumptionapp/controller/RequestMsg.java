package com.dongluhitec.consumptionapp.controller;

import lombok.Data;

@Data
public class RequestMsg {

    private Integer code;
    private String codeMsg;
    private Object msg;

    public RequestMsg() {
    }

    public RequestMsg(Integer code, String codeMsg, Object msg) {
        this.code = code;
        this.codeMsg = codeMsg;
        this.msg = msg;
    }

    public static RequestMsg not_logged_in(){
        return new RequestMsg(403,"用户未登录",null);
    }

    public static RequestMsg request_error(){
        return new RequestMsg(500 ,"服务器处理失败",null);
    }

    public static RequestMsg request_timeout() {
        return new RequestMsg(500 ,"服务器响应超时",null);
    }

}
