package com.soundcloud.android.streaming;

import android.content.Context;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.utils.Range;

import java.io.*;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

public class ScStreamStorage {
    private static long DEFAULT_CHUNK_SIZE = 128*1024;
    public long chunkSize;

    private File mBaseDir;
    private File mCompleteDir;
    private File mIncompleteDir;

    private Dictionary<String, Long>  mIncompleteContentLengths;
    private Dictionary<String, HashSet<Long>> mIncompleteIndexes;

    public void ScStreamStorage(Context context){
        mBaseDir = FileCache.getCacheDir(context);
        mIncompleteDir = new File(mBaseDir,"Incomplete");
        mCompleteDir = new File(mBaseDir,"Incomplete");

        if (!mIncompleteDir.exists()){
            mIncompleteDir.mkdirs();
        }

        if (!mCompleteDir.exists()){
            mCompleteDir.mkdirs();
        }

        chunkSize = DEFAULT_CHUNK_SIZE;
    }

    public Set<Long> getMissingChunksForItem(ScStreamItem item, Range chunkRange) {

        resetDataIfNecessary(item);
        String key = item.getURLHash();

        //If the complete file exists
        if (completeFileForKey(key).exists()) {
            return new HashSet<Long>();
        }

        ensureMetadataIsLoadedForKey(key);
        long contentLength = getContentLengthForKey(key);

        //We have no idea about track size, so let's assume that all chunks are missing
        if (contentLength == 0) {
            return chunkRange.toIndexSet();
        }

        long lastChunk = (long) Math.ceil((double) contentLength / (double) chunkSize) - 1;
        final HashSet<Long> allIncompleteIndexes = mIncompleteIndexes.get(key);
        HashSet<Long> missingIndexes = new HashSet<Long>();
        for (Long chunk = chunkRange.start; chunk < chunkRange.end(); chunk++) {
            if (!allIncompleteIndexes.contains(chunk) && chunk <= lastChunk){
                missingIndexes.add(chunk);
            }
        }
        return missingIndexes;
    }

    private void ensureMetadataIsLoadedForKey(String key) {

        //Return if already loaded
        if (mIncompleteContentLengths.get(key) != null
                && mIncompleteIndexes.get(key) != null) {
            return;
        }

        //If the complete file exists
        if (completeFileForKey(key).exists()) {
            return;
        }

        File indexFile = new File(incompleteFileForKey(key) + "_index");
        if (indexFile.exists()) {
            try {
                DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
                mIncompleteContentLengths.put(key, din.readLong());

                int count = (int) (indexFile.length() / 8) - 2;
                HashSet<Long> indexes = new HashSet<Long>();
                for (int i = 0; i < count; i++) {
                    indexes.add(din.readLong());
                }
                mIncompleteIndexes.put(key, indexes);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private long contentLengthForKey(String key) {
        final File completeFile = completeFileForKey(key);
        if (completeFile.exists()) return completeFile.length();
        if (mIncompleteContentLengths.get(key) != null) return mIncompleteContentLengths.get(key);
        return 0;
    }

    private File completeFileForKey(String key){
        return new File(mCompleteDir,key);

    }

    private File incompleteFileForKey(String key){
        return new File(mIncompleteDir,key);

    }

    private boolean resetDataIfNecessary(ScStreamItem item) {
        String key = item.getURLHash();
        if (item.getContentLength() != 0 &&
                item.getContentLength() != getContentLengthForKey(key)) {
            removeAllDataForKey(key);
            return true;
        }
        return false;
    }

    private long getContentLengthForKey(String key) {
        return 0;
    }

    private void removeAllDataForKey(String key) {
         if (key.length() == 0) return;
         /*
         remove all existing data from file system

         https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamStorage.m#L386
          */

        removeIncompleteDataForKey(key);
    }

    private void removeIncompleteDataForKey(String key){
         if (key.length() == 0) return;
        /*
        remove all incomplete data from file system

         https://github.com/nxtbgthng/SoundCloudStreaming/blob/master/Sources/SoundCloudStreaming/SCStreamStorage.m#L395
          */
    }
}
