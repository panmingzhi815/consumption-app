package com.dongluhitec.consumptionapp.service;

import com.alibaba.fastjson.JSONObject;
import com.dongluhitec.card.blservice.*;
import com.dongluhitec.card.domain.db.CardUser;
import com.dongluhitec.card.domain.db.Device;
import com.dongluhitec.card.domain.db.PhysicalCard;
import com.dongluhitec.card.domain.db.consumption.*;
import com.dongluhitec.card.domain.security.SystemAccout;
import com.dongluhitec.card.domain.util.StrUtil;
import com.dongluhitec.card.util.AdvanceArith;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jws.WebMethod;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.dongluhitec.consumptionapp.service.Controllers.isEmpty;

public class ConsumptionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionController.class);
    private final static Cache<String, String> cache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
        @Override
        public String load(String s) {
            return "";
        }
    });


    @Inject
    private DatabaseServiceProvider serviceProvider;

    @WebMethod(action = "/dongYunWebservice/findConsumptionUser")
    public RequestMsg findConsumptionUser(Map map) {
        if (isEmpty(map,"cardId","deviceId")){
            return RequestMsg.miss_require_parameter();
        }

        String deviceId = (String) map.get("deviceId");
        String cardId = (String) map.get("cardId");
        cardId = Strings.padStart(cardId,16,'0');
        LOGGER.info("请求查询余额 卡片：{} 设备编号：{}",cardId,deviceId);

        CardService cardService = serviceProvider.getCardService();
        DeviceService deviceService = serviceProvider.getDeviceService();
        ConsumptionService consumptionService = serviceProvider.getConsumptionService();

        Device device = deviceService.findDeviceByIdentifier(deviceId);
        if (device == null) {
            return RequestMsg.success(10001,"设备编号未登记",null);
        }

        PhysicalCard card = cardService.findPhysicalCardByIdentifier(cardId);
        if (card == null) {
            return RequestMsg.success(10002,"卡片未注册",null);
        }

        if (card.getCardUser() == null) {
            return RequestMsg.success(10003,"卡片未分配用户",null);
        }

        ConsumptionUser consumptionUser = consumptionService.findConsumptionUser(card.getCardUser().getId());
        if (consumptionUser == null) {
            return RequestMsg.success(10004,"卡片用户未分配消费模式组",null);
        }

        List<ConsumptionWallet> userWallet = consumptionService.findUserWallet(card.getCardUser());
        if (StrUtil.isEmpty(userWallet)) {
            return RequestMsg.success(10005,"卡片用户未充值",null);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", card.getCardUser().getIdentifier());
        jsonObject.put("userName",card.getCardUser().getName());
        jsonObject.put("cardId",card.getIdentifier());
        jsonObject.put("leftChong",userWallet.stream().filter(f->f.getName().equals("充值")).map(ConsumptionWallet::getLeftMoney).findFirst().orElse(0.0D));
        jsonObject.put("leftBuzhu",userWallet.stream().filter(f->f.getName().equals("补助")).map(ConsumptionWallet::getLeftMoney).findFirst().orElse(0.0D));
        jsonObject.put("leftYajin",userWallet.stream().filter(f->f.getName().equals("押金")).map(ConsumptionWallet::getLeftMoney).findFirst().orElse(0.0D));

        map.put("card", card);
        map.put("consumptionUser", consumptionUser);
        map.put("userWallet", userWallet);
        map.put("device", device);

        if (map.get("orderId") == null) {
            String orderId = String.valueOf(System.currentTimeMillis());
            cache.put(orderId,"有效订单号,5分钟有效");
            jsonObject.put("orderId", orderId);
        }

        return RequestMsg.success(200, "查询成功", jsonObject);
    }

    @WebMethod(action = "/dongYunWebservice/consumption")
    public RequestMsg consumption(Map map){
        if (isEmpty(map,"cardId","deviceId","money","orderId")){
            return RequestMsg.miss_require_parameter();
        }

        String ifPresent = cache.getIfPresent(map.getOrDefault("orderId", "无效订单"));
        if (StrUtil.isEmpty(ifPresent)) {
            return RequestMsg.success(10019,"订单己失效",null);
        }

        RequestMsg requestMsg = findConsumptionUser(map);
        if (requestMsg.getCode() != 200) {
            return requestMsg;
        }

        Float money = Float.valueOf((String) map.get("money"));
        Device device = (Device) map.get("device");
        PhysicalCard card = (PhysicalCard) map.get("card");
        ConsumptionUser consumptionUser = (ConsumptionUser) map.get("consumptionUser");
        ConsumptionService consumptionService = serviceProvider.getConsumptionService();

        ConsumptionModel consumptionModel = consumptionService.findConsumptionModel(device, consumptionUser.getConsumptionGroup());
        if(consumptionModel == null){
            return RequestMsg.success(10006,"卡片用户未分配消费模式",null);
        }

        if(StrUtil.isEmpty(consumptionModel.getConsumptionTypeList())){
            return RequestMsg.success(10007,"卡片用户未分配消费钱包",null);
        }

        ConsumptionStyle style = consumptionService.findConsumptionStyleById(consumptionModel.getStyle().getId());
        CardUser cardUser = card.getCardUser();
        if(style.getStyleEnum() == ConsumptionStyleEnum.定额消费){
            if(StrUtil.isEmpty(style.getPartTimeList())){
                return RequestMsg.success(10008,"卡片用户未设置定额消费时间段",null);
            }

            ConsumptionStylePartTime partTimeStyle = style.getPartTimeStyle(new Date());
            if(partTimeStyle == null){
                return RequestMsg.success(10009,"卡片用户不允许在当前时间段消费",null);
            }

            Long aLong = consumptionService.countConsumptionRecordList(Lists.newArrayList(ConsumptionRecordEnum.在线消费), partTimeStyle.getNowStartTime(), partTimeStyle.getNowEndTime(), cardUser);
            Float moneyByIndex = partTimeStyle.getMoneyByIndex(aLong.intValue());

            if(moneyByIndex == null){
                return RequestMsg.success(10010,"卡片用户不允许在当前时间段再次消费",null);
            }
            money = moneyByIndex;
        }

        String identifier = card.getIdentifier();
        Float queryBalance = consumptionService.queryCardBalanceByType(identifier,consumptionModel.getConsumptionTypeList(),false);
        Float overdrawMoney = style.getOverdrawMoney();

        Double leftMoney = AdvanceArith.sub(queryBalance , money);
        if (leftMoney < 0 && Math.abs(leftMoney) > overdrawMoney) {
            return RequestMsg.success(10011,"卡片用户不允许透支超限",null);
        }

        if (style.getOneDayMaxTimes() != 0F) {
            int i = consumptionService.findConsumptionRecordSizeOneDay(identifier);
            if (i >= style.getOneDayMaxTimes()) {
                return RequestMsg.success(10012,"卡片用户超过一天最大消费次数",null);
            }
        }

        if (style.getOneDayMaxMoney() != 0F) {
            Float onedayMoney = consumptionService.findConsumptionRecordMoneyOneDay(identifier);
            Double add = AdvanceArith.add(onedayMoney, money);
            if (add > style.getOneDayMaxMoney()) {
                return RequestMsg.success(10013,"卡片用户超过一天最大消费金额",null);
            }
        }

        if (style.getOnceMaxMoney() != 0F) {
            if (money > style.getOnceMaxMoney()) {
                return RequestMsg.success(10014,"卡片用户超过一次最大消费金额",null);
            }
        }

        if (style.getOneWeekMaxTimes() != 0F) {
            int i = consumptionService.findConsumptionRecordSizeOneWeek(identifier);
            if (i >= style.getOneWeekMaxTimes()) {
                return RequestMsg.success(10015,"卡片用户超过一周最大消费次数",null);
            }
        }

        if (style.getOneMonthMaxTimes() != 0F) {
            int i = consumptionService.findConsumptionRecordSizeOneMonth(identifier);
            if (i >= style.getOneMonthMaxTimes()) {
                return RequestMsg.success(10016,"卡片用户超过一月最大消费次数",null);
            }
        }

        if (style.getOneMonthMaxMoney() != 0F) {
            Float onedayMoney = consumptionService.findConsumptionRecordMoneyOneMonth(identifier);
            Double add = AdvanceArith.add(onedayMoney, money);
            if (add > style.getOneMonthMaxMoney()) {
                return RequestMsg.success(10017,"卡片用户超过一月最大消费金额",null);
            }
        }

        if (style.getZoom() != 0F) {
            money = money * style.getZoom();
        }

        if (Optional.ofNullable(style.getIntervalSecond()).orElse(0) > 0F) {
            ConsumptionRecord consumptionRecord = consumptionService.findLastConsumptionRecordByEnum(cardUser.getIdentifier(),ConsumptionRecordEnum.在线消费);
            if(consumptionRecord != null && System.currentTimeMillis() - consumptionRecord.getDatabaseTime().getTime()  < style.getIntervalSecond()*1000){
                return RequestMsg.success(10018,"卡片用户在规定时间内不能再次消费",null);
            }
        }

        ConsumptionRecord record = new ConsumptionRecord();
        record.setPhysicalCard(card);
        record.setCardUser(cardUser);
        record.setDevice(device);
        record.setDeviceTime(new Date());
        record.setLeftMoney(leftMoney);
        record.setOperatorMoney(Math.abs(money) * -1);
        record.setConsumptionRecordEnum(ConsumptionRecordEnum.在线消费);
        record.setStyleName(style.getStyleName());
        record.setZoom(style.getZoom());
        record.setOperatorName((String) map.get("username"));
        consumptionService.saveConsumptionRecord(record);

        return RequestMsg.success(200, "消费成功", null);
    }

    @WebMethod(action = "/dongYunWebservice/recordList")
    public RequestMsg recordList(HashMap map){
        ConsumptionService consumptionService = serviceProvider.getConsumptionService();

        String username = (String) map.get("username");
        Date todayTopTime = StrUtil.getTodayTopTime(new Date());
        Date todayBottomTime = StrUtil.getTodayBottomTime(new Date());
        List<ConsumptionRecord> consumptionRecordList = consumptionService.findConsumptionRecordList(0, 30, null, null, null, null, null, null, null, null, username, todayTopTime, todayBottomTime, Arrays.asList(ConsumptionRecordEnum.在线消费));
        int count = consumptionService.countConsumptionRecordList(null, null, null, null, null, null, null, null, username, todayTopTime, todayBottomTime, Arrays.asList(ConsumptionRecordEnum.在线消费));

        Object[] array = consumptionRecordList.stream().map(m -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", m.getCardUserIdentifier());
            jsonObject.put("userName", m.getCardUserName());
            jsonObject.put("money", m.getOperatorMoney());
            jsonObject.put("time", m.getDatabaseTimeLabel());
            return jsonObject;
        }).toArray();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("total", count);
        jsonObject.put("rows", array);

        return RequestMsg.success(200,"加载成功",jsonObject);
    }

    @WebMethod(action = "/dongYunWebservice/doLogin")
    public RequestMsg login(Map map) {
        SecurityService securityService = serviceProvider.getSecurityService();
        String username = (String)map.getOrDefault("username","");
        String password = (String)map.getOrDefault("password", "");

        if (username.isEmpty() || password.isEmpty()) {
            return RequestMsg.valid_fail();
        }

        SystemAccout account = securityService.findSystemAccoutByUserName((String)map.getOrDefault("username", ""));
        if (Optional.ofNullable(account).map(SystemAccout::getAccountPassword).orElse("").equals(password)) {
            return RequestMsg.success(200,"登录成功",username);
        }

        return RequestMsg.valid_fail();
    }
}
