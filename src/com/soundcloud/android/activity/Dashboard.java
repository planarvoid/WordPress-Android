
package com.soundcloud.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
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

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

public class Dashboard extends ScActivity implements AdapterView.OnItemClickListener {
    protected ScTabView mLastTab;

    protected LinearLayout mHolder;

    protected Parcelable mDetailsData;

    protected long mCurrentTrackId = -1;

    protected SoundCloudAPI.State mLastCloudState;


    protected LinearLayout mMainHolder;

    protected int mSearchListIndex;

    protected TabHost tabHost;

    protected TabWidget tabWidget;

    protected FrameLayout tabContent;

    protected ArrayList<LazyList> mLists;


    protected Integer setTabIndex = -1;
    private boolean mIgnorePlaybackStatus;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        CloudUtils.checkState(this);

        //initialize the db here in case it needs to be created, it won't result in locks
        new DBAdapter(this.getSoundCloudApplication());
        
        super.onCreate(savedInstanceState);

        mLists = new ArrayList<LazyList>();

        // XXX do in manifest

        handleIntent();

        setContentView(R.layout.main_holder);

        Log.d(TAG, "onCreate " + this.getIntent());

        build();

        mMainHolder = ((LinearLayout) findViewById(R.id.main_holder));
        mHolder.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        tracker.trackPageView("/dashboard");
        tracker.dispatch();
        super.onResume();
    }


    private void build() {
        mHolder = (LinearLayout) findViewById(R.id.main_holder);
        mHolder.setVisibility(View.GONE);

        FrameLayout tabLayout = CloudUtils.createTabLayout(this);
        tabLayout.setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
        mHolder.addView(tabLayout);

        tabHost = (TabHost) tabLayout.findViewById(android.R.id.tabhost);
        tabWidget = (TabWidget) tabLayout.findViewById(android.R.id.tabs);

        // setup must be called if you are not initialising the tabhost from XML
        createIncomingTab();
        createExclusiveTab();


        CloudUtils.setTabTextStyle(this, tabWidget);

        tabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(Dashboard.this)
                .getInt("lastDashboardIndex", 0));

        tabHost.setOnTabChangedListener(tabListener);
    }

    private OnTabChangeListener tabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String s) {
            ((ScTabView) tabHost.getCurrentView()).onStart();
            mLastTab = (ScTabView) tabHost.getCurrentView();

            PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit()
                    .putInt("lastDashboardIndex", tabHost.getCurrentTab())
                    .commit();
        }
    };

    protected void createIncomingTab() {
        LazyBaseAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new EventsAdapterWrapper(this, adp,
                CloudAPI.Enddpoints.MY_ACTIVITIES, CloudUtils.Model.event, "collection");
        adpWrap.setEmptyViewText(getResources().getString(R.string.empty_incoming_text));

        final ScTabView incomingView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, incomingView, adpWrap, CloudUtils.ListId.LIST_INCOMING);
        CloudUtils.createTab(tabHost, "incoming", getString(R.string.tab_incoming),
                getResources().getDrawable(R.drawable.ic_tab_incoming), incomingView);
    }

    protected void createExclusiveTab() {
        LazyBaseAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new EventsAdapterWrapper(this, adp,
                CloudAPI.Enddpoints.MY_EXCLUSIVE_TRACKS,
                CloudUtils.Model.event,
                "collection");

        final ScTabView exclusiveView = new ScTabView(this, adpWrap);

        CloudUtils.createTabList(this, exclusiveView, adpWrap, CloudUtils.ListId.LIST_EXCLUSIVE);
        CloudUtils.createTab(tabHost,
                "exclusive",
                getString(R.string.tab_exclusive),
                getResources().getDrawable(R.drawable.ic_tab_incoming),
                exclusiveView);

    }




