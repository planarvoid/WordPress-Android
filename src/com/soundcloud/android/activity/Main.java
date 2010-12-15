package com.soundcloud.android.activity;

import java.util.ArrayList;

import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
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

		

		// extract the OAUTH access token if it exists
		Uri uri = this.getIntent().getData();
		if(uri != null) {
		  	oAuthToken = uri.getQueryParameter("oauth_verifier");	
		}
		
		super.onCreate(savedInstanceState,R.layout.main_holder);

		mLastCloudState = mCloudComm.getState();
		
		mMainHolder = ((LinearLayout) findViewById(R.id.main_holder));
		
		
		
		if (oAuthToken != ""){
			mCloudComm.updateAuthorizationStatus(oAuthToken);
			
			build(); //rebuild with new status
		}
		
		if(getIntent() != null && getIntent().getAction() != null)
        {
                if(getIntent().getAction().equals(Intent.ACTION_SEND) && getIntent().getExtras().containsKey(Intent.EXTRA_STREAM)){
                    //setFileUri((Uri)getIntent().getExtras().get(Intent.EXTRA_STREAM));	
                }
        }
		
	}
	
	
	public void gotoUserTab(UserBrowser.UserTabs tab){
		tabHost.setCurrentTab(2);
		mUserBrowser.setUserTab(tab);
	}
	
	@Override
	public void mapDetails(Parcelable p){
		/*	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			String lastUserId = preferences.getString("currentUserId", ""); 
			if ( lastUserId == "" || lastUserId != ((User) p).getData(User.key_id))
				preferences.edit()
				.putString("currentUserId",((User) p).getData(User.key_id))
				.putString("currentUsername",((User) p).getData(User.key_username))
				.commit();*/
			
			//showRefreshBar(String.format(getString(R.string.message_logged_in_as),((User) p).getData(User.key_username)),false);
			//mHolder.addView(mList);
	 }
	
	@Override
	protected void build(){
		
		mHolder = (LinearLayout) findViewById(R.id.main_holder);
		//mHolder.removeAllViews();
		
		Log.i(TAG,"Building with " + mCloudComm.getState() + " " + oAuthToken);
		
		
		if (mCloudComm.getState() == SoundCloudAPI.State.AUTHORIZED){
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
			
			CloudUtils.setTabTextStyle(this, tabWidget);
			

			 tabHost.setOnTabChangedListener(tabListener);
			
			
		} else {
			
			if (oAuthToken == null || oAuthToken == ""){
				try {
					startActivity(mCloudComm.getAuthorizationIntent());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
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
	
	protected void createList(FrameLayout listHolder, LazyEndlessAdapter adpWrap){
		 
		listHolder.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
	    
	    ListView lv = buildList();
	    lv.setAdapter(adpWrap);
	    lv.setOnTouchListener(swipeListener);
	    listHolder.addView(lv);
	}

	
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
		final ScTabView recordView = mScCreate = new ScCreate(this);
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
		if (mUserBrowser != null) mUserBrowser.initLoadTasks();
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
        
        
        //if (mCreate != null) mCreate.onPause();
        
		  
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

        //if (mCreate != null) mCreate.onStop();
    }
    
	@Override
    public void onResume() {
    	super.onResume();
    	
    	if (_launch){
    		_launch = false;
    		CloudUtils.buildCacheDirs();
    	}
    	
    	Log.i(TAG,"Building with " + mCloudComm.getState() + " " + mLastCloudState);
    	
    	if (mCloudComm.getState() != mLastCloudState){
    		if (mCloudComm.getState() == SoundCloudAPI.State.AUTHORIZED) {
	    		initLoadTasks();
	    		activateLists();
	    		
	    		//for (LazyList list : mLists){
	    			//((LazyBaseAdapter) list.getAdapter()).notifyDataSetChanged();
	    		//}
    		} else {
    			if (CloudUtils.stringNullEmptyCheck(oAuthToken) && !initialAuth){
    				
    				
    				
    				
    				
    				try {
    					startActivity(mCloudComm.getAuthorizationIntent());
    				} catch (Exception e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    			}
    		}
    		
    		mLastCloudState = mCloudComm.getState();;
    	}
    	

   
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
    	Log.i(TAG,"On Save Instance State " + mScCreate);
		if (mScCreate != null){
			outState.putString("currentCreateStateIndex", Integer.toString(mScCreate.getCurrentState()));	
		}
        
        super.onSaveInstanceState(outState); 
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) 
    {
    	
    	Log.i(TAG,"On Restore Instance State " + mScCreate);
        

		if (mScCreate != null){
			String currentCreateStateIndex = savedInstanceState.getString("currentCreateStateIndex");  
			mScCreate.setCurrentState(Integer.parseInt(currentCreateStateIndex));
		}
        
		super.onRestoreInstanceState(savedInstanceState);
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