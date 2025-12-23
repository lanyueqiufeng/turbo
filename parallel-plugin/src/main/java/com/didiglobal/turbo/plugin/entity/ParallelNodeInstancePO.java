package com.didiglobal.turbo.plugin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
@TableName("ei_node_instance_parallel")
public class ParallelNodeInstancePO{
    @TableId(type = IdType.INPUT)
    private String id;
    private String executeId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExecuteId() {
        return executeId;
    }

    public void setExecuteId(String executeId) {
        this.executeId = executeId;
    }

    @Override
    public String toString() {
        return "ParallelNodeInstancePO{" +
            "id=" + id +
            ", executeId='" + executeId + '\'' +
            '}';
    }
}
