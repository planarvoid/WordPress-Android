
package com.soundcloud.android.view.adapter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserHolder;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.rx.observers.ScObserver;
import com.soundcloud.android.service.sync.SyncInitiator;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.EventAware;
import com.soundcloud.android.tracking.Level2;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.view.adapter.behavior.ListRow;
import rx.android.concurrency.AndroidSchedulers;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UserlistRow extends IconLayout implements ListRow {
    private User mUser;
    private TextView mUsername;
    private TextView mTracks;
    private TextView mFollowers;
    private View mVrStats;
    private RelativeLayout mFollowBtnHolder;
    private Button mFollowOnBtn, mFollowOffBtn;
    private AccountOperations mAccountOperations;
    private FollowingOperations mFollowingOperations;


    public UserlistRow(Context context) {
        super(context);
        mFollowingOperations = new FollowingOperations();
        mAccountOperations = new AccountOperations(context);
        mUsername = (TextView) findViewById(R.id.username);
        mTracks = (TextView) findViewById(R.id.tracks);
        mFollowers = (TextView) findViewById(R.id.followers);
        mIcon = (ImageView) findViewById(R.id.icon);
        mFollowOnBtn = (Button) findViewById(R.id.toggle_btn_follow_on);
        mFollowOffBtn = (Button) findViewById(R.id.toggle_btn_follow_off);
        mVrStats = findViewById(R.id.vr_stats);

        if (mFollowOnBtn != null) {
            mFollowOnBtn.setFocusable(false);
            mFollowOnBtn.setClickable(false);

            mFollowOffBtn.setFocusable(false);
            mFollowOffBtn.setClickable(false);

            mFollowBtnHolder = (RelativeLayout) findViewById(R.id.toggleFollowingHolder);
            mFollowBtnHolder.setFocusable(false);
            mFollowBtnHolder.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            mFollowBtnHolder.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    toggleFollowing(mUser);
                    track(getContext(), mUser.user_following ? Click.Follow : Click.Unfollow, mUser);
                }
            });

        }

    }

    @Override
    protected View addContent(AttributeSet attributeSet) {
        return View.inflate(getContext(),R.layout.user_list_row, this);
    }

    /** update the views with the data corresponding to selection index */
     @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), new User(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        checkArgument(p instanceof UserHolder, "Not a valid user holder: " + p);
        mUser = ((UserHolder) p).getUser();

        loadIcon();
        if (mUser != null) {
            mUsername.setText(mUser.username);
            setFollowingStatus();
            setTrackCount();
            setFollowerCount();
            mVrStats.setVisibility((mUser.track_count <= 0 || mUser.followers_count <= 0) ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected int getDefaultArtworkResId() {
        return R.drawable.avatar_badge;
    }

    private void setFollowingStatus() {
        final boolean following = mFollowingOperations.isFollowing(mUser);

        if (mUser.getId() == getCurrentUserId()) {
            mFollowOffBtn.setVisibility(View.INVISIBLE);
            mFollowOnBtn.setVisibility(View.INVISIBLE);
        } else {
            mFollowOnBtn.setVisibility(following ? View.VISIBLE : View.GONE);
            mFollowOffBtn.setVisibility(following ? View.GONE : View.VISIBLE);
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

    private void toggleFollowing(final User user) {
        mFollowingOperations.toggleFollowing(user).observeOn(AndroidSchedulers.mainThread()).subscribe(new ScObserver<Void>() {
            @Override
            public void onCompleted() {
                setFollowingStatus();
                SyncInitiator.pushFollowingsToApi(mAccountOperations.getSoundCloudAccount());
            }

            @Override
            public void onError(Throwable e) {
                setFollowingStatus();
            }
        });
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
