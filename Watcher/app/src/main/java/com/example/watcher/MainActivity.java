package com.example.watcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private String save(String fname, JSONArray jsonArray) throws JSONException, IOException {
        String userString = jsonArray.toString();
        File file = new File(MainActivity.this.getFilesDir(),fname + ".json");
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(userString);
        bufferedWriter.close();
        return MainActivity.this.getFilesDir().toString();
    }

    private JSONObject load(String fname) throws IOException, JSONException {
        File file = new File(MainActivity.this.getFilesDir(),fname);
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuilder stringBuilder = new StringBuilder();
        String line = bufferedReader.readLine();
        while (line != null){
            stringBuilder.append(line).append("\n");
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        JSONObject jsonObject  = new JSONObject(stringBuilder.toString());
        return jsonObject;
    }
    private void update(String fname){
//        load json object, if none. create new one.
//        append value
//        save
    }

    private JSONArray loadAsset(String inFile) {
        JSONArray  json = null;
        try {
            InputStream stream = getAssets().open(inFile);

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            String tContents = new String(buffer);
            json = new JSONArray (tContents);
        } catch (IOException | JSONException e) {
            // Handle exceptions here
        }

        return json;
    }
    public Object searchCode(String code) throws JSONException {
        String ex = code.substring(0,2);
        JSONArray jsonArray = loadAsset(ex+".json");
        for(int i=0; i < jsonArray.length(); i++){
            JSONObject j = jsonArray.getJSONObject(i);
            if(j.getString("cardCode").equals(code)){
                return j;
            }
        }
        return null ;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            initializeView();
        }
    }

    /**
     * Set and initialize the view elements.
     */
    private void initializeView() {
        findViewById(R.id.notify_me).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(new Intent(MainActivity.this, FloatingViewService.class));
//                finish();
            }
        });

        findViewById(R.id.write).setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v) {
                String text = "";
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.read).setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v) {
                JSONArray  jsonArray = null;
                try {
                    TextView txt = findViewById(R.id.display);
                    JSONObject card = (JSONObject) searchCode("02BW040");
                    txt.setText(card.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                initializeView();
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}