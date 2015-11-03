package com.soundcloud.android.activities;

import static com.soundcloud.android.api.legacy.model.activities.Activity.Type;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.tasks.CollectionParams;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.storage.LegacyActivitiesStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.java.collections.PropertySet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivitiesAdapter extends ScBaseAdapter<Activity> {

    private final LegacyActivitiesStorage activitiesStorage;
    private final Content content = Content.ME_ACTIVITIES;
    private final Uri contentUri = content.uri;

    @Inject ImageOperations imageOperations;
    @Inject PlaybackInitiator playbackInitiator;
    @Inject ActivityItemRenderer itemRenderer;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;
    @Inject Navigator navigator;


    public ActivitiesAdapter() {
        activitiesStorage = new LegacyActivitiesStorage();
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ActivitiesAdapter(ImageOperations imageOperations, PlaybackInitiator playbackInitiator,
                      ActivityItemRenderer itemRenderer) {
        this.activitiesStorage = new LegacyActivitiesStorage();
        this.imageOperations = imageOperations;
        this.playbackInitiator = playbackInitiator;
        this.itemRenderer = itemRenderer;
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount() + Type.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        int type = super.getItemViewType(position);
        if (type == IGNORE_ITEM_VIEW_TYPE) {
            return type;
        }

        return getItem(position).getType().ordinal();
    }

    public boolean isExpired(LocalCollection localCollection) {
        if (localCollection == null) {
            return false;
        } else if (data.size() == 0) {
            return true; // need to pull from DB
        } else {
            // check if there is anything newer
            // TODO: DB access on UI thread!
            final Activity latestActivity = activitiesStorage.getLatestActivity(content);
            return (latestActivity == null || latestActivity.getCreatedAt().getTime() > data.get(0).getCreatedAt().getTime());
        }
    }

    @Override
    protected View createRow(Context context, int position, ViewGroup parent) {
        return itemRenderer.createItemView(parent);
    }

    @Override
    protected void bindRow(int index, View rowView) {
        itemRenderer.bindItemView(index, rowView, toPropertySets(getItems()));
    }

    private List<PropertySet> toPropertySets(List<Activity> activities) {
        List<PropertySet> propertySets = new ArrayList<>(activities.size());
        for (Activity activity : activities) {
            propertySets.add(activity.toPropertySet());
        }
        return propertySets;
    }

    public CollectionParams getParams(boolean refresh) {
        CollectionParams params = new CollectionParams();
        params.loadModel = content.modelType;
        params.isRefresh = refresh;
        params.maxToLoad = Consts.LIST_PAGE_SIZE;
        params.startIndex = refresh ? 0 : page * Consts.LIST_PAGE_SIZE;
        params.contentUri = contentUri;
        if (data.size() > 0) {
            Activity first = getItem(0);
            Activity last = getItem(getItemCount() - 1);
            params.timestamp = refresh ? first.getCreatedAt().getTime() : last.getCreatedAt().getTime();
        }
        return params;
    }


    @Override
    public void addItems(List<Activity> newItems) {
        for (Activity newItem : newItems) {
            if (!data.contains(newItem)) {
                data.add(newItem);
            }
        }
        Collections.sort(data);
    }

    @Override
    protected void onSuccessfulRefresh() {
        // do nothing for now. new items will be merged and sorted with the existing items
    }

    public void handleListItemClick(Context context, int position, long id) {

        Type type = Type.values()[getItemViewType(position)];
        switch (type) {
            case COMMENT:
            case USER_MENTION:
                context.startActivity(new Intent(context, TrackCommentsActivity.class)
                        .putExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK, getItem(position).getPlayable().toPropertySet()));
                break;

            case TRACK_LIKE:
            case TRACK_REPOST:
                navigator.openProfile(context, getItem(position).getUser().getUrn());
                break;
            case PLAYLIST_LIKE:
            case PLAYLIST_REPOST:
                navigator.openProfile(context, getItem(position).getUser().getUrn());
                break;

            case AFFILIATION:
                navigator.openProfile(context, getItem(position).getUser().getUrn());
                break;

            default:
                Log.i(SoundCloudApplication.TAG, "Clicked on item " + id);
                break;
        }
    }

}
