package com.emcsthai.mobile.webrtc;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Socket;

import static io.socket.client.Socket.EVENT_CONNECT;
import static io.socket.client.Socket.EVENT_DISCONNECT;
import static io.socket.client.Socket.EVENT_MESSAGE;

public class WebRTCClient {

    private final static String TAG = WebRTCClient.class.getCanonicalName();

    private final static String BASE_URL = "http://10.2.2.82:3060/";

    private static final int VIDEO_RESOLUTION_WIDTH = 1280;
    private static final int VIDEO_RESOLUTION_HEIGHT = 720;
    private static final int FPS = 30;
    private static final String VIDEO_TRACK_ID = "100";
    private static final String AUDIO_TRACK_ID = "101";

    private Context mContext;
    private EglBase mEglBase;

    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoCapturer videoCapturer;

    private OnWebRTCClientListener mOnWebRTCClientListener;

    private Socket socket;

    private MediaConstraints mediaConstraints = new MediaConstraints();

    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;

    private String callId = "";
    private String roomId = "";

    public WebRTCClient(Context context, EglBase eglBase, OnWebRTCClientListener onWebRTCClientListener) {
        mContext = context;
        mEglBase = eglBase;
        mOnWebRTCClientListener = onWebRTCClientListener;

        // Method from this class
        initSocket();
        // Method from this class
        initPeerConnectionFactory();
        // Method from this class
        createVideoTrackFromCameraAndShowIt();
        // Method from this class
        initPeerConnections();
        // Method from this class
        startStreamingVideo();
    }

    public void switchCamera() {
        if (videoCapturer != null) {
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
            cameraVideoCapturer.switchCamera(null);
        }
    }

    public void muteOn() {
        localAudioTrack.setEnabled(true);
    }

    public void muteOff() {
        localAudioTrack.setEnabled(false);
    }

    public void hangUp() {
        if (socket != null) {
            socket.emit("message", "bye");
            socket.disconnect();
        }
    }

    public void close() {

        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
        }

        if (peerConnection != null) {
            peerConnection.dispose();
        }

