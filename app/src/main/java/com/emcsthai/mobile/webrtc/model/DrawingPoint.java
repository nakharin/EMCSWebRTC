package com.emcsthai.mobile.webrtc.model;

import com.google.gson.annotations.SerializedName;

public class DrawingPoint {

    @SerializedName("os")
    private String os = "";

    @SerializedName("from")
    private String from = "";

    @SerializedName("to")
    private String to = "";

    @SerializedName("x0")
    private float x0 = 0.0f;

    @SerializedName("y0")
    private float y0 = 0.0f;

    @SerializedName("x1")
    private float x1 = 0.0f;

    @SerializedName("y1")
    private float y1 = 0.0f;

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
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

    public float getX0() {
        return x0;
    }

    public void setX0(float x0) {
        this.x0 = x0;
    }

    public float getY0() {
        return y0;
    }

    public void setY0(float y0) {
        this.y0 = y0;
    }

    public float getX1() {
        return x1;
    }

    public void setX1(float x1) {
        this.x1 = x1;
    }

    public float getY1() {
        return y1;
    }

    public void setY1(float y1) {
        this.y1 = y1;
    }
}
