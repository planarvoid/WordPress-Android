package com.soundcloud.android.recommendations;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.LightCycle;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collections;
import java.util.List;

public class RecommendationsPresenter extends RecyclerViewPresenter<RecommendationItem> {

    final @LightCycle RecommendationsView recommendationsView;

    private final RecommendationsOperations recommendationsOperations;
    private final RecommendationsAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackOperations playbackOperations;

    @Inject
    RecommendationsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                             RecommendationsOperations recommendationsOperations,
                             RecommendationsView recommendationsView,
                             RecommendationsAdapter adapter,
                             Provider<ExpandPlayerSubscriber> subscriberProvider,
                             PlaybackOperations playbackOperations) {
        super(swipeRefreshAttacher, Options.cards());
        this.recommendationsOperations = recommendationsOperations;
        this.recommendationsView = recommendationsView;
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
    protected void onItemClicked(View view, int i) {
        //TODO: make it not abstract on android-kit.
        //no op.
    }

    @Override
    protected CollectionBinding<RecommendationItem> onBuildBinding(Bundle bundle) {
        return CollectionBinding.from(recommendationsOperations.recommendations(), RecommendationItem.fromPropertySets())
                .withAdapter(adapter)
                .build();
    }

    private void playTracks(List<Urn> playableUrns, PlaySessionSource playSessionSource) {
        playbackOperations.playTracks(playableUrns, 0, playSessionSource).subscribe(expandPlayerSubscriberProvider.get());
    }

    @Override
    protected EmptyView.Status handleError(Throwable throwable) {
        return null;
    }
}
