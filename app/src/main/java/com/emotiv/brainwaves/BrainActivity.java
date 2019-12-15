package com.emotiv.brainwaves;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.emotiv.mentalcommand.R;

import com.emotiv.sdk.*;
import com.emotiv.bluetooth.*;

public class BrainActivity extends Activity {

    private Thread processingThread;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 0;
    private BluetoothAdapter mBluetoothAdapter;

    private boolean lock = false;
    private boolean isEnablGetData = false;
    private boolean isEnableWriteFile = false;

    int userId;

    private SWIGTYPE_p_void handleEvent;
    private SWIGTYPE_p_void emoState;

    private BufferedWriter bp_writer;
    Button Start_button,Stop_button;
    IEE_DataChannel_t[] Channel_list;
    String[] Name_Channel = {"AF3","T7","Pz","T8","AF4"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brain);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_BLUETOOTH);
            }
            else{
                checkConnect();
            }
        }
        else {
            checkConnect();
        }

        Start_button = (Button)findViewById(R.id.startbutton);
        Stop_button  = (Button)findViewById(R.id.stopbutton);

        Start_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.e("Ondas Cerebrales","Comenzando a llenar archivo");
                setDataFile();
                isEnableWriteFile = true;
            }
        });

        Stop_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.e("Ondas Cerebrales","Deteniendo el llenado del archivo");
                StopWriteFile();
                isEnableWriteFile = false;
            }
        });

        Emotiv.IEE_EmoInitDevice(this);
        edkJava.IEE_EngineConnect("Emotiv Systems-5");
        IEE_DataChannel_t[] ChannelListTmp = {IEE_DataChannel_t.IED_AF3, IEE_DataChannel_t.IED_T7,IEE_DataChannel_t.IED_Pz,
                IEE_DataChannel_t.IED_T8,IEE_DataChannel_t.IED_AF4};
        Channel_list = ChannelListTmp;
        handleEvent = edkJava.IEE_EmoEngineEventCreate();
        emoState = edkJava.IEE_EmoStateCreate();

        processingThread=new Thread() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                super.run();
                while(true)
                {
                    try
                    {
                        handler.sendEmptyMessage(0);
                        handler.sendEmptyMessage(1);
                        if(isEnablGetData && isEnableWriteFile)handler.sendEmptyMessage(2);
                        Thread.sleep(5);
                    }

                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        };
        processingThread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_BLUETOOTH: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkConnect();

                } else {

                    Toast.makeText(this, "La aplicación no puede funcionar si no le ortorgas los permisos", Toast.LENGTH_SHORT).show();
                }
                return;
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK){
                //Connect to emoEngine
                //edkJava.IEE_EngineConnect("");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Necesitas activar el bluetooth para conectarte con el dispotivo Emotiv"
                        , Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case 0: {
                    int state = edkJava.IEE_EngineGetNextEvent(handleEvent);
                    if (state == edkJava.EDK_OK) {
                        IEE_Event_t eventType = edkJava.IEE_EmoEngineEventGetType(handleEvent);

                        SWIGTYPE_p_unsigned_int pEngineId = edkJava.new_uint_p();
                        int result = edkJava.IEE_EmoEngineEventGetUserId(handleEvent, pEngineId);
                        int tmpUserId = (int) edkJava.uint_p_value(pEngineId);
                        edkJava.delete_uint_p(pEngineId);
                        userId = tmpUserId;
                        switch (eventType) {
                            case IEE_UserAdded: {
                                Log.e("SDK", "Usuario añadido");
                                edkJava.IEE_FFTSetWindowingType(userId, IEE_WindowingTypes.IEE_BLACKMAN);
                                isEnablGetData = true;
                            }
                            break;
                            case IEE_UserRemoved: {
                                Log.e("SDK", "Usuario removido");
                                isEnablGetData = false;
                            }
                            break;
                        }
                    }
                    break;
                }
                case 1:
                 {
                    /*Connect device with Insight headset*/
                    int number = Emotiv.IEE_GetInsightDeviceCount();
                    if (number != 0) {
                        if (!lock) {
                            lock = true;
                            Emotiv.IEE_ConnectInsightDevice(0);
                        }
                    } else {
                        number = Emotiv.IEE_GetEpocPlusDeviceCount();
                        if (number != 0) {
                            if (!lock) {
                                lock = true;
                                Emotiv.IEE_ConnectEpocPlusDevice(0, false);
                            } else lock = false;
                        }
                    }
                }
                break;
                case 2: {
                    if (bp_writer == null) return;
                    for (int i = 0; i < Channel_list.length; i++) {
                        SWIGTYPE_p_double ptheta = edkJava.new_double_p();
                        SWIGTYPE_p_double palpha = edkJava.new_double_p();
                        SWIGTYPE_p_double plow_beta = edkJava.new_double_p();
                        SWIGTYPE_p_double phigh_beta = edkJava.new_double_p();
                        SWIGTYPE_p_double pgamma = edkJava.new_double_p();
                        int result = -1;
                        result = edkJava.IEE_GetAverageBandPowers(userId, Channel_list[i], ptheta, palpha, plow_beta, phigh_beta, pgamma);
                        if (result == edkJava.EDK_OK) {
                            Log.e("EEG", "Llenando la data EEG");
                            try {
                                bp_writer.write(Name_Channel[i] + ",");
                                addData(edkJava.double_p_value(ptheta));
                                addData(edkJava.double_p_value(palpha));
                                addData(edkJava.double_p_value(plow_beta));
                                addData(edkJava.double_p_value(phigh_beta));
                                addData(edkJava.double_p_value(pgamma));
                                bp_writer.newLine();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        edkJava.delete_double_p(ptheta);
                        edkJava.delete_double_p(palpha);
                        edkJava.delete_double_p(plow_beta);
                        edkJava.delete_double_p(phigh_beta);
                        edkJava.delete_double_p(pgamma);
                    }
                }
                break;
            }

        }

    };

    private void setDataFile() {
        try {
            String eeg_header = "Channel , Theta ,Alpha ,Low beta ,High beta , Gamma ";
            File root = Environment.getExternalStorageDirectory();
            String file_path = root.getAbsolutePath()+ "/OndasCerebrales/";
            File folder=new File(file_path);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if(!folder.exists())
                {
                    folder.mkdirs();
                }
                bp_writer = new BufferedWriter(new FileWriter(file_path+"brain.csv"));
                bp_writer.write(eeg_header);
                bp_writer.newLine();
            } else {
                // Petición de permiso para el usuario
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }

        } catch (Exception e) {
            Log.e("","Exception"+ e.getMessage());
        }
    }

    private void StopWriteFile(){
        try {
            bp_writer.flush();
            bp_writer.close();
            bp_writer = null;
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public void addData(double data) {

        if (bp_writer == null) {
            return;
        }

        String input = "";
        input += (String.valueOf(data) + ",");
        try {
            bp_writer.write(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void checkConnect(){
        if (!mBluetoothAdapter.isEnabled()) {
            /*Se requiere activar el Bluetooth*/
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {

        }
    }


}

