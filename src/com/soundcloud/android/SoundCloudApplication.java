package com.soundcloud.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ContentHandler;
import java.net.URLStreamHandlerFactory;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.urbanstew.soundcloudapi.SoundCloudAPI;
import org.urbanstew.soundcloudapi.SoundCloudOptions;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.http.CountingMultipartRequestEntity;
import com.soundcloud.utils.http.ProgressListener;

@ReportsCrashes(formKey="dGRxVlFzVXB5dy1WX0kwekRTRkNHbGc6MQ" ) 
public class SoundCloudApplication extends Application {

	
	 public static enum Events {
			track, favorite, playlist
		}

		private static final String TAG = "SoundCloudApplication";

		public static String ERROR_UNAUTHORIZED = "401 - unauthorized";

		public static String TYPE_TRACK = "track";
		public static String TYPE_FAVORITE = "favorite";
		public static String TYPE_PLAYLIST = "playlist";
		public static String TYPE_ACTIVITY = "activity";

		public static String ORDER_HOTNESS = "hotness";

		public static String PATH_MY_USERS = "me/followings";
		public static String PATH_MY_FEED = "events";
		public static String PATH_USERS = "users";
		public static String PATH_TRACKS = "tracks";
		public static String PATH_PLAYLISTS = "playlists";
		public static String PATH_MY_DETAILS = "me";
		public static String PATH_MY_ACTIVITIES = "me/activities/tracks";
		public static String PATH_MY_EXCLUSIVE_TRACKS = "me/activities/tracks/exclusive";
		public static String PATH_MY_TRACKS = "me/tracks";
		public static String PATH_MY_PLAYLISTS = "me/playlists";
		public static String PATH_MY_FAVORITES = "me/favorites";
		public static String PATH_MY_FOLLOWERS = "me/followers";
		public static String PATH_MY_FOLLOWINGS = "me/followings";
		public static String PATH_USER_DETAILS = "users/{user_id}";
		public static String PATH_USER_FOLLOWINGS = "users/{user_id}/followings";
		public static String PATH_USER_FOLLOWERS = "users/{user_id}/followers";
		public static String PATH_TRACK_DETAILS = "tracks/{track_id}";
		public static String PATH_USER_TRACKS = "users/{user_id}/tracks";
		public static String PATH_USER_FAVORITES = "users/{user_id}/favorites";
		public static String PATH_USER_PLAYLISTS = "users/{user_id}/playlists";
		public static String PATH_TRACK_COMMENTS = "tracks/{track_id}/comments";
		
		
		private SoundCloudAPI api;
		private ImageLoader mImageLoader;
		
		private static HttpClient httpClient;
		public static String sToken = null, sTokenSecret = null;

		
		
		public static SoundCloudOptions sSoundCloudOptions = 
			//SoundCloudAPI.USE_SANDBOX;
			SoundCloudAPI.USE_PRODUCTION;
		//SoundCloudAPI.USE_SANDBOX.with(OAuthVersion.V2_0);
		//SoundCloudAPI.USE_PRODUCTION.with(OAuthVersion.V2_0);

		
		 public void onCreate()
	     {
			 	ACRA.init(this);
	             super.onCreate();
	             Log.i(TAG,"Creating new image loader");
	             mImageLoader = createImageLoader(this);
	             refreshApi();
	     }
		
		 private void refreshApi(){
			 
			 SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
	 		/* Log.i(TAG,"Getting new soundcloud instance with tokens " +  preferences.getString(
	 								"oauth_access_token", "") + " and " +  preferences.getString(
	 								"oauth_access_token_secret", ""));*/
			 
			 api = new SoundCloudAPI(getConsumerKey(),
 						getConsumerSecret(), preferences.getString(
 								"oauth_access_token", ""), preferences.getString(
 								"oauth_access_token_secret", ""), sSoundCloudOptions);
		 }
		
	
		public final void clearSoundCloudAccount() {
			clearTokens();
			api.unauthorize();
			api = null;
			api = new SoundCloudAPI(getConsumerKey(),
						getConsumerSecret(),"", "", sSoundCloudOptions);
		}

