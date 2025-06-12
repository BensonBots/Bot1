package newgame;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ModuleListDialog extends JDialog {
    private final Main main;
    private final MemuInstance instance;
    private final Map<String, JCheckBox> checkboxes = new LinkedHashMap<>();

    public ModuleListDialog(Main main, MemuInstance instance) {
        super(main, "Module Configuration - " + instance.name, true);
        this.main = main;
        this.instance = instance;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(450, 170);
        setLocationRelativeTo(getParent());

        JPanel modulePanel = new JPanel();
        modulePanel.setLayout(new BoxLayout(modulePanel, BoxLayout.Y_AXIS));
        modulePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // Get current module states
        Map<String, ModuleState<?>> instanceModules = Main.instanceModules
            .getOrDefault(instance.index, new HashMap<>());

        // Auto Start Game module (simple checkbox)
        JPanel autoStartPanel = createModuleRow("Auto Start Game", instanceModules, null);
        modulePanel.add(autoStartPanel);
        modulePanel.add(Box.createVerticalStrut(15));

        // Auto Gather Resources module (checkbox + configure button)
        JPanel autoGatherPanel = createModuleRow("Auto Gather Resources", instanceModules, 
            e -> configureAutoGather());
        modulePanel.add(autoGatherPanel);
        modulePanel.add(Box.createVerticalStrut(10));

        // Bottom buttons panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        
        JButton saveBtn = new JButton("Save & Close");
        saveBtn.setBackground(new Color(34, 139, 34));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(e -> saveSettings());

        bottomPanel.add(saveBtn);

        add(modulePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createModuleRow(String moduleName, Map<String, ModuleState<?>> instanceModules, 
                                  java.awt.event.ActionListener configureAction) {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Checkbox for the module
        JCheckBox cb = new JCheckBox(moduleName);
        cb.setSelected(instanceModules.containsKey(moduleName) && 
            instanceModules.get(moduleName).enabled);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        checkboxes.put(moduleName, cb);

        panel.add(cb, BorderLayout.WEST);

        // Add configure button if action is provided
        if (configureAction != null) {
            JButton configureBtn = new JButton("Configure");
            configureBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            configureBtn.setPreferredSize(new Dimension(85, 26));
            configureBtn.setBackground(new Color(70, 130, 180));
            configureBtn.setForeground(Color.WHITE);
            configureBtn.addActionListener(configureAction);
            
            panel.add(configureBtn, BorderLayout.EAST);
        }

        return panel;
    }

    /**
     * FIXED: Simple configure dialog that doesn't rely on non-existent methods
     */
    private void configureAutoGather() {
        System.out.println("🔧 Opening Auto Gather configuration for instance " + instance.index);
        
        // Get current settings
        Map<String, ModuleState<?>> instanceModules = Main.instanceModules
            .getOrDefault(instance.index, new HashMap<>());
        
        ModuleState<?> gatherModule = instanceModules.get("Auto Gather Resources");
        String currentResourceLoop = "Food,Wood,Stone,Iron";
        int currentMaxQueues = 6;
        
        if (gatherModule != null && gatherModule.settings != null) {
            try {
                // Try to parse existing settings if they exist
                String settingsStr = gatherModule.settings.toString();
                if (settingsStr.contains("Loop:")) {
                    String[] parts = settingsStr.split(";");
                    for (String part : parts) {
                        if (part.startsWith("Loop:")) {
                            currentResourceLoop = part.substring(5);
                        } else if (part.startsWith("MaxQueues:")) {
                            currentMaxQueues = Integer.parseInt(part.substring(10));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Failed to parse existing settings, using defaults: " + e.getMessage());
            }
        }
        
        // Create simple configuration dialog
        JDialog configDialog = new JDialog(this, "Auto Gather Configuration - " + instance.name, true);
        configDialog.setLayout(new BorderLayout());
        configDialog.setSize(500, 300);
        configDialog.setLocationRelativeTo(this);
        
        // Main configuration panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Resource Loop
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Resource Loop:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JTextField resourceLoopField = new JTextField(currentResourceLoop, 20);
        configPanel.add(resourceLoopField, gbc);
        
        // Max Queues
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        configPanel.add(new JLabel("Max Queues:"), gbc);
        
        gbc.gridx = 1;
        JSpinner maxQueuesSpinner = new JSpinner(new SpinnerNumberModel(currentMaxQueues, 1, 6, 1));
        configPanel.add(maxQueuesSpinner, gbc);
        
        // Help text
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        JTextArea helpText = new JTextArea(
            "Resource Loop: Comma-separated list of resources to gather\n" +
            "Examples: Food,Wood,Stone,Iron or Food,Wood\n\n" +
            "Max Queues: Maximum number of march queues to use (1-6)\n\n" +
            "The bot will cycle through the resource list and automatically\n" +
            "start gathering marches on available queues."
        );
        helpText.setEditable(false);
        helpText.setBackground(configPanel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        configPanel.add(helpText, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton saveConfigBtn = new JButton("Save Configuration");
        saveConfigBtn.setBackground(new Color(34, 139, 34));
        saveConfigBtn.setForeground(Color.WHITE);
        saveConfigBtn.addActionListener(e -> {
            // Save settings
            String newResourceLoop = resourceLoopField.getText().trim();
            int newMaxQueues = (Integer) maxQueuesSpinner.getValue();
            
            // Create settings string
            String settingsString = "Loop:" + newResourceLoop + ";Index:0;MaxQueues:" + newMaxQueues;
            
            // Save to module state
            Map<String, ModuleState<?>> modules = Main.instanceModules
                .getOrDefault(instance.index, new HashMap<>());
            modules.put("Auto Gather Resources", new ModuleState<>(true, settingsString));
            Main.instanceModules.put(instance.index, modules);
            main.saveSettings();
            
            System.out.println("✅ Auto gather settings saved: " + settingsString);
            JOptionPane.showMessageDialog(configDialog,
                "Settings saved successfully!\n\n" +
                "Resource Loop: " + newResourceLoop + "\n" +
                "Max Queues: " + newMaxQueues,
                "Settings Saved",
                JOptionPane.INFORMATION_MESSAGE);
            
            configDialog.dispose();
        });
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> configDialog.dispose());
        
        buttonPanel.add(saveConfigBtn);
        buttonPanel.add(cancelBtn);
        
        configDialog.add(configPanel, BorderLayout.CENTER);
        configDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        configDialog.setVisible(true);
    }

    /**
     * Save module settings and close dialog
     */
    private void saveSettings() {
        Map<String, ModuleState<?>> modules = new HashMap<>();
        checkboxes.forEach((name, cb) -> {
            ModuleState<?> current = Main.instanceModules
                .getOrDefault(instance.index, new HashMap<>())
                .get(name);
            modules.put(name, new ModuleState<>(cb.isSelected(), 
                current != null ? current.settings : null));
        });
        
        Main.instanceModules.put(instance.index, modules);
        main.saveSettings();
        
        // Simple confirmation
        boolean autoStartEnabled = checkboxes.get("Auto Start Game").isSelected();
        boolean autoGatherEnabled = checkboxes.get("Auto Gather Resources").isSelected();
        
        String message = "Settings saved for " + instance.name + "\n\n" +
                        "🎮 Auto Start Game: " + (autoStartEnabled ? "ENABLED" : "Disabled") + "\n" +
                        "🌾 Auto Gather Resources: " + (autoGatherEnabled ? "ENABLED" : "Disabled");
        
        JOptionPane.showMessageDialog(this, message, "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
    }
}