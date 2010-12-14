package com.soundcloud.android.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.commonsware.cwac.adapter.AdapterWrapper;
import com.soundcloud.android.CloudCommunicator;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyList;


/**
 * A background task that will be run when there is a need
 * to append more data. Mostly, this code delegates to the
 * subclass, to append the data in the background thread and
 * rebind the pending view once that is done.
 */

	public class LazyEndlessAdapter extends AdapterWrapper {
		
		private View pendingView=null;
		private int pendingPosition=-1;
		private AppendTask appendTask;
		protected AtomicBoolean keepOnAppending=new AtomicBoolean(true);
		
		private String mUrl;
		private String mQuery;
		private CloudUtils.Model mLoadModel;
		private String mCollectionKey = "";
		
		private View mEmptyView;
		private View mListView;
		
		private int mCurrentPage;
		protected LazyActivity mActivity;
		
		public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped) {
			super(wrapped);
			
			mActivity = activity;
			mCurrentPage = 0;
			mQuery = "";
		
		}
		
		
		public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped, String url) {
			this(activity, wrapped);
			
			mUrl = url;
		}
		
		
		
		public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped, String url, CloudUtils.Model loadModel) {
			this(activity, wrapped,url);
			
			mLoadModel = loadModel;
		}
		
		public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped, String url, CloudUtils.Model loadModel, String collectionKey) {
			this(activity, wrapped,url,loadModel);
			
			mCollectionKey = collectionKey;
		}
		
		

		public void createListEmptyView(LazyList lv) {
			mListView = lv;
			
			
			
			TextView emptyView = new TextView(mActivity);
			emptyView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			emptyView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			emptyView.setVisibility(View.GONE);
			emptyView.setTextAppearance(mActivity, R.style.txt_empty_view);
			
			switch (getLoadModel()){
				case track:
					emptyView.setText(mActivity.getResources().getString(R.string.no_tracks_available));
					break;
				case user:
					emptyView.setText(mActivity.getResources().getString(R.string.no_users_available));
					break;
				case comment:
					emptyView.setText(mActivity.getResources().getString(R.string.no_comments_available));
					break;
				case event:
					emptyView.setText(mActivity.getResources().getString(R.string.no_activities_available));
					break;
			}
			
			//emptyView.setBackgroundColor(mActivity.getResources().getColor(R.color.cloudProgressBackgroundCenter));
			mEmptyView = emptyView;
			
			((ViewGroup)mListView.getParent()).addView(emptyView);
			
		}
		
		
		
		
		
		@Override
		public LazyBaseAdapter getWrappedAdapter(){
			return (LazyBaseAdapter) super.getWrappedAdapter();
		}
		
		public void restoreTask(AppendTask ap){
			Log.i("TASK","Restore Task " + ap);
			if (ap != null && !CloudUtils.isTaskFinished(ap)){
				appendTask = ap;
				ap.setContext(this,mActivity);
			}
		}
		
		public AppendTask getTask(){
			return appendTask;
		}
		
		public String getCollectionKey(){
			return mCollectionKey;
		}
		
		public CloudUtils.Model getLoadModel(){
			return mLoadModel;
		}
		
		public int getCurrentPage(){
			return mCurrentPage;
		}

		/**
			* How many items are in the data set represented by this
			* Adapter.
	    */
		@Override
		public int getCount() {
			if (keepOnAppending.get()) {
				return(super.getCount()+1);		// one more for "pending"
			}

			return(super.getCount());
		}

		
		
		/**
			* Get a View that displays the data at the specified
			* position in the data set. In this case, if we are at
			* the end of the list and we are still in append mode,
			* we ask for a pending view and return it, plus kick
			* off the background task to append more data to the
			* wrapped adapter.
			* @param position Position of the item whose data we want
			* @param convertView View to recycle, if not null
			* @param parent ViewGroup containing the returned View
	    */
		@Override
		public View getView(int position, View convertView,
												ViewGroup parent) {
			
			
			
			//int asdf = 12/0;
			
			if (position==super.getCount() &&
					keepOnAppending.get()) {
				if (pendingView==null) {
					pendingView=getPendingView(parent);
					pendingPosition=position;
					
					if (appendTask == null || CloudUtils.isTaskFinished(appendTask)){
						appendTask = new AppendTask();
						appendTask.setContext(this,mActivity);
						appendTask.execute(getUrl(), getQuery());	
					}
					
				}

				return(pendingView);
			}
			else if (convertView==pendingView) {
				// if we're not at the bottom, and we're getting the
				// pendingView back for recycling, skip the recycle
				// process
				return(super.getView(position, null, parent));
			}

			return(super.getView(position, convertView, parent));
		}
		
	
		
		@SuppressWarnings("unchecked")
		public void onPostTaskExecute(Boolean keepgoing){
			keepOnAppending.set(keepgoing);
			Log.i("TASK","On Post Task Execute " + keepOnAppending.get());
			rebindPendingView(pendingPosition, pendingView);
			pendingView=null;
			pendingPosition=-1;
			
			((AdapterView<ListAdapter>) mListView).setEmptyView(mEmptyView);
			notifyDataSetChanged();
			Log.i("TASK","On Post Task Execute " + this.getData().size());
		}
		
		
		
		public List<Parcelable> getData() {
			
			return (this.getWrappedAdapter()).getData();
		}

	
		protected View getPendingView(ViewGroup parent) {
			ViewGroup row=(this.getWrappedAdapter()).createRow();
			
			View content=row.findViewById(R.id.row_holder);
			content.setVisibility(View.GONE);
			
			View loader = row.findViewById(R.id.row_loader);
			loader.setVisibility(View.VISIBLE);
			
			ProgressBar list_loader = (ProgressBar) row.findViewById(R.id.list_loading);
			list_loader.setVisibility(View.VISIBLE);
			
			
			return(row);
		}
		
		protected void rebindPendingView(int position, View row) {
			Log.i("Endless","Rebinding something " + position);
			
			View child=row.findViewById(R.id.row_holder);
			child.setVisibility(View.VISIBLE);
			
			ProgressBar list_loader = (ProgressBar) row.findViewById(R.id.list_loading);
			list_loader.setVisibility(View.GONE);
			
			child = row.findViewById(R.id.row_loader);
			child.setVisibility(View.GONE);
			
			Log.i("Endless","REBOUND something " + row.findViewById(R.id.row_holder).getHeight());
			
		}

		
		public void onDataNode(JSONObject data) {
			// TODO Auto-generated method stub
			return;
		}
		
		protected void setActivity(LazyActivity activity){
			mActivity = activity;
		}
		
		protected void setPath (String url){
			setPath (url,"");
		}
		
		public void setPath(String url, String query){
			mUrl = url;
			mQuery = query;
		}
		
		
		
		protected void setLoadModel(CloudUtils.Model loadModel){
			mLoadModel = loadModel;
		}
		
		protected String getUrl(){
			return mUrl;
		}
		
		protected String getQuery(){
			return mQuery;
		}
		
		
		
		public void onPreTaskExecute(){
			
		}
		
		
		@SuppressWarnings("unchecked")
		public void clear(){
			if (mEmptyView != null) mEmptyView.setVisibility(View.GONE);
			if (mListView != null) ((AdapterView<ListAdapter>) mListView).setEmptyView(null);
			
			mCurrentPage = 0;
			keepOnAppending.set(true);
			
			getWrappedAdapter().clear();
			
			if (appendTask != null && !CloudUtils.isTaskFinished(appendTask)){
				appendTask.cancel(true);
			}
			
			this.notifyDataSetChanged();
		}
		
		public int[] savePagingData()
		{
			int[] ret = new int[2];
			ret[0] = (keepOnAppending.get()) ? 1 : 0;
			ret[1] = mCurrentPage;
			return ret;
			
		}
		
		public void restorePagingData(int[] restore)
		{
			keepOnAppending.set(restore[0] == 1 ? true : false);
			mCurrentPage = restore[1];
		}
		
		public String saveExtraData()
		{
			return "";
		}
		
		public void restoreExtraData(String restore)
		{
		}
		

		public void allowLoading(){
			keepOnAppending.set(true);
		}


		public void incrementPage() {
			mCurrentPage++;
		}
		

		/**
		 * A background task that will be run when there is a need
		 * to append more data. Mostly, this code delegates to the
		 * subclass, to append the data in the background thread and
		 * rebind the pending view once that is done.
		 */
		public class AppendTask extends AsyncTask<String, Parcelable, Boolean> {
			private static final String TAG = "AppendTask";

			private LazyEndlessAdapter mAdapter;
			private LazyActivity mActivity;
			
			private Boolean keepGoing = true;
			
			private Parcelable newItems[];
			
			public void setContext(LazyEndlessAdapter lazyEndlessAdapter,LazyActivity activity) {
				mAdapter = lazyEndlessAdapter;
				mActivity = activity;
			}
			
			@Override
			protected void onPreExecute() {
				if (mAdapter != null) mAdapter.onPreTaskExecute();
			}
			

			@Override
			protected void onPostExecute(Boolean keepGoing) {
				
				if (mAdapter == null || newItems == null)
					return;
				
				if (mAdapter != null) {
					if (newItems.length > 0){
						for (Parcelable newitem : newItems){
							mAdapter.getData().add(newitem);	
						}
					}
					
					mAdapter.onPostTaskExecute(keepGoing);
				}
				
			}

			@Override
			protected Boolean doInBackground(String... params) {

				
				String baseUrl = params[0];
				String query = params[1];
				if (baseUrl == null || baseUrl == "")
					return false;
				
				Boolean keep_appending = true;
				JSONObject collectionHolder;
				JSONArray collection;
			
				try {
					Log.i("DEBUG","Build upont url " + baseUrl);
					Uri u = Uri.parse(baseUrl);
					Uri.Builder builder = u.buildUpon();
					
					if (!query.contentEquals("")) builder.appendQueryParameter("q", query);
					if (baseUrl.indexOf("limit") == -1) builder.appendQueryParameter("limit", String.valueOf(mActivity.getPageSize()));
					
					builder.appendQueryParameter("rand", String.valueOf(((int) Math.random()*100000)));
					builder.appendQueryParameter("offset", String.valueOf(mActivity.getPageSize()*(mAdapter.getCurrentPage())));					
					builder.appendQueryParameter("consumer_key", mActivity.getResources().getString(R.string.consumer_key));
					
					//String jsonRaw = activity.mCloudComm.getContent(mUrl);
					InputStream is = mActivity.getCloudComm().getContent(builder.build().toString());
					String jsonRaw = CloudCommunicator.formatContent(is);
					
					if (CloudCommunicator.getErrorFromJSONResponse(jsonRaw) != ""){
						if (mActivity != null) mActivity.setError(CloudCommunicator.getErrorFromJSONResponse(jsonRaw));
						return false;
					}
					
					Log.i("TASK","Collection key is " + mAdapter.getCollectionKey());

					if (mAdapter.getCollectionKey() != ""){
						collectionHolder = new JSONObject(jsonRaw);
						collection = collectionHolder.getJSONArray(mAdapter.getCollectionKey());
						mAdapter.onDataNode(collectionHolder);
					} else if (jsonRaw.startsWith("{")){
						collection = new JSONArray("["+jsonRaw+"]");
					} else {
						collection = new JSONArray(jsonRaw);	
					}
					
					if (collection.length() < mActivity.getPageSize())
						keep_appending = false;
					
					Log.i("TASK","Parsing collection about to " + collection.length());
					
					newItems = new Parcelable[collection.length()];
					
					for (int i = 0; i < collection.length(); i++) {
						
						
						try {
							switch (mAdapter.getLoadModel()){
								case track:
									Track trk = new Track(collection.getJSONObject(i));
									mActivity.resolveParcelable(trk);
									newItems[i] = trk;
									break;
								case user:
									User usr = new User(collection.getJSONObject(i));
									mActivity.resolveParcelable(usr);
									newItems[i] = usr;
									break;
								case comment:
									Comment cmt = new Comment(collection.getJSONObject(i));
									mActivity.resolveParcelable(cmt);
									newItems[i] = cmt;	
									break;
								case event:
									Event evt = new Event(collection.getJSONObject(i));
									mActivity.resolveParcelable(evt);
									newItems[i] = evt;
									break;
							}
						
						} catch (JSONException e) {
							Log.i(getClass().getName(),e.toString());
						}
					
					}
					
					mAdapter.incrementPage();
					
					return keep_appending;
					
				} catch (IOException e) {
					if (mActivity != null) mActivity.setException(e);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthMessageSignerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthExpectationFailedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthCommunicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return false;
				
			}

	}
}