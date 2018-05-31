package com.wushuangtech.library;

public class AsyncResult {

	Object userObject;
	Object result;
	Exception exception;

	AsyncResult(Object userObject, Object result) {
		super();
		this.userObject = userObject;
		this.result = result;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}
	
	
}
