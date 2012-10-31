package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Date;

public class SharingNote implements Parcelable {
    @JsonProperty @JsonView(Views.Mini.class) public String text;
    @JsonProperty @JsonView(Views.Mini.class) public Date created_at;

    public SharingNote() {
        super();
    }

    public SharingNote(Parcel in) {
        text = in.readString();
        created_at = new Date(in.readLong());
    }


    public boolean isEmpty() {
        return TextUtils.isEmpty(text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SharingNote that = (SharingNote) o;

        return !(created_at != null ? !created_at.equals(that.created_at) : that.created_at != null)
                && !(text != null ? !text.equals(that.text) : that.text != null);

    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeLong(created_at.getTime());
    }
}
