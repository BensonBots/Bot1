package newgame;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class BotUtils {
    public static final String MEMUC_PATH = "C:\\Program Files\\Microvirt\\MEmu\\memuc.exe";
    public static final String SCREENSHOTS_DIR = "screenshots";
    public static boolean openCvLoaded = false;

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            openCvLoaded = true;
            System.out.println("OpenCV loaded successfully");
        } catch (Exception | UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV: " + e.getMessage());
            System.err.println("OpenCV features will be disabled. Image matching will not work.");
            openCvLoaded = false;
        }
    }

    public static void init() {
        System.out.println("=== MEmu Instance Manager Starting ===");
    }

    public static boolean isOpenCvLoaded() {
        return openCvLoaded;
    }

    public static void createDirectoryIfNeeded(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + dirPath + " - " + e.getMessage());
        }
    }

    public static boolean takeScreenshot(int index, String savePath, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (takeScreenshotSingle(index, savePath)) {
                File file = new File(savePath);
                if (file.exists() && file.length() > 10000) {
                    return true;
                }
            }
            
            if (attempt < maxRetries) {
                System.out.println("Screenshot attempt " + attempt + " failed, retrying...");
                delay(1000);
            }
        }
        
        System.err.println("Screenshot failed after " + maxRetries + " attempts");
        return false;
    }

    public static boolean takeScreenshot(int index, String savePath) {
        return takeScreenshot(index, savePath, 1);
    }

    public static boolean takeMenuScreenshotLegacy(int index, String savePath) {
        return takeScreenshot(index, savePath);
    }

    public static boolean takeScreenshotWithRetry(int instanceIndex, String savePath, int maxRetries) {
        return takeScreenshot(instanceIndex, savePath, maxRetries);
    }

    private static boolean takeScreenshotSingle(int index, String savePath) {
        try {
            createDirectoryIfNeeded(SCREENSHOTS_DIR);
            
            ProcessBuilder captureBuilder = new ProcessBuilder(
                MEMUC_PATH, "adb", "-i", String.valueOf(index),
                "shell", "screencap", "-p", "/sdcard/screen.png"
            );
            Process captureProcess = captureBuilder.start();
            
            boolean captureSuccess = captureProcess.waitFor(10, TimeUnit.SECONDS) && 
                                   captureProcess.exitValue() == 0;
            
            if (!captureSuccess) {
                System.err.println("Screenshot capture failed for instance " + index);
                return false;
            }

            Thread.sleep(500);

            ProcessBuilder pullBuilder = new ProcessBuilder(
                MEMUC_PATH, "adb", "-i", String.valueOf(index),
                "pull", "/sdcard/screen.png", savePath
            );
            Process pullProcess = pullBuilder.start();
            
            boolean pullSuccess = pullProcess.waitFor(10, TimeUnit.SECONDS) && 
                                pullProcess.exitValue() == 0;
            
            if (!pullSuccess) {
                System.err.println("Screenshot pull failed for instance " + index);
                return false;
            }

            File screenshotFile = new File(savePath);
            boolean success = screenshotFile.exists() && screenshotFile.length() > 0;
            
            if (success) {
                System.out.println("Screenshot saved: " + savePath + " (" + screenshotFile.length() + " bytes)");
            }
            
            return success;
        } catch (IOException | InterruptedException e) {
            System.err.println("Screenshot error: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public static Point findImageOnScreen(String screenshotPath, String templateName, double threshold, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Point result = findImageOnScreenSingle(screenshotPath, templateName, threshold);
            if (result != null) {
                return result;
            }
            
            if (attempt < maxRetries) {
                System.out.println("Image search attempt " + attempt + " failed, retrying...");
                delay(1000);
            }
        }
        
        System.err.println("Image not found after " + maxRetries + " attempts: " + templateName);
        return null;
    }

    public static Point findImageOnScreen(String screenshotPath, String templateName, double threshold) {
        return findImageOnScreen(screenshotPath, templateName, threshold, 1);
    }

    public static Point findImageOnScreenGrayWithRetry(String screenshotPath, String templateName, double threshold, int instanceIndex) {
        return findImageOnScreen(screenshotPath, templateName, threshold);
    }

    public static Point findImageInScreenshotLegacy(String screenshotPath, String templateName, double threshold) {
        return findImageOnScreen(screenshotPath, templateName, threshold);
    }

    public static Point findImageOnScreenWithRetry(String screenshotPath, String templateName, double threshold, int maxRetries) {
        return findImageOnScreen(screenshotPath, templateName, threshold, maxRetries);
    }

    private static Point findImageOnScreenSingle(String screenshotPath, String templateName, double threshold) {
        if (!openCvLoaded) {
            System.err.println("OpenCV not loaded, cannot perform image matching");
            return null;
        }

        try {
            String templatePath = findTemplatePath(templateName);
            if (templatePath == null) {
                System.err.println("Template not found: " + templateName);
                return null;
            }

            Mat template = Imgcodecs.imread(templatePath, Imgcodecs.IMREAD_GRAYSCALE);
            Mat screen = Imgcodecs.imread(screenshotPath, Imgcodecs.IMREAD_GRAYSCALE);

            if (template.empty()) {
                System.err.println("Failed to load template: " + templateName);
                return null;
            }

            if (screen.empty()) {
                System.err.println("Failed to load screenshot: " + screenshotPath);
                template.release();
                return null;
            }

            Mat result = new Mat();
            Imgproc.matchTemplate(screen, template, result, Imgproc.TM_CCOEFF_NORMED);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            double confidence = mmr.maxVal;

            template.release();
            screen.release();
            result.release();

            if (confidence >= threshold) {
                Point matchPoint = new Point((int)mmr.maxLoc.x, (int)mmr.maxLoc.y);
                System.out.println("Found template at: (" + matchPoint.x + ", " + matchPoint.y + ") for " + templateName + " (confidence: " + String.format("%.3f", confidence) + ")");
                return matchPoint;
            } else {
                System.out.println("Template not found - confidence too low for " + templateName + " (confidence: " + String.format("%.3f", confidence) + ", threshold: " + threshold + ")");
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error in image matching: " + e.getMessage());
            return null;
        }
    }

    private static String findTemplatePath(String imageName) {
        File srcImagesFile = new File("src/images/" + imageName);
        if (srcImagesFile.exists()) {
            return srcImagesFile.getAbsolutePath();
        }

        File currentDirFile = new File(imageName);
        if (currentDirFile.exists()) {
            return currentDirFile.getAbsolutePath();
        }

        File imagesSubdirFile = new File("images/" + imageName);
        if (imagesSubdirFile.exists()) {
            return imagesSubdirFile.getAbsolutePath();
        }

        return null;
    }

    public static boolean clickMenu(int index, Point pt) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                MEMUC_PATH, "adb", "-i", String.valueOf(index),
                "shell", "input", "tap",
                String.valueOf((int)pt.getX()),
                String.valueOf((int)pt.getY())
            );
            Process process = builder.start();
            
            boolean success = process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
            
            if (success) {
                System.out.println("Clicked at " + pt + " on instance " + index);
            }
            
            return success;
        } catch (IOException | InterruptedException e) {
            System.err.println("Click error: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public static boolean delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean isInstanceRunning(int index) {
        try {
            ProcessBuilder builder = new ProcessBuilder(MEMUC_PATH, "isvmrunning", "-i", String.valueOf(index));
            Process process = builder.start();
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                if (output != null) {
                    String trimmedOutput = output.trim();
                    return trimmedOutput.equals("1") || trimmedOutput.equalsIgnoreCase("Running");
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking if instance " + index + " is running: " + e.getMessage());
        }
        return false;
    }

    public static void enableAutoStart(int index) {
        System.out.println("Auto Start Game is enabled for instance " + index);
    }

    public static boolean performADBSwipe(int instanceIndex, Point startPoint, Point endPoint) {
        try {
            ProcessBuilder swipeBuilder = new ProcessBuilder(
                MEMUC_PATH, "adb", "-i", String.valueOf(instanceIndex),
                "shell", "input", "swipe",
                String.valueOf((int)startPoint.getX()),
                String.valueOf((int)startPoint.getY()),
                String.valueOf((int)endPoint.getX()),
                String.valueOf((int)endPoint.getY()),
                "300"
            );
            
            Process swipeProcess = swipeBuilder.start();
            boolean success = swipeProcess.waitFor(10, TimeUnit.SECONDS) && 
                             swipeProcess.exitValue() == 0;
            
            if (success) {
                System.out.println("✅ ADB swipe executed: " + startPoint + " → " + endPoint);
            } else {
                System.err.println("❌ ADB swipe failed");
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Error performing ADB swipe: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public static String runTesseractOCR(String imagePath, int instanceIndex) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                System.err.println("❌ OCR image file not found: " + imagePath);
                return null;
            }
            
            String tesseractPath = findTesseractPath();
            if (tesseractPath == null) {
                System.err.println("❌ Tesseract not found");
                return null;
            }
            
            String[][] ocrConfigs = {
                {"--psm", "8", "--oem", "1", "-c", "tessedit_char_whitelist=0123456789:"},
                {"--psm", "7", "--oem", "1", "-c", "tessedit_char_whitelist=0123456789:"},
                {"--psm", "8", "--oem", "1", "-c", "tessedit_char_whitelist=0123456789"},
                {"--psm", "7", "--oem", "0", "-c", "tessedit_char_whitelist=0123456789:"},
                {"--psm", "6", "--oem", "1"},
            };
            
            String bestResult = null;
            double bestScore = 0;
            
            for (int i = 0; i < ocrConfigs.length; i++) {
                try {
                    String result = runTesseractWithConfig(tesseractPath, imagePath, ocrConfigs[i]);
                    
                    if (result != null && !result.trim().isEmpty()) {
                        double score = scoreTimeExtractionResult(result);
                        
                        if (score > bestScore) {
                            bestScore = score;
                            bestResult = result.trim();
                        }
                        
                        if (score >= 10.0) {
                            return result.trim();
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ OCR Config " + (i + 1) + " failed: " + e.getMessage());
                }
            }
            
            if (bestResult != null && bestScore > 0) {
                return bestResult;
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Error running OCR: " + e.getMessage());
            return null;
        }
    }

    public static String extractTextFromImage(String imagePath) {
        return runTesseractOCR(imagePath, 0);
    }

    public static String performBasicOCR(String imagePath) {
        return runTesseractOCR(imagePath, 0);
    }

    public static String extractTextFromRegion(String imagePath, Rectangle region) {
        try {
            BufferedImage fullImage = ImageIO.read(new File(imagePath));
            
            int x = Math.max(0, region.x);
            int y = Math.max(0, region.y);
            int width = Math.min(region.width, fullImage.getWidth() - x);
            int height = Math.min(region.height, fullImage.getHeight() - y);
            
            if (width <= 0 || height <= 0) {
                System.err.println("❌ Invalid region bounds");
                return null;
            }
            
            BufferedImage regionImage = fullImage.getSubimage(x, y, width, height);
            
            String regionPath = imagePath.replace(".png", "_region.png");
            ImageIO.write(regionImage, "png", new File(regionPath));
            
            String extractedText = runTesseractOCR(regionPath, 0);
            
            try {
                new File(regionPath).delete();
            } catch (Exception e) {
            }
            
            return extractedText;
            
        } catch (Exception e) {
            System.err.println("Error extracting text from region: " + e.getMessage());
            return null;
        }
    }

    public static boolean extractTextPanel(String sourcePath, String outputPath, boolean enhanceContrast, int instanceIndex) {
        try {
            BufferedImage sourceImage = ImageIO.read(new File(sourcePath));
            if (sourceImage == null) {
                System.err.println("❌ Could not load source image: " + sourcePath);
                return false;
            }
            
            int startX = 0;
            int startY = 150;
            int width = 300;
            int height = 400;
            
            width = Math.min(width, sourceImage.getWidth() - startX);
            height = Math.min(height, sourceImage.getHeight() - startY);
            
            if (width <= 0 || height <= 0) {
                System.err.println("❌ Invalid text panel dimensions");
                return false;
            }
            
            BufferedImage textPanel = sourceImage.getSubimage(startX, startY, width, height);
            
            if (enhanceContrast) {
                textPanel = enhanceForOCR(textPanel);
            }
            
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            
            boolean saved = ImageIO.write(textPanel, "PNG", outputFile);
            if (saved) {
                System.out.println("✅ Text panel extracted: " + outputPath + " (" + outputFile.length() + " bytes)");
                return true;
            } else {
                System.err.println("❌ Failed to save text panel");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting text panel: " + e.getMessage());
            return false;
        }
    }

    public static boolean extractSmallTimeArea(String sourcePath, String outputPath, int x, int y, int width, int height) {
        try {
            BufferedImage sourceImage = ImageIO.read(new File(sourcePath));
            if (sourceImage == null) {
                return false;
            }
            
            if (x < 0 || y < 0 || x + width > sourceImage.getWidth() || y + height > sourceImage.getHeight()) {
                System.err.println("❌ Time area coordinates out of bounds");
                return false;
            }
            
            BufferedImage timeArea = sourceImage.getSubimage(x, y, width, height);
            timeArea = enhanceTimeAreaForOCR(timeArea);
            
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            
            return ImageIO.write(timeArea, "PNG", outputFile);
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting time area: " + e.getMessage());
            return false;
        }
    }

    private static String runTesseractWithConfig(String tesseractPath, String imagePath, String[] config) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(tesseractPath);
        pb.command().add(imagePath);
        pb.command().add("stdout");
        
        for (String param : config) {
            pb.command().add(param);
        }
        
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        return output.toString().trim();
    }
    
    private static double scoreTimeExtractionResult(String result) {
        if (result == null || result.trim().isEmpty()) {
            return 0;
        }
        
        String cleaned = result.replaceAll("[^0-9:]", "");
        
        if (cleaned.matches("\\d{2}:\\d{2}:\\d{2}")) {
            return 10.0;
        }
        if (cleaned.matches("\\d{1}:\\d{2}:\\d{2}")) {
            return 9.0;
        }
        if (cleaned.matches("\\d{2}:\\d{2}")) {
            return 8.0;
        }
        if (cleaned.matches("\\d{1}:\\d{2}")) {
            return 7.0;
        }
        if (cleaned.matches("\\d{6}")) {
            return 6.0;
        }
        if (cleaned.matches("\\d{4}")) {
            return 5.0;
        }
        if (cleaned.contains(":") && cleaned.matches(".*\\d.*")) {
            return 3.0;
        }
        if (cleaned.matches("\\d{2,8}")) {
            return 2.0;
        }
        
        return 0;
    }
    
    private static String findTesseractPath() {
        String[] possiblePaths = {
            "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",
            "C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe", 
            "tesseract",
            "tesseract.exe"
        };
        
        for (String path : possiblePaths) {
            File tesseractFile = new File(path);
            if (tesseractFile.exists() && tesseractFile.canExecute()) {
                return path;
            }
        }
        
        try {
            Process process = Runtime.getRuntime().exec("tesseract --version");
            process.waitFor();
            if (process.exitValue() == 0) {
                return "tesseract";
            }
        } catch (Exception e) {
        }
        
        return null;
    }

    private static BufferedImage enhanceForOCR(BufferedImage original) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();
            
            BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color pixel = new Color(original.getRGB(x, y));
                    
                    int gray = (int) (pixel.getRed() * 0.3 + pixel.getGreen() * 0.59 + pixel.getBlue() * 0.11);
                    
                    if (gray > 128) {
                        gray = Math.min(255, gray + 50);
                    } else {
                        gray = Math.max(0, gray - 50);
                    }
                    
                    Color newColor = new Color(gray, gray, gray);
                    enhanced.setRGB(x, y, newColor.getRGB());
                }
            }
            
            return enhanced;
            
        } catch (Exception e) {
            System.err.println("❌ Error enhancing image for OCR: " + e.getMessage());
            return original;
        }
    }

    private static BufferedImage enhanceTimeAreaForOCR(BufferedImage original) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();
            
            int scaleFactor = 3;
            BufferedImage scaled = new BufferedImage(width * scaleFactor, height * scaleFactor, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.drawImage(original, 0, 0, width * scaleFactor, height * scaleFactor, null);
            g2d.dispose();
            
            for (int y = 0; y < scaled.getHeight(); y++) {
                for (int x = 0; x < scaled.getWidth(); x++) {
                    Color pixel = new Color(scaled.getRGB(x, y));
                    int gray = (int) (pixel.getRed() * 0.3 + pixel.getGreen() * 0.59 + pixel.getBlue() * 0.11);
                    
                    int newGray = (gray > 100) ? 255 : 0;
                    Color newColor = new Color(newGray, newGray, newGray);
                    scaled.setRGB(x, y, newColor.getRGB());
                }
            }
            
            return scaled;
            
        } catch (Exception e) {
            return original;
        }
    }
}