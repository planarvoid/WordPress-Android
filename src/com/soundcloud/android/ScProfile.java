package com.soundcloud.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;


public class ScProfile extends TrackList  {


	private UserBrowser userBrowser;
	
    @Override
    public void onCreate(Bundle icicle) {
    	
        super.onCreate(icicle,R.layout.main_holder);
        
       
		
		
	}
    
    @Override
	protected void build(){
    	 mMainHolder = ((LinearLayout) findViewById(R.id.main_holder));
         userBrowser = new UserBrowser(this);
         mMainHolder.addView(userBrowser);
         
     	Intent intent = getIntent();
 		Bundle extras = intent.getExtras();
 		
 		if (extras != null){
 			
 			
 			if (!CloudUtils.stringNullEmptyCheck(extras.getString("userId"))){
 				userBrowser.loadUserById(extras.getString("userId"));
 			} else if (!CloudUtils.stringNullEmptyCheck(extras.getString("userPermalink"))){
 				userBrowser.loadUserByPermalink(extras.getString("userPermalink"));
 			}
 			
 		} else {
 			userBrowser.loadYou();
 		}
    }

    @Override
    protected void initLoadTasks(){
    	userBrowser.initLoadTasks();
    }

	
	
}
