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

import com.alta189.cyborg.api.event.SimpleEventManager;
import com.alta189.cyborg.api.event.bot.JoinEvent;
import com.alta189.cyborg.api.event.bot.PartEvent;
import com.alta189.cyborg.api.event.bot.SendActionEvent;
import com.alta189.cyborg.api.event.bot.SendMessageEvent;
import com.alta189.cyborg.api.event.bot.SendNoticeEvent;
import com.alta189.cyborg.api.event.channel.SetChannelTopicEvent;

import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

/**
 * Subclass of PircBotX designed to only connect to one server
 *
 */
public class CyborgBot extends PircBotX {
	public static final String NEWLINE = System.getProperty("line.separator");
	private static final SimpleEventManager eventManager = (SimpleEventManager) Cyborg.getInstance().getEventManager();

	public CyborgBot(Configuration<CyborgBot> configuration) {
		super(configuration);
	}

	public void joinChannel(String channel) {
		joinChannel(channel, null);
	}

	public void joinChannel(String channel, String key) {
		JoinEvent joinEvent = new JoinEvent(channel, key);
		joinEvent = eventManager.callEvent(joinEvent);
		if (!joinEvent.isCancelled()) {
			if (joinEvent.getKey() != null) {
				sendIRC().joinChannel(channel);
			} else {
				sendIRC().joinChannel(joinEvent.getChannel());
			}
		}
	}

	public void partChannel(Channel channel) {
		partChannel(channel, null);
	}

	public void partChannel(Channel channel, String reason) {
		PartEvent partEvent = new PartEvent(channel, reason);
		partEvent = eventManager.callEvent(partEvent);
		if (!partEvent.isCancelled()) {
			if (partEvent.getReason() != null) {
				channel.send().part(partEvent.getReason());
			} else {
				channel.send().part();
			}
		}
	}

	public void quitServer(String reason) {
		try {
			sendIRC().quitServer(reason);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (Exception ignored) {
		}
		super.shutdown();
	}

	public void quitServer() {
		try {
			sendIRC().quitServer();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (Exception ignored) {
		}
		super.shutdown();
	}

	@Override
	public void shutdown() {
		quitServer();
	}

	@Override
	public void shutdown(boolean noReconnect) {
		quitServer();
	}

	public void sendMessage(User target, String message) {
		SendMessageEvent event = new SendMessageEvent(target.getNick(), message);
		event = eventManager.callEvent(event);
		if (event.isCancelled())
			return;
		if (event.getMessage().contains(NEWLINE)) {
			for (String line : event.getMessage().split(NEWLINE)) {
				target.send().message(line);
			}
		} else {
			target.send().message(event.getMessage());
		}
	}

	public void sendMessage(Channel target, String message) {
		SendMessageEvent event = new SendMessageEvent(target.getName(), message);
		event = eventManager.callEvent(event);
		if (event.isCancelled())
			return;
		if (event.getMessage().contains(NEWLINE)) {
			for (String line : event.getMessage().split(NEWLINE)) {
				target.send().message(line);
			}
		} else {
			target.send().message(event.getMessage());
		}
	}

	public void sendMessage(Channel target, User user, String message) {
		SendMessageEvent event = new SendMessageEvent(target.getName(), user.getNick() + ":" + message);
		event = eventManager.callEvent(event);
		if (event.isCancelled())
			return;
		if (event.getMessage().contains(NEWLINE)) {
			for (String line : event.getMessage().split(NEWLINE)) {
				target.send().message(user, line);
			}
		} else {
			target.send().message(user, message);
		}
	}

	public void sendMessage(String user, String message) {
		SendMessageEvent event = new SendMessageEvent(user, message);
		event = eventManager.callEvent(event);
		if (event.isCancelled())
			return;
		if (event.getMessage().contains(NEWLINE)) {
			for (String line : event.getMessage().split(NEWLINE)) {
				sendIRC().message(user, line);
			}
		} else {
			sendIRC().message(user, message);
		}
	}

	public void sendAction(User target, String action) {
		SendActionEvent event = new SendActionEvent(target.getNick(), action);
		event = eventManager.callEvent(event);
		if (!event.isCancelled()) {
			if (event.getAction().contains(NEWLINE)) {
				for (String line : event.getAction().split(NEWLINE)) {
					target.send().action(line);
				}
			} else {
				target.send().action(event.getAction());
			}
		}
	}

	public void sendAction(Channel target, String action) {
		SendActionEvent event = new SendActionEvent(target.getName(), action);
		event = eventManager.callEvent(event);
		if (!event.isCancelled()) {
			if (event.getAction().contains(NEWLINE)) {
				for (String line : event.getAction().split(NEWLINE)) {
					target.send().action(line);
				}
			} else {
				target.send().action(event.getAction());
			}
		}
	}

	public void sendAction(String target, String action) {
		SendActionEvent event = new SendActionEvent(target, action);
		event = eventManager.callEvent(event);
		if (!event.isCancelled()) {
			if (event.getAction().contains(NEWLINE)) {
				for (String line : event.getAction().split(NEWLINE)) {
					sendIRC().action(target, line);
				}
			} else {
				sendIRC().action(target, event.getAction());
			}
		}
	}

	public void sendNotice(User target, String action) {
		SendNoticeEvent event = new SendNoticeEvent(target.getNick(), action);
		event = eventManager.callEvent(event);
		if (!event.isCancelled()) {
			if (event.getNotice().contains(NEWLINE)) {
				for (String line : event.getNotice().split(NEWLINE)) {
					target.send().notice(line);
				}
			} else {
				target.send().notice(event.getNotice());
			}
		}
	}

	public void sendNotice(Channel target, String action) {
		SendNoticeEvent event = new SendNoticeEvent(target.getName(), action);
		event = eventManager.callEvent(event);
		if (!event.isCancelled()) {
			if (event.getNotice().contains(NEWLINE)) {
				for (String line : event.getNotice().split(NEWLINE)) {
					target.send().notice(line);
				}
			} else {
				target.send().notice(event.getNotice());
			}
		}
	}

	public void sendNotice(String target, String action) {
		SendNoticeEvent event = new SendNoticeEvent(target, action);
		event = eventManager.callEvent(event);
		if (!event.isCancelled()) {
			if (event.getNotice().contains(NEWLINE)) {
				for (String line : event.getNotice().split(NEWLINE)) {
					sendIRC().notice(target, line);
				}
			} else {
				sendIRC().notice(target, event.getNotice());
			}
		}
	}

	public String getHostmask() {
		return getUserBot().getHostmask();
	}

	public void setTopic(Channel channel, String topic) {
		SetChannelTopicEvent event = eventManager.callEvent(new SetChannelTopicEvent(channel, topic));
		if (!event.isCancelled()) {
			event.getChannel().send().setTopic(event.getTopic());
		}
	}
}
