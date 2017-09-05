/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.sdrc.collect.uphpmis.android.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sdrc.collect.uphpmis.android.R;
import org.sdrc.collect.uphpmis.android.application.Collect;
import org.sdrc.collect.uphpmis.android.listeners.UpdateListener;
import org.sdrc.collect.uphpmis.android.preferences.AdminPreferencesActivity;
import org.sdrc.collect.uphpmis.android.preferences.PreferencesActivity;
import org.sdrc.collect.uphpmis.android.provider.InstanceProviderAPI;
import org.sdrc.collect.uphpmis.android.provider.FormsProviderAPI.FormsColumns;
import org.sdrc.collect.uphpmis.android.provider.InstanceProviderAPI.InstanceColumns;
import org.sdrc.collect.uphpmis.android.tasks.DeleteFormsTask;
import org.sdrc.collect.uphpmis.android.tasks.DiskSyncTask;
import org.sdrc.collect.uphpmis.android.tasks.UpdateTask;
import org.sdrc.collect.uphpmis.android.utilities.CompatibilityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 * 
 * <h1>Get blank form</h1> The Get blank form section will lead to a Programs
 * activity where all the program will be displayed. That will lead to form list
 * page where we will be able to download the blank forms.
 * 
 * <h1>Send finalize forms</h1>
 * 
 * 
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Ratikanta Pradhan(ratikanta@sdrc.co.in)
 */
public class MainMenuActivity extends Activity implements UpdateListener {

	private static final String t = "MainMenuActivity";

	private static final int PASSWORD_DIALOG = 1;
	private static final int WARNING_DIALOG = 2;
	private final static int PROGRESS_DIALOG = 3;

	// menu options
	private static final int MENU_PREFERENCES = Menu.FIRST;
	private static final int MENU_ADMIN = Menu.FIRST + 1;
	private static final int MENU_MAP = Menu.FIRST + 2;
	private static final int MENU_UPDATE = Menu.FIRST + 3;
	private static final int MENU_LOGOUT = Menu.FIRST + 4;

	// buttons
	private Button mEnterDataButton;
	private Button mManageFilesButton;
	private Button mSendDataButton;
	private Button mReviewDataButton;
	private Button mGetFormsButton;

	private Button mViewSendFormsButton; // @Author: Rohit
	// A view send button is declared to get the button.

	private View mReviewSpacer;
	private View mGetFormsSpacer;

	private AlertDialog mAlertDialog;
	private SharedPreferences mAdminPreferences;

	private int mCompletedCount;
	private int mSavedCount;

	private Cursor mFinalizedCursor;
	private Cursor mSavedCursor;

	private IncomingHandler mHandler = new IncomingHandler(this);
	private MyContentObserver mContentObserver = new MyContentObserver();

	private String db_username;
	private String db_password;

	private static boolean EXIT = true;

	/**
	 * Will help to access the database
	 */
	SQLiteDatabase db = null;

	/**
	 * This variable will help to know is the android back button tapped once
	 */
	private boolean doubleBackToExitPressedOnce = false;

	static class BackgroundTasks {
		DiskSyncTask mDiskSyncTask = null;
		DeleteFormsTask mDeleteFormsTask = null;

		BackgroundTasks() {
		};
	}

	BackgroundTasks mBackgroundTasks; // handed across orientation changes

	private ArrayList<Long> mSelected = new ArrayList<Long>();
	private UpdateTask updateTask;
	private ProgressDialog mProgressDialog;

	// private static boolean DO_NOT_EXIT = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// must be at the beginning of any activity that can be called from an
		// external intent
		Log.i(t, "Starting up, creating directories");

		try {
			Collect.createODKDirs();
		} catch (RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}
		db = openOrCreateDatabase("SDRCCollectDB", MODE_PRIVATE, null);
		Cursor resultSet = db.rawQuery("Select * from " + getString(R.string.user_table_name), null);
		int rows = resultSet.getCount();
		if (rows > 0) {
			resultSet.moveToFirst();
			db_username = resultSet.getString(0);
			db_password = resultSet.getString(1);
		}
		setContentView(R.layout.main_menu);

