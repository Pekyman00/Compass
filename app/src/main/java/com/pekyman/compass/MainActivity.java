package com.pekyman.compass;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    TextView azimuthTextView;
    ImageView compassFrontImageView;

    private Window mWindow;

    private SensorManager mSensorManager;
    private Sensor mMagnetometerSensor;
    private Sensor mAccelerometerSensor;
    private Sensor mGravitySensor;
    private Sensor mRotationVectorSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Screen is always going to be on
        mWindow = getWindow();
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        azimuthTextView = findViewById(R.id.text_azimuth);
        compassFrontImageView = findViewById(R.id.image_compass_front);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mMagnetometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mRotationVectorSensor != null) {
            mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

            operationMode = 2;
        } else if (mMagnetometerSensor != null && mGravitySensor != null) {
            mSensorManager.registerListener(this, mMagnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);

            operationMode = 1;
        }else if (mMagnetometerSensor != null && mAccelerometerSensor != null) {
            mSensorManager.registerListener(this, mMagnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

            operationMode = 0;
        }
    }

    ObjectAnimator rotationAnimation;

    private int operationMode = -1;

    private int azimuth;
    private int oldAzimuth;

    private float[] magneticField = new float[3];
    private float[] acceleration = new float[3];
    private float[] gravity = new float[3];
    private float[] rotationVector = new float[5];

    private float[] rotationMatrix = new float[9];
    private float[] inclinationMatrix = new float[9];

    private float[] orientation = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magneticField, 0, event.values.length);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            System.arraycopy(event.values, 0, gravity, 0, event.values.length);
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(event.values, 0, rotationVector, 0, event.values.length);
        }

        GeomagneticField geomagneticField = new GeomagneticField(rotationVector[0], rotationVector[1], rotationVector[2], System.currentTimeMillis());
        float value = geomagneticField.getDeclination();

        if (operationMode == 0 || operationMode == 1) {
            if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, (operationMode == 0) ? acceleration : gravity, magneticField)) {
                float azimuthRad = SensorManager.getOrientation(rotationMatrix, orientation)[0];
                double azimuthDeg = Math.toDegrees(azimuthRad);

                azimuth = ((int)azimuthDeg + 360) % 360;
            }
        } else if (operationMode == 2) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
            azimuth = (int) ((Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360)) % 360;
        }

        azimuthTextView.setText(azimuth + "°");

        float tempAzimuth;
        float tempCurrentAzimuth;

        if (Math.abs(azimuth - oldAzimuth) > 180) {
            if (oldAzimuth < azimuth) {
                tempCurrentAzimuth = oldAzimuth + 360;
                tempAzimuth = azimuth;
            } else {
                tempCurrentAzimuth = oldAzimuth;
                tempAzimuth = azimuth + 360;
            }
            rotationAnimation = ObjectAnimator.ofFloat(compassFrontImageView, "rotation", -tempCurrentAzimuth, -tempAzimuth);
        } else {
            rotationAnimation = ObjectAnimator.ofFloat(compassFrontImageView, "rotation", -oldAzimuth, -azimuth);
        }
        rotationAnimation.setDuration(250);
        rotationAnimation.start();

        oldAzimuth = azimuth;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.info) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Compass");
            alertDialogBuilder.setMessage("This is a compass application, made by Filip Perencevic.");
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}