package com.dji.mediaManagerDemo;

import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.ContactsContract;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class Splash extends AppCompatActivity {
    private TextView tv ;
    private ImageView iv ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        tv= (TextView) findViewById(R.id.tv);
        iv=(ImageView) findViewById(R.id.iv);

        Animation myanim = AnimationUtils.loadAnimation(this,R.anim.transition);
        tv.startAnimation(myanim);
        iv.startAnimation(myanim);
        final Intent Intent = new Intent(this,LoginActivity.class);
        Thread timer = new Thread(){
            public void run (){
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally{
                    startActivity(Intent);
                    finish();
                }
            }
        };
        timer.start();
    }
}
