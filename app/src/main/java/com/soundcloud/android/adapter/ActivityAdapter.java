package com.soundcloud.android.adapter;

import static com.soundcloud.android.activity.track.PlayableInteractionActivity.EXTRA_INTERACTION_TYPE;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.activity.track.PlaylistInteractionActivity;
import com.soundcloud.android.activity.track.TrackInteractionActivity;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.collection.CollectionParams;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.AffiliationActivityRow;
import com.soundcloud.android.view.adapter.CommentActivityRow;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.LikeActivityRow;
import com.soundcloud.android.view.adapter.PlayableRow;
import com.soundcloud.android.view.adapter.RepostActivityRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Collections;
import java.util.List;

public class ActivityAdapter extends ScBaseAdapter<Activity> implements PlayableAdapter {
    private ActivitiesStorage mActivitiesStorage;


    public ActivityAdapter(Context context, Uri uri) {
        super(context, uri);
        mActivitiesStorage = new ActivitiesStorage(context);
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
            final Activity firstActivity = mActivitiesStorage.getFirstActivity(mContent);
            return (firstActivity == null || firstActivity.created_at.getTime() > mData.get(0).created_at.getTime());
        }
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        Activity.Type type = Activity.Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
                return new PlayableRow(context);

            case TRACK_REPOST:
            case PLAYLIST_REPOST:
                return (mContent == Content.ME_ACTIVITIES) ?
                        new RepostActivityRow(context) : new PlayableRow(context);

            case PLAYLIST:
            case PLAYLIST_SHARING:
                // TODO, playlist view
                return new PlayableRow(context);

            case COMMENT:
                return new CommentActivityRow(context);

            case TRACK_LIKE:
                return new LikeActivityRow(context);


            case PLAYLIST_LIKE:
                return new LikeActivityRow(context);


            case AFFILIATION:
                return new AffiliationActivityRow(context);


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
            params.timestamp = refresh ? (first == null ? 0 : first.created_at.getTime())
                    : (last == null ? System.currentTimeMillis() : last.created_at.getTime());
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
    public int handleListItemClick(Context context, int position, long id) {

        Activity.Type type = Activity.Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
            case PLAYLIST:
            case PLAYLIST_SHARING:
                PlayUtils.playFromAdapter(context, this, mData, position);
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
                    PlayUtils.playFromAdapter(context, this, mData, position);
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
                    PlayUtils.playFromAdapter(context, this, mData, position);
                }
                return ItemClickResults.LEAVING;

            case AFFILIATION:
                context.startActivity(new Intent(context, UserBrowser.class)
                        .putExtra(UserBrowser.EXTRA_USER, getItem(position).getUser()));
                return ItemClickResults.LEAVING;

            default:
                Log.i(SoundCloudApplication.TAG, "Clicked on item " + id);
        }
        return ItemClickResults.IGNORE;
    }

    @Override
    public Uri getPlayableUri() {
        return mContentUri;
    }

    @Override
    public Playable getPlayable(int position) {
        return getItem(position).getPlayable();
    }
}
