package com.tl.tlcommande;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
    private BluetoothDevice device;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setTheme(R.style.PreferenceScreen);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device = extras.getParcelable("device");
        }else{
            startActivity(new Intent(SettingsActivity.this,MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(SettingsActivity.this,MainActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

}