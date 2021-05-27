package com.tl.tlcommande;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import eo.view.batterymeter.BatteryMeter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.aflak.bluetooth.Bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pwittchen.reactivesensors.library.ReactiveSensorEvent;
import com.github.pwittchen.reactivesensors.library.ReactiveSensors;
import com.github.pwittchen.reactivesensors.library.SensorNotFoundException;

import java.util.Locale;

public class Gyroscop extends AppCompatActivity {

    private AlertDialog alert;
    private Bluetooth bluetooth;
    private Toast toast;
    private BluetoothDevice device;
    private TextView lockText;

    protected String sensorName;
    private Disposable subscription;
    private TextView tensionTot;
    private TextView tensionCell1;
    private TextView tensionCell2;
    private TextView tensionCell3;
    private BatteryMeter batteryMeter;

    private double valueTensionTot = 0;
    private double valueTensionCell1 = 0;
    private double valueTensionCell2 = 0;
    private double valueTensionCell3 = 0;
    private boolean lockEnabled = false;
    private boolean autostop = true;
    private int tensionMax = 15;
    private int resolutionMax = 1024;
    private double batterieTensionCrit = 10.5;
    private double celluleTensionCrit = 3.5;
    private EditText periodeInput;
    private int vitesse = Constantes.vitesseNulle;
    private int direction = Constantes.directionNulle;
    private long paquet;
    private int interval;
    private Handler handler;
    private boolean intervalChange = false;
    private int delaisRestart = 5000;
    private Switch demarrage;
    private boolean pressed = false;

    private TextView tvSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_gyroscop);

        periodeInput = findViewById(R.id.periodeInput);
        tensionTot = findViewById(R.id.tensionTot);
        tensionCell1 = findViewById(R.id.tensionCell1);
        tensionCell2 = findViewById(R.id.tensionCell2);
        tensionCell3 = findViewById(R.id.tensionCell3);
        batteryMeter = findViewById(R.id.batteryMeter);
        lockText = findViewById(R.id.lock);
        demarrage = findViewById(R.id.switch1);

        tensionTot.setText("??.?? V");
        tensionCell1.setText("Cellule  1  : ??.?? V");
        tensionCell2.setText("Cellule 2  : ??.?? V");
        tensionCell3.setText("Cellule 3  : ??.?? V");
        batteryMeter.setChargeLevel(null);
        batteryMeter.setCriticalChargeLevel((int)Math.round(((batterieTensionCrit-Constantes.batterieTensionMin)/(Constantes.batterieTensionMax-Constantes.batterieTensionMin))*100));

        handler = new Handler();
        demarrage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    demarrage.setText("Start");
                    demarrage.setTextColor(ContextCompat.getColor(Gyroscop.this,R.color.vert));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        demarrage.setThumbTintList(ContextCompat.getColorStateList(Gyroscop.this,R.color.vert));
                    }
                    vitesse = 7;
                }
                else{
                    vitesse = 0;
                    lockText.setVisibility(View.VISIBLE);
                    demarrage.setText("Stop");
                    demarrage.setTextColor(ContextCompat.getColor(Gyroscop.this,R.color.rouge));
                    demarrage.setClickable(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        demarrage.setThumbTintList(ContextCompat.getColorStateList(Gyroscop.this,R.color.rouge));
                    }
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            demarrage.setClickable(true);
                        }
                    }, delaisRestart);
                }
            }
        });

        tvSensor = (TextView) findViewById(R.id.sensor);





    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if(intervalChange) {
                    interval = Integer.parseInt(periodeInput.getText().toString());
                    intervalChange = false;
                }
            } finally {
                byte[] data = new byte[]{(byte) (vitesse | 0b10000000), (byte) (direction & 0b01111111)};
                bluetooth.send(data);
                paquet++;
                handler.postDelayed(mStatusChecker, interval);
            }
        }
    };

    @Override protected void onResume() {
        final ReactiveSensors reactiveSensors = new ReactiveSensors(getApplicationContext());
        subscription = reactiveSensors.observeSensor(Sensor.TYPE_ROTATION_VECTOR)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(ReactiveSensorEvent::sensorChanged)
                .subscribe(event -> {
                    float x = event.sensorValues()[0];
                    float y = event.sensorValues()[1];
                    float z = event.sensorValues()[2];
                    final String format = "%s readings:\n x = %f\n y = %f\n z = %f";
                    String message = String.format(Locale.getDefault(), format, event.sensorName(), x, y, z);
                    direction = (int)(Constantes.directionMax*z);
                    tvSensor.setText(message + "Direction : " + direction);
                }, throwable -> {
                    if (throwable instanceof SensorNotFoundException) {
                        tvSensor.setText("Sorry, your device doesn't have required sensor.");
                    }
                });
        super.onResume();
    }

    @Override protected void onPause() {
        super.onPause();
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(mStatusChecker);
    }

    private void backMenu(){
        if(toast != null){
            toast.cancel();
        }
        toast = Toast.makeText(Gyroscop.this, "Aucun appareil connecté", Toast.LENGTH_SHORT);
        toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
        ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
        toast.show();
        startActivity(new Intent(Gyroscop.this,MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(Gyroscop.this,MainActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }
}