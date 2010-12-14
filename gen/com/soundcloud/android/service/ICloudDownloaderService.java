/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /sc_workspace/Soundcloud_Android/src/com/soundcloud/android/service/ICloudDownloaderService.aidl
 */
package com.soundcloud.android.service;
public interface ICloudDownloaderService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.soundcloud.android.service.ICloudDownloaderService
{
private static final java.lang.String DESCRIPTOR = "com.soundcloud.android.service.ICloudDownloaderService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.soundcloud.android.service.ICloudDownloaderService interface,
 * generating a proxy if needed.
 */
public static com.soundcloud.android.service.ICloudDownloaderService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.soundcloud.android.service.ICloudDownloaderService))) {
return ((com.soundcloud.android.service.ICloudDownloaderService)iin);
}
return new com.soundcloud.android.service.ICloudDownloaderService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_downloadTrack:
{
data.enforceInterface(DESCRIPTOR);
com.soundcloud.android.objects.Track _arg0;
if ((0!=data.readInt())) {
_arg0 = com.soundcloud.android.objects.Track.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.downloadTrack(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getCurrentDownloadPercentage:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getCurrentDownloadPercentage();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getCurrentDownloadId:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getCurrentDownloadId();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getCurrentDownloadingTrackInfo:
{
data.enforceInterface(DESCRIPTOR);
com.soundcloud.android.objects.Track _result = this.getCurrentDownloadingTrackInfo();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.soundcloud.android.service.ICloudDownloaderService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void downloadTrack(com.soundcloud.android.objects.Track trackdata) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((trackdata!=null)) {
_data.writeInt(1);
trackdata.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_downloadTrack, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public int getCurrentDownloadPercentage() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCurrentDownloadPercentage, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getCurrentDownloadId() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCurrentDownloadId, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public com.soundcloud.android.objects.Track getCurrentDownloadingTrackInfo() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
com.soundcloud.android.objects.Track _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCurrentDownloadingTrackInfo, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = com.soundcloud.android.objects.Track.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_downloadTrack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getCurrentDownloadPercentage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getCurrentDownloadId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getCurrentDownloadingTrackInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
}
public void downloadTrack(com.soundcloud.android.objects.Track trackdata) throws android.os.RemoteException;
public int getCurrentDownloadPercentage() throws android.os.RemoteException;
public java.lang.String getCurrentDownloadId() throws android.os.RemoteException;
public com.soundcloud.android.objects.Track getCurrentDownloadingTrackInfo() throws android.os.RemoteException;
}
