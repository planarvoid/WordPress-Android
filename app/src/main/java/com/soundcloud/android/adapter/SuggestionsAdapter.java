package com.soundcloud.android.adapter;

import static com.soundcloud.android.Consts.GraphicSize;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.model.SearchSuggestions;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SuggestionsAdapter extends CursorAdapter implements  DetachableResultReceiver.Receiver {
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final AndroidCloudAPI mApi;

    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    private ImageLoader mImageLoader;
    private Handler handler = new Handler();

    private final static int TYPE_TRACK = 0;
    private final static int TYPE_USER  = 1;

    private static final int MAX_LOCAL  = 3;
    private static final int MAX_REMOTE = 5;

    static final private UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private SuggestionsHandler mSuggestionsHandler;
    private HandlerThread mSuggestionsHandlerThread;
    public CharSequence mCurrentConstraint;

    private Cursor mLocalSuggestions;
    private SearchSuggestions mRemoteSuggestions;


    public SuggestionsAdapter(Context context, Cursor c, AndroidCloudAPI api) {
        super(context, c, false);
        mContentResolver = context.getContentResolver();
        mContext = context;
        mImageLoader = ImageLoader.get(mContext);
        mApi = api;

        mSuggestionsHandlerThread = new HandlerThread("SuggestionsHandler", android.os.Process.THREAD_PRIORITY_DEFAULT);
        mSuggestionsHandlerThread.start();
        mSuggestionsHandler = new SuggestionsHandler(mSuggestionsHandlerThread.getLooper());

        sMatcher.addURI(ScContentProvider.AUTHORITY, Content.USER.uriPath, Content.USER.id);
        sMatcher.addURI(ScContentProvider.AUTHORITY, Content.TRACK.uriPath, Content.TRACK.id);
    }

    public void onDestroy(){
        mSuggestionsHandler.removeMessages(0);
        mSuggestionsHandlerThread.getLooper().quit();
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
        Uri uri = getItemUri(position);
        return getUriType(uri);
    }

    private int getUriType(Uri uri) {
        return sMatcher.match(uri) != Content.USER.id ? TYPE_USER : TYPE_TRACK;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
        mCurrentConstraint = constraint;
        if (!TextUtils.isEmpty(constraint)) {
            mLocalSuggestions = fetchLocalSuggestions(constraint, MAX_LOCAL);

            mSuggestionsHandler.removeMessages(0);
            Message.obtain(mSuggestionsHandler,0,constraint).sendToTarget();

            return mLocalSuggestions;
        } else {
            mLocalSuggestions = null;
            return super.runQueryOnBackgroundThread(constraint);
        }
    }

    public Uri getItemUri(int position) {
        Cursor cursor = (Cursor) getItem(position);
        final String data = cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.INTENT_DATA));
        return Uri.parse(data);
    }

    private final class SuggestionsHandler extends Handler {

        public SuggestionsHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            final CharSequence constraint = (CharSequence) msg.obj;

            try {
                HttpResponse resp = mApi.get(Request.to("/search/suggest").with("q", constraint, "limit", MAX_REMOTE));

                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final SearchSuggestions searchSuggestions = mApi.getMapper().readValue(resp.getEntity().getContent(),
                            SearchSuggestions.class);
                    onRemoteSuggestions(constraint,searchSuggestions);
                    return;

                } else {
                    Log.w(TAG, "invalid status code returned: " + resp.getStatusLine());
                }
            } catch (IOException e) {
                Log.w(TAG, "error fetching suggestions", e);
            }

            onRemoteSuggestions(constraint, null);

        }

        private void onRemoteSuggestions(final CharSequence constraint, final SearchSuggestions suggestions) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // make sure we are still relevant
                    if (constraint == mCurrentConstraint) {
                        mRemoteSuggestions = suggestions;
                        reloadRemoteCursor();

                        if (mRemoteSuggestions != null){
                            final List<Long> trackLookupIds = new ArrayList<Long>();
                            final List<Long> userLookupIds = new ArrayList<Long>();
                            mRemoteSuggestions.putMissingIds(trackLookupIds, userLookupIds);

                            ArrayList<Uri> toSync = new ArrayList<Uri>();
                            if (!trackLookupIds.isEmpty()) toSync.add(Content.TRACK_LOOKUP.forQuery(TextUtils.join(",", trackLookupIds)));
                            if (!userLookupIds.isEmpty()) toSync.add(Content.USER_LOOKUP.forQuery(TextUtils.join(",", userLookupIds)));

                            if (!toSync.isEmpty()) {
                                Intent intent = new Intent(mContext, ApiSyncService.class)
                                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                                        .putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, toSync)
                                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true);
                                mContext.startService(intent);
                            }

                        }
                    }
                }
            });
        }

    }

    private void reloadRemoteCursor() {

        final MatrixCursor remote = new MatrixCursor(new String[]{
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                DBHelper.Suggestions.ICON_URL});

        if (mRemoteSuggestions != null) {
            for (SearchSuggestions.Query q : mRemoteSuggestions) {
                remote.addRow(new Object[]{
                        q.id,
                        q.query,
                        q.getUriPath(),
                        q.getIconUri()
                });
            }
        }

        if (remote.getCount() > 0){
            swapCursor(new MergeCursor(new Cursor[]{mLocalSuggestions, remote}));
        } else {
            swapCursor(mLocalSuggestions);
        }

    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
                reloadRemoteCursor();
                break;
            case ApiSyncService.STATUS_SYNC_ERROR: {
                break;
            }
        }
    }

    protected DetachableResultReceiver getReceiver() {
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }


    private Cursor fetchLocalSuggestions(CharSequence constraint, int max) {
        final MatrixCursor local = new MatrixCursor(new String[]{
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                DBHelper.Suggestions.ICON_URL});

        final Cursor cursor = mContentResolver.query(
                Content.ANDROID_SEARCH_SUGGEST.uri.buildUpon().appendQueryParameter("limit",
                        String.valueOf(max)).build(),
                null,
                null,
                new String[] { constraint.toString() },
                null);

        while (cursor.moveToNext()) {
            local.addRow(new Object[] {
                cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)),
                cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)),
                cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA)),
                cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.ICON_URL))
            });
        }
        cursor.close();
        return local;
    }

    private View createViewFromResource(Cursor cursor,
                                        @Nullable View convertView,
                                        @SuppressWarnings("UnusedParameters")
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
        tag.tv_main.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.COLUMN_TEXT1))));

        Uri uri = Uri.parse(data);
        if (sMatcher.match(uri) == Content.TRACK.id) {
            tag.iv_search_type.setImageResource(R.drawable.ic_search_sound);
        } else if (sMatcher.match(uri) == Content.USER.id) {
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