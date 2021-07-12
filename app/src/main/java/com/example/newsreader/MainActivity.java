package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>(); //ArrayList for news articles for MainActivity ListView
    ArrayList<String> urlList = new ArrayList<>(); // ArrayList for storing urls for each articles present in titles ArrayList
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;      //Database declaration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creating Database and Adding first table articles1
        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles1 " +
                "(id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, url VARCHAR)");

        //Downloading news from the Hacker news API URL
        DownloadTask task = new DownloadTask();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }

        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        //Calling New Activity when list item clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, ArticleActivity.class);
                intent.putExtra("url", urlList.get(position));
                startActivity(intent);
            }
        });

        //Calling to update List View to update in MainActivity
        updateListView();

    }

    public void updateListView(){
        //Cursor to travel through the saved database
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles1", null);

        //Getting index from cursor
        int urlIndex = c.getColumnIndex("url");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            urlList.clear();

            do{
                titles.add(c.getString(titleIndex));
                urlList.add(c.getString(urlIndex));
            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }

    //Downloading News using Async Task will change it to Something else in Future
    public class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpsURLConnection urlConnection;
            try{

                //Downloading News ID from first Call
                url = new URL(urls[0]);
                urlConnection = (HttpsURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();
                while(data != -1){
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }
//                Log.i("URL content : ", result);
                JSONArray jsonArray = new JSONArray(result);
                int numberOfItems = 20;
                if(jsonArray.length() < 20){
                    numberOfItems = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM articles1");

                //Downloading News Titles and Urls from Second Call to API
                for(int i = 0; i < numberOfItems; i++){
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    data = inputStreamReader.read();
                    String articleInfo = "";
                    while(data != -1){
                        char current = (char) data;
                        articleInfo   += current;
                        data = inputStreamReader.read();
                    }
//                    Log.i("ArticleInfo : ",articleInfo);
                    JSONObject jsonObject = new JSONObject(articleInfo);

                    //Parsing using JSON Object and saving it to database
                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        String sql = "INSERT INTO articles1 (articleId, title, url)" +
                                " VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleUrl);
                        statement.execute();
                    }
                }
                return result;
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

}