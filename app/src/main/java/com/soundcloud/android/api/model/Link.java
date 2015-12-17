package com.soundcloud.android.api.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Link implements Parcelable {

    public static final Parcelable.Creator<Link> CREATOR = new Parcelable.Creator<Link>() {
        public Link createFromParcel(Parcel in) {
            return new Link(in);
        }

        public Link[] newArray(int size) {
            return new Link[size];
        }
    };
    private String href;

    public Link() { /* deserialization */}

    public Link(String href) {
        this.href = href;
    }

    public Link(Parcel in) {
        href = in.readString();
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(href);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Link)) {
            return false;
        }

        Link link = (Link) o;

        return !(href != null ? !href.equals(link.href) : link.href != null);

    }

    @Override
    public int hashCode() {
        return href != null ? href.hashCode() : 0;
    }
}
