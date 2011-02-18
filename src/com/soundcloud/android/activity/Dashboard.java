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
import android.widget.LinearLayout;
import android.widget.ListView;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter.AppendTask;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.objects.BaseObj;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.view.LazyList;
import com.soundcloud.android.view.ScTabView;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class Dashboard extends ScActivity implements AdapterView.OnItemClickListener {
    protected long mCurrentTrackId = -1;
    protected SoundCloudAPI.State mLastCloudState;
    protected LinearLayout mMainHolder;

    protected LazyList mList;


    private boolean mIgnorePlaybackStatus;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        CloudUtils.checkState(this);

        //initialize the db here in case it needs to be created, it won't result in locks
        new DBAdapter(this.getSoundCloudApplication());

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


    protected void configureListToData(ArrayList<Parcelable> mAdapterData, int listIndex) {
        /**
         * we have to make sure that the search view has the right adapter set
         * on its list view so grab the first element out of the data we are
         * restoring and see if its a user. If it is then tell the search list
         * to use the user adapter, otherwise use the track adapter
         */

        //if (mSearchListIndex == listIndex && mAdapterData.size() > 0) {

        mList.setVisibility(View.VISIBLE);
        mList.setFocusable(true);
        //}
    }


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
            restoreListTasks(saved[3]);
            restoreListConfigs(saved[4]);
            restoreListExtras(saved[5]);
            restoreListAdapters(saved[6]);
            // mScCreate.setRecordTask((PCMRecordTask) saved[7]);
        }
    }

    private void setPlayingTrack(long l) {

        if (mList == null)
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
        Iterator<ArrayList<Parcelable>> mAdapterDataIterator = mAdapterDatas.iterator();
        int i = 0;
        while (mAdapterDataIterator.hasNext()) {

            ArrayList<Parcelable> mAdapterData = mAdapterDataIterator.next();

            configureListToData(mAdapterData, i);

            if (mAdapterData != null) {
                ((LazyBaseAdapter) mList.getAdapter()).getData().addAll(mAdapterData);
                ((LazyBaseAdapter) mList.getAdapter()).notifyDataSetChanged();
            }
            i++;
        }
    }


    public LazyList buildList(boolean isSearchList) {
        mList = CloudUtils.createList(this);
        return mList;
    }

    @Override
    protected void onServiceBound() {
        super.onServiceBound();
        try {
            setPlayingTrack(mService.getTrackId());
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mService != null)
            try {
                setPlayingTrack(mService.getTrackId());
            } catch (Exception e) {
                Log.e(TAG, "error", e);
            }
    }

    @Override
    protected void onStop() {
        mIgnorePlaybackStatus = false;
        super.onStop();
    }

    @Override
    protected void onAuthenticated() {
        super.onAuthenticated();
        mLastCloudState = getSoundCloudApplication().getState();

    }

    @Override
    public void onRefresh(boolean b) {
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

    public void playTrack(final List<Parcelable> list, final int playPos) {


        Track t = null;

        // is this a track of a list
        if (list.get(playPos) instanceof Track)
            t = ((Track) list.get(playPos));
        else if (list.get(playPos) instanceof Event)
            t = ((Event) list.get(playPos)).getTrack();

        // find out if this track is already playing. If it is, just go to the
        // player
        try {
            if (t != null && mService != null && mService.getTrackId() != -1
                    && mService.getTrackId() == (t.id)) {
                // skip the enqueuing, its already playing
                Intent intent = new Intent(this, ScPlayer.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        // pass the tracklist to the application. This is the quickest way to get it to the service
        // another option would be to pass the parcelables through the intent, but that has the
        // unnecessary overhead of unmarshalling/marshalling them in to bundles. This way
        // we are just passing pointers
        this.getSoundCloudApplication().cachePlaylist((ArrayList<Parcelable>) list);

        try {
            Log.i(TAG, "Play from app cache call");
            mService.playFromAppCache(playPos);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        Intent intent = new Intent(this, ScPlayer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);


        mIgnorePlaybackStatus = true;
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

            CloudUtils.resolveUser(getSoundCloudApplication(), (User) p, BaseObj.WriteState.all, ((User) p).id);
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
