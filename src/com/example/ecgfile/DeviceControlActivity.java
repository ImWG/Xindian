/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.ecgfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomControls;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
@SuppressLint({ "NewApi", "ResourceAsColor" })
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceAddress;
    private ToggleButton mButton_start, mButton_save, mButton_remoteUpload_enable;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false, firstConnect = true;
    private BluetoothGattCharacteristic mNotifyCharacteristic,mNCharW=null;
    private Handler mHandler;
    StringBuilder sBuilder = new StringBuilder();
    private Timer mTimer = null, mTimer2 = null;
    private TimerTask mTimerTask = null, mTimerTask2 = null;
    
    //�ļ��ϴ�����
    private File[] currentFiles;                                        // �ļ��а������ļ�
    private String srcPath ;
    String measure_time = "";
    int judge = -1;                                                     // ������json����
    Map<String, File> files = new HashMap<String, File>();              // ������ļ��б�
    List<String> uploadCache = new ArrayList<String>();     			// �Ѿ��ϴ����ļ�
    String URL_FileUpload = CommonVar.URL_FileUpload;                   // �ϴ���ַ
    boolean isRealTimeUp = false;                   // ʵʱ�ϴ���־ 
    boolean isFirst = true;                         // �ϴ��ĵ�һ���ļ�
    
    //ecg��ͼ
    public static String intentData = null;         // �ĵ������ַ���
    private SurfaceView mSurfaceView_ECG;
    private Paint mPaint;
    ZoomControls zoomX, zoomY;
    String dataTmp = "1000";
    private boolean isRecording = true;              // ��ͼ�߳̿��Ʊ��
    private boolean acceptData = true;               // �Ƿ�ȡ��stringbuilder�е���
    private int wait =100;                           // ���ݻ���ʱ��
    private int rateX = 1;                           // X����С�ı���
    private int rateY = 2;                           // Y����С�ı���
    private int baseLine = 500;                  	 // Y�����
    private int rateXmin = 1;
    private int rateXmax = 5;
    private int rateYmin = 2;
    private int rateYmax = 5;
    
    //��¼�¼��ĵ�ǰʱ��
    String timeString = "";
    
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /*Handles various events fired by the Service.
    * ACTION_GATT_CONNECTED: connected to a GATT server.
    * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    * ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    * ACTION_DATA_AVAILABLE: received data from the device. This can be a result of read or notification operations.
    **/
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mButton_start.setClickable(true);				
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mButton_start.setClickable(false);
                mButton_save.setClickable(false);
                mButton_remoteUpload_enable.setClickable(false);
                StopDraw();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                getGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                intentData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA); 
                System.out.println("intentdata:"+intentData);
               
                sBuilder.append(intentData);
                if(!timeString.equals("")){     //ʱ���ַ����ǿ�ʱ�������ݽ��б���
                    try {
                        if(isRealTimeUp){
                            continueSaveFile(timeString, intentData);
                        }else{
                            if(mButton_save.isChecked()){
                                save2file(timeString,intentData);
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
    
    //ʱ���� Timer�����������ļ������ʱ��--15���½�һ���ļ�
    public void TimerTask(){
        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimerTask = new TimerTask(){
            @Override
            public void run() {
                if(!mButton_remoteUpload_enable.isChecked()){
                    cleanTimerTask();
                }
                SimpleDateFormat formatter = new SimpleDateFormat("/yyyy-MM-dd HH:mm:ss");
                Date curDate = new Date(System.currentTimeMillis());
                timeString = formatter.format(curDate);
            }
        };
        mTimer.schedule(mTimerTask, 0, CommonVar.PERIOD_15);
    }

    //ֹͣѭ��ʱ��
    private void cleanTimerTask() {
        if(mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if(mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }
    
    //�ļ��ϴ� Timer --�����ļ��ϴ���ʱ����
    public void RTUploadTask(){
        if (mTimer2 == null) {
            mTimer2 = new Timer();
        }
        mTimerTask2 = new TimerTask(){
            @Override
            public void run() {
                if(!isRealTimeUp || !mButton_remoteUpload_enable.isChecked()){
                    cleanRTUploadTask();
                }
                //��ȡҪ�ϴ����ļ�
                getFile(CommonVar.ecgFilePathTemp);
                //ִ���ϴ�
                if(files.size() >=1){
                    new postFiles().execute();
                }
            }
        };
        mTimer2.schedule(mTimerTask2, 15*1000, 8*1000);    //15�뿪ʼ��8��һ��
    }
    //ֹͣ�ļ��ϴ�ѭ��
    private void cleanRTUploadTask() {
        if(mTimerTask2 != null) {
            mTimerTask2.cancel();
            mTimerTask2 = null;
        }
        if(mTimer2 != null) {
            mTimer2.cancel();
            mTimer2.purge();
            mTimer2 = null;
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.gatt_services_characteristics);
        if(getRequestedOrientation()!=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        init_ecg();
          
        mDeviceAddress = getIntent().getStringExtra(EXTRAS_DEVICE_ADDRESS);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);        
    }
    
    public void init_ecg(){
    	setTitle("�ĵ����");
        mSurfaceView_ECG = (SurfaceView)findViewById(R.id.surfaceView_ecg); //����
        mSurfaceView_ECG.setOnTouchListener(new TouchEvent());
        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);                                       // ����Ϊ��ɫ
        mPaint.setStrokeWidth(2);                                           // ���û��ʴ�ϸ
        
        zoomX = (ZoomControls)this.findViewById(R.id.zoomControls_x);
        MyZoomControls.convert(zoomX);
        zoomX.setOnZoomOutClickListener(new View.OnClickListener() {      	// ��С      
            @Override
            public void onClick(View v) {
                if(rateX > rateXmin){
                    rateX--;
                    zoomX.setIsZoomInEnabled(true);
                }else{
                	zoomX.setIsZoomOutEnabled(false);
                }
            }
        });
        zoomX.setOnZoomInClickListener(new View.OnClickListener() {			// �Ŵ�
            
            @Override
            public void onClick(View v) {
                if(rateX < rateXmax){
                    rateX++;
                    zoomX.setIsZoomOutEnabled(true);
                }else{
                	zoomX.setIsZoomInEnabled(false);
                }
            }
        });
        zoomY = (ZoomControls)this.findViewById(R.id.zoomControls_y);
        MyZoomControls.convert(zoomY);
        zoomY.setOnZoomInClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if(rateY > rateYmin){
                    rateY--;
                    zoomY.setIsZoomOutEnabled(true);
                }else{
                	zoomY.setIsZoomInEnabled(false);
                }
            }
        });
        zoomY.setOnZoomOutClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if(rateY < rateYmax){
                    rateY++;
                    zoomY.setIsZoomInEnabled(true);
                }else{
                	zoomY.setIsZoomOutEnabled(false);
                }
            }
        });
       
        mHandler = new Handler();        
        mButton_start = (ToggleButton)findViewById(R.id.bt_start);
        mButton_save = (ToggleButton)findViewById(R.id.bt_sava);
        mButton_remoteUpload_enable = (ToggleButton)findViewById(R.id.bt_realTime);
        
        //��ʼ����ͣ�¼�
        mButton_start.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View view) {
                //���͸��ĵ��豸�Ŀ�ʼ֪ͨ
                if(firstConnect){
                    String data = "1";
                    if(mNCharW == null){
                        Toast.makeText(getApplication(),"����ʧ��", Toast.LENGTH_SHORT).show();
                        DeviceControlActivity.this.finish();
                        return ;
                    }
                    mNCharW.setValue(data.getBytes());
                    mBluetoothLeService.writeCharacteristic(mNCharW);
                    //�򿪴������ݽ��յ�ʹ��λ
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                    firstConnect = false;
                    StartDraw();                    //��ʼ��ͼ
                    mButton_save.setClickable(true);
                    mButton_remoteUpload_enable.setClickable(true);
                    return ;
                }
                
                if(mButton_start.isChecked()){      //��������
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                    mButton_remoteUpload_enable.setClickable(true);                    
                    mButton_save.setClickable(true);
                    StartDraw();                    //ֹͣ��ͼ
                }else{                              //ֹͣ����
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                    mButton_remoteUpload_enable.setClickable(false);
                    mButton_save.setClickable(false);                    
                    mButton_remoteUpload_enable.setChecked(false);
                    StopDraw();
                }
            }
        });  
        
        // �����¼�,����ִ��15��       
        mButton_save.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View v) {
                if(intentData == null && mButton_save.isChecked()){
                    Toast.makeText(DeviceControlActivity.this, "������", Toast.LENGTH_SHORT).show();
                    return ;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        timeString = "";
                        mButton_save.setClickable(true);
                        mButton_start.setClickable(true);
                        mButton_remoteUpload_enable.setClickable(true);                       
                        mButton_save.setChecked(false);
                    }
                }, CommonVar.PERIOD_15);
                SimpleDateFormat formatter = new SimpleDateFormat("/yyyy-MM-dd HH:mm:ss");     
                Date curDate = new Date(System.currentTimeMillis());
                timeString = formatter.format(curDate);
                //����¼�ʹ��
                mButton_start.setClickable(false);
                mButton_save.setClickable(false);
                mButton_remoteUpload_enable.setClickable(false);                
                Toast.makeText(DeviceControlActivity.this, 
                        "���ڱ�������ڿ�ʼ��15��֮�ڵ�����", Toast.LENGTH_SHORT).show();
            }
        });
        mButton_save.setClickable(false);
        
        //ʵʱ�ϴ��¼�
        mButton_remoteUpload_enable.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View view){
                if(mButton_remoteUpload_enable.isChecked()){
                    //���水ť��Ч
                    mButton_save.setChecked(false);
                    mButton_save.setClickable(false);
                    //����ʵʱ��־
                    isRealTimeUp = true;                    
                    //�ж��Ƿ�������
                    if(NetConnect.checkNet(getApplication())){
                        //�ж��Ƿ��wifi
                        NetConnect.checkWifi(DeviceControlActivity.this);
                        //����ѭ��ִ�е� �����ļ�����/ÿ15�����һ��ʱ��
                        TimerTask();
                        //�ϴ���ع���
                        RTUploadTask();
                        Toast.makeText(getApplication(), 
                                "������ʵʱ�ϴ�����", Toast.LENGTH_SHORT).show();
                    }else {
                        return ;
                    }                    
                }else{
                    if(mButton_start.isChecked()){
                        mButton_save.setClickable(true);
                    }
                    //�ر�ʵʱ��־
                    isRealTimeUp = false;
                    Toast.makeText(getApplication(), 
                            "�ѹر�ʵʱ�ϴ�����", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mButton_remoteUpload_enable.setClickable(false);
    }
    
    /**
     * ��ͼ���ֿ�ʼ
     * ================================================================================
     */
    //���������¼�
    class TouchEvent implements OnTouchListener {
        //@Override
        public boolean onTouch(View v, MotionEvent event) {
            baseLine = (int) event.getY();
            return true;
        }
    }
    
    public void StartDraw() {
        isRecording = true;
        new DrawThread().start();           // ��ʼ�����߳�   
    }

    public void StopDraw() {
        isRecording = false;
    }
    
    class DrawThread extends Thread {
        private int oldX = 0;               // �ϴλ��Ƶ�X����
        private int oldY = 0;               // �ϴλ��Ƶ�Y����
        private int X_index = 0;            // ��ǰ��ͼ������ĻX�������
        int data_len;
        String[] dataArray;
       
        public void run(){
            while (isRecording)  {
                try { 
                	if(sBuilder.toString().length() > 10 && acceptData){        // ���ﳤ��10ֻ��Ϊ�˱����Ѹ�ֵ         
                		dataArray = sBuilder.toString().split(" ");
                		sBuilder.delete(0, sBuilder.length());                  // ���stringbuilder
                        data_len = dataArray.length;
                        SimpleDraw(X_index, dataArray, rateX, rateY, baseLine); // �ѻ��������ݻ�����                       
                        X_index = X_index + data_len;
                        if (X_index > mSurfaceView_ECG.getWidth()) {
                            X_index = 0;                                        // ������Ļ��ԭ���ػ�
                        }                                                                                          
                        Thread.sleep(wait);                                     // ��ʱһ��ʱ�仺������
                        acceptData = true;                                      // ���ջ�������ʹ��
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }   
        }
        
        void SimpleDraw(int start, String[] inputBuf, int rateX, int rateY, int baseLine) {
            acceptData = false;                                                             // �ݲ����ջ�������
        	if (start == 0){
        		oldX = 0;
        		ClearDraw();
        	}

            Canvas canvas = mSurfaceView_ECG.getHolder().lockCanvas(
                    new Rect(start, 0, start + data_len ,mSurfaceView_ECG.getHeight()));    // ��ȡ����
            canvas.drawColor(Color.BLACK);			                                        // �������
            for (int i = 0; i < data_len; i=i+rateX) {                                      // �ж��ٻ�����
                int x = i + start;
                int y = baseLine - Integer.parseInt(inputBuf[i])/rateY;                     // ������С���������ڻ�׼��
                if(i > 0){                                                                  // �ӵڶ����㿪ʼ����ԭ�㲻������
                	canvas.drawLine(oldX, oldY, x, y, mPaint);
                }
                oldX = x;
                oldY = y;
            }
            mSurfaceView_ECG.getHolder().unlockCanvasAndPost(canvas);                       // �����������ύ���õ�ͼ��           
        }
        
        // �����������
        void ClearDraw() {
            Canvas canvas = mSurfaceView_ECG.getHolder().lockCanvas(null);
            canvas.drawColor(Color.BLACK);
            mSurfaceView_ECG.getHolder().unlockCanvasAndPost(canvas);
        }
    }
    
    /**
     * ��ͼ���ֽ���
     * ================================================================================
     */
    
    //�������ݵ��ļ�
    public void save2file(String timeString, String dataString) throws FileNotFoundException{       
        String path = CommonVar.ecgFilePath;
        Log.d(CommonVar.TAG,"sean "+path);
        File filePath = new File(path);
        if(!filePath.exists()){
            Log.d(CommonVar.TAG, "�ļ��в����ڣ�����");
            if(!filePath.mkdirs()){
                System.out.println("����ʧ��");
            }else{
                System.out.println("�����ɹ�");
            }
        }        
        File file=new File( filePath + timeString+".txt");
        try {
            FileWriter fw = new FileWriter(file,true);
            BufferedWriter bufw = new BufferedWriter(fw);              
            bufw.write(dataString);
            bufw.close();
            fw.close();
        } 
        catch (IOException e){
            e.printStackTrace();
        }        
    }
    
    //���������ļ�
    public void continueSaveFile(String timeString, String dataString) throws FileNotFoundException{      
        String path = CommonVar.ecgFilePathTemp;
        File filePath = new File(path);
        if(!filePath.exists()){
            Log.d(CommonVar.TAG, "�ļ��в����ڣ�����");
            if(!filePath.mkdirs()){
                System.out.println("����ʧ��");
            }else{
                System.out.println("�����ɹ�");
            }
        }       
        File file=new File( filePath + timeString+".txt");
        try {
            FileWriter fw = new FileWriter(file,true);
            BufferedWriter bufw = new BufferedWriter(fw);              
            bufw.write(dataString);
            bufw.close();
            fw.close();
        } 
        catch (IOException e){
            e.printStackTrace();
        }        
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        cleanRTUploadTask();
        cleanTimerTask();
        deleteFiles(CommonVar.ecgFilePathTemp);
        StopDraw();
    }

    
    /**
     * ��ȡ�ļ�����������ļ�,��ӵ����ϴ��б���
     * ��ʼ���ϴ��ļ��� ����: measure_time,files
     * @param path
     */
    private void getFile(String path){  
        File root = new File(path);
        if (!root.exists())  
            return;   
        currentFiles = root.listFiles();
        if(currentFiles.length > 1){
            sortFiles(currentFiles);
        }else{
            return;
        }
        srcPath = path+"/"+currentFiles[currentFiles.length-1].getName();        
        File fileSelect = new File(srcPath);
        String filePath = fileSelect.getName();
        String fileName = filePath.substring(filePath.length()-23);
        measure_time = fileName.substring(0, fileName.length()-4);
        if(uploadCache.contains(fileName)){
            return;
        }
        files.put(fileName, fileSelect);
        System.out.println("file:"+fileName);
    }
    
    //����ļ���
    private void deleteFiles(String path){
        File root = new File(path);
        File[] files = root.listFiles();
        if(null != files){
            for(File file: files){
                file.delete();
            }
        }
    }
    
    //�ļ����ڲ��������µ��ļ�����ǰ��
    private void sortFiles(File[] files){  
        Arrays.sort(files, new Comparator<File>(){  
            public int compare(File paramFile1, File paramFile2){  
                return (int) (paramFile2.lastModified() - paramFile1.lastModified());
            }
        });
    }
    
    /*//�ϴ��ļ�
    class postFiles extends AsyncTask<String, String, String> {
        JSONParser jsonParser = new JSONParser();
        String is_real_time = "1";
        
        @Override       
        protected void onPreExecute() {
            super.onPreExecute();
        }
        
        @Override
        protected String doInBackground(String... args) {
            Map<String, String> params = new HashMap<String, String>();
//            params.put("login_id", String.valueOf(loginParams.loginId));
            params.put("login_id", "1");
            params.put("is_real_time", is_real_time);
            params.put("measure_time", measure_time);
            
            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject json = jsonParser.postFile(URL_FileUpload, params, files);
                try {
                    judge = json.getInt("judge");
                } catch (JSONException e) {
                    e.printStackTrace();
                }                
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        *//**
         * After completing background task Dismiss the progress dialog
         * **//*
        protected void onPostExecute(String file_url) {
            // dismiss the dialog once done
 
            switch (judge) {
            case 1:              
                File fileDelete = new File(srcPath);
                String filePath = fileDelete.getName();
                String fileName = filePath.substring(filePath.length()-23);
                //���ϴ��ļ���ȥ�����ļ�
                files.remove(fileName);
                //��temp�ļ�����ɾ�����ļ�
                if(fileDelete.exists()){
                    fileDelete.delete();                  
                }                
                System.out.println("�ɹ��ϴ�:"+fileName);
                Toast.makeText(DeviceControlActivity.this, "�ɹ��ϴ�"+fileName, Toast.LENGTH_SHORT).show();
                uploadCache.add(fileName);
                break;
            case 0:
                Toast.makeText(DeviceControlActivity.this, "�ϴ�ʧ�ܣ���������", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
        }

    }   */
    
  //�ϴ��ļ�
    class postFiles extends AsyncTask<String, String, String> {

        JSONParser jsonParser = new JSONParser();
        int result = -1;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... args) {
            // TODO Auto-generated method stub 
            Map<String, String> params = new HashMap<String, String>();
            params.put("username", "user1");
            params.put("password", "user1");
            params.put("record_id", String.valueOf(MainActivity.id));
            if(isFirst){
                params.put("status", "1");
            }else{
                params.put("status", "2");
            }
            
            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject json = jsonParser.postFile(CommonVar.URL_FileUpload, params, files);
                if(null != json){
                    System.out.println(json.toString());
                    if(json.toString().contains("error\":0")){
                        result = 0;
                    }else{
                        result = 1;
                    }
                }
//                    result = json.getInt("error");                   
                
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) { 
            switch (result) {
            case 0:
                File fileDelete = new File(srcPath);
                String filePath = fileDelete.getName();
                String fileName = filePath.substring(filePath.length()-23);
                //���ϴ��ļ���ȥ�����ļ�
                files.remove(fileName);
                //��temp�ļ�����ɾ�����ļ�
                if(fileDelete.exists()){
                    fileDelete.delete();                  
                }                
//                System.out.println("�ɹ��ϴ�:"+fileName);
                Toast.makeText(DeviceControlActivity.this, "�ɹ��ϴ�"+fileName, Toast.LENGTH_SHORT).show();
                uploadCache.add(fileName);
                isFirst = false;
                break;
            case 1:
                Toast.makeText(getApplicationContext(), "�ϴ�ʧ��", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
        }
    }
    
    /*
     * �����������ֻ�ǽ���λ������Ӧ uuid �� services �� characteristic ���ݳ���
     * ʹ Button ��Ӧ��ȷ���¼�
     */
    @SuppressLint("SimpleDateFormat") 
    private void getGattServices(List<BluetoothGattService> gattServices) {
        System.out.println("����getGattServices");
        if (gattServices == null){
            System.out.println("getGattServices null ����");
            return;
        }
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if(uuid.equals("0000ffe0-0000-1000-8000-00805f9b34fb")
                    || uuid.equals("0000ffe5-0000-1000-8000-00805f9b34fb")){	// ֻ��ȡ��2��uuid service
                
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
    
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    uuid = gattCharacteristic.getUuid().toString();
                    if(uuid.equals("0000ffe4-0000-1000-8000-00805f9b34fb")){
                        mNotifyCharacteristic = gattCharacteristic;
                        Toast.makeText(getApplicationContext(), "������", Toast.LENGTH_SHORT).show();
                    }
                    if(uuid.equals("0000ffe9-0000-1000-8000-00805f9b34fb")){
                        mNCharW = gattCharacteristic;
                    }
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }  
}
