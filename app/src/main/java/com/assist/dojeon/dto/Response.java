package com.assist.dojeon.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Response {

    @Expose
    @SerializedName("textAnnotations")
    public List<TextAnnotation> textAnnotations;

}
