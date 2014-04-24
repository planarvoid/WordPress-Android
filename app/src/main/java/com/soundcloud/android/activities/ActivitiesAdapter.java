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
    private ActivitiesStorage mActivitiesStorage;
    private PlaybackOperations mPlaybackOperations;
    private ImageOperations mImageOperations;


    public ActivitiesAdapter(Uri uri, ImageOperations imageOperations) {
        super(uri);
        mActivitiesStorage = new ActivitiesStorage();
        mPlaybackOperations = new PlaybackOperations();
        mImageOperations = imageOperations;
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
        } else if (mData.size() == 0) {
            return true; // need to pull from DB
        } else {
            // check if there is anything newer
            // TODO: DB access on UI thread!
            final Activity latestActivity = mActivitiesStorage.getLatestActivity(mContent);
            return (latestActivity == null || latestActivity.getCreatedAt().getTime() > mData.get(0).getCreatedAt().getTime());
        }
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        Activity.Type type = Activity.Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
                return new PlayableRow(context, mImageOperations);

            case TRACK_REPOST:
            case PLAYLIST_REPOST:
                return (mContent == Content.ME_ACTIVITIES) ?
                        new RepostActivityRow(context, mImageOperations) : new PlayableRow(context, mImageOperations);

            case PLAYLIST:
            case PLAYLIST_SHARING:
                // TODO, playlist view
                return new PlayableRow(context, mImageOperations);

            case COMMENT:
                return new CommentActivityRow(context, mImageOperations);

            case TRACK_LIKE:
                return new LikeActivityRow(context, mImageOperations);


            case PLAYLIST_LIKE:
                return new LikeActivityRow(context, mImageOperations);


            case AFFILIATION:
                return new AffiliationActivityRow(context, mImageOperations);


            default:
                throw new IllegalArgumentException("no view for " + type + " yet");
        }
    }

    @Override
    public CollectionParams getParams(boolean refresh) {
        CollectionParams params = super.getParams(refresh);
        if (mData.size() > 0) {
            Activity first = getItem(0);
            Activity last  = getItem(getItemCount() -1);
            params.timestamp = refresh ? first.getCreatedAt().getTime() : last.getCreatedAt().getTime();
        }
        return params;
    }


    @Override
    public void addItems(List<Activity> newItems) {
        for (Activity newItem : newItems){
            if (!mData.contains(newItem))mData.add(newItem);
        }
        Collections.sort(mData);
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
                mPlaybackOperations.playFromAdapter(context, mData, position, mContentUri, Screen.SIDE_MENU_STREAM);
                return ItemClickResults.LEAVING;

            case COMMENT:
            case TRACK_LIKE:
            case TRACK_REPOST:
                if (mContent == Content.ME_ACTIVITIES) {
                    // todo, scroll to specific repost
                    context.startActivity(new Intent(context, TrackInteractionActivity.class)
                            .putExtra(Track.EXTRA, getItem(position).getPlayable())
                            .putExtra(EXTRA_INTERACTION_TYPE, type));
                } else {
                    mPlaybackOperations.playFromAdapter(context, mData, position, mContentUri, Screen.SIDE_MENU_STREAM);
                }
                return ItemClickResults.LEAVING;
            case PLAYLIST_LIKE:
            case PLAYLIST_REPOST:
                if (mContent == Content.ME_ACTIVITIES) {
                    // todo, scroll to specific repost
                    context.startActivity(new Intent(context, PlaylistInteractionActivity.class)
                            .putExtra(Playlist.EXTRA, getItem(position).getPlayable())
                            .putExtra(EXTRA_INTERACTION_TYPE, type));
                } else {
                    mPlaybackOperations.playFromAdapter(context, mData, position, mContentUri, Screen.SIDE_MENU_STREAM);
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
