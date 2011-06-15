
package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Paint;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Comment;

public class MakeCommentBubble extends RelativeLayout {

    private Context mContext;

    private ImageView mAvatar;

    private Paint mPaint;

    private int mLeftMargin;

    private int mDuration;

    private String mTrackId;

    private String mUserId;

    private String mUsername;

    private String mBody;

    private int mTimestamp;

    private Comment mCommentData;

    private ContextMenuInfo mContextMenuInfo;

    public MakeCommentBubble(Context context) {
        
        super(context);

        mContext = context;
        this.setBackgroundResource(R.drawable.comment_bubble);
    }

    public void flashOn() {
        this.setBackgroundColor(getResources().getColor(R.color.cloudProgressCommentBarBgOn));
        this.invalidate();
    }

    public void flashOff() {
        this.setBackgroundColor(getResources().getColor(R.color.cloudProgressCommentBarBgOff));
        this.invalidate();
    }

    public void setLeftMargin(int leftMargin) {
        mLeftMargin = leftMargin;
        RelativeLayout.LayoutParams lp = (LayoutParams) this.getLayoutParams();
        lp.leftMargin = mLeftMargin - 16;
        this.setLayoutParams(lp);
    }

    public void setCommentData(Comment comment) {
        mCommentData = comment;

        /*
         * if (mCommentData.hasKey(Comment.key_track_id)) mTrackId =
         * mCommentData.getData(Comment.key_track_id); if
         * (mCommentData.hasKey(Comment.key_user_id)) mUserId =
         * mCommentData.getData(Comment.key_user_id); if
         * (mCommentData.hasKey(Comment.key_username)) mUsername =
         * mCommentData.getData(Comment.key_username); if
         * (mCommentData.hasKey(Comment.key_body)) mBody =
         * mCommentData.getData(Comment.key_body); if
         * (mCommentData.hasKey(Comment.key_timestamp)) mTimestamp =
         * Integer.parseInt(mCommentData.getData(Comment.key_timestamp)); if
         * (mCommentData.hasKey(Comment.key_user_avatar_url)){ String avatarUrl
         * =CloudUtils.formatGraphicsUrl(mCommentData.getData(Comment.
         * key_user_avatar_url),GraphicsSizes.badge); if
         * (CloudUtils.checkIconShouldLoad(avatarUrl)){
         * ImageLoader.get(_context).bind(mAvatar, avatarUrl, null); } }
         */

    }

    public void setTimestamp(int timestamp) {
        mTimestamp = timestamp;
    }

    public String getCommentId() {
        // return mCommentData.getData(Comment.key_id);
        return "";
    }

    public int getTimestamp() {
        return mTimestamp;

    }

    public int getLeftMargin() {
        return mLeftMargin;

    }

}
