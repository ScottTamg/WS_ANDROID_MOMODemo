package com.tttrtclive.ui;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtclive.Helper.TTTRtcEngineHelper;
import com.tttrtclive.LocalConfig;
import com.tttrtclive.LocalConstans;
import com.tttrtclive.MainApplication;
import com.tttrtclive.R;
import com.tttrtclive.bean.DisplayDevice;
import com.tttrtclive.bean.EnterUserInfo;
import com.tttrtclive.bean.JniObjs;
import com.tttrtclive.bean.VideoViewObj;
import com.tttrtclive.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclive.callback.PhoneListener;
import com.tttrtclive.dialog.DataInfoShowCallback;
import com.tttrtclive.dialog.ExitRoomDialog;
import com.tttrtclive.dialog.MoreInfoDialog;
import com.tttrtclive.dialog.MusicListDialog;
import com.tttrtclive.test.TestInterfaceListAdapter;
import com.tttrtclive.test.TestRelativeLayout;
import com.tttrtclive.ui.beauty.BeautyPopup;
import com.tttrtclive.utils.MyLog;
import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.bean.VideoCompositingLayout;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;
import static com.wushuangtech.library.Constants.VIDEO_PROFILE_120P;
import static com.wushuangtech.library.Constants.VIDEO_PROFILE_360P;
import static com.wushuangtech.library.LocalSDKConstants.CAPTURE_REQUEST_CODE;

public class MainActivity extends BaseActivity implements DataInfoShowCallback {

    public static final int MSG_TYPE_ERROR_ENTER_ROOM = 0;
    public static final int DISCONNECT = 100;

    public ViewGroup mFullScreenShowView;

    public ArrayList<VideoViewObj> mLocalSeiList;
    public HashSet<Long> mMutedAudioUserID;
    public List<EnterUserInfo> listData;
    public ConcurrentHashMap<Long, DisplayDevice> mShowingDevices;

    public Handler mHandler;

    public TextView mAudioSpeedShow;
    public TextView mVideoSpeedShow;
    private TextView mBroadcasterID;
    public ImageView mAudioChannel;
    private View mCannelMusicBT;
    private View mLocalMusicListBT;
    public View mRecordScreenBT;
    public TextView mRecordScreen;
    public ImageView mRecordScreenShare;
    private View mReversalCamera;
    public ScrollView mScrollView;
    private ImageView mRecordMeiYan;

