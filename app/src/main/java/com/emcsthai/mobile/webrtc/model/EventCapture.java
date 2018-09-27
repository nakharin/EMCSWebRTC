package com.emcsthai.mobile.webrtc.model;

import com.google.gson.annotations.SerializedName;

public class EventCapture {

    @SerializedName("os")
    private String os = "";

    @SerializedName("from")
    private String from = "";

    @SerializedName("to")
    private String to = "";

    @SerializedName("data")
    private String data = "";

    public String getOs() {
        return os;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getData() {
        return data;
    }
}
