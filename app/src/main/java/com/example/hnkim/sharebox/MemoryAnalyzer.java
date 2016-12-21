package com.example.hnkim.sharebox;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;
import android.widget.Toast;

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

/**
 * Created by hnkim on 2016-12-15.
 */

public class MemoryAnalyzer extends AppCompatActivity {

    private static final String TAG = "MemoryAnalyzer: ";
    TextView txtInterTotal, txtInterAvailTotal, txtImgTotal, txtMusicTotal, txtVideoTotal, txtTextTotal, txtAppTotal, txtSoOnTotal;
    Intent intent;


    private static final String[] image = {"png", "jpg", "jpeg", "bmp"};    //이미지 파일 확장자
    private static final String[] music = {"mp3", "wav", "wma, m4a", "ogg"};             //음악 파일 확장자(mp3파일 + 음성녹음, 통화녹음파일-m4a)
    private static final String[] video = {"avi", "flv", "wmv", "mp4", "3gp"};     //비디오 파일 확장자
    private static final String[] text = {"txt", "hwp", "ppt", "pdf", "doc", "pptx", "xlsx"};     //문서 파일 확장자
    private static final String[] app = {"apk"};                               //앱 파일 확장자
    String androidPath = null;


    //piechart
    private PieChart pieChart;
    private long[] yData = new long[7];
    private String[] xData={"사용가능한 공간", "사진", "음악", "동영상", "문서", "기타", "앱"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memoryanalyzer);

        intent = getIntent();
        File sdcardFile = Environment.getExternalStorageDirectory();    //sdcard 경로 받음

        androidPath = sdcardFile + "/Android";                      //Android 폴더 경로

        //용량 textview id
        txtInterTotal = (TextView) findViewById(R.id.txtInterTotal);
        txtInterAvailTotal = (TextView) findViewById(R.id.txtInterAvailTotal);
        txtImgTotal = (TextView) findViewById(R.id.txtImgTotal);
        txtMusicTotal = (TextView) findViewById(R.id.txtMusicTotal);
        txtVideoTotal = (TextView) findViewById(R.id.txtVideoTotal);
        txtTextTotal = (TextView) findViewById(R.id.txtTextTotal);
        txtAppTotal = (TextView) findViewById(R.id.txtAppTotal);
        txtSoOnTotal = (TextView) findViewById(R.id.txtSoOnTotal);

        // 접근 권한 확인 후, 권한이 없으면 권한 신청
        if (ContextCompat.checkSelfPermission(MemoryAnalyzer.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MemoryAnalyzer.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(MemoryAnalyzer.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }

        //배열에 구한 용량 저장
        yData[0] = getAvailableInternalMemorySize2();
        yData[1] = getDirectorySize(sdcardFile, image);
        yData[2] = getDirectorySize(sdcardFile, music);
        yData[3] = getDirectorySize(sdcardFile, video);
        yData[4] = getDirectorySize(sdcardFile, text);
        yData[6] = getDirectorySize(sdcardFile, app);

        long sum=0;
        for(int i=1; i<6; i++) {
            sum += yData[i];
        }
        yData[5] = getUsedMemorySize() - sum;

        //용량 setText
        txtInterTotal.setText(getTotalInternalMemorySize());
        txtInterAvailTotal.setText(getAvailableInternalMemorySize());
        txtImgTotal.setText(changeFormat(getDirectorySize(sdcardFile, image)));
        txtMusicTotal.setText(changeFormat(getDirectorySize(sdcardFile, music)));
        txtVideoTotal.setText(changeFormat(getDirectorySize(sdcardFile, video)));
        txtTextTotal.setText(changeFormat(getDirectorySize(sdcardFile, text)));
        txtAppTotal.setText(changeFormat(getDirectorySize(sdcardFile, app)));
        txtSoOnTotal.setText(changeFormat(getUsedMemorySize()-sum));

        //총 사용한 공간
        double total = (double)(getTotalInternalMemorySize2()); //총 저장공간
        double usable = (double)(getAvailableInternalMemorySize2());    //사용가능한 저장공간
        double used = total - usable; //사용한 공간
        long usedLong = (long)(total - usable);
        double usedPerc = 0;
        usedPerc = ((total-usable)/total) * 100 + 0.5;


        //Main으로 PieChart data 넘겨줌
        Intent pieIn = new Intent(MemoryAnalyzer.this, MainActivity.class);
        pieIn.putExtra("total", total);
        pieIn.putExtra("usable", usable);
        pieIn.putExtra("used" , used);
        pieIn.putExtra("usedLong", usedLong);
        pieIn.putExtra("usedPerc", usedPerc);
//        startActivity(pieIn);


        ///////////////////////pieChart
        pieChart = (PieChart)findViewById(R.id.piechart);
        pieChart.setUsePercentValues(true);
        pieChart.setDescription("");

//        pieChart.setCenterTextTypeface(t);
        pieChart.setCenterText(generateCenterSpannableText());

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
//        pieChart.setHoleColorTransparent(true);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(68f);

//        pieChart.setDrawCenterText(true);

        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);

        pieChart.animateY(1500);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                if(e == null)
                    return;
                //토스트 메세지 넣기
                double yData = (double)e.getVal();
                double total = (double)(getTotalInternalMemorySize2());
                double perc = 0;
                perc = (yData/total) * 100 + 0.5;
                Toast.makeText(getApplicationContext(), xData[e.getXIndex()]+" : "+(int)perc + "%", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected() {

            }
        });

