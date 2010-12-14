
/**
 * Audalyzer: an audio analyzer for Android.
 * <br>Copyright 2009-2010 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */


package com.soundcloud.utils;


import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.hermit.android.io.AudioReader;
import org.hermit.dsp.SignalPower;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


/**
 * The main audio analyser view.  This class relies on the parent SurfaceRunner
 * class to do the bulk of the animation control.
 */
public class Recorder
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
    public Recorder(Activity app) {
    	audioReader = new AudioReader();
    }
    
    public void setPowerGauge(PowerGauge powerGauge){
    	this.powerGauge = powerGauge;
    }
    
    public void activate(){
    	startRun();
    }
    
    public void deactivate(){
    	stopRun();
    }
    
   
    
    /**
     * Start writing audio output to given file
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void startRecording(File recordFile) {
    	
    	Log.i(TAG,"START RECORDING");
    	 
    	// Create the new file.
    	  try {
    		  if (recordFile.exists())
    			  recordFile.delete();
    		  
    		  recordFile.createNewFile();
    	  } catch (IOException e) {
    	    throw new IllegalStateException("Failed to create " + recordFile.toString());
    	  }
    	  
  	    // Create a DataOuputStream to write the audio data into the saved file.
  	    OutputStream ros;
  		try {
  			ros = new FileOutputStream(recordFile);
  		} catch (FileNotFoundException e) {
  			throw new IllegalStateException("File missing " + recordFile.toString());
  		}
  	    BufferedOutputStream rbos = new BufferedOutputStream(ros);
  	    mDosRec = new DataOutputStream(rbos);
    }
    
    
    /**
     * Stop writing audio output to given file
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void stopRecording() {
    	
    	Log.i(TAG,"STOP RECORDING");
    	
    	try {
      		
      		if (mDosRec != null) mDosRec.close();
      			mDosRec = null;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to close data output swtream");
		}
		
    }
    
    
    
    /**
     * Stop writing audio output to given file
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public Boolean isRecording() {
    	return mDosRec == null ? false : true;
    }
    
   
 
    
    // ******************************************************************** //
    // Private Classes.
    // ******************************************************************** //

    


    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    
    
    /**
     * Start the animation running.  All the conditions we need to
     * run are present (surface, size, resumed).
     */
    private void startRun() {
    
    	Log.i(TAG,"START RUN");
    	
    	 audioProcessed = audioSequence = 0;
         
         audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate, new AudioReader.Listener() {
             @Override
             public final void onReadComplete(short[] buffer) {
                 receiveAudio(buffer);
             }
             @Override
             public void onReadError(int error) {
                 Log.e(TAG,"On Audio Read Error");
             }
         });
    	
            if (recTicker != null && recTicker.isAlive())
                recTicker.kill();
            
            Log.i(TAG, "set running: start ticker");
            recTicker = new ThreadTicker();
    }


    /**
     * Stop the animation running.  Our surface may have been destroyed, so
     * stop all accesses to it.  If the caller is not the ticker thread,
     * this method will only return when the ticker thread has died.
     */
    private void stopRun() {
    	
    	Log.i(TAG,"STOP RUN");
    	
        // Kill the thread if it's running, and wait for it to die.
        // This is important when the surface is destroyed, as we can't
        // touch the surface after we return.  But if I am the ticker
    	// thread, don't wait for myself to die.
        Ticker ticker = null;
        synchronized (this) {
            ticker = recTicker;
        }
        
        if (ticker != null && ticker.isAlive()) {
        	if (onSurfaceThread())
        		ticker.kill();
        	else
        		ticker.killAndWait();
        }
        synchronized (this) {
            recTicker = null;
        }
        
        audioReader.stopReader();
    }

    
    private void tick() {
        try {
            // Do the application's physics.
            long now = System.currentTimeMillis();
            doUpdate(now);
        } catch (Exception e) {
            //errorReporter.reportException(e);
        }
    }
    
    

  // ******************************************************************** //
  // Audio Processing.
  // ******************************************************************** //

  /**
   * Handle audio input.  This is called on the thread of the audio
   * reader.
   * 
   */
  private final void receiveAudio(short[] buffer) {
      // Lock to protect updates to these local variables.  See run().
      synchronized (this) {
          audioData = buffer;
          ++audioSequence;
      }
  }
  
  
  // ******************************************************************** //
  // Main Loop.
  // ******************************************************************** //

  /**
   * Update the state of the instrument for the current frame.
   * This method must be invoked from the doUpdate() method of the
   * application's {@link SurfaceRunner}.
   * 
   * <p>Since this is called frequently, we first check whether new
   * audio data has actually arrived.
   * 
   * @param   now         Nominal time of the current frame in ms.
   */
  public final void doUpdate(long now) {
      short[] buffer = null;
      synchronized (this) {
          if (audioData != null && audioSequence > audioProcessed) {
              audioProcessed = audioSequence;
              buffer = audioData;
          }
      }

      // If we got data, process it without the lock.
      if (buffer != null)
          processAudio(buffer);
      
      
  }


  /**
   * Handle audio input.  This is called on the thread of the
   * parent surface.
   * 
   * @param   buffer      Audio data that was just read.
   */
  private final void processAudio(short[] buffer) {
      // Process the buffer.  While reading it, it needs to be locked.
      synchronized (buffer) {
          // Calculate the power now, while we have the input
          // buffer; this is pretty cheap.
          final int len = buffer.length;
          
          // If we have a power gauge, display the signal power.
          if (powerGauge != null)
        	  powerGauge.update(SignalPower.calculatePowerDb(buffer, 0, len));
          
          if (mDosRec != null){
          	try { 
	            	 // Element 0 isn't a frequency bucket; skip it.
	                 for (int idxBuffer = 0; idxBuffer < len; ++idxBuffer) {
	                	if (mDosRec != null){
	                		mDosRec.write(buffer[idxBuffer] & 0xff);
	                		mDosRec.write((buffer[idxBuffer] >> 8 ) & 0xff);
	                		mDosRec.write(buffer[idxBuffer] & 0xff);
	                		mDosRec.write((buffer[idxBuffer] >> 8 ) & 0xff);
	                	}
	                	 
	                	
	                 }
	                 
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
          	
          }
          
          // Tell the reader we're done with the buffer.
          buffer.notify();
          
      }

  }
  

    /**
     * Determine whether the caller is on the surface's animation thread.
     * 
     * @return             The resource value.
     */
    public boolean onSurfaceThread() {
        return Thread.currentThread() == recTicker;
    }
    
    
   
    
	/**
	 * Base interface for the ticker we use to control the animation.
	 */
	private interface Ticker {
	    // Stop this thread.  There will be no new calls to tick() after this.
	    public void kill();

	    // Stop this thread and wait for it to die.  When we return, it is
	    // guaranteed that tick() will never be called again.
	    // 
	    // Caution: if this is called from within tick(), deadlock is
	    // guaranteed.
	    public void killAndWait();

	    // Run method for this thread -- simply call tick() a lot until
	    // enable is false.
	    public void run();

	    // Determine whether this ticker is still going.
	    public boolean isAlive();
	}
	
	/**
	 * Thread-based ticker class.  This may be faster than LoopTicker.
	 */
	private class ThreadTicker
	    extends Thread
	    implements Ticker
	{

	    // Constructor -- start at once.
	    private ThreadTicker() {
	        super("Surface Runner");
	        Log.v(TAG, "ThreadTicker: start");
	        enable = true;
	        start();
	    }

	    // Stop this thread.  There will be no new calls to tick() after this.
	    public void kill() {
	        Log.v(TAG, "ThreadTicker: kill");
	        
	        enable = false;
	    }

	    // Stop this thread and wait for it to die.  When we return, it is
	    // guaranteed that tick() will never be called again.
	    // 
	    // Caution: if this is called from within tick(), deadlock is
	    // guaranteed.
	    public void killAndWait() {
	        Log.v(TAG, "ThreadTicker: killAndWait");
	        
	        if (Thread.currentThread() == this)
	        	throw new IllegalStateException("ThreadTicker.killAndWait()" +
	        								    " called from ticker thread");

	        enable = false;

	        // Wait for the thread to finish.  Ignore interrupts.
	        if (isAlive()) {
	            boolean retry = true;
	            while (retry) {
	                try {
	                    join();
	                    retry = false;
	                } catch (InterruptedException e) { }
	            }
	            Log.v(TAG, "ThreadTicker: killed");
	        } else {
	            Log.v(TAG, "Ticker: was dead");
	        }
	    }

	    // Run method for this thread -- simply call tick() a lot until
	    // enable is false.
	    @Override
	    public void run() {
	        while (enable) {
	            tick();
	            
                if (animationDelay != 0) try {
                    sleep(animationDelay);
                } catch (InterruptedException e) { }
	        }
	    }
	    
	    // Flag used to terminate this thread -- when false, we die.
	    private boolean enable = false;
	}
    
    
   

    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the state of the game in the provided Bundle.
     * 
     * @param   icicle      The Bundle in which we should save our state.
     */
    protected void saveState(Bundle icicle) {
//      gameTable.saveState(icicle);
    }


    /**
     * Restore the game state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
    protected void restoreState(Bundle icicle) {
//      gameTable.pause();
//      gameTable.restoreState(icicle);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "ScRecorder";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
	// The time in ms to sleep each time round the main animation loop.
    // If zero, we will not sleep, but will run continuously.
    private long animationDelay = 0;

    // The ticker thread which controls all recording functions
    private Ticker recTicker = null;
    
 // ******************************************************************** //
	// Private Data.
	// ******************************************************************** //


    // The desired sampling rate for this analyser, in samples/sec.
    private int sampleRate = 44100;

    // Audio input block size, in samples.
    private int inputBlockSize = 1024;
    
    // The desired decimation rate for this analyser.  Only 1 in
    // sampleDecimate blocks will actually be processed.
    private int sampleDecimate = 1;
   
    // Our audio input device.
    private final AudioReader audioReader;

    
    // The gauges associated with this instrument.  Any may be null if not
    // in use.
    private PowerGauge powerGauge = null;
    
    //Output stream for writing data, one for big endian , one for little endian
    private DataOutputStream mDosRec;

    
    // Buffered audio data, and sequence number of the latest block.
    private short[] audioData;
    private long audioSequence = 0;
    
    // Sequence number of the last block we processed.
    private long audioProcessed = 0;

}

