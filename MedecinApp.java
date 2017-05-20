package com.example.diouri.medecinapp;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import jlibrtp.*;

public class MedecinApp extends AppCompatActivity {

    EditText editText;
    private static String laptopAddress;
    private final static int myPort = 12500;
    private final static int laptopPort = 11600;
    private final static DatagramPacket packet= new DatagramPacket(new byte[4], 4);
    private static ActivityRTP server;
    private static final String TAG = "MyActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medecin_app);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // new WatchingThread().start();


        //EditText
        editText = (EditText) findViewById(R.id.laptopAdress);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    laptopAddress = "" + editText.getText();
                    new RtpThread().start();
                    handled = true;
                }
                return handled;
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


    }
    private class RtpThread extends Thread {
        public RtpThread(){
            super();
        }
        @Override
        public void run() {
            while(true){
                // 1. Create sockets for the RTPSession
                DatagramSocket rtpSocket = null;
                DatagramSocket rtcpSocket = null;
                try {
                    rtpSocket = new DatagramSocket(myPort);
                    rtcpSocket = new DatagramSocket(myPort + 1);

                    // 2. Create the RTP session
                    RTPSession rtpSession = new RTPSession(rtpSocket, rtcpSocket);

                    // 3. Instantiate the application object
                    server = new ActivityRTP(rtpSession);

                    // Lancement de Watching Thread
                     new WatchingThread().start();

                    // 4. Add participants we want to notify upon registration
                    rtpSession.addParticipant(new Participant(laptopAddress, laptopPort + 1, laptopPort + 2));

                    // 5. Register the callback interface, this launches RTCP threads too
                    // The two null parameters are for the RTCP and debug interfaces, no use here
                    rtpSession.RTPSessionRegister(server, null, null);

                }catch (Exception e) {
                    e.printStackTrace();
                }
                // Wait 2500 ms, because of the initial RTCP wait (specification)
                // Note: The wait is optional, but insures SDES packets receive participants before continuing
                try{ Thread.sleep(2000); } catch(Exception e) {e.printStackTrace();}
            }
        }
    }

    private class WatchingThread extends Thread {
        public WatchingThread(){
            super();
        }
        @Override
        public void run() {

            while(!server.getAlertReceived()){
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView text = (TextView) findViewById(R.id.AlertField);
                    text.setText("YOU HAVE RECEIVED AN ALERT !\nPATIENT FELL !");
                }
            });

            Log.d(TAG,"YOU HAVE RECEIVED AN ALERT !!!!!!!!!!");

        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_medecin_app, menu);
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
}
