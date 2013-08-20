package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExploreTracksCategories {

    private List<ExploreTracksCategory> mMusic;
    private List<ExploreTracksCategory> mAudio;

    @JsonProperty("audio")
    public void setAudio(List<ExploreTracksCategory> audio) {
        this.mAudio = audio;
    }

    @JsonProperty("music")
    public void setMusic(List<ExploreTracksCategory> music) {
        this.mMusic = music;
    }

    public List<ExploreTracksCategory> getMusic() {
        return mMusic;
    }

    public List<ExploreTracksCategory> getAudio() {
        return mAudio;
    }
}
