package com.emcs.nakharin.webrtc;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.emcs.nakharin.webrtc.model.EventCapture;
import com.emcs.nakharin.webrtc.model.EventDrawing;
import com.irozon.alertview.AlertActionStyle;
import com.irozon.alertview.AlertStyle;
import com.irozon.alertview.AlertView;
import com.irozon.alertview.objects.AlertAction;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

import java.util.List;

public class VideoCallActivity extends AppCompatActivity {

    private HeadSetStateReceiver headSetStateReceiver;

    private EglBase rootEglBase;

    private ConstraintSet consSetHalf = new ConstraintSet();
    private ConstraintSet consSetLocal = new ConstraintSet();
    private ConstraintSet consSetRemote = new ConstraintSet();

    private ConstraintLayout rootLayout;

    private ImageView imgScreenType;

    private SurfaceViewRenderer remoteSurfaceView;
    private SurfaceViewRenderer localSurfaceView;

    private CardView cardView;

    private ImageView imgHangUp;
    private ImageView imgSwitchCamera;
    private ImageView imgMute;

    private WebRTCClient webRTCClient;

    private String mRoomId = "";
    private String mCallId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call_remote_screen);

        headSetStateReceiver = new HeadSetStateReceiver();

        initWidgets();

        initConstraintSet();

        if (getIntent().hasExtra(Constants.KEY_ROOM_ID)) {
            mRoomId = getIntent().getStringExtra(Constants.KEY_ROOM_ID);

            Dexter.withActivity(this)
                    .withPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                // Method from this class
                                initSurfaceViews();

                                webRTCClient = new WebRTCClient(VideoCallActivity.this, mRoomId, rootEglBase, onWebRTCClientListener);
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
    protected void onResume() {
        super.onResume();

        if (headSetStateReceiver != null) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
            registerReceiver(headSetStateReceiver, filter);
        }

        if (webRTCClient != null) {
            webRTCClient.startPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (headSetStateReceiver != null) {
            unregisterReceiver(headSetStateReceiver);
        }

        if (webRTCClient != null) {
            webRTCClient.stopPreview();
        }
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

        remoteSurfaceView.init(rootEglBase.getEglBaseContext(), new CustomRendererEvents("remote"));
        remoteSurfaceView.setEnableHardwareScaler(true);
        remoteSurfaceView.setMirror(false);

        localSurfaceView.init(rootEglBase.getEglBaseContext(), new CustomRendererEvents("local"));
        localSurfaceView.setEnableHardwareScaler(true);
        localSurfaceView.setMirror(true);
    }

    private void initConstraintSet() {
        consSetRemote.clone(rootLayout);
        consSetLocal.load(this, R.layout.activity_video_call_local_screen);
        consSetHalf.load(this, R.layout.activity_video_call_half_screen);
    }

    private void sendViewToBack(@NonNull View child) {
        final ViewGroup parent = (ViewGroup) child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    private void updateSplitHalfVideoView() {
        consSetHalf.applyTo(rootLayout);
        windowType = WINDOW_TYPE.LOCAL_SMALL;

        localSurfaceView.setZOrderOnTop(false);
        remoteSurfaceView.setZOrderOnTop(false);
    }

    private void updateLocalSmallVideoView() {
        consSetLocal.applyTo(rootLayout);
        windowType = WINDOW_TYPE.REMOTE_SMALL;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            sendViewToBack(remoteSurfaceView);
        } else {
            remoteSurfaceView.setZOrderOnTop(false);
            localSurfaceView.setZOrderOnTop(true);
            localSurfaceView.setZOrderMediaOverlay(true);
        }
    }

    private void updateRemoteSmallVideoView() {
        consSetRemote.applyTo(rootLayout);
        windowType = WINDOW_TYPE.SPLIT_HALF;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            sendViewToBack(localSurfaceView);
        } else {
            localSurfaceView.setZOrderOnTop(false);
            remoteSurfaceView.setZOrderOnTop(true);
            remoteSurfaceView.setZOrderMediaOverlay(true);
        }
    }

    private void dialogHandUp() {
        AlertView alert = new AlertView("Hand up", "Do you want hand up?", AlertStyle.BOTTOM_SHEET);
        alert.addAction(new AlertAction("Yes", AlertActionStyle.POSITIVE, action -> {
            handUp();
            finish();
        }));
        alert.addAction(new AlertAction("No", AlertActionStyle.NEGATIVE, action -> {

        }));

        alert.show(this);
    }

    private void handUp() {

        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), "Video Call ended", Toast.LENGTH_SHORT).show();
        });

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

    private final View.OnClickListener onClickListener = v -> {

        if (v == rootLayout) {
            if (!isShow) {
                cardView.setVisibility(View.VISIBLE);
                imgScreenType.setVisibility(View.VISIBLE);
                isShow = true;
            } else {
                cardView.setVisibility(View.GONE);
                imgScreenType.setVisibility(View.GONE);
                isShow = false;
            }
        }

        if (v == imgScreenType) {
            switch (windowType) {
                case SPLIT_HALF:
                    updateSplitHalfVideoView();
                    break;
                case LOCAL_SMALL:
                    updateLocalSmallVideoView();
                    break;
                case REMOTE_SMALL:
                    updateRemoteSmallVideoView();
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

    private final DialogViewPhoto.OnDrawingTouchListener onDrawingTouchListener = new DialogViewPhoto.OnDrawingTouchListener() {
        @Override
        public void onDrawingTouch(float startX, float startY, float moveX, float moveY) {
            webRTCClient.emitDrawing(startX, startY, moveX, moveY);
        }
    };

    private DialogViewPhoto dialog = null;

    private final WebRTCClient.OnWebRTCClientListener onWebRTCClientListener = new WebRTCClient.OnWebRTCClientListener() {

        @Override
        public void onCallReady(String callId) {
            mCallId = callId;
        }

        @Override
        public void onLocalStream(MediaStream mediaStream) {

            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            VideoSink videoSink = videoFrame -> {
                if (videoFrame != null) {
                    if (localSurfaceView != null) {
                        localSurfaceView.onFrame(videoFrame);
                    }
                }
            };

            videoTrack.addSink(videoSink);
        }

        @Override
        public void onRemoteStream(MediaStream mediaStream) {

            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            VideoSink videoSink = videoFrame -> {
                if (videoFrame != null) {
                    if (remoteSurfaceView != null) {
                        remoteSurfaceView.onFrame(videoFrame);
                    }
                }
            };

            videoTrack.addSink(videoSink);

            runOnUiThread(() -> {
                TransitionManager.beginDelayedTransition(rootLayout);
                updateLocalSmallVideoView();
            });
        }

        @Override
        public void onReceiveImage(EventCapture eventCapture) {
            runOnUiThread(() -> {
                try {
                    if (dialog == null) {
                        dialog = DialogViewPhoto.Companion.newInstance(eventCapture.getData(), false);
                        dialog.setOnDrawingTouchListener(onDrawingTouchListener);
                        dialog.show(getSupportFragmentManager(), "dialog");
                    } else {
                        dialog.dismiss();
                        dialog = DialogViewPhoto.Companion.newInstance(eventCapture.getData(), false);
                        dialog.setOnDrawingTouchListener(onDrawingTouchListener);
                        dialog.show(getSupportFragmentManager(), "dialog");
                    }

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void onHangUp() {
            handUp();
            finish();
        }

        @Override
        public void onCameraSwitchDone(boolean isFrontSide) {
            runOnUiThread(() -> {
                localSurfaceView.setMirror(isFrontSide);
            });
        }

        @Override
        public void onCameraSwitchError(String error) {
            runOnUiThread(() -> {
                Toast.makeText(VideoCallActivity.this, "onCameraSwitchError: " + error, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onDrawingImage(EventDrawing eventDrawing) {
            if (dialog != null) {
                dialog.setDrawingPoint(eventDrawing);
            }
        }
    };
}
