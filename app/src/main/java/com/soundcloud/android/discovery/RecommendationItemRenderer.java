package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.checks.Preconditions;

import android.content.Context;
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
import java.util.Locale;

class RecommendationItemRenderer implements CellRenderer<RecommendationItem> {

    interface OnRecommendationClickListener {
        void onRecommendationReasonClicked(RecommendationItem recommendationItem);

        void onRecommendationArtworkClicked(RecommendationItem recommendationItem);

        void onRecommendationViewAllClicked(Context context, RecommendationItem recommendationItem);
    }

    private final Resources resources;
    private final ImageOperations imageOperations;

    private OnRecommendationClickListener onRecommendationClickListener;

    @Inject
    RecommendationItemRenderer(Resources resources, ImageOperations imageOperations) {
        this.resources = resources;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recommendation_item, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecommendationItem> list) {

        getTextView(itemView, R.id.recommendations_header).setVisibility(position == 0 ? View.VISIBLE : View.GONE);

        final RecommendationItem recommendationItem = list.get(position);
        getTextView(itemView, R.id.reason).setText(getReasonText(recommendationItem));
        getTextView(itemView, R.id.username).setText(recommendationItem.getRecommendationUserName());
        getTextView(itemView, R.id.title).setText(recommendationItem.getRecommendationTitle());
        getTextView(itemView, R.id.view_all).setText(getViewAllText(recommendationItem));
        loadArtwork(itemView, recommendationItem);
        setClickListeners(itemView, recommendationItem);
    }

    private void setClickListeners(View itemView, final RecommendationItem recommendationItem) {
        itemView.findViewById(R.id.reason).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final OnRecommendationClickListener clickListener = RecommendationItemRenderer.this.onRecommendationClickListener;
                if (clickListener != null) {
                    clickListener.onRecommendationReasonClicked(recommendationItem);
                }
            }
        });

        itemView.findViewById(R.id.image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final OnRecommendationClickListener clickListener = RecommendationItemRenderer.this.onRecommendationClickListener;
                if (clickListener != null) {
                    clickListener.onRecommendationArtworkClicked(recommendationItem);
                }
            }
        });

        itemView.findViewById(R.id.view_all_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final OnRecommendationClickListener clickListener = RecommendationItemRenderer.this.onRecommendationClickListener;
                if (clickListener != null) {
                    clickListener.onRecommendationViewAllClicked(v.getContext(), recommendationItem);
                }
            }
        });
    }

    void setOnRecommendationClickListener(OnRecommendationClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.onRecommendationClickListener = listener;
    }

    private String getViewAllText(RecommendationItem recommendationItem) {
        return resources.getString(R.string.recommendation_view_all, recommendationItem.getRecommendationCount());
    }

    private Spannable getReasonText(RecommendationItem recommendationItem) {
        String reason = getReason(recommendationItem.getRecommendationReason());
        String reasonText = resources.getString(R.string.recommendation_reason_because_you, reason,
                recommendationItem.getSeedTrackTitle());

        Spannable spannable = new SpannableString(reasonText);
        int endOfReasonIndex = reasonText.indexOf(reason) + reason.length();
        spannable.setSpan(new ForegroundColorSpan(resources.getColor(R.color.recommendation_reason_text)), 0, endOfReasonIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(resources.getColor(R.color.seed_track_text)), endOfReasonIndex, reasonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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
                return resources.getString(R.string.recommendation_reason_liked).toLowerCase(Locale.getDefault());
            case LISTENED_TO:
                return resources.getString(R.string.recommendation_reason_listened_to).toLowerCase(Locale.getDefault());
            default:
                throw new IllegalArgumentException("Unknown recommendation reason " + recommendationReason);
        }
    }
}
