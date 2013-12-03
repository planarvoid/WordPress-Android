package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.SectionedAdapter;
import com.soundcloud.android.model.ExploreGenre;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;


class ExploreGenresAdapter extends SectionedAdapter<ExploreGenre> {

    static final int AUDIO_SECTION = 0;
    static final int MUSIC_SECTION = 1;
    private final LayoutInflater mLayoutInflater;

    @Inject
    public ExploreGenresAdapter(LayoutInflater layoutInflater) {
        mLayoutInflater = layoutInflater;
    }


    @Override
    protected ExploreGenreCategoryRow createItemView(int position, ViewGroup parent) {
        return (ExploreGenreCategoryRow) mLayoutInflater.inflate(R.layout.explore_genre_item, parent, false);
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        super.bindItemView(position, itemView);
        String genreTitle = getItem(position).getTitle();
        ((ExploreGenreCategoryRow) itemView).setDisplayName(genreTitle);

        switch (getSection(position).getSectionId()) {
            case AUDIO_SECTION:
                itemView.setTag(Screen.EXPLORE_AUDIO_GENRE.get(genreTitle));
                break;
            case MUSIC_SECTION:
                itemView.setTag(Screen.EXPLORE_MUSIC_GENRE.get(genreTitle));
                break;
            default:
                IllegalArgumentException up = new IllegalArgumentException("Unrecognised genre section, cannot generate screen tag");
                throw up;
        }
    }
}
