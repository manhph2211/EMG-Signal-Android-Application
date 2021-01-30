package emgsignal.v3.SavedDataProcessing;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.complex.Complex;

import emgsignal.v3.R;
import emgsignal.v3.SignalProcessing.FFT;
import emgsignal.v3.SignalProcessing.MyComplex;
import emgsignal.v3.SignalProcessing.Progressing;

public class TabView extends AppCompatActivity {
    private static final double Fs = 1000;
    private final double maxYTimeGraph = 10;
    private final double maxYFFTGraph = 0.01;
    private LineGraphSeries<DataPoint> fftSeries, timeSeries, dBFFTSeries;
    private int lengthData;
    private TabHost tabHost;
    private GraphView timeGraph, frequencyGraph;
    private RadioGroup radioGroup;
    private double maxTimeSignal, minTimeSignal, maxFFTSignal, minFFTSignal, maxDBSignal, minDBSignal;
    private Button btnScaleTimeSignal, btnScaleFFTSignal;
    private int rdCheck = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_view);

        //Get intent - data from realtime signal saved
        final Intent getData = getIntent();
        lengthData = getData.getIntExtra("Length", 1);
        double[] amplitude = getData.getDoubleArrayExtra("TimeData");
        Log.i("CHECKING LONG", "long data " + lengthData);

        //Set up tabHost
        tabHost = findViewById(R.id.tabHost);
        tabHost.setup();
        timeGraph = findViewById(R.id.time_chart);
        frequencyGraph = findViewById(R.id.frequency_chart);
        TextView textView = findViewById(R.id.text_info);

        //create Time series
        timeSeries = new LineGraphSeries<>();
        timeSeries.setColor(Color.RED);
        timeSeries.setThickness(2);
        for (int k = 0; k < lengthData; k++) {
            timeSeries.appendData(new DataPoint(k, amplitude[k]), true, lengthData);
        }
        timeGraph.addSeries(timeSeries);
        timeGraph.getViewport().setMaxX(lengthData / 3.0);

        //min,max time signal
        minTimeSignal = Progressing.min(amplitude);
        String TAG = "DATA PROCESSING";
        Log.i(TAG, "MIN value of amplitude: " + minTimeSignal);
        maxTimeSignal = Progressing.max(amplitude);
        Log.i(TAG, "MAX value of amplitude: " + maxTimeSignal);

        //draw time graph
        Progressing.drawGraph(timeGraph, timeSeries, -maxYTimeGraph, maxYTimeGraph, lengthData / 3.0);

        btnScaleTimeSignal = findViewById(R.id.btn_scaleTimeSignal);
        btnScaleTimeSignal.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String text_btn = btnScaleTimeSignal.getText().toString().trim();
                if (text_btn.equals("Fit Amplitude")) {
                    Progressing.drawGraph(timeGraph, timeSeries, minTimeSignal, maxTimeSignal, lengthData / 3.0);
                    btnScaleTimeSignal.setText("Zoom out");
                } else {
                    Progressing.drawGraph(timeGraph, timeSeries, -maxYTimeGraph, maxYTimeGraph, lengthData / 3.0);
                    btnScaleTimeSignal.setText(R.string.fit_amplitude);
                }

            }
        });

        //CreateFFT Series
        fftSeries = new LineGraphSeries<>();
        fftSeries.setColor(Color.RED);
        fftSeries.setThickness(2);
        dBFFTSeries = new LineGraphSeries<>();
        dBFFTSeries.setColor(Color.BLUE);
        dBFFTSeries.setThickness(2);

        Complex[] fft = FFT.transform(amplitude);
        double[] absFFT = FFT.absFFT(fft);
        double[] dbFFT = FFT.dbFFT(absFFT);

        Log.d("LENGTH OF FFT", String.valueOf(absFFT.length));

        //min, max frequency
        minFFTSignal = Progressing.min(absFFT);
        maxFFTSignal = Progressing.max(absFFT);
        minDBSignal = Progressing.min(dbFFT);
        maxDBSignal = Progressing.max(dbFFT);

        for (int k1 = 0; k1 < absFFT.length; k1++) {
            fftSeries.appendData(new DataPoint(k1 * Fs / fft.length, absFFT[k1]), true, absFFT.length);
        }
        for (int k1 = 0; k1 < absFFT.length; k1++) {
            dBFFTSeries.appendData(new DataPoint(k1 * Fs / fft.length, dbFFT[k1]), true, absFFT.length);
        }

        Progressing.drawGraph(frequencyGraph, fftSeries, 0, maxYFFTGraph, 500);

        radioGroup = findViewById(R.id.group_radio);
        radioGroup.setOnCheckedChangeListener(
                new RadioGroup
                        .OnCheckedChangeListener() {
                    @SuppressLint("NonConstantResourceId")
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        // Get the selected Radio Button
                        int selected = radioGroup.getCheckedRadioButtonId();
                        switch (selected) {
                            case R.id.rd_fft_unit:
                                rdCheck = 0;
                                btnScaleFFTSignal.setText(R.string.fit_amplitude);
                                Progressing.drawGraph(frequencyGraph, fftSeries, 0, maxYFFTGraph, 500);
                                break;
                            case R.id.rd_db_unit:
                                rdCheck = 1;
                                btnScaleFFTSignal.setText(R.string.fit_amplitude);
                                Progressing.drawGraph(frequencyGraph, dBFFTSeries, -150, -10, 500);
                                break;
                        }
                    }
                });

        btnScaleFFTSignal = findViewById(R.id.btn_scaleFFTSignal);
        btnScaleFFTSignal.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String text_btn = btnScaleFFTSignal.getText().toString().trim();
                if (text_btn.equals("Fit Amplitude")) {
                    if (rdCheck == 0) {
                        Progressing.drawGraph(frequencyGraph, fftSeries, minFFTSignal, maxFFTSignal, 500);
                    } else
                        Progressing.drawGraph(frequencyGraph, dBFFTSeries, minDBSignal, maxDBSignal, 500);
                    btnScaleFFTSignal.setText("Zoom out");
                } else {
                    if (rdCheck == 0) {
                        Progressing.drawGraph(frequencyGraph, fftSeries, 0, maxYFFTGraph, 500);
                    } else
                        Progressing.drawGraph(frequencyGraph, dBFFTSeries, -150, -10, 500);
                    btnScaleFFTSignal.setText(R.string.fit_amplitude);
                }
            }
        });

        textView.setText("");
        textView.append("+) SNR: " + (double)Math.round(Progressing.snr(amplitude, Fs) * 100) / 100 + " dB\n");
        textView.append("+) Mean Frequency: " + (double)Math.round(Progressing.meanFrequency(absFFT, Fs) * 100) / 100 + " Hz\n");
        textView.append("+) Median Frequency: " + (double)Math.round(Progressing.medianFrequency(absFFT, Fs) * 100) / 100 + " Hz\n");
        textView.append("+) Max: " + (double)Math.round(Progressing.max(dbFFT, 1, dbFFT.length - 1) * 100) / 100 + " dB at " + (double)Math.round(Progressing.maxFrequency(dbFFT, Fs) * 100) / 100 + "Hz\n");
        textView.append("+) Min: " + (double)Math.round(Progressing.min(dbFFT, 1, dbFFT.length - 1) * 100) / 100 + " dB at " + (double)Math.round(Progressing.minFrequency(dbFFT, Fs) * 100) / 100 + "Hz\n");
        //Tab 1
        TabHost.TabSpec spec = tabHost.newTabSpec("Time domain");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Time domain");
        tabHost.addTab(spec);
        tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#2763a3"));
        TextView tv = tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).findViewById(android.R.id.title);
        tv.setTextColor(Color.WHITE);

        //Tab 2
        spec = tabHost.newTabSpec("Frequency domain");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Frequency domain");
        tabHost.addTab(spec);
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                int tab = tabHost.getCurrentTab();
                for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
                    // When tab is not selected
                    tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.parseColor("#444444"));
                    TextView tv = tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
                    tv.setTextColor(Color.BLACK);
                }
                // When tab is selected
                tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#2763a3"));
                TextView tv = tabHost.getTabWidget().getChildAt(tab).findViewById(android.R.id.title);
                tv.setTextColor(Color.WHITE);
            }
        });

        //Tab 3
        spec = tabHost.newTabSpec("Information");
        spec.setContent(R.id.tab3);
        spec.setIndicator("Information");
        tabHost.addTab(spec);
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                int tab = tabHost.getCurrentTab();
                for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
                    // When tab is not selected
                    tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.parseColor("#444444"));
                    TextView tv = tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
                    tv.setTextColor(Color.BLACK);
                }
                // When tab is selected
                tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#2763a3"));
                TextView tv = tabHost.getTabWidget().getChildAt(tab).findViewById(android.R.id.title);
                tv.setTextColor(Color.WHITE);
            }
        });
    }


    public void onRadioButtonClicked(View view) {
    }
}