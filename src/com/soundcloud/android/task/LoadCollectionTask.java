package com.soundcloud.android.task;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.EventsWrapper;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class LoadCollectionTask extends AsyncTask<HttpUriRequest, Parcelable, Boolean> {
    
        private static final String TAG = "LoadCollectionTask";

        private WeakReference<LazyActivity> mActivityReference;

        protected ArrayList<Parcelable> newItems;

        public CloudUtils.Model loadModel;
        
        public int pageSize;

        /**
         * Set the activity and adapter that this task now belong to. This will
         * be set as new context is destroyed and created in response to
         * orientation changes
         * 
         * @param lazyEndlessAdapter
         * @param activity
         */
        public void setContext(LazyActivity activity) {
            mActivityReference = new WeakReference<LazyActivity>(activity);
        }

        /**
         * Do any task preparation we need to on the UI thread
         */
        @Override
        protected void onPreExecute() {
            Log.i(TAG,"Loading collection model: " + loadModel);
        }

        /**
         * Add all new items that have been retrieved, now that we are back on a
         * UI thread
         */
        @Override
        protected void onPostExecute(Boolean keepGoing) {
            Log.i(TAG,"~~~Done loading collection model: " + loadModel);
            if (mActivityReference.get() != null) {
                mActivityReference.get().handleError();
                mActivityReference.get().handleException();
            }
        }

        /**
         * Perform our background loading
         */
        @Override
        protected Boolean doInBackground(HttpUriRequest... params) {

            // make sure we have a valid url
            HttpUriRequest req = params[0];
            if (req == null)
                return false;

            try {

                InputStream is = mActivityReference.get().getSoundCloudApplication()
                        .executeRequest(req);
                ObjectMapper mapper = new ObjectMapper();

                if (newItems != null)
                    newItems.clear();
                switch (loadModel) {
                    case track:
                        newItems = mapper.readValue(is, TypeFactory.collectionType(ArrayList.class,
                                Track.class));
                        break;
                    case user:
                        newItems = mapper.readValue(is, TypeFactory.collectionType(ArrayList.class,
                                User.class));
                        break;
                    case comment:
                        newItems = mapper.readValue(is, TypeFactory.collectionType(ArrayList.class,
                                Comment.class));
                        break;
                        
                    case event:
                        EventsWrapper evtWrapper = mapper.readValue(is, EventsWrapper.class);
                        newItems = new ArrayList<Parcelable>(evtWrapper.getCollection().size());
                        for (Event evt : evtWrapper.getCollection())
                            newItems.add(evt);
                        break;
                }

                // resolve data
                for (Parcelable p : newItems)
                    if (mActivityReference.get() != null)
                        mActivityReference.get().resolveParcelable(p);

                return true;

            } catch (IOException e) {
                e.printStackTrace();
            }

            // there was an exception of some kind, return failure
            return false;

        }

    }
