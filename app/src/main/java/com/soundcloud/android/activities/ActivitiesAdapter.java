package com.soundcloud.android.activities;

import static com.soundcloud.android.associations.PlayableInteractionActivity.EXTRA_INTERACTION_TYPE;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.PlaylistInteractionActivity;
import com.soundcloud.android.associations.TrackInteractionActivity;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.tasks.CollectionParams;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.provider.Content;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Collections;
import java.util.List;

public class ActivitiesAdapter extends ScBaseAdapter<Activity> {

    private final ActivitiesStorage activitiesStorage;
    private final PlaybackOperations playbackOperations;
    private final ImageOperations imageOperations;

    public ActivitiesAdapter(Uri uri, ImageOperations imageOperations) {
        super(uri);
        activitiesStorage = new ActivitiesStorage();
        playbackOperations = new PlaybackOperations();
        this.imageOperations = imageOperations;
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount() + Activity.Type.values().length;
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
    protected IconLayout createRow(Context context, int position) {
        Activity.Type type = Activity.Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
                return new PlayableRow(context, imageOperations);

            case TRACK_REPOST:
            case PLAYLIST_REPOST:
                return (content == Content.ME_ACTIVITIES) ?
                        new RepostActivityRow(context, imageOperations) : new PlayableRow(context, imageOperations);

            case PLAYLIST:
            case PLAYLIST_SHARING:
                // TODO, playlist view
                return new PlayableRow(context, imageOperations);

            case COMMENT:
                return new CommentActivityRow(context, imageOperations);

            case TRACK_LIKE:
                return new LikeActivityRow(context, imageOperations);


            case PLAYLIST_LIKE:
                return new LikeActivityRow(context, imageOperations);


            case AFFILIATION:
                return new AffiliationActivityRow(context, imageOperations);


            default:
                throw new IllegalArgumentException("no view for " + type + " yet");
        }
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

        Activity.Type type = Activity.Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
            case PLAYLIST:
            case PLAYLIST_SHARING:
                playbackOperations.playFromAdapter(context, data, position, contentUri, Screen.SIDE_MENU_STREAM);
                return ItemClickResults.LEAVING;

            case COMMENT:
            case TRACK_LIKE:
            case TRACK_REPOST:
                if (content == Content.ME_ACTIVITIES) {
                    // todo, scroll to specific repost
                    context.startActivity(new Intent(context, TrackInteractionActivity.class)
                            .putExtra(Track.EXTRA, getItem(position).getPlayable())
                            .putExtra(EXTRA_INTERACTION_TYPE, type));
                } else {
                    playbackOperations.playFromAdapter(context, data, position, contentUri, Screen.SIDE_MENU_STREAM);
                }
                return ItemClickResults.LEAVING;
            case PLAYLIST_LIKE:
            case PLAYLIST_REPOST:
                if (content == Content.ME_ACTIVITIES) {
                    // todo, scroll to specific repost
                    context.startActivity(new Intent(context, PlaylistInteractionActivity.class)
                            .putExtra(Playlist.EXTRA, getItem(position).getPlayable())
                            .putExtra(EXTRA_INTERACTION_TYPE, type));
                } else {
                    playbackOperations.playFromAdapter(context, data, position, contentUri, Screen.SIDE_MENU_STREAM);
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

}
