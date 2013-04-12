package com.soundcloud.android.view.adapter;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.DrawableSpan;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.util.Date;

public abstract class ActivityRow extends IconLayout implements ListRow {
    protected Activity mActivity;

    protected final TextView mUser;
    private final TextView mTitle;
    private final TextView mCreatedAt;

    private Drawable mDrawable, mPressedDrawable;
    protected SpannableStringBuilder mSpanBuilder;

    public ActivityRow(Context context) {
        super(context);

        mTitle = (TextView) findViewById(R.id.title);
        mUser = (TextView) findViewById(R.id.user);
        mCreatedAt = (TextView) findViewById(R.id.created_at);
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
    protected Playable getPlayable() {
        return mActivity.getPlayable();
    }

    protected User getOriginUser() {
        return (mActivity == null || mActivity.getUser() == null) ? null : mActivity.getUser();
    }

    protected Date getOriginCreatedAt() {
        return mActivity.created_at;
    }

    protected SpannableStringBuilder createSpan() {
        mSpanBuilder = new SpannableStringBuilder();
        mSpanBuilder.append("  ").append(getPlayable().title);
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
    protected View addContent(AttributeSet attributeSet) {
        return View.inflate(getContext(), R.layout.activity_list_row, this);
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor));
    }

    protected abstract  boolean fillParcelable(Parcelable p);

    @Override
    public void display(int position, Parcelable p) {
        mActivity = (Activity) p;
        if (!fillParcelable(p)) return;

        loadIcon();

        mActivity = (Activity) p;
        mSpanBuilder = createSpan();

        setImageSpan();
        mCreatedAt.setText(ScTextUtils.getTimeElapsed(getContext().getResources(), getOriginCreatedAt().getTime()));

        mUser.setText(getOriginUser().username);
        mCreatedAt.setText(ScTextUtils.getTimeElapsed(getContext().getResources(), getOriginCreatedAt().getTime()));

    }

    private void setImageSpan() {
        if (mSpanBuilder == null) return;
        mSpanBuilder.setSpan(new DrawableSpan(isPressed() ? getPressedDrawable() :
                getDrawable(), DrawableSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    @Override
    protected int getDefaultArtworkResId() {
        return R.drawable.avatar_badge;
    }
}
