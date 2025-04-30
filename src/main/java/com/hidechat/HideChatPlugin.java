/*
 * Copyright (c) 2020, PresNL
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.hidechat;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.CanvasSizeChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.ScriptID;

@PluginDescriptor(name = "Hide Chat", description = "Hides the chat box based on the toggle option", tags = {})
@Slf4j
public class HideChatPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private HideChatConfig config;

	@Inject
	private ClientThread clientThread;

	@Provides
	private HideChatConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(HideChatConfig.class);
	}

	@Subscribe
	public void onCanvasSizeChanged(CanvasSizeChanged canvasSizeChanged) {
		if (!client.isResized()) {
			showChatBox(); // Ensure the chatbox is shown if the client is not resized
		} else if (config.hideChatBox()) {
			toggleChatBox(); // Reapply hiding logic if the client is resized
		}
	}

	@Override
	protected void startUp() throws Exception {
		toggleChatBox(); // Ensure chat box visibility is set on startup
	}

	@Override
	protected void shutDown() throws Exception {
		showChatBox(); // Ensure chat box is shown when the plugin is disabled
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals("hidechat")) {
			toggleChatBox(); // Update chat box visibility when the config changes
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event) {
		// Reapply the hiding logic to ensure the chatbox remains hidden
		if (event.getScriptId() == ScriptID.TOPLEVEL_REDRAW || event.getScriptId() == 903) {
			if (config.hideChatBox()) {
				toggleChatBox();
			}
		}
	}

	protected void hideWidgetChildren(Widget root, boolean hide) {
		Widget[] rootDynamicChildren = root.getDynamicChildren();
		Widget[] rootNestedChildren = root.getNestedChildren();
		Widget[] rootStaticChildren = root.getStaticChildren();

		Widget[] rootChildren = new Widget[rootDynamicChildren.length + rootNestedChildren.length
				+ rootStaticChildren.length];
		System.arraycopy(rootDynamicChildren, 0, rootChildren, 0, rootDynamicChildren.length);
		System.arraycopy(rootNestedChildren, 0, rootChildren, rootDynamicChildren.length, rootNestedChildren.length);
		System.arraycopy(rootStaticChildren, 0, rootChildren, rootDynamicChildren.length + rootNestedChildren.length,
				rootStaticChildren.length);

		for (Widget w : rootChildren) {
			if (w != null) {
				// hiding the widget with content type 1337 prevents the game from rendering so
				if (w.getContentType() != 1337) {
					w.setHidden(hide);
				}
			}
		}
	}

	private void toggleChatBox() {
		clientThread.invokeLater(() -> {
			Widget chatbox = client.getWidget(InterfaceID.CHATBOX, 0);
			if (chatbox != null) {
				hideWidgetChildren(chatbox, config.hideChatBox());
			}
		});
	}

	private void showChatBox() {
		clientThread.invokeLater(() -> {
			Widget chatbox = client.getWidget(InterfaceID.CHATBOX, 0);
			if (chatbox != null) {
				hideWidgetChildren(chatbox, false);
			}
		});
	}
}
