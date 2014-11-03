package com.soundcloud.android.cache;

import com.soundcloud.android.utils.IOUtils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Local disk caching helper class, installs backported HttpResponseCache library.
 *
 * @see <a href="https://github.com/candrews/HttpResponseCache">https://github.com/candrews/HttpResponseCache</a>
 * @see <a href="http://developer.android.com/reference/android/net/http/HttpResponseCache.html">Android HttpResponseCache</a>
 */
public final class FileCache {
    public static final String TAG = FileCache.class.getSimpleName();

    public static class DeleteCacheTask extends AsyncTask<File, Integer, Boolean> {
        private final boolean recurse;

        public DeleteCacheTask(boolean recurse) {
            this.recurse = recurse;
        }

        @Override
        protected Boolean doInBackground(File... params) {
            final File dir = params[0];
            if (recurse) {
                deleteRecursively(dir);
            } else {
                deletePlain(dir);
            }
            return true;
        }

        private void deleteRecursively(File... dirs) {
            for (File d : dirs) {
                if (d.isDirectory()) {
                    IOUtils.deleteDir(d);
                }
            }
        }

        private void deletePlain(File... dirs) {
            List<File> allFiles = new ArrayList<>();
            for (File dir : dirs) {
                if (dir.isDirectory()) {
                    allFiles.addAll(Arrays.asList(IOUtils.nullSafeListFiles(dir, null)));
                }
            }

            for (int i = 0; i < allFiles.size(); i++) {
                File f = allFiles.get(i);
                if (!f.delete()) {
                    Log.w(TAG, "could not delete file " + f);
                }
                publishProgress(i, allFiles.size());
            }
        }
    }

    private FileCache() {}
}
