/*******************************************************************************
 * Copyright (c) 2001, 2008 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://robocode.sourceforge.net/license/cpl-v10.html
 *
 * Contributors:
 *     Mathew A. Nelson
 *     - Initial API and implementation
 *     Flemming N. Larsen
 *     - Added showInBrowser() for displaying content from an URL
 *     - Added showRoboWiki(), showYahooGroupRobocode(), showRobocodeRepository()
 *     - Removed the Thread.sleep(diff) from showSplashScreen()
 *     - Updated to use methods from the FileUtil, which replaces file operations
 *       that have been (re)moved from the robocode.util.Utils class
 *     - Changed showRobocodeFrame() to take a visible parameter
 *     - Added packCenterShow() for windows where the window position and
 *       dimension should not be read or saved to window.properties
 *     Luis Crespo & Flemming N. Larsen
 *     - Added showRankingDialog()
 *******************************************************************************/
package net.sf.robocode.ui;


import net.sf.robocode.battle.BattleProperties;
import net.sf.robocode.battle.BattleResultsTableModel;
import net.sf.robocode.battle.IBattleManager;
import net.sf.robocode.core.Container;
import net.sf.robocode.host.ICpuManager;
import net.sf.robocode.io.FileUtil;
import net.sf.robocode.repository.IRepositoryManager;
import net.sf.robocode.settings.ISettingsManager;
import net.sf.robocode.ui.battle.AwtBattleAdaptor;
import net.sf.robocode.ui.dialog.*;
import net.sf.robocode.ui.editor.RobocodeEditor;
import net.sf.robocode.ui.packager.RobotPackager;
import robocode.control.events.BattleCompletedEvent;
import robocode.control.events.IBattleListener;
import robocode.control.snapshot.ITurnSnapshot;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 * @author Luis Crespo (contributor)
 */
public class WindowManager implements IWindowManagerExt {

	private final static int TIMER_TICKS_PER_SECOND = 50;
	private final AwtBattleAdaptor awtAdaptor;
	private RobocodeEditor robocodeEditor;
	private RobotPackager robotPackager;
	private RobotExtractor robotExtractor;
	private RankingDialog rankingDialog;
	private final ISettingsManager properties;
	private final IImageManager imageManager;
	private final IBattleManager battleManager;
	private final ICpuManager cpuManager;
	private final IRepositoryManager repositoryManager;
	private IRobotDialogManager robotDialogManager;
	private RobocodeFrame robocodeFrame;
	private boolean isGUIEnabled = true;
	private boolean slave = false;