        if (socket != null) {
            socket.disconnect();
            socket.close();
        }
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return peerConnectionFactory;
    }

    private void initSocket() {
        try {
            socket = IO.socket(BASE_URL);

            socket.on(EVENT_CONNECT, args -> {
                Log.i(TAG, "event_connect = " + Arrays.toString(args));

            }).on("id", args -> {
                callId = (String) args[0];
                Log.i(TAG, "event_id : " + Arrays.toString(args));
                if (mOnWebRTCClientListener != null) {
                    mOnWebRTCClientListener.onCallReady(callId);
                }

                emitJoin("emcs02");

            }).on("joined", args -> {
                roomId = (String) args[0];
                Log.i(TAG, "event_joined : " + Arrays.toString(args));
                try {
                    emitMessage(roomId, "init", null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }).on(EVENT_MESSAGE, args -> {
                Log.i(TAG, "event_message : " + Arrays.toString(args));
                try {

                    JSONObject data = (JSONObject) args[0];
                    JSONObject payload = null;

                    String type = data.getString("type");

                    if (!type.equals("init")) {
                        payload = data.getJSONObject("payload");
                    }

                    if (type.equals("init")) {
                        createOfferPeerConnection();
                    }

                    if (type.equals("offer")) {
                        createAnswerPeerConnection(payload);
                    }

                    if (type.equals("answer")) {
                        setRemoteSDPCommand(payload);
                    }

                    if (type.equals("candidate")) {
                        addIceCandidateCommand(payload);
                    }

                } catch (JSONException | ClassCastException e) {
                    e.printStackTrace();
                }

            }).on("full", args -> {
                Log.i(TAG, "event_full : " + Arrays.toString(args));

            }).on("bye", args -> {
                Log.i(TAG, "event_bye : " + Arrays.toString(args));
                if (mOnWebRTCClientListener != null) {
                    mOnWebRTCClientListener.onRemoteHangUp();
                }

            }).on(EVENT_DISCONNECT, args -> {
                Log.i(TAG, "event_disconnect : " + Arrays.toString(args));
            });
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.e(TAG, "initSocket : URISyntaxException = " + e.getMessage());
        }
    }

    private void initPeerConnectionFactory() {
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(mContext)
                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                mEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        peerConnectionFactory.setVideoHwAccelerationOptions(mEglBase.getEglBaseContext(), mEglBase.getEglBaseContext());
    }

    private void createVideoTrackFromCameraAndShowIt() {
        //Now create a VideoCapturer instance.
        VideoCapturer videoCapturer = createVideoCapturer();
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer);
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(mediaConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        localVideoTrack.setEnabled(true);
    }

    private void initPeerConnections() {

        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:23.21.150.121").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);

        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

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
                    emitMessage(roomId, "candidate", payload);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                peerConnection.addStream(mediaStream);
                if (mOnWebRTCClientListener != null) {
                    mOnWebRTCClientListener.onRemoteStream(mediaStream);
                }
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

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, mediaConstraints, customPeerConnectionObserver);
    }

    private void startStreamingVideo() {
        MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(localVideoTrack);
        mediaStream.addTrack(localAudioTrack);
        peerConnection.addStream(mediaStream);
        if (mOnWebRTCClientListener != null) {
            mOnWebRTCClientListener.onLocalStream(mediaStream);
        }
    }

    private void createOfferPeerConnection() {
        if (peerConnection != null) {
            peerConnection.createOffer(new CustomSdpObserver("createOffer") {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);

                    peerConnection.setLocalDescription(new CustomSdpObserver("setLocalDescription"), sessionDescription);

                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("type", sessionDescription.type.canonicalForm());
                        payload.put("sdp", sessionDescription.description);
                        emitMessage(roomId, sessionDescription.type.canonicalForm(), payload);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, mediaConstraints);
        } else {
            Log.e(TAG, "createOfferPeerConnection == null");
        }
    }

    private void createAnswerPeerConnection(JSONObject payload) {

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

                    peerConnection.setLocalDescription(new CustomSdpObserver("setLocalDescription"), sessionDescription);

                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("type", sessionDescription.type.canonicalForm());
                        payload.put("sdp", sessionDescription.description);
                        emitMessage(roomId, sessionDescription.type.canonicalForm(), payload);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, mediaConstraints);
        } else {
            Log.e(TAG, "createAnswerPeerConnection == null");
        }
    }

    private void setRemoteSDPCommand(JSONObject payload) {
        try {
            String type = payload.getString("type");
            String sdp = payload.getString("sdp");
            SessionDescription sessionDescription = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), sdp);
            peerConnection.setRemoteDescription(new CustomSdpObserver("setRemoteSDPCommand"), sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addIceCandidateCommand(JSONObject payload) {
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
                Log.e(TAG, "addIceCandidateCommand == null");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private VideoCapturer createVideoCapturer() {
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(mContext));
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

        Toast.makeText(mContext, "CameraCapturer is null", Toast.LENGTH_SHORT).show();
        return null;
    }

    /*
     * Read more about Camera2 here
     * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
     * */
    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(mContext);
    }

    private void emitJoin(String roomName) {
        socket.emit("join", roomName);
        Log.d(TAG, "emit_join : " + roomName);
    }

    private void emitMessage(String roomId, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", roomId);
        message.put("type", type);
        message.put("payload", payload);
        socket.emit("message", message);
        Log.d(TAG, "emit_message : " + message);
    }

    /*******************************************************************************
     **************************** interface Class **********************************
     *******************************************************************************/

    public interface OnWebRTCClientListener {
        void onCallReady(String callId);

        void onLocalStream(MediaStream mediaStream);

        void onRemoteStream(MediaStream mediaStream);

        void onRemoteHangUp();
    }
}
