package com.soundcloud.android.streaming;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;

import java.io.*;
import java.util.*;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

public class StreamStorage {
    private static final int DEFAULT_CHUNK_SIZE = 128*1024;
    private static final int CLEANUP_INTERVAL = 20;

    public final int chunkSize;
    private Context mContext;

    private File mBaseDir, mCompleteDir, mIncompleteDir;
    private long mUsedSpace, mSpaceLeft;

    private Map<String, Long>  mIncompleteContentLengths;
    private Map<String, List<Integer>> mIncompleteIndexes;

    private ArrayList<String> mConvertingKeys;

    public StreamStorage(Context context, File basedir) {
        this(context,basedir,DEFAULT_CHUNK_SIZE);
    }

    public StreamStorage(Context context, File basedir, int chunkSize) {
        mContext = context;
        mBaseDir = basedir;
        mIncompleteDir = new File(mBaseDir,"Incomplete");
        mCompleteDir = new File(mBaseDir,"Complete");

        mIncompleteContentLengths = new HashMap<String, Long>();
        mIncompleteIndexes = new HashMap<String, List<Integer>>();

        mkdirs(mIncompleteDir);
        mkdirs(mCompleteDir);

        this.chunkSize = chunkSize;

        mConvertingKeys = new ArrayList<String>();
    }

    public boolean setData(byte[] data, int chunkIndex, StreamItem item) {
        if (data == null) return false;
        if (item.getContentLength() == 0) {
            Log.d(getClass().getSimpleName(), "Not Storing Data. Content Length is Zero.");
            return false;
        }

        final String key = item.getURLHash();
        setContentLength(key, item.getContentLength());

        //Do not add to complete files
        if (completeFileForKey(key).exists()) return false;

        //Prepare incomplete file
        ensureMetadataIsLoadedForKey(item);

        List<Integer> indexes = mIncompleteIndexes.get(key);

        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            mIncompleteIndexes.put(key, indexes);
        }

        //return if it's already in store
        if (indexes.contains(chunkIndex)) return false;

        final File incompleteFile = incompleteFileForKey(key);
        // always write chunk size even if it isn't a complete chunk (for offsetting I think)
        if (data.length != chunkSize){
            data = Arrays.copyOf(data, chunkSize);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(incompleteFile, true);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            Log.i(getClass().getSimpleName(), "Error storing chunk data ", e);
            return false;
        }

        //Add Index and save it
        indexes.add(chunkIndex);

        try {
            writeIndex(item, indexes);
        } catch (IOException e) {
            Log.i(getClass().getSimpleName(), "Error storing index data ", e);
            return false;
        }


        if (indexes.size() == numberOfChunksForKey(item)){
            mConvertingKeys.add(key);
            new ConvertFileToComplete(key, indexes).execute(incompleteFile, completeFileForKey(key));
        }

