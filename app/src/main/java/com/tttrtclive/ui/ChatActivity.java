package com.tttrtclive.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.tttrtclive.R;
import com.wushuangtech.api.EnterConfApi;

public class ChatActivity extends BaseActivity {

    private EditText mMessage;
    private int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mMessage = findViewById(R.id.message);
        EnterConfApi.getInstance().enableChat();
    }

    public void onSendButtonClick(View v) {
        mTTTEngine.sendChatMessage(0, 1, "0", "nihao");
        /*if (i % 2 == 0) {
            ((Button)v).setText("stop");
            mTTTEngine.startRecordChatAudio();
        } else {
            ((Button)v).setText("start");
            mTTTEngine.stopRecordAndSendChatAudio(0, "0");
        }
        i ++;*/
    }
}
