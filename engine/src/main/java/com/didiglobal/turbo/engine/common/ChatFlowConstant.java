package com.didiglobal.turbo.engine.common;

/**
 * @author Ding.Jinzhou
 * @date 2024/7/29 14:25
 */
public interface ChatFlowConstant {
    interface InstanceKey {
        String START_OUTPUT = "startOutput";
        String USER_TASK_OUTPUT = "userTaskOutput";
        String INPUT = "input";
        String INPUT_HISTORY = "inputHistory";
        String OPTION = "option";
        String FLOW_MAP = "flowMap";
        String SESSION_ID = "sessionId";
    }

    interface PropKey {
        /**
         * 环节入参要求
         */
        String INPUT_PARAM = "inParamMappingList";

        /**
         * 环节绑定的业务能力id
         */
        String BIZ_KEY = "bizKey";

        /**
         * 环节类型统一key
         */
        String NODE_TYPE = "nodeType";
    }

    interface NodeType {
        // 代码片段
        String CODE = "code";
        // 变量环节
        String VAR = "var";
        // 意图识别
        String INTENT = "intent";
        // 知识库
        String KNOWLEDGE = "knowledge";
        // 插件环节
        String PLUGIN = "plugin";
        // 大模型环节
        String LLM = "llm";
    }

}
