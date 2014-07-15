package com.soundcloud.android.explore;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExploreGenresSections {

    private List<ExploreGenre> mMusic;
    private List<ExploreGenre> mAudio;

    @JsonProperty("audio")
    public void setAudio(List<ExploreGenre> audio) {
        this.mAudio = audio;
    }

    @JsonProperty("music")
    public void setMusic(List<ExploreGenre> music) {
        this.mMusic = music;
    }

    public List<ExploreGenre> getMusic() {
        return mMusic;
    }

    public List<ExploreGenre> getAudio() {
        return mAudio;
    }
}
