package com.didiglobal.turbo.engine.validator;

import com.didiglobal.turbo.engine.common.ChatFlowConstant;
import com.didiglobal.turbo.engine.common.Constants;
import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.common.FlowElementType;
import com.didiglobal.turbo.engine.exception.DefinitionException;
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
import java.util.ArrayList;
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
        }

        int startEventCount = 0;
        int endEventCount = 0;

        for (FlowElement flowElement : flowElementList) {

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
            }
            if (FlowElementType.START_EVENT == flowElement.getType()) {
                startEventCount++;
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
        if (!forkJoinMap.isEmpty()) {
            forkJoinMap.forEach((node, type) -> {
                FlowElement flowElement = flowElementMap.get(node);
                Map<String, Object> properties = flowElement.getProperties();
                if (null == type){
                    // 以防万一
                    CheckFlowItemVo checkItemVo = new CheckFlowItemVo();
                    checkItemVo.setElementName((String) properties.get("name"));
                    checkItemVo.setExceptionMsg(ErrorEnum.FORK_JOIN_INVALID.getErrMsg());
                    checkItemVo.setErrNo(ErrorEnum.FORK_JOIN_INVALID.getErrNo());
                    checkItemVo.setElementType(type);
                    checkItemVos.add(checkItemVo);
                }
                //如果当前是fork
                Boolean isFork = "fork".equals(type);
                // 节点校验保证 forkJoinMatch 配置了 fork join 这里直接转型
                Object match = properties.get(ChatFlowConstant.PropKey.Fork_JOIN);
                Map forkJoinMatch = (Map) match;
                //获取fork和jion节点
                String fork = (String) forkJoinMatch.get("fork");
                String join = (String) forkJoinMatch.get("join");

                FlowElement opposite = flowElementMap.get(isFork ? join : fork);
                // 自己配置不是自己 对方的配置不是自己
                if (!node.equals(isFork ? fork : join) || null == opposite
                        || 9 != opposite.getType() || null == opposite.getProperties()
                        || !match.equals(opposite.getProperties().get(ChatFlowConstant.PropKey.Fork_JOIN))) {
                    CheckFlowItemVo checkItemVo = new CheckFlowItemVo();
                    checkItemVo.setElementName((String) properties.get("name"));
                    checkItemVo.setExceptionMsg(
                            isFork ? ErrorEnum.FORK_NOT_MATCH.getErrMsg() : ErrorEnum.JOIN_NOT_MATCH.getErrMsg());
                    checkItemVo.setErrNo(
                            isFork ? ErrorEnum.FORK_NOT_MATCH.getErrNo() : ErrorEnum.JOIN_NOT_MATCH.getErrNo());
                    checkItemVo.setElementType(type);
                    checkItemVo.setElementKey(flowElement.getKey());
                    checkItemVos.add(checkItemVo);
                }
                if ("fork".equals(type) && null != opposite) {
                    try {
                        checkNext(flowElement, flowElement, flowElementMap, forkJoinMap, new HashSet<>());
                    } catch (Exception e) {
                        CheckFlowItemVo checkItemVo = new CheckFlowItemVo();
                        checkItemVo.setElementName((String) properties.get("name"));
                        checkItemVo.setExceptionMsg(ErrorEnum.FORK_JOINNOT_EXIST.getErrMsg());
                        checkItemVo.setErrNo(ErrorEnum.FORK_JOINNOT_EXIST.getErrNo());
                        if (e.getMessage().equals("3")){
                            checkItemVo.setExceptionMsg(ErrorEnum.JOIN_NOT_END_EVENT.getErrMsg());
                            checkItemVo.setErrNo(ErrorEnum.JOIN_NOT_END_EVENT.getErrNo());
                        }
                        if (e.getMessage().equals("4")){
                            checkItemVo.setExceptionMsg(ErrorEnum.JOIN_NOT_USER_TASK.getErrMsg());
                            checkItemVo.setErrNo(ErrorEnum.JOIN_NOT_USER_TASK.getErrNo());
                        }
                        if (e.getMessage().equals("8")){
                            checkItemVo.setExceptionMsg(ErrorEnum.JOIN_NOT_CALL_ACTIVITY.getErrMsg());
                            checkItemVo.setErrNo(ErrorEnum.JOIN_NOT_CALL_ACTIVITY.getErrNo());
                        }
                        checkItemVo.setElementType(type);
                        checkItemVo.setElementKey(flowElement.getKey());
                        checkItemVos.add(checkItemVo);
                    }
                }
            });
        }
        return checkItemVos;
    }

    //
    private void checkNext(FlowElement forkElement,
                           FlowElement currentElement,
                           Map<String, FlowElement> flowElementMap,
                           Map<String, String> forkJoinTypeMap,
                           Set<String> visitedNodes) throws Exception {
        // 记录当前节点已访问
        if (!visitedNodes.add(currentElement.getKey())) {
            return;  // 如果已访问过，直接返回
        }
        List<String> outgoingKeyList = currentElement.getOutgoing();
        for (String next : outgoingKeyList) {
            FlowElement nextElement = FlowModelUtil.getFlowElement(flowElementMap, next);
            if (nextElement.getType() == FlowElementType.END_EVENT
                    || nextElement.getType() == FlowElementType.USER_TASK
                    || nextElement.getType() == FlowElementType.CALL_ACTIVITY) {
                throw new Exception(String.valueOf(nextElement.getType()));
            } else if (nextElement.getType() == FlowElementType.PARALLEL_PLUGIN) {
                // 其他join异常 其他fork/匹配的join结束
                Object join = ((Map) forkElement.getProperties().get(ChatFlowConstant.PropKey.Fork_JOIN)).get("join");
                if ("join".equals(forkJoinTypeMap.get(next)) && !next.equals(join)) {
                    // 三级并行 无法确定二级和三级上下级关系
                    throw new Exception("无法匹配");
                }
            } else {
                // 线或者其他无需校验环节
                checkNext(forkElement, nextElement, flowElementMap, forkJoinTypeMap, visitedNodes);
            }
        }
    }
}
