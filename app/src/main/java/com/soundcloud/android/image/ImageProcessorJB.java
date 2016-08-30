package com.soundcloud.android.image;

import com.soundcloud.java.optional.Optional;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import javax.inject.Inject;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ImageProcessorJB implements ImageProcessor {

    private static final float DEFAULT_RADIUS = 7.f;
    private final RenderScript renderscript;

    @Inject
    public ImageProcessorJB(Context context) {
        renderscript = RenderScript.create(context);
    }

    public Bitmap blurBitmap(Bitmap bitmap, Optional<Float> blurRadius) {

        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(renderscript, Element.U8_4(renderscript));

        Allocation allIn = Allocation.createFromBitmap(renderscript, bitmap);
        Allocation allOut = Allocation.createFromBitmap(renderscript, outBitmap);

        blurScript.setRadius(blurRadius.or(DEFAULT_RADIUS));
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);

        allOut.copyTo(outBitmap);

        return outBitmap;
    }
}
