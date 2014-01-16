package com.soundcloud.android.playback.views;

import static com.soundcloud.android.playback.service.PlaybackService.Actions.ADD_LIKE_ACTION;
import static com.soundcloud.android.playback.service.PlaybackService.Actions.ADD_REPOST_ACTION;
import static com.soundcloud.android.playback.service.PlaybackService.Actions.REMOVE_LIKE_ACTION;
import static com.soundcloud.android.playback.service.PlaybackService.Actions.REMOVE_REPOST_ACTION;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.views.PlayableBar;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import javax.annotation.Nullable;

public class PlayableInfoAndEngagementsController {

    private View mRootView;
    @Nullable
    private ToggleButton mToggleLike;
    @Nullable
    private ToggleButton mToggleRepost;
    @Nullable
    private ImageButton mShareButton;
    @Nullable
    private PlayableBar mTrackInfoBar;

    private Playable mPlayable;

    public PlayableInfoAndEngagementsController(View rootView) {
        this(rootView, null);
    }

    public PlayableInfoAndEngagementsController(View rootView, final PlayerTrackView.PlayerTrackViewListener mListener) {
        mRootView = rootView;
        mToggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        mToggleRepost = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        mShareButton = (ImageButton) rootView.findViewById(R.id.btn_share);

        if (mToggleLike != null) {
            mToggleLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mPlayable != null) {
                        String action = mToggleLike.isChecked() ? ADD_LIKE_ACTION : REMOVE_LIKE_ACTION;
                        Intent intent = new Intent(action);
                        intent.setData(mPlayable.toUri());
                        view.getContext().startService(intent);
                    }
                }
            });
        }

        if (mToggleRepost != null) {
            mToggleRepost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mPlayable != null) {
                        String action = mToggleRepost.isChecked() ? ADD_REPOST_ACTION : REMOVE_REPOST_ACTION;
                        Intent intent = new Intent(action);
                        intent.setData(mPlayable.toUri());
                        view.getContext().startService(intent);
                    }
                }
            });
        }

        if (mShareButton != null) {
            mShareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayable != null) {
                        EventBus.UI.publish(UIEvent.fromShare("<unknown>", mPlayable));
                        Intent shareIntent = mPlayable.getShareIntent();
                        if (shareIntent != null) {
                            mRootView.getContext().startActivity(shareIntent);
                        }
                    }
                }
            });
        }

        if (rootView.findViewById(R.id.btn_addToSet) != null) {
            rootView.findViewById(R.id.btn_addToSet).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayable instanceof Track && mListener != null) {
                        mListener.onAddToPlaylist((Track) mPlayable);
                    }
                }
            });
        }

        mTrackInfoBar = (PlayableBar) rootView.findViewById(R.id.playable_bar);
        if (mTrackInfoBar != null) {
            if (mTrackInfoBar.findViewById(R.id.playable_private_indicator) != null){
                mTrackInfoBar.findViewById(R.id.playable_private_indicator).setVisibility(View.GONE);
            }

            mTrackInfoBar.addTextShadows();
            mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ProfileActivity.startFromPlayable(mTrackInfoBar.getContext(), mPlayable);
                }
            });
        }
    }

    public void setTrack(@NotNull Playable playable) {
        mPlayable = playable;
        if (mToggleLike != null) {
            setLikes((int) mPlayable.likes_count, mPlayable.user_like);
        }
        if (mToggleRepost != null) {
            setReposts((int) mPlayable.reposts_count, mPlayable.user_repost);
        }

        boolean showRepost = mPlayable.isPublic() && mPlayable.getUserId() != SoundCloudApplication.getUserId();
        if (mToggleRepost != null) {
            mToggleRepost.setVisibility(showRepost ? View.VISIBLE : View.GONE);
        }
        if (mShareButton != null) {
            mShareButton.setVisibility(mPlayable.isPublic() ? View.VISIBLE : View.GONE);
        }
        if (mTrackInfoBar != null){
            mTrackInfoBar.setTrack(playable);
        }
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

    @VisibleForTesting
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
