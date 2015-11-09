package com.akarthik10.locationreporter;

import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.text.format.Time;
import android.util.Log;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


public class LocationReporterActivity extends ActionBarActivity {

	Button startButton, stopButton;
	ServiceReceiver serviceReceiver;
	boolean auto_scroll = true;
	String deviceName = "";
	SharedPreferences prefs;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_reporter);
        
        prefs = this.getSharedPreferences(
        	      "settings", Context.MODE_PRIVATE);
        deviceName = prefs.getString("DEVICE_NAME", "");
        if(deviceName == "")
        {
        	promptDeviceName();
        }
        
        
        startButton = (Button) findViewById(R.id.startbutton);
        stopButton = (Button) findViewById(R.id.stopbutton);
        
		serviceReceiver = new ServiceReceiver();
		IntentFilter intentFilter = new IntentFilter();
	    intentFilter.addAction(LocationReporterService.LOGGING_ACTION);
		registerReceiver(serviceReceiver, intentFilter);
		
		
        startButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Toast.makeText(LocationReporterActivity.this, "Clicked button start", Toast.LENGTH_SHORT).show();

				Intent LocRepSvc = new Intent(LocationReporterActivity.this, LocationReporterService.class);
				LocRepSvc.putExtra("DEVICE_NAME", deviceName);
				startService(LocRepSvc);
//				startButton.setEnabled(false);
//				stopButton.setEnabled(true);
				appendLog("Service started");
			}
		});
        
        
        stopButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Toast.makeText(LocationReporterActivity.this, "Clicked button stop", Toast.LENGTH_SHORT).show();
				Intent LocRepSvc = new Intent(LocationReporterActivity.this, LocationReporterService.class);
				stopService(LocRepSvc);
//				startButton.setEnabled(true);
//				stopButton.setEnabled(false);
				appendLog("Service stopped");
			}
		});


        
        appendLog("Application Started");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.location_reporter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        else if( id == R.id.auto_scroll_checkbox)
        {
        	if(item.isCheckable() && item.isChecked())
        	{
        		auto_scroll = false;
        		item.setChecked(false);
        		Toast.makeText(LocationReporterActivity.this, "Autoscroll disabled", Toast.LENGTH_SHORT).show();
        		
        	}
        	else
        	{
        		auto_scroll = true;
        		item.setChecked(true);
        		Toast.makeText(LocationReporterActivity.this, "Autoscroll enabled", Toast.LENGTH_SHORT).show();
        	}
        }
        else if(id == R.id.device_name)
        {
        	promptDeviceName();
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
       Intent setIntent = new Intent(Intent.ACTION_MAIN);
       setIntent.addCategory(Intent.CATEGORY_HOME);
       setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       startActivity(setIntent);
    }
    
    
    
    public void appendLog(String s)
    {
    	Time now = new Time();
    	now.setToNow();
    	TextView logWindow = (TextView) findViewById(R.id.textView1);
    	logWindow.append("\n["+now.format("%l:%M:%S")+"] "+s);
    }
    
    
    private class ServiceReceiver extends BroadcastReceiver{
    	 
    	 @Override
    	 public void onReceive(Context arg0, Intent arg1) {
    	  // TODO Auto-generated method stub
    	  
    	  String datapassed = arg1.getStringExtra("DATA");
    	  appendLog(datapassed);
    	  if(auto_scroll)
    	  {
    		  ScrollView scrollView1 = (ScrollView) findViewById(R.id.scrollView1);
    		  scrollView1.fullScroll(ScrollView.FOCUS_DOWN);
    	  }
    	  
    	 }
    	 
    }
    
    void promptDeviceName()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Enter location tracking name");

    	// Set up the input
    	final EditText input = new EditText(this);
    	// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
    	input.setInputType(InputType.TYPE_CLASS_TEXT);
    	input.setText(deviceName);
    	builder.setView(input);

    	// Set up the buttons
    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	       
    	        if(!input.getText().toString().matches("[a-zA-Z0-9 ]+") && input.getText().toString().trim() != "")
    	        {
    	        	dialog.cancel();
    	        	Toast.makeText(LocationReporterActivity.this, "Invalid tracking name", Toast.LENGTH_LONG).show();
    	        	promptDeviceName();
    	        	return;
    	        }
    	        deviceName = input.getText().toString();
    	        prefs.edit().putString("DEVICE_NAME", deviceName).commit();
    	        Toast.makeText(LocationReporterActivity.this, "Device name changed to "+ deviceName, Toast.LENGTH_SHORT).show();
    	        startButton.performClick();
    	        
    	    }
    	});
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	        dialog.cancel();
    	        if(deviceName == ""){
    	        	 Toast.makeText(LocationReporterActivity.this, "Tracking name is empty! Please provide a name.", Toast.LENGTH_SHORT).show();
    	        }
    	    }
    	});

    	builder.show();
    }
    

}
