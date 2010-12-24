package com.soundcloud.android.view;

import java.io.File;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

public class UserlistRow extends LazyRow {
	
    private static final String TAG = "UserlistRow";

	protected User mUser;
	
	protected RemoteImageView mIcon;
	
	protected TextView mUsername;
	protected TextView mLocation;
	protected TextView mTracks;
	protected TextView mFollowers;
	
	protected ImageView mTracksIcon;
	protected ImageView mFollowersIcon;
	
	protected Boolean _isFollowing;
	
	public UserlistRow(Context _context) {
		  super(_context);
		  
		  mUsername = (TextView) findViewById(R.id.username);
		  mLocation = (TextView) findViewById(R.id.location);
		  mTracks = (TextView) findViewById(R.id.tracks);
		  mFollowers = (TextView) findViewById(R.id.followers);
		  
		  mIcon = (RemoteImageView) findViewById(R.id.icon);
		  
		  mTracksIcon = (ImageView) findViewById(R.id.tracks_icon);
		  mFollowersIcon = (ImageView) findViewById(R.id.followers_icon);
		  
	  }
	  
	  @Override
	  protected int getRowResourceId(){
		  return R.layout.user_list_item;
	  }
	 
	 

	  /** update the views with the data corresponding to selection index */
	  @Override
	  public void display(Parcelable p, boolean selected) {
		  	super.display(p, selected);
		  
			mUser = (User) p;
			mUsername.setText(mUser.getData(User.key_username));
			
			setLocation();
			setTrackCount();
			setFollowerCount();
			
			Log.i(TAG,"SIZE 1 " + mTracksIcon.getWidth() + " " + mTracksIcon.getHeight());
			Log.i(TAG,"SIZE 2 " + mFollowersIcon.getWidth() + " " + mFollowersIcon.getHeight());
			
			
			if (mUser.getData(User.key_user_following).equalsIgnoreCase("true")){
				_isFollowing = true;
			} else {
				_isFollowing = false;
			}
			
			
		
				  
		  String remoteUrl = "";
			if (getContext().getResources().getDisplayMetrics().density > 1){
				mIcon.getLayoutParams().width = 67;
				mIcon.getLayoutParams().height = 67;
				remoteUrl = CloudUtils.formatGraphicsUrl(mUser.getData(User.key_avatar_url),GraphicsSizes.large); 
			} else
				remoteUrl = CloudUtils.formatGraphicsUrl(mUser.getData(User.key_avatar_url),GraphicsSizes.badge);
			
			//Log.i("asdf","set row icon " + remoteUrl);
			
			super.setViewImage(mIcon,remoteUrl);
			
			mIcon.loadImage();
		  
	  }
	  
	  @Override
	  protected Drawable getTemporaryDrawable(){
		  return mContext.getResources().getDrawable(R.drawable.artwork_badge);
	  }
	
	  
	  
	  //**********************
	  // givent their own functions to be easily overwritten by subclasses who may not use them or use them differently
	  
	  protected void setLocation(){
		 mLocation.setText(mUser.getData(User.key_location));
	  }
	  
	  protected void setTrackCount(){
		  String trackCount = mContext.getResources().getQuantityString(R.plurals.user_track_count, Integer.parseInt(mUser.getData(User.key_track_count)), Integer.parseInt(mUser.getData(User.key_track_count)));
		  mTracks.setText(trackCount);
	  }
	  
	  protected void setFollowerCount(){
		  String followerCount = mContext.getResources().getQuantityString(R.plurals.user_follower_count, Integer.parseInt(mUser.getData(User.key_followers_count)), Integer.parseInt(mUser.getData(User.key_followers_count)));
		  mFollowers.setText(followerCount);
	  }
	  
	  //**********************End
	  
	  
	  

	 
	
}
