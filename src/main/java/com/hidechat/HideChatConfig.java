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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("hidechat")
public interface HideChatConfig extends Config {
	@ConfigItem(position = 0, keyName = "Hide Chat", name = "Hide Chat", description = "Hide the chat box from screen")
	default boolean hideChatBox() {
		return false;
	}

	@ConfigItem(position = 1, keyName = "toggleHotkey", name = "Toggle Hotkey", description = "Keybind to toggle hiding the chat box")
	default Keybind toggleHotkey() {
		return Keybind.NOT_SET; // Default to no keybind
	}

	@ConfigItem(position = 2, keyName = "hideInPvm", name = "Hide chat in PvM", description = "Hide the chatbox when interacting with NPCs (PvM)")
	default boolean hideInPvm() {
		return false;
	}

	@ConfigItem(position = 3, keyName = "hideInPvp", name = "Hide chat in PvP", description = "Hide the chatbox when interacting with other players (PvP)")
	default boolean hideInPvp() {
		return false;
	}

	@ConfigItem(position = 4, keyName = "combatTimeoutSeconds", name = "Combat Hide Timeout", description = "Number of seconds after last combat XP to keep chat hidden.")
	@Range(min = 1, max = 60)
	default int combatTimeoutSeconds() {
		return 8;
	}
	// TODO add option to unhide chat when level up window appears
	// TODO (to investigate) add option to unhide when talking with an NPC
	// TODO add option to unhide when any dialog window is triggered, such as
	// teleport menus, confirmation messages... etc
}
