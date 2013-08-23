package com.ninjatjj.btjoypad.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

public class StartAtBoot extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"startAtBoot", true)) {
			Log.d("btjoypad", "com.ninjatjj.btjoypad.server.StartAtBoot true");

			if ("android.intent.action.BOOT_COMPLETED".equals(intent
					.getAction())) {

				context.startService(new Intent(BTJoypadServer.class.getName()));
			}
		} else {
			Log.d("btjoypad", "com.ninjatjj.btjoypad.server.StartAtBoot false");
		}
	}
}
