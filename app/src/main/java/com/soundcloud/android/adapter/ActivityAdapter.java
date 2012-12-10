package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.activity.track.TrackComments;
import com.soundcloud.android.activity.track.TrackReposters;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.collection.CollectionParams;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.AffiliationActivityRow;
import com.soundcloud.android.view.adapter.CommentActivityRow;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.LikeActivityRow;
import com.soundcloud.android.view.adapter.TrackInfoBar;
import com.soundcloud.android.view.adapter.TrackRepostActivityRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Collections;

public class ActivityAdapter extends ScBaseAdapter<Activity> implements PlayableAdapter {

    public ActivityAdapter(Context context, Uri uri) {
        super(context, uri);
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

    public boolean isExpired() {
        if (mData.size() == 0) return true;
        final Activity firstActivity = Activities.getFirstActivity(mContent, mContext.getContentResolver());
        return (firstActivity == null || firstActivity.created_at.getTime() > mData.get(0).created_at.getTime());
    }

    @Override
    protected LazyRow createRow(int position) {
        Activity.Type type = Activity.Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
                return new TrackInfoBar(mContext, this);

            case TRACK_REPOST:
                return (mContent == Content.ME_ACTIVITIES) ?
                        new TrackRepostActivityRow(mContext, this) : new TrackInfoBar(mContext, this);

            case COMMENT:
                return new CommentActivityRow(mContext, this);

            case TRACK_LIKE:
                return new LikeActivityRow(mContext, this);

            case AFFILIATION:
                return new AffiliationActivityRow(mContext, this);


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
    public void addItems(CollectionHolder<Activity> newItems) {
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
    public int handleListItemClick(int position, long id) {

        Activity.Type type = Activity.Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
                PlayUtils.playFromAdapter(mContext, this, mData, position);
                return ItemClickResults.LEAVING;

            case TRACK_REPOST:
                if (mContent == Content.ME_ACTIVITIES) {
                    // todo, scroll to specific repost
                    mContext.startActivity(new Intent(mContext, TrackReposters.class)
                        .putExtra(Track.EXTRA, getItem(position).getTrack()));
                } else {
                    PlayUtils.playFromAdapter(mContext, this, mData, position);
                }
                return ItemClickResults.LEAVING;

            case COMMENT:
                mContext.startActivity(new Intent(mContext, TrackComments.class)
                        .putExtra(Track.EXTRA, getItem(position).getTrack()));
                // todo, scroll to specific comment
                return ItemClickResults.LEAVING;

            case AFFILIATION:
                mContext.startActivity(new Intent(mContext, UserBrowser.class)
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
}
