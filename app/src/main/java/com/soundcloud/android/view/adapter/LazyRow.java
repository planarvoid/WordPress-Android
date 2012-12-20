
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

        if (mIconOptions == null) mIconOptions = ImageLoader.Options.listFadeIn();
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
        final String iconUri = getIconRemoteUri();
        if (ImageUtils.checkIconShouldLoad(iconUri)) {
            if (mImageLoader.bind(mIcon,iconUri,null,mIconOptions) != ImageLoader.BindResult.OK){
                mIcon.setImageResource(getDefaultArtworkResId());
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
