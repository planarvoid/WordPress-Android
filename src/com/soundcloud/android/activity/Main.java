package com.soundcloud.android.activity;

import java.util.ArrayList;

import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TabHost.OnTabChangeListener;

import com.soundcloud.android.CloudCommunicator;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.PCMPlaybackTask;
import com.soundcloud.android.task.PCMRecordTask;
import com.soundcloud.android.view.ScCreate;
import com.soundcloud.android.view.ScSearch;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.android.view.UserBrowser;

public class Main extends LazyTabActivity {

    
    private static final String TAG = "Main";
	
	protected ScTabView mLastTab;
	protected int mFavoritesIndex = 1;
	protected int mSetsIndex = 2;
	
	
	private UserBrowser mUserBrowser;
	private ScCreate mScCreate;
	private ScSearch mScSearch;
	
	private Boolean initialAuth = true;
	
	
	protected interface Tabs {
		public final static String TAB_INCOMING = "incoming";
		public final static String TAB_EXCLUSIVE = "exclusive";
		public final static String TAB_MY_TRACKS = "myTracks";
		public final static String TAB_RECORD = "record";
		public final static String TAB_SEARCH = "search";
		
		public final static String TAB_USER_DETAILS = "userDetails";
	    public final static String TAB_USER_TRACKS = "userTracks";
	    public final static String TAB_USER_SETS = "userSets";
	    public final static String TAB_FAVORITES = "favorites";
	    public final static String TAB_FOLLOWINGS = "followings";
	    public final static String TAB_FOLLOWERS = "followers";
	}
	

	
	private SoundCloudAPI.State mLastCloudState;
	
