package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuggestionsAdapter extends CursorAdapter {
    private final Context context;

    private final static int TYPE_SEARCH_ITEM = 0;
    private final static int TYPE_TRACK = 1;
    private final static int TYPE_USER = 2;

    private static final int MAX_LOCAL = 5;
    static final int MAX_REMOTE = 5;

    private final ImageOperations imageOperations;

    public static final String ID = "_id"; // required by CursorAdapter
    public static final String URN = "urn";
    public static final String TYPE = "type";
    public static final String QUERY = "query";
    public static final String LOCAL = "_local";
    public static final String HIGHLIGHTS = "_highlights";
    public static final String QUERY_URN = "_query_urn";
    public static final String QUERY_POSITION = "_query_position";

    public static final String[] COLUMN_NAMES = new String[]{
            ID,
            URN,
            TYPE,
            QUERY,
            LOCAL,
            HIGHLIGHTS,
            QUERY_URN,
            QUERY_POSITION
    };

    private final ShortcutsStorage shortcutsStorage;
    private final SearchSuggestionOperations searchSuggestionOperations;
    private Subscription remoteSuggestionsSubscription = RxUtils.invalidSubscription();

    private String currentConstraint;
    private Pattern currentPattern;

    private @NotNull SearchSuggestions<Shortcut> localSuggestions = SearchSuggestions.empty();
    private @NotNull ApiSearchSuggestions remoteSuggestions = ApiSearchSuggestions.empty();

    private final int colorTextUnhighlight;
    private final int colorTextSuggestion;

    @Inject
    public SuggestionsAdapter(Context context,
                              ShortcutsStorage shortcutsStorage,
                              ImageOperations imageOperations,
                              SearchSuggestionOperations searchSuggestionOperations) {
        super(context, null, 0);
        this.context = context;
        this.imageOperations = imageOperations;
        this.shortcutsStorage = shortcutsStorage;
        this.searchSuggestionOperations = searchSuggestionOperations;

        colorTextSuggestion = context.getResources().getColor(R.color.search_suggestion_text);
        colorTextUnhighlight = context.getResources().getColor(R.color.search_suggestion_unhighlighted_text);
    }

    public void onDestroy() {
        remoteSuggestionsSubscription.unsubscribe();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return createViewFromResource(cursor, null, parent.getContext());
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        createViewFromResource(cursor, view, view.getContext());
    }

    @Override
    public int getItemViewType(int position) {
        final Cursor cursor = (Cursor) getItem(position);
        return cursor.getInt(cursor.getColumnIndex(TYPE));
    }

    @Override
    public long getItemId(int position) {
        final Cursor cursor = (Cursor) getItem(position);
        return cursor.getLong(cursor.getColumnIndex(ID));
    }

    public Urn getUrn(int position) {
        final Cursor cursor = (Cursor) getItem(position);
        return new Urn(cursor.getString(cursor.getColumnIndex(URN)));
    }

    public Urn getQueryUrn(int position) {
        final Cursor cursor = (Cursor) getItem(position);
        final String data = cursor.getString(cursor.getColumnIndex(SuggestionsAdapter.QUERY_URN));
        return data == null ? Urn.NOT_SET : new Urn(data);
    }

    public int getQueryPosition(int position) {
        final Cursor cursor = (Cursor) getItem(position);
        return cursor.getInt(cursor.getColumnIndex(SuggestionsAdapter.QUERY_POSITION));
    }

    public boolean isSearchItem(int position) {
        return position == 0;
    }

    public boolean isLocalResult(int position) {
        final Cursor cursor = (Cursor) getItem(position);
        return cursor.getInt(cursor.getColumnIndex(LOCAL)) == 1;
    }

    public void showSuggestionsFor(CharSequence searchQuery) {
        remoteSuggestionsSubscription.unsubscribe();
        getFilter().filter(searchQuery);
    }

    public void clearSuggestions() {
        localSuggestions = SearchSuggestions.empty();
        remoteSuggestions = ApiSearchSuggestions.empty();
        remoteSuggestionsSubscription.unsubscribe();
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public Cursor runQueryOnBackgroundThread(@Nullable final CharSequence constraint) {
        final String searchQuery = Strings.safeToString(constraint).trim();
        if (!TextUtils.isEmpty(searchQuery)) {
            currentConstraint = searchQuery;
            currentPattern = getHighlightPattern(currentConstraint);
            localSuggestions = fetchLocalSuggestions(currentConstraint, MAX_LOCAL);

            remoteSuggestionsSubscription = searchSuggestionOperations.searchSuggestions(currentConstraint)
                    .delaySubscription(200, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new RemoteSuggestionsSubscriber());

            return getMixedCursor();

        } else {
            clearSuggestions();
            return super.runQueryOnBackgroundThread(searchQuery);
        }
    }

    public Cursor createCursor(SearchSuggestions<? extends SearchSuggestion> searchSuggestions) {
        int searchQueryIndex = 0;
        final MatrixCursor cursor = new MatrixCursor(SuggestionsAdapter.COLUMN_NAMES);
        for (SearchSuggestion suggestion : searchSuggestions.getCollection()) {
            addSuggestionToCursor(cursor, suggestion, searchQueryIndex, searchSuggestions.getQueryUrn());
            if(suggestion.isRemote()) {
                searchQueryIndex = searchQueryIndex + 1;
            }
        }
        return cursor;
    }

    private void addSuggestionToCursor(MatrixCursor cursor, SearchSuggestion suggestion, int searchQueryIndex, Urn queryUrn) {
        boolean isRemote = suggestion.isRemote();

        final Urn urn = suggestion.getUrn();
        cursor.addRow(createRow(urn,
                urn.isTrack() ? TYPE_TRACK : TYPE_USER,
                suggestion.getQuery(),
                buildHighlightData(suggestion),
                queryUrn,
                searchQueryIndex,
                isRemote));
    }

    private Cursor getMixedCursor() {
        if (!remoteSuggestions.getCollection().isEmpty()) {
            if (!localSuggestions.getCollection().isEmpty()) {
                return withHeader(createCursor(mergeLocalWithRemote(localSuggestions, remoteSuggestions)));
            } else {
                return withHeader(createCursor(remoteSuggestions));
            }
        } else {
            return withHeader(createCursor(localSuggestions));
        }
    }

    public SearchSuggestions<SearchSuggestion> mergeLocalWithRemote(SearchSuggestions<Shortcut> localSuggestions,
                                                                    ApiSearchSuggestions remoteSuggestions) {
        List<SearchSuggestion> mergedSuggestions = new ArrayList<>();
        for (SearchSuggestion suggestion : localSuggestions.getCollection()) {
            mergedSuggestions.add(suggestion);
        }

        for (ApiSearchSuggestion remoteSuggestion : remoteSuggestions.getCollection()) {
            if (!mergedSuggestions.contains(remoteSuggestion)) {
                mergedSuggestions.add(remoteSuggestion);
            }
        }
        return new SearchSuggestions<>(mergedSuggestions, remoteSuggestions.getQueryUrn());
    }

    @NonNull
    private Object[] createRow(Urn urn, int rowType, Object query, String highlightData, Urn queryUrn, int searchQueryIndex, boolean isRemote) {
        return new Object[]{
                urn.getNumericId(),
                urn,
                rowType,
                query,
                isRemote ? 0 : 1,
                highlightData,
                isRemote ? queryUrn : null,                     // Set query_urn only on remote suggestions
                isRemote ? searchQueryIndex : Consts.NOT_SET    // Set query_position only on remote suggestions
        };
    }

    private SearchSuggestions<Shortcut> fetchLocalSuggestions(String constraint, int max) {
        final List<Shortcut> shortcuts = shortcutsStorage.getShortcuts(constraint, max);
        return SearchSuggestions.fromShortcuts(shortcuts);
    }

    private Cursor withHeader(Cursor c1) {
        return new MergeCursor(new Cursor[]{createHeader(currentConstraint), c1}) {
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
            cursor.addRow(createRow(
                    Urn.NOT_SET,
                    TYPE_SEARCH_ITEM,
                    context.getResources().getString(R.string.search_for_query, constraint),
                    Strings.EMPTY,
                    Urn.NOT_SET,
                    Consts.NOT_SET,
                    false
            ));
        }
        return cursor;
    }

    private View createViewFromResource(Cursor cursor,
                                        @Nullable View convertView,
                                        Context context) {
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


        final long id = cursor.getLong(cursor.getColumnIndex(ID));
        final String query = cursor.getString(cursor.getColumnIndex(QUERY));
        final String highlightData = cursor.getString(cursor.getColumnIndex(HIGHLIGHTS));

        if (id == -1 /* header */) {
            tag.tv_main.setText(query);
            tag.iv_icon.setVisibility(View.GONE);
        } else {
            tag.tv_main.setText(highlight(query, highlightData));
            tag.iv_icon.setVisibility(View.VISIBLE);
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

                imageOperations.displayCircularInAdapterView(urn,
                        ApiImageSize.getListItemImageSize(context),
                        tag.iv_icon);
            } else {
                urn = Urn.forTrack(id);
                tag.iv_search_type.setImageResource(R.drawable.ic_search_sound);

                imageOperations.displayInAdapterView(urn,
                        ApiImageSize.getListItemImageSize(context),
                        tag.iv_icon);
            }
        }
        return view;
    }

    static class SearchTag {
        ImageView iv_icon;
        ImageView iv_search_type;
        TextView tv_main;
    }

    private Spanned highlight(String query, String highlightData) {
        if (Strings.isBlank(highlightData)){
            return highlightLocal(query);
        } else {
            return highlightRemote(query, highlightData);
        }
    }

    protected Spanned highlightRemote(final String query, final String highlightData) {
        SpannableString spanned = new SpannableString(query);
        if (!TextUtils.isEmpty(highlightData)) {
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
        Matcher m = currentPattern.matcher(query);
        if (m.find()) {
            setHighlightSpans(spanned, m.start(2), m.end(2));
        } else {
            setHighlightSpans(spanned, -1, -1);
        }
        return spanned;
    }

    private void setHighlightSpans(SpannableString spanned, int start, int end) {
        spanned.setSpan(new ForegroundColorSpan(colorTextUnhighlight),
                0, spanned.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        if (start >= 0 && start < end && end > 0 && end <= spanned.length()) {
            spanned.setSpan(new ForegroundColorSpan(colorTextSuggestion),
                    start, end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    /**
     * @param query the search query
     * @return a highlight pattern
     * @see <a href="https://github.com/soundcloud/v2/blob/016de18498c410c4c9ff1875bc48286741df69e3/app/collections/shortcuts.js#L30">
     * Definition in next/v2</a>
     */
    /* package */
    static Pattern getHighlightPattern(String query) {
        return Pattern.compile("(^|[\\s.\\(\\)\\[\\]_-])(" + Pattern.quote(query) + ")", Pattern.CASE_INSENSITIVE);
    }

    /****

     NOTE : These are all old comments, and I am leaving them here. For now, we do have to use a cursor adapter.
     -- jon

     //FIXME: this is a wild hack, but we need to pipe the highlight data through the cursor somehow.
     //I don't think SuggestionsAdapter has to be a CursorAdapter to begin with, but should operate directly
     //on SearchSuggestions

     ****/
    private String buildHighlightData(SearchSuggestion suggestion) {
        final List<Map<String, Integer>> hightlights = suggestion.getHighlights();
        if (hightlights == null || hightlights.isEmpty()) {
            return null;
        }

        StringBuilder highlightData = new StringBuilder();
        Iterator<Map<String, Integer>> iterator = hightlights.iterator();
        while (iterator.hasNext()) {
            Map<String, Integer> highlight = iterator.next();
            highlightData
                    .append(highlight.get("pre"))
                    .append(',')
                    .append(highlight.get("post"));
            if (iterator.hasNext()) {
                highlightData.append(';');
            }
        }
        return highlightData.toString();
    }

    private class RemoteSuggestionsSubscriber extends DefaultSubscriber<ApiSearchSuggestions> {
        @Override
        public void onNext(ApiSearchSuggestions suggestions) {
            remoteSuggestions = suggestions;
            swapCursor(getMixedCursor());
        }
    }
}
