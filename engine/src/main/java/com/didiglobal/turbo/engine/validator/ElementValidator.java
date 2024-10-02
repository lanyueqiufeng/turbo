package com.didiglobal.turbo.engine.validator;

import com.didiglobal.turbo.engine.common.Constants;
import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.exception.DefinitionException;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.param.CommonParam;
import com.didiglobal.turbo.engine.util.FlowModelUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

public class ElementValidator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ElementValidator.class);

    protected void checkIncoming(Map<String, FlowElement> flowElementMap,
                                 FlowElement flowElement,
                                 Boolean isNotFormat) throws DefinitionException {
        List<String> incomingList = flowElement.getIncoming();

        if (CollectionUtils.isEmpty(incomingList)) {
            throwElementValidatorException(flowElement, ErrorEnum.ELEMENT_LACK_INCOMING, isNotFormat);
        }
    }

    protected void checkOutgoing(Map<String, FlowElement> flowElementMap,
                                 FlowElement flowElement, Boolean isNotFormat) throws DefinitionException {
        List<String> outgoingList = flowElement.getOutgoing();

        if (CollectionUtils.isEmpty(outgoingList)) {
            throwElementValidatorException(flowElement, ErrorEnum.ELEMENT_LACK_OUTGOING, isNotFormat);
        }
    }

    protected void validate(Map<String, FlowElement> flowElementMap,
                            FlowElement flowElement,
                            CommonParam commonParam) throws DefinitionException {
        checkIncoming(flowElementMap, flowElement, null);
        checkOutgoing(flowElementMap, flowElement, null);
    }

    protected void throwElementValidatorException(FlowElement flowElement,
                                                  ErrorEnum errorEnum,
                                                  Boolean isNotFormat) throws DefinitionException {
        if (null == isNotFormat || !isNotFormat) {
            String exceptionMsg = getElementValidatorExceptionMsg(flowElement, errorEnum);
            LOGGER.warn(exceptionMsg);
            throw new DefinitionException(errorEnum.getErrNo(), exceptionMsg);
        }
        throw new DefinitionException(errorEnum);
    }

    protected void recordElementValidatorException(FlowElement flowElement, ErrorEnum errorEnum, Boolean isNotFormat) {
        if (null == isNotFormat || !isNotFormat) {
            String exceptionMsg = getElementValidatorExceptionMsg(flowElement, errorEnum);
            LOGGER.warn(exceptionMsg);
        }else {
            String exceptionMsg =  errorEnum.getErrMsg();
            LOGGER.warn(exceptionMsg);
        }
    }

    private String getElementValidatorExceptionMsg(FlowElement flowElement, ErrorEnum errorEnum) {
        String elementName = FlowModelUtil.getElementName(flowElement);
        String elementKey = flowElement.getKey();
        return MessageFormat.format(Constants.MODEL_DEFINITION_ERROR_MSG_FORMAT, errorEnum.getErrMsg(), elementName,
                elementKey);
    }

    protected void check(Map<String, FlowElement> flowElementMap, FlowElement flowElement) throws DefinitionException {
        checkIncoming(flowElementMap, flowElement, Boolean.TRUE);
        checkOutgoing(flowElementMap, flowElement, Boolean.TRUE);
    }
}
