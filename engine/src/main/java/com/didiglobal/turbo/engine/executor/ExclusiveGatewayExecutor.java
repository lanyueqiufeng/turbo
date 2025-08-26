package com.didiglobal.turbo.engine.executor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.bo.NodeInstanceBO;
import com.didiglobal.turbo.engine.common.*;
import com.didiglobal.turbo.engine.entity.InstanceDataPO;
import com.didiglobal.turbo.engine.exception.ProcessException;
import com.didiglobal.turbo.engine.model.ExclusiveGatewayInParamMapping;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.didiglobal.turbo.engine.spi.ExclusiveGatewayLogService;
import com.didiglobal.turbo.engine.spi.HookService;
import com.didiglobal.turbo.engine.util.FlowModelUtil;
import com.didiglobal.turbo.engine.util.InstanceDataUtil;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class ExclusiveGatewayExecutor extends ElementExecutor implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExclusiveGatewayExecutor.class);

    /**
     * 存放出参的键
     */
    private static final String COMPARE_DETAILS_KEY = "$$CompareDetails";

    @Resource
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private ExclusiveGatewayLogService exclusiveGatewayLogService;
    private List<HookService> hookServices;

    /**
     * Update data map: invoke hook service to update data map
     * You can implement HookService and all implementations of 'HookService' will be executed.
     * Param: one of flowElement's properties
     */
    @Override
    protected void doExecute(RuntimeContext runtimeContext) throws ProcessException {
        // 1.get hook param
        FlowElement flowElement = runtimeContext.getCurrentNodeModel();
        String hookInfoParam = FlowModelUtil.getHookInfos(flowElement);

        // 2.ignore while properties is empty
        if (StringUtils.isBlank(hookInfoParam)) {
            return;
        }

        // 3.invoke hook and get data result
        Map<String, InstanceData> hookInfoValueMap = getHookInfoValueMap(runtimeContext);
        LOGGER.info("doExecute getHookInfoValueMap.||hookInfoValueMap={}", hookInfoValueMap);
        if (MapUtils.isEmpty(hookInfoValueMap)) {
            LOGGER.warn("doExecute: hookInfoValueMap is empty.||flowInstanceId={}||hookInfoParam={}||nodeKey={}",
                    runtimeContext.getFlowInstanceId(), hookInfoParam, flowElement.getKey());
            return;
        }

        // 4.merge data to current dataMap
        Map<String, InstanceData> dataMap = runtimeContext.getInstanceDataMap();
        dataMap.putAll(hookInfoValueMap);

        // 5.save data
        if (MapUtils.isNotEmpty(dataMap)) {
            String instanceDataId = saveInstanceDataPO(runtimeContext);
            runtimeContext.setInstanceDataId(instanceDataId);
        }
    }

    private Map<String, InstanceData> getHookInfoValueMap(RuntimeContext runtimeContext) {
        List<InstanceData> dataList = Lists.newArrayList();
        for (HookService service : hookServices) {
            // try {
            List<InstanceData> list = service.invoke(runtimeContext);
            dataList.addAll(list);
            // } catch (Exception e) {
            //     LOGGER.warn("hook service invoke fail, serviceName={}, runtimeContext={}",
            //         service.getClass().getValueName(), runtimeContext);
            // }
        }
        return InstanceDataUtil.getInstanceDataMap(dataList);
    }

    private String saveInstanceDataPO(RuntimeContext runtimeContext) {
        String instanceDataId = genId();
        InstanceDataPO instanceDataPO = buildHookInstanceData(instanceDataId, runtimeContext);
        instanceDataDAO.insert(instanceDataPO);
        return instanceDataId;
    }

    @Override
    protected void postExecute(RuntimeContext runtimeContext) throws ProcessException {
        NodeInstanceBO currentNodeInstance = runtimeContext.getCurrentNodeInstance();
        currentNodeInstance.setInstanceDataId(runtimeContext.getInstanceDataId());
        currentNodeInstance.setStatus(NodeInstanceStatus.COMPLETED);
        runtimeContext.getNodeInstanceList().add(currentNodeInstance);
    }

    /**
     * Calculate unique outgoing
     * Expression: one of flowElement's properties
     * Input: data map
     *
     * @return
     * @throws Exception
     */
    @Override
    protected RuntimeExecutor getExecuteExecutor(RuntimeContext runtimeContext) throws ProcessException {
        FlowElement nextNode = null;
        Exception exception = null;
        FlowElement currentNodeModel = runtimeContext.getCurrentNodeModel();
        Map<String, Object> properties = currentNodeModel.getProperties();
        try {
            Map<String, InstanceData> instanceDataMap = runtimeContext.getInstanceDataMap();
            properties.put(COMPARE_DETAILS_KEY, new LinkedHashMap<String, Object>());
            nextNode = calculateNextNode(currentNodeModel,
                    runtimeContext.getFlowElementMap(), instanceDataMap);
        } catch (Exception e) {
            exception = e;
            String errorMsg = "分支计算失败。";
            if (e.getMessage() != null) {
                errorMsg += e.getMessage();
            }
            throw new RuntimeException(errorMsg, e);
        } finally {
            if (exclusiveGatewayLogService != null) {
                Map<String, Object> compareDetails = (Map<String, Object>) properties.remove(COMPARE_DETAILS_KEY);
                exclusiveGatewayLogService.invoke(runtimeContext, compareDetails, nextNode, exception);
            }
        }
        runtimeContext.setCurrentNodeModel(nextNode);
        return executorFactory.getElementExecutor(nextNode);
    }

    @Override
    protected FlowElement calculateNextNode(FlowElement currentFlowElement, Map<String, FlowElement> flowElementMap, Map<String, InstanceData> instanceDataMap) throws ProcessException {
        FlowElement nextFlowElement = calculateOutgoing(currentFlowElement, flowElementMap, instanceDataMap);

        while (nextFlowElement.getType() == FlowElementType.SEQUENCE_FLOW) {
            nextFlowElement = getUniqueNextNode(nextFlowElement, flowElementMap);
        }
        return nextFlowElement;
    }

    private FlowElement calculateOutgoing(FlowElement flowElement, Map<String, FlowElement> flowElementMap,
                                          Map<String, InstanceData> instanceDataMap) throws ProcessException {

        FlowElement defaultElement = null;
        InstanceData flowMap = instanceDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP);
        //当前流程用到的值都在这里
        JSONObject flowMapValue = (JSONObject) flowMap.getValue();

        List<String> outgoingList = flowElement.getOutgoing();
        int outgoingSize = outgoingList.size();
        Map<String, List<Map<String, Object>>> compareDetails = (Map<String, List<Map<String, Object>>>) flowElement.getProperties().get(COMPARE_DETAILS_KEY);
        //读取分支条件
        JSONArray conditionList = (JSONArray) flowElement.getProperties().get(ChatFlowConstant.ExclusiveGateway.CONDITION_LIST);
        if (conditionList == null) {
            throw new IllegalArgumentException("无法获取分支条件");
        }

        nextLoop:
        for (int i = 0; i < outgoingSize; i++) {
            List<Map<String, Object>> branchCompareDetails = new ArrayList<>();
            String outgoingKey = outgoingList.get(i);
            FlowElement outgoingSequenceFlow = FlowModelUtil.getFlowElement(flowElementMap, outgoingKey);
            //分支必须有至少一个出口，并且默认出口放到了最后
            if (i == outgoingSize - 1) {
                defaultElement = outgoingSequenceFlow;
                break;
            }
            compareDetails.put("分支" + (i + 1), branchCompareDetails);
            JSONObject condition = conditionList.getJSONObject(i);
            //每个分支的条件集合
            JSONArray itemList = condition.getJSONArray(ChatFlowConstant.ExclusiveGateway.ITEM_LIST);
            //子条件的组合方式，有两种组合方式：并、或
            String operator = condition.getString(ChatFlowConstant.ExclusiveGateway.OPERATOR);
            //是否匹配
            boolean isMatch = false;

            for (int k = 0; k < itemList.size(); k++) {
                //子条件
                JSONObject item = itemList.getJSONObject(k);
                //左侧变量所属节点
                String leftSideNodeKey = item.getString(ChatFlowConstant.ExclusiveGateway.ACT);
                //左侧变量所属节点名称
                String leftSideNodeName = item.getString(ChatFlowConstant.ExclusiveGateway.ACT_NAME);
                //左侧引用的参数名
                String leftSideValue = item.getString(ChatFlowConstant.ExclusiveGateway.NAME);
                //比较符号
                String itemOperator = item.getString(ChatFlowConstant.ExclusiveGateway.OPERATOR);
                //右侧值类型，有两种：引用、输入
                String rightSideFrom = item.getString(ChatFlowConstant.ExclusiveGateway.FROM);
                //右侧变量所属节点
                String rightSideNodeKey = item.getString(ChatFlowConstant.ExclusiveGateway.NODE_KEY);
                //右侧变量所属节点名称
                String rightSideNodeName = item.getString(ChatFlowConstant.ExclusiveGateway.NODE_NAME);
                //右侧引用的参数名或者输入值
                String rightSideValue = item.getString(ChatFlowConstant.ExclusiveGateway.VALUE);

                ExclusiveGatewayInParamMapping inParamMapping = new ExclusiveGatewayInParamMapping();
                inParamMapping.setLeftSideNodeKey(leftSideNodeKey);
                inParamMapping.setLeftSideNodeName(leftSideNodeName);
                inParamMapping.setLeftSideValue(leftSideValue);

                inParamMapping.setRightSideFrom(rightSideFrom);

                inParamMapping.setRightSideNodeKey(rightSideNodeKey);
                inParamMapping.setRightSideNodeName(rightSideNodeName);
                inParamMapping.setRightSideValue(rightSideValue);

                Object leftSideObject = inParamMapping.getLeftSideObject(flowMapValue);
                Map<String, Object> compareDetail = new LinkedHashMap<>();
                compareDetail.put("左侧值", leftSideObject);
                compareDetail.put("比较条件", translate(itemOperator));
                branchCompareDetails.add(compareDetail);
                boolean predicate = false;
                Object rightSideObject = null;
                if (!Objects.equals("isNull", itemOperator) && !Objects.equals("isNotNull", itemOperator)) {
                    rightSideObject = inParamMapping.getRightSideObject(flowMapValue);
                    compareDetail.put("右侧值", rightSideObject);
                }
                if (Objects.equals(rightSideFrom, ChatFlowConstant.ExclusiveGateway.REFERENCE)) {
                    predicate = predicateWhenValueIsPassed(itemOperator, leftSideObject, rightSideObject, true);
                } else {
                    predicate = predicateWhenValueIsInput(itemOperator, leftSideObject, (String) rightSideObject);
                }
                compareDetail.put("比较结果", predicate);

                if (operator.equals("and")) {
                    if (!predicate) {
                        continue nextLoop;
                    }
                    isMatch = true;
                } else if (operator.equals("or")) {
                    if (predicate) {
                        return outgoingSequenceFlow;
                    } else if (isMatch) {
                        return outgoingSequenceFlow;
                    }
                }
            }

            if (isMatch) {
                return outgoingSequenceFlow;
            }

        }
        //case2 return default while it has is configured
        if (defaultElement != null) {
            LOGGER.info("calculateOutgoing: return defaultElement.||nodeKey={}", flowElement.getKey());
            return defaultElement;
        }

        LOGGER.warn("calculateOutgoing failed.||nodeKey={}", flowElement.getKey());
        throw new ProcessException(ErrorEnum.GET_OUTGOING_FAILED);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ensureHookService();
    }

    private void ensureHookService() {
        if (hookServices != null) {
            return;
        }

        // init hook services by Spring application context
        synchronized (ExclusiveGatewayExecutor.class) {
            if (hookServices != null) {
                return;
            }
            hookServices = new ArrayList<>();
            String[] names = applicationContext.getBeanNamesForType(HookService.class);
            for (String name : names) {
                Object bean = applicationContext.getBean(name);
                if (bean != null) {
                    hookServices.add((HookService) bean);
                }
            }
        }
    }

    /**
     * 当右侧值是从上游传递下来的时候，此时不需要强制转换右侧值，直接比较值即可
     *
     * @param operator        条件符号
     * @param leftSideObject  左侧值
     * @param rightSideObject 右侧值
     * @return
     */
    private boolean predicateWhenValueIsPassed(String operator, Object leftSideObject, Object rightSideObject, boolean isFirst) {
        switch (operator) {
            case "eq":
                //不相等的类型就不用比较了
                typeCheck(leftSideObject, rightSideObject, isFirst);
                if (Objects.equals(leftSideObject, rightSideObject)) {
                    return true;
                }
                //有一个为空，另一个肯定不为空，此时不相等
                if (leftSideObject == null || rightSideObject == null) {
                    return false;
                }
                //如果是数组，那就比较数组内的元素
                if (leftSideObject instanceof Collection) {
                    return collectionCompare((Collection) leftSideObject, (Collection) rightSideObject);
                }
                break;
            case "ne":
                return !predicateWhenValueIsPassed("eq", leftSideObject, rightSideObject, isFirst);
            case "in":
                typeCheck(leftSideObject, rightSideObject, isFirst);
                //只有三种情况才算包含
                //1、两者都是字符串且左包含右
                if (leftSideObject instanceof String && rightSideObject instanceof String && ((String) leftSideObject).contains(rightSideObject.toString())) {
                    return true;
                }

                if (leftSideObject instanceof List) {
                    //2、左和右都是数组,且左包含右
                    if (rightSideObject instanceof List && Collections.indexOfSubList((List) leftSideObject, (List) rightSideObject) != -1) {
                        return true;
                    }
                    //3、左是数组，且数组的某个子项与右相等
                    List valueArray = (List) leftSideObject;
                    for (Object valueItem : valueArray) {
                        if (predicateWhenValueIsPassed("eq", valueItem, rightSideObject, false)) {
                            return true;
                        }
                    }
                }
                break;
            case "notIn":
                return !predicateWhenValueIsPassed("in", leftSideObject, rightSideObject, isFirst);
            case "gt":
            case "gte":
            case "lt":
            case "lte":
                //如果值是字符串，那就先转成数字再比较
                return numCompare(leftSideObject, rightSideObject, operator);
            case "isNull":
                return isEmptyContent(leftSideObject);
            case "isNotNull":
                return !predicateWhenValueIsPassed("isNull", leftSideObject, null, isFirst);
            default:
                throw new IllegalArgumentException("无法解析的比较符号:" + operator);
        }
        return false;
    }

    /**
     * 当右侧值是手输的时候，此时可能需要尝试强制转换右侧值后再比较
     *
     * @param operator        条件符号
     * @param leftSideObject  左侧值
     * @param rightSideObject 右侧值
     * @return
     */
    private boolean predicateWhenValueIsInput(String operator, Object leftSideObject, String rightSideObject) {
        switch (operator) {
            case "eq":
                if (Objects.equals(leftSideObject, rightSideObject)) {
                    return true;
                }
                //有一个为空，另一个肯定不为空，此时不相等
                if (leftSideObject == null || rightSideObject == null) {
                    return false;
                }
                //右不可输入为数组
                if (leftSideObject instanceof List) {
                    return false;
                }
                if (Objects.equals(leftSideObject.toString(), rightSideObject)) {
                    return true;
                }
                break;
            case "ne":
                return !predicateWhenValueIsInput("eq", leftSideObject, rightSideObject);
            case "in":
                //只有两种情况才算包含
                //1、两者都是字符串且左包含右
                if (leftSideObject instanceof String && rightSideObject != null && ((String) leftSideObject).contains(rightSideObject)) {
                    return true;
                }
                //2、左是数组，且数组的某个子项与右相等
                if (leftSideObject instanceof List) {
                    List leftSideArray = (List) leftSideObject;
                    for (Object o : leftSideArray) {
                        if (predicateWhenValueIsInput("eq", o, rightSideObject)) {
                            return true;
                        }
                    }
                }
                break;
            case "notIn":
                return !predicateWhenValueIsInput("in", leftSideObject, rightSideObject);
            case "gt":
            case "gte":
            case "lt":
            case "lte":
                return numCompare(leftSideObject, rightSideObject, operator);
            case "isNull":
                return isEmptyContent(leftSideObject);
            case "isNotNull":
                return !predicateWhenValueIsInput("isNull", leftSideObject, null);
            default:
                throw new IllegalArgumentException("无法解析的比较符号:" + operator);
        }
        return false;
    }

    private boolean isIntOrLong(String num) {
        return num.matches("-?\\d+");
    }

    private boolean isEmptyContent(Object obj) {
        if (Objects.isNull(obj)) {
            return true;
        }
        if (obj instanceof Collection && ((Collection) obj).isEmpty()) {
            return true;
        }
        if (obj instanceof Map && ((Map) obj).isEmpty()) {
            return true;
        }
        if (obj instanceof String && StringUtils.isBlank((String) obj)) {
            return true;
        }
        return false;
    }

    /**
     * 类型检查
     */
    private void typeCheck(Object leftSideObject, Object rightSideObject, boolean needThrowException) {
        if (!needThrowException) {
            return;
        }
        if (leftSideObject == null || rightSideObject == null) {
            return;
        }
        if (leftSideObject instanceof Collection && rightSideObject instanceof Collection) {
            return;
        }
        if (leftSideObject instanceof Number && rightSideObject instanceof Number) {
            return;
        }
        if (leftSideObject instanceof Map && rightSideObject instanceof Map) {
            return;
        }
        if (!leftSideObject.getClass().equals(rightSideObject.getClass())) {
            throw new IllegalArgumentException("类型不一致，无法比较。其中，左侧值为" + leftSideObject.getClass().getName() + "类型,右侧值为" + rightSideObject.getClass().getName() + "类型。");
        }
    }

    /**
     * 比较集合类型
     */
    private boolean collectionCompare(Collection<?> leftSideObject, Collection<?> rightSideObject) {
        if (leftSideObject.size() != rightSideObject.size()) {
            return false;
        }
        Iterator<?> leftIter = leftSideObject.iterator();
        Iterator<?> rightIter = rightSideObject.iterator();
        while (leftIter.hasNext()) {
            if (!Objects.equals(leftIter.next(), rightIter.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较整数
     */
    private boolean numCompare(Object leftSideObject, Object rightSideObject, String operator) {
        //如果值是字符串，那就先转成数字再比较
        if (leftSideObject == null) {
            throw new IllegalArgumentException("左侧值为null，无法进行比较");
        }
        if (rightSideObject == null) {
            throw new IllegalArgumentException("右侧值为null，无法进行比较");
        }
        Long leftValue = null;
        Long rightValue = null;
        if (leftSideObject instanceof String) {
            if (isIntOrLong((String) leftSideObject)) {
                leftValue = Long.parseLong((String) leftSideObject);
            } else {
                throw new IllegalArgumentException("左侧值不是整数类型字符串，无法比较");
            }
        } else if (leftSideObject instanceof Integer || leftSideObject instanceof Long) {
            leftValue = Long.valueOf(leftSideObject.toString());
        } else {
            throw new IllegalArgumentException("左侧值为" + leftSideObject.getClass().getName() + "类型，不支持比较");
        }
        if (rightSideObject instanceof String) {
            if (isIntOrLong((String) rightSideObject)) {
                rightValue = Long.parseLong((String) rightSideObject);
            } else {
                throw new IllegalArgumentException("右侧值不是整数类型字符串，无法比较");
            }
        } else if (rightSideObject instanceof Integer || rightSideObject instanceof Long) {
            rightValue = Long.valueOf(rightSideObject.toString());
        } else {
            throw new IllegalArgumentException("右侧值为" + rightSideObject.getClass().getName() + "类型，不支持比较");
        }
        if (operator.equals("gt")) {
            return leftValue > rightValue;
        } else if (operator.equals("gte")) {
            return leftValue >= rightValue;
        } else if (operator.equals("lt")) {
            return leftValue < rightValue;
        } else if (operator.equals("lte")) {
            return leftValue <= rightValue;
        }
        return false;
    }

    /**
     * 包装比较符号
     */
    private String translate(String operator) {
        switch (operator) {
            case "eq":
                return "等于";
            case "ne":
                return "不等于";
            case "in":
                return "包含";
            case "notIn":
                return "不包含";
            case "gt":
                return "大于";
            case "gte":
                return "大于等于";
            case "lt":
                return "小于";
            case "lte":
                return "小于等于";
            case "isNull":
                return "等于空";
            case "isNotNull":
                return "不等于空";
            default:
                return operator;
        }
    }
}
