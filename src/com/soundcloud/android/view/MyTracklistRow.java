
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.FastBitmapDrawable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

public class MyTracklistRow extends TracklistRow {

    private boolean mUploading;

    private boolean mIsPendingUpload;

    private Bitmap mPendingDefaultIconBitmap;

    private FastBitmapDrawable mPendingDefaultIcon;

    public MyTracklistRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mPendingDefaultIconBitmap = BitmapFactory.decodeResource(mActivity.getResources(),R.drawable.artwork_badge_white);
        mPendingDefaultIcon = new FastBitmapDrawable(mPendingDefaultIconBitmap);

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.my_track_list_item;
    }

    @Override
    public void display(int position) {
        if (mAdapter.getItem(position) instanceof Recording){
            setBackgroundColor(mActivity.getResources().getColor(R.color.recordUploadBackground));
            mIsPendingUpload = true;
            fillRowFromRecording(((Recording) mAdapter.getItem(position)));
        } else {
            setBackgroundColor(0xFFFFFFFF);
            mIsPendingUpload = false;
            super.display(position);

        }

    }

    private void fillRowFromRecording(Recording recording){
        mPlayIndicator.setVisibility(View.GONE);

        mTitle.setText(CloudUtils.generateRecordingSharingNote(
                recording.what_text,
                recording.where_text,
                recording.timestamp));
        mTitle.setTextColor(0xFFFFFFFF);

        if (recording.is_private){
            mPrivateIndicator.setVisibility(View.VISIBLE);
        } else {
            mPrivateIndicator.setVisibility(View.GONE);
        }

        if (mUploading) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mCreatedAt.setTextColor(mActivity.getResources().getColor(R.color.listTxtSecondaryDark));
            mProgressBar.setVisibility(View.GONE);
            mCreatedAt.setText(recording.upload_status == 1 ? mActivity
                    .getString(R.string.cloud_upload_currently_uploading) : CloudUtils
                    .getTimeElapsed(mActivity, recording.timestamp)
                    + ", "
                    + (recording.upload_error ? mActivity
                            .getString(R.string.cloud_upload_upload_failed) : mActivity
                            .getString(R.string.cloud_upload_not_yet_uploaded)));
        }

        mIcon.setImageDrawable(mPendingDefaultIcon);
        mIcon.getLayoutParams().width = (int) (getContext().getResources().getDisplayMetrics().density * getIconWidth());
        mIcon.getLayoutParams().height = (int) (getContext().getResources().getDisplayMetrics().density * getIconHeight());
    }

    /**
     * Make the background transparent during touch events to show the normal list selector
     */
    @Override
    public void setPressed(boolean pressed){
        super.setPressed(pressed);
        if (mIsPendingUpload){
            if (pressed) {
                setBackgroundColor(0x00000000);
            } else {
                setBackgroundColor(mActivity.getResources()
                        .getColor(R.color.recordUploadBackground));
            }
        }
    }
}
