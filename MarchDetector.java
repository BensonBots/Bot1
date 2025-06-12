package newgame;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class MarchDetector {
    
    public enum MarchStatus {
        IDLE,
        GATHERING,
        RETURNING,
        CANNOT_USE,
        UNLOCK
    }
    
    public static class MarchInfo {
        public int queueNumber;
        public MarchStatus status;
        public String rawStatus;
        
        public MarchInfo(int queueNumber, MarchStatus status, String rawStatus) {
            this.queueNumber = queueNumber;
            this.status = status;
            this.rawStatus = rawStatus;
        }
        
        @Override
        public String toString() {
            return "Queue " + queueNumber + ": " + status + " (raw: '" + rawStatus + "')";
        }
    }
    
    public static List<MarchInfo> readMarchQueues(int instanceIndex) {
        try {
            System.out.println("📋 [DEBUG] Reading march queues for instance " + instanceIndex);
            
            String fullScreenPath = "screenshots/debug_march_full_" + instanceIndex + ".png";
            if (!BotUtils.takeScreenshot(instanceIndex, fullScreenPath)) {
                System.err.println("❌ Failed to take march screenshot");
                return new ArrayList<>();
            }
            
            System.out.println("📸 [DEBUG] Full march screenshot: " + fullScreenPath + " (size: " + new File(fullScreenPath).length() + " bytes)");
            
            BufferedImage fullImage = ImageIO.read(new File(fullScreenPath));
            if (fullImage == null) {
                System.err.println("❌ Could not load march screenshot");
                return new ArrayList<>();
            }
            
            System.out.println("📐 [DEBUG] Full screen dimensions: " + fullImage.getWidth() + "x" + fullImage.getHeight());
            
            String textPanelPath = "screenshots/debug_march_text_panel_" + instanceIndex + ".png";
            if (!extractMarchTextPanel(fullScreenPath, textPanelPath)) {
                System.err.println("❌ Failed to extract march text panel");
                return new ArrayList<>();
            }
            
            // Use shared OCRUtils for march queue specific OCR
            String ocrText = OCRUtils.performMarchQueueOCR(textPanelPath, instanceIndex);
            if (ocrText == null || ocrText.trim().isEmpty()) {
                System.err.println("❌ OCR failed or returned empty text");
                return new ArrayList<>();
            }
            
            List<MarchInfo> queues = parseMarchQueues(ocrText);
            
            System.out.println("📊 [DEBUG] Parsed " + queues.size() + " queues:");
            for (MarchInfo queue : queues) {
                System.out.println("  " + queue.toString());
            }
            
            return queues;
            
        } catch (Exception e) {
            System.err.println("❌ Error reading march queues: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * FIXED: Extract march text panel with narrower width to avoid icons
     */
    private static boolean extractMarchTextPanel(String sourcePath, String outputPath) {
        try {
            BufferedImage sourceImage = ImageIO.read(new File(sourcePath));
            if (sourceImage == null) {
                System.err.println("❌ Could not load march screenshot: " + sourcePath);
                return false;
            }
            
            // FIXED: Narrower extraction to avoid left/right icons and focus on text
            int panelX = 80;        // Moved right from 50 to skip left icons
            int panelY = 190;       // Keep same Y position
            int panelWidth = 140;   // Much narrower from 230 to avoid right icons
            int panelHeight = 310;  // Keep same height
            
            System.out.println("📐 [DEBUG] Extracting NARROWER panel region: x=" + panelX + 
                              ", y=" + panelY + ", w=" + panelWidth + ", h=" + panelHeight);
            System.out.println("🎯 [DEBUG] This avoids left/right icons and focuses on text only");
            
            // Bounds checking
            panelX = Math.max(0, panelX);
            panelY = Math.max(0, panelY);
            panelWidth = Math.min(panelWidth, sourceImage.getWidth() - panelX);
            panelHeight = Math.min(panelHeight, sourceImage.getHeight() - panelY);
            
            if (panelWidth <= 0 || panelHeight <= 0) {
                System.err.println("❌ Invalid panel dimensions after bounds check");
                return false;
            }
            
            // Extract the narrower text-only region
            BufferedImage textPanel = sourceImage.getSubimage(panelX, panelY, panelWidth, panelHeight);
            
            // Use shared OCRUtils for image enhancement
            textPanel = OCRUtils.enhanceImageForOCR(textPanel);
            
            // Save the extracted panel
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            if (ImageIO.write(textPanel, "PNG", outputFile)) {
                System.out.println("✅ [DEBUG] Text panel extracted: " + outputPath + " (size: " + outputFile.length() + " bytes)");
                return true;
            } else {
                System.err.println("❌ Failed to save text panel");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting march text panel: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * ENHANCED: Parse march queues with better UNLOCK detection
     */
    private static List<MarchInfo> parseMarchQueues(String ocrText) {
        try {
            System.out.println("🔧 [DEBUG] Starting enhanced march queue parsing...");
            
            String[] lines = ocrText.split("\n");
            List<MarchInfo> queues = new ArrayList<>();
            
            System.out.println("📝 [DEBUG] Raw lines (" + lines.length + "):");
            for (int i = 0; i < lines.length; i++) {
                String cleaned = lines[i].trim();
                System.out.println("  " + i + ": '" + lines[i] + "' -> '" + cleaned + "'");
                lines[i] = cleaned;
            }
            
            System.out.println("📋 [DEBUG] Clean lines (" + lines.length + "):");
            for (int i = 0; i < lines.length; i++) {
                System.out.println("  " + i + ": '" + lines[i] + "'");
            }
            
            // Enhanced parsing with better UNLOCK detection
            int currentQueueNumber = 0;
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].toLowerCase().trim();
                
                System.out.println("🔍 [DEBUG] Analyzing line " + i + ": '" + lines[i] + "'");
                
                // Extract queue number from "March Queue X" lines
                if (line.contains("march queue")) {
                    Pattern queuePattern = Pattern.compile("march queue (\\d+)");
                    Matcher matcher = queuePattern.matcher(line);
                    if (matcher.find()) {
                        currentQueueNumber = Integer.parseInt(matcher.group(1));
                        System.out.println("  🔍 [DEBUG] Found queue number: " + currentQueueNumber);
                        continue;
                    }
                }
                
                // Detect status keywords
                MarchStatus status = detectMarchStatus(line, lines[i]);
                if (status != null) {
                    if (currentQueueNumber > 0) {
                        System.out.println("  ✅ [DEBUG] Found " + status + " queue: Queue " + currentQueueNumber);
                        queues.add(new MarchInfo(currentQueueNumber, status, lines[i]));
                        currentQueueNumber = 0; // Reset after finding status
                    } else {
                        // Try to find queue number from previous lines
                        for (int j = i - 1; j >= 0 && j >= i - 3; j--) {
                            String prevLine = lines[j].toLowerCase();
                            if (prevLine.contains("march queue")) {
                                Pattern queuePattern = Pattern.compile("march queue (\\d+)");
                                Matcher matcher = queuePattern.matcher(prevLine);
                                if (matcher.find()) {
                                    int queueNum = Integer.parseInt(matcher.group(1));
                                    System.out.println("    🔍 [DEBUG] Found explicit queue number " + queueNum + " from line: '" + prevLine + "'");
                                    System.out.println("  ✅ [DEBUG] Found " + status + " queue: Queue " + queueNum);
                                    queues.add(new MarchInfo(queueNum, status, lines[i]));
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("  ⚠️ [DEBUG] Line not recognized as any queue status");
                }
            }
            
            System.out.println("📊 [DEBUG] Detected " + queues.size() + " queues from OCR");
            
            // Fill in missing queues with default IDLE status (up to 6 queues total)
            List<MarchInfo> finalQueues = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                MarchInfo found = null;
                for (MarchInfo queue : queues) {
                    if (queue.queueNumber == i) {
                        found = queue;
                        break;
                    }
                }
                
                if (found != null) {
                    System.out.println("📊 [DEBUG] Final Queue " + i + ": " + found.status);
                    finalQueues.add(found);
                } else {
                    // Only default to IDLE for queues 1-3, others likely CANNOT_USE
                    MarchStatus defaultStatus = (i <= 3) ? MarchStatus.IDLE : MarchStatus.CANNOT_USE;
                    System.out.println("📊 [DEBUG] Default Queue " + i + ": " + defaultStatus + " (not detected in OCR)");
                    finalQueues.add(new MarchInfo(i, defaultStatus, "default"));
                }
            }
            
            System.out.println("📊 [DEBUG] Parsed " + finalQueues.size() + " queues:");
            for (MarchInfo queue : finalQueues) {
                System.out.println("  March Queue " + queue.queueNumber + ": " + queue.status);
            }
            
            return finalQueues;
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing march queues: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * ENHANCED: Detect march status with better UNLOCK recognition
     */
    private static MarchStatus detectMarchStatus(String line, String originalLine) {
        // Convert to lowercase for matching but keep original for display
        String lowerLine = line.toLowerCase().trim();
        
        // Direct matches
        if (lowerLine.equals("idle")) {
            return MarchStatus.IDLE;
        }
        if (lowerLine.equals("gathering")) {
            return MarchStatus.GATHERING;
        }
        if (lowerLine.equals("returning")) {
            return MarchStatus.RETURNING;
        }
        if (lowerLine.equals("cannot use")) {
            return MarchStatus.CANNOT_USE;
        }
        if (lowerLine.equals("unlock")) {
            return MarchStatus.UNLOCK;
        }
        
        // Fuzzy matches for OCR errors
        if (lowerLine.contains("idle") || lowerLine.matches(".*i.*d.*l.*e.*")) {
            return MarchStatus.IDLE;
        }
        if (lowerLine.contains("gather") || lowerLine.matches(".*g.*a.*t.*h.*e.*r.*")) {
            return MarchStatus.GATHERING;
        }
        if (lowerLine.contains("return") || lowerLine.matches(".*r.*e.*t.*u.*r.*n.*")) {
            return MarchStatus.RETURNING;
        }
        if (lowerLine.contains("cannot") || lowerLine.contains("can not") || 
            lowerLine.matches(".*c.*a.*n.*n.*o.*t.*") || lowerLine.contains("use")) {
            return MarchStatus.CANNOT_USE;
        }
        
        // ENHANCED: Better UNLOCK detection for OCR errors like "wu S", "unl", "ock", etc.
        if (lowerLine.contains("unlock") || lowerLine.contains("unl") || 
            lowerLine.matches(".*u.*n.*l.*o.*c.*k.*") ||
            lowerLine.matches(".*w.*u.*s.*") ||  // "wu S" OCR error
            lowerLine.matches(".*u.*n.*l.*") ||  // "unl" partial
            lowerLine.matches(".*l.*o.*c.*k.*") || // "lock" partial
            (lowerLine.length() <= 4 && (lowerLine.contains("wu") || lowerLine.contains("ul") || lowerLine.contains("ock")))) {
            return MarchStatus.UNLOCK;
        }
        
        return null; // No status detected
    }
}