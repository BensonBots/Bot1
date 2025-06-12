package newgame;

import javax.swing.SwingWorker;
import java.awt.AWTException;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoGatherResourcesTask extends SwingWorker<Void, String> {
    private final MemuInstance instance;
    private volatile boolean shouldStop = false;
    private AutoGatherModule.AutoGatherSettings gatherSettings;
    
    // Add field to store extracted time
    private String lastExtractedTime = "02:30:00";

    public AutoGatherResourcesTask(MemuInstance instance) throws AWTException {
        this.instance = instance;
        loadGatherSettings();
    }
    
    private void loadGatherSettings() {
        Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new java.util.HashMap<>());
        ModuleState<?> gatherModule = modules.get("Auto Gather Resources");
        
        if (gatherModule != null && gatherModule.settings != null) {
            gatherSettings = AutoGatherModule.AutoGatherSettings.fromString(gatherModule.settings.toString());
            System.out.println("✅ Loaded Auto Gather settings for instance " + instance.index + ": " + gatherSettings.toString());
        } else {
            gatherSettings = new AutoGatherModule.AutoGatherSettings();
            System.out.println("⚠️ No Auto Gather settings found, using defaults for instance " + instance.index);
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        try {
            instance.setAutoGatherRunning(true);
            instance.setState("Starting auto gather...");
            
            Main.addToConsole("🔄 Auto Gather started for " + instance.name);
            System.out.println("🔄 Starting AutoGatherResourcesTask for instance " + instance.index);
            System.out.println("📋 Using settings: " + gatherSettings.toString());
            
            while (!shouldStop && !isCancelled()) {
                try {
                    publish("📋 Checking march queues...");
                    
                    // Step 1: Open left panel and setup march view
                    if (!setupMarchView()) {
                        publish("❌ Failed to setup march view, retrying in 30s...");
                        Thread.sleep(30000);
                        continue;
                    }
                    
                    // Step 2: Read queue statuses
                    List<MarchDetector.MarchInfo> queues = MarchDetector.readMarchQueues(instance.index);
                    if (queues.isEmpty()) {
                        publish("⚠️ No queues detected, retrying in 30s...");
                        Thread.sleep(30000);
                        continue;
                    }
                    
                    // Step 3: Find idle queues within our limit
                    List<Integer> idleQueues = findIdleQueues(queues);
                    int activeQueues = countActiveQueues(queues);
                    
                    String status = String.format("%d active, %d idle", activeQueues, idleQueues.size());
                    Main.addToConsole("📊 " + instance.name + " queues: " + status);
                    publish("📊 " + status);
                    
                    // Step 4: Start marches if we have idle queues and haven't reached limit
                    if (!idleQueues.isEmpty() && activeQueues < gatherSettings.maxQueues) {
                        startMarchesSequentially(idleQueues, activeQueues);
                        
                        Main.addToConsole("⏳ " + instance.name + " waiting 3 minutes for marches to complete cycle...");
                        Thread.sleep(180000);
                    } else {
                        // No action needed, wait and check again
                        Thread.sleep(30000);
                    }
                    
                } catch (InterruptedException e) {
                    Main.addToConsole("🛑 " + instance.name + " auto gather interrupted");
                    break;
                } catch (Exception e) {
                    System.err.println("Error in gather loop: " + e.getMessage());
                    publish("❌ Error: " + e.getMessage());
                    Thread.sleep(20000);
                }
            }
            
        } finally {
            instance.setAutoGatherRunning(false);
            instance.setState("Auto gather stopped");
            Main.addToConsole("🛑 " + instance.name + " auto gather stopped");
        }
        
        return null;
    }
    
    private boolean setupMarchView() {
        try {
            System.out.println("🔧 Setting up march view for instance " + instance.index);
            
            publish("🔍 Opening left march panel...");
            if (!clickOpenLeft()) {
                System.err.println("❌ Failed to open left panel");
                return false;
            }
            
            publish("🏔️ Clicking wilderness button...");
            if (!clickWildernessButton()) {
                System.err.println("❌ Failed to click wilderness button");
                return false;
            }
            
            System.out.println("✅ March view setup complete");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error setting up march view: " + e.getMessage());
            return false;
        }
    }
    
    private boolean clickOpenLeft() {
        try {
            String screenPath = "screenshots/open_left_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for opening left panel");
                return false;
            }
            
            Point openLeftButton = BotUtils.findImageOnScreen(screenPath, "open_left.png", 0.6);
            if (openLeftButton != null) {
                if (BotUtils.clickMenu(instance.index, openLeftButton)) {
                    System.out.println("✅ Clicked open left panel button");
                    Thread.sleep(2000);
                    return true;
                }
            }
            
            System.err.println("❌ Could not find or click open_left.png");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking open left: " + e.getMessage());
            return false;
        }
    }
    
    private boolean clickWildernessButton() {
        try {
            String screenPath = "screenshots/wilderness_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for wilderness button");
                return false;
            }
            
            Point wildernessButton = BotUtils.findImageOnScreen(screenPath, "wilderness_button.png", 0.6);
            if (wildernessButton != null) {
                if (BotUtils.clickMenu(instance.index, wildernessButton)) {
                    System.out.println("✅ Clicked wilderness button");
                    Thread.sleep(3000);
                    return true;
                }
            }
            
            System.err.println("❌ Could not find or click wilderness_button.png");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking wilderness button: " + e.getMessage());
            return false;
        }
    }
    
    private void startMarchesSequentially(List<Integer> idleQueues, int currentActive) {
        try {
            int slotsAvailable = gatherSettings.maxQueues - currentActive;
            int marchesToStart = Math.min(slotsAvailable, idleQueues.size());
            
            Main.addToConsole("🚀 " + instance.name + " starting " + marchesToStart + " march(es) SEQUENTIALLY");
            publish("🚀 Starting " + marchesToStart + " march(es) sequentially");
            
            int successCount = 0;
            boolean isFirstMarch = true;
            
            for (int i = 0; i < marchesToStart; i++) {
                String resourceType = gatherSettings.getNextResource();
                int queueNumber = idleQueues.get(i);
                
                Main.addToConsole("📋 " + instance.name + " starting " + resourceType + " on Queue " + queueNumber + " (" + (i+1) + "/" + marchesToStart + ")");
                publish("📋 Starting " + resourceType + " on Queue " + queueNumber + " (" + (i+1) + "/" + marchesToStart + ")");
                
                boolean marchSuccess;
                
                if (isFirstMarch) {
                    // First march uses full sequence including world view checks
                    marchSuccess = startFirstMarch(resourceType, queueNumber);
                    isFirstMarch = false;
                } else {
                    // Subsequent marches use simplified sequence (no icon checks)
                    marchSuccess = startSubsequentMarchSimplified(resourceType, queueNumber);
                }
                
                if (marchSuccess) {
                    successCount++;
                    Main.addToConsole("✅ " + instance.name + " successfully started " + resourceType + " on Queue " + queueNumber);
                    
                    registerMarchWithTracker(resourceType, queueNumber);
                    
                    if (i < marchesToStart - 1) {
                        Thread.sleep(2000); // Wait between marches
                    }
                } else {
                    Main.addToConsole("❌ " + instance.name + " failed to start " + resourceType + " on Queue " + queueNumber);
                    // Reset to first march logic if subsequent march fails
                    isFirstMarch = true;
                }
            }
            
            if (successCount > 0) {
                Main.addToConsole("🎉 " + instance.name + " sequential deployment complete: " + successCount + "/" + marchesToStart + " successful");
                
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        MarchTrackerGUI.showTracker();
                    } catch (Exception e) {
                        System.err.println("Error showing march tracker: " + e.getMessage());
                    }
                });
            } else {
                Main.addToConsole("❌ " + instance.name + " no marches started successfully");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in sequential march starting: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " sequential march error: " + e.getMessage());
        }
    }
    
    /**
     * FIXED: First march with CORRECTED view logic
     */
    private boolean startFirstMarch(String resourceType, int queueNumber) {
        try {
            System.out.println("🚀 Starting FIRST march for " + resourceType + " on queue " + queueNumber);
            
            // CRITICAL: First verify we're in world view by checking for town_icon
            if (!verifyWorldViewWithTownIcon()) {
                System.err.println("❌ Failed to verify world view - cannot proceed with first march");
                return false;
            }
            
            if (!clickSearchIconSimplified()) {
                System.err.println("❌ Failed to click search icon");
                return false;
            }
            
            if (!scrollToRevealResources()) {
                System.err.println("❌ Failed to scroll to reveal resources");
                return false;
            }
            
            if (!selectResourceIcon(resourceType)) {
                System.err.println("❌ Failed to select " + resourceType + " icon");
                return false;
            }
            
            if (!setMaxLevel()) {
                System.err.println("❌ Failed to set max level");
                return false;
            }
            
            if (!searchForAvailableResource()) {
                System.err.println("❌ Failed to find available resource at any level");
                return false;
            }
            
            if (!deployMarchWithTimeExtraction()) {
                System.err.println("❌ Failed to deploy march");
                return false;
            }
            
            System.out.println("✅ Successfully started FIRST " + resourceType + " march on queue " + queueNumber);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error starting first march: " + e.getMessage());
            return false;
        }
    }
    
    private boolean verifyWorldViewWithTownIcon() {
        try {
            System.out.println("🌍 Verifying world view by checking for town_icon...");
            
            String screenPath = "screenshots/verify_world_view_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for world view verification");
                return false;
            }
            
            Point townIcon = BotUtils.findImageOnScreen(screenPath, "town_icon.png", 0.6);
            if (townIcon != null) {
                System.out.println("✅ Verified world view - found town_icon at " + townIcon);
                return true;
            }
            
            System.err.println("❌ Could not find town_icon - not in world view");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error verifying world view: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FIXED: Subsequent march - COMPLETELY skip all view checks, go directly to search
     */
    private boolean startSubsequentMarchSimplified(String resourceType, int queueNumber) {
        try {
            System.out.println("🔄 Starting SUBSEQUENT march for " + resourceType + " on queue " + queueNumber + " (ultra-simplified)");
            System.out.println("✅ After deploying previous march, we're on world view - NO view checks needed");
            
            // CRITICAL FIX: Absolutely NO view checking or icon clicking
            // We are GUARANTEED to be on world view after deploying a march
            // Skip ALL of: ensureWorldView(), town_icon detection, world_icon clicking
            
            // Go DIRECTLY to search - this is the ONLY step needed
            System.out.println("🔍 Going DIRECTLY to search - no UI navigation needed");
            
            if (!clickSearchIconSimplified()) {
                System.err.println("❌ Failed to click search icon for subsequent march");
                return false;
            }
            
            // Continue with resource selection (same as before)
            if (!scrollToRevealResources()) {
                System.err.println("❌ Failed to scroll to reveal resources");
                return false;
            }
            
            if (!selectResourceIcon(resourceType)) {
                System.err.println("❌ Failed to select " + resourceType + " icon");
                return false;
            }
            
            if (!setMaxLevel()) {
                System.err.println("❌ Failed to set max level");
                return false;
            }
            
            if (!searchForAvailableResource()) {
                System.err.println("❌ Failed to find available resource at any level");
                return false;
            }
            
            if (!deployMarchWithTimeExtraction()) {
                System.err.println("❌ Failed to deploy march");
                return false;
            }
            
            System.out.println("✅ Successfully started SUBSEQUENT " + resourceType + " march on queue " + queueNumber);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error in ultra-simplified subsequent march: " + e.getMessage());
            return false;
        }
    }
    
    private boolean ensureWorldView() {
        try {
            System.out.println("🌍 Ensuring world view...");
            
            String screenPath = "screenshots/world_view_check_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for world view check");
                return false;
            }
            
            // First check if we're already in world view (town_icon visible)
            Point townIcon = BotUtils.findImageOnScreen(screenPath, "town_icon.png", 0.6);
            if (townIcon != null) {
                System.out.println("✅ Already in world view (town_icon found)");
                return true;
            }
            
            // If not in world view, check if we're in town view (world_icon visible)
            Point worldIcon = BotUtils.findImageOnScreen(screenPath, "world_icon.png", 0.6);
            if (worldIcon != null) {
                System.out.println("🏘️ Found world_icon - currently in town view, switching to world view");
                if (BotUtils.clickMenu(instance.index, worldIcon)) {
                    Thread.sleep(3000);
                    
                    // Verify we successfully switched to world view
                    if (!verifyWorldViewWithTownIcon()) {
                        System.err.println("❌ Failed to switch to world view");
                        return false;
                    }
                    return true;
                }
            }
            
            System.err.println("❌ Could not determine current view state");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error ensuring world view: " + e.getMessage());
            return false;
        }
    }
    /**
     * SIMPLIFIED: Click search icon using known working position - NO icon detection
     */
    private boolean clickSearchIconSimplified() {
        try {
            System.out.println("🔍 Clicking search button (simplified - using known position)...");
            
            // Use the known working position from successful logs: (31, 535)
            Point searchButton = new Point(31, 535);
            System.out.println("✅ Using known working search button position: " + searchButton);
            
            if (BotUtils.clickMenu(instance.index, searchButton)) {
                System.out.println("✅ Successfully clicked search button at " + searchButton);
                Thread.sleep(3000);
                
                if (!dismissSearchPopup()) {
                    System.err.println("❌ Failed to dismiss search popup");
                    return false;
                }
                
                // Verify we're on the correct resource selection screen
                if (!verifyResourceSelectionScreen()) {
                    System.err.println("❌ Not on resource selection screen after search click");
                    return false;
                }
                
                return true;
            } else {
                System.err.println("❌ Failed to click search button at known position");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in simplified search click: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * NEW: Verify we're on the correct resource selection screen
     */
    private boolean verifyResourceSelectionScreen() {
        try {
            System.out.println("🔍 Verifying we're on resource selection screen...");
            
            String verifyPath = "screenshots/verify_resource_screen_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, verifyPath)) {
                return false;
            }
            
            // Look for indicators that we're on the resource selection screen
            // Check for plus_button (level selector)
            Point plusButton = BotUtils.findImageOnScreen(verifyPath, "plus_button.png", 0.5);
            if (plusButton != null) {
                System.out.println("✅ Found plus_button - we're on resource selection screen");
                return true;
            }
            
            // Check for minus_button
            Point minusButton = BotUtils.findImageOnScreen(verifyPath, "minus_button.png", 0.5);
            if (minusButton != null) {
                System.out.println("✅ Found minus_button - we're on resource selection screen");
                return true;
            }
            
            // Check for resource icons at expected positions
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
    
    private boolean dismissSearchPopup() {
        try {
            System.out.println("🚫 Dismissing search popup by clicking bottom left area...");
            
            String popupPath = "screenshots/search_popup_dismiss_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, popupPath)) {
                System.err.println("Failed to take screenshot for popup dismissal");
                return false;
            }
            
            Point dismissArea = new Point(50, 750);
            if (BotUtils.clickMenu(instance.index, dismissArea)) {
                System.out.println("✅ Clicked bottom left area to dismiss popup at " + dismissArea);
                Thread.sleep(2000);
                
                String verifyPath = "screenshots/verify_popup_closed_" + instance.index + ".png";
                if (BotUtils.takeScreenshot(instance.index, verifyPath)) {
                    Point breadIcon = BotUtils.findImageOnScreen(verifyPath, "bread_icon.png", 0.3);
                    Point woodIcon = BotUtils.findImageOnScreen(verifyPath, "wood_icon.png", 0.3);
                    
                    if (breadIcon != null || woodIcon != null) {
                        System.out.println("✅ Popup dismissed successfully - resource icons now visible");
                        return true;
                    } else {
                        System.out.println("⚠️ Popup may not be fully dismissed, but proceeding...");
                        return true;
                    }
                }
                
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
    
    private boolean scrollToRevealResources() {
        try {
            System.out.println("📜 Step 6: Scrolling to reveal all resources...");
            
            Point startPoint = new Point(400, 570);
            Point endPoint = new Point(80, 570);
            
            if (BotUtils.performADBSwipe(instance.index, startPoint, endPoint)) {
                System.out.println("✅ Step 6: Successfully scrolled from " + startPoint + " to " + endPoint);
                Thread.sleep(2000);
                return true;
            } else {
                System.err.println("❌ Step 6: ADB swipe failed");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Step 6 error: " + e.getMessage());
            return false;
        }
    }
    
    private boolean selectResourceIcon(String resourceType) {
        try {
            System.out.println("🎯 Step 7: Selecting " + resourceType + " icon...");
            
            String screenPath = "screenshots/step7_select_resource_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for step 7");
                return false;
            }
            
            String iconFile = getResourceIconFile(resourceType);
            if (iconFile == null) {
                System.err.println("❌ Step 7: Unknown resource type: " + resourceType);
                return false;
            }
            
            double[] confidences = {0.8, 0.7, 0.6, 0.5, 0.4};
            Point resourceIcon = null;
            
            for (double confidence : confidences) {
                resourceIcon = BotUtils.findImageOnScreen(screenPath, iconFile, confidence);
                if (resourceIcon != null) {
                    System.out.println("✅ Step 7: Found " + resourceType + " icon (" + iconFile + ") at " + resourceIcon + " with confidence " + confidence);
                    break;
                }
            }
            
            if (resourceIcon != null) {
                Point clickPoint = new Point(resourceIcon.x + 40, resourceIcon.y + 35);
                if (BotUtils.clickMenu(instance.index, clickPoint)) {
                    System.out.println("✅ Step 7: Successfully clicked " + resourceType + " icon at " + clickPoint);
                    Thread.sleep(3000); // Wait for resource selection to load
                    
                    // Verify we're now on the level selection screen
                    if (!verifyLevelSelectionScreen()) {
                        System.err.println("❌ Not on level selection screen after clicking resource");
                        return false;
                    }
                    
                    return true;
                }
            } else {
                System.err.println("❌ Step 7: Could not find " + resourceType + " icon (" + iconFile + ")");
                
                System.out.println("⚠️ Resource icon not found, trying to scroll to reveal it...");
                if (scrollToRevealResources()) {
                    Thread.sleep(1000);
                    String retryScreenPath = "screenshots/step7_retry_resource_" + instance.index + ".png";
                    if (BotUtils.takeScreenshot(instance.index, retryScreenPath)) {
                        for (double confidence : confidences) {
                            resourceIcon = BotUtils.findImageOnScreen(retryScreenPath, iconFile, confidence);
                            if (resourceIcon != null) {
                                System.out.println("✅ Found " + resourceType + " icon after scroll at " + resourceIcon);
                                Point clickPoint = new Point(resourceIcon.x + 40, resourceIcon.y + 35);
                                if (BotUtils.clickMenu(instance.index, clickPoint)) {
                                    System.out.println("✅ Successfully clicked " + resourceType + " icon after scroll");
                                    Thread.sleep(3000);
                                    
                                    if (!verifyLevelSelectionScreen()) {
                                        System.err.println("❌ Not on level selection screen after clicking resource");
                                        return false;
                                    }
                                    
                                    return true;
                                }
                                break;
                            }
                        }
                    }
                }
                
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Step 7 error: " + e.getMessage());
            return false;
        }
        
        return false;
    }
    
    /**
     * NEW: Verify we're on the level selection screen after clicking a resource
     */
    private boolean verifyLevelSelectionScreen() {
        try {
            System.out.println("🔍 Verifying we're on level selection screen...");
            
            String verifyPath = "screenshots/verify_level_screen_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, verifyPath)) {
                return false;
            }
            
            // Look for level selection indicators (plus and minus buttons)
            Point plusButton = BotUtils.findImageOnScreen(verifyPath, "plus_button.png", 0.5);
            Point minusButton = BotUtils.findImageOnScreen(verifyPath, "minus_button.png", 0.5);
            
            if (plusButton != null && minusButton != null) {
                System.out.println("✅ Found both plus and minus buttons - we're on level selection screen");
                return true;
            } else if (plusButton != null) {
                System.out.println("✅ Found plus button - we're on level selection screen");
                return true;
            } else {
                System.err.println("❌ Could not find level selection controls");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error verifying level selection screen: " + e.getMessage());
            return false;
        }
    }
    
    private String getResourceIconFile(String resourceType) {
        switch (resourceType.toLowerCase()) {
            case "food": return "bread_icon.png";
            case "wood": return "wood_icon.png";
            case "stone": return "stone_icon.png";
            case "iron": return "iron_icon.png";
            default: return null;
        }
    }
    
    private boolean setMaxLevel() {
        try {
            System.out.println("📈 Step 8: Setting resource level to maximum...");
            
            String levelPath = "screenshots/level_max_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, levelPath);
            
            Point plusButton = BotUtils.findImageOnScreen(levelPath, "plus_button.png", 0.7);
            if (plusButton != null) {
                for (int i = 0; i < 8; i++) {
                    BotUtils.clickMenu(instance.index, plusButton);
                    Thread.sleep(200);
                }
                System.out.println("✅ Set to maximum resource level (clicked plus 8 times)");
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
    
    private boolean searchForAvailableResource() {
        try {
            System.out.println("🔍 Steps 9-10: Searching for available resources...");
            
            for (int level = 8; level >= 1; level--) {
                System.out.println("🎯 Searching at level " + level + "...");
                
                if (!clickSearchResourceButton()) {
                    continue;
                }
                
                Thread.sleep(4000);
                
                if (checkForGatherButton()) {
                    System.out.println("✅ Found available resource at level " + level);
                    return true;
                }
                
                if (level > 1) {
                    System.out.println("❌ No resource at level " + level + ", reducing to " + (level - 1));
                    if (!clickMinusButton()) {
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
    
    private boolean clickSearchResourceButton() {
        try {
            String searchPath = "screenshots/searchrss_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, searchPath);
            
            Point searchButton = BotUtils.findImageOnScreen(searchPath, "searchrss_button.png", 0.7);
            if (searchButton == null) {
                searchButton = new Point(237, 789);
            }
            
            BotUtils.clickMenu(instance.index, searchButton);
            System.out.println("✅ Clicked search resource button");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking search resource button: " + e.getMessage());
            return false;
        }
    }
    
    private boolean checkForGatherButton() {
        try {
            String gatherCheckPath = "screenshots/gather_check_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, gatherCheckPath);
            
            Point gatherButton = BotUtils.findImageOnScreen(gatherCheckPath, "gather_button.png", 0.6);
            if (gatherButton != null) {
                System.out.println("✅ Found gather_button - resource available");
                BotUtils.clickMenu(instance.index, gatherButton);
                Thread.sleep(3000);
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
    
    private boolean clickMinusButton() {
        try {
            String minusPath = "screenshots/minus_level_" + instance.index + ".png";
            BotUtils.takeScreenshot(instance.index, minusPath);
            
            Point minusButton = BotUtils.findImageOnScreen(minusPath, "minus_button.png", 0.7);
            if (minusButton != null) {
                BotUtils.clickMenu(instance.index, minusButton);
                Thread.sleep(500);
                System.out.println("✅ Clicked minus button to reduce level");
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
    
    private boolean deployMarchWithTimeExtraction() {
        try {
            System.out.println("🚀 Step 11: Deploying march with precise time extraction...");
            
            String deployPath = "screenshots/deploy_screen_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, deployPath)) {
                System.err.println("Failed to take deploy screen screenshot");
                return false;
            }
            
            // FIXED: Extract time BEFORE clicking deploy button with corrected coordinates
            String extractedTime = extractTimeFromDeployScreen(deployPath);
            if (extractedTime != null) {
                System.out.println("⏱️ Successfully extracted march time: " + extractedTime);
            } else {
                System.out.println("⚠️ Could not extract time, using default: 02:30:00");
                extractedTime = "02:30:00";
            }
            
            // Store the extracted time for march tracker registration
            this.lastExtractedTime = extractedTime;
            
            // Now find and click deploy button
            Point deployButton = BotUtils.findImageOnScreen(deployPath, "deploy_button.png", 0.6);
            if (deployButton == null) {
                // Try alternative deploy button names
                deployButton = BotUtils.findImageOnScreen(deployPath, "deploy.png", 0.6);
            }
            
            if (deployButton != null) {
                System.out.println("✅ Found deploy button at: " + deployButton);
                
                if (BotUtils.clickMenu(instance.index, deployButton)) {
                    System.out.println("✅ Clicked deploy button successfully");
                    Thread.sleep(2000);
                    
                    // Click any confirmation dialog that might appear
                    Point confirmPoint = new Point(400, 750);
                    BotUtils.clickMenu(instance.index, confirmPoint);
                    Thread.sleep(2000);
                    
                    System.out.println("🎉 March deployed successfully with time: " + extractedTime);
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
     * FIXED: Extract time from deploy screen with further adjusted coordinates
     */
    private String extractTimeFromDeployScreen(String screenPath) {
        try {
            System.out.println("⏱️ Extracting march time from deploy screen with refined coordinates...");
            
            String timeRegionPath = "screenshots/precise_time_" + instance.index + ".png";
            
            // FURTHER REFINED: The OCR is getting "90:02:32" instead of "00:02:32"
            // This suggests we're still catching part of an icon or other element
            // Moving further right and adjusting to capture only the time digits
            // Previous: x=345, y=712, w=60, h=18
            // New coordinates: x=350, y=713, w=55, h=16
            if (OCRUtils.extractImageRegion(screenPath, timeRegionPath, 350, 713, 55, 16)) {
                String timeText = OCRUtils.performTimeOCR(timeRegionPath, instance.index);
                if (timeText != null && !timeText.trim().isEmpty()) {
                    System.out.println("📋 OCR extracted text: '" + timeText + "'");
                    
                    String parsedTime = TimeUtils.parseTimeFromText(timeText);
                    if (parsedTime != null && TimeUtils.isValidMarchTime(parsedTime)) {
                        System.out.println("✅ Successfully parsed time: " + parsedTime + " from deploy screen");
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
    
    private void registerMarchWithTracker(String resourceType, int queueNumber) {
        try {
            // Use the extracted time instead of default
            String totalTime = lastExtractedTime != null ? lastExtractedTime : "02:30:00";
            String gatheringTime = calculateGatheringTime(totalTime);
            String marchingTime = "00:30:00"; // Estimate marching time
            
            MarchTrackerGUI.getInstance().addMarch(
                instance.index, 
                queueNumber, 
                resourceType, 
                gatheringTime,
                marchingTime,
                totalTime
            );
            System.out.println("📊 Registered march with tracker: " + resourceType + " on Queue " + queueNumber + " (Total time: " + totalTime + ")");
        } catch (Exception e) {
            System.err.println("❌ Error registering march with tracker: " + e.getMessage());
        }
    }
    
    /**
     * Calculate gathering time from total time (total - marching time)
     */
    private String calculateGatheringTime(String totalTime) {
        try {
            long totalSeconds = TimeUtils.parseTimeToSeconds(totalTime);
            long marchingSeconds = TimeUtils.parseTimeToSeconds("00:30:00"); // 30 minutes marching
            long gatheringSeconds = Math.max(0, totalSeconds - (marchingSeconds * 2)); // Round trip
            
            return TimeUtils.formatTime(gatheringSeconds);
        } catch (Exception e) {
            return "02:00:00"; // Default gathering time
        }
    }
    
    /**
     * FIXED: Better queue validation to handle OCR detection issues
     */
    private List<Integer> findIdleQueues(List<MarchDetector.MarchInfo> queues) {
        List<Integer> idleQueues = new ArrayList<>();
        
        System.out.println("🔍 Validating detected queues against expected status:");
        for (MarchDetector.MarchInfo queue : queues) {
            System.out.println("  Queue " + queue.queueNumber + ": " + queue.status);
            
            // Only add confirmed IDLE queues
            if (queue.status == MarchDetector.MarchStatus.IDLE) {
                idleQueues.add(queue.queueNumber);
                System.out.println("  ✅ Added Queue " + queue.queueNumber + " as available");
            } else {
                System.out.println("  ❌ Queue " + queue.queueNumber + " not available: " + queue.status);
            }
        }
        
        System.out.println("📊 Found " + idleQueues.size() + " idle queues: " + idleQueues);
        return idleQueues;
    }
    
    private int countActiveQueues(List<MarchDetector.MarchInfo> queues) {
        int count = 0;
        for (MarchDetector.MarchInfo queue : queues) {
            if (queue.status == MarchDetector.MarchStatus.GATHERING) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    protected void process(List<String> chunks) {
        if (!chunks.isEmpty()) {
            String latestMessage = chunks.get(chunks.size() - 1);
            instance.setState(latestMessage);
        }
    }
    
    @Override
    protected void done() {
        try {
            get();
            Main.addToConsole("✅ " + instance.name + " auto gather completed");
        } catch (Exception e) {
            Main.addToConsole("❌ " + instance.name + " auto gather failed");
        }
    }
    
    public void stopGathering() {
        shouldStop = true;
        cancel(true);
        Main.addToConsole("🛑 " + instance.name + " auto gather stop requested");
    }
}