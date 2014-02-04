
package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

// TODO: Ugly as FUCK. This class overrides practically everything from its super classes.
// It doesn't even operate on a Playable. Could make nicer by wrapping the Recording in PlayableAdapter.
public class MyTracklistRow extends PlayableRow {

    private Recording mRecording;

    public MyTracklistRow(Context activity, ImageOperations imageOperations) {
        super(activity, imageOperations);
    }

    @Override
    protected View addContent(AttributeSet attributeSet) {
        return inflate(getContext(), R.layout.record_list_item_row, this);
    }

    @Override
    protected void setTitle() {
        mTitle.setText(mRecording.sharingNote(getResources()));
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.sModelManager.getCachedTrackFromCursor(cursor));
    }
    @Override
    public void display(int position, Parcelable p) {
        if (!(p instanceof Recording)) {
            SoundCloudApplication.handleSilentException("item "+p+" at position " +position + " is not a recording", null);
            return;
        }

        mRecording = ((Recording) p);
        setTitle();

        final TextView createdAt = (TextView) findViewById(R.id.playable_created_at);
        createdAt.setTextColor(getContext().getResources().getColor(R.color.listTxtRecSecondary));
        createdAt.setText(mRecording.getStatus(getContext().getResources()));

        findViewById(R.id.playable_private_indicator).setVisibility(mRecording.is_private ? VISIBLE : GONE);

        loadIcon(mRecording);

        if (mRecording.isUploading()) {
            if (findViewById(R.id.processing_progress) != null) {
                findViewById(R.id.processing_progress).setVisibility(VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.processing_progress_stub)).inflate();
            }
        } else if (findViewById(R.id.processing_progress) != null) {
            findViewById(R.id.processing_progress).setVisibility(GONE);
        }
    }

    protected void loadIcon(Recording recording) {
        if (recording.artwork_path == null) {
            mImageOperations.cancel(mIcon);
            mIcon.setImageDrawable(null);
        } else {
            mImageOperations.display("file://" + recording.artwork_path.getAbsolutePath(), mIcon);
        }
    }

}
