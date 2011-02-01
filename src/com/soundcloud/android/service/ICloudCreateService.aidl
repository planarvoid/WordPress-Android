package com.soundcloud.android.service;

interface ICloudCreateService
{

	void startRecording(String path);
	boolean isRecording();
 	void stopRecording();
 	void updateRecordTicker();
 	void uploadTrack(in Map trackdata);
 	boolean isUploading();
 	void cancelUpload();
}

