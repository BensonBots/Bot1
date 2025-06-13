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
        
        boolean gameStarted = false;
        
        try {
            String screenPath = Paths.get(BotUtils.SCREENSHOTS_DIR, "game_status_check_" + instance.index + ".png").toString();
            BotUtils.createDirectoryIfNeeded(BotUtils.SCREENSHOTS_DIR);
            
            for (int i = 0; i < attempts && !shouldStop && !isCancelled() && !gameStarted; i++) {
                System.out.println("Game start attempt " + (i+1) + "/" + attempts + " for instance " + instance.index);
                publish("Game start attempt " + (i+1) + "/" + attempts);
                
                // OPTIMIZED: Take only ONE screenshot per attempt and check everything
                if (!takeValidScreenshot(screenPath, i + 1, attempts)) {
                    if (!BotUtils.delay(5000)) break;
                    continue;
                }
                
                // OPTIMIZED: Single comprehensive status check
                GameStatusResult statusResult = performComprehensiveStatusCheck(screenPath);
                
                if (statusResult.gameRunning) {
                    Main.addToConsole("✅ " + instance.name + " game already running");
                    publish("Game already running");
                    System.out.println("Game already detected running for instance " + instance.index);
                    gameStarted = true;
                    break;
                }
                
                if (statusResult.popupFound) {
                    System.out.println("🚫 Found popup, attempting to close...");
                    if (closePopup(statusResult.popupLocation, statusResult.popupType)) {
                        Main.addToConsole("🚫 " + instance.name + " closed popup");
                        publish("Closed popup, retrying...");
                        
                        // Wait and check again after popup closure
                        BotUtils.delay(3000);
                        
                        // Take new screenshot after popup closure
                        if (takeValidScreenshot(screenPath, i + 1, attempts)) {
                            GameStatusResult afterPopupResult = performComprehensiveStatusCheck(screenPath);
                            if (afterPopupResult.gameRunning) {
                                Main.addToConsole("✅ " + instance.name + " game running after popup closure");
                                publish("Game running after popup closure");
                                gameStarted = true;
                                break;
                            }
                            
                            // Try launcher if game not running after popup
                            if (afterPopupResult.launcherFound) {
                                System.out.println("🚀 Found game launcher after popup, attempting to start...");
                                if (clickGameLauncher(afterPopupResult.launcherLocation)) {
                                    if (waitForGameToStart()) {
                                        Main.addToConsole("✅ " + instance.name + " game started successfully");
                                        publish("Game started successfully");
                                        gameStarted = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else if (statusResult.launcherFound) {
                    System.out.println("🚀 Found game launcher, attempting to start...");
                    if (clickGameLauncher(statusResult.launcherLocation)) {
                        if (waitForGameToStart()) {
                            Main.addToConsole("✅ " + instance.name + " game started successfully");
                            publish("Game started successfully");
                            gameStarted = true;
                            break;
                        } else {
                            System.err.println("❌ Game failed to start after clicking launcher");
                            publish("Game failed to start");
                        }
                    } else {
                        System.err.println("❌ Failed to click game launcher");
                        publish("Failed to click launcher");
                    }
                } else {
                    System.out.println("⚠️ No actionable elements found in screenshot");
                    publish("No game elements found, retrying...");
                }
                
                if (i < attempts - 1 && !gameStarted && !BotUtils.delay(5000)) {
                    break;
                }
            }
            
            // Final verification if we didn't stop early and haven't confirmed game started
            if (!shouldStop && !isCancelled() && !gameStarted) {
                gameStarted = performFinalVerification(screenPath);
            }
            
            if (gameStarted) {
                Main.addToConsole("✅ " + instance.name + " game launch completed successfully");
                publish("Game launch completed");
            } else {
                Main.addToConsole("⚠️ " + instance.name + " game launch uncertain");
                publish("Game launch uncertain");
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
     * OPTIMIZED: Take a valid screenshot with retry logic
     */
    private boolean takeValidScreenshot(String screenPath, int attempt, int totalAttempts) {
        for (int retry = 0; retry < 3; retry++) {
            System.out.println("📸 Screenshot attempt " + (retry + 1) + "/3 for game start attempt " + attempt + "/" + totalAttempts);
            
            File existingFile = new File(screenPath);
            if (existingFile.exists()) {
                existingFile.delete();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            if (BotUtils.takeMenuScreenshotLegacy(instance.index, screenPath)) {
                File screenFile = new File(screenPath);
                if (screenFile.exists() && screenFile.length() > 15000) {
                    System.out.println("✅ Valid screenshot: " + screenFile.length() + " bytes");
                    
                    // Validate with OpenCV if available
                    if (BotUtils.isOpenCvLoaded()) {
                        org.opencv.core.Mat testMat = org.opencv.imgcodecs.Imgcodecs.imread(screenPath);
                        if (!testMat.empty()) {
                            System.out.println("✅ OpenCV validated screenshot: " + testMat.cols() + "x" + testMat.rows());
                            testMat.release();
                            return true;
                        } else {
                            testMat.release();
                            System.err.println("❌ OpenCV cannot load screenshot, retrying...");
                        }
                    } else {
                        return true;
                    }
                } else {
                    System.err.println("❌ Screenshot too small (" + 
                        (screenFile.exists() ? screenFile.length() + " bytes" : "doesn't exist") + 
                        "), retrying...");
                }
            } else {
                System.err.println("❌ Screenshot command failed, retrying...");
            }
            
            BotUtils.delay(2000);
        }
        
        System.err.println("❌ All screenshot attempts failed for game start attempt " + attempt);
        publish("[ERROR] Screenshot failed after 3 retries (" + attempt + "/" + totalAttempts + ")");
        return false;
    }

    /**
     * OPTIMIZED: Single comprehensive check for game status, popups, and launcher
     */
    private GameStatusResult performComprehensiveStatusCheck(String screenPath) {
        System.out.println("🔍 Performing comprehensive game status check...");
        
        GameStatusResult result = new GameStatusResult();
        
        // Check for game running indicators FIRST (highest priority)
        Point worldIcon = BotUtils.findImageOnScreen(screenPath, "world_icon.png", 0.7);
        if (worldIcon != null) {
            System.out.println("✅ Found world_icon.png - Game is running!");
            result.gameRunning = true;
            return result; // Early return if game is running
        }
        
        Point gameIcon = BotUtils.findImageOnScreen(screenPath, "game_icon.png", 0.7);
        if (gameIcon != null) {
            System.out.println("✅ Found game_icon.png - Game is running!");
            result.gameRunning = true;
            return result; // Early return if game is running
        }
        
        Point townIcon = BotUtils.findImageOnScreen(screenPath, "town_icon.png", 0.7);
        if (townIcon != null) {
            System.out.println("✅ Found town_icon.png - Game is running!");
            result.gameRunning = true;
            return result; // Early return if game is running
        }
        
        // Check for popups (second priority)
        String[] popupCloseButtons = {"close_x.png", "close_x2.png"};
        for (String closeBtn : popupCloseButtons) {
            Point popupLoc = BotUtils.findImageOnScreen(screenPath, closeBtn, 0.8);
            if (popupLoc != null && isValidPopupLocation(popupLoc)) {
                System.out.println("🚫 Found valid popup: " + closeBtn + " at " + popupLoc);
                result.popupFound = true;
                result.popupLocation = popupLoc;
                result.popupType = closeBtn;
                return result; // Return after finding first valid popup
            }
        }
        
        // Check for game launcher (lowest priority)
        double[] confidences = {0.8, 0.7, 0.6, 0.5};
        for (double confidence : confidences) {
            Point launcher = BotUtils.findImageOnScreen(screenPath, "game_launcher.png", confidence);
            if (launcher != null) {
                System.out.println("🚀 Found game launcher at: " + launcher + " (confidence: " + confidence + ")");
                result.launcherFound = true;
                result.launcherLocation = launcher;
                return result; // Return after finding launcher
            }
        }
        
        System.out.println("❌ No game indicators, popups, or launcher found");
        return result;
    }

    /**
     * IMPROVED: Validate popup location to avoid false positives but allow valid game popups
     */
    private boolean isValidPopupLocation(Point location) {
        // Allow reasonable popup areas within the game screen
        if (location.x < 20 || location.x > 460) {
            System.out.println("❌ Popup location too close to screen edge: " + location);
            return false;
        }
        
        if (location.y < 50 || location.y > 750) {
            System.out.println("❌ Popup location too close to top/bottom: " + location);
            return false;
        }
        
        // Remove overly restrictive known false positive - (417, 142) is actually valid
        // Only filter out clearly invalid locations
        if (location.x == 0 && location.y == 0) {
            System.out.println("❌ Invalid zero coordinate location: " + location);
            return false;
        }
        
        System.out.println("✅ Valid popup location: " + location);
        return true;
    }

    /**
     * OPTIMIZED: Close popup with confirmation
     */
    private boolean closePopup(Point popupLocation, String popupType) {
        try {
            System.out.println("🚫 Closing popup " + popupType + " at " + popupLocation);
            
            if (BotUtils.clickMenu(instance.index, popupLocation)) {
                System.out.println("✅ Successfully clicked popup close button");
                return true;
            } else {
                System.err.println("❌ Failed to click popup close button");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error closing popup: " + e.getMessage());
            return false;
        }
    }

    /**
     * FIXED: Click game launcher with better error handling
     */
    private boolean clickGameLauncher(Point launcherLocation) {
        try {
            System.out.println("🚀 Clicking game launcher at " + launcherLocation);
            
            if (BotUtils.clickMenu(instance.index, launcherLocation)) {
                System.out.println("✅ Successfully clicked game launcher");
                return true;
            } else {
                System.err.println("❌ Failed to click game launcher");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error clicking game launcher: " + e.getMessage());
            return false;
        }
    }

    /**
     * FIXED: Wait for game to start with popup handling during load
     */
    private boolean waitForGameToStart() {
        try {
            System.out.println("⏳ Waiting for game to start...");
            
            // Wait initial load time
            if (!BotUtils.delay(10000)) {
                return false;
            }
            
            // Check if game started with multiple attempts, handling popups during load
            String verifyPath = "screenshots/verify_game_start_" + instance.index + ".png";
            for (int attempt = 1; attempt <= 3; attempt++) {
                System.out.println("🔍 Verifying game start attempt " + attempt + "/3");
                
                if (BotUtils.takeMenuScreenshotLegacy(instance.index, verifyPath)) {
                    GameStatusResult result = performComprehensiveStatusCheck(verifyPath);
                    
                    if (result.gameRunning) {
                        System.out.println("🎉 Game successfully started!");
                        return true;
                    } else if (result.popupFound) {
                        System.out.println("🚫 Found popup during game load, closing it...");
                        if (closePopup(result.popupLocation, result.popupType)) {
                            System.out.println("✅ Closed popup during game load, continuing wait...");
                            // After closing popup, wait a bit and check again
                            if (!BotUtils.delay(3000)) {
                                return false;
                            }
                            
                            // Take another screenshot to check if game is now running
                            if (BotUtils.takeMenuScreenshotLegacy(instance.index, verifyPath)) {
                                GameStatusResult afterPopupResult = performComprehensiveStatusCheck(verifyPath);
                                if (afterPopupResult.gameRunning) {
                                    System.out.println("🎉 Game started successfully after closing popup!");
                                    return true;
                                }
                            }
                        } else {
                            System.err.println("❌ Failed to close popup during game load");
                        }
                    }
                }
                
                if (attempt < 3) {
                    System.out.println("⏳ Game not started yet, waiting more...");
                    if (!BotUtils.delay(5000)) {
                        return false;
                    }
                }
            }
            
            System.err.println("❌ Game did not start after waiting");
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error waiting for game to start: " + e.getMessage());
            return false;
        }
    }

    /**
     * FIXED: Final verification with return value
     */
    private boolean performFinalVerification(String screenPath) {
        try {
            Main.addToConsole("🔍 " + instance.name + " verifying final game status...");
            
            if (takeValidScreenshot(screenPath, 0, 0)) {
                GameStatusResult finalResult = performComprehensiveStatusCheck(screenPath);
                
                if (finalResult.gameRunning) {
                    Main.addToConsole("✅ " + instance.name + " game verified running");
                    publish("Game running successfully");
                    System.out.println("Game confirmed running for instance " + instance.index);
                    return true;
                } else {
                    Main.addToConsole("⚠️ " + instance.name + " game status uncertain");
                    publish("Game status uncertain");
                    return false;
                }
            } else {
                Main.addToConsole("❌ " + instance.name + " final verification failed");
                publish("Final verification failed");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in final verification: " + e.getMessage());
            return false;
        }
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

    /**
     * OPTIMIZED: Data class to hold comprehensive status check results
     */
    private static class GameStatusResult {
        boolean gameRunning = false;
        boolean popupFound = false;
        Point popupLocation = null;
        String popupType = null;
        boolean launcherFound = false;
        Point launcherLocation = null;
    }
}