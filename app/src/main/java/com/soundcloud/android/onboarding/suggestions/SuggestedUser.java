package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.utils.ScTextUtils;

import android.os.Parcel;
import android.os.Parcelable;

public class SuggestedUser extends ScModel {

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public SuggestedUser createFromParcel(Parcel in) {
            return new SuggestedUser(in);
        }

        public SuggestedUser[] newArray(int size) {
            return new SuggestedUser[size];
        }
    };
    private String username;
    private String city;
    private String country;
    private String token;

    public SuggestedUser() {
    }

    public SuggestedUser(String urn) {
        super(urn);
    }

    public SuggestedUser(Parcel parcel) {
        super(parcel);
        username = parcel.readString();
        city = parcel.readString();
        country = parcel.readString();
        token = parcel.readString();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(username);
        dest.writeString(city);
        dest.writeString(country);
        dest.writeString(token);
    }

    @Override
    public String toString() {
        return "SuggestedUser{" +
                "mUsername='" + username + '\'' +
                ", mCity='" + city + '\'' +
                ", mCountry='" + country + '\'' +
                '}';
    }

    public String getLocation() {
        return ScTextUtils.getLocation(city, country);
    }
}
