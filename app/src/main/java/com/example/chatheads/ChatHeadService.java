package com.example.chatheads;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CircularProgressDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.blankj.utilcode.util.ScreenUtils;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.lang.ref.WeakReference;

public class ChatHeadService extends Service {

    private WindowManager windowManager;
    private View rootView;
    private PhotoView photoView;
    private CircularProgressDrawable circularProgressDrawable;
    private View imageLayout;
    private View chatHead;
    static final int DEFAULT_IMAGE_WIDTH = (int) (ScreenUtils.getScreenWidth() * 2f / 3);
    static final int ZOOM_IMAGE_WIDTH = (int) (ScreenUtils.getScreenWidth() * 4f / 5);
    static final double HW_RATE = 2.f / 3;
    boolean isZoomed = false;

    public static WeakReference<ChatHeadService> instance;

    public ChatHeadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra("IMAGE_URL");
        loadImage(url);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = new WeakReference<>(this);

        // init circular loading
        circularProgressDrawable = new CircularProgressDrawable(this);
        circularProgressDrawable.setStrokeWidth(5f);
        circularProgressDrawable.setCenterRadius(30f);
        circularProgressDrawable.start();

        //Inflate the chat head layout we created
        rootView = LayoutInflater.from(this).inflate(R.layout.layout_chat_head, null);
        photoView = rootView.findViewById(R.id.pv_image);
        imageLayout = rootView.findViewById(R.id.rl_image);
        chatHead = rootView.findViewById(R.id.rl_chat_head);

        // toggle on/off
        photoView.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(ImageView view, float x, float y) {
                imageLayout.setVisibility(View.GONE);
                chatHead.setVisibility(View.VISIBLE);
            }
        });

        // set zoom button
        final ImageView zoomButton = rootView.findViewById(R.id.img_zoom);
        zoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), isZoomed ? R.drawable.zoom_in : R.drawable.zoom_out));
                isZoomed = !isZoomed;
                Log.d("ZOOM", "Zoom: " + ZOOM_IMAGE_WIDTH + " Default: " + DEFAULT_IMAGE_WIDTH);
                setViewSize(imageLayout, isZoomed ? ZOOM_IMAGE_WIDTH : DEFAULT_IMAGE_WIDTH);
            }
        });

        //Set the close button.
        ImageView closeButton = (ImageView) rootView.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //close the service and remove the chat head from the window
                if (instance != null) instance.clear();
                instance = null;
                stopSelf();
            }
        });

        // render image with predefined size
        setViewSize(imageLayout, DEFAULT_IMAGE_WIDTH);
        addChatHeadToWindow();
    }

    private void addChatHeadToWindow() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the chat head position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(rootView, params);
        //Drag and move chat head using user's touch action.
        final ImageView chatHeadImage = (ImageView) rootView.findViewById(R.id.iv_chat_head);
        chatHeadImage.setOnTouchListener(new View.OnTouchListener() {
            private int lastAction;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

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

                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_UP:
                        //As we implemented on touch listener with ACTION_MOVE,
                        //we have to check if the previous action was ACTION_DOWN
                        //to identify if the user clicked the view or not.
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            imageLayout.setVisibility(View.VISIBLE);
                            chatHead.setVisibility(View.GONE);
                        }
                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        windowManager.updateViewLayout(rootView, params);
                        lastAction = event.getAction();
                        return true;
                }
                return false;
            }
        });
    }

    private void setViewSize(View view, int width) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = (int) (width * HW_RATE);
        view.setLayoutParams(layoutParams);
    }

    public void loadImage(String url) {
        Glide.with(this).load(url).placeholder(circularProgressDrawable).into(photoView);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootView != null) windowManager.removeView(rootView);
    }
}
