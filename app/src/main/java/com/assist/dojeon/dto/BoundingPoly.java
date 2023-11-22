package com.assist.dojeon.dto;

import android.graphics.RectF;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BoundingPoly {
    //인식한 텍스트의 좌표 클래스
    @Expose
    @SerializedName("vertices")
    public List<Vertice> vertices; //4개의 영역 점

    public RectF getRectF(){
        RectF rectF = new RectF();
        if(vertices != null){
            rectF.top = vertices.get(0).y;
            rectF.left = vertices.get(0).x;
            rectF.right = vertices.get(1).x;
            rectF.bottom = vertices.get(2).y;
        }

        return rectF;
    }
}
