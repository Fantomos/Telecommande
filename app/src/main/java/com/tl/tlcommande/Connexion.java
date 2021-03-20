package com.tl.tlcommande;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DiscoveryCallback;


public class Connexion extends AppCompatActivity {
    ListView pairedDeviceList;
    ListView scannedDeviceList;
    TextView state;
    ConstraintLayout scanButton;
    TextView name;

    Bluetooth bluetooth;
    private ArrayAdapter<String> scanListAdapter;
    private ArrayAdapter<String> pairedListAdapter;
    private List<BluetoothDevice> pairedDevices;
    private List<BluetoothDevice> scannedDevices;
    private boolean scanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_connexion);

        scanButton = findViewById(R.id.scanLayout);
        state = findViewById(R.id.scan);
        pairedDeviceList = findViewById(R.id.pairedList);
        scannedDeviceList = findViewById(R.id.scannedList);
        name = findViewById(R.id.textView4);

        bluetooth = new Bluetooth(this);
        bluetooth.setCallbackOnUI(this);
        bluetooth.setBluetoothCallback(bluetoothCallback);
        bluetooth.setDiscoveryCallback(discoveryCallback);

        // list for paired devices
        pairedListAdapter = new ArrayAdapter<>(this, R.layout.item_list, new ArrayList<String>());
        pairedDeviceList.setAdapter(pairedListAdapter);
        pairedDeviceList.setOnItemClickListener(onPairedListItemClick);

        // list for scanned devices
        scanListAdapter = new ArrayAdapter<>(this, R.layout.item_list, new ArrayList<String>());
        scannedDeviceList.setAdapter(scanListAdapter);
        scannedDeviceList.setOnItemClickListener(onScanListItemClick);

        scanButton.setClickable(false);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("K","click");
                name.setText("Appareils scannés");
                pairedDeviceList.setVisibility(View.INVISIBLE);
                scannedDeviceList.setVisibility(View.VISIBLE);
                bluetooth.startScanning();
            }
        });
        setProgressAndState("SCAN");


    }

    private AdapterView.OnItemClickListener onPairedListItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if(scanning){
                bluetooth.stopScanning();
            }
            startMenuActivity(pairedDevices.get(i));
        }
    };

    private AdapterView.OnItemClickListener onScanListItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if(scanning){
                bluetooth.stopScanning();
            }
            scanButton.setClickable(false);
            Toast.makeText(Connexion.this, "Appairage en cours...", Toast.LENGTH_SHORT).show();
            bluetooth.pair(scannedDevices.get(i));
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();
        pairedListAdapter.clear();
        if(bluetooth.isEnabled()){
            displayPairedDevices();
            scanButton.setClickable(true);
        } else {
            bluetooth.enable();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetooth.onStop();
    }

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override public void onBluetoothTurningOn() {}
        @Override public void onBluetoothTurningOff() {
            scanButton.setClickable(false);
        }
        @Override public void onBluetoothOff() {}
        @Override public void onUserDeniedActivation() {}
        @Override
        public void onBluetoothOn() {
            displayPairedDevices();
            scanButton.setClickable(true);
        }
    };

    private DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDiscoveryStarted() {
            Toast.makeText(Connexion.this, "Recherche...", Toast.LENGTH_SHORT).show();
            scannedDevices = new ArrayList<>();
            scanning = true;
            scanButton.setClickable(false);
        }

        @Override
        public void onDiscoveryFinished() {
            Toast.makeText(Connexion.this, "Scan terminé", Toast.LENGTH_SHORT).show();
            scanning = false;
            scanButton.setClickable(true);
        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {
            scannedDevices.add(device);
            scanListAdapter.add(device.getAddress()+" : "+device.getName());
        }

        @Override
        public void onDevicePaired(BluetoothDevice device) {
            Toast.makeText(Connexion.this, "Appairé !", Toast.LENGTH_SHORT).show();
            startMenuActivity(device);
        }

        @Override
        public void onDeviceUnpaired(BluetoothDevice device) {

        }

        @Override
        public void onError(int errorCode) {
            Log.d("K", String.valueOf(errorCode));
        }
    };


    private void setProgressAndState(String msg){
        state.setText(msg);
    }



    private void displayPairedDevices(){
        pairedDevices = bluetooth.getPairedDevices();
        for(BluetoothDevice device : pairedDevices){
            pairedListAdapter.add(device.getAddress()+" : "+device.getName());
        }
    }

    private void startMenuActivity(BluetoothDevice device){
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {

    }
}
