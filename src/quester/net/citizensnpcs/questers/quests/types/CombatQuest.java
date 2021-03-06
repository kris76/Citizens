package net.citizensnpcs.questers.quests.types;

import net.citizensnpcs.permissions.CitizensGroup;
import net.citizensnpcs.permissions.PermissionManager;
import net.citizensnpcs.questers.QuestUtils;
import net.citizensnpcs.questers.quests.progress.ObjectiveProgress;
import net.citizensnpcs.questers.quests.progress.QuestUpdater;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDeathEvent;

public class CombatQuest implements QuestUpdater {
	private static final Type[] EVENTS = new Type[] { Type.ENTITY_DEATH };

	@Override
	public boolean update(Event event, ObjectiveProgress progress) {
		if (event instanceof EntityDeathEvent) {
			EntityDeathEvent ev = (EntityDeathEvent) event;
			if (!(ev.getEntity() instanceof Player))
				return false;
			Player player = (Player) ev.getEntity();
			String search = progress.getObjective().getString();
			boolean found = false, reversed = !search.isEmpty()
					&& search.charAt(0) == '-';
			if (search.contains(player.getName().toLowerCase())
					|| search.contains("*")) {
				found = true;
			} else if (search.contains("g:") && PermissionManager.hasBackend()) {
				// Should be the last else statement, as it needs to do
				// extra processing.
				for (CitizensGroup group : PermissionManager.getGroups(player)) {
					if (search.contains("g:" + group.getName().toLowerCase())) {
						found = true;
					}
				}
			}
			if ((reversed && !found) || (!reversed && found)) {
				progress.addAmount(1);
			}
		}
		return progress.getAmount() >= progress.getObjective().getAmount();
	}

	@Override
	public Type[] getEventTypes() {
		return EVENTS;
	}

	@Override
	public String getStatus(ObjectiveProgress progress) {
		return QuestUtils.defaultAmountProgress(progress, "players defeated");
	}
}