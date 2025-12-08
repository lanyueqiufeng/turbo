package com.didiglobal.turbo.plugin.spi;

import com.didiglobal.turbo.engine.common.RuntimeContext;

/**
 * @version 1.0
 * @Author yzw
 * @Date 2025/12/4 14:32
 * @注释
 */
public interface ParallelGatewayElementService {

    void invoke(RuntimeContext runtimeContext,String nodeType,int outgoingSize,String nextNodeName);
}
