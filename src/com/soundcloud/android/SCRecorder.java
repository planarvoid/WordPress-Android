
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


package com.soundcloud.android;


import java.io.File;

import org.hermit.android.instruments.AudioAnalyser;
import org.hermit.android.instruments.InstrumentSurface;
import org.hermit.android.instruments.PowerGauge;
import org.hermit.dsp.Window;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;


/**
 * The main audio analyser view.  This class relies on the parent SurfaceRunner
 * class to do the bulk of the animation control.
 */
public class SCRecorder
	extends InstrumentSurface
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
    public SCRecorder(Activity app) {
        super(app, SURFACE_DYNAMIC);
        
        audioAnalyser = new AudioAnalyser(this);
        
        
        powerGauge = audioAnalyser.getPowerGauge(this);
        
        addInstrument(audioAnalyser);
        addGauge(powerGauge);

        // On-screen debug stats display.
        statsCreate(new String[] { "µs FFT", "Skip/s" });
    }
    
   
    
    /**
     * Start writing audio output to given file
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void startRecording(File recordFile) {
        audioAnalyser.startFileOutput(recordFile);
    }
    
    /**
     * Stop writing audio output to given file
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void stopRecording() {
        audioAnalyser.stopFileOutput();
    }
    
    /**
     * Stop writing audio output to given file
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public Boolean isRecording() {
        return audioAnalyser.isOutputting();
    }
    
    /**
     * Start writing gauge body
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void startPowerGauge() {
        //powerGauge.setDoNotDrawBody(false);
    }
    
    /**
     * Stop writing gauge body
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void stopPowerGauge() {
        //powerGauge.setDoNotDrawBody(true);
    }
    

    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Set the sample rate for this instrument.
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void setSampleRate(int rate) {
        audioAnalyser.setSampleRate(rate);
    }
    

    /**
     * Set the input block size for this instrument.
     * 
     * @param   size        The desired block size, in samples.
     */
    public void setBlockSize(int size) {
        audioAnalyser.setBlockSize(size);
    }
    

    /**
     * Set the spectrum analyser windowing function for this instrument.
     * 
     * @param   func        The desired windowing function.
     *                      Window.Function.BLACKMAN_HARRIS is a good option.
     *                      Window.Function.RECTANGULAR turns off windowing.
     */
    public void setWindowFunc(Window.Function func) {
        audioAnalyser.setWindowFunc(func);
    }
    

    /**
     * Set the decimation rate for this instrument.
     * 
     * @param   rate        The desired decimation.  Only 1 in rate blocks
     *                      will actually be processed.
     */
    public void setDecimation(int rate) {
        audioAnalyser.setDecimation(rate);
    }
    

    /**
     * Set the histogram averaging window for this instrument.
     * 
     * @param   rate        The averaging interval.  1 means no averaging.
     */
    public void setAverageLen(int rate) {
        audioAnalyser.setAverageLen(rate);
    }
    

    /**
     * Enable or disable stats display.
     * 
     * @param   enable        True to display performance stats.
     */
    public void setShowStats(boolean enable) {
        setDebugPerf(enable);
    }
    
   
    // ******************************************************************** //
    // Layout Processing.
    // ******************************************************************** //

    /**
     * Lay out the display for a given screen size.
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     */
    @Override
    protected void layout(int width, int height) {    	        
        // Set the gauge geometries.
        powerGauge.setGeometry(new Rect(0, 0, width, height));
    }
    

    /**
     * Lay out the display for a given screen size.
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     * @param   gutter      Spacing to leave between items.
     */
    private void layoutPortrait(int width, int height, int gutter) {
    	
    	Log.i("REC","Layout Portrait " + width + "," + height + "," + gutter);
    	
        // Divide the display into three vertical elements, the
        // spectrum display being double-height.
        int unit = (height - gutter * 4) / 4;
        int col = width - gutter * 2;

        int x = gutter;
        int y = gutter;
        meterRect = new Rect(x, y, x + col, y + unit);
        
        Log.i("REC","Layout Portrait meter recr" + x + "," + y + "," + col + "," + unit);
    }
    
    
    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //

    /**
	 * Handle key input.
	 * 
     * @param	keyCode		The key code.
     * @param	event		The KeyEvent object that defines the
     * 						button action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	return false;
    }
    
    
    /**
	 * Handle touchscreen input.
	 * 
     * @param	event		The MotionEvent object that defines the action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	int action = event.getAction();
//    	final float x = event.getX();
//        final float y = event.getY();
    	switch (action) {
    	case MotionEvent.ACTION_DOWN:
            break;
        case MotionEvent.ACTION_MOVE:
            break;
    	case MotionEvent.ACTION_UP:
            break;
    	case MotionEvent.ACTION_CANCEL:
            break;
        default:
            break;
    	}

		return true;
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
	private static final String TAG = "Audalyzer";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our audio input device.
    private final AudioAnalyser audioAnalyser;
    
    // The gauges associated with this instrument.
    private PowerGauge powerGauge = null;

    // Bounding rectangles for the waveform, spectrum, and VU meter displays.
    private Rect waveRect = null;
    private Rect specRect = null;
    private Rect meterRect = null;

}

