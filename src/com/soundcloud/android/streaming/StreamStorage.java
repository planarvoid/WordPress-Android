package com.soundcloud.android.streaming;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamStorage {
    static final String LOG_TAG = StreamStorage.class.getSimpleName();
    public static final int DEFAULT_CHUNK_SIZE = 128 * 1024;     // 128k
    public static final int STREAM_CACHE_SIZE  = 200* 1024 * 1024; // 200 MB
    public static final long STREAM_CACHE_MIN_FREE_SPACE = 20 * 1024  * 1024; // 20 MB
    public static final double MAXIMUM_PERCENTAGE_OF_FREE_SPACE = 0.1d;

    private static final int CLEANUP_INTERVAL = 20;
    public static final String STREAMING_WRITES_SINCE_CLEANUP = "streamingWritesSinceCleanup";

    public static final String INDEX_EXTENSION = "index";
    public static final String CHUNKS_EXTENSION = "chunks";

    public final int chunkSize;

    private Context mContext;
    private File mBaseDir, mCompleteDir, mIncompleteDir;

    private long mUsedSpace;
    private long mUsableSpace;

    private Map<String, StreamItem> mItems = new HashMap<String, StreamItem>();
    private Set<String> mConvertingUrls = new HashSet<String>();

    private final int mCleanupInterval;

    public StreamStorage(Context context, File basedir) {
        this(context, basedir, DEFAULT_CHUNK_SIZE, CLEANUP_INTERVAL);
    }

    public StreamStorage(Context context, File basedir, int chunkSize, int cleanupInterval) {
        mContext = context;
        mBaseDir = basedir;
        mIncompleteDir = new File(mBaseDir, "Incomplete");
        mCompleteDir = new File(mBaseDir, "Complete");
        mCleanupInterval = cleanupInterval;

        mkdirs(mIncompleteDir);
        mkdirs(mCompleteDir);

        this.chunkSize = chunkSize;
    }

    public boolean storeMetadata(StreamItem item) {
        try {
            File indexFile = incompleteIndexFileForUrl(item.url);
            if (indexFile.exists() && !indexFile.delete()) Log.w(LOG_TAG, "could not delete "+indexFile);
            mkdirs(indexFile.getParentFile());
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
            item.write(dos);
            dos.close();
            mItems.put(item.urlHash, item);
            return true;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error storing index data ", e);
            return false;
        }
    }

    public StreamItem getMetadata(String url) {
        String hashed = StreamItem.urlHash(url);
        if (!mItems.containsKey(hashed)) {
            mItems.put(hashed, readMetadata(url));
        }
        return mItems.get(hashed);
    }

    public boolean removeMetadata(String url) {
        return mItems.remove(StreamItem.urlHash(url)) != null;
    }

    public ByteBuffer fetchStoredDataForUrl(String url, Range range) throws IOException {
        StreamItem item = getMetadata(url);
        if (item == null) throw new FileNotFoundException("stored data not found");

        Range actualRange = range;
        if (item.getContentLength() > 0) {
            actualRange = range.intersection(Range.from(0, item.getContentLength()));
            if (actualRange == null) {
                throw new IOException("Invalid range, outside content length. Requested range " + range + " from item " + item);
            }
        }
        Range chunkRange = actualRange.chunkRange(chunkSize);
        ByteBuffer data = ByteBuffer.allocate(chunkRange.length * chunkSize);

        // read all the chunks we need
        for (int index : chunkRange) {
            data.put(getChunkData(url, index));
        }
        // and adjust offsets
        data.position(actualRange.start % chunkSize);
        data.limit(actualRange.start % chunkSize + actualRange.length);
        return data;
    }

    /**
     * @param data       the data to store
     * @param chunkIndex the chunk index the data belongs to
     * @param url        the url for the data
     * @return if the data was set
     * @throws java.io.IOException IO error
     */
    public boolean storeData(final String url, ByteBuffer data, final int chunkIndex) throws IOException {
        if (data == null) throw new IllegalArgumentException("buffer is null");
        else if (data.limit() == 0) {
            Log.w(LOG_TAG, "Not Storing Data. Content Length is Zero.");
            return false;
        }
        // Do not add to complete files
        if (completeFileForUrl(url).exists()) {
            Log.d(LOG_TAG, "complete file exists, not adding data");
            return false;
        }

        StreamItem item = getMetadata(url);
        if (item == null) {
            Log.w(LOG_TAG, "no metadata found for url "+url+", not storing");
            return false;
        }

        // return if it's already in store
        if (item.downloadedChunks.contains(chunkIndex)) return false;

        // Prepare incomplete file
        final File incompleteFile = incompleteFileForUrl(url);
        appendToFile(data, incompleteFile);

        // Add Index and save it
        item.downloadedChunks.add(chunkIndex);
        storeMetadata(item);

        if (item.downloadedChunks.size() == item.numberOfChunks(chunkSize)) {
            new CompleteFileTask(item.getContentLength(), chunkSize, item.downloadedChunks) {
                @Override protected void onPreExecute() {
                    mConvertingUrls.add(url);
                }
                @Override protected void onPostExecute(Boolean success) {
                    if (success) {
                        removeIncompleteDataForItem(url);
                    }
                    mConvertingUrls.remove(url);
                }
            }.execute(incompleteFile, completeFileForUrl(url));
        }

        if (mCleanupInterval > 0) {
            //Update the number of writes, cleanup if necessary
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final int currentCount = prefs.getInt(STREAMING_WRITES_SINCE_CLEANUP, 0) + 1;
            prefs.edit().putInt(STREAMING_WRITES_SINCE_CLEANUP, currentCount).commit();

            if (currentCount >= mCleanupInterval) {
                calculateFileMetrics();
                if (cleanup()) {
                    if (SoundCloudApplication.DEV_MODE) {
                        // print file stats again
                        calculateFileMetrics();
                    }
                }
            }
        }
        return true;
    }

    public ByteBuffer getChunkData(String url, int chunkIndex) throws IOException {
        if (completeFileForUrl(url).exists()) {
            return completeDataForChunk(url, chunkIndex);
        } else {
            return incompleteDataForChunk(url, chunkIndex);
        }
    }

    public Index getMissingChunksForItem(String url, Range chunkRange) {
        //we have everything if the complete file exists
        if (completeFileForUrl(url).exists()) {
            return Index.empty();
        } else {
            StreamItem item = getMetadata(url);
            //We have no idea about track size, so let's assume that all chunks are missing
            if (item == null || item.getContentLength() == 0) {
                return chunkRange.toIndex();
            } else {
                long lastChunk = (long) Math.ceil((double) item.getContentLength() / (double) chunkSize) - 1;
                final Index missingIndexes = new Index();
                for (int chunk : chunkRange) {
                    if (!item.downloadedChunks.contains(chunk) && chunk <= lastChunk) {
                        missingIndexes.set(chunk);
                    }
                }
                return missingIndexes;
            }
        }
    }


    /* package */ ByteBuffer getBuffer() {
        return ByteBuffer.allocate(chunkSize);
    }

    /* package */ File completeFileForUrl(String url) {
        return new File(mCompleteDir, StreamItem.urlHash(url));
    }

    /* package */ File incompleteFileForUrl(String url) {
        return new File(mIncompleteDir, StreamItem.urlHash(url)+"."+CHUNKS_EXTENSION);
    }

    /* package */ File incompleteIndexFileForUrl(String url) {
        return new File(mIncompleteDir, StreamItem.urlHash(url)+"."+INDEX_EXTENSION);
    }

    private boolean appendToFile(ByteBuffer data, File incompleteFile) throws IOException {
        mkdirs(incompleteFile.getParentFile());
        final int length = data.remaining();
        FileChannel fc = new FileOutputStream(incompleteFile, true).getChannel();
        fc.write(data);
        // always write chunk size even if it isn't a complete chunk (for offsetting)
        if (length < chunkSize) {
            fc.write(ByteBuffer.allocate(chunkSize - length));
        }
        return true;
    }

    private StreamItem readMetadata(String url) {
        File f = incompleteIndexFileForUrl(url);
        if (f.exists()) {
            try {
                return StreamItem.fromIndexFile(f);
            } catch (IOException e) {
                Log.e(LOG_TAG, "could not read metadata, deleting", e);
                removeAllDataForItem(url);
                return null;
            }
        } else if (completeFileForUrl(url).exists()) {
            return StreamItem.fromCompleteFile(url, completeFileForUrl(url));
        } else {
            // we don't have anything yet
            return null;
        }
    }

    private void removeAllDataForItem(String url) {
        Log.w(LOG_TAG, "removing all data for "+url);
        removeCompleteDataForItem(url);
        removeIncompleteDataForItem(url);
    }

    private boolean removeIncompleteDataForItem(String url) {
        final File incompleteFile = incompleteFileForUrl(url);
        final File indexFile = incompleteIndexFileForUrl(url);
        boolean fileDeleted = true, indexDeleted = true;
        if (incompleteFile.exists()) fileDeleted = incompleteFile.delete();
        if (indexFile.exists()) indexDeleted = indexFile.delete();
        mItems.remove(StreamItem.urlHash(url));
        return fileDeleted && indexDeleted;
    }

    private boolean removeCompleteDataForItem(String url) {
        final File completeFile = completeFileForUrl(url);
        return completeFile.exists() && completeFile.delete();
    }

    /* package */ ByteBuffer incompleteDataForChunk(String url, int chunkIndex) throws IOException {
        StreamItem item = getMetadata(url);
        if (item == null || !item.downloadedChunks.contains(chunkIndex)) {
            throw new FileNotFoundException("download chunk not available");
        }
        int readLength = chunkIndex == item.numberOfChunks(chunkSize) ? (int) item.getContentLength() % chunkSize : chunkSize;
        return readBuffer(incompleteFileForUrl(url), item.downloadedChunks.indexOf(chunkIndex) * chunkSize, readLength);
    }

    /* package */ ByteBuffer completeDataForChunk(String url, long chunkIndex) throws IOException {
        final long totalChunks = getMetadata(url).numberOfChunks(chunkSize);
        if (chunkIndex >= totalChunks) {
            throw new IOException("Requested invalid chunk index. Requested index " + chunkIndex + " of size " + totalChunks);
        }
        return readBuffer(completeFileForUrl(url), chunkIndex * chunkSize, chunkSize);
    }

    private ByteBuffer readBuffer(File f, long pos, int length) throws IOException {
        if (!f.exists()) throw new FileNotFoundException("file "+f+" does not exist");
        FileChannel fc = new FileInputStream(f).getChannel();
        fc.position(pos);
        ByteBuffer bb = ByteBuffer.allocate(length);
        try {
            fc.read(bb);
            bb.flip();
            return bb;
        } finally {
            fc.close();
        }
    }

    private boolean cleanup() {
        if (!mConvertingUrls.isEmpty()) {
            Log.d(LOG_TAG, "Not doing storage cleanup, conversion is going on");
            return false;
        }

        // reset counter
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .putInt(STREAMING_WRITES_SINCE_CLEANUP, 0).commit();

        final long spaceToClean = mUsedSpace - mUsableSpace;
        if (spaceToClean > 0) {
            Log.d(LOG_TAG, "performing cleanup");
            final List<File> files = new ArrayList<File>();
            files.addAll(Arrays.asList(mIncompleteDir.listFiles(extension(CHUNKS_EXTENSION))));
            files.addAll(Arrays.asList(mCompleteDir.listFiles()));
            Collections.sort(files, FileLastModifiedComparator.INSTANCE);
            long cleanedSpace = 0;
            for (File f : files) {
                if (f.delete()) {
                    Log.d(LOG_TAG, "deleted "+f);
                    String name = f.getName();
                    if (name.endsWith(CHUNKS_EXTENSION)) {
                        String hash = name.substring(0, name.indexOf('.'));
                        File indexFile = new File(mIncompleteDir, hash+"."+INDEX_EXTENSION);
                        if (indexFile.exists() && indexFile.delete()) {
                            Log.d(LOG_TAG, "deleted "+indexFile);
                            // invalidate cache
                            mItems.remove(hash);
                        }
                    }
                    cleanedSpace += f.length();
                    if (cleanedSpace >= spaceToClean) break;
                } else {
                    Log.w(LOG_TAG, "could not delete "+f);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /* package */ void calculateFileMetrics() {
        long spaceLeft = getSpaceLeft();
        mUsedSpace = getUsedSpace();
        mUsableSpace = Math.min(
                (long) (Math.floor((mUsedSpace + spaceLeft) * MAXIMUM_PERCENTAGE_OF_FREE_SPACE)),
                STREAM_CACHE_SIZE);

        Log.d(LOG_TAG, String.format("[File Metrics] %.1f mb used, %.1f mb free, %.1f mb usable",
                mUsedSpace/(1024d*1024d), spaceLeft /(1024d*1024d), mUsableSpace/(1024d*1024d)));
    }

    /* package */ long getUsedSpace() {
        long currentlyUsedSpace = 0;
        for (File f : mCompleteDir.listFiles()) {
            currentlyUsedSpace += f.length();
        }
        for (File f : mIncompleteDir.listFiles()) {
            currentlyUsedSpace += f.length();
        }
        return currentlyUsedSpace;
    }

    /* package */ long getSpaceLeft() {
        StatFs fs = new StatFs(mBaseDir.getAbsolutePath());
       return fs.getBlockSize() * fs.getAvailableBlocks();
    }

    /* package */ void verifyMetadata(String url) {
        // perform etag comparison to make sure file data is correct
        // XXX TODO
        /*
        Metadata existing = mMetadata.get(url);
        if (existing != null
            && item.getETag() != null
            && !item.getETag().equals(existing.eTag)) {

            Log.d(LOG_TAG, "eTag don't match, removing cached data");

            removeAllDataForItem(url);
            mMetadata.put(url, item.getMetadata());
        }
        */
    }

    /* package */ static class FileLastModifiedComparator implements Comparator<File> {
        static final FileLastModifiedComparator INSTANCE = new FileLastModifiedComparator();
        @Override
        public int compare(File f1, File f2) {
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
    }

    static FilenameFilter extension(final String ext) {
        return new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith("."+ext);
            }
        };
    }
}
