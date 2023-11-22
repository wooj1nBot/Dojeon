package com.assist.dojeon.dto;

import java.util.ArrayList;

public class User {

    public String uid;
    public String name;
    public String email;
    public String profile;
    ArrayList<Dojeon> dojeons;

    public Bill bill;

    public User(){}

    public User(String uid, String name, String email){
        this.uid = uid;
        this.name = name;
        this.email = email;
        dojeons = new ArrayList<>();
    }

    public Bill getBill() {
        return bill;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ArrayList<Dojeon> getDojeons() {
        if (dojeons == null) dojeons = new ArrayList<>();
        return dojeons;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public void addDojeon(Dojeon dojeon){
        if (dojeons == null) dojeons = new ArrayList<>();
        dojeons.add(dojeon);
    }

    public void addAllDojeon(ArrayList<Dojeon> doj){
        if (dojeons == null) dojeons = new ArrayList<>();
        dojeons.addAll(doj);
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setDojeons(ArrayList<Dojeon> dojeons) {
        this.dojeons = dojeons;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }
}
