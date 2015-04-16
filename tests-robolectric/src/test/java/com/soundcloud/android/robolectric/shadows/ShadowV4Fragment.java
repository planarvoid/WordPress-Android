package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowFragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;

import java.io.FileDescriptor;
import java.io.PrintWriter;

@Implements(Fragment.class)
public class ShadowV4Fragment extends ShadowFragment {

    private View view;

    @Implementation
    public boolean isAdded() {
        return isAttached();
    }
    @Implementation
    public LayoutInflater getLayoutInflater(Bundle savedInstanceState){
        return Robolectric.getShadowApplication().getLayoutInflater();
    }
    @Implementation
    public LoaderManager getLoaderManager(){
        return new ShadowV4LoaderManager();
    }
    @Implementation
    public View getView() {
        return view;
    }

    @Implementation
    public Resources getResources() {
        return Robolectric.getShadowApplication().getResources();
    }

    public void setView(View view){
        this.view = view;
    }

    @Implements(LoaderManager.class)
    public static class ShadowV4LoaderManager extends LoaderManager{

        @Override
        public <D> Loader<D> initLoader(int id, Bundle args, LoaderCallbacks<D> callback) {
            return null;
        }

        @Override
        public <D> Loader<D> restartLoader(int id, Bundle args, LoaderCallbacks<D> callback) {
            return null;
        }

        @Override
        public void destroyLoader(int id) {
        }

        @Override
        public <D> Loader<D> getLoader(int id) {
            return null;
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        }
    }

}
