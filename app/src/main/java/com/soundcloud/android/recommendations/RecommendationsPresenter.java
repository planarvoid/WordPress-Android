package com.soundcloud.android.recommendations;

import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import rx.Subscription;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import javax.inject.Inject;

public class RecommendationsPresenter extends DefaultSupportFragmentLightCycle<RecommendationsFragment> {

    private final PlaybackOperations playbackOperations;
    private final RecommendationsOperations recommendationsOperations;
    private final EventBus eventBus;

    private RecommendationsView recommendationsView;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    RecommendationsPresenter(PlaybackOperations playbackOperations,
                             RecommendationsOperations recommendationsOperations,
                             EventBus eventBus) {
        this.playbackOperations = playbackOperations;
        this.recommendationsOperations = recommendationsOperations;
        this.eventBus = eventBus;
    }

    public void setView(@NonNull RecommendationsView recommendationsView) {
        this.recommendationsView = recommendationsView;
    }

    @Override
    public void onViewCreated(final RecommendationsFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        if (recommendationsView == null) {
            throw new IllegalStateException("Presenter must reference a RecommendationsView. You must call setView()");
        }
        recommendationsView.bindViews(fragment.getActivity(), view);
    }

    @Override
    public void onDestroyView(RecommendationsFragment fragment) {
        recommendationsView.unbindViews();
        subscription.unsubscribe();
        super.onDestroyView(fragment);
    }
}
