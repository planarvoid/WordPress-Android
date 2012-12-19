
package com.soundcloud.android.view.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.utils.ImageUtils;
import org.jetbrains.annotations.Nullable;

public abstract class LazyRow extends FrameLayout {

    private ImageLoader.Options mIconOptions;

    protected @Nullable IScAdapter mAdapter;
    protected ImageLoader mImageLoader;
    protected ImageView mIcon;

    public LazyRow(Context context, @Nullable IScAdapter adapter) {
        this(context, null, adapter);
    }

    public LazyRow(Context context, @Nullable AttributeSet attributeSet, @Nullable IScAdapter adapter) {
        super(context, attributeSet);
        mAdapter = adapter;

        if (mIconOptions == null) mIconOptions = new ImageLoader.Options();
        mImageLoader = ImageLoader.get(context);

        addContent();

        mIcon = (ImageView) findViewById(R.id.icon);
    }

    public long getCurrentUserId() {
        return SoundCloudApplication.getUserId();
    }

    protected abstract View addContent();

    public abstract void display(Cursor cursor);

    public abstract void display(int position, Parcelable p);

    /** update the views with the data corresponding to selection index */
    public void display(int position) {
        final long id = mAdapter.getItemId(position);
        final String iconUri = getIconRemoteUri();
        if (ImageUtils.checkIconShouldLoad(iconUri)) {
            Drawable drawable = mAdapter.getDrawableFromId(id);
            if (drawable != null) {
                // we have already made a drawable for this item
                mIcon.setImageDrawable(drawable);
            } else {
                // no drawable yet, check for a bitmap
                final Bitmap bmp = mImageLoader.getBitmap(iconUri, null, ImageLoader.Options.dontLoadRemote());
                if (bmp != null) {
                    // we have a bitmap, check to see if this was previously empty (should be animated in)
                    if (mAdapter.getIconNotReady(id)) {
                        TransitionDrawable tDrawable = (TransitionDrawable) (drawable = new TransitionDrawable(
                                new Drawable[]{getResources().getDrawable(getDefaultArtworkResId()), new BitmapDrawable(bmp)}));
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
                    mIcon.setImageResource(getDefaultArtworkResId());
                } else {
                    // already loading, just make sure we aren't displaying an old one
                    mIcon.setImageResource(getDefaultArtworkResId());
                }
            }
        } else {
            mImageLoader.unbind(mIcon);
            mIcon.setImageResource(getDefaultArtworkResId());
        }
    }

    abstract protected int getDefaultArtworkResId();

    public String getIconRemoteUri() {
        return "";
    }
}
