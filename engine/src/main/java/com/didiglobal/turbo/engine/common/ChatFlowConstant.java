package com.didiglobal.turbo.engine.common;

/**
 * @author Ding.Jinzhou
 * @date 2024/7/29 14:25
 */
public interface ChatFlowConstant {
    interface InstanceKey {
        String START_OUTPUT = "startOutput";
        String END_OUTPUT = "endOutput";
        String USER_TASK_OUTPUT = "userTaskOutput";
        String INPUT = "input";
        String MSG_TYPE = "msgType";
        String OPTION = "option";
        String USER_ID = "userId";
        String FLOW_MAP = "flowMap";
        String AGENT_MAP = "$agent";
        String SESSION_ID = "sessionId";
        String LAST_INPUT = "lastInput";
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
        // 固定子流程
        String FLOW = "flow";
        String DYNAMIC_FLOW = "dynamic_flow";
        // 分支环节
        String BRANCH = "branch";
        // 消息回复
        String MSG_REPLY = "msg_reply";
        // 用户输入
        String USER_INPUT = "user_input";
        // 开始
        String START = "start";
        // 结束
        String END = "end";
        // 线
        String SEQUENCE = "sequence";

    }

    interface ExclusiveGateway {
        /**
         * 分支条件列表
         */
        String CONDITION_LIST = "conditionList";
        /**
         * 单个分支的条件列表
         */
        String ITEM_LIST = "itemList";
        /**
         * 比较类型
         */
        String OPERATOR = "operator";
        /**
         * 左侧变量所属节点
         */
        String ACT="act";
        /**
         * 左侧变量所属节点名称
         */
        String ACT_NAME = "actName";
        /**
         * 左侧引用的参数名
         */
        String NAME = "name";
        /**
         * 右侧值类型
         */
        String FROM = "from";
        /**
         * 右侧变量所属节点
         */
        String NODE_KEY = "nodeKey";
        /**
         * 右侧变量所属节点名称
         */
        String NODE_NAME = "nodeName";
        /**
         * 右侧引用的参数名或者输入值
         */
        String VALUE = "value";
        String REFERENCE = "Reference";
        /**
         * 记录分支循环次数
         */
        String LOOP_COUNT = "LoopCount";
    }

}
