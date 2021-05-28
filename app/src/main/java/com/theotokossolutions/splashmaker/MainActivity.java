package com.theotokossolutions.splashmaker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.theotokossolutions.splashmaker.R;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //PI value
    static private float PI = 3.14159f;

    //Screen Info and Objects
    private int pixelWidth;
    private int pixelHeight;
    private Bitmap bitmap;
    private Bitmap testBit;
    private ImageView imageView;
    private int bitmapHeight;
    private int bitmapWidth;
    private int bitmapSize;
    private IntBuffer buf;
    private IntBuffer retBuf;
    private int[] byt;

    //PixelMap and Photons
    private PixelColor[][] PixelMap;
    private PixelColor[] PixelArray;
    private Photon photonHead;
    private Photon photonTail;

    //Important variables for the photons
    private float masterSpeed = 7.0f;
    private int masterRed  = 255;
    private int masterGreen = 255;
    private int masterBlue = 255;
    private int masterHue = 255;
    private int masterDecay = 2;
    private float masterWidth = 30;
    private int masterShape = 3;
    private float masterRotation = PI + 1.5f;
    private int backgroundRed  = 0;
    private int backgroundGreen = 0;
    private int backgroundBlue = 0;
    private int backgroundHue = 255;
    private int shapeLimit = 400;
    private int shapeTotal = 0;
    private float taperDecay = 1.0f;

    //Seek Bars for color change
    private SeekBar red_select;
    private SeekBar green_select;
    private SeekBar blue_select;

    ActivityThread thread;
    MainActivity main = this;

    //Boolean variables
    private boolean lock = false;
    private volatile boolean bitmapTurn = true;
    private boolean first = true;
    private boolean pause = false;
    private boolean runBitmap = true;
    private boolean stopBitmap = false;

    //Bitmap Handler
    Handler handler = new Handler();
    Runnable draw = new Runnable() {
        @Override
        public void run() {
            if(runBitmap) {
                drawBitmap();
                handler.postDelayed(draw, 1);
            }
            else{
                if(!stopBitmap) handler.postDelayed(draw, 700);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Hide Title Bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Buttons
        Button circle_button = (Button) findViewById(R.id.circle_button);
        Button triangle_button = (Button) findViewById(R.id.triangle_button);
        Button square_button = (Button) findViewById(R.id.square_button);
        final Button pause_button = (Button) findViewById(R.id.pause_button);
        Button camera_button = (Button) findViewById(R.id.camera_button);

        //Drawables
        final Drawable pauseIcon = getDrawable(R.drawable.ic_pause);
        final Drawable playIcon = getDrawable(R.drawable.ic_play);

        //Circle button
        circle_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                masterShape = 12;
            }
        });

        //Triangle button
        triangle_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                masterShape = 3;
            }
        });

        //Circle button
        square_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                masterShape = 4;
            }
        });

        //Pause button
        pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pause) {
                    runBitmap = false;
                    pause = true;
                    pause_button.setForeground(playIcon);
                }
                else{
                    runBitmap = true;
                    pause = false;
                    pause_button.setForeground(pauseIcon);
                }
            }
        });

        //Camera Button
        camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Requesting Permission to access External Storage
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED)
                {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                }
                else {
                    boolean failed1 = false;
                    boolean failed2 = false;
                    boolean failed3 = false;

                    //Get date and time
                    DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                    String date = df.format(Calendar.getInstance().getTime());

                    //Prepare image
                    ImageView image = findViewById(R.id.image);
                    image.buildDrawingCache();
                    Bitmap bm = image.getDrawingCache();

                    //Output file
                    OutputStream fOut = null;
                    Uri outputFileUri;
                    File root = null;

                    try{
                        Context context = getApplicationContext();
                        ContentResolver resolver = context.getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "splash" + date);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                        OutputStream out = resolver.openOutputStream(uri);
                        bm.compress(Bitmap.CompressFormat.PNG, 100, out);

                        failed2 = false;
                    } catch (Exception e){
                        failed2 = true;
                    }

                    if(failed2){
                        try {
                            //Get Pictures Directory
                            root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
                            if (!root.exists()) root.mkdirs();
                            File sdImageMainDirectory = new File(root, "splash" + date + ".png");
                            outputFileUri = Uri.fromFile(sdImageMainDirectory);
                            fOut = new FileOutputStream(sdImageMainDirectory);
                        } catch (Exception e) {
                            failed1 = true;
                        }

                        try {
                            //Compress Bitmap
                            bm.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                            fOut.flush();
                            fOut.close();
                        } catch (Exception e) {
                            failed3 = true;
                        }
                    }

                    if (!failed2){
                        Toast t = Toast.makeText(MainActivity.this, "Screenshot stored at: " + Environment.DIRECTORY_PICTURES, Toast.LENGTH_SHORT);
                        t.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                        t.show();
                    }
                    else if(!failed1){
                        Toast t = Toast.makeText(MainActivity.this, "Screenshot stored at: " + root, Toast.LENGTH_SHORT);
                        t.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                        t.show();
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Compression error. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

        });

        //Seek bars
        red_select = (SeekBar) findViewById(R.id.red_select);
        green_select = (SeekBar) findViewById(R.id.green_select);
        blue_select = (SeekBar) findViewById(R.id.blue_select);

        //Red Select
        red_select.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                masterRed = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Green Select
        green_select.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                masterGreen = progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }


            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        //Blue Select
        blue_select.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                masterBlue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        //Set max values for seek bar
        red_select.setMax(255);
        blue_select.setMax(255);
        green_select.setMax(255);

        //Get ImageView
        imageView = findViewById(R.id.image);

        //On Touch Listener - Add Photon Vectors
        imageView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                 float touchX = event.getX();
                 float touchY = event.getY();

                lock = true;

                if(event.getAction() == MotionEvent.ACTION_UP && shapeTotal < shapeLimit){          //Create Photons and add them to Photon List

                    shapeTotal+=masterShape;
                    float ang = ((2.0f * PI ) /  masterShape);
                    for(int i = 0; i < masterShape; i++){
                        float angle = ((2.0f * PI ) /  masterShape) * (i+1) + masterRotation;
                        float directionX = (float) (Math.sin(angle));
                        float directionY = (float) (Math.cos(angle));

                        if (directionX < 0.01f && directionX > -0.01f) directionX = 0;
                        if (directionY < 0.01f && directionY > -0.01f) directionY = 0;

                        if(photonHead == null){
                            Photon temp = new Photon(touchX, touchY, masterSpeed, directionX, directionY, ang, 0,
                                    masterDecay, masterRed, masterGreen, masterBlue, masterHue, masterShape, 0, masterWidth);
                            photonHead = temp;
                            photonTail = temp;
                        }
                        else if(photonHead == photonTail){
                            photonTail = new Photon(touchX, touchY, masterSpeed, directionX, directionY, ang, 0,
                                    masterDecay, masterRed, masterGreen, masterBlue, masterHue, masterShape, 0, masterWidth);
                            photonHead.nextPhoton = photonTail;
                        }
                        else {
                            Photon temp = new Photon(touchX, touchY, masterSpeed, directionX, directionY, ang, 0,
                                    masterDecay, masterRed, masterGreen, masterBlue, masterHue, masterShape, 0, masterWidth);
                            photonTail.nextPhoton = temp;
                            photonTail = temp;
                        }
                    }
                }

                lock = false;
                return true;
            }
        });

        //Bitmap and Thread
        if(imageView.getViewTreeObserver().isAlive()) {
            imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if(imageView.getViewTreeObserver().isAlive()) {
                        // only need to calculate once, so remove listener
                        imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }

                    //imageview's height and width here
                    pixelHeight = imageView.getHeight();
                    pixelWidth = imageView.getWidth();

                    //Initialize Pixel Array
                    PixelArray = new PixelColor[pixelWidth*pixelHeight];
                    for(int i = 0; i < pixelHeight*pixelWidth; i++){
                        PixelArray[i] = new PixelColor(255, 255, 255, 255, 0, PixelColor.Status.NO_DRAW);
                    }

                    //Create Bitmap
                    bitmap = Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.ARGB_8888);
                    imageView.setImageBitmap(bitmap);
                    drawBitmap();

                    bitmapHeight = bitmap.getHeight();
                    bitmapWidth = bitmap.getRowBytes();
                    bitmapSize = bitmapWidth * bitmapHeight;

                    //Start Thread
                    thread = new ActivityThread(main);
                    thread.runState(true);
                    thread.start();

                }
            });
        }

        handler.postDelayed(draw, 1000);

    }


    public void updatePhotons(){
        Photon temp = photonHead;
        Photon previous = null;

        //Iterate through Photon List
        while(temp != null){

            if(lock) continue;      //LOCK

            //Perform Photon Calculations
            temp.xPosition = temp.xPosition + (temp.directionX * temp.speed);                       //Update X position
            temp.yPosition = temp.yPosition + (temp.directionY * temp.speed);                       //Update Y Position
            temp.radius = temp.radius + temp.speed;                                                 //Update Radius
            temp.length = (int) (temp.radius * (float) Math.tan((double) (temp.angle/2.0f)));       //Update Length
            temp.hue = temp.hue - temp.decaySpeed;                                                  //Update Life (Hue Value)

            //Remove Photon if out of life
            if(temp.hue < 1){
                if(temp == photonHead && temp == photonTail){
                    photonHead = null;
                    photonTail = null;
                    temp = null;
                }
                else if(temp == photonHead){
                    photonHead = temp.nextPhoton;
                    previous = null;
                    temp = photonHead;
                }
                else if(temp == photonTail){
                    previous.nextPhoton = null;
                    temp = null;
                }
                else{
                    previous.nextPhoton = temp.nextPhoton;
                    temp = temp.nextPhoton;
                }
                shapeTotal--;
            }
            else{
                previous = temp;
                temp = temp.nextPhoton;
            }
        }
    }

    public void updatePixelMap(){

        if(PixelArray != null) {
            PixelColor color;
            while(true){
                if (bitmapTurn) continue;

                Photon temp = photonHead;
                float currentPixelX;
                float currentPixelY;

                while (temp != null) {

                    //To the right of Photon
                    currentPixelX = temp.xPosition;
                    currentPixelY = temp.yPosition;

                    for (int len = 0; len <= temp.length; len++) {

                        float widthFade = 255.0f /  (float) masterWidth;
                        float tempFade = widthFade;
                        float cpx = currentPixelX;
                        float cpy = currentPixelY;
                        for (int w = 0; w <= masterWidth && w <= temp.radius && widthFade <= temp.hue; w++) {           //

                            //Out of screen, bounce
                            float tempX = currentPixelX;
                            float tempY = currentPixelY;
                            if(currentPixelX > pixelWidth - 1) currentPixelX = (2 * pixelWidth - 1) - currentPixelX;
                            if(currentPixelX < 0) currentPixelX *= -1;
                            if(currentPixelY > pixelHeight) currentPixelY = (2 * pixelHeight - 1) - currentPixelY;
                            if(currentPixelY < 0) currentPixelY *= -1;

                            //Get pixel
                            if(currentPixelX >= pixelWidth || currentPixelY >= pixelHeight) continue;
                            //color = PixelMap[(int) currentPixelX][(int) currentPixelY];
                            color = PixelArray[((int)currentPixelY * (pixelWidth)) + (int) currentPixelX];


                            //Get weight values
                            float weight1 = 1.0f / (float) color.weight;
                            float weight2 = 1.0f - weight1;
                            weight2 -= ((((float) (w+1))/(float)masterWidth) * weight2);
                            weight1 = 1.0f - weight2;

                            //Color Calculations
                            color.red = (int) ((((float) temp.red) * weight1) + (((float) color.red) * weight2));
                            color.green = (int) ((((float) temp.green) * weight1) + (((float) color.green) * weight2));
                            color.blue = (int) ((((float) temp.blue) * weight1) + (((float) color.blue) * weight2));
                            color.hue = (int) ((((float) temp.hue - widthFade) * weight1) + (((float) color.hue) * weight2));
                            if(color.red > 255) color.red = 255;
                            if(color.blue > 255) color.blue = 255;
                            if(color.green > 255) color.green = 255;
                            if(color.hue > 255) color.hue = 255;
                            color.weight++;
                            color.status = PixelColor.Status.DRAW;

                            //Set Pixel
                            //PixelMap[(int) currentPixelX][(int) currentPixelY] = color;
                            PixelArray[((int)currentPixelY * (pixelWidth)) + (int) currentPixelX] = color;


                            widthFade += tempFade;


                            currentPixelX = tempX - temp.directionX;
                            currentPixelY = tempY - temp.directionY;
                        }

                        currentPixelX = cpx + temp.directionY;
                        currentPixelY = cpy - temp.directionX;
                    }

                    //To the left of Photon
                    currentPixelX = temp.xPosition;
                    currentPixelY = temp.yPosition;

                    for (int len = 0; len <= temp.length; len++) {

                        float widthFade = 255.0f / (float) masterWidth;
                        float tempFade = widthFade;
                        float cpx = currentPixelX;
                        float cpy = currentPixelY;
                        for (int w = 0; w <= masterWidth && w <= temp.radius && widthFade <= temp.hue; w++) {               //

                            //Out of screen, bounce
                            float tempX = currentPixelX;
                            float tempY = currentPixelY;

                            if(currentPixelX > pixelWidth - 1) currentPixelX = (2 * pixelWidth - 1) - currentPixelX;
                            if(currentPixelX < 0) currentPixelX *= -1;
                            if(currentPixelY > pixelHeight) currentPixelY = (2 * pixelHeight - 1) - currentPixelY;
                            if(currentPixelY < 0) currentPixelY *= -1;

                            //Get Pixel
                            if(currentPixelX >= pixelWidth || currentPixelY >= pixelHeight) continue;
                            //color = PixelMap[(int) currentPixelX][(int) currentPixelY];
                            color = PixelArray[((int)currentPixelY * (pixelWidth)) + (int) currentPixelX];


                            //Get weights
                            float weight1 = 1.0f / (float) color.weight;
                            float weight2 = 1.0f - weight1;
                            weight2 -= ((((float) (w+1))/(float)masterWidth) * weight2);
                            weight1 = 1.0f - weight2;

                            color.red = (int) ((((float) temp.red) * weight1) + (((float) color.red) * weight2));
                            color.green = (int) ((((float) temp.green) * weight1) + (((float) color.green) * weight2));
                            color.blue = (int) ((((float) temp.blue) * weight1) + (((float) color.blue) * weight2));
                            color.hue = (int) ((((float) temp.hue  - widthFade) * weight1) + (((float) color.hue) * weight2));
                            if(color.red > 255) color.red = 255;
                            if(color.blue > 255) color.blue = 255;
                            if(color.green > 255) color.green = 255;
                            if(color.hue > 255) color.hue = 255;
                            color.weight++;
                            color.status = PixelColor.Status.DRAW;

                            //Set Pixel
                            //PixelMap[(int) currentPixelX][(int) currentPixelY] = color;
                            PixelArray[((int)currentPixelY * (pixelWidth)) + (int) currentPixelX] = color;

                            widthFade += tempFade;

                            currentPixelX = tempX - temp.directionX;
                            currentPixelY = tempY - temp.directionY;
                        }

                        currentPixelX = cpx - temp.directionY;
                        currentPixelY = cpy + temp.directionX;
                    }

                    temp = temp.nextPhoton;
                }

                bitmapTurn = true;
                break;
            }
        }
    }

    public void drawBitmap(){

        masterRotation += 0.3;

        while(true){
            if(!bitmapTurn) continue;

            //If bitmap hasn't been initialized yet
            if(bitmapWidth != bitmap.getRowBytes() && bitmapHeight != bitmap.getHeight()) {
                bitmapWidth = bitmap.getRowBytes();
                bitmapHeight = bitmap.getHeight();
                bitmapSize = bitmapWidth * bitmapHeight;
                buf = IntBuffer.allocate(bitmapSize);
                byt = buf.array();
            }

            if(bitmapWidth == bitmap.getRowBytes() && bitmapHeight == bitmap.getHeight()) {
                //Iterate through PixelArray and byt buffer, performing pixel manipulation
                for (int ctr = 0; ctr < pixelWidth * pixelHeight; ctr++){
                    if(PixelArray[ctr].status == PixelColor.Status.WAS_DRAWN){
                        byt[ctr] = (backgroundHue & 0xff) << 24 | (backgroundBlue & 0xff) << 16 | (backgroundGreen & 0xff) << 8 | (backgroundRed & 0xff); //ABGR
                        PixelArray[ctr].status = PixelColor.Status.NO_DRAW;
                        PixelArray[ctr].red = backgroundRed;
                        PixelArray[ctr].green = backgroundGreen;
                        PixelArray[ctr].blue = backgroundBlue;
                        PixelArray[ctr].hue = backgroundHue;
                        PixelArray[ctr].weight = 0;
                    } else if (PixelArray[ctr].status == PixelColor.Status.DRAW) {
                        byt[ctr] = (PixelArray[ctr].hue & 0xff) << 24 | (PixelArray[ctr].blue & 0xff) << 16 | (PixelArray[ctr].green & 0xff) << 8 | (PixelArray[ctr].red & 0xff); //ABGR
                        PixelArray[ctr].status = PixelColor.Status.WAS_DRAWN;
                        PixelArray[ctr].weight = 1;
                    }
                }

                //Reset Bitmap
                retBuf = IntBuffer.wrap(byt);
                bitmap.copyPixelsFromBuffer(retBuf);
                imageView.setImageBitmap(bitmap);

                bitmapTurn = false;
                break;
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        runBitmap = false;
    }

    @Override
    public void onStop(){
        super.onStop();
        runBitmap = false;
    }

    @Override
    public void onResume(){
        super.onResume();
        if(!pause) runBitmap = true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopBitmap = true;
    }
}