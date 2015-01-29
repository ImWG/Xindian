package com.example.ecgfile;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class JSONParser{

	static InputStream is = null;
	static JSONObject jObj = null;
	static String json = "";

	// constructor
	public JSONParser() {

	}

	//post数据
	public JSONObject makeHttpRequest(String url) {
		// Making HTTP request
		try {
			// defaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			is = httpEntity.getContent();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			is.close();
			json = sb.toString();
			Log.d("json数据", json);
		} catch (Exception e) {
			Log.e("Buffer Error", "Error converting result " + e.toString());
		}

		// try parse the string to a JSON object
		try {
			jObj = new JSONObject(json);
		} catch (JSONException e) {
			Log.e("JSON Parser", "Error parsing data " + e.toString());
		}
		
		// return JSON String
		return jObj;
	}
	
	public Bitmap getBitMap(String url) {  
        URL myFileUrl = null;  
        Bitmap bitmap = null;  
        try {  
         myFileUrl = new URL(url);  
        } catch (MalformedURLException e) {  
         e.printStackTrace();  
        }  
        try {  
         HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();  
         conn.setDoInput(true);  
         conn.connect();  
         InputStream is = conn.getInputStream();  
         bitmap = BitmapFactory.decodeStream(is);  
         is.close();  
        } catch (IOException e) {  
         e.printStackTrace();  
        }  
        return bitmap;  
     } 
	
	//同时上传参数和文件
    public JSONObject postFile(String actionUrl, Map<String, String> params,  
            Map<String, File> files) throws IOException {   
      
        String BOUNDARY = java.util.UUID.randomUUID().toString();  
        String PREFIX = "--", LINEND = "\r\n";  
        String MULTIPART_FROM_DATA = "multipart/form-data";  
        String CHARSET = "UTF-8";  
        URL uri = new URL(actionUrl);  
        HttpURLConnection conn = (HttpURLConnection) uri.openConnection();  
        conn.setReadTimeout(20 * 1000);  
        conn.setDoInput(true);// 允许输入  
        conn.setDoOutput(true);// 允许输出  
        conn.setUseCaches(false);  
        conn.setRequestMethod("POST"); // Post方式  
        conn.setRequestProperty("connection", "keep-alive");  
        conn.setRequestProperty("Charsert", "UTF-8");  
        conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA  
                + ";boundary=" + BOUNDARY);  
      
        // 首先组拼文本类型的参数  
        StringBuilder sbParam = new StringBuilder();  
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sbParam.append(PREFIX + BOUNDARY + LINEND);    
            sbParam.append("Content-Disposition: form-data; name=\""  
                    + entry.getKey() + "\"" + LINEND);  
            sbParam.append("Content-Type: text/plain; charset=" + CHARSET + LINEND);  
            sbParam.append("Content-Transfer-Encoding: 8bit" + LINEND);  
            sbParam.append(LINEND);  
            sbParam.append(entry.getValue());  
            sbParam.append(LINEND);  
        }  
      
        DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());  
        outStream.write(sbParam.toString().getBytes());  
      
        // 发送文件数据  
        if (files != null)  
            for (Map.Entry<String, File> file : files.entrySet()) {  
                System.out.println(file.getKey());
                StringBuilder sbFile = new StringBuilder();  
                sbFile.append(PREFIX + BOUNDARY + LINEND);   
                sbFile.append("Content-Disposition: form-data; name=\"ecg_file\"; filename=\""  
                                + file.getKey() + "\"" + LINEND);  
                sbFile.append("Content-Type: application/octet-stream; charset="  
                        + CHARSET + LINEND);  
                sbFile.append(LINEND);  
                outStream.write(sbFile.toString().getBytes());  
                InputStream is = new FileInputStream(file.getValue());  
                byte[] buffer = new byte[1024];  
                int len = 0;  
                while ((len = is.read(buffer)) != -1) {  
                    outStream.write(buffer, 0, len);  
                }     
                is.close();  
                outStream.write(LINEND.getBytes());  
            }  
      
        // 请求结束标志  
        byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();  
        outStream.write(end_data);  
        outStream.flush();  
      
        // 得到响应码  
        int res = conn.getResponseCode(); 

        InputStream in = conn.getInputStream();
        InputStreamReader isReader = new InputStreamReader(in);  
        BufferedReader bufReader = new BufferedReader(isReader);  
        String line = null;  
        StringBuilder sbRespon = new StringBuilder();
      
        while ((line = bufReader.readLine()) != null)  {
            System.out.print(line);
            sbRespon.append(line);
        }

        outStream.close();  
        conn.disconnect(); 
        try {
            jObj = new JSONObject(sbRespon.toString());
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }
        return jObj;   
    }   
}
