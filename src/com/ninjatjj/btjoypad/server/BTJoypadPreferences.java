package com.ninjatjj.btjoypad.server;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.ninjatjj.btjoypad.R;
import com.ninjatjj.keyboardhandler.KeyHandlerPreferences;

public class BTJoypadPreferences extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Intent intent = getIntent();
		String unlock = intent.getStringExtra("unlock");

		if (unlock != null && unlock.equals("true")) {
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
			window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		}

		// final BTJoypadPreferences userSettingsActivity = this;

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.layout.settings);

		Preference selectIME = (Preference) findPreference("selectIME");
		selectIME
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {

						InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
						im.showInputMethodPicker();
						return true;
					}
				});

		Preference turnOn = (Preference) findPreference("turnOn");
		turnOn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {

				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(enableBtIntent);
				return true;
			}
		});

		Preference makeDiscoverable = (Preference) findPreference("makeDiscoverable");
		makeDiscoverable
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {

						Intent discoverableIntent = new Intent(
								BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
						discoverableIntent.putExtra(
								BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
								300);
						startActivity(discoverableIntent);
						return true;
					}
				});
		Preference enableBTJoypad = (Preference) findPreference("enableBTJoypad");
		enableBTJoypad
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {

						startActivity(new Intent(
								Settings.ACTION_INPUT_METHOD_SETTINGS));
						return true;
					}
				});

		new KeyHandlerPreferences(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
