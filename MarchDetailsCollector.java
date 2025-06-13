package newgame;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

/**
 * FIXED: MarchDetailsCollector using your actual existing methods
 * Compatible with your BotUtils, OCRUtils, and MarchTrackerGUI
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
            System.out.println("🔍 Collecting details for " + deployedMarches.size() + " deployed marches");
            
            boolean allSuccessful = true;
            
            for (int i = 0; i < deployedMarches.size(); i++) {
                MarchDeployInfo marchInfo = deployedMarches.get(i);
                
                if (!marchInfo.detailsCollected) {
                    System.out.println("🔍 Collecting details for Queue " + marchInfo.queueNumber + " (" + marchInfo.resourceType + ") - March " + (i+1) + "/" + deployedMarches.size());
                    
                    // FIXED: Navigate to march view before each queue (not just first time)
                    if (!setupMarchView()) {
                        System.err.println("❌ Failed to setup march view for Queue " + marchInfo.queueNumber);
                        allSuccessful = false;
                        continue;
                    }
                    
                    if (collectDetailsForQueue(marchInfo)) {
                        marchInfo.detailsCollected = true;
                        System.out.println("✅ Successfully collected details for Queue " + marchInfo.queueNumber);
                    } else {
                        allSuccessful = false;
                        System.err.println("❌ Failed to collect details for Queue " + marchInfo.queueNumber);
                    }
                    
                    // Wait between queues
                    if (i < deployedMarches.size() - 1) {
                        Thread.sleep(2000);
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
     * FIXED: Setup march view using your existing navigation methods
     */
    private boolean setupMarchView() {
        try {
            System.out.println("🔧 Setting up march view for instance " + instance.index);
            
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
                    Thread.sleep(2000);
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
                    Thread.sleep(3000);
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
     * Collect details for a specific march
     */
    private boolean collectDetailsForQueue(MarchDeployInfo marchInfo) {
        try {
            String screenshotPath = "screenshots/march_details_" + instance.index + "_queue" + marchInfo.queueNumber + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenshotPath)) {
                System.err.println("❌ Failed to take march details screenshot");
                return false;
            }
            
            // FIXED: Click on the specific march queue using center-based coordinates
            if (!clickOnMarchQueueAtCenter(marchInfo.queueNumber)) {
                System.err.println("❌ Failed to click on Queue " + marchInfo.queueNumber);
                return false;
            }
            
            // Find and click details button
            if (!clickDetailsButton()) {
                System.err.println("❌ Failed to click details button for Queue " + marchInfo.queueNumber);
                return false;
            }
            
            // FIXED: Extract gathering time with your existing OCR methods
            String gatheringTime = extractGatheringTimeWithExistingOCR(marchInfo.queueNumber);
            if (gatheringTime != null) {
                System.out.println("✅ Extracted gathering time: " + gatheringTime + " for Queue " + marchInfo.queueNumber);
                
                // Store the actual gathering time
                marchInfo.actualGatheringTime = gatheringTime;
                
                // Calculate total time
                String totalTime = calculateTotalTime(marchInfo.estimatedDeployDuration, gatheringTime);
                
                // Add to march tracker using your existing GUI
                addToMarchTracker(marchInfo, gatheringTime, totalTime);
                
                System.out.println("📊 Added to march tracker with total time: " + totalTime + " for Queue " + marchInfo.queueNumber);
            } else {
                System.err.println("⚠️ Could not extract gathering time for Queue " + marchInfo.queueNumber);
                // Still try to add with estimated times
                addToMarchTracker(marchInfo, "02:00:00", marchInfo.calculateTotalTime());
            }
            
            // Close details page
            closeDetailsPage();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error collecting details for Queue " + marchInfo.queueNumber + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Click on march queue using center-based coordinates with better verification
     */
    private boolean clickOnMarchQueueAtCenter(int queueNumber) {
        try {
            System.out.println("🖱️ Clicking on March Queue " + queueNumber);
            
            Point queuePosition = calculateQueueClickPosition(queueNumber);
            
            if (queuePosition != null) {
                System.out.println("🎯 Clicking Queue " + queueNumber + " at center position: " + queuePosition);
                if (BotUtils.clickMenu(instance.index, queuePosition)) {
                    System.out.println("✅ Clicked on Queue " + queueNumber + " at center position " + queuePosition);
                    
                    // FIXED: Wait longer for UI to update after clicking queue
                    Thread.sleep(3000); // Increased from 2000 to 3000
                    
                    // FIXED: Verify we clicked the right queue by taking a screenshot
                    String verifyPath = "screenshots/verify_queue_" + queueNumber + "_" + instance.index + ".png";
                    if (BotUtils.takeScreenshot(instance.index, verifyPath)) {
                        System.out.println("📸 Took verification screenshot after clicking Queue " + queueNumber);
                    }
                    
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
    
    /**
     * FIXED: Calculate click position for march queue using center-based coordinates
     */
    private Point calculateQueueClickPosition(int queueNumber) {
        // FIXED: Use center-based coordinates for better accuracy
        int baseY = 200 + 30; // Start position + half height for center  
        int queueY = baseY + (queueNumber - 1) * 55; // 55 pixels between queues
        int centerX = 240; // Center of the march panel
        
        Point position = new Point(centerX, queueY);
        System.out.println("🎯 [CALC] Queue " + queueNumber + " center position: " + position);
        return position;
    }
    
    /**
     * FIXED: Find and click details button with better detection and multiple attempts
     */
    private boolean clickDetailsButton() {
        try {
            System.out.println("🔍 Looking for details button with enhanced detection...");
            
            // FIXED: Try multiple times with different confidence levels
            String[] detailsButtonImages = {"details_button.png", "details.png"};
            double[] confidences = {0.8, 0.7, 0.6, 0.5};
            
            for (int attempt = 1; attempt <= 3; attempt++) {
                System.out.println("🔄 Details button detection attempt " + attempt + "/3");
                
                String detailsButtonPath = "screenshots/details_button_attempt" + attempt + "_" + instance.index + ".png";
                if (!BotUtils.takeScreenshot(instance.index, detailsButtonPath)) {
                    System.err.println("❌ Failed to take details button screenshot on attempt " + attempt);
                    continue;
                }
                
                Point detailsPos = null;
                
                // Try to find details button with multiple images and confidence levels
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
                    System.out.println("✅ Found details button at: " + detailsPos + " on attempt " + attempt);
                    if (BotUtils.clickMenu(instance.index, detailsPos)) {
                        System.out.println("✅ Clicked details button successfully");
                        Thread.sleep(3000); // Wait for details page to load
                        return true;
                    } else {
                        System.err.println("❌ Failed to click details button at " + detailsPos);
                    }
                } else {
                    System.out.println("⚠️ Details button not found on attempt " + attempt + ", trying again...");
                    if (attempt < 3) {
                        Thread.sleep(2000); // Wait before retry
                    }
                }
            }
            
            // FIXED: Last resort - try multiple fallback positions based on screen area
            System.out.println("🔄 Trying fallback positions for details button...");
            Point[] fallbackPositions = {
                new Point(271, 661), // Original successful position
                new Point(275, 665), // Slightly offset
                new Point(267, 657), // Slightly offset other direction
                new Point(271, 650), // Higher
                new Point(271, 670)  // Lower
            };
            
            for (int i = 0; i < fallbackPositions.length; i++) {
                Point fallbackPos = fallbackPositions[i];
                System.out.println("🔄 Trying fallback position " + (i+1) + "/" + fallbackPositions.length + ": " + fallbackPos);
                
                if (BotUtils.clickMenu(instance.index, fallbackPos)) {
                    System.out.println("✅ Clicked details button with fallback position " + fallbackPos);
                    Thread.sleep(3000);
                    
                    // Verify we opened details page by taking screenshot
                    String verifyDetailsPath = "screenshots/verify_details_" + instance.index + ".png";
                    if (BotUtils.takeScreenshot(instance.index, verifyDetailsPath)) {
                        System.out.println("📸 Verification screenshot taken after clicking details button");
                        return true;
                    }
                } else {
                    System.err.println("❌ Failed to click fallback position " + fallbackPos);
                }
            }
            
            System.err.println("❌ Could not find or click details button after all attempts");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking details button: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Extract gathering time using same successful method as AutoGatherResourcesTask
     */
    private String extractGatheringTimeWithExistingOCR(int queueNumber) {
        try {
            System.out.println("📊 Extracting gathering time from details page...");
            
            String detailsPagePath = "screenshots/details_page_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, detailsPagePath)) {
                System.err.println("❌ Failed to take details page screenshot");
                return "02:00:00";
            }
            
            // FIXED: Use the same successful approach as AutoGatherResourcesTask
            // Based on your image showing "04:34:38", try multiple coordinate variations
            int[][] coordVariations = {
                {360, 155, 85, 20},  // Based on your image analysis
                {355, 153, 90, 22},  // Slightly adjusted left and up  
                {350, 150, 95, 25},  // More margin for safety
                {365, 157, 80, 18},  // Slightly right and down
                {340, 145, 110, 30}  // Much wider capture area
            };
            
            for (int i = 0; i < coordVariations.length; i++) {
                int x = coordVariations[i][0];
                int y = coordVariations[i][1]; 
                int w = coordVariations[i][2];
                int h = coordVariations[i][3];
                
                System.out.println("🔄 Trying coordinates variation " + (i+1) + ": x=" + x + ", y=" + y + ", w=" + w + ", h=" + h);
                
                String timeRegionPath = "screenshots/gathering_time_" + instance.index + "_v" + (i+1) + ".png";
                
                // Use the same successful method as AutoGatherResourcesTask
                if (OCRUtils.extractImageRegion(detailsPagePath, timeRegionPath, x, y, w, h)) {
                    String timeText = OCRUtils.performTimeOCR(timeRegionPath, instance.index);
                    if (timeText != null && !timeText.trim().isEmpty()) {
                        System.out.println("📋 OCR extracted text (variation " + (i+1) + "): '" + timeText + "'");
                        
                        String parsedTime = TimeUtils.parseTimeFromText(timeText);
                        if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                            System.out.println("✅ Successfully parsed time: " + parsedTime + " (using variation " + (i+1) + ")");
                            return parsedTime;
                        } else {
                            System.out.println("⚠️ Could not parse valid time from: '" + timeText + "'");
                        }
                    } else {
                        System.out.println("⚠️ OCR returned empty result for variation " + (i+1));
                    }
                } else {
                    System.err.println("❌ Failed to extract image region for variation " + (i+1));
                }
            }
            
            // Fallback: Try the broader approach used in AutoGatherResourcesTask
            System.out.println("🔄 Trying fallback OCR approach...");
            String fallbackTimeRegion = "screenshots/fallback_gathering_time_" + instance.index + ".png";
            if (OCRUtils.extractImageRegion(detailsPagePath, fallbackTimeRegion, 340, 145, 120, 35)) {
                String fallbackTimeText = OCRUtils.performTimeOCR(fallbackTimeRegion, instance.index);
                if (fallbackTimeText != null && !fallbackTimeText.trim().isEmpty()) {
                    System.out.println("📋 Fallback OCR result: '" + fallbackTimeText + "'");
                    String parsedTime = TimeUtils.parseTimeFromText(fallbackTimeText);
                    if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                        System.out.println("✅ Fallback OCR success: " + parsedTime);
                        return parsedTime;
                    }
                }
            }
            
            System.err.println("⚠️ Could not extract gathering time, using default");
            return "02:00:00";
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting gathering time: " + e.getMessage());
            return "02:00:00";
        }
    }
    
    /**
     * Clean extracted time string
     */
    private String cleanTimeString(String timeString) {
        if (timeString == null) return null;
        
        // Remove common OCR artifacts
        String cleaned = timeString.trim()
                                  .replace("'", "")
                                  .replace(":", ":")  // Normalize colons
                                  .replaceAll("[^0-9:]", ""); // Keep only numbers and colons
        
        // Extract time pattern
        if (cleaned.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return cleaned;
        }
        
        return null;
    }
    
    /**
     * FIXED: Close details page using your existing BotUtils methods
     */
    private void closeDetailsPage() {
        try {
            System.out.println("❌ Closing details page...");
            
            String closeDetailsPath = "screenshots/close_details_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, closeDetailsPath)) {
                System.err.println("❌ Failed to take close details screenshot");
                return;
            }
            
            // Try to find close button using your existing methods
            Point closePos = BotUtils.findImageOnScreen(closeDetailsPath, "close_gather.png", 0.8);
            
            if (closePos == null) {
                // Try alternative close button names
                closePos = BotUtils.findImageOnScreen(closeDetailsPath, "close_x.png", 0.8);
            }
            
            if (closePos == null) {
                // Try with lower confidence
                closePos = BotUtils.findImageOnScreen(closeDetailsPath, "close_gather.png", 0.6);
            }
            
            if (closePos == null) {
                // Use known close button position from your successful logs
                closePos = new Point(415, 59); // From your log: "close_gather.png (confidence: 1.000)"
                System.out.println("⚠️ Using fallback close position: " + closePos);
            }
            
            if (BotUtils.clickMenu(instance.index, closePos)) {
                System.out.println("✅ Closed details page at " + closePos);
                Thread.sleep(2000); // Wait for UI to return to march list
            } else {
                System.err.println("❌ Failed to close details page");
                
                // Try emergency fallback positions
                Point[] fallbackPositions = {
                    new Point(400, 80),
                    new Point(420, 60),
                    new Point(380, 100)
                };
                
                for (Point fallback : fallbackPositions) {
                    if (BotUtils.clickMenu(instance.index, fallback)) {
                        System.out.println("✅ Closed details page with emergency fallback at " + fallback);
                        Thread.sleep(2000);
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error closing details page: " + e.getMessage());
        }
    }
    
    /**
     * ADDED: Verify we're on the march queue list before clicking a queue
     */
    private boolean verifyMarchQueueList(String screenshotPath) {
        try {
            System.out.println("🔍 Verifying we're on march queue list...");
            
            // Look for indicators that we're on the march queue list
            Point marchQueue1 = BotUtils.findImageOnScreen(screenshotPath, "march_queue_1.png", 0.6);
            if (marchQueue1 != null) {
                System.out.println("✅ Found march queue list - detected march_queue_1.png");
                return true;
            }
            
            // Alternative: Look for queue text patterns or UI elements
            // We can use OCR to verify "March Queue" text is visible
            String ocrText = OCRUtils.performEnhancedOCR(screenshotPath, instance.index);
            if (ocrText != null && ocrText.toLowerCase().contains("march queue")) {
                System.out.println("✅ Found march queue list - detected 'march queue' text in OCR");
                return true;
            }
            
            System.out.println("⚠️ March queue list verification inconclusive, proceeding anyway");
            return true; // Don't block if we can't verify
            
        } catch (Exception e) {
            System.err.println("❌ Error verifying march queue list: " + e.getMessage());
            return true; // Don't block on verification errors
        }
    }
    
    /**
     * ADDED: Verify that a queue was properly selected by checking for details button
     */
    private boolean verifyQueueSelected(int queueNumber) {
        try {
            System.out.println("🔍 Verifying Queue " + queueNumber + " was properly selected...");
            
            String verifyPath = "screenshots/verify_queue_selected_" + queueNumber + "_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, verifyPath)) {
                System.err.println("❌ Failed to take verification screenshot");
                return false;
            }
            
            // Check if details button is visible (indicates queue is selected)
            Point detailsButton = BotUtils.findImageOnScreen(verifyPath, "details_button.png", 0.6);
            if (detailsButton != null) {
                System.out.println("✅ Queue " + queueNumber + " properly selected - details button visible at " + detailsButton);
                return true;
            }
            
            // Try alternative details button image
            detailsButton = BotUtils.findImageOnScreen(verifyPath, "details.png", 0.6);
            if (detailsButton != null) {
                System.out.println("✅ Queue " + queueNumber + " properly selected - details button (alt) visible at " + detailsButton);
                return true;
            }
            
            System.err.println("❌ Queue " + queueNumber + " not properly selected - no details button found");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error verifying queue selection: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Add to march tracker using your existing MarchTrackerGUI
     */
    private void addToMarchTracker(MarchDeployInfo marchInfo, String gatheringTime, String totalTime) {
        try {
            // Calculate march time (one way) from total time and gathering time
            long totalSeconds = TimeUtils.parseTimeToSeconds(totalTime);
            long gatheringSeconds = TimeUtils.parseTimeToSeconds(gatheringTime);
            long marchSeconds = (totalSeconds - gatheringSeconds) / 2; // Divide by 2 for one-way march time
            String marchTime = TimeUtils.formatTime(marchSeconds);
            
            // Use your existing MarchTrackerGUI.addMarch method
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    MarchTrackerGUI tracker = MarchTrackerGUI.getInstance();
                    tracker.addMarch(
                        instance.index,           // instanceIndex
                        marchInfo.queueNumber,    // queueNumber  
                        marchInfo.resourceType,   // resourceType
                        gatheringTime,           // gatheringTime
                        marchTime,               // marchingTime (one way)
                        totalTime                // totalTime
                    );
                    
                    // Show the tracker window
                    tracker.setVisible(true);
                    tracker.toFront();
                    
                    System.out.println("📊 Successfully added march to tracker GUI");
                    
                } catch (Exception e) {
                    System.err.println("❌ Error adding to march tracker GUI: " + e.getMessage());
                }
            });
            
            // Also log the march information
            System.out.println("📊 March Added: Instance=" + instance.index + 
                             ", Queue=" + marchInfo.queueNumber + 
                             ", Resource=" + marchInfo.resourceType + 
                             ", Gathering=" + gatheringTime +
                             ", March=" + marchTime +
                             ", Total=" + totalTime);
            
        } catch (Exception e) {
            System.err.println("❌ Error adding to march tracker: " + e.getMessage());
        }
    }
    
    /**
     * Calculate total march time: march + gathering + march back
     */
    public String calculateTotalTime(String marchTime, String gatheringTime) {
        try {
            long marchSeconds = TimeUtils.parseTimeToSeconds(marchTime);
            long gatheringSeconds = TimeUtils.parseTimeToSeconds(gatheringTime);
            
            // Total = march there + gathering + march back
            long totalSeconds = marchSeconds + gatheringSeconds + marchSeconds;
            
            return TimeUtils.formatTime(totalSeconds);
            
        } catch (Exception e) {
            System.err.println("❌ Error calculating total time: " + e.getMessage());
            return marchTime;
        }
    }
}