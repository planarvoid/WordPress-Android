package com.soundcloud.android.recommendations;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class RecommendationItemRenderer implements CellRenderer<RecommendationItem> {

    private final Resources resources;
    private final ImageOperations imageOperations;

    @Inject
    public RecommendationItemRenderer(Resources resources, ImageOperations imageOperations) {
        this.resources = resources;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recommendation_item, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecommendationItem> recommendations) {
        RecommendationItem recommendationItem = recommendations.get(position);
        getTextView(itemView, R.id.reason).setText(getReasonText(recommendationItem));
        getTextView(itemView, R.id.username).setText(recommendationItem.getRecommendationUserName());
        getTextView(itemView, R.id.title).setText(recommendationItem.getRecommendationTitle());
        getTextView(itemView, R.id.view_all).setText(getViewAllText(recommendationItem));
        loadArtwork(itemView, recommendationItem);
    }

    private String getViewAllText(RecommendationItem recommendationItem) {
        return resources.getString(R.string.recommendation_view_all, recommendationItem.getRecommendationCount());
    }

    private Spannable getReasonText(RecommendationItem recommendationItem) {
        String reason = getReason(recommendationItem.getRecommendationReason());
        String string = resources.getString(R.string.recommendation_reason_because_you, reason,
                recommendationItem.getSeedTrackTitle());

        Spannable spannable = new SpannableString(string);

        int endOfReasonIndex = string.indexOf(reason) + reason.length();
        spannable.setSpan(new ForegroundColorSpan(resources.getColor(R.color.recommendation_reason_text)), 0, endOfReasonIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(resources.getColor(R.color.seed_track_text)), endOfReasonIndex, string.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }

    private void loadArtwork(View itemView, RecommendationItem recommendationItem) {
        final ApiImageSize apiImageSize = ApiImageSize.getFullImageSize(itemView.getResources());
        imageOperations.displayInAdapterView(recommendationItem.getRecommendationUrn(),
                apiImageSize, (ImageView) itemView.findViewById(R.id.image));
    }

    private String getReason(RecommendationReason recommendationReason) {
        switch (recommendationReason) {
            case LIKED:
                return resources.getString(R.string.recommendation_reason_liked).toLowerCase();
            case LISTENED_TO:
                return resources.getString(R.string.recommendation_reason_listened_to).toLowerCase();
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + recommendationReason);
        }
    }
}
