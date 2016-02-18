package com.soundcloud.android.search;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class SearchPagerAdapter extends FragmentPagerAdapter {

    protected static final int TAB_ALL = SearchOperations.TYPE_ALL;
    protected static final int TAB_TRACKS = SearchOperations.TYPE_TRACKS;
    protected static final int TAB_PLAYLISTS = SearchOperations.TYPE_PLAYLISTS;
    protected static final int TAB_PEOPLE = SearchOperations.TYPE_USERS;

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
                return SearchResultsFragment.create(TAB_ALL, query, firstTime);
            case TAB_TRACKS:
                return SearchResultsFragment.create(TAB_TRACKS, query, false);
            case TAB_PLAYLISTS:
                return SearchResultsFragment.create(TAB_PLAYLISTS, query, false);
            case TAB_PEOPLE:
                return SearchResultsFragment.create(TAB_PEOPLE, query, false);
            default:
                throw new IllegalArgumentException("Unexpected position for getEntityHolder " + position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_ALL:
                return resources.getString(R.string.search_type_all);
            case TAB_TRACKS:
                return resources.getString(R.string.search_type_tracks);
            case TAB_PLAYLISTS:
                return resources.getString(R.string.search_type_playlists);
            case TAB_PEOPLE:
                return resources.getString(R.string.search_type_people);
            default:
                throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

}
