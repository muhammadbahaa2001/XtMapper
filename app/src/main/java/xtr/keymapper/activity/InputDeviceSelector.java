package xtr.keymapper.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import xtr.keymapper.IRemoteServiceCallback;
import xtr.keymapper.KeymapConfig;
import xtr.keymapper.TouchPointer;
import xtr.keymapper.databinding.ActivityConfigureBinding;

public class InputDeviceSelector extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    // Spinner Drop down elements
    private final List<String> devices = new ArrayList<>();

    private ArrayAdapter<String> dataAdapter;
    private ActivityConfigureBinding binding;

    private KeymapConfig keymapConfig;
    private TouchPointer pointerOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConfigureBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Spinner click listener
        binding.spinner.setOnItemSelectedListener(this);

        // Creating adapter for spinner
        dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, devices);

        // attaching data adapter to spinner
        binding.spinner.setAdapter(dataAdapter);

        keymapConfig = new KeymapConfig(this);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.endButton.setOnClickListener(v -> this.finish());

        bindService(new Intent(this, TouchPointer.class), connection, BIND_AUTO_CREATE);
    }

    private void updateView(String s){
        runOnUiThread(() -> binding.textView.append(s + "\n"));
    }

    @Override
    protected void onDestroy() {
        try {
            pointerOverlay.sendSettingstoServer();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();
        binding.textView2.setText(item);
        keymapConfig.setDevice(item);
        // Showing selected spinner item
        Toast.makeText(parent.getContext(), item, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }

    private final IRemoteServiceCallback mCallback = new IRemoteServiceCallback.Stub() {
        public void onMouseEvent(int code, int value) {}
        public void receiveEvent(String event) {
            getDevices(event);
        }
        public void loadKeymap() {}
    };


    private void getDevices(String event)  {
        String[] input_event, data;
        String evdev;
        data = event.split(":"); // split a string like "/dev/input/event2: EV_REL REL_X ffffffff"
        evdev = data[0];
        input_event = data[1].split("\\s+");
        if(!input_event[1].equals("EV_SYN"))
            updateView(event);

        if( !devices.contains(evdev) && ! binding.textView2.getText().equals(evdev) )
            if (input_event[1].equals("EV_REL")) {
                devices.add(evdev);
                dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, devices);
                runOnUiThread(() -> binding.spinner.setAdapter(dataAdapter));
            }
    }
    /** Defines callbacks for service binding, passed to bindService() */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to Service, cast the IBinder and get TouchPointer instance
            TouchPointer.TouchPointerBinder binder = (TouchPointer.TouchPointerBinder) service;
            pointerOverlay = binder.getService();
            try {
                pointerOverlay.mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Log.e("serviceConnection", e.toString());
            }
        }
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };
}