package com.wushuangtech.library;

import java.lang.ref.WeakReference;

import android.os.Handler;

public class HandlerWrap {

	public HandlerWrap(Handler h, int what, Object obj) {
		refH = new WeakReference<Handler>(h);
		this.what = what;
		userObj = obj;
	}

	public Handler getHandler() {
		if (refH == null)
			return null;

		return (Handler) refH.get();
	}

	public int getWhat() {
		return what;
	}

	public Object getObject() {
		return userObj;
	}

	WeakReference<Handler> refH;
	int what;
	Object userObj;

}
