package com.emcsthai.mobile.webrtc;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;

import static io.socket.client.Socket.EVENT_CONNECT;
import static io.socket.client.Socket.EVENT_DISCONNECT;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int VIDEO_RESOLUTION_WIDTH = 1280;
    public static final int VIDEO_RESOLUTION_HEIGHT = 720;
    public static final int FPS = 30;
    public static final String VIDEO_TRACK_ID = "100";
    public static final String AUDIO_TRACK_ID = "101";

    private Socket socket;

    private MediaConstraints pcConstraints = new MediaConstraints();

    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;

    private VideoTrack videoTrackFromCamera;
    private AudioTrack audioTrackFromCamera;
    private VideoCapturer videoCapturer;

    private EglBase rootEglBase;

    private SurfaceViewRenderer remoteSurfaceView;
    private SurfaceViewRenderer localSurfaceView;

    private ImageView imgHangUp;
    private ImageView imgSwitchCamera;
    private ImageView imgMute;

    private String id = "";

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
                            connectToSignallingServer();
                            // Method from this class
                            initSurfaceViews();
                            // Method from this class
                            initPeerConnectionFactory();
                            // Method from this class
                            createVideoTrackFromCameraAndShowIt();
                            // Method from this class
                            initPeerConnections();
                            // Method from this class
                            startStreamingVideo();
                            // Method from this class
                            startRoom();
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
        if (socket != null) {
            socket.disconnect();
        }
    }

    private void initWidgets() {
        remoteSurfaceView = findViewById(R.id.remoteSurfaceView);
        localSurfaceView = findViewById(R.id.localSurfaceView);

        imgHangUp = findViewById(R.id.imgHangUp);
        imgSwitchCamera = findViewById(R.id.imgSwitchCamera);
        imgMute = findViewById(R.id.imgMute);
    }

    private void connectToSignallingServer() {
        String webRTCUrl = getResources().getString(R.string.web_rtc_url);

        try {
            socket = IO.socket(webRTCUrl);

            socket.on(EVENT_CONNECT, args -> {
                Log.d(TAG, "connectToSignallingServer : connect");

            }).on("id", args -> {
                id = (String) args[0];
                Log.i(TAG, "id : " + webRTCUrl + id);

            }).on("message", args -> {
                Log.i(TAG, "message : " + Arrays.toString(args));
                JSONObject data = (JSONObject) args[0];
                try {
                    String from = data.getString("from");
                    String type = data.getString("type");

                    JSONObject payload = null;
                    if (!type.equals("init")) {
                        payload = data.getJSONObject("payload");
                    }

//                    if (from.equals(id)) {
                        if (type.equals("init")) {
                            createOfferPeerConnection(from);
                        }

                        if (type.equals("offer")) {
                            createAnswerPeerConnection(from, payload);
                        }

                        if (type.equals("answer")) {
                            SetRemoteSDPCommand(payload);
                        }

                        if (type.equals("candidate")) {
                            AddIceCandidateCommand(payload);
                        }
//                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }).on(EVENT_DISCONNECT, args -> {
                Log.d(TAG, "connectToSignallingServer: disconnect");
            });
            socket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "URISyntaxException : " + e.getMessage());
        }
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

    private void initPeerConnectionFactory() {
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        peerConnectionFactory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
    }

    private void createVideoTrackFromCameraAndShowIt() {
        //Now create a VideoCapturer instance.
        VideoCapturer videoCapturer = createVideoCapturer();
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer);
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        videoTrackFromCamera = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addRenderer(new VideoRenderer(localSurfaceView));

        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        audioTrackFromCamera = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        audioTrackFromCamera.setEnabled(true);
    }

    private void createOfferPeerConnection(String from) {
        if (peerConnection != null) {
            peerConnection.createOffer(new CustomSdpObserver("createOffer") {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    try {
                        peerConnection.setLocalDescription(new CustomSdpObserver("setLocalDescription"), sessionDescription);
                        JSONObject payload = new JSONObject();
                        payload.put("type", sessionDescription.type.canonicalForm());
                        payload.put("sdp", sessionDescription.description);
                        emitMessage(from, sessionDescription.type.canonicalForm(), payload);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, pcConstraints);
        } else {
            Log.e(TAG, "createOfferPeerConnection == null");
        }
    }

    private void createAnswerPeerConnection(String from, JSONObject payload) {
        try {
            String type = payload.getString("type");
            String sdp = payload.getString("sdp");
            SessionDescription sessionDescription = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), sdp);
            peerConnection.setRemoteDescription(new CustomSdpObserver("setRemoteDescription"), sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (peerConnection != null) {
            peerConnection.createAnswer(new CustomSdpObserver("createAnswer") {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("type", sessionDescription.type.canonicalForm());
                        payload.put("sdp", sessionDescription.description);
                        emitMessage(from, sessionDescription.type.canonicalForm(), payload);
                        peerConnection.setLocalDescription(new CustomSdpObserver("setLocalDescription"), sessionDescription);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, pcConstraints);
        } else {
            Log.e(TAG, "createAnswerPeerConnection == null");
        }
    }

    private void SetRemoteSDPCommand(JSONObject payload) {
        try {
            String type = payload.getString("type");
            String sdp = payload.getString("sdp");
            SessionDescription sessionDescription = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), sdp);
            peerConnection.setRemoteDescription(new CustomSdpObserver("SetRemoteSDPCommand"), sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void AddIceCandidateCommand(JSONObject payload) {
        try {
            if (peerConnection != null) {
                if (peerConnection.getRemoteDescription() != null) {
                    IceCandidate candidate = new IceCandidate(
                            payload.getString("id"),
                            payload.getInt("label"),
                            payload.getString("candidate")
                    );
                    peerConnection.addIceCandidate(candidate);
                }
            } else {
                Log.e(TAG, "AddIceCandidateCommand == null");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initPeerConnections() {
        peerConnection = createLocalPeerConnection(peerConnectionFactory);
    }

    private void startStreamingVideo() {
        MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(videoTrackFromCamera);
        mediaStream.addTrack(audioTrackFromCamera);
        peerConnection.addStream(mediaStream);
    }

    private PeerConnection createLocalPeerConnection(PeerConnectionFactory
                                                             peerConnectionFactory) {
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:23.21.150.121").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        PeerConnection.Observer customPeerConnectionObserver = new CustomPeerConnectionObserver(TAG) {

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                peerConnection.addIceCandidate(iceCandidate);
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("label", iceCandidate.sdpMLineIndex);
                    payload.put("id", iceCandidate.sdpMid);
                    payload.put("candidate", iceCandidate.sdp);
                    emitMessage(id, "candidate", payload);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream " + mediaStream.label());

                peerConnection.addStream(mediaStream);

                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addRenderer(new VideoRenderer(remoteSurfaceView));

                AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                remoteAudioTrack.setEnabled(true);
                remoteAudioTrack.setVolume(5);
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                mediaStream.dispose();
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
            }

            @Override
            public void onRenegotiationNeeded() {
            }
        };

        return peerConnectionFactory.createPeerConnection(rtcConfig, pcConstraints, customPeerConnectionObserver);
    }

    private VideoCapturer createVideoCapturer() {
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        Toast.makeText(this, "CameraCapturer is null", Toast.LENGTH_SHORT).show();
        return null;
    }

    /*
     * Read more about Camera2 here
     * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
     * */
    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    private void startRoom() {
        try {
            emitMessage(id, "init", null);
            createRoom("Room_" + id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void createRoom(String roomName) {
        try {
            JSONObject message = new JSONObject();
            message.put("name", roomName);
            socket.emit("readyToStream", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void emitMessage(String to, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", to);
        message.put("type", type);
        message.put("payload", payload);
        socket.emit("message", message);
        Log.i(TAG, "emitMessage : " + message);
    }

    /****************************************************************************
     ******************************* Listener ***********************************
     ****************************************************************************/

    private View.OnClickListener onClickListener = v -> {
        if (v == imgHangUp) {
            try {
                if (peerConnection != null) {
                    peerConnection.close();
                    peerConnection = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (v == imgSwitchCamera) {
            if (videoCapturer != null) {
                CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
                cameraVideoCapturer.switchCamera(null);
            }
        }

        if (v == imgMute) {

        }
    };
}
