package newgame;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

/**
 * DEBUG VERSION: MarchDetailsCollector with extensive debugging for OCR coordinate detection
 * This will help us find the correct coordinates for gathering time extraction
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
            System.out.println("🔍 Collecting details for " + deployedMarches.size() + " deployed marches (DEBUG MODE)");
            
            boolean allSuccessful = true;
            
            for (int i = 0; i < deployedMarches.size(); i++) {
                MarchDeployInfo marchInfo = deployedMarches.get(i);
                
                if (!marchInfo.detailsCollected) {
                    System.out.println("🔍 Collecting details for Queue " + marchInfo.queueNumber + " (" + marchInfo.resourceType + ") - March " + (i+1) + "/" + deployedMarches.size() + " (DEBUG)");
                    
                    // Setup march view before each queue
                    if (!setupMarchViewFast()) {
                        System.err.println("❌ Failed to setup march view for Queue " + marchInfo.queueNumber);
                        allSuccessful = false;
                        continue;
                    }
                    
                    if (collectDetailsForQueueWithDebug(marchInfo)) {
                        marchInfo.detailsCollected = true;
                        System.out.println("✅ Successfully collected details for Queue " + marchInfo.queueNumber + " (DEBUG)");
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
            System.out.println("🔧 Setting up march view for instance " + instance.index + " (DEBUG)");
            
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
     * DEBUG VERSION: Collect details for a specific march with extensive debugging
     */
    private boolean collectDetailsForQueueWithDebug(MarchDeployInfo marchInfo) {
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
            
            // DEBUG: Extract gathering time with extensive debugging
            String gatheringTime = extractGatheringTimeWithExtensiveDebug(marchInfo.queueNumber);
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
     * DEBUG VERSION: Extract gathering time with extensive coordinate debugging
     */
    private String extractGatheringTimeWithExtensiveDebug(int queueNumber) {
        try {
            System.out.println("📊 [DEBUG] Extracting gathering time from details page with extensive debugging...");
            
            // FIXED: Add retries for gathering time extraction
            int maxRetries = 3;
            String gatheringTime = null;
            String screenPath = "screenshots/details_page_" + instance.index + ".png";
            
            for (int retry = 0; retry < maxRetries && gatheringTime == null; retry++) {
                if (retry > 0) {
                    System.out.println("🔄 [DEBUG] Retry " + retry + "/" + maxRetries + " for gathering time extraction");
                    Thread.sleep(2000); // Wait before retry
                }
                
                if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                    System.err.println("❌ Failed to take details page screenshot");
                    continue;
                }
                
                System.out.println("📸 [DEBUG] Details page screenshot saved: " + screenPath);
                
                // DEBUG: Try a wide range of coordinates to find the gathering time
                System.out.println("🔍 [DEBUG] Testing multiple coordinate grids to locate gathering time...");
                
                // Test a grid of positions across the details page
                int[] xPositions = {300, 320, 340, 360, 380, 400, 420};
                int[] yPositions = {120, 140, 160, 180, 200, 220};
                int[] widths = {60, 80, 100, 120};
                int[] heights = {15, 20, 25, 30};
                
                String bestResult = null;
                double bestConfidence = 0;
                String bestCoordinates = "";
                
                int testCount = 0;
                for (int x : xPositions) {
                    for (int y : yPositions) {
                        for (int w : widths) {
                            for (int h : heights) {
                                testCount++;
                                String testRegionPath = "screenshots/debug_test_" + testCount + "_" + instance.index + ".png";
                                
                                System.out.println("🔍 [DEBUG] Test " + testCount + ": x=" + x + ", y=" + y + ", w=" + w + ", h=" + h);
                                
                                if (OCRUtils.extractImageRegion(screenPath, testRegionPath, x, y, w, h)) {
                                    String timeText = OCRUtils.performTimeOCR(testRegionPath, instance.index);
                                    
                                    if (timeText != null && !timeText.trim().isEmpty()) {
                                        System.out.println("📋 [DEBUG] Test " + testCount + " OCR result: '" + timeText + "'");
                                        
                                        String parsedTime = TimeUtils.parseTimeFromText(timeText);
                                        if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                                            double confidence = calculateTimeConfidence(timeText, parsedTime);
                                            System.out.println("✅ [DEBUG] Test " + testCount + " found valid time: " + parsedTime + " (confidence: " + confidence + ")");
                                            
                                            if (confidence > bestConfidence) {
                                                bestResult = parsedTime;
                                                bestConfidence = confidence;
                                                bestCoordinates = "x=" + x + ", y=" + y + ", w=" + w + ", h=" + h;
                                                System.out.println("⭐ [DEBUG] New best result: " + bestResult + " at " + bestCoordinates);
                                            }
                                        } else {
                                            System.out.println("⚠️ [DEBUG] Test " + testCount + " invalid time format: '" + timeText + "'");
                                        }
                                    } else {
                                        System.out.println("❌ [DEBUG] Test " + testCount + " empty OCR result");
                                    }
                                } else {
                                    System.err.println("❌ [DEBUG] Test " + testCount + " failed to extract region");
                                }
                                
                                // Limit total tests to avoid too much output
                                if (testCount >= 50) {
                                    System.out.println("📊 [DEBUG] Limiting to first 50 tests to avoid spam");
                                    break;
                                }
                            }
                            if (testCount >= 50) break;
                        }
                        if (testCount >= 50) break;
                    }
                    if (testCount >= 50) break;
                }
                
                System.out.println("📊 [DEBUG] Completed " + testCount + " coordinate tests");
                
                if (bestResult != null) {
                    System.out.println("🎉 [DEBUG] BEST RESULT FOUND!");
                    System.out.println("🎯 [DEBUG] Best time: " + bestResult);
                    System.out.println("📍 [DEBUG] Best coordinates: " + bestCoordinates);
                    System.out.println("⭐ [DEBUG] Best confidence: " + bestConfidence);
                    gatheringTime = bestResult;
                }
            }
            
            if (gatheringTime != null) {
                System.out.println("🎉 [DEBUG] BEST RESULT FOUND!");
                System.out.println("🎯 [DEBUG] Best time: " + gatheringTime);
                return gatheringTime;
            }
            
            // DEBUG: Also try enhanced OCR on the full details page
            System.out.println("🔍 [DEBUG] Trying enhanced OCR on full details page...");
            String fullPageOCR = OCRUtils.performEnhancedOCR(screenPath, instance.index);
            if (fullPageOCR != null) {
                System.out.println("📋 [DEBUG] Full page OCR result:");
                System.out.println("=== FULL PAGE OCR START ===");
                System.out.println(fullPageOCR);
                System.out.println("=== FULL PAGE OCR END ===");
                
                // Try to find time patterns in the full OCR text
                String[] lines = fullPageOCR.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    String parsedTime = TimeUtils.parseTimeFromText(line);
                    if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                        System.out.println("🎯 [DEBUG] Found time pattern in full OCR line " + i + ": '" + line + "' -> " + parsedTime);
                        return parsedTime;
                    }
                }
            }
            
            System.err.println("⚠️ [DEBUG] Could not extract gathering time after extensive testing, using default");
            return "02:00:00";
            
        } catch (Exception e) {
            System.err.println("❌ Error in debug gathering time extraction: " + e.getMessage());
            return "02:00:00";
        }
    }
    
    /**
     * Calculate confidence score for time extraction results
     */
    private double calculateTimeConfidence(String rawText, String parsedTime) {
        double confidence = 0;
        
        // Higher confidence for clean time formats
        if (rawText.matches("\\d{2}:\\d{2}:\\d{2}")) {
            confidence += 10;
        } else if (rawText.matches("\\d{1}:\\d{2}:\\d{2}")) {
            confidence += 8;
        }
        
        // Higher confidence for reasonable march times
        if (parsedTime != null) {
            String[] parts = parsedTime.split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                
                // Reasonable march times (usually 1-6 hours)
                if (hours >= 1 && hours <= 6) confidence += 5;
                if (minutes >= 0 && minutes <= 59) confidence += 2;
                if (seconds >= 0 && seconds <= 59) confidence += 2;
            }
        }
        
        return confidence;
    }
    
    // ... (rest of the methods remain the same as the fast version)
    
    private boolean clickOnMarchQueueAtCenterFast(int queueNumber) {
        try {
            System.out.println("🖱️ Clicking on March Queue " + queueNumber + " (DEBUG)");
            
            Point queuePosition = calculateQueueClickPosition(queueNumber);
            
            if (queuePosition != null) {
                System.out.println("🎯 Clicking Queue " + queueNumber + " at center position: " + queuePosition);
                if (BotUtils.clickMenu(instance.index, queuePosition)) {
                    System.out.println("✅ Clicked on Queue " + queueNumber + " at center position " + queuePosition + " (DEBUG)");
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
            System.out.println("🔍 Looking for details button (DEBUG)...");
            
            String[] detailsButtonImages = {"details_button.png", "details.png"};
            double[] confidences = {0.6, 0.5, 0.4};
            
            for (int attempt = 1; attempt <= 2; attempt++) {
                System.out.println("🔄 Details button detection attempt " + attempt + "/2");
                
                String detailsButtonPath = "screenshots/details_button_debug" + attempt + "_" + instance.index + ".png";
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
                        System.out.println("✅ Clicked details button successfully (DEBUG)");
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
                    System.out.println("✅ Clicked details button with fallback position " + fallbackPos + " (DEBUG)");
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
            System.out.println("❌ Closing details page (DEBUG)...");
            
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
                System.out.println("✅ Closed details page at " + closePos + " (DEBUG)");
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error closing details page: " + e.getMessage());
        }
    }
    
    private void addToMarchTrackerFast(MarchDeployInfo marchInfo, String gatheringTime, String totalTime) {
        try {
            long totalSeconds = TimeUtils.parseTimeToSeconds(totalTime);
            long gatheringSeconds = TimeUtils.parseTimeToSeconds(gatheringTime);
            long marchSeconds = (totalSeconds - gatheringSeconds) / 2;
            String marchTime = TimeUtils.formatTime(marchSeconds);
            
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
                    
                    System.out.println("📊 Successfully added march to tracker GUI (DEBUG)");
                    
                } catch (Exception e) {
                    System.err.println("❌ Error adding to march tracker GUI: " + e.getMessage());
                }
            });
            
            System.out.println("📊 March Added (DEBUG): Instance=" + instance.index + 
                             ", Queue=" + marchInfo.queueNumber + 
                             ", Resource=" + marchInfo.resourceType + 
                             ", Gathering=" + gatheringTime +
                             ", March=" + marchTime +
                             ", Total=" + totalTime);
            
        } catch (Exception e) {
            System.err.println("❌ Error adding to march tracker: " + e.getMessage());
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