//    protected Object[] saveLoadTasks() {
//        return mUserBrowser != null ? mUserBrowser.saveLoadTasks() : null;
//    }
//
//    protected void restoreLoadTasks(Object[] taskObject) {
//        if (mUserBrowser != null) mUserBrowser.restoreLoadTasks(taskObject);
//
//    }
//
//    protected Parcelable saveParcelable() {
//        return mUserBrowser != null ? mUserBrowser.saveParcelable() : null;
//
//    }
//
//
//    protected void restoreParcelable(Parcelable p) {
//        if (mUserBrowser != null)
//            mUserBrowser.restoreParcelable(p);
//
//        mDetailsData = p;
//    }







    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.unregisterReceiver(mPlaybackStatusListener);

        for (ListView mList : mLists) {
            CloudUtils.cleanupList(mList);
        }
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

        if (mSearchListIndex == listIndex && mAdapterData.size() > 0) {
            //mScSearch.setAdapterType(mAdapterData.get(0) instanceof User);

            mLists.get(mSearchListIndex).setVisibility(View.VISIBLE);
            mLists.get(mSearchListIndex).setFocusable(true);
        }
    }


    protected void configureListToTask(AppendTask task, int listIndex) {
        if (mSearchListIndex == listIndex && task != null) {
            //mScSearch.setAdapterType(task.loadModel == CloudUtils.Model.user);

            mLists.get(mSearchListIndex).setVisibility(View.VISIBLE);
            mLists.get(mSearchListIndex).setFocusable(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {

        if (tabHost != null) {
                 state.putString("currentTabIndex", Integer.toString(tabHost.getCurrentTab()));
             }


        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);


        if (setTabIndex == -1) {
            String setTabIndexString = state.getString("currentTabIndex");
            if (!TextUtils.isEmpty(setTabIndexString)) {
                setTabIndex = Integer.parseInt(setTabIndexString);
            } else
                setTabIndex = 0;
        }
        if (tabHost != null)
            tabHost.setCurrentTab(setTabIndex);

    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {
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

      @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent();
        super.onNewIntent(intent);
    }


    private void handleIntent() {
        if (getIntent() != null && getIntent().getExtras() != null
                && getIntent().getIntExtra("tabIndex", -1) != -1) {
            if (this.tabHost != null) {
                tabHost.setCurrentTab(getIntent().getIntExtra("tabIndex", 0));
            } else {
                setTabIndex = getIntent().getIntExtra("tabIndex", -1);
            }
            getIntent().getExtras().clear();
        }
    }

    private void setPlayingTrack(long l) {

        if (mLists == null || mLists.size() == 0)
            return;

        mCurrentTrackId = l;

        for (LazyList mList1 : mLists) {
            if (mList1.getAdapter() != null) {
                if (mList1.getAdapter() instanceof TracklistAdapter)
                    ((TracklistAdapter) mList1.getAdapter()).setPlayingId(mCurrentTrackId);
                else if (mList1.getAdapter() instanceof EventsAdapter)
                    ((EventsAdapter) mList1.getAdapter()).setPlayingId(mCurrentTrackId);
            }
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
                ((LazyBaseAdapter) mLists.get(i).getAdapter()).getData().addAll(mAdapterData);
                ((LazyBaseAdapter) mLists.get(i).getAdapter()).notifyDataSetChanged();
            }
            i++;
        }
    }


    public LazyList buildList(boolean isSearchList) {
        if (isSearchList) {
            mSearchListIndex = mLists.size();
        }
        mLists.add(CloudUtils.createList(this));
        return mLists.get(mLists.size() - 1);
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
       ((ScTabView) tabHost.getCurrentView()).onStart();
    }

    @Override
    protected void onStop() {
        mIgnorePlaybackStatus = false;
        super.onStop();
    }

    @Override
    protected void onAuthenticated() {
        super.onAuthenticated();
        if (tabContent == null) {
            tabContent = (FrameLayout) findViewById(android.R.id.tabcontent);
        }

        for (int i=0; i<tabContent.getChildCount(); i++) {
            ((ScTabView)tabContent.getChildAt(i)).onAuthenticated();
        }

        if (mHolder != null)
            mHolder.setVisibility(View.VISIBLE);
        mLastCloudState = getSoundCloudApplication().getState();

    }

    @Override
    protected void onReauthenticate() {
        super.onReauthenticate();
        if (tabContent == null) {
            tabContent = (FrameLayout) findViewById(android.R.id.tabcontent);
        }

        for (int i=0; i<tabContent.getChildCount(); i++) {
            ((ScTabView)tabContent.getChildAt(i)).onReauthenticate();
        }
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
        if (mLists == null || mLists.size() == 0)
            return null;

        AppendTask[] appendTasks = new AppendTask[mLists.size()];
        for (int i = 0; i < mLists.size(); i++) {
            if (mLists.get(i).getWrapper() != null)
                appendTasks[i] = (mLists.get(i).getWrapper()).getTask();
        }
        return appendTasks;
    }

    protected Object saveListConfigs() {
        if (mLists == null || mLists.size() == 0)
            return null;

        int[][] configArrays = new int[mLists.size()][2];
        for (int i = 0; i < mLists.size(); i++) {
            if (mLists.get(i).getWrapper() != null)
                configArrays[i] = (mLists.get(i).getWrapper()).savePagingData();
        }
        return configArrays;
    }

    protected Object saveListExtras() {
        if (mLists == null || mLists.size() == 0)
            return null;

        String[] extraArray = new String[mLists.size()];
        for (int i = 0; i < mLists.size(); i++) {
            if (mLists.get(i).getWrapper() != null)
                extraArray[i] = (mLists.get(i).getWrapper()).saveExtraData();
        }
        return extraArray;
    }

    protected Object saveListAdapters() {
        if (mLists == null || mLists.size() == 0)
            return null;

        // store the data for current lists
        ArrayList<ArrayList<Parcelable>> mAdapterDatas = new ArrayList<ArrayList<Parcelable>>();
        for (LazyList list : mLists) {
            if (list.getAdapter() != null)
                mAdapterDatas.add((ArrayList<Parcelable>) ((LazyBaseAdapter) list.getAdapter())
                        .getData());
            else
                mAdapterDatas.add(null);
        }

        return mAdapterDatas;
    }


    protected void restoreListTasks(Object taskObject) {
        AppendTask[] appendTasks = (AppendTask[]) taskObject;

        if (appendTasks == null)
            return;

        int i = 0;
        for (AppendTask task : appendTasks) {

            configureListToTask(task, i);

            if (mLists.get(i).getWrapper() != null)
                (mLists.get(i).getWrapper()).restoreTask(task);
            i++;
        }
    }



    protected void restoreListConfigs(Object configObject) {
        int[][] configArrays = (int[][]) configObject;

        if (configArrays == null)
            return;

        int i = 0;
        for (int[] config : configArrays) {
            if (mLists.get(i).getWrapper() != null)
                (mLists.get(i).getWrapper()).restorePagingData(config);
            i++;
        }
    }

    protected void restoreListExtras(Object extraObject) {
        String[] extraArrays = (String[]) extraObject;

        if (extraArrays == null)
            return;

        int i = 0;
        for (String extra : extraArrays) {
            if (mLists.get(i).getWrapper() != null)
                (mLists.get(i).getWrapper()).restoreExtraData(extra);
            i++;
        }
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
