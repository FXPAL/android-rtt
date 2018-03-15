package com.fxpal.android_rtt;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.WifiRttManager;
import android.net.wifi.rtt.RangingResultCallback;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";
    private WifiRttManager rttManager;
    private WifiManager wifiManager;

    private int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 100;

    private ListView lv;
    private TextView textStatus;
    private Button buttonScan;
    private Button buttonUpdate;
    private int size = 0;
    private List<ScanResult> scanResults;
    private List<RangingResult> rangingResults;

    private String ITEM_KEY = "key";
    private ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    private SimpleAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = (TextView) findViewById(R.id.textStatus);
        buttonScan = (Button) findViewById(R.id.buttonScan);
        buttonScan.setOnClickListener(this);
        buttonUpdate = (Button) findViewById(R.id.buttonUpdate);
        buttonUpdate.setOnClickListener(this);
        lv = (ListView)findViewById(R.id.list);
        adapter = new SimpleAdapter(this, arraylist, R.layout.row, new String[] { ITEM_KEY }, new int[] { R.id.list_value });
        lv.setAdapter(this.adapter);


        Context context = getApplicationContext();
        rttManager = (WifiRttManager)context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        final MainActivity self = this;
        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    self.onScanResultsAvailable();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);

        if (checkPermissions()) {
            wifiManager.startScan();
        }
    }

    public void onRefreshList() {
        arraylist.clear();
        for (int i = 0; i < scanResults.size(); i++) {
            ScanResult result = scanResults.get(i);
            Log.v(TAG, result.SSID + ", " + result.is80211mcResponder());
            HashMap<String, String> item = new HashMap<String, String>();

            RangingResult ranging = null;
            try {
                ranging = rangingResults.get(i);
            } catch (Exception e) {

            }

            String value = result.SSID + "(" + result.BSSID + "): 802.11mcResponder: " + result.is80211mcResponder() + "\n"
                    + result.capabilities + "\n";
            if (ranging != null && ranging.getStatus() == RangingResult.STATUS_SUCCESS) {
                value += "mac: " + ranging.getMacAddress() + " mmdist: " + ranging.getDistanceMm() + " stddev: " + ranging.getDistanceStdDevMm();
            }

            item.put(ITEM_KEY, value);
            arraylist.add(item);
        }
        adapter.notifyDataSetChanged();
    }

    public void onScanResultsAvailable() {
        // not getting called, only after running app and manually going to the wifi settings in android
        scanResults = wifiManager.getScanResults();
        size = scanResults.size();

        // Filter out PALWIFI acess points only
//        List<ScanResult> newScanResults = new ArrayList<ScanResult>();
//        for (int i = 0; i < scanResults.size(); i++) {
//            ScanResult result = scanResults.get(i);
//            if (result.SSID.equals("PALWIFI")) {
//                newScanResults.add(result);
//            }
//        }
        List<ScanResult> newScanResults = scanResults;

        //scanResults = scanResults.subList(0, min(scanResults.size(), RangingRequest.getMaxPeers()));
        scanResults = newScanResults.subList(0, min(newScanResults.size(), RangingRequest.getMaxPeers()));

        RangingRequest.Builder builder = new RangingRequest.Builder();
        builder.addAccessPoints(scanResults);

        final MainActivity self = this;
        RangingRequest request = builder.build();
        if (rttManager.isAvailable()) {
            rttManager.startRanging(request, new RangingResultCallback() {

                @Override
                public void onRangingFailure(int i) {
                    Log.e(TAG, "onRangingFailure: " + i);
                    rangingResults = new ArrayList<RangingResult>();
                    self.onRefreshList();
                }

                @Override
                public void onRangingResults(List<RangingResult> list) {
                    Log.v(TAG, "onRangingResults size = " + list.size());
                    rangingResults = list;
                    self.onRefreshList();
                }
            }, null);
        }


    }


    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Do something with granted permission
            //wifiManager.startScan();
        }
    }

    public void onClick(View view)
    {
        if (view == buttonUpdate) {
            onScanResultsAvailable();
        }

        if (view == buttonScan) {
            arraylist.clear();
            adapter.notifyDataSetChanged();

            wifiManager.startScan();
            Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();
        }
    }

}
