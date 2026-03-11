package com.attendance;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.javacpp.IntPointer;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class FaceTrainer {

    private static final String DATASET_DIR = System.getProperty("user.dir") + "/dataset/";
    private static final String TRAINER_DIR = System.getProperty("user.dir") + "/trainer/";
    private static final String TRAINER_FILE = TRAINER_DIR + "trainer.yml";
    private static final String LABELS_CSV = TRAINER_DIR + "labels.csv";

    public static void main(String[] args) {
        try {
            System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
            System.out.println("✅ OpenCV library loaded successfully!");

            File datasetRoot = new File(DATASET_DIR);
            if (!datasetRoot.exists()) {
                System.out.println("❌ Dataset folder not found!");
                return;
            }

            File[] personFolders = datasetRoot.listFiles(File::isDirectory);
            if (personFolders == null || personFolders.length == 0) {
                System.out.println("❌ No person folders found in dataset/");
                return;
            }

            new File(TRAINER_DIR).mkdirs();

            // Step 1️⃣: Count all valid images first
            int totalImages = 0;
            for (File folder : personFolders) {
                File[] imageFiles = folder.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".jpg") ||
                                name.toLowerCase().endsWith(".png"));
                if (imageFiles != null) totalImages += imageFiles.length;
            }

            if (totalImages == 0) {
                System.out.println("❌ No valid images found in any folder!");
                return;
            }

            // Step 2️⃣: Allocate exact vector size
            MatVector images = new MatVector(totalImages);
            IntPointer labels = new IntPointer(totalImages);

            int counter = 0;
            int label = 0;

            try (PrintWriter pw = new PrintWriter(new FileWriter(LABELS_CSV))) {
                pw.println("label,name");

                for (File folder : personFolders) {
                    String personName = folder.getName();
                    pw.println(label + "," + personName);
                    System.out.println("📂 Loading images for: " + personName);

                    File[] imageFiles = folder.listFiles((dir, name) ->
                            name.toLowerCase().endsWith(".jpg") ||
                                    name.toLowerCase().endsWith(".png"));

                    if (imageFiles == null || imageFiles.length == 0) {
                        System.out.println("⚠️ No images in " + personName + ", skipping...");
                        continue;
                    }

                    for (File image : imageFiles) {
                        Mat img = opencv_imgcodecs.imread(image.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                        if (img.empty()) {
                            System.out.println("⚠️ Skipped invalid image: " + image.getName());
                            continue;
                        }

                        opencv_imgproc.resize(img, img, new Size(200, 200));
                        images.put(counter, img);
                        labels.put(counter, label);
                        counter++;
                    }
                    label++;
                }
            }

            if (counter == 0) {
                System.out.println("❌ No valid training data after loading!");
                return;
            }

            // Step 3️⃣: Train the recognizer
            System.out.println("🧠 Training model on " + counter + " face images...");
            Mat labelsMat = new Mat(counter, 1, opencv_core.CV_32SC1);
            labelsMat.data().put(labels);

            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            recognizer.train(images, labelsMat);
            recognizer.save(TRAINER_FILE);

            System.out.println("✅ Model trained successfully!");
            System.out.println("📁 Saved trainer.yml at: " + TRAINER_FILE);
            System.out.println("📋 Label map saved at: " + LABELS_CSV);

        } catch (Exception e) {
            System.err.println("⚠️ Training failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // Wrapper to call training from menu
    public void trainModel() {
        try {
            // if existing method name is train or similar, attempt to call it
            try {
                java.lang.reflect.Method m = this.getClass().getMethod("train");
                m.invoke(this);
            } catch (NoSuchMethodException nsme) {
                try {
                    java.lang.reflect.Method m2 = this.getClass().getMethod("trainRecognizer");
                    m2.invoke(this);
                } catch (NoSuchMethodException ex) {
                    System.out.println("Please implement a train() method in FaceTrainer to perform training.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
