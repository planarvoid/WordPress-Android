package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.view.ClearText;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

public class SearchActivity extends ScActivity {

    private static final int SPINNER_POS_ALL = 0;
    private static final int SPINNER_POS_SOUNDS = 1;
    private static final int SPINNER_POS_PLAYLISTS = 2;
    private static final int SPINNER_POS_USERS = 3;

    private ClearText mTxtQuery;
    private Spinner mSpinner;
    private Search mCurrentSearch;
    private SearchFragment mSearchFragment;
    private Search mPendingSearch;
    private int mLastSelectedPosition;

    private static final String EXTRA_SEARCH_TYPE = "search_type";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setTitle(getString(R.string.title_search));

        setContentView(R.layout.sc_search);

        mSpinner = (Spinner) findViewById(R.id.spinner_search_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.search_types, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mLastSelectedPosition != position) {
                    perform(getSearchFromInputText());
                }
                mLastSelectedPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mTxtQuery = (ClearText) findViewById(R.id.txt_query);

        mTxtQuery.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return !isFinishing() && actionId == EditorInfo.IME_ACTION_SEARCH && perform(getSearchFromInputText());
            }
        });

        mTxtQuery.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return !isFinishing() && ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) && perform(getSearchFromInputText());
            }
        });


        Object[] previousState = getLastCustomNonConfigurationInstance();
        if (previousState != null) {
            restorePreviousState(previousState);
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            mSearchFragment = SearchFragment.newInstance();
            ft.add(R.id.results_holder, mSearchFragment).commit();
            handleIntent();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent();
    }

    private void handleIntent() {
        final Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()) ||
                "android.media.action.MEDIA_PLAY_FROM_SEARCH".equals(intent.getAction())) {

            String query = intent.getStringExtra(SearchManager.QUERY);
            mPendingSearch = new Search(query, intent.getIntExtra(EXTRA_SEARCH_TYPE, Search.ALL));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && !intent.getData().getPath().equals("/search") /* came from search url intercept */) {
            final Content c = Content.match(intent.getData());
            if (c == Content.UNKNOWN) {
                // probably handling a deeplink. Set the spinner from the last segment if it represents a valid category
                final String segment = Uri.decode(intent.getData().getLastPathSegment());
                final String titleCased = (Character.toTitleCase(segment.charAt(0)) + segment.substring(1).toLowerCase(Locale.US));
                final int index = ((ArrayAdapter<String>) mSpinner.getAdapter()).getPosition(titleCased);
                if (index != -1) mSpinner.setSelection(index);

            } else if (c == Content.SEARCH_ITEM) {
                final String query = Uri.decode(intent.getData().getLastPathSegment());
                mPendingSearch = new Search(query, intent.getIntExtra(EXTRA_SEARCH_TYPE, Search.ALL));
            } else {
                // probably came through quick search box, resolve intent through normal system
                startActivity(new Intent(Intent.ACTION_VIEW).setData(intent.getData()));
            }
        }
    }

    @Override
    public Object[] getLastCustomNonConfigurationInstance() {
        return (Object[]) super.getLastCustomNonConfigurationInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPendingSearch != null) {
            perform(mPendingSearch);
            mPendingSearch = null;
        }
    }

    private Search getSearchFromInputText() {
        String query = mTxtQuery.getText().toString();
        switch (mSpinner.getSelectedItemPosition()) {
            case SPINNER_POS_ALL:
                return Search.forAll(query);
            case SPINNER_POS_SOUNDS:
                return Search.forTracks(query);
            case SPINNER_POS_USERS:
                return Search.forUsers(query);
            case SPINNER_POS_PLAYLISTS:
                return Search.forPlaylists(query);
            default:
                throw new IllegalStateException("Unexpected search filter");
        }
    }

    boolean perform(final Search search) {
        if (search == null || search.isEmpty()) return false;
        // when called from Main or History
        mTxtQuery.setText(search.query);

        switch (search.search_type) {
            case Search.TRACKS:
                mSpinner.setSelection(SPINNER_POS_SOUNDS);
                break;

            case Search.USERS:
                mSpinner.setSelection(SPINNER_POS_USERS);
                break;

            case Search.PLAYLISTS:
                mSpinner.setSelection(SPINNER_POS_PLAYLISTS);
                break;

            default:
                mSpinner.setSelection(SPINNER_POS_ALL);
                break;
        }

        if (mSearchFragment != null) {
            mSearchFragment.setCurrentSearch(search);
            mCurrentSearch = search;
        } else {
            mPendingSearch = search;
        }

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(mTxtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        return true;
    }

    @Override
    public int getMenuResourceId() {
        return -1;
    }

    @Override
    public Object[] onRetainCustomNonConfigurationInstance() {
        return new Object[] { mCurrentSearch };
    }

    void restorePreviousState(Object[] previous) {
        mCurrentSearch = (Search) previous[0];
        if (mCurrentSearch != null) {
            mTxtQuery.setText(mCurrentSearch.query);
        }
        mSearchFragment = (SearchFragment) getSupportFragmentManager().findFragmentById(R.id.results_holder);
    }


}
