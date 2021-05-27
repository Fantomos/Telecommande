package com.tl.tlcommande;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
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
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class Joystick extends AppCompatActivity {
    private AlertDialog alert;
    private Bluetooth bluetooth;
    private Toast toast;
    private BluetoothDevice device;
    private TextView forceText;
    private TextView angleText;
    private TextView vitesseText;
    private TextView directionText;
    private TextView lockText;

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

    private JoystickView joystick;
    private EditText periodeInput;
    private int vitesse = Constantes.vitesseNulle;
    private int direction = Constantes.directionNulle;
    private long paquet;
    private int interval;
    private Handler handler;
    private boolean intervalChange = false;
    private int delaisRestart = 5000;

    private Switch demarrage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_joystick);

        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        bluetooth.setBluetoothCallback(bluetoothCallback);
        bluetooth.setDeviceCallback(deviceCallback);

        forceText = findViewById(R.id.force);
        angleText = findViewById(R.id.angle);
        vitesseText = findViewById(R.id.vitesse);
        directionText = findViewById(R.id.direction);
        periodeInput = findViewById(R.id.periodeInput);
        tensionTot = findViewById(R.id.tensionTot);
        tensionCell1 = findViewById(R.id.tensionCell1);
        tensionCell2 = findViewById(R.id.tensionCell2);
        tensionCell3 = findViewById(R.id.tensionCell3);
        batteryMeter = findViewById(R.id.batteryMeter);
        lockText = findViewById(R.id.lock);
        demarrage = findViewById(R.id.switch1);

        lockEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lock",false);
        tensionMax = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("tensionMax","15"));
        resolutionMax = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("resolutionMax","1024"));
        batterieTensionCrit = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("batterieTensionCrit","10.5"));
        celluleTensionCrit = Double.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("celluleTensionCrit","3.5"));
        delaisRestart = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("delaisRestart","5000"));
        autostop = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autostop",true);

        tensionTot.setText("??.?? V");
        tensionCell1.setText("Cellule  1  : ??.?? V");
        tensionCell2.setText("Cellule 2  : ??.?? V");
        tensionCell3.setText("Cellule 3  : ??.?? V");
        batteryMeter.setChargeLevel(null);
        batteryMeter.setCriticalChargeLevel((int)Math.round(((batterieTensionCrit-Constantes.batterieTensionMin)/(Constantes.batterieTensionMax-Constantes.batterieTensionMin))*100));




        joystick = (JoystickView) findViewById(R.id.joystickView);
        interval = Constantes.periodeTransmissionDefaut;
        periodeInput.setText(String.valueOf(interval));
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                 if(!joystick.isEnabled()){
                     vitesse = 0;
                    direction = Constantes.directionNulle;
                }else {
                     // Remet à l'échelle la valeur de la force pour la vitesse
                     // Remet à l'échelle la valeur de l'angle pour la direction
                     if (angle > 180) { // Si curseur dans la partie basse du joystick
                         direction = (int) Math.round((360 - angle) * Constantes.directionMax / 180.0);
                         vitesse = (int) Math.round(Constantes.vitesseNulle - strength * (Constantes.vitesseNulle) / 100.0) + 7;
                     } else { // Si curseur dans la partie haute du joystick
                         direction = (int) Math.round(angle * Constantes.directionMax / 180.0);
                         vitesse = (int) Math.round(strength * (Constantes.vitesseMax - Constantes.vitesseNulle) / 100.0 + Constantes.vitesseNulle) + 7;
                     }
                 }
                forceText.setText(String.format("(Force : %d %%)",strength));
                angleText.setText(String.format("(Angle : %d °)",angle));

            }
        },1);

    if(autostop) {
        joystick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == android.view.MotionEvent.ACTION_UP) {
                    demarrage.setChecked(false);
                }
                return false;
            }
        });
    }

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

        joystick.setEnabled(false);
        joystick.setBorderColor(ContextCompat.getColor(Joystick.this,R.color.rouge));
        joystick.setButtonColor(ContextCompat.getColor(Joystick.this,R.color.rouge));
        joystick.setAlpha(0.25f);
        lockText.setVisibility(View.VISIBLE);
        demarrage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    joystick.setBorderColor(ContextCompat.getColor(Joystick.this,R.color.blanc));
                    joystick.setButtonColor(ContextCompat.getColor(Joystick.this,R.color.blanc));
                    joystick.setAlpha(1f);
                    lockText.setVisibility(View.INVISIBLE);
                    joystick.setEnabled(true);
                    demarrage.setText("Start");
                    demarrage.setTextColor(ContextCompat.getColor(Joystick.this,R.color.vert));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        demarrage.setThumbTintList(ContextCompat.getColorStateList(Joystick.this,R.color.vert));
                    }
                    vitesse = 7;
                }
                else{
                    vitesse = 0;
                    joystick.resetButtonPosition();
                    joystick.setEnabled(false);
                    joystick.setBorderColor(ContextCompat.getColor(Joystick.this,R.color.rouge));
                    joystick.setButtonColor(ContextCompat.getColor(Joystick.this,R.color.rouge));
                    joystick.setAlpha(0.25f);
                    lockText.setVisibility(View.VISIBLE);
                    demarrage.setText("Stop");
                    demarrage.setTextColor(ContextCompat.getColor(Joystick.this,R.color.rouge));
                    demarrage.setClickable(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        demarrage.setThumbTintList(ContextCompat.getColorStateList(Joystick.this,R.color.rouge));
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
                    vitesseText.setText(String.format("Vitesse : %d",vitesse));
                    directionText.setText(String.format("Direction : %d",direction));
                    byte[] data = new byte[]{(byte) (vitesse | 0b10000000), (byte) (direction & 0b01111111)};
                    bluetooth.send(data);
                    paquet++;
                    handler.postDelayed(mStatusChecker, interval);
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
            toast = Toast.makeText(Joystick.this, "Aéroglisseur connecté", Toast.LENGTH_SHORT);
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
            toast = Toast.makeText(Joystick.this, "Aéroglisseur déconnecté", Toast.LENGTH_SHORT);
            toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
            ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
            toast.show();
            onBackPressed();
        }

        @Override
        public void onMessage(byte[] message) {

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
            toast = Toast.makeText(Joystick.this, "Impossible de se connecter, nouvel essai dans 3 secondes", Toast.LENGTH_SHORT);
            toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
            ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
            toast.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetooth.connectToDevice(device);
                }
            }, 3000);
        }
    };

    private void setLocked(boolean etat){
        if(lockEnabled && etat) {
            demarrage.setChecked(false);
            demarrage.setClickable(false);
        }else if(lockEnabled && !etat){
            demarrage.setClickable(true);
        }
    }

    private void backMenu(){
        if(toast != null){
            toast.cancel();
        }
        toast = Toast.makeText(Joystick.this, "Aucun appareil connecté", Toast.LENGTH_SHORT);
        toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
        ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
        toast.show();
        startActivity(new Intent(Joystick.this,MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(Joystick.this,MainActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

}
