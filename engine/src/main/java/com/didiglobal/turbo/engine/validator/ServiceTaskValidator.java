package com.didiglobal.turbo.engine.validator;

import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.exception.DefinitionException;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.param.CommonParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ServiceTaskValidator extends ElementValidator {

    @Autowired(required = false)
    private NodeValidatorInterface nodeValidator;

    @Override
    protected void validate(Map<String, FlowElement> flowElementMap, FlowElement flowElement, CommonParam commonParam) throws DefinitionException {
        checkIncoming(flowElementMap, flowElement);
        checkOutgoing(flowElementMap, flowElement);
        checkProperties(flowElementMap, flowElement);
    }

    @Override
    protected void check(Map<String, FlowElement> flowElementMap,FlowElement flowElement) throws DefinitionException {
        checkIncoming(flowElementMap, flowElement);
        checkOutgoing(flowElementMap, flowElement);
        checkProperties(flowElementMap, flowElement);
    }

    @Override
    protected void checkIncoming(Map<String, FlowElement> flowElementMap, FlowElement flowElement) throws DefinitionException {
        super.checkIncoming(flowElementMap, flowElement);
    }

    @Override
    protected void checkOutgoing(Map<String, FlowElement> flowElementMap, FlowElement flowElement) throws DefinitionException {
        super.checkOutgoing(flowElementMap, flowElement);
        List<String> outgoingList = flowElement.getOutgoing();

        if (outgoingList.size() != 1) {
            throwElementValidatorException(flowElement, ErrorEnum.ELEMENT_TOO_MUCH_OUTGOING);
        }
    }

    protected void checkProperties(Map<String, FlowElement> flowElementMap, FlowElement flowElement) throws DefinitionException {
        nodeValidator.inputParamConfigCheck(flowElementMap, flowElement);
    }
}
