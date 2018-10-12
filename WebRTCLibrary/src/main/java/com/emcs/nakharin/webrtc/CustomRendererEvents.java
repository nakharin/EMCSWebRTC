package com.emcs.nakharin.webrtc;

import android.util.Log;

import org.webrtc.RendererCommon;

class CustomRendererEvents implements RendererCommon.RendererEvents {

    private String tag;

    CustomRendererEvents(String logTag) {
        tag = this.getClass().getCanonicalName();
        this.tag = this.tag + " -> " + logTag;
    }


    @Override
    public void onFirstFrameRendered() {
        Log.i(tag, "onFirstFrameRendered() called");
    }

    @Override
    public void onFrameResolutionChanged(int i, int i1, int i2) {
        Log.d(tag, "onFrameResolutionChanged() called with: i = [" + i + "], i1 = [" + i1 + "], i2 = [" + i2 + "]");
    }
}
