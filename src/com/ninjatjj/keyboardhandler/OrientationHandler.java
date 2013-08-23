package com.ninjatjj.keyboardhandler;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.view.KeyEvent;

public class OrientationHandler implements SensorEventListener {
	private final float DEADZONE = (float) 4.0;
	private final float UPDOWNVIEW_ANGLE = (float) 30.0;
	private static boolean orientationButtonLeft = false;
	private static boolean orientationButtonRight = false;
	private static boolean orientationButtonUp = false;
	private static boolean orientationButtonDown = false;
	private static boolean orientationButtonRollLeft = false;
	private static boolean orientationButtonRollRight = false;
	private Sensor orientationSensor;
	private KeyHandler keyHandler;

	public OrientationHandler(KeyHandler handler, Sensor sensor) {
		keyHandler = handler;
		orientationSensor = sensor;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		if (sensor == orientationSensor) {
			boolean press_ORIENTATION_BUTTON_LEFT = false;
			boolean press_ORIENTATION_BUTTON_RIGHT = false;
			boolean press_ORIENTATION_BUTTON_UP = false;
			boolean press_ORIENTATION_BUTTON_DOWN = false;
			boolean press_ORIENTATION_BUTTON_ROLL_LEFT = false;
			boolean press_ORIENTATION_BUTTON_ROLL_RIGHT = false;

			float azimuth_x = event.values[0];
			float pitch_y = event.values[1];
			float roll_z = event.values[2];
			Log.v("btjoypad", "ORIENTATION_BUTTON_RT: " + azimuth_x);
			Log.v("btjoypad", "ORIENTATION_BUTTON_LR: " + pitch_y);
			Log.v("btjoypad", "ORIENTATION_BUTTON_UD: " + roll_z);

			if (azimuth_x <= 360f - DEADZONE && azimuth_x >= 180f) {
				// Log.d("btjoypad", "ORIENTATION_BUTTON_ROLL_LEFT");
				press_ORIENTATION_BUTTON_ROLL_LEFT = true;
				keyHandler.handle(KeyEvent.ACTION_DOWN,
						"ORIENTATION_BUTTON_ROLL_LEFT");
				orientationButtonRollLeft = true;
			} else if (azimuth_x >= 0f + DEADZONE && azimuth_x <= 180f) {
				// Log.d("btjoypad", "ORIENTATION_BUTTON_ROLL_RIGHT");
				press_ORIENTATION_BUTTON_ROLL_RIGHT = true;
				keyHandler.handle(KeyEvent.ACTION_DOWN,
						"ORIENTATION_BUTTON_ROLL_RIGHT");
				orientationButtonRollRight = true;
			} else {
				// Log.d("btjoypad", "ORIENTATION_BUTTON_ROLL deadzone");
			}

			if (pitch_y >= 0f + DEADZONE) {// 270f && x < 360f) {
				// Log.d("btjoypad", "ORIENTATION_BUTTON_LEFT");
				press_ORIENTATION_BUTTON_LEFT = true;
				keyHandler.handle(KeyEvent.ACTION_DOWN,
						"ORIENTATION_BUTTON_LEFT");
				orientationButtonLeft = true;
			} else if (pitch_y < 0f - DEADZONE) { // x >= 180f && x <= 270f
				// Log.d("btjoypad", "ORIENTATION_BUTTON_RIGHT");
				press_ORIENTATION_BUTTON_RIGHT = true;
				keyHandler.handle(KeyEvent.ACTION_DOWN,
						"ORIENTATION_BUTTON_RIGHT");
				orientationButtonRight = true;
			} else {
				// Log.d("btjoypad", "ORIENTATION_BUTTON_LR DEADZONE");
			}

			if (roll_z <= UPDOWNVIEW_ANGLE - DEADZONE) {
				// Log.d("btjoypad", "ORIENTATION_BUTTON_UP");
				press_ORIENTATION_BUTTON_UP = true;
				keyHandler
						.handle(KeyEvent.ACTION_DOWN, "ORIENTATION_BUTTON_UP");
				orientationButtonUp = true;
			} else if (roll_z > UPDOWNVIEW_ANGLE + DEADZONE) {
				// Log.d("btjoypad", "ORIENTATION_BUTTON_DOWN");
				press_ORIENTATION_BUTTON_DOWN = true;
				keyHandler.handle(KeyEvent.ACTION_DOWN,
						"ORIENTATION_BUTTON_DOWN");
				orientationButtonDown = true;
			} else {
				// Log.d("btjoypad", "ORIENTATION_BUTTON_UPDOWN deadzone");
			}

			if (orientationButtonLeft && !press_ORIENTATION_BUTTON_LEFT) {
				keyHandler
						.handle(KeyEvent.ACTION_UP, "ORIENTATION_BUTTON_LEFT");
				orientationButtonLeft = false;
			}
			if (orientationButtonRight && !press_ORIENTATION_BUTTON_RIGHT) {
				keyHandler.handle(KeyEvent.ACTION_UP,
						"ORIENTATION_BUTTON_RIGHT");
				orientationButtonRight = false;
			}
			if (orientationButtonUp && !press_ORIENTATION_BUTTON_UP) {
				keyHandler.handle(KeyEvent.ACTION_UP, "ORIENTATION_BUTTON_UP");
				orientationButtonUp = false;
			}
			if (orientationButtonDown && !press_ORIENTATION_BUTTON_DOWN) {
				keyHandler
						.handle(KeyEvent.ACTION_UP, "ORIENTATION_BUTTON_DOWN");
				orientationButtonDown = false;
			}
			if (orientationButtonRollLeft
					&& !press_ORIENTATION_BUTTON_ROLL_LEFT) {
				keyHandler.handle(KeyEvent.ACTION_UP,
						"ORIENTATION_BUTTON_ROLL_LEFT");
				orientationButtonRollLeft = false;
			}
			if (orientationButtonRollRight
					&& !press_ORIENTATION_BUTTON_ROLL_RIGHT) {
				keyHandler.handle(KeyEvent.ACTION_UP,
						"ORIENTATION_BUTTON_ROLL_RIGHT");
				orientationButtonRollRight = false;
			}
		}
	}
}
