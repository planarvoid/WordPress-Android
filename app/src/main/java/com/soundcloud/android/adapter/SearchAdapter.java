package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.TrackInfoBar;
import com.soundcloud.android.view.adapter.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class SearchAdapter extends ScBaseAdapter<ScResource> {

    private static final int TYPE_TRACK = 0;
    private static final int TYPE_USER = 1;

    public SearchAdapter(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount() + 2; // Tracks + Users, for now
    }

    @Override
    public int getItemViewType(int position) {
        int type = super.getItemViewType(position);
        if (type == IGNORE_ITEM_VIEW_TYPE) return type;

        return getItem(position) instanceof User ? TYPE_USER : TYPE_TRACK;
    }

    @Override
    protected LazyRow createRow(int position) {
        int type = getItemViewType(position);
        switch (type) {
            case TYPE_TRACK:
                return new TrackInfoBar(mContext, this);
            case TYPE_USER:
                return new UserlistRow(mContext, this);
            default:
                throw new IllegalArgumentException("no view for playlists yet");
        }
    }

    @Override
    public void handleListItemClick(int position, long id) {
        int type = getItemViewType(position);
        switch (type) {
            case TYPE_TRACK:
                playPosition(position, id);
                break;
            case TYPE_USER:
                mContext.startActivity(new Intent(mContext, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER, getItem(position)));
                break;
            default:
                throw new IllegalArgumentException("no view for playlists yet");
        }
    }

    @Override
    protected Uri getPlayableUri() {
        return null;
    }
}
