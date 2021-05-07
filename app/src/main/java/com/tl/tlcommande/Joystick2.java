package com.tl.tlcommande;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import eo.view.batterymeter.BatteryMeter;
import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DeviceCallback;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Joystick2 extends AppCompatActivity {
    private AlertDialog alert;
    private Bluetooth bluetooth;
    private Toast toast;
    private BluetoothDevice device;
    private TextView forceText;
    private TextView angleText;
    private TextView vitesseText;
    private TextView directionText;
    private TextView paquetText;
    private EditText periodeInput;
    private int vitesse = Constantes.vitesseNulle;;
    private int direction = Constantes.directionNulle;
    private int interval;
    private JoystickView joystickVitesse;
    private JoystickView joystickDirection;
    private Handler handler;
    private boolean intervalChange = false;

    private TextView tensionTot;
    private TextView tensionCell1;
    private TextView tensionCell2;
    private TextView tensionCell3;
    private BatteryMeter batteryMeter;

    private double valueTensionTot;
    private double valueTensionCell1;
    private double valueTensionCell2;
    private double valueTensionCell3;

    private boolean locked = true;
    private boolean lockEnabled = false;
    private int tensionMax = 15;
    private int resolutionMax = 1024;
    private double batterieTensionCrit = 10.5;
    private double celluleTensionCrit = 3.5;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_joystick2);

        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        bluetooth.setBluetoothCallback(bluetoothCallback);
        bluetooth.setDeviceCallback(deviceCallback);


        periodeInput = findViewById(R.id.periodeInput);

        tensionTot = findViewById(R.id.tensionTot);
        tensionCell1 = findViewById(R.id.tensionCell1);
        tensionCell2 = findViewById(R.id.tensionCell2);
        tensionCell3 = findViewById(R.id.tensionCell3);
        batteryMeter = findViewById(R.id.batteryMeter);

        tensionTot.setText("??.?? V");
        tensionCell1.setText("Cellule  1  : ??.?? V");
        tensionCell2.setText("Cellule 2  : ??.?? V");
        tensionCell3.setText("Cellule 3  : ??.?? V");
        batteryMeter.setChargeLevel(null);
        batteryMeter.setCriticalChargeLevel((int)Math.round(((Constantes.batterieTensionCrit-Constantes.batterieTensionMin)/(Constantes.batterieTensionMax-Constantes.batterieTensionMin))*100));

        lockEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lock",false);
        tensionMax = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("tensionMax","15"));
        resolutionMax = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("resolutionMax","1024"));
        batterieTensionCrit = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("batterieTensionCrit","10.5"));
        celluleTensionCrit = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("celluleTensionCrit","3.5"));

        joystickVitesse = (JoystickView) findViewById(R.id.joystickSpeed);
        joystickDirection = (JoystickView) findViewById(R.id.joystickDirection);
        interval = Constantes.periodeTransmissionDefaut;
        periodeInput.setText(String.valueOf(interval));
        setLocked(true);
        joystickVitesse.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if(angle>180){ // Si curseur dans la partie basse du joystick
                    //vitesse = (int)Math.round(63.5 - strength*63.5/100);
                    vitesse = (int)Math.round(Constantes.vitesseNulle - strength*(Constantes.vitesseNulle)/100.0);
                }else{ // Si curseur dans la partie haute du joystick
                    vitesse = (int)Math.round(strength*(Constantes.vitesseMax - Constantes.vitesseNulle)/100.0 + Constantes.vitesseNulle);
                }
            }
        },1);
        joystickDirection.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if(angle == 0){
                    direction = (int)Math.round(Constantes.directionMax/2.0 - strength*Constantes.directionMax/200.0);
                }else{
                    direction = (int)Math.round(Constantes.directionMax/2.0 + strength*Constantes.directionMax/200.0);
                }
            }
        },1);

        periodeInput.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    intervalChange = true;
                }
                return false;
            }
        });

        handler = new Handler();

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
                if(!locked | !lockEnabled) {
                    byte[] data = new byte[]{(byte) (vitesse | 0b10000000), (byte) (direction & 0b01111111)};
                    bluetooth.send(data);
                    handler.postDelayed(mStatusChecker, interval);
                }
            }
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device = extras.getParcelable("device");
            bluetooth.connectToDevice(device);
        } else {
            backMenu();
        }

        alert = new AlertDialog.Builder(this,R.style.DialogTheme)
                .setTitle("Bluetooth")
                .setMessage("Connexion en cours ...")
                .setIcon(R.drawable.bluetooth)
                .setCancelable(false)
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK &&
                                event.getAction() == KeyEvent.ACTION_UP &&
                                !event.isCanceled()) {
                            onBackPressed();
                            return true;
                        }
                        return false;
                    }
                })
                .show();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bluetooth.isConnected()) {
            bluetooth.disconnect();
        }
        bluetooth.removeDeviceCallback();
        bluetooth.removeBluetoothCallback();
        bluetooth.onStop();
        handler.removeCallbacks(mStatusChecker);
    }

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override public void onBluetoothTurningOn() {}
        @Override public void onBluetoothTurningOff() {
            backMenu();
        }
        @Override public void onBluetoothOff() {backMenu();}
        @Override public void onUserDeniedActivation() {}
        @Override public void onBluetoothOn() {}
    };


    private DeviceCallback deviceCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            alert.cancel();
            if(toast != null){
                toast.cancel();
            }
            toast = Toast.makeText(Joystick2.this, "Aéroglisseur connecté", Toast.LENGTH_SHORT);
            toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
            ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
            toast.show();
            mStatusChecker.run();
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            if(toast != null){
                toast.cancel();
            }
            toast = Toast.makeText(Joystick2.this, "Aéroglisseur déconnecté", Toast.LENGTH_SHORT);
            toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
            ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
            toast.show();
            onBackPressed();
        }

        @Override
        public void onMessage(byte[] message) {
            Log.d("Kada", String.valueOf(message.length));
            if (message.length == 6) {
                valueTensionCell1 = (((message[1] & 0xFF) << 8 | (message[0] & 0xFF))) * (double) tensionMax / (double) resolutionMax;
                valueTensionCell2 = (((message[3] & 0xFF) << 8 | (message[2] & 0xFF))) * (double) tensionMax / (double) resolutionMax;
                valueTensionCell3 = (((message[5] & 0xFF) << 8 | (message[4] & 0xFF))) * (double) tensionMax / (double) resolutionMax;
                valueTensionTot = valueTensionCell1 + valueTensionCell2 + valueTensionCell3;
                Log.d("data", String.valueOf(valueTensionTot));
                batteryMeter.setChargeLevel((int) Math.round(((valueTensionTot - Constantes.batterieTensionMin) / (Constantes.batterieTensionMax - Constantes.batterieTensionMin)) * 100));
                tensionTot.setText(String.format("%.2f V", valueTensionTot));
                tensionCell1.setText(String.format("Cellule  1 : %.2f V", valueTensionCell1));
                tensionCell2.setText(String.format("Cellule 2 : %.2f V", valueTensionCell2));
                tensionCell3.setText(String.format("Cellule 3 : %.2f V", valueTensionCell3));

                if (valueTensionTot <= batterieTensionCrit) {
                    tensionTot.setTextColor(ContextCompat.getColor(getApplication(), R.color.rouge));
                } else {
                    tensionTot.setTextColor(ContextCompat.getColor(getApplication(), R.color.vert));
                }
                if (valueTensionCell1 <= celluleTensionCrit) {
                    tensionCell1.setTextColor(ContextCompat.getColor(getApplication(), R.color.rouge));
                } else {
                    tensionCell1.setTextColor(ContextCompat.getColor(getApplication(), R.color.vert));
                }
                if (valueTensionCell2 <= celluleTensionCrit) {
                    tensionCell2.setTextColor(ContextCompat.getColor(getApplication(), R.color.rouge));
                } else {
                    tensionCell2.setTextColor(ContextCompat.getColor(getApplication(), R.color.vert));
                }
                if (valueTensionCell3 <= celluleTensionCrit) {
                    tensionCell3.setTextColor(ContextCompat.getColor(getApplication(), R.color.rouge));
                } else {
                    tensionCell3.setTextColor(ContextCompat.getColor(getApplication(), R.color.vert));
                }

                if (valueTensionCell3 <= celluleTensionCrit || valueTensionCell2 <= celluleTensionCrit || valueTensionCell1 <= celluleTensionCrit || valueTensionTot <= batterieTensionCrit) {
                    setLocked(true);
                } else {
                    setLocked(false);
                }
            }

        }

        @Override
        public void onError(int errorCode) {

        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            if(toast != null){
                toast.cancel();
            }
            toast = Toast.makeText(Joystick2.this, "Impossible de se connecter, nouvel essai dans 3 secondes", Toast.LENGTH_SHORT);
            toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
            ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
            toast.show();            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetooth.connectToDevice(device);
                }
            }, 3000);
        }
    };

    private void setLocked(boolean etat){
        locked = etat;
        if(lockEnabled) {
            joystickDirection.setEnabled(!etat);
            joystickVitesse.setEnabled(!etat);
            if(locked){
                joystickDirection.setBorderColor(ContextCompat.getColor(this,R.color.rouge));
                joystickDirection.setButtonColor(ContextCompat.getColor(this,R.color.rouge));
                joystickDirection.setAlpha(0.25f);
                joystickVitesse.setBorderColor(ContextCompat.getColor(this,R.color.rouge));
                joystickVitesse.setButtonColor(ContextCompat.getColor(this,R.color.rouge));
                joystickVitesse.setAlpha(0.25f);
            }else{
                joystickDirection.setBorderColor(ContextCompat.getColor(this,R.color.blanc));
                joystickDirection.setButtonColor(ContextCompat.getColor(this,R.color.blanc));
                joystickDirection.setAlpha(1f);
                joystickVitesse.setBorderColor(ContextCompat.getColor(this,R.color.blanc));
                joystickVitesse.setButtonColor(ContextCompat.getColor(this,R.color.blanc));
                joystickVitesse.setAlpha(1f);
            }
        }

    }

    private void backMenu(){
        if(toast != null){
            toast.cancel();
        }
        toast = Toast.makeText(Joystick2.this, "Aucun appareil connecté", Toast.LENGTH_SHORT);
        toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
        ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
        toast.show();
        startActivity(new Intent(Joystick2.this,MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed()
    {
        alert.cancel();
        Intent intent = new Intent(Joystick2.this,MainActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

}
