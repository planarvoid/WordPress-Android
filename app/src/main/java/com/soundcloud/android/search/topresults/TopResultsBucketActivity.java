package com.soundcloud.android.search.topresults;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.BottomNavigationViewPresenter;
import com.soundcloud.android.search.SearchResultsFragment;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class TopResultsBucketActivity extends PlayerActivity implements TopResultsBucketPresenter.TopResultsBucketView {

    public static final String EXTRA_QUERY = "extra_query";
    public static final String EXTRA_QUERY_URN = "extra_query_urn";
    public static final String EXTRA_BUCKET_KIND = "extra_bucket_kind";
    public static final String EXTRA_IS_PREMIUM = "extra_is_premium";
    private static final String TITLE_FORMAT = "\"%s\"";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject TopResultsBucketPresenter presenter;
    @Inject @LightCycle BottomNavigationViewPresenter bottomNavigationViewPresenter;

    public TopResultsBucketActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public Optional<Urn> getQueryUrn() {
        return Urns.optionalUrnFromIntent(getIntent(), EXTRA_QUERY_URN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String query = getIntent().getStringExtra(EXTRA_QUERY);
        setTitle(String.format(TITLE_FORMAT, query));

        if (savedInstanceState == null) {
            final TopResultsBucketViewModel.Kind kind = getKind();
            createFragment(query, getQueryUrn(), kind, getIntent().getBooleanExtra(EXTRA_IS_PREMIUM, false));
        }
        presenter.attachView(this);
    }

    public TopResultsBucketViewModel.Kind getKind() {
        return (TopResultsBucketViewModel.Kind) getIntent().getSerializableExtra(EXTRA_BUCKET_KIND);
    }

    public boolean isPremium() {
        return getIntent().getBooleanExtra(EXTRA_IS_PREMIUM, false);
    }

    @Override
    protected void onDestroy() {
        presenter.detachView();
        super.onDestroy();
    }


    private void createFragment(String query, Optional<Urn> queryUrn, TopResultsBucketViewModel.Kind kind, boolean isPremium) {
        final SearchResultsFragment searchResultsFragment = SearchResultsFragment.createForViewAll(kind.toSearchType(), query, query, queryUrn, Optional.absent(), isPremium);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, searchResultsFragment, SearchResultsFragment.TAG)
                .commit();
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }
}
