package com.tnpxu.droneeyes.api;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tnpxu on 11/12/2558.
 */
public class SendingPhotoData {

    @SerializedName("token")
    String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }


    public SendingPhotoData() {
    }
}
