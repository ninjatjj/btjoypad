package com.ninjatjj.btjoypad;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.ninjatjj.btjoypad.server.BTJoypadPreferences;
import com.ninjatjj.btjoypad.server.BTJoypadServer;
import com.ninjatjj.btjoypad.server.StartedStatusListener;

public class BTJoypad extends Activity implements StartedStatusListener {

	private BTJoypadServer btjoypadServer;

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			btjoypadServer = ((BTJoypadServer.LocalBinder) service)
					.getService();
			btjoypadServer.setStartedStatusListener(BTJoypad.this);

			if (menu != null) {
				if (btjoypadServer.isStarted()) {
					menu.getItem(1).setVisible(false);
				} else {
					menu.getItem(2).setVisible(false);
				}
			}

		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			btjoypadServer = null;
		}
	};

	private Menu menu;

	// Initiating Menu XML file (menu.xml)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.menu_server, menu);

		this.menu = menu;

		if (btjoypadServer != null) {
			if (btjoypadServer.isStarted()) {
				menu.getItem(1).setVisible(false);
			} else {
				menu.getItem(2).setVisible(false);
			}
		}

		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.startClient:
			startActivity(new Intent(this, BTJoypadClient.class));
			return true;
		case R.id.startServer:
			startService(new Intent(BTJoypadServer.class.getName()));
			menu.getItem(1).setVisible(false);
			menu.getItem(2).setVisible(true);
			return true;
		case R.id.stopServer:
			stopService(new Intent(BTJoypadServer.class.getName()));
			menu.getItem(1).setVisible(true);
			menu.getItem(2).setVisible(false);
			return true;
		case R.id.selectKeyboard:
			InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			im.showInputMethodPicker();
			return true;
		case R.id.mappingPrefs:
			startActivity(new Intent(this, BTJoypadPreferences.class));
			return true;
		case R.id.exit:
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_server);

		Button selectKeyboard = (Button) this.findViewById(R.id.selectKeyboard);
		selectKeyboard.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				im.showInputMethodPicker();
			}
		});
		
		Button startClient = (Button) this.findViewById(R.id.startClient);
		startClient.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(BTJoypad.this, BTJoypadClient.class));
			}
		});

		bindService(new Intent(this, BTJoypadServer.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		btjoypadServer.setStartedStatusListener(null);
		unbindService(mConnection);
	}

	@Override
	public void onResume() {
		super.onResume();
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (BTJoypadServer.class.getName().equals(
					service.service.getClassName())) {
				// return true;
			}
		}
	}

	@Override
	public void startedStatusChanged(boolean isStarted) {
		if (menu != null) {
			menu.getItem(1).setVisible(!isStarted);
			menu.getItem(2).setVisible(isStarted);
		}
	}
}