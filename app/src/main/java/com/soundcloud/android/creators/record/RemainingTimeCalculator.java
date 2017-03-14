
package com.soundcloud.android.creators.record;

import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/**
 * Calculates remaining recording time based on available disk space and
 * optionally a maximum recording file size. The reason why this is not trivial
 * is that the file grows in blocks every few seconds or so, while we want a
 * smooth countdown.
 *
 * Based on com.android.soundrecorder.SoundRecorder
 *
 * @see <a href="http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android-apps/2.0_r1/com/android/soundrecorder/SoundRecorder.java?av=h">
 * com.android.soundrecorder.SoundRecorder
 * </a>
 */

class RemainingTimeCalculator {
    // need to keep some space for amplitude data etc which gets written after audio files
    private static final int KEEP_BLOCKS = 100;

    private final File externalStorageDirectory;

    // State for tracking file size of recording.
    private File encodedFile;

    // Rate at which the file grows
    private final int bytesPerSecond;

    // An estimate of current bytes per second for the encoded file
    private int encodedBytesPerSecond;
    private long encodedBytesPerSecondTotal;

    private int numDatapoints;

    // time at which number of free blocks last changed
    private long blocksChangedTime;

    // number of available blocks at that time
    private long lastBlocks;

    // time at which the size of the file has last changed
    private long fileSizeChangedTime;

    // size of the file at that time
    private long lastFileSize;

    /**
     * @param bytesPerSecond the estimated bps rate of the audio stream to be recorded
     */
    RemainingTimeCalculator(Context context, int bytesPerSecond) {
        externalStorageDirectory = IOUtils.getExternalStorageDir(context);
        this.bytesPerSecond = bytesPerSecond;
    }

    /**
     * If called, the calculator take an additional encoded file into account when giving the remaining
     * time estimate, based on file growth of the encoded file.
     *
     * @param file the file to watch
     */
    void setEncodedFile(File file) {
        encodedFile = file;
    }

    /**
     * Resets the interpolation.
     */
    public void reset() {
        encodedFile = null;
        blocksChangedTime = fileSizeChangedTime = -1;
        encodedBytesPerSecondTotal = encodedBytesPerSecond = numDatapoints = 0;
    }

    /**
     * @return how long (in seconds) we can continue recording, 0 if no more time left.
     */
    long timeRemaining() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return 0;
        }

        // Calculate how long we can record based on free disk space
        StatFs fs = new StatFs(externalStorageDirectory.getAbsolutePath());
        final long blocks = Math.max(0, fs.getAvailableBlocks() - KEEP_BLOCKS);
        final long blockSize = fs.getBlockSize();
        final long now = System.currentTimeMillis();

        if (blocksChangedTime == -1 || blocks != lastBlocks) {
            blocksChangedTime = now;
            lastBlocks = blocks;
        }

        /*
         * The calculation below always leaves one free block, since free space
         * in the block we're currently writing to is not added. This last block
         * might get nibbled when we close and flush the file, but we won't run
         * out of disk.
         */
        if (encodedFile != null) {
            // If we have a recording file set, add an estimate of bytes per second
            encodedFile = new File(encodedFile.getAbsolutePath());
            long fileSize = encodedFile.length();
            if (fileSizeChangedTime == -1 || fileSize > lastFileSize) {

                long growth = fileSize - lastFileSize;
                long timePassed = now - fileSizeChangedTime;
                fileSizeChangedTime = now;
                lastFileSize = fileSize;

                final int bps = (int) (growth / (timePassed / 1000d));

                encodedBytesPerSecondTotal += bps;
                encodedBytesPerSecond = (int) (encodedBytesPerSecondTotal / (double) ++numDatapoints);

                // moving average of last 5 values
                if (numDatapoints % 5 == 0) {
                    encodedBytesPerSecondTotal = 0;
                    numDatapoints = 0;
                }
            }
        }

        // at blocksChangedTime we had this much time
        final long totalBytesPerSecond = bytesPerSecond + encodedBytesPerSecond;
        long result = (lastBlocks * blockSize) / totalBytesPerSecond;
        // so now we have this much time
        result -= (now - blocksChangedTime) / 1000;
        return Math.max(0, result);
    }

    /**
     * @return if some diskspace is available
     */
    boolean isDiskSpaceAvailable() {
        if (externalStorageDirectory != null && IOUtils.isExternalStorageAvailable()) {
            StatFs fs = new StatFs(externalStorageDirectory.getAbsolutePath());
            // keep some free block
            return fs.getAvailableBlocks() > KEEP_BLOCKS;
        } else {
            return false;
        }
    }
}
