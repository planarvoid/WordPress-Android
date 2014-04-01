
package com.soundcloud.android.collections.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * The base class for anything that needs to lazily load an icon.
 */
public abstract class IconLayout extends FrameLayout {

    protected ImageView mIcon;
    protected ImageOperations mImageOperations;

    public IconLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, SoundCloudApplication.fromContext(context).getImageOperations());
    }

    public IconLayout(Context context, ImageOperations imageOperations) {
        this(context, null, imageOperations);
    }

    public IconLayout(Context context, @Nullable AttributeSet attributeSet, ImageOperations imageOperations) {
        super(context, attributeSet);
        addContent(attributeSet);
        mIcon = (ImageView) findViewById(R.id.icon);
        mImageOperations = imageOperations;
    }

    public long getCurrentUserId() {
        return SoundCloudApplication.getUserId();
    }

    protected abstract View addContent(AttributeSet attributeSet);

    protected void loadIcon() {
        mImageOperations.displayInAdapterView(getResourceUrn(),
                ImageSize.getListItemImageSize(getContext()),
                mIcon);
    }

    @Nullable
    public String getResourceUrn() {
        return null;
    }

}
