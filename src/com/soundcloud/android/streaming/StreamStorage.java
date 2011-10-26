package com.soundcloud.android.streaming;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;

import com.soundcloud.android.Consts;

import java.io.*;
import java.util.*;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

public class StreamStorage {
    static final String LOG_TAG = StreamStorage.class.getSimpleName();
    public static final int DEFAULT_CHUNK_SIZE = 128 * 1024;
    /** @noinspection UnusedDeclaration*/
    private static final int CLEANUP_INTERVAL = 20;

    public final int chunkSize;

    private Context mContext;
    private File mBaseDir, mCompleteDir, mIncompleteDir;

    private long mUsedSpace, mSpaceLeft;
    private Map<StreamItem, Long> mIncompleteContentLengths = new HashMap<StreamItem, Long>();
    private Map<StreamItem, List<Integer>> mIncompleteIndexes = new HashMap<StreamItem, List<Integer>>();
    private List<StreamItem> mConvertingItems = new ArrayList<StreamItem>();

    public StreamStorage(Context context, File basedir) {
        this(context, basedir, DEFAULT_CHUNK_SIZE);
    }

    public StreamStorage(Context context, File basedir, int chunkSize) {
        mContext = context;
        mBaseDir = basedir;
        mIncompleteDir = new File(mBaseDir, "Incomplete");
        mCompleteDir = new File(mBaseDir, "Complete");

        mkdirs(mIncompleteDir);
        mkdirs(mCompleteDir);

        this.chunkSize = chunkSize;
    }

    /**
     * @param data       the data to store
     * @param chunkIndex
     * @param item       the item the data belongs to
     * @return if the data was set
     */
    public boolean setData(byte[] data, final int chunkIndex, final StreamItem item) {
        if (data == null) return false;
        if (item.getContentLength() == 0) {
            Log.w(LOG_TAG, "Not Storing Data. Content Length is Zero.");
            return false;
        }

        setContentLength(item, item.getContentLength());

        // Do not add to complete files
        if (completeFileForItem(item).exists()) return false;

        // Prepare incomplete file
        ensureMetadataIsLoadedForItem(item);

        List<Integer> indexes = mIncompleteIndexes.get(item);

        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            mIncompleteIndexes.put(item, indexes);
        }

        // return if it's already in store
        if (indexes.contains(chunkIndex)) return false;

        final File incompleteFile = incompleteFileForItem(item);
        // always write chunk size even if it isn't a complete chunk (for offsetting I think)
        if (data.length != chunkSize) {
            data = Arrays.copyOf(data, chunkSize);
        }

        if (!appendToFile(data, incompleteFile)) return false;

        // Add Index and save it
        indexes.add(chunkIndex);

        try {
            writeIndex(item, indexes);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error storing index data ", e);
            return false;
        }

        if (indexes.size() == numberOfChunksForItem(item)) {
            new ConvertFileToComplete(contentLengthForItem(item), chunkSize, indexes) {
                @Override protected void onPreExecute() {
                    mConvertingItems.add(item);
                }
                @Override protected void onPostExecute(Boolean success) {
                    if (success) {
                        removeIncompleteDataForItem(item);
                        mConvertingItems.remove(item);
                    }
                }
            }.execute(incompleteFile, completeFileForItem(item));
        }

