package com.emcsthai.mobile.webrtc;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

public class WebRTCClient {

    private final static String TAG = WebRTCClient.class.getCanonicalName();
    private final static int MAX_PEER = 2;
    private boolean[] endPoints = new boolean[MAX_PEER];

    /*******************************************************************************
     **************************** interface Class **********************************
     *******************************************************************************/

    public interface OnWebRTCClientListener {
        void onCallReady(String callId);
        void onStatusChanged(String newStatus);
        void onLocalStream(MediaStream localStream);
        void onAddRemoteStream(MediaStream remoteStream, int endPoint);
        void onRemoveRemoteStream(int endPoint);
    }

    private interface Command {
        void execute(String peerId, JSONObject payload) throws JSONException;
    }
}
