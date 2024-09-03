package com.didiglobal.turbo.engine.model;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务能力入参映射
 *
 * @author liuchengbiao
 */
public class ExclusiveGatewayInParamMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExclusiveGatewayInParamMapping.class);
    /**
     * 参数名分隔符
     */
    private static String valueSplit = "/";
    /**
     * 左侧变量所属节点
     */
    private String leftSideNodeKey;
    /**
     * 左侧引用的参数名(参数名为a/b/c格式)
     */
    private String leftSideValue;

    /**
     * 右侧参数来源
     */
    private String rightSideFrom = FROM.INPUT;
    /**
     * 右侧变量所属节点
     */
    private String rightSideNodeKey;
    /**
     * 右侧引用的参数名或者输入值，如果rightSideFrom是input那这里就是输入值，否则就是引用的参数名(参数名为a/b/c格式)
     */
    private String rightSideValue;

    /**
     * 参数来源
     */
    public interface FROM {
        /**
         * 引用环节变量
         */
        String REFERENCE = "Reference";
        /**
         * 输入、固定值，支持 ${xxxx} 格式获取智能体变量
         */
        String INPUT = "Input";
    }

    public Object getLeftSideObject(JSONObject flowMap) {
        JSONObject nodeMap = (JSONObject) flowMap.get(this.leftSideNodeKey);
        Assert.isTrue(StringUtils.isNotBlank(this.leftSideNodeKey), "左侧变量所属节点未配置");
        Assert.isTrue(StringUtils.isNotBlank(this.leftSideValue), "左侧引用的参数名未配置");
        if (null == nodeMap) {
            LOGGER.warn("参数【{}】来源对应环节未找到, 将返回null，请谨慎处理", leftSideValue);
            return null;
        }
        String[] valueKeys = this.leftSideValue.split(valueSplit);
        List<Object> results = new ArrayList<>();
        Boolean isArray = getItemValue(nodeMap, valueKeys, 0, results, Boolean.FALSE);
        if (results.size() == 1 && !isArray) {
            return results.get(0);
        }
        if (results.isEmpty()) {
            return null;
        }
        return results;
    }

    /**
     * 从 nodeMap 对象中按照path路径获取对应属性值或获取固定值
     *
     * @param flowMap
     * @return
     */
    @Nullable
    public Object getRightSideObject(JSONObject flowMap) {
        Assert.isTrue(StringUtils.isNotBlank(this.rightSideFrom), "右侧参数来源未配置");
        if (!FROM.INPUT.equals(this.rightSideFrom) && !FROM.REFERENCE.equals(this.rightSideFrom)) {
            Assert.isTrue(StringUtils.isNotBlank(this.rightSideFrom), "右侧参数来源超出支持范围");
        }
        if (FROM.INPUT.equals(this.rightSideFrom)) {
            return this.rightSideValue;
        }
        JSONObject nodeMap = (JSONObject) flowMap.get(this.rightSideNodeKey);

        Assert.isTrue(StringUtils.isNotBlank(this.rightSideNodeKey), "右侧变量所属节点未配置");
        Assert.isTrue(StringUtils.isNotBlank(this.rightSideValue), "右侧引用的参数名未配置");
        if (null == nodeMap) {
            LOGGER.warn("参数【{}】来源对应环节未找到, 将返回null，请谨慎处理", rightSideValue);
            return null;
        }
        String[] valueKeys = this.rightSideValue.split(valueSplit);
        List<Object> results = new ArrayList<>();
        Boolean isArray = getItemValue(nodeMap, valueKeys, 0, results, Boolean.FALSE);
        if (results.size() == 1 && !isArray) {
            return results.get(0);
        }
        if (results.isEmpty()) {
            return null;
        }
        return results;
    }

    public Boolean getItemValue(Map<String, ?> nodeMap,
                                String[] valueKeys,
                                int index,
                                List<Object> results,
                                Boolean isArray) {
        if (null == nodeMap) {
            return isArray;
        }
        int searchNum = valueKeys.length;
        // 越界
        if (index >= searchNum) {
            return isArray;
        }
        String currentKey = valueKeys[index];
        Object currentValue = nodeMap.get(currentKey);
        // 层级对应
        if (index == searchNum - 1) {
            if (currentValue instanceof List<?>) {
                // 类型转换
                isArray = Boolean.TRUE;
                List<?> list = (List<?>) currentValue;
                results.addAll(list);
                return isArray;
            }
            results.add(currentValue);
            return isArray;
        }
        // 向下检索
        if (!isString(currentValue)) {
            if (currentValue instanceof List<?>) {
                // 类型转换
                List<?> list = (List<?>) currentValue;
                for (Object obj : list) {
                    if (obj instanceof Map) {
                        // 类型转换
                        Map<String, ?> objMap = (Map<String, ?>) obj;
                        isArray = Boolean.TRUE;
                        isArray = getItemValue(objMap, valueKeys, index + 1, results, isArray);
                    }
                }
            } else {
                // 类型转换
                nodeMap = (JSONObject) currentValue;
                isArray = getItemValue(nodeMap, valueKeys, index + 1, results, isArray);
            }
        }
        // 基本数据类型或者null 无法向下检索
        return isArray;
    }

    private Boolean isString(Object obj) {
        if (obj != null) {
            return obj instanceof Number
                    || obj.getClass().equals(String.class)
                    || obj.getClass().equals(Boolean.class);
        }
        return false;
    }

    public String getRightSideFrom() {
        return rightSideFrom;
    }

    public void setRightSideFrom(String rightSideFrom) {
        this.rightSideFrom = rightSideFrom;
    }

    public String getRightSideNodeKey() {
        return rightSideNodeKey;
    }

    public void setRightSideNodeKey(String rightSideNodeKey) {
        this.rightSideNodeKey = rightSideNodeKey;
    }

    public String getRightSideValue() {
        return rightSideValue;
    }

    public void setRightSideValue(String rightSideValue) {
        this.rightSideValue = rightSideValue;
    }

    public String getLeftSideNodeKey() {
        return leftSideNodeKey;
    }

    public void setLeftSideNodeKey(String leftSideNodeKey) {
        this.leftSideNodeKey = leftSideNodeKey;
    }

    public String getLeftSideValue() {
        return leftSideValue;
    }

    public void setLeftSideValue(String leftSideValue) {
        this.leftSideValue = leftSideValue;
    }
}
