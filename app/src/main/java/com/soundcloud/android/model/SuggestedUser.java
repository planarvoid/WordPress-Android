package com.soundcloud.android.model;

import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.image.ImageSize;

import android.os.Parcel;
import android.os.Parcelable;

@Model
public class SuggestedUser extends ScModel {

    private String mUsername;
    private String mCity;
    private String mCountry;

    private String mToken;

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
        mToken = parcel.readString();
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

    public String getToken() {
        return mToken;
    }

    public void setToken(String token) {
        mToken = token;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mUsername);
        dest.writeString(mCity);
        dest.writeString(mCountry);
        dest.writeString(mToken);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public SuggestedUser createFromParcel(Parcel in) {
            return new SuggestedUser(in);
        }

        public SuggestedUser[] newArray(int size) {
            return new SuggestedUser[size];
        }
    };

    @Override
    public String toString() {
        return "SuggestedUser{" +
                "mUsername='" + mUsername + '\'' +
                ", mCity='" + mCity + '\'' +
                ", mCountry='" + mCountry + '\'' +
                '}';
    }

    public String getAvatarUrl() {
        return Urn.parse(mURN).imageUri(ImageSize.T500).toString();
    }

    public String getLocation() {
        return ScTextUtils.getLocation(mCity, mCountry);
    }
}
