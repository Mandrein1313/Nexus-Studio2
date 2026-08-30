package com.dev.ministudio;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class AiSettingsActivity extends AppCompatActivity {

    private EditText etApiKey;
    private Spinner spinTextModel;
    private Spinner spinVisionModel;

    // รายการโมเดล — ปรับ/เพิ่มได้ตามที่ Groq รองรับ
    private static final String[] TEXT_MODELS = {
            "openai/gpt-oss-20b",
            "meta-llama/llama-4-scout-17b-16e-instruct",
            "qwen/qwen3.6-27b"
    };

    private static final String[] VISION_MODELS = {
            "qwen/qwen3.6-27b",
            "qwen/qwen3.8-27b",
            "meta-llama/llama-4-scout-17b-16e-instruct"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#1E1E1E"));
        getWindow().setNavigationBarColor(Color.parseColor("#1E1E1E"));

        setContentView(R.layout.activity_ai_settings);

        etApiKey = findViewById(R.id.etApiKey);
        spinTextModel = findViewById(R.id.spinTextModel);
        spinVisionModel = findViewById(R.id.spinVisionModel);
        Button btnSave = findViewById(R.id.btnSaveApi);

        SharedPreferences prefs = getSharedPreferences("ai_settings", MODE_PRIVATE);

        // API Key
        etApiKey.setText(prefs.getString("groq_api_key", ""));

        // Spinner โมเดลข้อความ
        ArrayAdapter<String> textAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, TEXT_MODELS);
        spinTextModel.setAdapter(textAdapter);
        selectSpinnerValue(spinTextModel, TEXT_MODELS,
                prefs.getString("groq_model", TEXT_MODELS[0]));

        // Spinner โมเดลรูป
        ArrayAdapter<String> visionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, VISION_MODELS);
        spinVisionModel.setAdapter(visionAdapter);
        selectSpinnerValue(spinVisionModel, VISION_MODELS,
                prefs.getString("groq_vision_model", VISION_MODELS[0]));

        btnSave.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            String textModel = (String) spinTextModel.getSelectedItem();
            String visionModel = (String) spinVisionModel.getSelectedItem();

            prefs.edit()
                    .putString("groq_api_key", key)
                    .putString("groq_model", textModel != null ? textModel : TEXT_MODELS[0])
                    .putString("groq_vision_model", visionModel != null ? visionModel : VISION_MODELS[0])
                    .apply();

            Toast.makeText(this, "บันทึกการตั้งค่า AI แล้ว", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void selectSpinnerValue(Spinner spinner, String[] items, String value) {
        if (value == null) {
            spinner.setSelection(0);
            return;
        }
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(0);
    }
}