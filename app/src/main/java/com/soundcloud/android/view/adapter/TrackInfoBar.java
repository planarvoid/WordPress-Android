package com.soundcloud.android.view.adapter;

import static com.soundcloud.android.utils.AndroidUtils.setTextShadowForGrayBg;
import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.TrackRepostActivity;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.StatsView;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

public class TrackInfoBar extends LazyRow {
    public static final ImageLoader.Options ICON_OPTIONS = ImageLoader.Options.postAtFront();

    private Playable mPlayable;

    private TextView mUser;
    private TextView mReposter;
    private TextView mTitle;
    private TextView mCreatedAt;
    private TextView mPrivateIndicator;

    private StatsView mStatsView;

    private boolean mShouldLoadIcon;

    private SpannableStringBuilder mSpanBuilder;

    protected ImageView mIcon;
    private ImageLoader.BindResult mCurrentIconBindResult;

    public TrackInfoBar(Context context, @Nullable IScAdapter adapter) {
        super(context,adapter);
        init();
    }

    public TrackInfoBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, null);
        init();
    }

    public TrackInfoBar(Context context) {
        super(context, null);
        init();
    }

    @Override
    protected View addContent() {
        return View.inflate(getContext(), R.layout.track_info_bar, this);
    }

    private void init(){
        mIcon = (ImageView) findViewById(R.id.icon);
        mTitle = (TextView) findViewById(R.id.track);
        mUser = (TextView) findViewById(R.id.user);
        mReposter = (TextView) findViewById(R.id.reposter);
        mCreatedAt = (TextView) findViewById(R.id.track_created_at);

        mPrivateIndicator = (TextView) findViewById(R.id.private_indicator);

        mStatsView = (StatsView) findViewById(R.id.stats);

        if (mAdapter == null) {
            // player view, these need to be set
            setId(R.id.track_info_bar);
        } else {
            if (mIcon != null && mAdapter.getQuickActionMenu() != null) {
                final QuickTrackMenu quickTrackMenu = (QuickTrackMenu) mAdapter.getQuickActionMenu();
                mIcon.setFocusable(false);
                mIcon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mPlayable != null && mPlayable.getTrack() != null){
                            quickTrackMenu.show(mIcon, mPlayable.getTrack());
                        }
                    }
                });
            }
        }
    }

    /**
     *  update the displayed track
     * @param p the playable to display
     * @param playingId the currently playing track, or -1 to ignore the playback status
     * @param shouldLoadIcon should handle loading of the icon here (lists handle elsewhere)
     * @param keepHeight keep the height of the view, even if there are no stats
     * @param showFullStats show full stats, or just play count (for player only)
     */
    public void display(Playable p, long playingId, boolean shouldLoadIcon, boolean keepHeight, boolean showFullStats) {
        mPlayable = p;
        mShouldLoadIcon = shouldLoadIcon;

        final Track track = mPlayable.getTrack();
        if (track == null) return;

        final Context context = getContext();

        mUser.setText(track.user != null ? track.user.username : "");
        mCreatedAt.setText(p.getTimeSinceCreated(context));
        mReposter.setVisibility(View.GONE);

        if (mPlayable instanceof TrackRepostActivity) {
            mReposter.setText(((TrackRepostActivity) mPlayable).user.username);
            mReposter.setVisibility(View.VISIBLE);
        } else if (mPlayable instanceof SoundAssociation) {

            SoundAssociation sa = (SoundAssociation) mPlayable;

            if (sa.associationType == ScContentProvider.CollectionItemTypes.REPOST) {
                mReposter.setVisibility(View.VISIBLE);
                User reposter = null;

                if (sa.user == null)  {
                    // currently active user
                    if (getContext() instanceof UserBrowser) {
                        reposter = ((UserBrowser)getContext()).getUser();
                    }
                }
                if (reposter !=  null && reposter.id != SoundCloudApplication.getUserId()) {
                    mReposter.setText(reposter.username);
                }
            }
        }

        if (track.isPublic()) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            if (track.shared_to_count <= 0) {
                mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange_states);
                mPrivateIndicator.setText(R.string.tracklist_item_shared_count_unavailable);
            } else if (track.shared_to_count == 1){
                mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange_states);
                mPrivateIndicator.setText(track.user_id == SoundCloudApplication.getUserId() ? R.string.tracklist_item_shared_with_1_person : R.string.tracklist_item_shared_with_you);
            } else {
                if (track.shared_to_count < 8){
                    mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange_states);
                } else {
                    mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_gray_states);
                }
                mPrivateIndicator.setText(context.getString(R.string.tracklist_item_shared_with_x_people, track.shared_to_count));
            }
            mPrivateIndicator.setVisibility(View.VISIBLE);
        }

        if (showFullStats) {
            mStatsView.updateWithTrack(track);
        } else {
            mStatsView.setPlays(track.playback_count);
            mStatsView.setLikes(0);
            mStatsView.setReposts(0);
            mStatsView.setComments(0);
        }

        setTitle(false);

        if (track.isProcessing()) {
            if (findViewById(R.id.processing_progress) != null){
                findViewById(R.id.processing_progress).setVisibility(View.VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.processing_progress_stub)).inflate();
            }
        } else if (findViewById(R.id.processing_progress) != null) {
            findViewById(R.id.processing_progress).setVisibility(View.GONE);
        }

        if (shouldLoadIcon) loadIcon();
    }

    private void setTitle(boolean pressed) {
        if (mPlayable == null) return;

        if (mAdapter != null && mPlayable.getTrack().id == CloudPlaybackService.getCurrentTrackId()) {
            if (mSpanBuilder == null) mSpanBuilder = new SpannableStringBuilder();
            mSpanBuilder.clear();
            mSpanBuilder.append("  ");
            mSpanBuilder.setSpan(new ImageSpan(getContext(), pressed ?
                    R.drawable.list_playing_white_50 : R.drawable.list_playing, ImageSpan.ALIGN_BASELINE),
                    0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpanBuilder.append(mPlayable.getTrack().title);
            mTitle.setText(mSpanBuilder);
        } else {
            mTitle.setText(mPlayable.getTrack().title);
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        setTitle(pressed);
    }

    /** List specific functions **/
    @Override
    public String getIconRemoteUri() {
        return mPlayable.getTrack() == null ? null : mPlayable.getTrack().getListArtworkUrl(getContext());
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.MODEL_MANAGER.getTrackFromCursor(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        if (!(p instanceof Playable)) throw new IllegalArgumentException("Not a valid track " + p);

        // have to set the playable here for list icon loading purposes, it gets set again above for non-lists
        mPlayable = (Playable) p;

        super.display(position);

        if (mPlayable.getTrack().isStreamable()) {
            setStaticTransformationsEnabled(false);
        } else {
            setStaticTransformationsEnabled(true);
        }

        display(mPlayable, CloudPlaybackService.getCurrentTrackId(), false, false, true);
    }

    @Override
    protected int getDefaultArtworkResId() {
        return R.drawable.artwork_badge;
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
         super.getChildStaticTransformation(child, t);
         t.setAlpha(0.4f);
         return true;
     }

    /** Non-list, Icon functions **/

    public void onConnected(){
        if (mCurrentIconBindResult == ImageLoader.BindResult.ERROR && mShouldLoadIcon){
            loadIcon();
        }
    }

    private void loadIcon() {
        final String iconUrl = mPlayable.getTrack() == null ? null : Consts.GraphicSize.formatUriForList(getContext(), mPlayable.getTrack().getArtwork());
        if (TextUtils.isEmpty(iconUrl)) {
            mCurrentIconBindResult = ImageLoader.BindResult.OK;
            ImageLoader.get(getContext()).unbind(mIcon); // no artwork
        } else {
            mCurrentIconBindResult = ImageLoader.get(getContext()).bind(mIcon,
                    iconUrl, mImageLoaderCallback, ICON_OPTIONS);
        }
    }

    private ImageLoader.Callback mImageLoaderCallback = new ImageLoader.Callback() {
        @Override
        public void onImageError(ImageView view, String url, Throwable error) {
            mCurrentIconBindResult = ImageLoader.BindResult.ERROR;
        }

        @Override
        public void onImageLoaded(ImageView view, String url) {
            mCurrentIconBindResult = ImageLoader.BindResult.OK;
        }

    };

    public void addTextShadows() {
        setTextShadowForGrayBg(mTitle);
        setTextShadowForGrayBg(mUser);
        setTextShadowForGrayBg(mCreatedAt);
    }

    @Override
    public CharSequence getContentDescription() {
        Track track = mPlayable.getTrack();

        StringBuilder builder = new StringBuilder();
        builder.append(track.getUser().getDisplayName());
        builder.append(": ");
        builder.append(track.title);
        builder.append(", ");

        if (mPlayable instanceof TrackRepostActivity) {
            TrackRepostActivity repost = (TrackRepostActivity)mPlayable;

            builder.append(getContext().getResources().getString(R.string.accessibility_infix_reposted_by));
            builder.append(" ");
            builder.append(repost.getUser().getDisplayName());

            builder.append(", ");
            builder.append(getTimeElapsed(getContext().getResources(), repost.created_at.getTime(), true));
            builder.append(", ");
        } else {
            builder.append(getTimeElapsed(getContext().getResources(), track.created_at.getTime(), true));
            builder.append(", ");
        }


        if (track.playback_count > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_plays,
                                                                         track.playback_count,
                                                                         track.playback_count));
            builder.append(", ");
        }

        if (track.likes_count > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_likes,
                                                                         track.likes_count,
                                                                         track.likes_count));
            builder.append(", ");
        }

        if (track.user_like) {
            builder.append(getContext().getResources().getString(R.string.accessibility_stats_user_liked));
            builder.append(", ");
        }

        if (track.reposts_count > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_reposts,
                                                                         track.reposts_count,
                                                                         track.reposts_count));
            builder.append(", ");
        }

        if (track.user_repost) {
            builder.append(getContext().getResources().getString(R.string.accessibility_stats_user_reposted));
            builder.append(", ");
        }

        if (track.comment_count > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_comments,
                                                                         track.comment_count,
                                                                         track.comment_count));
        }

        return builder.toString();
    }
}
