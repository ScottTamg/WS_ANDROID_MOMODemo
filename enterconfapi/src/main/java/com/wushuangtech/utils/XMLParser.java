package com.wushuangtech.utils;

import android.util.Xml;

import com.wushuangtech.bean.RemoteVideoMute;
import com.wushuangtech.bean.UserDeviceInfos;
import com.wushuangtech.library.GlobalConfig;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

/**
 * Created by wangzhiguo on 17/7/4.
 */

public class XMLParser {

    public static RemoteVideoMute parseVideoState(String xmlStr) {
        XmlPullParser pullParser = Xml.newPullParser();
        // 设置需要解析的XML数据
        try {
            pullParser.setInput(new StringReader(xmlStr));
            int event = pullParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String nodeName = pullParser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG: // 标签开始
                        if ("video".equals(nodeName)) {
                            RemoteVideoMute mute = new RemoteVideoMute();
                            int attributeCount =
                                    pullParser.getAttributeCount();
                            for (int i = 0; i < attributeCount; i++) {
                                String attributeName =
                                        pullParser.getAttributeName(i);
                                if (attributeName.equals("id")) {
                                    String attributeValue =
                                            pullParser.getAttributeValue(i);
                                    try {
                                        long tempID = Long.valueOf(attributeValue);
                                        if (tempID == GlobalConfig.mLocalUserID) {
                                            return null;
                                        }
                                        mute.setUserID(tempID);
                                    } catch (Exception e) {
                                        e.fillInStackTrace();
                                    }
                                }

                                if (attributeName.equals("inuse")) {
                                    String inuse = pullParser.getAttributeValue(i);
                                    if ("1".equals(inuse)) {
                                        mute.setIsMuted(false);
                                    } else {
                                        mute.setIsMuted(true);
                                    }
                                }
                            }
                            return mute;
                        }
                        break;
                }
                event = pullParser.next(); // 下一个标签
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static UserDeviceInfos getRemoteUserDeviceInfos(long uid , String szUserXml) {
        XmlPullParser pullParser = Xml.newPullParser();
        // 设置需要解析的XML数据
        try {
            UserDeviceInfos mUserDeviceInfos = new UserDeviceInfos();
            mUserDeviceInfos.mUserID = uid;
            pullParser.setInput(new StringReader(szUserXml));
            int event = pullParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String nodeName = pullParser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG: // 标签开始
                        if ("video".equals(nodeName)) {
                            int attributeCount =
                                    pullParser.getAttributeCount();
                            for (int i = 0; i < attributeCount; i++) {
                                String attributeName =
                                        pullParser.getAttributeName(i);
                                if ("id".equals(attributeName)) {
                                    mUserDeviceInfos.mUserDeviceID =
                                            pullParser.getAttributeValue(i);
                                } else if ("inuse".equals(attributeName)) {
                                    String inuse = pullParser.getAttributeValue(i);
                                    mUserDeviceInfos.mInUse = "1".equals(inuse);
                                }
                            }
                            return mUserDeviceInfos;
                        }
                        break;
                }
                event = pullParser.next(); // 下一个标签
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
