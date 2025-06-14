package newgame;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

/**
 * SIMPLIFIED VERSION: MarchDetailsCollector with efficient OCR strategy
 * Focus on full page OCR which actually works, skip endless coordinate testing
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
            System.out.println("🔍 Collecting details for " + deployedMarches.size() + " deployed marches (SIMPLIFIED)");
            
            boolean allSuccessful = true;
            
            for (int i = 0; i < deployedMarches.size(); i++) {
                MarchDeployInfo marchInfo = deployedMarches.get(i);
                
                if (!marchInfo.detailsCollected) {
                    System.out.println("🔍 Collecting details for Queue " + marchInfo.queueNumber + " (" + marchInfo.resourceType + ") - March " + (i+1) + "/" + deployedMarches.size() + " (SIMPLIFIED)");
                    
                    // Setup march view before each queue
                    if (!setupMarchViewFast()) {
                        System.err.println("❌ Failed to setup march view for Queue " + marchInfo.queueNumber);
                        allSuccessful = false;
                        continue;
                    }
                    
                    if (collectDetailsForQueueSimplified(marchInfo)) {
                        marchInfo.detailsCollected = true;
                        System.out.println("✅ Successfully collected details for Queue " + marchInfo.queueNumber + " (SIMPLIFIED)");
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
            System.out.println("🔧 Setting up march view for instance " + instance.index + " (SIMPLIFIED)");
            
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
     * SIMPLIFIED VERSION: Collect details with efficient strategy - skip coordinate loops!
     */
    private boolean collectDetailsForQueueSimplified(MarchDeployInfo marchInfo) {
        try {
            String screenshotPath = "screenshots/march_details_" + instance.index + "_queue" + marchInfo.queueNumber + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenshotPath)) {
                System.err.println("❌ Failed to take march details screenshot");
                return false;
            }
            
            // Click on the specific march queue using center-based coordinates
            if (!clickOnMarchQueueAtCenterFast(marchInfo.queueNumber)) {
                System.err.println("❌ Failed to click on Queue " + marchInfo.queueNumber);
                return false;
            }
            
            // Find and click details button
            if (!clickDetailsButtonFast()) {
                System.err.println("❌ Failed to click details button for Queue " + marchInfo.queueNumber);
                return false;
            }
            
            // SIMPLIFIED: Extract gathering time efficiently (no endless coordinate loops!)
            String gatheringTime = extractGatheringTimeSimplified(marchInfo.queueNumber);
            if (gatheringTime != null) {
                System.out.println("✅ Extracted gathering time: " + gatheringTime + " for Queue " + marchInfo.queueNumber);
                
                // Store the actual gathering time
                marchInfo.actualGatheringTime = gatheringTime;
                
                // Calculate total time
                String totalTime = calculateTotalTime(marchInfo.estimatedDeployDuration, gatheringTime);
                
                // Add to march tracker using existing GUI
                addToMarchTrackerFast(marchInfo, gatheringTime, totalTime);
                
                System.out.println("📊 Added to march tracker with total time: " + totalTime + " for Queue " + marchInfo.queueNumber);
            } else {
                System.err.println("⚠️ Could not extract gathering time for Queue " + marchInfo.queueNumber);
                // Still try to add with estimated times
                addToMarchTrackerFast(marchInfo, "02:00:00", marchInfo.calculateTotalTime());
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
     * SIMPLIFIED: Extract gathering time efficiently - focus on what works!
     * Skip endless coordinate testing, go straight to full page OCR
     */
    private String extractGatheringTimeSimplified(int queueNumber) {
        try {
            System.out.println("📊 [SIMPLIFIED] Extracting gathering time efficiently...");
            
            String screenPath = "screenshots/details_page_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("❌ Failed to take details page screenshot");
                return "02:00:00";
            }
            
            System.out.println("📸 [SIMPLIFIED] Details page screenshot saved: " + screenPath);
            
            // STRATEGY 1: Try a few key coordinates (but don't loop endlessly!)
            String[] testCoordinates = {
                "440,165,80,20",  // Your original corrected coordinates
                "400,165,80,20",  // Slightly left
                "380,165,100,25", // Wider area
                "420,155,100,30", // Higher and wider
                "360,160,120,25"  // Even wider area
            };
            
            System.out.println("🎯 [SIMPLIFIED] Testing " + testCoordinates.length + " key coordinate areas...");
            
            for (int i = 0; i < testCoordinates.length; i++) {
                String[] coords = testCoordinates[i].split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int w = Integer.parseInt(coords[2]);
                int h = Integer.parseInt(coords[3]);
                
                System.out.println("🔍 [SIMPLIFIED] Test " + (i+1) + "/" + testCoordinates.length + ": x=" + x + ", y=" + y + ", w=" + w + ", h=" + h);
                
                String timeRegionPath = "screenshots/test_region_" + (i+1) + "_" + instance.index + ".png";
                if (OCRUtils.extractImageRegion(screenPath, timeRegionPath, x, y, w, h)) {
                    // Try enhanced OCR (not just digits) to get context
                    String timeText = OCRUtils.performEnhancedOCR(timeRegionPath, instance.index);
                    if (timeText != null && !timeText.trim().isEmpty()) {
                        System.out.println("📋 [SIMPLIFIED] Test " + (i+1) + " enhanced OCR: '" + timeText + "'");
                        
                        String parsedTime = TimeUtils.parseTimeFromText(timeText);
                        if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                            System.out.println("✅ [SIMPLIFIED] SUCCESS! Found valid time: " + parsedTime + " at coordinates " + testCoordinates[i]);
                            return parsedTime;
                        }
                    }
                }
            }
            
            // STRATEGY 2: Full page OCR (this is what actually worked in your log!)
            System.out.println("🔍 [SIMPLIFIED] Key coordinates failed, trying full page OCR (this worked before)...");
            String fullPageOCR = OCRUtils.performEnhancedOCR(screenPath, instance.index);
            if (fullPageOCR != null) {
                System.out.println("📋 [SIMPLIFIED] Full page OCR result:");
                System.out.println("=== FULL PAGE OCR START ===");
                System.out.println(fullPageOCR);
                System.out.println("=== FULL PAGE OCR END ===");
                
                // Look for patterns like "Gatheredin 024726" or "Gathering 02:47:26"
                String[] lines = fullPageOCR.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].toLowerCase();
                    
                    // Look for gathering-related lines
                    if (line.contains("gather") || line.contains("time") || line.contains("duration")) {
                        System.out.println("🎯 [SIMPLIFIED] Potential gathering line " + i + ": '" + lines[i] + "'");
                        
                        String parsedTime = TimeUtils.parseTimeFromText(lines[i]);
                        if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                            System.out.println("✅ [SIMPLIFIED] SUCCESS! Found time from full page OCR: " + parsedTime);
                            return parsedTime;
                        }
                    }
                }
                
                // If no gathering-specific lines, check all lines for time patterns
                for (int i = 0; i < lines.length; i++) {
                    String parsedTime = TimeUtils.parseTimeFromText(lines[i]);
                    if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                        System.out.println("✅ [SIMPLIFIED] SUCCESS! Found time pattern in line " + i + ": '" + lines[i] + "' -> " + parsedTime);
                        return parsedTime;
                    }
                }
            }
            
            System.err.println("⚠️ [SIMPLIFIED] Could not extract gathering time, using default");
            return "02:00:00";
            
        } catch (Exception e) {
            System.err.println("❌ Error in simplified gathering time extraction: " + e.getMessage());
            return "02:00:00";
        }
    }
    
    private boolean clickOnMarchQueueAtCenterFast(int queueNumber) {
        try {
            System.out.println("🖱️ Clicking on March Queue " + queueNumber + " (SIMPLIFIED)");
            
            Point queuePosition = calculateQueueClickPosition(queueNumber);
            
            if (queuePosition != null) {
                System.out.println("🎯 Clicking Queue " + queueNumber + " at center position: " + queuePosition);
                if (BotUtils.clickMenu(instance.index, queuePosition)) {
                    System.out.println("✅ Clicked on Queue " + queueNumber + " at center position " + queuePosition + " (SIMPLIFIED)");
                    Thread.sleep(2000);
                    return true;
                } else {
                    System.err.println("❌ Failed to click on Queue " + queueNumber);
                    return false;
                }
            } else {
                System.err.println("❌ Could not determine position for Queue " + queueNumber);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking on march queue: " + e.getMessage());
            return false;
        }
    }
    
    private Point calculateQueueClickPosition(int queueNumber) {
        int baseY = 200 + 30;
        int queueY = baseY + (queueNumber - 1) * 55;
        int centerX = 240;
        
        Point position = new Point(centerX, queueY);
        System.out.println("🎯 [CALC] Queue " + queueNumber + " center position: " + position);
        return position;
    }
    
    private boolean clickDetailsButtonFast() {
        try {
            System.out.println("🔍 Looking for details button (SIMPLIFIED)...");
            
            String[] detailsButtonImages = {"details_button.png", "details.png"};
            double[] confidences = {0.6, 0.5, 0.4};
            
            for (int attempt = 1; attempt <= 2; attempt++) {
                System.out.println("🔄 Details button detection attempt " + attempt + "/2");
                
                String detailsButtonPath = "screenshots/details_button_simplified" + attempt + "_" + instance.index + ".png";
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
                        System.out.println("✅ Clicked details button successfully (SIMPLIFIED)");
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
                    System.out.println("✅ Clicked details button with fallback position " + fallbackPos + " (SIMPLIFIED)");
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
            System.out.println("❌ Closing details page (SIMPLIFIED)...");
            
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
                System.out.println("✅ Closed details page at " + closePos + " (SIMPLIFIED)");
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error closing details page: " + e.getMessage());
        }
    }
    
    private void addToMarchTrackerFast(MarchDeployInfo marchInfo, String gatheringTime, String totalTime) {
        try {
            // Calculate times correctly whether we have actual gathering time or not
            if (gatheringTime != null && !gatheringTime.equals("02:00:00")) {
                // We have real gathering time from details page
                long deploySeconds = TimeUtils.parseTimeToSeconds(marchInfo.estimatedDeployDuration);
                long gatheringSeconds = TimeUtils.parseTimeToSeconds(gatheringTime);
                
                // Total time is deploy + gather + deploy (round trip)
                long totalSeconds = deploySeconds + gatheringSeconds + deploySeconds;
                String calculatedTotal = TimeUtils.formatTime(totalSeconds);
                
                // March time is one-way deploy time
                String marchTime = marchInfo.estimatedDeployDuration;
                
                System.out.println("📊 Time calculation for Queue " + marchInfo.queueNumber + ":");
                System.out.println("  - Deploy time (one-way): " + marchTime + " (" + deploySeconds + "s)");
                System.out.println("  - Gathering time: " + gatheringTime + " (" + gatheringSeconds + "s)");
                System.out.println("  - Total time: " + calculatedTotal + " (" + totalSeconds + "s)");
                
                addToTracker(marchInfo, gatheringTime, marchTime, calculatedTotal);
            } else {
                // Details collection failed - use deploy time + estimated gathering
                long deploySeconds = TimeUtils.parseTimeToSeconds(marchInfo.estimatedDeployDuration);
                String estimatedGatherTime = "02:00:00"; // Default gathering time
                long gatheringSeconds = TimeUtils.parseTimeToSeconds(estimatedGatherTime);
                
                // Total time is deploy + gather + deploy
                long totalSeconds = deploySeconds + gatheringSeconds + deploySeconds;
                String calculatedTotal = TimeUtils.formatTime(totalSeconds);
                
                System.out.println("⚠️ Using estimated times for Queue " + marchInfo.queueNumber + ":");
                System.out.println("  - Deploy time (one-way): " + marchInfo.estimatedDeployDuration + " (" + deploySeconds + "s)");
                System.out.println("  - Estimated gathering: " + estimatedGatherTime + " (" + gatheringSeconds + "s)");
                System.out.println("  - Total time: " + calculatedTotal + " (" + totalSeconds + "s)");
                
                addToTracker(marchInfo, estimatedGatherTime, marchInfo.estimatedDeployDuration, calculatedTotal);
            }
        } catch (Exception e) {
            System.err.println("❌ Error adding to march tracker: " + e.getMessage());
        }
    }
    
    private void addToTracker(MarchDeployInfo marchInfo, String gatheringTime, String marchTime, String totalTime) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                MarchTrackerGUI tracker = MarchTrackerGUI.getInstance();
                tracker.addMarch(
                    instance.index,
                    marchInfo.queueNumber,
                    marchInfo.resourceType,
                    gatheringTime,
                    marchTime,
                    totalTime
                );
                
                tracker.setVisible(true);
                tracker.toFront();
                
                System.out.println("📊 Successfully added march to tracker GUI (SIMPLIFIED)");
                System.out.println("📊 March Details: Gathering=" + gatheringTime + 
                                 ", March=" + marchTime + 
                                 ", Total=" + totalTime);
                
            } catch (Exception e) {
                System.err.println("❌ Error adding to march tracker GUI: " + e.getMessage());
            }
        });
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