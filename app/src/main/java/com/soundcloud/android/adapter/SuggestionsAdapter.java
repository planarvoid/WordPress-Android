package com.soundcloud.android.adapter;

import static com.soundcloud.android.Consts.GraphicSize;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
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
    public final static int TYPE_USER  = 1;

    public SuggestionsAdapter(Context context, Cursor c) {
        super(context, c, false);
        mContentResolver = context.getContentResolver();
        mContext = context;
        mImageLoader = ImageLoader.get(mContext);
    }

    @Override
    public long getItemId(int position) {
        final Cursor cursor = (Cursor) getItem(position);
        return cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
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
        ClientUri uri = getItemUri(position);
        return uri.isSound() ? TYPE_TRACK : TYPE_USER;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (constraint != null) {
            return mContentResolver.query(
                Content.ANDROID_SEARCH_SUGGEST.uri,
                    null,
                    null,
                    new String[] { constraint.toString() },
                    null);
        } else {
            return super.runQueryOnBackgroundThread(constraint);
        }
    }

    public ClientUri getItemUri(int position) {
        Cursor cursor = (Cursor) getItem(position);
        final String data = cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.INTENT_DATA));
        return ClientUri.fromUri(data);
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
        } else {
            tag = (SearchTag) view.getTag();
        }

        setIcon(tag, GraphicSize.formatUriForList(mContext,
                cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.ICON_URL))));

        final String data = cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.INTENT_DATA));

        tag.tv_main.setText(cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.COLUMN_TEXT1)));

        ClientUri uri = ClientUri.fromUri(data);
        if (uri.isSound()) {
            tag.iv_search_type.setImageResource(R.drawable.ic_search_sound);
        } else if (uri.isUser()) {
            tag.iv_search_type.setImageResource(R.drawable.ic_search_user);
        }

        return view;
    }

    private void setIcon(SearchTag tag, String iconUri) {
        if (ImageUtils.checkIconShouldLoad(iconUri)) {
            ImageLoader.BindResult result = mImageLoader.bind(this, tag.iv_icon,
                    GraphicSize.formatUriForSearchSuggestionsList(mContext, iconUri)
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
}