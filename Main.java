package newgame;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.Timer;
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
    private static Main mainInstance;

    // Enhanced system management
    private Map<Integer, String> hibernationStates = new HashMap<>();
    private InstanceQueueManager queueManager;
    private SystemSettings systemSettings;
    
    // Selection system
    private Set<Integer> selectedInstances = new HashSet<>();
    private JCheckBox selectAllCheckbox;
    
    // Action panels
    private JPanel selectionActionsPanel;
    private JLabel selectionStatusLabel;

    public Main() {
        instance = this;
        mainInstance = this;
        
        // Initialize system components
        systemSettings = new SystemSettings();
        queueManager = new InstanceQueueManager(systemSettings);
        
        BotUtils.init();
        configureWindow();
        initializeUI();
        loadSettings();
        refreshInstances();
        startCleanStatusUpdater();
        addConsoleMessage("🚀 Benson v1.0.3 started - Enhanced Module Orchestration System!");
    }

    public static Main getInstance() {
        return mainInstance;
    }

    public void forceUpdateInstanceStatus(int instanceIndex, String newStatus) {
        try {
            if (isHibernationStatus(newStatus)) {
                hibernationStates.put(instanceIndex, newStatus);
                queueManager.updateInstanceStatus(instanceIndex, InstanceQueueManager.InstanceStatus.HIBERNATING);
                System.out.println("🔥 [HIBERNATION] Stored hibernation status for instance " + instanceIndex + ": " + newStatus);
            } else if (newStatus.contains("Stopped")) {
                hibernationStates.remove(instanceIndex);
                queueManager.updateInstanceStatus(instanceIndex, InstanceQueueManager.InstanceStatus.STOPPED);
            } else {
                hibernationStates.remove(instanceIndex);
                queueManager.updateInstanceStatus(instanceIndex, InstanceQueueManager.InstanceStatus.RUNNING);
            }
            
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < instances.size() && i < tableModel.getRowCount(); i++) {
                    MemuInstance inst = instances.get(i);
                    if (inst.index == instanceIndex) {
                        tableModel.setValueAt(newStatus, i, 3); // Status column
                        instancesTable.repaint();
                        updateSelectionActionsPanel();
                        break;
                    }
                }
            });
            
        } catch (Exception e) {
            System.err.println("❌ Error in forceUpdateInstanceStatus: " + e.getMessage());
        }
    }

    private void configureWindow() {
        setTitle("Benson v1.0.3 - Enhanced Module Orchestration System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 700);
        setLocationRelativeTo(null);
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to set LAF: " + ex);
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Enhanced table model with selection and module queue columns
        tableModel = new DefaultTableModel(
            new Object[]{"☑", "Priority", "Index", "Name", "Status", "Module Queue", "Actions"}, 0) {
            @Override 
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 6; // Checkbox and actions columns
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class;
                return String.class;
            }
        };

        instancesTable = new JTable(tableModel);
        configureTable();

        JPanel topPanel = createTopPanel();
        JPanel selectionPanel = createSelectionActionsPanel();
        JPanel consolePanel = createConsolePanel();

        // System status panel
        JPanel systemStatusPanel = createSystemStatusPanel();

        // Main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Upper panel with table and selection actions
        JPanel upperPanel = new JPanel(new BorderLayout());
        upperPanel.add(new JScrollPane(instancesTable), BorderLayout.CENTER);
        upperPanel.add(selectionPanel, BorderLayout.SOUTH);
        
        mainSplitPane.setTopComponent(upperPanel);
        mainSplitPane.setBottomComponent(consolePanel);
        mainSplitPane.setDividerLocation(450);
        mainSplitPane.setResizeWeight(0.7);

        add(topPanel, BorderLayout.NORTH);
        add(systemStatusPanel, BorderLayout.WEST);
        add(mainSplitPane, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        
        topPanel.add(createButton("Refresh", e -> refreshInstances()));
        topPanel.add(createButton("Create Instance", e -> createNewInstance()));
        
        optimizeAllButton = createButton("Optimize All", e -> optimizeAllInstances());
        optimizeAllButton.setBackground(new Color(34, 139, 34));
        optimizeAllButton.setToolTipText("Optimize all stopped instances to 480x800 resolution");
        topPanel.add(optimizeAllButton);

        JButton marchTrackerButton = createButton("March Tracker", e -> showMarchTracker());
        marchTrackerButton.setBackground(new Color(138, 43, 226));
        marchTrackerButton.setForeground(Color.WHITE);
        topPanel.add(marchTrackerButton);

        JButton systemSettingsButton = createButton("System Settings", e -> showSystemSettings());
        systemSettingsButton.setBackground(new Color(255, 152, 0));
        systemSettingsButton.setForeground(Color.WHITE);
        topPanel.add(systemSettingsButton);

        return topPanel;
    }

    private JPanel createSelectionActionsPanel() {
        selectionActionsPanel = new JPanel();
        selectionActionsPanel.setLayout(new BoxLayout(selectionActionsPanel, BoxLayout.Y_AXIS));
        selectionActionsPanel.setBorder(BorderFactory.createTitledBorder("Selection Actions"));
        selectionActionsPanel.setPreferredSize(new Dimension(0, 120));

        // Selection status
        selectionStatusLabel = new JLabel("No instances selected");
        selectionStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(selectionStatusLabel);
        selectionActionsPanel.add(statusPanel);

        // Instance control buttons
        JPanel instanceControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        instanceControlPanel.add(new JLabel("Instance Control:"));
        instanceControlPanel.add(createActionButton("Start Selected", e -> startSelectedInstances()));
        instanceControlPanel.add(createActionButton("Stop Selected", e -> stopSelectedInstances()));
        instanceControlPanel.add(createActionButton("Restart Selected", e -> restartSelectedInstances()));
        selectionActionsPanel.add(instanceControlPanel);

        // Module control buttons
        JPanel moduleControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        moduleControlPanel.add(new JLabel("Module Control:"));
        moduleControlPanel.add(createActionButton("▶️ Start All Modules", e -> startAllModulesOnSelected()));
        moduleControlPanel.add(createActionButton("⏸️ Pause All", e -> pauseAllModulesOnSelected()));
        moduleControlPanel.add(createActionButton("⏹️ Stop All", e -> stopAllModulesOnSelected()));
        moduleControlPanel.add(createActionButton("🔄 Restart Chains", e -> restartModuleChainsOnSelected()));
        selectionActionsPanel.add(moduleControlPanel);

        // Quick action buttons  
        JPanel quickActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        quickActionsPanel.add(new JLabel("Quick Actions:"));
        quickActionsPanel.add(createActionButton("🌾 Start Gathering", e -> startGatheringOnSelected()));
        quickActionsPanel.add(createActionButton("🎁 Run Gift Claims", e -> startGiftClaimingOnSelected()));
        quickActionsPanel.add(createActionButton("🛠️ Configure All", e -> configureSelectedInstances()));
        selectionActionsPanel.add(quickActionsPanel);

        return selectionActionsPanel;
    }

    private JPanel createSystemStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("System Status"));
        panel.setPreferredSize(new Dimension(200, 0));

        // Queue status
        JLabel queueStatusLabel = new JLabel("<html><b>Queue Status:</b><br/>Loading...</html>");
        panel.add(queueStatusLabel);

        // Resource usage
        JLabel resourceLabel = new JLabel("<html><b>Resources:</b><br/>Calculating...</html>");
        panel.add(resourceLabel);

        // Update timer for system status
        Timer systemTimer = new Timer(2000, e -> {
            SwingUtilities.invokeLater(() -> {
                InstanceQueueManager.QueueStatus status = queueManager.getQueueStatus();
                queueStatusLabel.setText(String.format(
                    "<html><b>Queue Status:</b><br/>" +
                    "Running: %d/%d<br/>" +
                    "Hibernating: %d<br/>" +
                    "Queued: %d<br/>" +
                    "Next slot: %s</html>",
                    status.runningCount, 
                    systemSettings.maxConcurrentInstances,
                    status.hibernatingCount,
                    status.queuedCount,
                    status.nextSlotETA.isEmpty() ? "Available" : status.nextSlotETA
                ));

                // Simple resource calculation
                long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                resourceLabel.setText(String.format(
                    "<html><b>Resources:</b><br/>" +
                    "Memory: %.1f GB<br/>" +
                    "Active: %d instances</html>",
                    usedMemory / (1024.0 * 1024.0 * 1024.0),
                    status.runningCount
                ));
            });
        });
        systemTimer.start();

        return panel;
    }

    private void configureTable() {
        instancesTable.setRowHeight(45);
        instancesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        instancesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Column widths
        instancesTable.getColumnModel().getColumn(0).setPreferredWidth(30);  // Checkbox
        instancesTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Priority
        instancesTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Index
        instancesTable.getColumnModel().getColumn(3).setPreferredWidth(180); // Name
        instancesTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Status
        instancesTable.getColumnModel().getColumn(5).setPreferredWidth(250); // Module Queue
        instancesTable.getColumnModel().getColumn(6).setPreferredWidth(200); // Actions

        // Custom renderers
        instancesTable.getColumnModel().getColumn(4).setCellRenderer(new StatusCellRenderer());
        instancesTable.getColumnModel().getColumn(5).setCellRenderer(new ModuleQueueRenderer());
        instancesTable.getColumnModel().getColumn(6).setCellRenderer(new ActionCellRenderer(this, instancesTable));
        instancesTable.getColumnModel().getColumn(6).setCellEditor(new ActionCellRenderer(this, instancesTable));

        // Selection handling
        instancesTable.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 0) { // Checkbox column
                updateSelectedInstances();
            }
        });

        // Header checkbox for select all
        selectAllCheckbox = new JCheckBox();
        selectAllCheckbox.addActionListener(e -> selectAllInstances(selectAllCheckbox.isSelected()));
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        headerPanel.add(selectAllCheckbox);
        instancesTable.getColumnModel().getColumn(0).setHeaderRenderer(new HeaderCheckboxRenderer(headerPanel));
    }

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                     boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            String status = value.toString();
            
            if (status.contains("Hibernating") || status.contains("😴")) {
                setForeground(isSelected ? Color.WHITE : new Color(255, 193, 7));
            } else if (status.contains("Gathering") || status.contains("🌾")) {
                setForeground(isSelected ? Color.WHITE : new Color(76, 175, 80));
            } else if (status.contains("Running") || status.contains("Idle")) {
                setForeground(isSelected ? Color.WHITE : new Color(33, 150, 243));
            } else if (status.contains("Stopped") || status.contains("⏹️")) {
                setForeground(isSelected ? Color.WHITE : new Color(158, 158, 158));
            } else if (status.contains("Queued") || status.contains("⏳")) {
                setForeground(isSelected ? Color.WHITE : new Color(156, 39, 176));
            } else {
                setForeground(isSelected ? Color.WHITE : Color.LIGHT_GRAY);
            }
            
            return c;
        }
    }

    private class ModuleQueueRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                     boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(new Font("Segoe UI", Font.PLAIN, 11));
            return this;
        }
    }

    private class HeaderCheckboxRenderer implements TableCellRenderer {
        private final JPanel panel;
        
        public HeaderCheckboxRenderer(JPanel panel) {
            this.panel = panel;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                     boolean hasFocus, int row, int column) {
            return panel;
        }
    }

    private JButton createButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.addActionListener(action);
        return btn;
    }

    private JButton createActionButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.addActionListener(action);
        return btn;
    }

    private void updateSelectedInstances() {
        selectedInstances.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
            if (selected != null && selected) {
                Integer index = (Integer) tableModel.getValueAt(i, 2);
                selectedInstances.add(index);
            }
        }
        updateSelectionActionsPanel();
    }

    private void selectAllInstances(boolean select) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(select, i, 0);
        }
        updateSelectedInstances();
    }

    private void updateSelectionActionsPanel() {
        int count = selectedInstances.size();
        if (count == 0) {
            selectionStatusLabel.setText("No instances selected");
        } else {
            selectionStatusLabel.setText(count + " instance(s) selected - Actions available");
        }
        
        // Update select all checkbox state
        if (count == 0) {
            selectAllCheckbox.setSelected(false);
            selectAllCheckbox.setEnabled(true);
        } else if (count == tableModel.getRowCount()) {
            selectAllCheckbox.setSelected(true);
            selectAllCheckbox.setEnabled(true);
        } else {
            selectAllCheckbox.setSelected(false);
            selectAllCheckbox.setEnabled(true);
        }
    }

    // Selection action methods
    private void startSelectedInstances() {
        for (Integer index : selectedInstances) {
            queueManager.requestInstanceStart(index);
        }
        addConsoleMessage("🚀 Requested start for " + selectedInstances.size() + " selected instances");
    }

    private void stopSelectedInstances() {
        for (Integer index : selectedInstances) {
            stopInstance(index);
        }
        addConsoleMessage("🛑 Stopping " + selectedInstances.size() + " selected instances");
    }

    private void restartSelectedInstances() {
        addConsoleMessage("🔄 Restarting " + selectedInstances.size() + " selected instances");
        for (Integer index : selectedInstances) {
            stopInstance(index);
            // Wait a moment then start
            Timer restartTimer = new Timer(3000, e -> queueManager.requestInstanceStart(index));
            restartTimer.setRepeats(false);
            restartTimer.start();
        }
    }

    private void startAllModulesOnSelected() {
        for (Integer index : selectedInstances) {
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                // Start module chain for this instance
                ModuleOrchestrator.startModuleChain(inst);
            }
        }
        addConsoleMessage("▶️ Starting all modules on " + selectedInstances.size() + " selected instances");
    }

    private void pauseAllModulesOnSelected() {
        addConsoleMessage("⏸️ Pausing all modules on " + selectedInstances.size() + " selected instances");
        // Implementation for pausing modules
    }

    private void stopAllModulesOnSelected() {
        for (Integer index : selectedInstances) {
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                ModuleOrchestrator.stopModuleChain(inst);
            }
        }
        addConsoleMessage("⏹️ Stopping all modules on " + selectedInstances.size() + " selected instances");
    }

    private void restartModuleChainsOnSelected() {
        addConsoleMessage("🔄 Restarting module chains on " + selectedInstances.size() + " selected instances");
        for (Integer index : selectedInstances) {
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                ModuleOrchestrator.restartModuleChain(inst);
            }
        }
    }

    private void startGatheringOnSelected() {
        for (Integer index : selectedInstances) {
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                ModuleOrchestrator.startSpecificModule(inst, "Auto Gather Resources");
            }
        }
        addConsoleMessage("🌾 Starting Auto Gather on " + selectedInstances.size() + " selected instances");
    }

    private void startGiftClaimingOnSelected() {
        for (Integer index : selectedInstances) {
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                ModuleOrchestrator.startSpecificModule(inst, "Auto Gift Claim");
            }
        }
        addConsoleMessage("🎁 Starting Auto Gift Claim on " + selectedInstances.size() + " selected instances");
    }

    private void configureSelectedInstances() {
        if (selectedInstances.size() == 1) {
            Integer index = selectedInstances.iterator().next();
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                showModulesDialog(inst);
            }
        } else {
            // Bulk configuration dialog
            showBulkModuleConfiguration();
        }
    }

    private void showBulkModuleConfiguration() {
        // Implementation for bulk module configuration
        addConsoleMessage("🛠️ Opening bulk configuration for " + selectedInstances.size() + " instances");
        JOptionPane.showMessageDialog(this, 
            "Bulk module configuration for " + selectedInstances.size() + " instances\n" +
            "This feature will be implemented in the next update.",
            "Bulk Configuration", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSystemSettings() {
        SystemSettingsDialog dialog = new SystemSettingsDialog(this, systemSettings);
        dialog.setVisible(true);
        
        if (dialog.wasModified()) {
            queueManager.updateSettings(systemSettings);
            addConsoleMessage("⚙️ System settings updated");
            refreshInstances();
        }
    }

    // Rest of the existing methods remain the same but with enhanced module queue display
    private String getModuleQueueDisplay(MemuInstance inst) {
        Map<String, ModuleState<?>> modules = instanceModules.getOrDefault(inst.index, new HashMap<>());
        StringBuilder display = new StringBuilder();
        
        // Show enabled modules with icons
        boolean hasModules = false;
        if (modules.containsKey("Auto Start Game") && modules.get("Auto Start Game").enabled) {
            display.append("[🎮Start] ");
            hasModules = true;
        }
        if (modules.containsKey("Auto Gather Resources") && modules.get("Auto Gather Resources").enabled) {
            display.append("[🌾Gather] ");
            hasModules = true;
        }
        if (modules.containsKey("Auto Gift Claim") && modules.get("Auto Gift Claim").enabled) {
            display.append("[🎁Gift] ");
            hasModules = true;
        }
        
        if (!hasModules) {
            display.append("No modules enabled");
        } else {
            // Add status or next action
            if (inst.isAutoGatherRunning()) {
                display.append("(Running)");
            } else {
                InstanceQueueManager.InstanceStatus status = queueManager.getInstanceStatus(inst.index);
                if (status == InstanceQueueManager.InstanceStatus.QUEUED) {
                    int position = queueManager.getQueuePosition(inst.index);
                    display.append("(Queued #").append(position).append(")");
                } else if (status == InstanceQueueManager.InstanceStatus.HIBERNATING) {
                    display.append("(Hibernating)");
                } else {
                    display.append("(Ready)");
                }
            }
        }
        
        return display.toString();
    }

    // Continue with existing methods...
    public void refreshInstances() {
        new SwingWorker<List<MemuInstance>, Void>() {
            @Override 
            protected List<MemuInstance> doInBackground() throws Exception {
                return getInstancesFromMemuc();
            }
            
            @Override 
            protected void done() {
                try {
                    List<MemuInstance> freshInstances = get();
                    
                    // Preserve states
                    for (MemuInstance freshInst : freshInstances) {
                        MemuInstance existingInst = instances.stream()
                            .filter(inst -> inst.index == freshInst.index)
                            .findFirst()
                            .orElse(null);
                        
                        if (existingInst != null) {
                            freshInst.setAutoGatherRunning(existingInst.isAutoGatherRunning());
                            freshInst.setAutoStartGameRunning(existingInst.isAutoStartGameRunning());
                            
                            String hibernationState = hibernationStates.get(freshInst.index);
                            if (hibernationState != null) {
                                freshInst.setState(hibernationState);
                            }
                        }
                        
                        // Update queue manager
                        queueManager.registerInstance(freshInst.index, getInstancePriority(freshInst.index));
                    }
                    
                    instances = freshInstances;
                    
                    // Rebuild table
                    tableModel.setRowCount(0);
                    for (MemuInstance inst : instances) {
                        String displayStatus = getDisplayStatus(inst);
                        String moduleQueue = getModuleQueueDisplay(inst);
                        String priority = getInstancePriorityDisplay(inst.index);
                        
                        tableModel.addRow(new Object[]{
                            false, // Checkbox
                            priority,
                            inst.index,
                            inst.name,
                            displayStatus,
                            moduleQueue,
                            "" // Actions
                        });
                    }
                    
                    addConsoleMessage("🔄 Refreshed " + instances.size() + " instances (Enhanced)");
                    updateSelectionActionsPanel();
                    
                } catch (Exception ex) {
                    showError("Refresh Failed", "Couldn't get instances: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private String getInstancePriorityDisplay(int index) {
        InstanceQueueManager.Priority priority = queueManager.getInstancePriority(index);
        switch (priority) {
            case HIGH: return "High ⭐";
            case LOW: return "Low ⭇";
            default: return "Normal";
        }
    }

    private InstanceQueueManager.Priority getInstancePriority(int index) {
        // Default to normal priority - this could be made configurable
        return InstanceQueueManager.Priority.NORMAL;
    }

    // Include all the existing methods from the original Main.java...
    // (createConsolePanel, addConsoleMessage, etc. - keeping them exactly the same)
    
    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Activity Console"));

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleArea.setBackground(new Color(30, 30, 30));
        consoleArea.setForeground(new Color(200, 200, 200));
        consoleArea.setRows(6);

        consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

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

    // Add other essential methods...
    public void createNewInstance() {
        try {
            addConsoleMessage("🔧 Creating new MEmu instance...");
            
            String instanceName = JOptionPane.showInputDialog(
                this,
                "Enter name for the new instance:",
                "Create New Instance",
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (instanceName == null || instanceName.trim().isEmpty()) {
                addConsoleMessage("❌ Instance creation cancelled - no name provided");
                return;
            }
            
            instanceName = instanceName.trim();
            addConsoleMessage("🔧 Creating instance with name: " + instanceName);
            
            SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try {
                        ProcessBuilder createBuilder = new ProcessBuilder(
                            MEMUC_PATH, "create", instanceName
                        );
                        Process createProcess = createBuilder.start();
                        
                        boolean success = createProcess.waitFor(30, TimeUnit.SECONDS) && 
                                        createProcess.exitValue() == 0;
                        
                        if (success) {
                            Thread.sleep(2000);
                            return true;
                        } else {
                            return false;
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error creating instance: " + e.getMessage());
                        return false;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        
                        if (success) {
                            addConsoleMessage("✅ Successfully created instance: " + instanceName);
                            refreshInstances();
                            
                            JOptionPane.showMessageDialog(
                                Main.this,
                                "Instance created successfully!\n\n" +
                                "Name: " + instanceName + "\n" +
                                "You can now start and configure the instance.",
                                "Instance Created",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                            
                        } else {
                            addConsoleMessage("❌ Failed to create instance: " + instanceName);
                            
                            JOptionPane.showMessageDialog(
                                Main.this,
                                "Failed to create the instance.\n\n" +
                                "Please check that MEmu is properly installed\n" +
                                "and you have sufficient system resources.",
                                "Creation Failed",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                        
                    } catch (Exception ex) {
                        addConsoleMessage("❌ Error during instance creation: " + ex.getMessage());
                        showError("Creation Error", "An error occurred: " + ex.getMessage());
                    }
                }
            };
            
            worker.execute();
            
        } catch (Exception e) {
            addConsoleMessage("❌ Error initiating instance creation: " + e.getMessage());
            showError("Error", "Could not create instance: " + e.getMessage());
        }
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
                    
                    boolean success = optimizeSingleInstance(instance.index);
                    
                    if (success) {
                        successCount++;
                        SwingUtilities.invokeLater(() -> {
                            addConsoleMessage("✅ " + instance.name + " optimized successfully");
                        });
                    } else {
                        failureCount++;
                        SwingUtilities.invokeLater(() -> {
                            addConsoleMessage("❌ " + instance.name + " optimization failed");
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

    private void showMarchTracker() {
        MarchTrackerGUI.showTracker();
        addConsoleMessage("📈 March Tracker opened");
    }

    private String getDisplayStatus(MemuInstance inst) {
        String hibernationState = hibernationStates.get(inst.index);
        if (hibernationState != null) {
            return hibernationState;
        }
        
        String instanceState = inst.getState();
        if (instanceState != null && !instanceState.equals("Idle")) {
            return instanceState;
        }
        
        // Check queue status
        InstanceQueueManager.InstanceStatus queueStatus = queueManager.getInstanceStatus(inst.index);
        if (queueStatus == InstanceQueueManager.InstanceStatus.QUEUED) {
            int position = queueManager.getQueuePosition(inst.index);
            return "⏳ Queued (Position #" + position + ")";
        }
        
        return inst.status;
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
            hibernationStates.remove(index);
        }
        
        MemuActions.startInstance(this, index, () -> {
            refreshInstances();
            enableAutoStartIfConfigured(index);
        });
    }

    private void enableAutoStartIfConfigured(int index) {
        Map<String, ModuleState<?>> modules = instanceModules.getOrDefault(index, Collections.emptyMap());
        
        ModuleState<?> autoStartModule = modules.get("Auto Start Game");
        ModuleState<?> autoGatherModule = modules.get("Auto Gather Resources");
        
        boolean autoStartEnabled = autoStartModule != null && autoStartModule.enabled;
        boolean autoGatherEnabled = autoGatherModule != null && autoGatherModule.enabled;
        
        MemuInstance inst = getInstanceByIndex(index);
        String instanceName = inst != null ? inst.name : "Instance " + index;
        
        if (autoStartEnabled || autoGatherEnabled) {
            // Use new module orchestrator instead of individual module starts
            ModuleOrchestrator.startModuleChain(inst);
        } else {
            addConsoleMessage("ℹ️ No auto modules enabled for " + instanceName);
        }
    }

    public void stopInstance(int index) {
        MemuInstance inst = getInstanceByIndex(index);
        if (inst != null) {
            addConsoleMessage("🛑 Stopping " + inst.name);
            hibernationStates.remove(index);
            ModuleOrchestrator.stopModuleChain(inst);
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

    private void startCleanStatusUpdater() {
        statusTimer = new javax.swing.Timer(1000, e -> {
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < instances.size() && i < tableModel.getRowCount(); i++) {
                    MemuInstance inst = instances.get(i);
                    
                    if (hibernationStates.containsKey(inst.index)) {
                        continue;
                    }
                    
                    String currentStatus = getDisplayStatus(inst);
                    Object tableValue = tableModel.getValueAt(i, 4); // Status column
                    
                    if (!currentStatus.equals(tableValue)) {
                        tableModel.setValueAt(currentStatus, i, 4);
                    }
                    
                    // Update module queue display
                    String moduleQueue = getModuleQueueDisplay(inst);
                    Object currentModuleQueue = tableModel.getValueAt(i, 5);
                    if (!moduleQueue.equals(currentModuleQueue)) {
                        tableModel.setValueAt(moduleQueue, i, 5);
                    }
                }
            });
        });
        statusTimer.start();
    }

    private boolean isHibernationStatus(String status) {
        if (status == null) return false;
        String lowerStatus = status.toLowerCase();
        return lowerStatus.contains("hibernating") || 
               lowerStatus.contains("😴") ||
               (lowerStatus.contains("wake in") && lowerStatus.contains(":"));
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