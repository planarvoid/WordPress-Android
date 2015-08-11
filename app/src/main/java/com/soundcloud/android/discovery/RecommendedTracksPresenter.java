package com.soundcloud.android.discovery;

import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

public class RecommendedTracksPresenter extends RecyclerViewPresenter<RecommendedTrackItem> {

    private static final String EXTRA_LOCAL_SEED_ID = "localSeedId";

    private final DiscoveryOperations discoveryOperations;
    private final RecommendedTracksAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackOperations playbackOperations;

    @Inject
    RecommendedTracksPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                               DiscoveryOperations discoveryOperations,
                               RecommendedTracksAdapter adapter,
                               Provider<ExpandPlayerSubscriber> subscriberProvider,
                               PlaybackOperations playbackOperations) {
        super(swipeRefreshAttacher, Options.list());
        this.discoveryOperations = discoveryOperations;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = subscriberProvider;
        this.playbackOperations = playbackOperations;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected void onItemClicked(View view, int position) {

    }

    @Override
    protected CollectionBinding<RecommendedTrackItem> onBuildBinding(Bundle bundle) {
        final long localSeedId = bundle.getLong(EXTRA_LOCAL_SEED_ID);
        return CollectionBinding.from(discoveryOperations.recommendedTracksForSeed(localSeedId))
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable throwable) {
        return null;
    }
}
