package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.collections.PropertySet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.ArrayList;

public class SearchPremiumResultsActivity extends PlayerActivity {

    public static final String EXTRA_SEARCH_QUERY = "searchQuery";
    public static final String EXTRA_SEARCH_TYPE = "searchType";
    public static final String EXTRA_PREMIUM_CONTENT_RESULTS = "searchPremiumContent";
    public static final String EXTRA_PREMIUM_CONTENT_NEXT_HREF = "searchPremiumNextHref";

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String searchQuery = getIntent().getStringExtra(EXTRA_SEARCH_QUERY);
        setTitle("\"" + searchQuery + "\"");
        if (savedInstanceState == null) {
            createFragmentForPremiumResults();
        }
    }

    private void createFragmentForPremiumResults() {
        final Intent intent = getIntent();
        final String searchQuery = intent.getStringExtra(EXTRA_SEARCH_QUERY);
        final int searchType = intent.getIntExtra(EXTRA_SEARCH_TYPE, 0);
        final ArrayList<PropertySet> premiumContentList = intent.getParcelableArrayListExtra(EXTRA_PREMIUM_CONTENT_RESULTS);
        final Link nextHref = intent.getParcelableExtra(EXTRA_PREMIUM_CONTENT_NEXT_HREF);
        Preconditions.checkState(premiumContentList != null && !premiumContentList.isEmpty(), "Invalid search premium content list");
        final Fragment fragment = SearchPremiumResultsFragment.create(searchQuery, searchType, premiumContentList, nextHref);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, SearchPremiumResultsFragment.TAG).commit();
    }

    @Override
    public Screen getScreen() {
        return Screen.SEARCH_PREMIUM_CONTENT;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

}
