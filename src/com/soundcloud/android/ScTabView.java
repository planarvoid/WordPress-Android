package com.soundcloud.android;

import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ListAdapter;


public class ScTabView extends FrameLayout {
    private static final String TAG = "ScTabView";
    
    private LazyActivity mActivity;;
    private ListAdapter mAdapter;
    private CloudUtils.LoadType mLoadType;
    
    public ScTabView(Context c) {
        super(c);
        
        mActivity = (LazyActivity) c;

        //this.setBackgroundColor(c.getResources().getColor(R.color.cloudProgressBackgroundCenter));
        setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.FILL_PARENT));
    }
    
    public ScTabView(Context c, ListAdapter adpWrap) {
        this(c);
        
        mAdapter = adpWrap;

    }
    
    public ListAdapter getAdapter(){
    	return mAdapter;
    }
    
    public CloudUtils.LoadType getLoadType(){
    	return mLoadType;
    }
    
    public void onStart(){
    	Log.i("TAB","ONSTART");
    	if (mAdapter != null){    	
    		if (mAdapter instanceof TracklistAdapter)
    			((TracklistAdapter) mAdapter).setPlayingId(mActivity.getCurrentTrackId());
    		else if (mAdapter instanceof LazyEndlessAdapter)
    				if (((LazyEndlessAdapter) mAdapter).getWrappedAdapter() instanceof TracklistAdapter)
    					((TracklistAdapter) ((LazyEndlessAdapter) mAdapter).getWrappedAdapter()).setPlayingId(mActivity.getCurrentTrackId());
    	}
    }
    
    public void onStop(){
    	Log.i("TAB","ONSTOP");
    }
    
    public void onRefresh(){
    	if (mAdapter != null){    	
    		if (mAdapter instanceof LazyEndlessAdapter){
    			((LazyEndlessAdapter) mAdapter).clear();
    		}
    		
    	}
    }
    
  
}