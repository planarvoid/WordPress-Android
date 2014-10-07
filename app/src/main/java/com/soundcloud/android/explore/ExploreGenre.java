package com.soundcloud.android.explore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.model.Link;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExploreGenre implements Parcelable {

    public static final String EXPLORE_GENRE_EXTRA = "category";
    public static final ExploreGenre POPULAR_MUSIC_CATEGORY = new ExploreGenre("Trending Music");
    public static final ExploreGenre POPULAR_AUDIO_CATEGORY = new ExploreGenre("Trending Audio");
    static final String SUGGESTED_TRACKS_LINK_REL = "suggested_tracks";
    private static final int POPULAR_MUSIC_DESCRIPTION = 1;
    private static final int POPULAR_AUDIO_DESCRIPTION = 2;
    public static final Parcelable.Creator<ExploreGenre> CREATOR = new Parcelable.Creator<ExploreGenre>() {
        public ExploreGenre createFromParcel(Parcel in) {
            int description = in.readInt();
            switch (description) {
                case POPULAR_MUSIC_DESCRIPTION:
                    return POPULAR_MUSIC_CATEGORY;

                case POPULAR_AUDIO_DESCRIPTION:
                    return POPULAR_AUDIO_CATEGORY;

                default:
                    return new ExploreGenre(in);
            }
        }

        public ExploreGenre[] newArray(int size) {
            return new ExploreGenre[size];
        }
    };
    private String mTitle;
    private Map<String, Link> mLinks = Collections.emptyMap();

    public ExploreGenre() { /* For Deserialization */ }

    public ExploreGenre(String title) {
        this.mTitle = title;
    }

    @VisibleForTesting
    public ExploreGenre(String title, String suggestedTracksUrl) {
        this.mTitle = title;
        mLinks = new HashMap<String, Link>();
        mLinks.put(SUGGESTED_TRACKS_LINK_REL, new Link(suggestedTracksUrl));
    }

    public ExploreGenre(Parcel in) {
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
}
