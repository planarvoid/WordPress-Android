package com.soundcloud.android.search;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.collections.views.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;

public class SearchAdapter extends ScBaseAdapter<ScResource> {

    private static final int TYPE_TRACK = 0;
    private static final int TYPE_USER = 1;
    private PlaybackOperations mPlaybackOperations;

    public SearchAdapter(Uri uri) {
        super(uri);
        mPlaybackOperations = new PlaybackOperations();
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount() + 2; // Tracks + Users, for now
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
    }

    @Override
    public int getItemViewType(int position) {
        int type = super.getItemViewType(position);
        if (type == IGNORE_ITEM_VIEW_TYPE) return type;

        return getItem(position) instanceof User ? TYPE_USER : TYPE_TRACK;
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        int type = getItemViewType(position);
        switch (type) {
            case TYPE_TRACK:
                return new PlayableRow(context);
            case TYPE_USER:
                return new UserlistRow(context);
            default:
                throw new IllegalArgumentException("no view for playlists yet");
        }
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        int type = getItemViewType(position);
        if (type == TYPE_TRACK) {
            mPlaybackOperations.playFromAdapter(context, mData, position, null, screen);
            return ItemClickResults.LEAVING;
        } else if (type == TYPE_USER) {
            context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER, getItem(position)));
            return ItemClickResults.LEAVING;
        }
        return ItemClickResults.IGNORE;
    }

}
