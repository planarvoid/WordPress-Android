package com.soundcloud.android.playback.streaming;

import static com.soundcloud.android.utils.IOUtils.mkdirs;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.FiletimeComparator;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
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

    public static final String STREAM_CACHE_SIZE = "streamCacheSize";

    public static final int DEFAULT_CHUNK_SIZE = 128 * 1024; // 128k
    public static final int DEFAULT_PCT_OF_FREE_SPACE = 10;  // use 10% of sd card

    private static final int CLEANUP_INTERVAL = 20;

    public static final String INDEX_EXTENSION = "index";
    public static final String CHUNKS_EXTENSION = "chunks";

    public final int chunkSize;

    private Context context;
    private File baseDir, completeDir, incompleteDir;

    private final Map<String, StreamItem> items = new HashMap<>();
    private final Set<String> convertingUrls = new HashSet<>();

    private final int cleanupInterval;
    private ApplicationProperties applicationProperties;

    public StreamStorage(Context context, File basedir) {
        this(context, basedir, new ApplicationProperties(context.getResources()), DEFAULT_CHUNK_SIZE, CLEANUP_INTERVAL);
    }

    @VisibleForTesting
    protected StreamStorage(Context context, File baseDir, ApplicationProperties applicationProperties,
                            int chunkSize, int cleanupInterval) {
        this.context = context;
        this.baseDir = baseDir;
        incompleteDir = new File(baseDir, "Incomplete");
        completeDir = new File(baseDir, "Complete");
        this.cleanupInterval = cleanupInterval;
        this.applicationProperties = applicationProperties;
        mkdirs(incompleteDir);
        mkdirs(completeDir);

        this.chunkSize = chunkSize;
    }

    public synchronized boolean storeMetadata(StreamItem item) {
        verifyMetadata(item);

        items.put(item.urlHash, item);
        try {
            File indexFile = incompleteIndexFileForUrl(item.streamItemUrl());
            if (indexFile.exists() && !indexFile.delete()) {
                Log.w(LOG_TAG, "could not delete " + indexFile);
            }
            item.toIndexFile(indexFile);
            return true;
        } catch (IOException e) {
            if (IOUtils.isSDCardAvailable()) {
                Log.e(LOG_TAG, "Error storing index data ", e);
            }
            return false;
        }
    }

    public synchronized
    @NotNull
    StreamItem getMetadata(String url) {
        String hashed = StreamItem.urlHash(url);
        if (!items.containsKey(hashed)) {
            items.put(hashed, readMetadata(url));
        }
        return items.get(hashed);
    }

    public synchronized boolean removeMetadata(String url) {
        return items.remove(StreamItem.urlHash(url)) != null;
    }

    public ByteBuffer fetchStoredDataForUrl(String url, Range range) throws IOException {
        StreamItem item = getMetadata(url);

        Range actualRange = range;
        if (item.getContentLength() > 0) {
            actualRange = range.intersection(Range.from(0, item.getContentLength()));
            if (actualRange == null) {
                throw new IOException("Invalid range, outside content length. Requested range " + range + " from item " + item);
            }
        }
        Range chunkRange = actualRange.chunkRange(chunkSize);
        if (chunkRange.length == 1 && actualRange.length == chunkSize) {
            // optimise for most common case
            return getChunkData(url, chunkRange.start);
        } else {
            ByteBuffer data = ByteBuffer.allocate(chunkRange.length * chunkSize);
            // read all the chunks we need
            for (int index : chunkRange) {
                data.put(getChunkData(url, index));
            }
            // and adjust offsets
            data.position(actualRange.start % chunkSize);
            data.limit(actualRange.start % chunkSize + actualRange.length);
            return data.asReadOnlyBuffer();
        }
    }

    public boolean storeData(final URL url, ByteBuffer data, final int chunkIndex) throws IOException {
        return storeData(url.toString(), data, chunkIndex);
    }

    /**
     * @param data       the data to store
     * @param chunkIndex the chunk index the data belongs to
     * @param url        the url for the data
     * @return if the data was set
     * @throws java.io.IOException IO error
     */
    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public boolean storeData(final String url, ByteBuffer data, final int chunkIndex) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("buffer is null");
        } else if (data.limit() == 0) {
            Log.w(LOG_TAG, "Not Storing Data. Content Length is Zero.");
            return false;
        }
        // Do not add to complete files
        else if (completeFileForUrl(url).exists()) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "complete file exists, not adding data");
            }
            return false;
        } else if (!IOUtils.isSDCardAvailable()) {
            Log.w(LOG_TAG, "storage not available, not adding data");
            return false;
        }

        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, String.format("Storing %d bytes at index %d for url %s",
                    data.limit(), chunkIndex, url));
        }

        final StreamItem item = getMetadata(url);
        // return if it's already in store
        if (item.downloadedChunks.contains(chunkIndex)) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, String.format("already got chunk"));
            }
            return false;
        }

        // Prepare incomplete file
        final File incompleteFile = incompleteFileForUrl(url);
        appendToFile(data, incompleteFile);

        // Add Index and save it
        item.downloadedChunks.add(chunkIndex);
        storeMetadata(item);

        if (item.downloadedChunks.size() == item.numberOfChunks(chunkSize)) {
            new CompleteFileTask(item.getContentLength(), item.etag(), chunkSize, item.downloadedChunks) {
                @Override
                protected void onPreExecute() {
                    convertingUrls.add(url);
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    if (success) {
                        removeIncompleteDataForItem(url);
                        new UpdateMetadataTask(context.getContentResolver()).execute(item);
                    } else {
                        removeAllDataForItem(url);
                    }
                    convertingUrls.remove(url);
                }
            }.execute(incompleteFile, completeFileForUrl(url));
        }

        if (cleanupInterval > 0) {
            //Update the number of writes, cleanup if necessary
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final int currentCount = prefs.getInt(Consts.PrefKeys.STREAMING_WRITES_SINCE_CLEANUP, 0) + 1;

            prefs.edit().putInt(Consts.PrefKeys.STREAMING_WRITES_SINCE_CLEANUP, currentCount).apply();

            if (currentCount >= cleanupInterval) {
                if (cleanup(calculateUsableSpace())) {
                    if (applicationProperties.isDevBuildRunningOnDevice()) {
                        // print file stats again
                        calculateUsableSpace();
                    }
                }
            }
        }
        return true;
    }

    public ByteBuffer getChunkData(URL url, int chunkIndex) throws IOException {
        return getChunkData(url.toString(), chunkIndex);
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
            if (item.getContentLength() == 0) {
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

    /* package */ File completeFileForUrl(String url) {
        return new File(completeDir, StreamItem.urlHash(url));
    }

    /* package */ File incompleteFileForUrl(String url) {
        return new File(incompleteDir, StreamItem.urlHash(url) + "." + CHUNKS_EXTENSION);
    }

    /* package */ File incompleteIndexFileForUrl(String url) {
        return new File(incompleteDir, StreamItem.urlHash(url) + "." + INDEX_EXTENSION);
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
        fc.close();
        return true;
    }

    private
    @NotNull
    StreamItem readMetadata(String url) {
        File f = incompleteIndexFileForUrl(url);
        if (f.exists()) {
            try {
                return StreamItem.fromIndexFile(f);
            } catch (IOException e) {
                Log.e(LOG_TAG, "could not read metadata, deleting", e);
                removeAllDataForItem(url);
                return new StreamItem(url);
            }
        } else if (completeFileForUrl(url).exists()) {
            return new StreamItem(url, completeFileForUrl(url));
        } else {
            // we don't have anything yet
            return new StreamItem(url);
        }
    }

    private void removeAllDataForItem(String url) {
        Log.w(LOG_TAG, "removing all data for " + url);
        removeCompleteDataForItem(url);
        removeIncompleteDataForItem(url);
    }

    private synchronized boolean removeIncompleteDataForItem(String url) {
        final File incompleteFile = incompleteFileForUrl(url);
        final File indexFile = incompleteIndexFileForUrl(url);
        boolean fileDeleted = true, indexDeleted = true;
        if (incompleteFile.exists()) {
            fileDeleted = incompleteFile.delete();
        }
        if (indexFile.exists()) {
            indexDeleted = indexFile.delete();
        }
        items.remove(StreamItem.urlHash(url));
        return fileDeleted && indexDeleted;
    }

    private boolean removeCompleteDataForItem(String url) {
        final File completeFile = completeFileForUrl(url);
        return completeFile.exists() && completeFile.delete();
    }

    /* package */ ByteBuffer incompleteDataForChunk(String url, int chunkIndex) throws IOException {
        StreamItem item = getMetadata(url);
        if (!item.downloadedChunks.contains(chunkIndex)) {
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
        if (!f.exists()) {
            throw new FileNotFoundException("file " + f + " does not exist");
        }
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

    /**
     * @return the usable space for caching
     */
    /* package */ long calculateUsableSpace() {
        long result = getUsedSpace();
        long spaceLeft = getSpaceLeft();
        long totalSpace = getTotalSpace();

        int percentageOfExternal = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getInt(STREAM_CACHE_SIZE, DEFAULT_PCT_OF_FREE_SPACE);

        if (percentageOfExternal < 0) {
            percentageOfExternal = 0;
        }

        if (percentageOfExternal > 100) {
            percentageOfExternal = 100;
        }

        result = IOUtils.getUsableSpace(result, spaceLeft, totalSpace, percentageOfExternal / 100.0);

        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, String.format("[File Metrics] %.1f mb used, %.1f mb free, %.1f mb usable for caching",
                    result / (1024d * 1024d), spaceLeft / (1024d * 1024d), result / (1024d * 1024d)));
        }

        return result;
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private synchronized boolean cleanup(long usableSpace) {
        if (!convertingUrls.isEmpty()) {

            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Not doing storage cleanup, conversion is going on");
            }
            return false;
        }
        // reset counter
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(Consts.PrefKeys.STREAMING_WRITES_SINCE_CLEANUP, 0)
                .apply();

        final long spaceToClean = getUsedSpace() - usableSpace;
        if (spaceToClean > 0) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, String.format("performing cleanup, need to free %.1f mb", spaceToClean / (1024d * 1024d)));
            }
            final List<File> files = allFiles(new FiletimeComparator(true));
            long cleanedSpace = 0;
            for (File f : files) {
                final long length = f.length();
                if (f.delete()) {
                    cleanedSpace += length;
                    if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                        Log.d(LOG_TAG, "deleted " + f);
                    }

                    String name = f.getName();
                    if (name.endsWith(CHUNKS_EXTENSION)) {
                        String hash = name.substring(0, name.indexOf('.'));
                        File indexFile = new File(incompleteDir, hash + "." + INDEX_EXTENSION);
                        if (indexFile.exists() && indexFile.delete()) {
                            Log.d(LOG_TAG, "deleted " + indexFile);
                            // invalidate cache
                            items.remove(hash);
                        }
                    }
                } else {
                    Log.w(LOG_TAG, "could not delete " + f);
                }
                if (cleanedSpace >= spaceToClean) {
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private List<File> allFiles(Comparator<File> comparator) {
        final List<File> files = new ArrayList<File>();
        File[] chunks = IOUtils.nullSafeListFiles(incompleteDir, extension(CHUNKS_EXTENSION));
        if (chunks.length > 0) {
            files.addAll(Arrays.asList(chunks));
        }

        File[] complete = IOUtils.nullSafeListFiles(completeDir, null);
        if (complete.length > 0) {
            files.addAll(Arrays.asList(complete));
        }

        if (comparator != null) {
            Collections.sort(files, comparator);
        }
        return files;
    }


    /* package */ long getUsedSpace() {
        long currentlyUsedSpace = 0;
        File[] complete = IOUtils.nullSafeListFiles(completeDir, null);
        for (File f : complete) {
            currentlyUsedSpace += f.length();
        }

        File[] incomplete = IOUtils.nullSafeListFiles(incompleteDir, null);
        for (File f : incomplete) {
            currentlyUsedSpace += f.length();
        }
        return currentlyUsedSpace;
    }

    /* package */ long getSpaceLeft() {
        return IOUtils.getSpaceLeft(baseDir);
    }

    /* package */ long getTotalSpace() {
        return IOUtils.getTotalSpace(baseDir);
    }

    /* package */ void verifyMetadata(StreamItem item) {
        StreamItem existing = items.get(item.urlHash);
        if (existing != null &&
                existing.etag() != null &&
                !existing.etag().equals(item.etag())) {

            Log.w(LOG_TAG, "eTag don't match, removing cached data");
            removeAllDataForItem(item.streamItemUrl());
        }
    }

    static FilenameFilter extension(final String ext) {
        return new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith("." + ext);
            }
        };
    }
}
