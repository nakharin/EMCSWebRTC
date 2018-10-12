package com.emcs.nakharin.webrtc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.emcs.nakharin.webrtc.model.EventCapture;
import com.emcs.nakharin.webrtc.model.EventDrawing;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CapturerObserver;
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
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;

import static android.content.Context.AUDIO_SERVICE;

public class WebRTCClient {

    private final static String TAG = WebRTCClient.class.getCanonicalName();

    private final static String BASE_URL = "https://inspect.emcsthai.com";
    private final static String PATH = "/socket/socket.io";

    private static final int VIDEO_RESOLUTION_WIDTH = 1280;
    private static final int VIDEO_RESOLUTION_HEIGHT = 720;
    private static final int FPS = 30;

    private static final String LOCAL_MEDIA_STREAM = "ARDAMS";
    private static final String VIDEO_TRACK_ID = "100";
    private static final String AUDIO_TRACK_ID = "101";

    private Context mContext;
    private EglBase mEglBase;

    private OnWebRTCClientListener mOnWebRTCClientListener;

    private AudioManager mAudioManager;

    private VideoTrack mLocalVideoTrack = null;
    private AudioTrack mLocalAudioTrack = null;

    private VideoSource mVideoSource = null;
    private AudioSource mAudioSource = null;

    private VideoCapturer mVideoCapturer = null;

    private Socket mSocket = null;

    private PeerConnectionFactory mPeerConnectionFactory = null;

    private PeerConnection mPeerConnection = null;

    private String mCallId = "";
    private String mRoomId = "";

    private class IOEmit {
        static final String READY = "ready";
        static final String BYE = "bye";
        static final String CANDIDATE = "candidate";
        static final String JOIN = "join";
        static final String SCREEN = "screen";
        static final String MESSAGE = "message";
        static final String DRAWING = "drawing";
    }

    private class IOEvent {
        static final String CONNECT = "connect";
        static final String ID = "id";
        static final String JOINED = "joined";
        static final String MESSAGE = "message";
        static final String CAPTURE = "capture";
        static final String DRAWING = "drawing";
        static final String FULL = "full";
        static final String BYE = "bye";
        static final String DISCONNECT = "disconnect";
    }

    public WebRTCClient(Context context, String roomId, EglBase eglBase, OnWebRTCClientListener onWebRTCClientListener) {
        mContext = context;
        mRoomId = roomId;
        mEglBase = eglBase;
        mOnWebRTCClientListener = onWebRTCClientListener;

        mAudioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);

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

    public PeerConnectionFactory getPeerConnectionFactory() {
        return mPeerConnectionFactory;
    }

