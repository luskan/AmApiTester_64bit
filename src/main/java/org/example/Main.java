package org.example;

import com.sun.jna.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Main {
    // ---- JNA struct mappings ----
    public static class CVersionInfo extends Structure {
        public short ucMajorVersion;
        public short ucMinorVersion;
        public short ucMajorBuild;
        public short ucMinorBuild;
        public byte  ucPlatform;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "ucMajorVersion","ucMinorVersion","ucMajorBuild","ucMinorBuild","ucPlatform"
            );
        }
    }

    public static class ApiInitOptions extends Structure {
        public boolean bStartAmIfNotRunning;
        public int     dwTimeout;
        public Pointer pbProcessCreated;
        public boolean bFastStartMode;
        public boolean bKeepInBack;
        public byte[]  szInitLang = new byte[16];
        public byte[]  szMapPath  = new byte[512];
        public byte[]  szProfile  = new byte[256];

        public ApiInitOptions() {
            bStartAmIfNotRunning = true;
            dwTimeout = 60000;
            pbProcessCreated = null;
            bFastStartMode = false;
            bKeepInBack = false;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "bStartAmIfNotRunning","dwTimeout","pbProcessCreated",
                    "bFastStartMode","bKeepInBack",
                    "szInitLang","szMapPath","szProfile"
            );
        }
    }

    // ---- JNA interface ----
    public interface AmApi extends Library {
        void      GetAmApiVersion(CVersionInfo pv);
        boolean   GetAmVersion(CVersionInfo pv);
        boolean   GetAmPath(char[] path);
        void      SetAmApiRecieveTimeout(int timeout);
        boolean   AmApiInit(ApiInitOptions opts);
        void      AmApiDone();
        boolean   IsAmAndApiReady();
        boolean   GetAmCurrentLanguage(char[] lang);
        boolean   PostCommandToAm(String command, boolean bBeep);
        boolean   CloseAm(boolean bKillIfNotResponding);
        double    AmMetersToScale(int meters);
    }

    // ---- Fields for dynamic DLL selection ----
    private static String dllPath = "tpcAmApi.dll";
    private static AmApi api = null;
    private static boolean dllLoaded = false;
    private static String lastLoadError = null;

    private JTextArea output;
    private java.util.List<JButton> apiButtons = new java.util.ArrayList<>();

    private boolean tryLoadDll(String path) {
        try {
            api = Native.load(path, AmApi.class);
            dllLoaded = true;
            lastLoadError = null;
            return true;
        } catch (Throwable ex) {
            api = null;
            dllLoaded = false;
            lastLoadError = ex.getMessage();
            if (output != null) {
                append("ERROR: Failed to load DLL '" + path + "'");
                append("   Reason: " + lastLoadError);
            }
            return false;
        }
    }
    
    private void setButtonsEnabled(boolean enabled) {
        for (JButton button : apiButtons) {
            button.setEnabled(enabled);
        }
    }

    public Main() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ex) {
            // ignore; just means "fall back to default"
        }

        final JFrame frame = new JFrame("AutoMapa API Tester");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 900); // Doubled window size for HD displays

        // Create UI components
        output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18)); // Much larger font size for HD displays
        JScrollPane scroll = new JScrollPane(output);
        
        // Try to load the default DLL
        boolean initialDllLoaded = tryLoadDll(dllPath);
        
        if (!initialDllLoaded) {
            append("WARNING: Default DLL could not be loaded.");
            append("Please use the 'Select DLL...' button to choose a valid DLL file.");
            if (lastLoadError != null) {
                append("Error details: " + lastLoadError);
            }
        }

        // Show architecture info and JRE location at bottom
        String bits = System.getProperty("sun.arch.data.model");
        String info = "Running under " + bits + "-bit JRE";
        if ("64".equals(bits)) {
            info += " at " + System.getProperty("java.home");
        }
        final JTextField archField = new JTextField(info);
        archField.setEditable(false);
        archField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        // Status field for DLL loading
        final JTextField statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setForeground(initialDllLoaded ? Color.GREEN.darker() : Color.RED);
        statusField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20)); // Much larger, bold font for HD displays
        statusField.setText(initialDllLoaded ?
                "DLL loaded successfully: " + dllPath :
                "Failed to load DLL. Please select correct DLL path.");

        // Panel for buttons
        JPanel btnPanel = new JPanel(new GridLayout(0, 3, 10, 10)); // More columns and bigger gaps
        
        // Button: Select DLL
        JButton selectDllButton = makeButton("Select DLL...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select AutoMapa API DLL");

                // Filter to show only DLL files
                chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".dll");
                    }

                    public String getDescription() {
                        return "DLL Files (*.dll)";
                    }
                });

                // Start with the current DLL path
                chooser.setSelectedFile(new File(dllPath));

                int res = chooser.showOpenDialog(frame);
                if (res == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    dllPath = f.getAbsolutePath();
                    boolean success = tryLoadDll(dllPath);
                    if (success) {
                        append("DLL loaded successfully: " + dllPath);
                        statusField.setText("DLL loaded successfully: " + dllPath);
                        statusField.setForeground(Color.GREEN.darker());
                        setButtonsEnabled(true);
                    } else {
                        append("Failed to load DLL: " + dllPath);
                        statusField.setText("Failed to load DLL. Please select correct DLL path.");
                        statusField.setForeground(Color.RED);
                        setButtonsEnabled(false);
                    }
                }
            }
        });
        btnPanel.add(selectDllButton);

        // Button: Get API Version
        JButton apiVersionButton = makeButton("Get API Version", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CVersionInfo vi = new CVersionInfo();
                api.GetAmApiVersion(vi);
                append("API version: " + vi.ucMajorVersion + "." + vi.ucMinorVersion + "." + vi.ucMajorBuild + "." + vi.ucMinorBuild + " (platform=" + (vi.ucPlatform & 0xFF) + ")");
            }
        });
        btnPanel.add(apiVersionButton);
        apiButtons.add(apiVersionButton);

        // Button: Get installed AutoMapa version
        JButton amVersionButton = makeButton("Get AutoMapa Version", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CVersionInfo vi = new CVersionInfo();
                if (api.GetAmVersion(vi)) {
                    append("AutoMapa version: " + vi.ucMajorVersion + "." + vi.ucMinorVersion + "." + vi.ucMajorBuild + "." + vi.ucMinorBuild);
                } else {
                    append("AutoMapa not installed or call failed.");
                }
            }
        });
        btnPanel.add(amVersionButton);
        apiButtons.add(amVersionButton);

        // Button: Get AutoMapa install path
        JButton amPathButton = makeButton("Get AutoMapa Path", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                char[] buf = new char[512];
                if (api.GetAmPath(buf)) {
                    append("AutoMapa path: " + Native.toString(buf));
                } else {
                    append("Failed to get path.");
                }
            }
        });
        btnPanel.add(amPathButton);
        apiButtons.add(amPathButton);

        // Button: Initialize API
        JButton initApiButton = makeButton("Init API", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ApiInitOptions opts = new ApiInitOptions();
                if (api.AmApiInit(opts)) {
                    append("API initialized.");
                } else {
                    append("API init failed.");
                }
            }
        });
        btnPanel.add(initApiButton);
        apiButtons.add(initApiButton);

        // Button: Check readiness
        JButton isReadyButton = makeButton("Is API Ready?", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                append("API ready: " + api.IsAmAndApiReady());
            }
        });
        btnPanel.add(isReadyButton);
        apiButtons.add(isReadyButton);

        // Button: Done API
        JButton doneApiButton = makeButton("Done API", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                api.AmApiDone();
                append("API done.");
            }
        });
        btnPanel.add(doneApiButton);
        apiButtons.add(doneApiButton);

        // Button: Get current language
        JButton getLangButton = makeButton("Get Language", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                char[] lang = new char[16];
                if (api.GetAmCurrentLanguage(lang)) {
                    append("Language: " + Native.toString(lang));
                } else {
                    append("Failed to get language.");
                }
            }
        });
        btnPanel.add(getLangButton);
        apiButtons.add(getLangButton);

        // Button: Zoom to 1 km
        JButton zoomButton = makeButton("Zoom to 1 km", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                append("PostCommand: " + api.PostCommandToAm("showmap %lat %lon 1000", false));
            }
        });
        btnPanel.add(zoomButton);
        apiButtons.add(zoomButton);

        // Button: Close AutoMapa
        JButton closeButton = makeButton("Close AutoMapa", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                append("CloseAm: " + api.CloseAm(true));
            }
        });
        btnPanel.add(closeButton);
        apiButtons.add(closeButton);

        // Button: Meters→Scale (2 km)
        JButton scaleButton = makeButton("Meters -> Scale (2 km)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                append("Scale for 2000 m: " + api.AmMetersToScale(2000));
            }
        });
        btnPanel.add(scaleButton);
        apiButtons.add(scaleButton);

        // If the initial DLL loading failed, disable API buttons
        if (!initialDllLoaded) {
            setButtonsEnabled(false);
        }

        // Layout components
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusField, BorderLayout.CENTER);
        statusPanel.add(archField, BorderLayout.SOUTH);
        
        frame.getContentPane().add(btnPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);

        frame.pack();   // let layout do its job
        frame.setSize(1600, 900);    // larger size for HD displays

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    private JButton makeButton(String text, ActionListener act) {
        JButton b = new JButton(text);
        b.addActionListener(act);
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16)); // Larger font for buttons
        return b;
    }

    private void append(String line) {
        output.append(line + "\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Main();
            }
        });
    }
}