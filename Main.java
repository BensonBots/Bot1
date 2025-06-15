package newgame;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main extends JFrame {
    public static final String MEMUC_PATH = "C:\\Program Files\\Microvirt\\MEmu\\memuc.exe";
    private DefaultTableModel tableModel;
    private JTable instancesTable;
    public static Map<Integer, Map<String, ModuleState<?>>> instanceModules = new HashMap<>();
    private List<MemuInstance> instances = new ArrayList<>();
    private javax.swing.Timer statusTimer;
    private JButton optimizeAllButton;
    
    private JTextArea consoleArea;
    private JScrollPane consoleScrollPane;
    private static Main instance;

    public Main() {
        instance = this;
        BotUtils.init();
        configureWindow();
        initializeUI();
        loadSettings();
        refreshInstances();
        startStatusUpdater();
        addConsoleMessage("🚀 Benson v1.0.3 started - Ready for automation!");
    }

    private void configureWindow() {
        setTitle("Benson v1.0.3");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 600);
        setLocationRelativeTo(null);
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to set LAF: " + ex);
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(
            new Object[]{"Index", "Name", "Status", "Serial", "Actions"}, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return col == 4;
            }
        };

        instancesTable = new JTable(tableModel);
        configureTable();

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(createButton("Refresh", e -> refreshInstances()));
        topPanel.add(createButton("Create", e -> createInstance()));
        
        optimizeAllButton = createButton("Optimize All", e -> optimizeAllInstances());
        optimizeAllButton.setBackground(new Color(34, 139, 34));
        optimizeAllButton.setToolTipText("Optimize all stopped instances to 480x800 resolution");
        topPanel.add(optimizeAllButton);

        JButton marchTrackerButton = createButton("March Tracker", e -> showMarchTracker());
        marchTrackerButton.setBackground(new Color(138, 43, 226));
        marchTrackerButton.setForeground(Color.WHITE);
        marchTrackerButton.setToolTipText("View active marches with countdown timers");
        topPanel.add(marchTrackerButton);

        // ADDED: Debug button for hibernation troubleshooting
        JButton debugButton = createButton("Debug States", e -> debugInstanceStates());
        debugButton.setBackground(new Color(255, 165, 0));
        debugButton.setForeground(Color.WHITE);
        debugButton.setToolTipText("Debug instance states for hibernation troubleshooting");
        topPanel.add(debugButton);

        JPanel consolePanel = createConsolePanel();

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(new JScrollPane(instancesTable));
        mainSplitPane.setBottomComponent(consolePanel);
        mainSplitPane.setDividerLocation(350);
        mainSplitPane.setResizeWeight(0.7);

        add(topPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Activity Console"));

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleArea.setBackground(new Color(30, 30, 30));
        consoleArea.setForeground(new Color(200, 200, 200));
        consoleArea.setRows(8);

        consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        consoleScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel consoleControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        
        JButton clearConsoleBtn = new JButton("Clear");
        clearConsoleBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearConsoleBtn.addActionListener(e -> clearConsole());
        
        JButton copyConsoleBtn = new JButton("Copy All");
        copyConsoleBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copyConsoleBtn.addActionListener(e -> copyConsoleToClipboard());
        
        consoleControls.add(clearConsoleBtn);
        consoleControls.add(copyConsoleBtn);

        panel.add(consoleScrollPane, BorderLayout.CENTER);
        panel.add(consoleControls, BorderLayout.SOUTH);

        return panel;
    }

    public void addConsoleMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String formattedMessage = "[" + timestamp + "] " + message + "\n";
            
            consoleArea.append(formattedMessage);
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            
            String[] lines = consoleArea.getText().split("\n");
            if (lines.length > 500) {
                StringBuilder newText = new StringBuilder();
                for (int i = lines.length - 500; i < lines.length; i++) {
                    newText.append(lines[i]).append("\n");
                }
                consoleArea.setText(newText.toString());
                consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            }
        });
    }

    public static void addToConsole(String message) {
        if (instance != null) {
            instance.addConsoleMessage(message);
        }
    }

    private void clearConsole() {
        consoleArea.setText("");
        addConsoleMessage("Console cleared");
    }

    private void copyConsoleToClipboard() {
        try {
            java.awt.datatransfer.StringSelection stringSelection = 
                new java.awt.datatransfer.StringSelection(consoleArea.getText());
            java.awt.datatransfer.Clipboard clipboard = 
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            addConsoleMessage("📋 Console content copied to clipboard");
        } catch (Exception e) {
            addConsoleMessage("❌ Failed to copy to clipboard");
        }
    }

    private void configureTable() {
        instancesTable.setRowHeight(40);
        instancesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        instancesTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        instancesTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        instancesTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        instancesTable.getColumnModel().getColumn(4).setPreferredWidth(350);

        TableColumn actionsCol = instancesTable.getColumnModel().getColumn(4);
        actionsCol.setCellRenderer(new ActionCellRenderer(this, instancesTable));
        actionsCol.setCellEditor(new ActionCellRenderer(this, instancesTable));
    }

    private JButton createButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.addActionListener(action);
        return btn;
    }

    private void showMarchTracker() {
        MarchTrackerGUI.showTracker();
        addConsoleMessage("📈 March Tracker opened");
    }

    /**
     * DEBUG: Debug instance states for hibernation troubleshooting
     */
    public void debugInstanceStates() {
        addConsoleMessage("=== DEBUG INSTANCE STATES ===");
        System.out.println("=== DEBUG INSTANCE STATES ===");
        for (int i = 0; i < instances.size(); i++) {
            MemuInstance inst = instances.get(i);
            
            String currentState = inst.getState();
            String currentStatus = inst.getStatus();
            boolean autoGatherRunning = inst.isAutoGatherRunning();
            boolean actuallyRunning = BotUtils.isInstanceRunning(inst.index);
            
            String debugInfo = "Instance " + inst.index + " (" + inst.name + "):";
            addConsoleMessage(debugInfo);
            System.out.println(debugInfo);
            
            String stateInfo = "  State: '" + currentState + "'";
            addConsoleMessage(stateInfo);
            System.out.println(stateInfo);
            
            String statusInfo = "  Status: '" + currentStatus + "'";
            addConsoleMessage(statusInfo);
            System.out.println(statusInfo);
            
            String autoGatherInfo = "  Auto Gather Running: " + autoGatherRunning;
            addConsoleMessage(autoGatherInfo);
            System.out.println(autoGatherInfo);
            
            String runningInfo = "  Actually Running: " + actuallyRunning;
            addConsoleMessage(runningInfo);
            System.out.println(runningInfo);
            
            if (i < tableModel.getRowCount()) {
                Object tableStatus = tableModel.getValueAt(i, 2);
                String tableInfo = "  Table Status: '" + tableStatus + "'";
                addConsoleMessage(tableInfo);
                System.out.println(tableInfo);
            }
            
            addConsoleMessage("---");
            System.out.println("---");
        }
        addConsoleMessage("=== END DEBUG ===");
        System.out.println("=== END DEBUG ===");
    }

    private void optimizeAllInstances() {
        List<MemuInstance> stoppedInstances = instances.stream()
            .filter(inst -> "Stopped".equals(inst.status))
            .collect(java.util.stream.Collectors.toList());
        
        if (stoppedInstances.isEmpty()) {
            addConsoleMessage("⚠️ No stopped instances to optimize");
            JOptionPane.showMessageDialog(this, 
                "No stopped instances found to optimize.\nOnly stopped instances can be optimized.", 
                "No Instances to Optimize", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            "This will optimize " + stoppedInstances.size() + " stopped instance(s) to 480x800 resolution.\n" +
            "This process may take several minutes.\n\nContinue?",
            "Optimize All Instances",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        addConsoleMessage("🔧 Starting optimization of " + stoppedInstances.size() + " instances");
        
        optimizeAllButton.setEnabled(false);
        optimizeAllButton.setText("Optimizing...");
        
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            private int currentInstance = 0;
            private int totalInstances = stoppedInstances.size();
            private int successCount = 0;
            private int failureCount = 0;
            
            @Override
            protected Void doInBackground() throws Exception {
                for (MemuInstance instance : stoppedInstances) {
                    currentInstance++;
                    
                    String message = "⚙️ Optimizing " + instance.name + " (" + currentInstance + "/" + totalInstances + ")";
                    SwingUtilities.invokeLater(() -> addConsoleMessage(message));
                    
                    SwingUtilities.invokeLater(() -> {
                        instance.setState("Optimizing (" + currentInstance + "/" + totalInstances + ")...");
                        refreshInstanceInTable(instance);
                    });
                    
                    boolean success = optimizeSingleInstance(instance.index);
                    
                    if (success) {
                        successCount++;
                        SwingUtilities.invokeLater(() -> {
                            addConsoleMessage("✅ " + instance.name + " optimized successfully");
                            instance.setState("Optimized ✅");
                            refreshInstanceInTable(instance);
                        });
                    } else {
                        failureCount++;
                        SwingUtilities.invokeLater(() -> {
                            addConsoleMessage("❌ " + instance.name + " optimization failed");
                            instance.setState("Optimization failed ❌");
                            refreshInstanceInTable(instance);
                        });
                    }
                    
                    Thread.sleep(1000);
                }
                
                return null;
            }
            
            @Override
            protected void done() {
                optimizeAllButton.setEnabled(true);
                optimizeAllButton.setText("Optimize All");
                
                addConsoleMessage("🎉 Optimization complete: " + successCount + " successful, " + failureCount + " failed");
                
                String summary = String.format(
                    "Batch optimization completed!\n\n" +
                    "✅ Successful: %d\n" +
                    "❌ Failed: %d\n" +
                    "📊 Total: %d",
                    successCount, failureCount, totalInstances
                );
                
                JOptionPane.showMessageDialog(Main.this, summary, 
                    "Optimization Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                refreshInstances();
            }
        };
        
        worker.execute();
    }
    
    private boolean optimizeSingleInstance(int index) {
        try {
            ProcessBuilder stopBuilder = new ProcessBuilder(MEMUC_PATH, "stop", "-i", String.valueOf(index));
            Process stopProcess = stopBuilder.start();
            stopProcess.waitFor(15, TimeUnit.SECONDS);
            Thread.sleep(3000);
            
            ProcessBuilder[] commands = {
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "disable_resize", "0"),
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "is_customed_resolution", "1"),
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "custom_resolution", "480", "800", "160"),
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "is_full_screen", "0"),
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "start_window_mode", "1"),
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "win_scaling_percent2", "75")
            };
            
            for (ProcessBuilder cmd : commands) {
                try {
                    cmd.start().waitFor(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
            }
            
            ProcessBuilder[] perfCommands = {
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "cpus", "2"),
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "memory", "3000"),
                new ProcessBuilder(MEMUC_PATH, "setconfigex", "-i", String.valueOf(index), "fps", "30")
            };
            
            for (ProcessBuilder cmd : perfCommands) {
                try {
                    cmd.start().waitFor(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private void refreshInstanceInTable(MemuInstance instance) {
        for (int i = 0; i < instances.size(); i++) {
            if (instances.get(i).index == instance.index) {
                if (i < tableModel.getRowCount()) {
                    tableModel.setValueAt(instance.status, i, 2);
                }
                break;
            }
        }
    }

    public MemuInstance createInstance() {
        int newIndex = findNextAvailableIndex();
        MemuInstance newInstance = new MemuInstance(newIndex, "New Instance " + newIndex, "Stopped", "");
        instances.add(newInstance);
        saveSettings();
        refreshInstances();
        addConsoleMessage("➕ Created new instance: " + newInstance.name);
        return newInstance;
    }

    private int findNextAvailableIndex() {
        return instances.stream()
            .mapToInt(inst -> inst.index)
            .max()
            .orElse(0) + 1;
    }

    public void refreshInstances() {
        new SwingWorker<List<MemuInstance>, Void>() {
            @Override protected List<MemuInstance> doInBackground() throws Exception {
                return getInstancesFromMemuc();
            }
            
            @Override protected void done() {
                try {
                    instances = get();
                    tableModel.setRowCount(0);
                    for (MemuInstance inst : instances) {
                        tableModel.addRow(new Object[]{
                            inst.index,
                            inst.name,
                            inst.status,
                            inst.deviceSerial,
                            ""
                        });
                    }
                    
                    addConsoleMessage("🔄 Refreshed " + instances.size() + " instances");
                    
                } catch (Exception ex) {
                    showError("Refresh Failed", "Couldn't get instances: " + ex.getMessage());
                    addConsoleMessage("❌ Failed to refresh instances: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private List<MemuInstance> getInstancesFromMemuc() throws IOException {
        List<MemuInstance> result = new ArrayList<>();
        Process p = Runtime.getRuntime().exec(new String[]{MEMUC_PATH, "listvms"});
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    try {
                        int index = Integer.parseInt(parts[0].trim());
                        String name = parts[1].trim();
                        String status = getInstanceStatus(index);
                        result.add(new MemuInstance(index, name, status, ""));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return result;
    }

    private String getInstanceStatus(int index) {
        try {
            Process p = Runtime.getRuntime().exec(
                new String[]{MEMUC_PATH, "isvmrunning", "-i", String.valueOf(index)});
            
            boolean finished = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return "Unknown";
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String output = reader.readLine();
                if (output != null) {
                    String trimmedOutput = output.trim();
                    if (trimmedOutput.equals("1") || trimmedOutput.equalsIgnoreCase("Running")) {
                        return "Running";
                    } else {
                        return "Stopped";
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Status check failed for instance " + index + ": " + ex.getMessage());
        }
        return "Unknown";
    }

    public void startInstance(int index) {
        MemuInstance inst = getInstanceByIndex(index);
        if (inst != null) {
            addConsoleMessage("🚀 Starting " + inst.name);
        }
        
        MemuActions.startInstance(this, index, () -> {
            refreshInstances();
            enableAutoStartIfConfigured(index);
        });
    }

    /**
     * FIXED: Enhanced auto start method that properly chains Auto Start Game → Auto Gather
     */
    private void enableAutoStartIfConfigured(int index) {
        Map<String, ModuleState<?>> modules = instanceModules.getOrDefault(index, Collections.emptyMap());
        
        ModuleState<?> autoStartModule = modules.get("Auto Start Game");
        ModuleState<?> autoGatherModule = modules.get("Auto Gather Resources");
        
        boolean autoStartEnabled = autoStartModule != null && autoStartModule.enabled;
        boolean autoGatherEnabled = autoGatherModule != null && autoGatherModule.enabled;
        
        MemuInstance inst = getInstanceByIndex(index);
        String instanceName = inst != null ? inst.name : "Instance " + index;
        
        System.out.println("🔧 Module check for instance " + index + ":");
        System.out.println("  Auto Start Game: " + (autoStartEnabled ? "ENABLED" : "DISABLED"));
        System.out.println("  Auto Gather Resources: " + (autoGatherEnabled ? "ENABLED" : "DISABLED"));
        System.out.println("  Available modules: " + modules.keySet());
        
        if (autoStartEnabled) {
            addConsoleMessage("🎮 Auto Start Game enabled for " + instanceName);
            if (inst != null) {
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        
                        AutoStartGameTask autoStartTask = new AutoStartGameTask(inst, 10, () -> {
                            addConsoleMessage("✅ Game started successfully for " + instanceName);
                            
                            // FIXED: After game starts, check if auto gather should also start
                            Map<String, ModuleState<?>> currentModules = instanceModules.getOrDefault(index, Collections.emptyMap());
                            ModuleState<?> currentAutoGatherModule = currentModules.get("Auto Gather Resources");
                            boolean currentAutoGatherEnabled = currentAutoGatherModule != null && currentAutoGatherModule.enabled;
                            
                            System.out.println("🔧 Post-game-start module check for instance " + index + ":");
                            System.out.println("  Auto Gather Resources: " + (currentAutoGatherEnabled ? "ENABLED" : "DISABLED"));
                            
                            if (currentAutoGatherEnabled) {
                                addConsoleMessage("🌾 Starting Auto Gather for " + instanceName + " after game start");
                                startEnhancedAutoGatherAfterDelay(inst);
                            } else {
                                addConsoleMessage("ℹ️ Auto Gather not enabled for " + instanceName);
                            }
                        });
                        
                        autoStartTask.execute();
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        } else if (autoGatherEnabled) {
            addConsoleMessage("🌾 Auto Gather Resources enabled for " + instanceName + " (without Auto Start Game)");
            if (inst != null) {
                startEnhancedAutoGatherAfterDelay(inst);
            }
        } else {
            addConsoleMessage("ℹ️ No auto modules enabled for " + instanceName);
            System.out.println("ℹ️ No auto modules enabled for instance " + index);
        }
    }

    private void startEnhancedAutoGatherAfterDelay(MemuInstance instance) {
        new Thread(() -> {
            try {
                Thread.sleep(8000);
                
                addConsoleMessage("🌾 Starting Auto Gather for " + instance.name);
                instance.setState("Starting enhanced auto gather...");
                
                new AutoGatherResourcesTask(instance).execute();
                
            } catch (InterruptedException | AWTException e) {
                Thread.currentThread().interrupt();
                addConsoleMessage("❌ Auto gather start interrupted for " + instance.name);
            }
        }).start();
    }

    public void stopInstance(int index) {
        MemuInstance inst = getInstanceByIndex(index);
        if (inst != null) {
            addConsoleMessage("🛑 Stopping " + inst.name);
        }
        
        MemuActions.stopInstance(this, index, this::refreshInstances);
    }

    public void showModulesDialog(MemuInstance instance) {
        new ModuleListDialog(this, instance).setVisible(true);
    }

    public MemuInstance getInstanceByIndex(int index) {
        return instances.stream()
            .filter(inst -> inst.index == index)
            .findFirst()
            .orElse(null);
    }

    /**
     * FIXED: Enhanced status updater with proper hibernation detection and Auto Start Game after wake
     */
    private void startStatusUpdater() {
        statusTimer = new javax.swing.Timer(1000, e -> { // Update every second for hibernation countdown
            for (int i = 0; i < instances.size() && i < tableModel.getRowCount(); i++) {
                MemuInstance inst = instances.get(i);
                
                // Get the current state from the instance
                String currentState = inst.getState();
                String displayStatus = currentState;
                boolean actuallyRunning = BotUtils.isInstanceRunning(inst.index);
                boolean autoGatherRunning = inst.isAutoGatherRunning();
                
                // FIXED: ABSOLUTE PRIORITY for hibernation detection
                boolean isHibernating = false;
                
                // Check multiple ways to detect hibernation
                if (currentState != null && currentState.contains("Hibernating - Wake in")) {
                    // Direct hibernation status from AutoGatherResourcesTask
                    java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("Wake in (\\d{2}:\\d{2}:\\d{2})");
                    java.util.regex.Matcher matcher = timePattern.matcher(currentState);
                    
                    if (matcher.find()) {
                        String timeRemaining = matcher.group(1);
                        displayStatus = "😴 Hibernating ⏰ " + timeRemaining;
                    } else {
                        displayStatus = "😴 Hibernating...";
                    }
                    isHibernating = true;
                    
                    // FORCE UPDATE: Ensure hibernation status shows in table immediately
                    Object currentTableValue = tableModel.getValueAt(i, 2);
                    if (!displayStatus.equals(currentTableValue)) {
                        tableModel.setValueAt(displayStatus, i, 2);
                        System.out.println("🔥 [FORCE HIBERNATION] " + inst.name + " forced to: " + displayStatus);
                    }
                    
                } else if (autoGatherRunning && !actuallyRunning) {
                    // Instance stopped but auto gather running = must be hibernating
                    displayStatus = "😴 Hibernating...";
                    isHibernating = true;
                    
                    // FORCE UPDATE: Ensure hibernation status shows in table immediately
                    Object currentTableValue = tableModel.getValueAt(i, 2);
                    if (!displayStatus.equals(currentTableValue)) {
                        tableModel.setValueAt(displayStatus, i, 2);
                        System.out.println("🔥 [FORCE HIBERNATION] " + inst.name + " forced to: " + displayStatus);
                    }
                }
                
                // FIXED: Only process other states if NOT hibernating
                if (!isHibernating) {
                    // FIXED: Enhanced hibernation detection and status display for other states
                    if (currentState != null) {
                        if (currentState.contains("Waking up")) {
                            displayStatus = "🌅 Waking up...";
                        } else if (currentState.contains("Awake")) {
                            displayStatus = "☀️ Awake - Ready";
                        } else if (currentState.contains("Starting") || currentState.contains("Deploying")) {
                            displayStatus = "🚀 " + currentState;
                        } else if (currentState.contains("Collecting")) {
                            displayStatus = "📊 " + currentState;
                        } else if (currentState.contains("hibernating auto gather")) {
                            displayStatus = "🔄 " + currentState;
                        } else if (currentState.contains("Starting game")) {
                            displayStatus = "🎮 " + currentState;
                        } else if (currentState.equals("Idle")) {
                            if (autoGatherRunning && actuallyRunning) {
                                // Instance is running and auto gather is running = actively gathering
                                displayStatus = "🌾 Auto Gathering...";
                            } else if (autoGatherRunning && !actuallyRunning) {
                                // This should have been caught by hibernation check above, but failsafe
                                displayStatus = "😴 Hibernating...";
                            } else {
                                // Normal idle state
                                displayStatus = actuallyRunning ? "💤 Idle" : "⏹️ Stopped";
                            }
                        } else {
                            // Handle other states normally
                            if (actuallyRunning) {
                                displayStatus = currentState != null ? currentState : "💤 Idle";
                            } else {
                                displayStatus = "⏹️ Stopped";
                            }
                        }
                    } else {
                        // FIXED: Handle null currentState case
                        if (autoGatherRunning && actuallyRunning) {
                            displayStatus = "🌾 Auto Gathering...";
                        } else {
                            displayStatus = actuallyRunning ? "💤 Idle" : "⏹️ Stopped";
                        }
                    }
                } // End of !isHibernating check
                
                // FIXED: Only update table if status actually changed to prevent flickering
                Object currentTableValue = tableModel.getValueAt(i, 2);
                if (!displayStatus.equals(currentTableValue)) {
                    // EXTRA PROTECTION: Don't overwrite hibernation status with basic status
                    String currentTableStr = currentTableValue.toString();
                    boolean tableShowsHibernation = currentTableStr.contains("Hibernating") || currentTableStr.contains("😴");
                    boolean newStatusIsBasic = displayStatus.equals("⏹️ Stopped") || displayStatus.equals("💤 Idle") || displayStatus.equals("Running");
                    
                    if (tableShowsHibernation && newStatusIsBasic && isHibernating) {
                        // Don't overwrite hibernation status with basic status
                        System.out.println("🛡️ [PROTECTION] Preventing overwrite of hibernation status for " + inst.name);
                        System.out.println("    Current table: '" + currentTableStr + "', Attempted: '" + displayStatus + "'");
                    } else {
                        tableModel.setValueAt(displayStatus, i, 2);
                        
                        // FIXED: Enhanced logging for hibernation state changes
                        if (displayStatus.contains("Hibernating") || currentTableValue.toString().contains("Hibernating")) {
                            System.out.println("🔄 [STATUS UPDATE] " + inst.name + ": '" + currentTableValue + "' → '" + displayStatus + "'");
                            System.out.println("    Actually Running: " + actuallyRunning + ", Auto Gather: " + autoGatherRunning);
                            System.out.println("    Current State: '" + currentState + "'");
                            System.out.println("    Is Hibernating: " + isHibernating);
                            addConsoleMessage("🔄 " + inst.name + " status: " + displayStatus);
                        }
                    }
                }
                
                // FIXED: Periodically verify actual instance status (every 10 seconds)
                if (Math.random() < 0.1) { // 10% chance per second = roughly every 10 seconds
                    String actualStatus = getInstanceStatus(inst.index);
                    if (!actualStatus.equals(inst.status)) {
                        inst.status = actualStatus;
                        
                        // FIXED: Handle hibernation wake-up detection
                        if (actualStatus.equals("Running") && 
                            currentState != null && 
                            currentState.contains("Hibernating") && 
                            autoGatherRunning) {
                            
                            System.out.println("🌅 [STATUS] Detected wake-up for " + inst.name + " - instance now running");
                            addConsoleMessage("🌅 " + inst.name + " awakened from hibernation");
                            inst.setState("🌅 Instance awakened");
                            
                            // FIXED: Trigger Auto Start Game after hibernation wake-up
                            triggerAutoStartGameAfterHibernation(inst);
                        }
                        
                        // Only overwrite display status if we're not showing special hibernation info
                        if (!isHibernating && 
                            !currentState.contains("Waking") && 
                            !currentState.contains("Starting game")) {
                            
                            if (displayStatus.equals(currentState)) {
                                tableModel.setValueAt(actualStatus, i, 2);
                            }
                        }
                    }
                }
            }
        });
        statusTimer.start();
    }

    /**
     * FIXED: Trigger Auto Start Game after hibernation wake-up
     */
    private void triggerAutoStartGameAfterHibernation(MemuInstance inst) {
        // Check if Auto Start Game is enabled for this instance
        Map<String, ModuleState<?>> modules = instanceModules.getOrDefault(inst.index, Collections.emptyMap());
        ModuleState<?> autoStartModule = modules.get("Auto Start Game");
        
        boolean autoStartEnabled = autoStartModule != null && autoStartModule.enabled;
        
        if (autoStartEnabled) {
            addConsoleMessage("🎮 " + inst.name + " triggering Auto Start Game after hibernation wake-up");
            
            new Thread(() -> {
                try {
                    // Wait a moment for instance to fully start
                    Thread.sleep(5000);
                    
                    AutoStartGameTask autoStartTask = new AutoStartGameTask(inst, 10, () -> {
                        addConsoleMessage("✅ " + inst.name + " game started successfully after hibernation");
                        inst.setState("🎮 Game ready after hibernation");
                    });
                    
                    autoStartTask.execute();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    addConsoleMessage("⚠️ " + inst.name + " Auto Start Game interrupted after hibernation");
                } catch (Exception e) {
                    addConsoleMessage("❌ " + inst.name + " Auto Start Game failed after hibernation: " + e.getMessage());
                }
            }).start();
        } else {
            System.out.println("ℹ️ Auto Start Game not enabled for " + inst.name + " after hibernation wake-up");
        }
    }

    public void saveSettings() {
        try (FileWriter writer = new FileWriter("settings.json")) {
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(new TypeToken<ModuleState<?>>(){}.getType(), new ModuleStateAdapter())
                .setPrettyPrinting()
                .create();
            
            java.lang.reflect.Type type = new TypeToken<Map<Integer, Map<String, ModuleState<?>>>>(){}.getType();
            gson.toJson(instanceModules, type, writer);
            
            System.out.println("💾 Settings saved to settings.json");
            
        } catch (IOException ex) {
            showError("Save Failed", "Couldn't save settings: " + ex.getMessage());
        }
    }

    private void loadSettings() {
        File file = new File("settings.json");
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new GsonBuilder()
                    .registerTypeAdapter(new TypeToken<ModuleState<?>>(){}.getType(), new ModuleStateAdapter())
                    .create();
                
                java.lang.reflect.Type type = new TypeToken<Map<Integer, Map<String, ModuleState<?>>>>(){}.getType();
                Map<Integer, Map<String, ModuleState<?>>> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    instanceModules = loaded;
                    System.out.println("💾 Settings loaded from settings.json");
                    System.out.println("📋 Loaded modules for instances: " + loaded.keySet());
                    
                    for (Map.Entry<Integer, Map<String, ModuleState<?>>> entry : loaded.entrySet()) {
                        int instanceIdx = entry.getKey();
                        Map<String, ModuleState<?>> modules = entry.getValue();
                        System.out.println("  Instance " + instanceIdx + " modules:");
                        for (Map.Entry<String, ModuleState<?>> moduleEntry : modules.entrySet()) {
                            String moduleName = moduleEntry.getKey();
                            ModuleState<?> moduleState = moduleEntry.getValue();
                            System.out.println("    " + moduleName + ": enabled=" + moduleState.enabled + 
                                             ", settings=" + (moduleState.settings != null ? "present" : "null"));
                        }
                    }
                }
            } catch (IOException | JsonParseException ex) {
                System.err.println("❌ Failed to load settings: " + ex.getMessage());
                instanceModules = new HashMap<>();
            }
        } else {
            System.out.println("💾 No settings.json found, starting with empty configuration");
            instanceModules = new HashMap<>();
        }
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}