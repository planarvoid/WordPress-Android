package com.soundcloud.android.model;

import com.soundcloud.java.strings.Charsets;
import org.jetbrains.annotations.NotNull;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class ContentStringHelper<T extends ContentStringHelper> implements Comparable<T>, Parcelable {

    abstract String getContent();

    @Override
    public int compareTo(@NotNull T another) {
        return this.getContent().compareTo(another.getContent());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getContent());
    }

    @Override
    public String toString() {
        return getContent();
    }

    public String toEncodedString() {
        try {
            return URLEncoder.encode(getContent(), Charsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

}
