package com.soundcloud.android.activities;

import static com.soundcloud.android.api.legacy.model.activities.Activity.Type;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.tasks.CollectionParams;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.storage.ActivitiesStorage;
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

    private final ActivitiesStorage activitiesStorage;

    @Inject ImageOperations imageOperations;
    @Inject PlaybackInitiator playbackInitiator;
    @Inject ActivityItemRenderer itemRenderer;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;
    @Inject Navigator navigator;


    public ActivitiesAdapter(Uri uri) {
        super(uri);
        activitiesStorage = new ActivitiesStorage();
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ActivitiesAdapter(Uri uri, ImageOperations imageOperations, PlaybackInitiator playbackInitiator, ActivityItemRenderer itemRenderer) {
        super(uri);
        this.activitiesStorage = new ActivitiesStorage();
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

    @Override
    public CollectionParams getParams(boolean refresh) {
        CollectionParams params = super.getParams(refresh);
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

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {

        Type type = Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
            case PLAYLIST:
            case PLAYLIST_SHARING:
                playTrackOrStartPlaylistFragment(context, position);
                return ItemClickResults.LEAVING;

            case COMMENT:
            case USER_MENTION:
                context.startActivity(new Intent(context, TrackCommentsActivity.class)
                        .putExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK, getItem(position).getPlayable().toPropertySet()));
                return ItemClickResults.LEAVING;

            case TRACK_LIKE:
            case TRACK_REPOST:
                if (content == Content.ME_ACTIVITIES) {
                    navigator.openProfile(context, getItem(position).getUser().getUrn());
                } else {
                    playTrackOrStartPlaylistFragment(context, position);
                }
                return ItemClickResults.LEAVING;
            case PLAYLIST_LIKE:
            case PLAYLIST_REPOST:
                if (content == Content.ME_ACTIVITIES) {
                    navigator.openProfile(context, getItem(position).getUser().getUrn());
                } else {
                    playTrackOrStartPlaylistFragment(context, position);
                }
                return ItemClickResults.LEAVING;

            case AFFILIATION:
                navigator.openProfile(context, getItem(position).getUser().getUrn());
                return ItemClickResults.LEAVING;

            default:
                Log.i(SoundCloudApplication.TAG, "Clicked on item " + id);
        }
        return ItemClickResults.IGNORE;
    }

    private void playTrackOrStartPlaylistFragment(Context context, int position) {
        Playable playable = data.get(position).getPlayable();
        if (playable instanceof PublicApiTrack) {
            List<Urn> trackUrns = toTrackUrn(filterPlayables(data));
            int adjustedPosition = filterPlayables(data.subList(0, position)).size();
            Urn initialTrack = trackUrns.get(adjustedPosition);
            playbackInitiator
                    .playTracksFromUri(contentUri, adjustedPosition, initialTrack, new PlaySessionSource(Screen.STREAM))
                    .subscribe(subscriberProvider.get());
        } else if (playable instanceof PublicApiPlaylist) {
            PlaylistDetailActivity.start(context, playable.getUrn(), Screen.ACTIVITIES);
        }
    }

}
