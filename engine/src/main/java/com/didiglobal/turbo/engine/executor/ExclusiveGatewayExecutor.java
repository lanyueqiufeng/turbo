package com.didiglobal.turbo.engine.executor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.bo.NodeInstanceBO;
import com.didiglobal.turbo.engine.common.*;
import com.didiglobal.turbo.engine.entity.InstanceDataPO;
import com.didiglobal.turbo.engine.exception.ProcessException;
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
            //         service.getClass().getName(), runtimeContext);
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
        InstanceData flowMap = instanceDataMap.get("flowMap");
        //当前流程用到的值都在这里
        JSONObject flowMapValue = (JSONObject) flowMap.getValue();
        //key为节点id
        Map<String, Map<String, Object>> valueMap = new HashMap<>();
        flowMapValue.forEach((key, value) -> valueMap.put(key, JSONObject.parseObject(value.toString(), Map.class)));

        List<String> outgoingList = flowElement.getOutgoing();
        //读取分支条件
        JSONArray conditionList = (JSONArray) flowElement.getProperties().get("conditionList");

        int outgoingSize = outgoingList.size();

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
            JSONArray itemList = condition.getJSONArray("itemList");
            //子条件的组合方式，有两种组合方式：并、或
            String operator = condition.getString("operator");
            //是否匹配
            boolean isMatch = false;

            for (int k = 0; k < itemList.size(); k++) {
                //子条件
                JSONObject conditionItem = itemList.getJSONObject(k);
                //变量所在节点的id
                String act = conditionItem.getString("act");
                //变量的名字
                String name = conditionItem.getString("name");
                //比较符号
                String conditionItemOperator = conditionItem.getString("operator");
                //比较值类型，有两种：引用、输入
                String from = conditionItem.getString("from");
                //如果是引用类型，此值表示引用节点的id
                String nodeKey = conditionItem.getString("nodeKey");
                //如果是引用类型，此值表示引用节点的变量名
                Object value = conditionItem.getString("value");
                if (from.equals("Reference")) {
                    value = valueMap.get(nodeKey).get(value);
                }
                boolean predicate = false;
                if (Objects.equals(from, "Reference")) {
                    predicate = predicateWhenValueIsPassed(conditionItemOperator, valueMap.get(act).get(name), value);
                } else {
                    predicate = predicateWhenValueIsInput(conditionItemOperator, valueMap.get(act).get(name), (String) value);
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
     * 当value是从上游传递下来的时候，此时不需要强制转换value，直接比较值即可
     *
     * @param conditionItemOperator 条件符号
     * @param variable              变量
     * @param value                 比较值
     * @return
     */
    private boolean predicateWhenValueIsPassed(String conditionItemOperator, Object variable, Object value) {
        switch (conditionItemOperator) {
            case "eq":
                if (Objects.equals(variable, value)) {
                    return true;
                }
                //有一个为空，另一个肯定不为空，此时不相等
                if (variable == null || value == null) {
                    return false;
                }
                //不相等的类型就不用比较了
                if (!variable.getClass().equals(value.getClass())) {
                    return false;
                }
                //如果是数组，那就比较数组内的元素
                if (variable instanceof JSONArray) {
                    return variable.toString().equals(value.toString());
                }
                break;
            case "ne":
                if (!Objects.equals(variable, value)) {
                    return true;
                }
                //variable和value不可能同时为null，所以此时有一个为null那就证明两个值不相等
                if (variable == null || value == null) {
                    return true;
                }
                //不相等的类型就不用比较了
                if (!variable.getClass().equals(value.getClass())) {
                    return true;
                }
                //如果是数组，那就比较数组内的元素
                if (variable instanceof JSONArray) {
                    return !variable.toString().equals(value.toString());
                }
                break;
            case "in":
                if (variable instanceof String && value instanceof String && ((String) variable).contains(value.toString())) {
                    return true;
                }
                if (!(value instanceof JSONArray)) {
                    return false;
                }
                JSONArray valueArray = (JSONArray) value;
                for (Object valueItem : valueArray) {
                    if (Objects.equals(variable + "", valueItem.toString())) {
                        return true;
                    }
                }
                break;
            case "notIn":
                if (variable instanceof String && value instanceof String && !((String) variable).contains(value.toString())) {
                    return true;
                }
                if (!(value instanceof JSONArray)) {
                    return false;
                }
                valueArray = (JSONArray) value;
                for (Object valueItem : valueArray) {
                    if (Objects.equals(variable + "", valueItem.toString())) {
                        return false;
                    }
                }
                return true;
            case "gt":
            case "gte":
            case "lt":
            case "lte":
                if (!(value instanceof Integer) || !(variable instanceof Integer)) {
                    return false;
                }
                if (conditionItemOperator.equals("gt")) {
                    return (Integer) variable > (Integer) value;
                } else if (conditionItemOperator.equals("gte")) {
                    return (Integer) variable >= (Integer) value;
                } else if (conditionItemOperator.equals("lt")) {
                    return (Integer) variable < (Integer) value;
                } else if (conditionItemOperator.equals("lte")) {
                    return (Integer) variable <= (Integer) value;
                }
                break;
            default:
                throw new IllegalArgumentException("无法解析的比较符号:" + conditionItemOperator);
        }
        return false;
    }

    /**
     * 当value是手输的时候，此时可能需要尝试强制转换value后再比较
     *
     * @param conditionItemOperator 条件符号
     * @param variable              变量
     * @param value                 比较值
     * @return
     */
    private boolean predicateWhenValueIsInput(String conditionItemOperator, Object variable, String value) {
        switch (conditionItemOperator) {
            case "eq":
                if (Objects.equals(variable, value)) {
                    return true;
                }
                //有一个为空，另一个肯定不为空，此时不相等
                if (variable == null || value == null) {
                    return false;
                }
                //value不可能输入为数组
                if (variable instanceof JSONArray) {
                    return false;
                }
                if (Objects.equals(variable.toString(), value)) {
                    return true;
                }
                break;
            case "ne":
                if (!Objects.equals(variable, value)) {
                    return true;
                }
                //variable和value不可能同时为null，所以此时有一个为null那就证明两个值不相等
                if (variable == null || value == null) {
                    return true;
                }
                //value不可能输入为数组
                if (variable instanceof JSONArray) {
                    return true;
                }
                if (!Objects.equals(variable.toString(), value)) {
                    return true;
                }
                break;
            case "in":
                if (variable instanceof String && value != null && ((String) variable).contains(value)) {
                    return true;
                }
                break;
            case "notIn":
                if (variable instanceof String && value != null && !((String) variable).contains(value)) {
                    return true;
                }
                break;
            case "gt":
            case "gte":
            case "lt":
            case "lte":
                if (value == null) {
                    return false;
                }
                if (!(variable instanceof Integer) || !value.matches("-?\\d+")) {
                    return false;
                }
                int parsed = Integer.parseInt(value);
                if (conditionItemOperator.equals("gt")) {
                    return (Integer) variable > parsed;
                } else if (conditionItemOperator.equals("gte")) {
                    return (Integer) variable >= parsed;
                } else if (conditionItemOperator.equals("lt")) {
                    return (Integer) variable < parsed;
                } else if (conditionItemOperator.equals("lte")) {
                    return (Integer) variable <= parsed;
                }
                break;
            default:
                throw new IllegalArgumentException("无法解析的比较符号:" + conditionItemOperator);
        }
        return false;
    }
}
