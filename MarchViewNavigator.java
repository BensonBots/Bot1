package newgame;

import java.awt.Point;

/**
 * Handles navigation within the march view UI
 * Separated from main task to improve code organization
 */
public class MarchViewNavigator {
    private final MemuInstance instance;
    
    public MarchViewNavigator(MemuInstance instance) {
        this.instance = instance;
    }
    
    /**
     * Setup march view by opening left panel and clicking wilderness
     */
    public boolean setupMarchView() {
        try {
            System.out.println("🔧 Setting up march view for instance " + instance.index);
            
            if (!clickOpenLeft()) {
                System.err.println("❌ Failed to open left panel");
                return false;
            }
            
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
    
    /**
     * Click the open left panel button
     */
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
    
    /**
     * Click the wilderness button to enter march mode
     */
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
    
    /**
     * Click world icon to enter world view from town view
     */
    public boolean clickWorldIcon() {
        try {
            System.out.println("🌍 Clicking world_icon to enter world view...");
            
            String screenPath = "screenshots/click_world_icon_" + instance.index + ".png";
            if (!BotUtils.takeScreenshot(instance.index, screenPath)) {
                System.err.println("Failed to take screenshot for world_icon");
                return false;
            }
            
            Point worldIcon = BotUtils.findImageOnScreen(screenPath, "world_icon.png", 0.6);
            if (worldIcon != null) {
                System.out.println("✅ Found world_icon at " + worldIcon);
                if (BotUtils.clickMenu(instance.index, worldIcon)) {
                    System.out.println("✅ Clicked world_icon successfully");
                    Thread.sleep(3000);
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
     * Verify we're in world view by checking for town_icon
     */
    public boolean verifyWorldViewWithTownIcon() {
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
}