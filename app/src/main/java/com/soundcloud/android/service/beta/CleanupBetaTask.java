package com.soundcloud.android.service.beta;

import static com.soundcloud.android.service.beta.BetaService.TAG;
import static com.soundcloud.android.utils.IOUtils.deleteFile;

import com.soundcloud.android.utils.IOUtils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CleanupBetaTask extends AsyncTask<File, Void, List<File>> {
    @Override
    protected List<File> doInBackground(File... params) {
        final File downloaded = params[0];
        File[] files = IOUtils.nullSafeListFiles(downloaded.getParentFile(), null);
        List<File> deleted = new ArrayList<File>();
        for (File f : files) {
            if (!f.equals(downloaded) && !f.getName().endsWith(Beta.META_DATA_EXT)) {
                Log.d(TAG, "deleting "+f);
                deleteFile(f);
                deleted.add(f);
            }
        }
        return  deleted;
    }
}
