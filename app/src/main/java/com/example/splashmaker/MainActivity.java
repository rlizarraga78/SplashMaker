package com.example.splashmaker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private int pixelWidth;
    private int pixelHeight;
    private Bitmap bitmap;
    private ImageView imageView;
    private PixelColor[][] PixelMap;
    private boolean pause = false;
    private Photon photonHead;
    private Photon photonTail;
    private float masterSpeed = 7.0f;
    private int masterRed  = 0;
    private int masterGreen = 0;
    private int masterBlue = 200;
    private int masterHue = 255;
    private int masterDecay = 7;
    private float masterWidth = 3;
    private int masterShape = 20;

    static private float PI = 3.14159f;

    ActivityThread thread;
    MainActivity main = this;

    //BackgroundThread backgroundThread;

    private boolean lock = false;
    private volatile boolean bitmapTurn = true;

    Handler handler = new Handler();
    Runnable draw = new Runnable() {
        @Override
        public void run() {
            drawBitmap();
            handler.postDelayed(draw, 1);
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get ImageView
        imageView = findViewById(R.id.image);

        //On Touch Listener - Add Photon Vectors
        imageView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                 float touchX = event.getX();
                 float touchY = event.getY();

                        //TEST POSITION
                        TextView test = (TextView) findViewById(R.id.test);
                        test.setText(""+" "+ touchX + "       " + touchY);

                //Log.d("TOUCH: " , ""+ (Looper.myLooper() == Looper.getMainLooper()));

                lock = true;

                if(event.getAction() == MotionEvent.ACTION_UP){

                    float ang = ((2.0f * PI ) /  masterShape);
                    for(int i = 0; i < masterShape; i++){
                        float angle = ((2.0f * PI ) /  masterShape) * (i+1);
                        float directionX = (float) (Math.sin(angle));
                        float directionY = (float) (Math.cos(angle));

                        if (directionX < 0.01f && directionX > -0.01f) directionX = 0;
                        if (directionY < 0.01f && directionY > -0.01f) directionY = 0;

                        //Log.e("TOUCH", "DX: " + directionX + "  DY: " + directionY + "  ANG: " + ang + "  ANGLE: " + angle);

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

                Photon tempp = photonHead;
                int j = 0;
                while(tempp != null){
                    j++;
                    tempp = tempp.nextPhoton;
                }


                TextView test2 = (TextView) findViewById(R.id.test2);
                test2.setText("\n# of Nodes: "+j);

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
                    Log.d("DDDD", "" +pixelHeight+", " + pixelWidth);

                    //Photon List and Pixel Maps
                    PixelMap = new PixelColor[pixelWidth][pixelHeight];
                    for(int j = 0; j < pixelHeight; j++){
                        for(int i = 0; i < pixelWidth; i++){
                            PixelColor temp = new PixelColor(255, 255, 255, 255, 1, PixelColor.Status.NO_DRAW);
                            PixelMap[i][j] = temp;
                        }
                    }

                    //Create Bitmap
                    bitmap = Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.ARGB_8888);
                    imageView.setImageBitmap(bitmap);
                    drawBitmap();

                    // the listener might not have been 'alive', and so might not have been removed....so you are only
                    // 99% sure the code you put here will only run once.  So, you might also want to put some kind
                    // of "only run the first time called" guard in here too

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
            }
            else{
                previous = temp;
                temp = temp.nextPhoton;
            }
        }
    }

    public void updatePixelMap(){

        if(PixelMap != null) {
            while(true){
                if (bitmapTurn) continue;

                Log.e("PIXEL", "TESTZ");

                /**
                //Clear pixel map
                for (int j = 0; j < PixelMap[0].length; j++) {
                    for (int i = 0; i < PixelMap.length; i++) {
                        PixelMap[i][j].weight = 1;
                        PixelMap[i][j].red = 255;
                        PixelMap[i][j].green = 255;
                        PixelMap[i][j].blue = 255;
                        PixelMap[i][j].hue = 255;
                        PixelMap[i][j].draw = false;
                    }
                }
*/

                Log.e("PIXEL", "TESTA");

                Photon temp = photonHead;
                float currentPixelX;
                float currentPixelY;

                while (temp != null) {

                    //To the right of Photon
                    currentPixelX = temp.xPosition;
                    currentPixelY = temp.yPosition;

                    for (int len = 0; len <= temp.length; len++) {

                        float widthFade = 255.0f /  masterWidth;
                        float cpx = currentPixelX;
                        float cpy = currentPixelY;
                        for (int w = 0; w <= masterWidth && w <= temp.radius; w++) {
                            PixelColor color = PixelMap[(int) currentPixelX][(int) currentPixelY];

                            float weight1 = 1.0f / (float) color.weight;
                            float weight2 = 1.0f - weight1;

                            color.red = (int) ((((float) temp.red) * weight1) + (((float) color.red) * weight2));
                            color.green = (int) ((((float) temp.green) * weight1) + (((float) color.green) * weight2));
                            color.blue = (int) ((((float) temp.blue) * weight1) + (((float) color.blue) * weight2));
                            color.hue = (int) ((((float) temp.hue - widthFade) * weight1) + (((float) color.hue) * weight2));
                            color.weight++;
                            color.status = PixelColor.Status.DRAW;

                            //Log.e("COLORS: ", "Weight1: " + weight1 + "  Weight2: " + weight2 + "   tempBlue: " + temp.blue + "  colorBlue: " + color.blue);

                            PixelMap[(int) currentPixelX][(int) currentPixelY] = color;

                            widthFade *= w;
                            if(widthFade > 255) break;


                            currentPixelX = currentPixelX - temp.directionX;
                            currentPixelY = currentPixelY - temp.directionY;
                        }

                        currentPixelX = cpx + temp.directionY;
                        currentPixelY = cpy - temp.directionX;

                        //Log.e("Current", "X: " + currentPixelX + "  Y: "+currentPixelY + "  Length: " + temp.length);
                    }

                    //To the left of Photon
                    currentPixelX = temp.xPosition;
                    currentPixelY = temp.yPosition;

                    for (int len = 0; len <= temp.length; len++) {

                        float widthFade = 255.0f / (float) masterWidth;
                        float cpx = currentPixelX;
                        float cpy = currentPixelY;
                        for (int w = 0; w <= masterWidth && w <= temp.radius; w++) {
                            PixelColor color = PixelMap[(int) currentPixelX][(int) currentPixelY];

                            float weight1 = 1.0f / (float) color.weight;
                            float weight2 = 1.0f - weight1;

                            color.red = (int) ((((float) temp.red) * weight1) + (((float) color.red) * weight2));
                            color.green = (int) ((((float) temp.green) * weight1) + (((float) color.green) * weight2));
                            color.blue = (int) ((((float) temp.blue) * weight1) + (((float) color.blue) * weight2));
                            color.hue = (int) ((((float) temp.hue  - widthFade) * weight1) + (((float) color.hue) * weight2));
                            color.weight++;
                            color.status = PixelColor.Status.DRAW;

                            PixelMap[(int) currentPixelX][(int) currentPixelY] = color;

                            widthFade *= w;
                            if(widthFade > 250) break;

                            currentPixelX = currentPixelX - temp.directionX;
                            currentPixelY = currentPixelY - temp.directionY;
                        }

                        currentPixelX = cpx - temp.directionY;
                        currentPixelY = cpy + temp.directionX;
                    }

                    temp = temp.nextPhoton;

                }

                Log.e("PIXEL", "TESTB");
                bitmapTurn = true;
                break;
            }
        }
    }

    public void drawBitmap(){

        //Log.e("BITMAP: ", "A: " + bitmapTurn);
int test = 0;
        while(true){
            if(!bitmapTurn) continue;
            //Log.e("BITMAP: ", "B: " + bitmapTurn);

            int size = bitmap.getRowBytes() * bitmap.getHeight();
            IntBuffer buf = IntBuffer.allocate(size);
            bitmap.copyPixelsToBuffer(buf);
            int[] byt = buf.array();

            Log.e("BITMAP TIME: ", "TEST1");

            int i = 0;
            int j = 0;
            for (int ctr = 0; i < pixelWidth; ctr+=pixelWidth) {
                test++;
                //If i value greater than size, reset
                //if(ctr >= size){
                //    i++;
                //    ctr = i;
                //    if(i >= pixelWidth) break;
                //}

                //Update buffer colors
                //If no draw, skip
                if(PixelMap[i][j].status == PixelColor.Status.WAS_DRAWN){
                    byt[ctr] = (255 & 0xff) << 24 | (255 & 0xff) << 16 | (255 & 0xff) << 8 | (255 & 0xff); //ABGR
                    PixelMap[i][j].status = PixelColor.Status.NO_DRAW;
                    PixelMap[i][j].red = 255;
                    PixelMap[i][j].green = 255;
                    PixelMap[i][j].blue = 255;
                    PixelMap[i][j].hue = 255;
                }
                else if (PixelMap[i][j].status == PixelColor.Status.DRAW){
                    byt[ctr] = (PixelMap[i][j].hue & 0xff) << 24 | (PixelMap[i][j].blue & 0xff) << 16 | (PixelMap[i][j].green & 0xff) << 8 | (PixelMap[i][j].red & 0xff); //ABGR
                    PixelMap[i][j].status = PixelColor.Status.WAS_DRAWN;
                    PixelMap[i][j].weight = 1;
                }
                //if(size % 1000 == 0) Log.e("COLORS", "R: " + PixelMap[i][j].red+ " G: " + PixelMap[i][j].green+ " B: " + PixelMap[i][j].blue+ " A: " +PixelMap[i][j].hue);

                //Increment j value
                j++;
                if(j >= pixelHeight) {j = 0; i++; ctr = i;}
            }

            Log.e("BITMAP TIME: ", "TEST2");

            IntBuffer retBuf = IntBuffer.wrap(byt);
            bitmap.copyPixelsFromBuffer(retBuf);
            imageView.setImageBitmap(bitmap);

            Log.e("TEST NUM", ""+test);

            bitmapTurn = false;
            break;
        }
    }
}
