/*
 * Copyright (C) 2014 Nafundi
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

package org.sdrc.collect.uphpmis.android.preferences;

import org.sdrc.collect.uphpmis.android.R;
import org.sdrc.collect.uphpmis.android.utilities.UrlUtils;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Toast;

/**
 * Handles aggregate specific preferences.
 * 
 * @author Carl Hartung (chartung@nafundi.com)
 * @author Ratikanta Pradhan (ratikanta@sdrc.co.in)
 */
public class AggregatePreferencesActivity extends PreferenceActivity {

	/**
	 * This variable will help the user to modify SDRC middle layer URL
	 */
	protected EditTextPreference mSubmissionServerUrlPreference;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.aggregate_preferences);
		mSubmissionServerUrlPreference = (EditTextPreference) findPreference(PreferencesActivity.KEY_SUBMISSION_SERVER_URL);
		mSubmissionServerUrlPreference.setEnabled(false);
		
		
		
		
		
		
		//Ratikanta
		mSubmissionServerUrlPreference
		.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				String url = newValue.toString();

//				 remove all trailing "/"s
				while (url.endsWith("/")) {
					url = url.substring(0, url.length() - 1);
				}

				if (UrlUtils.isValidUrl(url)) {
					preference.setSummary(newValue.toString());
					return true;
				} else {
					Toast.makeText(getApplicationContext(),
							R.string.url_error, Toast.LENGTH_SHORT)
							.show();
					return false;
				}
			}
		});
		mSubmissionServerUrlPreference.setSummary(mSubmissionServerUrlPreference.getText());
		mSubmissionServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });


	}

	/**
	 * Disallows carriage returns from user entry
	 * 
	 * @return
	 */
	protected InputFilter getReturnFilter() {
		InputFilter returnFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.getType((source.charAt(i))) == Character.CONTROL) {
						return "";
					}
				}
				return null;
			}
		};
		return returnFilter;
	}

	protected InputFilter getWhitespaceFilter() {
		InputFilter whitespaceFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.isWhitespace(source.charAt(i))) {
						return "";
					}
				}
				return null;
			}
		};
		return whitespaceFilter;
	}

}
