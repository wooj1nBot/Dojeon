package com.assist.dojeon.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Feature {

    @Expose
    @SerializedName("type") public String type;

    public Feature(String type){
        this.type = type;
    }

}