		{
			// dynamically construct the "ODK Collect vA.B" string
			TextView mainMenuMessageLabel = (TextView) findViewById(R.id.main_menu_header);
			mainMenuMessageLabel.setText(Collect.getInstance().getVersionedAppName());
		}

		setTitle(getString(R.string.app_name) + " > " + getString(R.string.main_menu));

		File f = new File(Collect.ODK_ROOT + "/collect.settings");
		if (f.exists()) {
			boolean success = loadSharedPreferencesFromFile(f);
			if (success) {
				Toast.makeText(this, "Settings successfully loaded from file", Toast.LENGTH_LONG).show();
				f.delete();
			} else {
				Toast.makeText(this, "Sorry, settings file is corrupt and should be deleted or replaced",
						Toast.LENGTH_LONG).show();
			}
		}

		mReviewSpacer = findViewById(R.id.review_spacer);
		mGetFormsSpacer = findViewById(R.id.get_forms_spacer);

		mAdminPreferences = this.getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		// enter data button. expects a result.
		mEnterDataButton = (Button) findViewById(R.id.enter_data);
		mEnterDataButton.setText(getString(R.string.enter_data_button));
		mEnterDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Ratikanta start
				Collect.getInstance().setControlFromFillBlankForm(true);
				Collect.getInstance().setControlFromEditForm(false);
				//Ratikanta end
				Collect.getInstance().getActivityLogger().logAction(this, "fillBlankForm", "click");
				Intent i = new Intent(getApplicationContext(), FormChooserList.class);
				startActivity(i);
			}
		});

		// review data button. expects a result.
		mReviewDataButton = (Button) findViewById(R.id.review_data);
		mReviewDataButton.setText(getString(R.string.review_data_button));
		mReviewDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Ratikanta start
				Collect.getInstance().setControlFromFillBlankForm(false);
				Collect.getInstance().setControlFromEditForm(true);
				//Ratikanta end
				Collect.getInstance().getActivityLogger().logAction(this, "editSavedForm", "click");
				Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
				//Avinash
				i.putExtra("Action", "EditSaved");
				startActivity(i);
			}
		});

		/* @Author: Rohit */
		// view data button. expects a result.
		mViewSendFormsButton = (Button) findViewById(R.id.view_data);
		mViewSendFormsButton.setText(getString(R.string.view_data_button));
		mViewSendFormsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Ratikanta start
				Collect.getInstance().setControlFromFillBlankForm(false);
				Collect.getInstance().setControlFromEditForm(false);
				//Ratikanta end
				Collect.getInstance().getActivityLogger().logAction(this, "viewSentForm", "click");
				Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
				i.putExtra("Action", "ViewSent");
				startActivity(i);
			}
		});

		// send data button. expects a result.
		mSendDataButton = (Button) findViewById(R.id.send_data);
		mSendDataButton.setText(getString(R.string.send_data_button));
		mSendDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger().logAction(this, "uploadForms", "click");
				Intent i = new Intent(getApplicationContext(), InstanceUploaderList.class);
				startActivity(i);
			}
		});

		// manage forms button. no result expected.
		mGetFormsButton = (Button) findViewById(R.id.get_forms);
		mGetFormsButton.setText(getString(R.string.get_forms));
		mGetFormsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger().logAction(this, "downloadBlankForms", "click");
				SharedPreferences sharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(MainMenuActivity.this);
				String protocol = sharedPreferences.getString(PreferencesActivity.KEY_PROTOCOL,
						getString(R.string.protocol_odk_default));
				Intent i = null;
				if (protocol.equalsIgnoreCase(getString(R.string.protocol_google_maps_engine))) {
					i = new Intent(getApplicationContext(), GoogleDriveActivity.class);
				} else {
					// Ratikanta
					// i = new Intent(getApplicationContext(),
					// FormDownloadList.class);

					i = new Intent(getApplicationContext(), ProgramsActivity.class);
				}
				startActivity(i);

			}
		});

		// manage forms button. no result expected.
		mManageFilesButton = (Button) findViewById(R.id.manage_forms);
		mManageFilesButton.setText(getString(R.string.manage_files));
		mManageFilesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger().logAction(this, "deleteSavedForms", "click");
				Intent i = new Intent(getApplicationContext(), FileManagerTabs.class);
				startActivity(i);
			}
		});

		updateUI();
	}

	private void deleteSavedForms() {
		// TODO Auto-generated method stub

		Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null, null, null, null);

		for (int i = 1; i <= c.getCount(); i++) {
			Long l = (long) i;
			try {
				Uri deleteForm = Uri.withAppendedPath(InstanceColumns.CONTENT_URI, l.toString());

				int wasDeleted = Collect.getInstance().getContentResolver().delete(deleteForm, null, null);

				if (wasDeleted == 0)
					break;
			} catch (Exception ex) {
				Log.e(t, "Exception during delete of: " + l.toString() + " exception: " + ex.toString());
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences sharedPreferences = this.getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		boolean edit = sharedPreferences.getBoolean(AdminPreferencesActivity.KEY_EDIT_SAVED, true);
		if (!edit) {
			mReviewDataButton.setVisibility(View.GONE);
			mReviewSpacer.setVisibility(View.GONE);
		} else {
			mReviewDataButton.setVisibility(View.VISIBLE);
			mReviewSpacer.setVisibility(View.VISIBLE);
		}

		boolean send = sharedPreferences.getBoolean(AdminPreferencesActivity.KEY_SEND_FINALIZED, true);
		if (!send) {
			mSendDataButton.setVisibility(View.GONE);
		} else {
			mSendDataButton.setVisibility(View.VISIBLE);
		}

		boolean get_blank = sharedPreferences.getBoolean(AdminPreferencesActivity.KEY_GET_BLANK, true);
		if (!get_blank) {
			mGetFormsButton.setVisibility(View.GONE);
			mGetFormsSpacer.setVisibility(View.GONE);
		} else {
			mGetFormsButton.setVisibility(View.VISIBLE);
			mGetFormsSpacer.setVisibility(View.VISIBLE);
		}

		boolean delete_saved = sharedPreferences.getBoolean(AdminPreferencesActivity.KEY_DELETE_SAVED, true);
		if (!delete_saved) {
			mManageFilesButton.setVisibility(View.GONE);
		} else {
			mManageFilesButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Collect.getInstance().getActivityLogger().logOnStart(this);
	}

	@Override
	protected void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(this);
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Collect.getInstance().getActivityLogger().logAction(this, "onCreateOptionsMenu", "show");
		super.onCreateOptionsMenu(menu);

		CompatibilityUtils.setShowAsAction(
				menu.add(0, MENU_PREFERENCES, 0, R.string.general_preferences).setIcon(R.drawable.ic_menu_preferences),
				MenuItem.SHOW_AS_ACTION_NEVER);
		CompatibilityUtils.setShowAsAction(
				menu.add(0, MENU_UPDATE, 0, R.string.update_preferences).setIcon(R.drawable.ic_menu_login),
				MenuItem.SHOW_AS_ACTION_NEVER);
		CompatibilityUtils.setShowAsAction(
				menu.add(0, MENU_LOGOUT, 0, R.string.logout_preferences).setIcon(R.drawable.ic_menu_login),
				MenuItem.SHOW_AS_ACTION_NEVER);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			Collect.getInstance().getActivityLogger().logAction(this, "onOptionsItemSelected", "MENU_PREFERENCES");
			Intent ig = new Intent(this, PreferencesActivity.class);
			startActivity(ig);
			return true;
		case MENU_MAP:
			Collect.getInstance().getActivityLogger().logAction(this, "onOptionsItemSelected", "MENU_PREFERENCES");
			Intent fc = new Intent(this, Facility_activity.class);
			startActivity(fc);
			return true;
		case MENU_ADMIN:
			Collect.getInstance().getActivityLogger().logAction(this, "onOptionsItemSelected", "MENU_ADMIN");
			String pw = mAdminPreferences.getString(AdminPreferencesActivity.KEY_ADMIN_PW, "");
			if ("".equalsIgnoreCase(pw)) {
				Intent i = new Intent(getApplicationContext(), AdminPreferencesActivity.class);
				startActivity(i);
			} else {
				showDialog(PASSWORD_DIALOG);
				Collect.getInstance().getActivityLogger().logAction(this, "createAdminPasswordDialog", "show");
			}
			return true;
		case MENU_UPDATE:

			// update
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferencesActivity.ODK_SERVER_URLS, "");
			editor.commit();
			eraseLoginData();

			// hit the server again and update the database

			ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			if (activeNetwork != null && activeNetwork.isConnected()) {

				settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				String webServerURL = settings.getString(PreferencesActivity.KEY_SUBMISSION_SERVER_URL, "");

				if (!(webServerURL.charAt(webServerURL.length() - 1) == '/'))
					webServerURL += "/";

				showDialog(PROGRESS_DIALOG);

				if (updateTask != null && updateTask.getStatus() != AsyncTask.Status.FINISHED) {
					// return; // Already login in progress
				} else if (updateTask != null) {
					updateTask.setUpdateListener(null);
					updateTask.cancel(true);
					updateTask = null;
				}

				updateTask = new UpdateTask();
				updateTask.setUpdateListener(this);
				updateTask.execute(webServerURL, Base64.encodeToString((db_username + ":" + db_password).getBytes(), Base64.DEFAULT), db_password);

			} else {
				Toast.makeText(getApplicationContext(), getString(R.string.check_your_intenet_connection),
						Toast.LENGTH_SHORT).show();
			}

			return true;
		case MENU_LOGOUT:
			// showDialog(WARNING_DIALOG);
			db.execSQL("UPDATE " + getString(R.string.user_table_name) + " set IsLoggedIn = 0;");
			goToLoginPage();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void deleteBlankForms() {
		// TODO Auto-generated method stub
		Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, null, null, null);

		for (int i = 1; i <= c.getCount(); i++) {
			Long l = (long) i;

			try {
				Uri deleteForm = Uri.withAppendedPath(FormsColumns.CONTENT_URI, l.toString());

				Collect.getInstance().getContentResolver().delete(deleteForm, null, null);

			} catch (Exception ex) {
				Log.e(t, "Exception during delete of: " + l.toString() + " exception: " + ex.toString());
			}
		}
	}

	private void eraseLoginData() {
		// TODO Auto-generated method stub
		Collect.getInstance().getActivityLogger().logAction(this, "onOptionsItemSelected", "MENU_LOGOUT");
		db.execSQL("UPDATE " + getString(R.string.user_table_name) + " set IsLoggedIn = 0;");

		// delete all the program, xform and their mapping data, they will get
		// inserted again in next login
		// we have do it because in next login another user may come, which
		// might have assigned to different programs and xforms.
		db.execSQL("delete from " + getString(R.string.program_table_name) + ";");
		db.execSQL("delete from " + getString(R.string.xform_table_name) + ";");
		db.execSQL("delete from " + getString(R.string.program_xform_mapping_table_name) + ";");
	}

	private void createErrorDialog(String errorMsg, final boolean shouldExit) {
		Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(errorMsg);
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON_POSITIVE:
					Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog",
							shouldExit ? "exitApplication" : "OK");
					if (shouldExit) {
						finish();
					}
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), errorListener);
		mAlertDialog.show();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.PROGRESS_DIALOG", "show");
			mProgressDialog = new ProgressDialog(this);
			DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.PROGRESS_DIALOG", "OK");
					dialog.dismiss();
					if (updateTask != null) {
						updateTask.setUpdateListener(null);
						updateTask.cancel(true);
						updateTask = null;
					}
				}
			};
			mProgressDialog.setTitle(getString(R.string.downloading_data));
			mProgressDialog.setMessage(getString(R.string.update_dialog_message));
			mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
			return mProgressDialog;
		case PASSWORD_DIALOG:

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final AlertDialog passwordDialog = builder.create();

			passwordDialog.setTitle(getString(R.string.enter_admin_password));
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			input.setTransformationMethod(PasswordTransformationMethod.getInstance());
			passwordDialog.setView(input, 20, 10, 20, 10);

			passwordDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							String value = input.getText().toString();
							String pw = mAdminPreferences.getString(AdminPreferencesActivity.KEY_ADMIN_PW, "");
							if (pw.compareTo(value) == 0) {
								Intent i = new Intent(getApplicationContext(), AdminPreferencesActivity.class);
								startActivity(i);
								input.setText("");
								passwordDialog.dismiss();
							} else {
								Toast.makeText(MainMenuActivity.this, getString(R.string.admin_password_incorrect),
										Toast.LENGTH_SHORT).show();
								Collect.getInstance().getActivityLogger().logAction(this, "adminPasswordDialog",
										"PASSWORD_INCORRECT");
							}
						}
					});

			passwordDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							Collect.getInstance().getActivityLogger().logAction(this, "adminPasswordDialog", "cancel");
							input.setText("");
							return;
						}
					});

			passwordDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			return passwordDialog;
		

		}
		return null;
	}

	private void updateButtons() {
		if (mFinalizedCursor != null && !mFinalizedCursor.isClosed()) {
			mFinalizedCursor.requery();
			mCompletedCount = mFinalizedCursor.getCount();
			if (mCompletedCount > 0) {
				mSendDataButton.setText(getString(R.string.send_data_button, mCompletedCount));
			} else {
				mSendDataButton.setText(getString(R.string.send_data));
			}
		} else {
			mSendDataButton.setText(getString(R.string.send_data));
			Log.w(t, "Cannot update \"Send Finalized\" button label since the database is closed. Perhaps the app is running in the background?");
		}

		if (mSavedCursor != null && !mSavedCursor.isClosed()) {
			mSavedCursor.requery();
			mSavedCount = mSavedCursor.getCount();
			if (mSavedCount > 0) {
				mReviewDataButton.setText(getString(R.string.review_data_button, mSavedCount));
			} else {
				mReviewDataButton.setText(getString(R.string.review_data));
			}
		} else {
			mReviewDataButton.setText(getString(R.string.review_data));
			Log.w(t, "Cannot update \"Edit Form\" button label since the database is closed. Perhaps the app is running in the background?");
		}
	}

	/**
	 * notifies us that something changed
	 *
	 */
	private class MyContentObserver extends ContentObserver {

		public MyContentObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.sendEmptyMessage(0);
		}
	}

	/*
	 * Used to prevent memory leaks
	 */
	static class IncomingHandler extends Handler {
		private final WeakReference<MainMenuActivity> mTarget;

		IncomingHandler(MainMenuActivity target) {
			mTarget = new WeakReference<MainMenuActivity>(target);
		}

		@Override
		public void handleMessage(Message msg) {
			MainMenuActivity target = mTarget.get();
			if (target != null) {
				target.updateButtons();
			}
		}
	}

	private boolean loadSharedPreferencesFromFile(File src) {
		// this should probably be in a thread if it ever gets big
		boolean res = false;
		ObjectInputStream input = null;
		try {
			input = new ObjectInputStream(new FileInputStream(src));
			Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
			prefEdit.clear();
			// first object is preferences
			Map<String, ?> entries = (Map<String, ?>) input.readObject();
			for (Entry<String, ?> entry : entries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();

				if (v instanceof Boolean)
					prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					prefEdit.putFloat(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					prefEdit.putInt(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					prefEdit.putLong(key, ((Long) v).longValue());
				else if (v instanceof String)
					prefEdit.putString(key, ((String) v));
			}
			prefEdit.commit();

			// second object is admin options
			Editor adminEdit = getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0).edit();
			adminEdit.clear();
			// first object is preferences
			Map<String, ?> adminEntries = (Map<String, ?>) input.readObject();
			for (Entry<String, ?> entry : adminEntries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();

				if (v instanceof Boolean)
					adminEdit.putBoolean(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					adminEdit.putFloat(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					adminEdit.putInt(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					adminEdit.putLong(key, ((Long) v).longValue());
				else if (v instanceof String)
					adminEdit.putString(key, ((String) v));
			}
			adminEdit.commit();

			res = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return res;
	}

	private void goToLoginPage() {

		// moving control to the login page
		Intent loginIntent = new Intent(this, LoginActivity.class);
		loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(loginIntent);

	}


	/**
	 * This method will help in login process
	 * 
	 * @param params
	 * @return
	 * @throws UnsupportedEncodingException
	 * @since v1.0.0.0
	 */
	private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
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

	/**
	 * This method will be responsible for inserting data into program, xform
	 * and their mapping table.
	 * 
	 * @param jsonArray
	 * @throws JSONException
	 * @since v1.0.0.0
	 */

	private void insertDataIntoTables(JSONArray jsonArray) throws Exception {
		// TODO Auto-generated method stub
		Map<String, JSONObject> xForms = new HashMap<String, JSONObject>();
		int count = 0;
		// iterate over programs
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject programWithXFormObj = jsonArray.getJSONObject(i);
			JSONObject pogramObj = programWithXFormObj.getJSONObject("programModel");

			db.execSQL("INSERT INTO " + getString(R.string.program_table_name) + " VALUES("
					+ pogramObj.getInt("programId") + ",'" + pogramObj.getString("programName") + "', 0);");

			JSONArray xFormArray = programWithXFormObj.getJSONArray("xFormsModel");

			// iterate over xForms
			for (int j = 0; j < xFormArray.length(); j++) {
				JSONObject xFormObject = xFormArray.getJSONObject(j);
				xForms.put(xFormObject.getString("xFormId"), xFormObject);
				db.execSQL("INSERT INTO " + getString(R.string.program_xform_mapping_table_name) + " VALUES(" + ++count
						+ ", " + pogramObj.getInt("programId") + ",'" + xFormObject.getString("xFormId") + "');");
			}

		}

		// boolean countForm = true;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String odkServerURLs = settings.getString(PreferencesActivity.ODK_SERVER_URLS, "");

		// iterate over all xForms
		for (JSONObject obj : xForms.values()) {

			if (odkServerURLs.equals("")) {
				odkServerURLs += obj.getString("odkServerURL") + PreferencesActivity.KEY_SEPARATOR
						+ obj.getString("username") + PreferencesActivity.KEY_SEPARATOR + obj.getString("password");
			} else {
				if (!odkServerURLs.contains(obj.getString("odkServerURL"))) {
					odkServerURLs += PreferencesActivity.URL_SEPARATOR;
					odkServerURLs += obj.getString("odkServerURL") + PreferencesActivity.KEY_SEPARATOR
							+ obj.getString("username") + PreferencesActivity.KEY_SEPARATOR + obj.getString("password");
				}
			}

			db.execSQL("INSERT INTO " + getString(R.string.xform_table_name) + " VALUES('" + obj.getString("xFormId")
					+ "','" + obj.getString("odkServerURL") + "','" + obj.getString("username") + "','"
					+ obj.getString("password") + "');");
		}
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferencesActivity.USERNAME, db_username);
		editor.putString(PreferencesActivity.PASSWORD, db_password);
		editor.putString(PreferencesActivity.ODK_SERVER_URLS, odkServerURLs);
		editor.commit();

	}

	@Override
	public void updateOperationComplete(HashMap<Integer, String> result) {
		// TODO Auto-generated method stub
		dismissDialog(PROGRESS_DIALOG);

		try {
			String s = result.get(1);
			if (s != null) {

				// then try to insert
				insertDataIntoTables(new JSONArray(s));
				if (db_username.equals("")) {
					// insert
					db.execSQL("INSERT INTO " + getString(R.string.user_table_name) + " VALUES('" + db_username + "','"
							+ db_password + "', 1);");
				} else {
					// update
					db.execSQL("UPDATE " + getString(R.string.user_table_name) + " set username = '" + db_username
							+ "', password = '" + db_password + "', IsLoggedIn = 1;");
				}
				setFormListWhereClause();
				updateUI();
				Toast.makeText(getApplicationContext(), getString(R.string.update_successful), Toast.LENGTH_SHORT)
						.show();

			} else {
				s = result.get(0);
				int resultNumber = Integer.parseInt(s);
				switch (resultNumber) {
				case 0:
					s = getString(R.string.invalid_username_password);
					goToLoginPage();
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
					s = getString(R.string.exception_while_update);
					break;
				default:
					s = "Exception";
				}
				Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), getString(R.string.error_processing_after_login_data),
					Toast.LENGTH_SHORT).show();
		}

	}

	private void updateUI() {
		// TODO Auto-generated method stub
		// count for finalized instances
		String selection = "(" + InstanceColumns.STATUS + "=? or " + InstanceColumns.STATUS + "=?)";

		// Ratikanta
		selection += Collect.getInstance().getFormIdWhereClauseString() != null
				? " AND " + Collect.getInstance().getFormIdWhereClauseString() : " AND 0 = 1";

		String selectionArgs[] = { InstanceProviderAPI.STATUS_COMPLETE, InstanceProviderAPI.STATUS_SUBMISSION_FAILED };

		try {
			mFinalizedCursor = managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, null);
		} catch (Exception e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}

		if (mFinalizedCursor != null) {
			startManagingCursor(mFinalizedCursor);
		}
		mCompletedCount = mFinalizedCursor != null ? mFinalizedCursor.getCount() : 0;
		getContentResolver().registerContentObserver(InstanceColumns.CONTENT_URI, true, mContentObserver);
		// mFinalizedCursor.registerContentObserver(mContentObserver);

		// count for finalized instances
		String selectionSaved = InstanceColumns.STATUS + "=?";

		// Ratikanta
		selectionSaved += Collect.getInstance().getFormIdWhereClauseString() != null
				? " AND " + Collect.getInstance().getFormIdWhereClauseString() : " AND 0 = 1";

		String selectionArgsSaved[] = { InstanceProviderAPI.STATUS_INCOMPLETE };

		try {
			mSavedCursor = managedQuery(InstanceColumns.CONTENT_URI, null, selectionSaved, selectionArgsSaved, null);
		} catch (Exception e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}

		if (mSavedCursor != null) {
			startManagingCursor(mSavedCursor);
		}
		mSavedCount = mSavedCursor != null ? mSavedCursor.getCount() : 0;
		// don't need to set a content observer because it can't change in the
		// background

		updateButtons();
	}

	public void deleteRecursive(File fileOrDirectory) {

		if (fileOrDirectory.isDirectory()) {
			for (File child : fileOrDirectory.listFiles()) {
				deleteRecursive(child);
			}
		}

		fileOrDirectory.delete();
	}

	private void setFormListWhereClause() {
		// TODO Auto-generated method stub

		Cursor resultSet = db.rawQuery("Select * from " + getString(R.string.xform_table_name), null);
		String whereClauseString = "jrFormId IN (";
		while (resultSet.moveToNext()) {
			whereClauseString += "'" + resultSet.getString(resultSet.getColumnIndex("id")) + "', ";
		}

		if (resultSet.getCount() > 0) {
			whereClauseString = whereClauseString.substring(0, whereClauseString.length() - 2);
			whereClauseString += ")";
			Collect.getInstance().setFormIdWhereClauseString(whereClauseString);
		} else {
			Collect.getInstance().setFormIdWhereClauseString(null);
		}

	}
}
