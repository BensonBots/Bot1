package newgame;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Shared OCR utility class for all modules to use
 * Provides enhanced OCR functionality with multiple configurations and scoring
 */
public class OCRUtils {
    
    /**
     * Perform enhanced OCR with multiple configurations and return best result
     */
    public static String performEnhancedOCR(String imagePath, int instanceIndex) {
        try {
            System.out.println("🔍 [OCR] Performing enhanced OCR on: " + imagePath);
            
            // Try multiple OCR configurations for best results
            String[] ocrConfigs = {
                "--psm 6 --oem 1 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 :",
                "--psm 7 --oem 1 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 :",
                "--psm 8 --oem 1 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 :",
                "--psm 6 --oem 3",
                "--psm 7 --oem 3"
            };
            
            String bestResult = "";
            int bestScore = 0;
            
            for (int i = 0; i < ocrConfigs.length; i++) {
                System.out.println("🔍 [OCR] Trying config " + (i+1) + "/" + ocrConfigs.length + " (PSM " + extractPSM(ocrConfigs[i]) + ")...");
                
                String result = runTesseractWithConfig(imagePath, instanceIndex, ocrConfigs[i]);
                if (result != null && !result.trim().isEmpty()) {
                    int score = calculateGeneralOCRScore(result);
                    System.out.println("📊 [OCR] Config " + (i+1) + " score: " + score);
                    System.out.println("📝 [OCR] Config " + (i+1) + " result: '" + result.replace("\n", " | ") + "'");
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestResult = result;
                        System.out.println("⭐ [OCR] New best result with score " + score);
                        
                        // If we get an excellent score, use it immediately
                        if (score >= 95) {
                            System.out.println("🎉 [OCR] Excellent result found, using immediately");
                            break;
                        }
                    }
                }
            }
            
            System.out.println("📋 [OCR] Final Results:");
            System.out.println("=== START OCR TEXT ===");
            System.out.println(bestResult);
            System.out.println("=== END OCR TEXT ===");
            
            return bestResult;
            
        } catch (Exception e) {
            System.err.println("❌ [OCR] Error performing enhanced OCR: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Perform OCR specifically optimized for march queue text
     */
    public static String performMarchQueueOCR(String imagePath, int instanceIndex) {
        try {
            System.out.println("🔍 [OCR] Performing march queue specific OCR on: " + imagePath);
            
            String[] ocrConfigs = {
                "--psm 6 --oem 1 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 :",
                "--psm 7 --oem 1 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 :",
                "--psm 6 --oem 3",
                "--psm 7 --oem 3"
            };
            
            String bestResult = "";
            int bestScore = 0;
            
            for (int i = 0; i < ocrConfigs.length; i++) {
                System.out.println("🔍 [OCR] Trying march queue config " + (i+1) + "/" + ocrConfigs.length);
                
                String result = runTesseractWithConfig(imagePath, instanceIndex, ocrConfigs[i]);
                if (result != null && !result.trim().isEmpty()) {
                    int score = calculateMarchQueueOCRScore(result);
                    System.out.println("📊 [OCR] March queue config " + (i+1) + " score: " + score);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestResult = result;
                        
                        if (score >= 95) {
                            break;
                        }
                    }
                }
            }
            
            return bestResult;
            
        } catch (Exception e) {
            System.err.println("❌ [OCR] Error performing march queue OCR: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Perform OCR specifically optimized for time text (HH:MM:SS format)
     */
    public static String performTimeOCR(String imagePath, int instanceIndex) {
        try {
            System.out.println("🔍 [OCR] Performing time-specific OCR on: " + imagePath);
            
            String tesseractPath = "C:\\Program Files\\Tesseract-OCR\\tesseract.exe";
            java.io.File tesseractExe = new java.io.File(tesseractPath);
            
            if (!tesseractExe.exists()) {
                System.err.println("❌ [OCR] Tesseract not found at: " + tesseractPath);
                return BotUtils.runTesseractOCR(imagePath, instanceIndex);
            }
            
            // OCR configuration optimized for time digits (HH:MM:SS format)
            String[] ocrConfig = {
                "--psm", "8",  // Single word mode
                "--oem", "1",  // LSTM engine
                "-c", "tessedit_char_whitelist=0123456789:"  // Only digits and colon
            };
            
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(tesseractPath);
            command.add(imagePath);
            command.add("stdout");
            
            for (String param : ocrConfig) {
                command.add(param);
            }
            
            System.out.println("🖥️ [OCR] Running time-specific OCR: " + String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            String result = output.toString().trim();
            
            System.out.println("📊 [OCR] Time OCR result: '" + result + "' (exit code: " + exitCode + ")");
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ [OCR] Error running time-specific OCR: " + e.getMessage());
            return BotUtils.runTesseractOCR(imagePath, instanceIndex);
        }
    }
    
    /**
     * Extract region from image for better OCR targeting
     */
    public static boolean extractImageRegion(String sourcePath, String outputPath, int x, int y, int width, int height) {
        try {
            BufferedImage sourceImage = ImageIO.read(new File(sourcePath));
            if (sourceImage == null) {
                System.err.println("❌ [OCR] Could not load source image: " + sourcePath);
                return false;
            }
            
            // Bounds checking
            x = Math.max(0, Math.min(x, sourceImage.getWidth() - width));
            y = Math.max(0, Math.min(y, sourceImage.getHeight() - height));
            width = Math.min(width, sourceImage.getWidth() - x);
            height = Math.min(height, sourceImage.getHeight() - y);
            
            if (width <= 0 || height <= 0) {
                System.err.println("❌ [OCR] Invalid region bounds");
                return false;
            }
            
            System.out.println("🎯 [OCR] Extracting region: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height);
            
            BufferedImage regionImage = sourceImage.getSubimage(x, y, width, height);
            
            // Save the extracted region
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            if (ImageIO.write(regionImage, "PNG", outputFile)) {
                System.out.println("✅ [OCR] Extracted region: " + outputPath + " (" + outputFile.length() + " bytes)");
                return true;
            } else {
                System.err.println("❌ [OCR] Failed to save region");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ [OCR] Error extracting image region: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Enhance image for better OCR with high contrast
     */
    public static BufferedImage enhanceImageForOCR(BufferedImage original) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();
            
            BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    java.awt.Color pixel = new java.awt.Color(original.getRGB(x, y));
                    
                    // Convert to grayscale
                    int gray = (int) (pixel.getRed() * 0.299 + pixel.getGreen() * 0.587 + pixel.getBlue() * 0.114);
                    
                    // High contrast threshold
                    int threshold = 128;
                    int newGray = (gray > threshold) ? 255 : 0;
                    
                    java.awt.Color newColor = new java.awt.Color(newGray, newGray, newGray);
                    enhanced.setRGB(x, y, newColor.getRGB());
                }
            }
            
            return enhanced;
            
        } catch (Exception e) {
            System.err.println("❌ [OCR] Error enhancing image: " + e.getMessage());
            return original;
        }
    }
    
    // === PRIVATE HELPER METHODS ===
    
    private static String runTesseractWithConfig(String imagePath, int instanceIndex, String config) {
        try {
            String tesseractPath = "C:\\Program Files\\Tesseract-OCR\\tesseract.exe";
            java.io.File tesseractExe = new java.io.File(tesseractPath);
            
            if (!tesseractExe.exists()) {
                System.err.println("❌ [OCR] Tesseract not found, using fallback");
                return BotUtils.runTesseractOCR(imagePath, instanceIndex);
            }
            
            String[] configArgs = config.split(" ");
            
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(tesseractPath);
            command.add(imagePath);
            command.add("stdout");
            
            for (String arg : configArgs) {
                if (!arg.trim().isEmpty()) {
                    command.add(arg.trim());
                }
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            process.waitFor();
            return output.toString().trim();
            
        } catch (Exception e) {
            System.err.println("❌ [OCR] Error running Tesseract with config: " + e.getMessage());
            return BotUtils.runTesseractOCR(imagePath, instanceIndex);
        }
    }
    
    private static String extractPSM(String config) {
        Pattern pattern = Pattern.compile("--psm (\\d+)");
        Matcher matcher = pattern.matcher(config);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "?";
    }
    
    private static int calculateGeneralOCRScore(String ocrResult) {
        if (ocrResult == null || ocrResult.trim().isEmpty()) {
            return 0;
        }
        
        int score = 50; // Base score
        String lowerResult = ocrResult.toLowerCase();
        
        // Award points for readable text
        String[] words = ocrResult.split("\\s+");
        for (String word : words) {
            if (word.length() >= 3) {
                score += 5;
            }
        }
        
        // Penalty for too many short words or special characters
        for (String word : words) {
            if (word.length() < 2 && !word.matches("\\d+")) {
                score -= 3;
            }
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    private static int calculateMarchQueueOCRScore(String ocrResult) {
        if (ocrResult == null || ocrResult.trim().isEmpty()) {
            return 0;
        }
        
        int score = 0;
        String lowerResult = ocrResult.toLowerCase();
        
        // Award points for expected march queue keywords
        if (lowerResult.contains("march queue")) score += 20;
        if (lowerResult.contains("idle")) score += 15;
        if (lowerResult.contains("cannot use")) score += 15;
        if (lowerResult.contains("unlock")) score += 15;
        if (lowerResult.contains("gathering")) score += 15;
        if (lowerResult.contains("returning")) score += 10;
        
        // Award points for queue numbers
        for (int i = 1; i <= 6; i++) {
            if (lowerResult.contains("queue " + i)) score += 5;
        }
        
        // Penalty for gibberish
        String[] words = ocrResult.split("\\s+");
        for (String word : words) {
            if (word.length() < 2 && !word.matches("\\d+")) {
                score -= 2;
            }
        }
        
        return Math.max(0, Math.min(100, score));
    }
}