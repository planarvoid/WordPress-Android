package com.soundcloud.android.activities;

import static com.soundcloud.android.api.legacy.model.activities.Activity.Type;
import static com.soundcloud.android.associations.PlayableInteractionActivity.EXTRA_INTERACTION_TYPE;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.associations.PlayableInteractionActivity;
import com.soundcloud.android.associations.PlaylistInteractionActivity;
import com.soundcloud.android.associations.TrackInteractionActivity;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.tasks.CollectionParams;
import com.soundcloud.android.crop.util.VisibleForTesting;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.view.adapters.PlayQueueChangedSubscriber;
import com.soundcloud.propeller.PropertySet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivitiesAdapter extends ScBaseAdapter<Activity> {

    private final ActivitiesStorage activitiesStorage;

    @Inject ImageOperations imageOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject ActivityItemPresenter activityItemPresenter;
    @Inject EventBus eventBus;


    public ActivitiesAdapter(Uri uri) {
        super(uri);
        activitiesStorage = new ActivitiesStorage();
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ActivitiesAdapter(Uri uri, ImageOperations imageOperations, PlaybackOperations playbackOperations, ActivityItemPresenter presenter, EventBus eventBus) {
        super(uri);
        this.activitiesStorage = new ActivitiesStorage();
        this.imageOperations = imageOperations;
        this.playbackOperations = playbackOperations;
        this.activityItemPresenter = presenter;
        this.eventBus = eventBus;
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount() + Type.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        int type = super.getItemViewType(position);
        if (type == IGNORE_ITEM_VIEW_TYPE) return type;

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
        return activityItemPresenter.createItemView(position, parent);
    }

    @Override
    protected void bindRow(int index, View rowView) {
        activityItemPresenter.bindItemView(index, rowView, toPropertySets(getItems()));
    }
    
    private List<PropertySet> toPropertySets(List<Activity> activities) {
        List<PropertySet> propertySets = new ArrayList<PropertySet>(activities.size());
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
            if (!data.contains(newItem)) data.add(newItem);
        }
        Collections.sort(data);
    }

    @Override
    protected void onSuccessfulRefresh() {
        // do nothing for now. new items will be merged and sorted with the existing items
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {

        Type type = Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
            case PLAYLIST:
            case PLAYLIST_SHARING:
                playTrackOrStartPlaylistFragment(context, position);
                return ItemClickResults.LEAVING;

            case COMMENT:
            case TRACK_LIKE:
            case TRACK_REPOST:
                if (content == Content.ME_ACTIVITIES) {
                    // todo, scroll to specific repost
                    context.startActivity(new Intent(context, TrackInteractionActivity.class)
                            .putExtra(TrackInteractionActivity.PROPERTY_SET_EXTRA, getItem(position).getPlayable().toPropertySet())
                            .putExtra(EXTRA_INTERACTION_TYPE, type));
                } else {
                    playTrackOrStartPlaylistFragment(context, position);
                }
                return ItemClickResults.LEAVING;
            case PLAYLIST_LIKE:
            case PLAYLIST_REPOST:
                if (content == Content.ME_ACTIVITIES) {
                    // todo, scroll to specific repost
                    context.startActivity(new Intent(context, PlaylistInteractionActivity.class)
                            .putExtra(PlayableInteractionActivity.PROPERTY_SET_EXTRA, getItem(position).getPlayable().toPropertySet())
                            .putExtra(EXTRA_INTERACTION_TYPE, type));
                } else {
                    playTrackOrStartPlaylistFragment(context, position);
                }
                return ItemClickResults.LEAVING;

            case AFFILIATION:
                context.startActivity(new Intent(context, ProfileActivity.class)
                        .putExtra(ProfileActivity.EXTRA_USER, getItem(position).getUser()));
                return ItemClickResults.LEAVING;

            default:
                Log.i(SoundCloudApplication.TAG, "Clicked on item " + id);
        }
        return ItemClickResults.IGNORE;
    }

    private void playTrackOrStartPlaylistFragment(Context context, int position) {
        Playable playable = data.get(position).getPlayable();
        if (playable instanceof PublicApiTrack) {
            List<TrackUrn> trackUrns = toTrackUrn(filterPlayables(data));
            int adjustedPosition = filterPlayables(data.subList(0, position)).size();
            TrackUrn initialTrack = trackUrns.get(adjustedPosition);
            playbackOperations
                    .playTracksFromUri(contentUri, adjustedPosition, initialTrack, new PlaySessionSource(Screen.SIDE_MENU_STREAM))
                    .subscribe(new PlayQueueChangedSubscriber(eventBus));
        } else if (playable instanceof PublicApiPlaylist) {
            PlaylistDetailActivity.start(context, ((PublicApiPlaylist) playable).getUrn(), Screen.SIDE_MENU_STREAM);
        }
    }

}
