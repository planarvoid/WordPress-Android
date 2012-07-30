
package com.soundcloud.android.record;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;

/**
 * Calculates remaining recording time based on available disk space and
 * optionally a maximum recording file size. The reason why this is not trivial
 * is that the file grows in blocks every few seconds or so, while we want a
 * smooth countdown.
 *
 * Based on com.android.soundrecorder.SoundRecorder
 *
 * @see <a href="http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android-apps/2.0_r1/com/android/soundrecorder/SoundRecorder.java?av=h">
 *  com.android.soundrecorder.SoundRecorder
 * </a>
 */

public class RemainingTimeCalculator {
    // need to keep some space for amplitude data etc which gets written after audio files
    public static final int KEEP_BLOCKS = 100;

    private final File mSDCardDirectory;

    // State for tracking file size of recording.
    private File mEncodedFile;

    // Rate at which the file grows
    private final int mBytesPerSecond;

    // An estimate of current bytes per second for the encoded file
    private int mEncodedBytesPerSecond;
    private long mEncodedBytesPerSecondTotal;

    private int mNumDatapoints;

    // time at which number of free blocks last changed
    private long mBlocksChangedTime;

    // number of available blocks at that time
    private long mLastBlocks;

    // time at which the size of the file has last changed
    private long mFileSizeChangedTime;

    // size of the file at that time
    private long mLastFileSize;

    public RemainingTimeCalculator(int bytesPerSecond) {
        mSDCardDirectory = Environment.getExternalStorageDirectory();
        mBytesPerSecond = bytesPerSecond;
    }

    /**
     * If called, the calculator take an additional encoded file into account when giving the remaining
     * time estimate, based on file growth of the encoded file.
     * @param file the file to watch
     */
    public void setEncodedFile(File file) {
        mEncodedFile = file;
    }

    /**
     * Resets the interpolation.
     */
    public void reset() {
        mEncodedFile = null;
        mBlocksChangedTime = mFileSizeChangedTime = -1;
        mEncodedBytesPerSecondTotal = mEncodedBytesPerSecond = mNumDatapoints = 0;
    }

    /**
     * Returns how long (in seconds) we can continue recording.
     */
    public long timeRemaining() {

        // Calculate how long we can record based on free disk space
        StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
        final long blocks = Math.max(0, fs.getAvailableBlocks() - KEEP_BLOCKS);
        final long blockSize = fs.getBlockSize();
        final long now = System.currentTimeMillis();

        if (mBlocksChangedTime == -1 || blocks != mLastBlocks) {
            mBlocksChangedTime = now;
            mLastBlocks = blocks;
        }

        /*
         * The calculation below always leaves one free block, since free space
         * in the block we're currently writing to is not added. This last block
         * might get nibbled when we close and flush the file, but we won't run
         * out of disk.
         */
        if (mEncodedFile != null) {
            // If we have a recording file set, add an estimate of bytes per second
            mEncodedFile = new File(mEncodedFile.getAbsolutePath());
            long fileSize = mEncodedFile.length();
            if (mFileSizeChangedTime == -1 || fileSize > mLastFileSize) {

                long growth = fileSize - mLastFileSize;
                long timePassed = now - mFileSizeChangedTime;
                mFileSizeChangedTime = now;
                mLastFileSize = fileSize;

                final int bps = (int) (growth / (timePassed / 1000d));

                mEncodedBytesPerSecondTotal += bps;
                mEncodedBytesPerSecond = (int) (mEncodedBytesPerSecondTotal / (double) ++mNumDatapoints);

                // moving average of last 5 values
                if (mNumDatapoints % 5 == 0) {
                    mEncodedBytesPerSecondTotal = 0;
                    mNumDatapoints  = 0;
                }
            }
        }

        // at mBlocksChangedTime we had this much time
        final long totalBytesPerSecond = mBytesPerSecond + mEncodedBytesPerSecond;
        long result = (mLastBlocks * blockSize) / totalBytesPerSecond;
        // so now we have this much time
        result -= (now - mBlocksChangedTime) / 1000;

        return result;
    }

    public boolean isDiskSpaceAvailable() {
        StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
        // keep some free block
        return fs.getAvailableBlocks() > KEEP_BLOCKS;
    }
}
