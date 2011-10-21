package com.soundcloud.android.streaming;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import com.soundcloud.android.Consts;

import java.io.*;
import java.util.*;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

/** @noinspection ResultOfMethodCallIgnored*/
public class StreamStorage {
    private static final int DEFAULT_CHUNK_SIZE = 128*1024;
    private static final int CLEANUP_INTERVAL = 20;

    public final int chunkSize;
    private Context mContext;

    private File mBaseDir, mCompleteDir, mIncompleteDir;
    private long mUsedSpace, mSpaceLeft;

    private Map<StreamItem, Long>  mIncompleteContentLengths = new HashMap<StreamItem, Long>();
    private Map<StreamItem, List<Integer>> mIncompleteIndexes =  new HashMap<StreamItem, List<Integer>>();
    private List<StreamItem> mConvertingItems = new ArrayList<StreamItem>();

    public StreamStorage(Context context, File basedir) {
        this(context,basedir,DEFAULT_CHUNK_SIZE);
    }

    public StreamStorage(Context context, File basedir, int chunkSize) {
        mContext = context;
        mBaseDir = basedir;
        mIncompleteDir = new File(mBaseDir,"Incomplete");
        mCompleteDir = new File(mBaseDir,"Complete");

        mkdirs(mIncompleteDir);
        mkdirs(mCompleteDir);

        this.chunkSize = chunkSize;
    }

    public boolean setData(byte[] data, int chunkIndex, final StreamItem item) {
        if (data == null) return false;
        if (item.getContentLength() == 0) {
            Log.d(getClass().getSimpleName(), "Not Storing Data. Content Length is Zero.");
            return false;
        }

        setContentLength(item, item.getContentLength());

        //Do not add to complete files
        if (completeFileForItem(item).exists()) return false;

        //Prepare incomplete file
        ensureMetadataIsLoadedForItem(item);

        List<Integer> indexes = mIncompleteIndexes.get(item);

        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            mIncompleteIndexes.put(item, indexes);
        }

        //return if it's already in store
        if (indexes.contains(chunkIndex)) return false;

        final File incompleteFile = incompleteFileForItem(item);
        // always write chunk size even if it isn't a complete chunk (for offsetting I think)
        if (data.length != chunkSize){
            data = Arrays.copyOf(data, chunkSize);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(incompleteFile, true);
            fos.write(data);
        } catch (IOException e) {
            Log.i(getClass().getSimpleName(), "Error storing chunk data ", e);
            return false;
        } finally {
            if (fos!=null) try {
                fos.close();
            } catch (IOException ignored) {
            }
        }

        //Add Index and save it
        indexes.add(chunkIndex);

        try {
            writeIndex(item, indexes);
        } catch (IOException e) {
            Log.i(getClass().getSimpleName(), "Error storing index data ", e);
            return false;
        }


