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

    // Simplified system management
    private Map<Integer, String> hibernationStates = new HashMap<>();
    
    // Simplified selection system
    private Set<Integer> selectedInstances = new HashSet<>();
    private JCheckBox selectAllCheckbox;
    
    // Simplified action panel
    private JPanel selectionActionsPanel;
    private JLabel selectionStatusLabel;

    public Main() {
        instance = this;
        mainInstance = this;
        
        BotUtils.init();
        configureWindow();
        initializeUI();
        loadSettings();
        refreshInstances();
        startCleanStatusUpdater();
        addModuleManagementMenu(); // Add the modules menu
        addConsoleMessage("🚀 Benson v1.0.3 started - Ready to automate your game!");
    }

    public static Main getInstance() {
        return mainInstance;
    }

    public void forceUpdateInstanceStatus(int instanceIndex, String newStatus) {
        try {
            if (isHibernationStatus(newStatus)) {
                hibernationStates.put(instanceIndex, newStatus);
                System.out.println("🔥 [HIBERNATION] Stored hibernation status for instance " + instanceIndex + ": " + newStatus);
            } else if (newStatus.contains("Stopped")) {
                hibernationStates.remove(instanceIndex);
            } else {
                hibernationStates.remove(instanceIndex);
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
        setTitle("Benson v1.0.3 - Game Automation Bot");
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

        // Simplified table model - removed confusing columns
        tableModel = new DefaultTableModel(
            new Object[]{"☑", "Index", "Name", "Status", "Modules", "Actions"}, 0) {
            @Override 
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 5; // Checkbox and actions columns
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
        JPanel selectionPanel = createSimplifiedSelectionPanel();
        JPanel consolePanel = createConsolePanel();

        // Main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Upper panel with table and selection actions
        JPanel upperPanel = new JPanel(new BorderLayout());
        upperPanel.add(new JScrollPane(instancesTable), BorderLayout.CENTER);
        upperPanel.add(selectionPanel, BorderLayout.SOUTH);
        
        mainSplitPane.setTopComponent(upperPanel);
        mainSplitPane.setBottomComponent(consolePanel);
        mainSplitPane.setDividerLocation(350);
        mainSplitPane.setResizeWeight(0.65);

        add(topPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        
        topPanel.add(createButton("Refresh Instances", e -> refreshInstances()));
        topPanel.add(createButton("Create New Instance", e -> createNewInstance()));
        
        optimizeAllButton = createButton("Optimize All Instances", e -> optimizeAllInstances());
        optimizeAllButton.setBackground(new Color(34, 139, 34));
        optimizeAllButton.setToolTipText("Optimize all stopped instances to proper resolution");
        topPanel.add(optimizeAllButton);

        JButton marchTrackerButton = createButton("March Tracker", e -> showMarchTracker());
        marchTrackerButton.setBackground(new Color(138, 43, 226));
        marchTrackerButton.setForeground(Color.WHITE);
        topPanel.add(marchTrackerButton);

        return topPanel;
    }

    private JPanel createSimplifiedSelectionPanel() {
        selectionActionsPanel = new JPanel();
        selectionActionsPanel.setLayout(new BoxLayout(selectionActionsPanel, BoxLayout.Y_AXIS));
        selectionActionsPanel.setBorder(BorderFactory.createTitledBorder("Control Selected Instances"));
        selectionActionsPanel.setPreferredSize(new Dimension(0, 80));

        // Selection status
        selectionStatusLabel = new JLabel("No instances selected");
        selectionStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(selectionStatusLabel);
        selectionActionsPanel.add(statusPanel);

        // Simplified control buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        controlPanel.add(createActionButton("▶️ Start Selected", e -> startSelectedInstances()));
        controlPanel.add(createActionButton("⏹️ Stop Selected", e -> stopSelectedInstances()));
        controlPanel.add(createActionButton("🌾 Start Auto Gather", e -> startGatheringOnSelected()));
        controlPanel.add(createActionButton("⚙️ Configure Modules", e -> configureSelectedInstances()));
        
        selectionActionsPanel.add(controlPanel);

        return selectionActionsPanel;
    }

    private void configureTable() {
        instancesTable.setRowHeight(45);
        instancesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        instancesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Simplified column widths
        instancesTable.getColumnModel().getColumn(0).setPreferredWidth(30);  // Checkbox
        instancesTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // Index
        instancesTable.getColumnModel().getColumn(2).setPreferredWidth(180); // Name
        instancesTable.getColumnModel().getColumn(3).setPreferredWidth(200); // Status
        instancesTable.getColumnModel().getColumn(4).setPreferredWidth(250); // Modules
        instancesTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Actions

        // Custom renderers
        instancesTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        instancesTable.getColumnModel().getColumn(4).setCellRenderer(new ModuleQueueRenderer());
        instancesTable.getColumnModel().getColumn(5).setCellRenderer(new ActionCellRenderer(this, instancesTable));
        instancesTable.getColumnModel().getColumn(5).setCellEditor(new ActionCellRenderer(this, instancesTable));

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
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
                Integer index = (Integer) tableModel.getValueAt(i, 1);
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
            selectionStatusLabel.setText("No instances selected - Select instances to control them");
        } else if (count == 1) {
            selectionStatusLabel.setText("1 instance selected - Ready for commands");
        } else {
            selectionStatusLabel.setText(count + " instances selected - Ready for batch commands");
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

    // === ENHANCED MODULE CONFIGURATION METHODS ===

    /**
     * Show the enhanced module configuration dialog
     */
    public void showModulesDialog(MemuInstance instance) {
        new EnhancedModuleConfigDialog(this, instance).setVisible(true);
    }

    /**
     * Enhanced module chain starter that uses priority-based orchestrator
     */
    private void enableAutoStartIfConfigured(int index) {
        Map<String, ModuleState<?>> modules = instanceModules.getOrDefault(index, Collections.emptyMap());
        
        MemuInstance inst = getInstanceByIndex(index);
        String instanceName = inst != null ? inst.name : "Instance " + index;
        
        // Check if any modules are enabled
        boolean hasEnabledModules = modules.values().stream()
            .anyMatch(moduleState -> moduleState != null && moduleState.enabled);
        
        if (hasEnabledModules) {
            // Use the new priority-based orchestrator
            addToConsole("🎯 " + instanceName + " - Starting priority-based module chain");
            PriorityModuleOrchestrator.startModuleChain(inst);
        } else {
            addToConsole("ℹ️ " + instanceName + " - No modules enabled");
        }
    }

    /**
     * Enhanced module queue display that shows priority order
     */
    private String getModuleQueueDisplay(MemuInstance inst) {
        Map<String, ModuleState<?>> modules = instanceModules.getOrDefault(inst.index, new HashMap<>());
        StringBuilder display = new StringBuilder();
        
        // Load execution settings to get priority order
        ModuleState<?> executionModule = modules.get("Module Execution Settings");
        List<String> priorityOrder = new ArrayList<>();
        
        if (executionModule != null && executionModule.settings != null) {
            try {
                String settingsStr = executionModule.settings.toString();
                String[] parts = settingsStr.split(";");
                
                for (String part : parts) {
                    if (part.startsWith("Priority:")) {
                        String priorityStr = part.substring(9);
                        if (!priorityStr.isEmpty()) {
                            priorityOrder = Arrays.asList(priorityStr.split(","));
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        // Show enabled modules in priority order
        boolean hasModules = false;
        int priority = 1;
        
        if (!priorityOrder.isEmpty()) {
            for (String moduleName : priorityOrder) {
                moduleName = moduleName.trim();
                ModuleState<?> moduleState = modules.get(moduleName);
                if (moduleState != null && moduleState.enabled) {
                    if (hasModules) display.append(" → ");
                    display.append(priority++).append(".").append(getModuleShortName(moduleName));
                    hasModules = true;
                }
            }
        } else {
            // Fallback to showing enabled modules without order
            String[] moduleOrder = {"Auto Start Game", "Auto Gather Resources", "Auto Gift Claim", 
                                   "Auto Building & Upgrades", "Auto Troop Training", "Auto Daily Tasks"};
            
            for (String moduleName : moduleOrder) {
                ModuleState<?> moduleState = modules.get(moduleName);
                if (moduleState != null && moduleState.enabled) {
                    if (hasModules) display.append(" → ");
                    display.append(getModuleShortName(moduleName));
                    hasModules = true;
                }
            }
        }
        
        if (!hasModules) {
            display.append("No modules enabled");
        } else {
            // Show current status
            if (PriorityModuleOrchestrator.isChainRunning(inst.index)) {
                display.append(" (Active)");
            } else if (inst.isAutoGatherRunning()) {
                display.append(" (Gathering)");
            } else {
                display.append(" (Ready)");
            }
        }
        
        return display.toString();
    }

    /**
     * Get short name for module display
     */
    private String getModuleShortName(String fullName) {
        switch (fullName) {
            case "Auto Start Game": return "🎮Start";
            case "Auto Gather Resources": return "🌾Gather";
            case "Auto Gift Claim": return "🎁Gift";
            case "Auto Building & Upgrades": return "🏗️Build";
            case "Auto Troop Training": return "⚔️Troop";
            case "Auto Daily Tasks": return "📋Daily";
            default: return fullName.substring(0, Math.min(fullName.length(), 8));
        }
    }

    /**
     * Enhanced status display that shows module chain progress
     */
    private String getDisplayStatus(MemuInstance inst) {
        String hibernationState = hibernationStates.get(inst.index);
        if (hibernationState != null) {
            return hibernationState;
        }
        
        // Check if priority module chain is running
        if (PriorityModuleOrchestrator.isChainRunning(inst.index)) {
            String chainStatus = PriorityModuleOrchestrator.getChainStatus(inst.index);
            if (chainStatus != null && !chainStatus.equals("No active chain")) {
                return chainStatus;
            }
        }
        
        String instanceState = inst.getState();
        if (instanceState != null && !instanceState.equals("Idle")) {
            return instanceState;
        }
        
        return inst.status;
    }

    // === SELECTION ACTION METHODS (ENHANCED) ===

    private void startSelectedInstances() {
        for (Integer index : selectedInstances) {
            startInstance(index);
        }
        addConsoleMessage("🚀 Starting " + selectedInstances.size() + " selected instances");
    }

    /**
     * Updated stop selected instances method to use priority orchestrator
     */
    private void stopSelectedInstances() {
        for (Integer index : selectedInstances) {
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                PriorityModuleOrchestrator.stopModuleChain(inst);
            }
            stopInstance(index);
        }
        addConsoleMessage("🛑 Stopping " + selectedInstances.size() + " selected instances");
    }

    /**
     * Updated selection action methods to work with priority orchestrator
     */
    private void startGatheringOnSelected() {
        for (Integer index : selectedInstances) {
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                // Check if Auto Gather is enabled, if not start it specifically
                Map<String, ModuleState<?>> modules = instanceModules.getOrDefault(index, new HashMap<>());
                ModuleState<?> gatherModule = modules.get("Auto Gather Resources");
                
                if (gatherModule != null && gatherModule.enabled) {
                    // Start the full module chain
                    PriorityModuleOrchestrator.startModuleChain(inst);
                } else {
                    // Start just gathering
                    PriorityModuleOrchestrator.startSpecificModule(inst, "Auto Gather Resources");
                }
            }
        }
        addConsoleMessage("🌾 Starting Auto Gather on " + selectedInstances.size() + " selected instances");
    }

    /**
     * Enhanced configure selected instances method
     */
    private void configureSelectedInstances() {
        if (selectedInstances.size() == 1) {
            Integer index = selectedInstances.iterator().next();
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                showModulesDialog(inst);
            }
        } else if (selectedInstances.size() > 1) {
            // Show batch configuration dialog
            showBatchConfigurationDialog();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Please select an instance to configure its modules.",
                "Module Configuration", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // === BATCH CONFIGURATION METHODS ===

    /**
     * Batch configuration dialog for multiple instances
     */
    private void showBatchConfigurationDialog() {
        String[] options = {
            "Copy settings from one instance to others",
            "Enable/disable specific modules on all selected",
            "Cancel"
        };
        
        int choice = JOptionPane.showOptionDialog(this,
            "Configure " + selectedInstances.size() + " selected instances:",
            "Batch Configuration",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        switch (choice) {
            case 0:
                showCopySettingsDialog();
                break;
            case 1:
                showBatchModuleToggleDialog();
                break;
            default:
                // Cancel - do nothing
                break;
        }
    }

    /**
     * Copy settings from one instance to others
     */
    private void showCopySettingsDialog() {
        // Get list of selected instances for source selection
        List<MemuInstance> selectedInstList = new ArrayList<>();
        for (Integer index : selectedInstances) {
            MemuInstance inst = getInstanceByIndex(index);
            if (inst != null) {
                selectedInstList.add(inst);
            }
        }
        
        if (selectedInstList.size() < 2) {
            JOptionPane.showMessageDialog(this,
                "Need at least 2 instances selected to copy settings.",
                "Copy Settings",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        MemuInstance sourceInstance = (MemuInstance) JOptionPane.showInputDialog(this,
            "Select source instance to copy settings FROM:",
            "Copy Settings",
            JOptionPane.QUESTION_MESSAGE,
            null,
            selectedInstList.toArray(),
            selectedInstList.get(0));
        
        if (sourceInstance != null) {
            Map<String, ModuleState<?>> sourceModules = instanceModules.get(sourceInstance.index);
            if (sourceModules == null || sourceModules.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Source instance has no module configuration to copy.",
                    "Copy Settings",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int targetCount = 0;
            for (MemuInstance targetInstance : selectedInstList) {
                if (targetInstance.index != sourceInstance.index) {
                    // Copy the entire module configuration
                    Map<String, ModuleState<?>> copiedModules = new HashMap<>();
                    for (Map.Entry<String, ModuleState<?>> entry : sourceModules.entrySet()) {
                        ModuleState<?> original = entry.getValue();
                        copiedModules.put(entry.getKey(), new ModuleState<>(original.enabled, original.settings));
                    }
                    
                    instanceModules.put(targetInstance.index, copiedModules);
                    targetCount++;
                }
            }
            
            saveSettings();
            
            JOptionPane.showMessageDialog(this,
                String.format("Successfully copied module configuration from %s to %d other instance(s).\n\n" +
                             "Copied settings include:\n" +
                             "• Module enable/disable states\n" +
                             "• Module priority order\n" +
                             "• Execution settings\n" +
                             "• Individual module configurations",
                             sourceInstance.name, targetCount),
                "Copy Settings Complete",
                JOptionPane.INFORMATION_MESSAGE);
            
            addConsoleMessage("📋 Copied module settings from " + sourceInstance.name + " to " + targetCount + " instances");
        }
    }

    /**
     * Batch enable/disable modules dialog
     */
    private void showBatchModuleToggleDialog() {
        String[] modules = {
            "Auto Start Game",
            "Auto Gather Resources", 
            "Auto Gift Claim",
            "Auto Building & Upgrades",
            "Auto Troop Training",
            "Auto Daily Tasks"
        };
        
        JPanel panel = new JPanel(new GridLayout(modules.length + 1, 3, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Header
        panel.add(new JLabel("Module"));
        panel.add(new JLabel("Enable"));
        panel.add(new JLabel("Disable"));
        
        ButtonGroup[] groups = new ButtonGroup[modules.length];
        JRadioButton[] enableButtons = new JRadioButton[modules.length];
        JRadioButton[] disableButtons = new JRadioButton[modules.length];
        
        for (int i = 0; i < modules.length; i++) {
            groups[i] = new ButtonGroup();
            
            JLabel moduleLabel = new JLabel(modules[i]);
            enableButtons[i] = new JRadioButton();
            disableButtons[i] = new JRadioButton();
            
            groups[i].add(enableButtons[i]);
            groups[i].add(disableButtons[i]);
            
            panel.add(moduleLabel);
            panel.add(enableButtons[i]);
            panel.add(disableButtons[i]);
        }
        
        int result = JOptionPane.showConfirmDialog(this, panel,
            "Batch Module Configuration - " + selectedInstances.size() + " instances",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            int changedInstances = 0;
            int totalChanges = 0;
            
            for (Integer instanceIndex : selectedInstances) {
                Map<String, ModuleState<?>> instanceModules = Main.instanceModules
                    .getOrDefault(instanceIndex, new HashMap<>());
                boolean instanceChanged = false;
                
                for (int i = 0; i < modules.length; i++) {
                    String moduleName = modules[i];
                    
                    if (enableButtons[i].isSelected()) {
                        ModuleState<?> currentState = instanceModules.get(moduleName);
                        Object settings = currentState != null ? currentState.settings : null;
                        instanceModules.put(moduleName, new ModuleState<>(true, settings));
                        instanceChanged = true;
                        totalChanges++;
                    } else if (disableButtons[i].isSelected()) {
                        ModuleState<?> currentState = instanceModules.get(moduleName);
                        Object settings = currentState != null ? currentState.settings : null;
                        instanceModules.put(moduleName, new ModuleState<>(false, settings));
                        instanceChanged = true;
                        totalChanges++;
                    }
                }
                
                if (instanceChanged) {
                    Main.instanceModules.put(instanceIndex, instanceModules);
                    changedInstances++;
                }
            }
            
            if (changedInstances > 0) {
                saveSettings();
                
                JOptionPane.showMessageDialog(this,
                    String.format("Batch configuration completed!\n\n" +
                                 "Changed %d module setting(s) across %d instance(s).",
                                 totalChanges, changedInstances),
                    "Batch Configuration Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                
                addConsoleMessage("⚙️ Applied batch module changes to " + changedInstances + " instances (" + totalChanges + " total changes)");
            } else {
                JOptionPane.showMessageDialog(this,
                    "No changes were made.",
                    "Batch Configuration",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    // === MODULE MANAGEMENT MENU ===

    /**
     * Add system menu item for global module management
     */
    private void addModuleManagementMenu() {
        JMenuBar menuBar = getJMenuBar();
        if (menuBar == null) {
            menuBar = new JMenuBar();
            setJMenuBar(menuBar);
        }
        
        JMenu modulesMenu = new JMenu("Modules");
        
        JMenuItem globalSettings = new JMenuItem("Global Module Settings");
        globalSettings.addActionListener(e -> showGlobalModuleSettings());
        
        JMenuItem exportSettings = new JMenuItem("Export Module Configurations");
        exportSettings.addActionListener(e -> exportModuleConfigurations());
        
        JMenuItem importSettings = new JMenuItem("Import Module Configurations");
        importSettings.addActionListener(e -> importModuleConfigurations());
        
        JMenuItem resetAllSettings = new JMenuItem("Reset All Module Settings");
        resetAllSettings.addActionListener(e -> resetAllModuleSettings());
        
        modulesMenu.add(globalSettings);
        modulesMenu.addSeparator();
        modulesMenu.add(exportSettings);
        modulesMenu.add(importSettings);
        modulesMenu.addSeparator();
        modulesMenu.add(resetAllSettings);
        
        menuBar.add(modulesMenu);
    }

    /**
     * Show global module settings dialog
     */
    private void showGlobalModuleSettings() {
        JDialog dialog = new JDialog(this, "Global Module Settings", true);
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        JTextArea infoText = new JTextArea(
            "Global Module Management\n\n" +
            "This dialog allows you to manage module settings across all instances.\n\n" +
            "Current Statistics:\n" +
            "• Total instances: " + instances.size() + "\n" +
            "• Instances with modules configured: " + instanceModules.size() + "\n" +
            "• Total module configurations: " + instanceModules.values().stream()
                .mapToInt(Map::size).sum() + "\n\n" +
            "Use the Modules menu to export/import configurations or reset settings.\n" +
            "Individual instance configuration is available through the 'Configure Modules' button."
        );
        infoText.setEditable(false);
        infoText.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(closeBtn);
        
        panel.add(infoText, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    /**
     * Export module configurations to file
     */
    private void exportModuleConfigurations() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Module Configurations");
            fileChooser.setSelectedFile(new File("module_configurations_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json"));
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                
                try (FileWriter writer = new FileWriter(file)) {
                    Gson gson = new GsonBuilder()
                        .registerTypeAdapter(new TypeToken<ModuleState<?>>(){}.getType(), new ModuleStateAdapter())
                        .setPrettyPrinting()
                        .create();
                    
                    java.lang.reflect.Type type = new TypeToken<Map<Integer, Map<String, ModuleState<?>>>>(){}.getType();
                    gson.toJson(instanceModules, type, writer);
                    
                    JOptionPane.showMessageDialog(this,
                        "Module configurations exported successfully to:\n" + file.getAbsolutePath(),
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    addConsoleMessage("📤 Exported module configurations to " + file.getName());
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error exporting configurations: " + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Import module configurations from file
     */
    private void importModuleConfigurations() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Import Module Configurations");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
            
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                
                int confirmResult = JOptionPane.showConfirmDialog(this,
                    "Import module configurations from:\n" + file.getAbsolutePath() + "\n\n" +
                    "This will overwrite existing module settings.\nContinue?",
                    "Confirm Import",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                
                if (confirmResult == JOptionPane.YES_OPTION) {
                    try (FileReader reader = new FileReader(file)) {
                        Gson gson = new GsonBuilder()
                            .registerTypeAdapter(new TypeToken<ModuleState<?>>(){}.getType(), new ModuleStateAdapter())
                            .create();
                        
                        java.lang.reflect.Type type = new TypeToken<Map<Integer, Map<String, ModuleState<?>>>>(){}.getType();
                        Map<Integer, Map<String, ModuleState<?>>> imported = gson.fromJson(reader, type);
                        
                        if (imported != null) {
                            instanceModules.putAll(imported);
                            saveSettings();
                            
                            JOptionPane.showMessageDialog(this,
                                "Module configurations imported successfully!\n\n" +
                                "Imported settings for " + imported.size() + " instance(s).",
                                "Import Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                            
                            addConsoleMessage("📥 Imported module configurations from " + file.getName());
                        } else {
                            throw new Exception("Invalid file format");
                        }
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error importing configurations: " + e.getMessage(),
                "Import Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Reset all module settings
     */
    private void resetAllModuleSettings() {
        int result = JOptionPane.showConfirmDialog(this,
            "Reset ALL module settings for ALL instances?\n\n" +
            "This will:\n" +
            "• Disable all modules on all instances\n" +
            "• Clear all module configurations\n" +
            "• Reset execution priorities to defaults\n\n" +
            "This action cannot be undone!",
            "Reset All Module Settings",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            instanceModules.clear();
            saveSettings();
            
            JOptionPane.showMessageDialog(this,
                "All module settings have been reset.\n\n" +
                "All instances now have no enabled modules.\n" +
                "You can reconfigure modules using the 'Configure Modules' button.",
                "Reset Complete",
                JOptionPane.INFORMATION_MESSAGE);
            
            addConsoleMessage("🔄 Reset all module settings - all instances now have clean configuration");
        }
    }

    // === ORIGINAL MAIN METHODS (EXISTING CODE) ===

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
                    }
                    
                    instances = freshInstances;
                    
                    // Rebuild table
                    tableModel.setRowCount(0);
                    for (MemuInstance inst : instances) {
                        String displayStatus = getDisplayStatus(inst);
                        String moduleQueue = getModuleQueueDisplay(inst);
                        
                        tableModel.addRow(new Object[]{
                            false, // Checkbox
                            inst.index,
                            inst.name,
                            displayStatus,
                            moduleQueue,
                            "" // Actions
                        });
                    }
                    
                    addConsoleMessage("🔄 Refreshed " + instances.size() + " instances");
                    updateSelectionActionsPanel();
                    
                } catch (Exception ex) {
                    showError("Refresh Failed", "Couldn't get instances: " + ex.getMessage());
                }
            }
        }.execute();
    }

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
            "This will optimize " + stoppedInstances.size() + " stopped instance(s) to proper resolution.\n" +
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
                optimizeAllButton.setText("Optimize All Instances");
                
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
            
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
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

    /**
     * Updated stop instance method to use priority orchestrator
     */
    public void stopInstance(int index) {
        MemuInstance inst = getInstanceByIndex(index);
        if (inst != null) {
            addConsoleMessage("🛑 Stopping " + inst.name);
            hibernationStates.remove(index);
            
            // Stop the priority-based module chain
            PriorityModuleOrchestrator.stopModuleChain(inst);
        }
        
        MemuActions.stopInstance(this, index, this::refreshInstances);
    }

    public MemuInstance getInstanceByIndex(int index) {
        return instances.stream()
            .filter(inst -> inst.index == index)
            .findFirst()
            .orElse(null);
    }

    private void startCleanStatusUpdater() {
        statusTimer = new Timer(1000, e -> {
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < instances.size() && i < tableModel.getRowCount(); i++) {
                    MemuInstance inst = instances.get(i);
                    
                    if (hibernationStates.containsKey(inst.index)) {
                        continue;
                    }
                    
                    String currentStatus = getDisplayStatus(inst);
                    Object tableValue = tableModel.getValueAt(i, 3); // Status column
                    
                    if (!currentStatus.equals(tableValue)) {
                        tableModel.setValueAt(currentStatus, i, 3);
                    }
                    
                    // Update module queue display
                    String moduleQueue = getModuleQueueDisplay(inst);
                    Object currentModuleQueue = tableModel.getValueAt(i, 4);
                    if (!moduleQueue.equals(currentModuleQueue)) {
                        tableModel.setValueAt(moduleQueue, i, 4);
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