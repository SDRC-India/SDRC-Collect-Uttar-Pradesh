package org.sdrc.collect.uphpmis.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.sdrc.collect.uphpmis.android.R;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
//import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import beans.AreaAndId;
import beans.LatLong;
import beans.Levels;

public class Facility_activity extends Activity {
    Map<ArrayList<CharSequence>,ArrayList<CharSequence> > levelsMap;
    Map<ArrayList<String>,ArrayList<String> > areaIdMap;
    Map<ArrayList<String>,ArrayList<String> > faciMap;
    ProgressDialog progress;
    Spinner levSpinner,stateSpinner,faciltitySpeener;
    TextView stateText,facilityText;
    String state;
    String idSelected="";
    ArrayList<String> areas1,ids,fareas;
    ArrayList<CharSequence> levels,levNames;
    CharSequence levselected;
    int check=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facility_activity);
            levSpinner = (Spinner) findViewById(R.id.spinnerLev);


            levSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    CharSequence levText = levSpinner.getSelectedItem().toString();
                    check = check + 1;
                    if (check > 1 && !levText.equals("Choose")) {
                   /* levselected = levels.get(levSpinner.getSelectedItemPosition());
                    System.out.println("in side levSpnner on select************************"+levselected);*/

                        System.out.println("in side levSpnner on levtext************************" + levText);
                        Intent i = new Intent(Facility_activity.this, Facility_activity2.class);
                        i.putExtra("level", levSpinner.getSelectedItemPosition() - 1);
                        i.putCharSequenceArrayListExtra("levels", levels);
                        i.putCharSequenceArrayListExtra("levNames", levNames);
                        i.putExtra("levTextSelected", levText);
                        Facility_activity.this.startActivity(i);
                        System.out.println("went to start act2........");
                   } 

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
      
            levelsMap = new HashMap<ArrayList<CharSequence>, ArrayList<CharSequence>>();
            areaIdMap = new HashMap<ArrayList<String>, ArrayList<String>>();
            faciMap = new HashMap<ArrayList<String>, ArrayList<String>>();
            new HttpRequestTask().execute();
    }

    public boolean isOnline() {

        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }

        return false;
    }
    private class HttpRequestTask extends AsyncTask<Void, Void, Levels> {
        @Override
        protected Levels doInBackground(Void... params) {
            try {
                final String url = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getLevels";
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
                Levels level=restTemplate.getForObject(url, Levels.class);
                return level;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Levels lev) {

            levels=lev.getLevels();
            System.out.println(levels.size());
            levels.remove(3);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // progress.dismiss();
                }
            });

            int counter=1;
            levNames=new ArrayList<CharSequence>();
            levNames.add("Choose");
            levNames.add("State");
            levNames.add("District");
            levNames.add("Block");
            levelsMap.put(levels, levNames);
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(Facility_activity.this, android.R.layout.simple_spinner_dropdown_item,levNames);
            levSpinner.setAdapter(adapter);


        }

    }
    private class HttpRequestTaskStateRead extends AsyncTask<Void, Void, AreaAndId> {
        @Override
        protected AreaAndId doInBackground(Void... params) {
            try {


                final String url = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getAreaAndId?areaLevel="+levselected;
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
                AreaAndId aid=restTemplate.getForObject(url, AreaAndId.class);

                return aid;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(AreaAndId aid) {

            final ArrayList<String> arid = aid.getAreaAndId();
            System.out.println(arid.size());
            areas1=new ArrayList<String>();
            ids=new ArrayList<String>();
            for(String ad:arid)
            {
                String splittedValues[]=ad.split("##");
                areas1.add(splittedValues[0]);
                ids.add(splittedValues[1]);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  //  progress.dismiss();

                }
            });


            areaIdMap.put(areas1, ids);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(Facility_activity.this, android.R.layout.simple_spinner_dropdown_item,areas1);
            stateSpinner.setAdapter(adapter);
            stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    String pSelected = levSpinner.getSelectedItem().toString();
                    Toast.makeText(Facility_activity.this, ids.get(stateSpinner.getSelectedItemPosition()), Toast.LENGTH_LONG).show();

                }

                public void onNothingSelected(AdapterView<?> adapterView) {
                    return;
                }
            });
        }

    }
    private class HttpRequestTaskFacRead extends AsyncTask<Void, Void, AreaAndId> {
        @Override
        protected AreaAndId doInBackground(Void... params) {
            try {


                final String url = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getAreaAndId?areaLevel="+idSelected;
                System.out.println("in side levSpnner doinbackground  on select*****URL*******************"+idSelected);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
                AreaAndId aid=restTemplate.getForObject(url, AreaAndId.class);

                return aid;
            }

            catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }


            return null;
        }

        @Override
        protected void onPostExecute(AreaAndId aid)
        {

            final ArrayList<String> arid = aid.getAreaAndId();
            System.out.println(arid.size());
            fareas=new ArrayList<String>();

            ids=new ArrayList<String>();
            for(String ad:arid)
            {
                String splittedValues[]=ad.split("##");
                fareas.add(splittedValues[0]);
                ids.add(splittedValues[1]);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  //  progress.dismiss();
                }
            });


            faciMap.put(fareas, ids);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(Facility_activity.this, android.R.layout.simple_spinner_dropdown_item,fareas);
            faciltitySpeener.setAdapter(adapter);

        }

    }
    private class HttpRequestTaskFacility extends AsyncTask<Void, Void, LatLong> {
        @Override
        protected LatLong doInBackground(Void... params){
            try {


                final String url = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getLatLangNew?areaId="+levselected;
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
                LatLong latLong=restTemplate.getForObject(url, LatLong.class);

                return latLong;
            }

            catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }


            return null;
        }

        @Override
        protected void onPostExecute(LatLong ltLong) {

            final String lat = ltLong.getLat();
            final String lang = ltLong.getLang();
            Toast.makeText(Facility_activity.this, lat + " " + lang, Toast.LENGTH_LONG).show();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                 //   progress.dismiss();
                }
            });


            /*areaIdMap.put(areas,ids);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(Facility_activity.this, android.R.layout.simple_spinner_dropdown_item,areas);
            stateSpinner.setAdapter(adapter);
            stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    String pSelected= levSpinner.getSelectedItem().toString();
                    Toast.makeText(Facility_activity.this,ids.get(stateSpinner.getSelectedItemPosition()),Toast.LENGTH_LONG).show();

                }

                public void onNothingSelected(AdapterView<?> adapterView) {
                    return;
                }
            });*/
        }

    }

}
