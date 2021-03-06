/*******************************************************************************
 * Any modification, copies of sections of this file must be attached with this
 * license and shown clearly in the developer's project. The code can be used
 * as long as you state clearly you do not own it. Any violation might result in
 *  a take-down.
 *
 * MIT License
 *
 * Copyright (c) 2016, 2017 Anthony Law
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.github.mob41.osumer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import com.github.mob41.organdebug.exceptions.DebuggableException;
import com.github.mob41.osumer.Config;
import com.github.mob41.osumer.Osumer;
import com.github.mob41.osumer.exceptions.NoBuildsForVersionException;
import com.github.mob41.osumer.exceptions.NoSuchBuildNumberException;
import com.github.mob41.osumer.exceptions.NoSuchVersionException;
import com.github.mob41.osumer.io.legacy.URLDownloader;
import com.github.mob41.osumer.io.queue.Queue;
import com.github.mob41.osumer.io.queue.QueueAction;
import com.github.mob41.osumer.io.queue.QueueManager;
import com.github.mob41.osumer.io.queue.actions.AfterSoundAction;
import com.github.mob41.osumer.io.queue.actions.BeatmapImportAction;
import com.github.mob41.osumer.io.queue.actions.BeforeSoundAction;
import com.github.mob41.osumer.io.queue.actions.CustomImportAction;
import com.github.mob41.osumer.io.queue.actions.UpdaterRunAction;
import com.github.mob41.osumer.sock.SockThread;
import com.github.mob41.osumer.updater.UpdateInfo;
import com.github.mob41.osumer.updater.Updater;
import com.github.mob41.osums.io.beatmap.OsuBeatmap;
import com.github.mob41.osums.io.beatmap.OsuDownloader;
import com.github.mob41.osums.io.beatmap.Osums;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.border.TitledBorder;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;

public class UIFrame extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = -4742856302707419966L;
    private boolean daemonMode = false;
    private final QueueManager mgr;
    private final Updater updater;
    private final Osums osu;
    private final SockThread sockThread;
    private final Config config;
    private final TrayIcon icon;
    private final Timer timer;
    
    private File selectedFile = null;
    private File selectedFolder = null;

    private JPanel contentPane;
    private QueueCellTableModel tableModel;
    private JTable table;
    private JTextField beatmapField;
    private JCheckBox chckbxShowBeatmapPreview;
    
    private boolean checkingUpdate = false;
    
    private String targetFile = "";
    private String targetFolder = "";

    /**
     * Create the frame.
     */
    public UIFrame(Config config, QueueManager mgr, TrayIcon icon) {
        this.icon = icon;
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                if (daemonMode) {
                    setVisible(false);
                    icon.displayMessage("osumer2", "osumer2 is now running in background.", TrayIcon.MessageType.INFO);
                } else {
                    dispose();
                    System.exit(0);
                    return;
                }
            }
        });
        this.mgr = mgr;
        this.config = config;
        this.osu = new Osums();
        this.updater = new Updater(config);

        mgr.startQueuing();

        setTitle("osumer");
        setIconImage(Toolkit.getDefaultToolkit()
                .getImage(UIFrame.class.getResource("/com/github/mob41/osumer/ui/osumerIcon_32px.png")));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setBounds(100, 100, 883, 578);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnOsumer2 = new JMenu("osumer2");
        menuBar.add(mnOsumer2);

        JMenuItem mntmViewDumps = new JMenuItem("View dumps");
        mntmViewDumps.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ViewDumpDialog dialog = new ViewDumpDialog();
                dialog.setModal(true);
                dialog.setVisible(true);
            }
        });
        mnOsumer2.add(mntmViewDumps);

        JMenuItem mntmPreferences = new JMenuItem("Preferences");
        mntmPreferences.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                PreferenceDialog dialog = new PreferenceDialog(config, UIFrame.this);
                dialog.setLocationRelativeTo(UIFrame.this);
                dialog.setModal(true);
                dialog.setVisible(true);
            }
        });
        
        JMenuItem mntmViewConfigurationLocation = new JMenuItem("Open configuration location");
        mntmViewConfigurationLocation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().open(new File(System.getenv("LOCALAPPDATA") + "\\osumerExpress"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        mnOsumer2.add(mntmViewConfigurationLocation);
        mnOsumer2.add(mntmPreferences);

        JSeparator separator = new JSeparator();
        mnOsumer2.add(separator);

        JMenuItem mntmExit = new JMenuItem("Exit");
        mntmExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (daemonMode) {
                    int option = JOptionPane.showOptionDialog(UIFrame.this,
                            "This application is running in daemon mode.\nExiting might slow down the osumerExpress background download speed.\n\nAre you sure? Or you may prefer running in background.",
                            "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                            new String[] { "Yes", "No", "Run in Background" }, JOptionPane.NO_OPTION);
                    if (option == JOptionPane.NO_OPTION || option == JOptionPane.CLOSED_OPTION) {
                        return;
                    } else if (option == 2) {
                        setVisible(false);
                        icon.displayMessage("osumer2", "osumer2 is now running in background.",
                                TrayIcon.MessageType.INFO);
                        return;
                    }
                }

                dispose();
                System.exit(0);
                return;
            }
        });
        
        JMenuItem mntmCheckForUpdates = new JMenuItem("Check for updates");
        mntmCheckForUpdates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                checkUpdate();
            }
        });
        mnOsumer2.add(mntmCheckForUpdates);
        
        JSeparator separator_1 = new JSeparator();
        mnOsumer2.add(separator_1);
        mnOsumer2.add(mntmExit);

        JMenu mnQueue = new JMenu("Queue");
        menuBar.add(mnQueue);

        JMenuItem mntmRefreshQueueList = new JMenuItem("Refresh queue list");
        mntmRefreshQueueList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                tableModel.fireTableDataChanged();
            }
        });

        JMenuItem mntmAddLegacyQueue = new JMenuItem("Add legacy queue");
        mntmAddLegacyQueue.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String input = JOptionPane.showInputDialog("Specify URL to download:");

                URL url = null;
                try {
                    url = new URL(input);
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(UIFrame.this, "Error: Invalid URL", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                mgr.addQueue(new Queue("Legacy download",
                        new URLDownloader("C:\\Users\\Anthony\\Desktop\\Unknown", "legacy.test", url), null, null,
                        null));
                tableModel.fireTableDataChanged();
            }
        });
        mnQueue.add(mntmAddLegacyQueue);

        JMenuItem mntmEditQueues = new JMenuItem("Edit queues");
        mntmEditQueues.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EditQueueDialog dialog = new EditQueueDialog(mgr);
                dialog.setModal(true);
                dialog.setVisible(true);
            }
        });
        mnQueue.add(mntmEditQueues);
        mnQueue.add(mntmRefreshQueueList);
        
        JMenu mnAbout = new JMenu("About");
        menuBar.add(mnAbout);
        
        JMenuItem mntmGithubProject = new JMenuItem("GitHub Project");
        mntmGithubProject.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URL("https://github.com/mob41/osumer").toURI());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        mnAbout.add(mntmGithubProject);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        JLabel lblImg = new JLabel("");
        lblImg.setIcon(new ImageIcon(UIFrame.class.getResource("/com/github/mob41/osumer/ui/osumerIcon_64px.png")));

        JLabel lblTitle = new JLabel("osumer2");
        lblTitle.setVerticalAlignment(SwingConstants.BOTTOM);
        lblTitle.setFont(new Font("Tahoma", Font.PLAIN, 27));

        tab = new JTabbedPane(JTabbedPane.TOP);

        JLabel lblCopyright = new JLabel("Copyright (c) 2016-2017 Anthony Law. Licensed under MIT License.");
        lblCopyright.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblCopyright.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel label = new JLabel("");
        
        JLabel lblTheEasiestExpress = new JLabel("The easiest, express way to obtain beatmaps");
        lblTheEasiestExpress.setHorizontalAlignment(SwingConstants.LEFT);
        lblTheEasiestExpress.setFont(new Font("Tahoma", Font.PLAIN, 12));
        
        lblUpdateStatus = new JLabel("");
        lblUpdateStatus.setHorizontalAlignment(SwingConstants.RIGHT);
        lblUpdateStatus.setFont(new Font("Tahoma", Font.PLAIN, 12));
        GroupLayout gl_contentPane = new GroupLayout(contentPane);
        gl_contentPane.setHorizontalGroup(
            gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                        .addComponent(tab)
                        .addGroup(gl_contentPane.createSequentialGroup()
                            .addComponent(lblImg)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                                .addGroup(gl_contentPane.createSequentialGroup()
                                    .addComponent(lblTheEasiestExpress)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(lblUpdateStatus, GroupLayout.DEFAULT_SIZE, 436, Short.MAX_VALUE))
                                .addGroup(gl_contentPane.createSequentialGroup()
                                    .addGap(255)
                                    .addComponent(label))
                                .addComponent(lblTitle, GroupLayout.DEFAULT_SIZE, 691, Short.MAX_VALUE)))
                        .addComponent(lblCopyright, GroupLayout.DEFAULT_SIZE, 761, Short.MAX_VALUE))
                    .addGap(0))
        );
        gl_contentPane.setVerticalGroup(
            gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_contentPane.createSequentialGroup()
                            .addComponent(lblTitle)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(lblUpdateStatus, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblTheEasiestExpress, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                                .addComponent(label)))
                        .addComponent(lblImg))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(tab, GroupLayout.PREFERRED_SIZE, 366, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.UNRELATED)
                    .addComponent(lblCopyright))
        );

        JPanel panel = new JPanel();
        tab.addTab("Quick start", null, panel, null);

        JLabel lblBeatmapUrl = new JLabel("Beatmap URL:");
        lblBeatmapUrl.setFont(new Font("Tahoma", Font.PLAIN, 21));

        beatmapField = new JTextField();
        beatmapField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent arg0) {
                String text = beatmapField.getText();
                if (Osums.isVaildBeatMapUrl(text)){
                    beatmapField.setBackground(Color.WHITE);
                    beatmapField.setForeground(Color.BLACK);
                } else {
                    beatmapField.setBackground(Color.PINK);
                    beatmapField.setForeground(Color.WHITE);
                }
            }
        });
        beatmapField.setFont(new Font("Tahoma", Font.PLAIN, 18));
        beatmapField.setColumns(10);

        JButton btnAddToQueue = new JButton("Add to queue");
        btnAddToQueue.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!Osums.isVaildBeatMapUrl(beatmapField.getText())){
                    JOptionPane.showMessageDialog(UIFrame.this, "Please enter a valid song/beatmap URL.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                addBtQueue(beatmapField.getText(), chckbxShowBeatmapPreview.isSelected());
            }

        });
        btnAddToQueue.setFont(new Font("Tahoma", Font.PLAIN, 15));

        JLabel lblSpecifyYourDesired = new JLabel(
                "Specify your desired osu! beatmap URL to download. (e.g. http://osu.ppy.sh/s/320118)");
        lblSpecifyYourDesired.setFont(new Font("Tahoma", Font.PLAIN, 12));

        JLabel lblNewWebpageUrls = new JLabel(
                "New web links with more than 6 digits (e.g. https://osu.ppy.sh/b/1483372) are currently not supported.");
        lblNewWebpageUrls.setFont(new Font("Tahoma", Font.PLAIN, 12));
        
        ButtonGroup dwnSelectionBtnGp = new ButtonGroup();

        JButton btnOsumerPreferences = new JButton("osumer2 Preferences");
        btnOsumerPreferences.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreferenceDialog dialog = new PreferenceDialog(config, UIFrame.this);
                dialog.setLocationRelativeTo(UIFrame.this);
                dialog.setModal(true);
                dialog.setVisible(true);
            }
        });
        btnOsumerPreferences.setFont(new Font("Tahoma", Font.PLAIN, 18));

        chckbxShowBeatmapPreview = new JCheckBox("Show beatmap preview");
        chckbxShowBeatmapPreview.setSelected(true);
        chckbxShowBeatmapPreview.setFont(new Font("Tahoma", Font.PLAIN, 12));
        
        JButton btnDownloadAList = new JButton("Download a list of beatmaps");
        btnDownloadAList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MultiDownloadDialog d = new MultiDownloadDialog(UIFrame.this);
                d.setLocationRelativeTo(UIFrame.this);
                d.setModal(true);
                d.setVisible(true);
            }
        });
        btnDownloadAList.setFont(new Font("Tahoma", Font.PLAIN, 18));
        
        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, "Beatmap Import Actions", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
                        .addGroup(Alignment.TRAILING, gl_panel.createSequentialGroup()
                            .addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
                                .addComponent(lblSpecifyYourDesired, GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE)
                                .addGroup(gl_panel.createSequentialGroup()
                                    .addComponent(lblBeatmapUrl)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(beatmapField, GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(btnAddToQueue, GroupLayout.PREFERRED_SIZE, 151, GroupLayout.PREFERRED_SIZE))
                                .addComponent(lblNewWebpageUrls, GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE))
                            .addContainerGap())
                        .addGroup(Alignment.TRAILING, gl_panel.createSequentialGroup()
                            .addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
                                .addComponent(btnDownloadAList, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE)
                                .addComponent(btnOsumerPreferences, GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE))
                            .addContainerGap())
                        .addGroup(gl_panel.createSequentialGroup()
                            .addComponent(panel_1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addContainerGap())))
                .addGroup(Alignment.TRAILING, gl_panel.createSequentialGroup()
                    .addGap(10)
                    .addComponent(chckbxShowBeatmapPreview, GroupLayout.DEFAULT_SIZE, 826, Short.MAX_VALUE)
                    .addContainerGap())
        );
        gl_panel.setVerticalGroup(
            gl_panel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_panel.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(btnAddToQueue, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
                        .addComponent(beatmapField, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblBeatmapUrl, Alignment.LEADING))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(lblSpecifyYourDesired)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(lblNewWebpageUrls)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(panel_1, GroupLayout.PREFERRED_SIZE, 145, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(chckbxShowBeatmapPreview)
                    .addGap(33)
                    .addComponent(btnOsumerPreferences)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(btnDownloadAList, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
        );
        
        rdbtnUseDefaultSettings = new JRadioButton("Use default settings in Preferences");
        rdbtnUseDefaultSettings.setSelected(true);
        
        rdbtnDownloadAndImport = new JRadioButton("Download and Import (Recommended)");
        
        rdbtnDownloadToOsu = new JRadioButton("Download to osu! Song folder (Press F5 in osu! to load)");
        
        rdbtnDownloadToFile = new JRadioButton("Download to file");
        
        rdbtnDownloadToFolder = new JRadioButton("Download to folder");
        
        ButtonGroup downloadSettingsGroup = new ButtonGroup();
        downloadSettingsGroup.add(rdbtnUseDefaultSettings);
        downloadSettingsGroup.add(rdbtnDownloadAndImport);
        downloadSettingsGroup.add(rdbtnDownloadToOsu);
        downloadSettingsGroup.add(rdbtnDownloadToFile);
        downloadSettingsGroup.add(rdbtnDownloadToFolder);
        
        JButton btnSelectFile = new JButton("Select file");
        btnSelectFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                JFileChooser c = new JFileChooser();
                c.setFileSelectionMode(JFileChooser.FILES_ONLY);
                
                File file = new File(targetFile);
                c.setSelectedFile(file);
                
                int opt = c.showSaveDialog(UIFrame.this);
                if (opt == JFileChooser.APPROVE_OPTION) {
                    File sf = c.getSelectedFile();
                    targetFile = sf.getAbsolutePath();
                }
            }
        });
        
        JButton btnSelectFolder = new JButton("Select folder");
        btnSelectFolder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser c = new JFileChooser();
                c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                
                File file = new File(targetFolder);
                c.setSelectedFile(file);
                
                int opt = c.showSaveDialog(UIFrame.this);
                if (opt == JFileChooser.APPROVE_OPTION) {
                    File sf = c.getSelectedFile();
                    targetFolder = sf.getAbsolutePath();
                }
            }
        });
        GroupLayout gl_panel_1 = new GroupLayout(panel_1);
        gl_panel_1.setHorizontalGroup(
            gl_panel_1.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel_1.createSequentialGroup()
                    .addGroup(gl_panel_1.createParallelGroup(Alignment.LEADING)
                        .addComponent(rdbtnUseDefaultSettings)
                        .addComponent(rdbtnDownloadAndImport)
                        .addComponent(rdbtnDownloadToOsu)
                        .addGroup(gl_panel_1.createSequentialGroup()
                            .addComponent(rdbtnDownloadToFile)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(btnSelectFile))
                        .addGroup(gl_panel_1.createSequentialGroup()
                            .addComponent(rdbtnDownloadToFolder)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(btnSelectFolder)))
                    .addContainerGap(529, Short.MAX_VALUE))
        );
        gl_panel_1.setVerticalGroup(
            gl_panel_1.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_panel_1.createSequentialGroup()
                    .addComponent(rdbtnUseDefaultSettings)
                    .addComponent(rdbtnDownloadAndImport)
                    .addComponent(rdbtnDownloadToOsu)
                    .addGroup(gl_panel_1.createParallelGroup(Alignment.BASELINE)
                        .addComponent(rdbtnDownloadToFile)
                        .addComponent(btnSelectFile))
                    .addGroup(gl_panel_1.createParallelGroup(Alignment.BASELINE)
                        .addComponent(rdbtnDownloadToFolder)
                        .addComponent(btnSelectFolder)))
        );
        panel_1.setLayout(gl_panel_1);
        panel.setLayout(gl_panel);

        JPanel queuePanel = new JPanel();
        tab.addTab("Queue", null, queuePanel, null);
        queuePanel.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        queuePanel.add(scrollPane, BorderLayout.CENTER);

        tableModel = new QueueCellTableModel(mgr);

        table = new JTable(tableModel);
        table.setDefaultRenderer(Queue.class, new QueueCellRenderer(tableModel));
        table.setDefaultEditor(Queue.class, new QueueCellEditor(tableModel));
        table.setRowHeight(153);

        timer = new Timer(100, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                tableModel.fireTableDataChanged();

            }

        });
        timer.start();

        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem mntmStart = new JMenuItem("Start");
        popupMenu.add(mntmStart);

        JMenuItem mntmPause = new JMenuItem("Pause");
        popupMenu.add(mntmPause);

        JMenuItem mntmResume = new JMenuItem("Resume");
        popupMenu.add(mntmResume);

        JMenuItem mntmCancel = new JMenuItem("Cancel");
        mntmCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = table.getSelectedRow();
                if (index != -1) {
                    Queue queue = (Queue) table.getValueAt(index, 0);
                    mgr.removeQueue(queue);
                    tableModel.fireTableDataChanged();
                }
            }
        });
        popupMenu.add(mntmCancel);
        // timer.start();

        scrollPane.setViewportView(table);
        contentPane.setLayout(gl_contentPane);
        
        JPanel panel_2 = new JPanel();
        tab.addTab("Manage beatmaps", null, panel_2, null);
        tab.setEnabledAt(2, false);
        
        tab.add("Search beatmaps", new BeatmapSearchPanel(this, osu));

        this.sockThread = new SockThread(this);
        sockThread.start();
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                checkUpdate();
                if (config.isShowGettingStartedOnStartup()) {
                    GettingStartedDialog gsd = new GettingStartedDialog();
                    gsd.setLocationRelativeTo(UIFrame.this);
                    gsd.setModal(true);
                    gsd.setVisible(true);
                }
            }
        });
    }
    
    public Config getConfig() {
        return config;
    }

    public Osums getOsums() {
        return osu;
    }
    
    public JTabbedPane getTab() {
        return tab;
    }

    public QueueManager getQueueManager() {
        return mgr;
    }

    private OsuBeatmap map = null;
    private BufferedImage thumb = null;
    private ProgressDialog pbd = null;
    private JTabbedPane tab;
    private JLabel lblUpdateStatus;
    private JRadioButton rdbtnUseDefaultSettings;
    private JRadioButton rdbtnDownloadAndImport;
    private JRadioButton rdbtnDownloadToOsu;
    private JRadioButton rdbtnDownloadToFile;
    private JRadioButton rdbtnDownloadToFolder;

    public boolean addBtQueue(String url, boolean preview) {
        return addBtQueue(url, preview, true, null, null);
    }
    
    public boolean addQuietBtQueue(String url) {
        if (config.getCheckUpdateFreq() == Config.CHECK_UPDATE_FREQ_EVERY_ACT) {
            checkUpdate();
        }
        
        String user = config.getUser();
        String pass = config.getPass();

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            LoginPanel loginPanel = new LoginPanel();
            int option = JOptionPane.showOptionDialog(UIFrame.this, loginPanel, "Login to osu!",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
                    JOptionPane.CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                if (loginPanel.getUsername().isEmpty() || loginPanel.getPassword().isEmpty()) {
                    JOptionPane.showMessageDialog(UIFrame.this, "Username or password cannot be empty.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                user = loginPanel.getUsername();
                pass = loginPanel.getPassword();
            } else {
                return false;
            }
        }
        
        try {
            osu.login(user, pass);
        } catch (DebuggableException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(UIFrame.this, "Error logging in:\n" + e.getDump().getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        try {
            map = osu.getBeatmapInfo(url);
        } catch (DebuggableException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(UIFrame.this,
                    "Error getting beatmap info:\n" + e.getDump().getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        String modUrl = map.getThumbUrl();
        URL thumbUrl = null;
        try {
            thumbUrl = new URL("http:" + modUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

        URLConnection conn = null;
        try {
            conn = thumbUrl.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try {
            thumb = ImageIO.read(conn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            thumb = null;
        }
        
        URL downloadUrl = null;
        try {
            downloadUrl = new URL("http://osu.ppy.sh" + map.getDwnUrl());
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(UIFrame.this, "Error validating download URL:\n" + e1, "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String tmpdir = System.getProperty("java.io.tmpdir");

        final String mapName = map.getName();
        OsuDownloader dwn = new OsuDownloader(tmpdir,
                map.getDwnUrl().substring(3, map.getDwnUrl().length()) + " " + map.getName(), osu, downloadUrl);
        
        QueueAction importAction;
        if (rdbtnUseDefaultSettings.isSelected()) {
            importAction = new BeatmapImportAction(config);
        } else {
            int action = -1;
            String targetFileOrFolder = null;
            
            if (rdbtnDownloadAndImport.isSelected()) {
                action = 0;
            } else if (rdbtnDownloadToOsu.isSelected()) {
                action = 1;
            } else if (rdbtnDownloadToFile.isSelected()) {
                action = 2;
                targetFileOrFolder = targetFile;
            } else if (rdbtnDownloadToFolder.isSelected()) {
                action = 3;
                targetFileOrFolder = targetFolder;
            }
            
            importAction = new CustomImportAction(action, targetFileOrFolder);
        }

        QueueAction[] beforeActions = new QueueAction[] {
                new BeforeSoundAction(config)
        };
        
        QueueAction[] afterActions = new QueueAction[] { 
                new AfterSoundAction(config),
                new QueueAction(){

                    @Override
                    public void run(Queue queue) {
                        icon.displayMessage("Download completed for \"" + mapName + "\"", "This osumer queue has completed downloading.", TrayIcon.MessageType.INFO);
                    }
                    
                },
                importAction
        };
        
        boolean added = mgr.addQueue(new Queue(map.getName(), dwn, thumb, beforeActions, afterActions));
        
        if (added){
            icon.displayMessage("Downloading \"" + mapName + "\"", "osumerExpress is downloading the requested beatmap!", TrayIcon.MessageType.INFO);
        } else {
            icon.displayMessage("Could not add \"" + mapName + "\" to queue", "It has already in queue/downloading or completed.", TrayIcon.MessageType.INFO);
        }
        
        tableModel.fireTableDataChanged();
        
        return true;
    }

    public boolean addBtQueue(String url, boolean preview, boolean changeTab, QueueAction[] beforeActions, QueueAction[] afterActions) {
        if (config.getCheckUpdateFreq() == Config.CHECK_UPDATE_FREQ_EVERY_ACT) {
            checkUpdate();
        }
        
        map = null;
        thumb = null;
        pbd = new ProgressDialog();

        new Thread() {
            public void run() {
                pbd.getProgressBar().setIndeterminate(true);
                pbd.getLabel().setText("Status: Getting configuration...");
                String user = config.getUser();
                String pass = config.getPass();

                if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
                    pbd.getLabel().setText("Status: Prompting username and password...");
                    LoginPanel loginPanel = new LoginPanel();
                    int option = JOptionPane.showOptionDialog(UIFrame.this, loginPanel, "Login to osu!",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
                            JOptionPane.CANCEL_OPTION);

                    if (option == JOptionPane.OK_OPTION) {
                        if (loginPanel.getUsername().isEmpty() || loginPanel.getPassword().isEmpty()) {
                            JOptionPane.showMessageDialog(UIFrame.this, "Username or password cannot be empty.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            pbd.dispose();
                            return;
                        }

                        user = loginPanel.getUsername();
                        pass = loginPanel.getPassword();
                    } else {
                        pbd.dispose();
                        return;
                    }
                }

                pbd.getLabel().setText("Status: Logging in...");
                try {
                    osu.login(user, pass);
                } catch (DebuggableException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(UIFrame.this, "Error logging in:\n" + e.getDump().getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    pbd.dispose();
                    return;
                }

                pbd.getLabel().setText("Status: Obtaining beatmap information...");
                try {
                    map = osu.getBeatmapInfo(url);
                } catch (DebuggableException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(UIFrame.this,
                            "Error getting beatmap info:\n" + e.getDump().getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    pbd.dispose();
                    return;
                }

                pbd.dispose();
                pbd = null;
            }
        }.start();

        pbd.setLocationRelativeTo(UIFrame.this);
        pbd.setModal(true);
        pbd.setVisible(true);

        if (map == null) {
            return false;
        }

        boolean stillDwn = true;

        if (preview) {
            BeatmapPreviewDialog bpd = new BeatmapPreviewDialog(map);
            bpd.setLocationRelativeTo(UIFrame.this);
            bpd.setModal(true);
            bpd.setVisible(true);

            stillDwn = bpd.isSelectedYes();
            thumb = bpd.getDownloadedImage();
        }

        if (thumb == null) {
            pbd = new ProgressDialog();

            new Thread() {
                public void run() {
                    pbd.getProgressBar().setIndeterminate(true);
                    pbd.getLabel().setText("Status: Downloading thumb image...");
                    URL url = null;
                    try {
                        url = new URL("http:" + map.getThumbUrl());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();

                        pbd.dispose();
                        return;
                    }

                    URLConnection conn = null;
                    try {
                        conn = url.openConnection();
                    } catch (IOException e) {
                        e.printStackTrace();

                        pbd.dispose();
                        return;
                    }

                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    try {
                        thumb = ImageIO.read(conn.getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();

                        pbd.dispose();
                        return;
                    }

                    pbd.dispose();
                }
            }.start();

            pbd.setLocationRelativeTo(UIFrame.this);
            pbd.setModal(true);
            pbd.setVisible(true);
        }

        if (stillDwn) {
            URL downloadUrl = null;
            try {
                downloadUrl = new URL("http://osu.ppy.sh" + map.getDwnUrl());
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(UIFrame.this, "Error validating download URL:\n" + e1, "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            String tmpdir = System.getProperty("java.io.tmpdir");

            final String mapName = map.getName();
            OsuDownloader dwn = new OsuDownloader(tmpdir,
                    map.getDwnUrl().substring(3, map.getDwnUrl().length()) + " " + map.getName(), osu, downloadUrl);
            
            QueueAction importAction;
            if (rdbtnUseDefaultSettings.isSelected()) {
                importAction = new BeatmapImportAction(config);
            } else {
                int action = -1;
                String targetFileOrFolder = null;
                
                if (rdbtnDownloadAndImport.isSelected()) {
                    action = 0;
                } else if (rdbtnDownloadToOsu.isSelected()) {
                    action = 1;
                } else if (rdbtnDownloadToFile.isSelected()) {
                    action = 2;
                    targetFileOrFolder = targetFile;
                } else if (rdbtnDownloadToFolder.isSelected()) {
                    action = 3;
                    targetFileOrFolder = targetFolder;
                }
                
                importAction = new CustomImportAction(action, targetFileOrFolder);
            }
            
            if (beforeActions == null) {
                beforeActions = new QueueAction[] {
                        new BeforeSoundAction(config)
                };
            }
            
            if (afterActions == null){
                afterActions = new QueueAction[] { 
                        new AfterSoundAction(config),
                        new QueueAction(){

                            @Override
                            public void run(Queue queue) {
                                icon.displayMessage("Download completed for \"" + mapName + "\"", "This osumer queue has completed downloading.", TrayIcon.MessageType.INFO);
                            }
                            
                        },
                        importAction
                };
            }
            
            boolean added = mgr.addQueue(new Queue(map.getName(), dwn, thumb, beforeActions, afterActions));
            
            if (added){
                icon.displayMessage("Downloading \"" + mapName + "\"", "osumerExpress is downloading the requested beatmap!", TrayIcon.MessageType.INFO);
            } else {
                icon.displayMessage("Could not add \"" + mapName + "\" to queue", "It has already in queue/downloading or completed.", TrayIcon.MessageType.INFO);
            }
            
            tableModel.fireTableDataChanged();

            if (changeTab) {
                tab.setSelectedIndex(1);
            }
        } else {
            icon.displayMessage("Could not download \"" + url + "\"", "Error occurred when finding beatmap. Start UI to see details.", TrayIcon.MessageType.INFO);
        }

        return stillDwn;
    }

    private UpdateInfo getUpdateInfoByConfig() throws DebuggableException {
        String algo = config.getCheckUpdateAlgo();
        if (algo.equals(Config.CHECK_UPDATE_ALGO_PER_VER_PER_BRANCH)) {
            return updater.getPerVersionPerBranchLatestVersion();
        } else if (algo.equals(Config.CHECK_UPDATE_ALGO_LATEST_VER_PER_BRANCH)) {
            return updater.getLatestVersion();
        } else { //TODO: Implement other algo
            return updater.getLatestVersion();
        }
    }
    
    public void checkUpdate() {
        if (checkingUpdate) {
            return;
        }
        
        checkingUpdate = true;
        Thread thread = new Thread() {
            public void run() {
                lblUpdateStatus.setForeground(Color.BLACK);
                lblUpdateStatus.setText("Checking for updates...");
                
                UpdateInfo verInfo = null;
                try {
                    verInfo = getUpdateInfoByConfig();
                } catch (NoBuildsForVersionException e){
                    lblUpdateStatus.setForeground(Color.RED);
                    lblUpdateStatus.setText("No builds available for the new version. See dump.");
                    checkingUpdate = false;
                    return;
                } catch (NoSuchVersionException e){
                    lblUpdateStatus.setForeground(Color.RED);
                    lblUpdateStatus.setText("No current version in the selected branch. See dump.");
                    JOptionPane.showMessageDialog(UIFrame.this, "We don't have version " + Osumer.OSUMER_VERSION + " in the current update branch\n\nPlease try another update branch (snapshot, beta, stable).", "Version not available", JOptionPane.INFORMATION_MESSAGE);
                    checkingUpdate = false;
                    return;
                } catch (NoSuchBuildNumberException e){
                    lblUpdateStatus.setForeground(Color.RED);
                    lblUpdateStatus.setText("This version has a invalid build number. See dump");
                    JOptionPane.showMessageDialog(UIFrame.this, 
                            "We don't have build number greater or equal to " + Osumer.OSUMER_BUILD_NUM + " in version " + Osumer.OSUMER_VERSION + ".\n" +
                            "If you are using a modified/development osumer,\n"
                            + " you can just ignore this message.\n" +
                            "If not, this might be the versions.json in GitHub goes wrong,\n"
                            + " post a new issue about this.", "Build not available", JOptionPane.WARNING_MESSAGE);
                    checkingUpdate = false;
                    return;
                } catch (DebuggableException e){
                    e.printStackTrace();
                    lblUpdateStatus.setForeground(Color.RED);
                    lblUpdateStatus.setText("Could not connect to update server.");
                    JOptionPane.showMessageDialog(UIFrame.this, "Could not connect to update server.", "Error", JOptionPane.ERROR_MESSAGE);
                    checkingUpdate = false;
                    return;
                }
                
                if (verInfo == null) {
                    lblUpdateStatus.setForeground(Color.RED);
                    lblUpdateStatus.setText("Could not obtain update info.");
                    JOptionPane.showMessageDialog(UIFrame.this, "Could not obtain update info.", "Error", JOptionPane.ERROR_MESSAGE);
                    checkingUpdate = false;
                    return;
                }
                
                if (verInfo.isThisVersion()) {
                    lblUpdateStatus.setForeground(Color.BLACK);
                    lblUpdateStatus.setText("You are running the latest version of osumer"
                            + " (" + verInfo.getVersion() + "-" + Updater.getBranchStr(verInfo.getBranch()) + "-" + verInfo.getBuildNum() + ")");
                    checkingUpdate = false;
                    return;
                }
                
                lblUpdateStatus.setForeground(new Color(0,153,0));
                lblUpdateStatus.setText(
                        (verInfo.isUpgradedVersion() ? "Upgrade" : "Update") +
                        " available! New version: " + verInfo.getVersion() +
                        "-" + Updater.getBranchStr(verInfo.getBranch()) +
                        "-b" + verInfo.getBuildNum());
                
                int option;
                String desc = verInfo.getDescription();
                if (desc == null){
                    option = JOptionPane.showOptionDialog(UIFrame.this,
                            "New " +
                            (verInfo.isUpgradedVersion() ? "upgrade" : "update") +
                            " available! New version:\n" + verInfo.getVersion() +
                            "-" + Updater.getBranchStr(verInfo.getBranch()) +
                            "-b" + verInfo.getBuildNum() + "\n\n" +
                            "Do you want to update it now?", "Update available", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, JOptionPane.NO_OPTION);
                } else {
                    option = JOptionPane.showOptionDialog(UIFrame.this,
                            "New " +
                            (verInfo.isUpgradedVersion() ? "upgrade" : "update") +
                            " available! New version:\n" + verInfo.getVersion() +
                            "-" + Updater.getBranchStr(verInfo.getBranch()) +
                            "-b" + verInfo.getBuildNum() + "\n\n" +
                            "Do you want to update it now?", "Update available", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Yes", "No", "Description/Changelog"}, JOptionPane.NO_OPTION);
                    
                    if (option == 2){
                        option = JOptionPane.showOptionDialog(UIFrame.this, new TextPanel(desc), "Update description/change-log", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, 0);
                    }
                }
                
                if (option == JOptionPane.YES_OPTION){
                    /*
                    try {
                        Desktop.getDesktop().browse(new URI(verInfo.getWebLink()));
                    } catch (IOException | URISyntaxException e) {
                        DebugDump dump = new DebugDump(
                                verInfo.getWebLink(),
                                "Show option dialog of updating osumer or not",
                                "Set checkingUpdate to false",
                                "(End of function / thread)",
                                "Error when opening the web page",
                                false,
                                e);
                        DumpManager.getInstance().addDump(dump);
                        DebugDump.showDebugDialog(dump);
                    }
                    */
                    try {
                        String updaterLink = Updater.getUpdaterLink();
                        
                        if (updaterLink == null){
                            System.out.println("No latest updater .exe defined! Falling back to legacy updater!");
                            updaterLink = Updater.LEGACY_UPDATER_JAR;
                        }
                        
                        URL url;
                        try {
                            url = new URL(updaterLink);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Error:\n" + e, "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        
                        final String folder = System.getProperty("java.io.tmpdir");
                        final String fileName = "osumer_updater_" + Calendar.getInstance().getTimeInMillis() + ".exe";
                        
                        mgr.addQueue(new Queue(
                                "osumer Updater",
                                new URLDownloader(folder, fileName, url),
                                null,
                                null,
                                new QueueAction[] {
                                        new UpdaterRunAction(folder + fileName)
                                })
                         );
                        tab.setSelectedIndex(1);
                        new Thread() {
                            public void run() {
                                JOptionPane.showMessageDialog(UIFrame.this, "The web updater will be downloaded and started very soon.", "Notice", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }.start();
                    } catch (DebuggableException e){
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error:\n" + e, "Error", JOptionPane.ERROR_MESSAGE);
                        checkingUpdate = false;
                    }
                }
                checkingUpdate = false;
            }
        };
        thread.start();
    }

    private static void addPopup(Component component, final JPopupMenu popup) {
        component.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            private void showMenu(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    public boolean isDaemonMode() {
        return daemonMode;
    }

    public void setDaemonMode(boolean daemonMode) {
        this.daemonMode = daemonMode;
    }
}
