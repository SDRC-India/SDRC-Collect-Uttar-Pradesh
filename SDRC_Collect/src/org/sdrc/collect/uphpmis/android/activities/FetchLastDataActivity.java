package org.sdrc.collect.uphpmis.android.activities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sdrc.collect.uphpmis.android.R;
import org.sdrc.collect.uphpmis.android.application.Collect;
import org.sdrc.collect.uphpmis.android.listeners.FetchAreaForXForm;
import org.sdrc.collect.uphpmis.android.listeners.FormSavedListener;
import org.sdrc.collect.uphpmis.android.listeners.SubmissionFileDownloadListener;
import org.sdrc.collect.uphpmis.android.logic.FormController;
import org.sdrc.collect.uphpmis.android.preferences.PreferencesActivity;
import org.sdrc.collect.uphpmis.android.provider.FormsProviderAPI.FormsColumns;
import org.sdrc.collect.uphpmis.android.provider.InstanceProviderAPI;
import org.sdrc.collect.uphpmis.android.provider.InstanceProviderAPI.InstanceColumns;
import org.sdrc.collect.uphpmis.android.tasks.DownloadFileAsyncTask;
import org.sdrc.collect.uphpmis.android.tasks.FetchAreaForXFormAsyncTask;
import org.sdrc.collect.uphpmis.android.tasks.SaveResult;
import org.sdrc.collect.uphpmis.android.tasks.SaveToDiskTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author Amit Kumar Sahoo (amit@sdrc.co.in)
 * @author Ratikanta Pradhan (ratikanta@sdrc.co.in)
 *
 */

