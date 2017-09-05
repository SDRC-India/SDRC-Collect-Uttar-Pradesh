package org.sdrc.collect.uphpmis.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.sdrc.collect.uphpmis.android.R;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
//import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;

import beans.AreaAndId;
import beans.Children;
import beans.LatLong;

public class Facility_activity2 extends Activity {

    Map<ArrayList<String>,ArrayList<String> > levelsMap;
    Map<ArrayList<String>,ArrayList<String> > areaIdMap;
    Map<ArrayList<String>,ArrayList<String> > faciMap;
    ProgressDialog progress,progressLatLong;
    Spinner stateSpinner,districtSpeener,blockSpeener;
    TextView stateText,blockText,districtText;
    String state;
    String idSelected="";
    ArrayList<String> areas1,ids,fareas;
    ArrayList<CharSequence> levels,levNames;
    int levselected;
    String url1,url2,urld,urlb;
    int check=0,check1=0;
    CharSequence areaLevel;
    String latlong,levTextSelected;
    Button draw;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.print("in Faci2--------");
        setContentView(R.layout.activity_facility_activity2);
        levselected=getIntent().getIntExtra("level", 0);
        System.out.print("levselected from intent:::::::::::::::" + levselected);
        levels=getIntent().getCharSequenceArrayListExtra("levels");
        levTextSelected=getIntent().getStringExtra("levTextSelected");
        levNames=getIntent().getCharSequenceArrayListExtra("levNames");
        levNames.remove(0);
        draw=(Button)findViewById(R.id.buttonMap);
        stateSpinner=(Spinner)findViewById(R.id.spinnerState);
        blockText=(TextView)findViewById(R.id.textViewBlock);
        blockSpeener=(Spinner)findViewById(R.id.spinnerBlock1);
        if(levTextSelected.equals("District")) {
            districtText = (TextView) findViewById(R.id.textViewDistrict);
            districtSpeener = (Spinner) findViewById(R.id.spinnerDistrict);
            districtText.setVisibility(View.VISIBLE);
            districtSpeener.setVisibility(View.VISIBLE);
        }
        else  if(levTextSelected.equals("Block")) {
            districtText = (TextView) findViewById(R.id.textViewDistrict);
            districtSpeener = (Spinner) findViewById(R.id.spinnerDistrict);
            districtText.setVisibility(View.VISIBLE);
            districtSpeener.setVisibility(View.VISIBLE);
            blockText.setVisibility(View.VISIBLE);
            blockSpeener.setVisibility(View.VISIBLE);
        }

        /*ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(Facility_activity2.this, android.R.layout.simple_spinner_dropdown_item,levNames);
        stateSpinner.setAdapter(adapter);
        levSpinner.setSelection(levselected);
        stateSpinner=(Spinner)findViewById(R.id.spinnerState);
        facilityText=(TextView)findViewById(R.id.textViewState);

        areaLevel=levels.get(levselected);
        if(levNames.get(levselected).equals("State"))
            facilityText.setText("Select State");
        else if(levNames.get(levselected).equals("District") || levNames.get(levselected).equals("Block"))
            facilityText.setText("Select State");
*/

        Log.d("Area Text== ", levTextSelected);
        url1 = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getAreaAndId?areaLevel="+2;

