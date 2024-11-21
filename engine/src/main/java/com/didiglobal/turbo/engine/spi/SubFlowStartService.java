package com.didiglobal.turbo.engine.spi;

import com.didiglobal.turbo.engine.common.RuntimeContext;

/**
 * <p> HookService </p>
 *
 * @author ding.jinzhou
 * @version v1.0
 * @date 2023/2/16 6:59 PM
 */
public interface SubFlowStartService {

    /**
     * 环节运行时生命周期钩子
     *
     * @param runtimeContext     运行时上下文
     * @param subStartInputParam 子流程启动入参
     * @param subFlowInstanceId  拉起的子流程实例Id
     * @param exception          分支计算异常
     * @param startTime          开始时间
     */
    void invoke(RuntimeContext runtimeContext, String subStartInputParam, String subFlowInstanceId, Exception exception, long startTime);

    /**
     * 异步业务流结束时生命周期钩子
     *
     * @param parentNodeName     父流程节点名称
     * @param subStartInputParam 子流程启动入参
     * @param subRuntimeContext  子流程运行时上下文
     * @param exception          分支计算异常
     * @param startTime          开始时间
     */
    void asyncEndRecord(String parentNodeName, String subStartInputParam, RuntimeContext subRuntimeContext, Exception exception, long startTime);
}
