package com.example.splashmaker;

import android.graphics.Color;

public class Photon {
    //Instance Variables
    float xPosition;
    float yPosition;
    float speed;
    float directionX;
    float directionY;
    float angle;
    float radius;
    int decaySpeed;
    int red;
    int green;
    int blue;
    int hue;
    int shape;
    int length;
    float width;
    Photon nextPhoton;

    public Photon(float x, float y, float s, float dx, float dy, float an, float ra, int decay,
                  int r, int g, int b, int a, int shape, int l, float w) {
        this.xPosition = x;
        this.yPosition = y;
        this.speed = s;
        this.directionX = dx;
        this.directionY = dy;
        this.angle = an;
        this.radius = ra;
        this.decaySpeed = decay;
        this.red = r;
        this.green = g;
        this.blue = b;
        this.hue = a;
        this.shape = shape;
        this.length = l;
        this.width = w;
    }
}
