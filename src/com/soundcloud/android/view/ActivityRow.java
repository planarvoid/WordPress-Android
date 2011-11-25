package com.soundcloud.android.view;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
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

import java.util.Date;

public class ActivityRow extends LazyRow {
    private Event mEvent;
    private final TextView mUser;
    private final TextView mTitle;
    private final TextView mCreatedAt;
    private Drawable mFavoritedDrawable;
    private Drawable mCommentedDrawable;
    private Drawable mFavoritedPressedDrawable;
    private Drawable mCommentedPressedDrawable;
    protected SpannableStringBuilder mSpanBuilder;

    protected enum ActivityType {
        Comment,Favorite
    }

    public ActivityRow(Context activity, LazyBaseAdapter adapter) {
        super(activity, adapter);

        mTitle = (TextView) findViewById(R.id.title);
        mUser = (TextView) findViewById(R.id.user);
        mCreatedAt = (TextView) findViewById(R.id.created_at);

        init();
    }

    protected void init(){
        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final User u = getOriginUser();
                if (u == null) return;

                Intent intent = new Intent(getContext(), UserBrowser.class);
                intent.putExtra("user", u);
                getContext().startActivity(intent);
            }
        });
        mIcon.setFocusable(false);
    }

    // override these for non-dashboard activities to account for different parcelable structures

    protected boolean fillParcelable(Parcelable p){
        mEvent = (Event) p;
        return mEvent != null;
    }

    protected ActivityType getType(){
        if (mEvent.origin instanceof Comment){
            return ActivityType.Comment;
        } else return ActivityType.Favorite;
    }

    protected Track getTrack(){
        return mEvent.getTrack();
    }

    protected Comment getComment(){
        return (mEvent.origin instanceof Comment) ? (Comment) mEvent.origin : null;
    }

    protected User getOriginUser(){
        return (mEvent == null || mEvent.getUser() == null) ? null : mEvent.getUser();
    }

    protected Date getOriginCreatedAt(){
        return mEvent.created_at;
    }

    protected SpannableStringBuilder createSpan() {
        mSpanBuilder = new SpannableStringBuilder();
        mSpanBuilder.append("  ").append(getTrack().title);

        if (getType() == ActivityType.Comment) {
            mSpanBuilder.append(": ");
            mSpanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 1, mSpanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpanBuilder.append("\"").append(getComment().body).append("\"");
        } else {
            mSpanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 1, mSpanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return mSpanBuilder;
    }

    @Override
    public String getIconRemoteUri() {
        if (mEvent == null || mEvent.getUser() == null || mEvent.getUser().avatar_url == null) return "";
        return ImageUtils.formatGraphicsUriForList(getContext(), mEvent.getUser().avatar_url);
    }

    // end override


    @Override
    protected int getRowResourceId() {
        return R.layout.activity_list_row;
    }

     @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), new Track(cursor));
    }
    @Override
    public void display(int position, Parcelable p) {
        boolean isNull = !fillParcelable(p);

        super.display(position);

        if (isNull) return;

        mSpanBuilder = createSpan();

        setImageSpan();
        
        mUser.setText(getOriginUser().username);
        mCreatedAt.setText(CloudUtils.getTimeElapsed(getContext().getResources(), getOriginCreatedAt().getTime()));

    }



    private void setImageSpan(){
        if (mSpanBuilder == null) return;
        if (getType() == ActivityType.Comment){
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
