package com.soundcloud.android.tests;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class FakeGallery extends Activity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            File image = generateImage();
            setResult(RESULT_OK, new Intent().setData(Uri.fromFile(image)));
        } catch (IOException e) {
            Toast.makeText(this, "Error generating image: "+e.getMessage(), Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
        }
        finish();
    }

    private boolean checkSdCard() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private File generateImage() throws IOException {
        if (checkSdCard()) {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!dir.exists()) {
                if (!dir.mkdirs()) throw new IOException("Can not create dir "+dir);
            }
            File out = new File(dir, System.currentTimeMillis()+".jpg");
            if (FakeCamera.generatePicture(out)) {
                return out;
            } else {
                throw new RuntimeException("Could not create image");
            }
        } else throw new IOException("SD card not available");
    }
}
