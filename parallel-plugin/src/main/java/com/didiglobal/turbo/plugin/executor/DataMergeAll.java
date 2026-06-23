package com.didiglobal.turbo.plugin.executor;

import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.common.ChatFlowConstant;
import com.didiglobal.turbo.engine.common.RuntimeContext;
import com.didiglobal.turbo.engine.dao.InstanceDataDAO;
import com.didiglobal.turbo.engine.dao.NodeInstanceDAO;
import com.didiglobal.turbo.engine.entity.InstanceDataPO;
import com.didiglobal.turbo.engine.entity.NodeInstancePO;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.didiglobal.turbo.engine.util.InstanceDataUtil;
import com.didiglobal.turbo.plugin.common.MergeStrategy;
import com.didiglobal.turbo.plugin.util.ExecutorUtil;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DataMergeAll extends DataMergeStrategy {

    @Resource
    protected InstanceDataDAO instanceDataDAO;
    @Resource
    protected NodeInstanceDAO nodeInstanceDAO;

    @Override
    public InstanceDataPO merge(RuntimeContext runtimeContext, InstanceDataPO forkNodeInstanceData, InstanceDataPO joinNodeInstanceData) {
        Map<String, InstanceData> instanceDataMap = runtimeContext.getInstanceDataMap();
        if (instanceDataMap == null) {
            instanceDataMap = Maps.newHashMap();
        }

        // 当前线程 runtimeContext 里的 flowMap，也就是当前分支刚执行完后的上下文数据。
        JSONObject runtimeFlowMap = (JSONObject) instanceDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP).getValue();

        // 本次要往里面合并的目标数据：joinFirst 时是 fork 数据；joinMerge 时是之前已经合并过的 join 数据。
        Map<String, InstanceData> mergeDataMap = InstanceDataUtil.getInstanceDataMap(getInstanceData(forkNodeInstanceData));
        JSONObject mergedFlowMap = (JSONObject) mergeDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP).getValue();

        // 当前到达 join 的分支数据，来自 joinNodeInstanceData。
        Map<String, InstanceData> branchDataMap = InstanceDataUtil.getInstanceDataMap(joinNodeInstanceData.getInstanceData());
        JSONObject branchFlowMap = (JSONObject) branchDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP).getValue();

        // 父 fork 的原始快照，用来判断当前分支到底改了哪些 key。
        JSONObject baseFlowMap = getBaseFlowMap(runtimeContext, forkNodeInstanceData);

        // 并行分支里带的是完整 flowMap 快照。
        // 只有相对 fork 原始快照发生变化的 key，才认为是当前分支真正产出的数据。
        putChangedOnly(mergedFlowMap, baseFlowMap, branchFlowMap);
        putChangedOnly(mergedFlowMap, baseFlowMap, runtimeFlowMap);

        runtimeContext.setInstanceDataMap(mergeDataMap);
        String dataListStr = InstanceDataUtil.getInstanceDataListStr(mergeDataMap);
        joinNodeInstanceData.setInstanceData(dataListStr);

        // joinFirst 第一个参数是 fork 数据，不能更新；joinMerge 第一个参数才是已合并的 join 数据。
        if (runtimeContext.getCurrentNodeInstance() != null
                && StringUtils.equals(forkNodeInstanceData.getNodeKey(), runtimeContext.getCurrentNodeInstance().getNodeKey())) {
            forkNodeInstanceData.setInstanceData(dataListStr);
            instanceDataDAO.updateData(forkNodeInstanceData);
        }
        return joinNodeInstanceData;
    }

    private void putChangedOnly(JSONObject target, JSONObject base, JSONObject branch) {
        if (branch == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : branch.entrySet()) {
            String key = entry.getKey();
            Object baseValue = base == null ? null : base.get(key);
            Object branchValue = entry.getValue();
            if (ChatFlowConstant.InstanceKey.AGENT_MAP.equals(key)
                    && target.get(key) instanceof JSONObject
                    && branchValue instanceof JSONObject) {
                putChangedOnly((JSONObject) target.get(key),
                        baseValue instanceof JSONObject ? (JSONObject) baseValue : null,
                        (JSONObject) branchValue);
            } else if (!Objects.equals(baseValue, branchValue)) {
                target.put(key, branchValue);
            }
        }
    }

    private JSONObject getBaseFlowMap(RuntimeContext runtimeContext, InstanceDataPO mergeData) {
        InstanceDataPO baseData = mergeData;
        // 如果当前合并基准已经是 join 数据，需要重新找到父 fork 原始快照做比较。
        if (runtimeContext.getCurrentNodeInstance() != null && StringUtils.equals(mergeData.getNodeKey(), runtimeContext.getCurrentNodeInstance().getNodeKey())) {
            String executeId = (String) runtimeContext.getCurrentNodeInstance().get("executeId");
            String currentExecuteId = ExecutorUtil.getCurrentExecuteId(executeId);
            Pair<String, String> forkAndJoinNodeKey = ExecutorUtil.getForkAndJoinNodeKey(runtimeContext.getCurrentNodeModel());
            List<NodeInstancePO> forkNodeList = nodeInstanceDAO.selectByFlowInstanceIdAndNodeKey(
                    runtimeContext.getFlowInstanceId(), forkAndJoinNodeKey.getLeft());
            for (NodeInstancePO forkNode : forkNodeList) {
                if (ExecutorUtil.getExecuteIdSet((String) forkNode.get("executeId")).contains(currentExecuteId)) {
                    baseData = instanceDataDAO.select(runtimeContext.getFlowInstanceId(), forkNode.getInstanceDataId());
                    break;
                }
            }
        }

        Map<String, InstanceData> baseDataMap = InstanceDataUtil.getInstanceDataMap(getInstanceData(baseData));
        InstanceData baseFlowData = baseDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP);
        return baseFlowData == null ? new JSONObject() : (JSONObject) baseFlowData.getValue();
    }

    private String getInstanceData(InstanceDataPO data) {
        return data == null ? null : data.getInstanceData();
    }

    @Override
    public String name() {
        return MergeStrategy.DATA_MERGE.ALL;
    }
}
