package com.assist.dojeon.dto;

import android.graphics.RectF;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TextAnnotation implements Comparable<TextAnnotation>{

    @Expose
    @SerializedName("locale")
    public String locale; //텍스트의 언어

    @Expose
    @SerializedName("description")
    public String description; //텍스트
    @Expose

    @SerializedName("boundingPoly")
    public BoundingPoly boundingPoly; //텍스트의 좌표
    int type = 0;
    float diff = 5; //좌표의 오차 허용값


    @Override
    public int compareTo(TextAnnotation o) {
        RectF a = boundingPoly.getRectF();
        RectF b = o.boundingPoly.getRectF();


        if(type == 1 && o.type == 1){ //type = 1 : left 값을 기준으로 정렬
            return Float.compare(a.left, b.left);
        }
        else if(type == 2 && o.type == 2){
            //type = 2 : Rect 직가각형 넓이를 기준으로 정렬
            return Float.compare(a.width()*a.height(), b.width()*b.height());
        }
        else {
            //type = 0 : 세로(top)으로 정렬하되 허용오차값 이하일 경우, left 로 정렬
            if (Math.abs(a.top - b.top) <= diff) {
                return Float.compare(a.left, b.left);
            }
            return Float.compare(a.top, b.top);
        }
    }

    @Override
    public String toString() {
        return "TextAnnotation{" +
                "locale='" + locale + '\'' +
                ", description='" + description + '\'' +
                ", boundingPoly=" + boundingPoly.getRectF() +
                '}';
    }
}
