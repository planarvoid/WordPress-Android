package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.adapter.PlayableRow;

import android.content.Context;
import android.net.Uri;
import android.view.View;

public class PlaylistTracksAdapter2 extends ScBaseAdapter<Track> {

    public PlaylistTracksAdapter2(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected View createRow(int position) {
        return new PlayableRow(mContext);

    }

    @Override
    public int handleListItemClick(int position, long id) {
        return 0;
    }
}