        // add data
        addData();

        // customize legends
        Legend l = pieChart.getLegend();
//        l.setEnabled(false);
        //l.setForm(Legend.LegendForm.CIRCLE);
        l.setWordWrapEnabled(true);
        l.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
        l.setFormSize(10f);
        l.setXEntrySpace(40f);
        l.setYEntrySpace(5f);
    }

    //총 저장공간 - 시스템 저장공간
    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return changeFormat(totalBlocks * blockSize);
    }

    //long 타입 : 총 저장공간 - 시스템 저장공간
    public long getTotalInternalMemorySize2() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    //사용 가능한 저장공간
    public static String getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return changeFormat(availableBlocks * blockSize);
    }


    //long 타입 : 사용가능한 저장공간
    public long getAvailableInternalMemorySize2() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    //사용중인 공간
    public static long getUsedMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long TotalBlockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();

        long total = TotalBlockSize * totalBlocks;

        long AvailableBlockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();

        long avail = AvailableBlockSize * availableBlocks;

        long usedMemory = total - avail;

        return usedMemory;
    }

    //사용중인 전체 파일 용량 구함
    public long getDirectorySize(File dir, String[] fileExtends) {
        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory() && !fileList[i].getAbsolutePath().equals(androidPath)) {    //디렉터리이고, Android폴더가 아니면 //시간이 오래걸려서 Android폴더는 뺌
                    result += getDirectorySize(fileList[i], fileExtends);

                } else {  //디렉터리가 아니면
                    if (fileExtends == null)
                        result += fileList[i].length();
                    else {
                        for (String ex : fileExtends) {
                            String fileName = fileList[i].getName();
                            if (fileName.length() < ex.length()) continue;
                            String strex = fileName.substring(fileName.length() - ex.length());
                            if (strex.equals(ex)) {
                                result += fileList[i].length();
                                //break;
                            }
                        }
                    }
                }
            }
            return result;
        }
        return 0;
    }


    //format 변경
    public static String floatForm(double d) {
        return new DecimalFormat("#.##").format(d);
    }

    public static String changeFormat(long size) {
        long Kb = 1 * 1024;
        long Mb = Kb * 1024;
        long Gb = Mb * 1024;
        long Tb = Gb * 1024;
        long Pb = Tb * 1024;
        long Eb = Pb * 1024;

        if (size < Kb) return floatForm(size) + " byte";
        if (size >= Kb && size < Mb) return floatForm((double) size / Kb) + " KB";
        if (size >= Mb && size < Gb) return floatForm((double) size / Mb) + " MB";
        if (size >= Gb && size < Tb) return floatForm((double) size / Gb) + " GB";
        if (size >= Tb && size < Pb) return floatForm((double) size / Tb) + " TB";
        if (size >= Pb && size < Eb) return floatForm((double) size / Pb) + " PB";
        if (size >= Eb) return floatForm((double) size / Eb) + " EB";

        return "???";


    }


    //pieChart method
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
        dataSet.setSliceSpace(8);
        dataSet.setSelectionShift(5);

        int[] MyColorTheme = {Color.rgb(204,204,204), //사용가능한 공간 -회
                Color.rgb(198,58,68),           //사진 - 빨
                Color.rgb(251,135,83),          //음악 - 주
                Color.rgb(231,185,15),           //비디오 - 찐노
                Color.rgb(33,133,89),           //문서 - 초
                Color.rgb(38,99,163),           //기타
                Color.rgb(0,188,212)          //앱 - 하
                };
        ArrayList<Integer> colors = new ArrayList<Integer>();


        for(int c: MyColorTheme) colors.add(c);
        dataSet.setColors(colors);


        // instantiate pie data object now
        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter()); //% 포멧으로 넣어주는 것
        data.setValueTextSize(0);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);

        // undo all highlights
        pieChart.highlightValues(null);

        // update pie chart
        pieChart.invalidate();
    }


    private SpannableString generateCenterSpannableText() {

        double total = (double)(getTotalInternalMemorySize2());
        double unused = (double)(getAvailableInternalMemorySize2());
        double perc = 0;
        perc = ((total-unused)/total) * 100 + 0.5;

        SpannableString s = new SpannableString((int)perc+"%\n사용한 공간");
        s.setSpan(new RelativeSizeSpan(2.7f), 0, 3, 0);
        s.setSpan(new StyleSpan(Typeface.NORMAL), 3, s.length() - 3, 0);
        s.setSpan(new RelativeSizeSpan(.8f),4, s.length(), 0);
        return s;
    }
}