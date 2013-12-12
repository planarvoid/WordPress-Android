package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.utils.images.ImageOptionsFactory;

import android.widget.ImageView;

import javax.inject.Inject;

public class ImageOperations {

    @Inject
    ImageLoader mImageLoader;

    public void displayInGridView(String imageUrl, ImageView imageView){
        mImageLoader.displayImage(imageUrl, imageView, ImageOptionsFactory.gridView());
    }

}
