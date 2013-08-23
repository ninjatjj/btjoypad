package com.ninjatjj.keyboardhandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

public class KeyHandler implements Runnable {

	private static final Integer KEYCODE_BUTTON_1 = 188;
	private static final Integer KEYCODE_BUTTON_10 = 197;
	private static final Integer KEYCODE_BUTTON_11 = 198;
	private static final Integer KEYCODE_BUTTON_12 = 199;
	private static final Integer KEYCODE_BUTTON_13 = 200;
	private static final Integer KEYCODE_BUTTON_14 = 201;
	private static final Integer KEYCODE_BUTTON_15 = 202;
	private static final Integer KEYCODE_BUTTON_16 = 203;
	private static final Integer KEYCODE_BUTTON_2 = 189;
	private static final Integer KEYCODE_BUTTON_3 = 190;
	private static final Integer KEYCODE_BUTTON_4 = 191;
	private static final Integer KEYCODE_BUTTON_5 = 192;
	private static final Integer KEYCODE_BUTTON_6 = 193;
	private static final Integer KEYCODE_BUTTON_7 = 194;
	private static final Integer KEYCODE_BUTTON_8 = 195;
	private static final Integer KEYCODE_BUTTON_9 = 196;

	public static final Integer ANALOG_LEFT_UP = 210;
	public static final Integer ANALOG_LEFT_DOWN = 211;
	public static final Integer ANALOG_LEFT_LEFT = 212;
	public static final Integer ANALOG_LEFT_RIGHT = 213;
	public static final Integer ANALOG_RIGHT_UP = 214;
	public static final Integer ANALOG_RIGHT_DOWN = 215;
	public static final Integer ANALOG_RIGHT_LEFT = 216;
	public static final Integer ANALOG_RIGHT_RIGHT = 217;
	public static final Integer ANALOG_MID = 218;

	private static List<String> dontSendUnmapped = Arrays.asList(new String[] {
			"ANALOG_LEFT_UP", "ANALOG_LEFT_DOWN", "ANALOG_LEFT_LEFT",
			"ANALOG_LEFT_RIGHT", "ANALOG_RIGHT_UP", "ANALOG_RIGHT_DOWN",
			"ANALOG_RIGHT_LEFT", "ANALOG_RIGHT_RIGHT", "ANALOG_MID" });

	private volatile boolean autofireStop;
	private Thread autofire = new Thread(this, "Autofire");
	private volatile List<Integer> autofireEnabled = new ArrayList<Integer>();

	private List<String> orientationMappingButtons = Arrays
			.asList(new String[] { "ORIENTATION_BUTTON_LEFT",
					"ORIENTATION_BUTTON_RIGHT", "ORIENTATION_BUTTON_UP",
					"ORIENTATION_BUTTON_DOWN", "ORIENTATION_BUTTON_ROLL_LEFT",
					"ORIENTATION_BUTTON_ROLL_RIGHT" });

	private InputMethodService ims;
	// This must be a reference as the preferences listener is a weak reference
	private OnSharedPreferenceChangeListener listener;
	private boolean shiftOn;

	static Map<String, Integer> keyCodesByKey = new HashMap<String, Integer>();
	static SparseArray<String> keyCodesByValue = new SparseArray<String>();

	public static String getKeyCodesByValue(int value) {
		return keyCodesByValue.get(value);
	}

