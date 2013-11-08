package com.soundcloud.android.collections.views;

import static com.google.common.base.Preconditions.checkArgument;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.behavior.Repost;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.playback.service.CloudPlaybackService;
import com.soundcloud.android.collections.ListRow;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
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
    private final ForegroundColorSpan fcs = new ForegroundColorSpan(getResources().getColor(R.color.scOrange));

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
    protected boolean isListViewRow() {
        return true;
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        checkArgument(p instanceof PlayableHolder, "Not a valid playable holder: " + p);

        super.display((PlayableHolder) p);

        Playable playable = mPlayableHolder.getPlayable();
        setupReposter();
        setupProcessingIndicator(playable);

        // makes the row slightly transparent if not playable
        setStaticTransformationsEnabled(!playable.isStreamable());

        if (playable instanceof Playlist && ((Playlist) playable).getTrackCount() >= 0){
            mTrackCount.setText(String.valueOf(((Playlist) playable).getTrackCount()));
            mTrackCount.setVisibility(VISIBLE);
        } else {
            mTrackCount.setVisibility(GONE);
        }
    }

    private void setupProcessingIndicator(Playable playable) {
        if (playable instanceof Track && ((Track) playable).isProcessing()) {
            if (findViewById(R.id.processing_progress) != null){
                findViewById(R.id.processing_progress).setVisibility(VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.processing_progress_stub)).inflate();
            }
        } else if (findViewById(R.id.processing_progress) != null) {
            findViewById(R.id.processing_progress).setVisibility(GONE);
        }
    }

    private void setupReposter() {
        mReposter.setVisibility(GONE);

        if (mPlayableHolder instanceof Repost) {
            mReposter.setText(((Repost) mPlayableHolder).getReposter().username);
            mReposter.setVisibility(VISIBLE);

        } else if (mPlayableHolder instanceof SoundAssociation) {
            SoundAssociation sa = (SoundAssociation) mPlayableHolder;
            if (sa.associationType == ScContentProvider.CollectionItemTypes.REPOST) {
                mReposter.setVisibility(VISIBLE);
                User reposter = null;

                if (sa.owner == null)  {
                    // currently active user
                    if (getContext() instanceof ProfileActivity) {
                        reposter = ((ProfileActivity)getContext()).getUser();
                    }
                }
                if (reposter !=  null && reposter.getId() != SoundCloudApplication.getUserId()) {
                    mReposter.setText(reposter.username);
                }
            }
        }
    }

    @Override
    protected void setTitle() {
        if (mPlayableHolder.getPlayable().getId() == CloudPlaybackService.getCurrentTrackId()) {
            if (mSpanBuilder == null) mSpanBuilder = new SpannableStringBuilder();

            mSpanBuilder.clear();
            mSpanBuilder.append("  ");
            mSpanBuilder.setSpan(new ImageSpan(getContext(), R.drawable.ic_list_playing_orange, ImageSpan.ALIGN_BOTTOM),
                    0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpanBuilder.append(mPlayableHolder.getPlayable().title);
            // offset by 2 because of the speaker image and space
            mSpanBuilder.setSpan(fcs, 2, mPlayableHolder.getPlayable().title.length()+2,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
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
