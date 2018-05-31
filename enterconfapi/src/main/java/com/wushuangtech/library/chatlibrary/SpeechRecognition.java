package com.wushuangtech.library.chatlibrary;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.wushuangtech.jni.ChatJni;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by Administrator on 2018-01-17.
 */

public class SpeechRecognition {

    private String mAppId = "appid=5a2f7721";
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    // 函数调用返回值
    int ret = 0;
	private boolean mFinished = false;
    private Context mContext;

    public SpeechRecognition(Context context) {
        mContext = context;
        SpeechUtility.createUtility(context, mAppId);
        mIat = SpeechRecognizer.createRecognizer(context, mInitListener);

        if( null == mIat ){
            Log.d("zhx", "SpeechRecognizer初始化失败");
            return;
        }
    }

    public void startRecognition(String audioPath) {
        mIatResults.clear();
        // 设置参数
        setParam();
        // 设置音频来源为外部文件
//        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        // 也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
         mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
         mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, audioPath);
        ret = mIat.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.d("zhx", "识别失败,错误码：" + ret);
        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                Log.d("zhx", "Speech初始化失败!!!");
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() { }

        @Override
        public void onError(SpeechError error) {
            Log.d("zhx", "onError: SpeechError" + error);
            ChatJni.getInstance().OnSpeechRecognized(error.toString());
        }

        @Override
        public void onEndOfSpeech() { }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            if (isLast) {
                mFinished = true;
                mIat.stopListening();
            } else {
                mFinished = false;
            }

            printResult(results);
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) { }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) { }
    };

    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
    }

    private void printResult(RecognizerResult results) {
        String text = parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        if (mFinished) {
            Log.d("zhx", "printResult: " + resultBuffer.toString());
            ChatJni.getInstance().OnSpeechRecognized(resultBuffer.toString());
        }
    }

    public String parseIatResult(String json) {
        StringBuffer ret = new StringBuffer();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
//				如果需要多候选结果，解析数组其他字段
//				for(int j = 0; j < items.length(); j++)
//				{
//					JSONObject obj = items.getJSONObject(j);
//					ret.append(obj.getString("w"));
//				}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

    public void onDestroy() {
        if( null != mIat ){
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }

}
