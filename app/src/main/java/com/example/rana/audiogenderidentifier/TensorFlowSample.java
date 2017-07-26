package com.example.rana.audiogenderidentifier;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/**
 * Created by Rana on 20.07.17.
 */

public class TensorFlowSample extends AppCompatActivity {
    double num1 = 0;
    double num2 = 3.33;
    double num3 = 1.2;

    private static final String MODEL_FILE = "file:///android_asset/optimized_tfdroid2.pb";
    private static final String INPUT_NODE = "I";
    private static final String OUTPUT_NODE = "O";

    private static final int[] INPUT_SIZE = {1,3};

    private TensorFlowInferenceInterface inferenceInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inferenceInterface = new TensorFlowInferenceInterface();
        inferenceInterface.initializeTensorFlow(getAssets(), MODEL_FILE);
        double[] inputFloats = {num1, num2, num3};

        inferenceInterface.fillNodeDouble(INPUT_NODE, INPUT_SIZE, inputFloats);
        inferenceInterface.runInference(new String[] {OUTPUT_NODE});

        float[] resu = {0, 0};
        inferenceInterface.readNodeFloat(OUTPUT_NODE, resu);

        Log.v("output_mode" , OUTPUT_NODE);

    }


}
