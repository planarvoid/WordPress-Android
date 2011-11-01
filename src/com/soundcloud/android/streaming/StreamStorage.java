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

    private Map<StreamItem, Metadata> mIncompleteStreams = new HashMap<StreamItem, Metadata>();
    private Set<StreamItem> mConvertingItems = new HashSet<StreamItem>();

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

    /**
     * @param data       the data to store
     * @param chunkIndex the chunk index the data belongs to
     * @param item       the item the data belongs to
     * @return if the data was set
     * @throws java.io.IOException IO error
     */
    public boolean storeData(ByteBuffer data, final int chunkIndex, final StreamItem item) throws IOException {
        if (data == null) throw new IllegalArgumentException("buffer is null");

        if (item.getContentLength() == 0) {
            Log.w(LOG_TAG, "Not Storing Data. Content Length is Zero.");
            return false;
        }

        verifyMetadata(item);

        // Do not add to complete files
        if (completeFileForItem(item).exists()) return false;

        // Prepare incomplete file
        Metadata md = ensureMetadataIsLoadedForItem(item);

        List<Integer> downloadedChunks = md.downloadedChunks;

        // return if it's already in store
        if (downloadedChunks.contains(chunkIndex)) return false;

        final File incompleteFile = incompleteFileForItem(item);


        appendToFile(data, incompleteFile);

        // Add Index and save it
        downloadedChunks.add(chunkIndex);

        try {
            writeMetadata(item, md);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error storing index data ", e);
            return false;
        }

        if (downloadedChunks.size() == numberOfChunksForItem(item)) {
            new ConvertFileToComplete(item.getContentLength(), chunkSize, downloadedChunks) {
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


    public ByteBuffer getChunkData(StreamItem item, int chunkIndex) throws IOException {
        verifyMetadata(item);
        ensureMetadataIsLoadedForItem(item);

        if (completeFileForItem(item).exists()) {
            return completeDataForChunk(item, chunkIndex);
        } else {
            return incompleteDataForChunk(item, chunkIndex);
        }
    }


    public Index getMissingChunksForItem(StreamItem item, Range chunkRange) {
        verifyMetadata(item);

        //If the complete file exists
        if (completeFileForItem(item).exists()) {
            return Index.empty();
        }

        Metadata md = ensureMetadataIsLoadedForItem(item);

        //We have no idea about track size, so let's assume that all chunks are missing
        if (md.contentLength == 0) {
            return chunkRange.toIndex();
        } else {
            long lastChunk = (long) Math.ceil((double) md.contentLength / (double) chunkSize) - 1;
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

    /* package */ ByteBuffer getBuffer() {
        return ByteBuffer.allocate(chunkSize);
    }

    /* package */ File completeFileForItem(StreamItem item) {
        return completeFileForItem(item.getURLHash());
    }

    /* package */ File completeFileForItem(String url) {
        return new File(mCompleteDir, StreamItem.urlHash(url));
    }

    /* package */ File incompleteFileForItem(StreamItem item) {
        return new File(mIncompleteDir, item.getURLHash());
    }

    /* package */ File incompleteIndexFileForItem(StreamItem item) {
        return new File(mIncompleteDir, item.getURLHash() + "_index");
    }

    private Metadata ensureMetadataIsLoadedForItem(StreamItem item) {
        if (!mIncompleteStreams.containsKey(item) && !completeFileForItem(item).exists()) {
            mIncompleteStreams.put(item, readMetadata(item));
        }
        return mIncompleteStreams.get(item);
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


    private void writeMetadata(StreamItem item, Metadata md) throws IOException {
        File indexFile = incompleteIndexFileForItem(item);
        if (indexFile.exists() && !indexFile.delete()) Log.w(LOG_TAG, "could not delete "+indexFile);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
        md.write(dos);
        dos.close();
    }

    private Metadata readMetadata(StreamItem item) {
        File f = incompleteIndexFileForItem(item);
        if (f.exists()) {
            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
                return Metadata.read(dis);
            } catch (IOException e) {
                Log.e(LOG_TAG, "could not read metadata, deleting", e);
                removeAllDataForItem(item);
                return item.getMetadata();
            }
        } else {
            // no metadata found, initialise from item
            return item.getMetadata();
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
        mIncompleteStreams.remove(item);
        return fileDeleted && indexDeleted;
    }

    private boolean removeCompleteDataForItem(StreamItem item) {
        final File completeFile = completeFileForItem(item);
        return completeFile.exists() && completeFile.delete();
    }

    /* package */ ByteBuffer incompleteDataForChunk(StreamItem item, int chunkIndex) throws IOException {
        final List<Integer> downloadedChunks = mIncompleteStreams.get(item).downloadedChunks;
        if (!downloadedChunks.contains(chunkIndex)) throw new FileNotFoundException("download chunk not available");
        int readLength = chunkIndex == numberOfChunksForItem(item) ? (int) item.getContentLength() % chunkSize : chunkSize;
        return readBuffer(incompleteFileForItem(item), downloadedChunks.indexOf(chunkIndex) * chunkSize, readLength);
    }

    /* package */ ByteBuffer completeDataForChunk(StreamItem item, long chunkIndex) throws IOException {
        final long totalChunks = numberOfChunksForItem(item);
        if (chunkIndex >= totalChunks) {
            throw new IOException("Requested invalid chunk index. Requested index " + chunkIndex + " of size " + totalChunks);
        }

        int readLength = chunkIndex == totalChunks - 1 ?  (int) item.getContentLength() % chunkSize : chunkSize;
        return readBuffer(completeFileForItem(item), chunkIndex * chunkSize, readLength);
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

    /* package */ long numberOfChunksForItem(StreamItem item) {
        return item.chunkRange(chunkSize).length;
    }

    private void cleanup() {
        if (!mConvertingItems.isEmpty()) {
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

    /* package */ void verifyMetadata(StreamItem item) {
        Metadata existing = mIncompleteStreams.get(item);
        // perform etag comparison to make sure file data is correct
        if (existing != null
            && item.getETag() != null
            && !item.getETag().equals(existing.eTag)) {

            Log.d(LOG_TAG, "eTag don't match, removing cached data");

            removeAllDataForItem(item);
            mIncompleteStreams.put(item, item.getMetadata());
        }
    }

    static class Metadata {
        String eTag;
        long contentLength;
        List<Integer> downloadedChunks = new ArrayList<Integer>();

        public void write(DataOutputStream dos) throws IOException {
            dos.writeLong(contentLength);
            dos.writeUTF(eTag == null ? "" : eTag);
            dos.writeInt(downloadedChunks.size());
            for (Integer index : downloadedChunks) {
                dos.writeInt(index);
            }
        }

        static Metadata read(DataInputStream dis) throws IOException {
            Metadata md = new Metadata();
            md.contentLength  = dis.readLong();
            md.eTag = dis.readUTF();
            int n = dis.readInt();
            for (int i = 0; i < n; i++) {
                md.downloadedChunks.add(dis.readInt());
            }
            return md;
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
