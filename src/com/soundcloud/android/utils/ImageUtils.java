package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    public static final int GRAPHIC_DIMENSIONS_BADGE = 47;

    public static BitmapFactory.Options determineResizeOptions(File imageUri, int targetWidth,
            int targetHeight) throws IOException {
        return determineResizeOptions(imageUri, targetHeight, targetHeight, false);
    }

    public static BitmapFactory.Options determineResizeOptions(File imageUri, int targetWidth,
                                                               int targetHeight, boolean crop) throws IOException {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        if (targetWidth == 0 || targetHeight == 0) return options; // some devices report 0

        options.inJustDecodeBounds = true;
        InputStream is = new FileInputStream(imageUri);
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        int height = options.outHeight;
        int width = options.outWidth;

        if (crop){
        if (height > targetHeight || width > targetWidth) {
            if (targetHeight / height < targetWidth / width) {
                options.inSampleSize = Math.round(height / targetHeight);
            } else {
                options.inSampleSize = Math.round(width / targetWidth);
            }

        }
        } else  if (targetHeight / height > targetWidth / width) {
            options.inSampleSize = Math.round(height / targetHeight);
        } else {
            options.inSampleSize = Math.round(width / targetWidth);
        }
        return options;
    }

    public static int getExifRotation(String filepath){
        ExifInterface exif;
        try {
            exif = new ExifInterface(filepath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                    case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                    case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                    default: return 0;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
        return -1;
    }

    public static void clearBitmap(Bitmap bmp) {
        if (bmp != null) {
            bmp.recycle();
            System.gc();
        }
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, pixels, pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Bitmap loadContactPhoto(ContentResolver cr, long id) {
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }

    public static boolean setImage(File imageFile, ImageView imageView, int viewWidth, int viewHeight) {
        Bitmap bitmap;
        try {
            BitmapFactory.Options opt = determineResizeOptions(imageFile, viewWidth, viewHeight);

            BitmapFactory.Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), sampleOpt);

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
            int exifRotation = getExifRotation(imageFile.getAbsolutePath());
            if (exifRotation != 0) {
                m.postRotate(exifRotation, viewWidth / 2, viewHeight / 2);
            }

            imageView.setScaleType(ImageView.ScaleType.MATRIX);
            imageView.setImageMatrix(m);

            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return false;
        }
    }

    public static boolean resizeImageFile(File inputFile, File outputFile, int width, int height)
            throws IOException {
        BitmapFactory.Options options = determineResizeOptions(inputFile, width, height);
        int sampleSize = options.inSampleSize;
        int degree = 0;
        ExifInterface exif = new ExifInterface(inputFile.getAbsolutePath());
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
        if (orientation != -1) {
            // We only recognize a subset of orientation tag values.
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    degree = 0;
                    break;
            }
        }

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

            if (bitmap == null) throw new IOException("error decoding bitmap (bitmap == null)");

            FileOutputStream out = new FileOutputStream(outputFile);
            final boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            ImageUtils.clearBitmap(bitmap);
            return success;
        } else {
            return false;
        }
    }

    /**
     * Shows a dialog with the choice to take a new picture or select one from the gallery.
     */
    public static abstract  class ImagePickListener implements View.OnClickListener {
        public static final int GALLERY_IMAGE_PICK = 9000;
        public static final int GALLERY_IMAGE_TAKE = 9001;

        private final Activity mActivity;

        public ImagePickListener(Activity activity) {
            mActivity = activity;
        }

        protected abstract File getFile();

        @Override public void onClick(View view) {
            new AlertDialog.Builder(mActivity)
                    .setMessage(R.string.image_where)
                    .setPositiveButton(R.string.take_new_picture, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            final File file = getFile();
                            if (file != null) {
                                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                    .putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                                try {
                                    mActivity.startActivityForResult(i, GALLERY_IMAGE_TAKE);
                                } catch (ActivityNotFoundException e) {
                                    CloudUtils.showToast(mActivity, R.string.take_new_picture_error);
                                }
                            }
                        }
                    }).setNegativeButton(R.string.use_existing_image, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    try {
                        mActivity.startActivityForResult(intent, GALLERY_IMAGE_PICK);
                    } catch (ActivityNotFoundException e) {
                        CloudUtils.showToast(mActivity, R.string.use_existing_image_error);
                    }
                }
            })
            .create()
            .show();
        }
    }

    public static String formatGraphicsUrlForList(Context c, String url){
        return formatGraphicsUrl(url,getListItemGraphicSize(c));
    }

    public static String formatGraphicsUrl(String url, String targetSize) {
        return url == null ? null : targetSize.equals(Consts.GraphicsSizes.LARGE) ? url : url.replace(Consts.GraphicsSizes.LARGE, targetSize);
    }

    public static String getListItemGraphicSize(Context c) {

        if (CloudUtils.isScreenXL(c)) {
            return Consts.GraphicsSizes.LARGE;
        } else {
            if (c.getResources().getDisplayMetrics().density > 1) {
                return Consts.GraphicsSizes.LARGE;
            } else {
                return Consts.GraphicsSizes.BADGE;
            }
        }

    }
}
