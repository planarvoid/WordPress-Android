package com.soundcloud.android;

import com.soundcloud.android.objects.Track;

interface ICloudDownloaderService
{
 	void downloadTrack(in Track trackdata);
    int getCurrentDownloadPercentage();
    String getCurrentDownloadId();
    Track getCurrentDownloadingTrackInfo();
}

