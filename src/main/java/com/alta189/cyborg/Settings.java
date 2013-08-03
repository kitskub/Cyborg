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

import com.alta189.cyborg.api.util.yaml.YAMLProcessor;
import lombok.AccessLevel;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class Settings {
	@Setter(AccessLevel.PROTECTED)
	private static YAMLProcessor settings;

	public static String getNick() {
		return settings.getString("nick", "Cyborg");
	}

	public static void setNick(String nick) {
		settings.setProperty("nick", nick);
	}

	public static String getIdent() {
		return settings.getString("ident", "Cyborg");
	}

	public static void setIdent(String ident) {
		settings.setProperty("ident", ident);
	}

	public static List<String> getAlternateNicks() {
		return settings.getStringList("alt-nicks", new ArrayList<String>());
	}

	public static void setAlternativeNicks(List<String> nicks) {
		settings.setProperty("alt-nicks", nicks);
	}

	public static long getMessageDelay() {
		return Long.valueOf(settings.getString("message-delay", "1000"));
	}

	public static void setMessageDelay(long delay) {
		settings.setProperty("message-delay", delay);
	}

	public static List<String> getServerAddresses() {
		return settings.getKeys("servers");
	}

	public static int getServerPort(String server) {
		return settings.getInt("servers." + server + ".port", 6667);
	}

	public static void setServerPort(String server, int port) {
		settings.setProperty("servers." + server + ".port", port);
	}

	public static String getServerPass(String server) {
		String pass = settings.getString("servers." + server + "password", "none");
		if (pass.equals("none")) {
			return null;
		}
		return pass;
	}

	public static void setServerPass(String server, String pass) {
		if (pass == null) {
			settings.setProperty("servers." + server + "password", "none");
		} else {
			settings.setProperty("servers." + server + "password", pass);
		}
	}

	public static List<String> getChannels(String server) {
		return settings.getStringList("servers." + server + "channels", new ArrayList<String>());
	}

	public static void setChannels(String server, List<String> nicks) {
		settings.setProperty("servers." + server + "channels", nicks);
	}
}
