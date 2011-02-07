
package com.soundcloud.android.activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.ViewFlipper;
import android.widget.TabHost.OnTabChangeListener;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter.AppendTask;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.view.LazyList;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.utils.AnimUtils;

public class LazyTabActivity extends LazyActivity {

    private static final String TAG = "LazyTabActivity";

    protected TabHost tabHost;

    protected TabWidget tabWidget;

    protected FrameLayout tabContent;

    protected int mUserTracksIndex = 0;

    protected int mFavoritesIndex = 1;

    protected int mSetsIndex = 2;

    protected ViewFlipper viewFlipper;

    protected ArrayList<LazyList> mLists;

    protected Boolean mIgnorePlaybackStatus = false;

    protected Integer setTabIndex = -1;

    protected LazyBaseAdapter currentAdapter;

    protected interface Tabs {
        public final static String TAB_USER_DETAILS = "userDetails";

        public final static String TAB_USER_TRACKS = "userTracks";

        public final static String TAB_USER_SETS = "userSets";

        public final static String TAB_FAVORITES = "favorites";

        public final static String TAB_FOLLOWINGS = "followings";

        public final static String TAB_FOLLOWERS = "followers";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, int layoutResId) {
        mLists = new ArrayList<LazyList>();
        super.onCreate(savedInstanceState, layoutResId);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudPlaybackService.META_CHANGED);
        playbackFilter.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        this.registerReceiver(mPlaybackStatusListener, new IntentFilter(playbackFilter));

        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(CloudCreateService.RECORD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        uploadFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        this.registerReceiver(mUploadStatusListener, new IntentFilter(uploadFilter));

        handleIntent();
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
            if (this.tabHost != null)
                tabHost.setCurrentTab(getIntent().getIntExtra("tabIndex", 0));
            else
                setTabIndex = getIntent().getIntExtra("tabIndex", -1);
            getIntent().getExtras().clear();
        }
    }

    public LazyList buildList(Boolean isSearchList) {

        if (isSearchList) {
            mSearchListIndex = mLists.size();
        }

        mLists.add(CloudUtils.createList(this));

        return mLists.get(mLists.size() - 1);
    }

    protected void createTabHost() {
        createTabHost(false);
    }

    protected LazyList buildList() {
        return buildList(false);
    }

    protected void createTabHost(Boolean scrolltabs) {
        FrameLayout tabLayout = new FrameLayout(this);
        tabLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (scrolltabs)
            inflater.inflate(R.layout.cloudscrolltabs, tabLayout);
        else
            inflater.inflate(R.layout.cloudtabs, tabLayout);

        mHolder.addView(tabLayout);

        viewFlipper = new ViewFlipper(this);
        viewFlipper.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));

        mHolder.addView(viewFlipper);

        // construct the tabhost
        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setOnTabChangedListener(new OnTabChangeListener() {

            @Override
            public void onTabChanged(String arg0) {
                if (tabHost.getCurrentTab() < 0)
                    return;

                switch (viewFlipper.getDisplayedChild() - tabHost.getCurrentTab()) {
                    case 1:
                        viewFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
                        viewFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
                        break;
                    case -1:
                        viewFlipper.setInAnimation(AnimUtils.inFromRightAnimation());
                        viewFlipper.setOutAnimation(AnimUtils.outToLeftAnimation());
                        break;
                    default:
                        viewFlipper.setInAnimation(null);
                        viewFlipper.setOutAnimation(null);
                        break;
                }
                if (viewFlipper.getCurrentView() == null)
                    return;
                viewFlipper.setDisplayedChild(tabHost.getCurrentTab());
                // viewFlipper.getCurrentView().setOnTouchListener(swipeListener);
                ; // viewFlipper.getCurrentView().setOnTouchListener(gestureListener);
            }
        });

        // Convert the tabHeight depending on screen density
        final float scale = getResources().getDisplayMetrics().density;
        int tabHeight = (int) (scale * 64);

        // every tabhost needs a tabwidget - a container for the clickable tabs
        // up top. The id is important!
        tabWidget = (TabWidget) findViewById(android.R.id.tabs);

        FrameLayout frameLayout = (FrameLayout) findViewById(android.R.id.tabcontent);
        frameLayout.setPadding(0, 0, 0, 0);

        // setup must be called if you are not initialising the tabhost from XML
        tabHost.setup();

        if (setTabIndex != -1)
            tabHost.setCurrentTab(setTabIndex);

    }

    protected void createTab(String tabId, String indicatorText, Drawable indicatorIcon,
            final ScTabView tabContent, Boolean scrolltabs) {
        Resources res = getResources(); // Resource object to get Drawables
        TabHost.TabSpec spec;

        spec = tabHost.newTabSpec(tabId);
        // spec.setIndicator(new ScTab(this,
        // R.drawable.ic_tab_track,res.getString(R.string.tab_tracks),scrolltabs));
        spec.setIndicator(indicatorText, indicatorIcon);
        viewFlipper.addView(tabContent);

        spec.setContent(new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                return viewFlipper;

            }
        });

        tabHost.addTab(spec);
    }

    @Override
    protected void onServiceBound() {
        super.onServiceBound();
        try {
            setPlayingTrack(mService.getTrackId());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mService != null)
            try {
                setPlayingTrack(mService.getTrackId());
            } catch (Exception e) {
                e.printStackTrace();
            }

       ((ScTabView) tabHost.getCurrentView()).onStart();
    }

    @Override
    protected void onStop() {
        mIgnorePlaybackStatus = false;
        ((ScTabView) tabHost.getCurrentView()).onStop();

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

    }

    @Override
    public void playTrack(final List<Parcelable> list, final int playPos) {
        super.playTrack(list, playPos);
        mIgnorePlaybackStatus = true;
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

    protected void onRecordingError() {

    }

    protected void onCreateComplete(Boolean success) {

    }

    private void setPlayingTrack(long l) {

        if (mLists == null || mLists.size() == 0)
            return;

        mCurrentTrackId = l;

        Iterator<LazyList> mListsIterator = mLists.iterator();
        int i = 0;
        while (mListsIterator.hasNext()) {
            ListView mList = mListsIterator.next();
            if (mList.getAdapter() != null) {
                if (mList.getAdapter() instanceof TracklistAdapter)
                    ((TracklistAdapter) mList.getAdapter()).setPlayingId(mCurrentTrackId);
                else if (mList.getAdapter() instanceof EventsAdapter)
                    ((EventsAdapter) mList.getAdapter()).setPlayingId(mCurrentTrackId);
            }
        }
    }

    protected int getCurrentSectionIndex() {
        return tabHost.getCurrentTab();
    }

    @Override
    public void leftSwipe() {
        if (tabHost == null || tabHost.getChildCount() < 2)
            return;

        /*
         * viewFlipper.setInAnimation(AnimUtils.inFromRightAnimation());
         * viewFlipper.setOutAnimation(AnimUtils.outToLeftAnimation());
         * viewFlipper.showNext();
         * tabHost.setCurrentTab(viewFlipper.getDisplayedChild());
         */
    }

    @Override
    public void rightSwipe() {
        if (tabHost == null || tabHost.getChildCount() < 2)
            return;

        /*
         * viewFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
         * viewFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
         * viewFlipper.showPrevious();
         * tabHost.setCurrentTab(viewFlipper.getDisplayedChild());
         */
    }

    // ******************************************************************** //
    // State Controls
    // ******************************************************************** //

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (tabHost != null) {
            outState.putString("currentTabIndex", Integer.toString(tabHost.getCurrentTab()));
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (setTabIndex == -1) {
            String setTabIndexString = savedInstanceState.getString("currentTabIndex");
            if (!TextUtils.isEmpty(setTabIndexString)) {
                setTabIndex = Integer.parseInt(setTabIndexString);
            } else
                setTabIndex = 0;
        }
        if (tabHost != null)
            tabHost.setCurrentTab(setTabIndex);
    }

    protected Object[] saveLoadTasks() {
        Object[] ret = {
            mLoadTask
        };
        return ret;
    }

    protected void restoreLoadTasks(Object[] taskObjects) {
        mLoadTask = (LoadTask) taskObjects[0];
    }

    protected Parcelable saveParcelable() {
        return mDetailsData;
    }

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
        Iterator<LazyList> mListsIterator = mLists.iterator();
        int i = 0;
        while (mListsIterator.hasNext()) {
            ListView mList = mListsIterator.next();
            if (mList.getAdapter() != null)
                mAdapterDatas.add((ArrayList<Parcelable>) ((LazyBaseAdapter) mList.getAdapter())
                        .getData());
            else
                mAdapterDatas.add(null);
        }

        return mAdapterDatas;
    }

    protected void restoreParcelable(Parcelable p) {
        mDetailsData = p;
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

    protected void configureListToTask(AppendTask task, int listIndex) {

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

    protected void configureListToData(ArrayList<Parcelable> mAdapterData, int listIndex) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(menu.size(), CloudUtils.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(
                R.drawable.context_refresh);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CloudUtils.OptionsMenu.REFRESH:
                ((ScTabView) tabHost.getCurrentView()).onRefresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {
                super.onRetainNonConfigurationInstance(), saveLoadTasks(), saveParcelable(),
                saveListTasks(), saveListConfigs(), saveListExtras(), saveListAdapters()
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
        }
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

}
