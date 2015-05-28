package com.soundcloud.android.activities;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.adapters.CellRenderer;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class ActivityItemRenderer implements CellRenderer<PropertySet> {
    private final Resources resources;
    private final ImageOperations imageOperations;

    @Inject
    public ActivityItemRenderer(Resources resources, ImageOperations imageOperations) {
        this.resources = resources;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.engagement_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> items) {
        final PropertySet propertySet = items.get(position);

        setUserName(itemView, propertySet);
        setTimeElapsed(itemView, propertySet);
        setMainText(itemView, propertySet);
        setUserAvatar(itemView, propertySet);
    }

    private void setUserAvatar(View itemView, PropertySet propertySet) {
        imageOperations.displayInAdapterView(
                propertySet.get(ActivityProperty.USER_URN),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void setMainText(View itemView, PropertySet propertySet) {
        final String titleText;
        final int iconId;
        switch (propertySet.get(ActivityProperty.TYPE)) {
            case ActivityProperty.TYPE_FOLLOWER :
                titleText = resources.getString(R.string.started_following_you);
                iconId = R.drawable.stats_followers;
                break;
            case ActivityProperty.TYPE_LIKE :
                titleText = String.format(resources.getString(R.string.liked), propertySet.get(ActivityProperty.SOUND_TITLE));
                iconId = R.drawable.stats_likes_grey;
                break;
            case ActivityProperty.TYPE_REPOST :
                titleText = String.format(resources.getString(R.string.reposted), propertySet.get(ActivityProperty.SOUND_TITLE));
                iconId = R.drawable.stats_repost;
                break;
            case ActivityProperty.TYPE_COMMENT :
                titleText = String.format(resources.getString(R.string.commented_on), propertySet.get(ActivityProperty.SOUND_TITLE));
                iconId = R.drawable.stats_comment;
                break;
            case ActivityProperty.TYPE_USER_MENTION :
                titleText = String.format(resources.getString(R.string.mentioned_you_on), propertySet.get(ActivityProperty.SOUND_TITLE));
                iconId = R.drawable.stats_comment;
                break;
            default:
                throw new IllegalArgumentException("Unexpected activity type");
        }
        final TextView titleTextView = (TextView) itemView.findViewById(R.id.body);
        titleTextView.setText(titleText);
        titleTextView.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);
    }

    private void setUserName(View itemView, PropertySet propertySet) {
        ((TextView) itemView.findViewById(R.id.username)).setText(propertySet.get(ActivityProperty.USER_NAME));
    }

    private void setTimeElapsed(View itemView, PropertySet propertySet) {
        final Date date = propertySet.get(ActivityProperty.DATE);
        final String formattedTime = ScTextUtils.formatTimeElapsedSince(resources, date.getTime(), true);
        ((TextView) itemView.findViewById(R.id.date)).setText(formattedTime);
    }
}
