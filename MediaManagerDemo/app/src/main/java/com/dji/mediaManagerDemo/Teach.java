package com.dji.mediaManagerDemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Teach extends AppCompatActivity {

    private ViewPager mSliderViewPager;
    private LinearLayout mDotLayout;
    private TextView[] mDots;
    private SliderAdaptator sliderAdaptator;
    private Button mNextBtn;
    private Button mBackBtn;
    private int myCurrentPage;
    private boolean firstOpenApp = true;
    private SharedPreferences mSharedPreferences;
    private static final String DATA = "DATA";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teach2);

        mSharedPreferences = getSharedPreferences(DATA, MODE_PRIVATE);
        readData();
        CheckFirstIn();


        mSliderViewPager=(ViewPager) findViewById(R.id.slideViewPager);
        mDotLayout = (LinearLayout) findViewById(R.id.dotsLayout);

        mNextBtn = (Button) findViewById(R.id.nextBtn);
        mBackBtn = (Button) findViewById(R.id.prevBtn);


        sliderAdaptator = new SliderAdaptator(this);
        mSliderViewPager.setAdapter(sliderAdaptator);

        addDotsIndicator(0);
        mSliderViewPager.addOnPageChangeListener(viewListener);

        //onclicklistener

        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(myCurrentPage==2){
                    Intent intent = new Intent(Teach.this,ConnectionActivity.class);
                    startActivity(intent);
                }else{
                    mSliderViewPager.setCurrentItem(myCurrentPage + 1);
                }
            }
        });

        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mSliderViewPager.setCurrentItem(myCurrentPage - 1);
            }
        });
    }
    private void readData() {//讀取
        firstOpenApp = mSharedPreferences.getBoolean("Open", firstOpenApp);
    }

    private void saveData() {//儲存
        mSharedPreferences.edit()
                .putBoolean("Open", false)
                .apply();
    }
    private void CheckFirstIn() {
        if (firstOpenApp) {
            new AlertDialog.Builder(this)
                    .setMessage("這是第一次開啟App")
                    .setPositiveButton("確定進入", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();//關閉Dialog
                        }
                    }).show();
            firstOpenApp = false;
        }
        else if (firstOpenApp == false){
            Intent intent = new Intent(this, ConnectionActivity.class);
            startActivity(intent);
        }
    }
    public void addDotsIndicator(int position){
        mDots = new TextView[3];

        for(int i= 0; i<mDots.length; i++){
            mDots[i]= new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226;"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.colorWhite));
        }

        if(mDots.length>0){
            mDots[position].setTextColor(getResources().getColor(R.color.colorWhite));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {

        }

        @Override
        public void onPageSelected(int i) {

            addDotsIndicator(i);
            myCurrentPage = i;

            if(i==0){

                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.INVISIBLE);

                mNextBtn.setText("Next");
                mBackBtn.setText("");

            } else if (i == mDots.length-1) {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.VISIBLE);

                mNextBtn.setText("Finish");

                mBackBtn.setText("Back");
            } else {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.VISIBLE);

                mNextBtn.setText("Next");
                mBackBtn.setText("Back");
            }

        }



        @Override
        public void onPageScrollStateChanged(int i) {

        }

    };
    @Override
    protected void onPause() {//在onPause內儲存
        super.onPause();
        saveData();
    }
}
