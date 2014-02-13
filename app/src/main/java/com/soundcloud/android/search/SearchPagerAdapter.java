package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class SearchPagerAdapter extends FragmentPagerAdapter {

    protected static final int TAB_ALL = 0;
    protected static final int TAB_TRACKS = 1;
    protected static final int TAB_PLAYLISTS = 2;
    protected static final int TAB_PEOPLE = 3;

    private final Resources mResources;
    private final String mQuery;

    public SearchPagerAdapter(Resources resources, FragmentManager fm, String query) {
        super(fm);
        mResources = resources;
        mQuery = query;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case TAB_ALL:
                return SearchResultsFragment.newInstance(SearchResultsFragment.TYPE_ALL, mQuery);
            case TAB_TRACKS:
                return SearchResultsFragment.newInstance(SearchResultsFragment.TYPE_TRACKS, mQuery);
            case TAB_PLAYLISTS:
                return SearchResultsFragment.newInstance(SearchResultsFragment.TYPE_PLAYLISTS, mQuery);
            case TAB_PEOPLE:
                return SearchResultsFragment.newInstance(SearchResultsFragment.TYPE_PEOPLE, mQuery);
        }
        throw new IllegalArgumentException("Unexpected position for getItem " + position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_ALL:
                return ScTextUtils.formatTabTitle(mResources, R.string.search_type_all);
            case TAB_TRACKS:
                return ScTextUtils.formatTabTitle(mResources, R.string.search_type_tracks);
            case TAB_PLAYLISTS:
                return ScTextUtils.formatTabTitle(mResources, R.string.search_type_playlists);
            case TAB_PEOPLE:
                return ScTextUtils.formatTabTitle(mResources, R.string.search_type_people);
        }
        throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
    }

    @Override
    public int getCount() {
        return 4;
    }

}
