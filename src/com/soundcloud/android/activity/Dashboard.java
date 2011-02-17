
package com.soundcloud.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
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
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.PCMPlaybackTask;
import com.soundcloud.android.view.LazyList;
import com.soundcloud.android.view.ScCreate;
import com.soundcloud.android.view.ScSearch;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.android.view.UserBrowser;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

public class Dashboard extends ScActivity implements AdapterView.OnItemClickListener {
    protected ScTabView mLastTab;

    private UserBrowser mUserBrowser;
    private ScCreate mScCreate;
    private ScSearch mScSearch;

    protected LinearLayout mHolder;

    protected Parcelable mDetailsData;

    protected long mCurrentTrackId = -1;

    protected SoundCloudAPI.State mLastCloudState;

    private MenuItem menuCurrentPlayingItem;

    private MenuItem menuCurrentUploadingItem;

    protected LinearLayout mMainHolder;

    protected int mSearchListIndex;

    protected TabHost tabHost;

    protected TabWidget tabWidget;

    protected FrameLayout tabContent;

    protected ArrayList<LazyList> mLists;

    protected Boolean mIgnorePlaybackStatus = false;

    protected Integer setTabIndex = -1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        CloudUtils.checkState(this);

        //initialize the db here in case it needs to be created, it won't result in locks
        new DBAdapter(this.getSoundCloudApplication());
        
        super.onCreate(savedInstanceState);

        mLists = new ArrayList<LazyList>();

