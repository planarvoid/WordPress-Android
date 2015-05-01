package com.soundcloud.android.utils.images;


import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.OneShotTransitionDrawable;
import com.soundcloud.android.utils.AndroidUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class ImageUtils {
    private static final String TAG = ImageUtils.class.getSimpleName();
    private static final String ERROR = "error";

    public static final int DEFAULT_TRANSITION_DURATION = 200;
    public static final int RECOMMENDED_IMAGE_SIZE = 2048;

    private ImageUtils() {
    }

    public static BitmapFactory.Options decode(File imageFile) throws IOException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;  /* don't allocate bitmap */
        InputStream is = new FileInputStream(imageFile);
        /* output ignored */
        BitmapFactory.decodeStream(is, null, options);
        is.close();
        return options;
    }

    public static BitmapFactory.Options determineResizeOptions(File imageFile,
                                                               int targetWidth,
                                                               int targetHeight,
                                                               boolean crop) throws IOException {

        if (targetWidth == 0 || targetHeight == 0) {
            // some devices report 0
            return new BitmapFactory.Options();
        }
        BitmapFactory.Options options = decode(imageFile);

        final int height = options.outHeight;
        final int width = options.outWidth;

        if (crop) {
            if (height > targetHeight || width > targetWidth) {
                if (targetHeight / height < targetWidth / width) {
                    options.inSampleSize = Math.round((float) height / (float) targetHeight);
                } else {
                    options.inSampleSize = Math.round((float) width / (float) targetWidth);
                }

            }
        } else if (targetHeight / height > targetWidth / width) {
            options.inSampleSize = Math.round((float) height / (float) targetHeight);
        } else {
            options.inSampleSize = Math.round((float) width / (float) targetWidth);
        }
        return options;
    }

    public static int getExifRotation(File imageFile) {
        if (imageFile == null) {
            return -1;
        }
        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            // We only recognize a subset of orientation tag values.
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            Log.e(TAG, ERROR, e);
            return -1;
        }
    }

    public static void clearBitmap(Bitmap bmp) {
        if (bmp != null) {
            bmp.recycle();
        }
    }

    public static Bitmap getConfiguredBitmap(File imageFile, int minWidth, int minHeight) {
        Bitmap bitmap;
        try {
            BitmapFactory.Options opt = determineResizeOptions(imageFile, minWidth, minHeight, false);

            BitmapFactory.Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), sampleOpt);

            Matrix m = new Matrix();
            float scale;
            float dx = 0, dy = 0;

            // assumes height and width are the same
            if (bitmap.getWidth() > bitmap.getHeight()) {
                scale = (float) minHeight / (float) bitmap.getHeight();
                dx = (minWidth - bitmap.getWidth() * scale) * 0.5f;
            } else {
                scale = (float) minWidth / (float) bitmap.getWidth();
                dy = (minHeight - bitmap.getHeight() * scale) * 0.5f;
            }

            m.setScale(scale, scale);
            m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            int exifRotation = getExifRotation(imageFile);
            if (exifRotation != 0) {
                m.postRotate(exifRotation, minWidth / 2, minHeight / 2);
            }

            return (m.isIdentity()) ? bitmap : Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

        } catch (IOException e) {
            Log.e(TAG, ERROR, e);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, ERROR, e);
        }
        return null;
    }

    public static boolean setImage(File imageFile, ImageView imageView, int viewWidth, int viewHeight) {
        Bitmap bitmap;
        try {
            BitmapFactory.Options opt = determineResizeOptions(imageFile, viewWidth, viewHeight, false);
            BitmapFactory.Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), sampleOpt);
            if (bitmap == null) {
                Log.w(TAG, "error decoding " + imageFile);
                return false;
            }

            setImage(bitmap, imageView, viewWidth, viewHeight, getExifRotation(imageFile));
            return true;

        } catch (IOException e) {
            Log.e(TAG, ERROR, e);
            return false;
        }
    }

    /**
     * Set ImageView from Bitmap with a custom matrix that takes into account scaling and exif rotation
     *
     * @param bitmap       source bitmap
     * @param imageView    imageview to set
     * @param viewWidth    imageview width, passed in as this call often occurs before measurement
     * @param viewHeight   imageview height
     * @param exifRotation exif rotation to account for in the matrix
     */
    public static void setImage(Bitmap bitmap, ImageView imageView, int viewWidth, int viewHeight, int exifRotation) {
        Matrix m = new Matrix();
        float scale;
        float dx = 0, dy = 0;

        // assumes height and width are the same
        if (bitmap.getWidth() > bitmap.getHeight()) {
            scale = (float) viewHeight / (float) bitmap.getHeight();
            dx = (viewWidth - bitmap.getWidth() * scale) * 0.5f;
        } else {
            scale = (float) viewWidth / (float) bitmap.getWidth();
            dy = (viewHeight - bitmap.getHeight() * scale) * 0.5f;
        }

        m.setScale(scale, scale);
        m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        if (exifRotation != 0) {
            m.postRotate(exifRotation, viewWidth / 2, viewHeight / 2);
        }

        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        imageView.setImageMatrix(m);

        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(View.VISIBLE);
    }

    public static boolean resizeImageFile(File inputFile, File outputFile, int width, int height) throws IOException {
        BitmapFactory.Options options = determineResizeOptions(inputFile, width, height, false);

        final int sampleSize = options.inSampleSize;
        final int degree = getExifRotation(inputFile);

        if (sampleSize > 1 || degree > 0) {
            InputStream is = new FileInputStream(inputFile);

            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();

            if (degree != 0) {
                Bitmap preRotate = bitmap;
                Matrix mat = new Matrix();
                mat.postRotate(degree);
                bitmap = Bitmap.createBitmap(preRotate, 0, 0, preRotate.getWidth(),
                        preRotate.getHeight(), mat, true);
                preRotate.recycle();
            }

            if (bitmap == null) {
                throw new IOException("error decoding bitmap (bitmap == null)");
            }

            FileOutputStream out = new FileOutputStream(outputFile);
            final boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            clearBitmap(bitmap);

            if (!success) {
                Log.w(TAG, "bitmap.compress returned false");
            }
            return success;
        } else {
            Log.w(TAG, String.format("not resizing: sampleSize %d, degree %d", sampleSize, degree));
            return false;
        }
    }

    public static boolean isScreenXL(Resources resources) {
        return (resources.getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static boolean checkIconShouldLoad(String url) {
        return !(TextUtils.isEmpty(url)
                || url.toLowerCase(Locale.US).equals("null")
                || url.contains("default_avatar"));
    }

    public static File createTempAvatarFile() {
        try {
            return File.createTempFile(Long.toString(System.currentTimeMillis()), ".bmp");
        } catch (IOException e) {
            Log.w(TAG, "error creating avatar temp file", e);
            return null;
        }
    }

    public static void showImagePickerDialog(final Activity activity, final File newImageLocation) {
        new AlertDialog.Builder(activity)
                .setMessage(R.string.image_where)
                .setPositiveButton(R.string.take_new_picture, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ImageUtils.startTakeNewPictureIntent(activity, newImageLocation, Consts.RequestCodes.GALLERY_IMAGE_TAKE);
                    }
                })
                .setNegativeButton(R.string.use_existing_image, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ImageUtils.startPickImageIntent(activity, Consts.RequestCodes.GALLERY_IMAGE_PICK);
                    }
                })
                .show();
    }

    public static void startTakeNewPictureIntent(Activity activity, File destinationFile, int requestCode) {
        if (destinationFile != null) {
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(destinationFile));
            try {
                activity.startActivityForResult(i, requestCode);
            } catch (ActivityNotFoundException e) {
                AndroidUtils.showToast(activity, R.string.take_new_picture_error);
            }
        }
    }

    public static void startPickImageIntent(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            AndroidUtils.showToast(activity, R.string.use_existing_image_error);
        }
    }


    public static void sendCropIntent(Activity activity, Uri imageUri) {
        sendCropIntent(activity, imageUri, imageUri, RECOMMENDED_IMAGE_SIZE, RECOMMENDED_IMAGE_SIZE);
    }

    public static void sendCropIntent(Activity activity, Uri inputUri, Uri outputUri) {
        sendCropIntent(activity, inputUri, outputUri, RECOMMENDED_IMAGE_SIZE, RECOMMENDED_IMAGE_SIZE);
    }

    public static void sendCropIntent(Activity activity, Uri inputUri, Uri outputUri, int width, int height) {
        new Crop(inputUri).output(outputUri).asSquare().withMaxSize(width, height).start(activity);
    }

    public static void recycleImageViewBitmap(ImageView view) {
        if (view.getDrawable() instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                clearBitmap(bitmap);
                view.setImageDrawable(null);
            }
        }
    }

    /*
    http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     */

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static TransitionDrawable createTransitionDrawable(Drawable from, Drawable to) {
        TransitionDrawable tDrawable = new OneShotTransitionDrawable(
                new Drawable[]{
                        from == null ? new ColorDrawable(Color.TRANSPARENT) : from,
                        to
                });
        return tDrawable;
    }

    public static Bitmap toBitmap(Drawable drawable, int width, int height) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Listener that will hold a strong reference to the fake imageview so loading tasks that do not have
     * view population will actually succeed. This is a workaround for
     * https://github.com/nostra13/Android-Universal-Image-Loader/issues/356
     */
    public abstract static class ViewlessLoadingListener implements ImageListener {
        View hardViewRef;

        @Override
        public void onLoadingStarted(String imageUri, View view) {
            hardViewRef = view;
        }
    }
}
