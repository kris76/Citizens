package net.citizensnpcs.api.event.npc;

import net.citizensnpcs.resources.npclib.HumanNPC;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class NPCRightClickEvent extends NPCPlayerEvent implements Cancellable {
	private static final long serialVersionUID = 1L;
	private boolean cancelled = false;

	public NPCRightClickEvent(HumanNPC npc, Player player) {
		super("NPCRightClickEvent", npc, player);
	}

	/**
	 * Get the cancellation state of the event.
	 * 
	 * @return true if the event is cancelled
	 */
	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	/**
	 * Set the cancellation state of an event.
	 * 
	 * @param cancelled
	 *            the cancellation state of the event
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}