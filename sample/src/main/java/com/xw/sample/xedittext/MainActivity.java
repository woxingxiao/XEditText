package com.xw.sample.xedittext;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xw.repo.xedittext.XEditText;

public class MainActivity extends AppCompatActivity {

    private XEditText defaultSeparatorEdit;
    private XEditText customSeparatorEdit;
    private XEditText showSeparatorEdit;
    private XEditText customMarkerEdit1;
    private TextView textView1, textView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        defaultSeparatorEdit = (XEditText) findViewById(R.id.default_edit_text);
        customSeparatorEdit = (XEditText) findViewById(R.id.custom_edit_text);
        showSeparatorEdit = (XEditText) findViewById(R.id.show_separator_edit_text);
        customMarkerEdit1 = (XEditText) findViewById(R.id.custom_marker_edit_text1);
        textView1 = (TextView) findViewById(R.id.text1);
        textView2 = (TextView) findViewById(R.id.text2);
        Button button = (Button) findViewById(R.id.show_pattern_btn);

        customSeparatorEdit.setPattern(new int[]{4, 4, 4, 4});
        customSeparatorEdit.setSeparator("-");

        defaultSeparatorEdit.setOnTextChangeListener(new XEditText.OnTextChangeListener() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                textView1.setText(defaultSeparatorEdit.getNonSeparatorText());
            }
        });
        customSeparatorEdit.setOnTextChangeListener(new XEditText.OnTextChangeListener() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                textView2.setText(customSeparatorEdit.getNonSeparatorText());
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSeparatorEdit.setTextToSeparate("13800000000");
            }
        });

        customMarkerEdit1.setCustomizeMarkerEnable(true);
        customMarkerEdit1.setOnMarkerClickListener(new XEditText.OnMarkerClickListener() {
            @Override
            public void onMarkerClick(float x, float y) {
                new MarkerPopWindow(MainActivity.this, customMarkerEdit1, (int) x, (int) y);
            }
        });
    }

}
