package com.assist.dojeon.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Requests {
    @Expose
    @SerializedName("requests") private List<Request> requests;

    public Requests(List<Request> requests){
        this.requests = requests;
    }
}
