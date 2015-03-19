/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
 */

package com.example.ant_simple_test;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc.ICalculatedCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc.CalculatedSpeedReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;

import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * Base class to connects to Heart Rate Plugin and display all the event data.
 */
public abstract class Activity_DisplayBase extends Activity
{
	private static final String TAG = Activity_DisplayBase.class.getSimpleName();
	
    protected abstract void requestAccessToPcc();

    AntPlusBikeSpeedDistancePcc antPcc = null;
    AntPlusBikeCadencePcc bcPcc = null;
    protected PccReleaseHandle<AntPlusBikeSpeedDistancePcc> releaseHandle = null;
    protected PccReleaseHandle<AntPlusBikeCadencePcc> bcReleaseHandle = null;

    TextView tv_status;
    TextView tv_value;
    TextView tv_value_2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handleReset();
    }

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */
    protected void handleReset()
    {
        //Release the old access if it exists
        if(releaseHandle != null)
        {
            releaseHandle.close();
        }
        if(bcReleaseHandle != null)
        {
        	bcReleaseHandle.close();
        }

        requestAccessToPcc();
    }

    protected void showDataDisplay(String status)
    {
        setContentView(R.layout.activity_display);

        tv_status = (TextView)findViewById(R.id.textView_Status);
        tv_value = (TextView)findViewById(R.id.textView_Value);
        tv_value_2 = (TextView)findViewById(R.id.textView_Value2);
        
        tv_status.setText(status);
        tv_value.setText("--");
        tv_value_2.setText("--");
    }

    /**
     * Switches the active view to the data display and subscribes to all the data events
     */
    public void subscribeToHrEvents()
    {
    	// 2.095m circumference = an average 700cx23mm road tire
    	antPcc.subscribeCalculatedSpeedEvent(new CalculatedSpeedReceiver(new BigDecimal(2.095))
        {
            @Override
            public void onNewCalculatedSpeed(final long estTimestamp,
                final EnumSet<EventFlag> eventFlags, final BigDecimal calculatedSpeed)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                    	Log.d(TAG, "calculatedSpeed : " + calculatedSpeed);
                        tv_value.setText(String.valueOf(calculatedSpeed));
                    }
                });
            }
        });
    	
    	Log.d(TAG, "antPcc.isSpeedAndCadenceCombinedSensor()" + antPcc.isSpeedAndCadenceCombinedSensor());
    	
    	if(antPcc.isSpeedAndCadenceCombinedSensor())
    	{
    		runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                	bcReleaseHandle = AntPlusBikeCadencePcc.requestAccess(
                        Activity_DisplayBase.this,
                        antPcc.getAntDeviceNumber(), 
                        0, 
                        true,
                        new IPluginAccessResultReceiver<AntPlusBikeCadencePcc>()
                        {
                            // Handle the result, connecting to events
                            // on success or reporting failure to user.
                            @Override
                            public void onResultReceived(AntPlusBikeCadencePcc result,
                                RequestAccessResult resultCode,
                                DeviceState initialDeviceStateCode)
                            {
                                switch (resultCode)
                                {
                                    case SUCCESS:
                                        bcPcc = result;
                                        bcPcc.subscribeCalculatedCadenceEvent(new ICalculatedCadenceReceiver()
                                            {
                                                @Override
                                                public void onNewCalculatedCadence(
                                                    long estTimestamp,
                                                    EnumSet<EventFlag> eventFlags,
                                                    final BigDecimal calculatedCadence)
                                                {
                                                    runOnUiThread(new Runnable()
                                                    {
                                                        @Override
                                                        public void run()
                                                        {
                                                        	Log.d(TAG, "calculatedCadence : " + calculatedCadence);
                                                            tv_value_2.setText(String.valueOf(calculatedCadence));
                                                        }
                                                    });
                                                }
                                            });
                                        break;
                                    case CHANNEL_NOT_AVAILABLE:
                                        break;
                                    case BAD_PARAMS:
                                        break;
                                    case OTHER_FAILURE:
                                        break;
                                    case DEPENDENCY_NOT_INSTALLED:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        },
                        // Receives state changes and shows it on the
                        // status display line
                        new IDeviceStateChangeReceiver()
                        {
                            @Override
                            public void onDeviceStateChange(final DeviceState newDeviceState)
                            {
                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        Log.d(TAG, "onDeviceStateChange : " + newDeviceState);
                                    }
                                });

                            }
                        });
                }
            });
    	}
    }

    protected IPluginAccessResultReceiver<AntPlusBikeSpeedDistancePcc> base_IPluginAccessResultReceiver =
        new IPluginAccessResultReceiver<AntPlusBikeSpeedDistancePcc>()
        {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(AntPlusBikeSpeedDistancePcc result, RequestAccessResult resultCode, DeviceState initialDeviceState)
        {
            showDataDisplay("Connecting...");
            switch(resultCode)
            {
                case SUCCESS:
                    antPcc = result;
                    tv_status.setText(result.getDeviceName() + ": " + initialDeviceState);
                    subscribeToHrEvents();
                    break;
                case CHANNEL_NOT_AVAILABLE:
                    Toast.makeText(Activity_DisplayBase.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case ADAPTER_NOT_DETECTED:
                    Toast.makeText(Activity_DisplayBase.this, "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case BAD_PARAMS:
                    //Note: Since we compose all the params ourself, we should never see this result
                    Toast.makeText(Activity_DisplayBase.this, "Bad request parameters.", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case OTHER_FAILURE:
                    Toast.makeText(Activity_DisplayBase.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case DEPENDENCY_NOT_INSTALLED:
                    tv_status.setText("Error. Do Menu->Reset.");
                    AlertDialog.Builder adlgBldr = new AlertDialog.Builder(Activity_DisplayBase.this);
                    adlgBldr.setTitle("Missing Dependency");
                    adlgBldr.setMessage("The required service\n\"" + AntPluginPcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                    adlgBldr.setCancelable(true);
                    adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent startStore = null;
                            startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPluginPcc.getMissingDependencyPackageName()));
                            startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            Activity_DisplayBase.this.startActivity(startStore);
                        }
                    });
                    adlgBldr.setNegativeButton("Cancel", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    final AlertDialog waitDialog = adlgBldr.create();
                    waitDialog.show();
                    break;
                case USER_CANCELLED:
                    tv_status.setText("Cancelled. Do Menu->Reset.");
                    break;
                case UNRECOGNIZED:
                    Toast.makeText(Activity_DisplayBase.this,
                        "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                        Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                default:
                    Toast.makeText(Activity_DisplayBase.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
            }
        }
        };

        //Receives state changes and shows it on the status display line
        protected  IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new IDeviceStateChangeReceiver()
        {
            @Override
            public void onDeviceStateChange(final DeviceState newDeviceState)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tv_status.setText(antPcc.getDeviceName() + ": " + newDeviceState);
                    }
                });


            }
        };

        @Override
        protected void onDestroy()
        {
            if(releaseHandle != null)
            {
                releaseHandle.close();
            }
            if(bcReleaseHandle != null)
            {
            	bcReleaseHandle.close();
            }
            super.onDestroy();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu)
        {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_display, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            switch(item.getItemId())
            {
                case R.id.menu_reset:
                    handleReset();
                    tv_status.setText("Resetting...");
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }
}
