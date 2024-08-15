package com.didiglobal.turbo.engine.executor;

import com.didiglobal.turbo.engine.bo.NodeInstanceBO;
import com.didiglobal.turbo.engine.common.*;
import com.didiglobal.turbo.engine.entity.InstanceDataPO;
import com.didiglobal.turbo.engine.exception.ProcessException;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.spi.EndEventExecuteService;
import com.didiglobal.turbo.engine.util.FlowModelUtil;
import com.didiglobal.turbo.engine.util.InstanceDataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Date;

@Service
public class EndEventExecutor extends ElementExecutor {

    @Autowired(required = false)
    private EndEventExecuteService endEventExecuteService;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndEventExecutor.class);


    @Override
    protected void doExecute(RuntimeContext runtimeContext) throws ProcessException {
        if (endEventExecuteService != null) {
            endEventExecuteService.invoke(runtimeContext);
        }
        saveInstanceDataPO(runtimeContext);
    }


    @Override
    protected void postExecute(RuntimeContext runtimeContext) throws ProcessException {
        NodeInstanceBO currentNodeInstance = runtimeContext.getCurrentNodeInstance();
        currentNodeInstance.setInstanceDataId(runtimeContext.getInstanceDataId());
        currentNodeInstance.setStatus(NodeInstanceStatus.COMPLETED);
        runtimeContext.getNodeInstanceList().add(currentNodeInstance);
    }

    @Override
    protected void doRollback(RuntimeContext runtimeContext) throws ProcessException {
        // when subFlowInstance, the EndEvent rollback is allowed
        if (isSubFlowInstance(runtimeContext)) {
            return;
        }
        FlowElement flowElement = runtimeContext.getCurrentNodeModel();
        String nodeName = FlowModelUtil.getElementName(flowElement);
        LOGGER.warn("doRollback: unsupported element type as EndEvent.||flowInstanceId={}||nodeKey={}||nodeName={}||nodeType={}",
            runtimeContext.getFlowInstanceId(), flowElement.getKey(), nodeName, flowElement.getType());
        throw new ProcessException(ErrorEnum.UNSUPPORTED_ELEMENT_TYPE,
            MessageFormat.format(Constants.NODE_INFO_FORMAT, flowElement.getKey(), nodeName, flowElement.getType()));
    }

    @Override
    protected void postRollback(RuntimeContext runtimeContext) throws ProcessException {
        // when subFlowInstance, the EndEvent rollback is allowed
        if (isSubFlowInstance(runtimeContext)) {
            super.postRollback(runtimeContext);
            return;
        }
        //do nothing
    }

    @Override
    protected RuntimeExecutor getExecuteExecutor(RuntimeContext runtimeContext) throws ProcessException {
        LOGGER.info("getExecuteExecutor: no executor after EndEvent.");
        return null;
    }

    private String saveInstanceDataPO(RuntimeContext runtimeContext) {
        String instanceDataId = genId();
        InstanceDataPO instanceDataPO = buildExecuteInstanceData(instanceDataId, runtimeContext);
        instanceDataDAO.insert(instanceDataPO);
        return instanceDataId;
    }

    private InstanceDataPO buildExecuteInstanceData(String instanceDataId, RuntimeContext runtimeContext) {
        InstanceDataPO instanceDataPO = new InstanceDataPO();
        BeanUtils.copyProperties(runtimeContext, instanceDataPO);
        instanceDataPO.setInstanceDataId(instanceDataId);
        instanceDataPO.setInstanceData(InstanceDataUtil.getInstanceDataListStr(runtimeContext.getInstanceDataMap()));
        instanceDataPO.setNodeInstanceId(runtimeContext.getCurrentNodeInstance().getNodeInstanceId());
        instanceDataPO.setNodeKey(runtimeContext.getCurrentNodeModel().getKey());
        instanceDataPO.setType(InstanceDataType.EXECUTE);
        instanceDataPO.setCreateTime(new Date());
        return instanceDataPO;
    }
}
