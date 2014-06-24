package com.soundcloud.android.activities;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ActivityProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.adapters.CellPresenter;
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

public class ActivityItemPresenter implements CellPresenter<PropertySet> {
    private final LayoutInflater inflater;
    private final Resources resources;
    private final ImageOperations imageOperations;

    @Inject
    public ActivityItemPresenter(LayoutInflater inflater, Resources resources, ImageOperations imageOperations) {
        this.resources = resources;
        this.inflater = inflater;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return inflater.inflate(R.layout.activity_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> items) {
        final PropertySet propertySet = items.get(position);

        setUserName(itemView, propertySet);
        setTimeElapsed(itemView, propertySet);
        setTitleText(itemView, propertySet);
        setUserAvatar(itemView, propertySet);
    }

    private void setUserAvatar(View itemView, PropertySet propertySet) {
        imageOperations.displayInAdapterView(
                propertySet.get(ActivityProperty.USER_URN),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void setTitleText(View itemView, PropertySet propertySet) {
        final String titleText;
        final int iconId;
        switch (propertySet.get(ActivityProperty.TYPE)) {
            case ActivityProperty.TYPE_FOLLOWER :
                titleText = resources.getString(R.string.started_following_you);
                iconId = R.drawable.stats_followers;
                break;
            case ActivityProperty.TYPE_LIKE :
                titleText = String.format(resources.getString(R.string.liked), propertySet.get(ActivityProperty.SOUND_TITLE));
                iconId = R.drawable.stats_likes;
                break;
            case ActivityProperty.TYPE_REPOST :
                titleText = String.format(resources.getString(R.string.reposted), propertySet.get(ActivityProperty.SOUND_TITLE));
                iconId = R.drawable.stats_repost;
                break;
            case ActivityProperty.TYPE_COMMENT :
                titleText = String.format(resources.getString(R.string.commented_on), propertySet.get(ActivityProperty.SOUND_TITLE));
                iconId = R.drawable.stats_comment;
                break;
            default:
                throw new IllegalArgumentException("Unexpected activity type");
        }
        final TextView titleTextView = (TextView) itemView.findViewById(R.id.title);
        titleTextView.setText(titleText);
        titleTextView.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);
    }

    private void setUserName(View itemView, PropertySet propertySet) {
        ((TextView) itemView.findViewById(R.id.username)).setText(propertySet.get(ActivityProperty.USER_NAME));
    }

    private void setTimeElapsed(View itemView, PropertySet propertySet) {
        final Date date = propertySet.get(ActivityProperty.DATE);
        final long elapsedSeconds = (System.currentTimeMillis() - date.getTime()) / 1000;
        final String formattedTime = ScTextUtils.getTimeString(resources, elapsedSeconds, true);
        ((TextView) itemView.findViewById(R.id.date)).setText(formattedTime);
    }
}
