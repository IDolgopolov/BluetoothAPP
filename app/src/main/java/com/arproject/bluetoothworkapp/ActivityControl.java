package com.arproject.bluetoothworkapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import com.example.bluetoothapp.R;

import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class ActivityControl extends AppCompatActivity {
    private Button buttonDriveControl;
    private float BDCheight, BDCwidth;
    private float centerBDCheight, centerBDCwidth;
    private TextView viewResultTouch;
    private String angle = "90"; //0, 30, 60, 90, 120, 150, 180
    private ConnectedThread threadCommand;
    private long lastTimeSendCommand = System.currentTimeMillis();
    private Button bLowHit, bHighHit;
    private Handler handler;
    private static final String LOW_HIT = "/8/";
    private static final String HIGH_HIT = "/9/";



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        threadCommand = new ConnectedThread(MainActivity.clientSocket);
        threadCommand.run();

        buttonDriveControl = findViewById(R.id.button_drive_control);
        final ViewTreeObserver vto = buttonDriveControl.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BDCheight = buttonDriveControl.getHeight();
                BDCwidth = buttonDriveControl.getWidth();
                centerBDCheight = BDCheight/2;
                centerBDCwidth = BDCwidth/2;
                buttonDriveControl.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }

        });

        bHighHit = findViewById(R.id.b_high_hit);
        bHighHit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                threadCommand.sendHit(HIGH_HIT);
            }
        });
        bLowHit = findViewById(R.id.b_low_hit);
        bLowHit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                threadCommand.sendHit(HIGH_HIT);
            }
        });
        viewResultTouch = findViewById(R.id.view_result_touch);
        viewResultTouch.setText("null");

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                viewResultTouch.setText(msg.obj.toString());
            }
        };

        buttonDriveControl.setOnTouchListener(new ControlDriveInputListener());

    }


    public class ControlDriveInputListener implements View.OnTouchListener {
        private Timer timer;

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            final float x = motionEvent.getX();
            final float y = motionEvent.getY();

            switch(motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                                calculateAndSendCommand(x, y);
                        }
                    }, 0, 10);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            calculateAndSendCommand(x, y);
                        }
                    }, 0, 10);
                    break;
                case MotionEvent.ACTION_UP:
                    if(timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                    break;
            }


            return false;
        }

        private void calculateAndSendCommand(float x, float y) {
            int quarter = identifyQuarter(x, y);
            int speed = speedCalculation(centerBDCheight - y);
            String angle = angleCalculation(x);

            String resultDown = "x: "+ Float.toString(x) + " y: " + Float.toString(y)
                    + " qr: " + Integer.toString(quarter) + "\n"
                    + "height: " + centerBDCheight + " width: " + centerBDCwidth + "\n"
                    + "speed: " + Integer.toString(speed) + " angle: " + angle;
            handler.sendMessage(handler.obtainMessage(1, resultDown));

            if((System.currentTimeMillis() - lastTimeSendCommand) > 100) {
                threadCommand.sendCommand(Integer.toString(speed), angle);
                lastTimeSendCommand = System.currentTimeMillis();
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
            if(speed > 0 && speed < 70) speed = 0;
            if(speed < 0 && speed > - 70)  speed = 0;
            if(speed < 120 && speed > 70) speed = 120;
            if(speed > -120 && speed < -70) speed = -120;
            if(speed > 255 ) speed = 255;
            if(speed < - 255) speed = -255;
            return speed;
        }

        private String angleCalculation(float x) {
            if(x < BDCwidth/6) {
                angle = "0";
            } else if (x > BDCwidth/6 && x < BDCwidth/3) {
                angle = "30";
            } else if (x > BDCwidth/3 && x < BDCwidth/2) {
                angle = "60";
            } else if (x > BDCwidth/2 && x < BDCwidth/3*2) {
                angle = "120";
            } else if (x > BDCwidth/3*2 && x < BDCwidth/6*5) {
                angle = "150";
            } else if (x > BDCwidth/6*5 && x < BDCwidth) {
                angle = "180";
            } else {
                angle = "90";
            }
            return angle;
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

        public void sendCommand(String speed, String angle) {
            byte[] speedArray = speed.getBytes();
            byte[] angleArray = angle.getBytes();
            String a = "#";
            String b = "@";
            String c = "*";

            try {
                outputStream.write(b.getBytes());
                outputStream.write(speedArray);
                outputStream.write(a.getBytes());

                outputStream.write(c.getBytes());
                outputStream.write(angleArray);
                outputStream.write(a.getBytes());
            } catch(Exception e) {}
        }

        public void sendHit(String hit) {
            byte[] bHit = hit.getBytes();
            try {
                outputStream.write(bHit);
            }catch (Exception e) {e.printStackTrace(); }
        }

    }


}
