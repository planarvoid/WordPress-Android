package com.soundcloud.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.ViewFlipper;
import android.widget.TabHost.OnTabChangeListener;

import com.soundcloud.utils.AnimUtils;

public class TabbedTracklist extends TrackList{
	
	private static final String TAG = "TabbedTrackList";
	
	protected TabHost tabHost;
	protected TabWidget tabWidget;

	
	protected int mUserTracksIndex = 0;
	protected int mFavoritesIndex = 1;
	protected int mSetsIndex = 2;
	
	protected ViewFlipper viewFlipper;

	
	protected Integer setTabIndex = 0;
	protected Boolean mSetTabInstant = false;
	

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
	protected void build(){
		
	}
	
	

	protected void createTabHost(){
		createTabHost(false);
	}
	
	protected void createTabHost(Boolean scrolltabs){
		FrameLayout tabLayout = new FrameLayout(this);
		tabLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (scrolltabs)
			inflater.inflate(R.layout.cloudscrolltabs, tabLayout);
		else
			inflater.inflate(R.layout.cloudtabs, tabLayout);
		
		mHolder.addView(tabLayout);
		
		viewFlipper = new ViewFlipper(this);
		viewFlipper.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		
		
		mHolder.addView(viewFlipper);
		
		//construct the tabhost
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			
			
			   @Override
			  public void onTabChanged(String arg0) {
				   if (tabHost.getCurrentTab() < 0)
					   return;
				   
				   switch (viewFlipper.getDisplayedChild() - tabHost.getCurrentTab()){
				   		case 1 :
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
			   Log.i(TAG,"Setting listener to " + viewFlipper.getCurrentView());
			   //viewFlipper.getCurrentView().setOnTouchListener(swipeListener);
	;			   //viewFlipper.getCurrentView().setOnTouchListener(gestureListener);
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
	    
	
	    
	}
	
	
	
	protected void createTab(String tabId, String indicatorText, Drawable indicatorIcon, final ScTabView tabContent, Boolean scrolltabs)
	{
		Resources res = getResources(); // Resource object to get Drawables
	    TabHost.TabSpec spec;
	    
	    spec = tabHost.newTabSpec(tabId);
	    //spec.setIndicator(new ScTab(this, R.drawable.ic_tab_track,res.getString(R.string.tab_tracks),scrolltabs));
	    spec.setIndicator(indicatorText, indicatorIcon);
	    viewFlipper.addView(tabContent);
	    
	    spec.setContent(new TabHost.TabContentFactory(){
	        public View createTabContent(String tag)
	        {
	       	 	return viewFlipper;
	       	 	
	        }
	    }); 
	    
	    tabHost.addTab(spec);
	}
	
	
	

	
	
	
	@Override
	protected int getCurrentSectionIndex() { 
		return tabHost.getCurrentTab();
	}
	
	
	

	
	
	@Override
	public void leftSwipe(){
		if (tabHost == null || tabHost.getChildCount() < 2)
		 	 return;
		
		/*viewFlipper.setInAnimation(AnimUtils.inFromRightAnimation());
    	viewFlipper.setOutAnimation(AnimUtils.outToLeftAnimation());
        viewFlipper.showNext();
        tabHost.setCurrentTab(viewFlipper.getDisplayedChild());*/
	}
	
	@Override
	public void rightSwipe(){
		if (tabHost == null || tabHost.getChildCount() < 2)
		 	 return;
		
		/*viewFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
        viewFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
     	viewFlipper.showPrevious();
     	tabHost.setCurrentTab(viewFlipper.getDisplayedChild());*/
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) 
    {
		if (tabHost != null){
			Log.i("DEBUG","Putting out index " + Integer.toString(tabHost.getCurrentTab()));
			outState.putString("currentTabIndex", Integer.toString(tabHost.getCurrentTab()));	
		}
        
        super.onSaveInstanceState(outState); 
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) 
    {
        super.onRestoreInstanceState(savedInstanceState);
        
        Log.i("DEBUG","On restore instance state " + savedInstanceState.getString("currentTabIndex") );
    	mSetTabInstant = false;
    	
        if (tabHost != null){
        	String setTabIndexString = savedInstanceState.getString("currentTabIndex");   
            if (!CloudUtils.stringNullEmptyCheck(setTabIndexString)){
            	mSetTabInstant = true;
            	if (tabHost != null){
            		tabHost.setCurrentTab(Integer.parseInt(setTabIndexString));
            	} else {
            		setTabIndex = Integer.parseInt(setTabIndexString);
            	}
        }
        }
    }
	
	
	
}
