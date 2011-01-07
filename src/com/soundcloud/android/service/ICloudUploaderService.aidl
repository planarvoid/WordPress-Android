package com.soundcloud.android.service;

interface ICloudUploaderService
{
 	void uploadTrack(in Map trackdata);
 	boolean isUploading();
 	void cancelUpload();
}

