package com.didiglobal.turbo.engine.validator;

import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.exception.DefinitionException;
import com.didiglobal.turbo.engine.model.FlowElement;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SequenceFlowValidator extends ElementValidator {

    @Override
    public void checkIncoming(Map<String, FlowElement> flowElementMap,
                              FlowElement flowElement,
                              Boolean isNotFormat) throws DefinitionException {
        super.checkIncoming(flowElementMap, flowElement, isNotFormat);

        List<String> incomingList = flowElement.getIncoming();
        if (incomingList.size() > 1) {
            throwElementValidatorException(flowElement, ErrorEnum.ELEMENT_TOO_MUCH_INCOMING, isNotFormat);
        }
    }

    @Override
    public void checkOutgoing(Map<String, FlowElement> flowElementMap,
                              FlowElement flowElement,
                              Boolean isNotFormat) throws DefinitionException {
        super.checkOutgoing(flowElementMap, flowElement, isNotFormat);

        List<String> outgoingList = flowElement.getOutgoing();
        if (outgoingList.size() > 1) {
            throwElementValidatorException(flowElement, ErrorEnum.ELEMENT_TOO_MUCH_OUTGOING, isNotFormat);
        }
    }
}
