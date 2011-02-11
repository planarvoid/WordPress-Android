
package com.soundcloud.android.activity;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter.AppendTask;
import com.soundcloud.android.objects.BaseObj.WriteState;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.PCMPlaybackTask;
import com.soundcloud.android.view.ScCreate;
import com.soundcloud.android.view.ScSearch;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.android.view.UserBrowser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;

import java.util.ArrayList;


public class Dashboard extends LazyTabActivity {

    private static final String TAG = "Dashboard";

    protected ScTabView mLastTab;

    protected int mFavoritesIndex = 1;

    protected int mSetsIndex = 2;

    private ScTabView mIncomingView;

    private ScTabView mExclusiveView;

    private UserBrowser mUserBrowser;

    private ScCreate mScCreate;

    private ScSearch mScSearch;

    private Boolean initialAuth = true;

    public interface TabIndexes {
        public final static int TAB_INCOMING = 0;

        public final static int TAB_EXCLUSIVE = 1;

        public final static int TAB_MY_TRACKS = 2;

        public final static int TAB_RECORD = 3;

        public final static int TAB_SEARCH = 4;
    }

    private Boolean _launch = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        CloudUtils.checkState(this);
        
        //initialize the db here in case it needs to be created, it won't result in locks
        new DBAdapter(this.getSoundCloudApplication());
        
        super.onCreate(savedInstanceState, R.layout.main_holder);

