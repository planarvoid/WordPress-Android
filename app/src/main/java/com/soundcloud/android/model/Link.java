package com.soundcloud.android.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Link implements Parcelable {

    private String mHref;

    public Link() { /* deserialization */}

    public Link(String href) {
        this.mHref = href;
    }

    public Link(Parcel in) {
        mHref = in.readString();
    }

    public String getHref() {
        return mHref;
    }

    public void setHref(String href) {
        this.mHref = href;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mHref);
    }

    public static final Parcelable.Creator<Link> CREATOR = new Parcelable.Creator<Link>() {
        public Link createFromParcel(Parcel in) {
            return new Link(in);
        }

        public Link[] newArray(int size) {
            return new Link[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Link)) return false;

        Link link = (Link) o;

        if (mHref != null ? !mHref.equals(link.mHref) : link.mHref != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return mHref != null ? mHref.hashCode() : 0;
    }
}
