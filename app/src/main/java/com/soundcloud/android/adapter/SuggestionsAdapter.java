package com.soundcloud.android.adapter;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SuggestionsAdapter extends CursorAdapter {
    private final ContentResolver mContentResolver;
    private final Context mContext;

    private ImageLoader mImageLoader;

    public final static int TYPE_TRACK = 0;
    public final static int TYPE_USER = 1;

    public SuggestionsAdapter(Context context, Cursor c) {
        super(context, c, false);
        mContentResolver = context.getContentResolver();
        mContext = context;
        mImageLoader = ImageLoader.get(mContext);
    }

    @Override
    public long getItemId(int position) {
        final Cursor cursor = (Cursor) getItem(position);
        return cursor.getLong(cursor.getColumnIndex(DBHelper.ResourceTable._ID));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return createViewFromResource(cursor, null, parent);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        createViewFromResource(cursor, view, null);
    }

    @Override
    public int getItemViewType(int position) {
        return getCursor() == null || ((MergeSearchCursor) getCursor()).trackCount > position ? TYPE_TRACK : TYPE_USER;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (constraint != null) {
            MergeSearchCursor mergeCursor = new MergeSearchCursor(
                    mContentResolver.query(Content.TRACKS.uri,
                            /*new String[]{
                                    DBHelper.TrackView._ID,
                                    DBHelper.TrackView.TITLE,
                                    DBHelper.TrackView.USERNAME,
                                    DBHelper.TrackView.ARTWORK_URL,
                            }*/ null, "UPPER(" + DBHelper.TrackView.TITLE + ") GLOB ?",
                            new String[]{
                                    constraint.toString().toUpperCase() + "*"
                            }, DBHelper.TrackView.TITLE + " ASC"),
                    mContentResolver.query(Content.USERS.uri,
                            /*new String[]{
                                    DBHelper.Users._ID,
                                    DBHelper.Users.USERNAME,
                                    DBHelper.Users.AVATAR_URL,
                            }*/ null, "UPPER(" + DBHelper.Users.USERNAME + ") GLOB ?",
                            new String[]{
                                    constraint.toString().toUpperCase() + "*"
                            }, DBHelper.Users.USERNAME + " ASC"));
            return mergeCursor;
        } else {
            return super.runQueryOnBackgroundThread(constraint);
        }

    }

    private View createViewFromResource(Cursor cursor,
                                        @Nullable View convertView,
                                        @Nullable ViewGroup parent) {
        View view = convertView;
        SearchTag tag;
        if (convertView == null) {
            view = View.inflate(mContext, R.layout.search_suggestion, null);
            tag = new SearchTag();
            tag.iv_icon = (ImageView) view.findViewById(R.id.icon);
            tag.iv_search_type = (ImageView) view.findViewById(R.id.iv_search_type);
            tag.tv_main = (TextView) view.findViewById(R.id.title);
            view.setTag(tag);
            fixRowPadding(view);
        } else {
            tag = (SearchTag) view.getTag();
        }

        if (cursor.getPosition() < ((MergeSearchCursor) cursor).trackCount) {
            setIcon(tag, cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.ARTWORK_URL)));
            tag.tv_main.setText(cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.TITLE)));
            tag.iv_search_type.setImageResource(R.drawable.ic_search_sound);

        } else {
            setIcon(tag, cursor.getString(cursor.getColumnIndex(DBHelper.Users.AVATAR_URL)));
            tag.tv_main.setText(cursor.getString(cursor.getColumnIndex(DBHelper.Users.USERNAME)));
            tag.iv_search_type.setImageResource(R.drawable.ic_search_user);
        }
        return view;
    }

    private void fixRowPadding(View view) {
        view.setPadding(view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                view.getPaddingBottom());
    }

    private void setIcon(SearchTag tag, String iconUri) {
        if (ImageUtils.checkIconShouldLoad(iconUri)) {
            ImageLoader.BindResult result = mImageLoader.bind(this, tag.iv_icon,
                    Consts.GraphicSize.formatUriForSearchSuggestionsList(mContext, iconUri)
            );
            if (result != ImageLoader.BindResult.OK) {
                tag.iv_icon.setImageResource(R.drawable.cloud_no_logo_sm);
            }
        } else {
            mImageLoader.unbind(tag.iv_icon);
            tag.iv_icon.setImageResource(R.drawable.cloud_no_logo_sm);
        }
    }

    static class SearchTag {
        ImageView iv_icon;
        ImageView iv_search_type;
        TextView tv_main;
    }

    private static class MergeSearchCursor extends MergeCursor {
        public int trackCount;

        public MergeSearchCursor(Cursor trackCursor, Cursor userCursor) {
            super(new Cursor[]{trackCursor, userCursor});
            trackCount = trackCursor.getCount();
        }
    }
}