	public WindowManager(ISettingsManager properties, IBattleManager battleManager, ICpuManager cpuManager, IRepositoryManager repositoryManager, IImageManager imageManager) {
		this.properties = properties;
		this.battleManager = battleManager;
		this.repositoryManager = repositoryManager;
		this.cpuManager = cpuManager;
		this.imageManager = imageManager;
		awtAdaptor = new AwtBattleAdaptor(battleManager, TIMER_TICKS_PER_SECOND, true);

		// we will set UI better priority than robots and battle have
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
				} catch (SecurityException ex) {// that's a pity
				}
			}
		});
	}

	public synchronized void addBattleListener(IBattleListener listener) {
		awtAdaptor.addListener(listener);
	}

	public synchronized void removeBattleListener(IBattleListener listener) {
		awtAdaptor.removeListener(listener);
	}

	public boolean isGUIEnabled() {
		return isGUIEnabled;
	}

	public void setEnableGUI(boolean enable) {
		isGUIEnabled = enable;
	}

	public void setSlave(boolean value) {
		slave = value;
	}

	public boolean isSlave() {
		return slave;
	}

	public ITurnSnapshot getLastSnapshot() {
		return awtAdaptor.getLastSnapshot();
	}

	public int getFPS() {
		return awtAdaptor.getFPS();
	}

	public RobocodeFrame getRobocodeFrame() {
		if (robocodeFrame == null) {
			this.robocodeFrame = Container.getComponent(RobocodeFrame.class);

		}
		return robocodeFrame;
	}

	public void showRobocodeFrame(boolean visible, boolean iconified) {
		RobocodeFrame frame = getRobocodeFrame();

		if (iconified) {
			frame.setState(Frame.ICONIFIED);
		}
		
		if (visible) {
			// Pack frame to size all components
			WindowUtil.packCenterShow(frame);

			WindowUtil.setStatusLabel(frame.getStatusLabel());

			frame.checkUpdateOnStart();

		} else {
			frame.setVisible(false);
		}
	}

	public void showAboutBox() {
		packCenterShow(net.sf.robocode.core.Container.getComponent(AboutBox.class), true);
	}

	public String showBattleOpenDialog(final String defExt, final String name) {
		JFileChooser chooser = new JFileChooser(battleManager.getBattlePath());

		chooser.setFileFilter(
				new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory()
						|| pathname.getName().toLowerCase().lastIndexOf(defExt.toLowerCase())
								== pathname.getName().length() - defExt.length();
			}

			@Override
			public String getDescription() {
				return name;
			}
		});

		if (chooser.showOpenDialog(getRobocodeFrame()) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile().getPath();
		}
		return null;
	}

	public String saveBattleDialog(String path, final String defExt, final String name) {
		File f = new File(path);

		JFileChooser chooser;

		chooser = new JFileChooser(f);

		javax.swing.filechooser.FileFilter filter = new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory()
						|| pathname.getName().toLowerCase().lastIndexOf(defExt.toLowerCase())
								== pathname.getName().length() - defExt.length();
			}

			@Override
			public String getDescription() {
				return name;
			}
		};

		chooser.setFileFilter(filter);
		int rv = chooser.showSaveDialog(getRobocodeFrame());
		String result = null;

		if (rv == JFileChooser.APPROVE_OPTION) {
			result = chooser.getSelectedFile().getPath();
			int idx = result.lastIndexOf('.');
			String extension = "";

			if (idx > 0) {
				extension = result.substring(idx);
			}
			if (!(extension.equalsIgnoreCase(defExt))) {
				result += defExt;
			}
		}
		return result;
	}

	public void showVersionsTxt() {
		showInBrowser(
				"file://" + new File(FileUtil.getCwd(), "").getAbsoluteFile() + System.getProperty("file.separator")
				+ "versions.txt");
	}

	public void showHelpApi() {
		showInBrowser(
				"file://" + new File(FileUtil.getCwd(), "").getAbsoluteFile() + System.getProperty("file.separator")
				+ "javadoc" + System.getProperty("file.separator") + "index.html");
	}

	public void showFaq() {
		showInBrowser("http://robocode.sourceforge.net/help/robocode.faq.txt");
	}

	public void showOnlineHelp() {
		showInBrowser("http://robocode.sourceforge.net/help");
	}

	public void showJavaDocumentation() {
		showInBrowser("http://java.sun.com/j2se/1.5.0/docs");
	}

	public void showRobocodeHome() {
		showInBrowser("http://robocode.sourceforge.net");
	}

	public void showRoboWiki() {
		showInBrowser("http://robowiki.net");
	}

	public void showYahooGroupRobocode() {
		showInBrowser("http://groups.yahoo.com/group/robocode");
	}

	public void showRobocodeRepository() {
		showInBrowser("http://robocoderepository.com");
	}

	public void showOptionsPreferences() {
		try {
			battleManager.pauseBattle();

			WindowUtil.packCenterShow(getRobocodeFrame(), Container.getComponent(PreferencesDialog.class));
		} finally {
			battleManager.resumeIfPausedBattle(); // THIS is just dirty hack-fix of more complex problem with desiredTPS and pausing.  resumeBattle() belongs here.
		}
	}

	public void showResultsDialog(BattleCompletedEvent event) {
		final ResultsDialog dialog = Container.getComponent(ResultsDialog.class);

		dialog.setup(event.getSortedResults(), event.getBattleRules().getNumRounds());
		packCenterShow(dialog, true);
	}

	public void showRankingDialog(boolean visible) {
		if (rankingDialog == null) {
			rankingDialog = Container.getComponent(RankingDialog.class);
			if (visible) {
				packCenterShow(rankingDialog, true);
			} else {
				rankingDialog.dispose();
			}
		} else {
			if (visible) {
				packCenterShow(rankingDialog, false);
			} else {
				rankingDialog.dispose();
			}
		}
	}

	public void showRobocodeEditor() {
		if (robocodeEditor == null) {
			robocodeEditor = net.sf.robocode.core.Container.getComponent(RobocodeEditor.class);
			WindowUtil.packCenterShow(robocodeEditor);
		} else {
			robocodeEditor.setVisible(true);
		}
	}

	public void showRobotPackager() {
		if (robotPackager != null) {
			robotPackager.dispose();
			robotPackager = null;
		}

		robotPackager = net.sf.robocode.core.Container.getComponent(RobotPackager.class);
		WindowUtil.packCenterShow(robotPackager);
	}

	public void showRobotExtractor(JFrame owner) {
		if (robotExtractor != null) {
			robotExtractor.dispose();
			robotExtractor = null;
		}

		robotExtractor = new net.sf.robocode.ui.dialog.RobotExtractor(owner, this, repositoryManager);
		WindowUtil.packCenterShow(robotExtractor);
	}

	public void showSplashScreen() {
		RcSplashScreen splashScreen = Container.getComponent(RcSplashScreen.class);

		packCenterShow(splashScreen, true);

		WindowUtil.setStatusLabel(splashScreen.getSplashLabel());

		repositoryManager.loadRobotRepository();

		WindowUtil.setStatusLabel(splashScreen.getSplashLabel());
		imageManager.initialize();
		cpuManager.getCpuConstant();

		WindowUtil.setStatus("");
		WindowUtil.setStatusLabel(null);

		splashScreen.dispose();
	}

	public void showNewBattleDialog(BattleProperties battleProperties) {
		try {
			battleManager.pauseBattle();
			final NewBattleDialog battleDialog = Container.getComponent(NewBattleDialog.class);

			battleDialog.setup(battleManager.getBattleProperties());
			WindowUtil.packCenterShow(getRobocodeFrame(), battleDialog);
		} finally {
			battleManager.resumeBattle();
		}
	}

	public boolean closeRobocodeEditor() {
		return robocodeEditor == null || !robocodeEditor.isVisible() || robocodeEditor.close();
	}

	public void showCreateTeamDialog() {
		TeamCreator teamCreator = Container.getComponent(TeamCreator.class);

		WindowUtil.packCenterShow(teamCreator);
	}

	public void showImportRobotDialog() {
		JFileChooser chooser = new JFileChooser();

		chooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isHidden()) {
					return false;
				}
				if (pathname.isDirectory()) {
					return true;
				}
				String filename = pathname.getName();

				if (filename.equals("robocode.jar")) {
					return false;
				}
				int idx = filename.lastIndexOf('.');

				String extension = "";

				if (idx >= 0) {
					extension = filename.substring(idx);
				}
				return extension.equalsIgnoreCase(".jar") || extension.equalsIgnoreCase(".zip");
			}

			@Override
			public String getDescription() {
				return "Jar Files";
			}
		});

		chooser.setDialogTitle("Select the robot .jar file to copy to " + repositoryManager.getRobotsDirectory());

		if (chooser.showDialog(getRobocodeFrame(), "Import") == JFileChooser.APPROVE_OPTION) {
			File inputFile = chooser.getSelectedFile();
			String fileName = chooser.getSelectedFile().getName();
			int idx = fileName.lastIndexOf('.');
			String extension = "";

			if (idx >= 0) {
				extension = fileName.substring(idx);
			}
			if (!extension.equalsIgnoreCase(".jar")) {
				fileName += ".jar";
			}
			File outputFile = new File(repositoryManager.getRobotsDirectory(), fileName);

			if (inputFile.equals(outputFile)) {
				JOptionPane.showMessageDialog(getRobocodeFrame(),
						outputFile.getName() + " is already in the robots directory!");
				return;
			}
			if (outputFile.exists()) {
				if (JOptionPane.showConfirmDialog(getRobocodeFrame(), outputFile + " already exists.  Overwrite?",
						"Warning", JOptionPane.YES_NO_OPTION)
						== JOptionPane.NO_OPTION) {
					return;
				}
			}
			if (JOptionPane.showConfirmDialog(getRobocodeFrame(),
					"Robocode will now copy " + inputFile.getName() + " to " + outputFile.getParent(), "Import robot",
					JOptionPane.OK_CANCEL_OPTION)
					== JOptionPane.OK_OPTION) {
				try {
					FileUtil.copy(inputFile, outputFile);
					repositoryManager.clearRobotList();
					JOptionPane.showMessageDialog(getRobocodeFrame(), "Robot imported successfully.");
				} catch (IOException e) {
					JOptionPane.showMessageDialog(getRobocodeFrame(), "Import failed: " + e);
				}
			}
		}
	}

	/**
	 * Shows a web page using the browser manager.
	 *
	 * @param url The URL of the web page
	 */
	private void showInBrowser(String url) {
		try {
			BrowserManager.openURL(url);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(getRobocodeFrame(), e.getMessage(), "Unable to open browser!",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void showSaveResultsDialog(BattleResultsTableModel tableModel) {
		JFileChooser chooser = new JFileChooser();

		chooser.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.isHidden()) {
					return false;
				}
				if (pathname.isDirectory()) {
					return true;
				}
				String filename = pathname.getName();
				int idx = filename.lastIndexOf('.');

				String extension = "";

				if (idx >= 0) {
					extension = filename.substring(idx);
				}
				return extension.equalsIgnoreCase(".csv");
			}

			@Override
			public String getDescription() {
				return "Comma Separated Value (CSV) File Format";
			}
		});

		chooser.setDialogTitle("Save battle results");

		if (chooser.showSaveDialog(getRobocodeFrame()) == JFileChooser.APPROVE_OPTION) {

			String filename = chooser.getSelectedFile().getPath();

			if (!filename.endsWith(".csv")) {
				filename += ".csv";
			}

			boolean append = properties.getOptionsCommonAppendWhenSavingResults();

			tableModel.saveToFile(filename, append);
		}
	}

	/**
	 * Packs, centers, and shows the specified window on the screen.
	 * @param window the window to pack, center, and show
	 * @param center {@code true} if the window must be centered; {@code false} otherwise
	 */
	private void packCenterShow(Window window, boolean center) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		window.pack();
		if (center) {
			window.setLocation((screenSize.width - window.getWidth()) / 2, (screenSize.height - window.getHeight()) / 2);
		}
		window.setVisible(true);
	}

	public void cleanup() {
		if (isGUIEnabled()) {
			getRobocodeFrame().dispose();
		}
	}

	public void setStatus(String s) {
		WindowUtil.setStatus(s);
	}

	public boolean isIconified() {
		return getRobocodeFrame().isIconified();
	}

	public IRobotDialogManager getRobotDialogManager() {
		if (robotDialogManager == null) {
			robotDialogManager = new RobotDialogManager();
		}
		return robotDialogManager;
	}

	/**
	 * Sets the Look and Feel (LAF). This method first try to set the LAF to the
	 * system's LAF. If this fails, it try to use the cross platform LAF.
	 * If this also fails, the LAF will not be changed.
	 */
	public void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {
			// Work-around for problems with setting Look and Feel described here:
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6468089
			Locale.setDefault(Locale.US);

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Throwable t2) {
				// For some reason Ubuntu 7 can cause a NullPointerException when trying to getting the LAF
				System.err.println("Could not set the Look and Feel (LAF).  The default LAF is used instead");
			}
		}
	}

	public void setVisibleForRobotEngine(boolean visible) {
		if (visible && !isGUIEnabled()) {
			// The GUI must be enabled in order to show the window
			setEnableGUI(true);

			// Set the Look and Feel (LAF)
			setLookAndFeel();
		}

		if (isGUIEnabled()) {
			showRobocodeFrame(visible, false);
			properties.setOptionsCommonShowResults(visible);
		}
	}
}
