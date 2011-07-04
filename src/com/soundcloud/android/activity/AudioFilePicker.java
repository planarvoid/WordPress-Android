package com.soundcloud.android.activity;


import com.soundcloud.android.R;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Based on https://github.com/Kaloer/Android-File-Picker-Activity
public class AudioFilePicker extends ListActivity {
    private static final File DEFAULT_INITIAL_DIRECTORY = new File("/");

    private static final int SHOW_ALL = 0;
    private static final int REFRESH = 1;

    public static final String LAST_DIR_KEY = "last_dir";
    public static final String START_DIR_KEY = "start_dir";
    public static final String SHOW_ALL_KEY = "show_all";

    private File mDirectory, mStartDirectory;
    private FilePickerListAdapter mAdapter;
    private final List<File> mFiles = new ArrayList<File>();

    private boolean mShowAll;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View emptyView = inflator.inflate(R.layout.audio_file_picker_empty_view, null);
        ((ViewGroup) getListView().getParent()).addView(emptyView);
        getListView().setEmptyView(emptyView);

        File lastDir = fromBundle(bundle, LAST_DIR_KEY, null);
        if (lastDir != null && lastDir.exists()) {
            mDirectory = lastDir;
        } else if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mDirectory = Environment.getExternalStorageDirectory();
        } else {
            mDirectory = DEFAULT_INITIAL_DIRECTORY;
        }

        mStartDirectory = fromBundle(bundle, START_DIR_KEY, mDirectory);
        mShowAll = bundle != null && bundle.getBoolean(SHOW_ALL_KEY, false);
        mAdapter = new FilePickerListAdapter(this, mFiles);
        setListAdapter(mAdapter);
    }


    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

        bundle.putString(LAST_DIR_KEY, mDirectory.getAbsolutePath());
        bundle.putString(START_DIR_KEY, mStartDirectory.getAbsolutePath());
        bundle.putBoolean(SHOW_ALL_KEY, mShowAll);
    }

    @Override
    protected void onResume() {
        refreshFilesList(mDirectory);
        super.onResume();
    }

    protected void refreshFilesList(File file) {
        mFiles.clear();
        File[] files = file.listFiles(mShowAll ? null : AudioFileFilter.INSTANCE);
        if (files != null && files.length > 0) {
            for (File f : files) {
                if (!f.isHidden() || mShowAll) mFiles.add(f);
            }
            Collections.sort(mFiles, FileComparator.INSTANCE);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (mDirectory.getParentFile() != null &&
           !mDirectory.getParentFile().equals(mStartDirectory.getParentFile())) {

            mDirectory = mDirectory.getParentFile();
            refreshFilesList(mDirectory);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        File file = (File) list.getItemAtPosition(position);
        if (file.isFile()) {
            setResult(RESULT_OK, new Intent().setData(Uri.fromFile(file)));
            finish();
        } else {
            mDirectory = file;
            refreshFilesList(mDirectory);
        }
        super.onListItemClick(list, view, position, id);
    }

    private static class FilePickerListAdapter extends ArrayAdapter<File> {
        private List<File> mObjects;

        public FilePickerListAdapter(Context context, List<File> objects) {
            super(context, R.layout.audio_file_picker_list_item, android.R.id.text1, objects);
            mObjects = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.audio_file_picker_list_item, parent, false);
            } else {
                row = convertView;
            }
            File object = mObjects.get(position);
            ImageView imageView = (ImageView) row.findViewById(R.id.file_picker_image);
            TextView textView = (TextView) row.findViewById(R.id.file_picker_text);
            textView.setText(object.getName());
            imageView.setImageResource(object.isFile() ? R.drawable.sounds : R.drawable.file_picker_folder);
            return row;
        }
    }

    private static class FileComparator implements Comparator<File> {
        static FileComparator INSTANCE = new FileComparator();
        @Override
        public int compare(File f1, File f2) {
            if (f1 == f2) {
                return 0;
            } else if (f1.isDirectory() && f2.isFile()) {
                return -1;
            } else if (f1.isFile() && f2.isDirectory()) {
                return 1;
            } else {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        }
    }

    private static class AudioFileFilter implements FilenameFilter {
        static FilenameFilter INSTANCE = new AudioFileFilter();
        private static final String[] AUDIO_EXTENSIONS = {
           "mp3", "ogg",  "wav", "pcm", "aiff", "aif", "wma", "3gp", "aac", "amr",
           "m4a", "flac", "raw", "pcm"
        };

        @Override
        public boolean accept(File dir, String filename) {
            final File file = new File(dir, filename);
            if (file.isDirectory()) {
                return true;
            } else {
                int dot = filename.lastIndexOf(".");
                if (dot != -1 && dot < filename.length()) {
                    final String ext = filename.substring(dot + 1, filename.length()).toLowerCase();
                    for (String e : AUDIO_EXTENSIONS) if (ext.equals(e)) return true;
                }
            }
            return false;
        }
    }

    private File fromBundle(Bundle bundle, String key, File defaultFile) {
        return bundle != null && bundle.containsKey(key ) ?
                new File(bundle.getString(key)) : defaultFile;
    }
spsp
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, SHOW_ALL, 0, mShowAll ? R.string.menu_show_audio : R.string.menu_show_all)
            .setIcon(android.R.drawable.ic_menu_view);
        menu.add(1, REFRESH, 0, R.string.menu_refresh).setIcon(R.drawable.ic_menu_refresh);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SHOW_ALL:
                mShowAll = !mShowAll;
                refreshFilesList(mDirectory);
                return true;

            case REFRESH:
                refreshFilesList(mDirectory);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}