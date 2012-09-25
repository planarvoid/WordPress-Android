package com.soundcloud.android.view.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import java.util.Date;

public abstract class ActivityRow extends LazyRow {
    protected Activity mActivity;

    private final TextView mUser;
    private final TextView mTitle;
    private final TextView mCreatedAt;

    private Drawable mDrawable, mPressedDrawable;
    private SpannableStringBuilder mSpanBuilder;

    public ActivityRow(Context context, IScAdapter adapter) {
        super(context, adapter);

        mTitle = (TextView) findViewById(R.id.title);
        mUser = (TextView) findViewById(R.id.user);
        mCreatedAt = (TextView) findViewById(R.id.created_at);

        init();
    }

    protected void init() {
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
    protected Track getTrack() {
        return mActivity.getTrack();
    }

    protected Comment getComment() {
        return (mActivity.origin instanceof Comment) ? (Comment) mActivity.origin : null;
    }

    protected User getOriginUser() {
        return (mActivity == null || mActivity.getUser() == null) ? null : mActivity.getUser();
    }

    protected Date getOriginCreatedAt() {
        return mActivity.created_at;
    }

    private SpannableStringBuilder createSpan() {
        mSpanBuilder = new SpannableStringBuilder();
        mSpanBuilder.append("  ").append(getTrack().title);
        addSpan(mSpanBuilder);
        return mSpanBuilder;
    }

    protected void addSpan(SpannableStringBuilder builder) {
        mSpanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 1, mSpanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public String getIconRemoteUri() {
        if (mActivity == null || mActivity.getUser() == null || mActivity.getUser().avatar_url == null) return "";
        return Consts.GraphicSize.formatUriForList(getContext(), mActivity.getUser().avatar_url);
    }

    @Override
    protected View addContent() {
        return View.inflate(getContext(), R.layout.activity_list_row, this);
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.MODEL_MANAGER.getTrackFromCursor(cursor));
    }

    protected abstract  boolean fillParcelable(Parcelable p);

    @Override
    public void display(int position, Parcelable p) {
        mActivity = (Activity) p;
        boolean isNull = !fillParcelable(p);
        super.display(position);
        if (isNull) return;

        mActivity = (Activity) p;
        mSpanBuilder = createSpan();

        setImageSpan();

        mUser.setText(getOriginUser().username);
        mCreatedAt.setText(ScTextUtils.getTimeElapsed(getContext().getResources(), getOriginCreatedAt().getTime()));

    }

    private void setImageSpan() {
        if (mSpanBuilder == null) return;
        mSpanBuilder.setSpan(new ImageSpan(isPressed() ? getPressedDrawable() :
                getDrawable(), ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mTitle.setText(mSpanBuilder);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        setImageSpan();
    }

    protected abstract Drawable doGetDrawable(boolean pressed);


    private Drawable getDrawable() {
        if (mDrawable == null) {
            mDrawable = doGetDrawable(false);
        }
        return mDrawable;
    }

    private Drawable getPressedDrawable() {
        if (mPressedDrawable == null) {
            mPressedDrawable = doGetDrawable(true);
        }
        return mPressedDrawable;
    }
}
