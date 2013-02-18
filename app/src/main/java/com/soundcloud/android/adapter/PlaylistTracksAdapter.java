package com.soundcloud.android.adapter;

import com.soundcloud.android.view.adapter.PlayableRow;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

public class PlaylistTracksAdapter extends CursorAdapter {

    public PlaylistTracksAdapter(Context context) {
        this(context, null, true);
    }

    public PlaylistTracksAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new PlayableRow(mContext);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((PlayableRow) view).display(cursor);
    }
}
