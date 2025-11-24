package com.didiglobal.turbo.engine.validator;

import com.didiglobal.turbo.engine.exception.DefinitionException;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.param.CommonParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @version 1.0
 * @Author yzw
 * @Date 2025/11/19 17:17
 * @注释   并行网关checklist
 */
@Component
public class ParallelPluginValidator extends ElementValidator {



    @Override
    protected void validate(Map<String, FlowElement> flowElementMap, FlowElement flowElement, CommonParam commonParam) throws DefinitionException {
        //并行网关入线 和出线，以及连线里面不能有暂停节点
        checkIncoming(flowElementMap, flowElement);
        checkOutgoing(flowElementMap, flowElement);
        //
        checkProperties(flowElementMap, flowElement);
    }


    protected void checkProperties(Map<String, FlowElement> flowElementMap, FlowElement flowElement) throws DefinitionException {
        //在这个地方可以去checklist 并行网关必须成对出现，同时，并行网关的连线不能有 用户任务，或者业务流






    }





















 }
