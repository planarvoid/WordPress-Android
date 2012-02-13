
package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.utils.ImageUtils;

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
        if (ImageUtils.checkIconShouldLoad(iconUri)) {
            Drawable drawable = mAdapter.getDrawableFromId(id);
            if (drawable != null) {
                // we have already made a drawable for this item
                mIcon.setImageDrawable(drawable);
            } else {
                // no drawable yet, check for a bitmap
                final Bitmap bmp = mImageLoader.getBitmap(iconUri, null, ICON_OPTIONS);
                if (bmp != null) {
                    // we have a bitmap, check to see if this was previously empty (should be animated in)
                    if (mAdapter.getIconNotReady(id)) {
                        TransitionDrawable tDrawable = (TransitionDrawable) (drawable = new TransitionDrawable(new Drawable[]{mIcon.getBackground(), new BitmapDrawable(bmp)}));
                        tDrawable.setCrossFadeEnabled(true);
                        tDrawable.setCallback(new android.graphics.drawable.Drawable.Callback() {
                            @Override public void invalidateDrawable(Drawable drawable) { mIcon.invalidate();}
                            @Override public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) { }
                            @Override public void unscheduleDrawable(Drawable drawable, Runnable runnable) { }
                        });
                        tDrawable.startTransition(300);
                    } else {
                        drawable = new BitmapDrawable(bmp);
                    }
                    mAdapter.assignDrawableToId(id, drawable);
                    mIcon.setImageDrawable(drawable);

                } else if (!mAdapter.getIconNotReady(id)) {
                    // mark it as not ready and tell the imageloader to load it (it will notify the adapter when done)
                    mAdapter.setIconNotReady(id);
                    mImageLoader.bind((BaseAdapter) mAdapter, mIcon, iconUri, mIconOptions);
                } else {
                    // already loading, just make sure we aren't displaying an old one
                    mIcon.setImageBitmap(null);
                }
            }
        } else {
            mImageLoader.unbind(mIcon);
            mIcon.setImageDrawable(null);
        }
    }

    public String getIconRemoteUri() {
        return "";
    }
}
