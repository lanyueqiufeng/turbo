package com.didiglobal.turbo.engine.common;

public enum ErrorEnum {

    //1000~1999 非阻断性错误码
    SUCCESS(1000, "Success"),
    REENTRANT_WARNING(1001, "Reentrant warning"),
    COMMIT_SUSPEND(1002, "Commit task suspend"),
    ROLLBACK_SUSPEND(1003, "Rollback task suspend"),

    //2000~2999 通用业务错误
    PARAM_INVALID(2001, "Invalid param"),
    FLOW_NESTED_LEVEL_EXCEEDED(2002, "Flow nested level exceeded"),
    FLOW_NESTED_DEAD_LOOP(2003, "Flow nested dead loop"),

    //3000~3999 流程定义错误
    DEFINITION_INSERT_INVALID(3001, "数据库插入失败"),
    DEFINITION_UPDATE_INVALID(3002, "数据库更新失败"),

    // Flow not exist
    FLOW_NOT_EXIST(3101, "流程不存在"),
    // Flow not editing status
    FLOW_NOT_EDITING(3102, "流程不是编辑状态"),

    // Empty model
    MODEL_EMPTY(3201, "模型为空"),
    // Zero or more than one start node
    START_NODE_INVALID(3202, "流程必须有且仅有一个开始节点"),
    // Element key not unique
    ELEMENT_KEY_NOT_UNIQUE(3203, "流程元素key必须唯一"),
    // No end node
    END_NODE_INVALID(3204, "流程至少需要有一个结束节点"),
    // Not unicom
    MODEL_NOT_UNICOM(3205, "该流程从开始节点不能到底每一个节点"),
    // Sequence belong to multi pair node
    SEQUENCE_BELONG_TO_MULTI_PAIR_NODE(3206, "边应该属于一个入口节点和一个出口节点"),
    // Ring wrong
    RING_WRONG(3207, "流程中环结构中必须至少包含一个用户节点"),
    // Gateway no outgoing
    GATEWAY_NO_OUTGOING(3208, "解析环节应该至少有一条出口"),
    // Empty sequence outgoing
    EMPTY_SEQUENCE_OUTGOING(3209, "解析环节条件分支除默认分支外均需要配置条件表达式"),
    // Too many default sequence
    TOO_MANY_DEFAULT_SEQUENCE(3210, "解析环节最多只能有一条默认分支"),
    // Unknown element key
    MODEL_UNKNOWN_ELEMENT_KEY(3211, "不支持该类型"),
    //Too many incoming
    ELEMENT_TOO_MUCH_INCOMING(3212, "非条件判断节点以及起始节点外，其他节点有且仅有一个出口"),
    // Too many outgoing
    ELEMENT_TOO_MUCH_OUTGOING(3213, "太多的出口分支"),
    // Element lack incoming
    ELEMENT_LACK_INCOMING(3214, "元素缺少入口"),
    //Element lack outgoing
    ELEMENT_LACK_OUTGOING(3215, "元素缺少出口"),
    // required element attributes
    REQUIRED_ELEMENT_ATTRIBUTES(3216, "缺少节点必要的属性"),
    // Unknown element value
    MODEL_UNKNOWN_ELEMENT_VALUE(3217, "不识别的流程属性值"),

    //4000~4999 流程执行错误
    COMMIT_FAILED(4001, "Commit task failed"),
    ROLLBACK_FAILED(4002, "Rollback task failed"),
    COMMIT_REJECTRD(4003, "Commit rejected, flow is terminate"),
    ROLLBACK_REJECTRD(4004, "Rollback rejected, non-running flowInstance to rollback"),
    NO_NODE_TO_ROLLBACK(4005, "No node to rollback"),
    NO_USER_TASK_TO_ROLLBACK(4006, "No userTask to rollback"),
    GET_FLOW_DEPLOYMENT_FAILED(4007, "Get flowDeployment failed"),
    GET_FLOW_INSTANCE_FAILED(4008, "Get flowInstance failed"),
    GET_NODE_FAILED(4009, "Get current node failed"),
    GET_NODE_INSTANCE_FAILED(4010, "Get nodeInstance failed"),
    GET_INSTANCE_DATA_FAILED(4011, "Get instanceData failed"),
    GET_HOOK_CONFIG_FAILED(4012, "Get hook config failed"),
    GET_OUTGOING_FAILED(4013, "Get outgoing failed"),
    UNSUPPORTED_ELEMENT_TYPE(4014, "Unsupported element type"),
    MISSING_DATA(4015, "Miss data"),
    SAVE_FLOW_INSTANCE_FAILED(4016, "Save flowInstance failed"),
    SAVE_INSTANCE_DATA_FAILED(4017, "Save instanceData failed"),
    GROOVY_CALCULATE_FAILED(4018, "Groovy calculate failed"),
    GET_CALL_ACTIVITY_MODEL_FAILED(4019, "Get CallActivity model failed"),
    NO_RECEIVE_SUB_FLOW_INSTANCE(4020, "Do not receive subFlowInstanceId"),


    //5000~5999 系统错误
    //保留错误码
    SYSTEM_ERROR(5000, "System error"),
    FAILED(5001, "Failed");

    ErrorEnum(int errNo, String errMsg) {
        this.errNo = errNo;
        this.errMsg = errMsg;
    }

    private int errNo;
    private String errMsg;

    public int getErrNo() {
        return errNo;
    }

    public void setErrNo(int errNo) {
        this.errNo = errNo;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public static ErrorEnum getErrorEnum(int errNo) {
        for (ErrorEnum e : ErrorEnum.values()) {
            if (e.getErrNo() == errNo) {
                return e;
            }
        }
        return null;
    }

    public static boolean isSuccess(int errNo) {
        return errNo >= 1000 && errNo < 2000;
    }
}
