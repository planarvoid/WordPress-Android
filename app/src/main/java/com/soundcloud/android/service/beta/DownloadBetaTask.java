package com.soundcloud.android.service.beta;

import static com.soundcloud.android.service.beta.BetaService.TAG;
import static com.soundcloud.android.utils.ScTextUtils.hexString;
import static com.soundcloud.android.utils.IOUtils.mkdirs;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DownloadBetaTask extends AsyncTask<Beta, Void, File> {
    private HttpClient mClient;

    public DownloadBetaTask(HttpClient client) {
        this.mClient = client;
    }

    @Override
    protected File doInBackground(Beta... params) {
        final Beta beta = params[0];
        HttpUriRequest request = new HttpGet(beta.getURI().toString());
        final File dest = beta.getLocalFile();
        final File tmp = new File(dest.getAbsolutePath()+".tmp") {{ deleteOnExit(); }};

        mkdirs(BetaService.APK_PATH);

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            HttpResponse resp = mClient.execute(request);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                InputStream is = resp.getEntity().getContent();
                OutputStream fos = new FileOutputStream(tmp);

                byte[] buffer = new byte[8192];
                int n;
                while ((n = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, n);
                    digest.update(buffer, 0, n);
                }
                fos.close();
                is.close();

                final String hex = hexString(digest.digest());
                if (!hex.equals(beta.etag)) {
                    Log.w(TAG, "MD5 sums don't match: " + hex + "!=" + beta.etag);
                    return null;
                } else if (!tmp.renameTo(dest)) {
                    Log.w(TAG, "could not rename file");
                    return  null;
                } else {
                    return dest;
                }
            } else {
                Log.w(TAG, "unexpected status code: " + resp.getStatusLine());
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "error downloading " + beta, e);
            return null;
        } catch (NoSuchAlgorithmException e) {
            // WTF
            throw new RuntimeException(e);
        }
    }
}
