package newgame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Dialog for configuring system-wide settings
 */
public class SystemSettingsDialog extends JDialog {
    private final SystemSettings settings;
    private boolean wasModified = false;
    
    private JSpinner maxConcurrentSpinner;
    private JCheckBox autoRotationCheckbox;
    private JSpinner rotationIntervalSpinner;
    private JCheckBox hibernationCheckbox;
    private JCheckBox resourceMonitoringCheckbox;
    private JSpinner maxMemorySpinner;
    private JSpinner maxCpuSpinner;
    
    public SystemSettingsDialog(Frame parent, SystemSettings settings) {
        super(parent, "System Settings", true);
        this.settings = settings;
        initializeUI();
        loadCurrentSettings();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(500, 400);
        setLocationRelativeTo(getParent());
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Concurrent Instances Section
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(createSectionLabel("Instance Management"), gbc);
        
        gbc.gridwidth = 1; gbc.gridy++;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Max Concurrent Instances:"), gbc);
        gbc.gridx = 1;
        maxConcurrentSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        mainPanel.add(maxConcurrentSpinner, gbc);
        
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        autoRotationCheckbox = new JCheckBox("Enable automatic queue rotation");
        mainPanel.add(autoRotationCheckbox, gbc);
        
        gbc.gridy++; gbc.gridwidth = 1;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Rotation interval (hours):"), gbc);
        gbc.gridx = 1;
        rotationIntervalSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 24, 1));
        mainPanel.add(rotationIntervalSpinner, gbc);
        
        // Hibernation Section
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        mainPanel.add(createSectionLabel("Hibernation"), gbc);
        
        gbc.gridy++; gbc.gridwidth = 2;
        hibernationCheckbox = new JCheckBox("Enable smart hibernation");
        mainPanel.add(hibernationCheckbox, gbc);
        
        // Resource Monitoring Section
        gbc.gridy++; gbc.gridwidth = 2;
        mainPanel.add(createSectionLabel("Resource Monitoring"), gbc);
        
        gbc.gridy++; gbc.gridwidth = 2;
        resourceMonitoringCheckbox = new JCheckBox("Enable resource monitoring");
        mainPanel.add(resourceMonitoringCheckbox, gbc);
        
        gbc.gridy++; gbc.gridwidth = 1;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Max Memory Usage (GB):"), gbc);
        gbc.gridx = 1;
        maxMemorySpinner = new JSpinner(new SpinnerNumberModel(8.0, 1.0, 64.0, 0.5));
        mainPanel.add(maxMemorySpinner, gbc);
        
        gbc.gridy++; gbc.gridx = 0;
        mainPanel.add(new JLabel("Max CPU Usage (%):"), gbc);
        gbc.gridx = 1;
        maxCpuSpinner = new JSpinner(new SpinnerNumberModel(80.0, 10.0, 100.0, 5.0));
        mainPanel.add(maxCpuSpinner, gbc);
        
        // Help text
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH;
        JTextArea helpText = new JTextArea(
            "System Settings Help:\n\n" +
            "• Max Concurrent Instances: Limits how many instances run simultaneously\n" +
            "• Auto Rotation: Automatically cycles through queued instances\n" +
            "• Smart Hibernation: Stops instances during long waits to save resources\n" +
            "• Resource Monitoring: Tracks system usage and adjusts performance"
        );
        helpText.setEditable(false);
        helpText.setBackground(mainPanel.getBackground());
        helpText.setBorder(BorderFactory.createTitledBorder("Help"));
        mainPanel.add(helpText, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton saveButton = new JButton("Save Settings");
        saveButton.setBackground(new Color(34, 139, 34));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(this::saveSettings);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        
        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(this::resetToDefaults);
        
        buttonPanel.add(resetButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        return label;
    }
    
    private void loadCurrentSettings() {
        maxConcurrentSpinner.setValue(settings.maxConcurrentInstances);
        autoRotationCheckbox.setSelected(settings.autoRotationEnabled);
        rotationIntervalSpinner.setValue(settings.rotationIntervalHours);
        hibernationCheckbox.setSelected(settings.hibernationEnabled);
        resourceMonitoringCheckbox.setSelected(settings.resourceMonitoringEnabled);
        maxMemorySpinner.setValue(settings.maxMemoryUsageGB);
        maxCpuSpinner.setValue(settings.maxCpuUsagePercent);
    }
    
    private void saveSettings(ActionEvent e) {
        settings.maxConcurrentInstances = (Integer) maxConcurrentSpinner.getValue();
        settings.autoRotationEnabled = autoRotationCheckbox.isSelected();
        settings.rotationIntervalHours = (Integer) rotationIntervalSpinner.getValue();
        settings.hibernationEnabled = hibernationCheckbox.isSelected();
        settings.resourceMonitoringEnabled = resourceMonitoringCheckbox.isSelected();
        settings.maxMemoryUsageGB = (Double) maxMemorySpinner.getValue();
        settings.maxCpuUsagePercent = (Double) maxCpuSpinner.getValue();
        
        wasModified = true;
        
        JOptionPane.showMessageDialog(this,
            "Settings saved successfully!\n\n" +
            "Max Concurrent Instances: " + settings.maxConcurrentInstances + "\n" +
            "Auto Rotation: " + (settings.autoRotationEnabled ? "Enabled" : "Disabled") + "\n" +
            "Hibernation: " + (settings.hibernationEnabled ? "Enabled" : "Disabled"),
            "Settings Saved",
            JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
    }
    
    private void resetToDefaults(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(this,
            "Reset all settings to default values?",
            "Reset Settings",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            SystemSettings defaults = new SystemSettings();
            settings.maxConcurrentInstances = defaults.maxConcurrentInstances;
            settings.autoRotationEnabled = defaults.autoRotationEnabled;
            settings.rotationIntervalHours = defaults.rotationIntervalHours;
            settings.hibernationEnabled = defaults.hibernationEnabled;
            settings.resourceMonitoringEnabled = defaults.resourceMonitoringEnabled;
            settings.maxMemoryUsageGB = defaults.maxMemoryUsageGB;
            settings.maxCpuUsagePercent = defaults.maxCpuUsagePercent;
            
            loadCurrentSettings();
        }
    }
    
    public boolean wasModified() {
        return wasModified;
    }
}