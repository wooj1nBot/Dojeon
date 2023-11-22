package com.assist.dojeon.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Responses {

    @Expose
    @SerializedName("responses")
    public List<Response> responses;

}
