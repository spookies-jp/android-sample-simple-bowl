package jp.co.spookies.android.simplebowl;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;

public class SimpleBowlActivity extends Activity implements SensorEventListener {
	private SensorManager sensorManager;
	private BowlingView view;

	private float[] orientation = new float[3];
	private float[] geomagnetic = new float[3];
	private float[] gravity = new float[3];
	private float[] inR = new float[9];
	private float[] outR = new float[9];
	private float[] I = new float[9];

	public static final int STATE_INIT = 0;
	public static final int STATE_THROW = 1;
	public static final int STATE_ROLL = 2;
	public static final int STATE_RESULT = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = new BowlingView(this);
		setContentView(view);
	}

	@Override
	public void onResume() {
		super.onResume();

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		Sensor sensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (sensor != null) {
			// 加速度センサー登録
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_FASTEST);
		}
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (sensor != null) {
			// 磁気センサー登録
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_FASTEST);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_MAGNETIC_FIELD:
			for (int i = 0; i < 3; i++) {
				geomagnetic[i] = event.values[i] * 0.1f + geomagnetic[i] * 0.9f;
			}
			break;
		case Sensor.TYPE_ACCELEROMETER:
			for (int i = 0; i < 3; i++) {
				gravity[i] = event.values[i] * 0.1f + gravity[i] * 0.9f;
			}
			break;
		}

		// 傾きの値を取得する
		if (SensorManager.getRotationMatrix(inR, I, gravity, geomagnetic)) {
			SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X,
					SensorManager.AXIS_Y, outR);
			SensorManager.getOrientation(outR, orientation);
		}
		// 投げ初め
		if (view.getState() == BowlingView.STATE_INIT && gravity[1] < -13.0f) {
			view.doThrow();
			// 投げ終わり
		} else if (view.getState() == BowlingView.STATE_THROW
				&& orientation[1] < -0.1f) {
			float speed = (gravity[2] - SensorManager.GRAVITY_EARTH)
					* (gravity[2] - SensorManager.GRAVITY_EARTH) / 2;
			float curve = gravity[0];
			view.doRoll(speed, curve);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			// 初期化
			view.doInit();
			return true;
		}
		return super.onTouchEvent(event);
	}
}