package com.soundcloud.android.view.adapter;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.AffiliationActivity;
import com.soundcloud.android.model.act.CommentActivity;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;

import java.util.Date;

public class AffiliationActivityRow extends ActivityRow {

    public AffiliationActivityRow(Context context, ScBaseAdapter adapter) {
        super(context, adapter);
    }

    @Override
    protected void init() {
        // do nothing
    }

    @Override
    protected Drawable doGetDrawable(boolean pressed) {
        Drawable drawable =
                getResources().getDrawable(pressed ? R.drawable.activity_following_white_50 : R.drawable.activity_following);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    protected boolean fillParcelable(Parcelable p) {
        return mActivity.getUser() != null;
    }

    @Override
    public void display(int position, Parcelable p) {
        super.display(position, p);
        mUser.setVisibility(View.INVISIBLE);
    }

    @Override
    protected SpannableStringBuilder createSpan() {
        mSpanBuilder = new SpannableStringBuilder();
        mSpanBuilder.append("  ").append(mActivity.getUser().username);
        mSpanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 1, mSpanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mSpanBuilder.append(" " + getContext().getResources().getString(R.string.started_following_you));
        return mSpanBuilder;
    }

}
