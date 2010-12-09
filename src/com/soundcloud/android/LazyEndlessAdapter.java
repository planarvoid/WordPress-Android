package com.soundcloud.android;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

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

public class LazyEndlessAdapter extends AdapterWrapper {
	
	private View pendingView=null;
	private int pendingPosition=-1;
	private AppendTask appendTask;
	protected AtomicBoolean keepOnAppending=new AtomicBoolean(true);
	
	private String mUrl;
	private CloudUtils.Model mLoadModel;
	private String mCollectionKey = "";
	
	private View mEmptyView;
	private View mListView;
	
	private int mCurrentPage;
	
	private Parcelable newItems[];
	
	
		protected LazyActivity mActivity;
		
		public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped) {
			super(wrapped);
			
			mActivity = activity;
			mCurrentPage = 0;
		
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
			emptyView.setText(mActivity.getResources().getString(R.string.no_tracks_available));
			emptyView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			emptyView.setVisibility(View.GONE);
			emptyView.setTextAppearance(mActivity, R.style.txt_empty_view);
			//emptyView.setBackgroundColor(mActivity.getResources().getColor(R.color.cloudProgressBackgroundCenter));
			mEmptyView = emptyView;
			
			((ViewGroup)mListView.getParent()).addView(emptyView);
			
		}
		
		
		
		
		
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
						appendTask.execute(getUrl());	
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
			
			return ((LazyBaseAdapter) this.getWrappedAdapter()).getData();
		}

	
		protected View getPendingView(ViewGroup parent) {
			ViewGroup row=((LazyBaseAdapter) this.getWrappedAdapter()).createRow();
			
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
		
		protected void setUrl(String url){
			mUrl = url;
		}
		
		protected void setLoadModel(CloudUtils.Model loadModel){
			mLoadModel = loadModel;
		}
		
		private void setCurrentPage(String url){
			mUrl = url;
		}
		
		
		protected String getUrl(){
			return mUrl;
		}
		
		
		
		public void onPreTaskExecute(){
			
		}
		
		
		public void clear(){
			mEmptyView.setVisibility(View.GONE);
			((AdapterView<ListAdapter>) mListView).setEmptyView(null);
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
	}