package com.dongluhitec.consumptionapp.service;

public enum ResultCode {
    usernameOrPasswordIsNull(1001,"用户名或密码为空"),
    usernameHasNotExist(1002,"用户名不存在"),
    usernameAndPasswordError(1003,"用户名与密码不匹配"),
    phoneHasExist(1004,"手机号己存在"),
    phoneIsNull(1005,"手机号为空"),
    infoNotFull(1005,"信息不完整"),
    saveMeetError(1006,"添加会议出错"),
    meetTimeError(1007,"时间冲突"),
    ;

    public int code;
    public String describe;

    private ResultCode(int code, String describe){
        this.code = code;
        this.describe = describe;
    }
}
