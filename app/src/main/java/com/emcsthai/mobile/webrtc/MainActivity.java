package com.emcsthai.mobile.webrtc;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
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

    private SurfaceViewRenderer remoteSurfaceView;
    private SurfaceViewRenderer localSurfaceView;

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

        remoteSurfaceView = findViewById(R.id.remoteSurfaceView);
        localSurfaceView = findViewById(R.id.localSurfaceView);

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

    private void updateLocalVideoViews() {
        runOnUiThread(() -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) localSurfaceView.getLayoutParams();
            params.height = dpToPx(220);
            params.width = dpToPx(150);
            params.rightMargin = dpToPx(8);
            params.bottomMargin = dpToPx(80);

            localSurfaceView.setLayoutParams(params);
        });
    }

    /****************************************************************************
     ******************************* Listener ***********************************
     ****************************************************************************/

    boolean isMute = false;

    private View.OnClickListener onClickListener = v -> {
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

    private WebRTCClient.OnWebRTCClientListener onWebRTCClientListener = new WebRTCClient.OnWebRTCClientListener() {
        @Override
        public void onCallReady(String callId) {
        }

        @Override
        public void onLocalStream(MediaStream mediaStream) {
            VideoRenderer videoRenderer = new VideoRenderer(localSurfaceView);
            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.addRenderer(videoRenderer);
        }

        @Override
        public void onRemoteStream(MediaStream mediaStream) {
            VideoRenderer videoRenderer = new VideoRenderer(remoteSurfaceView);
            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.addRenderer(videoRenderer);

            // Method from this class
            updateLocalVideoViews();
        }

        @Override
        public void onRemoteHangUp() {
            webRTCClient.hangUp();
        }
    };
}
