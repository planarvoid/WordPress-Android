
package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.FastBitmapDrawable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.View;

import java.io.File;
import java.io.IOException;

public class MyTracklistRow extends TracklistRow {

    private static final String TAG = "MyTracklistRow";

    private boolean mIsPendingUpload;

    private Bitmap mPendingDefaultIconBitmap;

    private FastBitmapDrawable mPendingDefaultIcon;

    public MyTracklistRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mPendingDefaultIconBitmap = BitmapFactory.decodeResource(mActivity.getResources(),R.drawable.artwork_badge_onhold);
        mPendingDefaultIcon = new FastBitmapDrawable(mPendingDefaultIconBitmap);

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.my_track_list_item;
    }

    @Override
    public void display(int position) {
        if (mAdapter.getItem(position) instanceof Recording){
            setBackgroundColor(mActivity.getResources().getColor(R.color.recordListItemBackground));
            mIsPendingUpload = true;
            fillRowFromRecording(((Recording) mAdapter.getItem(position)));
        } else {
            setBackgroundColor(0xFFFFFFFF);
            mIsPendingUpload = false;
            super.display(position);

        }

    }

    private void fillRowFromRecording(Recording recording){

        //get rid of submenu if it exists
        onNoSubmenu();
        if (findViewById(R.id.row_submenu) != null) findViewById(R.id.row_submenu).setVisibility(View.GONE);

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

        mCreatedAt.setTextColor(mActivity.getResources().getColor(R.color.listTxtRecSecondary));
        mCreatedAt.setText(recording.upload_status == 1 ? mActivity
                .getString(R.string.cloud_upload_currently_uploading) : CloudUtils
                .getTimeElapsed(mActivity, recording.timestamp)
                + ", "
                + (recording.upload_error ? mActivity
                        .getString(R.string.cloud_upload_upload_failed) : mActivity
                        .getString(R.string.cloud_upload_not_yet_uploaded)));

        mCloseIcon.setVisibility(recording.upload_status == 1 ? View.VISIBLE : View.GONE);

        if (TextUtils.isEmpty(recording.artwork_path)){
            mImageLoader.unbind(getRowIcon());
            setTemporaryRecordingDrawable(BindResult.ERROR);
            return;
        }

        BindResult result = BindResult.ERROR;

        try {
            result = mImageLoader.bind(mAdapter, getRowIcon(), recording.artwork_path,
                            CloudUtils.determineResizeOptions(
                                            new File(recording.artwork_path),
                                            (int) (getContext().getResources().getDisplayMetrics().density * getIconWidth()),
                                            (int) (getContext().getResources().getDisplayMetrics().density * getIconHeight())).inSampleSize);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setTemporaryRecordingDrawable(result);

    }

    private void setTemporaryRecordingDrawable(BindResult result) {
        if (mIcon == null)
            return;

        if (result != BindResult.OK)
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
                        .getColor(R.color.recordListItemBackground));
            }
        } else {
            if (pressed)
                setBackgroundColor(0x00000000);
        }

    }
}
