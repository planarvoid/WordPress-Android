package com.soundcloud.android.playlists;

import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

public class PlaylistTracksAdapter extends CursorAdapter {

    private ImageOperations mImageOperations;

    public PlaylistTracksAdapter(Context context, ImageOperations imageOperations) {
        this(context, null, false);
    }

    public PlaylistTracksAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new PlayableRow(mContext, mImageOperations);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((PlayableRow) view).display(cursor);
    }
}
