package com.soundcloud.android;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.utils.RemoteImageView;

public class UserBrowser extends ScTabView {

	private static String TAG = "UserBrowser";
	
	private LazyActivity mActivity;
	
	protected RemoteImageView mIcon;
	protected TableLayout mUserTable;
	protected LinearLayout mUserTableHolder;
	
	protected LinearLayout mDetailsView;
	
	protected TextView mUser;
	protected TextView mLocation;
	protected TextView mTracks;
	protected TextView mFollowers;
	
	protected TextView mFullName;
	protected TextView mWebsite;
	protected TextView mDiscogsName;
	protected TextView mMyspaceName;
	protected TextView mDescription;
	
	protected Button mToggleView;
	protected TextView mFavoriteStatus;
	protected ImageButton mFavorite;
	
	protected ListView lv_followers;
	protected ListView lv_followings;
	
	protected LoadTask mLoadFollowingsTask;
	protected LoadTask mLoadFollowersTask;
	
	protected String _permalink;
	protected String _username;
	protected String _location;
	protected String _tracks;
	protected String _followers;
	protected String _iconURL;
	
	protected int mDetailsIndex = 1;
	protected int mFollowersIndex = 3;
	protected int mFollowingsIndex = 4;
		
	protected String mUserLoadPermalink = "";
	protected Boolean _isFollowing = false;
	
	protected LoadTask mLoadDetailsTask;
	
	protected String mFollowResult;
	
	private User mUserData;
	private Boolean mIsOtherUser;
	
	protected DecimalFormat mSecondsFormat = new DecimalFormat("00");


	public UserBrowser(LazyActivity c) {
		super(c);
		
		mActivity = c;
		
		LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.user_view, this);
		mDetailsView = (LinearLayout) inflater.inflate(R.layout.user_details, null);

		mIcon = (RemoteImageView) findViewById(R.id.user_icon);
		mUser = (TextView) findViewById(R.id.username);
		mLocation = (TextView) findViewById(R.id.location);
		mTracks = (TextView) mDetailsView.findViewById(R.id.tracks);
		mFollowers = (TextView) mDetailsView.findViewById(R.id.followers);
		
		mFullName = (TextView) mDetailsView.findViewById(R.id.fullname);
		mWebsite = (TextView) mDetailsView.findViewById(R.id.website);
		mDiscogsName = (TextView) mDetailsView.findViewById(R.id.discogs_name);
		mMyspaceName = (TextView) mDetailsView.findViewById(R.id.myspace_name);
		mDescription = (TextView) mDetailsView.findViewById(R.id.description);
		
		//mUserTableHolder = (LinearLayout) mDetailsView.findViewById(R.id.user_details_table_holder);
		//mUserTable = (TableLayout) mDetailsView.findViewById(R.id.user_details_table);
		//mUserTableHolder.removeView(mUserTable);
		
