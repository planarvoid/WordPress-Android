package com.soundcloud.android.discovery.recommendations;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
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
import java.util.Locale;
import java.util.Map;

@AutoFactory(allowSubclasses = true)
public class RecommendationBucketRenderer implements CellRenderer<RecommendedTracksBucketItem> {

    private final boolean isViewAllBucket;
    private final RecommendationRendererFactory rendererFactory;
    private final Navigator navigator;

    private final TrackRecommendationListener listener;
    private final Map<Long, Parcelable> scrollingState = new HashMap<>();

    RecommendationBucketRenderer(
            boolean isViewAllBucket,
            TrackRecommendationListener listener,
            @Provided RecommendationRendererFactory rendererFactory,
            @Provided Navigator navigator) {
        this.isViewAllBucket = isViewAllBucket;
        this.listener = listener;
        this.navigator = navigator;
        this.rendererFactory = rendererFactory;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.recommendation_bucket, viewGroup, false);
        initCarousel(view, ButterKnife.<RecyclerView>findById(view, R.id.recommendations_carousel));
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
            viewAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigator.openViewAllRecommendations(v.getContext());
                }
            });
        }
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
        final String reason = getReasonType(recommendationBucket.getRecommendationReason(), context);
        final String reasonText = context.getString(R.string.recommendation_reason_because_you_reason_tracktitle,
                                                    reason,
                                                    recommendationBucket.getSeedTrackTitle());
        final int endOfReasonIndex = reasonText.indexOf(reason) + reason.length();

        final Spannable spannable = new SpannableString(reasonText);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.recommendation_reason_text)),
                          0,
                          endOfReasonIndex,
                          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.seed_track_text)),
                          endOfReasonIndex,
                          reasonText.length(),
                          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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
    private View.OnClickListener buildOnReasonClickListener(final RecommendedTracksBucketItem bucket) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onReasonClicked(bucket.getSeedTrackUrn());
            }
        };
    }
}
