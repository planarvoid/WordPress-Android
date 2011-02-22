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
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.view.LazyList;
import com.soundcloud.android.view.ScTabView;

import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class Dashboard extends ScActivity implements AdapterView.OnItemClickListener {

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
        CloudUtils.createTabList(this, view, adpWrap, listId);
        return view;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mPlaybackStatusListener);
        CloudUtils.cleanupList(mList);
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
        if (mList != null)
            return;

        mCurrentTrackId = l;


        if (mList.getAdapter() != null) {
            if (mList.getAdapter() instanceof TracklistAdapter)
                ((TracklistAdapter) mList.getAdapter()).setPlayingId(mCurrentTrackId);
            else if (mList.getAdapter() instanceof EventsAdapter)
                ((EventsAdapter) mList.getAdapter()).setPlayingId(mCurrentTrackId);
        }
    }


    @SuppressWarnings("unchecked")
    protected void restoreListAdapters(Object adapterObject) {
        if (adapterObject == null)
            return;

        ArrayList<ArrayList<Parcelable>> mAdapterDatas = (ArrayList<ArrayList<Parcelable>>) adapterObject;
        for (ArrayList<Parcelable> data : mAdapterDatas) {
            if (data != null) {
                ((LazyBaseAdapter) mList.getAdapter()).getData().addAll(data);
                ((LazyBaseAdapter) mList.getAdapter()).notifyDataSetChanged();
            }
        }
    }


    public LazyList buildList() {
        mList = CloudUtils.createList(this);
        return mList;
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
    public void onRefresh(boolean b) {
        mList.getWrapper().clear();
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
        if (mList == null)
            return null;
        return mList.getWrapper().getTask();
    }

    protected Object saveListConfigs() {
        if (mList == null)
            return null;

        return mList.getWrapper().savePagingData();
    }

    protected Object saveListExtras() {
        if (mList == null)
            return null;
        return mList.getWrapper().saveExtraData();
    }

    protected Object saveListAdapters() {
        if (mList == null)
            return null;
        return mList.getWrapper().getData();
    }


    protected void restoreListTasks(Object taskObject) {
        AppendTask appendTask = (AppendTask) taskObject;

        if (appendTask == null)
            return;

        mList.getWrapper().restoreTask(appendTask);
    }


    protected void restoreListConfigs(Object configObject) {
        int[] config = (int[]) configObject;

        if (config != null) {
            mList.getWrapper().restorePagingData(config);
        }
    }

    protected void restoreListExtras(Object extraObject) {
        if (extraObject == null)
            return;

        mList.getWrapper().restoreExtraData(extraObject.toString());
    }


    /**
     * A list item has been clicked
     */
    public void onItemClick(AdapterView<?> list, View row, int position, long id) {
        // XXX WTF
        if (((LazyBaseAdapter) list.getAdapter()).getData().size() <= 0
                || position >= ((LazyBaseAdapter) list.getAdapter()).getData().size())
            return; // bad list item clicked (possibly loading item)

        if (((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof Track
                || ((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof Event) {
            // track clicked
            this.playTrack(((LazyBaseAdapter) list.getAdapter()).getData(), position);

        } else if (((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof User) {

            // user clicked
            Intent i = new Intent(this, UserBrowser.class);
            i.putExtra("user", ((LazyBaseAdapter) list.getAdapter()).getData().get(position));
            startActivity(i);

        }
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
