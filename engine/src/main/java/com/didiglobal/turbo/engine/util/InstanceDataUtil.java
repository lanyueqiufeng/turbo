package com.didiglobal.turbo.engine.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.common.DataType;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstanceDataUtil {

    private InstanceDataUtil() {}

    /**
     * 用于将turbo定制的List类型的全局参数池转化为Map类型，方便取值
     *
     * @param instanceDataList 流程实例中的全局变量
     * @return Map类型的全局变量
     */
    public static Map<String, Object> changeInstanceDataToMap(List<InstanceData> instanceDataList) {
        Map<String, Object> instanceDataMap = new HashMap<>();
        for (InstanceData data : instanceDataList) {
            instanceDataMap.put(data.getKey(), data.getValue());
        }
        return instanceDataMap;
    }

    public static Map<String, Object> changeInstanceDataToMap(Map<String, InstanceData> instanceData) {
        Map<String, Object> instanceDataMap = new HashMap<>();
        for (String key : instanceData.keySet()) {
            instanceDataMap.put(key, instanceData.get(key).getValue());
        }
        return instanceDataMap;
    }

    /**
     * 还得给他转换回去，淦
     * @param instanceDataMap map类型的instance
     * @return List
     */
    public static List<InstanceData> changeInstanceDataToList(Map<String, Object> instanceDataMap) {
        List<InstanceData> instanceDataList = new ArrayList<>();
        for (String key : instanceDataMap.keySet()) {
            instanceDataList.add(new InstanceData(key, instanceDataMap.get(key)));
        }
        return instanceDataList;
    }

    public static Map<String, InstanceData> getInstanceDataMap(List<InstanceData> instanceDataList) {
        if (CollectionUtils.isEmpty(instanceDataList)) {
            return Maps.newHashMap();
        }
        Map<String, InstanceData> instanceDataMap = Maps.newHashMap();
        instanceDataList.forEach(instanceData -> {
            instanceDataMap.put(instanceData.getKey(), instanceData);
        });
        return instanceDataMap;
    }

    public static Map<String, InstanceData> getInstanceDataMap(String instanceDataStr) {
        if (StringUtils.isBlank(instanceDataStr)) {
            return Maps.newHashMap();
        }
        List<InstanceData> instanceDataList = JSON.parseArray(instanceDataStr, InstanceData.class);
        return getInstanceDataMap(instanceDataList);
    }

    public static List<InstanceData> getInstanceDataList(Map<String, InstanceData> instanceDataMap) {
        if (MapUtils.isEmpty(instanceDataMap)) {
            return Lists.newArrayList();
        }
        List<InstanceData> instanceDataList = Lists.newArrayList();
        instanceDataMap.forEach((key, instanceData) -> {
            instanceDataList.add(instanceData);
        });
        return instanceDataList;
    }

    public static String getInstanceDataListStr(Map<String, InstanceData> instanceDataMap) {
        if (MapUtils.isEmpty(instanceDataMap)) {
            return JSONObject.toJSONString(CollectionUtils.EMPTY_COLLECTION);
        }
        return JSONObject.toJSONString(instanceDataMap.values());
    }

    public static Map<String, Object> parseInstanceDataMap(Map<String, InstanceData> instanceDataMap) {
        if (MapUtils.isEmpty(instanceDataMap)) {
            return Maps.newHashMap();
        }
        Map<String, Object> dataMap = Maps.newHashMap();
        instanceDataMap.forEach((keyName, instanceData) -> {
            dataMap.put(keyName, parseInstanceData(instanceData));
        });
        return dataMap;
    }

    private static Object parseInstanceData(InstanceData instanceData) {
        if (instanceData == null) {
            return null;
        }
        String dataTypeStr = instanceData.getType();
        DataType dataType = DataType.getType(dataTypeStr);

        // TODO: 2019/12/16
        return instanceData.getValue();
    }
}