    public void switchCamera() {
        if (mVideoCapturer != null) {
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) mVideoCapturer;
            cameraVideoCapturer.switchCamera(cameraSwitchHandler);
        }
    }

    public void microphoneOn() {
        mAudioManager.setMicrophoneMute(false);
        Toast.makeText(mContext, "Microphone is On.", Toast.LENGTH_SHORT).show();
    }

    public void microphoneOff() {
        mAudioManager.setMicrophoneMute(true);
        Toast.makeText(mContext, "Microphone is Off.", Toast.LENGTH_SHORT).show();
    }

    public void disconnect() {
        if (mSocket != null) {
            // Method from this class
            emitMessage(mRoomId, IOEmit.BYE, null);

            // Method from this class
            close();
        }
    }

    public void startPreview() {
        Log.i(TAG, "startPreview");
//        if (mCapturerObserver != null) {
//            mCapturerObserver.onCapturerStarted(mVideoCapturer.isScreencast());
//        }
    }

    public void stopPreview() {
        Log.i(TAG, "stopPreview");
//        if (mCapturerObserver != null) {
//            mCapturerObserver.onCapturerStopped();
//        }
    }

    private void close() {

        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.close();
        }

        if (mOnWebRTCClientListener != null) {
            mOnWebRTCClientListener = null;
        }

        if (mVideoSource != null) {
            mVideoSource.dispose();
            mVideoSource = null;
        }

        if (mAudioSource != null) {
            mAudioSource.dispose();
            mAudioSource = null;
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

        if (mPeerConnection != null) {
            mPeerConnection.close();
            mPeerConnection = null;
        }
    }

    private X509TrustManager x509TrustManager = new X509TrustManager() {
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    };

    private void initSocket() {
        Log.i(TAG, "initSocket");
        try {

            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new javax.net.ssl.TrustManager[]{x509TrustManager}, new SecureRandom());

            HostnameVerifier hostnameVerifier = (hostname, session) -> true;

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(hostnameVerifier)
                    .sslSocketFactory(sslcontext.getSocketFactory(), x509TrustManager)
                    .build();

            IO.Options opts = new IO.Options();
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;
            opts.path = PATH;

            mSocket = IO.socket(BASE_URL, opts);

            mSocket.on(IOEvent.CONNECT, args -> {
                Log.i(TAG, "event_connect = " + Arrays.toString(args));

            }).on(IOEvent.ID, args -> {
                mCallId = (String) args[0];
                Log.i(TAG, "event_id : " + Arrays.toString(args));
                if (mOnWebRTCClientListener != null) {
                    mOnWebRTCClientListener.onCallReady(mCallId);
                }

                // Method from this class
                emitJoin(mRoomId);

                // Method from this class
                emitScreenSize(mRoomId);

            }).on(IOEvent.JOINED, args -> {
                mRoomId = (String) args[0];
                Log.i(TAG, "event_joined : " + Arrays.toString(args));
                // Method from this class
                emitMessage(mRoomId, IOEmit.READY, null);

            }).on(IOEvent.MESSAGE, args -> {
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

            }).on(IOEvent.DRAWING, args -> {
                JSONObject data = (JSONObject) args[0];
                Log.i(TAG, "event_drawing : " + data.toString());
                if (mOnWebRTCClientListener != null) {
                    EventDrawing eventDrawing = new Gson().fromJson(data.toString(), EventDrawing.class);
                    mOnWebRTCClientListener.onDrawingImage(eventDrawing);
                }

            }).on(IOEvent.CAPTURE, args -> {
                JSONObject data = (JSONObject) args[0];
                Log.i(TAG, "event_capture : " + data.toString());
                if (mOnWebRTCClientListener != null) {
                    EventCapture capture = new Gson().fromJson(data.toString(), EventCapture.class);
                    mOnWebRTCClientListener.onReceiveImage(capture);
                }

            }).on(IOEvent.FULL, args -> {
                Log.i(TAG, "event_full : " + Arrays.toString(args));

            }).on(IOEvent.BYE, args -> {
                Log.i(TAG, "event_bye : " + Arrays.toString(args));
                if (mOnWebRTCClientListener != null) {
                    mOnWebRTCClientListener.onHangUp();
                }

            }).on(IOEvent.DISCONNECT, args -> {
                Log.i(TAG, "event_disconnect : " + Arrays.toString(args));
            });
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.e(TAG, "URISyntaxException = " + e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "NoSuchAlgorithmException = " + e.getLocalizedMessage());
        } catch (KeyManagementException e) {
            e.printStackTrace();
            Log.e(TAG, "KeyManagementException = " + e.getLocalizedMessage());
        }
    }

    private void initPeerConnectionFactory() {
        Log.i(TAG, "initPeerConnectionFactory");
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(mContext)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                mEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());

//        AudioDeviceModule audioDeviceModule = new AudioDeviceModule() {
//            @Override
//            public long getNativeAudioDeviceModulePointer() {
//                return 0;
//            }
//
//            @Override
//            public void release() {
//
//            }
//
//            @Override
//            public void setSpeakerMute(boolean b) {
//
//            }
//
//            @Override
//            public void setMicrophoneMute(boolean b) {
//
//            }
//        };
//
//        AudioProcessingFactory audioProcessingFactory = new AudioProcessingFactory() {
//            @Override
//            public long createNative() {
//                return 0;
//            }
//        };

        mPeerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
