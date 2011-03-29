
package com.soundcloud.android.view;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.Recording;

import android.view.View;

public class MyTracklistRow extends TracklistRow {

    private boolean mUploading;

    private boolean mIsPendingUpload;

    public MyTracklistRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.my_track_list_item;
    }

    @Override
    public void display(int position) {
        if (mAdapter.getItem(position) instanceof Recording){
            mIsPendingUpload = true;
            fillRowFromRecording(((Recording) mAdapter.getItem(position)));
        } else {
            mIsPendingUpload = false;
            super.display(position);
        }

    }

    private void fillRowFromRecording(Recording recording){
        setBackgroundColor(mActivity.getResources().getColor(R.color.recordUploadBackground));
        mTitle.setText(CloudUtils.generateRecordingSharingNote(
                recording.what_text,
                recording.where_text,
                recording.timestamp));
        mTitle.setTextColor(0xFFFFFFFF);

        if (mUploading) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mCreatedAt.setTextColor(mActivity.getResources().getColor(R.color.listTxtSecondaryDark));
            mProgressBar.setVisibility(View.GONE);
            mCreatedAt
                    .setText(CloudUtils.getTimeElapsed(mActivity,
                            recording.timestamp)
                            + ", "
                            + (recording.upload_error ? mActivity
                                    .getString(R.string.cloud_upload_not_yet_uploaded) : mActivity
                                    .getString(R.string.cloud_upload_upload_failed)));
        }
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
