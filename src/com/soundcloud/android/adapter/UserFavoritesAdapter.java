package com.soundcloud.android.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TracklistRow;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import java.util.List;

public class UserFavoritesAdapter extends ScCursorAdapter implements ITracklistAdapter {


	private LayoutInflater inflater;

    public UserFavoritesAdapter(Context context, Cursor cursor, boolean requery) {
        super(context, cursor, requery);
    }

    public UserFavoritesAdapter(FragmentActivity activity, Cursor cursor) {
        super(activity, cursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new TracklistRow(context, this);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((TracklistRow) view).display(cursor);
    }

    @Override
    public List<Parcelable> getData() {
        return null;
    }

    @Override
    public Track getTrackAt(int index) {
        return null;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void setPlayingId(long currentTrackId, boolean isPlaying) {
    }

    @Override
    public long getPlayingId() {
        return 0;
    }

    @Override
    public void removeFavorite(Track track) {
    }

    @Override
    public void addFavorite(Track track) {
    }

    @Override
    public QuickTrackMenu getQuickTrackMenu() {
        return null;
    }
}
