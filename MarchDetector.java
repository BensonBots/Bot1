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
            
            // FIXED: Extract even narrower text panel to avoid ALL icons and focus only on status text
            String textPanelPath = "screenshots/debug_march_text_panel_" + instanceIndex + ".png";
            if (!extractMarchTextPanelImproved(fullScreenPath, textPanelPath)) {
                System.err.println("❌ Failed to extract march text panel");
                return new ArrayList<>();
            }
            
            // Use shared OCRUtils for march queue specific OCR
            String ocrText = OCRUtils.performMarchQueueOCR(textPanelPath, instanceIndex);
            if (ocrText == null || ocrText.trim().isEmpty()) {
                System.err.println("❌ OCR failed or returned empty text");
                return new ArrayList<>();
            }
            
            List<MarchInfo> queues = parseMarchQueuesImproved(ocrText);
            
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
     * IMPROVED: Extract even narrower march text panel to focus only on status text
     */
    private static boolean extractMarchTextPanelImproved(String sourcePath, String outputPath) {
        try {
            BufferedImage sourceImage = ImageIO.read(new File(sourcePath));
            if (sourceImage == null) {
                System.err.println("❌ Could not load march screenshot: " + sourcePath);
                return false;
            }
            
            // IMPROVED: Even more focused extraction to capture only status text
            // Based on your screenshot showing the march queue panel
            int panelX = 100;       // Further right to avoid left icons completely
            int panelY = 200;       // Slightly lower to start from queue text
            int panelWidth = 120;   // Even narrower to focus on status words only
            int panelHeight = 280;  // Slightly shorter to avoid bottom elements
            
            System.out.println("📐 [DEBUG] Extracting ULTRA-NARROW panel region: x=" + panelX + 
                              ", y=" + panelY + ", w=" + panelWidth + ", h=" + panelHeight);
            System.out.println("🎯 [DEBUG] Ultra-focused on status text only - avoiding ALL icons");
            
            // Bounds checking
            panelX = Math.max(0, panelX);
            panelY = Math.max(0, panelY);
            panelWidth = Math.min(panelWidth, sourceImage.getWidth() - panelX);
            panelHeight = Math.min(panelHeight, sourceImage.getHeight() - panelY);
            
            if (panelWidth <= 0 || panelHeight <= 0) {
                System.err.println("❌ Invalid panel dimensions after bounds check");
                return false;
            }
            
            // Extract the ultra-narrow text-only region
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
                System.out.println("✅ [DEBUG] Ultra-narrow text panel extracted: " + outputPath + " (size: " + outputFile.length() + " bytes)");
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
     * IMPROVED: Better parsing with enhanced status detection for "Gathering" specifically
     */
    private static List<MarchInfo> parseMarchQueuesImproved(String ocrText) {
        try {
            System.out.println("🔧 [DEBUG] Starting IMPROVED march queue parsing...");
            
            String[] lines = ocrText.split("\n");
            List<MarchInfo> queues = new ArrayList<>();
            
            System.out.println("📝 [DEBUG] Raw lines (" + lines.length + "):");
            for (int i = 0; i < lines.length; i++) {
                String cleaned = lines[i].trim();
                System.out.println("  " + i + ": '" + lines[i] + "' -> '" + cleaned + "'");
                lines[i] = cleaned;
            }
            
            // IMPROVED: Enhanced parsing with better context awareness
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                System.out.println("🔍 [DEBUG] Analyzing line " + i + ": '" + line + "'");
                
                // Look for queue numbers in current or nearby lines
                int queueNumber = extractQueueNumber(line, lines, i);
                if (queueNumber > 0) {
                    System.out.println("  🔍 [DEBUG] Found queue number: " + queueNumber);
                    
                    // Look for status in current line or next few lines
                    MarchStatus status = findStatusForQueue(lines, i, queueNumber);
                    if (status != null) {
                        System.out.println("  ✅ [DEBUG] Found " + status + " for Queue " + queueNumber);
                        queues.add(new MarchInfo(queueNumber, status, getStatusContext(lines, i)));
                    }
                }
            }
            
            // IMPROVED: Fill in missing queues with intelligent defaults
            List<MarchInfo> finalQueues = createCompleteQueueList(queues);
            
            System.out.println("📊 [DEBUG] Final queue status:");
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
     * IMPROVED: Extract queue number with better pattern matching
     */
    private static int extractQueueNumber(String line, String[] allLines, int currentIndex) {
        String lowerLine = line.toLowerCase();
        
        // Direct queue number patterns
        Pattern queuePattern = Pattern.compile("(?:march\\s+)?queue\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = queuePattern.matcher(line);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        // Look for standalone numbers that might be queue numbers (1-6)
        Pattern numberPattern = Pattern.compile("\\b([1-6])\\b");
        Matcher numberMatcher = numberPattern.matcher(line);
        if (numberMatcher.find()) {
            int number = Integer.parseInt(numberMatcher.group(1));
            // Validate this is likely a queue number by checking context
            if (isLikelyQueueNumber(allLines, currentIndex, number)) {
                return number;
            }
        }
        
        return 0;
    }
    
    /**
     * IMPROVED: Find status for a specific queue with better context search
     */
    private static MarchStatus findStatusForQueue(String[] lines, int startIndex, int queueNumber) {
        // Check current line and next 3 lines for status
        for (int i = startIndex; i < Math.min(lines.length, startIndex + 4); i++) {
            MarchStatus status = detectMarchStatusImproved(lines[i]);
            if (status != null) {
                System.out.println("    🎯 [DEBUG] Found status " + status + " for queue " + queueNumber + " in line: '" + lines[i] + "'");
                return status;
            }
        }
        
        // If no status found, return default based on queue number
        return (queueNumber <= 3) ? MarchStatus.IDLE : MarchStatus.CANNOT_USE;
    }
    
    /**
     * IMPROVED: Enhanced status detection with better "Gathering" recognition
     */
    private static MarchStatus detectMarchStatusImproved(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        String lowerLine = line.toLowerCase().trim();
        System.out.println("      🔍 [STATUS] Analyzing: '" + lowerLine + "'");
        
        // IMPROVED: Better gathering detection patterns
        String[] gatheringPatterns = {
            "gathering",
            "gather",
            "gath",
            "ering",
            "athering",
            "g.*a.*t.*h.*e.*r",  // Scattered letters
            ".*g.*a.*t.*h.*",    // Partial with scattered
            "lvl.*\\d+.*mill",   // "Gathering Lvl X Mill" pattern from your screenshot
            "lv.*\\d+.*mill",    // "Gathering Lv X Mill" variant
            "mill",              // Just "mill" often indicates gathering
            "\\d{2}:\\d{2}:\\d{2}" // Time pattern often indicates active gathering
        };
        
        for (String pattern : gatheringPatterns) {
            if (lowerLine.matches(".*" + pattern + ".*")) {
                System.out.println("        ✅ [STATUS] GATHERING detected via pattern: " + pattern);
                return MarchStatus.GATHERING;
            }
        }
        
        // Direct status matches
        if (lowerLine.equals("idle")) {
            System.out.println("        ✅ [STATUS] IDLE detected");
            return MarchStatus.IDLE;
        }
        if (lowerLine.equals("returning")) {
            System.out.println("        ✅ [STATUS] RETURNING detected");
            return MarchStatus.RETURNING;
        }
        if (lowerLine.contains("cannot") || lowerLine.contains("can not")) {
            System.out.println("        ✅ [STATUS] CANNOT_USE detected");
            return MarchStatus.CANNOT_USE;
        }
        if (lowerLine.contains("unlock")) {
            System.out.println("        ✅ [STATUS] UNLOCK detected");
            return MarchStatus.UNLOCK;
        }
        
        // Fuzzy matches for OCR errors
        if (lowerLine.matches(".*i.*d.*l.*e.*")) {
            System.out.println("        ✅ [STATUS] IDLE detected (fuzzy)");
            return MarchStatus.IDLE;
        }
        if (lowerLine.matches(".*r.*e.*t.*u.*r.*n.*")) {
            System.out.println("        ✅ [STATUS] RETURNING detected (fuzzy)");
            return MarchStatus.RETURNING;
        }
        
        System.out.println("        ❌ [STATUS] No status pattern matched");
        return null;
    }
    
    /**
     * Check if a number is likely a queue number based on context
     */
    private static boolean isLikelyQueueNumber(String[] lines, int lineIndex, int number) {
        // Check if nearby lines contain queue-related words
        for (int i = Math.max(0, lineIndex - 1); i < Math.min(lines.length, lineIndex + 2); i++) {
            String line = lines[i].toLowerCase();
            if (line.contains("queue") || line.contains("march")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get context around a status detection for debugging
     */
    private static String getStatusContext(String[] lines, int index) {
        StringBuilder context = new StringBuilder();
        for (int i = Math.max(0, index - 1); i < Math.min(lines.length, index + 2); i++) {
            if (context.length() > 0) context.append(" | ");
            context.append(lines[i]);
        }
        return context.toString();
    }
    
    /**
     * Create complete queue list (1-6) with intelligent defaults
     */
    private static List<MarchInfo> createCompleteQueueList(List<MarchInfo> detectedQueues) {
        List<MarchInfo> finalQueues = new ArrayList<>();
        
        for (int i = 1; i <= 6; i++) {
            MarchInfo found = null;
            for (MarchInfo queue : detectedQueues) {
                if (queue.queueNumber == i) {
                    found = queue;
                    break;
                }
            }
            
            if (found != null) {
                finalQueues.add(found);
            } else {
                // Intelligent defaults based on typical game patterns
                MarchStatus defaultStatus;
                if (i <= 3) {
                    defaultStatus = MarchStatus.IDLE; // First 3 queues usually available
                } else {
                    defaultStatus = MarchStatus.CANNOT_USE; // Later queues often locked
                }
                
                finalQueues.add(new MarchInfo(i, defaultStatus, "default"));
                System.out.println("📊 [DEBUG] Default Queue " + i + ": " + defaultStatus + " (not detected in OCR)");
            }
        }
        
        return finalQueues;
    }
}