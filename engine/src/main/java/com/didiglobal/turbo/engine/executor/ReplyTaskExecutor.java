package com.didiglobal.turbo.engine.executor;

import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.common.ChatFlowConstant;
import com.didiglobal.turbo.engine.common.RuntimeContext;
import com.didiglobal.turbo.engine.exception.ProcessException;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.didiglobal.turbo.engine.spi.ReplyTaskExecuteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class ReplyTaskExecutor extends ServiceTaskExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplyTaskExecutor.class);
    @Resource
    @Lazy
    private ReplyTaskExecuteService replyNodeHookService;

    /**
     * Update data map: invoke hook service to update data map
     * You can implement HookService and all implementations of 'HookService' will be executed.
     * Param: one of flowElement's properties
     */
    @Override
    protected void doExecute(RuntimeContext runtimeContext) throws ProcessException {
        // 业务能力环节处理
        JSONObject nodeMap = replyNodeHookService.invoke(runtimeContext);
        // 向flowMap追加本轮nodeMap
        Map<String, InstanceData> dataMap = runtimeContext.getInstanceDataMap();
        JSONObject flowMap = (JSONObject) dataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP).getValue();
        flowMap.put(runtimeContext.getCurrentNodeInstance().getNodeKey(), nodeMap);
        // 数据持久化
        // if (MapUtils.isNotEmpty(dataMap)) {
        //     String instanceDataId = saveInstanceDataPO(runtimeContext);
        //     runtimeContext.setInstanceDataId(instanceDataId);
        // }
    }
}
