package com.tl.tlcommande;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView connexionText;
    private ConstraintLayout consoleLayout;
    private ConstraintLayout joystickLayout;
    private ConstraintLayout joystick2Layout;
    private ImageView colorConnection;
    private TextView textConnection;
    private TextView textButtonConnection;
    private ImageView settings;
    private BluetoothDevice device;
    private Toast toast;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        connexionText = findViewById(R.id.textConnection);
        consoleLayout = findViewById(R.id.consoleLayout);
        joystickLayout = findViewById(R.id.joystickLayout);
        joystick2Layout = findViewById(R.id.joystickLayout2);
        settings = findViewById(R.id.settings);
        askPermissions();


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device = extras.getParcelable("device");
            connexionText.setText(String.format("Aéroglisseur : " + device.getName()));
        }else {
            device = null;
        }

        consoleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(device != null){
                    Intent intent = new Intent(MainActivity.this,Console.class);
                    intent.putExtra("device", device);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                }
                else{
                    if(toast != null){
                        toast.cancel();
                    }
                    toast = Toast.makeText(MainActivity.this, "Aucun appareil selectionné", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        joystickLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(device != null){
                    Intent intent = new Intent(MainActivity.this, Joystick.class);
                    intent.putExtra("device", device);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                }
                else{
                    if(toast != null){
                        toast.cancel();
                    }
                    toast = Toast.makeText(MainActivity.this, "Aucun appareil selectionné", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        joystick2Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(device != null){
                    Intent intent = new Intent(MainActivity.this, Joystick2.class);
                    intent.putExtra("device", device);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                }
                else{
                    if(toast != null){
                        toast.cancel();
                    }
                    toast = Toast.makeText(MainActivity.this, "Aucun appareil selectionné", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(device != null){
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    intent.putExtra("device", device);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                }
                else{
                    if(toast != null){
                        toast.cancel();
                    }
                    toast = Toast.makeText(MainActivity.this, "Aucun appareil selectionné", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

    }

    void askPermissions() {
        Dexter.withContext(this).withPermissions(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if(report.areAllPermissionsGranted()){
                            connexionText.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startActivity(new Intent(MainActivity.this,Connexion.class));
                                    overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                                }
                            });
                        }
                        else{
                            if(toast != null){
                                toast.cancel();
                            }
                            toast = Toast.makeText(MainActivity.this, "I need these permissions...", Toast.LENGTH_SHORT);
                            toast.show();
                            askPermissions();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(MainActivity.this,Connexion.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }


}
