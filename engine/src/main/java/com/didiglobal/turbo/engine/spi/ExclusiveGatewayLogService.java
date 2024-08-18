package com.didiglobal.turbo.engine.spi;

import com.didiglobal.turbo.engine.common.RuntimeContext;

/**
 * <p> HookService </p>
 *
 * @author lijinghao
 * @version v1.0
 * @date 2023/2/16 6:59 PM
 */
public interface ExclusiveGatewayLogService {

    /**
     * 环节运行时生命周期钩子
     *
     * @param runtimeContext 运行时上下文
     */
    void invoke(RuntimeContext runtimeContext);
}
