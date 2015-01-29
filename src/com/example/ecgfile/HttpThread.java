package com.example.ecgfile;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;


public class HttpThread extends Thread{

    public static final int MESSAGE_GET_ID = 0;     // 获取可用ID
    public static final int MESSAGE_UPLOAD = 1;     // 上传文件
    
    String httpUrl;
    int type;
    Handler handler;
    
    HttpThread(String url, int type, Handler handler){
        this.type=type;
        this.httpUrl=url;
        this.handler=handler;
    }
    
    
    public void run(){
        JSONParser jsonParser = new JSONParser();         
        Message msg = new Message();
        switch (type) {
        case MESSAGE_GET_ID:
            msg.what = MESSAGE_GET_ID ;
            JSONObject info = jsonParser.makeHttpRequest(httpUrl);
            msg.obj = info;
            break;
        case MESSAGE_UPLOAD:            
            msg.what = MESSAGE_UPLOAD ;
            break;
        default:
            break;
        }       
        handler.sendMessage(msg);       
    }
}
