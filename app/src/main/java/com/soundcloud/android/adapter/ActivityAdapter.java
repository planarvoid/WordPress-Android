package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Activity;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.view.adapter.CommentRow;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.LikeRow;
import com.soundcloud.android.view.adapter.TrackInfoBar;

import android.content.Context;
import android.net.Uri;

public class ActivityAdapter extends ScBaseAdapter<Activity> {

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

        return getItem(position).type.ordinal();
    }

    @Override
    protected LazyRow createRow(int position) {
        Activity.Type type = Activity.Type.values()[getItemViewType(position)];
        switch (type) {
            case TRACK:
            case TRACK_SHARING:
                return new TrackInfoBar(mContext, this);

            case COMMENT:
                return new CommentRow(mContext, this);
            case FAVORITING:
                return new LikeRow(mContext, this);
            case PLAYLIST:
            default:
                throw new IllegalArgumentException("no view for playlists yet");
        }
    }

    public long getItemId(int position) {
        Activity a = getItem(position);
        if (a != null) {
            a.created_at.getTime();
        }
        return position;
    }


    @Override
    public RemoteCollectionTask.CollectionParams getParams(boolean refresh) {
        RemoteCollectionTask.CollectionParams params = super.getParams(refresh);
        if (!isEmpty()) {
            Activity first = getItem(0);
            Activity last  = getItem(getItemCount() -1);
            params.timestamp = refresh ? first.created_at.getTime() : last.created_at.getTime();
        }
        return params;
    }
}
