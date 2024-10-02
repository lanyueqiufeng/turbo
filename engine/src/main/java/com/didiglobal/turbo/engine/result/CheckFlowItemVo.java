package com.didiglobal.turbo.engine.result;

/**
 * @author deng_sky
 * @Date: 2024/10/2 22:33
 * @Description:
 */
public class CheckFlowItemVo {

	private String elementName;
	private String elementKey;
	private String elementType;

	public String getElementType() {
		return elementType;
	}

	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	private String exceptionMsg;
	private int errNo;

	public int getErrNo() {
		return errNo;
	}

	public void setErrNo(int errNo) {
		this.errNo = errNo;
	}

	public String getElementName() {
		return elementName;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	public String getElementKey() {
		return elementKey;
	}

	public void setElementKey(String elementKey) {
		this.elementKey = elementKey;
	}

	public String getExceptionMsg() {
		return exceptionMsg;
	}

	public void setExceptionMsg(String exceptionMsg) {
		this.exceptionMsg = exceptionMsg;
	}


}



