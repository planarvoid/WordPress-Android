package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.view.ClearText;
import com.soundcloud.android.view.ScListView;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
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
public class ScSearch extends ScListActivity {

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    private ClearText mTxtQuery;
    private Spinner mSpinner;
    private ScListView mList;
    private Search mCurrentSearch;
    public static String EXTRA_SEARCH_TYPE = "search_type";
    public static String EXTRA_QUERY = "query";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.sc_search);

        mSpinner = (Spinner) findViewById(R.id.spinner_search_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.search_types, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        mTxtQuery = (ClearText) findViewById(R.id.txt_query);

        mList = (ScListView) findViewById(R.id.list_results);
        mList.setVisibility(View.GONE);

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
        if (activities.size() == 0) {
            mTxtQuery.setDefaultDrawableClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to find?");
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
            final Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_QUERY)) {
                perform(new Search(intent.getCharSequenceExtra(EXTRA_QUERY).toString(), intent.getIntExtra(EXTRA_SEARCH_TYPE, Search.SOUNDS)));
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
        track(getClass());
    }

    private Search getSearch() {
        return new Search(mTxtQuery.getText().toString(), mSpinner.getSelectedItemId() < 2 ?
                Search.SOUNDS : Search.USERS);
    }

    boolean perform(final Search search) {
        if (search.isEmpty()) return false;
        // when called from Main or History
        mTxtQuery.setText(search.query);

        switch (search.search_type) {
            case Search.SOUNDS:
                mSpinner.setSelection(1);
                //configureAdapter(mSoundAdpWrapper, search);
                track(Page.Search_results__sounds__keyword, search.query);
                break;

            case Search.USERS:
                mSpinner.setSelection(2);
                //configureAdapter(mUserAdpWrapper, search);
                track(Page.Search_results__people__keyword, search.query);
                break;
            default:
                Log.w(TAG, "unknown search type " + search.search_type);
                return false;
        }

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(mTxtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        mList.setLastUpdated(0);
        mList.setVisibility(View.VISIBLE);
        mCurrentSearch = search;

        return true;
    }


    @Override
    public Object[] onRetainCustomNonConfigurationInstance() {
        return new Object[]{
                mCurrentSearch,
                mList.getVisibility(),

        };
    }

    void restorePreviousState(Object[] previous) {
        mCurrentSearch = (Search) previous[0];
        if ((Integer) previous[1] == View.VISIBLE) {
            mList.setVisibility(View.VISIBLE);
        }

        if (mCurrentSearch != null) {
            mTxtQuery.setText(mCurrentSearch.query);
            //configureAdapter(mCurrentSearch.search_type == Search.SOUNDS ?
            //      mSoundAdpWrapper : mUserAdpWrapper, mCurrentSearch);
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
