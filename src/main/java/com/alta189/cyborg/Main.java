/**
 * Copyright (C) 2012 CyborgDev <cyborg@alta189.com>
 *
 * This file is part of Cyborg
 *
 * Cyborg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cyborg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.alta189.cyborg;

import com.alta189.cyborg.api.terminal.TerminalThread;
import com.alta189.cyborg.api.util.yaml.YAMLFormat;
import com.alta189.cyborg.api.util.yaml.YAMLProcessor;
import com.beust.jcommander.JCommander;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.pircbotx.Configuration;
import org.pircbotx.exception.IrcException;

public class Main {
	private static TerminalThread terminalThread;
	@Getter
	private static String[] args;
	@Getter
	private static long start;
	@Getter
	private static File settingsBase;

	public static void main(String[] args) throws IOException, IrcException {
		start = System.currentTimeMillis();
		Main.args = args;
		// Parse arguments
		StartupArguments params = new StartupArguments();
		try {
			Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[]{String.class});
			m.setAccessible(true);
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			Object test1 = m.invoke(cl, "com.beust.jcommander.JCommander");
			File lib = new File("jline-1.0.jar");
			if (test1 == null) {
				if (!extractFromJar("jline-1.0.jar", lib)) {
					throw new Exception("Could not extract jar!");
				}
				if (!lib.exists()) {
					throw new Exception("There was a critical error! Could not find lib: " + lib.getName());
				}
				addClassPath(getJarUrl(lib));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		new JCommander(params, args);
		StartupArguments.setInstance(params);
		if (StartupArguments.getInstance().useConsole()) {
			terminalThread = new TerminalThread();
			terminalThread.start();
			CyborgLogger.initConsole();
		}
		CyborgLogger.initSlf();
		CyborgLogger.log(CyborgLogger.Level.INFO, "Cyborg is starting up!");
		if (StartupArguments.getInstance().getSettingsFile() == null) {
			settingsBase = new File(".");
		} else {
			settingsBase = new File(StartupArguments.getInstance().getSettingsFile());
		}
		File settingsFile = new File(settingsBase, "settings.yml");
		if (params.isWriteDefaults()) {
			settingsFile.delete();
		}
		YAMLProcessor settings = setupSettings(settingsFile);
		if (settings == null) {
			throw new NullPointerException("The YAMLProcessor object was null for settings.");
		}
		settings.load();
		Settings.setSettings(settings);
		Cyborg cyborg = Cyborg.getInstance();
		cyborg.getPluginDirectory().mkdirs();

		if (params.isExitAfterWrite()) {
			System.exit(0);
		}

		cyborg.loadPlugins();
		cyborg.enablePlugins();

		for (String server : Settings.getServerAddresses()) {
			Configuration.Builder<CyborgBot> builder = new Configuration.Builder<CyborgBot>();
			builder.setName(Settings.getNick());
			builder.setLogin(Settings.getIdent());
			builder.setMessageDelay(Settings.getMessageDelay());
			builder.addListener(new PircBotXListener());
			CyborgBot bot;
			if (Settings.getServerPass(server) == null) {
				builder.setServer(server, Settings.getServerPort(server));
				bot = new CyborgBot(builder.buildConfiguration());
				bot.startBot();
			} else {
				builder.setServer(server, Settings.getServerPort(server), Settings.getServerPass(server));
				bot = new CyborgBot(builder.buildConfiguration());
				bot.startBot();
			}

			for (String channel : Settings.getChannels(server)) {
				bot.joinChannel(channel);
			}
		}
	}

	private static YAMLProcessor setupSettings(File file) {
		if (!file.exists()) {
			try {
				InputStream input = Main.class.getResource("/settings.yml").openStream();
				if (input != null) {
					FileOutputStream output = null;
					try {
						if (!file.getParentFile().exists()) {
							file.getParentFile().mkdirs();
						}
						file.createNewFile();
						output = new FileOutputStream(file);
						byte[] buf = new byte[8192];
						int length;

						while ((length = input.read(buf)) > 0) {
							output.write(buf, 0, length);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							input.close();
						} catch (Exception e) {
						}
						try {
							if (output != null) {
								output.close();
							}
						} catch (Exception e) {
						}
					}
				} else {
					throw new IllegalStateException("input cannot be null");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (!file.getParentFile().exists() || !file.exists()) {
			throw new IllegalStateException("settings.yml cannot be null");
		}
		return new YAMLProcessor(file, false, YAMLFormat.EXTENDED);
	}

	public static void shutdownTerminal() {
		if (terminalThread != null) {
			terminalThread.interrupt();
		}
	}

	// In case of "sealing violation"
	public static JarFile getRunningJar() throws IOException {
		final URL resource = Main.class.getClassLoader().getResource("settings.yml");
		if (resource != null) {
			return null; // null if not running from jar
		}
		String path = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
		path = URLDecoder.decode(path, "UTF-8");
		return new JarFile(path);
	}

	public static boolean extractFromJar(final String fileName, final File file) throws IOException {
		if (getRunningJar() == null) {
			return false;
		}
		if (file.isDirectory()) {
			file.mkdir();
			return false;
		}
		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}

		final JarFile jar = getRunningJar();
		final Enumeration<JarEntry> e = jar.entries();
		while (e.hasMoreElements()) {
			final JarEntry je = e.nextElement();
			if (!je.getName().contains(fileName)) {
				continue;
			}
			final InputStream in = new BufferedInputStream(jar.getInputStream(je));
			final OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			copyInputStream(in, out);
			jar.close();
			return true;
		}
		jar.close();
		return false;
	}

	private static void copyInputStream(final InputStream in, final OutputStream out) throws IOException {
		try {
			final byte[] buff = new byte[4096];
			int n;
			while ((n = in.read(buff)) > 0) {
				out.write(buff, 0, n);
			}
		} finally {
			out.flush();
			out.close();
			in.close();
		}
	}

	private static void addClassPath(final URL url) throws IOException {
		final URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		final Class<URLClassLoader> sysclass = URLClassLoader.class;
		try {
			final Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
			method.setAccessible(true);
			method.invoke(sysloader, new Object[]{url});
		} catch (final Throwable t) {
			t.printStackTrace();
			throw new IOException("Error adding " + url
				+ " to system classloader");
		}
	}

	public static URL getJarUrl(final File file) throws IOException {
		return new URL("jar:" + file.toURI().toURL().toExternalForm() + "!/");
	}
}
