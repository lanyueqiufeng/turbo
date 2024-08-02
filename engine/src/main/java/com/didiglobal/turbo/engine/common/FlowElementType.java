package com.didiglobal.turbo.engine.common;

public class FlowElementType {
    // 线
    public static final int SEQUENCE_FLOW = 1;
    // 开始环节
    public static final int START_EVENT = 2;
    // 结束环节
    public static final int END_EVENT = 3;
    // 等待输入
    public static final int USER_TASK = 4;
    // 分支环节
    public static final int EXCLUSIVE_GATEWAY = 6;
    // 子流程（业务流+动态业务流）
    public static final int CALL_ACTIVITY = 8;

    // 回复环节
    public static final int REPLY_NODE = 601;

    // 意图识别、插件、代码块、变量、知识库、大模型
    public static final int SERVICE_TASK = 602;

}
