package com.assist.dojeon.dto;

import com.google.gson.annotations.SerializedName;

public class PostResult {
    @SerializedName("responses")
    public String responses;


    public String toJson() {
        return responses;
    }


}
