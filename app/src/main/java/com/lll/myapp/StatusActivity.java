package com.lll.myapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class StatusActivity extends AppCompatActivity {

    private TextView mTextView;
    private TextView statusView;
    private Button mButton;
    StringBuilder statusBuilder=new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        mTextView = findViewById(R.id.my_status);
        statusView = findViewById(R.id.statu_view);
        mButton =findViewById(R.id.btn_view);


        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                statusView.setText("");
                try {
                    mRead();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                statusView.setText(statusBuilder);
            }
        });

    }
    protected void mRead ()throws IOException
    {
        File file =new File("/sdcard/" + "MyStatus.txt");
        if(file.exists())
        {

            BufferedReader in =new BufferedReader(new FileReader(file));
            String line;
            statusBuilder.delete(0,statusBuilder.length());
            while ((line=in.readLine())!=null)
            {
                statusBuilder.append(line+"\r\n");
            }
            in.close();
        }
    }
}
