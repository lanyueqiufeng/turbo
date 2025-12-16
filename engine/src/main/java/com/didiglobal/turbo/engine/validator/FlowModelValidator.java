package com.didiglobal.turbo.engine.validator;

import com.didiglobal.turbo.engine.common.ChatFlowConstant;
import com.didiglobal.turbo.engine.common.Constants;
import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.common.FlowElementType;
import com.didiglobal.turbo.engine.exception.DefinitionException;
import com.didiglobal.turbo.engine.exception.ForkJoinValidationException;
import com.didiglobal.turbo.engine.exception.ProcessException;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.model.FlowModel;
import com.didiglobal.turbo.engine.param.CommonParam;
import com.didiglobal.turbo.engine.result.CheckFlowItemVo;
import com.didiglobal.turbo.engine.util.FlowModelUtil;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FlowModelValidator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FlowModelValidator.class);

    @Resource
    private ElementValidatorFactory elementValidatorFactory;

    public void validate(FlowModel flowModel) throws ProcessException, DefinitionException {
        this.validate(flowModel, null);
    }

    public void validate(FlowModel flowModel, CommonParam commonParam) throws ProcessException, DefinitionException {
        if (flowModel == null || CollectionUtils.isEmpty(flowModel.getFlowElementList())) {
            LOGGER.warn("message={}", ErrorEnum.MODEL_EMPTY.getErrMsg());
            throw new DefinitionException(ErrorEnum.MODEL_EMPTY);
        }

        List<FlowElement> flowElementList = flowModel.getFlowElementList();
        Map<String, FlowElement> flowElementMap = Maps.newHashMap();

        for (FlowElement flowElement : flowElementList) {
            if (flowElementMap.containsKey(flowElement.getKey())) {
                String elementName = FlowModelUtil.getElementName(flowElement);
                String elementKey = flowElement.getKey();
                String exceptionMsg = MessageFormat.format(Constants.MODEL_DEFINITION_ERROR_MSG_FORMAT,
                        ErrorEnum.ELEMENT_KEY_NOT_UNIQUE, elementName, elementKey);
                LOGGER.warn(exceptionMsg);
                throw new DefinitionException(ErrorEnum.ELEMENT_KEY_NOT_UNIQUE.getErrNo(), exceptionMsg);
            }
            flowElementMap.put(flowElement.getKey(), flowElement);
        }

        int startEventCount = 0;
        int endEventCount = 0;

        for (FlowElement flowElement : flowElementList) {

            ElementValidator elementValidator = elementValidatorFactory.getElementValidator(flowElement);
            elementValidator.validate(flowElementMap, flowElement, commonParam);

            if (FlowElementType.START_EVENT == flowElement.getType()) {
                startEventCount++;
            }

            if (FlowElementType.END_EVENT == flowElement.getType()) {
                endEventCount++;
            }
        }

        if (startEventCount != 1) {
            LOGGER.warn("message={}||startEventCount={}", ErrorEnum.START_NODE_INVALID.getErrMsg(), startEventCount);
            throw new DefinitionException(ErrorEnum.START_NODE_INVALID);
        }

        if (endEventCount < 1) {
            LOGGER.warn("message={}", ErrorEnum.END_NODE_INVALID.getErrMsg());
            throw new DefinitionException(ErrorEnum.END_NODE_INVALID);
        }
    }

    /**
     * 改造{@link #validate}
     * 抛出异常 —> 返回元素错误信息
     **/
    public List<CheckFlowItemVo> check(FlowModel flowModel) throws ProcessException, DefinitionException {
        List<CheckFlowItemVo> checkItemVos = new ArrayList<>();
        if (flowModel == null || CollectionUtils.isEmpty(flowModel.getFlowElementList())) {
            throw new DefinitionException(ErrorEnum.MODEL_EMPTY);
        }

        List<FlowElement> flowElementList = flowModel.getFlowElementList();
        Map<String, FlowElement> flowElementMap = Maps.newHashMap();
        // key nodeType
        Map<String, String> forkJoinMap = Maps.newHashMap();
        int startEventCount = 0;
        int endEventCount = 0;
        FlowElement startElement = null;
        boolean next = true;
        for (FlowElement flowElement : flowElementList) {
            if (flowElementMap.containsKey(flowElement.getKey())) {
                String elementName = FlowModelUtil.getElementName(flowElement);
                String elementKey = flowElement.getKey();
                String exceptionMsg = MessageFormat.format(Constants.MODEL_DEFINITION_ERROR_MSG_FORMAT,
                        ErrorEnum.ELEMENT_KEY_NOT_UNIQUE, elementName, elementKey);
                throw new DefinitionException(ErrorEnum.ELEMENT_KEY_NOT_UNIQUE.getErrNo(), exceptionMsg);
            }
            flowElementMap.put(flowElement.getKey(), flowElement);
            if (9 == flowElement.getType()) {
                // 这里也不校验空指针，相信前端
                forkJoinMap.put(flowElement.getKey(), (String) flowElement.getProperties().get("nodeType"));
            }
            ElementValidator elementValidator = elementValidatorFactory.getElementValidator(flowElement);
            try {
                elementValidator.check(flowElementMap, flowElement);
            } catch (DefinitionException e) {
                CheckFlowItemVo checkItemVo = new CheckFlowItemVo();
                checkItemVo.setElementKey(flowElement.getKey());
                checkItemVo.setElementType(FlowModelUtil.getElementType(flowElement));
                checkItemVo.setElementName(FlowModelUtil.getElementName(flowElement));
                checkItemVo.setErrNo(e.getErrNo());
                checkItemVo.setExceptionMsg(e.getMessage());
                checkItemVos.add(checkItemVo);
                next = false;
            }
            if (FlowElementType.START_EVENT == flowElement.getType()) {
                startEventCount++;
                startElement = flowElement;
            }

            if (FlowElementType.END_EVENT == flowElement.getType()) {
                endEventCount++;
            }
        }

        if (startEventCount != 1) {
            CheckFlowItemVo checkItemVo = new CheckFlowItemVo();
            checkItemVo.setElementName("START_EVENT");
            checkItemVo.setExceptionMsg(ErrorEnum.START_NODE_INVALID.getErrMsg());
            checkItemVo.setErrNo(ErrorEnum.START_NODE_INVALID.getErrNo());
            checkItemVo.setElementType(ChatFlowConstant.NodeType.START);
            checkItemVos.add(checkItemVo);
        }

        if (endEventCount < 1) {
            CheckFlowItemVo checkItemVo = new CheckFlowItemVo();
            checkItemVo.setElementName("END_EVENT");
            checkItemVo.setExceptionMsg(ErrorEnum.END_NODE_INVALID.getErrMsg());
            checkItemVo.setElementType(ChatFlowConstant.NodeType.END);
            checkItemVo.setErrNo(ErrorEnum.END_NODE_INVALID.getErrNo());
            checkItemVos.add(checkItemVo);
        }
        if (!forkJoinMap.isEmpty() && startEventCount == 1 && next) {
            checkForkJoin(startElement, forkJoinMap, flowElementMap, checkItemVos);
        }
        return checkItemVos;
    }

    private void checkForkJoin(FlowElement startElement,
                               Map<String, String> forkJoinMap,
                               Map<String, FlowElement> flowElementMap, List<CheckFlowItemVo> checkItemVos) {
        boolean next = true;
        try {
            for (Map.Entry<String, String> entry : forkJoinMap.entrySet()) {
                // key: flowElement.getKey(),  value:nodeType
                String nodeKey = entry.getKey();
                String type = entry.getValue();
                FlowElement flowElement = flowElementMap.get(nodeKey);
                Map<String, Object> properties = flowElement.getProperties();
                if (null == type) {
                    throw new ForkJoinValidationException(nodeKey, (String) properties.get("name"), type, "节点类型不存在");
                }
                //如果当前是fork
                boolean isFork = "fork".equals(type);
                // 之前节点校验保证 forkJoinMatch 配置了 fork join 这里直接转型
                Object match = properties.get(ChatFlowConstant.PropKey.Fork_JOIN);
                Map forkJoinMatch = (Map) match;
                //获取fork和jion节点
                String fork = (String) forkJoinMatch.get("fork");
                String join = (String) forkJoinMatch.get("join");
                FlowElement forkORjoin = flowElementMap.get(isFork ? join : fork);
                // !nodeKey.equals(isFork ? fork : join) 自己配置不是自己(一般来说不太可能)    对方的配置不是自己
                if (!nodeKey.equals(isFork ? fork : join) || null == forkORjoin
                        || 9 != forkORjoin.getType() || null == forkORjoin.getProperties()
                        || !match.equals(forkORjoin.getProperties().get(ChatFlowConstant.PropKey.Fork_JOIN))) {
                    throw new ForkJoinValidationException(
                            nodeKey,
                            (String) properties.get("name"),
                            type,
                            isFork ? ErrorEnum.FORK_NOT_MATCH.getErrMsg() : ErrorEnum.JOIN_NOT_MATCH.getErrMsg()
                    );
                }
            }
        } catch (ForkJoinValidationException e) {
            CheckFlowItemVo checkItemVo = new CheckFlowItemVo();
            checkItemVo.setElementName(e.getElementName());
            checkItemVo.setExceptionMsg(e.getMessage());
            checkItemVo.setElementType(e.getType());
            checkItemVo.setElementKey(e.getElementKey());
            // 缺少nodetype
            checkItemVo.setErrNo(ErrorEnum.FORK_JOIN_INVALID.getErrNo());
            if (e.getType().equals("fork")){
                checkItemVo.setErrNo(ErrorEnum.FORK_NOT_MATCH.getErrNo());
            }
            if (e.getType().equals("join")){
                checkItemVo.setErrNo(ErrorEnum.JOIN_NOT_MATCH.getErrNo());
            }
            checkItemVos.add(checkItemVo);
            next = false;
        }
        if (next) {
            try {
                checkAll(startElement, new ArrayDeque<>(), new HashSet<>(), flowElementMap, forkJoinMap);
            } catch (ForkJoinValidationException e) {
                CheckFlowItemVo checkItemVo = new CheckFlowItemVo();
                checkItemVo.setElementName(e.getElementName());
                checkItemVo.setExceptionMsg(e.getMessage());
                checkItemVo.setErrNo(ErrorEnum.FORK_JOINNOT_EXIST.getErrNo());
                checkItemVo.setElementType(FlowModelUtil.getElementType(flowElementMap.get(e.getElementKey())));
                if (e.getType().equals("3")){
                    checkItemVo.setExceptionMsg(ErrorEnum.JOIN_NOT_END_EVENT.getErrMsg());
                    checkItemVo.setErrNo(ErrorEnum.JOIN_NOT_END_EVENT.getErrNo());
                }
                if (e.getType().equals("4")){
                    checkItemVo.setExceptionMsg(ErrorEnum.JOIN_NOT_USER_TASK.getErrMsg());
                    checkItemVo.setErrNo(ErrorEnum.JOIN_NOT_USER_TASK.getErrNo());
                }
                if (e.getType().equals("8")){
                    checkItemVo.setExceptionMsg(ErrorEnum.JOIN_NOT_CALL_ACTIVITY.getErrMsg());
                    checkItemVo.setErrNo(ErrorEnum.JOIN_NOT_CALL_ACTIVITY.getErrNo());
                }
                checkItemVo.setElementKey(e.getElementKey());
                checkItemVos.add(checkItemVo);
            }
        }
    }

    private void checkAll(
            FlowElement current,
            Deque<String> expectedJoins,
            Set<String> parentVisited,
            Map<String, FlowElement> flowElementMap,
            Map<String, String> forkJoinTypeMap) throws ForkJoinValidationException {

        String nodeKey = current.getKey();
        // 判断是否遍历过这个节点，如果遍历过则退出
        if (parentVisited.contains(nodeKey)) {
            return;
        }
        int elementType = current.getType();

        Set<String> currentVisited = new HashSet<>(parentVisited);
        currentVisited.add(nodeKey);

        Deque<String> newStack = new ArrayDeque<>(expectedJoins);
        String type = forkJoinTypeMap.get(nodeKey);

        // 1. 禁止中断任务
        if (!newStack.isEmpty()) {
            if (elementType == FlowElementType.END_EVENT
                    || elementType == FlowElementType.USER_TASK
                    || elementType == FlowElementType.CALL_ACTIVITY) {
                String typeName;
                switch (elementType) {
                    case FlowElementType.END_EVENT:
                        typeName = "结束节点";
                        break;
                    case FlowElementType.USER_TASK:
                        typeName = "用户任务";
                        break;
                    case FlowElementType.CALL_ACTIVITY:
                        typeName = "子流程";
                        break;
                    default:
                        typeName = "非法节点";
                }
                throw new ForkJoinValidationException(
                        nodeKey,
                        (String) current.getProperties().get("name"),
                        String.valueOf(elementType),
                        "并行块内不允许出现"+typeName +"节点"
                );
            }
        }

        // 2. fork join
        if ("fork".equals(type)) {
            Map<String, Object> props = current.getProperties();
            Object matchObj = props.get(ChatFlowConstant.PropKey.Fork_JOIN);
            Map match = (Map) matchObj;
            String joinKey = (String) match.get("join");
            newStack.push(joinKey);
        } else if ("join".equals(type)) {
            if (newStack.isEmpty()) {
                throw new ForkJoinValidationException(nodeKey, (String) current.getProperties().get("name"), type,
                        "汇聚节点无对应的并行节点"
                );
            }
            String expected = newStack.peek();
            if (!expected.equals(nodeKey)) {
                throw new ForkJoinValidationException(nodeKey, (String) current.getProperties().get("name"), type,
                        "汇聚节点"+nodeKey+"与期望的"+expected+"不匹配（可能交叉配置）"
                );
            }
            newStack.pop();
        }

        // 3.分支终点检查
        List<String> outgoing = current.getOutgoing();
        if (outgoing == null || outgoing.isEmpty()) {
            if (!newStack.isEmpty()) {
                throw new ForkJoinValidationException(nodeKey, (String) current.getProperties().get("name"), String.valueOf(elementType),
                        "路径结束，但存在未关闭的并行节点: " + newStack
                );
            }
            return;
        }

        // 4.循环
        for (String nextKey : outgoing) {
            FlowElement next = flowElementMap.get(nextKey);
            if (next != null) {
                checkAll(next, newStack, currentVisited, flowElementMap, forkJoinTypeMap);
            }
        }
    }
}