	private Boolean _launch = true;
	
	
	
	
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState,R.layout.main_holder);

		mLastCloudState = mCloudComm.getState();
		
		mMainHolder = ((LinearLayout) findViewById(R.id.main_holder));
		if(getIntent() != null && getIntent().getAction() != null)
        {
                if(getIntent().getAction().equals(Intent.ACTION_SEND) && getIntent().getExtras().containsKey(Intent.EXTRA_STREAM)){
                    //setFileUri((Uri)getIntent().getExtras().get(Intent.EXTRA_STREAM));	
                }
        }
		
	}
	
	

	@Override
    public void onResume() {
		
		 tracker.trackPageView("/dashboard");
		
    	super.onResume();
    	
    	if (_launch){
    		_launch = false;
    		CloudUtils.buildDirs();
    	}
    	
    	Uri uri = this.getIntent().getData();
		if(uri != null) {
			if (uri.getQueryParameter("oauth_verifier") != ""){
				Log.i(TAG,"Updating auth state");
				mCloudComm.updateAuthorizationStatus(uri.getQueryParameter("oauth_verifier"));
				if (mCloudComm.getState() == SoundCloudAPI.State.AUTHORIZED) {
					
				}
			}
		}
    	
    	
    	Log.i(TAG,"Building with " + mCloudComm.getState() + " " + mLastCloudState);
    	
		if (mCloudComm.getState() == SoundCloudAPI.State.AUTHORIZED) {
			
			mHolder.setVisibility(View.VISIBLE);
			
    		initLoadTasks();
    		activateLists();
    		
    		//for (LazyList list : mLists){
    			//((LazyBaseAdapter) list.getAdapter()).notifyDataSetChanged();
    		//}
		} else {
				try {
					startActivity(mCloudComm.getAuthorizationIntent());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		mLastCloudState = mCloudComm.getState();
    	

   
    }
	
	
	public void gotoUserTab(UserBrowser.UserTabs tab){
		tabHost.setCurrentTab(2);
		mUserBrowser.setUserTab(tab);
	}
	
	public void forcePause(){
		try {
			if (mService != null){
				if (mService.isPlaying()){
						mService.pause();
				}
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onRecordingComplete(Boolean success){
		Log.i(TAG,"ON RECORDING COMPLETE " + success);
		if (mScCreate != null)
			mScCreate.unlock(success);
	}
	
	@Override
	public void mapDetails(Parcelable p){
		
		CloudUtils.resolveUser(this, (User) p, true, ((User) p).getData(User.key_id));
	
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String lastUserId = preferences.getString("currentUserId", ""); 
		if ( lastUserId == "" || lastUserId != ((User) p).getData(User.key_id))
			preferences.edit()
			.putString("currentUserId",((User) p).getData(User.key_id))
			.putString("currentUsername",((User) p).getData(User.key_username))
			.commit();
		
			
	 }
	
	@Override
	protected void build(){
		
		mHolder = (LinearLayout) findViewById(R.id.main_holder);
		mHolder.setVisibility(View.GONE);
		
		initialAuth = false;
		
		FrameLayout tabLayout = CloudUtils.createTabLayout(this);
		tabLayout.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.FILL_PARENT));
		mHolder.addView(tabLayout);
		
		//
		tabHost = (TabHost) tabLayout.findViewById(android.R.id.tabhost);
		tabWidget = (TabWidget) tabLayout.findViewById(android.R.id.tabs);
		
		// setup must be called if you are not initialising the tabhost from XML
	   //tabHost.setup();
	    
		createIncomingTab();
		createExclusiveTab();
		createYouTab();
		createRecordTab();
		createSearchTab();
		
		Log.i(TAG,"BBBBBUILD " + mLists.size());
		
		CloudUtils.setTabTextStyle(this, tabWidget);
		

		 tabHost.setOnTabChangedListener(tabListener);
		
	}
	
	private OnTabChangeListener tabListener = new OnTabChangeListener(){
		 @Override
		  public void onTabChanged(String arg0) {
			 if (mLastTab != null){
				 mLastTab.onStop();
			 } 
			 
			 ((ScTabView) tabHost.getCurrentView()).onStart();
			 mLastTab = (ScTabView) tabHost.getCurrentView();
		 }
	};
	
	// Create an anonymous implementation of OnClickListener
	private OnClickListener mAuthorizeListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			try {
				startActivity(mCloudComm.getAuthorizationIntent());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
	

	
	protected void createIncomingTab(){
		Log.i(TAG,"Creating incoming tab");
		LazyBaseAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>());
		LazyEndlessAdapter adpWrap = new EventsAdapterWrapper(this,adp,CloudCommunicator.PATH_MY_ACTIVITIES,CloudUtils.Model.event,"collection");
		
		final ScTabView incomingView = new ScTabView(this,adpWrap);
		CloudUtils.createTabList(this, incomingView, adpWrap);
		CloudUtils.createTab(this, tabHost, "incoming",getString(R.string.tab_incoming),getResources().getDrawable(R.drawable.ic_tab_incoming),incomingView, false);
		Log.i(TAG,"incoming tab created");
	}
	
	protected void createExclusiveTab(){
		LazyBaseAdapter adp = new EventsAdapter(this, new ArrayList<Parcelable>());
		LazyEndlessAdapter adpWrap = new EventsAdapterWrapper(this,adp,CloudCommunicator.PATH_MY_EXCLUSIVE_TRACKS,CloudUtils.Model.event,"collection");
		//LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this,adp,getFavoritesUrl(),CloudUtils.Model.track);
		
		final ScTabView favoritesView = new ScTabView(this,adpWrap);
		CloudUtils.createTabList(this, favoritesView, adpWrap);
		CloudUtils.createTab(this, tabHost, "exclusive",getString(R.string.tab_exclusive),getResources().getDrawable(R.drawable.ic_tab_incoming),favoritesView, false);
		
	}
	
	protected void createYouTab(){
		final UserBrowser youView = mUserBrowser = new UserBrowser(this);
		youView.loadYou();
	      
		CloudUtils.createTab(this, tabHost, "you",getString(R.string.tab_you),getResources().getDrawable(R.drawable.ic_tab_you),youView,false);
		
	}
	
	protected void createRecordTab(){
		final ScCreate recordView = mScCreate = new ScCreate(this);
		CloudUtils.createTab(this, tabHost, "favorites",getString(R.string.tab_record),getResources().getDrawable(R.drawable.ic_tab_record),recordView,false);
	}
	
	protected void createSearchTab(){
		final ScTabView searchView = mScSearch = new ScSearch(this);
		CloudUtils.createTab(this, tabHost, "search",getString(R.string.tab_search),getResources().getDrawable(R.drawable.ic_tab_search),searchView,false);
	}
	
		
	
	/*protected void createFavoritesTab(){
		LazyBaseAdapter adp = new TracklistAdapter(this, new ArrayList<Parcelable>());
		LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(this,adp,getFavoritesUrl(),CloudUtils.Model.track);
		
		final ScTabView favoritesView = new ScTabView(this,adpWrap, CloudUtils.LoadType.favorites);
		createList(favoritesView, adpWrap);
		createTab("favorites",getString(R.string.tab_you),getResources().getDrawable(R.drawable.ic_tab_user),favoritesView,false);
	}*/
	
	
	
	
	protected String getPlaylistsUrl() {
		mFilter = CloudCommunicator.TYPE_PLAYLIST;
		return "";
	}

	protected String getFavoritesUrl() {
		return CloudUtils.buildRequestPath(CloudCommunicator.PATH_MY_FAVORITES, getTrackOrder()).toString();
		
	}
	
	@Override
	protected void initLoadTasks(){
		//if (mUserBrowser != null) mUserBrowser.initLoadTasks();
	}
	
	
	@Override
	protected Object[] saveLoadTasks(){
		if (mUserBrowser != null) 
			return mUserBrowser.saveLoadTasks();
		else return null;
	}
	
	@Override
	protected void restoreLoadTasks(Object[] taskObject){
		if (mUserBrowser != null) 
		mUserBrowser.restoreLoadTasks(taskObject);
		
	}
	
	@Override
	protected Parcelable saveParcelable(){
		if (mUserBrowser != null) 
			return mUserBrowser.saveParcelable();
		return null;
		
	}
	
	@Override
	protected void restoreParcelable(Parcelable p){
		if (mUserBrowser != null)
			mUserBrowser.restoreParcelable(p);
	}
	
	@Override
	protected void configureListToData(ArrayList<Parcelable> mAdapterData, int listIndex){
		/**
		 * we have to make sure that the search view has the right adapter set on its list view
		 * so grab the first element out of the data we are restoring and see if its a user. If 
		 * it is then tell the search list to use the user adapter, otherwise use the track
		 * adapter
		 */
		if (mSearchListIndex == listIndex && mAdapterData.size() > 0){
			if (mAdapterData.get(0) instanceof User){
				mScSearch.setAdapterType(true);
			}
			
			mLists.get(mSearchListIndex).setVisibility(View.VISIBLE);
			mLists.get(mSearchListIndex).setFocusable(true);
		}
	}
	
	
	
	

    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user.  It will
     * be followed by {@link #onRestart}.
     */
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart()");
        
        super.onStart();
        
        
        ((ScTabView) tabHost.getTabContentView().getChildAt(0)).onStart();
        
        
        //if (mCreate != null) mCreate.onStart();
        
   
	   
    }



    
    /**
     * Called as part of the activity lifecycle when an activity is going
     * into the background, but has not (yet) been killed.  The counterpart
     * to onResume(). 
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        
        super.onPause();
        
    }


    /**
     * Called when you are no longer visible to the user.  You will next
     * receive either {@link #onStart}, {@link #onDestroy}, or nothing,
     * depending on later user activity.
     */
    @Override
    protected void onStop() {
        Log.i(TAG, "onStop()");
        super.onStop();
        
        ((ScTabView) tabHost.getTabContentView().getChildAt(0)).onStop();

        //if (mCreate != null) mCreate.onStop();
    }
    
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(menu.size(), CloudUtils.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(R.drawable.context_refresh);
		return super.onCreateOptionsMenu(menu);
	}
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case CloudUtils.OptionsMenu.REFRESH:
				((ScTabView) tabHost.getTabContentView().getChildAt(0)).onRefresh();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	
    
	
    @Override
	public void onSaveInstanceState(Bundle outState) 
    {
		mScCreate.onSaveInstanceState(outState);
		mUserBrowser.onSaveInstanceState(outState);
		mScSearch.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState); 
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) 
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	mScCreate.onRestoreInstanceState(savedInstanceState);
    	mUserBrowser.onRestoreInstanceState(savedInstanceState);
    	mScSearch.onRestoreInstanceState(savedInstanceState);
		
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
				mScCreate.getRecordTask(),
				mScCreate.getPlaybackTask()
		};
	}
    
    protected void restoreState() {
		
		
		//restore state
		Object[] saved = (Object[]) getLastNonConfigurationInstance();
		
		Log.i("START","Restore State " + saved);
		
		if (saved != null) {
			restoreLoadTasks((Object[]) saved[1]);
			restoreParcelable((Parcelable) saved[2]);
			restoreListTasks(saved[3]);
			restoreListConfigs(saved[4]);
			restoreListExtras(saved[5]);
			restoreListAdapters(saved[6]);
			mScCreate.setRecordTask((PCMRecordTask) saved[7]);
			mScCreate.setPlaybackTask((PCMPlaybackTask) saved[8]);
		} 
	}
	

    
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

        switch(requestCode) { 
        case CloudUtils.GALLERY_IMAGE_PICK_CODE:
            if(resultCode == RESULT_OK){  
                Uri selectedImage = imageReturnedIntent.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();
                
                if (mScCreate != null){
                	mScCreate.setPickedImage(filePath);
                }
                
            }
        }
    }
    
    
}