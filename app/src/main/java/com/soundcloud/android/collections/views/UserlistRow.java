
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
import com.soundcloud.android.model.Urn;
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

    private User user;

    private final TextView username;
    private final TextView tracks;
    private final TextView followers;
    private final View vrStats;
    private final ToggleButton followBtn;
    private final AccountOperations accountOperations;
    private final FollowingOperations followingOperations;
    private final Screen originScreen;
    private final EventBus eventBus;

    public UserlistRow(Context context, Screen originScreen, ImageOperations imageOperations) {
        super(context, imageOperations);
        eventBus = ((SoundCloudApplication) context.getApplicationContext()).getEventBus();
        this.originScreen = originScreen;
        followingOperations = new FollowingOperations();
        accountOperations = new AccountOperations(context);
        username = (TextView) findViewById(R.id.username);
        tracks = (TextView) findViewById(R.id.tracks);
        followers = (TextView) findViewById(R.id.followers);
        icon = (ImageView) findViewById(R.id.icon);
        followBtn = (ToggleButton) findViewById(R.id.toggle_btn_follow);
        vrStats = findViewById(R.id.vr_stats);

        if (followBtn != null) {
            followBtn.setFocusable(false);
            followBtn.setClickable(false);

            RelativeLayout mFollowBtnHolder = (RelativeLayout) findViewById(R.id.toggleFollowingHolder);
            mFollowBtnHolder.setFocusable(false);
            mFollowBtnHolder.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            mFollowBtnHolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowing(user);
                    eventBus.publish(EventQueue.UI, UIEvent.fromToggleFollow(!followBtn.isChecked(),
                            UserlistRow.this.originScreen.get(), user.getId()));
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
        user = ((UserHolder) p).getUser();

        loadIcon();
        if (user != null) {
            username.setText(user.username);
            setFollowingStatus();
            setTrackCount();
            setFollowerCount();
            vrStats.setVisibility((user.track_count <= 0 || user.followers_count <= 0) ? GONE : VISIBLE);
        }
    }

    private void setFollowingStatus() {
        final boolean following = followingOperations.isFollowing(user);
        if (user.getId() == getCurrentUserId()) {
            followBtn.setVisibility(INVISIBLE);
        } else {
            followBtn.setVisibility(VISIBLE);
            followBtn.setChecked(following);
        }
    }

    private void setTrackCount() {
        if (user.track_count <= 0){
            tracks.setVisibility(GONE);
        } else {
            tracks.setText(Integer.toString(user.track_count));
            tracks.setVisibility(VISIBLE);
        }
    }

    private void setFollowerCount() {
        if (user.followers_count <= 0){
            followers.setVisibility(GONE);
        } else {
            followers.setText(Integer.toString(user.followers_count));
            followers.setVisibility(VISIBLE);
        }
    }

    private void toggleFollowing(final User user) {
        final SyncInitiator syncInitiator = new SyncInitiator(getContext(), accountOperations);
        followingOperations.toggleFollowing(user).observeOn(
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
    public Urn getResourceUrn() {
        return user == null ? null : user.getUrn();
    }

}
