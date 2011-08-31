/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/entry/testing_multicast_support_on_android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cafbit.multicasttest;

import com.cafbit.multicasttest.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * This utility listens for mDNS multicast activity on the local network,
 * and allows the user to submit their own mDNS multicast queries.
 * 
 * @author simmons
 */
public class MulticastTestActivity extends Activity implements OnEditorActionListener {

    public static final String TAG = "MulticastTest";

    private TextView statusLine;
    private EditText hostBox;
    private ListView listView;
    private NetThread netThread = null;
    private PacketListAdapter packetListAdapter;

    /**
     * Set up the user interface and perform certain setup steps.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        statusLine = (TextView) findViewById(R.id.status_line);
        statusLine.setText("Enter a hostname to query.");
        hostBox = (EditText) findViewById(R.id.host_box);
        listView = (ListView) findViewById(R.id.list_view);
        
        hostBox.setOnEditorActionListener(this);
        hostBox.setText("_services._dns-sd._udp.local");
    }

    /**
     * This is called when the user resumes using the activity
     * after using other programs (and at activity creation time).
     * 
     * We don't keep the network thread running when the user is
     * not running this program in the foreground, so we use this
     * method to initialize the packet list and start the
     * network thread.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "resume activity");
        
        packetListAdapter = new PacketListAdapter(this);
        listView.setAdapter(packetListAdapter);

        if (netThread != null) {
            Log.e(TAG, "netThread should be null!");
            netThread.submitQuit();
        }
        netThread = new NetThread(this);
        netThread.start();
    }

    /**
     * This is called when the user leaves the activity to run
     * another program.  We stop the network thread when this
     * happens.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "pause activity");
        
        packetListAdapter.clear();
        packetListAdapter = null;
        
        if (netThread == null) {
            Log.e(TAG, "netThread should not be null!");
            return;
        }
        netThread.submitQuit();
        netThread = null;
    }

    /**
     * Support the user pressing "enter" to activate the query.
     */
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        handleQueryButton(hostBox);
        return true;
    }

    /**
     * Handle submitting an mDNS query.
     */
    public void handleQueryButton(View view) {
        String host = hostBox.getText().toString().trim();
        if (host.length() == 0) {
            return;
        }
        
        statusLine.setText("sending query...");
        try {
            netThread.submitQuery(host);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
            statusLine.setText("query error: "+e.getMessage());
            return;
        }
        statusLine.setText("query sent.");
    }
    
    /**
     * Clear the list.
     * @param view
     */
    public void handleClearButton(View view) {
        packetListAdapter.clear();
    }

    // inter-process communication
 
    public IPCHandler ipc = new IPCHandler();
    
    /**
     * Allow the network thread to send us messages
     * via this IPC mechanism.
     * @author simmons
     */
    class IPCHandler extends Handler {

        private static final int MSG_SET_STATUS = 1;
        private static final int MSG_ADD_PACKET = 2;
        private static final int MSG_ERROR = 3;
    
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            
            // don't process incoming IPC if we are paused.
            if (packetListAdapter == null) {
                Log.w(TAG, "dropping incoming message: "+msg);
                return;
            }
            
            switch (msg.what) {
            case MSG_SET_STATUS:
                statusLine.setText((String)msg.obj);
                break;
            case MSG_ADD_PACKET:
                packetListAdapter.addPacket((Packet)msg.obj);
                listView.setSelection(packetListAdapter.getCount() - 1);
                break;
            case MSG_ERROR:
                Packet packet = new Packet();
                packet.description = ((Throwable)msg.obj).getMessage();
                packetListAdapter.addPacket(packet);
                listView.setSelection(packetListAdapter.getCount() - 1);
                break;
            default:
                Log.w(TAG, "unknown activity message code: "+msg);
                break;
            }
        }

        public void setStatus(String status) {
            sendMessage(Message.obtain(ipc, MSG_SET_STATUS, status));
        }

        public void addPacket(Packet packet) {
            sendMessage(Message.obtain(ipc, MSG_ADD_PACKET, packet));
        }

        public void error(Throwable throwable) {
            sendMessage(Message.obtain(ipc, MSG_ERROR, throwable));
        }
    };

}