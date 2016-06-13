package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.RecommendationsTracker.discoveryScreen;
import static com.soundcloud.android.discovery.RecommendationsTracker.recommendationsScreen;
import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static java.util.Collections.singleton;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Provider;
import java.util.List;
import java.util.Locale;

@AutoFactory(allowSubclasses = true)
class RecommendationBucketRenderer implements CellRenderer<RecommendedTracksItem> {

    private final boolean isViewAllBucket;
    private final Screen trackingScreen;
    private final RecommendationRendererFactory rendererFactory;
    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final Navigator navigator;
    private final RecommendationsTracker tracker;

    RecommendationBucketRenderer(
            boolean isViewAllBucket,
            @Provided PlaybackInitiator playbackInitiator,
            @Provided Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
            @Provided RecommendationRendererFactory rendererFactory,
            @Provided Navigator navigator,
            @Provided RecommendationsTracker tracker) {
        this.isViewAllBucket = isViewAllBucket;
        this.trackingScreen = isViewAllBucket ? discoveryScreen() : recommendationsScreen();
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.navigator = navigator;
        this.rendererFactory = rendererFactory;
        this.tracker = tracker;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recommendation_bucket, viewGroup, false);
        initCarousel(view, ButterKnife.<RecyclerView>findById(view, R.id.recommendations_carousel));
        return view;
    }

    private void initCarousel(View bucketView, final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final RecommendationsAdapter adapter = new RecommendationsAdapter(trackingScreen, rendererFactory);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        bucketView.setTag(adapter);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<RecommendedTracksItem> list) {
        final RecommendedTracksItem recommendedTracksItem = list.get(position);

        bindViewAllViews(bucketView);
        bindReasonView(bucketView, recommendedTracksItem);
        bindCarousel((RecommendationsAdapter) bucketView.getTag(), recommendedTracksItem);
    }

    private void bindViewAllViews(View bucketView) {
        final int visibility = isViewAllBucket ? View.VISIBLE : View.GONE;
        final View viewAllButton = ButterKnife.findById(bucketView, R.id.recommendations_view_all);

        ButterKnife.findById(bucketView, R.id.recommendations_header).setVisibility(visibility);
        ButterKnife.findById(bucketView, R.id.recommendations_header_divider).setVisibility(visibility);
        ButterKnife.findById(bucketView, R.id.recommendations_view_all_divider).setVisibility(visibility);
        viewAllButton.setVisibility(visibility);

        if (isViewAllBucket) {
            viewAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigator.openViewAllRecommendations(v.getContext());
                }
            });
        }
    }

    private void bindReasonView(View bucketView, final RecommendedTracksItem bucket) {
        final TextView reasonView = ButterKnife.findById(bucketView, R.id.reason);
        reasonView.setText(getReasonText(bucket, bucketView.getContext()));
        reasonView.setOnClickListener(buildOnReasonClickListener(bucket));
    }

    private void bindCarousel(RecommendationsAdapter adapter, RecommendedTracksItem recommendationBucket) {
        final List<Recommendation> viewModels = recommendationBucket.getRecommendations();
        adapter.clear();
        for (int i = 0; i < viewModels.size(); i++) {
            adapter.addItem(viewModels.get(i));
        }
        adapter.notifyDataSetChanged();
    }

    private Spannable getReasonText(RecommendedTracksItem recommendationBucket, Context context) {
        final String reason = getReasonType(recommendationBucket.getRecommendationReason(), context);
        final String reasonText = context.getString(R.string.recommendation_reason_because_you_reason_tracktitle, reason,
                recommendationBucket.getSeedTrackTitle());
        final int endOfReasonIndex = reasonText.indexOf(reason) + reason.length();

        final Spannable spannable = new SpannableString(reasonText);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.recommendation_reason_text)), 0, endOfReasonIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.seed_track_text)), endOfReasonIndex, reasonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }

    private String getReasonType(RecommendationReason recommendationReason, Context context) {
        switch (recommendationReason) {
            case LIKED:
                return context.getString(R.string.recommendation_reason_liked).toLowerCase(Locale.getDefault());
            case PLAYED:
                return context.getString(R.string.recommendation_reason_played).toLowerCase(Locale.getDefault());
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + recommendationReason);
        }
    }

    @NonNull
    private View.OnClickListener buildOnReasonClickListener(final RecommendedTracksItem bucket) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tracker.trackSeedTrackClick(trackingScreen,  bucket.getSeedTrackUrn(), bucket.getQueryUrn());

                final PlaySessionSource playSessionSource = PlaySessionSource.forRecommendations(trackingScreen,
                        bucket.getSeedTrackQueryPosition(), bucket.getQueryUrn());
                final List<Urn> playQueue = toPlayQueue(bucket.getSeedTrackUrn(), bucket.getRecommendations());

                playbackInitiator.playTracks(playQueue, playQueue.indexOf(bucket.getSeedTrackUrn()), playSessionSource)
                        .subscribe(expandPlayerSubscriberProvider.get());
            }
        };
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private List<Urn> toPlayQueue(Urn seedUrn, List<Recommendation> recommendations) {
        Iterable<Urn> recommendationUrns = transform(recommendations, Recommendation.TO_TRACK_URN);
        return newArrayList(concat(singleton(seedUrn), recommendationUrns));
    }
}
