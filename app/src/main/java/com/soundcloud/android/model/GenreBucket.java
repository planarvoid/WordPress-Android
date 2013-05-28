package com.soundcloud.android.model;

import java.util.List;

public class GenreBucket extends ScModel {

    private Genre mGenre;
    private List<User> mCreators;

    public Genre getGenre() {
        return mGenre;
    }

    public void setGenre(Genre genre) {
        this.mGenre = genre;
    }

    public List<User> getCreators() {
        return mCreators;
    }

    public void setCreators(List<User> creators) {
        this.mCreators = creators;
    }
}
