package com.soundcloud.android.view.adapter;

import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.TrackRepostActivity;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Transformation;
import android.widget.TextView;

/**
 * A playable info bar to be used in list views.
 */
public class PlayableRow extends PlayableBar implements ListRow {

    protected TextView mReposter;
    protected TextView mTrackCount;

    // used to build the string for the title text
    private SpannableStringBuilder mSpanBuilder;

    public PlayableRow(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mReposter = (TextView) findViewById(R.id.playable_reposter);
        mTrackCount = (TextView) findViewById(R.id.playable_track_count);
    }

    public PlayableRow(Context context) {
        this(context, null);
    }

    @Override
    protected void setViewId() {
        // rows should not share the same ID
    }

    @Override
    protected boolean shouldShowFullStats() {
        return true;
    }

    /** update the views with the data corresponding to selection index */
    public void display(int position) {
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        if (!(p instanceof PlayableHolder)) throw new IllegalArgumentException("Not a valid track " + p);

        super.display((PlayableHolder) p);

        Playable playable = mPlayableHolder.getPlayable();
        setupReposter();
        setupProcessingIndicator(playable);

        // makes the row slightly transparent if not playable
        setStaticTransformationsEnabled(!playable.isStreamable());

        if (playable instanceof Playlist && ((Playlist) playable).track_count >= 0){
            mTrackCount.setText(String.valueOf(((Playlist) playable).track_count));
            mTrackCount.setVisibility(View.VISIBLE);
        } else {
            mTrackCount.setVisibility(View.GONE);
        }
    }

    private void setupProcessingIndicator(Playable playable) {
        if (playable instanceof Track && ((Track) playable).isProcessing()) {
            if (findViewById(R.id.processing_progress) != null){
                findViewById(R.id.processing_progress).setVisibility(View.VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.processing_progress_stub)).inflate();
            }
        } else if (findViewById(R.id.processing_progress) != null) {
            findViewById(R.id.processing_progress).setVisibility(View.GONE);
        }
    }

    private void setupReposter() {
        mReposter.setVisibility(View.GONE);

        if (mPlayableHolder instanceof TrackRepostActivity) {
            mReposter.setText(((TrackRepostActivity) mPlayableHolder).user.username);
            mReposter.setVisibility(View.VISIBLE);
        } else if (mPlayableHolder instanceof SoundAssociation) {

            SoundAssociation sa = (SoundAssociation) mPlayableHolder;

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
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        setTitle(pressed);
    }

    @Override
    protected void setTitle() {
        setTitle(isPressed());
    }

    private void setTitle(boolean pressed) {
        if (mPlayableHolder.getPlayable().id == CloudPlaybackService.getCurrentTrackId()) {
            if (mSpanBuilder == null) mSpanBuilder = new SpannableStringBuilder();
            mSpanBuilder.clear();
            mSpanBuilder.append("  ");
            mSpanBuilder.setSpan(new ImageSpan(getContext(), pressed ?
                    R.drawable.list_playing_white_50 : R.drawable.list_playing, ImageSpan.ALIGN_BASELINE),
                    0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpanBuilder.append(mPlayableHolder.getPlayable().title);
            mTitle.setText(mSpanBuilder);
        } else {
            super.setTitle();
        }
    }

    /** List specific functions **/
    @Override
    public String getIconRemoteUri() {
        return mPlayableHolder.getPlayable() == null ? null : mPlayableHolder.getPlayable().getListArtworkUrl(getContext());
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
         super.getChildStaticTransformation(child, t);
         t.setAlpha(0.4f);
         return true;
     }
}
