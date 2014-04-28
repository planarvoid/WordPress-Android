package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.DrawableSpan;
import com.soundcloud.android.collections.ListRow;

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
    protected Activity activity;

    private final TextView user;
    private final TextView title;
    private final TextView createdAt;

    private Drawable drawable;
    protected SpannableStringBuilder spanBuilder;

    public ActivityRow(Context context, ImageOperations imageOperations) {
        super(context, imageOperations);

        title = (TextView) findViewById(R.id.title);
        user = (TextView) findViewById(R.id.user);
        createdAt = (TextView) findViewById(R.id.created_at);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final User u = getOriginUser();
                if (u == null) return;

                Intent intent = new Intent(getContext(), ProfileActivity.class);
                intent.putExtra("user", u);
                getContext().startActivity(intent);
            }
        });
        icon.setFocusable(false);
    }

    // override these for non-dashboard activities to account for different parcelable structures
    protected Playable getPlayable() {
        return activity.getPlayable();
    }

    protected User getOriginUser() {
        return (activity == null || activity.getUser() == null) ? null : activity.getUser();
    }

    protected Date getOriginCreatedAt() {
        return activity.getCreatedAt();
    }

    protected SpannableStringBuilder createSpan() {
        spanBuilder = new SpannableStringBuilder();
        spanBuilder.append("  ").append(getPlayable().title);
        addSpan(spanBuilder);
        return spanBuilder;
    }

    protected void addSpan(SpannableStringBuilder builder) {
        spanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 1, spanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public Urn getResourceUrn() {
        if (activity != null && activity.getUser() != null) {
            return activity.getUser().getUrn();
        }
        return null;
    }

    @Override
    protected View addContent(AttributeSet attributeSet) {
        return View.inflate(getContext(), R.layout.activity_list_row, this);
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.sModelManager.getCachedTrackFromCursor(cursor));
    }

    protected abstract  boolean fillParcelable(Parcelable p);

    @Override
    public void display(int position, Parcelable p) {
        activity = (Activity) p;
        if (!fillParcelable(p)) return;

        loadIcon();

        activity = (Activity) p;
        spanBuilder = createSpan();

        setImageSpan();
        createdAt.setText(ScTextUtils.getTimeElapsed(getContext().getResources(), getOriginCreatedAt().getTime()));

        user.setText(getOriginUser().username);
        createdAt.setText(ScTextUtils.getTimeElapsed(getContext().getResources(), getOriginCreatedAt().getTime()));

    }

    private void setImageSpan() {
        if (spanBuilder == null) return;
        spanBuilder.setSpan(new DrawableSpan(getDrawable(), DrawableSpan.ALIGN_BASELINE),
                0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(spanBuilder);
    }

    protected abstract Drawable doGetDrawable();


    private Drawable getDrawable() {
        if (drawable == null) {
            drawable = doGetDrawable();
        }
        return drawable;
    }

}