        new HttpRequestTaskStateRead().execute();
       /* progress = ProgressDialog.show(this, "State names",
                "Reading State facilities from server", true);
        progress.show();*/
        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                check=check+1;
                if(check>1) {
                    areaLevel=levels.get(stateSpinner.getSelectedItemPosition());
                    url1 = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getAreaAndId?areaLevel=" + areaLevel.toString();
                    Log.d("URL::in event::  ", url1);
                    new HttpRequestTaskStateRead().execute();
                 /*   progress = ProgressDialog.show(Facility_activity2.this, "Facility Data",
                            "Reading Facilities from server", true);
                    progress.show();*/
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void mapMe(View view)
    {
        Intent i=new Intent(this,SearchMyLocation.class);

        String areaName=stateSpinner.getSelectedItem().toString();
        if(levTextSelected.equals("District"))
            areaName=districtSpeener.getSelectedItem().toString();
        else if(levTextSelected.equals("Block"))
            areaName=blockSpeener.getSelectedItem().toString();
        i.putExtra("latlong",latlong);
        i.putExtra("areaName",areaName);
        startActivity(i);

    }
    private class HttpRequestTaskStateRead extends AsyncTask<Void, Void, AreaAndId> {
        @Override
        protected AreaAndId doInBackground(Void... params) {
            try {



                //  System.out.print("URL:::::::::::::::"+url);
                Log.d("URL:in thread:   ", url1);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
                AreaAndId aid=restTemplate.getForObject(url1, AreaAndId.class);

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
            ids.add("0");
            areas1.add("Choose");
            for(String ad:arid)
            {
                String splittedValues[]=ad.split("##");
                areas1.add(splittedValues[0]);
                ids.add(splittedValues[1]);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // progress.dismiss();

                }
            });


//            areaIdMap.put(areas1,ids);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(Facility_activity2.this, android.R.layout.simple_spinner_dropdown_item,areas1);
            stateSpinner.setAdapter(adapter);
            stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    String pSelected = stateSpinner.getSelectedItem().toString();
                    //Toast.makeText(Facility_activity2.this, ids.get(stateSpinner.getSelectedItemPosition()), Toast.LENGTH_LONG).show();
                    check1=check1+1;
                    if(check1>1 && !stateSpinner.getSelectedItem().toString().equals("Choose")) {
                        draw=(Button)findViewById(R.id.buttonMap);
                        String areaId = ids.get(stateSpinner.getSelectedItemPosition());
                        if(levTextSelected.equals("State")) {
                            url2 = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getLatLangNew?areaId=" + areaId;
                            Log.d("URL2===", url2);

                            new HttpRequestTaskLatLang().execute();


                        }
                        else
                        {
                            urld="http://180.87.230.91:8089/SSRI_WS/rest/facilities/getChildren?parentId="+areaId;
                            new HttpRequestTaskDistrictRead().execute();
                        }

                    }
                }

                public void onNothingSelected(AdapterView<?> adapterView) {
                    return;
                }
            });
        }

    }
    private class HttpRequestTaskDistrictRead extends AsyncTask<Void, Void, Children> {
        @Override
        protected Children doInBackground(Void... params) {
            try {



                //  System.out.print("URL:::::::::::::::"+url);
                Log.d("URL:in district:   ", urld);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
                Children aid=restTemplate.getForObject(urld, Children.class);

                return aid;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Children aid) {

            final ArrayList<String> arid = aid.getParentId();
            System.out.println(arid.size());
            areas1=new ArrayList<String>();
            ids=new ArrayList<String>();
            ids.add("0");
            check=0;
            areas1.add("Choose");
            for(String ad:arid)
            {
                String splittedValues[]=ad.split("##");
                areas1.add(splittedValues[0]);
                ids.add(splittedValues[1]);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // progress.dismiss();

                }
            });


//            areaIdMap.put(areas1,ids);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(Facility_activity2.this, android.R.layout.simple_spinner_dropdown_item,areas1);
            districtSpeener.setAdapter(adapter);
            districtSpeener.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    String pSelected = districtSpeener.getSelectedItem().toString();
                    // Toast.makeText(Facility_activity2.this, ids.get(districtSpeener.getSelectedItemPosition()), Toast.LENGTH_LONG).show();
                    check1=check1+1;
                    if(check1>1 && !districtSpeener.getSelectedItem().toString().equals("Choose")) {
                        draw=(Button)findViewById(R.id.buttonMap);
                        String areaId = ids.get(districtSpeener.getSelectedItemPosition());
                        if(levTextSelected.equals("District")) {
                            url2 = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getLatLangNew?areaId=" + areaId;
                            Log.d("URL2===", url2);

                            new HttpRequestTaskLatLang().execute();


                        }
                        else
                        {
                            urlb="http://180.87.230.91:8089/SSRI_WS/rest/facilities/getChildren?parentId="+areaId;
                            new HttpRequestTaskBlockRead().execute();
                        }
                    }
                }

                public void onNothingSelected(AdapterView<?> adapterView) {
                    return;
                }
            });
        }

    }
    private class HttpRequestTaskBlockRead extends AsyncTask<Void, Void, Children> {
        @Override
        protected Children doInBackground(Void... params) {
            try {



                //  System.out.print("URL:::::::::::::::"+url);
                Log.d("URL:in block:   ", urlb);
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
                Children aid=restTemplate.getForObject(urlb, Children.class);

                return aid;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Children aid) {

            final ArrayList<String> arid = aid.getParentId();
            System.out.println(arid.size());
            areas1=new ArrayList<String>();
            ids=new ArrayList<String>();
            ids.add("0");
            check=0;
            areas1.add("Choose");
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


//            areaIdMap.put(areas1,ids);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(Facility_activity2.this, android.R.layout.simple_spinner_dropdown_item,areas1);
            blockSpeener.setAdapter(adapter);
            blockSpeener.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    String pSelected = blockSpeener.getSelectedItem().toString();
                    //  Toast.makeText(Facility_activity2.this, ids.get(blockSpeener.getSelectedItemPosition()), Toast.LENGTH_LONG).show();
                    check1=check1+1;
                    if(check1>1 && !blockSpeener.getSelectedItem().toString().equals("Choose")) {
                        draw=(Button)findViewById(R.id.buttonMap);
                        String areaId = ids.get(blockSpeener.getSelectedItemPosition());

                        url2 = "http://180.87.230.91:8089/SSRI_WS/rest/facilities/getLatLangNew?areaId=" + areaId;
                        Log.d("URL2===", url2);

                        new HttpRequestTaskLatLang().execute();


                    }
                }

                public void onNothingSelected(AdapterView<?> adapterView) {
                    return;
                }
            });
        }

    }
    private class HttpRequestTaskLatLang extends AsyncTask<Void, Void, LatLong> {
        @Override
        protected LatLong doInBackground(Void... params) {
            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                     /*   progressLatLong = ProgressDialog.show(Facility_activity2.this, "Location Search",
                                "Getting Lat-Long of Facility", true);
                        progressLatLong.show();*/
                    }
                });

                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
                LatLong latLong=restTemplate.getForObject(url2, LatLong.class);

                return latLong;
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(LatLong ltLong) {

            final String lat = ltLong.getLat();
            final String lang = ltLong.getLang();
            //Toast.makeText(Facility_activity2.this, lat + " " + lang, Toast.LENGTH_LONG).show();
            latlong=lat+" "+lang;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  //  progressLatLong.dismiss();
                }
            });

            if(lat==null || lang==null)
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(Facility_activity2.this);
                alert.setTitle("No Position");
                alert.setMessage("No Lat Long Available");
                alert.setPositiveButton("OK", null);
                alert.show();
            }
            else
            {

                draw.setVisibility(View.VISIBLE);
           /* blockText.setVisibility(View.VISIBLE);
               blockSpeener.setVisibility(View.VISIBLE);*/
            }

        }

    }
}
