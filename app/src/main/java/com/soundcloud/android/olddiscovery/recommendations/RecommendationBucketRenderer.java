package com.soundcloud.android.olddiscovery.recommendations;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.os.Parcelable;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoFactory(allowSubclasses = true)
public class RecommendationBucketRenderer implements CellRenderer<RecommendedTracksBucketItem> {

    private final boolean isViewAllBucket;
    private final RecommendationRendererFactory rendererFactory;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final NavigationExecutor navigationExecutor;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    private final TrackRecommendationListener listener;
    private final Map<Long, Parcelable> scrollingState = new HashMap<>();

    RecommendationBucketRenderer(
            boolean isViewAllBucket,
            TrackRecommendationListener listener,
            @Provided RecommendationRendererFactory rendererFactory,
            @Provided NavigationExecutor navigationExecutor,
            @Provided PerformanceMetricsEngine performanceMetricsEngine,
            @Provided ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        this.isViewAllBucket = isViewAllBucket;
        this.listener = listener;
        this.navigationExecutor = navigationExecutor;
        this.rendererFactory = rendererFactory;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.recommendation_bucket, viewGroup, false);
        initCarousel(view, ButterKnife.findById(view, R.id.recommendations_carousel));
        return view;
    }

    private void initCarousel(View bucketView, final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final RecommendationsAdapter adapter = new RecommendationsAdapter(rendererFactory.create(listener));

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        bucketView.setTag(adapter);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<RecommendedTracksBucketItem> list) {
        final RecommendedTracksBucketItem recommendedTracksItem = list.get(position);

        bindViewAllViews(bucketView);
        bindReasonView(bucketView, recommendedTracksItem);
        bindCarousel(bucketView, recommendedTracksItem);
    }

    private void bindViewAllViews(View bucketView) {
        final int visibility = isViewAllBucket ? View.VISIBLE : View.GONE;
        final View viewAllButton = ButterKnife.findById(bucketView, R.id.recommendations_view_all);

        ButterKnife.findById(bucketView, R.id.recommendations_header).setVisibility(visibility);
        ButterKnife.findById(bucketView, R.id.recommendations_header_divider).setVisibility(visibility);
        ButterKnife.findById(bucketView, R.id.recommendations_view_all_divider).setVisibility(visibility);
        viewAllButton.setVisibility(visibility);

        if (isViewAllBucket) {
            viewAllButton.setOnClickListener(this::onViewAllButtonClick);
        }
    }

    void onViewAllButtonClick(View v) {
        performanceMetricsEngine.startMeasuring(MetricType.SUGGESTED_TRACKS_LOAD);
        navigationExecutor.openViewAllRecommendations(v.getContext());
    }

    private void bindReasonView(View bucketView, final RecommendedTracksBucketItem bucket) {
        final TextView reasonView = ButterKnife.findById(bucketView, R.id.reason);
        reasonView.setText(getReasonText(bucket, bucketView.getContext()));
        reasonView.setOnClickListener(buildOnReasonClickListener(bucket));
    }

    private void bindCarousel(View bucketView, RecommendedTracksBucketItem recommendedTracksItem) {
        final RecommendationsAdapter adapter = (RecommendationsAdapter) bucketView.getTag();
        final RecyclerView recyclerView = ButterKnife.findById(bucketView, R.id.recommendations_carousel);
        if (adapter.hasBucketItem()) {
            //Save previous scrolling state
            scrollingState.put(adapter.bucketId(), recyclerView.getLayoutManager().onSaveInstanceState());
        }
        //Set new content
        adapter.setRecommendedTracksBucketItem(recommendedTracksItem);
        if (scrollingState.containsKey(adapter.bucketId())) {
            //Apply previous scrolling state
            recyclerView.getLayoutManager().onRestoreInstanceState(scrollingState.get(adapter.bucketId()));
        } else {
            recyclerView.scrollToPosition(0);
        }
    }

    private Spannable getReasonText(RecommendedTracksBucketItem recommendationBucket, Context context) {
        final String trackTitle = recommendationBucket.seedTrackTitle();
        final String reasonText = getRecommendationReason(context, recommendationBucket, trackTitle);

        final Spannable spannable = new SpannableString(reasonText);

        final int trackTitleStart = reasonText.indexOf(trackTitle);
        final int trackTitleEnd = trackTitleStart + trackTitle.length();

        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.recommendation_reason_text)),
                          0,
                          trackTitleStart,
                          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.seed_track_text)),
                          trackTitleStart,
                          trackTitleEnd,
                          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.recommendation_reason_text)),
                          trackTitleEnd,
                          reasonText.length(),
                          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }

    @NonNull
    private String getRecommendationReason(Context context, RecommendedTracksBucketItem recommendationBucket, String trackTitle) {
        final String reasonText;
        switch (recommendationBucket.recommendationReason()) {
            case LIKED:
                reasonText = context.getString(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.RECOMMENDATION_REASON_BECAUSE_YOU_LIKED_TRACKTITLE),
                                               trackTitle);
                break;
            case PLAYED:
                reasonText = context.getString(R.string.recommendation_reason_because_you_played_tracktitle, trackTitle);
                break;
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + recommendationBucket.recommendationReason());
        }
        return reasonText;
    }

    @NonNull
    private View.OnClickListener buildOnReasonClickListener(final RecommendedTracksBucketItem bucket) {
        return v -> listener.onReasonClicked(bucket.seedTrackUrn());
    }
}
