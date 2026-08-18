package com.dev.ministudio;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import android.content.SharedPreferences;
import android.graphics.Color;

public class AiSettingsActivity extends AppCompatActivity {

    private EditText etApiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#1E1E1E"));
        getWindow().setNavigationBarColor(Color.parseColor("#1E1E1E"));

        setContentView(R.layout.activity_ai_settings);

        etApiKey = findViewById(R.id.etApiKey);
        Button btnSave = findViewById(R.id.btnSaveApi);

        SharedPreferences prefs =
                getSharedPreferences("ai_settings", MODE_PRIVATE);

        etApiKey.setText(
                prefs.getString("groq_api_key", "")
        );

        btnSave.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();

            prefs.edit()
                    .putString("groq_api_key", key)
                    .apply();

            Toast.makeText(
                    this,
                    "บันทึก API Key แล้ว",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        });
    }
}