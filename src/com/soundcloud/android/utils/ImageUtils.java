package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

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

    public static boolean setImage(File imageFile, ImageView imageView, DisplayMetrics metrics) {
        Bitmap bitmap;
        try {
            final int viewDimension = (int) metrics.density * 100;
            BitmapFactory.Options opt = determineResizeOptions(imageFile, viewDimension, viewDimension);

            BitmapFactory.Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), sampleOpt);

            Matrix m = new Matrix();
            float scale;
            float dx = 0, dy = 0;

            // assumes height and width are the same
            if (bitmap.getWidth() > bitmap.getHeight()) {
                scale = (float) viewDimension / (float) bitmap.getHeight();
                dx = (viewDimension - bitmap.getWidth() * scale) * 0.5f;
            } else {
                scale = (float) viewDimension / (float) bitmap.getWidth();
                dy = (viewDimension - bitmap.getHeight() * scale) * 0.5f;
            }

            m.setScale(scale, scale);
            m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            if (getExifRotation(imageFile.getAbsolutePath()) != 0) {
                m.postRotate(90, viewDimension / 2, viewDimension / 2);
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
        Log.d(TAG, "resizing "+inputFile+"=>"+outputFile);
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

    public static File getFromMediaUri(ContentResolver resolver, Uri uri) {
        String[] filePathColumn = { MediaStore.MediaColumns.DATA };
        Cursor cursor = resolver.query(uri, filePathColumn, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                return new File(filePath);
            }
            cursor.close();
        }
        return null;
    }
}
