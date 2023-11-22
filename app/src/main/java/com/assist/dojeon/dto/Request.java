package com.assist.dojeon.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Request {
    @Expose
    @SerializedName("image") public Image image;

    @Expose
    @SerializedName("features") public List<Feature> features;

    public Request(Image image, List<Feature> features){
        this.image = image;
        this.features = features;
    }

}
