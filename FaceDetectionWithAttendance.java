package com.attendance;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Aurora University – AMIL 2C
 * Smart Face Attendance (Excel version)
 */
public class FaceDetectionWithAttendance {

    // ---------- PATHS ----------
    private static final String BASE_PATH = System.getProperty("user.dir");
    private static final String DATASET_PATH = BASE_PATH + File.separator + "dataset";
    private static final String TRAINER_YML = BASE_PATH + File.separator + "trainer" + File.separator + "trainer.yml";
    private static final String LABELS_CSV = BASE_PATH + File.separator + "trainer" + File.separator + "labels.csv";
    private static final String CASCADE_PATH = BASE_PATH + File.separator + "resources" + File.separator + "haarcascade_frontalface_default.xml";
    private static final String EXCEL_DIR = BASE_PATH + File.separator + "attendance";
    private static final String EXCEL_FILE = EXCEL_DIR + File.separator + "attendance.xlsx";

    // ---------- UI / STYLE ----------
    private static final String WINDOW_TITLE = "Aurora University | B.Tech – AMIL 2C | Smart Attendance System";
    private static final Scalar GREEN = new Scalar(0, 255, 0, 0);
    private static final Scalar RED = new Scalar(0, 0, 255, 0);

    // Lower distance = better match. Keep this fairly strict.
    private static final double RECOGNITION_THRESHOLD = 55.0;

