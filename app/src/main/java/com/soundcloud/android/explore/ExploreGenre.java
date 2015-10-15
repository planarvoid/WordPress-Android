package com.soundcloud.android.explore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExploreGenre implements Parcelable {

    public static final String EXPLORE_GENRE_EXTRA = "genre";
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
    private String title;
    private Map<String, Link> links = Collections.emptyMap();

    public ExploreGenre() { /* For Deserialization */ }

    public ExploreGenre(String title) {
        this.title = title;
    }

    @VisibleForTesting
    public ExploreGenre(String title, String suggestedTracksUrl) {
        this.title = title;
        links = new HashMap<>();
        links.put(SUGGESTED_TRACKS_LINK_REL, new Link(suggestedTracksUrl));
    }

    public ExploreGenre(Parcel in) {
        Bundle b = in.readBundle(Link.class.getClassLoader());
        title = b.getString("title");
        links = (Map<String, Link>) b.getSerializable("links");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("_links")
    public Map<String, Link> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    public String getSuggestedTracksPath() {
        return links.get(SUGGESTED_TRACKS_LINK_REL).getHref();
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
        b.putString("title", title);
        b.putSerializable("links", (Serializable) links);
        dest.writeBundle(b);
    }
}
