package com.soundcloud.utils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.io.AudioReader;
import org.hermit.dsp.SignalPower;

import android.util.Log;

public class AudioAnalyser {
	 // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
     * @param   parent          Parent surface.
	 */
    public AudioAnalyser() {
        audioReader = new AudioReader();
    }


    public void startFileOutput(File recordFle){
    	  // Create the new file.
    	  try {
    		 
    		  if (recordFle.exists())
    			  recordFle.delete();
    		  
    		  recordFle.createNewFile();

    		  
    	  } catch (IOException e) {
    	    throw new IllegalStateException("Failed to create " + recordFle.toString());
    	  }
    	  
	    // Create a DataOuputStream to write the audio data into the saved file.
	    OutputStream ros;
		try {
			ros = new FileOutputStream(recordFle);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("File missing " + recordFle.toString());
		}
	    BufferedOutputStream rbos = new BufferedOutputStream(ros);
	    mDosRec = new DataOutputStream(rbos);
    	    
    	  
    	 
    }
    
    public void stopFileOutput(){
    	try {
    		
    		if (mDosRec != null) mDosRec.close();
    		mDosRec = null;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to close data output swtream");
		}
		
		
    }
    
    public Boolean isOutputting(){
    	return mDosRec == null ? false : true;
    }
    

    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * The application is starting.  
     */

    /**
     * We are starting the main run; start measurements.
     */
    public void measureStart() {
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
    }


    /**
     * We are stopping / pausing the run; stop measurements.
     */
    public void measureStop() {
        audioReader.stopReader();
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
    

   

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //


    // The desired sampling rate for this analyser, in samples/sec.
    private int sampleRate = 44100;

    // Audio input block size, in samples.
    private int inputBlockSize = 256;
    
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
