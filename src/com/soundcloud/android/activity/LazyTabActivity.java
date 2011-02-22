
package com.soundcloud.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter.AppendTask;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.view.LazyList;
import com.soundcloud.android.view.ScTabView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LazyTabActivity extends LazyActivity {

    private static final String TAG = "LazyTabActivity";

    protected TabHost tabHost;

    protected TabWidget tabWidget;

    protected FrameLayout tabContent;

    protected ArrayList<LazyList> mLists;

    protected Boolean mIgnorePlaybackStatus = false;

    protected Integer setTabIndex = -1;

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
        Log.i(TAG,"Handle intent " + getIntent());
        
        /*if(getIntent() != null && getIntent().getAction() != null)
        {
                if(getIntent().getAction().equals(Intent.ACTION_SEND) && getIntent().getExtras().containsKey(Intent.EXTRA_STREAM))
                        setFileUri((Uri)getIntent().getExtras().get(Intent.EXTRA_STREAM));
        }*/
        
        
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

    protected void onCreateComplete(boolean success) {
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
    
    @Override
    protected void onFavoriteStatusSet(long trackId, boolean isFavorite){

        if (mLists == null || mLists.size() == 0)
            return;

        for (LazyList mList1 : mLists) {
            if (mList1.getAdapter() != null) {
                if (mList1.getAdapter() instanceof TracklistAdapter)
                    ((TracklistAdapter) mList1.getAdapter()).setFavoriteStatus(trackId, isFavorite);
                else if (mList1.getAdapter() instanceof EventsAdapter)
                    ((EventsAdapter) mList1.getAdapter()).setFavoriteStatus(trackId, isFavorite);
            }
        }
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
        return new Object[]{
                mLoadTask
        };
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
        for (LazyList list : mLists) {
            if (list.getAdapter() != null)
                mAdapterDatas.add((ArrayList<Parcelable>) ((LazyBaseAdapter) list.getAdapter())
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
                Log.i(TAG,"Get Current Tab " + tabHost.getCurrentView());
                ((ScTabView) tabHost.getCurrentView()).onRefresh(false);
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
        
        ((ScTabView) tabHost.getCurrentView()).onDestroy();
    }
}
