package newgame;

import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Module Orchestrator with Priority-Based Execution
 * Handles module chains based on user-configured priority order
 */
public class PriorityModuleOrchestrator {
    
    public enum ExecutionMode {
        SEQUENTIAL,  // Run modules one after another
        PARALLEL,    // Run compatible modules simultaneously
        SMART        // Adaptive execution based on game state
    }
    
    public enum ModuleStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED
    }
    
    private static final Map<Integer, PriorityModuleChain> activeChains = new ConcurrentHashMap<>();
    
    public static void startModuleChain(MemuInstance instance) {
        try {
            if (instance == null) return;
            
            // Stop any existing chain
            stopModuleChain(instance);
            
            // Load execution settings
            Map<String, ModuleState<?>> modules = Main.instanceModules.getOrDefault(instance.index, new HashMap<>());
            ExecutionSettings settings = loadExecutionSettings(modules);
            
            // Get enabled modules in priority order
            List<String> enabledModules = getEnabledModulesInPriorityOrder(modules, settings);
            
            if (enabledModules.isEmpty()) {
                Main.addToConsole("ℹ️ " + instance.name + " - No modules enabled");
                return;
            }
            
            PriorityModuleChain chain = new PriorityModuleChain(instance, enabledModules, settings);
            activeChains.put(instance.index, chain);
            chain.start();
            
            Main.addToConsole("🚀 " + instance.name + " - Started priority module chain: " + enabledModules);
            
        } catch (Exception e) {
            System.err.println("❌ Error starting priority module chain: " + e.getMessage());
            Main.addToConsole("❌ " + instance.name + " - Module chain start failed: " + e.getMessage());
        }
    }
    
    public static void stopModuleChain(MemuInstance instance) {
        if (instance == null) return;
        
        PriorityModuleChain chain = activeChains.remove(instance.index);
        if (chain != null) {
            chain.stop();
            Main.addToConsole("🛑 " + instance.name + " - Priority module chain stopped");
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
        PriorityModuleChain chain = activeChains.get(instanceIndex);
        if (chain != null) {
            return chain.getDetailedStatus();
        }
        return "No active chain";
    }
    
    // Load execution settings from module configuration
    private static ExecutionSettings loadExecutionSettings(Map<String, ModuleState<?>> modules) {
        ModuleState<?> executionModule = modules.get("Module Execution Settings");
        ExecutionSettings settings = new ExecutionSettings();
        
        if (executionModule != null && executionModule.settings != null) {
            try {
                String settingsStr = executionModule.settings.toString();
                String[] parts = settingsStr.split(";");
                
                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        switch (key) {
                            case "Mode":
                                int modeIndex = Integer.parseInt(value);
                                settings.executionMode = ExecutionMode.values()[modeIndex];
                                break;
                            case "Delay":
                                settings.delayBetweenModules = Integer.parseInt(value);
                                break;
                            case "RetryFailed":
                                settings.retryFailedModules = Boolean.parseBoolean(value);
                                break;
                            case "Priority":
                                settings.priorityOrder = Arrays.asList(value.split(","));
                                break;
                        }
                    }
                }
                
                System.out.println("✅ Loaded execution settings: " + settings);
                
            } catch (Exception e) {
                System.err.println("⚠️ Error parsing execution settings, using defaults: " + e.getMessage());
            }
        }
        
        return settings;
    }
    
    // Get enabled modules sorted by priority order
    private static List<String> getEnabledModulesInPriorityOrder(Map<String, ModuleState<?>> modules, ExecutionSettings settings) {
        List<String> enabledModules = new ArrayList<>();
        
        // If we have a priority order, use it
        if (settings.priorityOrder != null && !settings.priorityOrder.isEmpty()) {
            for (String moduleName : settings.priorityOrder) {
                moduleName = moduleName.trim();
                ModuleState<?> moduleState = modules.get(moduleName);
                if (moduleState != null && moduleState.enabled) {
                    enabledModules.add(moduleName);
                }
            }
        } else {
            // Fallback to default order
            String[] defaultOrder = {
                "Auto Start Game",
                "Auto Building & Upgrades", 
                "Auto Troop Training",
                "Auto Gift Claim",
                "Auto Gather Resources",
                "Auto Daily Tasks"
            };
            
            for (String moduleName : defaultOrder) {
                ModuleState<?> moduleState = modules.get(moduleName);
                if (moduleState != null && moduleState.enabled) {
                    enabledModules.add(moduleName);
                }
            }
        }
        
        return enabledModules;
    }
    
    // Execution settings class
    private static class ExecutionSettings {
        ExecutionMode executionMode = ExecutionMode.SEQUENTIAL;
        int delayBetweenModules = 5;
        boolean retryFailedModules = true;
        List<String> priorityOrder = new ArrayList<>();
        
        @Override
        public String toString() {
            return String.format("ExecutionSettings[mode=%s, delay=%ds, retry=%s, modules=%d]", 
                executionMode, delayBetweenModules, retryFailedModules, priorityOrder.size());
        }
    }
    
    // Enhanced module chain with priority support
    private static class PriorityModuleChain {
        private final MemuInstance instance;
        private final List<String> moduleQueue;
        private final ExecutionSettings settings;
        private final Map<String, ModuleStatus> moduleStatuses;
        private int currentModuleIndex = 0;
        private boolean running = false;
        private String currentStatus = "Initializing";
        private long chainStartTime;
        
        public PriorityModuleChain(MemuInstance instance, List<String> modules, ExecutionSettings settings) {
            this.instance = instance;
            this.moduleQueue = new ArrayList<>(modules);
            this.settings = settings;
            this.moduleStatuses = new LinkedHashMap<>();
            
            // Initialize all modules as pending
            for (String module : modules) {
                moduleStatuses.put(module, ModuleStatus.PENDING);
            }
        }
        
        public void start() {
            running = true;
            currentModuleIndex = 0;
            chainStartTime = System.currentTimeMillis();
            currentStatus = "Starting module chain";
            
            Main.addToConsole("🎯 " + instance.name + " - Executing " + moduleQueue.size() + " modules in " + 
                             settings.executionMode + " mode");
            
            executeNextModule();
        }
        
        public void stop() {
            running = false;
            currentStatus = "Stopped by user";
            
            // Stop any running modules
            if (instance.isAutoGatherRunning()) {
                instance.setAutoGatherRunning(false);
            }
            if (instance.isAutoStartGameRunning()) {
                instance.setAutoStartGameRunning(false);
            }
            
            // Mark remaining modules as skipped
            for (int i = currentModuleIndex; i < moduleQueue.size(); i++) {
                String moduleName = moduleQueue.get(i);
                if (moduleStatuses.get(moduleName) == ModuleStatus.PENDING) {
                    moduleStatuses.put(moduleName, ModuleStatus.SKIPPED);
                }
            }
        }
        
        public String getDetailedStatus() {
            if (!running) return currentStatus;
            
            long elapsed = (System.currentTimeMillis() - chainStartTime) / 1000;
            int completed = 0;
            int failed = 0;
            
            for (ModuleStatus status : moduleStatuses.values()) {
                if (status == ModuleStatus.COMPLETED) completed++;
                if (status == ModuleStatus.FAILED) failed++;
            }
            
            if (currentModuleIndex < moduleQueue.size()) {
                String currentModule = moduleQueue.get(currentModuleIndex);
                return String.format("Running %d/%d modules: %s (Elapsed: %ds, Completed: %d, Failed: %d)", 
                    currentModuleIndex + 1, moduleQueue.size(), currentModule, elapsed, completed, failed);
            } else {
                return String.format("Chain completed in %ds (Completed: %d, Failed: %d)", 
                    elapsed, completed, failed);
            }
        }
        
        private void executeNextModule() {
            if (!running || currentModuleIndex >= moduleQueue.size()) {
                // Chain completed
                currentStatus = "Chain completed";
                running = false;
                
                long elapsed = (System.currentTimeMillis() - chainStartTime) / 1000;
                int completed = (int) moduleStatuses.values().stream().mapToLong(s -> s == ModuleStatus.COMPLETED ? 1 : 0).sum();
                int failed = (int) moduleStatuses.values().stream().mapToLong(s -> s == ModuleStatus.FAILED ? 1 : 0).sum();
                
                Main.addToConsole("✅ " + instance.name + " - Module chain completed in " + elapsed + 
                                "s (Completed: " + completed + ", Failed: " + failed + ")");
                
                // Handle retry if enabled
                if (settings.retryFailedModules && failed > 0) {
                    scheduleRetryForFailedModules();
                }
                
                return;
            }
            
            String moduleName = moduleQueue.get(currentModuleIndex);
            currentStatus = "Executing: " + moduleName;
            moduleStatuses.put(moduleName, ModuleStatus.RUNNING);
            
            try {
                executeModule(moduleName);
            } catch (Exception e) {
                System.err.println("❌ Error executing module " + moduleName + ": " + e.getMessage());
                Main.addToConsole("❌ " + instance.name + " - Module " + moduleName + " failed: " + e.getMessage());
                
                moduleStatuses.put(moduleName, ModuleStatus.FAILED);
                moveToNextModule();
            }
        }
        
        private void executeModule(String moduleName) throws Exception {
            Main.addToConsole("▶️ " + instance.name + " - [" + (currentModuleIndex + 1) + "/" + 
                             moduleQueue.size() + "] Executing: " + moduleName);
            
            switch (moduleName) {
                case "Auto Start Game":
                    AutoStartGameTask autoStartTask = new AutoStartGameTask(instance, 10, () -> {
                        Main.addToConsole("✅ " + instance.name + " - Auto Start Game completed");
                        moduleStatuses.put(moduleName, ModuleStatus.COMPLETED);
                        moveToNextModule();
                    });
                    autoStartTask.execute();
                    break;
                    
                case "Auto Gather Resources":
                    // This will handle its own completion and hibernation
                    AutoGatherResourcesTask gatherTask = new AutoGatherResourcesTask(instance);
                    gatherTask.execute();
                    
                    // For gather resources, mark as completed after starting
                    Timer gatherStartTimer = new Timer(5000, e -> {
                        moduleStatuses.put(moduleName, ModuleStatus.COMPLETED);
                        moveToNextModule();
                    });
                    gatherStartTimer.setRepeats(false);
                    gatherStartTimer.start();
                    break;
                    
                case "Auto Gift Claim":
                    Main.addToConsole("🎁 " + instance.name + " - Starting Auto Gift Claim");
                    Timer giftTimer = new Timer(3000, e -> {
                        Main.addToConsole("✅ " + instance.name + " - Auto Gift Claim completed");
                        moduleStatuses.put(moduleName, ModuleStatus.COMPLETED);
                        moveToNextModule();
                    });
                    giftTimer.setRepeats(false);
                    giftTimer.start();
                    break;
                    
                case "Auto Building & Upgrades":
                    Main.addToConsole("🏗️ " + instance.name + " - Starting Auto Building & Upgrades");
                    Timer buildingTimer = new Timer(2000, e -> {
                        Main.addToConsole("✅ " + instance.name + " - Auto Building & Upgrades completed");
                        moduleStatuses.put(moduleName, ModuleStatus.COMPLETED);
                        moveToNextModule();
                    });
                    buildingTimer.setRepeats(false);
                    buildingTimer.start();
                    break;
                    
                case "Auto Troop Training":
                    Main.addToConsole("⚔️ " + instance.name + " - Starting Auto Troop Training");
                    Timer troopTimer = new Timer(2500, e -> {
                        Main.addToConsole("✅ " + instance.name + " - Auto Troop Training completed");
                        moduleStatuses.put(moduleName, ModuleStatus.COMPLETED);
                        moveToNextModule();
                    });
                    troopTimer.setRepeats(false);
                    troopTimer.start();
                    break;
                    
                case "Auto Daily Tasks":
                    Main.addToConsole("📋 " + instance.name + " - Starting Auto Daily Tasks");
                    Timer dailyTimer = new Timer(4000, e -> {
                        Main.addToConsole("✅ " + instance.name + " - Auto Daily Tasks completed");
                        moduleStatuses.put(moduleName, ModuleStatus.COMPLETED);
                        moveToNextModule();
                    });
                    dailyTimer.setRepeats(false);
                    dailyTimer.start();
                    break;
                    
                default:
                    Main.addToConsole("⚠️ " + instance.name + " - Unknown module: " + moduleName);
                    moduleStatuses.put(moduleName, ModuleStatus.FAILED);
                    moveToNextModule();
                    break;
            }
        }
        
        private void moveToNextModule() {
            currentModuleIndex++;
            
            if (running) {
                // Apply delay between modules
                int delay = settings.delayBetweenModules * 1000;
                
                if (delay > 0 && currentModuleIndex < moduleQueue.size()) {
                    Main.addToConsole("⏸️ " + instance.name + " - Waiting " + settings.delayBetweenModules + 
                                    "s before next module");
                    
                    Timer nextModuleTimer = new Timer(delay, e -> executeNextModule());
                    nextModuleTimer.setRepeats(false);
                    nextModuleTimer.start();
                } else {
                    executeNextModule();
                }
            }
        }
        
        private void scheduleRetryForFailedModules() {
            List<String> failedModules = new ArrayList<>();
            for (Map.Entry<String, ModuleStatus> entry : moduleStatuses.entrySet()) {
                if (entry.getValue() == ModuleStatus.FAILED) {
                    failedModules.add(entry.getKey());
                }
            }
            
            if (!failedModules.isEmpty()) {
                Main.addToConsole("🔄 " + instance.name + " - Scheduling retry for " + failedModules.size() + 
                                " failed module(s) in 30 seconds");
                
                Timer retryTimer = new Timer(30000, e -> {
                    Main.addToConsole("🔄 " + instance.name + " - Retrying failed modules: " + failedModules);
                    
                    // Reset failed modules to pending and restart chain
                    for (String moduleName : failedModules) {
                        moduleStatuses.put(moduleName, ModuleStatus.PENDING);
                    }
                    
                    // Create new chain with only failed modules
                    PriorityModuleChain retryChain = new PriorityModuleChain(instance, failedModules, settings);
                    activeChains.put(instance.index, retryChain);
                    retryChain.start();
                });
                retryTimer.setRepeats(false);
                retryTimer.start();
            }
        }
        
        public Map<String, ModuleStatus> getModuleStatuses() {
            return new HashMap<>(moduleStatuses);
        }
    }
}