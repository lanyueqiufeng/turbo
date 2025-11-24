package com.didiglobal.turbo.plugin.executor;

import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.common.ChatFlowConstant;
import com.didiglobal.turbo.engine.common.RuntimeContext;
import com.didiglobal.turbo.engine.dao.InstanceDataDAO;
import com.didiglobal.turbo.engine.entity.InstanceDataPO;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.didiglobal.turbo.engine.util.InstanceDataUtil;
import com.didiglobal.turbo.plugin.common.MergeStrategy;
import com.google.common.collect.Maps;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class DataMergeAll extends DataMergeStrategy{


    @Resource
    protected InstanceDataDAO instanceDataDAO;
    @Override
    public InstanceDataPO merge(RuntimeContext runtimeContext, InstanceDataPO forkNodeInstanceData, InstanceDataPO joinNodeInstanceData) {
        // 正在并行网关上下文
        Map<String, InstanceData> instanceDataMap = runtimeContext.getInstanceDataMap();
        if (instanceDataMap == null) {
            instanceDataMap = Maps.newHashMap();
        }
        InstanceData instanceData = instanceDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP);
        JSONObject instanceDataValue = (JSONObject) instanceData.getValue();
        //取出$agent
        JSONObject agentMap =(JSONObject)instanceDataValue.get(ChatFlowConstant.InstanceKey.AGENT_MAP);
        // 汇聚前 数据  forkNodeInstanceData
        Map<String, InstanceData> forkInstanceDataMap = InstanceDataUtil.getInstanceDataMap(getForkData(forkNodeInstanceData));
        InstanceData forkInstanceData = forkInstanceDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP);
        // 当前节点
        Map<String, InstanceData> joinInstanceDataMap = InstanceDataUtil.getInstanceDataMap(joinNodeInstanceData.getInstanceData());
        InstanceData joinInstanceData = joinInstanceDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP);
        JSONObject joinInstanceValue = (JSONObject) joinInstanceData.getValue();
        //取出$agent
        JSONObject joinInstanceagentMap =(JSONObject)joinInstanceValue.get(ChatFlowConstant.InstanceKey.AGENT_MAP);
        JSONObject flowMapValue = (JSONObject) forkInstanceData.getValue();
        //取出$agent
        JSONObject flowMapagentMap=(JSONObject)flowMapValue.get(ChatFlowConstant.InstanceKey.AGENT_MAP);
        // 合并节点信息
        flowMapValue.putAll(joinInstanceValue);
        flowMapValue.putAll(instanceDataValue);
        // 合并$agent
        agentMap.putAll(joinInstanceagentMap);
        agentMap.putAll(flowMapagentMap);

        runtimeContext.setInstanceDataMap(forkInstanceDataMap);
        String dataListStr = InstanceDataUtil.getInstanceDataListStr(forkInstanceDataMap);
        joinNodeInstanceData.setInstanceData(dataListStr);
        // 更新数据 汇聚节点
        forkNodeInstanceData.setInstanceData(dataListStr);
        instanceDataDAO.updateData(forkNodeInstanceData);
        return joinNodeInstanceData;
    }

    private String getForkData(InstanceDataPO forkNodeInstanceData) {
        return forkNodeInstanceData == null ? null : forkNodeInstanceData.getInstanceData();
    }

    @Override
    public String name() {
        return MergeStrategy.DATA_MERGE.ALL;
    }
}
