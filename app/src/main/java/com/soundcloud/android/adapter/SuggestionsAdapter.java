package com.soundcloud.android.adapter;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static com.soundcloud.android.Consts.GraphicSize;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.model.SearchSuggestions;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuggestionsAdapter extends CursorAdapter implements DetachableResultReceiver.Receiver {
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final AndroidCloudAPI mApi;

    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    private ImageLoader mImageLoader;
    private Handler handler = new Handler();

    private final static int TYPE_SEARCH_ITEM = 0;
    private final static int TYPE_TRACK  = 1;
    private final static int TYPE_USER  = 2;

    private static final int MAX_LOCAL  = 5;
    private static final int MAX_REMOTE = 5;

    public static final String LOCAL = "_local";

    public static final String[] COLUMN_NAMES = new String[]{
            BaseColumns._ID,                 // suggest id
            DBHelper.Suggestions.ID,         // model id
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            DBHelper.Suggestions.ICON_URL,
            LOCAL
    };

    private SuggestionsHandler mSuggestionsHandler;
    private HandlerThread mSuggestionsHandlerThread;
    private String mCurrentConstraint;
    private Pattern mCurrentPattern;

    private @NotNull SearchSuggestions mLocalSuggestions  = SearchSuggestions.EMPTY;
    private @NotNull SearchSuggestions mRemoteSuggestions = SearchSuggestions.EMPTY;

    public SuggestionsAdapter(Context context, Cursor c, AndroidCloudAPI api) {
        super(context, c, 0);
        mContentResolver = context.getContentResolver();
        mContext = context;
        mImageLoader = ImageLoader.get(mContext);
        mApi = api;

        mSuggestionsHandlerThread = new HandlerThread("SuggestionsHandler", THREAD_PRIORITY_DEFAULT);
        mSuggestionsHandlerThread.start();
        mSuggestionsHandler = new SuggestionsHandler(mSuggestionsHandlerThread.getLooper());
    }

    public void onDestroy() {
        mSuggestionsHandler.removeMessages(0);
        mSuggestionsHandlerThread.getLooper().quit();
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
        return getUriType(getItemIntentData(position));
    }

    @Override
    public long getItemId(int position) {
        return getItemIntentData(position).hashCode();
    }

    public Uri getItemIntentData(int position) {
        Cursor cursor = (Cursor) getItem(position);
        final String data = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA));
        return Uri.parse(data);
    }

    private int getUriType(Uri uri) {
        switch (Content.match(uri)) {
            case SEARCH_ITEM: return TYPE_SEARCH_ITEM;
            case TRACK: return TYPE_TRACK;
            case USER:  return TYPE_USER;
            default: return TYPE_TRACK;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public Cursor runQueryOnBackgroundThread(@Nullable final CharSequence constraint) {
        if (constraint != null && !TextUtils.isEmpty(constraint)) {
            mCurrentConstraint = constraint.toString();
            mCurrentPattern = getHighlightPattern(mCurrentConstraint);

            mLocalSuggestions = fetchLocalSuggestions(mCurrentConstraint, MAX_LOCAL);

            mSuggestionsHandler.removeMessages(0);
            Message.obtain(mSuggestionsHandler,0,constraint).sendToTarget();

            return getMixedCursor();

        } else {
            mLocalSuggestions = SearchSuggestions.EMPTY;
            return super.runQueryOnBackgroundThread(constraint);
        }
    }


    private final class SuggestionsHandler extends Handler {
        public SuggestionsHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            final CharSequence constraint = (CharSequence) msg.obj;
            try {
                HttpResponse resp = mApi.get(Request.to("/search/suggest").with(
                        "q", constraint,
                        "highlight", "true",
                        "limit", MAX_REMOTE));

                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final SearchSuggestions searchSuggestions = mApi.getMapper().readValue(resp.getEntity().getContent(),
                            SearchSuggestions.class);
                    onRemoteSuggestions(constraint, searchSuggestions);
                    return;
                } else {
                    Log.w(TAG, "invalid status code returned: " + resp.getStatusLine());
                }
            } catch (IOException e) {
                Log.w(TAG, "error fetching suggestions", e);
            }
            onRemoteSuggestions(constraint, SearchSuggestions.EMPTY);
        }

        private void onRemoteSuggestions(final CharSequence constraint, final @NotNull SearchSuggestions suggestions) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // make sure we are still relevant
                    if (constraint.equals(mCurrentConstraint)) {
                        mRemoteSuggestions = suggestions;
                        swapCursor(getMixedCursor());

                        prefetchResults(mRemoteSuggestions);
                    }
                }
            });
        }
    }

    private void prefetchResults(SearchSuggestions suggestions) {
        if (suggestions.isEmpty() || !shouldPrefetch()) return;

        final List<Long> trackLookupIds = new ArrayList<Long>();
        final List<Long> userLookupIds = new ArrayList<Long>();
        suggestions.putRemoteIds(trackLookupIds, userLookupIds);

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

    private boolean shouldPrefetch() {
        return false;
    }

    private Cursor getMixedCursor() {
        if (!mRemoteSuggestions.isEmpty()) {
            if (!mLocalSuggestions.isEmpty()) {
                return withHeader(mLocalSuggestions.merge(mRemoteSuggestions).asCursor());
            } else {
                return withHeader(mRemoteSuggestions.asCursor());
            }
        } else {
            return withHeader(mLocalSuggestions.asCursor());
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
                swapCursor(getMixedCursor());
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

    private SearchSuggestions fetchLocalSuggestions(String constraint, int max) {
        final Cursor cursor = mContentResolver.query(
                Content.ANDROID_SEARCH_SUGGEST.uri
                        .buildUpon()
                        .appendQueryParameter("limit", String.valueOf(max))
                        .build(),
                null,
                null,
                new String[] { constraint},
                null);

        SearchSuggestions suggestions = new SearchSuggestions(cursor);
        cursor.close();
        return suggestions;
    }

    private Cursor withHeader(Cursor c1) {
        return new MergeCursor(new Cursor[] { createHeader(mCurrentConstraint), c1 });
    }

    private MatrixCursor createHeader(String constraint) {
        MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES, 1);
        if (!TextUtils.isEmpty(constraint)) {
            cursor.addRow(new Object[]{
                    -1,
                    -1,
                    mContext.getResources().getString(R.string.search_for_query, constraint),
                    Content.SEARCH_ITEM.forQuery(constraint),
                    "",
                    1 /* local */
            });
        }
        return cursor;
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

        final long id = cursor.getLong(cursor.getColumnIndex(DBHelper.Suggestions.ID));
        final String query = cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.COLUMN_TEXT1));
        final boolean local = cursor.getInt(cursor.getColumnIndex(LOCAL)) == 1;

        if (id == -1 /* header */) {
            tag.tv_main.setText(query);
        } else {
            tag.tv_main.setText(local ? highlightLocal(query) : highlightRemote(query));
        }

        final int rowType = getItemViewType(cursor.getPosition());
        if (rowType == TYPE_SEARCH_ITEM) {
            tag.iv_icon.setVisibility(View.GONE);
            tag.iv_search_type.setVisibility(View.GONE);
        } else {
            tag.iv_icon.setVisibility(View.VISIBLE);
            tag.iv_search_type.setVisibility(View.VISIBLE);

            boolean isUser = rowType == TYPE_USER;
            setIcon(tag, GraphicSize.formatUriForList(mContext,
                    cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.ICON_URL))), isUser);

            if (isUser) {
                tag.iv_search_type.setImageResource(R.drawable.ic_search_user);
            } else {
                tag.iv_search_type.setImageResource(R.drawable.ic_search_sound);
            }

        }
        return view;
    }

    private void setIcon(SearchTag tag, String iconUri, boolean isUser) {
        if (ImageUtils.checkIconShouldLoad(iconUri)) {
            ImageLoader.BindResult result = mImageLoader.bind(tag.iv_icon,
                    GraphicSize.formatUriForSearchSuggestionsList(mContext, iconUri),
                    null
            );
            if (result == ImageLoader.BindResult.OK) return;
        } else {
            mImageLoader.unbind(tag.iv_icon);
        }
        tag.iv_icon.setImageResource(isUser ? R.drawable.no_user_cover : R.drawable.no_sound_cover);
    }

    static class SearchTag {
        ImageView iv_icon;
        ImageView iv_search_type;
        TextView tv_main;
    }

    private Spanned highlightRemote(String query) {
        // need to replace <b>foo</b> tags
        // NOTE: don't use Html.fromHtml(), it's slow!
        final int leftTag  = query.indexOf("<b>");
        final int rightTag = query.indexOf("</b>") - 3;
        if (leftTag != -1 && rightTag != -1) {
            query = query.replace("<b>", "");
            query = query.replace("</b>", "");
        }
        return createSpanned(query, leftTag, rightTag);
    }

    private Spanned highlightLocal(String query) {
        Matcher m = mCurrentPattern.matcher(query);
        if (m.find()) {
            return createSpanned(query, m.start(2), m.end(2));
        } else {
            return createSpanned(query, -1, -1);
        }
    }

    private static Spanned createSpanned(String s, int start, int end) {
        final SpannableString spanned = new SpannableString(s);
        spanned.setSpan(new ForegroundColorSpan(0xFF666666),
                0, s.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        if (start >= 0 && start < end && end > 0 && end < s.length()) {
            spanned.setSpan(new ForegroundColorSpan(Color.WHITE),
                    start, end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return spanned;
    }

    /**
     * @param query the search query
     * @return a highlight pattern
     *
     * @see <a href="https://github.com/soundcloud/v2/blob/016de18498c410c4c9ff1875bc48286741df69e3/app/collections/shortcuts.js#L30">
     *     Definition in next/v2</a>
     */
    /* package */ static Pattern getHighlightPattern(String query) {
        return Pattern.compile("(^|[\\s.\\(\\)\\[\\]_-])(" +Pattern.quote(query)+")", Pattern.CASE_INSENSITIVE);
    }
}