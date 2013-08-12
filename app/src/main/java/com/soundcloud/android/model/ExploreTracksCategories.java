package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ExploreTracksCategories {

    private List<ExploreTracksCategory> mMusic;
    private List<ExploreTracksCategory> mAudio;
    private Map<String, Link> mLinks;

    public List<ExploreTracksCategory> getAudio() {
        return mAudio;
    }

    @JsonProperty("audio")
    public void setAudio(List<ExploreTracksCategory> audio) {
        this.mAudio = audio;
    }

    public List<ExploreTracksCategory> getMusic() {
        return mMusic;
    }

    @JsonProperty("music")
    public void setMusic(List<ExploreTracksCategory> music) {
        this.mMusic = music;
    }

    public Map<String, Link> getLinks() {
        return mLinks;
    }

    @JsonProperty("_links")
    public void setLinks(Map<String, Link> links) {
        this.mLinks = links;
    }
}
