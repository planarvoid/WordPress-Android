package com.soundcloud.android.search.topresults;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.search.PlaylistResultsFragment;
import com.soundcloud.android.search.SearchResultsFragment;
import com.soundcloud.android.search.topresults.TopResults.Bucket;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.optional.Optional;

import android.os.Bundle;

import javax.inject.Inject;

public class TopResultsBucketActivity extends PlayerActivity {

    public static final String EXTRA_QUERY = "extra_query";
    public static final String EXTRA_BUCKET_KIND = "extra_bucket_kind";
    public static final String EXTRA_IS_PREMIUM = "extra_is_premium";
    private static final String TITLE_FORMAT = "\"%s\"";

    @Inject BaseLayoutHelper baseLayoutHelper;

    public TopResultsBucketActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String query = getIntent().getStringExtra(EXTRA_QUERY);
        final boolean isPremium = getIntent().getBooleanExtra(EXTRA_IS_PREMIUM, false);
        setTitle(String.format(TITLE_FORMAT, query));

        if (savedInstanceState == null) {
            final Bucket.Kind kind = getKind();
            createFragmentForPlaylistDiscovery(query, kind, isPremium);
        }
    }

    private Bucket.Kind getKind() {
        return (Bucket.Kind) getIntent().getSerializableExtra(EXTRA_BUCKET_KIND);
    }

    private void createFragmentForPlaylistDiscovery(String query, Bucket.Kind kind, boolean isPremium) {
        final SearchResultsFragment searchResultsFragment = SearchResultsFragment.createForViewAll(kind.toSearchType(), query, query, Optional.absent(), Optional.absent(), isPremium);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, searchResultsFragment, PlaylistResultsFragment.TAG)
                .commit();
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    public Screen getScreen() {
        return getKind().toSearchType().getScreen();
    }
}
