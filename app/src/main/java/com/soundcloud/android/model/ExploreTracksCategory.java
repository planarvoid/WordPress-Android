package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class ExploreTracksCategory implements Parcelable {

    public static final String EXTRA = "category";
    public static final ExploreTracksCategory POPULAR_MUSIC_CATEGORY = new ExploreTracksCategory("Trending Music");
    public static final ExploreTracksCategory POPULAR_AUDIO_CATEGORY = new ExploreTracksCategory("Trending Audio");
    private static final int POPULAR_MUSIC_DESCRIPTION = 1;
    private static final int POPULAR_AUDIO_DESCRIPTION = 2;

    static final String SUGGESTED_TRACKS_LINK_REL = "suggested_tracks";
    private String mTitle;

    private Map<String, Link> mLinks = Collections.emptyMap();

    public ExploreTracksCategory() { /* For Deserialization */ }

    public ExploreTracksCategory(String title) {
        this.mTitle = title;
    }

    public ExploreTracksCategory(String title, String suggestedTracksUrl) {
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

    public String getSuggestedTracksPath() {
        return mLinks.get(SUGGESTED_TRACKS_LINK_REL).getHref();
    }

    @Override
    public int describeContents() {
        if (this == POPULAR_MUSIC_CATEGORY) {
            return POPULAR_MUSIC_DESCRIPTION;

        } else if (this == POPULAR_AUDIO_CATEGORY) {
            return POPULAR_AUDIO_DESCRIPTION;

        } else {
            return 0;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.describeContents());
        // use a bundle to avoid typing problems with the map
        Bundle b = new Bundle();
        b.setClassLoader(Link.class.getClassLoader());
        b.putString("title", mTitle);
        b.putSerializable("links", (Serializable) mLinks);
        dest.writeBundle(b);
    }

    public static final Parcelable.Creator<ExploreTracksCategory> CREATOR = new Parcelable.Creator<ExploreTracksCategory>() {
        public ExploreTracksCategory createFromParcel(Parcel in) {
            int description=in.readInt();
            switch(description)
            {
                case POPULAR_MUSIC_DESCRIPTION:
                    return POPULAR_MUSIC_CATEGORY;

                case POPULAR_AUDIO_DESCRIPTION:
                    return POPULAR_AUDIO_CATEGORY;

                default:
                    return new ExploreTracksCategory(in);
            }
        }

        public ExploreTracksCategory[] newArray(int size) {
            return new ExploreTracksCategory[size];
        }
    };
}