        // XXX do in manifest
        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudPlaybackService.META_CHANGED);
        playbackFilter.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        this.registerReceiver(mPlaybackStatusListener, new IntentFilter(playbackFilter));

        // XXX do in manifest
        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(CloudCreateService.RECORD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        uploadFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        this.registerReceiver(mUploadStatusListener, new IntentFilter(uploadFilter));

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

    protected void onRecordingError() {
        if (mScCreate != null) mScCreate.onRecordingError();
    }

    protected void onCreateComplete(boolean success) {
        if (mScCreate != null) mScCreate.unlock(success);
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
        createYouTab();
        createRecordTab();
        createSearchTab();

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

    protected void createYouTab() {
        mUserBrowser = new UserBrowser(this);
        mUserBrowser.loadYou();

        CloudUtils.createTab(tabHost, "you",
                getString(R.string.tab_you),
                getResources().getDrawable(R.drawable.ic_tab_you),
                mUserBrowser);

    }

    protected void createRecordTab() {
        this.mScCreate = new ScCreate(this);
        CloudUtils.createTab(tabHost, "record",
                getString(R.string.tab_record),
                getResources().getDrawable(R.drawable.ic_tab_record),
                mScCreate);
    }

    protected void createSearchTab() {
        this.mScSearch = new ScSearch(this);
        CloudUtils.createTab(tabHost, "search", getString(R.string.tab_search),
                getResources().getDrawable(R.drawable.ic_tab_search), mScSearch);
    }

    protected Object[] saveLoadTasks() {
        return mUserBrowser != null ? mUserBrowser.saveLoadTasks() : null;
    }

    protected void restoreLoadTasks(Object[] taskObject) {
        if (mUserBrowser != null) mUserBrowser.restoreLoadTasks(taskObject);

    }

    protected Parcelable saveParcelable() {
        return mUserBrowser != null ? mUserBrowser.saveParcelable() : null;

    }


    protected void restoreParcelable(Parcelable p) {
        if (mUserBrowser != null)
            mUserBrowser.restoreParcelable(p);

        mDetailsData = p;
    }



    /**
     * Prepare the options menu based on the current class and current play
     * state
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!this.getClass().getName().contentEquals("com.soundcloud.android.ScPlayer")) {
            menuCurrentPlayingItem.setVisible(true);
        } else {
            menuCurrentPlayingItem.setVisible(false);
        }

        try {
            if (mCreateService.isUploading()) {
                menuCurrentUploadingItem.setVisible(true);
            } else {
                menuCurrentUploadingItem.setVisible(false);
            }
        } catch (Exception e) {
            menuCurrentUploadingItem.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

           menuCurrentPlayingItem = menu.add(menu.size(), CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK,
                menu.size(), R.string.menu_view_current_track).setIcon(
                R.drawable.ic_menu_info_details);
        menuCurrentUploadingItem = menu.add(menu.size(),
                CloudUtils.OptionsMenu.CANCEL_CURRENT_UPLOAD, menu.size(),
                R.string.menu_cancel_current_upload).setIcon(R.drawable.ic_menu_delete);

        menu.add(menu.size(), CloudUtils.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings)
                .setIcon(R.drawable.ic_menu_preferences);

        menu.add(menu.size(), CloudUtils.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(
                R.drawable.context_refresh);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CloudUtils.OptionsMenu.REFRESH:
                Log.i(TAG,"Get Current Tab " + tabHost.getCurrentView());
                ((ScTabView) tabHost.getCurrentView()).onRefresh(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.unregisterReceiver(mUploadStatusListener);
        this.unregisterReceiver(mPlaybackStatusListener);

        for (ListView mList : mLists) {
            CloudUtils.cleanupList(mList);
        }
    }

    protected void configureListToData(ArrayList<Parcelable> mAdapterData, int listIndex) {
        /**
         * we have to make sure that the search view has the right adapter set
         * on its list view so grab the first element out of the data we are
         * restoring and see if its a user. If it is then tell the search list
         * to use the user adapter, otherwise use the track adapter
         */

        if (mSearchListIndex == listIndex && mAdapterData.size() > 0) {
            mScSearch.setAdapterType(mAdapterData.get(0) instanceof User);

            mLists.get(mSearchListIndex).setVisibility(View.VISIBLE);
            mLists.get(mSearchListIndex).setFocusable(true);
        }
    }


    protected void configureListToTask(AppendTask task, int listIndex) {
        if (mSearchListIndex == listIndex && task != null) {
            mScSearch.setAdapterType(task.loadModel == CloudUtils.Model.user);

            mLists.get(mSearchListIndex).setVisibility(View.VISIBLE);
            mLists.get(mSearchListIndex).setFocusable(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        mScCreate.onSaveInstanceState(state);
        mUserBrowser.onSaveInstanceState(state);
        mScSearch.onSaveInstanceState(state);

        if (tabHost != null) {
                 state.putString("currentTabIndex", Integer.toString(tabHost.getCurrentTab()));
             }


        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mScCreate.onRestoreInstanceState(state);
        mUserBrowser.onRestoreInstanceState(state);
        mScSearch.onRestoreInstanceState(state);


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
                saveLoadTasks(),
                saveParcelable(),
                saveListTasks(),
                saveListConfigs(),
                saveListExtras(),
                saveListAdapters(),
                // mScCreate.getRecordTask(),
                mScCreate.getPlaybackTask()
        };
    }

    @Override
    protected void restoreState() {
        // restore state
        Object[] saved = (Object[]) getLastNonConfigurationInstance();

        if (saved != null) {
            restoreLoadTasks((Object[]) saved[1]);
            restoreParcelable((Parcelable) saved[2]);
            restoreListTasks(saved[3]);
            restoreListConfigs(saved[4]);
            restoreListExtras(saved[5]);
            restoreListAdapters(saved[6]);
            // mScCreate.setRecordTask((PCMRecordTask) saved[7]);
            mScCreate.setPlaybackTask((PCMPlaybackTask) saved[7]);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        Log.i(TAG, "onActivityResult("+requestCode+", "+resultCode+", "+result+")");

        switch (requestCode) {
            case CloudUtils.RequestCodes.GALLERY_IMAGE_PICK:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = result.getData();
                    String[] filePathColumn = {
                        MediaColumns.DATA
                    };

                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null,
                            null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    if (mScCreate != null) {
                        mScCreate.setPickedImage(filePath);
                    }

                }
                break;

            case CloudUtils.RequestCodes.REAUTHORIZE:
                break;

            case EmailPicker.PICK_EMAILS:
                if (resultCode == RESULT_OK &&result != null && result.hasExtra(EmailPicker.BUNDLE_KEY)) {
                    String[] emails = result.getExtras().getStringArray(EmailPicker.BUNDLE_KEY);
                    if (emails != null) {
                        Log.d(TAG, "got emails " + Arrays.asList(emails));
                        mScCreate.setPrivateShareEmails(emails);
                    }
                }
                break;
            case LocationPicker.PICK_VENUE:
                if (resultCode == RESULT_OK && result != null && result.hasExtra("name")) {
                    mScCreate.setWhere(result.getStringExtra("name"),
                            result.getStringExtra("id"),
                            result.getDoubleExtra("longitude", 0),
                            result.getDoubleExtra("latitude",  0));
                }
                break;

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


    private BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CloudCreateService.UPLOAD_ERROR)
                    || action.equals(CloudCreateService.UPLOAD_CANCELLED))
                onCreateComplete(false);
            else if (action.equals(CloudCreateService.UPLOAD_SUCCESS))
                onCreateComplete(true);
            else if (action.equals(CloudCreateService.RECORD_ERROR))
                onRecordingError();
        }
    };




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
            Intent i = new Intent(this, ScProfile.class);
            i.putExtra("user", ((LazyBaseAdapter) list.getAdapter()).getData().get(position));
            startActivity(i);

        }
    }
}
