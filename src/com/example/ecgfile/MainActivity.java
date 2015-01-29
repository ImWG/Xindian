package com.example.ecgfile;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity{

    Button Simulation, Real;
    Intent intent;
    HttpThread myThread;
    Handler handler;
    String urlPath = "http://weiwangzhan2014.duapp.com/index.php/ecg/getAviableRecordId?username=user1&password=user1";
    public static int id = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Simulation = (Button)findViewById(R.id.button1);
        Real = (Button)findViewById(R.id.button2);
        
        Simulation.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                intent = new Intent(getApplicationContext(),UploadOneActivity.class);
                startActivity(intent);
            }
        });
        
        Real.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                intent = new Intent(getApplicationContext(),DeviceScanActivity.class);
                startActivity(intent);
            }
        });
        
        handler=new Handler(){
            @Override
            public void handleMessage(Message msg)
            {
                switch(msg.what)
                {
                case HttpThread.MESSAGE_GET_ID:
                    getIdDeal((JSONObject)msg.obj);
                    break;
                default:
                        break;
                }
            }
         };
         
         getId(urlPath);
    }
    
  //获取可用id
    public void getId(String url){
        myThread=new HttpThread(url, HttpThread.MESSAGE_GET_ID, handler);
        myThread.start();
    }
    
    //处理getId的返回
    public void getIdDeal(JSONObject json){
        try {
            if(null != json){
                int result = json.getInt("error");
                switch(result){
                case 0:
                    id = json.getInt("id");  
//                    Toast.makeText(getApplicationContext(), "可用ID："+id, Toast.LENGTH_SHORT).show();
                    break;               
                default:
                    String errorInfoString = json.getString("error_desc");
                    Toast.makeText(getApplicationContext(), errorInfoString, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
