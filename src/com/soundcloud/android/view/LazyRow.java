package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;

public class LazyRow extends RelativeLayout {
	private static final String TAG = "LazyRow";
	protected Context mContext;	
	protected ImageView mIcon;

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
	  
	  public ImageView getRowIcon(){
			return null;
	  }
	  
	  public String getIconRemoteUri(){
		  	return "";
	  }
	  
	  public void setTemporaryDrawable(BindResult result){
		  if (mIcon == null)
			  return;
		  
		  if (result != BindResult.OK)
			  mIcon.setImageDrawable(this.getTemporaryDrawable());
		  
		  mIcon.getLayoutParams().width = (int) (getContext().getResources().getDisplayMetrics().density*getIconWidth());
		  mIcon.getLayoutParams().height = (int) (getContext().getResources().getDisplayMetrics().density*getIconHeight());
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
