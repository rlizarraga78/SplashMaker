package com.example.splashmaker;

public class PixelColor {
    int red;
    int green;
    int blue;
    int hue;
    int weight;

    enum Status{
        NO_DRAW, WAS_DRAWN, WAS_DRAWN2, DRAW
    }
    Status status;

    public PixelColor(int r, int g, int b, int a, int w, Status s){
        red = r;
        green = g;
        blue = b;
        hue = a;
        weight = w;
        status = s;
    }
}
