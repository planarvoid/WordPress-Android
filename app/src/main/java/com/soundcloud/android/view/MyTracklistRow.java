
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.imageloader.OldImageLoader;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.adapter.PlayableRow;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;

import java.io.IOException;

// TODO: Ugly as FUCK. This class overrides practically everything from its super classes.
// It doesn't even operate on a Playable. Could make nicer by wrapping the Recording in PlayableAdapter.
public class MyTracklistRow extends PlayableRow {
    private Drawable mPrivateBgDrawable;
    private Drawable mVeryPrivateBgDrawable;
    private final int mTargetIconDimension;

    private OldImageLoader mOldImageLoader;

    private Recording mRecording;

    public MyTracklistRow(Context activity) {
        super(activity);
        mTargetIconDimension = (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE);
        mOldImageLoader = OldImageLoader.get(activity);
    }

    @Override
    protected View addContent(AttributeSet attributeSet) {
        return View.inflate(getContext(), R.layout.record_list_item_row, this);
    }

    private Drawable getPrivateBgDrawable(){
          if (mPrivateBgDrawable == null) {
              mPrivateBgDrawable = getResources().getDrawable(R.drawable.round_rect_gray);
              mPrivateBgDrawable.setBounds(0, 0, mPrivateBgDrawable.getIntrinsicWidth(), mPrivateBgDrawable.getIntrinsicHeight());
          }
        return mPrivateBgDrawable;
    }

    private Drawable getVeryPrivateBgDrawable(){
          if (mVeryPrivateBgDrawable == null) {
              mVeryPrivateBgDrawable = getResources().getDrawable(R.drawable.round_rect_orange);
              mVeryPrivateBgDrawable.setBounds(0, 0, mVeryPrivateBgDrawable.getIntrinsicWidth(), mVeryPrivateBgDrawable.getIntrinsicHeight());
          }
        return mVeryPrivateBgDrawable;
    }

    @Override
    protected void setTitle() {
        mTitle.setText(mRecording.sharingNote(getResources()));
    }

    @Override
    public void setTitle(boolean pressed) {
        setTitle();
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor));
    }
    @Override
    public void display(int position, Parcelable p) {
        if (!(p instanceof Recording)) {
            SoundCloudApplication.handleSilentException("item "+p+" at position " +position + " is not a recording", null);
            return;
        }

        mRecording = ((Recording) p);

        setTitle();

        if (!mRecording.is_private) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            if (!TextUtils.isEmpty(mRecording.getRecipientUsername())){
                mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                mPrivateIndicator.setText(mRecording.getRecipientUsername());
            } else {
                final int sharedToCount = TextUtils.isEmpty(mRecording.shared_emails) ? 0
                        : mRecording.shared_emails.split(",").length;
                if (sharedToCount < 8){
                    mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                } else {
                    mPrivateIndicator.setBackgroundDrawable(getPrivateBgDrawable());
                }
                mPrivateIndicator.setText(sharedToCount > 0 ? (sharedToCount == 1 ?
                            getContext().getString(R.string.tracklist_item_shared_with_1_person) :
                            getContext().getString(R.string.tracklist_item_shared_with_x_people, sharedToCount))
                        : getContext().getString(R.string.tracklist_item_shared_with_you));
            }
            mPrivateIndicator.setVisibility(View.VISIBLE);
        }

        mCreatedAt.setTextColor(getContext().getResources().getColor(R.color.listTxtRecSecondary));
        mCreatedAt.setText(mRecording.getStatus(getContext().getResources()));

        loadIcon(mRecording);

        if (mRecording.isUploading()) {
            if (findViewById(R.id.processing_progress) != null) {
                findViewById(R.id.processing_progress).setVisibility(View.VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.processing_progress_stub)).inflate();
            }
        } else if (findViewById(R.id.processing_progress) != null) {
            findViewById(R.id.processing_progress).setVisibility(View.GONE);
        }
    }

    protected void loadIcon(Recording recording) {
        if (recording.artwork_path == null) {
            mOldImageLoader.unbind(mIcon);
        } else {
            OldImageLoader.Options options = new OldImageLoader.Options();
            try {
                options.decodeInSampleSize = ImageUtils.determineResizeOptions(
                        recording.artwork_path,
                        (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE),
                        (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE), false
                ).inSampleSize;
            } catch (IOException e) {
                Log.w(SoundCloudApplication.TAG, "error", e);
            }
            mOldImageLoader.bind(mIcon, recording.artwork_path.getAbsolutePath(), null, options);
        }
    }

    private void setArtwork(final Recording recording) {

        if (recording.artwork_path == null) {
            mOldImageLoader.unbind(mIcon);
        } else {
            // use getBitmap instead of bind here because we have to account for exif rotation on local images
            final String fileUri = Uri.fromFile(recording.artwork_path).toString();
            mOldImageLoader.getBitmap(
                    fileUri,
                    new OldImageLoader.BitmapLoadCallback(){
                        @Override
                        public void onImageLoaded(Bitmap bitmap, String url) {
                            if (fileUri.equals(url)) setImageBitmap(recording, bitmap);
                        }

                        @Override
                        public void onImageError(String url, Throwable error) { }
                    }, getContext(),
                    ImageUtils.getImageLoaderOptionsWithResizeSet(recording.artwork_path, mTargetIconDimension, mTargetIconDimension, false)
            );
        }
    }

    private void setImageBitmap(Recording recording, Bitmap b) {
        ImageUtils.setImage(b, mIcon,
                (int) getResources().getDimension(R.dimen.list_icon_width),
                (int) getResources().getDimension(R.dimen.list_icon_height),
                ImageUtils.getExifRotation(recording.artwork_path));
    }
}
