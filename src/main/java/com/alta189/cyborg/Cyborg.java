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

import com.alta189.cyborg.api.command.CommandListener;
import com.alta189.cyborg.api.command.CommandManager;
import com.alta189.cyborg.api.command.CommonCommandManager;
import com.alta189.cyborg.api.command.Named;
import com.alta189.cyborg.api.command.annotation.EmptyConstructorInjector;
import com.alta189.cyborg.api.event.EventManager;
import com.alta189.cyborg.api.event.SimpleEventManager;
import com.alta189.cyborg.api.event.bot.SendMessageEvent;
import com.alta189.cyborg.api.plugin.CommonPluginLoader;
import com.alta189.cyborg.api.plugin.CommonPluginManager;
import com.alta189.cyborg.api.plugin.Plugin;
import com.alta189.cyborg.api.plugin.PluginManager;
import com.alta189.cyborg.api.terminal.TerminalCommands;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.Set;
import org.pircbotx.Channel;
import org.pircbotx.User;

public class Cyborg {
	private static final Cyborg INSTANCE = new Cyborg();
	@Getter
	private final static String version = readVersion();
	private final File pluginDir;
	private final CommonPluginManager pluginManager;
	private final SimpleEventManager eventManager;
	@Getter
	private final CommandManager commandManager;
	/**
	 * This maps bots to their server.
	 */
	private final BiMap<String, CyborgBot> bots = HashBiMap.create();

	private Cyborg() {
		pluginDir = new File(Main.getSettingsBase(), "plugins");
		pluginManager = new CommonPluginManager(this);
		pluginManager.registerPluginLoader(CommonPluginLoader.class);
		eventManager = new SimpleEventManager();
		commandManager = new CommonCommandManager();
		// Register Internal Listeners
		eventManager.registerEvents(new CommandListener(), this);
		eventManager.registerEvents(new InternalListener(), this);

		// Register Default Commands
		commandManager.registerCommands(new Named() {
			@Override
			public String getName() {
				return Cyborg.class.getCanonicalName();
			}
		}, TerminalCommands.class, new EmptyConstructorInjector());

	}

	private static String readVersion() {
		String version = "-1";
		try {
			version = IOUtils.toString(Main.class.getResource("version").openStream(), "UTF-8");
		} catch (Exception e) {
			// Ignored \\
		}
		if (version.equalsIgnoreCase("${build.number}")) {
			version = "custom_build";
		}
		return version;
	}

	public static Cyborg getInstance() {
		return INSTANCE;
	}

	protected final void loadPlugins() {
		if (!pluginDir.exists()) {
			pluginDir.mkdirs();
		}
		pluginManager.loadPlugins(pluginDir);
	}

	protected final void enablePlugins() {
		for (Plugin plugin : pluginManager.getPlugins()) {
			pluginManager.enablePlugin(plugin);
		}
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public PluginManager getPluginManager() {
		return pluginManager;
	}

	public File getUpdateFolder() {
		return new File(pluginDir, "updates");
	}

	public File getPluginDirectory() {
		return pluginDir;
	}

	public CyborgBot getBot(String server) {
		CyborgBot bot = bots.get(server);
		if (bot == null) {
			throw new IllegalStateException("Bot not initialized!");
		}
		return bot;
	}

	public Channel getChannel(String server, String channel) {
		return getBot(server).getUserChannelDao().getChannel(channel);
	}

	public void shutdown(String reason) {
		for (CyborgBot bot : bots.values()) {
			bot.sendIRC().quitServer(reason);
			bot.shutdown(true);
		}
		bots.clear();
		shutdownEnd();
	}

	public void shutdown() {
		for (CyborgBot bot : bots.values()) {
			bot.sendIRC().quitServer();
			bot.shutdown(true);
		}
		bots.clear();
		shutdownEnd();
	}

	private void shutdownEnd() {
		pluginManager.disablePlugins();
		Main.shutdownTerminal();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public String getHostmask(String server) {
		return getBot(server).getHostmask();
	}

	/**
	 * Gets the time in milliseconds that Cyborg has been running
	 *
	 * @return runningTime
	 */
	public long getRunningTime() {
		return System.currentTimeMillis() - Main.getStart();
	}
	
	public void sendMessage(User user, String message) {
		SendMessageEvent event = new SendMessageEvent(user.getNick(), message);
		event = eventManager.callEvent(event);
		if (event.isCancelled())
			return;
		if (event.getMessage().contains(CyborgBot.NEWLINE)) {
			for (String line : event.getMessage().split(CyborgBot.NEWLINE)) {
				user.send().message(line);
			}
		} else {
			user.send().message(message);
		}
	}
}
