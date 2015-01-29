package com.example.ecgfile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class UploadOneActivity extends Activity {

    HttpThread myThread;
//    Handler handler;
    ProgressDialog pDialog;
//    String urlPath = "http://weiwangzhan2014.duapp.com/index.php/ecg/getAviableRecordId?username=user1&password=user1";
    String URL_FileUpload = "http://weiwangzhan2014.duapp.com/index.php/ecg/uploadEcgData";
//    int id = 0;
    Button button;
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private File[] currentFiles;
    private String srcPath ;
    Map<String, File> files = new HashMap<String, File>();      // 保存的文件列表
    boolean isFirst = true;                                     // 上传的第一个文件
    static private int openfileDialogId = 0;
    String filepath, filename;                                  //要上传的文件路径、文件名
    SharedPreferences defaultFolder;
    Editor edit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_one);
        button = (Button)findViewById(R.id.button1);
        
//        handler=new Handler(){
//            @Override
//            public void handleMessage(Message msg)
//            {
//                switch(msg.what)
//                {
//                case HttpThread.MESSAGE_GET_ID:
//                    getIdDeal((JSONObject)msg.obj);
//                    break;
//                default:
//                        break;
//                }
//            }
//         };
//         
//         getId(urlPath);
         
         button.setOnClickListener(new OnClickListener() {
             
             @Override
             public void onClick(View v) {
                 if(NetConnect.checkNet(getApplication())){
                     //判断是否打开wifi
                     NetConnect.checkWifi(UploadOneActivity.this);
                     showDialog(openfileDialogId);
                 }
                 
             }
         });
         
         //界面增加进度显示框
         pDialog = new ProgressDialog(this);
         pDialog.setIndeterminate(false);
         pDialog.setCancelable(true);
    }

//    //获取可用id
//    public void getId(String url){
//        myThread=new HttpThread(url, HttpThread.MESSAGE_GET_ID, handler);
//        myThread.start();
//    }
    
//    //处理getId的返回
//    public void getIdDeal(JSONObject json){
//        try {
//            if(null != json){
//                int result = json.getInt("error");
//                switch(result){
//                case 0:
//                    id = json.getInt("id");  
////                    Toast.makeText(getApplicationContext(), "可用ID："+id, Toast.LENGTH_SHORT).show();
//                    break;               
//                default:
//                    String errorInfoString = json.getString("error_desc");
//                    Toast.makeText(getApplicationContext(), errorInfoString, Toast.LENGTH_SHORT).show();
//                    break;
//                }
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
    
    //文件上传 Timer
    public void RTUploadTask(){
        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimerTask = new TimerTask(){
            @Override
            public void run() {
//                getFile(CommonVar.ecgFilePath);
                //执行上传
                new postFiles().execute();
            }
        };
        mTimer.schedule(mTimerTask, 1*1000, 15*1000);    //1秒开始，15秒一次
    }
    
    //停止文件上传循环
    private void cleanRTUploadTask() {
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
    
    //获取要上传的文件
    private void getFile(String path)  {  
        File root = new File(path);
        if (!root.exists()){
            return;   
        }
        currentFiles = root.listFiles();        
        srcPath = path+currentFiles[0].getName();
        System.out.println("srcPath:"+srcPath);
        File fileSelect = new File(srcPath);
        String fileName = fileSelect.getName();
        files.put(fileName, fileSelect);
    }
    
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
                Toast.makeText(UploadOneActivity.this, "成功上传", Toast.LENGTH_SHORT).show();
                isFirst = false;
                break;
            case 1:
                Toast.makeText(UploadOneActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file, menu);
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                clearPreference();
                break;
        }
        return true;
    }
    
    
    @Override  
    protected Dialog onCreateDialog(int id) {  
        if(id==openfileDialogId){  
            Map<String, Integer> images = new HashMap<String, Integer>();  
            // 下面几句设置各文件类型的图标， 需要你先把图标添加到资源文件夹  
            images.put(OpenFileDialog.sRoot, R.drawable.filedialog_root);           // 根目录图标  
            images.put(OpenFileDialog.sParent, R.drawable.filedialog_folder_up);    // 返回上一层的图标  
            images.put(OpenFileDialog.sFolder, R.drawable.filedialog_folder);       // 文件夹图标  
            images.put("txt", R.drawable.txt);                                      // txt文件图标  
            images.put(OpenFileDialog.sEmpty, R.drawable.filedialog_root);  
            Dialog dialog = OpenFileDialog.createDialog(id, this, "选择文件", new CallbackBundle() {  
                @Override  
                public void callback(Bundle bundle) {  
                    filepath = bundle.getString("path");  
                    setTitle(filepath);                                             // 把文件路径显示在标题上 
                    File file = new File(filepath);
                    filename = bundle.getString("name");
                    System.out.println("selected:"+filename);
                    files.put(filename, file);
                    RTUploadTask();
                    pDialog.setMessage("上传中...\n退出系统自动结束");
                    pDialog.show();
                }  
            },   
            ".txt;",  
            images);  
            return dialog;  
        }  
        return null;  
    }
    
    public void clearPreference(){
        defaultFolder = this.getSharedPreferences("defaultFolder", Context.MODE_PRIVATE);
        edit = defaultFolder.edit();
        edit.clear();
        edit.commit();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();       
        cleanRTUploadTask();
    }

}
