package com.soundcloud.android.search.suggestions;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.SearchSuggestions;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuggestionsAdapter extends CursorAdapter implements DetachableResultReceiver.Receiver {
    private final ContentResolver contentResolver;
    private final Context context;

    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    private final static int TYPE_SEARCH_ITEM = 0;
    private final static int TYPE_TRACK  = 1;
    private final static int TYPE_USER  = 2;

    private static final int MAX_LOCAL  = 5;
    private static final int MAX_REMOTE = 5;

    private ImageOperations mImageOperations;

    public static final String LOCAL = "_local";
    public static final String HIGHLIGHTS = "_highlights";

    public static final String[] COLUMN_NAMES = new String[]{
            BaseColumns._ID,                 // suggest id
            TableColumns.Suggestions.ID,         // model id
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            TableColumns.Suggestions.ICON_URL,
            LOCAL,
            HIGHLIGHTS
    };

    //FIXME: ported this over to use static handler classes, but why not use AsyncTask instead?
    private SuggestionsHandler mSuggestionsHandler;
    private Handler mNewSuggestionsHandler = new Handler();
    private HandlerThread mSuggestionsHandlerThread;
    private String mCurrentConstraint;
    private Pattern mCurrentPattern;

    private @NotNull SearchSuggestions mLocalSuggestions  = SearchSuggestions.EMPTY;
    private @NotNull SearchSuggestions mRemoteSuggestions = SearchSuggestions.EMPTY;

    public SuggestionsAdapter(Context context, PublicCloudAPI api, ContentResolver contentResolver) {
        super(context, null, 0);
        this.contentResolver = contentResolver;
        this.context = context;
        mImageOperations = SoundCloudApplication.fromContext(context).getImageOperations();

        mSuggestionsHandlerThread = new HandlerThread("SuggestionsHandler", THREAD_PRIORITY_DEFAULT);
        mSuggestionsHandlerThread.start();
        mSuggestionsHandler = new SuggestionsHandler(this, contentResolver, api, mSuggestionsHandlerThread.getLooper());
    }

    public void onDestroy() {
        mSuggestionsHandler.removeMessages(0);
        mSuggestionsHandlerThread.getLooper().quit();
    }

    @VisibleForTesting
    Looper getApiTaskLooper() {
        return mSuggestionsHandlerThread.getLooper();
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

    public long getModelId(int position) {
        Cursor cursor = (Cursor) getItem(position);
        try {
            return cursor.getLong(cursor.getColumnIndex(TableColumns.Suggestions.ID));
        } finally {
            cursor.close();
        }
    }

    public Uri getItemIntentData(int position) {
        Cursor cursor = (Cursor) getItem(position);
        final String data = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA));
        cursor.close();
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

    public boolean isSearchItem(int position) {
        return position == 0;
    }

    public boolean isLocalResult(int position) {
        Cursor cursor = (Cursor) getItem(position);
        final int isLocal = cursor.getInt(cursor.getColumnIndex(LOCAL));
        cursor.close();
        return isLocal == 1;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public Cursor runQueryOnBackgroundThread(@Nullable final CharSequence constraint) {
        mSuggestionsHandler.removeMessages(0);
        final String searchQuery = ScTextUtils.safeToString(constraint).trim();
        if (!TextUtils.isEmpty(searchQuery)) {
            mCurrentConstraint = searchQuery;
            mCurrentPattern = getHighlightPattern(mCurrentConstraint);

            mLocalSuggestions = fetchLocalSuggestions(mCurrentConstraint, MAX_LOCAL);

            final Message message = mSuggestionsHandler.obtainMessage(0, mCurrentConstraint);
            mSuggestionsHandler.sendMessageDelayed(message, 300);
            return getMixedCursor();

        } else {
            mLocalSuggestions = SearchSuggestions.EMPTY;
            return super.runQueryOnBackgroundThread(searchQuery);
        }
    }

    // this is called from a background thread
    private void onRemoteSuggestions(final CharSequence constraint,
                                     final @NotNull SearchSuggestions suggestions) {
        mNewSuggestionsHandler.post(new Runnable() {
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

    private void prefetchResults(SearchSuggestions suggestions) {
        if (suggestions.isEmpty() || !shouldPrefetch()) return;

        final List<Long> trackIds = new ArrayList<Long>();
        final List<Long> userIds = new ArrayList<Long>();
        final List<Long> playlistIds = new ArrayList<Long>();
        suggestions.putRemoteIds(trackIds, userIds, playlistIds);

        ArrayList<Uri> toSync = new ArrayList<Uri>();
        if (!trackIds.isEmpty()) toSync.add(Content.TRACK_LOOKUP.forQuery(TextUtils.join(",", trackIds)));
        if (!userIds.isEmpty()) toSync.add(Content.USER_LOOKUP.forQuery(TextUtils.join(",", userIds)));
        if (!playlistIds.isEmpty()) toSync.add(Content.PLAYLIST_LOOKUP.forQuery(TextUtils.join(",", playlistIds)));

        if (!toSync.isEmpty()) {
            Intent intent = new Intent(context, ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                    .putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, toSync)
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true);
            context.startService(intent);
        }
    }

    private boolean shouldPrefetch() {
        return IOUtils.isWifiConnected(context);
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

    protected SearchSuggestions getRemote() {
        return mRemoteSuggestions;
    }

    protected SearchSuggestions getLocal() {
        return mLocalSuggestions;
    }

    protected DetachableResultReceiver getReceiver() {
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }

    private SearchSuggestions fetchLocalSuggestions(String constraint, int max) {
        final Cursor cursor = contentResolver.query(
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
        return new MergeCursor(new Cursor[] { createHeader(mCurrentConstraint), c1 }) {
            // for full screen IMEs (e.g. in landscape mode), not the view will be used but the toString method to
            // show results on the keyboard word completion list
            @Override
            public String toString() {
                return getString(getColumnIndex(TableColumns.Suggestions.COLUMN_TEXT1));
            }
        };
    }

    private MatrixCursor createHeader(String constraint) {
        MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES, 1);
        if (!TextUtils.isEmpty(constraint)) {
            cursor.addRow(new Object[]{
                    -1,
                    -1,
                    context.getResources().getString(R.string.search_for_query, constraint),
                    Content.SEARCH_ITEM.forQuery(constraint),
                    "",
                    1 /* local */,
                    null
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
            view = View.inflate(context, R.layout.search_suggestion, null);
            tag = new SearchTag();
            tag.iv_icon = (ImageView) view.findViewById(R.id.icon);
            tag.iv_search_type = (ImageView) view.findViewById(R.id.iv_search_type);
            tag.tv_main = (TextView) view.findViewById(R.id.title);
            view.setTag(tag);
        } else {
            tag = (SearchTag) view.getTag();
        }


        final long id = cursor.getLong(cursor.getColumnIndex(TableColumns.Suggestions.ID));
        final String query = cursor.getString(cursor.getColumnIndex(TableColumns.Suggestions.COLUMN_TEXT1));
        final boolean local = cursor.getInt(cursor.getColumnIndex(LOCAL)) == 1;
        final String highlightData = cursor.getString(cursor.getColumnIndex(HIGHLIGHTS));

        if (id == -1 /* header */) {
            tag.tv_main.setText(query);
        } else {
            tag.tv_main.setText(local ? highlightLocal(query) : highlightRemote(query, highlightData));
        }

        final int rowType = getItemViewType(cursor.getPosition());
        if (rowType == TYPE_SEARCH_ITEM) {
            tag.iv_search_type.setVisibility(View.GONE);
            tag.iv_icon.setImageResource(R.drawable.actionbar_search);
        } else {
            tag.iv_search_type.setVisibility(View.VISIBLE);

            boolean isUser = rowType == TYPE_USER;

            Urn urn;
            if (isUser) {
                urn = Urn.forUser(id);
                tag.iv_search_type.setImageResource(R.drawable.ic_search_user);
            } else {
                urn = Urn.forTrack(id);
                tag.iv_search_type.setImageResource(R.drawable.ic_search_sound);
            }

            mImageOperations.displayInAdapterView(urn,
                    ApiImageSize.getListItemImageSize(context),
                    tag.iv_icon);
        }
        return view;
    }

    static class SearchTag {
        ImageView iv_icon;
        ImageView iv_search_type;
        TextView tv_main;
    }

    protected Spanned highlightRemote(final String query, final String highlightData) {
        SpannableString spanned = new SpannableString(query);
        if (!TextUtils.isEmpty(highlightData)){
            String[] regions = highlightData.split(";");
            for (String regionData : regions) {
                String[] bounds = regionData.split(",");
                setHighlightSpans(spanned, Integer.parseInt(bounds[0]), Integer.parseInt(bounds[1]));
            }
        }
        return spanned;
    }

    private Spanned highlightLocal(String query) {
        SpannableString spanned = new SpannableString(query);
        Matcher m = mCurrentPattern.matcher(query);
        if (m.find()) {
            setHighlightSpans(spanned, m.start(2), m.end(2));
        } else {
            setHighlightSpans(spanned, -1, -1);
        }
        return spanned;
    }

    private static void setHighlightSpans(SpannableString spanned, int start, int end) {
        spanned.setSpan(new ForegroundColorSpan(0xFF666666),
                0, spanned.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        if (start >= 0 && start < end && end > 0 && end <= spanned.length()) {
            spanned.setSpan(new ForegroundColorSpan(Color.WHITE),
                    start, end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    private static final class SuggestionsHandler extends Handler {
        private final ContentResolver resolver;
        private WeakReference<SuggestionsAdapter> mAdapterRef;
        private PublicCloudAPI mApi;


        public SuggestionsHandler(SuggestionsAdapter adapter, ContentResolver resolver, PublicCloudAPI api, Looper looper) {
            super(looper);
            this.resolver = resolver;
            mAdapterRef = new WeakReference<SuggestionsAdapter>(adapter);
            mApi = api;
        }

        @Override
        public void handleMessage(Message msg) {
            final SuggestionsAdapter adapter = mAdapterRef.get();
            if (adapter == null) {
                return;
            }
            final CharSequence constraint = (CharSequence) msg.obj;
            try {
                HttpResponse resp = mApi.get(Request.to("/search/suggest").with(
                        "q", constraint,
                        "highlight_mode", "offsets",
                        "limit", MAX_REMOTE));

                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final SearchSuggestions searchSuggestions = mApi.getMapper().readValue(resp.getEntity().getContent(), SearchSuggestions.class);
                    for (SearchSuggestions.Query q : searchSuggestions) {
                        // make sure tracks exist in the DB, so that we can backfill them later
                        if (SearchSuggestions.Query.KIND_TRACK.equals(q.kind)) {
                            ContentValues values = new ContentValues(1);
                            values.put(TableColumns.Sounds._ID, q.id);
                            values.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK);
                            resolver.insert(Content.TRACKS.uri, values);
                        }
                    }
                    adapter.onRemoteSuggestions(constraint, searchSuggestions);
                    return;
                } else {
                    Log.w(TAG, "invalid status code returned: " + resp.getStatusLine());
                }
            } catch (IOException e) {
                Log.w(TAG, "error fetching suggestions", e);
            }
            adapter.onRemoteSuggestions(constraint, SearchSuggestions.EMPTY);
        }
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