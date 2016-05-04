package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.java.checks.Preconditions;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

class RecommendationBucketRenderer implements CellRenderer<RecommendationBucket> {

    private View.OnClickListener onRecommendationClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (onRecommendationBucketClickListener != null) {
                RecommendationBucket recommendationBucket = (RecommendationBucket) view.getTag(R.id.recommendation_bucket_tag);
                TrackItem trackItem = (TrackItem) view.getTag(R.id.recommendation_track_item_tag);

                onRecommendationBucketClickListener.onRecommendationClicked(recommendationBucket, trackItem);
            }
        }
    };
    private View.OnClickListener onReasonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (onRecommendationBucketClickListener != null) {
                RecommendationBucket recommendationBucket = (RecommendationBucket) view.getTag(R.id.recommendation_bucket_tag);
                onRecommendationBucketClickListener.onReasonClicked(recommendationBucket);
            }
        }
    };

    interface OnRecommendationBucketClickListener {
        void onReasonClicked(RecommendationBucket recommendationBucket);

        void onRecommendationClicked(RecommendationBucket recommendationBucket, TrackItem trackItem);

        void onViewAllClicked();
    }

    private final Resources resources;
    private final ImageOperations imageOperations;
    private final TrackItemMenuPresenter trackItemMenuPresenter;
    private OnRecommendationBucketClickListener onRecommendationBucketClickListener;
    @Inject
    RecommendationBucketRenderer(
            Resources resources,
            ImageOperations imageOperations,
            TrackItemMenuPresenter trackItemMenuPresenter) {

        this.resources = resources;
        this.imageOperations = imageOperations;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recommendation_bucket, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<RecommendationBucket> list) {
        // TODO : Unbind before binding the listeners

        final RecommendationBucket recommendationBucket = list.get(position);
        final LinearLayout carouselContainer = ButterKnife.findById(bucketView, R.id.recommendations_carousel);
        TextView reasonView = ButterKnife.findById(bucketView, R.id.reason);
        reasonView.setTag(R.id.recommendation_bucket_tag, recommendationBucket);

        reasonView.setText(getReasonText(recommendationBucket, bucketView.getContext()));

        for (TrackItem trackItem : recommendationBucket.getRecommendations()) {
            final View recommendationView = LayoutInflater.from(carouselContainer.getContext()).inflate(R.layout.recommendation_item, carouselContainer, false);
            ButterKnife.<TextView>findById(recommendationView, R.id.recommendation_title).setText(trackItem.getTitle());
            ButterKnife.<TextView>findById(recommendationView, R.id.recommendation_artist).setText(trackItem.getCreatorName());
            setOverflowClickListener(ButterKnife.<ImageView>findById(recommendationView, R.id.overflow_button), trackItem);
            loadArtwork(recommendationView, trackItem);
            carouselContainer.addView(recommendationView);
            recommendationView.setTag(R.id.recommendation_bucket_tag, recommendationBucket);
            recommendationView.setTag(R.id.recommendation_track_item_tag, trackItem);
            setOnRecommendationItemListener(recommendationView);
        }

        setOnReasonClickListener(reasonView);
    }

    private void setOnRecommendationItemListener(View recommendationView) {
        recommendationView.setOnClickListener(onRecommendationClickListener);
    }

    private void setOnReasonClickListener(TextView reasonView) {
        reasonView.setOnClickListener(onReasonClickListener);
    }

    void setOnRecommendationBucketClickListener(OnRecommendationBucketClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.onRecommendationBucketClickListener = listener;
    }

    private void setOverflowClickListener(final ImageView button, final TrackItem trackItem) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackItemMenuPresenter.show((FragmentActivity) button.getContext(), button, trackItem, 0);
            }
        });
    }

    private Spannable getReasonText(RecommendationBucket recommendationBucket, Context context) {
        String reason = getReason(recommendationBucket.getRecommendationReason());
        String reasonText = resources.getString(R.string.recommendation_reason_because_you_reason_tracktitle, reason,
                recommendationBucket.getSeedTrackTitle());

        Spannable spannable = new SpannableString(reasonText);
        int endOfReasonIndex = reasonText.indexOf(reason) + reason.length();

        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.recommendation_reason_text)), 0, endOfReasonIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.seed_track_text)), endOfReasonIndex, reasonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }

    private void loadArtwork(View itemView, TrackItem trackItem) {
        final ApiImageSize apiImageSize = ApiImageSize.getFullImageSize(itemView.getResources());
        imageOperations.displayInAdapterView(trackItem,
                apiImageSize, ButterKnife.<ImageView>findById(itemView, R.id.recommendation_artwork));
    }

    private String getReason(RecommendationReason recommendationReason) {
        switch (recommendationReason) {
            case LIKED:
                return resources.getString(R.string.recommendation_reason_liked).toLowerCase(Locale.getDefault());
            case LISTENED_TO:
                return resources.getString(R.string.recommendation_reason_listened_to).toLowerCase(Locale.getDefault());
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + recommendationReason);
        }
    }
}
