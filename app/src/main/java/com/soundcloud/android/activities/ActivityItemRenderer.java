package com.soundcloud.android.activities;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class ActivityItemRenderer implements CellRenderer<ActivityItem> {
    private final Resources resources;
    private final ImageOperations imageOperations;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Inject
    ActivityItemRenderer(Resources resources,
                         ImageOperations imageOperations,
                         ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                         ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        this.resources = resources;
        this.imageOperations = imageOperations;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.engagement_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ActivityItem> items) {
        final ActivityItem activityItem = items.get(position);

        setUserName(itemView, activityItem);
        setTimeElapsed(itemView, activityItem);
        setMainText(itemView, activityItem);
        setUserAvatar(itemView, activityItem);
        showProBadge(itemView, activityItem);
    }

    private void showProBadge(View itemView, ActivityItem activityItem) {
        final int visibility = activityItem.getUserIsPro() ? View.VISIBLE : View.GONE;
        itemView.findViewById(R.id.pro_badge).setVisibility(visibility);
    }

    private void setUserAvatar(View itemView, ActivityItem activityItem) {
        imageOperations.displayCircularInAdapterView(
                Optional.of(activityItem.getUrn()),
                activityItem.getImageUrlTemplate(),
                ApiImageSize.getListItemImageSize(resources),
                itemView.findViewById(R.id.image));
    }

    private void setMainText(View itemView, ActivityItem activityItem) {
        final String titleText;
        final int iconId;
        switch (activityItem.getKind()) {
            case USER_FOLLOW:
                titleText = resources.getString(R.string.notification_username_started_following_you);
                iconId = R.drawable.stats_followers;
                break;
            case TRACK_LIKE:
            case PLAYLIST_LIKE:
                titleText = String.format(
                        resources.getString(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.NOTIFICATION_USERNAME_LIKED_TRACKTITLE)),
                        activityItem.getPlayableTitle());
                iconId = changeLikeToSaveExperiment.isEnabled()
                         ? R.drawable.stats_added_grey
                         : R.drawable.stats_likes_grey;
                break;
            case TRACK_REPOST:
            case PLAYLIST_REPOST:
                titleText = String.format(
                        resources.getString(R.string.notification_username_reposted_tracktitle),
                        activityItem.getPlayableTitle());
                iconId = R.drawable.stats_repost;
                break;
            case TRACK_COMMENT:
                titleText = String.format(
                        resources.getString(R.string.notification_username_commented_on_tracktitle),
                        activityItem.getPlayableTitle());
                iconId = R.drawable.stats_comment;
                break;
            default:
                throw new IllegalArgumentException("Unexpected activity type");
        }
        final TextView titleTextView = itemView.findViewById(R.id.body);
        titleTextView.setText(titleText);
        titleTextView.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);
    }

    private void setUserName(View itemView, ActivityItem activityItem) {
        ((TextView) itemView.findViewById(R.id.username)).setText(activityItem.getUserName());
    }

    private void setTimeElapsed(View itemView, ActivityItem activityItem) {
        final Date date = activityItem.getCreatedAt();
        final String formattedTime = ScTextUtils.formatTimeElapsedSince(resources, date.getTime(), true);
        ((TextView) itemView.findViewById(R.id.date)).setText(formattedTime);
    }
}
