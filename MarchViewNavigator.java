package newgame;

import java.awt.Point;

/**
 * SPEED OPTIMIZED: Handles navigation within the march view UI
 * Much faster execution with reduced delays and optimized timing
 */
public class MarchViewNavigator {
    private final MemuInstance instance;
    
    public MarchViewNavigator(MemuInstance instance) {
        this.instance = instance;
    }
    
    /**
     * SPEED OPTIMIZED: Setup march view by opening left panel and clicking wilderness
     */
    public boolean setupMarchView() {
        try {
            System.out.println("🔧 Setting up march view for instance " + instance.index + " (FAST MODE)");
            
            if (!clickOpenLeftFast()) {
                System.err.println("❌ Failed to open left panel");
                return false;
            }
            
            if (!clickWildernessButtonFast()) {
                System.err.println("❌ Failed to click wilderness button");
                return false;
            }
            
            System.out.println("✅ March view setup complete (FAST)");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error setting up march view: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Click the open left panel button
     */
    private boolean clickOpenLeftFast() {
        try {
            String screenPath = "screenshots/open_left_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for opening left panel");
                return false;
            }
            
            Point openLeftButton = BotUtils.findImageOnScreen(screenPath, "open_left.png", 0.6);
            if (openLeftButton != null) {
                if (BotUtils.clickMenu(instance.index, openLeftButton)) {
                    System.out.println("✅ Clicked open left panel button (FAST)");
                    Thread.sleep(1000); // SPEED: 2s → 1s
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
    
    /**
     * SPEED OPTIMIZED: Click the wilderness button to enter march mode
     */
    private boolean clickWildernessButtonFast() {
        try {
            System.out.println("🌍 Clicking wilderness button to enter march mode (FAST)...");
            
            String screenPath = "screenshots/wilderness_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for wilderness button");
                return false;
            }
            
            Point wildernessButton = BotUtils.findImageOnScreen(screenPath, "wilderness_button.png", 0.6);
            if (wildernessButton != null) {
                if (BotUtils.clickMenu(instance.index, wildernessButton)) {
                    System.out.println("✅ Clicked wilderness button (FAST)");
                    Thread.sleep(2000); // SPEED: 3s → 2s
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
    
    /**
     * SPEED OPTIMIZED: Click world icon to enter world view from town view
     */
    public boolean clickWorldIconFast() {
        try {
            System.out.println("🌍 Clicking world_icon to enter world view (FAST)...");
            
            String screenPath = "screenshots/click_world_icon_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for world_icon");
                return false;
            }
            
            Point worldIcon = BotUtils.findImageOnScreen(screenPath, "world_icon.png", 0.6);
            if (worldIcon != null) {
                System.out.println("✅ Found world_icon at " + worldIcon);
                if (BotUtils.clickMenu(instance.index, worldIcon)) {
                    System.out.println("✅ Clicked world_icon successfully (FAST)");
                    Thread.sleep(2000); // SPEED: 3s → 2s
                    return true;
                }
            }
            
            System.err.println("❌ Could not find or click world_icon");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking world_icon: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * SPEED OPTIMIZED: Verify we're in world view by checking for town_icon with faster timing
     */
    public boolean verifyWorldViewWithTownIconFast() {
        try {
            System.out.println("🌍 Verifying world view by checking for town_icon (FAST MODE)...");
            
            // SPEED: Reduced attempts and faster checking
            int maxAttempts = 6; // SPEED: 10 → 6 attempts (6 seconds total)
            int attemptDelay = 1000; // Keep 1 second between attempts
            
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                System.out.println("🔍 Fast verification attempt " + attempt + "/" + maxAttempts + "...");
                
                String screenPath = "screenshots/verify_world_view_fast" + attempt + "_" + instance.index + ".png";
                if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                    System.err.println("❌ Failed to take screenshot for world view verification on attempt " + attempt);
                    if (attempt < maxAttempts) {
                        Thread.sleep(attemptDelay);
                        continue;
                    } else {
                        return false;
                    }
                }
                
                Point townIcon = BotUtils.findImageOnScreen(screenPath, "town_icon.png", 0.6);
                if (townIcon != null) {
                    System.out.println("✅ Verified world view - found town_icon at " + townIcon + " on attempt " + attempt + " (FAST)");
                    return true;
                }
                
                System.out.println("⏳ Town icon not found on attempt " + attempt + ", waiting " + attemptDelay + "ms before retry...");
                
                // Don't sleep after the last attempt
                if (attempt < maxAttempts) {
                    Thread.sleep(attemptDelay);
                }
            }
            
            System.err.println("❌ Could not find town_icon after " + maxAttempts + " attempts (FAST MODE) - not in world view or view hasn't loaded");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error verifying world view: " + e.getMessage());
            return false;
        }
    }
    
    // Legacy methods for compatibility - these just call the fast versions
    public boolean clickWorldIcon() {
        return clickWorldIconFast();
    }
    
    public boolean verifyWorldViewWithTownIcon() {
        return verifyWorldViewWithTownIconFast();
    }
}