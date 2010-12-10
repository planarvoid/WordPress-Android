package com.soundcloud.android;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.objects.Track;
import com.soundcloud.utils.RemoteImageView;

public class TracklistRow extends LazyRow {
	
    private static final String TAG = "TracklistRow";
	
	protected Track mTrack;
	
	protected ImageView mPlayIndicator;
	protected RemoteImageView mIcon;
	protected TextView mUser;
	protected TextView mTitle;
	protected TextView mDuration;
	
	protected ImageButton mPlayBtn;
	protected ImageButton mPlaylistBtn;
	protected ImageButton mFavoriteBtn;
	protected ImageButton mDownloadBtn;
	protected ImageButton mDetailsBtn;
	
	protected Boolean _isPlaying = false;
	protected Boolean _isFavorite = false;
	
	private String _iconURL;
	private String _playURL;
	
	private int mCurrentIndex = -1;
	
	
	

	  public TracklistRow(Context _context) {
		  super(_context);
		  
		 

		  mTitle = (TextView) findViewById(R.id.track);
		  mUser = (TextView) findViewById(R.id.user);
		  mDuration = (TextView) findViewById(R.id.duration);
		  mIcon = (RemoteImageView) findViewById(R.id.icon);
		  mPlayIndicator = (ImageView) findViewById(R.id.play_indicator);
		  
		 /* mPlayBtn = (ImageButton) findViewById(R.id.btn_play);
		  mPlaylistBtn = (ImageButton) findViewById(R.id.btn_playlist);
		  mFavoriteBtn = (ImageButton) findViewById(R.id.btn_favorite);
		  mDownloadBtn = (ImageButton) findViewById(R.id.btn_download);
		  mDetailsBtn = (ImageButton) findViewById(R.id.btn_details);
		  
		  
		  mPlayBtn.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {
	            	playClicked();
	            	Log.i("PlayButton","Play button done");
	        }
	      });
		  
		  mPlaylistBtn.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {
	            	((TrackList) mContext).enqueue(mTrack);
	        }
	      });
		  
		  mFavoriteBtn.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {
	            	toggleFavorite();
	        }
	      });
		  
		  mDownloadBtn.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {
	            	((TrackList) mContext).downloadTrack(mTrack);
	        }
	      });
		  
		  mDetailsBtn.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {
	            	CloudUtils.gotoTrackDetails(((TrackList) mContext), (Track) mTrack);
	        }
	      });*/
		  
	  }
	  
	  @Override
	  protected int getRowResourceId(){
		  return R.layout.track_list_item;
	  }
	 
	 
	  public void display(Parcelable p, boolean selected, boolean isPlaying) {
		  _isPlaying = isPlaying;
		  display(p,selected);
	  }
	  

	  /** update the views with the data corresponding to selection index */
	  @Override
	  public void display(Parcelable p, boolean selected) {
		  	super.display(p, selected);
		  	
		  

		  	
		  	Log.i(TAG,"SIZE " + mPlayIndicator.getWidth() + " " + mPlayIndicator.getHeight());
		  
			mTrack = getTrackFromParcelable(p);
		 	Log.i("DEBUG","Displaying track list row " + mTrack.getData(Track.key_title));	
			mTitle.setText(mTrack.getData(Track.key_title));
			mUser.setText(mTrack.getData(Track.key_username));
			//mDuration.setText(mTrack.getData(Track.key_duration_formatted));
			
			
			if (mTrack.getData(Track.key_user_favorite).equalsIgnoreCase("true")){
				_isFavorite = true;
			} else {
				_isFavorite = false;
			}
			
			Log.i(TAG,"Setting status " + mTrack.getData(Track.key_title) + ":" + _isPlaying +"|"+ mTrack.getData(Track.key_user_favorite) + "|"+mTrack.getData(Track.key_user_played));
			
			if (_isPlaying){
				mPlayIndicator.setImageDrawable(mContext.getResources().getDrawable(R.drawable.list_playing));
				mPlayIndicator.setVisibility(View.VISIBLE);
			} else if (_isFavorite){
				mPlayIndicator.setImageDrawable(mContext.getResources().getDrawable(R.drawable.list_favorite));
				mPlayIndicator.setVisibility(View.VISIBLE);
			} else if (!mTrack.getData(Track.key_user_played).contentEquals("true")){
				mPlayIndicator.setImageDrawable(mContext.getResources().getDrawable(R.drawable.list_unlistened));
				mPlayIndicator.setVisibility(View.VISIBLE);
			} else {
				mPlayIndicator.setVisibility(View.GONE);
			}
			
			
			setRowIcon();
			
			/*if (_isPlaying){
				mPlayIndicator.setImageResource(R.drawable.ic_play_indicator);
				mPlayIndicator.setVisibility(View.VISIBLE);
			} else
				mPlayIndicator.setVisibility(View.GONE);
			
			if (mTrack.getData(Track.key_play_url)== ""){
				mPlayBtn.setVisibility(View.GONE);
				mPlaylistBtn.setVisibility(View.GONE);
			} else {
				mPlayBtn.setVisibility(View.VISIBLE);
				mPlaylistBtn.setVisibility(View.VISIBLE);
			}
			
			
			if (mTrack.getData(Track.key_downloadable).contentEquals("false")){
				mDownloadBtn.setVisibility(View.GONE);
			} else {
				mDownloadBtn.setVisibility(View.VISIBLE);
			}*/
			
			setFavoriteStatus();
		  
	  }
	  
	  protected Track getTrackFromParcelable(Parcelable p){
		  return (Track) p;
	  }
	  
	  protected void setRowIcon() {
		  
		  
		  String remoteUrl = "";
			if (getContext().getResources().getDisplayMetrics().density > 1)
				remoteUrl = CloudUtils.formatGraphicsUrl(mTrack.getData(Track.key_artwork_url),GraphicsSizes.large); 
			else
				remoteUrl = CloudUtils.formatGraphicsUrl(mTrack.getData(Track.key_artwork_url),GraphicsSizes.badge);
			
			Log.i("asdf","set row icon " + remoteUrl);
			
			super.setViewImage(mIcon,remoteUrl);
			
			mIcon.loadImage();
	  }
	  
	  private void setFavoriteStatus(){
			if (_isFavorite){
				//mFavoriteBtn.setImageResource(R.drawable.ic_track_unfavorite_states);
			} else {
				//mFavoriteBtn.setImageResource(R.drawable.ic_track_favorite_states);
			}
	  }
	  
	
	  protected void sendTrack() {
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
		emailIntent .setType("plain/text");
		emailIntent .putExtra(android.content.Intent.EXTRA_SUBJECT, mTrack.getData(Track.key_title) + " [" + mTrack.getData(Track.key_username) + "]");
		emailIntent .putExtra(android.content.Intent.EXTRA_TEXT, "\n\n" + mTrack.getData(Track.key_permalink_url));
		mContext.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}

	  @Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
	       
	        super.onLayout(changed, l, t, r, b);
	        Log.i(TAG,"SIZE " + mPlayIndicator.getWidth() + " " + mPlayIndicator.getHeight());
	  }
		

		
		private void toggleFavorite(){
			/*if (_isFavorite){
				((TrackList) mContext).removeFavorite(mTrack, this);
			} else {
				((TrackList) mContext).addFavorite(mTrack);
				_isFavorite = !_isFavorite;
				setFavoriteStatus();
			}*/
			

		}
		
		public void setFavoriteStatus(Boolean favorited){
			_isFavorite = favorited;
			setFavoriteStatus();
		}
		
	
	  
	
}
