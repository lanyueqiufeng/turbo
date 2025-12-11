package com.didiglobal.turbo.engine.spi;

import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.common.RuntimeContext;

/**
 * <p> HookService </p>
 *
 * @author lijinghao
 * @version v1.0
 * @date 2023/2/16 6:59 PM
 */
public interface ServiceTaskExecuteService {

    /**
     * Invoke hook service, used to perform data refresh operations on the node.
     * Usage: Implement the interface and give the instance to Spring for management
     * @param runtimeContext 运行时上下文
     * @return new infos
     */
    JSONObject invoke(RuntimeContext runtimeContext);


    /**
     * 从redis中判断当前流程实例id是否需要中断
     *
     * @param flowInstanceId 流程实例id
     * @return 是否中断
     */
    Boolean flowInstanceTerminated(String flowInstanceId);

    /**
     * 修改流程实例状态 1：终止
     *
     * @param flowInstanceId     流程实例id
     * @param flowInstanceStatus 流程实例状态
     */
    void changeFlowInsStatus(String flowInstanceId, String flowInstanceStatus);
}
