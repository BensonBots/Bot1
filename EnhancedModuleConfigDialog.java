package newgame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Enhanced Module Configuration Dialog with Priority Management
 * Replaces the simple ModuleListDialog with comprehensive configuration
 */
public class EnhancedModuleConfigDialog extends JDialog {
    private final Main main;
    private final MemuInstance instance;
    private final Map<String, JCheckBox> moduleCheckboxes = new LinkedHashMap<>();
    private final Map<String, JButton> configureButtons = new LinkedHashMap<>();
    private DefaultListModel<ModuleItem> priorityListModel;
    private JList<ModuleItem> priorityList;
    private JSpinner delaySpinner;
    private JComboBox<String> executionModeCombo;
    private JCheckBox retryFailedCheckbox;
    private JLabel statusLabel;

    // Module definitions with descriptions
    private static final Map<String, String> MODULE_DESCRIPTIONS = new LinkedHashMap<>();
    static {
        MODULE_DESCRIPTIONS.put("Auto Start Game", "🎮 Automatically starts the game when instance launches");
        MODULE_DESCRIPTIONS.put("Auto Gather Resources", "🌾 Deploys marches to gather resources automatically");
        MODULE_DESCRIPTIONS.put("Auto Gift Claim", "🎁 Claims daily gifts, mail rewards, and event bonuses");
        MODULE_DESCRIPTIONS.put("Auto Building & Upgrades", "🏗️ Manages building construction and upgrades");
        MODULE_DESCRIPTIONS.put("Auto Troop Training", "⚔️ Trains troops continuously in background");
        MODULE_DESCRIPTIONS.put("Auto Daily Tasks", "📋 Completes daily quests and VIP activities");
    }

    public EnhancedModuleConfigDialog(Main main, MemuInstance instance) {
        super(main, "Enhanced Module Configuration - " + instance.name, true);
        this.main = main;
        this.instance = instance;
        initializeUI();
        loadCurrentSettings();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(800, 600);
        setLocationRelativeTo(getParent());

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Tab 1: Module Selection & Priority
        tabbedPane.addTab("🔧 Modules & Priority", createModulePriorityPanel());
        
        // Tab 2: Execution Settings
        tabbedPane.addTab("⚡ Execution Settings", createExecutionSettingsPanel());
        
        // Tab 3: Module-Specific Configuration
        tabbedPane.addTab("⚙️ Module Settings", createModuleSettingsPanel());

        // Status bar
        statusLabel = new JLabel("Configure modules and their execution order");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        // Button panel
        JPanel buttonPanel = createButtonPanel();

        add(tabbedPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createModulePriorityPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Left side: Module selection
        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Available Modules"));

        for (Map.Entry<String, String> entry : MODULE_DESCRIPTIONS.entrySet()) {
            String moduleName = entry.getKey();
            String description = entry.getValue();
            
            JPanel modulePanel = createModuleSelectionPanel(moduleName, description);
            selectionPanel.add(modulePanel);
            selectionPanel.add(Box.createVerticalStrut(8));
        }

        JScrollPane selectionScroll = new JScrollPane(selectionPanel);
        selectionScroll.setPreferredSize(new Dimension(400, 0));

        // Right side: Priority order
        JPanel priorityPanel = new JPanel(new BorderLayout());
        priorityPanel.setBorder(BorderFactory.createTitledBorder("Execution Priority Order"));

        priorityListModel = new DefaultListModel<>();
        priorityList = new JList<>(priorityListModel);
        priorityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        priorityList.setCellRenderer(new PriorityListCellRenderer());
        
        JScrollPane priorityScroll = new JScrollPane(priorityList);
        priorityScroll.setPreferredSize(new Dimension(300, 0));

        // Priority control buttons
        JPanel priorityControls = new JPanel(new FlowLayout());
        JButton moveUpBtn = new JButton("▲ Move Up");
        JButton moveDownBtn = new JButton("▼ Move Down");
        JButton removeBtn = new JButton("✖ Remove");

        moveUpBtn.addActionListener(e -> movePriorityItem(-1));
        moveDownBtn.addActionListener(e -> movePriorityItem(1));
        removeBtn.addActionListener(e -> removePriorityItem());

        priorityControls.add(moveUpBtn);
        priorityControls.add(moveDownBtn);
        priorityControls.add(removeBtn);

        priorityPanel.add(priorityScroll, BorderLayout.CENTER);
        priorityPanel.add(priorityControls, BorderLayout.SOUTH);

        // Help text
        JTextArea helpText = new JTextArea(
            "Module Priority Help:\n\n" +
            "1. Enable modules you want to use by checking the boxes on the left\n" +
            "2. Enabled modules automatically appear in the priority list on the right\n" +
            "3. Use the Move Up/Down buttons to arrange execution order\n" +
            "4. Higher modules in the list execute first\n" +
            "5. Auto Start Game always runs first (if enabled)\n\n" +
            "Recommended Order:\n" +
            "• Auto Start Game (always first)\n" +
            "• Auto Building & Upgrades (when resources available)\n" +
            "• Auto Troop Training (background activity)\n" +
            "• Auto Gift Claim (quick tasks)\n" +
            "• Auto Gather Resources (long-running)\n" +
            "• Auto Daily Tasks (end of cycle)"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));

        JScrollPane helpScroll = new JScrollPane(helpText);
        helpScroll.setPreferredSize(new Dimension(0, 120));

        panel.add(selectionScroll, BorderLayout.WEST);
        panel.add(priorityPanel, BorderLayout.CENTER);
        panel.add(helpScroll, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createModuleSelectionPanel(String moduleName, String description) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setBackground(Color.WHITE);

        // Checkbox and description
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        JCheckBox checkbox = new JCheckBox(moduleName);
        checkbox.setFont(new Font("Segoe UI", Font.BOLD, 12));
        checkbox.addActionListener(e -> updatePriorityList());
        moduleCheckboxes.put(moduleName, checkbox);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);

        leftPanel.add(checkbox, BorderLayout.NORTH);
        leftPanel.add(descLabel, BorderLayout.CENTER);

        // Configure button (if applicable)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (hasConfiguration(moduleName)) {
            JButton configBtn = new JButton("Configure");
            configBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            configBtn.setPreferredSize(new Dimension(80, 25));
            configBtn.addActionListener(e -> configureModule(moduleName));
            configureButtons.put(moduleName, configBtn);
            rightPanel.add(configBtn);
        }

