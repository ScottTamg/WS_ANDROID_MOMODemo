package com.wushuangtech.jni;

import android.content.Context;

public class NativeInitializer {
	
	private static NativeInitializer instance;
	
	private NativeInitializer() {
		
	}
	public static NativeInitializer getIntance() {
		if (instance == null) {
			synchronized (NativeInitializer.class) {
				if (instance == null) {
					instance = new NativeInitializer();
				}
			}
		}
		return instance;
	}
	public native void initialize(Context context , boolean enableChat , int logLevel);

	public native String getVersion();
}
