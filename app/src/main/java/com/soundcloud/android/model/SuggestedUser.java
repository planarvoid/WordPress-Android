package com.soundcloud.android.model;

import android.os.Parcel;

public class SuggestedUser extends ScModel {

    private String mUsername, mCity, mCountry;

    public SuggestedUser() {
    }

    public SuggestedUser(String urn) {
        super(urn);
    }

    public SuggestedUser(Parcel parcel) {
        super(parcel);
        mUsername = parcel.readString();
        mCity = parcel.readString();
        mCountry = parcel.readString();
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mUsername);
        dest.writeString(mCity);
        dest.writeString(mCountry);
    }
}
