package com.didiglobal.turbo.engine.dao;

import com.didiglobal.turbo.engine.dao.mapper.NodeInstanceLogMapper;
import com.didiglobal.turbo.engine.entity.NodeInstanceLogPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NodeInstanceLogDAO extends BaseDAO<NodeInstanceLogMapper, NodeInstanceLogPO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeInstanceLogDAO.class);

    /**
     * insert nodeInstanceLogPO
     *
     * @param nodeInstanceLogPO
     * @return -1 while insert failed
     */
    public int insert(NodeInstanceLogPO nodeInstanceLogPO) {
        try {
            LOGGER.debug("已忽略的nodeInstanceLogPO信息：{}", nodeInstanceLogPO);
            return 1;
        } catch (Exception e) {
            LOGGER.error("insert exception.||nodeInstanceLogPO={}", nodeInstanceLogPO, e);
        }
        return -1;
    }

    /**
     * nodeInstanceLogList batch insert
     *
     * @param nodeInstanceLogList
     * @return
     */
    public boolean insertList(List<NodeInstanceLogPO> nodeInstanceLogList) {
        if (nodeInstanceLogList.isEmpty()) {
            log.warn("nodeInstanceLogList is empty");
            return true;
        }
        LOGGER.debug("已忽略的nodeInstanceLogList信息：{}", nodeInstanceLogList);
        return true;
    }
}
