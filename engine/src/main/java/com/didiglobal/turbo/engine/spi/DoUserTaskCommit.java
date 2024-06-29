package com.didiglobal.turbo.engine.spi;

import com.didiglobal.turbo.engine.common.RuntimeContext;
import com.didiglobal.turbo.engine.exception.ProcessException;

/**
 * @author Ding.Jinzhou
 * @date 2024/6/26 20:50
 */
public interface DoUserTaskCommit {

    /**
     * 用户输入环节，doCommit生命周期钩子
     *
     * @param runtimeContext 流程运行时上下文
     */
    void judgeUserTaskCommit(RuntimeContext runtimeContext) throws ProcessException;
}
