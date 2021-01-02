package id.rllyhz.mobilecomputingproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreen.this, SmartMusicPlayerActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.do_nothing_in_anim);
                finish();
            }
        }, 3000);
    }
}