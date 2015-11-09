package com.akarthik10.locationreporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class LocationReporterService extends Service implements
LocationListener,
GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener{
 
	   
	String SERVER_URL = "http://utilitaire.in/location/index.php";
	final static String LOGGING_ACTION = "LOGGING_ACTION";
	private int NOTIFICATION_ID = 776066;
	private static GPSThread gpsthread = null;
	String deviceName = "";
    private static final long ONE_MIN = 1000 * 60;
    private static final long TWO_MIN = ONE_MIN * 2;
    private static final long FIVE_MIN = ONE_MIN * 5;
    private static final long POLLING_FREQ = 1000 * 30;
    private static final long FASTEST_UPDATE_FREQ = 1000 * 5;
    private static final float MIN_ACCURACY = 10.0f;
    private static final float MIN_LAST_READ_ACCURACY = 10.0f;

    private LocationRequest mLocationRequest;
    private Location mBestReading = null;

    private GoogleApiClient mGoogleApiClient;
    
    
	/** Called when the service is being created. */
	   @Override
	   public void onCreate() {
	     Toast.makeText(this, "Service created", Toast.LENGTH_SHORT).show();

	     if (!servicesAvailable()) {
	            sendLog("Service stopped. Please install Google Play Services.");
	            stopSelf();
	        }
	        // Use NotificationCompat.Builder to set up our notification.
	        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

	        //icon appears in device notification bar and right hand corner of notification
	        builder.setSmallIcon(R.drawable.ic_launcher);
	        Intent notificationIntent = new Intent(LocationReporterService.this, LocationReporterActivity.class);
	        PendingIntent pendingIntent = PendingIntent.getActivity(LocationReporterService.this, 0,
	                notificationIntent, PendingIntent.FLAG_ONE_SHOT);

	        // Set the intent that will fire when the user taps the notification.
	        builder.setContentIntent(pendingIntent);

	        // Large icon appears on the left of the notification
	        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

	        // Content title, which appears in large type at the top of the notification
	        builder.setContentTitle("Location Reporter Running");

	        // Content text, which appears in smaller text below the title
	        builder.setContentText("Your location is being reported");

	        // The subtext, which appears under the text on newer devices.
	        // This will show-up in the devices with Android 4.2 and above only
	        builder.setSubText("Tap to view status, enable or disable reporting.");
	        builder.setOngoing(true);
	        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

	        // Will display the notification in the notification bar
	        notificationManager.notify(NOTIFICATION_ID, builder.build());
	        
	        
	        
	        mLocationRequest = new LocationRequest();
	        mLocationRequest.setInterval(2000);
	        mLocationRequest.setFastestInterval(1000);
	        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	        
	        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
	        mGoogleApiClient.connect();
	        
	        
	        gpsthread = new GPSThread();
	        gpsthread.start();
	     
	   }

	  

	   /** A client is binding to the service with bindService() */
	   @Override
	   public IBinder onBind(Intent intent) {
	      return null;
	   }

	

	   /** Called when a client is binding to the service with bindService()*/
	   @Override
	   public void onRebind(Intent intent) {

	   }

	   	
	   /** Called when The service is no longer used and is being destroyed */
	   @Override
	   public void onDestroy() {
//		   Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
		   NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		   notificationManager.cancel(NOTIFICATION_ID);
		   gpsthread.interrupt();
		   gpsthread = null;
		   
		   
		   
		   
		   
		   if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			   LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, LocationReporterService.this);
	            mGoogleApiClient.disconnect();
	        }
		   
		   super.onDestroy();
		   
		   
	   }
	   
	   @Override
	   public int onStartCommand(Intent intent, int flags, int startId) {
	     //TODO do something useful
//		   Toast.makeText(this, "Command received", Toast.LENGTH_SHORT).show();
		   
			   
//			   gpsthread.start();
		  
//		     sendLog("Service has been already started");
		  
		 deviceName = intent.getStringExtra("DEVICE_NAME");
	     return Service.START_STICKY;
	   }
	   
	   void sendLog(String s){
		   Intent lastintent = new Intent();
	          lastintent.setAction(LOGGING_ACTION);
	          lastintent.putExtra("DATA", s);
	          sendBroadcast(lastintent);
	          Log.d("D", "Logged");
	   }
	   
	   
	   public class GPSThread extends Thread{
		    private ConnectivityManager cnnxManager;
			private boolean isGPSEnabled;
			private LocationManager locationManager;

			public void run() {

		      while(!Thread.interrupted()) {
		          try{
		        	 Thread.sleep(5000); 
		        	 doSendGPS();
		          }catch(InterruptedException E)
		          {
		        	  sendLog("Service has been interrupted");
		        	  stopSelf();
			          return;
		          }
		      }
		      stopSelf();
		       
		    }

			private void doSendGPS() {
				if(deviceName.trim() == "")
				{
					sendLog("Error, device name is empty");
	                return;
				}
				cnnxManager = (ConnectivityManager) 
		                getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo ni = cnnxManager.getActiveNetworkInfo();
				 if (ni == null || !ni.isAvailable() || !ni.isConnected()) {
		                sendLog("No internet connection available, data not sent");
		                return;
		            }
				 
				 locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				 isGPSEnabled = locationManager
			                .isProviderEnabled(LocationManager.GPS_PROVIDER);
				    final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

				    final String tmDevice, tmSerial, androidId;
				    tmDevice = "" + tm.getDeviceId();
				    tmSerial = "" + tm.getSimSerialNumber();
				    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

				    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
				    String deviceId = deviceUuid.toString();
				 if(!isGPSEnabled)
				 {
					 sendLog("GPS is disabled, data not sent");
					 return;
				 }
				 if(mBestReading == null)
				 {
					 sendLog("Waiting for GPS to lock..");
				 }
				 else
				 { 		sendLog("Sent location: LAT "+mBestReading.getLatitude()+", LON "+mBestReading.getLongitude());
					    HttpClient httpclient = new DefaultHttpClient();
					    HttpResponse response;
						try {
							response = httpclient.execute(new HttpGet(SERVER_URL+"?latitude="+mBestReading.getLatitude()+"&longitude="+mBestReading.getLongitude()+"&speed="+(mBestReading.getSpeed() * 3.6)+"&device="+URLEncoder.encode(deviceName, "utf-8")+"&deviceId="+URLEncoder.encode(deviceId, "utf-8")));
							StatusLine statusLine = response.getStatusLine();
						    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
						        ByteArrayOutputStream out = new ByteArrayOutputStream();
						        response.getEntity().writeTo(out);
						        String responseString = out.toString();
						        out.close();
						        sendLog("Got response: "+responseString);
						    } else{
						        //Closes the connection.
						        response.getEntity().getContent().close();
						        sendLog("An error occured in making a HTTP Request - "+ statusLine.getReasonPhrase());
						    }
						} catch (ClientProtocolException e) {
							sendLog("An error occured in making a HTTP Request - ClientProtocolException");
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							sendLog("An error occured in making a HTTP Request - IOException");
						}
					    
				 }
				 
				
			}
		    
		  
		   
	   }


	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
    public void onConnected(Bundle dataBundle) {
        // Get first reading. Get additional location updates if necessary
        if (servicesAvailable()) {
            // Get best last location measurement meeting criteria
            mBestReading = bestLastKnownLocation(MIN_LAST_READ_ACCURACY, 1);

            if (null == mBestReading
                    || mBestReading.getAccuracy() > MIN_LAST_READ_ACCURACY
                    || mBestReading.getTime() < System.currentTimeMillis() - TWO_MIN || 1==1) {

                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                
//                // Schedule a runnable to unregister location listeners
//                Executors.newScheduledThreadPool(1).schedule(new Runnable() {
//
//                    @Override
//                    public void run() {
////                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, LocationReporterService.this);
//                    }
//
//                }, ONE_MIN, TimeUnit.MILLISECONDS);
            }
        }
    }



	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
    public void onLocationChanged(Location location) {
        // Determine whether new location is better than current best
        // estimate
		mBestReading = location;
        if (null == mBestReading || location.getAccuracy() < mBestReading.getAccuracy()) {
            mBestReading = location;

            if (mBestReading.getAccuracy() < MIN_ACCURACY) {
//                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            }
        }
    }
	
	 private boolean servicesAvailable() {
	        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

	        if (ConnectionResult.SUCCESS == resultCode) {
	            return true;
	        }
	        else {
//	            GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0).show();
	        	sendLog("Google play services not available, data not sent");
	            return false;
	        }
	    }
	 
	 private Location bestLastKnownLocation(float minAccuracy, long minTime) {
	        Location bestResult = null;
	        float bestAccuracy = Float.MAX_VALUE;
	        long bestTime = Long.MIN_VALUE;

	        // Get the best most recent location currently available
	        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

	        if (mCurrentLocation != null) {
	            float accuracy = mCurrentLocation.getAccuracy();
	            long time = mCurrentLocation.getTime();

	            if (accuracy < bestAccuracy) {
	                bestResult = mCurrentLocation;
	                bestAccuracy = accuracy;
	                bestTime = time;
	            }
	        }

	        // Return best reading or null
	        if (bestAccuracy > minAccuracy || bestTime < minTime) {
	            return null;
	        }
	        else {
	            return bestResult;
	        }
	    }
		 
}
