package com.emcsthai.mobile.webrtc.model;

import android.graphics.Path;

public class EventPath {

    private int mState = 0;
    private Path mPath;

    EventPath(int state, Path path) {
        mState = state;
        mPath = path;
    }

    public int getmState() {
        return mState;
    }

    public Path getmPath() {
        return mPath;
    }
}