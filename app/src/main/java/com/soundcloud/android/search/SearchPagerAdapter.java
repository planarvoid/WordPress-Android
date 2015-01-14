package com.soundcloud.android.search;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class SearchPagerAdapter extends FragmentPagerAdapter {

    protected static final int TAB_ALL = 0;
    protected static final int TAB_TRACKS = 1;
    protected static final int TAB_PLAYLISTS = 2;
    protected static final int TAB_PEOPLE = 3;

    private final Resources resources;
    private final String query;

    public SearchPagerAdapter(Resources resources, FragmentManager fm, String query) {
        super(fm);
        this.resources = resources;
        this.query = query;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case TAB_ALL:
                return SearchResultsFragment.newInstance(SearchOperations.TYPE_ALL, query);
            case TAB_TRACKS:
                return SearchResultsFragment.newInstance(SearchOperations.TYPE_TRACKS, query);
            case TAB_PLAYLISTS:
                return SearchResultsFragment.newInstance(SearchOperations.TYPE_PLAYLISTS, query);
            case TAB_PEOPLE:
                return SearchResultsFragment.newInstance(SearchOperations.TYPE_USERS, query);
            default:
                throw new IllegalArgumentException("Unexpected position for getItem " + position);
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
