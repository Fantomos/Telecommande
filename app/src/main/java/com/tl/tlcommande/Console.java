package com.tl.tlcommande;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DeviceCallback;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Console extends AppCompatActivity {

    private EditText champVitesse;
    private EditText champDirection;
    private ConstraintLayout boutonEnvoyer;
    private RadioGroup radioGroup;
    private AlertDialog alert;
    private Bluetooth bluetooth;
    private Toast toast;
    private BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_console);


        champVitesse = findViewById(R.id.vitesseEnvoie);
        champDirection = findViewById(R.id.directionEnvoie);
        boutonEnvoyer = findViewById(R.id.envoyerLayout);
        radioGroup = findViewById(R.id.radioGroup);

        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        bluetooth.setBluetoothCallback(bluetoothCallback);
        bluetooth.setDeviceCallback(deviceCallback);

        boutonEnvoyer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(radioGroup.getCheckedRadioButtonId()){
                    case R.id.radioBIN:
                        if(champVitesse.getText().toString().matches("([01]*)") && champDirection.getText().toString().matches("([01]*)")){
                            int decVitesse = Integer.parseInt(champVitesse.getText().toString(),2);
                            int decDirection = Integer.parseInt(champDirection.getText().toString(),2);
                            if ((decVitesse > -1 && decVitesse <= Constantes.vitesseMax) && (decDirection> -1 && decDirection <= Constantes.directionMax)) {
                                byte[] data = new byte[]{(byte) (decVitesse | 0b10000000),(byte) (decDirection & 0b01111111)};
                                bluetooth.send(data);
                                champVitesse.setText("");
                                champDirection.setText("");
                            } else {
                                if (toast != null) {
                                    toast.cancel();
                                }
                                toast = Toast.makeText(Console.this, "La valeur doit être comprise entre 0 et 127", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } else{
                            if (toast != null) {
                                toast.cancel();
                            }
                            toast = Toast.makeText(Console.this, "La valeur doit être comprise entre 0 et 127", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        break;
                    case R.id.radioDEC:
                        if(champVitesse.getText().toString().matches("(\\d*)") && champDirection.getText().toString().matches("(\\d*)")){
                            int decVitesse = Integer.parseInt(champVitesse.getText().toString());
                            int decDirection = Integer.parseInt(champDirection.getText().toString());
                            if ((decVitesse > -1 && decVitesse <= Constantes.vitesseMax) && (decDirection> -1 && decDirection <= Constantes.directionMax)) {
                                byte[] data = new byte[]{(byte) (decVitesse | 0b10000000),(byte) (decDirection & 0b01111111) };
                                bluetooth.send(data);
                                champVitesse.setText("");
                                champDirection.setText("");
                            } else {
                                if (toast != null) {
                                    toast.cancel();
                                }
                                toast = Toast.makeText(Console.this, "La valeur doit être comprise entre 0 et 127", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } else{
                            if (toast != null) {
                                toast.cancel();
                            }
                            toast = Toast.makeText(Console.this, "La valeur doit être comprise entre 0 et 127", Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        break;

                    case R.id.radioHEX:
                        if(champVitesse.getText().toString().matches("(0*[1-F]*\\d*)") && champDirection.getText().toString().matches("(0*[1-F]*\\d*)")){
                            int decVitesse = Integer.parseInt(champVitesse.getText().toString(),16);
                            int decDirection = Integer.parseInt(champDirection.getText().toString(),16);
                            if ((decVitesse > -1 && decVitesse <= Constantes.vitesseMax) && (decDirection> -1 && decDirection <= Constantes.directionMax)) {
                                byte[] data = new byte[]{(byte) (decVitesse | 0b10000000),(byte) (decDirection & 0b01111111)};
                                bluetooth.send(data);
                                champVitesse.setText("");
                                champDirection.setText("");
                            } else {
                                if (toast != null) {
                                    toast.cancel();
                                }
                                toast = Toast.makeText(Console.this, "La valeur doit être comprise entre 0 et 127", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } else{
                            if (toast != null) {
                                toast.cancel();
                            }
                            toast = Toast.makeText(Console.this, "La valeur doit être comprise entre 0 et 127", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        break;
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device = extras.getParcelable("device");
            bluetooth.connectToDevice(device);
        }else{
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
            toast = Toast.makeText(Console.this, "Aéroglisseur connecté", Toast.LENGTH_SHORT);
            toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
            ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
            toast.show();
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            if(toast != null){
                toast.cancel();
            }
            toast = Toast.makeText(Console.this, "Aéroglisseur déconnecté", Toast.LENGTH_SHORT);
            toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
            ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
            toast.show();
        }

        @Override
        public void onMessage(byte[] message) {

        }

        @Override
        public void onError(int errorCode) {

        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            if(toast != null){
                toast.cancel();
            }
            toast = Toast.makeText(Console.this, "Impossible de se connecter, nouvel essai dans 3 secondes", Toast.LENGTH_SHORT);
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

    private void backMenu(){
        if(toast != null){
            toast.cancel();
        }
        toast = Toast.makeText(Console.this, "Aucun appareil connecté", Toast.LENGTH_SHORT);
        toast.getView().setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.bleuH)));
        ((TextView)toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.blanc));
        toast.show();
        startActivity(new Intent(Console.this,MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed(){
        alert.cancel();
        Intent intent = new Intent(Console.this,MainActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }
}