        mMainHolder = ((LinearLayout) findViewById(R.id.main_holder));
        mHolder.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        tracker.trackPageView("/dashboard");
        tracker.dispatch();
        super.onResume();
    }

    public void gotoUserTab(UserBrowser.UserTabs tab) {
        tabHost.setCurrentTab(2);
        mUserBrowser.setUserTab(tab);
    }

    @Override
    protected void onRecordingError() {
        // safeShowDialog(CloudUtils.Dialogs.DIALOG_ERROR_RECORDING);

        if (mScCreate != null)
            mScCreate.onRecordingError();
    }

    @Override
    protected void onCreateComplete(Boolean success) {
        if (mScCreate != null)
            mScCreate.unlock(success);
    }

    @Override
    public void mapDetails(Parcelable p) {
        if (((User) p).id == null)
            return;
        
        CloudUtils.resolveUser(getSoundCloudApplication(), (User) p, WriteState.all, ((User) p).id);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String lastUserId = preferences.getString("currentUserId", null);
        Log.i(TAG, "Checking users " + ((User) p).id + " " + lastUserId);
        if (lastUserId == null || lastUserId != Long.toString(((User) p).id)) {
            Log.i(TAG, "--------- new user");
            preferences.edit().putString("currentUserId", Long.toString(((User) p).id))
                    .putString("currentUsername", ((User) p).username).commit();
        }

    }

    @Override
    protected void build() {

        mHolder = (LinearLayout) findViewById(R.id.main_holder);
        mHolder.setVisibility(View.GONE);

        initialAuth = false;

        FrameLayout tabLayout = CloudUtils.createTabLayout(this);
        tabLayout.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
                android.view.ViewGroup.LayoutParams.FILL_PARENT));
        mHolder.addView(tabLayout);

        //
        tabHost = (TabHost) tabLayout.findViewById(android.R.id.tabhost);
        tabWidget = (TabWidget) tabLayout.findViewById(android.R.id.tabs);

        // setup must be called if you are not initialising the tabhost from XML
        // tabHost.setup();
        createIncomingTab();
        createExclusiveTab();
        createYouTab();
        createRecordTab();
        createSearchTab();

        CloudUtils.setTabTextStyle(this, tabWidget);

        tabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getInt(
                "lastDashboardIndex", 0));
        tabHost.setOnTabChangedListener(tabListener);

    }

    private OnTabChangeListener tabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String arg0) {
            if (mLastTab != null) {
                mLastTab.onStop();
            }

            ((ScTabView) tabHost.getCurrentView()).onStart();
            mLastTab = (ScTabView) tabHost.getCurrentView();

            PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit().putInt(
                    "lastDashboardIndex", tabHost.getCurrentTab()).commit();
        }
    };

    protected void createIncomingTab() {
        LazyBaseAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new EventsAdapterWrapper(this, adp,
                CloudAPI.Enddpoints.MY_ACTIVITIES, CloudUtils.Model.event, "collection");
        adpWrap.setEmptyViewText(getResources().getString(R.string.empty_incoming_text));

        final ScTabView incomingView = mIncomingView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, incomingView, adpWrap, CloudUtils.ListId.LIST_INCOMING);
        CloudUtils.createTab(this, tabHost, "incoming", getString(R.string.tab_incoming),
                getResources().getDrawable(R.drawable.ic_tab_incoming), incomingView, false);
    }

    protected void createExclusiveTab() {
        LazyBaseAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>());
        LazyEndlessAdapter adpWrap = new EventsAdapterWrapper(this, adp,
                CloudAPI.Enddpoints.MY_EXCLUSIVE_TRACKS, CloudUtils.Model.event,
                "collection");
        // LazyEndlessAdapter adpWrap = new
        // LazyEndlessAdapter(this,adp,getFavoritesUrl(),CloudUtils.Model.track);

        final ScTabView exclusiveView = mExclusiveView = new ScTabView(this, adpWrap);
        CloudUtils.createTabList(this, exclusiveView, adpWrap, CloudUtils.ListId.LIST_EXCLUSIVE);
        CloudUtils.createTab(this, tabHost, "exclusive", getString(R.string.tab_exclusive),
                getResources().getDrawable(R.drawable.ic_tab_incoming), exclusiveView, false);

    }

    protected void createYouTab() {
        final UserBrowser youView = mUserBrowser = new UserBrowser(this);
        youView.loadYou();

        CloudUtils.createTab(this, tabHost, "you", getString(R.string.tab_you), getResources()
                .getDrawable(R.drawable.ic_tab_you), youView, false);

    }

    protected void createRecordTab() {
        this.mScCreate = new ScCreate(this);
        CloudUtils.createTab(this, tabHost, "favorites", getString(R.string.tab_record),
                getResources().getDrawable(R.drawable.ic_tab_record), mScCreate, false);
    }

    protected void createSearchTab() {
        this.mScSearch = new ScSearch(this);
        CloudUtils.createTab(this, tabHost, "search", getString(R.string.tab_search),
                getResources().getDrawable(R.drawable.ic_tab_search), mScSearch, false);
    }

    @Override
    protected void initLoadTasks() {
        // if (mUserBrowser != null) mUserBrowser.initLoadTasks();
    }

    @Override
    protected Object[] saveLoadTasks() {
        if (mUserBrowser != null)
            return mUserBrowser.saveLoadTasks();
        else
            return null;
    }

    @Override
    protected void restoreLoadTasks(Object[] taskObject) {
        if (mUserBrowser != null)
            mUserBrowser.restoreLoadTasks(taskObject);

    }

    @Override
    protected Parcelable saveParcelable() {
        if (mUserBrowser != null)
            return mUserBrowser.saveParcelable();
        return null;

    }

    @Override
    protected void restoreParcelable(Parcelable p) {
        if (mUserBrowser != null)
            mUserBrowser.restoreParcelable(p);
    }

    @Override
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

    @Override
    protected void configureListToTask(AppendTask task, int listIndex) {
        if (mSearchListIndex == listIndex && task != null) {
            mScSearch.setAdapterType(task.loadModel == CloudUtils.Model.user);

            mLists.get(mSearchListIndex).setVisibility(View.VISIBLE);
            mLists.get(mSearchListIndex).setFocusable(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mScCreate.onSaveInstanceState(outState);
        mUserBrowser.onSaveInstanceState(outState);
        mScSearch.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mScCreate.onRestoreInstanceState(savedInstanceState);
        mUserBrowser.onRestoreInstanceState(savedInstanceState);
        mScSearch.onRestoreInstanceState(savedInstanceState);

    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {
                super.onRetainNonConfigurationInstance(), saveLoadTasks(), saveParcelable(),
                saveListTasks(), saveListConfigs(), saveListExtras(), saveListAdapters(),
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
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Log.i(TAG, "On Activity Result " + requestCode + " " + resultCode);

        switch (requestCode) {
            case CloudUtils.RequestCodes.GALLERY_IMAGE_PICK:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
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

            case CloudUtils.RequestCodes.REUATHORIZE:
                // CloudCommunicator.refreshInstance(this);
                break;
        }
    }

}
