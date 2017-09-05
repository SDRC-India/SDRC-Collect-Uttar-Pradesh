package org.sdrc.collect.uphpmis.android.activities;


import java.util.List;

import org.sdrc.collect.uphpmis.android.R;

import services.GPSTracker;

import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class SearchMyLocation extends Activity {
	String areaName,areaLatLong;
	  double longitude,latitude;
	     GPSTracker gpsTracker;
	     LocationManager locationManager;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		 areaName=getIntent().getStringExtra("areaName");
	       areaLatLong=getIntent().getStringExtra("latlong");
		setContentView(R.layout.mylocation_wait);
		 gpsTracker = new GPSTracker(this);
	        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

	        if (gpsTracker.getIsGPSTrackingEnabled())
	        {
	        	
	        	// Start
	        	Criteria criteria = new Criteria();
	            String provider = locationManager.getBestProvider(criteria, false);
	            Location location = locationManager.getLastKnownLocation(provider);
	            
	           //new LoadPlaces().execute();
	           //Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	           if(location != null)
	           {
	               longitude = location.getLongitude();
	               latitude = location.getLatitude();
	               Log.d("msg", "first lat long : "+latitude +" "+ longitude);
	               //new LoadPlaces().execute();
	           }else
	           {
	               locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {

	                   @Override
	                   public void onStatusChanged(String provider, int status, Bundle extras) {
	                       // TODO Auto-generated method stub

	                   }

	                   @Override
	                   public void onProviderEnabled(String provider) {
	                       // TODO Auto-generated method stub

	                   }

	                   @Override
	                   public void onProviderDisabled(String provider) {
	                       // TODO Auto-generated method stub

	                   }

	                   @Override
	                   public void onLocationChanged(Location location) {
	                       // TODO Auto-generated method stub
	                       longitude = location.getLongitude();
	                       latitude = location.getLatitude();
	                       
	                       Log.d("msg", "changed lat long : "+latitude +" "+ longitude);
	                      // Toast.makeText(SearchMyLocation.this,latitude+"  "+longitude +" changed is the current Loc "+gpsTracker.getLocality(SearchMyLocation.this),Toast.LENGTH_LONG).show();
	                       if(longitude !=0.0)
	                       {
	                    	   Intent i=new Intent(SearchMyLocation.this,MapsActivity.class);
	                    	   i.putExtra("latlong",areaLatLong);
	                           i.putExtra("areaName",areaName);
	                           i.putExtra("MyLat",latitude);
	                           i.putExtra("MyLong",longitude);
	                           startActivity(i);
	                           locationManager.removeUpdates(this);
	                           finish();

	                       }
	                   }
	               });
	           }

	        //End
	        	//String stringLatitude = String.valueOf(gpsTracker.latitude);
	        	//String stringLongitude = String.valueOf(gpsTracker.longitude);
	          // Toast.makeText(this,latitude+"  "+longitude +" is the current Loc", Toast.LENGTH_LONG).show();
	        }
	        else
	        {
	        	 gpsTracker.showSettingsAlert();
	        }
	       
	}

}
