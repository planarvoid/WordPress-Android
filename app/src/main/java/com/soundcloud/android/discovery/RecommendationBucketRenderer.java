package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.checks.Preconditions;

import android.content.Context;
import android.content.res.Resources;
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

    interface OnRecommendationClickListener {
        void onRecommendationReasonClicked(RecommendationBucket recommendationBucket);

        void onRecommendationArtworkClicked(RecommendationBucket recommendationBucket);

        void onRecommendationViewAllClicked(Context context, RecommendationBucket recommendationBucket);
    }

    private final Resources resources;
    private final ImageOperations imageOperations;

    private OnRecommendationClickListener onRecommendationClickListener;

    @Inject
    RecommendationBucketRenderer(Resources resources, ImageOperations imageOperations) {
        this.resources = resources;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recommendation_bucket, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecommendationBucket> list) {
        final RecommendationBucket recommendationBucket = list.get(position);
        final LinearLayout carouselContainer = ButterKnife.findById(itemView, R.id.recommendations_carousel);
        ButterKnife.<TextView>findById(itemView, R.id.reason).setText(getReasonText(recommendationBucket, itemView.getContext()));

        for (TrackItem trackItem : recommendationBucket.getRecommendations()) {
            final View view = LayoutInflater.from(carouselContainer.getContext()).inflate(R.layout.recommendation_item, carouselContainer, false);
            ButterKnife.<TextView>findById(view, R.id.recommendation_title).setText(trackItem.getTitle());
            ButterKnife.<TextView>findById(view, R.id.recommendation_artist).setText(trackItem.getCreatorName());
            loadArtwork(view, trackItem);
            carouselContainer.addView(view);
        }
    }

    void setOnRecommendationClickListener(OnRecommendationClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.onRecommendationClickListener = listener;
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
