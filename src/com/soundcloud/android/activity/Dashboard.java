package com.soundcloud.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.android.view.ScTabView;

import java.util.ArrayList;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class Dashboard extends ScActivity implements AdapterView.OnItemClickListener {
    protected LazyListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        CloudUtils.checkState(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_holder);

        Log.d(TAG, "onCreate " + this.getIntent());


        if (getIntent().hasExtra("tab")) {
            String tab = getIntent().getStringExtra("tab");
            if ("incoming".equalsIgnoreCase(tab)) {
                setContentView(
                        createList(CloudAPI.Enddpoints.MY_ACTIVITIES,
                                CloudUtils.Model.event,
                                R.string.empty_incoming_text,
                                CloudUtils.ListId.LIST_INCOMING)
                );
            } else if ("exclusive".equalsIgnoreCase(tab)) {
                setContentView(
                        createList(CloudAPI.Enddpoints.MY_EXCLUSIVE_TRACKS,
                                CloudUtils.Model.event,
                                -1,
                                CloudUtils.ListId.LIST_EXCLUSIVE)
                );
            } else {
                throw new IllegalArgumentException("no valid tab extra");
            }
        } else {
            throw new IllegalArgumentException("no tab extra");
        }

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudPlaybackService.META_CHANGED);
        playbackFilter.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        this.registerReceiver(mPlaybackStatusListener, new IntentFilter(playbackFilter));
    }

    @Override
    public void onResume() {
        tracker.trackPageView("/dashboard");
        tracker.dispatch();
        super.onResume();
    }


    protected View createList(String endpoint, CloudUtils.Model model, int emptyText, int listId) {
        LazyBaseAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new EventsAdapterWrapper(this, adp, endpoint, model, "collection");

        if (emptyText != -1) {
            adpWrap.setEmptyViewText(getResources().getString(emptyText));
        }

        final ScTabView view = new ScTabView(this, adpWrap);
        mListView = CloudUtils.createTabList(this, view, adpWrap, listId, null);
        return view;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mPlaybackStatusListener);
        CloudUtils.cleanupList(mListView);
    }

    private BroadcastReceiver mPlaybackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIgnorePlaybackStatus)
                return;

            String action = intent.getAction();
            if (action.equals(CloudPlaybackService.META_CHANGED)) {
                setPlayingTrack(intent.getIntExtra("trackId", -1));
            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPlayingTrack(-1);
            }
        }
    };


    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{
                super.onRetainNonConfigurationInstance(),
                saveListTasks(),
                saveListConfigs(),
                saveListExtras(),
                saveListAdapters(),
                // mScCreate.getRecordTask(),
        };
    }


    protected void restoreState() {
        // restore state
        Object[] saved = (Object[]) getLastNonConfigurationInstance();

        if (saved != null) {
            restoreListTasks(saved[0]);
            restoreListConfigs(saved[1]);
            restoreListExtras(saved[2]);
            restoreListAdapters(saved[3]);
        }
    }

    private void setPlayingTrack(long l) {
        if (mListView != null)
            return;

        mCurrentTrackId = l;


        if (mListView.getAdapter() != null) {
            if (mListView.getAdapter() instanceof TracklistAdapter)
                ((TracklistAdapter) mListView.getAdapter()).setPlayingId(mCurrentTrackId);
            else if (mListView.getAdapter() instanceof EventsAdapter)
                ((EventsAdapter) mListView.getAdapter()).setPlayingId(mCurrentTrackId);
        }
    }


    @SuppressWarnings("unchecked")
    protected void restoreListAdapters(Object adapterObject) {
        if (adapterObject == null)
            return;

        ArrayList<ArrayList<Parcelable>> mAdapterDatas = (ArrayList<ArrayList<Parcelable>>) adapterObject;
        for (ArrayList<Parcelable> data : mAdapterDatas) {
            if (data != null) {
                ((LazyBaseAdapter) mListView.getAdapter()).getData().addAll(data);
                ((LazyBaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onServiceBound() {
        super.onServiceBound();
        try {
            setPlayingTrack(mPlaybackService.getTrackId());
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mPlaybackService != null) {
            try {
                setPlayingTrack(mPlaybackService.getTrackId());
            } catch (Exception e) {
                Log.e(TAG, "error", e);
            }
        }
    }

    @Override
    protected void onStop() {
        mIgnorePlaybackStatus = false;
        super.onStop();
    }

    @Override
    public void onRefresh() {
        mListView.getWrapper().clear();
    }

    public void configureListMenu(ListView list) {
        list.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("With this track");
                menu.setHeaderIcon(R.drawable.ic_tab_you);
                menu.add(0, 0, 0, "Play").setIcon(R.drawable.play);
                menu.add(0, 1, 1, "View User Profile").setIcon(R.drawable.ic_tab_you);
                menu.add(0, 2, 2, "Favorite").setIcon(R.drawable.favorite);
                SubMenu sharingMenu = menu.addSubMenu("Share to").setIcon(R.drawable.share)
                        .setHeaderIcon(R.drawable.share);
                sharingMenu.add(0, 3, 1, "Twitter");
                sharingMenu.add(0, 4, 1, "Facebook");

            }
        });
    }


    // ******************************************************************** //
    // State Controls
    // ******************************************************************** //


    protected Object saveListTasks() {
        if (mListView == null)
            return null;
        return mListView.getWrapper().getTask();
    }

    protected Object saveListConfigs() {
        if (mListView == null)
            return null;

        return mListView.getWrapper().savePagingData();
    }

    protected Object saveListExtras() {
        if (mListView == null)
            return null;
        return mListView.getWrapper().saveExtraData();
    }

    protected Object saveListAdapters() {
        if (mListView == null)
            return null;
        return mListView.getWrapper().getData();
    }


    protected void restoreListTasks(Object taskObject) {
        AppendTask appendTask = (AppendTask) taskObject;

        if (appendTask == null)
            return;

        mListView.getWrapper().restoreTask(appendTask);
    }


    protected void restoreListConfigs(Object configObject) {
        int[] config = (int[]) configObject;

        if (config != null) {
            mListView.getWrapper().restorePagingData(config);
        }
    }

    protected void restoreListExtras(Object extraObject) {
        if (extraObject == null)
            return;

        mListView.getWrapper().restoreExtraData(extraObject.toString());
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        handleListItemClicked(parent, position);
    }
}