@SuppressWarnings("deprecation")
public class FetchLastDataActivity extends Activity
		implements FormSavedListener, SubmissionFileDownloadListener, FetchAreaForXForm, View.OnClickListener {

	SQLiteDatabase db = null;
	private String[] formList;
	private Button download_button;
	private Spinner assignedFormList;
	public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
	public static final int DIALOG_AREADETAILS_PROGRESS = 1;
	private ProgressDialog mProgressDialog;
	private String xFormId = null;
	private Uri uri = null;
	private static final boolean EXIT = true;
	private SaveToDiskTask mSaveToDiskTask;
	private DownloadFileAsyncTask downloadFileAsyncTask;
	private FetchAreaForXFormAsyncTask fetchAreaForXFormAsyncTask;
	private String url = null;
	private HashMap<String, String> map;
	private Button getAreaDetails_button;
	boolean downloadButtonClicked = false;
	private JSONArray areaModels;
	// private int areaLevel;
	private int[] levels;
	private HashMap<EditText, Integer> mapAreaLevel;
	private int tempLevel = 0;
	private int tempAreaLevel = 0;
	private int tempParentId = -1;
	private EditText tempEditText;
	private JSONArray models;
	private JSONArray areaLevelModels;
	private JSONObject secondaryAreas;
	private int selectedLastAreaId;
	private String selectedSecondaryArea = "";
	String[] areaLevelName;

	private TextView leble1;
	private TextView leble2;
	private TextView leble3;
	private TextView leble4;
	private TextView leble5;
	private TextView secondary_area_leble;

	private EditText level1;
	private EditText level2;
	private EditText level3;
	private EditText level4;
	private EditText level5;
	private EditText secondary_area;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fetch_last_data);

		leble1 = (TextView) findViewById(R.id.leble1);
		leble2 = (TextView) findViewById(R.id.leble2);
		leble3 = (TextView) findViewById(R.id.leble3);
		leble4 = (TextView) findViewById(R.id.leble4);
		leble5 = (TextView) findViewById(R.id.leble5);
		secondary_area_leble = (TextView) findViewById(R.id.secondary_area);

		level1 = (EditText) findViewById(R.id.level1);
		level2 = (EditText) findViewById(R.id.level2);
		level3 = (EditText) findViewById(R.id.level3);
		level4 = (EditText) findViewById(R.id.level4);
		level5 = (EditText) findViewById(R.id.level5);
		secondary_area = (EditText) findViewById(R.id.secondary_area_level);

		

		clearAllFields();

		level1.setKeyListener(null);
		level2.setKeyListener(null);
		level3.setKeyListener(null);
		level4.setKeyListener(null);
		level5.setKeyListener(null);
		secondary_area.setKeyListener(null);
		level1.setOnClickListener(this);
		level2.setOnClickListener(this);
		level3.setOnClickListener(this);
		level4.setOnClickListener(this);
		level5.setOnClickListener(this);
		secondary_area.setOnClickListener(this);

		mProgressDialog = new ProgressDialog(this);
		getAreaDetails_button = (Button) findViewById(R.id.get_area);
		download_button = (Button) findViewById(R.id.download_button);
		getAreaDetails_button.setEnabled(false);
		download_button.setEnabled(false);
		mapAreaLevel = new HashMap<EditText, Integer>();
		String sortOrder = FormsColumns.DISPLAY_NAME + " ASC, " + FormsColumns.JR_VERSION + " DESC";
		String selection = Collect.getInstance().getFormIdWhereClauseString() != null
				? Collect.getInstance().getFormIdWhereClauseString() : "0 = 1";
		String[] projection = { "jrFormId", "displayName" };
		Cursor c = managedQuery(FormsColumns.CONTENT_URI, projection, selection, null, sortOrder);
		map = new HashMap<String, String>();
		formList = new String[c.getCount() + 1];
		formList[0] = getString(R.string.select_a_form);
		int i = 1;
		while (c.moveToNext()) {
			String formId = c.getString(0);
			String name = c.getString(1);
			map.put(name, formId);
			formList[i] = c.getString(1);
			i++;
		}
		assignedFormList = (Spinner) findViewById(R.id.form_list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item_layout, formList);
		assignedFormList.setAdapter(adapter);
		assignedFormList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
				if (position != 0) {
					clearAllFields();
					download_button.setEnabled(false);
					Collect.getInstance().setFormToDownload(map.get(formList[position]));
					String xFormId = Collect.getInstance().getFormToDownload();
					getAreaDetails_button.setEnabled(true);
				} else {
					getAreaDetails_button.setEnabled(false);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {
			}
		});

		getAreaDetails_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				clearAllFields();
				xFormId = Collect.getInstance().getFormToDownload();
				showDialog(DIALOG_AREADETAILS_PROGRESS);
				fetchAreaForXForm();
			}
		});

		download_button = (Button) findViewById(R.id.download_button);
		download_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				downloadButtonClicked = true;
				xFormId = Collect.getInstance().getFormToDownload();
				// fetchAreaForXForm();
				System.out.println(selectedSecondaryArea);// secondaryAreaName
				System.out.println(selectedLastAreaId);// areaId
				System.out.println(xFormId);// xFormId
				String selection = "jrFormId = ?";
				String[] selectionArgs = { xFormId };
				
				Cursor c = managedQuery(FormsColumns.CONTENT_URI, null,
				selection, selectionArgs, null);
				if (c.moveToNext()) {
					uri = ContentUris.withAppendedId(FormsColumns.CONTENT_URI, c.getLong(c.getColumnIndex("_id")));
				}
				// url = getString(R.string.default_web_server_url) +
				// "getSubmissionFile?areaId=3&xFormId=" + xFormId;
				if (uri != null) {
					showDialog(DIALOG_DOWNLOAD_PROGRESS);
				 	downloadFile();
				 	//dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
				} else {
					dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
					Log.e("FetchLastDataActivity", "uri found null for xFormId :" + xFormId);
					Toast.makeText(getApplicationContext(),
					getString(R.string.problem_in_form_fetching),
					Toast.LENGTH_SHORT).show();
				 }

			}

		});

	}

	/**
	 * @author Ratikanta Pradhan (ratikanta@sdrc.co.in)
	 */
	private void fetchAreaForXForm() {
		// TODO Auto-generated method stub

		// fetching username and password

		String username = null;
		String password = null;

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
		username = settings.getString(PreferencesActivity.USERNAME, "");
		password = settings.getString(PreferencesActivity.PASSWORD, "");

		fetchAreaForXFormAsyncTask = new FetchAreaForXFormAsyncTask();
		fetchAreaForXFormAsyncTask.setFetchAreaForXForm(this);
		fetchAreaForXFormAsyncTask.execute(getString(R.string.default_web_server_url), xFormId,
				Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT));
	}

	/**
	 * @author Ratikanta Pradhan (ratikanta@sdrc.co.in)
	 */
	protected void downloadFile() {
		// TODO Auto-generated method stub
		String username = null;
		String password = null;

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
		username = settings.getString(PreferencesActivity.USERNAME, "");
		password = settings.getString(PreferencesActivity.PASSWORD, "");

		downloadFileAsyncTask = new DownloadFileAsyncTask();
		downloadFileAsyncTask.setSubmissionFileDownloadListener(this);
		downloadFileAsyncTask.execute(getString(R.string.default_web_server_url), String.valueOf(selectedLastAreaId),
				selectedSecondaryArea, xFormId,
				Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT));
	}

	/**
	 * @author Ratikanta Pradhan (ratikanta@sdrc.co.in)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void submissionFileDownloadComplete(HashMap<Integer, HashMap<InputStream, List<String>>> result) {
		// TODO Auto-generated method stub
		if (result != null) {
			try {

				// iterate through the global map to get id and (InputStream,
				// Filename)
				Iterator it = result.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry pair = (Map.Entry) it.next();

					int responseCode = (Integer) pair.getKey();
					switch (responseCode) {
					case 200:

						HashMap<InputStream, List<String>> inputStreamAndFilenames = (HashMap<InputStream, List<String>>) pair
								.getValue();

						// iterate over the inner map
						Iterator itInner = inputStreamAndFilenames.entrySet().iterator();

						while (itInner.hasNext()) {

							Map.Entry pairInner = (Map.Entry) itInner.next();

							List<String> filenames = (List<String>) pairInner.getValue();
							InputStream is = (InputStream) pairInner.getKey();

							if (filenames != null) {
								if (filenames.size() > 0) {
									String fileName = filenames.get(0);

									fileName = fileName.split("attachment; filename=\"")[1];

									fileName = fileName.substring(0, fileName.length() - 1);

									if (fileName.substring(fileName.length() - 4).equals(".xml")) {

										String folderName = fileName.substring(0, fileName.length() - 4);

										File dir = new File(Collect.INSTANCES_PATH + File.separator + folderName);
										if (dir.exists() == false) {
											dir.mkdirs();
										}

										File file = new File(dir, fileName);
										BufferedInputStream bufferinstream = new BufferedInputStream(is);

										ByteArrayBuffer baf = new ByteArrayBuffer(5000);
										int current = 0;
										while ((current = bufferinstream.read()) != -1) {
											baf.append((byte) current);
										}

										FileOutputStream fos = new FileOutputStream(file);
										fos.write(baf.toByteArray());
										fos.flush();
										fos.close();

										// erase previous downloaded data
										deleteDownLoadedForm();

										// save the data
										Collect.getInstance().setFormDownloaded(true);
										FormController formController = new FormController(null, null, file);
										Collect.getInstance().setFormController(formController);
										saveDataToDisk(EXIT, isInstanceComplete(false), null);
									} else {
										dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
										Toast.makeText(getApplicationContext(),
												getString(R.string.invalid_file_from_server), Toast.LENGTH_SHORT)
												.show();
									}

								} else {
									dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
									Toast.makeText(getApplicationContext(),
											getString(R.string.last_visit_data_not_found), Toast.LENGTH_SHORT).show();
								}
							} else {
								dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
								Toast.makeText(getApplicationContext(), getString(R.string.last_visit_data_not_found),
										Toast.LENGTH_LONG).show();
							}
						}
						dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
						break;

					case 500:
						dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
						Toast.makeText(getApplicationContext(), getString(R.string.some_other_server_error),
								Toast.LENGTH_LONG).show();
						break;
					case 404:
						dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
						Toast.makeText(getApplicationContext(), getString(R.string.last_visit_data_not_found),
								Toast.LENGTH_LONG).show();
						break;
					case 401:
						dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
						Toast.makeText(getApplicationContext(), getString(R.string.username_not_found),
								Toast.LENGTH_LONG).show();
						break;
					default:
						Toast.makeText(getApplicationContext(),
								getString(R.string.problem_in_form_fetching) + "  " + responseCode, Toast.LENGTH_LONG)
								.show();
						dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
						break;
					}
					break;
				}

			} catch (Exception e) {
				e.printStackTrace();
				dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
				Toast.makeText(getApplicationContext(), getString(R.string.problem_in_form_fetching), Toast.LENGTH_LONG)
						.show();
			}
		} else {
			dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
			Toast.makeText(getApplicationContext(), getString(R.string.problem_in_form_fetching), Toast.LENGTH_LONG)
					.show();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DOWNLOAD_PROGRESS:
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
			mProgressDialog.setTitle(getString(R.string.downloading_submission_file));
			mProgressDialog.setMessage(getString(R.string.submission_file_download_msg));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
			return mProgressDialog;
		case DIALOG_AREADETAILS_PROGRESS:
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
			mProgressDialog.setTitle(getString(R.string.getting_area_details));
			mProgressDialog.setMessage(getString(R.string.getting_areadetails_msg));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
			return mProgressDialog;
		default:
			return null;
		}
	}

	/**
	 * Saves data and writes it to disk. If exit is set, program will exit after
	 * save completes. Complete indicates whether the user has marked the
	 * isntancs as complete. If updatedSaveName is non-null, the instances
	 * content provider is updated with the new name
	 */
	// by default, save the current screen
	private boolean saveDataToDisk(boolean exit, boolean complete, String updatedSaveName) {
		return saveDataToDisk(exit, complete, updatedSaveName, true);
	}

	// but if you want save in the background, can't be current screen
	private boolean saveDataToDisk(boolean exit, boolean complete, String updatedSaveName, boolean current) {

		mSaveToDiskTask = new SaveToDiskTask(uri, exit, complete, updatedSaveName);
		mSaveToDiskTask.setFormSavedListener(this);
		// showDialog(SAVING_DIALOG);
		// show dialog before we execute...
		mSaveToDiskTask.execute();

		return true;
	}

	/**
	 * Checks the database to determine if the current instance being edited has
	 * already been 'marked completed'. A form can be 'unmarked' complete and
	 * then resaved.
	 *
	 * @return true if form has been marked completed, false otherwise.
	 */
	private boolean isInstanceComplete(boolean end) {
		FormController formController = Collect.getInstance().getFormController();
		// default to false if we're mid form
		boolean complete = true;

		// if we're at the end of the form, then check the preferences
		if (end) {
			// First get the value from the preferences
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			complete = sharedPreferences.getBoolean(PreferencesActivity.KEY_COMPLETED_DEFAULT, true);
		}

		// Then see if we've already marked this form as complete before
		String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
		String[] selectionArgs = { formController.getInstancePath().getAbsolutePath() };
		Cursor c = null;
		try {
			c = getContentResolver().query(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				String status = c.getString(c.getColumnIndex(InstanceColumns.STATUS));
				if (InstanceProviderAPI.STATUS_COMPLETE.compareTo(status) == 0) {
					complete = true;
				}
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return complete;
	}

	@Override
	public void onProgressStep(String stepMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void savingComplete(SaveResult saveStatus) {
		// TODO Auto-generated method stub
		dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
		Collect.getInstance().setFormDownloaded(false);
		Toast.makeText(getApplicationContext(), getString(R.string.file_download_successful), Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * @author Ratikanta Pradhan (ratikanta@sdrc.co.in)
	 */
	private void deleteDownLoadedForm() {
		// TODO Auto-generated method stub
		String[] selectionArgs = { InstanceProviderAPI.STATUS_DOWNLOADED, xFormId };
		String selection = InstanceColumns.STATUS + " = ? AND " + InstanceColumns.JR_FORM_ID + " = ?";
		Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, null);

		for (int i = 1; i <= c.getCount(); i++) {
			Long l = (long) i;
			try {
				Uri deleteForm = Uri.withAppendedPath(InstanceColumns.CONTENT_URI, l.toString());

				int wasDeleted = Collect.getInstance().getContentResolver().delete(deleteForm, null, null);

				if (wasDeleted == 0)
					break;
			} catch (Exception ex) {
				Log.e("FetchLastDataActivity",
						"Exception during delete of: " + l.toString() + " exception: " + ex.toString());
			}
		}
	}

	@Override
	public void fetchingAreaForXFormComplete(HashMap<Integer, String> result) {
		// TODO Auto-generated method stub

		// TODO Auto-generated method stub
		if (!downloadButtonClicked) {
			try {
				// System.out.println(result);

				if (!(result.containsKey(0))) {
					String s = result.get(1);
					JSONObject response = new JSONObject(s);
					areaLevelModels = response.getJSONArray("areaLevelModels");
					models = response.getJSONArray("areaModels");
					if (models.length() > 0) {
						secondaryAreas = response.getJSONObject("secondaryAreas");
						// levels = new String[areaLevelModels.length()];
						// int j =0;
						HashMap<Integer, String> levelAndName = new HashMap<Integer, String>();
						areaLevelName = new String[areaLevelModels.length()];
						levels = new int[areaLevelModels.length()];
						for (int i = 0; i < areaLevelModels.length(); i++) {
							JSONObject lebleModels = areaLevelModels.getJSONObject(i);
							/*
							 * if(lebleModels.getInt("areaLevelParentId") == -1)
							 * areaLevel = lebleModels.getInt("areaLevel");
							 */
							if (lebleModels.getInt("areaLevel") == 1) {
								// levels[i] = lebleModels.getInt("areaLevel");
								// areaLevelName[0] =
								// lebleModels.getString("areaLevelName");
								leble1.setText("Select a " + lebleModels.getString("areaLevelName"));
							} else if (lebleModels.getInt("areaLevel") == 2) {
								// levels[i] = lebleModels.getInt("areaLevel");
								// areaLevelName[1] =
								// lebleModels.getString("areaLevelName");
								leble2.setText("Select a " + lebleModels.getString("areaLevelName"));
							} else if (lebleModels.getInt("areaLevel") == 3) {
								// levels[i] = lebleModels.getInt("areaLevel");
								// areaLevelName[2] =
								// lebleModels.getString("areaLevelName");
								leble3.setText("Select a " + lebleModels.getString("areaLevelName"));
							} else if (lebleModels.getInt("areaLevel") == 4) {
								// levels[i] = lebleModels.getInt("areaLevel");
								// areaLevelName[3] =
								// lebleModels.getString("areaLevelName");
								leble4.setText("Select a " + lebleModels.getString("areaLevelName"));
							} else if (lebleModels.getInt("areaLevel") == 5) {
								// levels[i] = lebleModels.getInt("areaLevel");
								// areaLevelName[4] =
								// lebleModels.getString("areaLevelName");
								leble5.setText("Select a " + lebleModels.getString("areaLevelName"));
							}
							levelAndName.put(lebleModels.getInt("areaLevel"), lebleModels.getString("areaLevelName"));
						}
						// areaModels = response.getJSONArray("areaModels");
						int k = 0;
						Iterator entries = levelAndName.entrySet().iterator();
						while (entries.hasNext()) {
							Map.Entry entry = (Map.Entry) entries.next();
							levels[k] = (Integer) entry.getKey();
							areaLevelName[k] = (String) entry.getValue();
							k++;
							// System.out.println("Key = " + key + ", Value = "
							// +
							// value);
						}
						enableEditText();
						dismissDialog(DIALOG_AREADETAILS_PROGRESS);
					}
					else{
						dismissDialog(DIALOG_AREADETAILS_PROGRESS);
						Toast.makeText(getApplicationContext(), "Previous data is not availablr for this form", Toast.LENGTH_SHORT).show();
					}
				}
				
				else {
					String s = result.get(1);
					s = result.get(0);
					int resultNumber = Integer.parseInt(s);
					switch (resultNumber) {
					case 0:
						s = getString(R.string.invalid_username_password);
						break;
					case 1:
						s = getString(R.string.some_other_server_error);
						break;
					case 2:
						s = getString(R.string.server_not_found);
						break;
					case 3:
						s = getString(R.string.request_timeout);
						break;
					case 4:
						s = getString(R.string.error_processing_fetch_area_xForm);
						break;
					case 5:
						s = getString(R.string.check_your_intenet_connection);
						break;
					default:
						s = "Exception";
					}
					dismissDialog(DIALOG_AREADETAILS_PROGRESS);
					Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				dismissDialog(DIALOG_AREADETAILS_PROGRESS);
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), getString(R.string.error_processing_fetch_area_xForm),
						Toast.LENGTH_SHORT).show();
			}
		}

	}

	private void enableEditText() throws JSONException {
		if ((tempLevel < areaLevelModels.length())) {
			int j = levels[tempLevel];
			if ((tempLevel + 1) == 1) {
				// level1.setEnabled(true);
				leble1.setText("Select a " + areaLevelName[tempLevel]);
				level1.setVisibility(View.VISIBLE);
				leble1.setVisibility(View.VISIBLE);
				tempAreaLevel = j;
				mapAreaLevel.put(level1, tempLevel);
			} else if ((tempLevel + 1) == 2) {
				// level2.setEnabled(true);
				leble2.setText("Select a " + areaLevelName[tempLevel]);
				level2.setVisibility(View.VISIBLE);
				leble2.setVisibility(View.VISIBLE);
				tempAreaLevel = j;
				mapAreaLevel.put(level2, tempLevel);
			} else if ((tempLevel + 1) == 3) {
				// level3.setEnabled(true);
				leble3.setText("Select a " + areaLevelName[tempLevel]);
				level3.setVisibility(View.VISIBLE);
				leble3.setVisibility(View.VISIBLE);
				tempAreaLevel = j;
				mapAreaLevel.put(level3, tempLevel);
			} else if ((tempLevel + 1) == 4) {
				// level4.setEnabled(true);
				leble4.setText("Select a " + areaLevelName[tempLevel]);
				level4.setVisibility(View.VISIBLE);
				leble4.setVisibility(View.VISIBLE);
				tempAreaLevel = j;
				mapAreaLevel.put(level4, tempLevel);
			} else if ((tempLevel + 1) == 5) {
				// level5.setEnabled(true);
				leble5.setText("Select a " + areaLevelName[tempLevel]);
				level5.setVisibility(View.VISIBLE);
				leble5.setVisibility(View.VISIBLE);
				tempAreaLevel = j;
				mapAreaLevel.put(level5, tempLevel);
			}

		} else if (tempLevel == areaLevelModels.length()) {
			if (secondaryAreas.length() > 0) {
				// secondary_area.setEnabled(true);
				secondary_area_leble.setText(getString(R.string.get_secondary_area));
				secondary_area.setVisibility(View.VISIBLE);
				secondary_area_leble.setVisibility(View.VISIBLE);
			}
			download_button.setEnabled(true);
		}

	}

	public void chooseClass(final EditText tempEditText) throws JSONException {
		CharSequence t = tempEditText.getText();
		t = t.toString();
		if (t.length() == 0) {
			Log.i("EditTextClicked", "Empty Edit Text Box Clicked");
		} else {
			for (int i = 0; i < models.length(); i++) {
				if (t.equals(models.getJSONObject(i).getString("areaName"))) {
					System.out.println(models.getJSONObject(i).getString("areaName"));
					tempParentId = models.getJSONObject(i).getInt("parentAreaId");
					tempAreaLevel = models.getJSONObject(i).getInt("areaLevelId");
					clearOther(tempEditText);
					tempLevel = mapAreaLevel.get(tempEditText);
				}
			}
		}
		// System.out.println(t);
		final HashMap<String, Integer> tempAreaId = new HashMap<String, Integer>();
		// final ArrayList<String> areaList =new ArrayList<String>();
		final ArrayList<String> areaId = new ArrayList<String>();
		final ArrayList<String> areaName = new ArrayList<String>();
		// spinnerToEnable.setEnabled(true);
		for (int i = 0; i < models.length(); i++) {
			if (models.getJSONObject(i).getInt("parentAreaId") == tempParentId) {
				tempAreaId.put(models.getJSONObject(i).getString("areaName"), models.getJSONObject(i).getInt("areaId"));
				// areaList.add(models.getJSONObject(i).getString("areaName"));
				final JSONObject jsonObj = models.getJSONObject(i);
				areaId.add(jsonObj.getString("areaId"));
				areaName.add(jsonObj.getString("areaName"));
			}
		}
		if (areaName.size() > 0) {
			final CharSequence[] items = areaName.toArray(new CharSequence[areaName.size()]);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Choose Class");
			builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
				}
			}).setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
					String choosedArea = areaName.get(selectedPosition);
					tempParentId = tempAreaId.get(choosedArea);
					tempEditText.setText(choosedArea);
					selectedLastAreaId = tempAreaId.get(choosedArea);
					tempLevel++;
					try {
						enableEditText();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					dialog.dismiss();
					/*
					 * if(tempLevel<levels.length){ tempLevel =
					 * levels[tempLevel+1]; }
					 */
				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			});

			AlertDialog alert = builder.create();
			alert.show();
		} else {
			Toast.makeText(getApplicationContext(), "No Area Present to choose", Toast.LENGTH_SHORT).show();
			tempLevel++;
			try {
				enableEditText();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.level1:
			try {
				chooseClass(level1);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tempEditText = level1;
			break;
		case R.id.level2:
			try {
				chooseClass(level2);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tempEditText = level2;
			break;
		case R.id.level3:
			try {
				chooseClass(level3);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tempEditText = level3;
			break;
		case R.id.level4:
			try {
				chooseClass(level4);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tempEditText = level4;
			break;
		case R.id.level5:
			try {
				chooseClass(level5);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tempEditText = level5;
			break;
		case R.id.secondary_area_level:
			// Toast.makeText(getApplicationContext(), "Hello",
			// Toast.LENGTH_SHORT).show();
			try {
				fetchSecondaryArea(secondary_area);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tempEditText = level5;
			break;
		}

	}

	private void clearOther(EditText temp) {
		if (temp.equals(level1)) {
			level2.setText(null);
			leble2.setText(null);
			// level2.setEnabled(false);
			level2.setVisibility(View.INVISIBLE);
			leble2.setVisibility(View.INVISIBLE);
			clearOther(level2);
		} else if (temp.equals(level2)) {
			level3.setText(null);
			leble3.setText(null);
			// level3.setEnabled(false);
			level3.setVisibility(View.INVISIBLE);
			level3.setVisibility(View.INVISIBLE);
			clearOther(level3);
		} else if (temp.equals(level3)) {
			level4.setText(null);
			leble4.setText(null);
			// level4.setEnabled(false);
			level4.setVisibility(View.INVISIBLE);
			level4.setVisibility(View.INVISIBLE);
			clearOther(level4);
		} else if (temp.equals(level4)) {
			level5.setText(null);
			leble5.setText(null);
			// level5.setEnabled(false);
			level5.setVisibility(View.INVISIBLE);
			level5.setVisibility(View.INVISIBLE);
			clearOther(secondary_area);
		} else if (temp.equals(secondary_area)) {
			secondary_area.setText(null);
			secondary_area_leble.setText(null);
			// level5.setEnabled(false);
			secondary_area.setVisibility(View.INVISIBLE);
			secondary_area_leble.setVisibility(View.INVISIBLE);
			download_button.setEnabled(false);
			return;
		}

	}

	private void fetchSecondaryArea(final EditText secondary_area) throws JSONException {

		final ArrayList<String> areaName = new ArrayList<String>();
		// spinnerToEnable.setEnabled(true);
		JSONArray names = secondaryAreas.getJSONArray(tempParentId + "");

		for (int i = 0; i < names.length(); i++) {
			areaName.add(names.getString(i));
		}

		final CharSequence[] items = areaName.toArray(new CharSequence[areaName.size()]);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose Secondary Area");
		builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
			}
		}).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
				String choosedArea = areaName.get(selectedPosition);
				selectedSecondaryArea = choosedArea;
				secondary_area.setText(choosedArea);
				try {
					enableEditText();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				dialog.dismiss();
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void clearAllFields() {

		level1.setText(null);
		leble1.setText(null);
		level2.setText(null);
		leble2.setText(null);
		level3.setText(null);
		leble3.setText(null);
		level4.setText(null);
		leble4.setText(null);
		level5.setText(null);
		leble5.setText(null);
		secondary_area.setText(null);
		secondary_area_leble.setText(null);

		leble1.setVisibility(View.INVISIBLE);
		leble2.setVisibility(View.INVISIBLE);
		leble3.setVisibility(View.INVISIBLE);
		leble4.setVisibility(View.INVISIBLE);
		leble5.setVisibility(View.INVISIBLE);
		secondary_area_leble.setVisibility(View.INVISIBLE);

		level1.setVisibility(View.INVISIBLE);
		level2.setVisibility(View.INVISIBLE);
		level3.setVisibility(View.INVISIBLE);
		level4.setVisibility(View.INVISIBLE);
		level5.setVisibility(View.INVISIBLE);
		secondary_area.setVisibility(View.INVISIBLE);

		tempLevel = 0;
		tempAreaLevel = 0;
		tempParentId = -1;
		selectedSecondaryArea = "";
	}

}
