package com.example.diouri.medecinapp;

import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;

/**
 * Created by Diouri on 10/04/2016.
 */
public class ActivityRTP implements RTPAppIntf {
    //Instanciated with RTPSession
    /**
     * Holds a RTPSession instance
     */
    private RTPSession rtpSession = null;
    private static boolean alertReceived = false ;
    public ActivityRTP(RTPSession rtpSession) {
        this.rtpSession = rtpSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveData(DataFrame frame, Participant p) {

        // This concatenates all received packets for a single timestamp
       byte[] alert = frame.getConcatenatedData();

            alertReceived = true;
        }


    @Override
    public void userEvent(int type, Participant[] participant) {

    }

    @Override
    public int frameSize(int payloadType) {
        return 0;
    }

    public RTPSession getRtpSession(){
        return rtpSession;
    }

    public boolean getAlertReceived(){
        return this.alertReceived;
    }
}

