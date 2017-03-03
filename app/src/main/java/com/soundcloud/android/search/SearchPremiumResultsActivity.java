package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.checks.Preconditions;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.List;

public class SearchPremiumResultsActivity extends PlayerActivity {

    public static final String EXTRA_SEARCH_QUERY = "searchQuery";
    public static final String EXTRA_SEARCH_TYPE = "searchType";
    public static final String EXTRA_SEARCH_QUERY_URN = "searchQueryUrn";
    public static final String EXTRA_PREMIUM_CONTENT_RESULTS = "searchPremiumContent";
    public static final String EXTRA_PREMIUM_CONTENT_NEXT_HREF = "searchPremiumNextHref";

    @Inject BaseLayoutHelper baseLayoutHelper;

    public SearchPremiumResultsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

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
        checkIntentArguments(intent);
        final Fragment fragment = SearchPremiumResultsFragment.create(intent.getExtras());
        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.container, fragment, SearchPremiumResultsFragment.TAG)
                                   .commit();
    }

    private void checkIntentArguments(Intent intent) {
        final List<Urn> premiumContentList = intent.getParcelableArrayListExtra(EXTRA_PREMIUM_CONTENT_RESULTS);
        Preconditions.checkState(premiumContentList != null && !premiumContentList.isEmpty(),
                                 "Invalid search premium content list");
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