        if (indexes.size() == numberOfChunksForItem(item)){
            mConvertingItems.add(item);
            new ConvertFileToComplete(item, indexes).execute(incompleteFile, completeFileForItem(item));
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
        if (mConvertingItems.size() > 0) {
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

    /* package */ void setContentLength(StreamItem item, long contentLength) {

        if (contentLength != contentLengthForItem(item)) {
            if (contentLengthForItem(item) != 0) {
                removeAllDataForItem(item);
            }
            mIncompleteContentLengths.put(item, contentLength);
        }
    }

    public Set<Integer> getMissingChunksForItem(StreamItem item, Range chunkRange) {
        resetDataIfNecessary(item);

        //If the complete file exists
        if (completeFileForItem(item).exists()) {
            return Collections.emptySet();
        }
        ensureMetadataIsLoadedForItem(item);
        long contentLength = getContentLengthForItem(item);

        //We have no idea about track size, so let's assume that all chunks are missing
        if (contentLength == 0) {
            return chunkRange.toIndexSet();
        }

        long lastChunk = (long) Math.ceil((double) contentLength / (double) chunkSize) - 1;
        final List<Integer> allIncompleteIndexes = mIncompleteIndexes.get(item);
        HashSet<Integer> missingIndexes = new HashSet<Integer>();
        for (int chunk = chunkRange.location; chunk < chunkRange.end(); chunk++) {
            if (!allIncompleteIndexes.contains(chunk) && chunk <= lastChunk){
                missingIndexes.add(chunk);
            }
        }
        return missingIndexes;
    }

    private void ensureMetadataIsLoadedForItem(StreamItem item) {
        if (!isMetaDataLoaded(item) && !completeFileForItem(item).exists()) {
            readIndex(item);
        }
    }

    /* package */ void writeIndex(StreamItem item, List<Integer> indexes) throws IOException {
        File indexFile = incompleteIndexFileForItem(item);
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

            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /* package */ boolean isMetaDataLoaded(StreamItem key) {
        return mIncompleteContentLengths.get(key) != null && mIncompleteIndexes.get(key) != null;
    }


    private long contentLengthForItem(StreamItem key) {
        final File completeFile = completeFileForItem(key);
        if (completeFile.exists()) return completeFile.length();
        return mIncompleteContentLengths.containsKey(key) ? mIncompleteContentLengths.get(key) : 0;
    }

    /* package */ File completeFileForItem(StreamItem key) {
        return new File(mCompleteDir, key.getURLHash());
    }

    /* package */ File incompleteFileForItem(StreamItem key){
        return new File(mIncompleteDir, key.getURLHash());
    }

    private File incompleteIndexFileForItem(StreamItem item){
        return new File(mIncompleteDir,item.getURLHash()+"_index");

    }

    private boolean resetDataIfNecessary(StreamItem item) {
        if (item.getContentLength() != 0 &&
                item.getContentLength() != getContentLengthForItem(item)) {
            removeAllDataForItem(item);
            return true;
        }
        return false;
    }

    private long getContentLengthForItem(StreamItem key) {
        if (completeFileForItem(key).exists()) {
            return completeFileForItem(key).length();
        } else {
            return mIncompleteContentLengths.containsKey(key) ? mIncompleteContentLengths.get(key) : -1;
        }
    }

    private void removeAllDataForItem(StreamItem item) {
        removeCompleteDataForItem(item);
        removeIncompleteDataForItem(item);
    }

    private void removeIncompleteDataForItem(StreamItem key) {
        final File incompleteFile = incompleteFileForItem(key);
        final File indexFile = incompleteIndexFileForItem(key);

        if (incompleteFile.exists()) incompleteFile.delete();
        if (indexFile.exists()) indexFile.delete();

        mIncompleteIndexes.remove(key);
        mIncompleteContentLengths.remove(key);
    }

    private void removeCompleteDataForItem(StreamItem item) {
        final File completeFile = completeFileForItem(item);
        if (completeFile.exists()) completeFile.delete();
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
        final List<Integer> indexArray = mIncompleteIndexes.get(item);

        if (indexArray != null) {
            if (!indexArray.contains(chunkIndex)) return null;
            File chunkFile = incompleteFileForItem(item);

            if (chunkFile.exists()){
                int seekToChunkOffset = indexArray.indexOf(chunkIndex) * chunkSize;
                int readLength = chunkSize;
                if (chunkIndex == numberOfChunksForItem(item)){
                    readLength = (int) (contentLengthForItem(item) % chunkSize);
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
        final File completeFile = completeFileForItem(item);

        if (completeFile.exists()) {
            final long totalChunks = numberOfChunksForItem(item);
            if (chunkIndex >= totalChunks) {
                Log.e(getClass().getSimpleName(), "Requested invalid chunk index. Requested index " + chunkIndex + " of size " + totalChunks);
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
        return (long) Math.ceil(((float ) contentLengthForItem(item)) / ((float) chunkSize));
    }

    private static class FileLastModifiedComparator implements Comparator<File> {
        static FileLastModifiedComparator INSTANCE = new FileLastModifiedComparator();
        @Override
        public int compare(File f1, File f2) {
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        }
    }

    private class ConvertFileToComplete extends AsyncTask<File,Integer,Boolean> {
        private StreamItem mItem;
        private long mContentLength;
        private List<Integer> mIndexes;
        public ConvertFileToComplete(StreamItem item, List<Integer> indexes){
            mItem = item;
            mIndexes = indexes;
            mContentLength = contentLengthForItem(item);
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
                return false;
            } finally {
                if (raf != null) try { raf.close(); } catch (IOException ignored) {}
                if (fos != null) try { fos.close(); } catch (IOException ignored) {}
            }
            onPostExecute(true);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            removeIncompleteDataForItem(mItem);
            mConvertingItems.remove(mItem);
        }
    }

    public Map<StreamItem, List<Integer>> getIncompleteIndexes() {
        return mIncompleteIndexes;
    }

    public Map<StreamItem, Long> getIncompleteContentLengths() {
        return mIncompleteContentLengths;
    }
}
