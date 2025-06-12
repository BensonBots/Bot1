package newgame;

import java.awt.Point;
import java.nio.file.Paths;
import java.io.File;

public class AutoStartGameTask extends javax.swing.SwingWorker<Void, String> {
    private final MemuInstance instance;
    private final int attempts;
    private final Runnable onComplete;
    private volatile boolean shouldStop = false;

    public AutoStartGameTask(MemuInstance instance, int attempts, Runnable onComplete) {
        this.instance = instance;
        this.attempts = attempts;
        this.onComplete = onComplete;
    }

    @Override
    protected Void doInBackground() throws Exception {
        if (instance == null) {
            System.err.println("Cannot start game loop: instance is null");
            return null;
        }
        
        if (instance.isAutoStartGameRunning()) {
            System.out.println("Auto start game already running for instance " + instance.index);
            return null;
        }
        
        instance.setAutoStartGameRunning(true);
        instance.setState("Starting game...");
        
        Main.addToConsole("🎮 " + instance.name + " launching game...");
        
        try {
            String screenPath = Paths.get(BotUtils.SCREENSHOTS_DIR, "resolution_check_" + instance.index + ".png").toString();
            BotUtils.createDirectoryIfNeeded(BotUtils.SCREENSHOTS_DIR);
            
            for (int i = 0; i < attempts && !shouldStop && !isCancelled(); i++) {
                System.out.println("Game start attempt " + (i+1) + "/" + attempts + " for instance " + instance.index);
                publish("Game start attempt " + (i+1) + "/" + attempts);
                
                // Take screenshot with retry logic
                boolean screenshotSuccess = false;
                for (int retry = 0; retry < 3; retry++) {
                    System.out.println("Screenshot attempt " + (retry + 1) + "/3 for game start...");
                    
                    File existingFile = new File(screenPath);
                    if (existingFile.exists()) {
                        existingFile.delete();
                        System.out.println("Deleted existing resolution_check file");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    
                    if (BotUtils.takeMenuScreenshotLegacy(instance.index, screenPath)) {
                        File screenFile = new File(screenPath);
                        if (screenFile.exists() && screenFile.length() > 15000) {
                            screenshotSuccess = true;
                            System.out.println("✅ Screenshot successful: " + screenFile.length() + " bytes");
                            break;
                        } else {
                            System.err.println("❌ Screenshot too small (" + 
                                (screenFile.exists() ? screenFile.length() + " bytes" : "doesn't exist") + 
                                "), retrying...");
                            BotUtils.delay(2000);
                        }
                    } else {
                        System.err.println("❌ Screenshot command failed, retrying...");
                        BotUtils.delay(2000);
                    }
                }
                
                if (!screenshotSuccess) {
                    publish("[ERROR] Screenshot failed after 3 retries (" + (i+1) + "/" + attempts + ")");
                    System.err.println("All screenshot attempts failed, skipping this game start attempt");
                    if (!BotUtils.delay(5000)) break;
                    continue;
                }
                
                // Validate screenshot with OpenCV
                if (BotUtils.isOpenCvLoaded()) {
                    org.opencv.core.Mat testMat = org.opencv.imgcodecs.Imgcodecs.imread(screenPath);
                    if (testMat.empty()) {
                        System.err.println("❌ OpenCV cannot load screenshot, retrying...");
                        testMat.release();
                        if (!BotUtils.delay(2000)) break;
                        continue;
                    } else {
                        System.out.println("✅ OpenCV validated screenshot: " + testMat.cols() + "x" + testMat.rows());
                        testMat.release();
                    }
                }
                
                // FIXED: Check for game running indicators FIRST
                if (checkForGameRunning(screenPath)) {
                    Main.addToConsole("✅ " + instance.name + " game already running");
                    publish("Game already running");
                    System.out.println("Game already detected running for instance " + instance.index);
                    break;
                }
                
                // Handle popups and try to start game
                boolean gameStarted = handlePopupsAndStartGame(screenPath, i + 1, attempts);
                
                if (gameStarted) {
                    Main.addToConsole("✅ " + instance.name + " game started successfully");
                    publish("Game started successfully");
                    break;
                }
                
                if (i < attempts - 1 && !BotUtils.delay(5000)) {
                    break;
                }
            }
            
            // Final verification
            if (!shouldStop && !isCancelled()) {
                Main.addToConsole("🔍 " + instance.name + " verifying final game status...");
                
                boolean finalScreenshotSuccess = false;
                for (int retry = 0; retry < 3; retry++) {
                    if (BotUtils.takeMenuScreenshotLegacy(instance.index, screenPath)) {
                        File screenFile = new File(screenPath);
                        if (screenFile.exists() && screenFile.length() > 15000) {
                            finalScreenshotSuccess = true;
                            break;
                        }
                    }
                    BotUtils.delay(1000);
                }
                
                if (finalScreenshotSuccess) {
                    if (checkForGameRunning(screenPath)) {
                        Main.addToConsole("✅ " + instance.name + " game verified running");
                        publish("Game running successfully");
                        System.out.println("Game confirmed running for instance " + instance.index);
                    } else {
                        Main.addToConsole("⚠️ " + instance.name + " game status uncertain");
                        publish("Game status uncertain");
                    }
                } else {
                    Main.addToConsole("❌ " + instance.name + " final verification failed");
                    publish("Final verification failed");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in auto start game loop: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " game start error: " + e.getMessage());
            e.printStackTrace();
            publish("[ERROR] " + e.getMessage());
        } finally {
            instance.setAutoStartGameRunning(false);
            String finalState = instance.isAutoGatherRunning() ? "Gathering resources" : "Idle";
            instance.setState(finalState);
            System.out.println("Auto start game loop completed for instance " + instance.index);
        }
        
        return null;
    }

    /**
     * FIXED: Better game running detection
     */
    private boolean checkForGameRunning(String screenPath) {
        try {
            System.out.println("🔍 Checking for game running indicators...");
            
            // Check for world_icon.png (main game indicator)
            Point worldIcon = BotUtils.findImageOnScreenGrayWithRetry(screenPath, "world_icon.png", 0.7, instance.index);
            if (worldIcon != null) {
                System.out.println("✅ Found world_icon.png at: " + worldIcon + " - Game is running!");
                return true;
            }
            
            // Check for game_icon.png (alternative indicator)
            Point gameIcon = BotUtils.findImageOnScreenGrayWithRetry(screenPath, "game_icon.png", 0.7, instance.index);
            if (gameIcon != null) {
                System.out.println("✅ Found game_icon.png at: " + gameIcon + " - Game is running!");
                return true;
            }
            
            // Check for town_icon.png (indicates we're in game)
            Point townIcon = BotUtils.findImageOnScreenGrayWithRetry(screenPath, "town_icon.png", 0.7, instance.index);
            if (townIcon != null) {
                System.out.println("✅ Found town_icon.png at: " + townIcon + " - Game is running!");
                return true;
            }
            
            System.out.println("❌ No game running indicators found");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error checking for game running: " + e.getMessage());
            return false;
        }
    }

    /**
     * FIXED: Better popup detection and game launching
     */
    private boolean handlePopupsAndStartGame(String screenPath, int currentAttempt, int totalAttempts) {
        try {
            System.out.println("🔄 Handling popups and starting game (attempt " + currentAttempt + "/" + totalAttempts + ")");
            
            // Step 1: Try to close any blocking popups (but be more selective)
            boolean popupClosed = detectAndClosePopupSelective(screenPath, currentAttempt, totalAttempts);
            
            if (popupClosed) {
                System.out.println("✅ Popup closed, waiting for UI to settle...");
                BotUtils.delay(2000);
                
                // Take new screenshot after popup closure
                String afterPopupPath = "screenshots/after_popup_" + instance.index + ".png";
                if (BotUtils.takeMenuScreenshotLegacy(instance.index, afterPopupPath)) {
                    // Check if game is now running after popup closure
                    if (checkForGameRunning(afterPopupPath)) {
                        System.out.println("🎉 Game is running after popup closure!");
                        return true;
                    }
                    
                    // If not running, try to click launcher
                    System.out.println("🚀 Game not running after popup closure, looking for launcher...");
                    return tryClickGameLauncher(afterPopupPath, currentAttempt, totalAttempts);
                }
            } else {
                // No popup found, try to click launcher directly
                System.out.println("📱 No popup detected, looking for game launcher...");
                return tryClickGameLauncher(screenPath, currentAttempt, totalAttempts);
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error handling popups and starting game: " + e.getMessage());
            return false;
        }
    }

    /**
     * FIXED: More selective popup detection (avoid false positives)
     */
    private boolean detectAndClosePopupSelective(String screenPath, int currentAttempt, int totalAttempts) {
        System.out.println("=== SELECTIVE POPUP DETECTION ===");
        System.out.println("Screenshot path: " + screenPath);
        File screenFile = new File(screenPath);
        System.out.println("Screenshot exists: " + screenFile.exists() + ", size: " + screenFile.length() + " bytes");

        // FIXED: Only look for actual popup close buttons, not UI elements
        String[] realCloseButtons = {"close_x.png", "close_x2.png"};
        
        for (String closeBtn : realCloseButtons) {
            System.out.println("\n--- Testing " + closeBtn + " ---");
            
            Point closeBtnLoc = BotUtils.findImageOnScreenGrayWithRetry(screenPath, closeBtn, 0.8, instance.index);
            System.out.println(closeBtn + " result: " + closeBtnLoc);
            
            if (closeBtnLoc != null) {
                // FIXED: Validate this is actually a popup close button, not just UI
                if (isValidPopupCloseButton(closeBtnLoc, screenPath)) {
                    System.out.println("✅ Found valid popup close button: " + closeBtn);
                    if (performPopupClose(closeBtnLoc, closeBtn, currentAttempt, totalAttempts)) {
                        return true;
                    }
                } else {
                    System.out.println("❌ Invalid popup location (likely UI element): " + closeBtnLoc);
                }
            }
        }
        
        System.out.println("📱 No actual popups found");
        return false;
    }
    
    /**
     * FIXED: Validate if close button is actually a popup (not just UI)
     */
    private boolean isValidPopupCloseButton(Point location, String screenPath) {
        // Close buttons should be in popup areas, not main UI
        // For 480x800 resolution:
        
        // Reject locations that are clearly main UI elements
        if (location.x < 50 || location.x > 430) {
            System.out.println("❌ Close button too close to screen edge (likely UI): " + location);
            return false;
        }
        
        if (location.y < 100 || location.y > 700) {
            System.out.println("❌ Close button too close to top/bottom (likely UI): " + location);
            return false;
        }
        
        // Known false positive locations from your logs
        if (location.x == 400 && location.y == 265) {
            System.out.println("❌ Known false positive location: " + location);
            return false;
        }
        
        System.out.println("✅ Valid popup close button location: " + location);
        return true;
    }

    /**
     * FIXED: Try to click game launcher and verify if game starts
     */
    private boolean tryClickGameLauncher(String screenPath, int currentAttempt, int totalAttempts) {
        try {
            // Look for game launcher with multiple confidence levels
            double[] confidences = {0.8, 0.7, 0.6, 0.5};
            Point launcher = null;
            
            for (double confidence : confidences) {
                launcher = BotUtils.findImageOnScreenGrayWithRetry(screenPath, "game_launcher.png", confidence, instance.index);
                if (launcher != null) {
                    System.out.println("✅ Found game launcher at: " + launcher + " (confidence: " + confidence + ")");
                    break;
                }
            }
            
            if (launcher != null) {
                System.out.println("🖱️ Clicking game launcher at: " + launcher);
                if (BotUtils.clickMenu(instance.index, launcher)) {
                    Main.addToConsole("🚀 " + instance.name + " clicked game launcher");
                    publish("Launched game (" + currentAttempt + "/" + totalAttempts + ")");
                    
                    // Wait for game to start
                    System.out.println("⏳ Waiting for game to start after launcher click...");
                    BotUtils.delay(10000); // Wait longer for game to load
                    
                    // Take screenshot to check if game started
                    String afterLaunchPath = "screenshots/after_launch_" + instance.index + ".png";
                    if (BotUtils.takeMenuScreenshotLegacy(instance.index, afterLaunchPath)) {
                        if (checkForGameRunning(afterLaunchPath)) {
                            System.out.println("🎉 Game successfully started after launcher click!");
                            return true;
                        } else {
                            System.out.println("⚠️ Game not detected after launcher click, waiting more...");
                            
                            // Wait even more and check again
                            BotUtils.delay(8000);
                            String finalCheckPath = "screenshots/final_check_" + instance.index + ".png";
                            if (BotUtils.takeMenuScreenshotLegacy(instance.index, finalCheckPath)) {
                                return checkForGameRunning(finalCheckPath);
                            }
                        }
                    }
                } else {
                    publish("[ERROR] Click failed (" + currentAttempt + "/" + totalAttempts + ")");
                    System.err.println("❌ Failed to click game launcher");
                }
            } else {
                publish("[ERROR] Launcher not found (" + currentAttempt + "/" + totalAttempts + ")");
                System.out.println("❌ Game launcher not found for instance " + instance.index);
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error trying to click game launcher: " + e.getMessage());
            return false;
        }
    }
    
    private boolean performPopupClose(Point closeBtnLoc, String closeBtn, int currentAttempt, int totalAttempts, double confidence) {
        System.out.println("🎯 ATTEMPTING TO CLOSE POPUP:");
        System.out.println("  Button: " + closeBtn);
        System.out.println("  Location: " + closeBtnLoc);
        
        if (BotUtils.clickMenu(instance.index, closeBtnLoc)) {
            System.out.println("  ✅ Popup close click successful");
            Main.addToConsole("🚫 " + instance.name + " closed popup with " + closeBtn);
            publish("Closed popup (" + currentAttempt + "/" + totalAttempts + ")");
            
            BotUtils.delay(2000);
            return true;
        } else {
            System.err.println("🚫 POPUP CLOSE FAILED for " + closeBtn + " at " + closeBtnLoc);
            return false;
        }
    }
    
    // Overload for compatibility
    private boolean performPopupClose(Point closeBtnLoc, String closeBtn, int currentAttempt, int totalAttempts) {
        return performPopupClose(closeBtnLoc, closeBtn, currentAttempt, totalAttempts, 0.8);
    }

    @Override
    protected void process(java.util.List<String> chunks) {
        if (!chunks.isEmpty()) {
            String latestMessage = chunks.get(chunks.size() - 1);
            instance.setState(latestMessage);
        }
    }

    @Override
    protected void done() {
        try {
            get();
            System.out.println("✅ AutoStartGameTask completed successfully for instance " + instance.index);
        } catch (Exception e) {
            Main.addToConsole("❌ " + instance.name + " auto start game failed");
            System.err.println("AutoStartGameTask failed: " + e.getMessage());
        } finally {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public void stop() {
        shouldStop = true;
        cancel(true);
        Main.addToConsole("🛑 " + instance.name + " auto start game stopped");
        System.out.println("Stop requested for auto start game task on instance " + instance.index);
    }
}