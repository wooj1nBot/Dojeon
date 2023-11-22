package com.assist.dojeon.dto;

public class Bill {

    public String billing_expire_at;
    public String billing_key;
    public String id;
    public String published_at;
    public int state;
    public String subscription_id;

    public Bill(){}

    public int getState() {
        return state;
    }

    public String getBilling_expire_at() {
        return billing_expire_at;
    }

    public String getBilling_key() {
        return billing_key;
    }

    public String getId() {
        return id;
    }

    public String getPublished_at() {
        return published_at;
    }

    public String getSubscription_id() {
        return subscription_id;
    }
}