        //Update the number of writes, cleanup if necessary
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final int currentCount = prefs.getInt("streamingWritesSinceCleanup", 0) + 1;
        prefs.edit().putInt("streamingWritesSinceCleanup",currentCount).commit();

//        if (currentCount >= CLEANUP_INTERVAL) {
//            calculateFileMetrics();
//            cleanup();
//        }
        return true;
    }

    private void cleanup() {
        if (mConvertingKeys.size() > 0) {
            Log.d(getClass().getSimpleName(), "Not doing storage cleanup, conversion is going on");
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt("streamingWritesSinceCleanup",0).commit();

        if (mUsedSpace <= Consts.TRACK_MAX_CACHE && mSpaceLeft >= Consts.TRACK_CACHE_MIN_FREE_SPACE) return;

        ArrayList<File> files = new ArrayList<File>();
        files.addAll(Arrays.asList(mIncompleteDir.listFiles()));
        files.addAll(Arrays.asList(mCompleteDir.listFiles()));
        Collections.sort(files,FileLastModifiedComparator.INSTANCE);

        final long spaceToClean = Math.max(mUsedSpace - Consts.TRACK_MAX_CACHE, Consts.TRACK_CACHE_MIN_FREE_SPACE - mSpaceLeft);
        int i = 0;
        long cleanedSpace = 0;
        while (i < files.size() && cleanedSpace < spaceToClean){
            final File f = files.get(i);
            final File parent = f.getParentFile();
            cleanedSpace += f.length();
            f.delete();
            if (parent.equals(mIncompleteDir)){
                File indexFile = new File(f.getAbsolutePath() + "_index");
                if (indexFile.exists()){
                    cleanedSpace += indexFile.length();
                    indexFile.delete();
                }
            }
        }
    }

    /* package */ void calculateFileMetrics() {

        StatFs fs = new StatFs(mBaseDir.getAbsolutePath());
        mSpaceLeft = fs.getBlockSize() * fs.getAvailableBlocks();

        long currentlyUsedSpace = 0;
        for (File f : mCompleteDir.listFiles()){
            currentlyUsedSpace += f.length();
        }
        for (File f : mIncompleteDir.listFiles()){
            currentlyUsedSpace += f.length();
        }
        mUsedSpace = currentlyUsedSpace;

        Log.d(getClass().getSimpleName(), "[File Metrics] used: " + mUsedSpace + " , free: " + mSpaceLeft);
    }

    /* package */ void setContentLength(String key, long contentLength) {
        if (TextUtils.isEmpty(key)){
            Log.e(getClass().getSimpleName(),"No key provided for setting content length");
        }
        if (contentLength != contentLengthForKey(key)) {
            if (contentLengthForKey(key) != 0) {
                removeAllDataForKey(key);
            }
            mIncompleteContentLengths.put(key, contentLength);
        }
    }

    public Set<Integer> getMissingChunksForItem(StreamItem item, Range chunkRange) {
        resetDataIfNecessary(item);
        String key = item.getURLHash();

        //If the complete file exists
        if (completeFileForKey(key).exists()) {
            return Collections.emptySet();
        }
        ensureMetadataIsLoadedForKey(item);
        long contentLength = getContentLengthForKey(key);

        //We have no idea about track size, so let's assume that all chunks are missing
        if (contentLength == 0) {
            return chunkRange.toIndexSet();
        }

        long lastChunk = (long) Math.ceil((double) contentLength / (double) chunkSize) - 1;
        final List<Integer> allIncompleteIndexes = mIncompleteIndexes.get(key);
        HashSet<Integer> missingIndexes = new HashSet<Integer>();
        for (int chunk = chunkRange.location; chunk < chunkRange.end(); chunk++) {
            if (!allIncompleteIndexes.contains(chunk) && chunk <= lastChunk){
                missingIndexes.add(chunk);
            }
        }
        return missingIndexes;
    }

    private void ensureMetadataIsLoadedForKey(StreamItem item) {
        if (!isMetaDataLoaded(item) && !completeFileForKey(item).exists()) {
            readIndex(item);
        }
    }

    /* package */ void writeIndex(StreamItem item, List<Integer> indexes) throws IOException {
        File indexFile = incompleteIndexFileForKey(item.getURLHash());
        if (indexFile.exists()) {
            indexFile.delete();
        }
        DataOutputStream din = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
        din.writeLong(item.getContentLength());
        for (Integer index : indexes) {
            din.writeInt(index);
        }
        din.close();
    }

    /* package */ void readIndex(StreamItem item) {
        String key = item.getURLHash();
        File indexFile = incompleteIndexFileForKey(key);
        if (indexFile.exists()) {
            try {
                DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
                mIncompleteContentLengths.put(key, din.readLong());

                int count = (int) (indexFile.length() / 4) - 2;
                ArrayList<Integer> indexes = new ArrayList<Integer>();
                for (int i = 0; i < count; i++) {
                    indexes.add(din.readInt());
                }
                mIncompleteIndexes.put(key, indexes);

            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /* package */ boolean isMetaDataLoaded(StreamItem key) {
        return mIncompleteContentLengths.get(key.getURLHash()) != null && mIncompleteIndexes.get(key.getURLHash()) != null;
    }

    private long contentLengthForKey(StreamItem key) {
        return contentLengthForKey(key.getURLHash());
    }

    private long contentLengthForKey(String key) {
        final File completeFile = completeFileForKey(key);
        if (completeFile.exists()) return completeFile.length();
        return mIncompleteContentLengths.containsKey(key) ? mIncompleteContentLengths.get(key) : 0;
    }

    private File completeFileForKey(StreamItem key){
        return completeFileForKey(key.getURLHash());

    }

    /* package */ File completeFileForKey(String key){
        return new File(mCompleteDir,key);

    }

    /* package */ File incompleteFileForKey(String key){
        return new File(mIncompleteDir,key);
    }

    private File incompleteIndexFileForKey(String key){
        return new File(mIncompleteDir,key+"_index");

    }

    private boolean resetDataIfNecessary(StreamItem item) {
        final String key = item.getURLHash();
        if (item.getContentLength() != 0 &&
                item.getContentLength() != getContentLengthForKey(key)) {
            removeAllDataForKey(key);
            return true;
        }
        return false;
    }

    private long getContentLengthForKey(String key) {
        if (completeFileForKey(key).exists()) {
            return completeFileForKey(key).length();
        } else {
            return mIncompleteContentLengths.containsKey(key) ? mIncompleteContentLengths.get(key) : -1;
        }
    }

    private void removeAllDataForKey(String key) {
        if (key.length() != 0) {
            removeCompleteDataForKey(key);
            removeIncompleteDataForKey(key);
        }
    }

    private void removeIncompleteDataForKey(String key){
         if (key.length() == 0) return;

        final File incompleteFile = incompleteFileForKey(key);
        final File indexFile = incompleteIndexFileForKey(key);

        if (incompleteFile.exists()) incompleteFile.delete();
        if (indexFile.exists()) indexFile.delete();

        mIncompleteIndexes.remove(key);
        mIncompleteContentLengths.remove(key);
    }

    private void removeCompleteDataForKey(String key){
         if (key.length() == 0) return;

        final File completeFile = completeFileForKey(key);
        if (completeFile.exists()) completeFile.delete();
    }

    public byte[] getChunkData(StreamItem item, int chunkIndex) {
        resetDataIfNecessary(item);

        final String key = item.getURLHash();
        ensureMetadataIsLoadedForKey(item);

        long savedContentLength = contentLengthForKey(key);

        if (item.getContentLength() == 0) {
            item.setContentLength((int) savedContentLength);
        }

        byte[] data = null;

        try {
            data = incompleteDataForChunk(item, chunkIndex);
        } catch (IOException e) {
            Log.i(getClass().getSimpleName(), "Error retrieving chunk data: ", e);
        }
        if (data != null) return data;

        try{
            data = completeDataForChunk(item, chunkIndex);
        } catch (IOException e) {
            Log.i(getClass().getSimpleName(), "Error retrieving chunk data: ", e);
        }
        if (data != null) return data;

        return null;
    }

    /* package */ byte[] incompleteDataForChunk(StreamItem item, int chunkIndex) throws IOException {
        final String key = item.getURLHash();
        final List<Integer> indexArray = mIncompleteIndexes.get(key);

        if (indexArray != null) {
            if (!indexArray.contains(chunkIndex)) return null;
            File chunkFile = incompleteFileForKey(key);

            if (chunkFile.exists()){
                int seekToChunkOffset = indexArray.indexOf(chunkIndex) * chunkSize;
                int readLength = chunkSize;
                if (chunkIndex == numberOfChunksForKey(item)){
                    readLength = (int) (contentLengthForKey(key) % chunkSize);
                }
                byte [] buffer = new byte[readLength];
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(chunkFile,"r");
                    raf.seek(seekToChunkOffset);
                    raf.readFully(buffer,0,readLength);
                }  finally {
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
        final String key = item.getURLHash();
        final File completeFile = completeFileForKey(key);

        if (completeFile.exists()) {
            final long totalChunks = numberOfChunksForKey(item);
            if (chunkIndex >= totalChunks) {
                Log.e(getClass().getSimpleName(), "Requested invalid chunk index. Requested index " + chunkIndex + " of size " + totalChunks);
                return null;
            }
            int seekToChunkOffset = (int) (chunkIndex * chunkSize);
            int readLength = chunkSize;
            if (chunkIndex == totalChunks - 1) {
                readLength = (int) (contentLengthForKey(key) % chunkSize);
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

    /* package */ long numberOfChunksForKey(StreamItem item) {
        return (long) Math.ceil(((float ) contentLengthForKey(item)) / ((float) chunkSize));
    }

    private static class FileLastModifiedComparator implements Comparator<File> {
        static FileLastModifiedComparator INSTANCE = new FileLastModifiedComparator();
        @Override
        public int compare(File f1, File f2) {
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
    }

    private class ConvertFileToComplete extends AsyncTask<File,Integer,Boolean> {
        private String mKey;
        private long mContentLength;
        private List<Integer> mIndexes;
        public ConvertFileToComplete(String key, List<Integer> indexes){
            mKey = key;
            mIndexes = indexes;
            mContentLength = contentLengthForKey(key);
        }

        @Override
        protected Boolean doInBackground(File... params) {
            File chunkFile = params[0];
            File completeFile = params[1];

            if (completeFile.exists()){
                Log.e(getClass().getSimpleName(),"Complete file exists at path " + completeFile.getAbsolutePath());
                return false;
            }

            FileOutputStream fos = null;
            RandomAccessFile raf = null;

            try {
                fos = new FileOutputStream(completeFile);
                raf = new RandomAccessFile(chunkFile,"r");

                completeFile.createNewFile();
                byte[] buffer = new byte[chunkSize];

                for (int chunkNumber = 0; chunkNumber < mIndexes.size(); chunkNumber++) {
                    int offset = chunkSize * mIndexes.indexOf(chunkNumber);
                    raf.seek(offset);
                    raf.readFully(buffer, 0, chunkSize);

                    if (chunkNumber == mIndexes.size() - 1) {
                        fos.write(buffer, 0, (int) (mContentLength % chunkSize));
                    } else {
                        fos.write(buffer);
                    }

                }
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "IO error during complete file creation");
                e.printStackTrace();
                return false;
            } finally {
                if (raf != null){
                    try {raf.close();} catch (IOException e) {e.printStackTrace();}
                }
                if (fos != null){
                    try {fos.close();} catch (IOException e) {e.printStackTrace();}
                }
            }
            onPostExecute(true);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            removeIncompleteDataForKey(mKey);
            mConvertingKeys.remove(mKey);
        }
    }

    public Map<String, List<Integer>> getIncompleteIndexes() {
        return mIncompleteIndexes;
    }

    public Map<String, Long> getIncompleteContentLengths() {
        return mIncompleteContentLengths;
    }
}
