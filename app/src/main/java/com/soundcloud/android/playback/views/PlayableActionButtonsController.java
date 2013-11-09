package com.soundcloud.android.playback.views;

import static com.soundcloud.android.playback.service.CloudPlaybackService.Actions.ADD_LIKE_ACTION;
import static com.soundcloud.android.playback.service.CloudPlaybackService.Actions.ADD_REPOST_ACTION;
import static com.soundcloud.android.playback.service.CloudPlaybackService.Actions.REMOVE_LIKE_ACTION;
import static com.soundcloud.android.playback.service.CloudPlaybackService.Actions.REMOVE_REPOST_ACTION;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

public class PlayableActionButtonsController {

    private View mRootView;

    private ToggleButton mToggleLike;
    private ToggleButton mToggleRepost;
    private ImageButton mShareButton;

    private Playable mPlayable;

    public PlayableActionButtonsController(View rootView) {
        mRootView = rootView;

        mToggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        mToggleRepost = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        mShareButton = (ImageButton) rootView.findViewById(R.id.btn_share);

        mToggleLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayable != null){
                    String action = mToggleLike.isChecked() ? ADD_LIKE_ACTION : REMOVE_LIKE_ACTION;
                    Intent intent = new Intent(action);
                    intent.setData(mPlayable.toUri());
                    view.getContext().startService(intent);
                }
            }
        });

        mToggleRepost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayable != null){
                    String action = mToggleRepost.isChecked() ? ADD_REPOST_ACTION : REMOVE_REPOST_ACTION;
                    Intent intent = new Intent(action);
                    intent.setData(mPlayable.toUri());
                    view.getContext().startService(intent);
                }
            }
        });

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayable != null){
                    Intent shareIntent = mPlayable.getShareIntent();
                    if (shareIntent != null) {
                        mRootView.getContext().startActivity(shareIntent);
                    }
                }
            }
        });
    }

    public void setTrack(@NotNull Playable playable) {
        mPlayable = playable;
        setLikes((int) mPlayable.likes_count, mPlayable.user_like);
        setReposts((int) mPlayable.reposts_count, mPlayable.user_repost);

        boolean showRepost = mPlayable.isPublic() && mPlayable.getUserId() != SoundCloudApplication.getUserId();
        mToggleRepost.setVisibility(showRepost ? View.VISIBLE : View.GONE);
        mShareButton.setVisibility(mPlayable.isPublic() ? View.VISIBLE : View.GONE);
    }

    public void update(ToggleButton button, int actionStringID, int descriptionPluralID, int count, boolean checked, int checkedStringId) {
        button.setTextOn(labelForCount(count));
        button.setTextOff(labelForCount(count));
        button.setChecked(checked);
        button.invalidate();

        Context context = mRootView.getContext();
        if (AndroidUtils.accessibilityFeaturesAvailable(context)
                && TextUtils.isEmpty(button.getContentDescription())) {
            final StringBuilder builder = new StringBuilder();
            builder.append(context.getResources().getString(actionStringID));

            if (count >= 0) {
                builder.append(", ");
                builder.append(context.getResources().getQuantityString(descriptionPluralID, count, count));
            }

            if (checked) {
                builder.append(", ");
                builder.append(context.getResources().getString(checkedStringId));
            }

            button.setContentDescription(builder.toString());
        }
    }

    String labelForCount(int count) {
        if (count <= 0) {
            return "";
        } else if (count >= 10000) {
            return "9k+"; // top out at 9k or text gets too long again
        } else if (count >= 1000) {
            return count / 1000 + "k+";
        } else {
            return String.valueOf(count);
        }
    }

    private void setLikes(int count, boolean userLiked) {
        update(mToggleLike,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                count,
                userLiked,
                R.string.accessibility_stats_user_liked);
    }

    private void setReposts(int count, boolean userReposted) {
        update(mToggleRepost,
                R.string.accessibility_repost_action,
                R.plurals.accessibility_stats_reposts,
                count,
                userReposted,
                R.string.accessibility_stats_user_reposted);
    }

}