    private MoreInfoDialog mMoreInfoDialog;
    private MusicListDialog mMusicListDialog;
    private ExitRoomDialog mExitRoomDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private TTTRtcEngineHelper mTTTRtcEngineHelper;
    private boolean mIsMute;
    private boolean mIsPhoneComing;
    private boolean mIsStop;
    private boolean mIsSpeaker;
    public boolean mIsHeadset;
    private TestRelativeLayout mTestRelativeLayout;

    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;
    private TestInterfaceListAdapter mRecycleAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_videochat);
        mTTTRtcEngineHelper = new TTTRtcEngineHelper(this);
        initView();
        initData();
        initEngine();
        initDialog();
        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR)
            mTTTEngine.setVideoProfile(VIDEO_PROFILE_360P, true);
        else
            mTTTEngine.setVideoProfile(VIDEO_PROFILE_120P, true);
        mTTTEngine.enableAudioVolumeIndication(300, 3);
        MyLog.d("MainActivity onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyLog.d("MainActivity onStart mIsMute : " + mIsMute);
        mIsStop = false;
        if (LocalConfig.mRole != Constants.CLIENT_ROLE_AUDIENCE) {
            mTTTEngine.resumeAudioMixing();
            if (!mIsMute && !LocalConfig.mLocalMuteAuido) {
                mTTTEngine.muteLocalAudioStream(false);
            }
            mTTTEngine.muteLocalVideoStream(false);
        }
        mTTTEngine.muteAllRemoteAudioStreams(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyLog.d("MainActivity onStop");
        mIsStop = true;
        if (LocalConfig.mRole != Constants.CLIENT_ROLE_AUDIENCE) {
            mTTTEngine.pauseAudioMixing();
            mTTTEngine.muteLocalAudioStream(true);
            mTTTEngine.muteLocalVideoStream(true);
        }
        mTTTEngine.muteAllRemoteAudioStreams(true);
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        mMoreInfoDialog.dismiss();
        mMusicListDialog.dismiss();
        LocalConfig.mUserEnterOrder.clear();
        for (int i = 0; i < mLocalSeiList.size(); i++) {
            VideoViewObj videoViewObj = mLocalSeiList.get(i);
            videoViewObj.clear();
        }

        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        ((MainApplication)getApplicationContext()).unInitTestBroadcast();
        unregisterReceiver(mLocalBroadcast);
        mTTTEngine.stopScreenRecorder();
        super.onDestroy();
        MyLog.d("MainActivity onDestroy");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mTTTRtcEngineHelper.handlerRemoteDialogVisibile(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            mTTTRtcEngineHelper.realStartCapture(data);
        } else {
            mTTTRtcEngineHelper.mFlagRecord = 0;
        }
    }

    private void initView() {
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mFullScreenShowView = findViewById(R.id.main_background);
        mBroadcasterID = findViewById(R.id.main_btn_host);
        mCannelMusicBT = findViewById(R.id.main_btn_cannel_music);
        mLocalMusicListBT = findViewById(R.id.main_btn_music_channel);
        mAudioChannel = findViewById(R.id.main_btn_audio_channel);
        TextView mHourseID = findViewById(R.id.main_btn_title);
        mReversalCamera = findViewById(R.id.main_btn_camera);
        mRecordScreenBT = findViewById(R.id.main_btn_video_recorder);
        mRecordScreenShare = findViewById(R.id.main_btn_video_share);
        mRecordScreen = findViewById(R.id.main_btn_video_recorder_time);
        mScrollView = findViewById(R.id.main_btn_listly);
        mReversalCamera.setOnClickListener(v -> mTTTEngine.switchCamera());
        mTestRelativeLayout = findViewById(R.id.main_test_ly);
        mRecordMeiYan = findViewById(R.id.main_btn_meiyan);
        mRecordMeiYan.setOnClickListener(v -> showBeautyPopup());

        setTextViewContent(mHourseID, R.string.main_title, String.valueOf(LocalConfig.mLoginRoomID));

        findViewById(R.id.main_btn_exit).setOnClickListener((v) -> mExitRoomDialog.show());

        findViewById(R.id.main_btn_more).setOnClickListener(v -> {
            mMoreInfoDialog.show();
//            mTTTEngine.openFaceBeauty(true);
//            mTTTEngine.onEffectSelected(EffectEnum.getEffectsByEffectType(EFFECT_TYPE_NORMAL).get(1));
        });

        mLocalMusicListBT.setOnClickListener(v -> {
            mMusicListDialog.show();
//            mTTTEngine.openFaceBeauty(false);
//            mTTTEngine.onEffectSelected(EffectEnum.getEffectsByEffectType(EFFECT_TYPE_NORMAL).get(2));
        });

        mCannelMusicBT.setOnClickListener(v -> {
            mTTTEngine.stopAudioMixing();
            mMusicListDialog.setPlaying(false);
            mCannelMusicBT.setVisibility(View.INVISIBLE);
        });
        EnterConfApi.getInstance().applySpeakPermission(false);
        mAudioChannel.setOnClickListener(v -> {
            if (mIsMute) {
                if (mIsHeadset) {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
                } else {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_selector);
                }
                mTTTEngine.muteLocalAudioStream(false);
                mIsMute = false;
            } else {
                if (mIsHeadset) {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
                } else {
                    mAudioChannel.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
                }
                mTTTEngine.muteLocalAudioStream(true);
                mIsMute = true;
            }
        });

        mRecordScreenBT.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Toast.makeText(mContext, getResources().getString(R.string.sys_support_toast), Toast.LENGTH_SHORT).show();
                return;
            }
            startScreenRecord();
        });

        mRecordScreenShare.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Toast.makeText(mContext, getResources().getString(R.string.sys_support_toast), Toast.LENGTH_SHORT).show();
                return;
            }

            if (mTTTRtcEngineHelper.mIsRecordering) {
                return;
            }

            if (mTTTRtcEngineHelper.mIsShareRecordering) {
                mTTTRtcEngineHelper.mIsShareRecordering = false;
                mTTTEngine.shareScreenRecorder(false);
                mTTTEngine.stopScreenRecorder();
                mRecordScreenShare.setImageResource(R.drawable.mainly_btn_video_share);
            } else {
                mTTTEngine.shareScreenRecorder(true);
                mTTTEngine.tryScreenRecorder(this);
                mTTTRtcEngineHelper.mFlagRecord = TTTRtcEngineHelper.RECORD_TYPE_SHARE;
            }
        });
    }

    private void startScreenRecord() {
        if (mTTTRtcEngineHelper.mIsShareRecordering) {
            return;
        }

        if (mTTTRtcEngineHelper.mIsRecordering) {
            mTTTRtcEngineHelper.mIsRecordering = false;
            mTTTEngine.stopScreenRecorder();
            mRecordScreen.setVisibility(View.GONE);
            mRecordScreen.setText("00:00:00");
            mRecordScreenBT.setBackgroundResource(R.drawable.mainly_btn_video_recorder);
        } else {
            mTTTRtcEngineHelper.mFlagRecord = TTTRtcEngineHelper.RECORD_TYPE_FILE;
            mTTTEngine.tryScreenRecorder(this);
        }
    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    private void initEngine() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addCategory("ttt.test.interface");
        filter.addAction("ttt.test.interface.string");
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        ((MainApplication) getApplicationContext()).mMyTTTRtcEngineEventHandler.setIsSaveCallBack(false);
    }

    private void initDialog() {
        mMoreInfoDialog =
                new MoreInfoDialog(mContext, R.style.NoBackGroundDialog);
        mMoreInfoDialog.setDataInfoShowCallback(this);

        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(v -> {
            mTTTEngine.stopScreenRecorder();
            exitRoom();
            mExitRoomDialog.dismiss();
        });

        mExitRoomDialog.mDenyBT.setOnClickListener(v -> mExitRoomDialog.dismiss());

        mMusicListDialog = new MusicListDialog(mContext, R.style.NoBackGroundDialog);
        mMusicListDialog.setCanceledOnTouchOutside(true);
        mMusicListDialog.setMusicListOnClickListener(new MusicListDialog.MusicListOnClickListener() {
            @Override
            public void startAudioMixing(String filePath, boolean loopback, boolean replace, int cycle) {
                mTTTEngine.startAudioMixing(filePath, loopback, replace, cycle);
            }

            @Override
            public void stopAudioMixing() {
                mTTTEngine.stopAudioMixing();
            }

            @Override
            public void showCannelMusicPlayingBT() {
                mCannelMusicBT.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initData() {
        listData = new ArrayList<>();
        mShowingDevices = new ConcurrentHashMap<>();
        mMutedAudioUserID = new HashSet<>();
        if (mHandler == null) {
            mHandler = new LocalHandler();
        }
        mLocalSeiList = new ArrayList<>();
        if (LocalConfig.mRole == Constants.CLIENT_ROLE_AUDIENCE) {
            LocalConfig.mAudience++;
        } else if (LocalConfig.mRole == Constants.CLIENT_ROLE_BROADCASTER) {
            EnterUserInfo localInfo = new EnterUserInfo(LocalConfig.mLoginUserID, Constants.CLIENT_ROLE_BROADCASTER);
            addListData(localInfo);
        }

        mTTTRtcEngineHelper.initRemoteLayout(mLocalSeiList);

        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
            SurfaceView localSurfaceView;
            localSurfaceView = mTTTEngine.CreateRendererView(mContext);
            localSurfaceView.setZOrderMediaOverlay(false);
            mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN,
                    localSurfaceView), getRequestedOrientation());
            mFullScreenShowView.addView(localSurfaceView, 0);
            // 打开美颜
            mTTTEngine.openFaceBeauty(true);
        }

        if (LocalConfig.mRole != CLIENT_ROLE_ANCHOR) {
            mAudioChannel.setClickable(false);
        } else {
            mAudioChannel.setClickable(true);
        }

        TextView mRoleShow = findViewById(R.id.main_btn_role);
        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
            setTextViewContent(mBroadcasterID, R.string.main_broadcaster, String.valueOf(LocalConfig.mLoginUserID));
        }

        switch (LocalConfig.mRole) {
            case CLIENT_ROLE_ANCHOR:
                setTextViewContent(mRoleShow, R.string.main_local_role, getResources().getString(R.string.welcome_anchor));
                LocalConfig.mBroadcasterID = LocalConfig.mLoginUserID;
                break;
            case Constants.CLIENT_ROLE_BROADCASTER:
                setTextViewContent(mRoleShow, R.string.main_local_role, getResources().getString(R.string.welcome_auxiliary));
                mLocalMusicListBT.setVisibility(View.GONE);
                mReversalCamera.setVisibility(View.GONE);
                mRecordScreenBT.setVisibility(View.GONE);
                mRecordScreenShare.setVisibility(View.GONE);
                break;
            case Constants.CLIENT_ROLE_AUDIENCE:
                setTextViewContent(mRoleShow, R.string.main_local_role, getResources().getString(R.string.welcome_audience));
                mLocalMusicListBT.setVisibility(View.GONE);
                mReversalCamera.setVisibility(View.GONE);
                mRecordScreenBT.setVisibility(View.GONE);
                mRecordScreenShare.setVisibility(View.GONE);
                break;
        }

        if (LocalConfig.mCurrentAudioRoute != Constants.AUDIO_ROUTE_SPEAKER) {
            mIsHeadset = true;
            if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
                mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
            } else {
                for (int i = 0; i < mLocalSeiList.size(); i++) {
                    VideoViewObj videoCusSei = mLocalSeiList.get(i);
                    if (videoCusSei.mBindUid == LocalConfig.mLoginUserID) {
                        videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_headset_selector);
                        break;
                    }
                }
            }
        }

        RecyclerView mRecyclerView = mTestRelativeLayout.getTestListView();
        mRecycleAdapter = new TestInterfaceListAdapter(this);
        mRecycleAdapter.mDatas = ((MainApplication)getApplication()).mTestDatas;
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        //设置布局管理器
        mRecyclerView.setLayoutManager(layoutManager);
        //设置为垂直布局，这也是默认的
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        //设置Adapter
        mRecyclerView.setAdapter(mRecycleAdapter);
        //设置增加或删除条目的动画
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void addListData(EnterUserInfo info) {
        boolean bupdate = false;
        for (int i = 0; i < listData.size(); i++) {
            EnterUserInfo info1 = listData.get(i);
            if (info1.getId() == info.getId()) {
                listData.set(i, info);
                bupdate = true;
                break;
            }
        }
        if (!bupdate) {
            listData.add(info);
        }
    }

    private EnterUserInfo removeListData(long uid) {
        int index = -1;
        for (int i = 0; i < listData.size(); i++) {
            EnterUserInfo info1 = listData.get(i);
            if (info1.getId() == uid) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            return listData.remove(index);
        }
        return null;
    }

    public void exitRoom() {
        MyLog.d("exitRoom was called!... leave room , id : " + LocalConfig.mLoginUserID);
        mTTTEngine.stopAudioMixing();
        mTTTEngine.leaveChannel();
        LocalConfig.mLocalMuteAuido = false;
        finish();
    }

    @Override
    public void showDataInfo(boolean isShow) {
        Set<Map.Entry<Long, DisplayDevice>> entries = mShowingDevices.entrySet();
        if (isShow) {
            mAudioSpeedShow.setVisibility(View.VISIBLE);
            mVideoSpeedShow.setVisibility(View.VISIBLE);
            for (Map.Entry<Long, DisplayDevice> next : entries) {
                next.getValue().getDisplayView().mContentRoot.findViewById(R.id.videoly_video_down).setVisibility(View.VISIBLE);
                next.getValue().getDisplayView().mContentRoot.findViewById(R.id.videoly_audio_down).setVisibility(View.VISIBLE);
            }
        } else {
            mAudioSpeedShow.setVisibility(View.INVISIBLE);
            mVideoSpeedShow.setVisibility(View.INVISIBLE);
            for (Map.Entry<Long, DisplayDevice> next : entries) {
                next.getValue().getDisplayView().mContentRoot.findViewById(R.id.videoly_video_down).setVisibility(View.INVISIBLE);
                next.getValue().getDisplayView().mContentRoot.findViewById(R.id.videoly_audio_down).setVisibility(View.INVISIBLE);
            }
        }
    }

    protected BeautyPopup mBeautyPopup;

    /**
     * 显示美颜弹出框
     */
    protected void showBeautyPopup() {
        if (mBeautyPopup == null) {
            mBeautyPopup = new BeautyPopup(this, mTTTEngine);
        }
        if (!mBeautyPopup.isShowing()) {
            mBeautyPopup.showAtLocation(mFullScreenShowView,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        }
    }

    /**
     * 关闭美颜弹出框
     */
    protected void closeBeautyPopup() {
        if (mBeautyPopup != null && mBeautyPopup.isShowing()) {
            mBeautyPopup.dismiss();
        }
    }

    private class LocalHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TYPE_ERROR_ENTER_ROOM:
                    String message = "";
                    int errorType = (int) msg.obj;
                    if (errorType == Constants.ERROR_KICK_BY_HOST) {
                        message = getResources().getString(R.string.error_kicked);
                    } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                        message = getResources().getString(R.string.error_rtmp);
                    } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                        message = getResources().getString(R.string.error_server_overload);
                    } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                        message = getResources().getString(R.string.error_anchorexited);
                    } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                        message = getResources().getString(R.string.error_relogin);
                    } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                        message = getResources().getString(R.string.error_otherenter);
                    } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                        message = getResources().getString(R.string.error_noaudio);
                    } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                        message = getResources().getString(R.string.error_novideo);
                    } else if (errorType == DISCONNECT) {
                        message = getResources().getString(R.string.error_network_disconnected);
                    }
                    mTTTRtcEngineHelper.showErrorExitDialog(message);
                    break;
            }
        }
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(
                        MyTTTRtcEngineEventHandler.MSG_TAG);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_ERROR:
                        MyLog.d("UI onReceive CALL_BACK_ON_ERROR... ");
                        Message.obtain(mHandler, MSG_TYPE_ERROR_ENTER_ROOM, mJniObjs.mErrorType).sendToTarget();
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN:
                        long uid = mJniObjs.mUid;
                        int identity = mJniObjs.mIdentity;
                        MyLog.d("UI onReceive CALL_BACK_ON_USER_JOIN... uid : " + uid);
                        EnterUserInfo userInfo = new EnterUserInfo(uid, identity);
                        addListData(userInfo);
                        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
                            if (identity == Constants.CLIENT_ROLE_BROADCASTER) {
                                if (LocalConfig.mAuthorSize == TTTRtcEngineHelper.AUTHOR_MAX_NUM) {
                                    mTTTEngine.kickChannelUser(uid);
                                    return;
                                }
                            }

                            if (identity == Constants.CLIENT_ROLE_BROADCASTER) {
                                // 打开视频
                                DisplayDevice mRemoteDisplayDevice = mShowingDevices.get(uid);
                                if (mRemoteDisplayDevice == null) {
                                    mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, userInfo);
                                }
                            } else {
                                // 向观众发送sei
                                VideoCompositingLayout layout = new VideoCompositingLayout();
                                layout.regions = mTTTRtcEngineHelper.buildRemoteLayoutLocation();
                                mTTTEngine.setVideoCompositingLayout(layout);
                                LocalConfig.mAudience++;
                            }
                        } else {
                            if (identity == CLIENT_ROLE_ANCHOR) {
                                LocalConfig.mBroadcasterID = uid;
                                setTextViewContent(mBroadcasterID, R.string.main_broadcaster, String.valueOf(uid));
                                DisplayDevice mRemoteDisplayDevice = mShowingDevices.get(uid);
                                if (mRemoteDisplayDevice == null) {
                                    mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, userInfo);
                                }
                            } else if (identity == Constants.CLIENT_ROLE_AUDIENCE) {
                                LocalConfig.mAudience++;
                            }
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_OFFLINE:
                        long offLineUserID = mJniObjs.mUid;
                        MyLog.d("UI onReceive CALL_BACK_ON_USER_OFFLINE... offLineUserID : " + offLineUserID);
                        EnterUserInfo enterUserInfo = removeListData(offLineUserID);
                        if (enterUserInfo != null) {
                            if (enterUserInfo.getRole() == Constants.CLIENT_ROLE_AUDIENCE) {
                                LocalConfig.mAudience--;
                            }
                            mTTTRtcEngineHelper.mRemoteUserLastVideoData.remove(enterUserInfo.getId());
                            mTTTRtcEngineHelper.mRemoteUserLastAudioData.remove(enterUserInfo.getId());
                            mTTTRtcEngineHelper.adJustRemoteViewDisplay(false, enterUserInfo);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_AUDIO_VOLUME_INDICATION:
                        mTTTRtcEngineHelper.audioVolumeIndication(mJniObjs.mUid, mJniObjs.mAudioLevel, mIsMute);
                        break;
                    case LocalConstans.CALL_BACK_ON_SEI:
                        String sei = mJniObjs.mSEI;
                        TreeSet<EnterUserInfo> mInfos = new TreeSet<>();
                        try {
                            JSONObject jsonObject = new JSONObject(sei);
                            String mid = (String) jsonObject.get("mid");
                            LocalConfig.mBroadcasterID = Integer.valueOf(mid);
                            JSONArray jsonArray = jsonObject.getJSONArray("pos");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                                String devid = jsonobject2.getString("id");
                                float x = Float.valueOf(jsonobject2.getString("x"));
                                float y = Float.valueOf(jsonobject2.getString("y"));

                                long userId;
                                int index = devid.indexOf(":");
                                if (index > 0) {
                                    userId = Long.parseLong(devid.substring(0, index));
                                } else {
                                    userId = Long.parseLong(devid);
                                }

                                if (userId != LocalConfig.mBroadcasterID) {
                                    for (int j = 0; j < listData.size(); j++) {
                                        EnterUserInfo temp = listData.get(j);
                                        if (temp.getId() == userId) {
                                            temp.setXYLocation(y, x);
                                            mInfos.add(temp);
                                            break;
                                        }
                                    }
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        mTTTRtcEngineHelper.removeErrorIndexView(mInfos);
                        Iterator<EnterUserInfo> iterator = mInfos.iterator();
                        while (iterator.hasNext()) {
                            EnterUserInfo next = iterator.next();
                            mTTTRtcEngineHelper.adJustRemoteViewDisplay(true, next);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        mTTTRtcEngineHelper.remoteVideoStatus(mJniObjs.mRemoteVideoStats);
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE:
                        mTTTRtcEngineHelper.remoteAudioStatus(mJniObjs.mRemoteAudioStats);
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        mTTTRtcEngineHelper.localVideoStatus(mJniObjs.mLocalVideoStats);
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        mTTTRtcEngineHelper.LocalAudioStatus(mJniObjs.mLocalAudioStats);
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_COME:
                        mIsPhoneComing = true;
                        Log.d("WebRtcAudioRecord COME", "mIsStop : " + mIsStop);
                        mIsSpeaker = mTTTEngine.isSpeakerphoneEnabled();
                        if (mIsSpeaker) {
                            mTTTEngine.setEnableSpeakerphone(false);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:
                        Log.d("WebRtcAudioRecord IDLE", "mIsStop : " + mIsStop
                                + " | mIsPhoneComing ： " + mIsPhoneComing);
                        if (mIsPhoneComing) {
                            if (mIsSpeaker) {
                                mTTTEngine.setEnableSpeakerphone(true);
                            }
                            mIsPhoneComing = false;
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_SCREEN_RECORD_TIME:
                        if (!mTTTRtcEngineHelper.mIsShareRecordering) {
                            runOnUiThread(() -> {
                                String s = mTTTRtcEngineHelper.showTimeCount(mJniObjs.mScreenRecordTime);
                                mRecordScreen.setText(s);
                            });
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_MUTE_AUDIO:
                        long muteUid = mJniObjs.mUid;
                        boolean mIsMuteAuido = mJniObjs.mIsDisableAudio;
                        MyLog.i("OnRemoteAudioMuted CALL_BACK_ON_MUTE_AUDIO start! .... " + mJniObjs.mUid
                                + " | mIsMuteAuido : " + mIsMuteAuido);
                        boolean mIsFound = false;
                        for (int i = 0; i < mLocalSeiList.size(); i++) {
                            VideoViewObj videoCusSei = mLocalSeiList.get(i);
                            if (videoCusSei.mBindUid == muteUid) {
                                mIsFound = true;
                                MyLog.i("OnRemoteAudioMuted find it .... " + mJniObjs.mUid);
                                if (mIsMuteAuido) {
                                    videoCusSei.mIsRemoteDisableAudio = true;
                                    videoCusSei.mMuteVoiceIcon.setVisibility(View.VISIBLE);
                                    videoCusSei.mSpeakImage.setVisibility(View.INVISIBLE);
                                    videoCusSei.mIsMuted = true;
                                    videoCusSei.mMuteVoiceBT.setText(getResources().getString(R.string.remote_window_cancel_ban));
                                } else {
                                    videoCusSei.mIsRemoteDisableAudio = false;
                                    videoCusSei.mMuteVoiceIcon.setVisibility(View.INVISIBLE);
                                    videoCusSei.mSpeakImage.setVisibility(View.VISIBLE);
                                    videoCusSei.mIsMuted = false;
                                    videoCusSei.mMuteVoiceBT.setText(getResources().getString(R.string.remote_window_ban));

                                    if (muteUid == LocalConfig.mLoginUserID) {
                                        if (videoCusSei.mIsMuteRemote) {
                                            mTTTRtcEngineHelper.speakMuteClick(videoCusSei);
                                        }
                                    }
                                }
                                break;
                            }
                        }

                        if (!mIsFound && mJniObjs.mUid != LocalConfig.mBroadcasterID
                                && mIsMuteAuido) {
                            MyLog.i("OnRemoteAudioMuted could't find it .... " + mJniObjs.mUid);
                            mMutedAudioUserID.add(mJniObjs.mUid);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_AUDIO_ROUTE:
                        int mAudioRoute = mJniObjs.mAudioRoute;
                        if (LocalConfig.mRole == CLIENT_ROLE_ANCHOR) {
                            if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER) {
                                mIsHeadset = false;
                                if (mIsMute) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
                                } else {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_selector);
                                }
                            } else {
                                mIsHeadset = true;
                                if (mIsMute) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
                                } else {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
                                }
                            }
                        } else if (LocalConfig.mRole == Constants.CLIENT_ROLE_BROADCASTER) {
                            for (int i = 0; i < mLocalSeiList.size(); i++) {
                                VideoViewObj videoCusSei = mLocalSeiList.get(i);
                                if (videoCusSei.mBindUid == LocalConfig.mLoginUserID) {
                                    if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER) {
                                        mIsHeadset = false;
                                        if (videoCusSei.mIsMuteRemote) {
                                            videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
                                        } else {
                                            videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_selector);
                                        }
                                    } else {
                                        mIsHeadset = true;
                                        if (videoCusSei.mIsMuteRemote) {
                                            videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
                                        } else {
                                            videoCusSei.mSpeakImage.setImageResource(R.drawable.mainly_btn_headset_selector);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                }
            } else if ("ttt.test.interface.string".equals(action)) {
                String testString = intent.getStringExtra("testString");
                if (!TextUtils.isEmpty(testString)) {
                    mRecycleAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
