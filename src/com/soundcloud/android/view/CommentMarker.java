package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.ContextMenu;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.objects.Comment;

public class CommentMarker extends RelativeLayout implements ContextMenu.ContextMenuInfo {

	private Context _context;
	private ImageView mAvatar;
	private Paint mPaint;
	
	private int mLeftMargin;
	
	private int mDuration;
	
	private String mTrackId;
	private String mUserId;
	private String mUsername;
	private String mBody;
	private int mTimestamp;
	
	public interface OnCommentClicked {
		public void onClick(int timestamp);
	}
	
	private Comment mCommentData;
	
	private ContextMenuInfo mContextMenuInfo;
	
	public CommentMarker(Context context) {
		super(context);

		_context = context;
		this.setBackgroundResource(R.drawable.temp_comment_marker_bg);
		
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.FILL_PARENT);
		this.setLayoutParams(lp);
		
		lp = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		
		int targetDimension = (int) (CloudUtils.GRAPHIC_DIMENSIONS_SMALL * getContext().getResources().getDisplayMetrics().density);
		 
		mAvatar = new ImageView(_context, null);
		mAvatar.setImageDrawable(_context.getResources().getDrawable(R.drawable.artwork_badge));
		mAvatar.setScaleType(ScaleType.CENTER_INSIDE);
		mAvatar.setLayoutParams(lp);
		mAvatar.getLayoutParams().width = targetDimension;
		mAvatar.getLayoutParams().height = targetDimension;
		addView(mAvatar);
	}
	
	public void flashOn(){
		this.setBackgroundColor(getResources().getColor(R.color.cloudProgressCommentBarBgOn));
		this.invalidate();
	}
	
	public void flashOff(){
		this.setBackgroundColor(getResources().getColor(R.color.cloudProgressCommentBarBgOff));
		this.invalidate();
	}

	public void setLeftMargin(int leftMargin){
		mLeftMargin = leftMargin;
		RelativeLayout.LayoutParams lp = (LayoutParams) this.getLayoutParams();
		lp.leftMargin = mLeftMargin - 16;
		this.setLayoutParams(lp);
	}
	
	public void setCommentData(Comment comment){
		mCommentData = comment;

		/*if (mCommentData.hasKey(Comment.key_track_id)) mTrackId = mCommentData.getData(Comment.key_track_id);
		if (mCommentData.hasKey(Comment.key_user_id)) mUserId = mCommentData.getData(Comment.key_user_id);
		if (mCommentData.hasKey(Comment.key_username)) mUsername = mCommentData.getData(Comment.key_username);
		if (mCommentData.hasKey(Comment.key_body)) mBody = mCommentData.getData(Comment.key_body);
		if (mCommentData.hasKey(Comment.key_timestamp)) mTimestamp = Integer.parseInt(mCommentData.getData(Comment.key_timestamp));
		if (mCommentData.hasKey(Comment.key_user_avatar_url)){
			String avatarUrl = CloudUtils.formatGraphicsUrl(mCommentData.getData(Comment.key_user_avatar_url),GraphicsSizes.badge);
			if (CloudUtils.checkIconShouldLoad(avatarUrl)){
				ImageLoader.get(_context).bind(mAvatar, avatarUrl, null);
			}
		}*/
		
	}
	
	public void setTimestamp(int timestamp){
		mTimestamp = timestamp;
	}
	
	public String getCommentId(){
		//return mCommentData.getData(Comment.key_id);
		return "";
	}
	
	public int getTimestamp(){
		return mTimestamp;
		
	}
	
	public int getLeftMargin(){
		return mLeftMargin;
		
	}
	
	
	
    /**
     * Hit rectangle in parent's coordinates
     *
     * @param outRect The hit rectangle of the view.
     */
    @Override
	public void getHitRect(Rect outRect) {
    	Float tx = ((WaveformHolder) (this.getParent())).getTransformX();
        outRect.set((int) (this.getLeft()*tx), this.getTop(), (int) (this.getRight()*tx), this.getBottom());
    }
	
	
	
	@Override
	public boolean showContextMenu() {
		mContextMenuInfo = new CommentContextMenuInfo(mCommentData);
		performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
		return super.showContextMenu();
	}
	
	
	@Override
	protected ContextMenuInfo getContextMenuInfo() {
		return mContextMenuInfo;
	}
	

	/**
     * Extra menu information provided to the
     * {@link android.view.View.OnCreateContextMenuListener#onCreateContextMenu(ContextMenu, View, ContextMenuInfo) }
     * callback when a context menu is brought up for this AdapterView.
     *
     */
    public static class CommentContextMenuInfo implements ContextMenu.ContextMenuInfo {

        public CommentContextMenuInfo(Comment mCommentData) {
        	this.comment = mCommentData;
        }

        /**
         * The comment data associated with this item
         */
        public Comment comment;

      
    }


	public void onTouchListener(WaveformHolder mWaveformHolder) {
		// TODO Auto-generated method stub
		
	}
	
	

}
