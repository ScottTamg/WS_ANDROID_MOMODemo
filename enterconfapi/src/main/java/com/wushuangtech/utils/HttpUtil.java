package com.wushuangtech.utils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Administrator on 2017-09-14.
 */

public class HttpUtil {

    private static final int TIMEOUT_IN_MILLIONS = 5000;

    public abstract static class CallBack {

        protected String mUUID = "";
        protected CallBack(String mUUID){
            this.mUUID = mUUID;
        }

        public abstract void onRequestComplete(String result);

        public abstract void onRequestError(String error);
    }

    /**
     * 异步的Get请求
     *
     * @param urlStr
     * @param callBack
     */
    public static void doGetAsyn(final String urlStr, final CallBack callBack) {
        new Thread() {
            public void run() {
                try {
                    doGet(urlStr , callBack);
                } catch (Exception e) {
                    if (callBack != null) {
                        callBack.onRequestError(e.getLocalizedMessage());
                    }
                }

            }

            ;
        }.start();
    }

    /**
     * Get请求，获得返回数据
     *
     * @param urlStr
     * @return
     * @throws Exception
     */
    public static String doGet(String urlStr , final CallBack callBack) {
        URL url;
        HttpURLConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();
                int len = -1;
                byte[] buf = new byte[128];

                while ((len = is.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                baos.flush();
                String result = baos.toString();
                if (callBack != null) {
                    callBack.onRequestComplete(result);
                }
                return baos.toString();
            } else {
                int code = 0;
                int remain = 60 * 60;
                JSONObject obj = new JSONObject();
                obj.put("remain", remain);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code", code);
                jsonObject.put("data", obj);
                String s = jsonObject.toString();
                if (callBack != null) {
                    callBack.onRequestError(s);
                }
                return s;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (baos != null) baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;

    }
}
