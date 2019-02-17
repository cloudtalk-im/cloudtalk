package com.zhangwuji.im.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.R;
import com.zhangwuji.im.imcore.event.PriorityEvent;
import com.zhangwuji.im.imcore.service.IMService;
import com.zhangwuji.im.imcore.service.IMServiceConnector;
import com.zhangwuji.im.ui.entity.IMCallAction;
import com.zhangwuji.im.ui.entity.IMCallCommon;
import com.zhangwuji.im.ui.widget.IMBaseImageView;
import com.zhangwuji.im.utils.PickupDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class VideoChatViewActivity2 extends Activity implements Handler.Callback, PickupDetector.PickupDetectListener {

    private static final String LOG_TAG = VideoChatViewActivity2.class.getSimpleName();

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    public String roomid="";
    public int tageruserId=0;
    private IMService imService;
    private Toast mToast;
    protected PowerManager powerManager;
    protected PowerManager.WakeLock wakeLock;

    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;
    private long time = 0;
    private Runnable updateTimeRunnable;
    private LayoutInflater inflater;
    private FrameLayout mLPreviewContainer;
    private FrameLayout mSPreviewContainer;
    private FrameLayout mButtonContainer;
    private LinearLayout mUserInfoContainer;
    private Boolean isInformationShow = false;
    private SurfaceView mLocalVideo = null;
    private boolean muted = false;
    private boolean handFree = false;
    private boolean startForCheckPermissions = false;
    private int EVENT_FULL_SCREEN = 1;
    private int targetId = 0;
    private  int selfUserId=0;
    protected Handler handler;
    private boolean shouldShowFloat;
    private boolean shouldRestoreFloat;
    private IMCallCommon.CallMediaType mediaType;
    protected PickupDetector pickupDetector;
    private boolean isconnect=false;

    @Override
    final public boolean handleMessage(Message msg) {
        if (msg.what == EVENT_FULL_SCREEN) {
           // hideVideoCallInformation();
            return true;
        }
        return false;
    }

    public void showToast(int resId) {
        String text = getResources().getString(resId);
        if (mToast == null) {
            mToast = Toast.makeText(VideoChatViewActivity2.this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }
    /**
     * end 全局Toast
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("message_activity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            selfUserId=imService.getLoginManager().getLoginId();

            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)) {
                initdata();
            }
        }

        @Override
        public void onServiceDisconnected() {
        }
    };

    private RtcEngine mRtcEngine;// Tutorial Step 1
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1
        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) { // Tutorial Step 5
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //setupRemoteVideo(uid);

                    SurfaceView remoteVideo = RtcEngine.CreateRendererView(getBaseContext());
                    mRtcEngine.setupRemoteVideo(new VideoCanvas(remoteVideo, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));

                    if(mLocalVideo==null) {
                        mLocalVideo = RtcEngine.CreateRendererView(getBaseContext());
                        mLocalVideo.setZOrderMediaOverlay(true);
                        mRtcEngine.setupLocalVideo(new VideoCanvas(mLocalVideo, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));
                    }
                    onCallConnected();
                    onRemoteUserJoined(uid,mediaType,remoteVideo);
                    isconnect=true;

                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) { // Tutorial Step 7
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  end();
                }
            });
        }

        @Override
        public void onUserMuteVideo(final int uid, final boolean muted) { // Tutorial Step 10
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserVideoMuted(uid, muted);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_voip_activity_single_call);

        imServiceConnector.connect(this);
        roomid=getIntent().getStringExtra("roomid");
        tageruserId=getIntent().getIntExtra("tageruserId",0);

        if(roomid.contains("audo_"))
        {
            mediaType = IMCallCommon.CallMediaType.AUDIO;
        }
        else
        {
            mediaType = IMCallCommon.CallMediaType.VIDEO;
        }

        handler = new Handler();
        EventBus.getDefault().register(this);

    }

    public void initdata()
    {
        Intent intent = getIntent();
        mLPreviewContainer = (FrameLayout) findViewById(R.id.rc_voip_call_large_preview);
        mSPreviewContainer = (FrameLayout) findViewById(R.id.rc_voip_call_small_preview);
        mButtonContainer = (FrameLayout) findViewById(R.id.rc_voip_btn);
        mUserInfoContainer = (LinearLayout) findViewById(R.id.rc_voip_user_info);

        IMCallAction callAction = IMCallAction.valueOf(intent.getStringExtra("callAction"));


        if (mediaType != null) {
            inflater = LayoutInflater.from(this);
            initView(mediaType, callAction);

            setupIntent();
        } else {
//            RLog.w(TAG, "恢复的瞬间，对方已挂断");
//            setShouldShowFloat(false);
//            CallFloatBoxView.hideFloatBox();
            finish();
        }
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA);
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
            case PERMISSION_REQ_ID_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initdata();
                } else {
                    showLongToast("No permission for " + Manifest.permission.CAMERA);
                    finish();
                }
                break;
            }
        }
    }

    private void setupIntent() {

        initializeAgoraEngine();     //todo:第一步。初始化视频组件

        Intent intent = getIntent();
        IMCallAction callAction = IMCallAction.valueOf(intent.getStringExtra("callAction"));

        if (mediaType.equals(IMCallCommon.CallMediaType.AUDIO)) {
            handFree = false;
        } else if (mediaType.equals(IMCallCommon.CallMediaType.VIDEO)) {
            handFree = true;
        }
        setupVideoProfile();  //todo: 第二步。如果是视频。初始化视屏参数
        if (callAction.equals(IMCallAction.ACTION_INCOMING_CALL)) {

            targetId = tageruserId;

        } else if (callAction.equals(IMCallAction.ACTION_OUTGOING_CALL)) {

          //  mediaType = IMCallCommon.CallMediaType.VIDEO;
            targetId = tageruserId;

            List<Integer> userIds = new ArrayList<>();
            userIds.add(targetId);

            onCallOutgoing();  //todo: 发送视频  显示播出的ui。并加入到视频频道中。。

        } else { // resume call

        }


        //todo:设置对方的用户信息
        User userinfo=imService.getContactManager().findContact(tageruserId);
        if (userinfo != null) {
            TextView userName = (TextView) mUserInfoContainer.findViewById(R.id.rc_voip_user_name);
            userName.setText(userinfo.getMainName());
            if (mediaType.equals(IMCallCommon.CallMediaType.AUDIO)) {

                IMBaseImageView portraitImageView = (IMBaseImageView) mUserInfoContainer.findViewById(R.id.rc_voip_user_portrait);

                if (portraitImageView != null && userinfo.getAvatar() != null) {
                    //头像设置
                    portraitImageView.setDefaultImageRes(R.drawable.tt_default_user_portrait_corner);
                    portraitImageView.setCorner(15);
                    portraitImageView.setImageResource(R.drawable.tt_default_user_portrait_corner);
                    portraitImageView.setImageUrl(userinfo.getAvatar());
                }
            }
            mUserInfoContainer.setVisibility(View.VISIBLE);
        }

        createPowerManager();
        createPickupDetector();
    }

    private void initView(IMCallCommon.CallMediaType mediaType, IMCallAction callAction) {
        FrameLayout buttonLayout = (FrameLayout) inflater.inflate(R.layout.rc_voip_call_bottom_connected_button_layout, null);
        RelativeLayout userInfoLayout = (RelativeLayout) inflater.inflate(R.layout.rc_voip_audio_call_user_info, null);
        userInfoLayout.findViewById(R.id.rc_voip_call_minimize).setVisibility(View.GONE);

        if (callAction.equals(IMCallAction.ACTION_OUTGOING_CALL)) {
            buttonLayout.findViewById(R.id.rc_voip_call_mute).setVisibility(View.GONE);
            buttonLayout.findViewById(R.id.rc_voip_handfree).setVisibility(View.GONE);
        }

        if (mediaType.equals(IMCallCommon.CallMediaType.AUDIO)) {
            findViewById(R.id.rc_voip_call_information).setBackgroundColor(getResources().getColor(R.color.rc_voip_background_color));
            mLPreviewContainer.setVisibility(View.GONE);
            mSPreviewContainer.setVisibility(View.GONE);

            if (callAction.equals(IMCallAction.ACTION_INCOMING_CALL)) {
                buttonLayout = (FrameLayout) inflater.inflate(R.layout.rc_voip_call_bottom_incoming_button_layout, null);
                TextView callInfo = (TextView) userInfoLayout.findViewById(R.id.rc_voip_call_remind_info);
                callInfo.setText(R.string.rc_voip_audio_call_inviting);
                onIncomingCallRinging();
            }
        } else {
            userInfoLayout = (RelativeLayout) inflater.inflate(R.layout.rc_voip_video_call_user_info, null);
            if (callAction.equals(IMCallAction.ACTION_INCOMING_CALL)) {
                findViewById(R.id.rc_voip_call_information).setBackgroundColor(getResources().getColor(R.color.rc_voip_background_color));
                buttonLayout = (FrameLayout) inflater.inflate(R.layout.rc_voip_call_bottom_incoming_button_layout, null);
                TextView callInfo = (TextView) userInfoLayout.findViewById(R.id.rc_voip_call_remind_info);
                callInfo.setText(R.string.rc_voip_video_call_inviting);
                onIncomingCallRinging();
                ImageView answerV = (ImageView) buttonLayout.findViewById(R.id.rc_voip_call_answer_btn);
                answerV.setImageResource(R.drawable.rc_voip_vedio_answer_selector);
            }
        }
        mButtonContainer.removeAllViews();
        mButtonContainer.addView(buttonLayout);
        mUserInfoContainer.removeAllViews();
        mUserInfoContainer.addView(userInfoLayout);
    }

    public void onCallOutgoing() {

        mLocalVideo = RtcEngine.CreateRendererView(getBaseContext());
        mLocalVideo.setZOrderMediaOverlay(true);
        mRtcEngine.setupLocalVideo(new VideoCanvas(mLocalVideo, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));

        if (mediaType.equals(IMCallCommon.CallMediaType.VIDEO)) {
            mLPreviewContainer.setVisibility(View.VISIBLE);
            mLocalVideo.setTag(selfUserId);
            mLPreviewContainer.addView(mLocalVideo);
        }

        onOutgoingCallRinging();
        joinChannel();
    }

    //已连接
    public void onCallConnected() {

        stopRing();
        TextView remindInfo = (TextView) mUserInfoContainer.findViewById(R.id.rc_voip_call_remind_info);
        setupTime(remindInfo);

        if (mediaType.equals(IMCallCommon.CallMediaType.AUDIO)) {
            findViewById(R.id.rc_voip_call_minimize).setVisibility(View.VISIBLE);
            FrameLayout btnLayout = (FrameLayout) inflater.inflate(R.layout.rc_voip_call_bottom_connected_button_layout, null);
            mButtonContainer.removeAllViews();
            mButtonContainer.addView(btnLayout);
        } else {
          //  mLocalVideo = localVideo;
            mLocalVideo.setTag(selfUserId);
        }

        mRtcEngine.muteLocalAudioStream(muted);
        View muteV = mButtonContainer.findViewById(R.id.rc_voip_call_mute);
        if (muteV != null) {
            muteV.setSelected(muted);
        }

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager.isWiredHeadsetOn()) {
            mRtcEngine.setEnableSpeakerphone(false);
        } else {
            mRtcEngine.setEnableSpeakerphone(handFree);
        }
        View handFreeV = mButtonContainer.findViewById(R.id.rc_voip_handfree);
        if (handFreeV != null) {
            handFreeV.setSelected(handFree);
        }

    }

    public void onRemoteUserJoined(final int userId, IMCallCommon.CallMediaType mediaType,SurfaceView remoteVideo) {

        if (mediaType.equals(IMCallCommon.CallMediaType.VIDEO)) {
            findViewById(R.id.rc_voip_call_information).setBackgroundColor(getResources().getColor(android.R.color.transparent));
            mLPreviewContainer.setVisibility(View.VISIBLE);
            mLPreviewContainer.removeAllViews();
            remoteVideo.setTag(userId+"");

            mLPreviewContainer.addView(remoteVideo);
            mLPreviewContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isInformationShow) {
                        hideVideoCallInformation();
                    } else {
                        showVideoCallInformation();
                        handler.sendEmptyMessageDelayed(EVENT_FULL_SCREEN, 5 * 1000);
                    }
                }
            });



            mSPreviewContainer.setVisibility(View.VISIBLE);
            mSPreviewContainer.removeAllViews();
            if (mLocalVideo != null) {
                mLocalVideo.setZOrderMediaOverlay(true);
                mLocalVideo.setZOrderOnTop(true);
                mSPreviewContainer.addView(mLocalVideo);
            }
            mSPreviewContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SurfaceView fromView = (SurfaceView) mSPreviewContainer.getChildAt(0);
                    SurfaceView toView = (SurfaceView) mLPreviewContainer.getChildAt(0);

                    mLPreviewContainer.removeAllViews();
                    mSPreviewContainer.removeAllViews();
                    fromView.setZOrderOnTop(false);
                    fromView.setZOrderMediaOverlay(false);
                    mLPreviewContainer.addView(fromView);
                    toView.setZOrderOnTop(true);
                    toView.setZOrderMediaOverlay(true);
                    mSPreviewContainer.addView(toView);
                }
            });
            mButtonContainer.setVisibility(View.GONE);
            mUserInfoContainer.setVisibility(View.GONE);
        }
    }

    public void onMediaTypeChanged(String userId, IMCallCommon.CallMediaType mediaType, SurfaceView video) {
//        if (callSession.getSelfUserId().equals(userId)) {
//            showShortToast(getString(R.string.rc_voip_switched_to_audio));
//        } else {
//            if (callSession.getMediaType() != RongCallCommon.CallMediaType.AUDIO) {
//                RongCallClient.getInstance().changeCallMediaType(RongCallCommon.CallMediaType.AUDIO);
//                callSession.setMediaType(RongCallCommon.CallMediaType.AUDIO);
//                showShortToast(getString(R.string.rc_voip_remote_switched_to_audio));
//            }
//        }
        this.mediaType=mediaType;
        initAudioCallView();
        handler.removeMessages(EVENT_FULL_SCREEN);
        mButtonContainer.findViewById(R.id.rc_voip_call_mute).setSelected(muted);
    }

    private void initAudioCallView() {
        mLPreviewContainer.removeAllViews();
        mLPreviewContainer.setVisibility(View.GONE);
        mSPreviewContainer.removeAllViews();
        mSPreviewContainer.setVisibility(View.GONE);

        findViewById(R.id.rc_voip_call_information).setBackgroundColor(getResources().getColor(R.color.rc_voip_background_color));
        findViewById(R.id.rc_voip_audio_chat).setVisibility(View.GONE);

        View userInfoView = inflater.inflate(R.layout.rc_voip_audio_call_user_info, null);
        TextView timeView = (TextView) userInfoView.findViewById(R.id.rc_voip_call_remind_info);
        setupTime(timeView);

        mUserInfoContainer.removeAllViews();
        mUserInfoContainer.addView(userInfoView);

        User userinfo=imService.getContactManager().findContact(tageruserId);
        if (userinfo != null) {

            TextView userName = (TextView) mUserInfoContainer.findViewById(R.id.rc_voip_user_name);
            userName.setText(userinfo.getMainName());
            if (mediaType.equals(IMCallCommon.CallMediaType.AUDIO)) {

                IMBaseImageView portraitImageView = (IMBaseImageView) mUserInfoContainer.findViewById(R.id.rc_voip_user_portrait);

                if (portraitImageView != null && userinfo.getAvatar() != null) {
                    //头像设置
                    portraitImageView.setDefaultImageRes(R.drawable.tt_default_user_portrait_corner);
                    portraitImageView.setCorner(15);
                    portraitImageView.setImageResource(R.drawable.tt_default_user_portrait_corner);
                    portraitImageView.setImageUrl(userinfo.getAvatar());
                }
            }
        }

        mUserInfoContainer.setVisibility(View.VISIBLE);
        mUserInfoContainer.findViewById(R.id.rc_voip_call_minimize).setVisibility(View.VISIBLE);

        View button = inflater.inflate(R.layout.rc_voip_call_bottom_connected_button_layout, null);
        mButtonContainer.removeAllViews();
        mButtonContainer.addView(button);
        mButtonContainer.setVisibility(View.VISIBLE);
        View handFreeV = mButtonContainer.findViewById(R.id.rc_voip_handfree);
        handFreeV.setSelected(handFree);

        if (pickupDetector != null) {
            pickupDetector.register(this);
        }
    }

    public void onHangupBtnClick(View view) {
//        RongCallSession session = RongCallClient.getInstance().getCallSession();
//        if (session == null || isFinishing) {
//            finish();
//            return;
//        }
//        RongCallClient.getInstance().hangUpCall(session.getCallId());

        if(isconnect) {
            stopRing();
            end();
        }
        else
        {
            stopRing();
            imService.getMessageManager().sendVideoMessage(tageruserId,selfUserId,66660,roomid);  //拒绝接听
            end();
        }
    }

    public void onReceiveBtnClick(View view) {   //同意进行视频通话

        stopRing();//先停止铃声。
        joinChannel();

    }
    void onMinimizeClick(View view) {
        if (Build.BRAND.toLowerCase().contains("xiaomi")
                || Build.VERSION.SDK_INT >= 23) {
//            if (PermissionCheckUtil.canDrawOverlays(this)) {
//                finish();
//            } else {
//                Toast.makeText(this, R.string.rc_voip_float_window_not_allowed, Toast.LENGTH_LONG).show();
//            }
        } else {
            finish();
        }
    }

    public void onOutgoingCallRinging() {
        mMediaPlayer = MediaPlayer.create(this, R.raw.voip_outgoing_ring);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();
    }


    public static int getRingerMode(Context context) {
        @SuppressLint("WrongConstant") AudioManager audio = (AudioManager)context.getSystemService("audio");
        return audio.getRingerMode();
    }

    public void hideVideoCallInformation() {
        isInformationShow = false;
        mUserInfoContainer.setVisibility(View.GONE);
        mButtonContainer.setVisibility(View.GONE);

        findViewById(R.id.rc_voip_audio_chat).setVisibility(View.GONE);
    }

    public void showVideoCallInformation() {
        isInformationShow = true;
        mUserInfoContainer.setVisibility(View.VISIBLE);
        mUserInfoContainer.findViewById(R.id.rc_voip_call_minimize).setVisibility(View.VISIBLE);
        mButtonContainer.setVisibility(View.VISIBLE);
        FrameLayout btnLayout = (FrameLayout) inflater.inflate(R.layout.rc_voip_call_bottom_connected_button_layout, null);
        btnLayout.findViewById(R.id.rc_voip_call_mute).setSelected(muted);
        btnLayout.findViewById(R.id.rc_voip_handfree).setVisibility(View.GONE);
        btnLayout.findViewById(R.id.rc_voip_camera).setVisibility(View.VISIBLE);
        mButtonContainer.removeAllViews();
        mButtonContainer.addView(btnLayout);
        View view = findViewById(R.id.rc_voip_audio_chat);
        view.setVisibility(View.VISIBLE);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                RongCallClient.getInstance().changeCallMediaType(RongCallCommon.CallMediaType.AUDIO);
                mediaType=IMCallCommon.CallMediaType.AUDIO;
                initAudioCallView();
            }
        });
    }

    public void onHandFreeButtonClick(View view) {
        mRtcEngine.setEnableSpeakerphone(!view.isSelected());
        view.setSelected(!view.isSelected());
        handFree = view.isSelected();
    }

    public void onMuteButtonClick(View view) {
        mRtcEngine.muteLocalAudioStream(!view.isSelected());
        view.setSelected(!view.isSelected());
        muted = view.isSelected();
    }

    public void onIncomingCallRinging() {
        int ringerMode = getRingerMode(this);
        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                mVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                mVibrator.vibrate(new long[]{500, 1000}, 0);
            } else {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                mMediaPlayer = new MediaPlayer();
                try {
                    mMediaPlayer.setDataSource(this, uri);
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void setupTime(final TextView timeView) {
        if (updateTimeRunnable != null) {
            handler.removeCallbacks(updateTimeRunnable);
        }
        updateTimeRunnable = new UpdateTimeRunnable(timeView);
        handler.post(updateTimeRunnable);
    }

    private class UpdateTimeRunnable implements Runnable {
        private TextView timeView;

        public UpdateTimeRunnable(TextView timeView) {
            this.timeView = timeView;
        }

        @Override
        public void run() {
            time++;
            if (time >= 3600) {
                timeView.setText(String.format("%d:%02d:%02d", time / 3600, (time % 3600) / 60, (time % 60)));
            } else {
                timeView.setText(String.format("%02d:%02d", (time % 3600) / 60, (time % 60)));
            }
            handler.postDelayed(this, 1000);
        }
    }
    public long getTime() {
        return time;
    }

    public void stopRing() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer = null;
        }
        if (mVibrator != null) {
            mVibrator.cancel();
            mVibrator = null;
        }
    }

    protected void createPickupDetector() {
        if (pickupDetector == null) {
            pickupDetector = new PickupDetector(this);
        }
    }
    @Override
    public void onPickupDetected(boolean isPickingUp) {
        if (wakeLock == null) {
            return;
        }
        if (isPickingUp && !wakeLock.isHeld()) {
            setShouldShowFloat(false);
            shouldRestoreFloat = false;
            wakeLock.acquire();
        }
        if (!isPickingUp && wakeLock.isHeld()) {
            try {
                wakeLock.setReferenceCounted(false);
                wakeLock.release();
                setShouldShowFloat(true);
                shouldRestoreFloat = true;
            } catch (Exception e) {

            }
        }
    }
    public void setShouldShowFloat(boolean shouldShowFloat) {
        this.shouldShowFloat = shouldShowFloat;
    }
    protected void createPowerManager() {
        if (powerManager == null) {
            powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "");
        }
    }


    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();     // Tutorial Step 1
        setupVideoProfile();         // Tutorial Step 2
        setupLocalVideo();           // Tutorial Step 3
        joinChannel();               // Tutorial Step 4
    }



    public final void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRing();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.setReferenceCounted(false);
            wakeLock.release();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (pickupDetector != null && mediaType.equals(IMCallCommon.CallMediaType.AUDIO)) {
            pickupDetector.register(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pickupDetector != null) {
            pickupDetector.unRegister();
        }
    }

    // Tutorial Step 10
    public void onLocalVideoMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.muteLocalVideoStream(iv.isSelected());

        FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);
        surfaceView.setZOrderMediaOverlay(!iv.isSelected());
        surfaceView.setVisibility(iv.isSelected() ? View.GONE : View.VISIBLE);
    }

    // Tutorial Step 9
    public void onLocalAudioMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.muteLocalAudioStream(iv.isSelected());
    }
    public void onSwitchCameraClick(View view) {
        mRtcEngine.switchCamera();
    }

    // Tutorial Step 6
    public void onEncCallClicked(View view) {
        end();
    }

    public void end(){

        if(isconnect) {
            showToast(R.string.video_endof);
            imService.getMessageManager().sendVideoMessage(tageruserId, selfUserId, 66669, roomid);  //告诉对方。通话已结束
        }

        imServiceConnector.disconnect(this);
        EventBus.getDefault().unregister(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    leaveChannel();
                    RtcEngine.destroy();
                    mRtcEngine = null;
                } catch (Exception e) {
                }
            }
        }).start();

        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserEvent(PriorityEvent event){
        switch (event.event){
            case MSG_END_VIDEO: {
                end();
            }
            break;
            case MSG_REJECT_VIDEO:
            {
                showToast(R.string.video_reject);
                end();
            }
        }
    }
    // Tutorial Step 1
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    // Tutorial Step 2
    private void setupVideoProfile() {
        mRtcEngine.enableVideo();
        mRtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, false);
    }

    // Tutorial Step 3
    private void setupLocalVideo() {
        FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));
    }

    // Tutorial Step 4
    private void joinChannel() {
        mRtcEngine.joinChannel(null, roomid, "", 0); // if you do not specify the uid, we will generate the uid for you
    }

    // Tutorial Step 5
    private void setupRemoteVideo(int uid) {
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);

        if (container.getChildCount() >= 1) {
            return;
        }

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));

        surfaceView.setTag(uid); // for mark purpose
    }

    // Tutorial Step 6
    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    // Tutorial Step 7
    private void onRemoteUserLeft() {
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);
        container.removeAllViews();

    }

    // Tutorial Step 10
    private void onRemoteUserVideoMuted(int uid, boolean muted) {
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);

        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);

        Object tag = surfaceView.getTag();
        if (tag != null && (Integer) tag == uid) {
            surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
        }
    }


}
