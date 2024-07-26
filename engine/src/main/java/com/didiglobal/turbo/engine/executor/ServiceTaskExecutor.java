package com.didiglobal.turbo.engine.executor;

import com.alibaba.fastjson.JSON;
import com.didiglobal.turbo.engine.bo.NodeInstanceBO;
import com.didiglobal.turbo.engine.common.InstanceDataType;
import com.didiglobal.turbo.engine.common.NodeInstanceStatus;
import com.didiglobal.turbo.engine.common.RuntimeContext;
import com.didiglobal.turbo.engine.entity.InstanceDataPO;
import com.didiglobal.turbo.engine.exception.ProcessException;
import com.didiglobal.turbo.engine.model.FlowElement;
import com.didiglobal.turbo.engine.model.InstanceData;
import com.didiglobal.turbo.engine.spi.ReplyNodeHookService;
import com.didiglobal.turbo.engine.util.InstanceDataUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

@Service
public class ServiceTaskExecutor extends ElementExecutor implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskExecutor.class);

    @Resource
    private ApplicationContext applicationContext;

    private ReplyNodeHookService replyNodeHookService;

    /**
     * Update data map: invoke hook service to update data map
     * You can implement HookService and all implementations of 'HookService' will be executed.
     * Param: one of flowElement's properties
     */
    @Override
    protected void doExecute(RuntimeContext runtimeContext) throws ProcessException {
        // 1.对用户回复环节进行处理
        Map<String, InstanceData> hookInfoValueMap = getHookInfoValueMap(runtimeContext);
        LOGGER.info("用户回复环节处理完成，处理结果 {}", hookInfoValueMap);

        // 2.merge data to current dataMap
        Map<String, InstanceData> dataMap = runtimeContext.getInstanceDataMap();
        dataMap.putAll(hookInfoValueMap);

        // 3.save data
        if (MapUtils.isNotEmpty(dataMap)) {
            String instanceDataId = saveInstanceDataPO(runtimeContext);
            runtimeContext.setInstanceDataId(instanceDataId);
        }
    }

    private Map<String, InstanceData> getHookInfoValueMap(RuntimeContext runtimeContext) {
        // List<InstanceData> list = replyNodeHookService.invoke(runtimeContext);
        FlowElement flowElement = runtimeContext.getCurrentNodeModel();
        LOGGER.info("SERVICE_TASK环节模拟执行，执行配置如下：{}", JSON.toJSONString(flowElement.getProperties()));
        return InstanceDataUtil.getInstanceDataMap(new ArrayList<>());
    }

    private String saveInstanceDataPO(RuntimeContext runtimeContext) {
        String instanceDataId = genId();
        InstanceDataPO instanceDataPO = buildHookInstanceData(instanceDataId, runtimeContext);
        instanceDataDAO.insert(instanceDataPO);
        return instanceDataId;
    }

    private InstanceDataPO buildHookInstanceData(String instanceDataId, RuntimeContext runtimeContext) {
        InstanceDataPO instanceDataPO = new InstanceDataPO();
        BeanUtils.copyProperties(runtimeContext, instanceDataPO);
        instanceDataPO.setInstanceDataId(instanceDataId);
        instanceDataPO.setInstanceData(InstanceDataUtil.getInstanceDataListStr(runtimeContext.getInstanceDataMap()));
        instanceDataPO.setNodeInstanceId(runtimeContext.getCurrentNodeInstance().getNodeInstanceId());
        instanceDataPO.setNodeKey(runtimeContext.getCurrentNodeModel().getKey());
        instanceDataPO.setType(InstanceDataType.HOOK);
        instanceDataPO.setCreateTime(new Date());
        return instanceDataPO;
    }

    @Override
    protected void postExecute(RuntimeContext runtimeContext) throws ProcessException {
        NodeInstanceBO currentNodeInstance = runtimeContext.getCurrentNodeInstance();
        currentNodeInstance.setInstanceDataId(runtimeContext.getInstanceDataId());
        currentNodeInstance.setStatus(NodeInstanceStatus.COMPLETED);
        runtimeContext.getNodeInstanceList().add(currentNodeInstance);
    }

    /**
     * Calculate unique outgoing
     * Expression: one of flowElement's properties
     * Input: data map
     *
     * @return
     * @throws Exception
     */
    @Override
    protected RuntimeExecutor getExecuteExecutor(RuntimeContext runtimeContext) throws ProcessException {
        Map<String, FlowElement> flowElementMap = runtimeContext.getFlowElementMap();
        FlowElement currentFlowElement = runtimeContext.getCurrentNodeModel();

        FlowElement nextNode;
        if (currentFlowElement.getOutgoing().size() == 1) {
            //case1. unique outgoing
            nextNode = getUniqueNextNode(currentFlowElement, flowElementMap);
        } else {
            //case2. multiple outgoings and calculate the next node with instanceDataMap
            nextNode = calculateNextNode(currentFlowElement, flowElementMap, runtimeContext.getInstanceDataMap());
        }
        LOGGER.info("getExecuteExecutor.||nextNode={}||runtimeContext={}", nextNode, runtimeContext);
        runtimeContext.setCurrentNodeModel(nextNode);
        return executorFactory.getElementExecutor(nextNode);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ensureHookService();
    }

    private void ensureHookService() {
        if (replyNodeHookService != null) {
            return;
        }
        // init hook services by Spring application context
        synchronized (ServiceTaskExecutor.class) {
            if (replyNodeHookService != null) {
                return;
            }
            String[] names = applicationContext.getBeanNamesForType(ReplyNodeHookService.class);
            if (ObjectUtils.isNotEmpty(names)) {
                Object bean = applicationContext.getBean(names[0]);
                if (bean != null) {
                    replyNodeHookService = (ReplyNodeHookService) bean;
                }
            }

        }
    }
}
