package com.equaleyes.depender_demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.equaleyes.annotations.Depend;

public class MainActivity extends AppCompatActivity {
    @Depend
    protected String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivityProvider.provide(this, "John Smith");

        Log.d("NAME", name);
    }
}
