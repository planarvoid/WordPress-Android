package com.soundcloud.android.model;


import android.os.Parcelable;

public interface Origin extends Parcelable {
    Track getTrack();
    User getUser();
}
