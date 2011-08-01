package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class NewsRow extends LazyRow {
    protected Event mEvent;

    protected TextView mUser;
    protected TextView mTitle;
    protected TextView mCreatedAt;

    protected ImageView mCloseIcon;

    private Drawable mFavoritedDrawable;
    private Drawable mCommentedDrawable;


    public NewsRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

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
        return R.layout.news_list_row;
    }

    @Override
    protected Drawable getIconBgResourceId() {
        return getResources().getDrawable(R.drawable.artwork_badge);
    }

    protected long getTrackTime(Parcelable p) {
        return getTrackFromParcelable(p).created_at.getTime();
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

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        mEvent = (Event) mAdapter.getItem(position);

        super.display(position);

        if (mEvent == null)
            return;

        SpannableStringBuilder builder = new SpannableStringBuilder();
        ImageSpan typeImage = new ImageSpan(mEvent.type.contentEquals(Event.Types.COMMENT)
                ? getmCommentedDrawable() : getFavoritedDrawable(),ImageSpan.ALIGN_BASELINE);

        builder.append("  ");
        builder.setSpan(typeImage, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(mEvent.getTrack().title);

        if (mEvent.type.contentEquals(Event.Types.COMMENT)){
            builder.append(": ");
            builder.setSpan(new StyleSpan(Typeface.BOLD), 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append("\""+mEvent.comment.body+"\"");
        } else {
            builder.setSpan(new StyleSpan(Typeface.BOLD), 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }


        mTitle.setText(builder);
        mUser.setText(mEvent.getUser().username);
        mCreatedAt.setText(CloudUtils.getTimeElapsed(mActivity.getResources(), mEvent.created_at.getTime()));

    }

    protected Track getTrackFromParcelable(Parcelable p) {
        return (Track) p;
    }

    @Override
    public ImageView getRowIcon() {
        return mIcon;
    }

    @Override
    public String getIconRemoteUri() {
        if (mEvent == null || mEvent.getUser() == null || mEvent.getUser().avatar_url == null)
            return "";

        return ImageUtils.formatGraphicsUrlForList(mActivity, mEvent.getUser().avatar_url);

    }


}
