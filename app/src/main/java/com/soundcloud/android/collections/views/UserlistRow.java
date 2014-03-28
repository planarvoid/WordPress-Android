
package com.soundcloud.android.collections.views;

import static com.google.common.base.Preconditions.checkArgument;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.model.UserHolder;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncInitiator;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class UserlistRow extends IconLayout implements ListRow {
    private User mUser;
    private TextView mUsername;
    private TextView mTracks;
    private TextView mFollowers;
    private View mVrStats;
    private ToggleButton mFollowBtn;
    private AccountOperations mAccountOperations;
    private FollowingOperations mFollowingOperations;
    private Screen mOriginScreen;

    private EventBus mEventBus;

    public UserlistRow(Context context, Screen originScreen, ImageOperations imageOperations) {
        super(context, imageOperations);
        mEventBus = ((SoundCloudApplication) context.getApplicationContext()).getEventBus();
        mOriginScreen = originScreen;
        mFollowingOperations = new FollowingOperations();
        mAccountOperations = new AccountOperations(context);
        mUsername = (TextView) findViewById(R.id.username);
        mTracks = (TextView) findViewById(R.id.tracks);
        mFollowers = (TextView) findViewById(R.id.followers);
        mIcon = (ImageView) findViewById(R.id.icon);
        mFollowBtn = (ToggleButton) findViewById(R.id.toggle_btn_follow);
        mVrStats = findViewById(R.id.vr_stats);

        if (mFollowBtn != null) {
            mFollowBtn.setFocusable(false);
            mFollowBtn.setClickable(false);

            RelativeLayout mFollowBtnHolder = (RelativeLayout) findViewById(R.id.toggleFollowingHolder);
            mFollowBtnHolder.setFocusable(false);
            mFollowBtnHolder.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            mFollowBtnHolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowing(mUser);
                    mEventBus.publish(EventQueue.UI, UIEvent.fromToggleFollow(!mFollowBtn.isChecked(),
                            mOriginScreen.get(), mUser.getId()));
                }
            });
        }
    }

    @Override
    protected View addContent(AttributeSet attributeSet) {
        return inflate(getContext(),R.layout.user_list_row, this);
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
            mVrStats.setVisibility((mUser.track_count <= 0 || mUser.followers_count <= 0) ? GONE : VISIBLE);
        }
    }

    private void setFollowingStatus() {
        final boolean following = mFollowingOperations.isFollowing(mUser);
        if (mUser.getId() == getCurrentUserId()) {
            mFollowBtn.setVisibility(INVISIBLE);
        } else {
            mFollowBtn.setVisibility(VISIBLE);
            mFollowBtn.setChecked(following);
        }
    }

    private void setTrackCount() {
        if (mUser.track_count <= 0){
            mTracks.setVisibility(GONE);
        } else {
            mTracks.setText(Integer.toString(mUser.track_count));
            mTracks.setVisibility(VISIBLE);
        }
    }

    private void setFollowerCount() {
        if (mUser.followers_count <= 0){
            mFollowers.setVisibility(GONE);
        } else {
            mFollowers.setText(Integer.toString(mUser.followers_count));
            mFollowers.setVisibility(VISIBLE);
        }
    }

    private void toggleFollowing(final User user) {
        final SyncInitiator syncInitiator = new SyncInitiator(getContext(), mAccountOperations);
        mFollowingOperations.toggleFollowing(user).observeOn(
                AndroidSchedulers.mainThread()).subscribe(new DefaultSubscriber<UserAssociation>() {
            @Override
            public void onCompleted() {
                syncInitiator.pushFollowingsToApi();
                setFollowingStatus();
            }

            @Override
            public void onError(Throwable e) {
                super.onError(e);
                setFollowingStatus();
            }
        });
    }

    @Override
    public String getResourceUrn() {
        return mUser == null ? null : mUser.getUrn();
    }

}
