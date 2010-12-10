package com.soundcloud.utils;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RemoteImageView extends ImageView {
	private String mLocal;

	private String mRemote;
	private Drawable mTemp;
	private int mImageWidth = 0;
	private int mImageHeight = 0;
	private HTTPThread mThread = null;
	private float scale;

	public RemoteImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		
		this.setScaleType(ScaleType.CENTER_INSIDE);
	}

	public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);	
		scale = context.getResources().getDisplayMetrics().density;
	}
	
	public void setImageSize(int imageWidth, int imageHeight) {
		mImageWidth = imageWidth;
		mImageHeight = imageHeight;
	}

	public void setLocalURI(String local) {
		mLocal = local;
	}

	public void setTemporaryDrawable(Drawable temporaryDrawable) {
		mTemp = temporaryDrawable;
	}

	public void setRemoteURI(String uri) {
		
		//if (uri.startsWith("http")) {
			mRemote = uri;
		//}
	}

	public void loadImage() {
		if (mRemote != null && !mRemote.contentEquals("")) {
			if (mLocal == null) {
				mLocal = Environment.getExternalStorageDirectory() + "/.remote-image-view-cache/" + mRemote.hashCode() + ".jpg";
			}
			// check for the local file here instead of in the thread because
			// otherwise previously-cached files wouldn't be loaded until after
			// the remote ones have been downloaded.
			File local = new File(mLocal);
			
			if (local.exists()) {
				setFromLocal();
			} else {
				// we already have the local reference, so just make the parent
				// directories here instead of in the thread.
				local.getParentFile().mkdirs();
				queue();
			}
		} else if (mLocal != null && !mLocal.contentEquals("")) {
			
			File local = new File(mLocal);
			if (local.exists()) 
				setFromLocal();
			else if (mTemp != null)
				this.setImageDrawable(mTemp);
			
		} else if (mTemp != null) {
			
			this.setImageDrawable(mTemp);
		}
	}	

	@Override
	public void finalize() {
		if (mThread != null) {
			HTTPQueue queue = HTTPQueue.getInstance();
			queue.dequeue(mThread);
		}
	}

	private void queue() {
		if (mThread == null) {
			mThread = new HTTPThread(mRemote, mLocal, mHandler);
			HTTPQueue queue = HTTPQueue.getInstance();
			queue.enqueue(mThread, HTTPQueue.PRIORITY_HIGH);
		}
		this.setImageDrawable(mTemp);
	}

	private void setFromLocal() {
		mThread = null;
		
		// First let's just check the dimensions of the contact photo
	    BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    Bitmap bm = BitmapFactory.decodeFile(mLocal,options);
	   
	    options = determineDecodeOptions(options);
	    
	    bm = null;
	    try {
	    	bm = BitmapFactory.decodeFile(mLocal,options);
	    } catch (OutOfMemoryError e) {
	     
	    }

		if (bm != null) {
			this.setImageBitmap(bm);
		}
	}
	
	
	private BitmapFactory.Options determineDecodeOptions(BitmapFactory.Options options){
		
		// Raw height and width of contact photo
	    int height = options.outHeight;
	    int width = options.outWidth;

	    // If photo is too large or not found get out
//	    if (height > IMAGE_MAXSIZE || width > IMAGE_MAXSIZE  ||
//	        width == 0 || height == 0) return null;

	    if (width == 0 || height == 0) return null;

	    
	    // This time we're going to do it for real
	    options.inJustDecodeBounds = false;

	    // Calculate new thumbnail size based on screen density
	    int imageWidth = mImageWidth == 0 ? getWidth() : mImageWidth;
	    int imageHeight = mImageHeight == 0 ? getHeight() : mImageHeight;
	    
	    if (imageWidth == 0 || imageHeight == 0)
	    	return options;
	    
	    if (scale != 1.0) {
	    	imageWidth = imageWidth * Math.round(scale);
	    	imageHeight = imageHeight * Math.round(scale);
	    }
	    
	    

	    int newHeight = imageHeight;
	    int newWidth = imageWidth;

	    // If we have an abnormal photo size that's larger than thumbsize then sample it down
	    boolean sampleDown = false;

	    if (height > imageHeight || width > imageWidth) {
	      sampleDown = true;
	    }

	    // If the dimensions are not the same then calculate new scaled dimenions
	    if (imageHeight/height < imageWidth/width) {
	      if (sampleDown) {
	        options.inSampleSize = Math.round(height / imageHeight);
	      }
	      newHeight = Math.round(imageHeight * height / width);
	    } else {
	      if (sampleDown) {
	        options.inSampleSize = Math.round(width / imageWidth);
	      }
	      newWidth = Math.round(imageWidth * width / height);
	    }
	    
	    return options;
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			setFromLocal();
		}
	};
}
