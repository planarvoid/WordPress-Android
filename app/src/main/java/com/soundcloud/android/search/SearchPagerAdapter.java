package com.soundcloud.android.search;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.Locale;

public class SearchPagerAdapter extends FragmentPagerAdapter {

    protected static final int TAB_ALL = 0;
    protected static final int TAB_TRACKS = 1;
    protected static final int TAB_PLAYLISTS = 2;
    protected static final int TAB_PEOPLE = 3;

    private final Resources resources;
    private final String query;
    private final boolean firstTime;

    public SearchPagerAdapter(Resources resources, FragmentManager fm, String query, boolean firstTime) {
        super(fm);
        this.resources = resources;
        this.query = query;
        this.firstTime = firstTime;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case TAB_ALL:
                return SearchResultsFragment.create(SearchOperations.TYPE_ALL, query, firstTime);
            case TAB_TRACKS:
                return SearchResultsFragment.create(SearchOperations.TYPE_TRACKS, query, false);
            case TAB_PLAYLISTS:
                return SearchResultsFragment.create(SearchOperations.TYPE_PLAYLISTS, query, false);
            case TAB_PEOPLE:
                return SearchResultsFragment.create(SearchOperations.TYPE_USERS, query, false);
            default:
                throw new IllegalArgumentException("Unexpected position for getItem " + position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_ALL:
                return toUpperCase(resources.getString(R.string.search_type_all));
            case TAB_TRACKS:
                return toUpperCase(resources.getString(R.string.search_type_tracks));
            case TAB_PLAYLISTS:
                return toUpperCase(resources.getString(R.string.search_type_playlists));
            case TAB_PEOPLE:
                return toUpperCase(resources.getString(R.string.search_type_people));
            default:
                throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

    private String toUpperCase(String title) {
        return title.toUpperCase(Locale.getDefault());
    }

}
