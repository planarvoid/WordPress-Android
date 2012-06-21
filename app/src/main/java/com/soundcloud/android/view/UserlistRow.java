
package com.soundcloud.android.view;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.EventAware;
import com.soundcloud.android.tracking.Level2;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.tracking.Tracking;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UserlistRow extends LazyRow {
    private User mUser;
    private TextView mUsername;
    private TextView mTracks;
    private TextView mFollowers;
    private View mVrStats;
    private Button mFollowBtn;
    private Button mFollowingBtn;

    public UserlistRow(Context context, ScBaseAdapter baseAdapter) {
        this(context, baseAdapter, false);
    }

    public UserlistRow(Context _activity, IScAdapter _adapter, boolean useFollowBack) {
        super(_activity, _adapter);

        mUsername = (TextView) findViewById(R.id.username);
        mTracks = (TextView) findViewById(R.id.tracks);
        mFollowers = (TextView) findViewById(R.id.followers);
        mIcon = (ImageView) findViewById(R.id.icon);
        mFollowingBtn = (Button) findViewById(R.id.toggleFollowing);
        mFollowBtn = (Button) findViewById(R.id.toggleFollow);
        mVrStats = findViewById(R.id.vr_stats);

        // set proper follow back text and alignment to wider button
        if (useFollowBack) {
            mFollowBtn.setText(R.string.btn_follow_back);
            if (mFollowingBtn.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mFollowingBtn.getLayoutParams();
                layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.toggleFollow);
                layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.toggleFollow);
            }

        } else if (mFollowBtn.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mFollowBtn.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.toggleFollowing);
            layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.toggleFollowing);
        }

        if (mFollowingBtn != null) {
            mFollowingBtn.setFocusable(false);
            mFollowingBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    unfollow(mUser);
                }
            });
        }

        if (mFollowBtn != null) {
            mFollowBtn.setFocusable(false);
            mFollowBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    follow(mUser);
                }
            });
        }

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_row;
    }

    /** update the views with the data corresponding to selection index */
     @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), new User(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        if (!(p instanceof User)) throw new IllegalArgumentException("Not a valid user");

        mUser = (User) p;

        super.display(position);
        if (mUser != null) {
            mUsername.setText(mUser.username);
            setFollowingStatus(true);
            setTrackCount();
            setFollowerCount();
            mVrStats.setVisibility((mUser.track_count <= 0 || mUser.followers_count <= 0) ? View.GONE : View.VISIBLE);
        }
    }

    private void setFollowingStatus(boolean enabled) {
        boolean following = FollowStatus.get().isFollowing(mUser);

        if (mUser.id == mCurrentUserId) {
            mFollowingBtn.setVisibility(View.INVISIBLE);
            mFollowBtn.setVisibility(View.INVISIBLE);
        } else {
            mFollowingBtn.setVisibility(following ? View.VISIBLE : View.INVISIBLE);
            mFollowBtn.setVisibility(following ? View.INVISIBLE : View.VISIBLE);
            mFollowingBtn.setEnabled(enabled);
            mFollowBtn.setEnabled(enabled);
        }
    }

    private void setTrackCount() {
        if (mUser.track_count <= 0){
            mTracks.setVisibility(View.GONE);
        } else {
            mTracks.setText(Integer.toString(mUser.track_count));
            mTracks.setVisibility(View.VISIBLE);
        }
    }

    private void setFollowerCount() {
        if (mUser.followers_count <= 0){
            mFollowers.setVisibility(View.GONE);
        } else {
            mFollowers.setText(Integer.toString(mUser.followers_count));
            mFollowers.setVisibility(View.VISIBLE);
        }
    }

    private void follow(final User user) {
        track(getContext(), Click.Follow, user);
        toggleFollowing(user);
    }

    private void unfollow(final User user) {
        track(getContext(), Click.Unfollow, user);
        toggleFollowing(user);
    }

    private void toggleFollowing(final User user) {
        SoundCloudApplication app = SoundCloudApplication.fromContext(getContext());
        if (app != null) {
            FollowStatus.get().toggleFollowing(user.id, app, new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        setFollowingStatus(true);
                    }
                }
            });
            setFollowingStatus(false);
        }
    }

    @Override
    public String getIconRemoteUri() {
        return mUser == null ? null : mUser.getListAvatarUri(getContext());
    }

    private static void track(Context context, Event event, final User user) {
        if (context.getApplicationContext() instanceof Tracker) {
            Tracker tracker = (Tracker) context.getApplicationContext();
            Level2 level2 = null;

            if (context instanceof EventAware) {
                EventAware pt = (EventAware) context;
                level2 = pt.getEvent().level2();
            } else {
                Tracking tracking = context.getClass().getAnnotation(Tracking.class);
                if (tracking != null) {
                    level2 = tracking.page().level2;
                }
            }
            if (level2 != null) {
                tracker.track(event, user, level2);
            } else {
                Log.w(TAG, "could not track "+event);
            }
        } else {
            Log.w(TAG, "could not track "+event);
        }
    }
}
