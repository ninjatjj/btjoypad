package com.ninjatjj.keyboardhandler;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.KeyEvent;

import com.ninjatjj.btjoypad.R;

public class KeyHandlerPreferences {

	private PreferenceActivity preferenceActivity;

	public KeyHandlerPreferences(final PreferenceActivity preferenceActivity) {
		this.preferenceActivity = preferenceActivity;

		final SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(preferenceActivity);
		preferenceActivity.findPreference("configuration")
				.setOnPreferenceChangeListener(
						new Preference.OnPreferenceChangeListener() {
							@Override
							public boolean onPreferenceChange(
									Preference preference, Object newValue) {

								PreferenceCategory mCategory = (PreferenceCategory) preferenceActivity
										.findPreference("mappings");
								int preferenceCount = mCategory
										.getPreferenceCount();
								for (int i = 0; i < preferenceCount; i++) {
									Preference preference2 = mCategory
											.getPreference(0);
									mCategory.removePreference(preference2);
								}
								addMappings((String) newValue);

								mCategory = (PreferenceCategory) preferenceActivity
										.findPreference("autofires");
								preferenceCount = mCategory
										.getPreferenceCount();
								for (int i = 0; i < preferenceCount; i++) {
									Preference preference2 = mCategory
											.getPreference(0);
									mCategory.removePreference(preference2);
								}
								addAutofire((String) newValue);

								preferenceActivity.onContentChanged();
								return true;
							}
						});
		String configurationNumber = defaultSharedPreferences.getString(
				"configuration", "1");
		addMappings(configurationNumber);
		addAutofire(configurationNumber);
	}

