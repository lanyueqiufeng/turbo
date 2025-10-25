package com.didiglobal.turbo.engine.executor.callactivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.bo.NodeInstance;
import com.didiglobal.turbo.engine.bo.NodeInstanceBO;
import com.didiglobal.turbo.engine.common.*;
import com.didiglobal.turbo.engine.entity.*;
import com.didiglobal.turbo.engine.exception.ProcessException;
import com.didiglobal.turbo.engine.exception.SuspendException;
import com.didiglobal.turbo.engine.executor.ServiceTaskExecutor;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.didiglobal.turbo.engine.param.StartProcessParam;
import com.didiglobal.turbo.engine.result.RuntimeResult;
import com.didiglobal.turbo.engine.result.StartProcessResult;
import com.didiglobal.turbo.engine.spi.SubFlowStartService;
import com.didiglobal.turbo.engine.util.FlowModelUtil;
import com.didiglobal.turbo.engine.util.InstanceDataUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 异步单实例子流程
 *
 * @author Ding.Jinzhou
 * @date 2024/7/23 10:42
 */
@Service
public class AsyncSingleCallActivityExecutor extends SyncSingleCallActivityExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncSingleCallActivityExecutor.class);

    @Resource
    private ServiceTaskExecutor serviceTaskExecutor;

    @Autowired(required = false)
    private SubFlowStartService subFlowStartService;


    @Override
    protected void doCommit(RuntimeContext runtimeContext) throws ProcessException {
        boolean commitCallActivityNode = CollectionUtils.isEmpty(runtimeContext.getSuspendNodeInstanceStack());
        if (commitCallActivityNode) {
            startProcessCallActivity(runtimeContext);
        } else {
            // 异步业务流  一般不认为可以再次提交
            throw new ProcessException(ErrorEnum.COMMIT_FAILED, "异步子流程不可重复提交");
        }
        // String instanceDataId = serviceTaskExecutor.saveInstanceDataPO(runtimeContext);
        // runtimeContext.setInstanceDataId(instanceDataId);
    }

    @Override
    protected void startProcessCallActivity(RuntimeContext runtimeContext) throws ProcessException {
        // NodeInstanceBO currentNodeInstance = runtimeContext.getCurrentNodeInstance();
        // 1.check reentrant execute
        // FlowInstanceMappingPO flowInstanceMappingPO = flowInstanceMappingDAO.selectFlowInstanceMappingPO(runtimeContext.getFlowInstanceId(), currentNodeInstance.getNodeInstanceId());
        // if (flowInstanceMappingPO != null) {
        //     handleReentrantSubFlowInstance(runtimeContext, flowInstanceMappingPO);
        //     return;
        // }
        // 2.check CallActivity nested level
        preCheckCallActivityNestedLevel(runtimeContext);

        // 3.get flowModuleId
        String callActivityFlowModuleId = runtimeContext.getCallActivityFlowModuleId();
        String callActivityFlowDeployId = runtimeContext.getCallActivityFlowDeployId();
        // avoid misuse
        runtimeContext.setCallActivityFlowModuleId(null);
        runtimeContext.setCallActivityFlowDeployId(null);
        // 4.calculate variables
        List<InstanceData> callActivityVariables = getCallActivityVariables(runtimeContext);
        String subStartInputParam = JSON.toJSONString(InstanceDataUtil.changeInstanceDataToMap(callActivityVariables));
        StartProcessParam startProcessParam = new StartProcessParam();
        startProcessParam.setRuntimeContext(runtimeContext);
        startProcessParam.setFlowModuleId(callActivityFlowModuleId);
        startProcessParam.setFlowDeployId(callActivityFlowDeployId);
        startProcessParam.setVariables(callActivityVariables);
        StartProcessResult startProcessResult;
        long startTime = System.currentTimeMillis();
        String subFlowInstanceId = null;
        Exception exception = null;
        try {
            startProcessResult = runtimeProcessor.startProcessAsync(startProcessParam);
            subFlowInstanceId = startProcessResult.getFlowInstanceId();
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            subFlowStartService.invoke(runtimeContext, subStartInputParam, subFlowInstanceId, exception, startTime);
        }
        LOGGER.info("子流程启动 ||启动入参={}||执行结果={}", startProcessParam, startProcessResult);
        // 5.save flowInstance mapping
        saveFlowInstanceMapping(runtimeContext, startProcessResult.getFlowInstanceId());
        handleCallActivityResult(runtimeContext, startProcessResult);
    }

    private void preCheckCallActivityNestedLevel(RuntimeContext runtimeContext) throws ProcessException {
        int maxCallActivityNestedLevel = businessConfig.getCallActivityNestedLevel(runtimeContext.getCaller());
        int currentCallActivityNestedLevel = 0;
        RuntimeContext tmpRuntimeContext = runtimeContext;
        while (tmpRuntimeContext != null) {
            currentCallActivityNestedLevel++;
            tmpRuntimeContext = tmpRuntimeContext.getParentRuntimeContext();
        }
        if (maxCallActivityNestedLevel < currentCallActivityNestedLevel) {
            throw new ProcessException(ErrorEnum.FLOW_NESTED_LEVEL_EXCEEDED);
        }
    }

    private void saveFlowInstanceMapping(RuntimeContext runtimeContext, String subFlowInstanceId) {
        FlowInstanceMappingPO flowInstanceMappingPO = new FlowInstanceMappingPO();
        flowInstanceMappingPO.setFlowInstanceId(runtimeContext.getFlowInstanceId());
        NodeInstanceBO currentNodeInstance = runtimeContext.getCurrentNodeInstance();
        flowInstanceMappingPO.setNodeKey(currentNodeInstance.getNodeKey());
        flowInstanceMappingPO.setNodeInstanceId(currentNodeInstance.getNodeInstanceId());
        flowInstanceMappingPO.setSubFlowInstanceId(subFlowInstanceId);
        flowInstanceMappingPO.setType(FlowInstanceMappingType.EXECUTE);
        flowInstanceMappingPO.setTenant(runtimeContext.getTenant());
        flowInstanceMappingPO.setCaller(runtimeContext.getCaller());
        flowInstanceMappingPO.setCreateTime(new Date());
        flowInstanceMappingPO.setModifyTime(new Date());
        flowInstanceMappingDAO.save(flowInstanceMappingPO);
    }

    private void handleReentrantSubFlowInstance(RuntimeContext runtimeContext, FlowInstanceMappingPO flowInstanceMappingPO) throws ProcessException {
        String subFlowInstanceId = flowInstanceMappingPO.getSubFlowInstanceId();
        RuntimeResult subFlowInstanceFirstUserTask = getSubFlowInstanceFirstUserTask(subFlowInstanceId);
        if (subFlowInstanceFirstUserTask != null) {
            runtimeContext.setCallActivityRuntimeResultList(Arrays.asList(subFlowInstanceFirstUserTask));
            throw new SuspendException(ErrorEnum.COMMIT_SUSPEND);
        }
        LOGGER.info("callActivity did not find userTask.||subFlowInstanceId={}", subFlowInstanceId);
    }

    private RuntimeResult getSubFlowInstanceFirstUserTask(String subFlowInstanceId) {
        FlowInstancePO subFlowInstancePO = processInstanceDAO.selectByFlowInstanceId(subFlowInstanceId);
        FlowDeploymentPO subFlowDeploymentPO = flowDeploymentDAO.selectByDeployId(subFlowInstancePO.getFlowDeployId());
        Map<String, FlowElement> subFlowElementMap = FlowModelUtil.getFlowElementMap(subFlowDeploymentPO.getFlowModel());

        List<NodeInstancePO> nodeInstancePOList = nodeInstanceDAO.selectByFlowInstanceId(subFlowInstanceId);
        for (NodeInstancePO nodeInstancePO : nodeInstancePOList) {
            int elementType = FlowModelUtil.getElementType(nodeInstancePO.getNodeKey(), subFlowElementMap);
            if (elementType == FlowElementType.USER_TASK) {
                return buildCallActivityFirstUserTaskRuntimeResult(subFlowInstancePO, subFlowElementMap, nodeInstancePO);
            } else if (elementType == FlowElementType.CALL_ACTIVITY) {
                FlowInstanceMappingPO flowInstanceMappingPO = flowInstanceMappingDAO.selectFlowInstanceMappingPO(subFlowInstanceId, nodeInstancePO.getNodeInstanceId());
                if (flowInstanceMappingPO == null) {
                    LOGGER.warn("callActivity did not find instanceMapping.||subFlowInstanceId={}", subFlowInstanceId);
                    break;
                }
                RuntimeResult runtimeResult = getSubFlowInstanceFirstUserTask(flowInstanceMappingPO.getSubFlowInstanceId());
                if (runtimeResult != null) {
                    return runtimeResult;
                }
            }
        }
        return null;
    }

    private RuntimeResult buildCallActivityFirstUserTaskRuntimeResult(FlowInstancePO subFlowInstancePO, Map<String, FlowElement> subFlowElementMap, NodeInstancePO nodeInstancePO) {
        RuntimeResult runtimeResult = new RuntimeResult();
        runtimeResult.setErrCode(ErrorEnum.COMMIT_SUSPEND.getErrNo());
        runtimeResult.setErrMsg(ErrorEnum.COMMIT_SUSPEND.getErrMsg());
        runtimeResult.setFlowInstanceId(subFlowInstancePO.getFlowInstanceId());
        runtimeResult.setStatus(subFlowInstancePO.getStatus());

        NodeInstance nodeInstance = new NodeInstance();
        BeanUtils.copyProperties(nodeInstancePO, nodeInstance);
        nodeInstance.setCreateTime(null);
        nodeInstance.setModifyTime(null);
        nodeInstance.setModelKey(nodeInstancePO.getNodeKey());
        FlowElement flowElement = subFlowElementMap.get(nodeInstancePO.getNodeKey());
        nodeInstance.setModelName(FlowModelUtil.getElementName(flowElement));
        nodeInstance.setProperties(flowElement.getProperties());

        runtimeResult.setActiveTaskInstance(nodeInstance);
        InstanceDataPO instanceDataPO = instanceDataDAO.select(subFlowInstancePO.getFlowInstanceId(), nodeInstancePO.getInstanceDataId());
        Map<String, InstanceData> instanceDataMap = InstanceDataUtil.getInstanceDataMap(instanceDataPO.getInstanceData());
        runtimeResult.setVariables(InstanceDataUtil.getInstanceDataList(instanceDataMap));
        return runtimeResult;
    }

    /**
     * common handle RuntimeResult from startProcessCallActivity, commitCallActivity, rollbackCallActivity.
     *
     * @param runtimeContext
     * @param runtimeResult
     * @throws ProcessException
     */
    protected void handleCallActivityResult(RuntimeContext runtimeContext, RuntimeResult runtimeResult) throws ProcessException {
        handleSyncSubFlowResult(runtimeContext, runtimeResult);
    }

    private void handleSyncSubFlowResult(RuntimeContext runtimeContext, RuntimeResult runtimeResult) throws ProcessException {
        NodeInstanceBO currentNodeInstance = runtimeContext.getCurrentNodeInstance();
        currentNodeInstance.setStatus(NodeInstanceStatus.COMPLETED);
        saveAsyncCallActivityEndInstanceData(runtimeContext, runtimeResult);
    }

    private void saveAsyncCallActivityEndInstanceData(RuntimeContext runtimeContext, RuntimeResult runtimeResult) throws ProcessException {
        NodeInstanceBO currentNodeInstance = runtimeContext.getCurrentNodeInstance();
        // 根据父子传递规则 计算后的子流程应该传给父流程的参数
        List<InstanceData> instanceDataFromSubFlow = calculateCallActivityOutParamFromSubFlow(runtimeContext, runtimeResult.getVariables());
        // 1.merge to current data
        Map<String, InstanceData> mainInstanceDataMap = InstanceDataUtil.getInstanceDataMap(instanceDataDAO.select(runtimeContext.getFlowInstanceId(), runtimeContext.getInstanceDataId()).getInstanceData());
        mainInstanceDataMap.putAll(InstanceDataUtil.getInstanceDataMap(instanceDataFromSubFlow));
        // 注入原始父流程参数
        runtimeContext.setInstanceDataMap(mainInstanceDataMap);
        // 主流程流程flowMap
        JSONObject flowMap = (JSONObject) mainInstanceDataMap.get(ChatFlowConstant.InstanceKey.FLOW_MAP).getValue();
        // update: 将子流程end环节出参  绑定给对应子流程环节的出参
        Map<String, Object> subFlowInstanceData = InstanceDataUtil.changeInstanceDataToMap(runtimeResult.getVariables());
        // 注入异步子流程的流程实例id
        String nodeKey = runtimeContext.getCurrentNodeInstance().getNodeKey();
        JSONObject syncSubData = new JSONObject().fluentPut("subFlowInstanceId", runtimeResult.getFlowInstanceId());
        // 放入父流程对应环节上
        flowMap.put(nodeKey, syncSubData);
        // 2.save data
        String instanceDataId = genId();
        InstanceDataPO instanceDataPO = buildCallActivityEndInstanceData(instanceDataId, runtimeContext);
        instanceDataDAO.insert(instanceDataPO);
        runtimeContext.setInstanceDataId(instanceDataId);
        // 3.set currentNode completed
        currentNodeInstance.setInstanceDataId(runtimeContext.getInstanceDataId());
    }
}
