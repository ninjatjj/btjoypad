package com.ninjatjj.btjoypad;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.NativeActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import com.ninjatjj.btjoypad.server.BTJoypadPreferences;
import com.ninjatjj.keyboardhandler.KeyHandler;

public class BTJoypadClient extends NativeActivity {

	private static final byte SHOW_CONFIG = 0;
	private static final byte KEYDOWN = 1;
	private static final byte KEYUP = 2;

	private boolean leftLeft = false;
	private boolean leftRight = false;
	private boolean leftUp = false;
	private boolean leftDown = false;
	private boolean rightLeft = false;
	private boolean rightRight = false;
	private boolean rightUp = false;
	private boolean rightDown = false;
	private boolean mid;

	protected static final int DISCONNECTED = 1;
	protected static final int CONNECTED = 2;
	protected static final int NOT_CONNECTED = 3;
	protected static final int CANNOT_CONNECT = 4;
	protected static final int CHOSEN = 5;
	protected static final int CONNECTING = 6;

	private static List<Integer> ignoredKeyCodes = new ArrayList<Integer>();
	static {
		ignoredKeyCodes.add(KeyEvent.KEYCODE_MENU);
		System.loadLibrary("touchpadndkjava");
	}

	native int RegisterThis();

	private TextView mTitle;
	private Menu menu;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CONNECTING:
				mTitle.setText(R.string.title_connecting);
				if (menu != null) {
					menu.getItem(0).setEnabled(false);
				}
				break;
			case CONNECTED:
				mTitle.setText(R.string.title_connected);
				if (menu != null) {
					menu.getItem(0).setTitle("Disconnect");
					menu.getItem(0).setEnabled(true);
					menu.getItem(2).setEnabled(true);
				}
				break;
			case CHOSEN:
				SharedPreferences defaultSharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(BTJoypadClient.this);
				String string = defaultSharedPreferences.getString(
						"remoteAddress", null);
				MenuItem item = menu.getItem(1);
				item.setEnabled(true);
				item.setTitle("Forget " + string);
				break;
			case NOT_CONNECTED:
				mTitle.setText(R.string.title_not_connected);
				if (menu != null) {
					menu.getItem(0).setTitle("Connect");
					menu.getItem(0).setEnabled(true);
					menu.getItem(2).setEnabled(false);
				}
				break;
			case CANNOT_CONNECT:
				mTitle.setText(R.string.title_not_connected);
				if (menu != null) {
					menu.getItem(0).setTitle("Connect");
					menu.getItem(0).setEnabled(true);
					menu.getItem(2).setEnabled(false);
				}
				// Toast.makeText(BTJoypadClient.this, "Could not connect",
				// Toast.LENGTH_SHORT).show();
				AlertBox("Disconnected", "Could not connect: " + msg.obj);
				break;
			case DISCONNECTED:
				AlertBox("Disconnected", "Server has disconnected: " + msg.obj);
				BTJoypadConnection.getInstance().disconnect(this);
				mTitle.setText(R.string.title_not_connected);
				if (menu != null) {
					menu.getItem(0).setTitle("Connect");
					menu.getItem(2).setEnabled(false);
				}
				break;
			}
		}
	};

	// Initiating Menu XML file (menu.xml)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.menu, menu);
		this.menu = menu;
		if (BTJoypadConnection.getInstance().connected()) {
			menu.getItem(0).setTitle("Disconnect");
			menu.getItem(2).setEnabled(true);
		} else {
			menu.getItem(0).setTitle("Connect");
			menu.getItem(2).setEnabled(false);
		}
		SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String string = defaultSharedPreferences.getString("remoteAddress",
				null);
		MenuItem item = menu.getItem(1);
		if (string == null) {
			item.setEnabled(false);
			item.setTitle("Forget");
		} else {
			item.setEnabled(true);
			item.setTitle("Forget " + string);
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

		case R.id.disconnect:

			SharedPreferences defaultSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			final Editor editor = defaultSharedPreferences.edit();
			editor.putString("remoteAddress", null);
			editor.commit();
			menu.getItem(1).setEnabled(false);
			item.setEnabled(false);
			item.setTitle("Forget");

			BTJoypadConnection.getInstance().disconnect(handler);
			mTitle.setText(R.string.title_not_connected);
			menu.getItem(0).setTitle("Connect");
			menu.getItem(2).setEnabled(false);

			return true;
		case R.id.connect:
			if (BTJoypadConnection.getInstance().connected()) {
				BTJoypadConnection.getInstance().disconnect(handler);
				mTitle.setText(R.string.title_not_connected);
				menu.getItem(0).setTitle("Connect");
				menu.getItem(2).setEnabled(false);
			} else {
				final SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(this);
				String address = settings.getString("remoteAddress", null);
				if (address == null) {

					BluetoothConnectActivityReceiver mBluetoothPickerReceiver = new BluetoothConnectActivityReceiver(
							handler);
					registerReceiver(
							mBluetoothPickerReceiver,
							new IntentFilter(
									"android.bluetooth.devicepicker.action.DEVICE_SELECTED"));
					startActivity(new Intent(
							"android.bluetooth.devicepicker.action.LAUNCH")
							.putExtra(
									"android.bluetooth.devicepicker.extra.NEED_AUTH",
									false)
							.putExtra(
									"android.bluetooth.devicepicker.extra.FILTER_TYPE",
									0)
							.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
				} else {
					try {
						BTJoypadConnection.getInstance().connect(handler,
								address);
					} catch (Exception e) {
						Log.d("btjoypad",
								"Could not connect: " + e.getMessage(), e);
					}
				}
			}
			return true;
		case R.id.serverSettings:
			OutputStream outputStream = BTJoypadConnection.getInstance()
					.getOutputStream();
			try {
				synchronized (outputStream) {
					outputStream.write(SHOW_CONFIG);
					outputStream.flush();
				}
			} catch (IOException e) {
				Log.e("btjoypad",
						"Could not request server settings: " + e.getMessage(),
						e);
				AlertBox("Disconnecting", "Could not request server settings: "
						+ e.getMessage());
				BTJoypadConnection.getInstance().disconnect(handler);
			}
			return true;
		case R.id.mappingPrefs:
			startActivity(new Intent(this, BTJoypadPreferences.class));
			return true;
		case R.id.exit:
			BTJoypadConnection.getInstance().disconnect(handler);
			System.exit(0);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.onCreate(savedInstanceState);
		getWindow().takeSurface(null);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);
		setContentView(R.layout.main_client);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		handler.obtainMessage(NOT_CONNECTED).sendToTarget();

		BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!defaultAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBtIntent);
		}

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		String address = settings.getString("remoteAddress", null);
		if (address != null) {
			// try {
			BTJoypadConnection.getInstance().connect(handler, address);
			// } catch (IOException e) {
			// Log.d("btjoypad", "Could not connect: " + e.getMessage(), e);
			// }
		}
		RegisterThis();
	}

	// source is 1048584
	// action is2 is down, 1 is up
	// x on left is 360
	// x on right is 966
	// y is 0 to 360
	public boolean OnNativeMotion(final int action, final int x, final int y,
			final int source, final int device_id) {
		if (source == 1048584) {
			Log.d("btjoypad", x + " " + y + " " + action);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (x < 360) {
						processAnalog(action == 2, x, y, true);
					} else if (x > 606) {
						int xx = x - 606;
						processAnalog(action == 2, xx, y, false);
					} else {
						Log.d("btjoypad", "Mid pad (" + x + ", " + y + ", "
								+ source + ", " + device_id + ", " + action);

						if (x < 483) {
							if (leftLeft) {
								processKey(KeyHandler.ANALOG_LEFT_LEFT, KEYUP);
								leftLeft = false;
							}
							if (leftRight) {
								processKey(KeyHandler.ANALOG_LEFT_RIGHT, KEYUP);
								leftRight = false;
							}
							if (leftUp) {
								processKey(KeyHandler.ANALOG_LEFT_UP, KEYUP);
								leftUp = false;
							}
							if (leftDown) {
								processKey(KeyHandler.ANALOG_LEFT_DOWN, KEYUP);
								leftDown = false;
							}
						} else {
							if (rightLeft) {
								processKey(KeyHandler.ANALOG_RIGHT_LEFT, KEYUP);
								rightLeft = false;
							}
							if (rightRight) {
								processKey(KeyHandler.ANALOG_RIGHT_RIGHT, KEYUP);
								rightRight = false;
							}
							if (rightUp) {
								processKey(KeyHandler.ANALOG_RIGHT_UP, KEYUP);
								rightUp = false;
							}
							if (rightDown) {
								processKey(KeyHandler.ANALOG_RIGHT_DOWN, KEYUP);
								rightDown = false;
							}
						}

						if (action == 2 && !mid) {
							processKey(KeyHandler.ANALOG_MID, KEYDOWN);
							// processKey(KeyHandler.ANALOG_MID, KEYUP); //
							// HACK!
							mid = true;
							// mid = false;
						} else if (action == 1 && mid) {
							processKey(KeyHandler.ANALOG_MID, KEYUP);
							mid = false;
						}
					}
				}

				private void processAnalog(boolean down, int x, int y,
						boolean leftStick) {
					if (mid) {
						processKey(KeyHandler.ANALOG_MID, KEYUP);
						mid = false;
					}

					boolean pressLeft = false;
					boolean pressRight = false;
					boolean pressUp = false;
					boolean pressDown = false;

					double atan2 = Math.atan2(y - 180, x - 180) * 100;
					Log.d("btjoypad", "Left stick (" + x + ", " + y + ", "
							+ source + ", " + device_id + ", " + action + ", "
							+ atan2 + ")");

					double overlap = (315 / 3);
					if (down) {

						if (atan2 < 157.5 + overlap && atan2 > 157.5 - overlap) {
							pressUp = true;
							if (leftStick && !leftUp) {
								processKey(KeyHandler.ANALOG_LEFT_UP, KEYDOWN);
								leftUp = true;
							} else if (!leftStick && !rightUp) {
								processKey(KeyHandler.ANALOG_RIGHT_UP, KEYDOWN);
								rightUp = true;
							}
						}
						if (atan2 > 315 - overlap || atan2 < -315 + overlap) {
							pressLeft = true;
							if (leftStick && !leftLeft) {
								processKey(KeyHandler.ANALOG_LEFT_LEFT, KEYDOWN);
								leftLeft = true;
							} else if (!leftStick && !rightLeft) {
								processKey(KeyHandler.ANALOG_RIGHT_LEFT,
										KEYDOWN);
								rightLeft = true;
							}
						}
						if (atan2 > -157.5 - overlap
								&& atan2 < -157.5 + overlap) {
							pressDown = true;
							if (leftStick && !leftDown) {
								processKey(KeyHandler.ANALOG_LEFT_DOWN, KEYDOWN);
								leftDown = true;
							} else if (!leftStick && !rightDown) {
								processKey(KeyHandler.ANALOG_RIGHT_DOWN,
										KEYDOWN);
								rightDown = true;
							}
						}
						if (atan2 < 0 + overlap && atan2 > 0 - overlap) {
							pressRight = true;
							if (leftStick && !leftRight) {
								processKey(KeyHandler.ANALOG_LEFT_RIGHT,
										KEYDOWN);
								leftRight = true;
							} else if (!leftStick && !rightRight) {
								processKey(KeyHandler.ANALOG_RIGHT_RIGHT,
										KEYDOWN);
								rightRight = true;
							}
						}
					} else {
						Log.d("btjoypad", "was up");
					}

					if (leftStick) {
						if (leftLeft && !pressLeft) {
							processKey(KeyHandler.ANALOG_LEFT_LEFT, KEYUP);
							leftLeft = false;
						}
						if (leftRight && !pressRight) {
							processKey(KeyHandler.ANALOG_LEFT_RIGHT, KEYUP);
							leftRight = false;
						}
						if (leftUp && !pressUp) {
							processKey(KeyHandler.ANALOG_LEFT_UP, KEYUP);
							leftUp = false;
						}
						if (leftDown && !pressDown) {
							processKey(KeyHandler.ANALOG_LEFT_DOWN, KEYUP);
							leftDown = false;
						}
					} else {
						if (rightLeft && !pressLeft) {
							processKey(KeyHandler.ANALOG_RIGHT_LEFT, KEYUP);
							rightLeft = false;
						}
						if (rightRight && !pressRight) {
							processKey(KeyHandler.ANALOG_RIGHT_RIGHT, KEYUP);
							rightRight = false;
						}
						if (rightUp && !pressUp) {
							processKey(KeyHandler.ANALOG_RIGHT_UP, KEYUP);
							rightUp = false;
						}
						if (rightDown && !pressDown) {
							processKey(KeyHandler.ANALOG_RIGHT_DOWN, KEYUP);
							rightDown = false;
						}
					}
				}
			});
		}
		return true;
	}

	public void OnNativeKeyPress(final int action, final int keyCode) {
		// KeyEvent keyEvent = new KeyEvent(action, keyCode);
		// dispatchKeyEvent(keyEvent)

		if (!BTJoypadConnection.getInstance().connected()
				&& keyCode == KeyEvent.KEYCODE_BACK) {
			System.exit(0);
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			runOnUiThread(new Runnable() {
				public void run() {
					new Handler().post(new Runnable() {
						public void run() {
							openOptionsMenu();
						}
					});
				}
			});
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (action == KeyEvent.ACTION_DOWN) {
					onKeyDown(keyCode, new KeyEvent(action, keyCode));
				} else if (action == KeyEvent.ACTION_UP) {
					onKeyUp(keyCode, new KeyEvent(action, keyCode));
				}
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		processKey(keyCode, KEYDOWN);
		return super.onKeyDown(keyCode, event);
	}

	private boolean processKey(int keyCode, byte action) {
		Log.d("btjoypad", keyCode + " " + action);
		if (BTJoypadConnection.getInstance().connected()
				&& !ignoredKeyCodes.contains(keyCode)) {
			OutputStream outputStream = BTJoypadConnection.getInstance()
					.getOutputStream();
			try {
				synchronized (outputStream) {
					outputStream.write(action);
					outputStream.write(keyCode);
					outputStream.flush();
				}
				return true;
			} catch (IOException e) {
				AlertBox("Disconnecting",
						"Could not send key: " + e.getMessage());
				BTJoypadConnection.getInstance().disconnect(handler);
			}
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		processKey(keyCode, KEYUP);
		return super.onKeyUp(keyCode, event);
	}

	public void AlertBox(String title, String message) {
		new AlertDialog.Builder(this).setTitle(title)
				.setMessage(message + " Press OK to exit.")
				.setPositiveButton("OK", new OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
					}
				}).show();
	}
}