    public static void main(String[] args) {
        try {
            // 1) Load OpenCV native libs
            Loader.load(opencv_core.class);
            System.out.println("✅ OpenCV Loaded Successfully!");

            // 2) Quick sanity checks
            File trainerFile = new File(TRAINER_YML);
            File labelsFile = new File(LABELS_CSV);
            File cascadeFile = new File(CASCADE_PATH);

            if (!trainerFile.exists()) {
                System.err.println("❌ trainer.yml not found at: " + TRAINER_YML);
                return;
            }
            if (!labelsFile.exists()) {
                System.err.println("❌ labels.csv not found at: " + LABELS_CSV);
                return;
            }
            if (!cascadeFile.exists()) {
                System.err.println("❌ Haar cascade not found at: " + CASCADE_PATH);
                return;
            }
            new File(EXCEL_DIR).mkdirs();
            System.out.println("📁 Dataset & Trainer folders verified.");

            // 3) Load label → (name, roll) map
            Map<Integer, StudentInfo> labelMap = loadLabels(LABELS_CSV);
            System.out.println("✅ Loaded labels for " + labelMap.size() + " student(s).");

            // 4) Prepare recognizer
            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            recognizer.read(TRAINER_YML);

            // 5) Load face detector
            CascadeClassifier faceDetector = new CascadeClassifier(CASCADE_PATH);
            if (faceDetector.empty()) {
                System.err.println("❌ Failed to load CascadeClassifier.");
                return;
            }

            // 6) Build Attendance Dashboard UI
            DefaultTableModel tableModel = buildDashboardUI();

            // To prevent double-marking
            Set<String> markedRolls = new HashSet<>();

            // 7) Start camera
            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

            try {
                grabber.start();
                CanvasFrame cameraFrame = new CanvasFrame("Aurora University – Live Camera", 1);
                cameraFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                while (cameraFrame.isVisible()) {
                    Frame frame = grabber.grab();
                    if (frame == null) break;

                    Mat matColor = converter.convert(frame);
                    if (matColor == null || matColor.empty()) {
                        continue;
                    }

                    Mat matGray = new Mat();
                    opencv_imgproc.cvtColor(matColor, matGray, opencv_imgproc.COLOR_BGR2GRAY);

                    // Detect faces
                    RectVector faces = new RectVector();
                    faceDetector.detectMultiScale(matGray, faces, 1.2, 5, 0, new Size(100, 100), new Size());

                    for (int i = 0; i < faces.size(); i++) {
                        Rect faceRect = faces.get(i);

                        // Crop & resize face
                        Mat face = new Mat(matGray, faceRect);
                        opencv_imgproc.resize(face, face, new Size(200, 200));

                        // Predict
                        IntPointer labelPtr = new IntPointer(1);
                        DoublePointer distancePtr = new DoublePointer(1);
                        recognizer.predict(face, labelPtr, distancePtr);

                        int predictedLabel = labelPtr.get(0);
                        double distance = distancePtr.get(0);  // Lower = better
                        double confidencePercent = Math.max(0, 100.0 - distance);

                        String displayName = "Unknown";
                        Scalar boxColor = GREEN;

                        if (labelMap.containsKey(predictedLabel) && distance < RECOGNITION_THRESHOLD) {
                            StudentInfo info = labelMap.get(predictedLabel);
                            displayName = info.name + " (" + String.format("%.1f", confidencePercent) + "%)";
                            boxColor = GREEN;

                            // Mark attendance only once per roll
                            if (!markedRolls.contains(info.roll)) {
                                markedRolls.add(info.roll);
                                markAttendanceExcel(info, tableModel, EXCEL_FILE);
                                JOptionPane.showMessageDialog(
                                        null,
                                        "Attendance marked for " + info.name + " (" + info.roll + ")",
                                        "Aurora Attendance",
                                        JOptionPane.INFORMATION_MESSAGE
                                );
                            }
                        } else {
                            displayName = "Unknown";
                            boxColor = RED;
                        }

                        // Draw rectangle + label
                        opencv_imgproc.rectangle(matColor, faceRect, boxColor, 2, opencv_imgproc.LINE_8, 0);
                        Point textPoint = new Point(faceRect.x(), Math.max(faceRect.y() - 10, 0));
                        opencv_imgproc.putText(
                                matColor,
                                displayName,
                                textPoint,
                                opencv_imgproc.FONT_HERSHEY_SIMPLEX,
                                0.9,
                                boxColor,
                                2,
                                opencv_imgproc.LINE_AA,
                                false
                        );
                    }

                    cameraFrame.showImage(converter.convert(matColor));
                }

                cameraFrame.dispose();
                grabber.stop();

            } finally {
                if (grabber != null) {
                    grabber.release();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    //  LABEL LOADING (labels.csv: label,name,roll)
    // ---------------------------------------------------------------------
    private static Map<Integer, StudentInfo> loadLabels(String csvPath) throws IOException {
        Map<Integer, StudentInfo> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Skip header: "label,name,roll"
                if (firstLine && line.toLowerCase().startsWith("label")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] parts = line.split(",");
                if (parts.length < 3) continue;

                try {
                    int label = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    String roll = parts[2].trim();
                    map.put(label, new StudentInfo(name, roll));
                } catch (NumberFormatException nfe) {
                    // If the label is not a number, skip that row
                    System.err.println("⚠️ Skipping invalid row in labels.csv: " + line);
                }
            }
        }

        return map;
    }

    // ---------------------------------------------------------------------
    //  DASHBOARD UI
    // ---------------------------------------------------------------------
    private static DefaultTableModel buildDashboardUI() {
        JFrame frame = new JFrame(WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 300);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // Header panel with green background
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new java.awt.Color(0, 128, 0));

        JLabel title = new JLabel("Aurora University  |  B.Tech 2nd Year  |  AMIL-2C  |  Smart Attendance System");
        title.setForeground(java.awt.Color.WHITE);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        headerPanel.add(title);

        frame.add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Name", "Roll No", "Date", "Time", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        table.getTableHeader().setBackground(new java.awt.Color(0, 128, 0));
        table.getTableHeader().setForeground(java.awt.Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
        return model;
    }

    // ---------------------------------------------------------------------
    //  EXCEL ATTENDANCE WRITER
    // ---------------------------------------------------------------------
    private static void markAttendanceExcel(StudentInfo info,
                                            DefaultTableModel tableModel,
                                            String excelPath) {

        try {
            // Current date & time
            LocalDateTime now = LocalDateTime.now();
            String dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            String timeStr = now.format(DateTimeFormatter.ofPattern("hh:mm:ss a"));

            // Add to JTable
            int nextId = tableModel.getRowCount() + 1;
            tableModel.addRow(new Object[]{
                    String.format("%02d", nextId),
                    info.name,
                    info.roll,
                    dateStr,
                    timeStr,
                    "Present"
            });

            // Create / open workbook
            File excelFile = new File(excelPath);
            XSSFWorkbook workbook;
            XSSFSheet sheet;

            if (excelFile.exists()) {
                try (FileInputStream fis = new FileInputStream(excelFile)) {
                    workbook = new XSSFWorkbook(fis);
                }
            } else {
                workbook = new XSSFWorkbook();
            }

            sheet = workbook.getSheet("Attendance");
            if (sheet == null) {
                sheet = workbook.createSheet("Attendance");
                // Create header row
                Row header = sheet.createRow(0);
                String[] headers = {"ID", "Name", "Roll No", "Date", "Time", "Status"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(headers[i]);
                }
            }

            int lastRow = sheet.getLastRowNum();
            int newRowIdx = lastRow + 1;

            Row row = sheet.createRow(newRowIdx);
            row.createCell(0).setCellValue(String.format("%02d", nextId));
            row.createCell(1).setCellValue(info.name);
            row.createCell(2).setCellValue(info.roll);
            row.createCell(3).setCellValue(dateStr);
            row.createCell(4).setCellValue(timeStr);
            row.createCell(5).setCellValue("Present");

            // Autosize columns
            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
            workbook.close();

            System.out.println("📝 Attendance saved to Excel for " + info.name + " (" + info.roll + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    //  SIMPLE HOLDER FOR STUDENT DATA
    // ---------------------------------------------------------------------
    private static class StudentInfo {
        final String name;
        final String roll;

        StudentInfo(String name, String roll) {
            this.name = name;
            this.roll = roll;
        }
    }


    // Start detection and show simple Swing dashboard when recognized.
    public void startWithDashboard() {
        // ensure OpenCV is loaded by existing code flow; if your project uses nu.pattern.OpenCV, keep that.
        try {
            // attempt to load native library if present
            String dllPath = System.getProperty("user.dir") + File.separator + "lib" + File.separator + "opencv_java4120.dll";
            System.load(dllPath);
        } catch (Throwable t) {
            // ignore - user may use opencv jar loader
        }
        // call existing startCamera or detection method if available
        try {
            this.startCamera();
        } catch (Exception e) {
            System.out.println("Error starting camera/dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
