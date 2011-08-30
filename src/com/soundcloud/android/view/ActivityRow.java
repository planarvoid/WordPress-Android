package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityRow extends LazyRow {
    private Event mEvent;
    private final TextView mUser;
    private final TextView mTitle;
    private final TextView mCreatedAt;
    private Drawable mFavoritedDrawable;
    private Drawable mCommentedDrawable;
    private Drawable mFavoritedPressedDrawable;
    private Drawable mCommentedPressedDrawable;
    private SpannableStringBuilder mSpanBuilder;

    public ActivityRow(ScActivity activity, LazyBaseAdapter adapter) {
        super(activity, adapter);

        mTitle = (TextView) findViewById(R.id.title);
        mUser = (TextView) findViewById(R.id.user);
        mCreatedAt = (TextView) findViewById(R.id.created_at);

        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEvent == null || mEvent.getUser() == null) return;
                Intent intent = new Intent(mActivity, UserBrowser.class);
                intent.putExtra("user", mEvent.getUser());
                mActivity.startActivity(intent);
            }
        });
        mIcon.setFocusable(false);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.activity_list_row;
    }

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        mEvent = (Event) mAdapter.getItem(position);

        super.display(position);

        if (mEvent == null)
            return;

        mSpanBuilder = new SpannableStringBuilder();
        mSpanBuilder.append("  ");
        mSpanBuilder.append(mEvent.getTrack().title);

        if (mEvent.type.contentEquals(Event.Types.COMMENT)){
            mSpanBuilder.append(": ");
            mSpanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 1, mSpanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpanBuilder.append("\"").append(mEvent.comment.body).append("\"");
        } else {
            mSpanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 1, mSpanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        setImageSpan();
        
        mUser.setText(mEvent.getUser().username);
        mCreatedAt.setText(CloudUtils.getTimeElapsed(mActivity.getResources(), mEvent.created_at.getTime()));

    }
    
    private void setImageSpan(){
        if (mSpanBuilder == null) return;
        if (mEvent.type.contentEquals(Event.Types.COMMENT)){
             mSpanBuilder.setSpan(new ImageSpan(isPressed() ?
                       getmCommentedPressedDrawable() :
                     getmCommentedDrawable(),ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
             mSpanBuilder.setSpan(new ImageSpan(isPressed() ?
                       getFavoritedPressedDrawable() :
                     getFavoritedDrawable(),ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mTitle.setText(mSpanBuilder);
    }

    @Override
    public ImageView getRowIcon() {
        return mIcon;
    }

    @Override
    public String getIconRemoteUri() {
        if (mEvent == null || mEvent.getUser() == null || mEvent.getUser().avatar_url == null)
            return "";

        return ImageUtils.formatGraphicsUriForList(mActivity, mEvent.getUser().avatar_url);
    }

    @Override
    public void setPressed(boolean pressed){
        super.setPressed(pressed);
       setImageSpan();
    }

    private Drawable getFavoritedDrawable(){
             if (mFavoritedDrawable == null) {
                 mFavoritedDrawable = getResources().getDrawable(R.drawable.stats_favorited);
                 mFavoritedDrawable.setBounds(0, 0, mFavoritedDrawable.getIntrinsicWidth(), mFavoritedDrawable.getIntrinsicHeight());
             }
           return mFavoritedDrawable;
       }

       private Drawable getmCommentedDrawable(){
             if (mCommentedDrawable == null) {
                 mCommentedDrawable = getResources().getDrawable(R.drawable.stats_commented);
                 mCommentedDrawable.setBounds(0, 0, mCommentedDrawable.getIntrinsicWidth(), mCommentedDrawable.getIntrinsicHeight());
             }
           return mCommentedDrawable;
       }

       private Drawable getFavoritedPressedDrawable(){
             if (mFavoritedPressedDrawable == null) {
                 mFavoritedPressedDrawable = getResources().getDrawable(R.drawable.stats_favorites_white_50);
                 mFavoritedPressedDrawable.setBounds(0, 0, mFavoritedPressedDrawable.getIntrinsicWidth(), mFavoritedPressedDrawable.getIntrinsicHeight());
             }
           return mFavoritedPressedDrawable;
       }

       private Drawable getmCommentedPressedDrawable(){
             if (mCommentedPressedDrawable == null) {
                 mCommentedPressedDrawable = getResources().getDrawable(R.drawable.stats_comments_white_50);
                 mCommentedPressedDrawable.setBounds(0, 0, mCommentedPressedDrawable.getIntrinsicWidth(), mCommentedPressedDrawable.getIntrinsicHeight());
             }
           return mCommentedPressedDrawable;
       }

}