        panel.add(leftPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createExecutionSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Execution Mode
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Execution Mode:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        
        executionModeCombo = new JComboBox<>(new String[]{
            "Sequential - Run modules one after another (Recommended)",
            "Parallel - Run compatible modules simultaneously",
            "Smart - Adaptive execution based on game state"
        });
        panel.add(executionModeCombo, gbc);

        // Delay between modules
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Delay between modules:"), gbc);
        gbc.gridx = 1;
        JPanel delayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        delaySpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        delayPanel.add(delaySpinner);
        delayPanel.add(new JLabel("seconds"));
        panel.add(delayPanel, gbc);

        // Retry settings
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        retryFailedCheckbox = new JCheckBox("Retry failed modules after completing cycle");
        retryFailedCheckbox.setSelected(true);
        panel.add(retryFailedCheckbox, gbc);

        // Advanced settings section
        gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(20), gbc);

        gbc.gridy = 4;
        JLabel advancedLabel = new JLabel("Advanced Settings");
        advancedLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(advancedLabel, gbc);

        gbc.gridy = 5; gbc.gridwidth = 2;
        JCheckBox stopOnCriticalFailure = new JCheckBox("Stop all modules if critical module fails (Auto Start Game)");
        stopOnCriticalFailure.setSelected(true);
        panel.add(stopOnCriticalFailure, gbc);

        gbc.gridy = 6;
        JCheckBox enableLogging = new JCheckBox("Enable detailed module execution logging");
        enableLogging.setSelected(true);
        panel.add(enableLogging, gbc);

