
package com.soundcloud.android.view;

import android.util.Log;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

public abstract class LazyRow extends FrameLayout {

    public static final ImageLoader.Options ICON_OPTIONS = new ImageLoader.Options(false);
    protected IScAdapter mAdapter;
    protected ImageLoader mImageLoader;
    protected ImageView mIcon;
    protected String mCurrentImageUri;

    protected int mCurrentPosition;
    protected ImageLoader.Options mIconOptions;
    protected long mCurrentUserId;

    public LazyRow(Context context, IScAdapter adapter) {
        super(context);
        mAdapter = adapter;

        if (mIconOptions == null) mIconOptions = new ImageLoader.Options();
        if (context != null) mImageLoader = ImageLoader.get(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(getRowResourceId(), this);
        mIcon = (ImageView) findViewById(R.id.icon);

        if (getContext().getResources().getDisplayMetrics().density > 1) {
            mIcon.getLayoutParams().width  = 67;
            mIcon.getLayoutParams().height = 67;
        }

        // if we ever do not rebuild the app on logout, this will need to be changed to account for changing user ids
        mCurrentUserId = SoundCloudApplication.getUserIdFromContext(getContext());
    }


    protected abstract int getRowResourceId();

    public abstract void display(Cursor cursor);

    public abstract void display(int position, Parcelable p);

    /** update the views with the data corresponding to selection index */
    public void display(int position) {
        mCurrentPosition = position;
        final Long id = mAdapter.getItemId(mCurrentPosition);
        final String iconUri = getIconRemoteUri();
        if (CloudUtils.checkIconShouldLoad(iconUri)) {
            final Bitmap bmp = mImageLoader.getBitmap(iconUri,null, ICON_OPTIONS);
            if (bmp != null){
                Drawable drawable = mAdapter.getDrawableFromId(id);
                if (drawable == null){
                    if (mAdapter.getIconLoading(id)){
                        TransitionDrawable tDrawable = (TransitionDrawable) (drawable = new TransitionDrawable(new Drawable[]{mIcon.getBackground(),new BitmapDrawable(bmp)}));
                        tDrawable.setCrossFadeEnabled(true);
                        tDrawable.setCallback(new android.graphics.drawable.Drawable.Callback(){
                            @Override public void invalidateDrawable(Drawable drawable) { mIcon.invalidate(); }
                            @Override public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) { }
                            @Override public void unscheduleDrawable(Drawable drawable, Runnable runnable) { }
                        });
                        tDrawable.startTransition(300);
                    } else {
                        drawable = new BitmapDrawable(bmp);
                    }
                }
                mAdapter.assignDrawableToId(id, drawable);
                mIcon.setImageDrawable(drawable);
            } else {
                mAdapter.setIconLoading(id);
                mImageLoader.bind((BaseAdapter) mAdapter, mIcon, iconUri, mIconOptions);
            }
        } else {
            mImageLoader.unbind(mIcon);
            mIcon.setImageDrawable(null);
        }
    }

    public String getDebugName(int position) {
        return "name";
    }

    public String getIconRemoteUri() {
        return "";
    }

    public void cleanup() {
        if (mIcon != null) {
            mImageLoader.unbind(mIcon);
        }
    }
}