	private void addMappings(final String configurationNumber) {

		SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(preferenceActivity);
		Iterator<String> iterator = defaultSharedPreferences.getAll().keySet()
				.iterator();
		while (iterator.hasNext()) {
			String mappingKey = iterator.next();
			if (mappingKey.startsWith("mappingKey" + configurationNumber)) {
				addMappingPreference(configurationNumber, mappingKey.replace(
						"mappingKey" + configurationNumber, ""));
			}
		}
		Preference addButton = (Preference) preferenceActivity
				.findPreference("button");
		addButton
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						addMappingPreference(configurationNumber, UUID
								.randomUUID().toString());
						return true;
					}

				});

	}

	private void addMappingPreference(final String configurationNumber,
			final String mappingNumber) {
		Map<String, ?> all = PreferenceManager.getDefaultSharedPreferences(
				preferenceActivity).getAll();
		final PreferenceCategory preferencesCategory = (PreferenceCategory) preferenceActivity
				.findPreference("mappings");

		final PreferenceScreen preferenceScreen = preferenceActivity
				.getPreferenceManager().createPreferenceScreen(
						preferenceActivity);
		preferenceScreen.setKey("mappingScreen" + configurationNumber + ""
				+ mappingNumber);

		final ListPreference listPref = new ListPreference(preferenceActivity);

		preferenceScreen.addPreference(listPref);
		listPref.setEntries(R.array.keyCodes);
		listPref.setEntryValues(R.array.keyCodes);
		listPref.setKey("mappingKey" + configurationNumber + "" + mappingNumber);
		String mappingValue = (String) all.get("mappingKey"
				+ configurationNumber + "" + mappingNumber);
		if (mappingValue == null || mappingValue.length() == 0) {
			listPref.setTitle("Configure new mapping");
			preferenceScreen.setTitle("Configure new mapping");
		} else {
			listPref.setTitle(mappingValue);
			preferenceScreen.setTitle(mappingValue);
		}
		listPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference arg0,
					final Object value) {
				preferenceActivity.runOnUiThread(new Runnable() {
					public void run() {
						arg0.setTitle((CharSequence) value);
						preferenceScreen.setTitle((CharSequence) value);
						preferenceActivity.onContentChanged();
					}
				});
				return true;
			}
		});

		final ListPreference listPref2 = new ListPreference(preferenceActivity);
		preferenceScreen.addPreference(listPref2);
		listPref2.setEntries(R.array.keyCodes);
		listPref2.setEntryValues(R.array.keyCodes);
		listPref2.setKey("mappingValue" + configurationNumber + ""
				+ mappingNumber);
		String mappingValue2 = (String) all.get("mappingValue"
				+ configurationNumber + "" + mappingNumber);
		if (mappingValue2 == null || mappingValue2.length() == 0) {
			listPref2.setTitle("Configure new mapping");
			preferenceScreen.setSummary("Configure new mapping");
		} else {
			listPref2.setTitle(mappingValue2);
			preferenceScreen.setSummary(mappingValue2);
		}
		listPref2
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(final Preference arg0,
							final Object value) {
						preferenceActivity.runOnUiThread(new Runnable() {
							public void run() {
								arg0.setTitle((CharSequence) value);
								preferenceScreen
										.setSummary((CharSequence) value);
								preferenceActivity.onContentChanged();
							}
						});
						return true;
					}
				});

		Preference detectButton = new Preference(preferenceActivity);
		detectButton.setKey("mappingDetect" + configurationNumber + ""
				+ mappingNumber);
		detectButton.setTitle("Detect");
		detectButton
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						Builder setNegativeButton = new AlertDialog.Builder(
								preferenceActivity).setTitle("Press any key")
								.setNegativeButton("Cancel",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface arg0,
													int arg1) {
											}
										});
						final AlertDialog create = setNegativeButton.create();

						create.setOnKeyListener(new DialogInterface.OnKeyListener() {
							@Override
							public boolean onKey(DialogInterface dialog,
									final int keyCode, KeyEvent event) {
								preferenceActivity
										.runOnUiThread(new Runnable() {
											public void run() {

												String keyCodesByValue = KeyHandler
														.getKeyCodesByValue(keyCode);
												if (keyCodesByValue != null) {
													listPref.setTitle((CharSequence) keyCodesByValue);
													listPref.setValue(keyCodesByValue);
													preferenceScreen
															.setTitle((CharSequence) keyCodesByValue);
													preferenceActivity
															.onContentChanged();
												}
											}
										});

								create.dismiss();
								preferenceActivity.onContentChanged();
								return true;
							}
						});
						create.show();
						return true;
					}
				});
		preferenceScreen.addPreference(detectButton);

		Preference deleteButton = new Preference(preferenceActivity);
		deleteButton.setKey("mappingDelete" + configurationNumber + ""
				+ mappingNumber);
		deleteButton.setTitle("Delete");
		deleteButton
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						SharedPreferences defaultSharedPreferences = PreferenceManager
								.getDefaultSharedPreferences(preferenceActivity);
						SharedPreferences.Editor editor2 = defaultSharedPreferences
								.edit();
						editor2.remove("mappingKey" + configurationNumber + ""
								+ mappingNumber);
						editor2.remove("mappingValue" + configurationNumber
								+ "" + mappingNumber);
						editor2.commit();

						preferenceScreen.removeAll();

						preferencesCategory.removePreference(preferenceScreen);
						preferenceScreen.getDialog().dismiss();
						return true;
					}
				});
		preferenceScreen.addPreference(deleteButton);

		Preference backButton = new Preference(preferenceActivity);
		backButton.setKey("mappingBack" + configurationNumber + ""
				+ mappingNumber);
		backButton.setTitle("Back");
		backButton
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						preferenceScreen.getDialog().dismiss();
						return true;
					}
				});
		preferenceScreen.addPreference(backButton);

		preferencesCategory.addPreference(preferenceScreen);
	}

	private void addAutofire(final String configurationNumber) {

		SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(preferenceActivity);
		Iterator<String> iterator = defaultSharedPreferences.getAll().keySet()
				.iterator();
		while (iterator.hasNext()) {
			String mappingKey = iterator.next();
			if (mappingKey.startsWith("autofireKey" + configurationNumber)) {
				addAutofirePreference(configurationNumber, mappingKey.replace(
						"autofireKey" + configurationNumber, ""));
			}
		}
		Preference addButton = (Preference) preferenceActivity
				.findPreference("addAutofire");
		addButton
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						addAutofirePreference(configurationNumber, UUID
								.randomUUID().toString());
						return true;
					}

				});
	}

	private void addAutofirePreference(final String configurationNumber,
			final String autofireNumber) {

		Map<String, ?> all = PreferenceManager.getDefaultSharedPreferences(
				preferenceActivity).getAll();

		final PreferenceCategory preferencesCategory = (PreferenceCategory) preferenceActivity
				.findPreference("autofires");

		final PreferenceScreen preferenceScreen = preferenceActivity
				.getPreferenceManager().createPreferenceScreen(
						preferenceActivity);
		preferenceScreen.setKey("autofireScreen" + configurationNumber + ""
				+ autofireNumber);

		final ListPreference listPref = new ListPreference(preferenceActivity);
		preferenceScreen.addPreference(listPref);
		listPref.setEntries(R.array.keyCodes);
		listPref.setEntryValues(R.array.keyCodes);
		listPref.setKey("autofireKey" + configurationNumber + ""
				+ autofireNumber);

		String mappingValue = (String) all.get("autofireKey"
				+ configurationNumber + "" + autofireNumber);
		if (mappingValue == null || mappingValue.length() == 0) {
			listPref.setTitle("Configure new autofire");
			preferenceScreen.setTitle("Configure new autofire");
		} else {
			listPref.setTitle(mappingValue);
			preferenceScreen.setTitle(mappingValue);
		}

		listPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference arg0,
					final Object value) {
				preferenceActivity.runOnUiThread(new Runnable() {
					public void run() {

						arg0.setTitle((CharSequence) value);
						Preference findPreference = preferenceActivity
								.findPreference("autofireScreen"
										+ configurationNumber + ""
										+ autofireNumber);
						findPreference.setTitle((CharSequence) value);
						preferenceActivity.onContentChanged();
					}
				});
				return true;
			}
		});

		Preference detectButton = new Preference(preferenceActivity);
		detectButton.setKey("autofireDetect" + configurationNumber + ""
				+ autofireNumber);
		detectButton.setTitle("Detect");
		detectButton
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						Builder setNegativeButton = new AlertDialog.Builder(
								preferenceActivity).setTitle("Press any key")
								.setNegativeButton("Cancel",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface arg0,
													int arg1) {
											}
										});
						final AlertDialog create = setNegativeButton.create();

						create.setOnKeyListener(new DialogInterface.OnKeyListener() {
							@Override
							public boolean onKey(DialogInterface dialog,
									final int keyCode, KeyEvent event) {
								preferenceActivity
										.runOnUiThread(new Runnable() {
											public void run() {

												String keyCodesByValue = KeyHandler
														.getKeyCodesByValue(keyCode);
												if (keyCodesByValue != null) {
													listPref.setTitle((CharSequence) keyCodesByValue);
													listPref.setValue(keyCodesByValue);
													Preference findPreference = preferenceActivity
															.findPreference("autofireScreen"
																	+ configurationNumber
																	+ ""
																	+ autofireNumber);
													findPreference
															.setTitle((CharSequence) keyCodesByValue);
													preferenceActivity
															.onContentChanged();
												}
											}
										});

								create.dismiss();
								preferenceActivity.onContentChanged();
								return true;
							}
						});
						create.show();
						return true;
					}
				});
		preferenceScreen.addPreference(detectButton);

		Preference deleteButton = new Preference(preferenceActivity);
		deleteButton.setKey("autofireDelete" + configurationNumber + ""
				+ autofireNumber);
		deleteButton.setTitle("Delete");
		deleteButton
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						SharedPreferences defaultSharedPreferences = PreferenceManager
								.getDefaultSharedPreferences(preferenceActivity);
						SharedPreferences.Editor editor2 = defaultSharedPreferences
								.edit();
						editor2.remove("autofireKey" + configurationNumber + ""
								+ autofireNumber);
						editor2.commit();

						preferenceScreen.removeAll();

						preferencesCategory.removePreference(preferenceScreen);
						preferenceScreen.getDialog().dismiss();
						return true;
					}
				});
		preferenceScreen.addPreference(deleteButton);

		Preference backButton = new Preference(preferenceActivity);
		backButton.setKey("autofireBack" + configurationNumber + ""
				+ autofireNumber);
		backButton.setTitle("Back");
		backButton
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						preferenceScreen.getDialog().dismiss();
						return true;
					}
				});
		preferenceScreen.addPreference(backButton);

		preferencesCategory.addPreference(preferenceScreen);
	}
}
