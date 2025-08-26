package com.didiglobal.turbo.engine.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

/**
 * 流程实例 数据表  每个流程实例存在有且唯一一条数据   用于存储共享参数池
 */
@TableName("ei_flow_instance_data")
public class FlowInstanceDataPo {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 关联的流程实例id
    private String flowInstanceId;
    // 存储的流程实例 共享数据
    private String flowInsData;
    // 更新数据的对应环节实例id
    private String lastNodeInsId;
    // 流程版本
    private String flowDeployId;
    private String flowModuleId;
    // 数据更新时间
    private Date updateDate;
    // 是否被删除
    @TableLogic(delval = "1", value = "0")
    private Integer archive = 0;
    private String tenant;
    private String caller;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlowInstanceId() {
        return flowInstanceId;
    }

    public void setFlowInstanceId(String flowInstanceId) {
        this.flowInstanceId = flowInstanceId;
    }

    public String getFlowInsData() {
        return flowInsData;
    }

    public void setFlowInsData(String flowInsData) {
        this.flowInsData = flowInsData;
    }

    public String getLastNodeInsId() {
        return lastNodeInsId;
    }

    public void setLastNodeInsId(String lastNodeInsId) {
        this.lastNodeInsId = lastNodeInsId;
    }

    public String getFlowDeployId() {
        return flowDeployId;
    }

    public void setFlowDeployId(String flowDeployId) {
        this.flowDeployId = flowDeployId;
    }

    public String getFlowModuleId() {
        return flowModuleId;
    }

    public void setFlowModuleId(String flowModuleId) {
        this.flowModuleId = flowModuleId;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getArchive() {
        return archive;
    }

    public void setArchive(Integer archive) {
        this.archive = archive;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }
}
