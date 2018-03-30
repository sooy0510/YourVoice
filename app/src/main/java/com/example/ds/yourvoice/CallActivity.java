package com.example.ds.yourvoice;

import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Device.Device;
import com.vidyo.VidyoClient.Device.LocalCamera;
import com.vidyo.VidyoClient.Device.LocalMonitor;
import com.vidyo.VidyoClient.Device.LocalWindowShare;
import com.vidyo.VidyoClient.Endpoint.ChatMessage;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.util.ArrayList;
import java.util.logging.Logger;

public class CallActivity extends AppCompatActivity
        implements Connector.IConnect, Connector.IRegisterParticipantEventListener, Connector.IRegisterMessageEventListener {

    private Connector vc;
    private String token;
    private FrameLayout videoFrame;
    private boolean mVidyoClientInitialized = false;

    enum VidyoConnectorState {
        VidyoConnectorStateConnected,
        VidyoConnectorStateDisconnected,
        VidyoConnectorStateDisconnectedUnexpected,
        VidyoConnectorStateFailure
    }

    private VidyoConnectorState mVidyoConnectorState = VidyoConnectorState.VidyoConnectorStateDisconnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call);

        ConnectorPkg.setApplicationUIContext(this);
        mVidyoClientInitialized = ConnectorPkg.initialize();
        videoFrame = (FrameLayout)findViewById(R.id.videoFrame);
    }

    public void Start(View v) {
        vc = new Connector(videoFrame, Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, 15, "warning info@VidyoClient info@VidyoConnector", "", 0);
        vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());

        vc.selectDefaultCamera();
        vc.selectDefaultMicrophone();
        vc.selectDefaultSpeaker();
        vc.selectDefaultNetworkInterfaceForSignaling();
        vc.selectDefaultNetworkInterfaceForMedia();
}

    public void Connect(View v) {
        token = "cHJvdmlzaW9uAGluc2VvbkA0OTNmN2UudmlkeW8uaW8AMTYzNjg5NjA5MDM5AAAwMjA1NGY0YTIxOTRkYzQ1NTc1ZGM5NGVmZThjMTI3MGI1Yjk2Y2FmN2ZmYzAxYjJiOTZiYTY1ZGJiYjYwYmI2MTBmNGQ1MTcyNWI1NTQxMzY2NWNkZTFhNGViYzY1NWU=";

        if(mVidyoClientInitialized) {
            vc = new Connector(videoFrame, Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, 2, "warning info@VidyoClient info@VidyoConnector", "", 0);
            vc.showViewAt(videoFrame, 0, 0, videoFrame.getWidth(), videoFrame.getHeight());

            vc.selectDefaultCamera();
            vc.selectDefaultMicrophone();
            vc.selectDefaultSpeaker();
            vc.selectDefaultNetworkInterfaceForSignaling();
            vc.selectDefaultNetworkInterfaceForMedia();

            //mVidyoConnector.Connect(host, token, displayName, resourceId, this);
            vc.connect("prod.vidyo.io", token, "DemoUser", "inseon-soov1", this);
        } else {
            Log.d("Initialize failed", "not constructing VidyoConnector");
        }
    }

    public void Disconnect(View v) {
        vc.disconnect();
    }

    public void onSuccess() {
        Log.d("onSuccess", "successfully connected.");
        //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateConnected, "Connected");
    }

    public void onFailure(Connector.ConnectorFailReason reason) {
        Log.d("onFailure", ": connection attempt failed, reason = " + reason.toString());

        // Update UI to reflect connection failed
        //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateFailure, "Connection failed");
    }

    public void onDisconnected(Connector.ConnectorDisconnectReason reason) {
        if (reason == Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
            Log.d("onDisconnected", "successfully disconnected, reason = " + reason.toString());
            //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateDisconnected, "Disconnected");
        } else {
            Log.d("onDisconnected", "unexpected disconnection, reason = " + reason.toString());
            //connectorStateUpdated(VidyoConnectorState.VidyoConnectorStateDisconnectedUnexpected, "Unexpected disconnection");
        }
    }

    // Register for VidyoConnector event listeners. Note: this is an arbitrary function name.
    public void RegisterForVidyoEvents() {
        // Register for Participant callbacks
        vc.registerParticipantEventListener(this);

        // Register to receive chat messages
        vc.registerMessageEventListener(this);

       /* Register for local window share and local monitor events */
//        vc.registerLocalWindowShareEventListener(this);
//        vc.registerLocalMonitorEventListener(this);
    }
    // Participant Joined
    public void onParticipantJoined(Participant participant) {}
    // Participant Left
    public void onParticipantLeft(Participant participant) {}
    // Ordered array of participants according to rank
    public void onDynamicParticipantChanged(ArrayList participants, ArrayList cameras) {}
    // Current loudest speaker
    public void onLoudestParticipantChanged(Participant participant, boolean audioOnly) {}

    // Register for VidyoConnector event listeners. Note: this is an arbitrary function name.
//    public void RegisterForVidyoEvents() {
//        // Register to receive chat messages
//        vc.registerMessageEventListener(this);
//    }

    private void SendChatMessage(String message) {
        vc.sendChatMessage(message);
    }
    // Message received from other participants
    public void onChatMessageReceived(Participant participant, ChatMessage chatMessage) {}

//    /* Register for VidyoConnector event listeners. Note: this is an arbitrary function name. */
//    public void RegisterForVidyoEvents() {
//        /* Register for local window share and local monitor events */
//        vc.registerLocalWindowShareEventListener(this);
//        vc.registerLocalMonitorEventListener(this);
//    }


    /* WindowShare Event Listener */
//    public void onLocalWindowShareAdded(LocalWindowShare localWindowShare) { /* New window is available for sharing */
//        if (/* This is the window that should be shared */) {
//           vc.selectLocalWindowShare(localWindowShare);
//        }
//    }
//    public void onLocalWindowShareRemoved(LocalWindowShare localWindowShare)  { /* Existing window is no longer available for sharing */ }
//    public void onLocalWindowShareSelected(LocalWindowShare localWindowShare) { /* Window was selected */ }
//    public void onLocalWindowShareStateUpdated(LocalWindowShare localWindowShare, Device.DeviceState state) {  /* window share state has been updated */ }
//    /* Monitor Event Listener */
//    public void onLocalMonitorAdded(LocalMonitor localMonitor)    { /* New monitor is available for sharing*/
//        if (/* This is the monitor that should be shared */) {
//            vc.selectLocalMonitor(localMonitor);
//        }
//    }
//    public void onLocalMonitorRemoved(LocalMonitor localMonitor)  { /* Existing monitor is no longer available for sharing */ }
//    public void onLocalMonitorSelected(LocalMonitor localMonitor) { /* Monitor was selected */ }
//    public void onLocalMonitorStateUpdated(LocalMonitor localMonitor, Device.DeviceState state) {  /* monitor state has been updated */ }
// Connector.IRegisterLocalWindowShareEventListener,    Connector.IRegisterLocalMonitorEventListener
}
