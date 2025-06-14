package newgame;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

/**
 * FIXED MarchDetailsCollector with correct queue number handling
 * Queue numbers now match exactly what's shown in-game (no conversion)
 */
public class MarchDetailsCollector {
    
    private final MemuInstance instance;
    
    public MarchDetailsCollector(MemuInstance instance) {
        this.instance = instance;
    }
    
    /**
     * Main method called by AutoGatherResourcesTask
     */
    public boolean collectMarchDetailsFromAllDeployedMarches(List<MarchDeployInfo> deployedMarches) {
        try {
            System.out.println("🔍 Collecting details for " + deployedMarches.size() + " deployed marches (FIXED QUEUE NUMBERS)");
            
            boolean allSuccessful = true;
            
            for (int i = 0; i < deployedMarches.size(); i++) {
                MarchDeployInfo marchInfo = deployedMarches.get(i);
                
                if (!marchInfo.detailsCollected) {
                    System.out.println("🔍 Collecting details for Queue " + marchInfo.queueNumber + " (" + marchInfo.resourceType + ") - March " + (i+1) + "/" + deployedMarches.size() + " (FIXED)");
                    
                    // Setup march view before each queue
                    if (!setupMarchViewFast()) {
                        System.err.println("❌ Failed to setup march view for Queue " + marchInfo.queueNumber);
                        allSuccessful = false;
                        continue;
                    }
                    
                    if (collectDetailsForQueueFixed(marchInfo)) {
                        marchInfo.detailsCollected = true;
                        System.out.println("✅ Successfully collected details for Queue " + marchInfo.queueNumber + " (FIXED)");
                    } else {
                        allSuccessful = false;
                        System.err.println("❌ Failed to collect details for Queue " + marchInfo.queueNumber);
                    }
                    
                    // Wait between queues
                    if (i < deployedMarches.size() - 1) {
                        Thread.sleep(1000);
                    }
                }
            }
            
            return allSuccessful;
            
        } catch (Exception e) {
            System.err.println("❌ Error collecting march details: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Setup march view using faster navigation methods
     */
    private boolean setupMarchViewFast() {
        try {
            System.out.println("🔧 Setting up march view for instance " + instance.index + " (FIXED)");
            
            // Take screenshot to see current state
            String currentScreenPath = "screenshots/current_state_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, currentScreenPath)) {
                System.err.println("❌ Failed to take current state screenshot");
                return false;
            }
            
            // Check if we need to open left panel
            Point openLeftButton = BotUtils.findImageOnScreen(currentScreenPath, "open_left.png", 0.6);
            if (openLeftButton != null) {
                System.out.println("📍 Found open_left button, clicking to open panel...");
                if (BotUtils.clickMenu(instance.index, openLeftButton)) {
                    System.out.println("✅ Opened left panel");
                    Thread.sleep(1000);
                } else {
                    System.err.println("❌ Failed to click open_left button");
                    return false;
                }
            } else {
                System.out.println("📍 Left panel already open or not needed");
            }
            
            // Look for wilderness button to enter march mode
            String afterOpenPath = "screenshots/after_open_left_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, afterOpenPath);
            
            Point wildernessButton = BotUtils.findImageOnScreen(afterOpenPath, "wilderness_button.png", 0.6);
            if (wildernessButton != null) {
                System.out.println("🌍 Found wilderness button, entering march mode...");
                if (BotUtils.clickMenu(instance.index, wildernessButton)) {
                    System.out.println("✅ Entered wilderness/march mode");
                    Thread.sleep(2000);
                    return true;
                } else {
                    System.err.println("❌ Failed to click wilderness button");
                    return false;
                }
            } else {
                System.out.println("📍 Already in march view or wilderness button not visible");
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error setting up march view: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Collect details with correct queue number handling
     */
    private boolean collectDetailsForQueueFixed(MarchDeployInfo marchInfo) {
        try {
            String screenshotPath = "screenshots/march_details_" + instance.index + "_queue" + marchInfo.queueNumber + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenshotPath)) {
                System.err.println("❌ Failed to take march details screenshot");
                return false;
            }
            
            // FIXED: Click on the specific march queue using exact queue number
            if (!clickOnMarchQueueFixed(marchInfo.queueNumber)) {
                System.err.println("❌ Failed to click on Queue " + marchInfo.queueNumber);
                return false;
            }
            
            // Find and click details button
            if (!clickDetailsButtonFast()) {
                System.err.println("❌ Failed to click details button for Queue " + marchInfo.queueNumber);
                return false;
            }
            
            // Extract gathering time efficiently
            String gatheringTime = extractGatheringTimeSimplified(marchInfo.queueNumber);
            if (gatheringTime != null) {
                System.out.println("✅ Extracted gathering time: " + gatheringTime + " for Queue " + marchInfo.queueNumber);
                
                // Store the actual gathering time
                marchInfo.actualGatheringTime = gatheringTime;
                
                // Calculate total time
                String totalTime = calculateTotalTime(marchInfo.estimatedDeployDuration, gatheringTime);
                
                System.out.println("📊 Time calculation for Queue " + marchInfo.queueNumber + " (FIXED):");
                System.out.println("  - Deploy time: " + marchInfo.estimatedDeployDuration);
                System.out.println("  - Gathering time: " + gatheringTime);
                System.out.println("  - Total time: " + totalTime);
                
            } else {
                System.err.println("⚠️ Could not extract gathering time for Queue " + marchInfo.queueNumber);
                // Still mark as collected with estimated times
                marchInfo.actualGatheringTime = "02:00:00";
            }
            
            // Close details page
            closeDetailsPageFast();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error collecting details for Queue " + marchInfo.queueNumber + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * PRECISE: Extract gathering time using exact coordinates from the image analysis
     */
    private String extractGatheringTimeSimplified(int queueNumber) {
        try {
            System.out.println("📊 [PRECISE] Extracting gathering time for Queue " + queueNumber + " using exact coordinates...");
            
            String screenPath = "screenshots/details_page_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("❌ Failed to take details page screenshot");
                return "02:00:00";
            }
            
            System.out.println("📸 [PRECISE] Details page screenshot saved: " + screenPath);
            
            // PRECISE: Extract the exact "Gathered in" time value
            // Based on image analysis: "02:59:12" needs to be moved up and left from previous coordinates
            int x = 355;      // X position moved LEFT from 367
            int y = 145;      // Y position moved UP from 155  
            int width = 80;   // Width to capture "02:59:12"
            int height = 20;  // Height for single line text
            
            System.out.println("🎯 [PRECISE] Using exact coordinates: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height);
            
            String timeRegionPath = "screenshots/precise_gather_time_" + instance.index + ".png";
            if (OCRUtils.extractImageRegion(screenPath, timeRegionPath, x, y, width, height)) {
                // Use time-specific OCR for best results with HH:MM:SS format
                String timeText = OCRUtils.performTimeOCR(timeRegionPath, instance.index);
                if (timeText != null && !timeText.trim().isEmpty()) {
                    System.out.println("📋 [PRECISE] OCR result: '" + timeText + "'");
                    
                    String parsedTime = TimeUtils.parseTimeFromText(timeText);
                    if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                        System.out.println("✅ [PRECISE] SUCCESS! Found gathering time: " + parsedTime);
                        return parsedTime;
                    } else {
                        System.out.println("⚠️ [PRECISE] Could not parse valid time from: '" + timeText + "'");
                    }
                }
            }
            
            // FALLBACK: Full page OCR if precise extraction fails
            System.out.println("🔍 [PRECISE] Precise extraction failed, trying full page OCR as fallback...");
            String fullPageOCR = OCRUtils.performEnhancedOCR(screenPath, instance.index);
            if (fullPageOCR != null) {
                String[] lines = fullPageOCR.split("\n");
                for (String line : lines) {
                    // Look for "Gathered in" or "Gatheredin" pattern
                    if (line.toLowerCase().contains("gatheredin") || line.toLowerCase().contains("gathered in")) {
                        System.out.println("🎯 [PRECISE] Found gathering line: '" + line + "'");
                        
                        String parsedTime = TimeUtils.parseTimeFromText(line);
                        if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                            System.out.println("✅ [PRECISE] SUCCESS! Extracted from full page: " + parsedTime);
                            return parsedTime;
                        }
                    }
                }
            }
            
            System.err.println("⚠️ [PRECISE] Could not extract gathering time, using default");
            return "02:00:00";
            
        } catch (Exception e) {
            System.err.println("❌ Error in precise gathering time extraction: " + e.getMessage());
            return "02:00:00";
        }
    }
    
    /**
     * FIXED: Click on march queue with correct positioning (no queue conversion)
     */
    private boolean clickOnMarchQueueFixed(int queueNumber) {
        try {
            System.out.println("🖱️ Clicking on March Queue " + queueNumber + " (FIXED - exact queue number)");
            
            // FIXED: Calculate position based on exact queue number (no conversion)
            Point queuePosition = calculateQueueClickPositionFixed(queueNumber);
            
            if (queuePosition != null) {
                System.out.println("🎯 Clicking Queue " + queueNumber + " at FIXED position: " + queuePosition);
                if (BotUtils.clickMenu(instance.index, queuePosition)) {
                    System.out.println("✅ Clicked on Queue " + queueNumber + " at FIXED position " + queuePosition);
                    Thread.sleep(2000);
                    return true;
                } else {
                    System.err.println("❌ Failed to click on Queue " + queueNumber);
                    return false;
                }
            } else {
                System.err.println("❌ Could not determine FIXED position for Queue " + queueNumber);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking on march queue: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Calculate queue click position using exact queue numbers
     */
    private Point calculateQueueClickPositionFixed(int queueNumber) {
        // FIXED: Use exact queue number for position calculation
        int baseY = 200 + 30;
        int queueY = baseY + (queueNumber - 1) * 55;  // Queue 1 at baseY, Queue 2 at baseY+55, etc.
        int centerX = 240;
        
        Point position = new Point(centerX, queueY);
        System.out.println("🎯 [FIXED] Queue " + queueNumber + " position (no conversion): " + position);
        return position;
    }
    
    private boolean clickDetailsButtonFast() {
        try {
            System.out.println("🔍 Looking for details button (FIXED)...");
            
            String[] detailsButtonImages = {"details_button.png", "details.png"};
            double[] confidences = {0.6, 0.5, 0.4};
            
            for (int attempt = 1; attempt <= 2; attempt++) {
                System.out.println("🔄 Details button detection attempt " + attempt + "/2");
                
                String detailsButtonPath = "screenshots/details_button_fixed" + attempt + "_" + instance.index + ".png";
                if (!BotUtils.takeScreenshot(instance.index, detailsButtonPath)) {
                    System.err.println("❌ Failed to take details button screenshot on attempt " + attempt);
                    continue;
                }
                
                Point detailsPos = null;
                
                for (String imageName : detailsButtonImages) {
                    for (double confidence : confidences) {
                        detailsPos = BotUtils.findImageOnScreen(detailsButtonPath, imageName, confidence);
                        if (detailsPos != null) {
                            System.out.println("✅ Found " + imageName + " at " + detailsPos + " (confidence: " + confidence + ") on attempt " + attempt);
                            break;
                        }
                    }
                    if (detailsPos != null) break;
                }
                
                if (detailsPos != null) {
                    if (BotUtils.clickMenu(instance.index, detailsPos)) {
                        System.out.println("✅ Clicked details button successfully (FIXED)");
                        Thread.sleep(2000);
                        return true;
                    }
                } else {
                    if (attempt < 2) {
                        Thread.sleep(1000);
                    }
                }
            }
            
            // Fallback positions
            Point[] fallbackPositions = {
                new Point(271, 661),
                new Point(275, 665),
                new Point(267, 657)
            };
            
            for (Point fallbackPos : fallbackPositions) {
                if (BotUtils.clickMenu(instance.index, fallbackPos)) {
                    System.out.println("✅ Clicked details button with fallback position " + fallbackPos + " (FIXED)");
                    Thread.sleep(2000);
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking details button: " + e.getMessage());
            return false;
        }
    }
    
    private void closeDetailsPageFast() {
        try {
            System.out.println("❌ Closing details page (FIXED)...");
            
            String closeDetailsPath = "screenshots/close_details_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, closeDetailsPath)) {
                return;
            }
            
            Point closePos = BotUtils.findImageOnScreen(closeDetailsPath, "close_gather.png", 0.7);
            
            if (closePos == null) {
                closePos = BotUtils.findImageOnScreen(closeDetailsPath, "close_x.png", 0.6);
            }
            
            if (closePos == null) {
                closePos = new Point(415, 59);
            }
            
            if (BotUtils.clickMenu(instance.index, closePos)) {
                System.out.println("✅ Closed details page at " + closePos + " (FIXED)");
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error closing details page: " + e.getMessage());
        }
    }
    
    public String calculateTotalTime(String marchTime, String gatheringTime) {
        try {
            long marchSeconds = TimeUtils.parseTimeToSeconds(marchTime);
            long gatheringSeconds = TimeUtils.parseTimeToSeconds(gatheringTime);
            
            long totalSeconds = marchSeconds + gatheringSeconds + marchSeconds;
            
            return TimeUtils.formatTime(totalSeconds);
            
        } catch (Exception e) {
            System.err.println("❌ Error calculating total time: " + e.getMessage());
            return marchTime;
        }
    }
}