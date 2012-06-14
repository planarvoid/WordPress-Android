package com.soundcloud.android.tests;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @see https://github.com/bryanl/FakeCamera
 */
public class FakeCamera extends Activity {
    private final static String TAG = FakeCamera.class.getSimpleName();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        prepareToSnapPicture();
        setResult(RESULT_OK);
        finish();
    }

    private void prepareToSnapPicture() {
        Intent intent = getIntent();
        if (checkSdCard() && intent.hasExtra(MediaStore.EXTRA_OUTPUT)) {
            if (snapPicture(intent)) {
                Toast.makeText(this, "Click!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkSdCard() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private boolean snapPicture(Intent intent) {
        try {
            return generatePicture(getPicturePath(intent));
        } catch (IOException e) {
            Log.w(TAG, "Can't copy photo", e);
            return false;
        }
    }

    private File getPicturePath(Intent intent) {
        Uri uri = intent.getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
        return new File(uri.getPath());
    }

    private boolean generatePicture(File destination) throws IOException {
        Log.d(TAG, "Generating "+destination);

        OutputStream out = new FileOutputStream(destination);
        Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RadialGradient gradient = new RadialGradient(200, 200, 200,
                0xFFFFFFFF,
                0xFF000000,
                Shader.TileMode.CLAMP);
        Paint p = new Paint();
        p.setDither(true);
        p.setShader(gradient);
        canvas.drawCircle(200, 200, 200, p);
        final boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.close();
        return success;
    }
}
