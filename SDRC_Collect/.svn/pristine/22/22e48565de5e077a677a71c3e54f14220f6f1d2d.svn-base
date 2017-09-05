package org.sdrc.collect.uphpmis.android.tasks;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.sdrc.collect.uphpmis.android.listeners.SubmissionFileDownloadListener;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

public class DownloadFileAsyncTask extends AsyncTask<String, String, HashMap<Integer, HashMap<InputStream, List<String>>>> {
	
	private SubmissionFileDownloadListener submissionFileDownloadListener;
	
	private static final int TIMEOUT_CONNECTION = 30000;
	private static final int TIMEOUT_SOCKET = 40000;

	@SuppressLint("UseSparseArrays")
	@Override
	protected HashMap<Integer, HashMap<InputStream, List<String>>> doInBackground(String... params) {
		// TODO Auto-generated method stub
		
		try{
			URL url = new URL(params[0] + "getSubmissionFile");

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(55000);
			conn.setConnectTimeout(60000);
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);

			List<NameValuePair> paramList = new ArrayList<NameValuePair>();
			paramList.add(new BasicNameValuePair("areaId", params[1]));
			paramList.add(new BasicNameValuePair("secondaryAreaName", params[2]));
			paramList.add(new BasicNameValuePair("xFormId", params[3]));
			paramList.add(new BasicNameValuePair("userString", params[4]));

			OutputStream os = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(getQuery(paramList));
			writer.flush();
			writer.close();
			os.close();

			conn.connect();
			
			
			int code = conn.getResponseCode();
			
			InputStream is = conn.getInputStream();
			
			List<String> cd = conn.getHeaderFields().get("Content-Disposition");
			
			if(cd.size()>0){				
				try{
					code = Integer.parseInt(cd.get(0));					
				}catch(NumberFormatException e){
					
					//not exception 404/401
					code = 200;
				}				
			}else{
				return null;
			}			
			HashMap<Integer, HashMap<InputStream, List<String>>> data = new HashMap<Integer, HashMap<InputStream, List<String>>>();
			if(code == 200){
				HashMap<InputStream, List<String>> iData = new HashMap<InputStream, List<String>>();
				iData.put(is, cd);
				data.put(code, iData);			
			}else{
				data.put(code, null);
			}
			return data;				
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	protected void onPostExecute(HashMap<Integer, HashMap<InputStream, List<String>>> result) {
		// TODO Auto-generated method stub
		synchronized (this) {
            if (submissionFileDownloadListener != null) {
            	submissionFileDownloadListener.submissionFileDownloadComplete(result);
            }
        }
	}

	public void setSubmissionFileDownloadListener(SubmissionFileDownloadListener submissionFileDownloadListener) {
		this.submissionFileDownloadListener = submissionFileDownloadListener;
	}

	private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
	{
	    StringBuilder result = new StringBuilder();
	    boolean first = true;

	    for (NameValuePair pair : params)
	    {
	        if (first)
	            first = false;
	        else
	            result.append("&");

	        result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
	    }

	    return result.toString();
	}
}