		//mFavoriteStatus = (TextView) mDetailsView.findViewById(R.id.favorited_status);
		mFavorite = (ImageButton) findViewById(R.id.btn_favorite);
		mFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                toggleFollowing(); 
        }
        });
		
		mFavorite.setEnabled(false);
		
	
		
		

        
	}
	
	public void loadYou(){
		mIsOtherUser = false;
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		User userInfo = null;
		
		if (preferences.getString("currentUserId", "") != "")
			userInfo = CloudUtils.resolveUserById(mActivity, preferences.getString("currentUserId", ""), CloudUtils.getCurrentUserId(mActivity));
		
		 if (userInfo != null)
			 mapUser(userInfo);
		 
		 build();
	}
	
	public void loadUserByPermalink(String userPermalink){
		mIsOtherUser = true;
		mUserLoadPermalink = userPermalink;
		
		
		User userInfo = null;
		userInfo = CloudUtils.resolveUserByPermalink(mActivity, userPermalink, CloudUtils.getCurrentUserId(mActivity));
		
        if (userInfo != null)
        	mapUser(userInfo);
        
        build();
		
	}
	
	public void loadUserById(String userId){
		Log.i(TAG,"Load user by id " + userId);
		mIsOtherUser = true;
		User userInfo = null;
		userInfo = CloudUtils.resolveUserById(mActivity, userId, CloudUtils.getCurrentUserId(mActivity));
		mUserLoadPermalink = userId;
		
        if (userInfo != null)
        	mapUser(userInfo);
        
        build();
	}
	
	public void loadUserByObject(User userInfo){
		mIsOtherUser = true;
		mUserLoadPermalink = userInfo.getData(User.key_id);
		mapUser(userInfo);
		
		build();
	}
	

	protected void initLoadTasks(){
		
		Log.i(TAG,"Init Load Tasks " + mLoadDetailsTask);
		
		if (mLoadDetailsTask == null){
			mLoadDetailsTask = newLoadDetailsTask();
			mLoadDetailsTask.execute();	
		} else {
			mLoadDetailsTask.setActivity(mActivity);
			if (CloudUtils.isTaskPending(mLoadDetailsTask)) mLoadDetailsTask.execute();
		}
		
	}
	
	protected LoadTask newLoadDetailsTask() {
		LoadTask lt = new LoadUserDetailsTask();
		lt.loadModel = CloudUtils.Model.user;
		lt.mUrl = getDetailsUrl();
		lt.setActivity(mActivity);
		return lt;
	}
	
	protected class LoadUserDetailsTask extends LoadDetailsTask {
		@Override
		protected void mapDetails(Parcelable update){
			Log.i(TAG, "Map My Details");
			
			mapUser(update);
		}
	}
	
	
	protected void build(){

		
		
			FrameLayout tabLayout = CloudUtils.createTabLayout(mActivity);
		tabLayout.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,android.view.ViewGroup.LayoutParams.FILL_PARENT));
		
		((LinearLayout) findViewById(R.id.tab_holder)).addView(tabLayout);
		
		
		
		TabHost tabHost = (TabHost) tabLayout.findViewById(android.R.id.tabhost);
		TabWidget tabWidget = (TabWidget) tabLayout.findViewById(android.R.id.tabs);
		 
		LazyBaseAdapter adp = new TracklistAdapter(mActivity, new ArrayList<Parcelable>());
		LazyEndlessAdapter adpWrap = new LazyEndlessAdapter(mActivity,adp,getUserTracksUrl(),CloudUtils.Model.track);
		
		final ScTabView tracksView = new ScTabView(mActivity,adpWrap);
		CloudUtils.createTabList(mActivity, tracksView, adpWrap);
		CloudUtils.createTab(mActivity, tabHost,"tracks",mActivity.getString(R.string.tab_tracks),null,tracksView, false);
		adpWrap.notifyDataSetChanged();
		Log.i(TAG,"Making a favorites list " + getFavoritesUrl());
		
		adp = new TracklistAdapter(mActivity, new ArrayList<Parcelable>());
		adpWrap = new LazyEndlessAdapter(mActivity,adp,getFavoritesUrl(),CloudUtils.Model.track);
		
		final ScTabView favoritesView = new ScTabView(mActivity,adpWrap);
		CloudUtils.createTabList(mActivity, favoritesView, adpWrap);
		CloudUtils.createTab(mActivity, tabHost,"favorites",mActivity.getString(R.string.tab_favorites),null,favoritesView, false);
		adpWrap.notifyDataSetChanged();
		
		final ScTabView detailsView = new ScTabView(mActivity);
		detailsView.addView(mDetailsView);
		CloudUtils.createTab(mActivity, tabHost,"details",mActivity.getString(R.string.tab_info),null,detailsView, false);
		CloudUtils.setTabTextStyle(mActivity, tabWidget);
		CloudUtils.configureTabs(mActivity, tabWidget, 30);
		
		
	}
	
	
	
	protected void createDetailsTab(Boolean scrolltabs){
		
		/*Resources res = getResources(); // Resource object to get Drawables
	    TabHost.TabSpec spec;
	    
	    spec = tabHost.newTabSpec((String) "details");
	    spec.setIndicator(new ScTab(this, R.drawable.ic_tab_user,res.getString(R.string.tab_user_details),scrolltabs));
	    
	    viewFlipper.addView(mDetailsView);
	    
        spec.setContent(new TabHost.TabContentFactory(){
            public View createTabContent(String tag)
            {
            	
           	 	return viewFlipper;
            }
        }); 
        tabHost.addTab(spec);
        tabIndices.add(Tabs.TAB_USER_DETAILS);*/
	}
	
	
	
	
	private void toggleFollowing(){
		toggleFollowing(false);
	}
			
	
	private void toggleFollowing(Boolean confirmed){

		mFavorite.setEnabled(false);
		
		 // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread() {
            @Override
			public void run() {
            	try {
            		Log.i("FDA","Is Following? " + _isFollowing);
            		if (_isFollowing)
						mFollowResult = CloudCommunicator.formatContent(mActivity.mCloudComm.deleteContent(CloudCommunicator.PATH_MY_USERS + "/" + mUserData.getData(User.key_id)));
            		else
						mFollowResult = CloudCommunicator.formatContent(mActivity.mCloudComm.putContent(CloudCommunicator.PATH_MY_USERS + "/" + mUserData.getData(User.key_id)));
            		
            		Log.i("FOLLOW","Result is " + mFollowResult);
            		
            		
					} catch (Exception e) {
						e.printStackTrace();
						mActivity.setException(e);
						mActivity.handleException();
					}
					mActivity.mHandler.post(mUpdateFollowing);
            }
        };
        t.start();	
	}
	
	

    // Create runnable for posting since we update the following asynchronously
    final Runnable mUpdateFollowing = new Runnable() {
        public void run() {
        	updateFollowingInUi();
        }
    };
    
    protected void updateFollowingInUi(){
    	Boolean _success = false;
		

			
			if (mFollowResult != null){
				if (mFollowResult.indexOf("200 - OK") != -1 || mFollowResult.indexOf("201 - Created") != -1){
					_success = true;
				} else {
					_success = false;
				}
				
			
				if (_success){
					if (_isFollowing){
						//CloudUtils.removeUserFollowing(mActivity, mUserData.getData(Track.key_id), CloudUtils.getCurrentUserId(mActivity));
						_isFollowing = false;
						//mActivity.showDialog(CloudUtils.Dialogs.DIALOG_UNFOLLOWING);
					} else {
						//CloudUtils.addUserFollowing(mActivity, mUserData.getData(Track.key_id), CloudUtils.getCurrentUserId(mActivity));
						_isFollowing = true;
						//mActivity.showDialog(CloudUtils.Dialogs.DIALOG_FOLLOWING);
					}	
				} else {
					mActivity.showDialog(CloudUtils.Dialogs.DIALOG_ERROR_CHANGE_FOLLOWING_STATUS_ERROR);
				}
			}
			
		
		
		setFollowingButtonText();
		mFavorite.setEnabled(true);
    }

  
	protected void setFollowingButtonText(){
		if (!mIsOtherUser)
			return;
		
		if (_isFollowing){
			mFavorite.setImageResource(R.drawable.ic_unfollow_states);
			//mFavoriteStatus.setText(R.string.favorited);
		} else {
			mFavorite.setImageResource(R.drawable.ic_follow_states);
			//mFavoriteStatus.setText(R.string.not_favorited);
		}
	}
	


	
	
	
	
	
	protected String getDetailsUrl(){
		return mIsOtherUser ? 
					CloudUtils.buildRequestPath(CloudCommunicator.PATH_USER_DETAILS.replace("{user_permalink}",mUserLoadPermalink),null,null).toString() :  
					CloudUtils.buildRequestPath(CloudCommunicator.PATH_MY_DETAILS,null,null).toString();
	}

	protected String getUserTracksUrl() {
		return mIsOtherUser ? 
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_USER_TRACKS.replace("{user_permalink}",mUserLoadPermalink),mActivity.mFilter, mActivity.mTrackOrder).toString() :  
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_MY_TRACKS,mActivity.mFilter,mActivity.mTrackOrder).toString();
	}

	protected String getPlaylistsUrl() {
		return mIsOtherUser ? 
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_USER_PLAYLISTS.replace("{user_permalink}",mUserLoadPermalink),mActivity.mFilter, mActivity.mTrackOrder).toString() :  
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_MY_PLAYLISTS,mActivity.mFilter,mActivity.mTrackOrder).toString();
	}
	
	protected String getFavoritesUrl() {
		return mIsOtherUser ? 
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_USER_FAVORITES.replace("{user_permalink}",mUserLoadPermalink),mActivity.mFilter, mActivity.mTrackOrder).toString() :  
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_MY_FAVORITES,mActivity.mFilter,mActivity.mTrackOrder).toString();
	}
	
	protected String getFollowersUrl() {
		return mIsOtherUser ? 
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_USER_FOLLOWERS.replace("{user_permalink}",mUserLoadPermalink),mActivity.mFilter, mActivity.mUserOrder).toString() :  
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_MY_FOLLOWERS,mActivity.mFilter,mActivity.mTrackOrder).toString();
	}
	
	protected String getFollowingsUrl() {
		return mIsOtherUser ? 
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_USER_FOLLOWINGS.replace("{user_permalink}",mUserLoadPermalink),mActivity.mFilter, mActivity.mUserOrder).toString() :  
				CloudUtils.buildRequestPath(CloudCommunicator.PATH_MY_FOLLOWINGS,mActivity.mFilter,mActivity.mTrackOrder).toString();
	}
	

	public void mapUser(Parcelable p) {
		User mUserInfo = (User) p;
		
		//if (!CloudUtils.stringNullEmptyCheck(CloudUtils.getCurrentUserId(mActivity)))
			//CloudUtils.resolveUser(mActivity, mUserInfo, true, CloudUtils.getCurrentUserId(mActivity));
		
		mUserData = mUserInfo; //save to details object for restoring state
		mFavorite.setEnabled(true);
		
		mUserLoadPermalink = mUserData.getData(User.key_permalink);
		
		if (mUserData.getData(User.key_user_following) != null)
			if (mUserData.getData(User.key_user_following).equalsIgnoreCase("true"))
				_isFollowing = true;
		
		mUser.setText(mUserData.getData(User.key_username));
		mLocation.setText( CloudUtils.getLocationString(mUserData.getData(User.key_city),mUserData.getData(User.key_country)));
		mTracks.setText(mUserData.getData(User.key_track_count));
		mFollowers.setText(mUserData.getData(User.key_followers_count));
		
		
		mIcon.setTemporaryDrawable(getResources().getDrawable(R.drawable.artwork_badge));
		//mIcon.setUpdateLocal(true);
		
		
		
		//check for a local avatar and show it if it exists
		String localAvatarPath = CloudUtils.buildLocalAvatarUrl(mUserData.getData(Track.key_user_permalink));
		File avatarFile = new File(localAvatarPath);
		String remoteUrl = "";
		if (getContext().getResources().getDisplayMetrics().density > 1)
			remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.getData(User.key_avatar_url),GraphicsSizes.large);
		else
			remoteUrl = CloudUtils.formatGraphicsUrl(mUserData.getData(User.key_avatar_url),GraphicsSizes.badge);
		Log.i("DEBUG","Setting avatar url to " + remoteUrl);
		mIcon.setRemoteURI(remoteUrl);
		if (avatarFile.exists()){
			mIcon.setLocalURI(localAvatarPath);	
		} else if (CloudUtils.checkIconShouldLoad(mUserData.getData(User.key_avatar_url))){
			mIcon.setLocalURI(mActivity.getCacheDir() + "/" + CloudUtils.getCacheFileName(remoteUrl));
		}
		
		
		mIcon.loadImage();	
				
		if (mIsOtherUser){
			setFollowingButtonText();
			mFavorite.setVisibility(View.VISIBLE);
		}
		
		Boolean _showTable = false;
		
		if ((mUserData).hasKey(User.key_full_name))
		if (mUserData.getData(User.key_full_name) != ""){
			_showTable = true;
			mFullName.setText(mUserData.getData(User.key_full_name));
		} else {
			mFullName.setVisibility(View.GONE);
		}
				
		CharSequence styledText;
		if ((mUserData).hasKey(User.key_website))
		if (mUserData.getData(User.key_website) != ""){
			_showTable = true;
			styledText = Html.fromHtml("<a href='" + mUserData.getData(User.key_website) + "'>" + CloudUtils.stripProtocol(mUserData.getData(User.key_website)) + "</a>");
			mWebsite.setText(styledText);	
			mWebsite.setMovementMethod(LinkMovementMethod.getInstance());
			((TableRow) mDetailsView.findViewById(R.id.website_row)).setVisibility(View.VISIBLE);
		} else {
			((TableRow) mDetailsView.findViewById(R.id.website_row)).setVisibility(View.GONE);
		}
		
		if ((mUserData).hasKey(User.key_discogs_name))
		if (mUserData.getData(User.key_discogs_name) != ""){
			_showTable = true;
			styledText = Html.fromHtml("<a href='http://www.discogs.com/artist/" + mUserData.getData(User.key_discogs_name) + "'>" + mUserData.getData(User.key_discogs_name) + "</a>");
			mDiscogsName.setText(styledText);	
			mDiscogsName.setMovementMethod(LinkMovementMethod.getInstance());
			((TableRow) mDetailsView.findViewById(R.id.discogs_row)).setVisibility(View.VISIBLE);
		} else {
			((TableRow) mDetailsView.findViewById(R.id.discogs_row)).setVisibility(View.GONE);
		}
		
		if ((mUserData).hasKey(User.key_myspace_name))
		if ((mUserData).getData(User.key_myspace_name) != ""){
			_showTable = true;
			styledText = Html.fromHtml("<a href='http://www.myspace.com/" + (mUserData).getData(User.key_myspace_name) + "'>" + (mUserData).getData(User.key_myspace_name) + "</a>");
			mMyspaceName.setText(styledText);	
			mMyspaceName.setMovementMethod(LinkMovementMethod.getInstance());
			((TableRow) mDetailsView.findViewById(R.id.myspace_row)).setVisibility(View.VISIBLE);
		} else {
			((TableRow) mDetailsView.findViewById(R.id.myspace_row)).setVisibility(View.GONE);
		}
		
		if ((mUserData).hasKey(User.key_description))
		if ((mUserData).getData(User.key_description) != ""){
			_showTable = true;
			styledText = Html.fromHtml((mUserData).getData(User.key_description)); 
			mDescription.setText(styledText);
			mDescription.setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		/*if (_showTable){
			if (mUserTable.getParent() != mUserTableHolder)
				mUserTableHolder.addView(mUserTable);
		}*/

	}
	
	
	protected Object[] saveLoadTasks(){
		Object[] ret = {mLoadDetailsTask};
		return ret;
	}

	protected void restoreLoadTasks(Object[] taskObject){
		mLoadDetailsTask = (LoadTask) taskObject[0];
	}
	
	protected Parcelable saveParcelable(){
		return mUserData;
	}
	
	protected void restoreParcelable(Parcelable p){
		if (p != null) mapUser(p);
	}
	
}
