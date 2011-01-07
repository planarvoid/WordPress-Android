package com.soundcloud.utils;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HTTPQueue {
	public static final int PRIORITY_LOW = 0;
	public static final int PRIORITY_HIGH = 1;

	private volatile static HTTPQueue sInstance = null;

	private ArrayList<HTTPThread> mQueue = new ArrayList<HTTPThread>();
	private HashMap<Long, Boolean> mThreads = new HashMap<Long, Boolean>();
	private Handler mQueuedHandler = null;

	private HTTPQueue() {
	}

	public static HTTPQueue getInstance() {
		if (sInstance == null) {
			sInstance = new HTTPQueue();
		}
		return sInstance;
	}

	public void enqueue(HTTPThread task) {
		enqueue(task, PRIORITY_LOW);
	}
	
	public HTTPThread getThreadById(Long id){
		for (int i = 0; i < mQueue.size(); i++){
			if (mQueue.get(i).getId() == id){
				return mQueue.get(i);
			}
		}
		return null;
	}

	public synchronized void enqueue(HTTPThread task, int priority) {
		Log.i("QUEUE","Enqueue tyask " + task.getId() + " " + mThreads.get(task.getId()));
		
		Boolean exists = mThreads.get(task.getId());
		if (exists == null ) {
			Log.i("QUEUE","DOESNT EXIST");
			int addIndex =-1;
			for (int i = 0; i < mQueue.size(); i++){
				Log.i("QUEUE","Compaing " + mQueue.get(i).getLocal() + " found " + task.getLocal());
				if (mQueue.get(i).getLocal().contentEquals(task.getLocal())){
					addIndex = i + 1;
				}
			}
			if (addIndex > -1 && addIndex < mQueue.size())
				mQueue.add(addIndex, task);
			else
				mQueue.add(task);
			
			mThreads.put(task.getId(), true);
		} 

		runFirst();
	}

	public synchronized void dequeue(final HTTPThread task) {
		mThreads.remove(task.getId());
		mQueue.remove(task);
	}

	public synchronized void finished(int result) {
		if (mQueuedHandler != null) {
			mQueuedHandler.sendEmptyMessage(result);
		}
		runFirst();
	}

	private synchronized void runFirst() {
		if (mQueue.size() > 0) {
			HTTPThread task = mQueue.get(0);
			if (task.getStatus() == HTTPThread.STATUS_PENDING) {
				mQueuedHandler = task.getHandler();
				task.setHandler(mHandler);
				task.start();
			} else if (task.getStatus() == HTTPThread.STATUS_FINISHED) {
				HTTPThread thread = mQueue.remove(0);
				mThreads.remove(thread.getId());
				runFirst();
			}
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			finished(message.what);
		}
	};
}
