package com.example.watcher;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class FloatingViewService extends Service {
    private WindowManager mWindowManager;
    private View mFloatingView;
    private View fixingView;
    private Handler handler;
    private int count = 0;
    private boolean myDeckUpdated = true;
    private boolean enemyPlayedUpdated = true;
    private JSONObject currentDeck = new JSONObject();
    private JSONObject enemyPlayed = new JSONObject();
    private JSONObject myPlayed = new JSONObject();
    private String lastStatus = null;
    private final String phone = "http://127.0.0.1";
    private final String emu = "http://10.0.2.2";
    private final String url_game_result = emu+":21337/game-result";
    private final String url_deck_list = emu+":21337/static-decklist";
    private final String url_game_state = emu+":21337/positional-rectangles";
//    myPlayed = {"01DE001":["1","2"],"01DE022":["5","10"]};

    private void save(String fname, JSONArray jsonArray) throws JSONException, IOException {
        String userString = jsonArray.toString();
        File file = new File(FloatingViewService.this.getFilesDir(),fname + ".json");
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(userString);
        bufferedWriter.close();
    }

    private JSONObject load(String fname) throws IOException, JSONException {
        File file = new File(FloatingViewService.this.getFilesDir(),fname);
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

    public JSONObject searchCard(String code) throws JSONException {
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
    public String searchName(String code){
        JSONObject card = null;
        String name = "not found";
        try {
            card = searchCard(code);
            name = card.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return name;
    }
    private void skeleton_only(String url){
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(req);
    }
    public void showDeck() throws JSONException {
        final ListView list = mFloatingView.findViewById(R.id.list);
        final JSONObject colors = new JSONObject();
        final JSONObject backgroundCode = new JSONObject();
        final View collapsedView = mFloatingView.findViewById(R.id.collapse_view);
        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);

        if(currentDeck.length() == 0){
            return;
        }
//        Toast.makeText(FloatingViewService.this, "Deck: "+currentDeck, Toast.LENGTH_SHORT).show();
        ArrayList<String> arrayList = new ArrayList<>();
        Iterator<String> keys = currentDeck.keys();
        while(keys.hasNext()) {
            String code = keys.next();
            String cardName = searchName(code);
            int remain = currentDeck.getInt(code);
            if(myPlayed.has(code)){
                remain = remain - ((ArrayList<Integer>) myPlayed.get(code)).size();
            }
            try {
                arrayList.add("x"+remain+" "+cardName);
                backgroundCode.put(cardName,code);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        list.setAdapter(new ArrayAdapter<String>(FloatingViewService.this, android.R.layout.simple_list_item_1, arrayList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                String cardName = (String) textView.getText();
                try {
                    String bg = backgroundCode.getString(cardName.substring(3));
                    textView.setTextColor(Color.parseColor("#ffffff"));
                    int rawId = getResources().getIdentifier("bg_"+bg.toLowerCase(), "drawable", getPackageName());
                    textView.setBackgroundResource(rawId);
                    ViewGroup.LayoutParams vg = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    vg.height = 40*3;
                    vg.width = 220*3;

                    textView.setLayoutParams(vg);
                    textView.setTextSize(14);
                    textView.setShadowLayer(8f, -2, 2, Color.BLACK);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return textView;
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String clickedItem = (String) list.getItemAtPosition(position);
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
            }
        });
    }

    public void showEnemyPlayed() throws JSONException {
        final ListView list = mFloatingView.findViewById(R.id.enemy_list);
        final JSONObject colors = new JSONObject();
        final JSONObject backgroundCode = new JSONObject();
        final View collapsedView = mFloatingView.findViewById(R.id.collapse_view);
        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);

        if(enemyPlayed.length() == 0){
            return;
        }
//        Toast.makeText(FloatingViewService.this, "Deck: "+enemyPlayed, Toast.LENGTH_SHORT).show();
        ArrayList<String> arrayList = new ArrayList<>();
        Iterator<String> keys = enemyPlayed.keys();
        Log.v("code","" + enemyPlayed+ myPlayed);
        while(keys.hasNext()) {
            String code = keys.next();

            String cardName = searchName(code);
            int played = ((ArrayList<Integer>) enemyPlayed.get(code)).size();
            try {
                arrayList.add("x"+played+" "+cardName);
                backgroundCode.put(cardName,code);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        list.setAdapter(new ArrayAdapter<String>(FloatingViewService.this, android.R.layout.simple_list_item_1, arrayList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                String cardName = (String) textView.getText();
                try {
                    String bg = backgroundCode.getString(cardName.substring(3));
                    textView.setTextColor(Color.parseColor("#ffffff"));
                    int rawId = getResources().getIdentifier("bg_"+bg.toLowerCase(), "drawable", getPackageName());
                    textView.setBackgroundResource(rawId);
                    ViewGroup.LayoutParams vg = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    vg.height = 40*3;
                    vg.width = 220*3;

                    textView.setLayoutParams(vg);
                    textView.setTextSize(14);
                    textView.setShadowLayer(8f, -2, 2, Color.BLACK);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return textView;
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String clickedItem = (String) list.getItemAtPosition(position);
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
            }
        });
    }

    public void updateDeckList() throws JSONException {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,url_deck_list, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONObject json = response;
                try {
                    currentDeck = json.getJSONObject("CardsInDeck");
//                    Toast.makeText(FloatingViewService.this, "Deck list updated", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(req);
    }
    private void updatePlayedCards(JSONArray cards) throws JSONException {
        for(int i=0; i<cards.length(); i++){
            JSONObject c = cards.getJSONObject(i);
            JSONObject played;
            if(c.getBoolean("LocalPlayer")){
                played = myPlayed;
            }else{
                played = enemyPlayed;
            }
            String code = c.getString("CardCode");
            if(code.equals("face")){
                continue;
            }
            if(played.has(code)){
                ArrayList<Integer> id = (ArrayList<Integer>) played.get(code);
                if(!id.contains(c.getInt("CardID"))){
                    id.add(c.getInt("CardID"));
                    if(c.getBoolean("LocalPlayer")){
                        myDeckUpdated = true;
                    }else{
                        enemyPlayedUpdated = true;
                    }
                }
            }else{
                ArrayList<Integer> id = new ArrayList<>();
                id.add(c.getInt("CardID"));
                played.put(code, id);
                if(c.getBoolean("LocalPlayer")){
                    myDeckUpdated = true;
                }else{
                    enemyPlayedUpdated = true;
                }
            }
        }
    }
    private void updateOverlay() throws JSONException {
//        if(myDeckUpdated){
//            showDeck();
//            myDeckUpdated = false;
//        }
//        if(enemyPlayedUpdated) {
//            showEnemyPlayed();
//            enemyPlayedUpdated = false;
//        }
        showDeck();
        showEnemyPlayed();
    }
    private void updateState(){
        handler = new Handler();
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
//                state = requestJSON(url_game_state);
                JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,url_game_state, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject state) {
//                        Toast.makeText(FloatingViewService.this, ""+state, Toast.LENGTH_SHORT).show();
                        String status = null;
                        try {
                            status = state.getString("GameState");
                            if(status.equals("InProgress")){
                                if(currentDeck.length() == 0){
                                    updateDeckList();
                                }
                                JSONArray rect = (JSONArray) state.get("Rectangles");
                                updatePlayedCards(rect);
                                updateOverlay();
                            }else{ //if(status.equals("Menus")){
                                currentDeck = new JSONObject();;
                                //update history
                            }

                            lastStatus = status;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        NetworkResponse networkResponse = error.networkResponse;
//                        Toast.makeText(FloatingViewService.this, "Cannot get request "+ error, Toast.LENGTH_SHORT).show();
                    }
                });
                RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
                requestQueue.add(req);
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnableCode);
    }

    public FloatingViewService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ResourceType")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        //Inflate the floating view layout we created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);
        fixingView = LayoutInflater.from(this).inflate(R.layout.layout_destroy, null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 0;

        final WindowManager.LayoutParams paramsDes = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        paramsDes.gravity = Gravity.BOTTOM | Gravity.CENTER;        //Initially view will be added to top-left corner
        paramsDes.x = 0;
        paramsDes.y = 0;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(fixingView, paramsDes);
        mWindowManager.addView(mFloatingView, params);

        fixingView.setVisibility(View.GONE);

        //The root element of the collapsed view layout
        final View collapsedView = mFloatingView.findViewById(R.id.collapse_view);
        final ImageView poro = mFloatingView.findViewById(R.id.collapsed_iv);
        //The root element of the expanded view layout
        final View expandedView = mFloatingView.findViewById(R.id.expanded_container);

        final ImageView destroyButton = (ImageView) fixingView.findViewById(R.id.destroy);

//        final TextView text1 = (TextView) mFloatingView.findViewById(R.id.text1);
//        text1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                text1.setText("touched");
//            }
//        });
        collapsedView.setVisibility(View.VISIBLE);
        expandedView.setVisibility(View.GONE);
        updateState();

//        params.width = WindowManager.LayoutParams.MATCH_PARENT;

        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            int desPos[] = new int[2];
            int dX = 0;
            int dY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;
                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);
                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Math.abs(Xdiff) < 10 && Math.abs(Ydiff) < 10) {
                            if (isViewCollapsed()) {
                                try {
                                    myDeckUpdated = true;
                                    enemyPlayedUpdated = true;
                                    updateOverlay();
                                    collapsedView.setVisibility(View.GONE);
                                    expandedView.setVisibility(View.VISIBLE);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                collapsedView.setVisibility(View.VISIBLE);
                                expandedView.setVisibility(View.GONE);
                            }
                        }
                        fixingView.setVisibility(View.GONE);

                        if (collapsedView.getVisibility() == View.VISIBLE) {
                            destroyButton.getLocationOnScreen(desPos);
                            dX = desPos[0];
                            dY = desPos[1];
                            int w = destroyButton.getWidth();
                            int h = destroyButton.getHeight();
                            if (event.getRawX() > dX - w && event.getRawX() < dX + w + w && event.getRawY() > dY - h && event.getRawY() < dY + h + h) {
                                stopSelf();
                            }
//                            Toast.makeText(FloatingViewService.this, result, Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
//                        params.width = WindowManager.LayoutParams.WRAP_CONTENT;

                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        if(collapsedView.getVisibility() == View.VISIBLE) {
                            TransitionManager.beginDelayedTransition((ViewGroup) fixingView, new Slide());
                            fixingView.setVisibility(View.VISIBLE);
                            destroyButton.getLocationOnScreen(desPos);
                            dX = desPos[0];
                            dY = desPos[1];
                            int w = destroyButton.getWidth();
                            int h = destroyButton.getHeight();
                            if (event.getRawX() > dX - w && event.getRawX() < dX + w + w && event.getRawY() > dY - h && event.getRawY() < dY + h + h) {
//                                destroyButton.setScaleX((float)1.2);
//                                destroyButton.setScaleY((float)1.2);
                                params.x = dX;
                                params.y = dY - (int) (destroyButton.getHeight()*0.535);
                                poro.setImageResource(R.drawable.sad_poro);
                                poro.setAlpha(0.66f);
                            }else {
//                                destroyButton.setScaleX((float)(1/1.2));
//                                destroyButton.setScaleY((float)(1/1.2));
                                poro.setImageResource(R.drawable.ic_android_circle);
                                poro.setAlpha(1.0f);
                            }
                        }

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });
    }


    /**
     * Detect if the floating view is collapsed or expanded.
     *
     * @return true if the floating view is collapsed.
     */
    private boolean isViewCollapsed() {
        return mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }
}