		public InputStream deleteContent(String path) throws IllegalStateException,
				OAuthMessageSignerException, OAuthExpectationFailedException,
				ClientProtocolException, IOException, OAuthCommunicationException {
			return api.delete(path).getEntity().getContent();
		}

		public void clearTokens(){
			PreferenceManager
			.getDefaultSharedPreferences(this).edit().remove("oauth_access_token").remove("oauth_access_token_secret").putString("currentUserId", "")
			.putString("currentUsername", "").commit();
		}
		
		public final Intent getAuthorizationIntent() throws Exception {
			if (api.getState() == SoundCloudAPI.State.AUTHORIZED) {
				return null;
			}

			String authorizationUrl = api.obtainRequestToken("soundcloud://auth")
					+ "&display=popup";

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			preferences.edit().putString("oauth_access_token", api.getToken()).putString("oauth_access_token_secret",api.getTokenSecret()).commit();
			
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
			i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			i.setData(Uri.parse(authorizationUrl));
			return i;
		}
		
		public void updateAuthorizationStatus(String verificationCode) {
			try {
				
				api.obtainAccessToken(verificationCode);
				PreferenceManager.getDefaultSharedPreferences(this).edit().putString("oauth_access_token", api.getToken()).putString("oauth_access_token_secret",api.getTokenSecret()).commit();
				Log.i(TAG,"UPDATED TOKEN " + api.getToken());

			} catch (OAuthMessageSignerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public Document getCloudData(String path) {

			HttpResponse response;
			try {

				response = api.get(path);

				if (response.getStatusLine().getStatusCode() != 200) {
					return null;
				}

				DocumentBuilder db = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder();
				Document dom = db.parse(response.getEntity().getContent());
				dom.getDocumentElement().normalize();

				return dom;

			} catch (OAuthMessageSignerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FactoryConfigurationError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		public String getConsumerKey() {
			if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION) {
				return this.getResources().getString(R.string.consumer_key);
			} else {
				return this.getResources().getString(
						R.string.sandbox_consumer_key);
			}
		}

		public String getConsumerSecret() {
			if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION) {
				return this.getResources().getString(R.string.consumer_secret);
			} else {
				return this.getResources().getString(
						R.string.sandbox_consumer_secret);
			}
		}
		
		public HttpUriRequest getPreparedRequest(String path) throws IllegalStateException,
			OAuthMessageSignerException, OAuthExpectationFailedException,
			ClientProtocolException, IOException, OAuthCommunicationException {
	
		try{api.getToken();} catch (Exception e){refreshApi();}
		HttpUriRequest req = getRequest(path, null);
		req.getParams().setParameter("consumer_key", getConsumerKey());
		req.addHeader("Accept", "application/json");
	
		return req;
	}
		
		public InputStream executeRequest(HttpUriRequest req) throws IllegalStateException, IOException
		{
			HttpResponse response = getHttpClient().execute(req);
			return response.getEntity().getContent();
		}

		public InputStream getContent(String path) throws IllegalStateException,
				OAuthMessageSignerException, OAuthExpectationFailedException,
				ClientProtocolException, IOException, OAuthCommunicationException {

			return executeRequest(getPreparedRequest(path));
		}
		
		public HttpUriRequest getRequest(String path) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
			return getRequest(path, null);
		}
		
		public HttpUriRequest getRequest(String path,List<NameValuePair> params) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
			return api.getRequest(path, params);
		}

		private String getDomain() {
			if (sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION) {
				return "http://api.soundcloud.com/";
			} else {
				return "http://api.sandbox-soundcloud.com/";
			}
		}

