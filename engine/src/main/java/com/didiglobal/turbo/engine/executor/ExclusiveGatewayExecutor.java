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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class ExclusiveGatewayExecutor extends ElementExecutor implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExclusiveGatewayExecutor.class);

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

    private InstanceDataPO buildHookInstanceData(String instanceDataId, RuntimeContext runtimeContext) {
        InstanceDataPO instanceDataPO = new InstanceDataPO();
        BeanUtils.copyProperties(runtimeContext, instanceDataPO);
        instanceDataPO.setInstanceDataId(instanceDataId);
        instanceDataPO.setInstanceData(InstanceDataUtil.getInstanceDataListStr(runtimeContext.getInstanceDataMap()));
        instanceDataPO.setNodeInstanceId(runtimeContext.getCurrentNodeInstance().getNodeInstanceId());
        instanceDataPO.setNodeKey(runtimeContext.getCurrentNodeModel().getKey());
        instanceDataPO.setType(InstanceDataType.HOOK);
        instanceDataPO.setCreateTime(new Date());
        return instanceDataPO;
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
        FlowElement nextNode = calculateNextNode(runtimeContext.getCurrentNodeModel(),
                runtimeContext.getFlowElementMap(), runtimeContext.getInstanceDataMap());

        runtimeContext.setCurrentNodeModel(nextNode);
        if (exclusiveGatewayLogService != null) {
            exclusiveGatewayLogService.invoke(runtimeContext);
        }
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

        //读取分支条件
        JSONArray conditionList = (JSONArray) flowElement.getProperties().get(ChatFlowConstant.ExclusiveGateway.CONDITION_LIST);

        nextLoop:
        for (int i = 0; i < outgoingSize; i++) {
            String outgoingKey = outgoingList.get(i);
            FlowElement outgoingSequenceFlow = FlowModelUtil.getFlowElement(flowElementMap, outgoingKey);
            //分支必须有至少一个出口，并且默认出口放到了最后
            if (i == outgoingSize - 1) {
                defaultElement = outgoingSequenceFlow;
                break;
            }
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
                //左侧引用的参数名
                String leftSideValue = item.getString(ChatFlowConstant.ExclusiveGateway.NAME);
                //比较符号
                String itemOperator = item.getString(ChatFlowConstant.ExclusiveGateway.OPERATOR);
                //右侧值类型，有两种：引用、输入
                String rightSideFrom = item.getString(ChatFlowConstant.ExclusiveGateway.FROM);
                //右侧变量所属节点
                String rightSideNodeKey = item.getString(ChatFlowConstant.ExclusiveGateway.NODE_KEY);
                //右侧引用的参数名或者输入值
                String rightSideValue = item.getString(ChatFlowConstant.ExclusiveGateway.VALUE);

                ExclusiveGatewayInParamMapping inParamMapping = new ExclusiveGatewayInParamMapping();
                inParamMapping.setLeftSideNodeKey(leftSideNodeKey);
                inParamMapping.setLeftSideValue(leftSideValue);
                inParamMapping.setRightSideFrom(rightSideFrom);
                inParamMapping.setRightSideNodeKey(rightSideNodeKey);
                inParamMapping.setRightSideValue(rightSideValue);

                Object leftSideObject = inParamMapping.getLeftSideObject(flowMapValue);
                boolean predicate = false;
                if (Objects.equals("isNull", itemOperator)) {
                    predicate = Objects.isNull(leftSideObject);
                } else if (Objects.equals("isNotNull", itemOperator)) {
                    predicate = !Objects.isNull(leftSideObject);
                } else {
                    Object rightSideObject = inParamMapping.getRightSideObject(flowMapValue);
                    if (Objects.equals(rightSideFrom, ChatFlowConstant.ExclusiveGateway.REFERENCE)) {
                        predicate = predicateWhenValueIsPassed(itemOperator, leftSideObject, rightSideObject);
                    } else {
                        predicate = predicateWhenValueIsInput(itemOperator, leftSideObject, (String) rightSideObject);
                    }
                }
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
    private boolean predicateWhenValueIsPassed(String operator, Object leftSideObject, Object rightSideObject) {
        switch (operator) {
            case "eq":
                if (Objects.equals(leftSideObject, rightSideObject)) {
                    return true;
                }
                //有一个为空，另一个肯定不为空，此时不相等
                if (leftSideObject == null || rightSideObject == null) {
                    return false;
                }
                //不相等的类型就不用比较了
                if (!leftSideObject.getClass().equals(rightSideObject.getClass())) {
                    return false;
                }
                //如果是数组，那就比较数组内的元素
                if (leftSideObject instanceof List) {
                    return leftSideObject.toString().equals(rightSideObject.toString());
                }
                break;
            case "ne":
                if (!Objects.equals(leftSideObject, rightSideObject)) {
                    return true;
                }
                //左和右不可能同时为null，所以此时有一个为null那就证明两个值不相等
                if (leftSideObject == null) {
                    return true;
                }
                //不相等的类型就不用比较了
                if (!leftSideObject.getClass().equals(rightSideObject.getClass())) {
                    return true;
                }
                //如果是数组，那就比较数组内的元素
                if (leftSideObject instanceof List) {
                    return !leftSideObject.toString().equals(rightSideObject.toString());
                }
                break;
            case "in":
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
                        if (predicateWhenValueIsPassed("eq", valueItem, rightSideObject)) {
                            return true;
                        }
                    }
                }
                break;
            case "notIn":
                //只有三种情况才算包含，其他都是不包含
                //1、两者都是字符串且左包含右
                if (leftSideObject instanceof String && rightSideObject instanceof String && ((String) leftSideObject).contains(rightSideObject.toString())) {
                    return false;
                }
                if (leftSideObject instanceof List) {
                    //2、左和右都是数组,且左包含右
                    if (rightSideObject instanceof List && Collections.indexOfSubList((List) leftSideObject, (List) rightSideObject) != -1) {
                        return false;
                    }
                    //3、左是数组，且数组的某个子项与右相等
                    List valueArray = (List) leftSideObject;
                    for (Object valueItem : valueArray) {
                        if (predicateWhenValueIsPassed("eq", valueItem, rightSideObject)) {
                            return false;
                        }
                    }
                }
                return true;
            case "gt":
            case "gte":
            case "lt":
            case "lte":
                if (!(rightSideObject instanceof Integer) || !(leftSideObject instanceof Integer)) {
                    return false;
                }
                if (operator.equals("gt")) {
                    return (Integer) leftSideObject > (Integer) rightSideObject;
                } else if (operator.equals("gte")) {
                    return (Integer) leftSideObject >= (Integer) rightSideObject;
                } else if (operator.equals("lt")) {
                    return (Integer) leftSideObject < (Integer) rightSideObject;
                } else if (operator.equals("lte")) {
                    return (Integer) leftSideObject <= (Integer) rightSideObject;
                }
                break;
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
                //左和右不可能同时为null，所以此时有一个为null那就证明两个值不相等
                if (leftSideObject == null || rightSideObject == null) {
                    return true;
                }
                //右不可输入为数组
                if (leftSideObject instanceof List) {
                    return true;
                }
                if (!Objects.equals(leftSideObject.toString(), rightSideObject)) {
                    return true;
                }
                break;
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
                //只有两种情况才算包含，其他都是不包含
                //1、两者都是字符串且左包含右
                if (leftSideObject instanceof String && rightSideObject != null && ((String) leftSideObject).contains(rightSideObject)) {
                    return false;
                }
                //2、左是数组，且数组的某个子项与右相等
                if (leftSideObject instanceof List) {
                    List valueArray = (List) leftSideObject;
                    for (Object valueItem : valueArray) {
                        if (predicateWhenValueIsPassed("eq", valueItem, rightSideObject)) {
                            return false;
                        }
                    }
                }
                return true;
            case "gt":
            case "gte":
            case "lt":
            case "lte":
                if (rightSideObject == null) {
                    return false;
                }
                if (!(leftSideObject instanceof Integer) || !rightSideObject.matches("-?\\d+")) {
                    return false;
                }
                int parsed = Integer.parseInt(rightSideObject);
                if (operator.equals("gt")) {
                    return (Integer) leftSideObject > parsed;
                } else if (operator.equals("gte")) {
                    return (Integer) leftSideObject >= parsed;
                } else if (operator.equals("lt")) {
                    return (Integer) leftSideObject < parsed;
                } else if (operator.equals("lte")) {
                    return (Integer) leftSideObject <= parsed;
                }
                break;
            default:
                throw new IllegalArgumentException("无法解析的比较符号:" + operator);
        }
        return false;
    }
}
