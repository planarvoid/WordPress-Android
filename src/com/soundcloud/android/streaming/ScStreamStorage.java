package com.soundcloud.android.streaming;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.utils.Range;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class ScStreamStorage {
    private static int DEFAULT_CHUNK_SIZE = 128*1024;
    private static int CLEANUP_INTERVAL = 10;

    public int chunkSize;
    private Context mContext;

    private File mBaseDir, mCompleteDir, mIncompleteDir;
    private long mUsedSpace, mSpaceLeft;

    private Dictionary<String, Integer>  mIncompleteContentLengths;
    private Dictionary<String, ArrayList<Integer>> mIncompleteIndexes;

    private ArrayList<String> mConvertingKeys;

    public void ScStreamStorage(Context context){

        mContext = context;
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

        mConvertingKeys = new ArrayList<String>();
    }

    public void setData(byte[] data, int chunkIndex, ScStreamItem item) throws IOException {
        final String key = item.getURLHash();

        if (data == null) return;

        if (item.getContentLength() == 0) {
            Log.d(getClass().getSimpleName(), "Not Storing Data. Content Length is Zero.");
            return;
        }

        setContentLength(key,item.getContentLength());

        //Do not add to complete files
        if (completeFileForKey(key).exists()) return;

        //Prepare incomplete file
        ensureMetadataIsLoadedForKey(key);

        ArrayList<Integer> indexes = mIncompleteIndexes.get(key);
        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            mIncompleteIndexes.put(key, indexes);
        }

        //return if it's already in store
        if (indexes.contains(chunkIndex)) return;

        final File incompleteFile = incompleteFileForKey(key);
        if (!incompleteFile.exists()) {
            incompleteFile.createNewFile();
        }

        // always write chunk size even if it isn't a complete chunk (for offsetting I think)
        if (data.length != chunkSize){
            data = Arrays.copyOf(data,chunkSize);
        }

        FileOutputStream fos = new FileOutputStream(incompleteFile,true);
        fos.write(data);
        fos.close();

        //Add Index and save it
        indexes.add(chunkIndex);

        File indexFile = incompleteIndexFileForKey(key);
        if (indexFile.exists()) {
            indexFile.delete();
        }

        try {
            DataOutputStream din = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
            din.writeInt(item.getContentLength());
            for (Integer index : indexes) {
                din.writeInt(index);
            }
            din.close();

        } catch (IOException e) {
            Log.e("asdf","Error writing to index file " + e.getMessage());
            e.printStackTrace();
        }

        if (indexes.size() == numberOfChunksForKey(key)){
            mConvertingKeys.add(key);
           new ConvertFileToComplete(key, indexes).execute(new File[]{incompleteFile, completeFileForKey(key)});

        }

        //Update the number of writes, cleanup if necessary
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final int currentCount = prefs.getInt("streamingWritesSinceCleanup", 0) + 1;
        prefs.edit().putInt("streamingWritesSinceCleanup",currentCount).commit();

        if (currentCount >= CLEANUP_INTERVAL) {
            calculateFileMetrics();
            cleanup();
        }

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
            if (f.getParentFile().equals(mIncompleteDir)){
                File indexFile = new File(f.getAbsolutePath() + "_index");
                if (indexFile.exists()){
                    cleanedSpace += indexFile.length();
                    indexFile.delete();
                }
            }
        }
    }

    private void calculateFileMetrics() {

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

        Log.d(getClass().getSimpleName(),"[File Metrics] used: " + mUsedSpace + " , free: " + mSpaceLeft);
    }

    private void setContentLength(String key, int contentLength) {
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

        File indexFile = incompleteIndexFileForKey(key);
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

    private File incompleteIndexFileForKey(String key){
        return new File(mIncompleteDir,key+"_index");

    }

    private boolean resetDataIfNecessary(ScStreamItem item) {
        final String key = item.getURLHash();
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
        removeCompleteDataForKey(key);
        removeIncompleteDataForKey(key);
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
            BufferedInputStream bin = null;

            try {
                fos = new FileOutputStream(completeFile, true);
                bin = new BufferedInputStream(new FileInputStream(chunkFile));

                completeFile.createNewFile();
                byte[] buffer = new byte[chunkSize];

                for (int chunkNumber = 0; chunkNumber < mIndexes.size(); chunkNumber++) {
                    bin.read(buffer, mIndexes.indexOf(chunkNumber), chunkSize);
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
                if (bin != null){
                    try {bin.close();} catch (IOException e) {e.printStackTrace();}
                }
                if (fos != null){
                    try {fos.close();} catch (IOException e) {e.printStackTrace();}
                }
            }
            return true;
        }

        @Override
        public void onPostExecute(Boolean success){
            if (true){
                removeIncompleteDataForKey(mKey);
            } else {
                removeCompleteDataForKey(mKey);
            }

            mConvertingKeys.remove(mKey);
        }
    }

}
