package com.soundcloud.android.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyRow;

public class CommentsAdapter extends LazyExpandableBaseAdapter implements Filterable {
	
	
	@SuppressWarnings("unchecked")
	public CommentsAdapter(LazyActivity context, ArrayList<Parcelable> groupData,ArrayList<ArrayList<Parcelable>> childData) {
		super(context,groupData,childData);
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View row, ViewGroup parent) {
		CommentRowChild rowView = null;
		
		if (row == null) {
			rowView = new CommentRowChild(mActivity);
		} else {
			rowView = (CommentRowChild) row;
		}
		
		// update the cell renderer, and handle selection state
		rowView.display(mChildData.get(groupPosition).get(childPosition), (mSelectedGroupIndex == groupPosition && mSelectedChildIndex == childPosition));
		
		return rowView;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View row, ViewGroup parent) {
		CommentRowGroup rowView = null;
		
		if (row == null) {
			rowView = new CommentRowGroup(mActivity);
		} else {
			rowView = (CommentRowGroup) row;
		}
		
		// update the cell renderer, and handle selection state
		rowView.display(mGroupData.get(groupPosition), false);
		
		return rowView;
	}

	
	
	
	
	public class CommentRow extends LazyRow {
		
		protected Comment mComment;
		private TextView mUsername;
		private TextView mBody;
		
		public CommentRow(Context _context) {
			  super(_context);
			  
			  mUsername = (TextView) findViewById(R.id.username);
			  mBody = (TextView) findViewById(R.id.body);
			  
		  }
		  
		  @Override
		  protected int getRowResourceId(){
			  return R.layout.user_list_item;
		  }
		 
		 

		  /** update the views with the data corresponding to selection index */
		  @Override
		  public void display(Parcelable p, boolean selected) {
			  	super.display(p, selected);
			  
				mComment = (Comment) p;
				mUsername.setText(getUsernameString());
				//mBody.setText(mComment.getData(Comment.key_body));
		  }
		  
		  @Override
		  protected Drawable getTemporaryDrawable(){
			  return mContext.getResources().getDrawable(R.drawable.artwork_badge);
		  }
		  
		  @Override
		  public ImageView getRowIcon(){
				if (getContext().getResources().getDisplayMetrics().density > 1){
					mIcon.getLayoutParams().width = 67;
					mIcon.getLayoutParams().height = 67;
				}
				return mIcon;
		  }
		  
		  @Override
		  public String getIconRemoteUri(){
				//if (getContext().getResources().getDisplayMetrics().density > 1){
					//return CloudUtils.formatGraphicsUrl(mComment.getData(Comment.key_user_avatar_url),GraphicsSizes.large); 
				//} else
				//	return CloudUtils.formatGraphicsUrl(mComment.getData(Comment.key_user_avatar_url),GraphicsSizes.badge);
				return "";
		  }
		  
		  
		  @Override
		  protected int getIconWidth(){
			  return CloudUtils.GRAPHIC_DIMENSIONS_SMALL;
		  }
		  
		  @Override
		  protected int getIconHeight(){
			  return CloudUtils.GRAPHIC_DIMENSIONS_SMALL;
		  }
		  
		  
		protected String getUsernameString(){
		 // return mComment.getData(User.key_username) + " @ " + mComment.getData(Comment.key_timestamp_formatted);
			return "";
		}

	}
	
	
	
	public class CommentRowChild extends CommentRow {
		
		public CommentRowChild(Context _context) {
		  super(_context);
		}
	  
		@Override
		protected int getRowResourceId(){
		  return R.layout.comment_list_item;
		}
		
		@Override
		protected String getUsernameString(){
		  //return mComment.getData(User.key_username);
			return "";
		}
	  
	}
	
	public class CommentRowGroup extends CommentRow {
		
		public CommentRowGroup(Context _context) {
		  super(_context);
		}
	  
		@Override
		protected int getRowResourceId(){
		  return R.layout.comment_list_item_group;
		}
	  
	}
	
	
	
	
}
