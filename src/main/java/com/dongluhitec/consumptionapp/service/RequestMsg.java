package com.dongluhitec.consumptionapp.service;

public class RequestMsg {

    private Integer code;
    private String codeMsg;
    private Object msg;

    public RequestMsg(Integer code, String codeMsg, Object msg) {
        this.code = code;
        this.codeMsg = codeMsg;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getCodeMsg() {
        return codeMsg;
    }

    public void setCodeMsg(String codeMsg) {
        this.codeMsg = codeMsg;
    }

    public Object getMsg() {
        return msg;
    }

    public void setMsg(Object msg) {
        this.msg = msg;
    }


    public static RequestMsg not_logged_in(){
        return new RequestMsg(403,"用户未登录",null);
    }

    public static RequestMsg request_error(){
        return new RequestMsg(500 ,"服务器处理失败",null);
    }


    public static RequestMsg valid_fail() {
        return new RequestMsg(401 ,"用户名或密码错误",null);
    }

    public static RequestMsg success(int code, String codeMsg, Object msg) {
        return new RequestMsg(code ,codeMsg,msg);
    }

    public static RequestMsg miss_require_parameter() {
        return new RequestMsg(500 ,"必要信息不可为空",null);
    }
}
