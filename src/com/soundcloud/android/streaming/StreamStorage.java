package com.soundcloud.android.streaming;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;

import com.soundcloud.android.Consts;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

public class StreamStorage {
    static final String LOG_TAG = StreamStorage.class.getSimpleName();
    public static final int DEFAULT_CHUNK_SIZE = 128 * 1024;
    private static final int CLEANUP_INTERVAL = 20;
    public static final String STREAMING_WRITES_SINCE_CLEANUP = "streamingWritesSinceCleanup";

    public final int chunkSize;

    private Context mContext;
    private File mBaseDir, mCompleteDir, mIncompleteDir;

    private long mUsedSpace, mSpaceLeft;

    private Map<String, StreamItem> mItems = new HashMap<String, StreamItem>();
    private Set<String> mConvertingUrls = new HashSet<String>();

    private boolean mPerformCleanup;

    public StreamStorage(Context context, File basedir) {
        this(context, basedir, DEFAULT_CHUNK_SIZE, true);
    }

    public StreamStorage(Context context, File basedir, int chunkSize, boolean performCleanup) {
        mContext = context;
        mBaseDir = basedir;
        mIncompleteDir = new File(mBaseDir, "Incomplete");
        mCompleteDir = new File(mBaseDir, "Complete");
        mPerformCleanup = performCleanup;

        mkdirs(mIncompleteDir);
        mkdirs(mCompleteDir);

        this.chunkSize = chunkSize;
    }

    public boolean storeMetadata(StreamItem item) {
        try {
            File indexFile = incompleteIndexFileForUrl(item.url);
            if (indexFile.exists() && !indexFile.delete()) Log.w(LOG_TAG, "could not delete "+indexFile);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
            item.write(dos);
            dos.close();
            mItems.put(item.url, item);
            return true;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error storing index data ", e);
            return false;
        }
    }

    public StreamItem getMetadata(String url) {
        if (!mItems.containsKey(url)) {
            mItems.put(url, readMetadata(url));
        }
        return mItems.get(url);
    }

    public ByteBuffer fetchStoredDataForItem(String url, Range range) throws IOException {
        StreamItem item = getMetadata(url);
        if (item == null) throw new FileNotFoundException("stored data not found");

        Range actualRange = range;
        if (item.getContentLength() != 0) {
            actualRange = range.intersection(Range.from(0, item.getContentLength()));
            if (actualRange == null) {
                throw new IOException("Invalid range, outside content length. Requested range " + range + " from item " + url);
            }
        }
        Range chunkRange = actualRange.chunkRange(chunkSize);
        ByteBuffer data = ByteBuffer.allocate(chunkRange.length * chunkSize);
        for (int chunkIndex : chunkRange) {
            data.put(getChunkData(url, chunkIndex));
        }
        data.limit(actualRange.length);
        data.rewind();
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

        // Prepare incomplete file
        StreamItem item = getMetadata(url);
        verifyMetadata(url);

        if (item == null) throw new IllegalStateException("trying to store data for unknown item");

        List<Integer> downloadedChunks = item.downloadedChunks;

        // return if it's already in store
        if (downloadedChunks.contains(chunkIndex)) return false;

        final File incompleteFile = incompleteFileForUrl(url);

        appendToFile(data, incompleteFile);

        // Add Index and save it
        downloadedChunks.add(chunkIndex);
        storeMetadata(item);

        if (downloadedChunks.size() == item.numberOfChunks(chunkSize)) {
            new ConvertFileToComplete(item.getContentLength(), chunkSize, downloadedChunks) {
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

        if (mPerformCleanup) {
            //Update the number of writes, cleanup if necessary
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final int currentCount = prefs.getInt(STREAMING_WRITES_SINCE_CLEANUP, 0) + 1;
            prefs.edit().putInt(STREAMING_WRITES_SINCE_CLEANUP, currentCount).commit();

            if (currentCount >= CLEANUP_INTERVAL) {
                calculateFileMetrics();
                cleanup();
            }
        }
        return true;
    }

    public ByteBuffer getChunkData(String url, int chunkIndex) throws IOException {
        verifyMetadata(url);
        if (completeFileForUrl(url).exists()) {
            return completeDataForChunk(url, chunkIndex);
        } else {
            return incompleteDataForChunk(url, chunkIndex);
        }
    }

    public Index getMissingChunksForItem(String url, Range chunkRange) {
        verifyMetadata(url);
        //we have everything if the complete file exists
        if (completeFileForUrl(url).exists()) {
            return Index.empty();
        } else {
            StreamItem md = getMetadata(url);
            //We have no idea about track size, so let's assume that all chunks are missing
            if (md == null || md.getContentLength() == 0) {
                return chunkRange.toIndex();
            } else {
                long lastChunk = (long) Math.ceil((double) md.getContentLength() / (double) chunkSize) - 1;
                List<Integer> allIncompleteIndexes = md.downloadedChunks;
                final Index missingIndexes = new Index();
                for (int chunk : chunkRange) {
                    if (!allIncompleteIndexes.contains(chunk) && chunk <= lastChunk) {
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
        return new File(mIncompleteDir, StreamItem.urlHash(url));
    }

    /* package */ File incompleteIndexFileForUrl(String url) {
        return new File(mIncompleteDir, StreamItem.urlHash(url) + "_index");
    }

    private boolean appendToFile(ByteBuffer data, File incompleteFile) throws IOException {
        // always write chunk size even if it isn't a complete chunk (for offsetting)
        final int length = data.remaining();
        FileChannel fc = new FileOutputStream(incompleteFile, true).getChannel();
        fc.write(data);
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
        mItems.remove(url);
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

    private void cleanup() {
        if (!mConvertingUrls.isEmpty()) {
            Log.d(LOG_TAG, "Not doing storage cleanup, conversion is going on");
            return;
        } else {
            Log.d(LOG_TAG, "performing cleanup");
        }

        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt(STREAMING_WRITES_SINCE_CLEANUP, 0).commit();

        if (mUsedSpace <= Consts.TRACK_MAX_CACHE && mSpaceLeft >= Consts.TRACK_CACHE_MIN_FREE_SPACE) return;

        List<File> files = new ArrayList<File>();
        files.addAll(Arrays.asList(mIncompleteDir.listFiles()));
        files.addAll(Arrays.asList(mCompleteDir.listFiles()));
        Collections.sort(files, FileLastModifiedComparator.INSTANCE);

        final long spaceToClean = Math.max(mUsedSpace - Consts.TRACK_MAX_CACHE,
                Consts.TRACK_CACHE_MIN_FREE_SPACE - mSpaceLeft);
        long cleanedSpace = 0;

        for (File f : files) {
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
            if (cleanedSpace >= spaceToClean) break;
        }
    }

    /* package */ void calculateFileMetrics() {
        mSpaceLeft = getSpaceLeft();
        mUsedSpace = getUsedSpace();
        Log.d(LOG_TAG, "[File Metrics] used: " + mUsedSpace + " , free: " + mSpaceLeft);
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

    private static class FileLastModifiedComparator implements Comparator<File> {
        static FileLastModifiedComparator INSTANCE = new FileLastModifiedComparator();
        @Override
        public int compare(File f1, File f2) {
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
    }
}
