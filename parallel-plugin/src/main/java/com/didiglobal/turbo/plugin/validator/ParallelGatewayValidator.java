package com.didiglobal.turbo.plugin.validator;

import com.didiglobal.turbo.engine.common.ChatFlowConstant;
import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.exception.DefinitionException;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.param.CommonParam;
import com.didiglobal.turbo.engine.validator.ElementValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ParallelGatewayValidator extends ElementValidator {



    protected void check(Map<String, FlowElement> flowElementMap,FlowElement flowElement) throws DefinitionException {
        checkIncoming(flowElementMap, flowElement);
        checkOutgoing(flowElementMap, flowElement);
        checkProperties(flowElementMap, flowElement);
    }

    protected void checkProperties(Map<String, FlowElement> flowElementMap, FlowElement flowElement) throws DefinitionException {
        //在这个地方可以去checklist 并行网关必须成对出现，同时，并行网关的连线不能有 用户任务，或者业务流
        Map<String, Object> properties = flowElement.getProperties();
        Object forkJoinPropObj = properties.get(ChatFlowConstant.PropKey.Fork_JOIN);
        if (forkJoinPropObj instanceof Map) {
            Map<String, String> forkJoinProp = (Map<String, String>) forkJoinPropObj;
            if (StringUtils.isBlank(forkJoinProp.get("fork"))){
                throwElementValidatorException(flowElement, ErrorEnum.FORK_NOT_MATCH);
            }
            if (StringUtils.isBlank(forkJoinProp.get("join"))){
                throwElementValidatorException(flowElement, ErrorEnum.JOIN_NOT_MATCH);
            }
            return;
        }
        throwElementValidatorException(flowElement, ErrorEnum.FORK_JOIN_INVALID);
    }
}
