package com.didiglobal.turbo.engine.spi;

import com.didiglobal.turbo.engine.common.RuntimeContext;

/**
 * @author Ding.Jinzhou
 * @date 2024/8/16 10:13
 */
public interface UserTaskExecuteService {

    /**
     * 环节运行时生命周期钩子
     *
     * @param runtimeContext 运行时上下文
     */
    void commitInvoke(RuntimeContext runtimeContext);

    /**
     * 走到用户环节时  挂起前的钩子  做一些日志记录
     *
     * @param runtimeContext
     */
    void executeInvoke(RuntimeContext runtimeContext);
}
