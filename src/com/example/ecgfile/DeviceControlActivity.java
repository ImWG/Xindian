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
    
    //文件上传部分
    private File[] currentFiles;                                        // 文件夹包含的文件
    private String srcPath ;
    String measure_time = "";
    int judge = -1;                                                     // 服务器json返回
    Map<String, File> files = new HashMap<String, File>();              // 保存的文件列表
    List<String> uploadCache = new ArrayList<String>();     			// 已经上传的文件
    String URL_FileUpload = CommonVar.URL_FileUpload;                   // 上传地址
    boolean isRealTimeUp = false;                   // 实时上传标志 
    boolean isFirst = true;                         // 上传的第一个文件
    
    //ecg画图
    public static String intentData = null;         // 心电数据字符串
    private SurfaceView mSurfaceView_ECG;
    private Paint mPaint;
    ZoomControls zoomX, zoomY;
    String dataTmp = "1000";
    private boolean isRecording = true;              // 绘图线程控制标记
    private boolean acceptData = true;               // 是否取出stringbuilder中的数
    private int wait =100;                           // 数据缓冲时间
    private int rateX = 1;                           // X轴缩小的比例
    private int rateY = 2;                           // Y轴缩小的比例
    private int baseLine = 500;                  	 // Y轴基线
    private int rateXmin = 1;
    private int rateXmax = 5;
    private int rateYmin = 2;
    private int rateYmax = 5;
    
    //记录事件的当前时间
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
                if(!timeString.equals("")){     //时间字符串非空时，对数据进行保存
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
    
    //时间变更 Timer，用来控制文件保存的时间--15秒新建一个文件
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

    //停止循环时钟
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
    
    //文件上传 Timer --控制文件上传的时间间隔
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
                //获取要上传的文件
                getFile(CommonVar.ecgFilePathTemp);
                //执行上传
                if(files.size() >=1){
                    new postFiles().execute();
                }
            }
        };
        mTimer2.schedule(mTimerTask2, 15*1000, 8*1000);    //15秒开始，8秒一次
    }
    //停止文件上传循环
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
    	setTitle("心电测量");
        mSurfaceView_ECG = (SurfaceView)findViewById(R.id.surfaceView_ecg); //画布
        mSurfaceView_ECG.setOnTouchListener(new TouchEvent());
        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);                                       // 画笔为绿色
        mPaint.setStrokeWidth(2);                                           // 设置画笔粗细
        
        zoomX = (ZoomControls)this.findViewById(R.id.zoomControls_x);
        MyZoomControls.convert(zoomX);
        zoomX.setOnZoomOutClickListener(new View.OnClickListener() {      	// 缩小      
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
        zoomX.setOnZoomInClickListener(new View.OnClickListener() {			// 放大
            
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
        
        //开始、暂停事件
        mButton_start.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View view) {
                //发送给心电设备的开始通知
                if(firstConnect){
                    String data = "1";
                    if(mNCharW == null){
                        Toast.makeText(getApplication(),"连接失败", Toast.LENGTH_SHORT).show();
                        DeviceControlActivity.this.finish();
                        return ;
                    }
                    mNCharW.setValue(data.getBytes());
                    mBluetoothLeService.writeCharacteristic(mNCharW);
                    //打开串口数据接收的使能位
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                    firstConnect = false;
                    StartDraw();                    //开始画图
                    mButton_save.setClickable(true);
                    mButton_remoteUpload_enable.setClickable(true);
                    return ;
                }
                
                if(mButton_start.isChecked()){      //接收数据
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                    mButton_remoteUpload_enable.setClickable(true);                    
                    mButton_save.setClickable(true);
                    StartDraw();                    //停止画图
                }else{                              //停止接收
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                    mButton_remoteUpload_enable.setClickable(false);
                    mButton_save.setClickable(false);                    
                    mButton_remoteUpload_enable.setChecked(false);
                    StopDraw();
                }
            }
        });  
        
        // 保存事件,设置执行15秒       
        mButton_save.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View v) {
                if(intentData == null && mButton_save.isChecked()){
                    Toast.makeText(DeviceControlActivity.this, "无数据", Toast.LENGTH_SHORT).show();
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
                //点击事件使能
                mButton_start.setClickable(false);
                mButton_save.setClickable(false);
                mButton_remoteUpload_enable.setClickable(false);                
                Toast.makeText(DeviceControlActivity.this, 
                        "正在保存从现在开始，15秒之内的数据", Toast.LENGTH_SHORT).show();
            }
        });
        mButton_save.setClickable(false);
        
        //实时上传事件
        mButton_remoteUpload_enable.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View view){
                if(mButton_remoteUpload_enable.isChecked()){
                    //保存按钮无效
                    mButton_save.setChecked(false);
                    mButton_save.setClickable(false);
                    //开启实时标志
                    isRealTimeUp = true;                    
                    //判断是否有网络
                    if(NetConnect.checkNet(getApplication())){
                        //判断是否打开wifi
                        NetConnect.checkWifi(DeviceControlActivity.this);
                        //开启循环执行的 保存文件任务/每15秒更新一次时间
                        TimerTask();
                        //上传相关工作
                        RTUploadTask();
                        Toast.makeText(getApplication(), 
                                "已允许实时上传数据", Toast.LENGTH_SHORT).show();
                    }else {
                        return ;
                    }                    
                }else{
                    if(mButton_start.isChecked()){
                        mButton_save.setClickable(true);
                    }
                    //关闭实时标志
                    isRealTimeUp = false;
                    Toast.makeText(getApplication(), 
                            "已关闭实时上传数据", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mButton_remoteUpload_enable.setClickable(false);
    }
    
    /**
     * 画图部分开始
     * ================================================================================
     */
    //画布触摸事件
    class TouchEvent implements OnTouchListener {
        //@Override
        public boolean onTouch(View v, MotionEvent event) {
            baseLine = (int) event.getY();
            return true;
        }
    }
    
    public void StartDraw() {
        isRecording = true;
        new DrawThread().start();           // 开始绘制线程   
    }

    public void StopDraw() {
        isRecording = false;
    }
    
    class DrawThread extends Thread {
        private int oldX = 0;               // 上次绘制的X坐标
        private int oldY = 0;               // 上次绘制的Y坐标
        private int X_index = 0;            // 当前画图所在屏幕X轴的坐标
        int data_len;
        String[] dataArray;
       
        public void run(){
            while (isRecording)  {
                try { 
                	if(sBuilder.toString().length() > 10 && acceptData){        // 这里长度10只是为了表明已赋值         
                		dataArray = sBuilder.toString().split(" ");
                		sBuilder.delete(0, sBuilder.length());                  // 清空stringbuilder
                        data_len = dataArray.length;
                        SimpleDraw(X_index, dataArray, rateX, rateY, baseLine); // 把缓冲区数据画出来                       
                        X_index = X_index + data_len;
                        if (X_index > mSurfaceView_ECG.getWidth()) {
                            X_index = 0;                                        // 超过屏幕，原点重绘
                        }                                                                                          
                        Thread.sleep(wait);                                     // 延时一定时间缓冲数据
                        acceptData = true;                                      // 接收缓冲数据使能
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }   
        }
        
        void SimpleDraw(int start, String[] inputBuf, int rateX, int rateY, int baseLine) {
            acceptData = false;                                                             // 暂不接收缓冲数据
        	if (start == 0){
        		oldX = 0;
        		ClearDraw();
        	}

            Canvas canvas = mSurfaceView_ECG.getHolder().lockCanvas(
                    new Rect(start, 0, start + data_len ,mSurfaceView_ECG.getHeight()));    // 获取画布
            canvas.drawColor(Color.BLACK);			                                        // 清除背景
            for (int i = 0; i < data_len; i=i+rateX) {                                      // 有多少画多少
                int x = i + start;
                int y = baseLine - Integer.parseInt(inputBuf[i])/rateY;                     // 调节缩小比例，调节基准线
                if(i > 0){                                                                  // 从第二个点开始画，原点不画出来
                	canvas.drawLine(oldX, oldY, x, y, mPaint);
                }
                oldX = x;
                oldY = y;
            }
            mSurfaceView_ECG.getHolder().unlockCanvasAndPost(canvas);                       // 解锁画布，提交画好的图像           
        }
        
        // 清除整个画布
        void ClearDraw() {
            Canvas canvas = mSurfaceView_ECG.getHolder().lockCanvas(null);
            canvas.drawColor(Color.BLACK);
            mSurfaceView_ECG.getHolder().unlockCanvasAndPost(canvas);
        }
    }
    
    /**
     * 画图部分结束
     * ================================================================================
     */
    
    //保存数据到文件
    public void save2file(String timeString, String dataString) throws FileNotFoundException{       
        String path = CommonVar.ecgFilePath;
        Log.d(CommonVar.TAG,"sean "+path);
        File filePath = new File(path);
        if(!filePath.exists()){
            Log.d(CommonVar.TAG, "文件夹不存在，创建");
            if(!filePath.mkdirs()){
                System.out.println("创建失败");
            }else{
                System.out.println("创建成功");
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
    
    //连续保存文件
    public void continueSaveFile(String timeString, String dataString) throws FileNotFoundException{      
        String path = CommonVar.ecgFilePathTemp;
        File filePath = new File(path);
        if(!filePath.exists()){
            Log.d(CommonVar.TAG, "文件夹不存在，创建");
            if(!filePath.mkdirs()){
                System.out.println("创建失败");
            }else{
                System.out.println("创建成功");
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
     * 获取文件夹中最早的文件,添加到待上传列表中
     * 初始化上传文件的 参数: measure_time,files
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
    
    //清空文件夹
    private void deleteFiles(String path){
        File root = new File(path);
        File[] files = root.listFiles();
        if(null != files){
            for(File file: files){
                file.delete();
            }
        }
    }
    
    //文件夹内部排序，最新的文件排在前面
    private void sortFiles(File[] files){  
        Arrays.sort(files, new Comparator<File>(){  
            public int compare(File paramFile1, File paramFile2){  
                return (int) (paramFile2.lastModified() - paramFile1.lastModified());
            }
        });
    }
    
    /*//上传文件
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
                //待上传文件中去除此文件
                files.remove(fileName);
                //从temp文件夹中删除此文件
                if(fileDelete.exists()){
                    fileDelete.delete();                  
                }                
                System.out.println("成功上传:"+fileName);
                Toast.makeText(DeviceControlActivity.this, "成功上传"+fileName, Toast.LENGTH_SHORT).show();
                uploadCache.add(fileName);
                break;
            case 0:
                Toast.makeText(DeviceControlActivity.this, "上传失败，进行重试", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
        }

    }   */
    
  //上传文件
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
                //待上传文件中去除此文件
                files.remove(fileName);
                //从temp文件夹中删除此文件
                if(fileDelete.exists()){
                    fileDelete.delete();                  
                }                
//                System.out.println("成功上传:"+fileName);
                Toast.makeText(DeviceControlActivity.this, "成功上传"+fileName, Toast.LENGTH_SHORT).show();
                uploadCache.add(fileName);
                isFirst = false;
                break;
            case 1:
                Toast.makeText(getApplicationContext(), "上传失败", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
        }
    }
    
    /*
     * 这里的做作用只是将下位机的响应 uuid 的 services 和 characteristic 传递出来
     * 使 Button 响应正确的事件
     */
    @SuppressLint("SimpleDateFormat") 
    private void getGattServices(List<BluetoothGattService> gattServices) {
        System.out.println("进入getGattServices");
        if (gattServices == null){
            System.out.println("getGattServices null 结束");
            return;
        }
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if(uuid.equals("0000ffe0-0000-1000-8000-00805f9b34fb")
                    || uuid.equals("0000ffe5-0000-1000-8000-00805f9b34fb")){	// 只获取这2个uuid service
                
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
    
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    uuid = gattCharacteristic.getUuid().toString();
                    if(uuid.equals("0000ffe4-0000-1000-8000-00805f9b34fb")){
                        mNotifyCharacteristic = gattCharacteristic;
                        Toast.makeText(getApplicationContext(), "已连接", Toast.LENGTH_SHORT).show();
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
