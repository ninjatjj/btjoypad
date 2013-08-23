package com.ninjatjj.btjoypad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.ninjatjj.btjoypad.server.BTJoypadServer;

public class BTJoypadConnection {

	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;

	private static BTJoypadConnection instance;
	private DisconnectionHandler disconnectionHandler;
	public volatile boolean disconnected = true;

	public static synchronized BTJoypadConnection getInstance() {
		if (instance == null) {
			instance = new BTJoypadConnection();
		}
		return instance;
	}

	protected BTJoypadConnection() {
	}

	public synchronized void connect(final Handler handler, final String address) {
		if (connected()) {
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					handler.obtainMessage(BTJoypadClient.CONNECTING)
							.sendToTarget();
					BluetoothAdapter defaultAdapter = BluetoothAdapter
							.getDefaultAdapter();
					BluetoothDevice device = null;

					device = defaultAdapter.getRemoteDevice(address);

					// Two things are needed to make a connection:
					// A MAC address, which we got above.
					// A Service ID or UUID. In this case we are using the
					// UUID for SPP.
					btSocket = device
							.createRfcommSocketToServiceRecord(BTJoypadServer.MY_UUID_SECURE);

					// Discovery is resource intensive. Make sure it isn't
					// going on
					// when you attempt to connect and pass your message.
					defaultAdapter.cancelDiscovery();

					// Establish the connection. This will block until it
					// connects.
					btSocket.connect();
					Log.d("btjoypad", "recreating");
					// Create a data stream so we can talk to server.
					outStream = btSocket.getOutputStream();
					String message = "btjoypad\n";
					byte[] msgBuffer = message.getBytes();
					outStream.write(msgBuffer);
					outStream.flush();

					BufferedReader reader = new BufferedReader(
							new InputStreamReader(btSocket.getInputStream()));
					String readLine = reader.readLine();
					if (!readLine.equals("btjoypadserver")) {
						throw new IOException("did not complete handshake");
					} else {
						disconnected = false;

						disconnectionHandler = new DisconnectionHandler(
								handler, reader);
						disconnectionHandler.start();

						handler.obtainMessage(BTJoypadClient.CONNECTED)
								.sendToTarget();
					}
				} catch (IOException e) {
					Log.e("btjoypad", "Could not connect", e);
					handler.obtainMessage(BTJoypadClient.CANNOT_CONNECT, 0, 0,
							e.getMessage()).sendToTarget();
					disconnect(handler);
					// throw e;
				}
			}
		}).start();
	}

	public synchronized void disconnect(Handler handler) {

		disconnected = true;

		if (outStream != null) {
			try {
				outStream.flush();
			} catch (IOException e) {
				Log.d("btjoypad",
						"failed to flush output stream: " + e.getMessage()
								+ ".");
			}
			outStream = null;
		}

		if (btSocket != null) {
			try {
				btSocket.close();
			} catch (IOException e) {
				Log.d("btjoypad", "failed to close socket." + e.getMessage()
						+ ".");
			}
			btSocket = null;
		}

		if (disconnectionHandler != null) {
			try {
				disconnectionHandler.join();
			} catch (InterruptedException e) {
				Log.e("btjoypad", "Could not wait for disconnectionHandler", e);
			}
			disconnectionHandler = null;
		}
	}

	public boolean connected() {
		return btSocket != null;
	}

	public OutputStream getOutputStream() {
		return outStream;
	}

	private class DisconnectionHandler extends Thread {
		private BufferedReader reader;
		private Handler handler;

		public DisconnectionHandler(Handler handler, BufferedReader reader) {
			super("DisconnectionHandler");
			this.handler = handler;
			this.reader = reader;
		}

		@Override
		public void run() {
			try {
				reader.read();
			} catch (IOException e) {
				if (!disconnected) {
					handler.obtainMessage(BTJoypadClient.DISCONNECTED, 0, 0,
							e.getMessage()).sendToTarget();
					Log.d("btjoypad", "Server disconnected: " + e.getMessage());
				}
			}
		}
	}
}
