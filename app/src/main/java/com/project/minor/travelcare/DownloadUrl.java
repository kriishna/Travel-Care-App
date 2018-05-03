package com.project.minor.travelcare;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//Retrieve data from URL using http url connection and file handling method
public class DownloadUrl {

    public String readUrl(String myUrl) throws IOException
    {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(myUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            // Read data from URL
            inputStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();

            // Read each line and append it to string buffer
            String line;
            while((line = br.readLine()) != null)
            {
                sb.append(line);
            }
            // String Buffer to String
            data = sb.toString();
            Log.d("downloadUrl", data);

            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(inputStream != null)
                inputStream.close();
            assert urlConnection != null;
            urlConnection.disconnect();
        }

        Log.d("data downlaod",data);
        return data;

    }

}
