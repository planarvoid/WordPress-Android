package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.adapter.PlayableRow;

import android.content.Context;
import android.net.Uri;
import android.view.View;

public class PlaylistTracksAdapter2 extends ScBaseAdapter<Track> {

    public PlaylistTracksAdapter2(Uri uri) {
        super(uri);
    }

    @Override
    protected View createRow(Context context, int position) {
        return new PlayableRow(context);

    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        return 0;
    }
}
