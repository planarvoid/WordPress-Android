package com.soundcloud.android.discovery;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class RecommendedTracksPresenter extends RecyclerViewPresenter<RecommendedTrackItem> implements RecommendedTrackItemRenderer.OnRecommendedTrackClickListener {

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
        //TODO: make it not abstract on android-kit since there is no-op here.
    }

    @Override
    protected CollectionBinding<RecommendedTrackItem> onBuildBinding(Bundle bundle) {
        final long localSeedId = bundle.getLong(EXTRA_LOCAL_SEED_ID);
        adapter.setOnRecommendedTrackClickListener(this);
        return CollectionBinding.from(discoveryOperations.recommendedTracksForSeed(localSeedId))
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable throwable) {
        return null;
    }

    @Override
    public void onRecommendedTrackClicked(RecommendedTrackItem recommendedTrackItem) {
        playRecommendedTracks(recommendedTrackItem.getEntityUrn(), discoveryOperations.recommendedTracks());
    }

    private void playRecommendedTracks(Urn firstTrackUrn, Observable<List<Urn>> playQueue) {
        playbackOperations.playTracks(playQueue, firstTrackUrn, 0,
                new PlaySessionSource(Screen.RECOMMENDATIONS_MORE)).subscribe(expandPlayerSubscriberProvider.get());
    }
}
