package doszke.czujniki;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
//import android.support.v4.content.ContextCompat;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isRunning = false;
    private PowerManager.WakeLock wakeLock;
    private static final double G = 9.81;

    Button buttonMeasurement;
    TextView tvAx, tvAy, tvAz, tvSteps;

    private ArrayList<Double> magnitudeData;
    private int stepCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyAPPK:Test czujników");
        wakeLock.acquire(); //niezbędne

        buttonMeasurement = findViewById(R.id.button);
        tvAx = findViewById(R.id.tvAx);
        tvAy = findViewById(R.id.tvAy);
        tvAz = findViewById(R.id.tvAz);
        tvSteps = findViewById(R.id.tvSteps);
/*
        to do api 27+, mój telefon nie wymaga tego- wręcz z użyciem ContextCompat mój telefon nie spełnia wymagań
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

        }
*/


    }


    //zegar z częstotliwoscią 40Hz ustawia isDataPickingAllowed na true, jeżeli program wyłapie true, to ustawi wartość na false i dokona wpisu na ArrayListę
    @Override
    public void onSensorChanged(SensorEvent event) {
        Float ax, ay, az;

        if(isRunning){
            int sensorType = event.sensor.getType();
            if(sensorType == Sensor.TYPE_ACCELEROMETER){

                double magnitude;
                //uważam, że 40Hz to wystarczająca częstotliwość poboru danych

                    ax = event.values[0];
                    ay = event.values[1];
                    az = event.values[2];

                    tvAx.setText(String.valueOf(ax));
                    tvAy.setText(String.valueOf(ay));
                    tvAz.setText(String.valueOf(az));

                    //magnitude of acceleration vector

                    magnitude = Math.abs(Math.sqrt(ax*ax  +ay*ay + az*az) - G); //usuwam g, nie wiem czy dobrze, lecz myśle ze to nie wpłynie na wynik

                    //jeżeli przekroczy mój wyznaczony próg i lista ma conajmniej 2 elementy
                    if(magnitude > 3.5 && magnitudeData.size() > 2){
                        double oneBefore = magnitudeData.get(magnitudeData.size()-1); //pobieram element przed

                        //jeżeli nie jest prawdą, ze element przed jest większy od 3.5 to dodaj krok
                        if(!(oneBefore > 3.5)){
                            stepCounter++;
                        }

                    }

                    tvSteps.setText(String.valueOf(stepCounter));

                    magnitudeData.add(magnitude);

                    Log.d("onSensorChanged ax:", String.valueOf(ax) +  ", " + String.valueOf(ay)+", " + String.valueOf(az));

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void saveToFile(ArrayList<Double> data, String folder, String fileName) {


        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + folder);
        dir.mkdirs();
        File file = new File(dir, fileName);

        String test = file.getAbsolutePath();
        Log.i("My", "FILE LOCATION: " + test);


        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);


            for (int i = 0; i < data.size(); i++) {
                pw.println(data.get(i));
            }

            pw.flush();
            pw.close();
            f.close();


            Toast.makeText(getApplicationContext(),

                    "Data saved",

                    Toast.LENGTH_LONG).show();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("My", "******* File not found *********");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String readFromFile(String folder, String fileName) {

        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + folder);
        File file = new File(dir, fileName);

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("My", "******* File not found *********");
        } catch (IOException e) {
            //You'll need to add proper error handling here
        } finally {
            return text.toString();
        }


    }



    public void onClickBtn(View view) {
        if(!isRunning){
            //uruchamiam zegar,
            magnitudeData = new ArrayList<>();
            stepCounter = 0;
            wakeLock.acquire();

        } else {
            //wyłączam zegar
            wakeLock.release();
            saveToFile(magnitudeData, "/TEST/", "testMagnitudeStepCounter.txt");

        }

        Log.d("onClickBtn", "button pressed");
        isRunning = !isRunning;


    }
}
