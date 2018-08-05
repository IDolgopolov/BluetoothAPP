package com.arproject.bluetoothworkapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import com.example.bluetoothapp.R;

import java.io.OutputStream;

public class ActivityControl extends AppCompatActivity {
    private Button buttonDriveControl;
    private float BDCheight, BDCwidth;
    private float centerBDCheight, centerBDCwidth;
    private TextView viewResultTouch;
    private final String GO_FORWARD =  "1";
    private final String STAY =  "0";
    private final String GO_BACK = "2";
    private ConnectedThread threadCommand;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        viewResultTouch = (TextView) findViewById(R.id.view_result_touch);

        buttonDriveControl = (Button) findViewById(R.id.button_drive_control);
        final ViewTreeObserver vto = buttonDriveControl.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BDCheight = buttonDriveControl.getHeight();
                Log.i("button", BDCheight + "");
                BDCwidth = buttonDriveControl.getWidth();
                centerBDCheight = BDCheight/2;
                centerBDCwidth = BDCwidth/2;
                buttonDriveControl.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            }

        });

        buttonDriveControl.setOnTouchListener(new ControlDriveInputListener());

        threadCommand = new ConnectedThread(MainActivity.clientSocket);
        threadCommand.run();
    }


    public class ControlDriveInputListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();

            switch(motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    calculateAndSendCommand(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    calculateAndSendCommand(x, y);
                    break;
            }


            return false;
        }

        private void calculateAndSendCommand(float x, float y) {
            int quarter = identifyQuarter(x, y);
            String resultDown = "x: "+ Float.toString(x) + " y: " + Float.toString(y)
                    + " qr: " + Integer.toString(quarter) + "\n"
                    + "height: " + centerBDCheight + " width: " + centerBDCwidth;
            viewResultTouch.setText(resultDown);
            int speed = speedCalculation(centerBDCheight - y);
            if(speed > 0) {
                threadCommand.sendCommand(Integer.toString(speed), GO_FORWARD);
            } else if (speed < 0) {
                threadCommand.sendCommand(Integer.toString(speed), GO_BACK);
            } else {
                threadCommand.sendCommand(Integer.toString(speed), STAY);
            }
        }

        private int identifyQuarter(float x, float y) {
            if(x > centerBDCwidth && y > centerBDCheight) {
            return 4;
              } else if (x < centerBDCwidth && y >centerBDCheight) {
                return 3;
                } else if (x < centerBDCwidth && y < centerBDCheight) {
                return 2;
                 } else if (x > centerBDCwidth && y < centerBDCheight) {
                return 1;
            }
            return 0;
        }

        private int speedCalculation(float deviation) {
            float coefficient = 255/(BDCheight/2);
            int speed = Math.round(deviation * coefficient);
            return speed;
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket btSocket) {
            this.socket = btSocket;
            OutputStream os = null;
            try {
                os = socket.getOutputStream();
            } catch(Exception e) {}
            outputStream = os;
        }

        public void run() {

        }

        public void sendCommand(String command, String direction) {
            byte[] commandArray = command.getBytes();
            byte[] directionArray = direction.getBytes();
            try {
                outputStream.write(commandArray);
                outputStream.write(directionArray);
            } catch(Exception e) {}
        }

    }


}