package com.emcsthai.mobile.webrtc;

import android.Manifest;
import android.os.Bundle;
import android.support.percent.PercentFrameLayout;
import android.support.percent.PercentLayoutHelper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EglBase rootEglBase;

    private FrameLayout rootLayout;
    private ImageView imgScreenType;

    private SurfaceViewRenderer remoteSurfaceView;
    private SurfaceViewRenderer localSurfaceView;

    private CardView cardView;

    private ImageView imgHangUp;
    private ImageView imgSwitchCamera;
    private ImageView imgMute;

    private WebRTCClient webRTCClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initWidgets();

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            // Method from this class
                            initSurfaceViews();

                            webRTCClient = new WebRTCClient(MainActivity.this, rootEglBase, onWebRTCClientListener);
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

        rootLayout.setOnClickListener(onClickListener);
        imgScreenType.setOnClickListener(onClickListener);
        imgHangUp.setOnClickListener(onClickListener);
        imgSwitchCamera.setOnClickListener(onClickListener);
        imgMute.setOnClickListener(onClickListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webRTCClient.close();
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

    /**
     * Util Methods
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void updateLocalSmallVideoView() {
        runOnUiThread(() -> {

            PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) localSurfaceView.getLayoutParams();
            params.height = dpToPx(220);
            params.width = dpToPx(150);
            params.rightMargin = 0;
            params.bottomMargin = 0;

            localSurfaceView.setLayoutParams(params);

            cardView.setVisibility(View.GONE);
            isShow = false;

            localSurfaceView.setOnTouchListener(onTouchListener);
            remoteSurfaceView.setOnTouchListener(null);
        });
    }

    private void updateRemoteSmallVideoView() {
        runOnUiThread(() -> {

            PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) remoteSurfaceView.getLayoutParams();
            params.height = dpToPx(220);
            params.width = dpToPx(150);
            params.rightMargin = 8;
            params.bottomMargin = 20;

            remoteSurfaceView.setLayoutParams(params);

            cardView.setVisibility(View.GONE);
            isShow = false;

            localSurfaceView.setOnTouchListener(null);
            remoteSurfaceView.setOnTouchListener(onTouchListener);
        });
    }

    private void updatePercentVideoView() {
        runOnUiThread(() -> {

            PercentFrameLayout.LayoutParams params1 = (PercentFrameLayout.LayoutParams) localSurfaceView.getLayoutParams();
            params1.height = dpToPx(220);
            params1.width = dpToPx(150);
            params1.rightMargin = 0;
            params1.bottomMargin = 0;

            localSurfaceView.setLayoutParams(params1);


            localSurfaceView.setOnTouchListener(null);
            remoteSurfaceView.setOnTouchListener(null);

            PercentFrameLayout.LayoutParams params = (PercentFrameLayout.LayoutParams) remoteSurfaceView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            remoteSurfaceView.setLayoutParams(params);
            PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
            info.heightPercent = 0.50f;
            remoteSurfaceView.requestLayout();

            params = (PercentFrameLayout.LayoutParams) localSurfaceView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.rightMargin = 0;
            params.bottomMargin = 0;
            localSurfaceView.setLayoutParams(params);
            info = params.getPercentLayoutInfo();
            info.heightPercent = 0.50f;
            localSurfaceView.requestLayout();
        });
    }

    /****************************************************************************
     ******************************* Listener ***********************************
     ****************************************************************************/

    boolean isMute = false;
    boolean isShow = false;
    boolean isSmallToggle = false;

    private View.OnClickListener onClickListener = v -> {

        if (v == rootLayout) {
            if (!isShow) {
                cardView.setVisibility(View.VISIBLE);
                isShow = true;
            } else {
                cardView.setVisibility(View.GONE);
                isShow = false;
            }
        }

        if (v == imgScreenType) {
//            if (!isSmallToggle) {
//                updateRemoteSmallVideoView();
//                isSmallToggle = true;
//            } else {
//                updateLocalSmallVideoView();
//                isSmallToggle = false;
//            }

            updatePercentVideoView();
        }

        if (v == imgHangUp) {
            webRTCClient.hangUp();
        }

        if (v == imgSwitchCamera) {
            webRTCClient.switchCamera();
        }

        if (v == imgMute) {
            if (!isMute) {
                webRTCClient.muteOn();
                isMute = true;
            } else {
                webRTCClient.muteOff();
                isMute = true;
            }
        }
    };

    private float dX, dY;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    dX = localSurfaceView.getX() - event.getRawX();
                    dY = localSurfaceView.getY() - event.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    float x = event.getRawX() + dX;
                    float y = event.getRawY() + dY;
                    v.animate()
                            .x(x)
                            .y(y)
                            .setDuration(0)
                            .start();
                    break;
                default:
                    return false;
            }
            return true;
        }
    };

    private WebRTCClient.OnWebRTCClientListener onWebRTCClientListener = new WebRTCClient.OnWebRTCClientListener() {

        @Override
        public void onCallReady(String callId) {
        }

        @Override
        public void onLocalStream(MediaStream mediaStream) {
            VideoRenderer videoRenderer = new VideoRenderer(localSurfaceView);
            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.addRenderer(videoRenderer);

            // Method from this class
            updateLocalSmallVideoView();
        }

        @Override
        public void onRemoteStream(MediaStream mediaStream) {
            VideoRenderer videoRenderer = new VideoRenderer(remoteSurfaceView);
            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.addRenderer(videoRenderer);
        }

        @Override
        public void onRemoteHangUp() {
            webRTCClient.hangUp();
        }
    };
}
