package com.didiglobal.turbo.engine.validator;

import com.didiglobal.turbo.engine.model.FlowElement;

import java.util.Map;

/**
 * @author Ding.Jinzhou
 * @date 2024/12/7 14:13
 */
public interface NodeValidatorInterface {

    void inputParamConfigCheck(Map<String, FlowElement> flowElementMap, FlowElement flowElement);

    void subFlowInputParamConfigCheck(Map<String, FlowElement> flowElementMap, FlowElement flowElement);
}
