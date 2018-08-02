package com.emcsthai.mobile.webrtc;

import android.Manifest;
import android.os.Bundle;
import android.support.percent.PercentFrameLayout;
import android.support.percent.PercentLayoutHelper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.util.List;

public class VideoCallActivity extends AppCompatActivity {

    private final static String TAG = VideoCallActivity.class.getCanonicalName();

    private EglBase rootEglBase;

    private RelativeLayout rootLayout;
    private ImageView imgScreenType;

    private SurfaceViewRenderer remoteSurfaceView;
    private SurfaceViewRenderer localSurfaceView;

    private CardView cardView;

    private ImageView imgHangUp;
    private ImageView imgSwitchCamera;
    private ImageView imgMute;

    private WebRTCClient webRTCClient;

    private String roomId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        initWidgets();

        if (getIntent().hasExtra(MainActivity.KEY_ROOM_ID)) {
            roomId = getIntent().getStringExtra(MainActivity.KEY_ROOM_ID);

            Dexter.withActivity(this)
                    .withPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                // Method from this class
                                initSurfaceViews();

                                webRTCClient = new WebRTCClient(getApplicationContext(), roomId, rootEglBase, onWebRTCClientListener);
                            }

                            if (report.isAnyPermissionPermanentlyDenied()) {
                                Toast.makeText(getApplicationContext(), "Go to Setting", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                            token.continuePermissionRequest();
                        }

                    }).withErrorListener(error -> {
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();

            }).onSameThread()
                    .check();
        }

        rootLayout.setOnClickListener(onClickListener);
        imgScreenType.setOnClickListener(onClickListener);
        imgHangUp.setOnClickListener(onClickListener);
        imgSwitchCamera.setOnClickListener(onClickListener);
        imgMute.setOnClickListener(onClickListener);
    }

    @Override
    public void onBackPressed() {
        dialogHandUp();
    }

    private void initWidgets() {
        rootLayout = findViewById(R.id.rootLayout);
        imgScreenType = findViewById(R.id.imgScreenType);

        remoteSurfaceView = findViewById(R.id.remoteSurfaceView);
        localSurfaceView = findViewById(R.id.localSurfaceView);

        cardView = findViewById(R.id.cardView);

        imgHangUp = findViewById(R.id.imgHangUp);
        imgSwitchCamera = findViewById(R.id.imgSwitchCamera);
        imgMute = findViewById(R.id.imgMute);
    }

    private void initSurfaceViews() {
        rootEglBase = EglBase.create();

        remoteSurfaceView.init(rootEglBase.getEglBaseContext(), null);
        remoteSurfaceView.setEnableHardwareScaler(true);
        remoteSurfaceView.setMirror(true);

        localSurfaceView.init(rootEglBase.getEglBaseContext(), null);
        localSurfaceView.setEnableHardwareScaler(true);
        localSurfaceView.setMirror(true);
    }

    private void updateResetVideoView() {
        runOnUiThread(() -> {
            PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) remoteSurfaceView.getLayoutParams();
            params.gravity = -1;
            PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
            info.heightPercent = 1.00f;
            info.widthPercent = 1.00f;
            info.leftMarginPercent = 0.00f;
            info.topMarginPercent = 0.00f;
            remoteSurfaceView.requestLayout();

            params = (PercentFrameLayout.LayoutParams) localSurfaceView.getLayoutParams();
            params.gravity = -1;
            info = params.getPercentLayoutInfo();
            info.heightPercent = 1.00f;
            info.widthPercent = 1.00f;
            info.leftMarginPercent = 0.00f;
            info.topMarginPercent = 0.00f;
            localSurfaceView.requestLayout();
        });
    }

    private void updateLocalSmallVideoView() {
        // Method from this class
        updateResetVideoView();
        runOnUiThread(() -> {
            PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) localSurfaceView.getLayoutParams();
            PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
            info.heightPercent = 0.30f;
            info.widthPercent = 0.35f;
            info.leftMarginPercent = 0.60f;
            info.topMarginPercent = 0.65f;
            localSurfaceView.requestLayout();

            localSurfaceView.requestFocus();
            localSurfaceView.invalidate();
            remoteSurfaceView.invalidate();
            remoteSurfaceView.requestLayout();
            localSurfaceView.requestLayout();


            windowType = WINDOW_TYPE.LOCAL_SMALL;
        });
    }

    private void updateRemoteSmallVideoView() {
        // Method from this class
        updateResetVideoView();
        runOnUiThread(() -> {
            PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) remoteSurfaceView.getLayoutParams();
            PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
            info.heightPercent = 0.30f;
            info.widthPercent = 0.35f;
            info.leftMarginPercent = 0.60f;
            info.topMarginPercent = 0.65f;
            remoteSurfaceView.requestLayout();

            remoteSurfaceView.requestFocus();
            localSurfaceView.invalidate();
            remoteSurfaceView.invalidate();
            remoteSurfaceView.requestLayout();
            localSurfaceView.requestLayout();

            windowType = WINDOW_TYPE.REMOTE_SMALL;
        });
    }

    private void updateSplitHalfVideoView() {
        // Method from this class
        updateResetVideoView();
        runOnUiThread(() -> {
            PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) remoteSurfaceView.getLayoutParams();
            PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
            info.heightPercent = 0.50f;
            info.widthPercent = 1.00f;
            info.leftMarginPercent = 0.00f;
            info.topMarginPercent = 0.00f;
            remoteSurfaceView.requestLayout();

            params = (PercentFrameLayout.LayoutParams) localSurfaceView.getLayoutParams();
            params.gravity = Gravity.BOTTOM;
            info = params.getPercentLayoutInfo();
            info.heightPercent = 0.50f;
            info.widthPercent = 1.00f;
            info.leftMarginPercent = 0.00f;
            info.topMarginPercent = 0.00f;
            localSurfaceView.requestLayout();

            windowType = WINDOW_TYPE.SPLIT_HALF;
        });
    }

