package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScSearchFragment;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.view.ClearText;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

@Tracking(page = Page.Search_main)
public class ScSearch extends ScActivity {

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    private ClearText mTxtQuery;
    private Spinner mSpinner;
    private Search mCurrentSearch;
    private ScSearchFragment mSearchFragment;
    private Search pendingSearch;

    public static final String EXTRA_SEARCH_TYPE = "search_type";
    public static final String EXTRA_QUERY = "query";

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

        mTxtQuery = (ClearText) findViewById(R.id.txt_query);

        mTxtQuery.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return !isFinishing() && actionId == EditorInfo.IME_ACTION_SEARCH && perform(getSearch());
            }
        });

        // Disable button if no recognition service is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.isEmpty()) {
            mTxtQuery.setDefaultDrawableClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to find?");
                    startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
                }
            });
        } else {
            //use alternative drawable, disable button
        }

        Object[] previousState = getLastCustomNonConfigurationInstance();
        if (previousState != null) {
            restorePreviousState(previousState);
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            mSearchFragment = ScSearchFragment.newInstance();
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
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            pendingSearch = new Search(query, intent.getIntExtra(EXTRA_SEARCH_TYPE, Search.ALL));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Content c = Content.match(intent.getData());
            if (c == Content.SEARCH_ITEM){
                pendingSearch = new Search(intent.getData().getLastPathSegment(), intent.getIntExtra(EXTRA_SEARCH_TYPE, Search.ALL));
            } else {
                // probably came through quick search box, resolve intent through normal system
                startActivity(new Intent(Intent.ACTION_VIEW).setData(intent.getData()));
            }

        }
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }


    @Override
    public Object[] getLastCustomNonConfigurationInstance() {
        return (Object[]) super.getLastCustomNonConfigurationInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        track(getClass());

        if (pendingSearch != null){
            perform(pendingSearch);
            pendingSearch = null;
        }
    }

    private Search getSearch() {
        switch (mSpinner.getSelectedItemPosition()) {
            case 1:
                return new Search(mTxtQuery.getText().toString(), Search.SOUNDS);
            case 2:
                return new Search(mTxtQuery.getText().toString(), Search.USERS);
            default:
                return new Search(mTxtQuery.getText().toString(), Search.ALL);
        }
    }

    boolean perform(final Search search) {
        if (search == null || search.isEmpty()) return false;
        // when called from Main or History
        mTxtQuery.setText(search.query);

        switch (search.search_type) {
            case Search.SOUNDS:
                mSpinner.setSelection(1);
                track(Page.Search_results__sounds__keyword, search.query);
                break;

            case Search.USERS:
                mSpinner.setSelection(2);
                track(Page.Search_results__people__keyword, search.query);
                break;
            default:
                mSpinner.setSelection(0);
                track(Page.Search_results__all__keyword, search.query);
                break;
        }

        mSearchFragment.setCurrentSearch(search);

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(mTxtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        mCurrentSearch = search;
        return true;
    }

    @Override
    public int getMenuResourceId(){
            return -1;
    }


    @Override
    public Object[] onRetainCustomNonConfigurationInstance() {
        return new Object[]{
                mCurrentSearch,

        };
    }

    void restorePreviousState(Object[] previous) {
        mCurrentSearch = (Search) previous[0];
        if (mCurrentSearch != null) {
            mTxtQuery.setText(mCurrentSearch.query);
        }
    }

    /**
     * Handle the results from the voice recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (matches.size() > 0) {
                mTxtQuery.setText(matches.get(0));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
