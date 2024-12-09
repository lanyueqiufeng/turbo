package com.didiglobal.turbo.engine.validator;

import com.didiglobal.turbo.engine.exception.DefinitionException;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.param.CommonParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReplyNodeValidator extends ElementValidator {

    @Autowired(required = false)
    private NodeValidatorInterface nodeValidator;

    @Override
    protected void validate(Map<String, FlowElement> flowElementMap, FlowElement flowElement, CommonParam commonParam) throws DefinitionException {
        checkIncoming(flowElementMap, flowElement);
        checkOutgoing(flowElementMap, flowElement);
        checkProperties(flowElementMap, flowElement);
    }

    protected void checkProperties(Map<String, FlowElement> flowElementMap, FlowElement flowElement) throws DefinitionException {
        nodeValidator.inputParamConfigCheck(flowElementMap, flowElement);
        nodeValidator.replyTaskConfigCheck(flowElementMap, flowElement);
    }


    @Override
    protected void check(Map<String, FlowElement> flowElementMap,FlowElement flowElement) throws DefinitionException {
        validate(flowElementMap, flowElement, null);
    }
}
