package com.example.oceo.speedread;

public class SpeedReadUtilities {
    public static long WPMtoMS(long WPM) {
        return Math.round(1000.0 / (WPM / 60.0));
    }
}
