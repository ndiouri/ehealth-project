package com.example.diouri.myapplication;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jlibrtp.*;


public class MainActivity extends AppCompatActivity implements SensorEventListener, RTPAppIntf {

    private final static int controlPort = 10600;
    private final static int DataClockMs = 5000;
    private static int AllocatePort= 0;
    private static String RPIAdressIp;
    private static String telNumber="";
    private SensorManager mSensorManager;
    private Sensor mySensorAccelerometer;
    private Sensor mySensorStepDetector;
    private static final String TAG = "MyActivity";
    private DatagramSocket socketUDP = null;
    private static Handler mHandler;
    private int count;
    private int seq=0;
    private final static Timer Timer = new Timer();
    EditText editText;
    EditText editNumb;
    private boolean sendingData=false;
    private static ActivityRTP server;
    private boolean fallen = false;

    //Instanciated with RTPSession
    /**
     * Holds a RTPSession instance
     */
    //private RTPSession rtpSession = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * A minimal constructor
     */
    public MainActivity(){
    }


    //public MainActivity(RTPSession rtpSession) {
    //    this.rtpSession = rtpSession;
    //}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        seq=0;

        //EditText
        editText = (EditText) findViewById(R.id.IpAdressRPI);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    start();
                    handled = true;
                }
                return handled;
            }
        });

        //EditText
        editNumb = (EditText) findViewById(R.id.telNumber);
        editNumb.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    telNumber = ""+editNumb.getText();
                    Log.d(TAG,"Voici le numéro :"+telNumber);
                    handled = true;
                }
                return handled;
            }
        });
        //------------------------------//
        // ----> Listing sensors <----- //
        //-----------------------------//


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mySensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mySensorAccelerometer.getMinDelay();
        } else {
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            mySensorStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            Log.d(TAG, "Step Detector Activate");
            mySensorStepDetector.getMinDelay();
        } else {

        }

        Log.d(TAG, "      //--------------------------------------------------------//\n" +
                "        // ----> APPEL DE LA METHODE NETWORKTask.execute() <----- //\n" +
                "        //--------------------------------------------------------//");
        try {
            socketUDP = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

  private void start() {

        RPIAdressIp = ""+editText.getText();
        Log.d(TAG,"L'adresse IP du RPI est : "+RPIAdressIp);
            // ----> Send Data <----- //
        new NetworkTask().execute();
      //Timer

      Timer.scheduleAtFixedRate(new TimerTask() {

          @Override
          public void run() {
              sendingData = true;
              DatagramPacket ack = new DatagramPacket(new byte[1024], 1024);
              while (sendingData) {
                  try {
                      //Send datagram to laptop
                      DatagramPacket packetStepNumber = new DatagramPacket(new byte[4], 4, InetAddress.getByName(RPIAdressIp), AllocatePort);
                      packetStepNumber.setData(createStepXML(count).getBytes());
                      socketUDP.send(packetStepNumber);
                      Log.d(TAG, "___________SEND______________");
                      //Wait for ACK during a fraction of DataClockMs (ideal : RTT)
                      socketUDP.setSoTimeout(DataClockMs / 4);
                      socketUDP.receive(ack);
                      count = 0;
                      seq++;
                      sendingData = false;
                  } catch (SocketTimeoutException e) {
                      //means ACK was not received, must be sent again
                      System.out.println("ACK not received.");
                  } catch (IOException e) {
                      // Auto-generated catch block
                      e.printStackTrace();
                  }
              }

          }
      }, DataClockMs, DataClockMs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        float x, y, z, Steps;
        // The Accelerometer sensor return 3 values one for each axis.

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            count++;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            if (x > 9 | y > 12 ) {
                  Log.d(TAG, "ALERTING (" + x + "," + y + "," + z + ")");

                new RTPSendThread().start();

                if (!fallen){
                    // Send SMS
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(telNumber, null, "Alert ! Patient fell at "+new Date()+".", null, null);
                    fallen = true;

                }
            }
        }


    }

    public void FallDetection() {
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do something if sensor accuracy change
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mySensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mySensorStepDetector, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void receiveData(DataFrame frame, Participant participant) {


    }

    @Override
    public void userEvent(int type, Participant[] participant) {

    }

    @Override
    public int frameSize(int payloadType) {
        return 0;
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.diouri.myapplication/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.diouri.myapplication/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }



    private class RTPSendThread extends Thread {

        public RTPSendThread(){
            super();
        }

        @Override
        public void run(){

            server.getRtpSession().sendData(createAlertXML().getBytes());


        }
    }

    class NetworkTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] params) {
            /** ASK FOR RESOURCE */
            DatagramPacket packetUDP= null;
            try {
                packetUDP = new DatagramPacket(new byte[4],4, InetAddress.getByName(RPIAdressIp),controlPort);
                socketUDP.send(packetUDP);

                packetUDP = new DatagramPacket(new byte[1024],1024, InetAddress.getByName(RPIAdressIp), controlPort);
                socketUDP.receive(packetUDP);
                AllocatePort = Integer.parseInt((new String(packetUDP.getData()).trim()));

                Log.d(TAG, "      //------------------------------//\n" +
                        "        // ----> AllocatePort : " + AllocatePort + " <----- //\n" +
                        "        //-----------------------------//");

            } catch (IOException e) {
                Log.d(TAG,"      //------------------------------//\n" +
                        "        // ----> "+e.getMessage()+" <----- //\n" +
                        "        //-----------------------------//");
            }

            /** CREATE RTP SESSION */
            // 1. Create sockets for the RTPSession
            DatagramSocket rtpSocket = null;
            DatagramSocket rtcpSocket = null;
            try {
                rtpSocket = new DatagramSocket(15000);
                rtcpSocket = new DatagramSocket(15001);

                // 2. Create the RTP session
                final RTPSession rtpSession = new RTPSession(rtpSocket, rtcpSocket);
                rtpSession.CNAME("smartphone");

                // 3. Instantiate the application object
                // MainActivity server = new MainActivity(rtpSession);

                server = new ActivityRTP(rtpSession);
                // 4. Add participants we want to notify upon registration
                rtpSession.addParticipant(new Participant(RPIAdressIp, AllocatePort + 1, AllocatePort + 2));
                // 5. Register the callback interface, this launches RTCP threads too
                // The two null parameters are for the RTCP and debug interfaces, no use here
                rtpSession.RTPSessionRegister(server, null, null);

            } catch (Exception e) {
                Log.d(TAG, "     //------------------------------//\n" +
                        "        // ----> "+e.getMessage()+" <----- //\n" +
                        "       //-----------------------------//");

            }

            // Wait 2500 ms, because of the initial RTCP wait (specification)
            // Note: The wait is optional, but insures SDES packets receive participants before continuing
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


    }

    private String createStepXML( int count){
        String xmlChain ="\n<step seq="+seq+">\n\t";
        xmlChain = xmlChain.concat("Nombre de pas : "+count+" depuis "+DataClockMs/1000+" secondes.");
        xmlChain = xmlChain.concat("\n</step>\n");
        return xmlChain;
    }

    private String createAlertXML(){
        String xmlChain ="\n<alterte>\n\t";
        xmlChain = xmlChain.concat("Date : "+new Date() );
        xmlChain = xmlChain.concat("\n</alerte>");
        return xmlChain;
    }

}