//    private void updateMoveToBottomVideoView(SurfaceViewRenderer selectSurfaceView) {
//        PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) selectSurfaceView.getLayoutParams();
//        PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
//        info.heightPercent = 0.30f;
//        info.widthPercent = 0.35f;
//        info.leftMarginPercent = 0.60f;
//        info.topMarginPercent = 0.65f;
//        selectSurfaceView.requestLayout();
//    }
//
//    private void updateMoveToTopVideoView(SurfaceViewRenderer selectSurfaceView) {
//        PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) selectSurfaceView.getLayoutParams();
//        PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
//        info.heightPercent = 0.30f;
//        info.widthPercent = 0.35f;
//        info.leftMarginPercent = 0.60f;
//        info.topMarginPercent = 0.55f;
//        selectSurfaceView.requestLayout();
//    }

    private void dialogHandUp() {
        new MaterialDialog.Builder(this)
                .title("Hand up")
                .content("Do you want hand up?")
                .positiveText("Yes")
                .negativeText("No")
                .onPositive((dialog, which) -> {
                    dialog.dismiss();
                    handUp();
                    finish();
                })
                .onNegative((dialog, which) -> {
                    dialog.dismiss();
                }).show();
    }

    private void handUp() {
        if (localSurfaceView != null) {
            localSurfaceView.release();
            localSurfaceView = null;
        }

        if (remoteSurfaceView != null) {
            remoteSurfaceView.release();
            remoteSurfaceView = null;
        }

        if (rootEglBase != null) {
            rootEglBase.release();
            rootEglBase = null;
        }

        if (webRTCClient != null) {
            webRTCClient.disconnect();
            webRTCClient = null;
        }

        Toast.makeText(this, "Hang up finished", Toast.LENGTH_SHORT).show();
    }

    /****************************************************************************
     ******************************* Listener ***********************************
     ****************************************************************************/

    private boolean isMute = true;
    private boolean isShow = true;
    private WINDOW_TYPE windowType = WINDOW_TYPE.LOCAL_SMALL;

    private enum WINDOW_TYPE {
        LOCAL_SMALL, REMOTE_SMALL, SPLIT_HALF
    }

    private View.OnClickListener onClickListener = v -> {

        if (v == rootLayout) {
            if (!isShow) {
                cardView.setVisibility(View.VISIBLE);
                imgScreenType.setVisibility(View.VISIBLE);
//                updateMoveToTopVideoView(localSurfaceView);
                isShow = true;
            } else {
                cardView.setVisibility(View.GONE);
                imgScreenType.setVisibility(View.GONE);
//                updateMoveToBottomVideoView(localSurfaceView);
                isShow = false;
            }
        }

        if (v == imgScreenType) {
            switch (windowType) {
                case LOCAL_SMALL:
                    updateRemoteSmallVideoView();
                    break;
                case REMOTE_SMALL:
                    updateSplitHalfVideoView();
                    break;
                case SPLIT_HALF:
                    updateLocalSmallVideoView();
                    break;
            }
        }

        if (v == imgHangUp) {
            dialogHandUp();
        }

        if (v == imgSwitchCamera) {
            webRTCClient.switchCamera();
        }

        if (v == imgMute) {
            if (!isMute) {
                webRTCClient.microphoneOn();
                imgMute.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.ic_mic_on));
                isMute = true;
            } else {
                webRTCClient.microphoneOff();
                imgMute.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.ic_mic_off));
                isMute = false;
            }
        }
    };

//    private float dX, dY;
//
//    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
//
//        @Override
//        public boolean onTouch(View v, MotionEvent event) {
//            switch (event.getAction() & MotionEvent.ACTION_MASK) {
//                case MotionEvent.ACTION_DOWN:
//                    dX = localSurfaceView.getX() - event.getRawX();
//                    dY = localSurfaceView.getY() - event.getRawY();
//                    break;
//
//                case MotionEvent.ACTION_MOVE:
//                    float x = event.getRawX() + dX;
//                    float y = event.getRawY() + dY;
//                    v.animate()
//                            .x(x)
//                            .y(y)
//                            .setDuration(0)
//                            .start();
//                    break;
//                default:
//                    return false;
//            }
//            return true;
//        }
//    };

    private WebRTCClient.OnWebRTCClientListener onWebRTCClientListener = new WebRTCClient.OnWebRTCClientListener() {

        @Override
        public void onCallReady(String callId) {
        }

        @Override
        public void onLocalStream(MediaStream mediaStream) {
            Log.i(TAG, "onLocalStream");
            VideoRenderer videoRenderer = new VideoRenderer(localSurfaceView);
            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.addRenderer(videoRenderer);
        }

        @Override
        public void onRemoteStream(MediaStream mediaStream) {
            Log.d(TAG, "onRemoteStream");
            VideoRenderer videoRenderer = new VideoRenderer(remoteSurfaceView);

            AudioTrack audioTrack = mediaStream.audioTracks.get(0);
            audioTrack.setVolume(10);

            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.addRenderer(videoRenderer);

            // Method from this class
            updateLocalSmallVideoView();
        }

        @Override
        public void onRemoteHangUp() {
            Toast.makeText(getApplicationContext(), "Remote HangUp", Toast.LENGTH_SHORT).show();
            finish();
//            webRTCClient.hangUp();
        }
    };
}