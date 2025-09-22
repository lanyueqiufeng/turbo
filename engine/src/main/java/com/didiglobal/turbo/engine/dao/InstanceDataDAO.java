package com.didiglobal.turbo.engine.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.didiglobal.turbo.engine.common.ChatFlowConstant;
import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.common.InstanceDataType;
import com.didiglobal.turbo.engine.dao.mapper.InstanceDataMapper;
import com.didiglobal.turbo.engine.entity.FlowInstanceDataPo;
import com.didiglobal.turbo.engine.entity.InstanceDataPO;
import com.didiglobal.turbo.engine.exception.TurboException;
import com.didiglobal.turbo.engine.model.InstanceData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.didiglobal.turbo.engine.common.ChatFlowConstant.InstanceKey.FLOW_MAP;

@Repository
public class InstanceDataDAO extends BaseDAO<InstanceDataMapper, InstanceDataPO> {

    @Resource
    protected FlowInstanceDataDAO flowInstanceDataDAO;

    public InstanceDataPO select(String flowInstanceId, String instanceDataId) {
        InstanceDataPO instanceDataPO = baseMapper.select(flowInstanceId, instanceDataId);
        if (instanceDataPO == null) {
            return null;
        }else {
            FlowInstanceDataPo flowInstanceData = flowInstanceDataDAO.getOne(new LambdaQueryWrapper<FlowInstanceDataPo>().eq(FlowInstanceDataPo::getFlowInstanceId, flowInstanceId));
            if (flowInstanceData != null) {
                String flowInstanceDataStr = flowInstanceData.getFlowInsData();
                if (StringUtils.isNotEmpty(flowInstanceDataStr)) {
                    // 如果存在流程参数池  则进行合并
                    String instanceDataStr = instanceDataPO.getInstanceData();
                    if (StringUtils.isEmpty(instanceDataStr)) {
                        // 如果是空  直接用流程参数池替换掉
                        instanceDataPO.setInstanceData(JSON.toJSONString(Collections.singletonList(flowInstanceDataStr)));
                    } else {
                        // 非空的话  追加参数池进去
                        List<InstanceData> instanceDataList = JSON.parseArray(instanceDataStr, InstanceData.class);
                        InstanceData flowMapInstanceData = JSON.parseObject(flowInstanceDataStr, InstanceData.class);
                        // 正常环节实例数据与流程实例数据是一一对应的   但是如果查出来发现不一致  说明发生流程异常重入   此时就需要用环节内保存的全局变量来还原  避免数据异常
                        if (!Objects.equals(instanceDataPO.getNodeInstanceId(), flowInstanceData.getLastNodeInsId())) {
                            // 把$agent提取出来做覆盖
                            for(InstanceData instanceData : instanceDataList) {
                                if (ChatFlowConstant.InstanceKey.AGENT_MAP.equals(instanceData.getKey())) {
                                    JSONObject flowMap = (JSONObject) flowMapInstanceData.getValue();
                                    flowMap.put(ChatFlowConstant.InstanceKey.AGENT_MAP, instanceData.getValue());
                                }
                            }
                        }
                        if (flowMapInstanceData != null) {
                            instanceDataList.add(flowMapInstanceData);
                        }
                        instanceDataPO.setInstanceData(JSON.toJSONString(instanceDataList));
                    }
                }
            }
            return instanceDataPO;
        }
    }

    /**
     * select recent InstanceData order by id desc
     *
     * @param flowInstanceId
     * @return
     */
    public InstanceDataPO selectRecentOne(String flowInstanceId) {
        return baseMapper.selectRecentOne(flowInstanceId);
    }

    /**
     * insert instanceDataPO
     *
     * @param instanceDataPO
     * @return -1 while insert failed
     */
    public int insert(InstanceDataPO instanceDataPO) {
        try {
            Object flowMap = instanceDataPO.getProperties().get(FLOW_MAP);
            if (flowMap instanceof String) {
                if (instanceDataPO.getType() == InstanceDataType.INIT) {
                    // 新增：流程级别的参数池 对应start流程实例
                    FlowInstanceDataPo flowInstanceDataPO = new FlowInstanceDataPo();
                    flowInstanceDataPO.setFlowInstanceId(instanceDataPO.getFlowInstanceId());
                    flowInstanceDataPO.setFlowInsData((String) flowMap);
                    flowInstanceDataPO.setLastNodeInsId(instanceDataPO.getNodeInstanceId());
                    flowInstanceDataPO.setFlowDeployId(instanceDataPO.getFlowDeployId());
                    flowInstanceDataPO.setFlowModuleId(instanceDataPO.getFlowModuleId());
                    flowInstanceDataPO.setUpdateDate(new Date());
                    flowInstanceDataPO.setCaller(instanceDataPO.getCaller());
                    flowInstanceDataPO.setTenant(instanceDataPO.getTenant());
                    flowInstanceDataDAO.save(flowInstanceDataPO);
                } else {
                    // 新增：更新流程级别的参数池
                    flowInstanceDataDAO.update(new LambdaUpdateWrapper<FlowInstanceDataPo>()
                            .eq(FlowInstanceDataPo::getFlowInstanceId, instanceDataPO.getFlowInstanceId())
                            .set(FlowInstanceDataPo::getFlowInsData, flowMap)
                            .set(FlowInstanceDataPo::getLastNodeInsId, instanceDataPO.getNodeInstanceId()));
                }
            }
            return baseMapper.insert(instanceDataPO);
        } catch (Exception e) {
            // TODO: 2020/2/1 clear reentrant exception log 
            LOGGER.error("insert exception.||instanceDataPO={}", instanceDataPO, e);
        }
        return -1;
    }

    /**
     * update instanceData
     * @param instanceDataPO
     * @return
     */
    public int updateData(InstanceDataPO instanceDataPO) {
        try {
            return baseMapper.updateData(instanceDataPO);
        } catch (Exception e) {
            LOGGER.error("update instance data exception.||instanceDataPO={}", instanceDataPO, e);
            throw new TurboException(ErrorEnum.UPDATE_INSTANCE_DATA_FAILED);
        }
    }

    /**
     * insert or update instanceData
     * @param mergePo
     * @return
     */
    public int insertOrUpdate(InstanceDataPO mergePo) {
        if (mergePo.getId() != null) {
            return updateData(mergePo);
        }
        return insert(mergePo);
    }
}
