package com.soundcloud.android.search;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

class SearchPagerAdapter extends FragmentPagerAdapter {

    private final Resources resources;
    private final String query;
    private final boolean firstTime;
    private final List<SearchType> tabs;

    SearchPagerAdapter(Resources resources,
                       FragmentManager fm,
                       String query,
                       boolean firstTime) {
        super(fm);
        this.resources = resources;
        this.query = query;
        this.firstTime = firstTime;
        this.tabs = SearchType.asList();
    }

    @Override
    public Fragment getItem(int position) {
        final SearchType itemType = tabs.get(position);
        final boolean publishSearchSubmissionEvent = itemType.shouldPublishSearchSubmissionEvent();
        return SearchResultsFragment.create(itemType, query, firstTime && publishSearchSubmissionEvent);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabs.get(position).getPageTitle(resources);
    }

    @Override
    public int getCount() {
        return tabs.size();
    }

}