	static {
		keyCodesByKey.put("KEYCODE_BACK", KeyEvent.KEYCODE_BACK);
		keyCodesByKey.put("KEYCODE_BUTTON_1", KEYCODE_BUTTON_1);
		keyCodesByKey.put("KEYCODE_BUTTON_10", KEYCODE_BUTTON_10);
		keyCodesByKey.put("KEYCODE_BUTTON_11", KEYCODE_BUTTON_11);
		keyCodesByKey.put("KEYCODE_BUTTON_12", KEYCODE_BUTTON_12);
		keyCodesByKey.put("KEYCODE_BUTTON_13", KEYCODE_BUTTON_13);
		keyCodesByKey.put("KEYCODE_BUTTON_14", KEYCODE_BUTTON_14);
		keyCodesByKey.put("KEYCODE_BUTTON_15", KEYCODE_BUTTON_15);
		keyCodesByKey.put("KEYCODE_BUTTON_16", KEYCODE_BUTTON_16);
		keyCodesByKey.put("KEYCODE_BUTTON_2", KEYCODE_BUTTON_2);
		keyCodesByKey.put("KEYCODE_BUTTON_3", KEYCODE_BUTTON_3);
		keyCodesByKey.put("KEYCODE_BUTTON_4", KEYCODE_BUTTON_4);
		keyCodesByKey.put("KEYCODE_BUTTON_5", KEYCODE_BUTTON_5);
		keyCodesByKey.put("KEYCODE_BUTTON_6", KEYCODE_BUTTON_6);
		keyCodesByKey.put("KEYCODE_BUTTON_7", KEYCODE_BUTTON_7);
		keyCodesByKey.put("KEYCODE_BUTTON_8", KEYCODE_BUTTON_8);
		keyCodesByKey.put("KEYCODE_BUTTON_9", KEYCODE_BUTTON_9);
		keyCodesByKey.put("KEYCODE_BUTTON_A", KeyEvent.KEYCODE_BUTTON_A);
		keyCodesByKey.put("KEYCODE_BUTTON_B", KeyEvent.KEYCODE_BUTTON_B);
		keyCodesByKey.put("KEYCODE_BUTTON_C", KeyEvent.KEYCODE_BUTTON_C);
		keyCodesByKey.put("KEYCODE_BUTTON_L1", KeyEvent.KEYCODE_BUTTON_L1);
		keyCodesByKey.put("KEYCODE_BUTTON_L2", KeyEvent.KEYCODE_BUTTON_L2);
		keyCodesByKey.put("KEYCODE_BUTTON_MODE", KeyEvent.KEYCODE_BUTTON_MODE);
		keyCodesByKey.put("KEYCODE_BUTTON_R1", KeyEvent.KEYCODE_BUTTON_R1);
		keyCodesByKey.put("KEYCODE_BUTTON_R2", KeyEvent.KEYCODE_BUTTON_R2);
		keyCodesByKey.put("KEYCODE_BUTTON_SELECT",
				KeyEvent.KEYCODE_BUTTON_SELECT);
		keyCodesByKey
				.put("KEYCODE_BUTTON_START", KeyEvent.KEYCODE_BUTTON_START);
		keyCodesByKey.put("KEYCODE_BUTTON_THUMBL",
				KeyEvent.KEYCODE_BUTTON_THUMBL);
		keyCodesByKey.put("KEYCODE_BUTTON_THUMBR",
				KeyEvent.KEYCODE_BUTTON_THUMBR);
		keyCodesByKey.put("KEYCODE_BUTTON_X", KeyEvent.KEYCODE_BUTTON_X);
		keyCodesByKey.put("KEYCODE_BUTTON_Y", KeyEvent.KEYCODE_BUTTON_Y);
		keyCodesByKey.put("KEYCODE_BUTTON_Z", KeyEvent.KEYCODE_BUTTON_Z);
		keyCodesByKey.put("KEYCODE_CAMERA", KeyEvent.KEYCODE_CAMERA);
		keyCodesByKey.put("KEYCODE_DPAD_CENTER", KeyEvent.KEYCODE_DPAD_CENTER);
		keyCodesByKey.put("KEYCODE_DPAD_DOWN", KeyEvent.KEYCODE_DPAD_DOWN);
		keyCodesByKey.put("KEYCODE_DPAD_LEFT", KeyEvent.KEYCODE_DPAD_LEFT);
		keyCodesByKey.put("KEYCODE_DPAD_RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT);
		keyCodesByKey.put("KEYCODE_DPAD_UP", KeyEvent.KEYCODE_DPAD_UP);
		keyCodesByKey.put("KEYCODE_MENU", KeyEvent.KEYCODE_MENU);
		keyCodesByKey.put("KEYCODE_SPACE", KeyEvent.KEYCODE_SPACE);
		keyCodesByKey.put("KEYCODE_SEARCH", KeyEvent.KEYCODE_SEARCH);
		keyCodesByKey.put("KEYCODE_A", KeyEvent.KEYCODE_A);
		keyCodesByKey.put("KEYCODE_B", KeyEvent.KEYCODE_B);
		keyCodesByKey.put("KEYCODE_C", KeyEvent.KEYCODE_C);
		keyCodesByKey.put("KEYCODE_D", KeyEvent.KEYCODE_D);
		keyCodesByKey.put("KEYCODE_E", KeyEvent.KEYCODE_E);
		keyCodesByKey.put("KEYCODE_F", KeyEvent.KEYCODE_F);
		keyCodesByKey.put("KEYCODE_G", KeyEvent.KEYCODE_G);
		keyCodesByKey.put("KEYCODE_H", KeyEvent.KEYCODE_H);
		keyCodesByKey.put("KEYCODE_I", KeyEvent.KEYCODE_I);
		keyCodesByKey.put("KEYCODE_J", KeyEvent.KEYCODE_J);
		keyCodesByKey.put("KEYCODE_K", KeyEvent.KEYCODE_K);
		keyCodesByKey.put("KEYCODE_L", KeyEvent.KEYCODE_L);
		keyCodesByKey.put("KEYCODE_M", KeyEvent.KEYCODE_M);
		keyCodesByKey.put("KEYCODE_N", KeyEvent.KEYCODE_N);
		keyCodesByKey.put("KEYCODE_O", KeyEvent.KEYCODE_O);
		keyCodesByKey.put("KEYCODE_P", KeyEvent.KEYCODE_P);
		keyCodesByKey.put("KEYCODE_Q", KeyEvent.KEYCODE_Q);
		keyCodesByKey.put("KEYCODE_R", KeyEvent.KEYCODE_R);
		keyCodesByKey.put("KEYCODE_S", KeyEvent.KEYCODE_S);
		keyCodesByKey.put("KEYCODE_T", KeyEvent.KEYCODE_T);
		keyCodesByKey.put("KEYCODE_U", KeyEvent.KEYCODE_U);
		keyCodesByKey.put("KEYCODE_V", KeyEvent.KEYCODE_V);
		keyCodesByKey.put("KEYCODE_W", KeyEvent.KEYCODE_W);
		keyCodesByKey.put("KEYCODE_X", KeyEvent.KEYCODE_X);
		keyCodesByKey.put("KEYCODE_Y", KeyEvent.KEYCODE_Y);
		keyCodesByKey.put("KEYCODE_Z", KeyEvent.KEYCODE_Z);
		keyCodesByKey.put("KEYCODE_1", KeyEvent.KEYCODE_1);
		keyCodesByKey.put("KEYCODE_2", KeyEvent.KEYCODE_2);
		keyCodesByKey.put("KEYCODE_3", KeyEvent.KEYCODE_3);
		keyCodesByKey.put("KEYCODE_4", KeyEvent.KEYCODE_4);
		keyCodesByKey.put("KEYCODE_5", KeyEvent.KEYCODE_5);
		keyCodesByKey.put("KEYCODE_6", KeyEvent.KEYCODE_6);
		keyCodesByKey.put("KEYCODE_7", KeyEvent.KEYCODE_7);
		keyCodesByKey.put("KEYCODE_8", KeyEvent.KEYCODE_8);
		keyCodesByKey.put("KEYCODE_9", KeyEvent.KEYCODE_9);
		keyCodesByKey.put("KEYCODE_0", KeyEvent.KEYCODE_0);
		keyCodesByKey.put("KEYCODE_SHIFT_LEFT", KeyEvent.KEYCODE_SHIFT_LEFT);
		keyCodesByKey.put("KEYCODE_ENTER", KeyEvent.KEYCODE_ENTER);
		keyCodesByKey.put("KEYCODE_COMMA", KeyEvent.KEYCODE_COMMA);
		keyCodesByKey.put("KEYCODE_PERIOD", KeyEvent.KEYCODE_PERIOD);
		keyCodesByKey.put("KEYCODE_TAB", KeyEvent.KEYCODE_TAB);
		keyCodesByKey.put("KEYCODE_SLASH", KeyEvent.KEYCODE_SLASH);
		keyCodesByKey.put("KEYCODE_APOSTROPHE", KeyEvent.KEYCODE_APOSTROPHE);
		keyCodesByKey.put("KEYCODE_DEL", KeyEvent.KEYCODE_DEL);
		keyCodesByKey.put("KEYCODE_HOME", KeyEvent.KEYCODE_HOME);
		keyCodesByKey.put("KEYCODE_SEMICOLON", KeyEvent.KEYCODE_SEMICOLON);
		keyCodesByKey.put("KEYCODE_MINUS", KeyEvent.KEYCODE_MINUS);
		keyCodesByKey.put("KEYCODE_EQUALS", KeyEvent.KEYCODE_EQUALS);

		keyCodesByKey.put("ANALOG_LEFT_UP", ANALOG_LEFT_UP);
		keyCodesByKey.put("ANALOG_LEFT_DOWN", ANALOG_LEFT_DOWN);
		keyCodesByKey.put("ANALOG_LEFT_LEFT", ANALOG_LEFT_LEFT);
		keyCodesByKey.put("ANALOG_LEFT_RIGHT", ANALOG_LEFT_RIGHT);
		keyCodesByKey.put("ANALOG_RIGHT_UP", ANALOG_RIGHT_UP);
		keyCodesByKey.put("ANALOG_RIGHT_DOWN", ANALOG_RIGHT_DOWN);
		keyCodesByKey.put("ANALOG_RIGHT_LEFT", ANALOG_RIGHT_LEFT);
		keyCodesByKey.put("ANALOG_RIGHT_RIGHT", ANALOG_RIGHT_RIGHT);
		keyCodesByKey.put("ANALOG_MID", ANALOG_MID);

		keyCodesByValue.append(KeyEvent.KEYCODE_BACK, "KEYCODE_BACK");
		keyCodesByValue.append(KEYCODE_BUTTON_1, "KEYCODE_BUTTON_1");
		keyCodesByValue.append(KEYCODE_BUTTON_10, "KEYCODE_BUTTON_10");
		keyCodesByValue.append(KEYCODE_BUTTON_11, "KEYCODE_BUTTON_11");
		keyCodesByValue.append(KEYCODE_BUTTON_12, "KEYCODE_BUTTON_12");
		keyCodesByValue.append(KEYCODE_BUTTON_13, "KEYCODE_BUTTON_13");
		keyCodesByValue.append(KEYCODE_BUTTON_14, "KEYCODE_BUTTON_14");
		keyCodesByValue.append(KEYCODE_BUTTON_15, "KEYCODE_BUTTON_15");
		keyCodesByValue.append(KEYCODE_BUTTON_16, "KEYCODE_BUTTON_16");
		keyCodesByValue.append(KEYCODE_BUTTON_2, "KEYCODE_BUTTON_2");
		keyCodesByValue.append(KEYCODE_BUTTON_3, "KEYCODE_BUTTON_3");
		keyCodesByValue.append(KEYCODE_BUTTON_4, "KEYCODE_BUTTON_4");
		keyCodesByValue.append(KEYCODE_BUTTON_5, "KEYCODE_BUTTON_5");
		keyCodesByValue.append(KEYCODE_BUTTON_6, "KEYCODE_BUTTON_6");
		keyCodesByValue.append(KEYCODE_BUTTON_7, "KEYCODE_BUTTON_7");
		keyCodesByValue.append(KEYCODE_BUTTON_8, "KEYCODE_BUTTON_8");
		keyCodesByValue.append(KEYCODE_BUTTON_9, "KEYCODE_BUTTON_9");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_A, "KEYCODE_BUTTON_A");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_B, "KEYCODE_BUTTON_B");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_C, "KEYCODE_BUTTON_C");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_L1, "KEYCODE_BUTTON_L1");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_L2, "KEYCODE_BUTTON_L2");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_MODE,
				"KEYCODE_BUTTON_MODE");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_R1, "KEYCODE_BUTTON_R1");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_R2, "KEYCODE_BUTTON_R2");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_SELECT,
				"KEYCODE_BUTTON_SELECT");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_START,
				"KEYCODE_BUTTON_START");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_THUMBL,
				"KEYCODE_BUTTON_THUMBL");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_THUMBR,
				"KEYCODE_BUTTON_THUMBR");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_X, "KEYCODE_BUTTON_X");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_Y, "KEYCODE_BUTTON_Y");
		keyCodesByValue.append(KeyEvent.KEYCODE_BUTTON_Z, "KEYCODE_BUTTON_Z");
		keyCodesByValue.append(KeyEvent.KEYCODE_CAMERA, "KEYCODE_CAMERA");
		keyCodesByValue.append(KeyEvent.KEYCODE_DPAD_CENTER,
				"KEYCODE_DPAD_CENTER");
		keyCodesByValue.append(KeyEvent.KEYCODE_DPAD_DOWN, "KEYCODE_DPAD_DOWN");
		keyCodesByValue.append(KeyEvent.KEYCODE_DPAD_LEFT, "KEYCODE_DPAD_LEFT");
		keyCodesByValue.append(KeyEvent.KEYCODE_DPAD_RIGHT,
				"KEYCODE_DPAD_RIGHT");
		keyCodesByValue.append(KeyEvent.KEYCODE_DPAD_UP, "KEYCODE_DPAD_UP");
		keyCodesByValue.append(KeyEvent.KEYCODE_MENU, "KEYCODE_MENU");
		keyCodesByValue.append(KeyEvent.KEYCODE_SPACE, "KEYCODE_SPACE");
		keyCodesByValue.append(KeyEvent.KEYCODE_SEARCH, "KEYCODE_SEARCH");
		keyCodesByValue.append(KeyEvent.KEYCODE_A, "KEYCODE_A");
		keyCodesByValue.append(KeyEvent.KEYCODE_B, "KEYCODE_B");
		keyCodesByValue.append(KeyEvent.KEYCODE_C, "KEYCODE_C");
		keyCodesByValue.append(KeyEvent.KEYCODE_D, "KEYCODE_D");
		keyCodesByValue.append(KeyEvent.KEYCODE_E, "KEYCODE_E");
		keyCodesByValue.append(KeyEvent.KEYCODE_F, "KEYCODE_F");
		keyCodesByValue.append(KeyEvent.KEYCODE_G, "KEYCODE_G");
		keyCodesByValue.append(KeyEvent.KEYCODE_H, "KEYCODE_H");
		keyCodesByValue.append(KeyEvent.KEYCODE_I, "KEYCODE_I");
		keyCodesByValue.append(KeyEvent.KEYCODE_J, "KEYCODE_J");
		keyCodesByValue.append(KeyEvent.KEYCODE_K, "KEYCODE_K");
		keyCodesByValue.append(KeyEvent.KEYCODE_L, "KEYCODE_L");
		keyCodesByValue.append(KeyEvent.KEYCODE_M, "KEYCODE_M");
		keyCodesByValue.append(KeyEvent.KEYCODE_N, "KEYCODE_N");
		keyCodesByValue.append(KeyEvent.KEYCODE_O, "KEYCODE_O");
		keyCodesByValue.append(KeyEvent.KEYCODE_P, "KEYCODE_P");
		keyCodesByValue.append(KeyEvent.KEYCODE_Q, "KEYCODE_Q");
		keyCodesByValue.append(KeyEvent.KEYCODE_R, "KEYCODE_R");
		keyCodesByValue.append(KeyEvent.KEYCODE_S, "KEYCODE_S");
		keyCodesByValue.append(KeyEvent.KEYCODE_T, "KEYCODE_T");
		keyCodesByValue.append(KeyEvent.KEYCODE_U, "KEYCODE_U");
		keyCodesByValue.append(KeyEvent.KEYCODE_V, "KEYCODE_V");
		keyCodesByValue.append(KeyEvent.KEYCODE_W, "KEYCODE_W");
		keyCodesByValue.append(KeyEvent.KEYCODE_X, "KEYCODE_X");
		keyCodesByValue.append(KeyEvent.KEYCODE_Y, "KEYCODE_Y");
		keyCodesByValue.append(KeyEvent.KEYCODE_Z, "KEYCODE_Z");
		keyCodesByValue.append(KeyEvent.KEYCODE_1, "KEYCODE_1");
		keyCodesByValue.append(KeyEvent.KEYCODE_2, "KEYCODE_2");
		keyCodesByValue.append(KeyEvent.KEYCODE_3, "KEYCODE_3");
		keyCodesByValue.append(KeyEvent.KEYCODE_4, "KEYCODE_4");
		keyCodesByValue.append(KeyEvent.KEYCODE_5, "KEYCODE_5");
		keyCodesByValue.append(KeyEvent.KEYCODE_6, "KEYCODE_6");
		keyCodesByValue.append(KeyEvent.KEYCODE_7, "KEYCODE_7");
		keyCodesByValue.append(KeyEvent.KEYCODE_8, "KEYCODE_8");
		keyCodesByValue.append(KeyEvent.KEYCODE_9, "KEYCODE_9");
		keyCodesByValue.append(KeyEvent.KEYCODE_0, "KEYCODE_0");
		keyCodesByValue.append(KeyEvent.KEYCODE_SHIFT_LEFT,
				"KEYCODE_SHIFT_LEFT");
		keyCodesByValue.append(KeyEvent.KEYCODE_ENTER, "KEYCODE_ENTER");
		keyCodesByValue.append(KeyEvent.KEYCODE_COMMA, "KEYCODE_COMMA");
		keyCodesByValue.append(KeyEvent.KEYCODE_PERIOD, "KEYCODE_PERIOD");
		keyCodesByValue.append(KeyEvent.KEYCODE_TAB, "KEYCODE_TAB");
		keyCodesByValue.append(KeyEvent.KEYCODE_SLASH, "KEYCODE_SLASH");
		keyCodesByValue.append(KeyEvent.KEYCODE_APOSTROPHE,
				"KEYCODE_APOSTROPHE");
		keyCodesByValue.append(KeyEvent.KEYCODE_DEL, "KEYCODE_DEL");
		keyCodesByValue.append(KeyEvent.KEYCODE_HOME, "KEYCODE_HOME");
		keyCodesByValue.append(KeyEvent.KEYCODE_SEMICOLON, "KEYCODE_SEMICOLON");
		keyCodesByValue.append(KeyEvent.KEYCODE_MINUS, "KEYCODE_MINUS");
		keyCodesByValue.append(KeyEvent.KEYCODE_EQUALS, "KEYCODE_EQUALS");

		keyCodesByValue.put(ANALOG_LEFT_UP, "ANALOG_LEFT_UP");
		keyCodesByValue.put(ANALOG_LEFT_DOWN, "ANALOG_LEFT_DOWN");
		keyCodesByValue.put(ANALOG_LEFT_LEFT, "ANALOG_LEFT_LEFT");
		keyCodesByValue.put(ANALOG_LEFT_RIGHT, "ANALOG_LEFT_RIGHT");
		keyCodesByValue.put(ANALOG_RIGHT_UP, "ANALOG_RIGHT_UP");
		keyCodesByValue.put(ANALOG_RIGHT_DOWN, "ANALOG_RIGHT_DOWN");
		keyCodesByValue.put(ANALOG_RIGHT_LEFT, "ANALOG_RIGHT_LEFT");
		keyCodesByValue.put(ANALOG_RIGHT_RIGHT, "ANALOG_RIGHT_RIGHT");
		keyCodesByValue.put(ANALOG_MID, "ANALOG_MID");
	}

	public KeyHandler(final InputMethodService ims) {
		Log.d("btjoypad", "KeyHandler created");
		this.ims = ims;

		autofire.start();
	}

	public void shutdown() {
		synchronized (autofireEnabled) {
			autofireStop = true;
			autofireEnabled.clear();
			autofireEnabled.notify();
		}
	}

	public boolean hasOrientationMappings() {
		for (String orientationMappingButton : orientationMappingButtons) {
			Map<String, ?> all = PreferenceManager.getDefaultSharedPreferences(
					ims).getAll();
			for (Entry<String, ?> entry : all.entrySet()) {
				if (orientationMappingButton.equals(entry.getValue())
						&& entry.getKey().contains("mappingKey")) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean handle(int keyCode, int action) {
		String value = keyCodesByValue.get(keyCode);
		return handle(action, value);
	}

	public boolean handle(final int action, String value) {
		final SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(ims);
		String configurationNumber = defaultSharedPreferences.getString(
				"configuration", "1");

		InputConnection ic = ims.getCurrentInputConnection();
		boolean handled = false;
		if (ic != null) {

			List<Integer> keyCodesToProcess = new ArrayList<Integer>();
			if (PreferenceManager.getDefaultSharedPreferences(ims).getBoolean(
					"useMappings", true)
					&& value != null) {
				Map<String, ?> all = PreferenceManager
						.getDefaultSharedPreferences(ims).getAll();
				for (Entry<String, ?> entry : all.entrySet()) {
					if (value.equals(entry.getValue())
							&& entry.getKey().startsWith(
									"mappingKey" + configurationNumber)) {
						final String key = (String) all
								.get("mappingValue"
										+ configurationNumber
										+ entry.getKey().replace(
												"mappingKey"
														+ configurationNumber,
												""));
						final Integer keyCode = keyCodesByKey.get(key);
						keyCodesToProcess.add(keyCode);
					}
				}
			}
			if (keyCodesToProcess.size() == 0
					&& keyCodesByKey.containsKey(value)
					&& !dontSendUnmapped.contains(value)) {
				keyCodesToProcess.add(keyCodesByKey.get(value));
			}

			for (Integer keyCode : keyCodesToProcess) {
				boolean supportsAutofire = false;
				Map<String, ?> all = PreferenceManager
						.getDefaultSharedPreferences(ims).getAll();
				Iterator<String> iterator = all.keySet().iterator();
				while (iterator.hasNext()) {
					String mappingKey = iterator.next();
					if (mappingKey.startsWith("autofireKey"
							+ configurationNumber)) {
						Integer integer = keyCodesByKey
								.get(all.get(mappingKey));
						if (keyCode.equals(integer)) {
							supportsAutofire = true;
							break;
						}
					}
				}
				if (supportsAutofire && action == KeyEvent.ACTION_DOWN) {
					synchronized (autofireEnabled) {
						Log.d("btjoypad", "keydown: " + keyCode);
						synchronized (autofireEnabled) {
							if (!autofireEnabled.contains(keyCode)) {
								autofireEnabled.add(keyCode);
								autofireEnabled.notify();
							}
						}
					}
					handled = true;
				} else if (supportsAutofire && action == KeyEvent.ACTION_UP) {
					// TODO Its possible that this setting could be changed
					// mid key
					// up/down...
					Log.d("btjoypad", "keyup: " + keyCode);
					synchronized (autofireEnabled) {
						if (autofireEnabled.contains(keyCode)) {
							autofireEnabled.remove(keyCode);
						}
					}
					handled = true;
				} else {
					if (keyCode == KeyEvent.KEYCODE_HOME) {
						if (action == 1) {
							Intent startMain = new Intent(Intent.ACTION_MAIN);
							startMain.addCategory(Intent.CATEGORY_HOME);
							startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							ims.startActivity(startMain);
						}
					} else {
						ic.sendKeyEvent(decorateKeyEvent(action, keyCode));
					}
					handled = true;
				}
			}
		}
		return handled;
	}

	@Override
	public void run() {
		while (!autofireStop) {
			synchronized (autofireEnabled) {
				while (autofireEnabled.isEmpty()) {
					try {
						Log.d("btjoypad", "waiting");
						autofireEnabled.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if (!autofireStop) {
				Log.d("btjoypad", "attempting fire");
				InputConnection ic = ims.getCurrentInputConnection();
				try {
					if (ic != null) {
						List<Integer> toClear = new ArrayList<Integer>();
						for (Integer keyCode : autofireEnabled) {
							Log.d("btjoypad", "autofire50: " + keyCode);
							ic.sendKeyEvent(decorateKeyEvent(
									KeyEvent.ACTION_DOWN, keyCode));
							toClear.add(keyCode);
						}

						Thread.sleep(50); // 3 frames on ST
						for (Integer keyCode : toClear) {
							Log.d("btjoypad", "autofird50: " + keyCode);
							ic.sendKeyEvent(decorateKeyEvent(
									KeyEvent.ACTION_UP, keyCode));
						}
					} else {
						Log.d("btjoypad", "no ic");
					}
					Thread.sleep(50); // 3 frames on ST
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public KeyEvent decorateKeyEvent(int action, int keyCode) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return new KeyEvent(SystemClock.uptimeMillis(),
					SystemClock.uptimeMillis(), action, keyCode, 0,
					KeyEvent.META_ALT_ON);
		} else {

			if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) {
				if (action == KeyEvent.ACTION_DOWN) {
					shiftOn = true;
				} else {
					shiftOn = false;
				}
			}
			return new KeyEvent(SystemClock.uptimeMillis(),
					SystemClock.uptimeMillis(), action, keyCode, 0,
					shiftOn ? KeyEvent.META_SHIFT_ON : 0);
		}
	}
}
