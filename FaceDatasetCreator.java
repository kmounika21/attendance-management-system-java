package com.attendance;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.swing.*;
import java.io.File;

public class FaceDatasetCreator {

    public static void main(String[] args) {
        // Load OpenCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("✅ OpenCV loaded successfully!");

        // Ask for person name
        String personName = JOptionPane.showInputDialog(null,
                "Enter the person's name for dataset capture:",
                "Face Dataset Creator", JOptionPane.PLAIN_MESSAGE);

        if (personName == null || personName.trim().isEmpty()) {
            System.out.println("❌ No name entered. Exiting...");
            return;
        }

        // Create folder
        String datasetPath = System.getProperty("user.dir") + "/dataset/" + personName;
        File folder = new File(datasetPath);
        if (!folder.exists()) folder.mkdirs();

        // Load cascade classifier
        String cascadePath = System.getProperty("user.dir") + "/src/resources/haarcascade_frontalface_default.xml";
        CascadeClassifier faceDetector = new CascadeClassifier(cascadePath);

        // Initialize camera
        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("❌ Cannot open camera!");
            return;
        }

        // Create window for live feed
        CanvasFrame cameraWindow = new CanvasFrame("Capturing Dataset for " + personName, 1);
        cameraWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Mat frame = new Mat();
        int sampleCount = 0;
        int totalSamples = 80; // Number of images to capture
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        System.out.println("📸 Capturing dataset for " + personName + "...");
        System.out.println("Press 'X' to stop capturing.");

        while (camera.isOpened()) {
            if (!camera.read(frame)) break;

            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(gray, faces);

            for (Rect rect : faces.toArray()) {
                // Draw box around detected face
                Imgproc.rectangle(frame, rect, new Scalar(0, 255, 0, 0), 2);
                Mat face = new Mat(gray, rect);
                Imgproc.resize(face, face, new Size(200, 200));

                // Save image
                String filename = datasetPath + "/" + personName + "_" + sampleCount + ".jpg";
                Imgcodecs.imwrite(filename, face);
                sampleCount++;

                System.out.println("🖼 Saved: " + filename);
            }

            // Show camera feed in live window
            cameraWindow.showImage(converter.convert(frame));

            // Stop conditions
            if (!cameraWindow.isVisible() || sampleCount >= totalSamples) {
                break;
            }
        }

        camera.release();
        cameraWindow.dispose();

        System.out.println("✅ Dataset collection complete for " + personName);
        System.out.println("📁 Saved " + sampleCount + " images in folder: " + datasetPath);
    }


    // Interactive method to ask for name and roll and capture dataset
    public void captureDatasetInteractive() {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        System.out.print("Enter Student Name: ");
        String name = sc.nextLine().trim().replaceAll("\\\\s+","_");
        System.out.print("Enter Roll Number: ");
        String roll = sc.nextLine().trim().replaceAll("\\\\s+","_");
        String folderName = roll + "_" + name;
        java.io.File d = new java.io.File(System.getProperty("user.dir") + File.separator + "dataset" + File.separator + folderName);
        if (!d.exists()) d.mkdirs();
        System.out.println("Dataset folder created at: " + d.getAbsolutePath());
        // call existing capture method if present
        try {
            // If original class had captureDataset(folder) method, try to call it via reflection
            try {
                java.lang.reflect.Method m = this.getClass().getMethod("captureDataset", String.class);
                m.invoke(this, d.getAbsolutePath());
            } catch (NoSuchMethodException nsme) {
                System.out.println("Please implement captureDataset(String folderPath) in FaceDatasetCreator to automatically capture images.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
