package com.emcs.nakharin.webrtc;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Webrtc_Step2
 * Created by vivek-3102 on 11/03/17.
 */

class CustomSdpObserver implements SdpObserver {


    private String tag;

    CustomSdpObserver(String logTag) {
        tag = this.getClass().getCanonicalName();
        this.tag = this.tag + " -> " + logTag;
    }


    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(tag, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");
    }

    @Override
    public void onSetSuccess() {
        Log.i(tag, "onSetSuccess() called");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.e(tag, "onCreateFailure() called with: s = [" + s + "]");
    }

    @Override
    public void onSetFailure(String s) {
        Log.e(tag, "onSetFailure() called with: s = [" + s + "]");
    }

}
