
package com.soundcloud.android.activity;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter.AppendTask;
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
import java.util.Arrays;

public class Dashboard extends LazyTabActivity {
    protected ScTabView mLastTab;

    private UserBrowser mUserBrowser;
    private ScCreate mScCreate;
    private ScSearch mScSearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        CloudUtils.checkState(this);

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

    @Override
    protected void onRecordingError() {
        if (mScCreate != null) mScCreate.onRecordingError();
    }

    @Override
    protected void onCreateComplete(boolean success) {
        if (mScCreate != null) mScCreate.unlock(success);
    }

    @Override
    public void mapDetails(Parcelable p) {
        if (((User) p).id != null) {
            SoundCloudDB.getInstance().resolveUser(this.getContentResolver(), (User) p, WriteState.all, ((User) p).id);

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
    protected void build() {
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

    @Override
    protected Object[] saveLoadTasks() {
        return mUserBrowser != null ? mUserBrowser.saveLoadTasks() : null;
    }

    @Override
    protected void restoreLoadTasks(Object[] taskObject) {
        if (mUserBrowser != null) mUserBrowser.restoreLoadTasks(taskObject);

    }

    @Override
    protected Parcelable saveParcelable() {
        return mUserBrowser != null ? mUserBrowser.saveParcelable() : null;

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
    public void onSaveInstanceState(Bundle state) {
        mScCreate.onSaveInstanceState(state);
        mUserBrowser.onSaveInstanceState(state);
        mScSearch.onSaveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mScCreate.onRestoreInstanceState(state);
        mUserBrowser.onRestoreInstanceState(state);
        mScSearch.onRestoreInstanceState(state);

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
                
            case CloudUtils.RequestCodes.GALLERY_IMAGE_TAKE:
                Log.i(TAG,"Result Code " + resultCode);
                if (mScCreate != null) {
                    mScCreate.setTakenImage();
                }
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

}
