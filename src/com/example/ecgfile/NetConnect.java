package com.example.ecgfile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.http.ProtocolException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.widget.Toast;

public class NetConnect {
    
    static Activity activity;
    // ��ȡ�ֻ��������ӹ�����󣨰�����wifi,net�����ӵĹ���
    public static boolean checkNet(Context context) { 
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                // ��ȡ�������ӹ���Ķ���
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                System.out.println("Net:" + info);
                if (info == null || !info.isAvailable()) {
                    Toast.makeText(context,"���粻����",Toast.LENGTH_SHORT).show();
                    return false;
                } else {
//                    if(isNetAvailable()){
                        return true;
//                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(context,"���粻����",Toast.LENGTH_SHORT).show();
        return false;
    }
    
    //�ж�wifi�Ƿ��,���򵯳����ÿ�
    public static boolean checkWifi(final Activity activitiy) {
        WifiManager mWifiManager = (WifiManager) activitiy.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        int ipAddress = wifiInfo == null ? 0 : wifiInfo.getIpAddress();
        if (mWifiManager.isWifiEnabled() && ipAddress != 0) {
            return true;
        } else {
            new AlertDialog.Builder(activitiy)
            .setTitle("������wifi������ʹ��")  
            .setIcon(android.R.drawable.ic_dialog_info)                  
            .setPositiveButton("�õ�", new DialogInterface.OnClickListener() {                   
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    activitiy.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            })
            .setNegativeButton("������", null).show();           
            return false;    
        }
    }
    
    //�ж��Ƿ�����ܷ���ͨ���磨��CMCC������Ҫ��д�û�����������磬��Ȼwifi�������ˣ����ϲ�������
    public static boolean isNetAvailable() throws ProtocolException{
        HttpURLConnection conn = null;
        int responseCode = 0;
        try {
            URL url = new URL("HTTP://www.baidu.com/index.html");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Connection", "Close");
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(2000);
            conn.connect();
            responseCode = conn.getResponseCode();
            //���ص�״̬,����з���״̬�룬˵����������
            if (responseCode == 200 || responseCode == 206 || responseCode == 404) {
                return true;                    
            }
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }
}
