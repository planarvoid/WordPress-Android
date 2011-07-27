package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class NewsRow extends LazyRow {
    protected Event mEvent;

    protected TextView mUser;
    protected TextView mTitle;
    protected TextView mCreatedAt;

    protected ImageView mCloseIcon;


    public NewsRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mTitle = (TextView) findViewById(R.id.title);
        mUser = (TextView) findViewById(R.id.user);
        mCreatedAt = (TextView) findViewById(R.id.created_at);
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

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        mEvent = (Event) mAdapter.getItem(position);

        super.display(position);

        if (mEvent == null)
            return;

        mTitle.setText(mEvent.getTrack().title);
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
        if (mEvent == null || mEvent.getUser().avatar_url == null)
            return "";

        if (CloudUtils.isScreenXL(mActivity)) {
            return CloudUtils.formatGraphicsUrl(mEvent.getUser().avatar_url, Consts.GraphicsSizes.LARGE);
        } else {
            if (getContext().getResources().getDisplayMetrics().density > 1) {
                return CloudUtils.formatGraphicsUrl(mEvent.getUser().avatar_url, Consts.GraphicsSizes.LARGE);
            } else {
                return CloudUtils.formatGraphicsUrl(mEvent.getUser().avatar_url, Consts.GraphicsSizes.BADGE);
            }
        }

    }


}