        //Update the number of writes, cleanup if necessary
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final int currentCount = prefs.getInt("streamingWritesSinceCleanup", 0) + 1;
        prefs.edit().putInt("streamingWritesSinceCleanup", currentCount).commit();

//        if (currentCount >= CLEANUP_INTERVAL) {
//            calculateFileMetrics();
//            cleanup();
//        }
        return true;
    }


    public byte[] getChunkData(StreamItem item, int chunkIndex) {
        resetDataIfNecessary(item);
        ensureMetadataIsLoadedForItem(item);

        long savedContentLength = contentLengthForItem(item);

        if (item.getContentLength() == 0) {
            item.setContentLength((int) savedContentLength);
        }

        byte[] data = null;
        try {
            data = incompleteDataForChunk(item, chunkIndex);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error retrieving incomplete chunk data: ", e);
        }

        if (data != null) return data;

        try {
            data = completeDataForChunk(item, chunkIndex);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error retrieving chunk data: ", e);
        }
        if (data != null) return data;

        return null;
    }


    public Index getMissingChunksForItem(StreamItem item, Range chunkRange) {
        if (resetDataIfNecessary(item)) {
            Log.w(LOG_TAG, "data reset for " +item);
        }

        //If the complete file exists
        if (completeFileForItem(item).exists()) {
            return Index.empty();
        }
        ensureMetadataIsLoadedForItem(item);
        long contentLength = getContentLengthForItem(item);

        //We have no idea about track size, so let's assume that all chunks are missing
        if (contentLength == 0) {
            return chunkRange.toIndex();
        } else {
            long lastChunk = (long) Math.ceil((double) contentLength / (double) chunkSize) - 1;
            List<Integer> allIncompleteIndexes = mIncompleteIndexes.get(item);
            if (allIncompleteIndexes == null) allIncompleteIndexes = Collections.emptyList();

            final Index missingIndexes = new Index();
            for (int chunk = chunkRange.location; chunk < chunkRange.end(); chunk++) {
                if (!allIncompleteIndexes.contains(chunk) && chunk <= lastChunk) {
                    missingIndexes.set(chunk);
                }
            }
            return missingIndexes;
        }
    }

    public Map<StreamItem, List<Integer>> getIncompleteIndexes() {
        return mIncompleteIndexes;
    }

    public Map<StreamItem, Long> getIncompleteContentLengths() {
        return mIncompleteContentLengths;
    }

    private void ensureMetadataIsLoadedForItem(StreamItem item) {
        if (!isMetaDataLoaded(item) && !completeFileForItem(item).exists()) {
            readIndex(item);
        }
    }

    private boolean appendToFile(byte[] data, File incompleteFile) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(incompleteFile, true);
            fos.write(data);
            return true;
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error storing chunk data ", e);
            return false;
        } finally {
            if (fos != null) try {
                fos.close();
            } catch (IOException ignored) {
            }
        }
    }

    /* package */ void writeIndex(StreamItem item, List<Integer> indexes) throws IOException {
        File indexFile = incompleteIndexFileForItem(item);
        if (indexFile.exists() && !indexFile.delete()) Log.w(LOG_TAG, "could not delete "+indexFile);
        DataOutputStream din = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
        din.writeLong(item.getContentLength());
        for (Integer index : indexes) {
            din.writeInt(index);
        }
        din.close();
    }

    /* package */ boolean readIndex(StreamItem item) {
        File indexFile = incompleteIndexFileForItem(item);
        if (indexFile.exists()) {
            try {
                DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
                mIncompleteContentLengths.put(item, din.readLong());

                int count = (int) (indexFile.length() / 4) - 2;
                ArrayList<Integer> indexes = new ArrayList<Integer>();
                for (int i = 0; i < count; i++) {
                    indexes.add(din.readInt());
                }
                mIncompleteIndexes.put(item, indexes);

                return true;
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                return false;
            }
        } else return false;
    }

    /* package */ boolean isMetaDataLoaded(StreamItem item) {
        return mIncompleteContentLengths.get(item) != null && mIncompleteIndexes.get(item) != null;
    }


    private long contentLengthForItem(StreamItem item) {
        final File completeFile = completeFileForItem(item);
        if (completeFile.exists()) return completeFile.length();
        return mIncompleteContentLengths.containsKey(item) ? mIncompleteContentLengths.get(item) : 0;
    }

    /* package */ File completeFileForItem(StreamItem item) {
        return new File(mCompleteDir, item.getURLHash());
    }

    /* package */ File incompleteFileForItem(StreamItem item) {
        return new File(mIncompleteDir, item.getURLHash());
    }

    private File incompleteIndexFileForItem(StreamItem item) {
        return new File(mIncompleteDir, item.getURLHash() + "_index");

    }

    private boolean resetDataIfNecessary(StreamItem item) {
        if (item.getContentLength() != 0 &&
            item.getContentLength() != getContentLengthForItem(item)) {
            removeAllDataForItem(item);
            return true;
        }
        return false;
    }

    private long getContentLengthForItem(StreamItem item) {
        if (completeFileForItem(item).exists()) {
            return completeFileForItem(item).length();
        } else {
            return mIncompleteContentLengths.containsKey(item) ? mIncompleteContentLengths.get(item) : 0;
        }
    }

    private void removeAllDataForItem(StreamItem item) {
        Log.w(LOG_TAG, "removing all data for "+item);
        removeCompleteDataForItem(item);
        removeIncompleteDataForItem(item);
    }

    private boolean removeIncompleteDataForItem(StreamItem item) {
        final File incompleteFile = incompleteFileForItem(item);
        final File indexFile = incompleteIndexFileForItem(item);
        boolean fileDeleted = true, indexDeleted = true;
        if (incompleteFile.exists()) fileDeleted = incompleteFile.delete();
        if (indexFile.exists()) indexDeleted = indexFile.delete();
        mIncompleteIndexes.remove(item);
        mIncompleteContentLengths.remove(item);
        return fileDeleted && indexDeleted;
    }

    private boolean removeCompleteDataForItem(StreamItem item) {
        final File completeFile = completeFileForItem(item);
        return completeFile.exists() && completeFile.delete();
    }


    /* package */ byte[] incompleteDataForChunk(StreamItem item, int chunkIndex) throws IOException {
        final List<Integer> indexArray = mIncompleteIndexes.get(item);

        if (indexArray != null) {
            if (!indexArray.contains(chunkIndex)) return null;
            File chunkFile = incompleteFileForItem(item);

            if (chunkFile.exists()) {
                int seekToChunkOffset = indexArray.indexOf(chunkIndex) * chunkSize;
                int readLength = chunkSize;
                if (chunkIndex == numberOfChunksForItem(item)) {
                    readLength = (int) (contentLengthForItem(item) % chunkSize);
                }
                byte[] buffer = new byte[readLength];
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(chunkFile, "r");
                    raf.seek(seekToChunkOffset);
                    raf.readFully(buffer, 0, readLength);
                } finally {
                    if (raf != null) {
                        raf.close();
                    }
                }

                return buffer;
            }
        }
        return null;
    }

    /* package */ byte[] completeDataForChunk(StreamItem item, long chunkIndex) throws IOException {
        final File completeFile = completeFileForItem(item);

        if (completeFile.exists()) {
            final long totalChunks = numberOfChunksForItem(item);
            if (chunkIndex >= totalChunks) {
                Log.e(LOG_TAG, "Requested invalid chunk index. Requested index " + chunkIndex + " of size " + totalChunks);
                return null;
            }
            int seekToChunkOffset = (int) (chunkIndex * chunkSize);
            int readLength = chunkSize;
            if (chunkIndex == totalChunks - 1) {
                readLength = (int) (contentLengthForItem(item) % chunkSize);
            }
            byte[] buffer = new byte[readLength];
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(completeFile, "r");
                raf.seek(seekToChunkOffset);
                raf.readFully(buffer, 0, readLength);
            } finally {
                if (raf != null) {
                    raf.close();
                }
            }
            return buffer;
        }
        return null;
    }

    /* package */ long numberOfChunksForItem(StreamItem item) {
        return (long) Math.ceil(((float) contentLengthForItem(item)) / ((float) chunkSize));
    }


    /** @noinspection UnusedDeclaration*/
    private void cleanup() {
        if (mConvertingItems.size() > 0) {
            Log.d(LOG_TAG, "Not doing storage cleanup, conversion is going on");
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt("streamingWritesSinceCleanup", 0).commit();

        if (mUsedSpace <= Consts.TRACK_MAX_CACHE && mSpaceLeft >= Consts.TRACK_CACHE_MIN_FREE_SPACE) return;

        ArrayList<File> files = new ArrayList<File>();
        files.addAll(Arrays.asList(mIncompleteDir.listFiles()));
        files.addAll(Arrays.asList(mCompleteDir.listFiles()));
        Collections.sort(files, FileLastModifiedComparator.INSTANCE);

        final long spaceToClean = Math.max(mUsedSpace - Consts.TRACK_MAX_CACHE,
                Consts.TRACK_CACHE_MIN_FREE_SPACE - mSpaceLeft);
        int i = 0;
        long cleanedSpace = 0;
        while (i < files.size() && cleanedSpace < spaceToClean) {
            final File f = files.get(i);
            final File parent = f.getParentFile();
            cleanedSpace += f.length();
            if (!f.delete()) Log.w(LOG_TAG, "could not delete "+f);
            if (parent.equals(mIncompleteDir)) {
                File indexFile = new File(f.getAbsolutePath() + "_index");
                if (indexFile.exists()) {
                    cleanedSpace += indexFile.length();
                    if (!indexFile.delete()) Log.w(LOG_TAG, "could not delete "+indexFile);
                }
            }
        }
    }

    /* package */ void calculateFileMetrics() {

        StatFs fs = new StatFs(mBaseDir.getAbsolutePath());
        mSpaceLeft = fs.getBlockSize() * fs.getAvailableBlocks();

        long currentlyUsedSpace = 0;
        for (File f : mCompleteDir.listFiles()) {
            currentlyUsedSpace += f.length();
        }
        for (File f : mIncompleteDir.listFiles()) {
            currentlyUsedSpace += f.length();
        }
        mUsedSpace = currentlyUsedSpace;

        Log.d(LOG_TAG, "[File Metrics] used: " + mUsedSpace + " , free: " + mSpaceLeft);
    }

    /* package */ void setContentLength(StreamItem item, long contentLength) {

        if (contentLength != contentLengthForItem(item)) {
            if (contentLengthForItem(item) != 0) {
                removeAllDataForItem(item);
            }
            mIncompleteContentLengths.put(item, contentLength);
        }
    }

    private static class FileLastModifiedComparator implements Comparator<File> {
        static FileLastModifiedComparator INSTANCE = new FileLastModifiedComparator();
        @Override
        public int compare(File f1, File f2) {
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
    }
}
