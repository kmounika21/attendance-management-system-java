package com.attendance;

import java.io.File;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Aurora University Attendance System ===");
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Add New Student (Capture Dataset)");
            System.out.println("2. Train Face Recognition Model");
            System.out.println("3. Start Attendance Detection (Dashboard)");
            System.out.println("4. Exit");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            try {
                if ("1".equals(choice)) {
                    FaceDatasetCreator creator = new FaceDatasetCreator();
                    creator.captureDatasetInteractive();
                } else if ("2".equals(choice)) {
                    FaceTrainer trainer = new FaceTrainer();
                    trainer.trainModel();
                } else if ("3".equals(choice)) {
                    FaceDetectionWithAttendance detector = new FaceDetectionWithAttendance();
                    detector.startWithDashboard();
                } else if ("4".equals(choice)) {
                    System.out.println("Exiting. Goodbye!");
                    break;
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        sc.close();
    }

    // helper to build paths
    public static String resourcesPath() {
        return System.getProperty("user.dir") + File.separator + "src" + File.separator + "resources";
    }
}