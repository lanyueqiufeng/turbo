package com.didiglobal.turbo.engine.spi;

import com.didiglobal.turbo.engine.common.RuntimeContext;
import com.didiglobal.turbo.engine.model.InstanceData;

import java.util.List;

/**
 * <p> HookService </p>
 *
 * @author lijinghao
 * @version v1.0
 * @date 2023/2/16 6:59 PM
 */
public interface ReplyNodeHookService {

    /**
     * Invoke hook service, used to perform data refresh operations on the gateway node.
     * Usage: Implement the interface and give the instance to Spring for management
     * @param runtimeContext 运行时上下文
     * @return new infos
     */
    List<InstanceData> invoke(RuntimeContext runtimeContext);
}
