package com.emcsthai.mobile.webrtc.model;

import com.google.gson.annotations.SerializedName;

public class EventDrawing {

    @SerializedName("os")
    private String os = "";

    @SerializedName("from")
    private String from = "";

    @SerializedName("to")
    private String to = "";

    @SerializedName("x")
    private float x = 0.0f;

    @SerializedName("y")
    private float y = 0.0f;

    @SerializedName("state")
    private int state = 0;

    public String getOs() {
        return os;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getState() {
        return state;
    }
}