		public HttpClient getHttpClient() {
			if (httpClient == null) {
				HttpClient client = new DefaultHttpClient();
				httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(
						client.getParams(), client.getConnectionManager()
								.getSchemeRegistry()), client.getParams());
			}
			return httpClient;

		}

		public HttpResponse getHttpResponse(String path) {

			try {
				return api.get(path);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;

		}

		public SoundCloudAPI.State getState() {
			return api.getState();
		}

		public String getUserName() {
			try {
				Document dom = getCloudData("me");
				return dom.getElementsByTagName("username").item(0).getFirstChild()
						.getNodeValue();

			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}

		
		public HttpEntity putComment(Comment comment) {
			try {
				List<NameValuePair> params = comment.mapDataToParams();
				String path = PATH_TRACK_COMMENTS.replace("{track_id}", comment
						.getData(Comment.key_track_id));
				HttpResponse response = api.post(path + "/", params);
				response.getAllHeaders();

				return response.getEntity();

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;

		}

		public InputStream putContent(String path) {

			try {
				return api.put(path).getEntity().getContent();

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;

		}

		public String signStreamUrl(String path) {
			try {
				return api.signStreamUrl(path) + "&consumer_key="
						+ getConsumerKey();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;

		}

		public String signStreamUrlNaked(String path) {
			try {
				return api.signStreamUrl(path);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;

		}
		
		public HttpResponse upload(ContentBody trackBody,  List<NameValuePair> params, ProgressListener listener) throws OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException, IOException, OAuthCommunicationException
	    {
	            
	            return upload(trackBody,null,params, listener);
	    }
		
		public HttpResponse upload(ContentBody trackBody, ContentBody artworkBody, List<NameValuePair> params, ProgressListener listener) throws OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException, IOException, OAuthCommunicationException
	     {
	             HttpPost post = new HttpPost(urlEncode("tracks", null));  
	             // fix contributed by Bjorn Roche
	             post.getParams().setBooleanParameter( "http.protocol.expect-continue", false );

	             MultipartEntity entity = new MultipartEntity();
	             for(NameValuePair pair : params)
	             {
	                     try
	                     {
	                             entity.addPart(pair.getName(), new StringBodyNoHeaders(pair.getValue()));
	                     } catch (UnsupportedEncodingException e)
	                     {
	                     }  
	             }
	             entity.addPart("track[asset_data]", trackBody);  
	             
	             if (artworkBody != null) entity.addPart("track[artwork_data]", artworkBody);  

	             CountingMultipartRequestEntity countingEntity = new CountingMultipartRequestEntity(entity,listener);
	             post.setEntity(countingEntity);
	             //return api.upload(trackBody,params);
	             return api.performRequest(post);  
	     }
		

		private String urlEncode(String resource, List<NameValuePair> params) {
			String resourceUrl;
			if (resource.startsWith("/")) {
				resourceUrl = getDomain() + resource.substring(1);
			} else {
				resourceUrl = resource.contains("://") ? resource : getDomain()
						+ resource;
			}
			return params == null ? resourceUrl : resourceUrl + "?"
					+ URLEncodedUtils.format(params, "UTF-8");
		}
		
		
		
		class StringBodyNoHeaders extends StringBody
		{
		        public StringBodyNoHeaders(String value) throws UnsupportedEncodingException
		        {
		                super(value);
		        }       
		        
		        public String getMimeType()
		        {
		                return null;
		        }

		        public String getTransferEncoding()
		        {
		                return null;
		        }       
		}
		
		
		
		  private static ImageLoader createImageLoader(Context context) {
		        // Install the file cache (if it is not already installed)
		        CloudCache.install(context);

		        // Just use the default URLStreamHandlerFactory because
		        // it supports all of the required URI schemes (http).
		        URLStreamHandlerFactory streamFactory = null;

		        // Load images using a BitmapContentHandler
		        // and cache the image data in the file cache.
		        ContentHandler bitmapHandler = CloudCache.capture(new BitmapContentHandler(), null);

		        // For pre-fetching, use a "sink" content handler so that the
		        // the binary image data is captured by the cache without actually
		        // parsing and loading the image data into memory. After pre-fetching,
		        // the image data can be loaded quickly on-demand from the local cache.
		        ContentHandler prefetchHandler = CloudCache.capture(CloudCache.sink(), null);

		        // Perform callbacks on the main thread
		        Handler handler = null;

		        return new ImageLoader(streamFactory, bitmapHandler, prefetchHandler, handler);
		    }

		  @Override
		    public Object getSystemService(String name) {
		        if (ImageLoader.IMAGE_LOADER_SERVICE.equals(name)) {
		            return mImageLoader;
		        } else {
		            return super.getSystemService(name);
		        }
		    }
		  
}
