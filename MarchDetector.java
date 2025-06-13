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
            
            // FIXED: Extract wider text panel to capture more "idle" text
            String textPanelPath = "screenshots/debug_march_text_panel_" + instanceIndex + ".png";
            if (!extractMarchTextPanelFixed(fullScreenPath, textPanelPath)) {
                System.err.println("❌ Failed to extract march text panel");
                return new ArrayList<>();
            }
            
            // Use enhanced OCR for march queue detection
            String ocrText = OCRUtils.performMarchQueueOCR(textPanelPath, instanceIndex);
            if (ocrText == null || ocrText.trim().isEmpty()) {
                System.err.println("❌ OCR failed or returned empty text");
                return new ArrayList<>();
            }
            
            List<MarchInfo> queues = parseMarchQueuesFixed(ocrText);
            
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
     * FIXED: Extract wider march text panel to better capture "idle" status
     */
    private static boolean extractMarchTextPanelFixed(String sourcePath, String outputPath) {
        try {
            BufferedImage sourceImage = ImageIO.read(new File(sourcePath));
            if (sourceImage == null) {
                System.err.println("❌ Could not load march screenshot: " + sourcePath);
                return false;
            }
            
            // FIXED: Wider extraction to capture full "idle" words
            int panelX = 85;        // Slightly more left to capture full words
            int panelY = 195;       // Start from queue text area
            int panelWidth = 150;   // Wider to capture full "idle" text
            int panelHeight = 290;  // Cover all 6 queues completely
            
            System.out.println("📐 [DEBUG] Extracting FIXED WIDER panel region: x=" + panelX + 
                              ", y=" + panelY + ", w=" + panelWidth + ", h=" + panelHeight);
            System.out.println("🎯 [DEBUG] Wider extraction to capture full 'idle' words");
            
            // Bounds checking
            panelX = Math.max(0, panelX);
            panelY = Math.max(0, panelY);
            panelWidth = Math.min(panelWidth, sourceImage.getWidth() - panelX);
            panelHeight = Math.min(panelHeight, sourceImage.getHeight() - panelY);
            
            if (panelWidth <= 0 || panelHeight <= 0) {
                System.err.println("❌ Invalid panel dimensions after bounds check");
                return false;
            }
            
            // Extract the wider text region
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
                System.out.println("✅ [DEBUG] FIXED wider text panel extracted: " + outputPath + " (size: " + outputFile.length() + " bytes)");
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
     * FIXED: Better parsing with enhanced "idle" detection
     */
    private static List<MarchInfo> parseMarchQueuesFixed(String ocrText) {
        try {
            System.out.println("🔧 [DEBUG] Starting FIXED march queue parsing...");
            
            String[] lines = ocrText.split("\n");
            List<MarchInfo> queues = new ArrayList<>();
            
            System.out.println("📝 [DEBUG] Raw lines (" + lines.length + "):");
            for (int i = 0; i < lines.length; i++) {
                String cleaned = lines[i].trim();
                System.out.println("  " + i + ": '" + lines[i] + "' -> '" + cleaned + "'");
                lines[i] = cleaned;
            }
            
            // FIXED: Enhanced parsing with better "idle" detection
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                System.out.println("🔍 [DEBUG] Analyzing line " + i + ": '" + line + "'");
                
                // Look for queue numbers in current or nearby lines
                int queueNumber = extractQueueNumber(line, lines, i);
                if (queueNumber > 0) {
                    System.out.println("  🔍 [DEBUG] Found queue number: " + queueNumber);
                    
                    // Look for status in current line or next few lines
                    MarchStatus status = findStatusForQueueFixed(lines, i, queueNumber);
                    if (status != null) {
                        System.out.println("  ✅ [DEBUG] Found " + status + " for Queue " + queueNumber);
                        queues.add(new MarchInfo(queueNumber, status, getStatusContext(lines, i)));
                    }
                }
            }
            
            // FIXED: Improved queue completion with better idle detection
            List<MarchInfo> finalQueues = createCompleteQueueListFixed(queues, lines);
            
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
     * FIXED: Find status for a specific queue with enhanced idle detection
     */
    private static MarchStatus findStatusForQueueFixed(String[] lines, int startIndex, int queueNumber) {
        // Check current line and next 3 lines for status
        for (int i = startIndex; i < Math.min(lines.length, startIndex + 4); i++) {
            MarchStatus status = detectMarchStatusFixed(lines[i]);
            if (status != null) {
                System.out.println("    🎯 [DEBUG] Found status " + status + " for queue " + queueNumber + " in line: '" + lines[i] + "'");
                return status;
            }
        }
        
        // FIXED: If queue 1 or 2, check for isolated "idle" text patterns
        if (queueNumber <= 2) {
            for (int i = Math.max(0, startIndex - 2); i < Math.min(lines.length, startIndex + 4); i++) {
                if (isIdleText(lines[i])) {
                    System.out.println("    🎯 [DEBUG] Found IDLE pattern for queue " + queueNumber + " in line: '" + lines[i] + "'");
                    return MarchStatus.IDLE;
                }
            }
        }
        
        // If no status found, return intelligent default
        return (queueNumber <= 2) ? MarchStatus.IDLE : MarchStatus.CANNOT_USE;
    }
    
    /**
     * FIXED: Enhanced status detection with much better "idle" recognition
     */
    private static MarchStatus detectMarchStatusFixed(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        String lowerLine = line.toLowerCase().trim();
        System.out.println("      🔍 [STATUS] Analyzing: '" + lowerLine + "'");
        
        // FIXED: Much better idle detection patterns
        if (isIdleText(lowerLine)) {
            System.out.println("        ✅ [STATUS] IDLE detected");
            return MarchStatus.IDLE;
        }
        
        // Enhanced gathering detection patterns
        String[] gatheringPatterns = {
            "gathering",
            "gather",
            "gath",
            "ering",
            "athering",
            "g.*a.*t.*h.*e.*r",  // Scattered letters
            ".*g.*a.*t.*h.*",    // Partial with scattered
            "lvl.*\\d+.*mill",   // "Gathering Lvl X Mill" pattern
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
        if (lowerLine.matches(".*r.*e.*t.*u.*r.*n.*")) {
            System.out.println("        ✅ [STATUS] RETURNING detected (fuzzy)");
            return MarchStatus.RETURNING;
        }
        
        System.out.println("        ❌ [STATUS] No status pattern matched");
        return null;
    }
    
    /**
     * FIXED: Much better idle text detection
     */
    private static boolean isIdleText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase().trim();
        
        // Direct matches
        if (lowerText.equals("idle")) {
            return true;
        }
        
        // Common OCR variations of "idle"
        String[] idleVariations = {
            "idle",
            "ldle",     // 'i' mistaken for 'l'
            "ide",      // Missing first letter
            "dle",      // Missing first two letters  
            "idIe",     // Case variations
            "idl",      // Missing last letter
            "id1e",     // '1' mistaken for 'l'
            "1dle",     // '1' mistaken for 'i'
            "1d1e",     // Both mistakes
            "idie",     // 'l' mistaken for 'i'
            "ldie"      // 'i' and 'l' swapped
        };
        
        for (String variation : idleVariations) {
            if (lowerText.equals(variation)) {
                System.out.println("        ✅ [IDLE] Detected idle variation: '" + variation + "'");
                return true;
            }
        }
        
        // Fuzzy pattern matching for scattered "idle" letters
        if (lowerText.length() >= 3 && lowerText.length() <= 6) {
            // Must contain 'i', 'd', 'l', 'e' in roughly that order
            if (lowerText.matches(".*i.*d.*l.*e.*") || 
                lowerText.matches(".*i.*d.*[l1].*e.*") ||
                lowerText.matches(".*[i1].*d.*l.*e.*")) {
                System.out.println("        ✅ [IDLE] Detected fuzzy idle pattern: '" + lowerText + "'");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Extract queue number with better pattern matching
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
     * FIXED: Create complete queue list with better idle detection
     */
    private static List<MarchInfo> createCompleteQueueListFixed(List<MarchInfo> detectedQueues, String[] allLines) {
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
                // FIXED: Better intelligent defaults with idle detection
                MarchStatus defaultStatus;
                
                if (i <= 2) {
                    // For queues 1-2, check if there are idle indicators in the OCR text
                    boolean foundIdleInText = false;
                    for (String line : allLines) {
                        if (isIdleText(line)) {
                            foundIdleInText = true;
                            break;
                        }
                    }
                    
                    if (foundIdleInText) {
                        defaultStatus = MarchStatus.IDLE;
                        System.out.println("📊 [DEBUG] Default Queue " + i + ": IDLE (found idle text in OCR)");
                    } else {
                        defaultStatus = MarchStatus.IDLE; // Still default to idle for first 2 queues
                        System.out.println("📊 [DEBUG] Default Queue " + i + ": IDLE (standard default)");
                    }
                } else {
                    defaultStatus = MarchStatus.CANNOT_USE; // Later queues usually locked
                    System.out.println("📊 [DEBUG] Default Queue " + i + ": CANNOT_USE (not detected in OCR)");
                }
                
                finalQueues.add(new MarchInfo(i, defaultStatus, "default"));
            }
        }
        
        return finalQueues;
    }
}