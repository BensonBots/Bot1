package newgame;

import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates module execution chains for instances
 */
public class ModuleOrchestrator {
    
    public enum ModulePhase {
        STARTUP,      // Auto Start Game
        ACTIVE_WORK,  // Deploy marches, claim gifts, daily tasks
        HIBERNATION,  // Wait for long operations
        WAKE_UP       // Handle completions, restart cycle
    }
    
    private static final Map<Integer, ModuleChain> activeChains = new ConcurrentHashMap<>();
    
    public static void startModuleChain(MemuInstance instance) {
        try {
            if (instance == null) return;
            
            // Stop any existing chain
            stopModuleChain(instance);
            
            // Get enabled modules for this instance
            Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new HashMap<>());
            List<String> enabledModules = new ArrayList<>();
            
            // Build module chain in correct order
            if (modules.containsKey("Auto Start Game") && modules.get("Auto Start Game").enabled) {
                enabledModules.add("Auto Start Game");
            }
            if (modules.containsKey("Auto Gather Resources") && modules.get("Auto Gather Resources").enabled) {
                enabledModules.add("Auto Gather Resources");
            }
            if (modules.containsKey("Auto Gift Claim") && modules.get("Auto Gift Claim").enabled) {
                enabledModules.add("Auto Gift Claim");
            }
            
            if (enabledModules.isEmpty()) {
                Main.addToConsole("ℹ️ " + instance.name + " - No modules enabled");
                return;
            }
            
            ModuleChain chain = new ModuleChain(instance, enabledModules);
            activeChains.put(instance.index, chain);
            chain.start();
            
            Main.addToConsole("🚀 " + instance.name + " - Started module chain: " + enabledModules);
            
        } catch (Exception e) {
            System.err.println("❌ Error starting module chain: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " - Module chain start failed: " + e.getMessage());
        }
    }
    
    public static void stopModuleChain(MemuInstance instance) {
        if (instance == null) return;
        
        ModuleChain chain = activeChains.remove(instance.index);
        if (chain != null) {
            chain.stop();
            Main.addToConsole("🛑 " + instance.name + " - Module chain stopped");
        }
    }
    
    public static void restartModuleChain(MemuInstance instance) {
        stopModuleChain(instance);
        startModuleChain(instance);
    }
    
    public static void startSpecificModule(MemuInstance instance, String moduleName) {
        try {
            if ("Auto Gather Resources".equals(moduleName)) {
                new AutoGatherResourcesTask(instance).execute();
                Main.addToConsole("🌾 " + instance.name + " - Started Auto Gather Resources");
            } else if ("Auto Start Game".equals(moduleName)) {
                new AutoStartGameTask(instance, 10, () -> {
                    Main.addToConsole("🎮 " + instance.name + " - Auto Start Game completed");
                }).execute();
            }
            // Add other modules as they're implemented
            
        } catch (Exception e) {
            System.err.println("❌ Error starting specific module: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " - Failed to start " + moduleName);
        }
    }
    
    public static boolean isChainRunning(int instanceIndex) {
        return activeChains.containsKey(instanceIndex);
    }
    
    public static String getChainStatus(int instanceIndex) {
        ModuleChain chain = activeChains.get(instanceIndex);
        if (chain != null) {
            return chain.getStatus();
        }
        return "No active chain";
    }
    
    // Inner class for managing module execution chain
    private static class ModuleChain {
        private final MemuInstance instance;
        private final List<String> moduleQueue;
        private int currentModuleIndex = 0;
        private boolean running = false;
        private String currentStatus = "Initializing";
        
        public ModuleChain(MemuInstance instance, List<String> modules) {
            this.instance = instance;
            this.moduleQueue = new ArrayList<>(modules);
        }
        
        public void start() {
            running = true;
            currentModuleIndex = 0;
            executeNextModule();
        }
        
        public void stop() {
            running = false;
            currentStatus = "Stopped";
            
            // Stop any running modules
            if (instance.isAutoGatherRunning()) {
                instance.setAutoGatherRunning(false);
            }
            if (instance.isAutoStartGameRunning()) {
                instance.setAutoStartGameRunning(false);
            }
        }
        
        public String getStatus() {
            if (!running) return "Stopped";
            
            if (currentModuleIndex < moduleQueue.size()) {
                String currentModule = moduleQueue.get(currentModuleIndex);
                return String.format("Running %d/%d modules: %s", 
                    currentModuleIndex + 1, moduleQueue.size(), currentModule);
            } else {
                return "Chain completed";
            }
        }
        
        private void executeNextModule() {
            if (!running || currentModuleIndex >= moduleQueue.size()) {
                // Chain completed
                currentStatus = "Chain completed";
                running = false;
                Main.addToConsole("✅ " + instance.name + " - Module chain completed");
                return;
            }
            
            String moduleName = moduleQueue.get(currentModuleIndex);
            currentStatus = "Executing: " + moduleName;
            
            try {
                executeModule(moduleName);
            } catch (Exception e) {
                System.err.println("❌ Error executing module " + moduleName + ": " + e.getMessage());
                Main.addToConsole("❌ " + instance.name + " - Module " + moduleName + " failed: " + e.getMessage());
                
                // Continue to next module or stop based on settings
                moveToNextModule();
            }
        }
        
        private void executeModule(String moduleName) throws Exception {
            Main.addToConsole("▶️ " + instance.name + " - Executing: " + moduleName);
            
            switch (moduleName) {
                case "Auto Start Game":
                    AutoStartGameTask autoStartTask = new AutoStartGameTask(instance, 10, () -> {
                        Main.addToConsole("✅ " + instance.name + " - Auto Start Game completed");
                        moveToNextModule();
                    });
                    autoStartTask.execute();
                    break;
                    
                case "Auto Gather Resources":
                    // This will handle its own completion and hibernation
                    AutoGatherResourcesTask gatherTask = new AutoGatherResourcesTask(instance);
                    gatherTask.execute();
                    
                    // For gather resources, we consider it "started" and move to next
                    // The hibernation system will handle the timing
                    Timer gatherStartTimer = new Timer(5000, e -> moveToNextModule());
                    gatherStartTimer.setRepeats(false);
                    gatherStartTimer.start();
                    break;
                    
                case "Auto Gift Claim":
                    // Simulate gift claiming for now
                    Main.addToConsole("🎁 " + instance.name + " - Starting Auto Gift Claim");
                    Timer giftTimer = new Timer(3000, e -> {
                        Main.addToConsole("✅ " + instance.name + " - Auto Gift Claim completed");
                        moveToNextModule();
                    });
                    giftTimer.setRepeats(false);
                    giftTimer.start();
                    break;
                    
                default:
                    Main.addToConsole("⚠️ " + instance.name + " - Unknown module: " + moduleName);
                    moveToNextModule();
                    break;
            }
        }
        
        private void moveToNextModule() {
            currentModuleIndex++;
            
            if (running) {
                // Small delay between modules
                Timer nextModuleTimer = new Timer(2000, e -> executeNextModule());
                nextModuleTimer.setRepeats(false);
                nextModuleTimer.start();
            }
        }
    }
}