        // Help text for execution settings
        gbc.gridy = 7; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        JTextArea execHelpText = new JTextArea(
            "Execution Settings Help:\n\n" +
            "Sequential Mode (Recommended):\n" +
            "• Safest option - modules run in order\n" +
            "• Prevents conflicts between modules\n" +
            "• Easier to debug issues\n\n" +
            "Parallel Mode:\n" +
            "• Faster execution\n" +
            "• Some modules may conflict\n" +
            "• Best for experienced users\n\n" +
            "Smart Mode:\n" +
            "• Intelligently switches between sequential/parallel\n" +
            "• Adapts based on game state\n" +
            "• Experimental feature\n\n" +
            "Module Delays:\n" +
            "• Time to wait between starting each module\n" +
            "• Longer delays = more stable but slower\n" +
            "• Shorter delays = faster but may cause issues"
        );
        execHelpText.setEditable(false);
        execHelpText.setBackground(panel.getBackground());
        execHelpText.setBorder(BorderFactory.createTitledBorder("Help"));
        execHelpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(execHelpText, gbc);

        return panel;
    }

    private JPanel createModuleSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JTextArea settingsInfo = new JTextArea(
            "Module-Specific Configuration:\n\n" +
            "Use the 'Configure' buttons on the Modules & Priority tab to access detailed\n" +
            "settings for each module.\n\n" +
            "Quick Configuration Options:\n" +
            "• Auto Gather Resources: Resource loop, max queues, hibernation settings\n" +
            "• Auto Gift Claim: Gift types, claim frequency, safety settings\n" +
            "• Auto Building & Upgrades: Building priority, resource thresholds\n" +
            "• Auto Troop Training: Troop composition, training priority\n" +
            "• Auto Daily Tasks: Task types, execution timing\n\n" +
            "Note: Auto Start Game runs automatically and doesn't require configuration.\n" +
            "It will start the game when the instance launches."
        );
        settingsInfo.setEditable(false);
        settingsInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        settingsInfo.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        panel.add(settingsInfo, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));

        JButton testBtn = new JButton("🧪 Test Configuration");
        testBtn.setToolTipText("Preview how modules will execute");
        testBtn.addActionListener(this::testConfiguration);

        JButton saveBtn = new JButton("💾 Save Configuration");
        saveBtn.setBackground(new Color(34, 139, 34));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(this::saveConfiguration);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        panel.add(testBtn);
        panel.add(cancelBtn);
        panel.add(saveBtn);

        return panel;
    }

    private void loadCurrentSettings() {
        // Load existing module states
        Map<String, ModuleState<?>> instanceModules = Main.instanceModules
            .getOrDefault(instance.index, new HashMap<>());

        // Set checkboxes based on enabled modules
        for (String moduleName : MODULE_DESCRIPTIONS.keySet()) {
            JCheckBox checkbox = moduleCheckboxes.get(moduleName);
            ModuleState<?> moduleState = instanceModules.get(moduleName);
            
            if (checkbox != null) {
                checkbox.setSelected(moduleState != null && moduleState.enabled);
            }
        }

        // Load priority order (if exists)
        updatePriorityList();

        // Set default execution settings
        executionModeCombo.setSelectedIndex(0); // Sequential
        delaySpinner.setValue(5);
        retryFailedCheckbox.setSelected(true);

        statusLabel.setText("Configuration loaded for " + instance.name);
    }

    private void updatePriorityList() {
        priorityListModel.clear();
        int priority = 1;

        // Add enabled modules to priority list
        for (Map.Entry<String, String> entry : MODULE_DESCRIPTIONS.entrySet()) {
            String moduleName = entry.getKey();
            JCheckBox checkbox = moduleCheckboxes.get(moduleName);
            
            if (checkbox != null && checkbox.isSelected()) {
                priorityListModel.addElement(new ModuleItem(moduleName, priority++));
            }
        }

        statusLabel.setText("Priority list updated - " + priorityListModel.size() + " modules enabled");
    }

    private void movePriorityItem(int direction) {
        int selectedIndex = priorityList.getSelectedIndex();
        if (selectedIndex == -1) {
            statusLabel.setText("Select a module to move");
            return;
        }

        int newIndex = selectedIndex + direction;
        if (newIndex >= 0 && newIndex < priorityListModel.size()) {
            ModuleItem item = priorityListModel.remove(selectedIndex);
            priorityListModel.add(newIndex, item);
            priorityList.setSelectedIndex(newIndex);
            
            // Update priority numbers
            for (int i = 0; i < priorityListModel.size(); i++) {
                priorityListModel.getElementAt(i).priority = i + 1;
            }
            
            priorityList.repaint();
            statusLabel.setText("Moved " + item.moduleName + " " + (direction < 0 ? "up" : "down"));
        }
    }

    private void removePriorityItem() {
        int selectedIndex = priorityList.getSelectedIndex();
        if (selectedIndex == -1) {
            statusLabel.setText("Select a module to remove");
            return;
        }

        ModuleItem item = priorityListModel.remove(selectedIndex);
        
        // Uncheck the corresponding checkbox
        JCheckBox checkbox = moduleCheckboxes.get(item.moduleName);
        if (checkbox != null) {
            checkbox.setSelected(false);
        }
        
        // Update priority numbers
        for (int i = 0; i < priorityListModel.size(); i++) {
            priorityListModel.getElementAt(i).priority = i + 1;
        }
        
        priorityList.repaint();
        statusLabel.setText("Removed " + item.moduleName + " from execution list");
    }

    private boolean hasConfiguration(String moduleName) {
        // Modules that have detailed configuration options
        return !moduleName.equals("Auto Start Game");
    }

    private void configureModule(String moduleName) {
        switch (moduleName) {
            case "Auto Gather Resources":
                configureAutoGather();
                break;
            case "Auto Gift Claim":
                configureAutoGift();
                break;
            case "Auto Building & Upgrades":
                configureAutoBuilding();
                break;
            case "Auto Troop Training":
                configureAutoTroop();
                break;
            case "Auto Daily Tasks":
                configureAutoDailyTasks();
                break;
            default:
                JOptionPane.showMessageDialog(this,
                    "Configuration for " + moduleName + " is not yet implemented.",
                    "Configuration Not Available",
                    JOptionPane.INFORMATION_MESSAGE);
                break;
        }
    }

    private void configureAutoGather() {
        // Use the existing detailed configuration or create a simple one
        AutoGatherConfigDialog dialog = new AutoGatherConfigDialog(this, instance);
        dialog.setVisible(true);
        statusLabel.setText("Auto Gather Resources configured");
    }

    private void configureAutoGift() {
        JOptionPane.showMessageDialog(this,
            "Auto Gift Claim Configuration:\n\n" +
            "• Daily login gifts: Enabled\n" +
            "• Mail rewards: Enabled\n" +
            "• Event bonuses: Enabled\n" +
            "• Check frequency: Every 2 hours\n\n" +
            "Full configuration coming soon!",
            "Auto Gift Claim Configuration",
            JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText("Auto Gift Claim configured (basic settings)");
    }

    private void configureAutoBuilding() {
        JOptionPane.showMessageDialog(this,
            "Auto Building Configuration:\n\n" +
            "• Priority: Resource buildings first\n" +
            "• Start when: 75% storage capacity\n" +
            "• Max queue: 2 buildings\n" +
            "• Upgrade existing: Yes\n\n" +
            "Full configuration coming soon!",
            "Auto Building Configuration",
            JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText("Auto Building configured (basic settings)");
    }

    private void configureAutoTroop() {
        JOptionPane.showMessageDialog(this,
            "Auto Troop Training Configuration:\n\n" +
            "• Composition: Balanced army\n" +
            "• Resource allocation: 30%\n" +
            "• Training priority: Highest tier\n" +
            "• Train during gathering: Yes\n\n" +
            "Full configuration coming soon!",
            "Auto Troop Training Configuration",
            JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText("Auto Troop Training configured (basic settings)");
    }

    private void configureAutoDailyTasks() {
        JOptionPane.showMessageDialog(this,
            "Auto Daily Tasks Configuration:\n\n" +
            "• Daily quests: Enabled\n" +
            "• VIP rewards: Enabled\n" +
            "• Alliance help: Enabled\n" +
            "• Timing: Between gathering cycles\n\n" +
            "Full configuration coming soon!",
            "Auto Daily Tasks Configuration",
            JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText("Auto Daily Tasks configured (basic settings)");
    }

    private void testConfiguration(ActionEvent e) {
        StringBuilder testResults = new StringBuilder();
        testResults.append("Module Configuration Test\n");
        testResults.append("=".repeat(50)).append("\n\n");

        testResults.append("📋 EXECUTION ORDER:\n");
        for (int i = 0; i < priorityListModel.size(); i++) {
            ModuleItem item = priorityListModel.getElementAt(i);
            testResults.append(String.format("  %d. %s\n", item.priority, item.moduleName));
        }

        testResults.append("\n⚡ EXECUTION SETTINGS:\n");
        testResults.append("  • Mode: ").append(executionModeCombo.getSelectedItem()).append("\n");
        testResults.append("  • Delay between modules: ").append(delaySpinner.getValue()).append(" seconds\n");
        testResults.append("  • Retry failed modules: ").append(retryFailedCheckbox.isSelected() ? "Yes" : "No").append("\n");

        testResults.append("\n🚀 SIMULATION:\n");
        testResults.append("When you start ").append(instance.name).append(", it will:\n");
        
        int step = 1;
        for (int i = 0; i < priorityListModel.size(); i++) {
            ModuleItem item = priorityListModel.getElementAt(i);
            testResults.append(String.format("%d. Execute %s\n", step++, item.moduleName));
            if (i < priorityListModel.size() - 1) {
                testResults.append(String.format("   (wait %d seconds)\n", (Integer)delaySpinner.getValue()));
            }
        }

        if (priorityListModel.size() == 0) {
            testResults.append("❌ No modules enabled - instance will only start but won't automate anything.\n");
        }

        JTextArea resultArea = new JTextArea(testResults.toString());
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane,
            "Configuration Test - " + instance.name,
            JOptionPane.INFORMATION_MESSAGE);

        statusLabel.setText("Configuration test completed");
    }

    private void saveConfiguration(ActionEvent e) {
        try {
            // Get or create modules map for this instance
            Map<String, ModuleState<?>> modules = Main.instanceModules
                .getOrDefault(instance.index, new HashMap<>());

            // Save individual module states
            for (Map.Entry<String, JCheckBox> entry : moduleCheckboxes.entrySet()) {
                String moduleName = entry.getKey();
                boolean enabled = entry.getValue().isSelected();
                
                // Preserve existing settings if module already exists
                ModuleState<?> existingModule = modules.get(moduleName);
                Object settings = existingModule != null ? existingModule.settings : null;
                
                modules.put(moduleName, new ModuleState<>(enabled, settings));
            }

            // Save execution priority order
            StringBuilder priorityOrder = new StringBuilder();
            for (int i = 0; i < priorityListModel.size(); i++) {
                ModuleItem item = priorityListModel.getElementAt(i);
                if (priorityOrder.length() > 0) priorityOrder.append(",");
                priorityOrder.append(item.moduleName);
            }

            // Save execution settings
            String executionSettings = String.format(
                "Mode:%s;Delay:%d;RetryFailed:%s;Priority:%s",
                executionModeCombo.getSelectedIndex(),
                delaySpinner.getValue(),
                retryFailedCheckbox.isSelected(),
                priorityOrder.toString()
            );

            modules.put("Module Execution Settings", new ModuleState<>(true, executionSettings));

            // Save to main modules map
            Main.instanceModules.put(instance.index, modules);
            main.saveSettings();

            // Show success message
            int enabledCount = 0;
            for (JCheckBox cb : moduleCheckboxes.values()) {
                if (cb.isSelected()) enabledCount++;
            }

            String successMessage = String.format(
                "Configuration saved successfully!\n\n" +
                "Instance: %s\n" +
                "Modules enabled: %d\n" +
                "Execution mode: %s\n" +
                "Module delay: %d seconds\n\n" +
                "To activate these settings, start the instance using the\n" +
                "'Start Selected' button in the main window.",
                instance.name,
                enabledCount,
                executionModeCombo.getSelectedItem().toString().split(" - ")[0],
                delaySpinner.getValue()
            );

            JOptionPane.showMessageDialog(this, successMessage,
                "Settings Saved",
                JOptionPane.INFORMATION_MESSAGE);

            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error saving configuration: " + ex.getMessage(),
                "Save Error",
                JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Save failed: " + ex.getMessage());
        }
    }

    // Helper classes
    private static class ModuleItem {
        String moduleName;
        int priority;

        ModuleItem(String moduleName, int priority) {
            this.moduleName = moduleName;
            this.priority = priority;
        }

        @Override
        public String toString() {
            return String.format("%d. %s", priority, moduleName);
        }
    }

    private static class PriorityListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ModuleItem) {
                ModuleItem item = (ModuleItem) value;
                
                // Color coding by priority
                if (item.priority == 1) {
                    setIcon(new ColorIcon(Color.GREEN));
                } else if (item.priority <= 3) {
                    setIcon(new ColorIcon(Color.ORANGE));
                } else {
                    setIcon(new ColorIcon(Color.GRAY));
                }
                
                // Add emoji for module type
                String emoji = getModuleEmoji(item.moduleName);
                setText(emoji + " " + item.toString());
            }
            
            return this;
        }
        
        private String getModuleEmoji(String moduleName) {
            if (moduleName.contains("Start Game")) return "🎮";
            if (moduleName.contains("Gather")) return "🌾";
            if (moduleName.contains("Gift")) return "🎁";
            if (moduleName.contains("Building")) return "🏗️";
            if (moduleName.contains("Troop")) return "⚔️";
            if (moduleName.contains("Daily")) return "📋";
            return "⚙️";
        }
    }

    private static class ColorIcon implements Icon {
        private final Color color;
        
        ColorIcon(Color color) {
            this.color = color;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillOval(x, y, getIconWidth(), getIconHeight());
            g.setColor(Color.BLACK);
            g.drawOval(x, y, getIconWidth(), getIconHeight());
        }
        
        @Override
        public int getIconWidth() { return 8; }
        
        @Override
        public int getIconHeight() { return 8; }
    }

    // Simplified Auto Gather Configuration Dialog
    private static class AutoGatherConfigDialog extends JDialog {
        private final MemuInstance instance;
        private JTextField resourceLoopField;
        private JSpinner maxQueuesSpinner;

        AutoGatherConfigDialog(Dialog parent, MemuInstance instance) {
            super(parent, "Auto Gather Configuration", true);
            this.instance = instance;
            initUI();
        }

        private void initUI() {
            setLayout(new BorderLayout());
            setSize(400, 200);
            setLocationRelativeTo(getParent());

            JPanel configPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0; gbc.gridy = 0;
            configPanel.add(new JLabel("Resource Loop:"), gbc);
            
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            resourceLoopField = new JTextField("Food,Wood,Stone,Iron", 20);
            configPanel.add(resourceLoopField, gbc);
            
            gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            configPanel.add(new JLabel("Max Queues:"), gbc);
            
            gbc.gridx = 1;
            maxQueuesSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 6, 1));
            configPanel.add(maxQueuesSpinner, gbc);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton saveBtn = new JButton("Save");
            JButton cancelBtn = new JButton("Cancel");
            
            saveBtn.addActionListener(e -> {
                saveAutoGatherSettings();
                dispose();
            });
            cancelBtn.addActionListener(e -> dispose());
            
            buttonPanel.add(saveBtn);
            buttonPanel.add(cancelBtn);

            add(configPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        private void saveAutoGatherSettings() {
            String resourceLoop = resourceLoopField.getText().trim();
            int maxQueues = (Integer) maxQueuesSpinner.getValue();
            
            String settingsString = "Loop:" + resourceLoop + ";Index:0;MaxQueues:" + maxQueues;
            
            Map<String, ModuleState<?>> modules = Main.instanceModules
                .getOrDefault(instance.index, new HashMap<>());
            modules.put("Auto Gather Resources", new ModuleState<>(true, settingsString));
            Main.instanceModules.put(instance.index, modules);
        }
    }
}