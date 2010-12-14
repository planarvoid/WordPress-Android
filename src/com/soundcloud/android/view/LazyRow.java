package com.soundcloud.android.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;

public class LazyRow extends RelativeLayout {
	
	protected Context mContext;	

	  public LazyRow(Context _context) {
		  super(_context);
		  mContext = _context;
		  
		  LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		  inflater.inflate(getRowResourceId(), this);
		  
		  //if (findViewById(R.id.on_selected) != null)
			//	AnimUtils.setLayoutAnim_slidedownfromtop((ViewGroup) findViewById(R.id.on_selected), mContext);
	  }
	  
	  protected int getRowResourceId(){
		  return R.layout.track_list_item;
	  }
	 

	  /** update the views with the data corresponding to selection index */
	  public void display(Parcelable p, boolean selected) {
		 
		/*  if (selected && findViewById(R.id.on_selected) != null){
			  if (findViewById(R.id.on_selected).getVisibility() != View.VISIBLE){
				  findViewById(R.id.on_selected).setVisibility(View.VISIBLE);
				  findViewById(R.id.on_selected).forceLayout();
			  }
		  } else if (findViewById(R.id.on_selected) != null)
			findViewById(R.id.on_selected).setVisibility(View.GONE);*/
		  
		  //if (findViewById(R.id.on_selected) != null)
			//  findViewById(R.id.on_selected).forceLayout();
		  
	  }
	  
	  public void setViewImage(final ImageView image, final String value) {
		  
		  SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			if ( !(image instanceof RemoteImageView) || preferences.getBoolean("hideArtwork", false))
				return;
				
			RemoteImageView riv = (RemoteImageView) image;
			
			riv.setImageSize(getIconWidth(), getIconHeight());
			riv.setTemporaryDrawable(getTemporaryDrawable());
				
			if (CloudUtils.checkIconShouldLoad(value)) {
				if (value.startsWith("/")){
					riv.setLocalURI(value);
					riv.setRemoteURI(null);
				} else {
					riv.setLocalURI(mContext.getCacheDir().toString() + "/" + CloudUtils.getCacheFileName(value));
					riv.setRemoteURI(value);
				}
			} else {
				riv.setLocalURI(null);
				riv.setRemoteURI(null);
			}
			
		}
	  
	  
	  protected int getIconWidth(){
		  return CloudUtils.GRAPHIC_DIMENSIONS_BADGE;
	  }
	  
	  protected int getIconHeight(){
		  return CloudUtils.GRAPHIC_DIMENSIONS_BADGE;
	  }
	  
	  protected Drawable getTemporaryDrawable(){
		  return mContext.getResources().getDrawable(R.drawable.artwork_badge);
	  }
	
}
