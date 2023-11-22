package com.assist.dojeon.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Image {

    @Expose
    @SerializedName("content") private String content;

    public Image(String content){
        this.content = content;
    }

}
