package com.assist.dojeon.dto;

import android.net.Uri;

import com.assist.dojeon.R;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Dojeon implements Serializable {

    public static final int TYPE_MEDITATE = 0;
    public static final int TYPE_EXERCISE = 1;
    public static final int TYPE_WEIGHT = 2;

    public static final String[] TYPES = {"5분 명상", "운동", "체중 측정"};
    public static final int[] TYPES_IMAGE = {R.drawable.medi, R.drawable.exer, R.drawable.weight};

    public int type;
    public Date start;
    public Date end;

    public int range;
    public Map<String, Double> resMap;
    public Map<String, String> imageMap;
    public Map<String, Date> timeMap;

    public int alarmId;
    public int alarmHour;
    public int alarmMinute;
    public int dojeonId;

    public Dojeon(){}

    public Dojeon(int dojeonId, int type, Date start, Date end, int range){
        this.dojeonId = dojeonId;
        this.type = type;
        this.start = start;
        this.end = end;
        this.range = range;
        resMap = new HashMap<>();
        timeMap = new HashMap<>();
        imageMap = new HashMap<>();
    }

    public Date getEnd() {
        return end;
    }

    public Date getStart() {
        return start;
    }

    public int getType() {
        return type;
    }

    public Map<String, Double> getResMap() {
        return resMap;
    }

    public int getAlarmHour() {
        return alarmHour;
    }

    public int getAlarmMinute() {
        return alarmMinute;
    }

    public int getAlarmId() {
        return alarmId;
    }

    public Map<String, Date> getTimeMap() {
        return timeMap;
    }

    public Map<String, String> getImageMap() {
        return imageMap;
    }

    public int getRange() {
        return range;
    }

    public void setAlarmTime(int alarmHour, int alarmMinute){
        this.alarmHour = alarmHour;
        this.alarmMinute = alarmMinute;
    }

    public int getDojeonId() {
        return dojeonId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public void addImage(Calendar c, Uri uri){
        String key = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH) + 1)+"-"+c.get(Calendar.DAY_OF_MONTH);
        if (imageMap == null) imageMap = new HashMap<>();
        imageMap.put(key, uri.toString());
    }

    public Uri getImage(Calendar c){
        String key = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH) + 1)+"-"+c.get(Calendar.DAY_OF_MONTH);
        if (imageMap == null) imageMap = new HashMap<>();
        if (imageMap.containsKey(key)){
            return Uri.parse(imageMap.get(key));
        }
        return null;
    }

    public void check(Calendar c, double res){
        String key = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH) + 1)+"-"+c.get(Calendar.DAY_OF_MONTH);
        timeMap.put(key, c.getTime());
        resMap.put(key, res);
    }

    public boolean isCheck(Calendar c){
        String key = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH) + 1)+"-"+c.get(Calendar.DAY_OF_MONTH);
        return resMap.containsKey(key);
    }
}
