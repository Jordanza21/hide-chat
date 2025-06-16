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
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.ScriptID;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.api.events.StatChanged;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;

import java.awt.event.KeyEvent;

@PluginDescriptor(name = "Hide Chat", description = "Hides the chat box based on the toggle option", tags = {})
@Slf4j
public class HideChatPlugin extends Plugin implements KeyListener {
	@Inject
	private Client client;

	@Inject
	private HideChatConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ConfigManager configManager; // Inject ConfigManager

	private long lastCombatXpTime = 0;
	private boolean combatOverrideActive = false;
	private boolean hideInCombat = false;

	@Provides
	private HideChatConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(HideChatConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		keyManager.registerKeyListener(this); // Register the key listener
		toggleChatBox(); // Ensure chatbox visibility is set based on config on startup
	}

	@Override
	protected void shutDown() throws Exception {
		keyManager.unregisterKeyListener(this); // Unregister the key listener
		showChatBox(); // Ensure chatbox is shown when the plugin is disabled
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals("hidechat")) {
			// Sync hideInCombat with PvM/PvP options
			boolean pvm = config.hideInPvm();
			boolean pvp = config.hideInPvp();
			if (pvm || pvp) {
				hideInCombat = true;
			} else {
				hideInCombat = false;
			}
			toggleChatBox(); // Update chatbox visibility when the config changes
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

	@Subscribe
	public void onVarbitChanged(VarbitChanged event) {
		// Reapply the hiding logic if a relevant varbit changes
		if (config.hideChatBox()) {
			toggleChatBox();
		}
	}

	@Subscribe
	public void onCanvasSizeChanged(CanvasSizeChanged event) {
		if (!client.isResized()) {
			showChatBox(); // Ensure chatbox is shown if the client is not resized
		} else if (config.hideChatBox()) {
			toggleChatBox(); // Reapply hiding logic if the client is resized
		}
	}

	// Ensures all children under root widget is also hidden (helps prevent chatbox
	// from flashing sometimes)
	protected void hideWidgetChildren(Widget root, boolean hide) {
		if (root == null) {
			return;
		}

		Widget[] children = root.getDynamicChildren();
		if (children != null) {
			root.setHidden(hide);
			log.debug("hiding root");
			for (Widget child : children) {
				if (child != null && child.getContentType() != 1337) {
					child.setHidden(hide);
					log.debug("hiding child");
				}
			}
		}
	}

	private void toggleChatBox() {
		if (config.hideChatBox()) {
			hideChatBox(); // Hide chatbox if configured to do so
		} else {
			showChatBox(); // Show chatbox if not configured to hide
		}
	}

	private void hideChatBox() {
		clientThread.invokeLater(() -> {
			// Use the numeric group ID for the chatbox widget (162)
			Widget chatbox = client.getWidget(162, 0);
			if (chatbox != null) {
				hideWidgetChildren(chatbox, true);
			}
		});
	}

	private void showChatBox() {
		clientThread.invokeLater(() -> {
			// Use the numeric group ID for the chatbox widget (162)
			Widget chatbox = client.getWidget(162, 0);
			if (chatbox != null) {
				hideWidgetChildren(chatbox, false);
			}
		});
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Not used, just required for Key listener interface
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Not used, just required for Key listener interface
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// Check if the pressed key matches the configured keybind
		if (config.toggleHotkey().matches(e)) {
			boolean currentState = config.hideChatBox();

			// Update the configuration value
			configManager.setConfiguration("hidechat", "Hide Chat", !currentState);

			// Disable combat override and combat option when user manually toggles
			// Only if the hotkey is pressed while it is currently hidden b/c of combat
			if (hideInCombat && combatOverrideActive) {
				configManager.setConfiguration("hidechat", "hideInCombat", false);
				combatOverrideActive = false;
			}

			// Actually apply the change to the chatbox visibility
			toggleChatBox();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event) {
		Skill skill = event.getSkill();
		// List of combat skills to check
		if (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE ||
				skill == Skill.RANGED || skill == Skill.MAGIC || skill == Skill.HITPOINTS) {
			// Check if player is interacting with an NPC or player
			// Then check if the interaction is with a player or NPC
			if (client.getLocalPlayer() != null && client.getLocalPlayer().getInteracting() != null) {
				Object interacting = client.getLocalPlayer().getInteracting();
				// A.I. says this is a way of checking if the interaction is an NPC or Player
				// I couldn't think of a better way to do it...?
				boolean interactingIsNpc = interacting instanceof net.runelite.api.NPC;
				boolean interactingIsPlayer = interacting instanceof net.runelite.api.Player;
				if ((config.hideInPvm() && interactingIsNpc) || (config.hideInPvp() && interactingIsPlayer)) {
					lastCombatXpTime = System.currentTimeMillis();
				}
			} else {
				return;
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (hideInCombat) {
			int timeoutMs = config.combatTimeoutSeconds() * 1000;
			boolean inCombat = (System.currentTimeMillis() - lastCombatXpTime) < timeoutMs;
			if (inCombat) {
				if (!config.hideChatBox() || !combatOverrideActive) {
					configManager.setConfiguration("hidechat", "Hide Chat", true);
					toggleChatBox();
					combatOverrideActive = true;
				}
			} else {
				if (config.hideChatBox() && combatOverrideActive) {
					configManager.setConfiguration("hidechat", "Hide Chat", false);
					toggleChatBox();
				}
				combatOverrideActive = false;
			}
		}
	}
}
