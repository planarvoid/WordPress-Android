package com.soundcloud.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.UserBrowser;


public class ScProfile extends LazyActivity  {


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
 			
 			if (extras.getParcelable("user") != null){
 				userBrowser.loadUserByObject((User) extras.getParcelable("user"));
 			} if (!CloudUtils.stringNullEmptyCheck(extras.getString("userId"))){
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
