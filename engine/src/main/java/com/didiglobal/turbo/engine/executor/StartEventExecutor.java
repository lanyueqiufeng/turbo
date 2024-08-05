package com.didiglobal.turbo.engine.executor;

import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.bo.NodeInstanceBO;
import com.didiglobal.turbo.engine.common.ChatFlowConstant;
import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.common.NodeInstanceStatus;
import com.didiglobal.turbo.engine.common.RuntimeContext;
import com.didiglobal.turbo.engine.exception.ProcessException;
import com.didiglobal.turbo.engine.model.InstanceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class StartEventExecutor extends ElementExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartEventExecutor.class);

    @Override
    protected void doExecute(RuntimeContext runtimeContext) throws ProcessException {
        // 构造开始环节输出  nodeMap
        String nodeKey = runtimeContext.getCurrentNodeInstance().getNodeKey();
        // 获取开始环节输入
        Map<String, InstanceData> instanceDataMap = runtimeContext.getInstanceDataMap();
        Object nodeMap = instanceDataMap.get(ChatFlowConstant.InstanceKey.START_OUTPUT).getValue();
        // 放入flowMap
        JSONObject flowMap = (JSONObject) instanceDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP).getValue();
        flowMap.put(nodeKey, nodeMap);
    }

    @Override
    protected void postExecute(RuntimeContext runtimeContext) throws ProcessException {
        NodeInstanceBO currentNodeInstance = runtimeContext.getCurrentNodeInstance();
        currentNodeInstance.setInstanceDataId(runtimeContext.getInstanceDataId());
        currentNodeInstance.setStatus(NodeInstanceStatus.COMPLETED);
        runtimeContext.getNodeInstanceList().add(currentNodeInstance);
    }

    @Override
    protected void preRollback(RuntimeContext runtimeContext) throws ProcessException {
        // when subFlowInstance, the StartEvent rollback is allowed
        if (isSubFlowInstance(runtimeContext)) {
            super.preRollback(runtimeContext);
            return;
        }
        runtimeContext.setCurrentNodeInstance(runtimeContext.getSuspendNodeInstance());
        runtimeContext.setNodeInstanceList(Collections.emptyList());

        LOGGER.warn("postRollback: reset runtimeContext.||flowInstanceId={}||nodeKey={}||nodeType={}",
            runtimeContext.getFlowInstanceId(), runtimeContext.getCurrentNodeModel().getKey(), runtimeContext.getCurrentNodeModel().getType());
        throw new ProcessException(ErrorEnum.NO_USER_TASK_TO_ROLLBACK, "It's a startEvent.");
    }

    @Override
    protected void postRollback(RuntimeContext runtimeContext) throws ProcessException {
        // when subFlowInstance, the StartEvent rollback is allowed
        if (isSubFlowInstance(runtimeContext)) {
            super.postRollback(runtimeContext);
        }
    }
}
