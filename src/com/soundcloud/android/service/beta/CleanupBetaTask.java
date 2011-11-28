package com.soundcloud.android.service.beta;

import static com.soundcloud.android.service.beta.BetaService.TAG;
import static com.soundcloud.android.utils.CloudUtils.deleteFile;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CleanupBetaTask extends AsyncTask<File, Void, List<File>> {
    @Override
    protected List<File> doInBackground(File... params) {
        final File downloaded = params[0];
        File[] files = downloaded.getParentFile().listFiles();
        List<File> deleted = new ArrayList<File>();
        if (files != null) {
            for (File f : files) {
                if (!f.equals(downloaded) && !f.getName().endsWith(Beta.META_DATA_EXT)) {
                    Log.d(TAG, "deleting "+f);
                    deleteFile(f);
                    deleted.add(f);
                }
            }
        }
        return  deleted;
    }
}
