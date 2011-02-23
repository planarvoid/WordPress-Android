package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.android.view.ScTabView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;

public class Dashboard extends ScActivity {
    protected LazyListView mListView;
    private Object[] mPreviousState;
    private ScTabView mTracklistView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        CloudUtils.checkState(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_holder);

        if (getIntent().hasExtra("tab")) {
            String tab = getIntent().getStringExtra("tab");
            if ("incoming".equalsIgnoreCase(tab)) {
                mTracklistView = createList(CloudAPI.Enddpoints.MY_ACTIVITIES,
                        CloudUtils.Model.event,
                        R.string.empty_incoming_text,
                        CloudUtils.ListId.LIST_INCOMING);
            } else if ("exclusive".equalsIgnoreCase(tab)) {
                mTracklistView = createList(CloudAPI.Enddpoints.MY_EXCLUSIVE_TRACKS,
                        CloudUtils.Model.event,
                        -1,
                        CloudUtils.ListId.LIST_EXCLUSIVE);

            } else {
                throw new IllegalArgumentException("no valid tab extra");
            }
            setContentView(mTracklistView);
        } else {
            throw new IllegalArgumentException("no tab extra");
        }

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null)
            ((LazyEndlessAdapter) mTracklistView.adapter).restoreState(mPreviousState);
    }

    @Override
    public void onResume() {
        tracker.trackPageView("/dashboard");
        tracker.dispatch();
        super.onResume();
    }


    protected ScTabView createList(String endpoint, CloudUtils.Model model, int emptyText, int listId) {
        LazyBaseAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new EventsAdapterWrapper(this, adp, endpoint, model, "collection");
        mAdapters.add(adp);

        if (emptyText != -1) {
            adpWrap.setEmptyViewText(getResources().getString(emptyText));
        }

        final ScTabView view = new ScTabView(this, adpWrap);
        mListView = CloudUtils.createTabList(this, view, adpWrap, listId, null);
        return view;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return ((LazyEndlessAdapter) mTracklistView.adapter).saveState();
    }

    @Override
    public void onRefresh() {
        mTracklistView.onRefresh();
    }

    public void mapDetails(Parcelable p) {
        // XXX this should only happen once, after authorizing w/ soundcloud
        if (((User) p).id != null) {
            SoundCloudDB.getInstance().resolveUser(getContentResolver(), (User) p, SoundCloudDB.WriteState.all, ((User) p).id);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            String lastUserId = preferences.getString("currentUserId", null);

            Log.i(TAG, "Checking users " + ((User) p).id + " " + lastUserId);

            if (lastUserId == null || !lastUserId.equals(Long.toString(((User) p).id))) {
                Log.i(TAG, "--------- new user");
                preferences.edit().putString("currentUserId", Long.toString(((User) p).id))
                .putString("currentUsername", ((User) p).username).commit();

            }
        }
    }

}
