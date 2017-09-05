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

import java.util.ArrayList;
import java.util.List;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.sdrc.collect.uphpmis.android.R;
import org.sdrc.collect.uphpmis.android.adapters.HierarchyListAdapter;
import org.sdrc.collect.uphpmis.android.application.Collect;
import org.sdrc.collect.uphpmis.android.exception.JavaRosaException;
import org.sdrc.collect.uphpmis.android.logic.FormController;
import org.sdrc.collect.uphpmis.android.logic.HierarchyElement;
import org.sdrc.collect.uphpmis.android.preferences.PreferencesActivity;
import org.sdrc.collect.uphpmis.android.provider.FormsProviderAPI.FormsColumns;
import org.sdrc.collect.uphpmis.android.receivers.NetworkReceiver;
import org.sdrc.collect.uphpmis.android.utilities.InternetTestUtils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FormHierarchyActivity extends ListActivity {

	private static final String t = "FormHierarchyActivity";

	private static final int CHILD = 1;
	private static final int EXPANDED = 2;
	private static final int COLLAPSED = 3;
	private static final int QUESTION = 4;

	private static final String mIndent = "     ";

	private Button jumpPreviousButton;

	List<HierarchyElement> formList;
	TextView mPath;

	FormIndex mStartIndex;
	private FormIndex currentIndex;

	/**
	 * \
	 * 
	 * @author Amit Kumar Sahoo
	 * 
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST;
	private static final int GOOGLE_USER_DIALOG = 1;
	private static final int INSTANCE_UPLOADER = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hierarchy_layout);
		FormController formController = Collect.getInstance().getFormController();

		// We use a static FormEntryController to make jumping faster.
		mStartIndex = formController.getFormIndex();

		setTitle(getString(R.string.app_name) + " > " + formController.getFormTitle());

		mPath = (TextView) findViewById(R.id.pathtext);

		jumpPreviousButton = (Button) findViewById(R.id.jumpPreviousButton);
		jumpPreviousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger().logInstanceAction(this, "goUpLevelButton", "click");
				goUpLevel();
			}
		});
		// Avinash
		Button jumpBeginningButton = (Button) findViewById(R.id.jumpBeginningButton);
		Button jumpEndButton = (Button) findViewById(R.id.jumpEndButton);

		Intent parentintent = this.getIntent();
		if (!parentintent.getStringExtra("Action").equalsIgnoreCase("EditSaved")) {

			jumpBeginningButton.setText("Exit");
			jumpBeginningButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Collect.getInstance().getActivityLogger().logInstanceAction(this, "jumpToBeginning", "click");
					setResult(RESULT_OK);
					finish();
				}
			});

			jumpEndButton.setEnabled(false);
			jumpEndButton.setVisibility(View.GONE);

		}
		if(Collect.getInstance().getWantToSeePreviousData()){
			jumpBeginningButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					goToLastFormFillupActivity();					
				}
			});
			
		}
		else {

			jumpBeginningButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Collect.getInstance().getActivityLogger().logInstanceAction(this, "jumpToBeginning", "click");
					Collect.getInstance().getFormController().jumpToIndex(FormIndex.createBeginningOfFormIndex());
					setResult(RESULT_OK);
					finish();
				}
			});

			jumpEndButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Collect.getInstance().getActivityLogger().logInstanceAction(this, "jumpToEnd", "click");
					Collect.getInstance().getFormController().jumpToIndex(FormIndex.createEndOfFormIndex());
					setResult(RESULT_OK);
					finish();
				}
			});
			// refreshView();
		}
		refreshView();
		// kinda slow, but works.
		// this scrolls to the last question the user was looking at
		if (getListAdapter() != null && getListView() != null) {
			getListView().post(new Runnable() {
				@Override
				public void run() {
					int position = 0;
					for (int i = 0; i < getListAdapter().getCount(); i++) {
						HierarchyElement he = (HierarchyElement) getListAdapter().getItem(i);
						if (mStartIndex.equals(he.getFormIndex())) {
							position = i;
							break;
						}
					}
					getListView().setSelection(position);
				}
			});
		}
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		goToLastFormFillupActivity();		
		super.onBackPressed();
	}

	private void goToLastFormFillupActivity() {
		
		if(Collect.getInstance().isControlFromFillBlankForm()){
			Collect.getInstance().setBackPressed(true);
			
			// TODO Auto-generated method stub
			String sortOrder = FormsColumns.DISPLAY_NAME + " ASC, " + FormsColumns.JR_VERSION + " DESC";
	        Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, null, null, sortOrder);
			//long idFormsTable = ((SimpleCursorAdapter) getListAdapter()).getItemId(1);
	        long formIdFromList = Collect.getInstance().getFormIdFromList();
	        Uri formUri = ContentUris.withAppendedId(FormsColumns.CONTENT_URI, formIdFromList);
	
			Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", formUri.toString());
	
	        String action = getIntent().getAction();
	        if (Intent.ACTION_PICK.equals(action)) {
	            // caller is waiting on a picked form
	            setResult(RESULT_OK, new Intent().setData(formUri));
	        } else {
	            startActivity(new Intent(Intent.ACTION_EDIT, formUri));
	        }
	        
	        Collect.getInstance().setWantToSeePreviousData(false);
	        finish();
        }
		
		if(Collect.getInstance().isControlFromEditForm()){
			Intent intent = new Intent(FormHierarchyActivity.this,MainMenuActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
		}
		
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			Collect.getInstance().getActivityLogger().logAction(this, "onMenuItemSelected", "MENU_PREFERENCES");

			if (NetworkReceiver.running == true) {
				Toast.makeText(getApplicationContext(), "Background send running, please try again shortly",
						Toast.LENGTH_SHORT).show();
			} else {

				if (!InternetTestUtils.isConnected(getApplicationContext())) {

					Collect.getInstance().getActivityLogger().logAction(this, "uploadButton", "noConnection");

					sendSms();
				} else {
					uploadSelectedFiles();
					
				}
			}
		}
		return true;
	}

	private void sendSms() {
		try {
			SmsManager smsManager = SmsManager.getDefault();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void uploadSelectedFiles() {
		// send list of _IDs.

		long[] instanceIDs = new long[1];
		// long l=getIntent().getLongExtra("ID",0);
		instanceIDs[0] = Collect.getInstance().getFormNoChoosedInViewSentForm();
		/*
		 * for (int i = 0; i < mSelected.size(); i++) { instanceIDs[i] =
		 * mSelected.get(i); }
		 */
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String server = prefs.getString(PreferencesActivity.KEY_PROTOCOL, null);
		// Ratikanta
		if (server != null && server.equalsIgnoreCase(getString(R.string.protocol_google_maps_engine))) {
			// if it's maps engine, start the maps-engine uploader
			// first make sure we have a google account selected

			String googleUsername = prefs.getString(PreferencesActivity.KEY_SELECTED_GOOGLE_ACCOUNT, null);
			if (googleUsername == null || googleUsername.equals("")) {
				showDialog(GOOGLE_USER_DIALOG);
				return;
			}
			Intent i = new Intent(this, GoogleMapsEngineUploaderActivity.class);
			i.putExtra(FormEntryActivity.KEY_INSTANCES, instanceIDs);
			startActivityForResult(i, INSTANCE_UPLOADER);
		} else {
			// otherwise, do the normal agregate/other thing.
			Intent i = new Intent(this, InstanceUploaderActivity.class);
			i.putExtra(FormEntryActivity.KEY_INSTANCES, instanceIDs);
			startActivityForResult(i, INSTANCE_UPLOADER);
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

	private void goUpLevel() {
		Collect.getInstance().getFormController().stepToOuterScreenEvent();

		refreshView();
	}

	private String getCurrentPath() {
		FormController formController = Collect.getInstance().getFormController();
		FormIndex index = formController.getFormIndex();
		// move to enclosing group...
		index = formController.stepIndexOut(index);

		String path = "";
		while (index != null) {

			path = formController.getCaptionPrompt(index).getLongText() + " ("
					+ (formController.getCaptionPrompt(index).getMultiplicity() + 1) + ") > " + path;

			index = formController.stepIndexOut(index);
		}
		// return path?
		return path.substring(0, path.length() - 2);
	}

	public void refreshView() {
		try {
			FormController formController = Collect.getInstance().getFormController();
			// Record the current index so we can return to the same place if
			// the user hits 'back'.
			currentIndex = formController.getFormIndex();

			// If we're not at the first level, we're inside a repeated group so
			// we want to only display
			// everything enclosed within that group.
			String contextGroupRef = "";
			formList = new ArrayList<HierarchyElement>();

			// If we're currently at a repeat node, record the name of the node
			// and step to the next
			// node to display.
			if (formController.getEvent() == FormEntryController.EVENT_REPEAT) {
				contextGroupRef = formController.getFormIndex().getReference().toString(true);
				formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
			} else {
				FormIndex startTest = formController.stepIndexOut(currentIndex);
				// If we have a 'group' tag, we want to step back until we hit a
				// repeat or the
				// beginning.
				while (startTest != null && formController.getEvent(startTest) == FormEntryController.EVENT_GROUP) {
					startTest = formController.stepIndexOut(startTest);
				}
				if (startTest == null) {
					// check to see if the question is at the first level of the
					// hierarchy. If it is,
					// display the root level from the beginning.
					formController.jumpToIndex(FormIndex.createBeginningOfFormIndex());
				} else {
					// otherwise we're at a repeated group
					formController.jumpToIndex(startTest);
				}

				// now test again for repeat. This should be true at this point
				// or we're at the
				// beginning
				if (formController.getEvent() == FormEntryController.EVENT_REPEAT) {
					contextGroupRef = formController.getFormIndex().getReference().toString(true);
					formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
				}
			}

			int event = formController.getEvent();
			if (event == FormEntryController.EVENT_BEGINNING_OF_FORM) {
				// The beginning of form has no valid prompt to display.
				formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
				contextGroupRef = formController.getFormIndex().getReference().getParentRef().toString(true);
				mPath.setVisibility(View.GONE);
				jumpPreviousButton.setEnabled(false);
			} else {
				mPath.setVisibility(View.VISIBLE);
				mPath.setText(getCurrentPath());
				jumpPreviousButton.setEnabled(true);
			}

			// Refresh the current event in case we did step forward.
			event = formController.getEvent();

			// Big change from prior implementation:
			//
			// The ref strings now include the instance number designations
			// i.e., [0], [1], etc. of the repeat groups (and also [1] for
			// non-repeat elements).
			//
			// The contextGroupRef is now also valid for the top-level form.
			//
			// The repeatGroupRef is null if we are not skipping a repeat
			// section.
			//
			String repeatGroupRef = null;

			event_search: while (event != FormEntryController.EVENT_END_OF_FORM) {

				// get the ref to this element
				String currentRef = formController.getFormIndex().getReference().toString(true);

				// retrieve the current group
				String curGroup = (repeatGroupRef == null) ? contextGroupRef : repeatGroupRef;

				if (!currentRef.startsWith(curGroup)) {
					// We have left the current group
					if (repeatGroupRef == null) {
						// We are done.
						break event_search;
					} else {
						// exit the inner repeat group
						repeatGroupRef = null;
					}
				}

				if (repeatGroupRef != null) {
					// We're in a repeat group within the one we want to list
					// skip this question/group/repeat and move to the next
					// index.
					event = formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
					continue;
				}

				switch (event) {
				case FormEntryController.EVENT_QUESTION:

					FormEntryPrompt fp = formController.getQuestionPrompt();
					String label = fp.getLongText();
					if (!fp.isReadOnly() || (label != null && label.length() > 0)) {
						// show the question if it is an editable field.
						// or if it is read-only and the label is not blank.
						formList.add(new HierarchyElement(fp.getLongText(), fp.getAnswerText(), null, Color.WHITE,
								QUESTION, fp.getIndex()));
					}
					break;
				case FormEntryController.EVENT_GROUP:
					// ignore group events
					break;
				case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
					// this would display the 'add new repeat' dialog
					// ignore it.
					break;
				case FormEntryController.EVENT_REPEAT:
					FormEntryCaption fc = formController.getCaptionPrompt();
					// push this repeat onto the stack.
					repeatGroupRef = currentRef;
					// Because of the guard conditions above, we will skip
					// everything until we exit this repeat.
					//
					// Note that currentRef includes the multiplicity of the
					// repeat (e.g., [0], [1], ...), so every repeat will be
					// detected as different and reach this case statement.
					// Only the [0] emits the repeat header.
					// Every one displays the descend-into action element.

					if (fc.getMultiplicity() == 0) {
						// Display the repeat header for the group.
						HierarchyElement group = new HierarchyElement(fc.getLongText(), null,
								getResources().getDrawable(R.drawable.expander_ic_minimized), Color.WHITE, COLLAPSED,
								fc.getIndex());
						formList.add(group);
					}
					// Add this group name to the drop down list for this
					// repeating group.
					HierarchyElement h = formList.get(formList.size() - 1);
					h.addChild(new HierarchyElement(mIndent + fc.getLongText() + " " + (fc.getMultiplicity() + 1), null,
							null, Color.WHITE, CHILD, fc.getIndex()));
					break;
				}
				event = formController.stepToNextEvent(FormController.STEP_INTO_GROUP);
			}

			HierarchyListAdapter itla = new HierarchyListAdapter(this);
			itla.setListItems(formList);
			setListAdapter(itla);

			// set the controller back to the current index in case the user
			// hits 'back'
			formController.jumpToIndex(currentIndex);
		} catch (Exception e) {
			Log.e(t, e.getMessage(), e);
			createErrorDialog(e.getMessage());
		}
	}

	/**
	 * Creates and displays dialog with the given errorMsg.
	 */
	private void createErrorDialog(String errorMsg) {
		Collect.getInstance().getActivityLogger().logInstanceAction(this, "createErrorDialog", "show.");

		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setIcon(android.R.drawable.ic_dialog_info);
		alertDialog.setTitle(getString(R.string.error_occured));
		alertDialog.setMessage(errorMsg);
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON_POSITIVE:
					Collect.getInstance().getActivityLogger().logInstanceAction(this, "createErrorDialog", "OK");
					FormController formController = Collect.getInstance().getFormController();
					formController.jumpToIndex(currentIndex);
					break;
				}
			}
		};
		alertDialog.setCancelable(false);
		alertDialog.setButton(getString(R.string.ok), errorListener);
		alertDialog.show();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		HierarchyElement h = (HierarchyElement) l.getItemAtPosition(position);
		FormIndex index = h.getFormIndex();
		if (index == null) {
			goUpLevel();
			return;
		}

		switch (h.getType()) {
		case EXPANDED:
			Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick", "COLLAPSED",
					h.getFormIndex());
			h.setType(COLLAPSED);
			ArrayList<HierarchyElement> children = h.getChildren();
			for (int i = 0; i < children.size(); i++) {
				formList.remove(position + 1);
			}
			h.setIcon(getResources().getDrawable(R.drawable.expander_ic_minimized));
			h.setColor(Color.WHITE);
			break;
		case COLLAPSED:
			Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick", "EXPANDED",
					h.getFormIndex());
			h.setType(EXPANDED);
			ArrayList<HierarchyElement> children1 = h.getChildren();
			for (int i = 0; i < children1.size(); i++) {
				Log.i(t, "adding child: " + children1.get(i).getFormIndex());
				formList.add(position + 1 + i, children1.get(i));

			}
			h.setIcon(getResources().getDrawable(R.drawable.expander_ic_maximized));
			h.setColor(Color.WHITE);
			break;
		case QUESTION:
			Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick", "QUESTION-JUMP",
					index);
			Collect.getInstance().getFormController().jumpToIndex(index);
			if (Collect.getInstance().getFormController().indexIsInFieldList()) {
				try {
					Collect.getInstance().getFormController().stepToPreviousScreenEvent();
				} catch (JavaRosaException e) {
					Log.e(t, e.getMessage(), e);
					createErrorDialog(e.getCause().getMessage());
					return;
				}
			}
			setResult(RESULT_OK);
			//Avinash
			if (this.getIntent().getStringExtra("Action").equalsIgnoreCase("EditSaved"))
				finish();
			return;
		case CHILD:
			Collect.getInstance().getActivityLogger().logInstanceAction(this, "onListItemClick", "REPEAT-JUMP",
					h.getFormIndex());
			Collect.getInstance().getFormController().jumpToIndex(h.getFormIndex());
			setResult(RESULT_OK);
			refreshView();
			return;
		}

		// Should only get here if we've expanded or collapsed a group
		HierarchyListAdapter itla = new HierarchyListAdapter(this);
		itla.setListItems(formList);
		setListAdapter(itla);
		getListView().setSelection(position);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			Collect.getInstance().getActivityLogger().logInstanceAction(this, "onKeyDown", "KEYCODE_BACK.JUMP",
					mStartIndex);
			Collect.getInstance().getFormController().jumpToIndex(mStartIndex);
		}
		return super.onKeyDown(keyCode, event);
	}

}
