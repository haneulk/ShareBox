package com.example.hnkim.sharebox;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity : ";
    //piechart
    private PieChart pieChart;
    private long[] yData = new long[2];
    private String[] xData={"사용가능한 공간", "사용한 공간"};
    long total = 0, unavail=0, avail=0;
    double usedPerc=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startActivity(new Intent(this, Splash.class));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();



        total = getTotalInternalMemorySize2();
        avail = getAvailableInternalMemorySize2();
        unavail = getTotalInternalMemorySize2() - getAvailableInternalMemorySize2();
        yData[0] = avail;    //사용안한 공간
        yData[1] = unavail;      //사용한 공간
        usedPerc = ((total-avail)/total) * 100 + 0.5;


        ///////////////////////pieChart
        pieChart = (PieChart)findViewById(R.id.piechartMain);
        pieChart.setUsePercentValues(true);
        pieChart.setDescription("");

//        pieChart.setCenterTextTypeface(t);
        pieChart.setCenterText(generateCenterSpannableText());

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
//        pieChart.setHoleColorTransparent(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(55f);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                if(e == null)
                    return;
//                Toast.makeText(getApplicationContext(), "공간 계산중", Toast.LENGTH_LONG).show();
                Intent goMA = new Intent(MainActivity.this, MemoryAnalyzer.class);
                startActivity(goMA);
            }

            @Override
            public void onNothingSelected() {

            }
        });

        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);

        pieChart.animateY(500);

        // add data
        addData();

        // customize legends
        Legend l = pieChart.getLegend();
        l.setEnabled(false);



        //navigation drawer 설정
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    //splash initialize 메소드
    private void initialize() {
        InitializationRunnable init = new InitializationRunnable();
        new Thread(init).start();
    }
    class InitializationRunnable implements Runnable {
        public void run() {

        }
    }

    //pieChart Method
    private void addData() {
        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
        for (int i = 0; i < yData.length; i++)
            yVals1.add(new Entry(yData[i], i));

        ArrayList<String> xVals = new ArrayList<String>();
        for (int i = 0; i < xData.length; i++)
            xVals.add(xData[i]);

        // create pie data set
        PieDataSet dataSet = new PieDataSet(yVals1,"");
        //파이데이터 간격 넓이, 클릭시 커지는 비율
        dataSet.setSliceSpace(5);
        dataSet.setSelectionShift(2);

        int[] MyColorTheme = {Color.rgb(204,204,204), //사용가능한 공간 -회
                Color.rgb(198,58,68),           //사진 - 빨
        };
        ArrayList<Integer> colors = new ArrayList<Integer>();


        for(int c: MyColorTheme) colors.add(c);
        dataSet.setColors(colors);


        // instantiate pie data object now
        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter()); //% 포멧으로 넣어주는 것
        data.setValueTextSize(0);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);

        // undo all highlights
        pieChart.highlightValues(null);

        // update pie chart
        pieChart.invalidate();
    }


    private SpannableString generateCenterSpannableText() {
        double total = (double)(getTotalInternalMemorySize2());
        double avail = (double)(getAvailableInternalMemorySize2());
        double usedPerc = 0;
        usedPerc = ((total-avail)/total) * 100 + 0.5;

        SpannableString s = new SpannableString((int)usedPerc+"%\n사용한 공간");
        s.setSpan(new RelativeSizeSpan(2.7f), 0, 3, 0);
        s.setSpan(new StyleSpan(Typeface.NORMAL), 3, s.length() - 3, 0);
        s.setSpan(new RelativeSizeSpan(.8f),4, s.length(), 0);
//        s.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.length(), 0);
        return s;
    }

    //long 타입 : 총 저장공간 - 시스템 저장공간
    public long getTotalInternalMemorySize2() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    //long 타입 : 사용가능한 저장공간
    public long getAvailableInternalMemorySize2() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }


    //navigation drawer
    @Override
    public void onBackPressed() {   //navigation drawer 열기
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //action bar 아이템
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {   //action bar 아이템 눌렸을 경우
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {    //navigation에 있는 아이템 눌렸을 때
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {

        } else if (id == R.id.nav_sdcard) {
            Intent in2 = new Intent(MainActivity.this, ExplorerActivity.class);
            startActivity(in2);
        } else if (id == R.id.nav_analysis) {
            Intent in3 = new Intent(MainActivity.this, MemoryAnalyzer.class);
            startActivity(in3);

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    /////////////////Main화면 동작 메소드
    public void onClick(View view) {
        Intent intent = new Intent(this, ExplorerActivity.class);
        ArrayList<String> pathBarStatus = new ArrayList<>();
        pathBarStatus.add("루트");
        intent.putExtra("mPathBarStatus", pathBarStatus);
        String currentDirPath = "";
        switch(view.getId()) {
            case R.id.rootBtn :
                startActivity(new Intent(this, ExplorerActivity.class));
                return;

            case R.id.downloadBtn :
                currentDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                break;

            case R.id.picturesBtn :
                currentDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
                break;

            case R.id.musicBtn :
                currentDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
                break;

            case R.id.videoBtn :
                currentDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
                break;

            case R.id.dcimBtn :
                currentDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
                break;

            default:
                return;
        }
        if(!(new File(currentDirPath).exists()))
            return;
        intent.putExtra("mCurrentDirPath", currentDirPath);
        startActivity(intent);
    }
}
