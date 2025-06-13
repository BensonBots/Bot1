package newgame;

import java.awt.Point;

/**
 * SPEED OPTIMIZED: Handles the UI interactions for starting resource gathering marches
 * Much faster execution with reduced delays and optimized timing
 */
public class ResourceGatheringController {
    private final MemuInstance instance;
    private final MarchViewNavigator navigator;
    private String lastExtractedTime = "02:30:00";
    
    public ResourceGatheringController(MemuInstance instance) {
        this.instance = instance;
        this.navigator = new MarchViewNavigator(instance);
    }
    
    /**
     * SPEED OPTIMIZED: Start the first march with faster navigation sequence
     */
    public boolean startFirstMarchFast(String resourceType, int queueNumber) {
        try {
            System.out.println("🚀 Starting FIRST march for " + resourceType + " on queue " + queueNumber + " (FAST MODE)");
            
            System.out.println("📍 After march queue, we're in TOWN view - need to click world_icon");
            
            if (!navigator.clickWorldIconFast()) {
                System.err.println("❌ Failed to click world_icon to enter world view");
                return false;
            }
            
            if (!navigator.verifyWorldViewWithTownIconFast()) {
                System.err.println("❌ Failed to verify world view after clicking world_icon");
                return false;
            }
            
            return executeGatheringSequenceFast(resourceType, queueNumber);
            
        } catch (Exception e) {
            System.err.println("❌ Error starting first march: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Start subsequent march (already in world view)
     */
    public boolean startSubsequentMarchFast(String resourceType, int queueNumber) {
        try {
            System.out.println("🔄 Starting SUBSEQUENT march for " + resourceType + " on queue " + queueNumber + " (FAST MODE)");
            System.out.println("✅ We remain in WORLD view after previous deploy - no navigation needed");
            
            Thread.sleep(1500); // SPEED: 3s → 1.5s
            
            return executeGatheringSequenceFast(resourceType, queueNumber);
            
        } catch (Exception e) {
            System.err.println("❌ Error in subsequent march: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Execute the gathering sequence (search → select → deploy)
     */
    private boolean executeGatheringSequenceFast(String resourceType, int queueNumber) {
        try {
            if (!clickSearchIconSimplifiedFast()) {
                System.err.println("❌ Failed to click search icon");
                return false;
            }
            
            if (!scrollToRevealResourcesFast()) {
                System.err.println("❌ Failed to scroll to reveal resources");
                return false;
            }
            
            if (!selectResourceIconFast(resourceType)) {
                System.err.println("❌ Failed to select " + resourceType + " icon");
                return false;
            }
            
            if (!setMaxLevelFast()) {
                System.err.println("❌ Failed to set max level");
                return false;
            }
            
            if (!searchForAvailableResourceFast()) {
                System.err.println("❌ Failed to find available resource at any level");
                return false;
            }
            
            if (!deployMarchWithTimeExtractionFast()) {
                System.err.println("❌ Failed to deploy march");
                return false;
            }
            
            System.out.println("✅ Successfully started " + resourceType + " march on queue " + queueNumber + " (FAST MODE)");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error in fast gathering sequence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Click search icon using known position
     */
    private boolean clickSearchIconSimplifiedFast() {
        try {
            System.out.println("🔍 Clicking search button (FAST - using known position)...");
            
            Point searchButton = new Point(31, 535);
            System.out.println("✅ Using known working search button position: " + searchButton);
            
            if (BotUtils.clickMenu(instance.index, searchButton)) {
                System.out.println("✅ Successfully clicked search button at " + searchButton);
                Thread.sleep(1500); // SPEED: 3s → 1.5s
                
                if (!dismissSearchPopupFast()) {
                    System.err.println("❌ Failed to dismiss search popup");
                    return false;
                }
                
                if (!verifyResourceSelectionScreenFast()) {
                    System.err.println("❌ Not on resource selection screen after search click");
                    return false;
                }
                
                return true;
            } else {
                System.err.println("❌ Failed to click search button at known position");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in fast search click: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Dismiss the search popup
     */
    private boolean dismissSearchPopupFast() {
        try {
            System.out.println("🚫 Dismissing search popup by clicking bottom left area (FAST)...");
            
            Point dismissArea = new Point(50, 750);
            if (BotUtils.clickMenu(instance.index, dismissArea)) {
                System.out.println("✅ Clicked bottom left area to dismiss popup at " + dismissArea);
                Thread.sleep(1000); // SPEED: 2s → 1s
                return true;
            } else {
                System.err.println("❌ Failed to click bottom left area to dismiss popup");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error dismissing search popup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Verify we're on the resource selection screen
     */
    private boolean verifyResourceSelectionScreenFast() {
        try {
            System.out.println("🔍 Verifying we're on resource selection screen (FAST)...");
            
            String verifyPath = "screenshots/verify_resource_screen_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, verifyPath)) {
                return false;
            }
            
            Point plusButton = BotUtils.findImageOnScreen(verifyPath, "plus_button.png", 0.5);
            if (plusButton != null) {
                System.out.println("✅ Found plus_button - we're on resource selection screen");
                return true;
            }
            
            Point breadIcon = BotUtils.findImageOnScreen(verifyPath, "bread_icon.png", 0.4);
            Point woodIcon = BotUtils.findImageOnScreen(verifyPath, "wood_icon.png", 0.4);
            if (breadIcon != null || woodIcon != null) {
                System.out.println("✅ Found resource icons - we're on resource selection screen");
                return true;
            }
            
            System.err.println("❌ Could not verify resource selection screen");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error verifying resource selection screen: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Scroll to reveal all resource options
     */
    private boolean scrollToRevealResourcesFast() {
        try {
            System.out.println("📜 Scrolling to reveal all resources (FAST)...");
            
            Point startPoint = new Point(400, 570);
            Point endPoint = new Point(80, 570);
            
            if (BotUtils.performADBSwipe(instance.index, startPoint, endPoint)) {
                System.out.println("✅ Successfully scrolled from " + startPoint + " to " + endPoint);
                Thread.sleep(1000); // SPEED: 2s → 1s
                return true;
            } else {
                System.err.println("❌ ADB swipe failed");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Scroll error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Select the specified resource icon
     */
    private boolean selectResourceIconFast(String resourceType) {
        try {
            System.out.println("🎯 Selecting " + resourceType + " icon (FAST)...");
            
            String screenPath = "screenshots/select_resource_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for resource selection");
                return false;
            }
            
            String iconFile = getResourceIconFile(resourceType);
            if (iconFile == null) {
                System.err.println("❌ Unknown resource type: " + resourceType);
                return false;
            }
            
            // SPEED: Check fewer confidence levels, start with lower confidence
            double[] confidences = {0.6, 0.5, 0.4}; // SPEED: Reduced from 5 to 3 levels
            Point resourceIcon = null;
            
            for (double confidence : confidences) {
                resourceIcon = BotUtils.findImageOnScreen(screenPath, iconFile, confidence);
                if (resourceIcon != null) {
                    System.out.println("✅ Found " + resourceType + " icon (" + iconFile + ") at " + resourceIcon + " with confidence " + confidence);
                    break;
                }
            }
            
            if (resourceIcon != null) {
                Point clickPoint = new Point(resourceIcon.x + 40, resourceIcon.y + 35);
                if (BotUtils.clickMenu(instance.index, clickPoint)) {
                    System.out.println("✅ Successfully clicked " + resourceType + " icon at " + clickPoint);
                    Thread.sleep(1500); // SPEED: 3s → 1.5s
                    return true;
                }
            } else {
                System.err.println("❌ Could not find " + resourceType + " icon (" + iconFile + ")");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Resource selection error: " + e.getMessage());
            return false;
        }
        
        return false;
    }
    
    /**
     * Get the icon file name for a resource type
     */
    private String getResourceIconFile(String resourceType) {
        switch (resourceType.toLowerCase()) {
            case "food": return "bread_icon.png";
            case "wood": return "wood_icon.png";
            case "stone": return "stone_icon.png";
            case "iron": return "iron_icon.png";
            default: return null;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Set resource level to maximum
     */
    private boolean setMaxLevelFast() {
        try {
            System.out.println("📈 Setting resource level to maximum (FAST)...");
            
            String levelPath = "screenshots/level_max_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, levelPath);
            
            Point plusButton = BotUtils.findImageOnScreen(levelPath, "plus_button.png", 0.7);
            if (plusButton != null) {
                // SPEED: Faster clicking with shorter delays
                for (int i = 0; i < 8; i++) {
                    BotUtils.clickMenu(instance.index, plusButton);
                    Thread.sleep(100); // SPEED: 200ms → 100ms
                }
                System.out.println("✅ Set to maximum resource level (clicked plus 8 times, FAST)");
                return true;
            } else {
                System.err.println("❌ Could not find plus_button");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error setting max level: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Search for available resources at different levels
     */
    private boolean searchForAvailableResourceFast() {
        try {
            System.out.println("🔍 Searching for available resources (FAST)...");
            
            for (int level = 8; level >= 1; level--) {
                System.out.println("🎯 Searching at level " + level + " (FAST)...");
                
                if (!clickSearchResourceButtonFast()) {
                    continue;
                }
                
                Thread.sleep(2000); // SPEED: 4s → 2s
                
                if (checkForGatherButtonFast()) {
                    System.out.println("✅ Found available resource at level " + level);
                    return true;
                }
                
                if (level > 1) {
                    System.out.println("❌ No resource at level " + level + ", reducing to " + (level - 1));
                    if (!clickMinusButtonFast()) {
                        System.err.println("❌ Failed to reduce level");
                        return false;
                    }
                }
            }
            
            System.err.println("❌ No available resources found at any level (8 down to 1)");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error searching for available resource: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Click the search resource button
     */
    private boolean clickSearchResourceButtonFast() {
        try {
            String searchPath = "screenshots/searchrss_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, searchPath);
            
            Point searchButton = BotUtils.findImageOnScreen(searchPath, "searchrss_button.png", 0.7);
            if (searchButton == null) {
                searchButton = new Point(237, 789);
            }
            
            BotUtils.clickMenu(instance.index, searchButton);
            System.out.println("✅ Clicked search resource button (FAST)");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking search resource button: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Check for the gather button (indicates resource is available)
     */
    private boolean checkForGatherButtonFast() {
        try {
            String gatherCheckPath = "screenshots/gather_check_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, gatherCheckPath);
            
            Point gatherButton = BotUtils.findImageOnScreen(gatherCheckPath, "gather_button.png", 0.6);
            if (gatherButton != null) {
                System.out.println("✅ Found gather_button - resource available");
                BotUtils.clickMenu(instance.index, gatherButton);
                Thread.sleep(1500); // SPEED: 3s → 1.5s
                return true;
            } else {
                System.out.println("❌ No gather_button found - resource not available");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error checking for gather button: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Click the minus button to reduce level
     */
    private boolean clickMinusButtonFast() {
        try {
            String minusPath = "screenshots/minus_level_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, minusPath);
            
            Point minusButton = BotUtils.findImageOnScreen(minusPath, "minus_button.png", 0.7);
            if (minusButton != null) {
                BotUtils.clickMenu(instance.index, minusButton);
                Thread.sleep(300); // SPEED: 500ms → 300ms
                System.out.println("✅ Clicked minus button to reduce level (FAST)");
                return true;
            } else {
                System.err.println("❌ Could not find minus_button");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking minus button: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Deploy march and extract timing information
     */
    private boolean deployMarchWithTimeExtractionFast() {
        try {
            System.out.println("🚀 Deploying march with time extraction (FAST)...");
            
            String deployPath = "screenshots/deploy_screen_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, deployPath)) {
                System.err.println("Failed to take deploy screen screenshot");
                return false;
            }
            
            String extractedTime = extractTimeFromDeployScreenFast(deployPath);
            if (extractedTime != null) {
                System.out.println("⏱️ Successfully extracted march time: " + extractedTime);
            } else {
                System.out.println("⚠️ Could not extract time, using default: 02:30:00");
                extractedTime = "02:30:00";
            }
            
            this.lastExtractedTime = extractedTime;
            
            Point deployButton = BotUtils.findImageOnScreen(deployPath, "deploy_button.png", 0.6);
            if (deployButton == null) {
                deployButton = BotUtils.findImageOnScreen(deployPath, "deploy.png", 0.6);
            }
            
            if (deployButton != null) {
                System.out.println("✅ Found deploy button at: " + deployButton);
                
                if (BotUtils.clickMenu(instance.index, deployButton)) {
                    System.out.println("✅ Clicked deploy button successfully");
                    Thread.sleep(1500); // SPEED: 3s → 1.5s
                    
                    System.out.println("🎉 March deployed successfully with time: " + extractedTime + " (FAST MODE)");
                    System.out.println("📍 Remaining in world view after deploy");
                    return true;
                } else {
                    System.err.println("❌ Failed to click deploy button");
                    return false;
                }
            } else {
                System.err.println("❌ Could not find deploy_button.png");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error deploying march: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Extract time from deploy screen
     */
    private String extractTimeFromDeployScreenFast(String screenPath) {
        try {
            System.out.println("⏱️ Extracting march time from deploy screen (FAST)...");
            
            String timeRegionPath = "screenshots/precise_time_" + instance.index + ".png";
            
            // Extract time region with optimized coordinates
            if (OCRUtils.extractImageRegion(screenPath, timeRegionPath, 335, 713, 70, 16)) {
                String timeText = OCRUtils.performTimeOCR(timeRegionPath, instance.index);
                if (timeText != null && !timeText.trim().isEmpty()) {
                    System.out.println("📋 OCR extracted text: '" + timeText + "'");
                    
                    String parsedTime = TimeUtils.parseTimeFromText(timeText);
                    if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                        System.out.println("✅ Successfully parsed time: " + parsedTime + " from deploy screen (FAST)");
                        return parsedTime;
                    } else {
                        System.out.println("⚠️ Could not parse valid time from: '" + timeText + "'");
                    }
                }
            }
            
            System.out.println("⚠️ Could not extract time, using default");
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting time from deploy screen: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the last extracted time from deploy screen
     */
    public String getLastExtractedTime() {
        return lastExtractedTime;
    }
    
    // Legacy methods for compatibility - these just call the fast versions
    public boolean startFirstMarch(String resourceType, int queueNumber) {
        return startFirstMarchFast(resourceType, queueNumber);
    }
    
    public boolean startSubsequentMarch(String resourceType, int queueNumber) {
        return startSubsequentMarchFast(resourceType, queueNumber);
    }
}