package com.soundcloud.android.model;

public class SuggestedUser extends ScModel {

    private String mUsername, mCity, mCountry;

    public SuggestedUser() {
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String city) {
        this.mCity = city;
    }

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(String country) {
        this.mCountry = country;
    }
}
