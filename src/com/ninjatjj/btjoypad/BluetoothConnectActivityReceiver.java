package com.ninjatjj.btjoypad;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;

public class BluetoothConnectActivityReceiver extends BroadcastReceiver {

	private Handler handler;

	public BluetoothConnectActivityReceiver(Handler handler) {
		this.handler = handler;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if ("android.bluetooth.devicepicker.action.DEVICE_SELECTED"
				.equals(intent.getAction())) {
			context.unregisterReceiver(this);
			BluetoothDevice device = (BluetoothDevice) intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			final Editor settings = PreferenceManager
					.getDefaultSharedPreferences(context).edit();
			settings.putString("remoteAddress", device.getAddress());
			settings.commit();
			handler.obtainMessage(BTJoypadClient.CHOSEN).sendToTarget();

			// try {
			BTJoypadConnection.getInstance().connect(handler,
					device.getAddress());
			// } catch (IOException e) {
			// settings.putString("remoteAddress", null);
			// settings.commit();
			// }
		}
	}
}