//                .setAudioProcessingFactory(audioProcessingFactory)
//                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory();
    }

    private void createVideoTrackFromCameraAndShowIt() {
        Log.i(TAG, "createVideoTrackFromCameraAndShowIt");
        // Now create a VideoCapturer instance.
        mVideoCapturer = createVideoCapturer();

        mVideoSource = mPeerConnectionFactory.createVideoSource(mVideoCapturer.isScreencast());
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("VideoCapturerThread", mEglBase.getEglBaseContext());
        CapturerObserver mCapturerObserver = mVideoSource.getCapturerObserver();
        mVideoCapturer.initialize(surfaceTextureHelper, mContext, mCapturerObserver);
        mVideoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        mAudioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
        mLocalAudioTrack = mPeerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, mAudioSource);
        mLocalAudioTrack.setEnabled(true);

        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mAudioManager.setSpeakerphoneOn(true);
        mAudioManager.setMicrophoneMute(false);
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 50, 0);
        double volume = (double) mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        mLocalAudioTrack.setVolume(volume);


        mLocalVideoTrack = mPeerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, mVideoSource);
        mLocalVideoTrack.setEnabled(true);
    }

    private void initPeerConnections() {
        Log.i(TAG, "initPeerConnections");
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
//        iceServers.add(PeerConnection.IceServer.builder("stun:23.21.150.121").createIceServer());
//        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        iceServers.add(PeerConnection.IceServer.builder("turn:13.250.13.83:3478?transport=udp")
                .setUsername("YzYNCouZM1mhqhmseWk6")
                .setPassword("YzYNCouZM1mhqhmseWk6")
                .createIceServer());

        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);

