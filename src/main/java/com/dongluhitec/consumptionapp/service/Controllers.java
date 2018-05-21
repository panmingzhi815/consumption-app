package com.dongluhitec.consumptionapp.service;

import java.util.Map;
import java.util.Optional;

public class Controllers {

    public static void convertPageMap(Map map) {
        Integer page = Optional.ofNullable((String)map.get("page")).map(Integer::valueOf).orElse(0);
        Integer rows = Optional.ofNullable((String)map.get("rows")).map(Integer::valueOf).orElse(10);
        Integer start = (page - 1) * rows;

        map.put("maxSize", rows);
        map.put("currentSize", start);
    }

    public static boolean isEmpty(Map map,String... validKeys){
        if (map == null || map.isEmpty()) {
            return true;
        }

        for (String validKey : validKeys) {
            if (map.get(validKey) == null) {
                return true;
            }
        }

        return false;
    }

}
