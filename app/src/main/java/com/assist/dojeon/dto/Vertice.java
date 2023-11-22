package com.assist.dojeon.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Vertice {

    @Expose
    @SerializedName("x") int x;
    @Expose
    @SerializedName("y") int y;

    @Override
    public String toString() {
        return "Vertice{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
