package com.attendance;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;

public class TestOpenCV {
    public static void main(String[] args) {
        try {
            Loader.load(opencv_core.class);
            System.out.println("✅ OpenCV is working perfectly with JavaCV!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

