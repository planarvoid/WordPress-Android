package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

class SearchPagerAdapter extends FragmentPagerAdapter {

    private final Resources resources;
    private final String apiQuery;
    private final String userQuery;
    private final Optional<Urn> queryUrn;
    private final Optional<Integer> queryPosition;
    private final boolean firstTime;
    private final List<SearchType> tabs;

    SearchPagerAdapter(Resources resources,
                       FragmentManager fm,
                       String apiQuery,
                       String userQuery,
                       Optional<Urn> queryUrn,
                       Optional<Integer> queryPosition,
                       boolean firstTime) {
        super(fm);
        this.resources = resources;
        this.apiQuery = apiQuery;
        this.userQuery = userQuery;
        this.queryUrn = queryUrn;
        this.queryPosition = queryPosition;
        this.firstTime = firstTime;
        this.tabs = SearchType.asList();
    }

    @Override
    public Fragment getItem(int position) {
        final SearchType itemType = tabs.get(position);
        final boolean publishSearchSubmissionEvent = itemType.shouldPublishSearchSubmissionEvent();
        return SearchResultsFragment.create(itemType, apiQuery, userQuery, queryUrn, queryPosition, firstTime && publishSearchSubmissionEvent);
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
