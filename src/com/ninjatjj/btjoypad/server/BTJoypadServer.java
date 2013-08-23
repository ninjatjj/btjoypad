package com.ninjatjj.btjoypad.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;

public class BTJoypadServer extends Service {
	// Used by BTJoypadConnection
	public static final UUID MY_UUID_SECURE = UUID
			.fromString("86AF17F8-8A50-4D7F-9F23-4FD16E4E072A");

	// private volatile BTJoypadKeyboard btJoypadKeyboard;

	private final BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, final Intent intent) {
			final String action = intent.getAction();
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = intent.getIntExtra(
						BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch (state) {
				case BluetoothAdapter.STATE_ON:
					synchronized (BTJoypadServer.this) {
						BTJoypadServer.this.notify();
					}
					break;
				case BluetoothAdapter.STATE_OFF:
					ssp.disconnect();
					break;
				}
			}
		}
	};

	private boolean started;

	public class LocalBinder extends Binder {
		public BTJoypadServer getService() {
			return BTJoypadServer.this;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("btjoypad", "Received start id " + startId + ": " + intent);
		started = true;
		if (startedStatusListener != null) {
			startedStatusListener.startedStatusChanged(started);
		}
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	private final ServerSocketProcessor ssp = new ServerSocketProcessor();

	private StartedStatusListener startedStatusListener;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		started = false;
		new Thread(new Runnable() {
			public void run() {
				File root = Environment.getExternalStorageDirectory();
				if (root.canWrite()) {
					File smsappLogFolder = new File(root, "btjoypad");
					if (!smsappLogFolder.exists()) {
						smsappLogFolder.mkdir();
					}
					DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
					Date today = Calendar.getInstance().getTime();
					String reportDate = df.format(today);
					File gpslogfile = new File(smsappLogFolder, "btjoypad-"
							+ reportDate + ".txt");
					try {
						Process process = Runtime.getRuntime().exec(
								"logcat -v time -s btjoypad");
						BufferedReader bufferedReader = new BufferedReader(
								new InputStreamReader(process.getInputStream()));

						String line;
						while ((line = bufferedReader.readLine()) != null) {
							FileWriter gpswriter = new FileWriter(gpslogfile,
									true);
							PrintWriter out = new PrintWriter(gpswriter);
							out.append(line + "\n");
							out.flush();
							out.close();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}, "logwriter").start();

		super.onCreate();

		IntentFilter filter = new IntentFilter();
		filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
		registerReceiver(receiver, filter);
		ssp.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		ssp.shutdown();
		unregisterReceiver(receiver);
	}

	// public void setBTJoypadKeyboard(BTJoypadKeyboard btJoypadKeyboard) {
	// this.btJoypadKeyboard = btJoypadKeyboard;
	// }

	private class ServerSocketProcessor extends Thread {

		private static final int EXIT_CMD = -1;
		private static final byte SHOW_CONFIG = 0;
		private static final byte KEYDOWN = 1;
		private static final byte KEYUP = 2;

		private BluetoothAdapter btAdapter = BluetoothAdapter
				.getDefaultAdapter();
		private volatile BluetoothServerSocket mmServerSocket;
		private BluetoothSocket socket;
		private volatile boolean stopped;

		public ServerSocketProcessor() {
			super("ServerSocketProcessor");
		}

		public void shutdown() {
			Log.d("btjoypad", "shutdown called");
			stopped = true;
			disconnect();
			synchronized (this) {
				notify();
			}
			try {
				join();
			} catch (InterruptedException e) {
				Log.e("btjoypad", "Could not join server socket: " + e, e);
			}
		}

		public void disconnect() {
			Log.d("btjoypad", "disconnect called");
			if (mmServerSocket != null) {
				try {
					BluetoothServerSocket bss = mmServerSocket;
					mmServerSocket = null;
					bss.close();
				} catch (IOException e) {
					Log.d("btjoypad", "Could not close server socket", e);
				}
			}
			if (socket != null) {
				try {
					socket.close();
					socket = null;
				} catch (IOException e) {
					Log.d("btjoypad", "Could not close socket: " + e, e);
				}
			}
		}

		@Override
		public void run() {

			KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
			final KeyguardLock lock = keyguardManager
					.newKeyguardLock(KEYGUARD_SERVICE);

			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			BroadcastReceiver mReceiver = new BroadcastReceiver() {

				@Override
				public synchronized void onReceive(Context context,
						Intent intent) {
					if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
						lock.reenableKeyguard();
						Log.d("btjoypad", "re-enable lock");
					}
				}
			};
			registerReceiver(mReceiver, filter);

			while (btAdapter != null && !stopped) {
				try {
					synchronized (BTJoypadServer.this) {
						while (!btAdapter.isEnabled()) {
							if (PreferenceManager.getDefaultSharedPreferences(
									BTJoypadServer.this).getBoolean(
									"promptBluetooth", false)) {
								Intent enableBtIntent = new Intent(
										BluetoothAdapter.ACTION_REQUEST_ENABLE);
								// Required as not an Activity
								enableBtIntent
										.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(enableBtIntent);
							}

							try {
								BTJoypadServer.this.wait();
							} catch (InterruptedException e) {
								Log.e("btjoypad", "Couldn't wait", e);
							}
						}
					}
					if (!stopped) {
						mmServerSocket = btAdapter
								.listenUsingRfcommWithServiceRecord("btjoypad",
										MY_UUID_SECURE);
						while (!stopped && mmServerSocket != null) {

							Log.d("btjoypad", "Waiting for client");
							socket = mmServerSocket.accept();

							Log.d("btjoypad", "client connecting");
							PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
							WakeLock wl = pm
									.newWakeLock(
											PowerManager.FULL_WAKE_LOCK
													| PowerManager.ACQUIRE_CAUSES_WAKEUP,
											"btjoypad1");
							wl.setReferenceCounted(false);
							try {
								// Perform handshake
								InputStream inStream = socket.getInputStream();
								DataInputStream bReader = new DataInputStream(
										inStream);
								bReader.readLine();
								if (BTJoypadKeyboard.getInstance() == null) {
									InputMethodManager im = (InputMethodManager) getSystemService(InputMethodService.INPUT_METHOD_SERVICE);
									im.showInputMethodPicker();
								}
								OutputStream outStream = socket
										.getOutputStream();
								PrintWriter pWriter = new PrintWriter(
										new OutputStreamWriter(outStream));
								pWriter.write("btjoypadserver\r\n");
								pWriter.flush();
								Log.d("btjoypad", "client connected");

								wl.acquire(60000);
								lock.disableKeyguard();

								while (true) {
									byte command = bReader.readByte();
									Log.d("btjoypad", "read command: "
											+ command);

									lock.disableKeyguard();
									wl.acquire(60000);

									int action = -1;
									if (command == EXIT_CMD) {
										Log.d("btjoypad", "client EXIT_CMD");
										break;
									} else if (command == SHOW_CONFIG) {

										// KeyguardManager.KeyguardLock
										// newKeyguardLock =
										// ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).newKeyguardLock("HANDSFREE");
										// newKeyguardLock.disableKeyguard();
										// newKeyguardLock.reenableKeyguard();

										try {
											Intent i = new Intent(
													BTJoypadServer.this,
													BTJoypadPreferences.class);
											i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
											i.putExtra("unlock", "true");
											startActivity(i);
										} finally {
											// screenLock.release();
										}

										continue;
									} else if (command == KEYDOWN) {
										action = KeyEvent.ACTION_DOWN;
									} else if (command == KEYUP) {
										action = KeyEvent.ACTION_UP;
									} else {
										throw new Exception(
												"Unexpected command: "
														+ command);
									}
									int keyCode = bReader.read();
									Log.d("btjoypad", "read keycode: "
											+ keyCode);
									if (BTJoypadKeyboard.getInstance() == null) {
										InputMethodManager im = (InputMethodManager) getSystemService(InputMethodService.INPUT_METHOD_SERVICE);
										im.showInputMethodPicker();
									} else {
										BTJoypadKeyboard.getInstance().handle(
												keyCode, action);
									}
								}
							} catch (Exception e) {
								if (!stopped) {
									Log.d("btjoypad",
											"Could not handle socket: " + e, e);
								}
							} finally {
								lock.reenableKeyguard();
								wl.release();
								if (socket != null) {
									try {
										socket.close();
										socket = null;
									} catch (IOException e) {
										Log.e("btjoypad",
												"Could not close socket: " + e,
												e);
									}

									if (PreferenceManager
											.getDefaultSharedPreferences(
													BTJoypadServer.this)
											.getBoolean("showIMEOnClose", false)) {
										InputMethodManager im = (InputMethodManager) getSystemService(InputMethodService.INPUT_METHOD_SERVICE);
										im.showInputMethodPicker();
									}
								}
							}
						}
					}
				} catch (IOException e) {
					Log.e("btjoypad", "Could not start server socket: " + e, e);
				} finally {
					disconnect();
				}
			}
			unregisterReceiver(mReceiver);
		}
	}

	public boolean isStarted() {
		return started;
	}

	public void setStartedStatusListener(
			StartedStatusListener startedStatusListener) {
		this.startedStatusListener = startedStatusListener;
	}
}