//        MediaConstraints mMediaConstraints = new MediaConstraints();
//
//        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
//        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
//        mMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
//        mMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googCpuOveruseDetection", "false"));

        mPeerConnection = mPeerConnectionFactory.createPeerConnection(rtcConfiguration, customPeerConnectionObserver);
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
        mPeerConnection.createOffer(new CustomSdpObserver("createOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                mPeerConnection.setLocalDescription(new CustomSdpObserver("setLocalDescription"), sessionDescription);
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("type", sessionDescription.type.canonicalForm());
                    payload.put("sdp", sessionDescription.description);
                    // Method from this class
                    emitMessage(mRoomId, sessionDescription.type.canonicalForm(), payload);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }

    private void receiveOffer(JSONObject payload) {
        Log.i(TAG, "receiveOffer");
        try {
            String type = payload.getString("type");
            String sdp = payload.getString("sdp");
            SessionDescription sessionDescription = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), sdp);
            mPeerConnection.setRemoteDescription(new CustomSdpObserver("setRemoteDescription"), sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void createAnswer() {
        Log.i(TAG, "createAnswer");
        mPeerConnection.createAnswer(new CustomSdpObserver("createAnswer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                mPeerConnection.setLocalDescription(new CustomSdpObserver("setLocalDescription"), sessionDescription);
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("type", sessionDescription.type.canonicalForm());
                    payload.put("sdp", sessionDescription.description);
                    // Method from this class
                    emitMessage(mRoomId, sessionDescription.type.canonicalForm(), payload);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }

    private void receiveAnswer(JSONObject payload) {
        Log.i(TAG, "receiveAnswer");
        try {
            String type = payload.getString("type");
            String sdp = payload.getString("sdp");
            SessionDescription sessionDescription = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), sdp);
            mPeerConnection.setRemoteDescription(new CustomSdpObserver("setRemoteDescription"), sessionDescription);
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
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, cameraEventsHandler);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, cameraEventsHandler);

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
        mSocket.emit(IOEmit.JOIN, roomName);
        Log.d(TAG, "emit_join : " + roomName);
    }

    public void emitDrawing(float startX, float startY, float moveX, float moveY) {

        // TODO: 2/10/2018 AD emitDrawing

        JSONObject obj = new JSONObject();
        try {
            obj.put("os", "mobile");
            obj.put("to", mRoomId);
            obj.put("from", mCallId);
            obj.put("x0", startX);
            obj.put("y0", startY);
            obj.put("x1", moveX);
            obj.put("y1", moveY);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit(IOEmit.DRAWING, obj);
        Log.d(TAG, "emit_drawing : " + obj);
    }

    private int gcd(int p, int q) {
        if (q == 0) return p;
        else return gcd(q, p % q);
    }

    private String getAspectRatio(int a, int b) {
        final int gcd = gcd(a,b);
        if(a > b) {
            return (a/gcd) + ":" + (b/gcd);
        } else {
            return (b/gcd) + ":" + (a/gcd);
        }
    }

    private void emitScreenSize(String roomId) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        String ratio = getAspectRatio(width, height);

        JSONObject obj = new JSONObject();
        try {
            obj.put("os", "mobile");
            obj.put("to", roomId);
            obj.put("from", mCallId);
            obj.put("ratio", ratio);
            obj.put("height", height);
            obj.put("width", width);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit(IOEmit.SCREEN, obj);
        Log.d(TAG, "emit_screen : " + obj);
    }

    private void emitMessage(String roomId, String type, JSONObject payload) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("os", "mobile");
            obj.put("to", roomId);
            obj.put("from", mCallId);
            obj.put("type", type);
            obj.put("payload", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit(IOEmit.MESSAGE, obj);
        Log.d(TAG, "emit_message : " + obj);
    }

    /*****************************************************************************************
     ************************************** Listener *****************************************
     *****************************************************************************************/

    private final PeerConnection.Observer customPeerConnectionObserver = new CustomPeerConnectionObserver("customPeerConnectionObserver") {

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
                // Method from this class
                emitMessage(mRoomId, IOEmit.CANDIDATE, payload);
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

    private final CameraVideoCapturer.CameraSwitchHandler cameraSwitchHandler = new CameraVideoCapturer.CameraSwitchHandler() {
        @Override
        public void onCameraSwitchDone(boolean b) {
            Log.i(TAG, "onCameraSwitchDone: " + b);
            if (mOnWebRTCClientListener != null) {
                mOnWebRTCClientListener.onCameraSwitchDone(b);
            }
        }

        @Override
        public void onCameraSwitchError(String s) {
            Log.i(TAG, "onCameraSwitchError: " + s);
            if (mOnWebRTCClientListener != null) {
                mOnWebRTCClientListener.onCameraSwitchError(s);
            }
        }
    };

    private final CameraVideoCapturer.CameraEventsHandler cameraEventsHandler = new CameraVideoCapturer.CameraEventsHandler() {
        @Override
        public void onCameraError(String s) {
            Log.d(TAG, "onCameraError() called with: s = [" + s + "]");
        }

        @Override
        public void onCameraDisconnected() {
            Log.d(TAG, "onCameraDisconnected() called");
        }

        @Override
        public void onCameraFreezed(String s) {
            Log.d(TAG, "onCameraFreezed() called with: s = [" + s + "]");
        }

        @Override
        public void onCameraOpening(String s) {
            Log.d(TAG, "onCameraOpening() called with: s = [" + s + "]");
        }

        @Override
        public void onFirstFrameAvailable() {
            Log.d(TAG, "onFirstFrameAvailable() called");
        }

        @Override
        public void onCameraClosed() {
            Log.d(TAG, "onCameraClosed() called");
        }
    };

    /*****************************************************************************************
     ************************************ Interface Class ************************************
     *****************************************************************************************/

    public interface OnWebRTCClientListener {
        void onCallReady(String callId);
        void onLocalStream(MediaStream mediaStream);
        void onRemoteStream(MediaStream mediaStream);
        void onReceiveImage(EventCapture eventCapture);
        void onHangUp();
        void onCameraSwitchDone(boolean isFrontSide);
        void onCameraSwitchError(String error);
        void onDrawingImage(EventDrawing eventDrawing);
    }
}
