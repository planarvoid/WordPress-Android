package com.soundcloud.android.streaming;

import android.content.Context;
import android.util.Log;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.utils.Range;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

public class ScStreamStorage {
    private static int DEFAULT_CHUNK_SIZE = 128*1024;
    public int chunkSize;

    private File mBaseDir;
    private File mCompleteDir;
    private File mIncompleteDir;

    private Dictionary<String, Integer>  mIncompleteContentLengths;
    private Dictionary<String, ArrayList<Integer>> mIncompleteIndexes;

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

    public HashSet<Integer> getMissingChunksForItem(ScStreamItem item, Range chunkRange) {

        resetDataIfNecessary(item);
        String key = item.getURLHash();

        //If the complete file exists
        if (completeFileForKey(key).exists()) {
            return new HashSet<Integer>();
        }

        ensureMetadataIsLoadedForKey(key);
        long contentLength = getContentLengthForKey(key);

        //We have no idea about track size, so let's assume that all chunks are missing
        if (contentLength == 0) {
            return chunkRange.toIndexSet();
        }

        long lastChunk = (long) Math.ceil((double) contentLength / (double) chunkSize) - 1;
        final ArrayList<Integer> allIncompleteIndexes = mIncompleteIndexes.get(key);
        HashSet<Integer> missingIndexes = new HashSet<Integer>();
        for (int chunk = chunkRange.location; chunk < chunkRange.end(); chunk++) {
            if (!allIncompleteIndexes.contains(chunk) && chunk <= lastChunk){
                missingIndexes.add(chunk);
            }
        }
        return missingIndexes;
    }

    private void ensureMetadataIsLoadedForKey(String key) {

        //Return if already loaded
        if (mIncompleteContentLengths.get(key) != 0
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
                mIncompleteContentLengths.put(key, din.readInt());

                int count = (int) (indexFile.length() / 8) - 2;
                ArrayList<Integer> indexes = new ArrayList<Integer>();
                for (int i = 0; i < count; i++) {
                    indexes.add(din.readInt());
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
        return mIncompleteContentLengths.get(key);
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

    public byte[] getChunkData(ScStreamItem item, long chunkIndex) {
        resetDataIfNecessary(item);

        final String key = item.getURLHash();
        ensureMetadataIsLoadedForKey(key);

        long savedContentLength = contentLengthForKey(key);

        if (item.getContentLength() == 0) {
            item.setContentLength((int) savedContentLength);
        }

        byte[] data = new byte[0];
        try {
            data = incompleteDataForChunk(item, chunkIndex);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error reading incomplete chunk data " + e.getMessage());
        }
        if (data != null) return data;

        try {
            data = completeDataForChunk(item, chunkIndex);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error reading incomplete chunk data " + e.getMessage());
        }
        if (data != null) return data;

        return null;
    }

    private byte[] incompleteDataForChunk(ScStreamItem item, long chunkIndex) throws IOException {
        final String key = item.getURLHash();
        final ArrayList<Integer> indexArray = mIncompleteIndexes.get(key);

        if (indexArray != null) {
            if (!indexArray.contains(chunkIndex)) return null;
            File chunkFile = incompleteFileForKey(key);

            if (chunkFile.exists()){
                int seekToChunkOffset = (int) (indexArray.indexOf(chunkIndex) * chunkSize);
                int readLength = (int) chunkSize;
                if (chunkIndex == numberOfChunksForKey(key)){
                    readLength = (int) (contentLengthForKey(key) % chunkSize);
                }
                byte [] buffer = new byte[readLength];
                InputStream in = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(chunkFile));
                    in.read(buffer,seekToChunkOffset,readLength);
                }  finally {
                    if (in != null) {
                        in.close();
                    }
                }
                return buffer;
            }
        }
        return null;
    }

    private byte[] completeDataForChunk(ScStreamItem item, long chunkIndex) throws IOException {
        final String key = item.getURLHash();
        final File completeFile = completeFileForKey(key);
        if (completeFile.exists()){
            int seekToChunkOffset = (int) (chunkIndex * chunkSize);
                int readLength = (int) chunkSize;
                if (chunkIndex == numberOfChunksForKey(key)){
                    readLength = (int) (contentLengthForKey(key) % chunkSize);
                }
                byte [] buffer = new byte[readLength];
                InputStream in = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(completeFile));
                    in.read(buffer,seekToChunkOffset,readLength);
                }  finally {
                    if (in != null) {
                        in.close();
                    }
                }
                return buffer;
        }
        return null;
    }

    private long numberOfChunksForKey(String key) {
        return (long) Math.ceil(contentLengthForKey(key) / chunkSize);
    }


}
