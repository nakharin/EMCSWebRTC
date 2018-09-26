package com.emcsthai.mobile.webrtc.model;

import com.google.gson.annotations.SerializedName;

public class DrawingPoint {

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

    public String getOS() {
        return os;
    }

    public void setOS(String os) {
        this.os = os;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
}
