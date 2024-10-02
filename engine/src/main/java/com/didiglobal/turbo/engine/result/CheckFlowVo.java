package com.didiglobal.turbo.engine.result;

import java.util.List;

/**
 * @author deng_sky
 * @Date: 2024/10/2 22:33
 * @Description:
 */
public class CheckFlowVo extends CommonResult {

	private List<CheckFlowItemVo> failList;

	public List<CheckFlowItemVo> getFailList() {
		return failList;
	}

	public void setFailList(List<CheckFlowItemVo> failList) {
		this.failList = failList;
	}


}



