package com.example.chatheads;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    static final String LINK_ONE = "https://techcrunch.com/wp-content/uploads/2019/01/google-paying-users.jpg?w=1390&crop=1";
    static final String LINK_TWO = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTJ_GeCmfphA7YXcnKtsvdNPRuoVq1-bc8QrX-miPdTabH4SH4WqQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

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
        findViewById(R.id.bt_link_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadImage(LINK_TWO);
            }
        });
        loadImage(LINK_ONE);
    }

    private void loadImage(String url) {
        if (ChatHeadService.instance == null || ChatHeadService.instance.get() == null) {
            Intent i = new Intent(MainActivity.this, ChatHeadService.class);
            i.putExtra("IMAGE_URL", url);
            startService(i);
        } else {
            ChatHeadService.instance.get().loadImage(url);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

            //Check if the permission is granted or not.
            // Settings activity never returns proper value so instead check with following method
            if (Settings.canDrawOverlays(this)) {
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
