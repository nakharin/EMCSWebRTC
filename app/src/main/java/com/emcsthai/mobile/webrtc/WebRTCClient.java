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

    private static final String LOCAL_MEDIA_STREAM = "ARDAMS";
    private static final String VIDEO_TRACK_ID = "100";
    private static final String AUDIO_TRACK_ID = "101";

    private Context mContext;
    private EglBase mEglBase;

    private VideoTrack mLocalVideoTrack;
    private AudioTrack mLocalAudioTrack;
    private VideoCapturer mVideoCapturer;

    private OnWebRTCClientListener mOnWebRTCClientListener;

    private Socket mSocket;

    private MediaConstraints mMediaConstraints = new MediaConstraints();

    private PeerConnectionFactory mPeerConnectionFactory;
    private PeerConnection mPeerConnection;

    private String mCallId;
    private String mRoomId;

    WebRTCClient(Context context, String roomId, EglBase eglBase, OnWebRTCClientListener onWebRTCClientListener) {
        mContext = context;
        mRoomId = roomId;
        mEglBase = eglBase;
        mOnWebRTCClientListener = onWebRTCClientListener;

        // Method from this class
        initPeerConnectionFactory();
        // Method from this class
        createVideoTrackFromCameraAndShowIt();
        // Method from this class
        initPeerConnections();
        // Method from this class
        startStreamingVideo();
        // Method from this class
        initSocket();
    }

    public void switchCamera() {
        if (mVideoCapturer != null) {
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) mVideoCapturer;
            cameraVideoCapturer.switchCamera(null);
        }
    }

    public void microphoneOn() {
        mLocalAudioTrack.setEnabled(true);
    }

    public void microphoneOff() {
        mLocalAudioTrack.setEnabled(false);
    }

    public void disconnect() {
        if (mSocket != null) {
            mSocket.emit("message", "bye");
            close();
        }
    }

    private void close() {

        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.close();
        }

        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.dispose();
            mLocalVideoTrack = null;
        }

        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.dispose();
            mLocalAudioTrack = null;
        }

        if (mVideoCapturer != null) {
            mVideoCapturer.dispose();
            mVideoCapturer = null;
        }

        if (mOnWebRTCClientListener != null) {
            mOnWebRTCClientListener = null;
        }

        if (mPeerConnection != null) {
            mPeerConnection.close();
            mPeerConnection = null;
        }

        if (mPeerConnectionFactory != null) {
            mPeerConnectionFactory.dispose();
            mPeerConnectionFactory = null;
        }
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return mPeerConnectionFactory;
    }

    private void initSocket() {
        Log.i(TAG, "initSocket");
        try {
            mSocket = IO.socket(BASE_URL);

            mSocket.on(EVENT_CONNECT, args -> {
                Log.i(TAG, "event_connect = " + Arrays.toString(args));

            }).on("id", args -> {
                mCallId = (String) args[0];
                Log.i(TAG, "event_id : " + Arrays.toString(args));
                if (mOnWebRTCClientListener != null) {
                    mOnWebRTCClientListener.onCallReady(mCallId);
                }

                emitJoin(mRoomId);

            }).on("joined", args -> {
                mRoomId = (String) args[0];
                Log.i(TAG, "event_joined : " + Arrays.toString(args));
                try {

                    emitMessage(mRoomId, "ready", null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }).on(EVENT_MESSAGE, args -> {
                Log.i(TAG, "event_message : " + Arrays.toString(args));
                try {

                    JSONObject data = (JSONObject) args[0];

                    String type = data.getString("type");

                    if (type.equals("init")) {
                        createOffer();
                    }

                    if (type.equals("offer")) {
                        JSONObject payload = data.getJSONObject("payload");
                        receiveOffer(payload);
                        createAnswer();
                    }

                    if (type.equals("answer")) {
                        JSONObject payload = data.getJSONObject("payload");
                        receiveAnswer(payload);
                    }

                    if (type.equals("candidate")) {
                        JSONObject payload = data.getJSONObject("payload");
                        receiveIceCandidate(payload);
                    }

                } catch (JSONException | ClassCastException e) {
                    e.printStackTrace();
                }

            }).on("full", args -> {
                Log.i(TAG, "event_full : " + Arrays.toString(args));

            }).on("bye", args -> {
                Log.i(TAG, "event_bye : " + Arrays.toString(args));
                if (mOnWebRTCClientListener != null) {
                    mOnWebRTCClientListener.onHangUp();
                }

            }).on(EVENT_DISCONNECT, args -> {
                Log.i(TAG, "event_disconnect : " + Arrays.toString(args));
            });
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.e(TAG, "URISyntaxException = " + e.getLocalizedMessage());
        }
    }

    private void initPeerConnectionFactory() {
        Log.i(TAG, "initPeerConnectionFactory");
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

        mPeerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        mPeerConnectionFactory.setVideoHwAccelerationOptions(mEglBase.getEglBaseContext(), mEglBase.getEglBaseContext());
    }

    private void createVideoTrackFromCameraAndShowIt() {
        Log.i(TAG, "createVideoTrackFromCameraAndShowIt");
        //Now create a VideoCapturer instance.
        mVideoCapturer = createVideoCapturer();
        VideoSource videoSource = mPeerConnectionFactory.createVideoSource(mVideoCapturer);
        mVideoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        AudioSource audioSource = mPeerConnectionFactory.createAudioSource(mMediaConstraints);
        mLocalAudioTrack = mPeerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        mLocalAudioTrack.setEnabled(true);

        mLocalVideoTrack = mPeerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        mLocalVideoTrack.setEnabled(true);
    }

    private void initPeerConnections() {
        Log.i(TAG, "initPeerConnections");
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:23.21.150.121").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);

        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        mPeerConnection = mPeerConnectionFactory.createPeerConnection(rtcConfig, mMediaConstraints, customPeerConnectionObserver);
    }

    private void startStreamingVideo() {
        Log.i(TAG, "startStreamingVideo");
        MediaStream mediaStream = mPeerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM);
        mediaStream.addTrack(mLocalVideoTrack);
        mediaStream.addTrack(mLocalAudioTrack);
        mPeerConnection.addStream(mediaStream);
        if (mOnWebRTCClientListener != null) {
            mOnWebRTCClientListener.onLocalStream(mediaStream);
        }
    }

    private void createOffer() {
        Log.i(TAG, "createOffer");
        mPeerConnection.createOffer(new CustomSdpObserver(TAG) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                mPeerConnection.setLocalDescription(new CustomSdpObserver(TAG), sessionDescription);
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("type", sessionDescription.type.canonicalForm());
                    payload.put("sdp", sessionDescription.description);
                    emitMessage(mRoomId, sessionDescription.type.canonicalForm(), payload);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, mMediaConstraints);
    }

    private void receiveOffer(JSONObject payload) {
        Log.i(TAG, "receiveOffer");
        try {
            String type = payload.getString("type");
            String sdp = payload.getString("sdp");
            SessionDescription sessionDescription = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), sdp);
            mPeerConnection.setRemoteDescription(new CustomSdpObserver(TAG), sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void createAnswer() {
        Log.i(TAG, "createAnswer");
        mPeerConnection.createAnswer(new CustomSdpObserver(TAG) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                mPeerConnection.setLocalDescription(new CustomSdpObserver(TAG), sessionDescription);
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("type", sessionDescription.type.canonicalForm());
                    payload.put("sdp", sessionDescription.description);
                    emitMessage(mRoomId, sessionDescription.type.canonicalForm(), payload);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, mMediaConstraints);
    }

    private void receiveAnswer(JSONObject payload) {
        Log.i(TAG, "receiveAnswer");
        try {
            String type = payload.getString("type");
            String sdp = payload.getString("sdp");
            SessionDescription sessionDescription = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), sdp);
            mPeerConnection.setRemoteDescription(new CustomSdpObserver(TAG), sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void receiveIceCandidate(JSONObject payload) {
        Log.i(TAG, "receiveIceCandidate");
        try {
            if (mPeerConnection.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );
                mPeerConnection.addIceCandidate(candidate);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private VideoCapturer createVideoCapturer() {
        if (useCamera2()) {
            mVideoCapturer = createCameraCapturer(new Camera2Enumerator(mContext));
        } else {
            mVideoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        return mVideoCapturer;
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
        mSocket.emit("join", roomName);
        Log.d(TAG, "emit_join : " + roomName);
    }

    private void emitMessage(String roomId, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("os", "mobile");
        message.put("to", roomId);
        message.put("type", type);
        message.put("payload", payload);
        mSocket.emit("message", message);
        Log.d(TAG, "emit_message : " + message);
    }

    /*******************************************************************************
     ********************************* Listener ************************************
     *******************************************************************************/

    private PeerConnection.Observer customPeerConnectionObserver = new CustomPeerConnectionObserver("customPeerConnectionObserver") {

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                if (mOnWebRTCClientListener != null) {
                    mOnWebRTCClientListener.onHangUp();
                }
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
            Log.i(TAG, "onIceCandidate");
            mPeerConnection.addIceCandidate(iceCandidate);
            try {
                JSONObject payload = new JSONObject();
                payload.put("label", iceCandidate.sdpMLineIndex);
                payload.put("id", iceCandidate.sdpMid);
                payload.put("candidate", iceCandidate.sdp);
                emitMessage(mRoomId, "candidate", payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.i(TAG, "onAddStream");
            mPeerConnection.addStream(mediaStream);
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

    /*******************************************************************************
     **************************** interface Class **********************************
     *******************************************************************************/

    public interface OnWebRTCClientListener {
        void onCallReady(String callId);
        void onLocalStream(MediaStream mediaStream);
        void onRemoteStream(MediaStream mediaStream);
        void onHangUp();
    }
}
