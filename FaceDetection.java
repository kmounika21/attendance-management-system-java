package com.attendance;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

public class FaceDetection {
    public static void main(String[] args) {
        // Load OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("✅ OpenCV library loaded successfully!");

        // Path to Haar Cascade file (inside src/resources)
        String cascadePath = System.getProperty("user.dir") + "/src/resources/haarcascade_frontalface_default.xml";

        // Load the cascade classifier
        CascadeClassifier faceCascade = new CascadeClassifier(cascadePath);
        if (faceCascade.empty()) {
            System.out.println("❌ Failed to load cascade file at: " + cascadePath);
            return;
        } else {
            System.out.println("✅ Cascade loaded successfully from: " + cascadePath);
        }

        // Open the webcam
        VideoCapture camera = new VideoCapture(0, 700);
        if (!camera.isOpened()) {
            System.out.println("❌ Error: Camera not detected!");
            return;
        }

        Mat frame = new Mat();
        Mat gray = new Mat();
        System.out.println("📸 Press 'q' or 'Esc' to close the camera.");

        while (true) {
            if (!camera.read(frame)) {
                System.out.println("⚠️ Unable to read frame.");
                break;
            }

            // Convert to grayscale
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            // Detect faces
            MatOfRect faces = new MatOfRect();
            faceCascade.detectMultiScale(gray, faces, 1.1, 5, 0, new Size(60, 60), new Size());

            // Draw rectangles around faces
            for (Rect rect : faces.toArray()) {
                Imgproc.rectangle(
                        frame,
                        new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0),
                        2
                );
            }

            // Show camera feed
            HighGui.imshow("Live Camera Feed (Face Detection)", frame);

            int key = HighGui.waitKey(1);
            if (key == 113 || key == 27) { // 'q' or ESC
                System.out.println("👋 Exit key pressed.");
                break;
            }
        }

        // Release resources
        camera.release();
        HighGui.destroyAllWindows();
        System.out.println("🔒 Camera released successfully.");
    }
}
