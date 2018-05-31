package com.wushuangtech.jni;

import android.util.Base64;
import android.util.Log;

import com.google.tttgson.Gson;
import com.wushuangtech.bean.ChatInfo;
import com.wushuangtech.bean.TTTChatModuleContants;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.library.PviewConferenceRequest;
import com.wushuangtech.utils.PviewLog;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.wushuangtech.utils.PviewLog.CHAT_SEND;

/**
 * Created by Administrator on 2017-12-11.
 */

public class ChatJni {

    public static final int CHATDATATYPE_TEXT = 1;      //文字聊天
    public static final int CHATDATATYPE_PICTURE = 2;   //图片
    public static final int CHATDATATYPE_AUDIO = 3;     //音频文件
    public static final int CHATDATATYPE_CUSTOM = 4;    //自定义消息
    public static final int CHATDATATYPE_SIGNAL = 5;    //信令消息

    private static ChatJni mChatJni;
    private List<WeakReference<PviewConferenceRequest>> mCallBacks;
    private ChatInfoManager mChatInfoManager = new ChatInfoManager();

    private ChatJni() {
        mCallBacks = new ArrayList<>();
    }

    public static synchronized ChatJni getInstance() {
        if (mChatJni == null) {
            synchronized (ChatJni.class) {
                if (mChatJni == null) {
                    mChatJni = new ChatJni();
                    if (!mChatJni.initialize(mChatJni)) {
                        throw new RuntimeException("can't initilaize ChatJni");
                    }
                }
            }
        }
        return mChatJni;
    }

    /**
     * 添加自定义的回调，监听接收到的服务信令
     *
     * @param callback 回调对象
     */
    public void addCallback(PviewConferenceRequest callback) {
        this.mCallBacks.add(new WeakReference<PviewConferenceRequest>(callback));
    }

    /**
     * 移除自定义添加的回调
     *
     * @param callback 回调对象
     */
    public void removeCallback(PviewConferenceRequest callback) {
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                if (wf.get() == callback) {
                    mCallBacks.remove(wf);
                    return;
                }
            }
        }
    }

    public native boolean initialize(ChatJni request);

    public native void unInitialize();

    public native void enableChat();

    public native void SendChat(long nGroupID, long nDstUserID, int type, String sSeqID, String sData, int nLen);

    public native void enableSignal();

    public native void SendSignal(long nGroupID, long nDstUserID, int type, String sSeqID, String sData, int nLen);

    public void sendChat(long nGroupID, long nDstUserID, int type, String sSeqID, String sData) {
        PviewLog.d(CHAT_SEND, "nGroupID : " + nGroupID + " | nDstUserID : " + nDstUserID
                + " | type : " + type + " | sSeqID : " + sSeqID + " | sData : " + sData);
        ChatInfo chatInfo;
        if (type == CHATDATATYPE_AUDIO) {
            Gson gson = new Gson();
            chatInfo = gson.fromJson(sData, ChatInfo.class);
            chatInfo.chatData = GlobalConfig.mChatSendPath + chatInfo.chatData + ".wav";
        } else {
            chatInfo = new ChatInfo();
            chatInfo.chatData = sData;
            chatInfo.audioDuration = 0;
        }
        chatInfo.chatType = type;
        chatInfo.seqID = sSeqID;

        mChatInfoManager.put(chatInfo);

        String message = Base64.encodeToString(sData.getBytes(), Base64.NO_WRAP);
        SendChat(nGroupID, nDstUserID, type, sSeqID, message, message.getBytes().length);
    }

    private void OnChatSend(int type, String sSeqID, int error) {
        PviewLog.jniCall("ChatModule OnSendResult", "Begin");
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                wf.get().OnChatSend(mChatInfoManager.pop(sSeqID), error);
            }
        }
        PviewLog.jniCall("ChatModule OnSendResult", "End");
    }

    private void OnChatRecv(long nSrcUserID, int type, String sSeqID, String strData, int length) {
        PviewLog.jniCall("ChatModule OnRecvResult", "Begin");
        if (type == CHATDATATYPE_TEXT || type == CHATDATATYPE_SIGNAL) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    try {
                        String message = new String(Base64.decode(strData, Base64.NO_WRAP), "utf-8");
                        ChatInfo chatInfo = new ChatInfo();
                        chatInfo.chatType = type;
                        chatInfo.seqID = sSeqID;
                        chatInfo.chatData = message;
                        wf.get().OnChatRecv(nSrcUserID, chatInfo);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
            PviewLog.jniCall("ChatModule OnRecvResult", "End");
        } else {
            GlobalHolder instance = GlobalHolder.getInstance();
            if (instance != null) {
                try {
                    String message = new String(Base64.decode(strData, Base64.NO_WRAP), "utf-8");
                    Log.d("zhx", "OnChatRecv: " + message);
                    instance.handleChatModule(TTTChatModuleContants.ACTION_ALIOSS_DOWNLOAD, new Object[]{nSrcUserID, sSeqID, message});
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void OnAudioDonwload(long nSrcUserID, int type, String sSeqID, String strData) {
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                Gson gson = new Gson();
                ChatInfo chatInfo = gson.fromJson(strData, ChatInfo.class);
                chatInfo.chatType = type;
                chatInfo.seqID = sSeqID;
                wf.get().OnChatRecv(nSrcUserID, chatInfo);
            }
        }
    }

    public void OnPlayCompletion(String filePath) {
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                wf.get().onPlayChatAudioCompletion(filePath);
            }
        }
    }

    public void OnSpeechRecognized(String str) {
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                wf.get().onSpeechRecognized(str);
            }
        }
    }

    private class ChatInfoManager {

        private ArrayList<ChatInfo> chatInfos = new ArrayList<>();

        public void put(ChatInfo chatInfo) {
            chatInfos.add(chatInfo);
        }

        public ChatInfo pop(String seqID) {
            ChatInfo chatInfo;
            for (int i = 0; i < chatInfos.size(); i++) {
                if (chatInfos.get(i).seqID.equals(seqID)) {
                    chatInfo = chatInfos.remove(i);
                    return chatInfo;
                }
            }
            return null;
        }
    }

}
