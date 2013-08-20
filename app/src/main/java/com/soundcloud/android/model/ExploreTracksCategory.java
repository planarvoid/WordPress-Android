package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class ExploreTracksCategory implements Parcelable  {

    public static final String EXTRA = "category";

    private String mTitle;
    private Map<String, Link> mLinks = Collections.emptyMap();

    public ExploreTracksCategory(){ /* For Deserialization */ }

    public ExploreTracksCategory(String title) {
        this.mTitle = title;
    }

    public ExploreTracksCategory(Parcel in) {
        Bundle b = in.readBundle(Link.class.getClassLoader());
        mTitle = b.getString("title");
        mLinks = (Map<String, Link>) b.getSerializable("links");
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    @JsonProperty("_links")
    public Map<String, Link> getLinks() {
        return mLinks;
    }

    public void setLinks(Map<String, Link> links) {
        this.mLinks = links;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // use a bundle to avoid typing problems with the map
        Bundle b = new Bundle();
        b.setClassLoader(Link.class.getClassLoader());
        b.putString("title", mTitle);
        b.putSerializable("links", (Serializable) mLinks);
        dest.writeBundle(b);
    }

    public static final Parcelable.Creator<ExploreTracksCategory> CREATOR = new Parcelable.Creator<ExploreTracksCategory>() {
        public ExploreTracksCategory createFromParcel(Parcel in) {
            return new ExploreTracksCategory(in);
        }

        public ExploreTracksCategory[] newArray(int size) {
            return new ExploreTracksCategory[size];
        }
    };
}
