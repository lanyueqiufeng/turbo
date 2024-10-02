package com.didiglobal.turbo.engine.validator;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.didiglobal.turbo.engine.common.ErrorEnum;
import com.didiglobal.turbo.engine.exception.DefinitionException;
import com.didiglobal.turbo.engine.model.FlowElement;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ExclusiveGatewayValidator extends ElementValidator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ExclusiveGatewayValidator.class);

    @Override
    protected void checkOutgoing(Map<String, FlowElement> flowElementMap,
                                 FlowElement flowElement,
                                 Boolean isNotFormat) throws DefinitionException {
        List<String> outgoing = flowElement.getOutgoing();

        if (CollectionUtils.isEmpty(outgoing)) {
            throwElementValidatorException(flowElement, ErrorEnum.ELEMENT_LACK_OUTGOING, isNotFormat);
        }

        List<String> outgoingList = flowElement.getOutgoing();
        //读取分支条件
        JSONArray conditionList = (JSONArray) flowElement.getProperties().get("conditionList");
        //最少有两条分支
        if (conditionList == null || conditionList.isEmpty() || outgoingList.size() < 2) {
            throwElementValidatorException(flowElement, ErrorEnum.AT_LEAST_TWO_BRANCHES, isNotFormat);
        }

        if (outgoingList.size() - 1 != conditionList.size()) {
            throwElementValidatorException(flowElement, ErrorEnum.BRANCH_CONDITIONS_SHOULD_BE_FEWER_THAN_OUTGOING, isNotFormat);
        }

        for (int i = 0; i < outgoingList.size() - 1; i++) {
            JSONObject condition = conditionList.getJSONObject(i);
            //每个分支的条件集合
            JSONArray itemList = condition.getJSONArray("itemList");
            if (itemList == null) {
                throwElementValidatorException(flowElement, ErrorEnum.REQUIRED_ELEMENT_ATTRIBUTES, isNotFormat);
            }
            //子条件的组合方式，有两种组合方式：并、或
            String operator = condition.getString("operator");
            if (operator == null) {
                throwElementValidatorException(flowElement, ErrorEnum.REQUIRED_ELEMENT_ATTRIBUTES, isNotFormat);
            }
            for (int k = 0; k < itemList.size(); k++) {
                //子条件
                JSONObject conditionItem = itemList.getJSONObject(k);
                //变量所在节点的id
                String act = conditionItem.getString("act");
                if (StringUtils.isBlank(act)) {
                    throwElementValidatorException(flowElement, ErrorEnum.REQUIRED_ELEMENT_ATTRIBUTES, isNotFormat);
                }
                //变量的名字
                String name = conditionItem.getString("name");
                if (StringUtils.isBlank(name)) {
                    throwElementValidatorException(flowElement, ErrorEnum.REQUIRED_ELEMENT_ATTRIBUTES, isNotFormat);
                }
                //比较符号
                String conditionItemOperator = conditionItem.getString("operator");
                if (StringUtils.isBlank(conditionItemOperator)) {
                    throwElementValidatorException(flowElement, ErrorEnum.REQUIRED_ELEMENT_ATTRIBUTES, isNotFormat);
                }
                //比较值类型，有两种：引用、输入
                String from = conditionItem.getString("from");
                if (StringUtils.isBlank(from)) {
                    throwElementValidatorException(flowElement, ErrorEnum.REQUIRED_ELEMENT_ATTRIBUTES, isNotFormat);
                }
                //如果是引用类型，此值表示引用节点的id
                String nodeKey = conditionItem.getString("nodeKey");
                if (from.equals("Reference") && StringUtils.isBlank(nodeKey) && !"isNull".equals(conditionItemOperator) && !"isNotNull".equals(conditionItemOperator)) {
                    throwElementValidatorException(flowElement, ErrorEnum.REQUIRED_ELEMENT_ATTRIBUTES, isNotFormat);
                }
                //如果是引用类型，此值表示引用节点的变量名
                Object value = conditionItem.get("value");
                if (from.equals("Reference") && StringUtils.isBlank((String) value) && !"isNull".equals(conditionItemOperator) && !"isNotNull".equals(conditionItemOperator)) {
                    throwElementValidatorException(flowElement, ErrorEnum.REQUIRED_ELEMENT_ATTRIBUTES, isNotFormat);
                }
            }

        }
    }
}
