package org.sdrc.collect.uphpmis.android.activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sdrc.collect.uphpmis.android.R;

import services.GPSTracker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity{

    private GoogleMap googleMap; // Might be null if Google Play services APK is not available.
    String areaName,areaLatLong;
    Double myLat,mylong,lat,longt;
    double longitude,latitude;
    List<LatLng> polyz;
    ProgressDialog pDialog;
    GPSTracker gpsTracker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
       areaName=getIntent().getStringExtra("areaName");
       areaLatLong=getIntent().getStringExtra("latlong");
       longitude=getIntent().getDoubleExtra("MyLong", 0.0);
       latitude=getIntent().getDoubleExtra("MyLat", 0.0);
        String splitltlng[]=areaLatLong.split(" ");
        lat=Double.parseDouble(splitltlng[0]);
        longt=Double.parseDouble(splitltlng[1]);
        googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        System.out.println("Area in map****************** latlong in map..." + areaName+"  "+areaLatLong);
        try {
            new GetDirection().execute();
          
        } catch (Exception e) {
          System.out.println("Exception in on create........"+e.toString());
        }

    }

    @SuppressLint("NewApi")
    private void initilizeMap() {
        if (googleMap != null) {

         try {
             System.out.println("llllllllllllatttttttttttt+++++" + lat + "    " + longt);
           
           /*  LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
             LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
             Criteria criteria = new Criteria();
            String provider = lm.getBestProvider(criteria, false);
             Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location location1 = locationManager.getLastKnownLocation(provider);
             System.out.println("Provider " + provider + " has been selected.");*/
             LatLng coordinateCurr = new LatLng(latitude,longitude);
             LatLng coordinateDest = new LatLng(lat, longt);

             CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinateCurr, 3.5f);

             googleMap.addMarker(new MarkerOptions().position(coordinateCurr).title("Your Location"));
             googleMap.addMarker(new MarkerOptions().position(coordinateDest).title(areaName));

             googleMap.moveCamera(yourLocation);
             googleMap.animateCamera(yourLocation);
           //  new GetDirection().execute();
         }
         catch(Exception e)
         {
             System.out.println("Exception in initializeMap.."+e.toString());
         }
           // googleMap.s
            //check if map is created
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //initilizeMap();
    }

    class GetDirection extends AsyncTask<String, String, String>
    {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pDialog = new ProgressDialog(MapsActivity.this);
        pDialog.setMessage("Loading route to."+areaName+" Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        pDialog.show();
    }

        protected String doInBackground(String... args) {

            try {
                Intent i = getIntent();
             
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                myLat =latitude;
                mylong =longitude;
                String spLatLong[]=areaLatLong.split(" ");


                String stringUrl = "http://maps.googleapis.com/maps/api/directions/json?origin="+myLat+","+mylong+"&destination="+lat+","+longt+"&mode=driving&sensor=false";
                System.out.println("Map URL*************" + stringUrl);

                StringBuilder response = new StringBuilder();
                URL url = new URL(stringUrl);
                HttpURLConnection httpconn = (HttpURLConnection) url
                        .openConnection();
                if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(
                            new InputStreamReader(httpconn.getInputStream()),
                            8192);
                    String strLine = null;

                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }

                String jsonOutput = response.toString();
                System.out.println("JSSSSSSSSSSSSSSSSooon "+jsonOutput);
                JSONObject jsonObject = new JSONObject(jsonOutput);

                // routesArray contains ALL routes
                JSONArray routesArray = jsonObject.getJSONArray("routes");
                // Grab the first route
                JSONObject route = routesArray.getJSONObject(0);

                JSONObject poly = route.getJSONObject("overview_polyline");
                String polyline = poly.getString("points");
                polyz = decodePoly(polyline);

            } catch (Exception e) {
             System.out.println("Esception in drawing*************"+e.toString());
            }

            return null;

        }

        protected void onPostExecute(String file_url) {

            for (int i = 0; i < polyz.size() - 1; i++) {
                LatLng src = polyz.get(i);
                LatLng dest = polyz.get(i + 1);
                Polyline line = googleMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude),
                                new LatLng(dest.latitude,                dest.longitude))
                        .width(2).color(Color.RED).geodesic(true));

            }
           initilizeMap();
           pDialog.dismiss();

        }
    }

    /* Method to decode polyline points */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}


