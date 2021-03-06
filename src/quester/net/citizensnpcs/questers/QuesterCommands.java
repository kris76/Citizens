package net.citizensnpcs.questers;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.citizensnpcs.commands.CommandHandler;
import net.citizensnpcs.permissions.PermissionManager;
import net.citizensnpcs.questers.api.events.QuestCancelEvent;
import net.citizensnpcs.questers.data.PlayerProfile;
import net.citizensnpcs.questers.quests.CompletedQuest;
import net.citizensnpcs.questers.quests.progress.ObjectiveProgress;
import net.citizensnpcs.questers.quests.progress.QuestProgress;
import net.citizensnpcs.resources.npclib.HumanNPC;
import net.citizensnpcs.resources.sk89q.Command;
import net.citizensnpcs.resources.sk89q.CommandContext;
import net.citizensnpcs.resources.sk89q.CommandPermissions;
import net.citizensnpcs.resources.sk89q.CommandRequirements;
import net.citizensnpcs.resources.sk89q.ServerCommand;
import net.citizensnpcs.utils.HelpUtils;
import net.citizensnpcs.utils.Messaging;
import net.citizensnpcs.utils.PageUtils;
import net.citizensnpcs.utils.PageUtils.PageInstance;
import net.citizensnpcs.utils.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;

@CommandRequirements(
		requireSelected = true,
		requireOwnership = true,
		requiredType = "quester")
public class QuesterCommands extends CommandHandler {
	public static final QuesterCommands INSTANCE = new QuesterCommands();

	private QuesterCommands() {
	}

	@CommandRequirements()
	@Command(
			aliases = "quest",
			usage = "abort",
			desc = "aborts current quest",
			modifiers = "abort",
			min = 1,
			max = 1)
	@CommandPermissions("quester.use.quests.abort")
	public static void abortCurrentQuest(CommandContext args, Player player,
			HumanNPC npc) {
		PlayerProfile profile = PlayerProfile.getProfile(player.getName());
		if (!profile.hasQuest()) {
			player.sendMessage(ChatColor.GRAY
					+ "You don't have a quest at the moment.");
		} else {
			Bukkit.getPluginManager().callEvent(
					new QuestCancelEvent(QuestManager.getQuest(profile
							.getProgress().getQuestName()), player));
			profile.setProgress(null);
			player.sendMessage(ChatColor.GREEN + "Quest cleared.");
		}
	}

	@Command(
			aliases = "quester",
			usage = "assign [quest]",
			desc = "assign a quest to an NPC",
			modifiers = "assign",
			min = 2)
	@CommandPermissions("quester.modify.quests.assign")
	public static void assignQuest(CommandContext args, Player player,
			HumanNPC npc) {
		String quest = args.getJoinedStrings(1);
		if (!QuestManager.isValidQuest(quest)) {
			player.sendMessage(ChatColor.GRAY
					+ "There is no quest by that name.");
			return;
		}
		Quester quester = npc.getType("quester");
		if (quester.hasQuest(quest)) {
			player.sendMessage(ChatColor.GRAY
					+ "The quester already has that quest.");
			return;
		}
		quester.addQuest(quest);
		player.sendMessage(ChatColor.GREEN + "Quest " + StringUtils.wrap(quest)
				+ " added to " + StringUtils.wrap(npc.getName())
				+ "'s quests. " + StringUtils.wrap(npc.getName()) + " now has "
				+ StringUtils.wrap(quester.getQuests().size()) + " quests.");
	}

	@Command(
			aliases = "quest",
			usage = "add [player] [npcID] [quest]",
			desc = "gives a quest to a player",
			modifiers = "add",
			min = 4,
			flags = "f")
	@CommandRequirements()
	@CommandPermissions("quester.admin.quests.giveplayer")
	public static void assignQuestToPlayer(CommandContext args, Player player,
			HumanNPC npc) {
		String quest = args.getJoinedStrings(3);
		if (!QuestManager.isValidQuest(quest)) {
			player.sendMessage(ChatColor.GRAY
					+ "There is no quest by that name.");
			return;
		}
		String name = args.getString(1);
		Player other = Bukkit.getServer().getPlayer(args.getString(1));
		if (other == null
				&& !new File("plugins/Citizens/profiles/" + name + ".yml")
						.exists()) {
			player.sendMessage(ChatColor.GRAY
					+ "Couldn't find the offline player quest file.");
			return;
		}
		PlayerProfile profile = PlayerProfile.getProfile(args.getString(1),
				false);
		if (profile.hasQuest() && !args.hasFlag('f')) {
			player.sendMessage(ChatColor.GRAY
					+ "Player already has a quest. Use the -f flag to force add the quest.");
			return;
		}
		profile.setProgress(new QuestProgress(args.getInteger(2), other, name,
				System.currentTimeMillis()));
		if (other == null)
			profile.save();
		player.sendMessage(ChatColor.GREEN + "Quest added.");
	}

	@Command(
			aliases = "quest",
			usage = "clear [player|*] [quest|*]",
			desc = "gives a quest to a player",
			modifiers = "clear",
			min = 3,
			flags = "c")
	@CommandRequirements()
	@CommandPermissions("quester.admin.quests.clear")
	public static void clearQuests(CommandContext args, Player player,
			HumanNPC npc) {
		String quest = args.getJoinedStrings(2);
		if (!quest.equals("*") && !QuestManager.isValidQuest(quest)) {
			player.sendMessage(ChatColor.GRAY
					+ "There is no quest by that name.");
			return;
		}

		String name = args.getString(1).toLowerCase();
		List<PlayerProfile> profiles = Lists.newArrayList();
		if (name.equals("*")) {
			File dir = new File("plugins/Citizens/profiles/");
			if (!dir.exists() || !dir.isDirectory()) {
				player.sendMessage(ChatColor.GRAY
						+ "Profile directory is non-existent.");
				return;
			}
			for (File file : dir.listFiles()) {
				if (!file.isFile())
					continue;
				PlayerProfile profile = PlayerProfile.getProfile(file.getName()
						.replace(".yml", ""), false);
				if (profile != null)
					profiles.add(profile);
			}
		} else {
			PlayerProfile profile;
			if (!new File("plugins/Citizens/profiles/" + name + ".yml")
					.exists()
					|| (profile = PlayerProfile.getProfile(name, false)) == null) {
				player.sendMessage(ChatColor.GRAY
						+ "Couldn't find that player.");
				return;
			}
			profiles.add(profile);
		}
		boolean clearCompleted = args.hasFlag('c'), matchAny = quest
				.equals("*");
		for (PlayerProfile profile : profiles) {
			boolean changed = false;
			if (profile.hasQuest()
					&& (matchAny || profile.getQuest().equalsIgnoreCase(quest))) {
				profile.setProgress(null);
				changed = true;
			}
			if (clearCompleted) {
				if (matchAny)
					profile.removeAllCompletedQuests();
				else
					profile.removeCompletedQuest(quest);
				changed = true;
			}
			if (changed && !profile.isOnline())
				profile.save();
		}
		player.sendMessage(ChatColor.GREEN + "Quests cleared.");
	}

	@Command(
			aliases = "quester",
			usage = "remove [quest]",
			desc = "remove a quest from an NPC",
			modifiers = "remove",
			min = 2)
	@CommandPermissions("quester.modify.quests.remove")
	public static void removeQuest(CommandContext args, Player player,
			HumanNPC npc) {
		Quester quester = npc.getType("quester");
		String quest = args.getJoinedStrings(1);
		if (!quester.hasQuest(quest)) {
			player.sendMessage(ChatColor.GRAY
					+ "The quester doesn't have any quests by that name.");
			return;
		}
		quester.removeQuest(quest);
		player.sendMessage(ChatColor.GREEN + "Quest " + StringUtils.wrap(quest)
				+ " removed from " + StringUtils.wrap(npc.getName())
				+ "'s quests. " + StringUtils.wrap(npc.getName()) + " now has "
				+ StringUtils.wrap(quester.getQuests().size()) + " quests.");
	}

	@CommandRequirements()
	@ServerCommand()
	@Command(
			aliases = "quest",
			usage = "help",
			desc = "view the quests help page",
			modifiers = "help",
			min = 1,
			max = 1)
	@CommandPermissions("quester.use.quests.help")
	public static void questHelp(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		sendQuestHelp(sender);
	}

	@CommandRequirements()
	@ServerCommand()
	@Command(
			aliases = "quester",
			usage = "help",
			desc = "view the quester help page",
			modifiers = "help",
			min = 1,
			max = 1)
	@CommandPermissions("quester.use.help")
	public static void questerHelp(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		INSTANCE.sendHelpPage(sender);
	}

	@Command(
			aliases = "quester",
			usage = "quests (page)",
			desc = "view the assigned quests of a quester",
			modifiers = "quests",
			min = 1,
			max = 2)
	@CommandPermissions("quester.use.quests.view")
	public static void viewAssignedQuests(CommandContext args, Player player,
			HumanNPC npc) {
		int page = args.argsLength() == 2 ? args.getInteger(2) : 1;
		if (page < 0)
			page = 1;
		PageInstance instance = PageUtils.newInstance(player);
		Quester quester = npc.getType("quester");
		instance.header(ChatColor.GREEN
				+ StringUtils.listify("Completed Quests " + ChatColor.WHITE
						+ "<%x/%y>" + ChatColor.GREEN));
		for (String quest : quester.getQuests()) {
			if (instance.maxPages() > page)
				break;
			instance.push(ChatColor.GREEN + "   - " + StringUtils.wrap(quest));
		}
		if (page > instance.maxPages()) {
			player.sendMessage(ChatColor.GRAY
					+ "Invalid page entered. There are only "
					+ instance.maxPages() + " pages.");
			return;
		}
		instance.process(page);
	}

	@CommandRequirements()
	@Command(
			aliases = "quest",
			usage = "completed (page)",
			desc = "view completed quests",
			modifiers = "completed",
			min = 1,
			max = 2)
	@CommandPermissions("quester.use.quests.status")
	public static void viewCompleted(CommandContext args, Player player,
			HumanNPC npc) {
		PlayerProfile profile = PlayerProfile.getProfile(player.getName());
		if (profile.getAllCompleted().size() == 0) {
			player.sendMessage(ChatColor.GRAY
					+ "You haven't completed any quests yet.");
			return;
		}
		int page = args.argsLength() == 2 ? args.getInteger(2) : 1;
		if (page < 0)
			page = 1;
		PageInstance instance = PageUtils.newInstance(player);
		instance.header(ChatColor.GREEN
				+ StringUtils.listify("Completed Quests " + ChatColor.WHITE
						+ "<%x/%y>" + ChatColor.GREEN));
		for (CompletedQuest quest : profile.getAllCompleted()) {
			if (instance.maxPages() > page)
				break;
			instance.push(StringUtils.wrap(quest.getName()) + " - taking "
					+ StringUtils.wrap(quest.getHours()) + " hours. Completed "
					+ StringUtils.wrap(quest.getTimesCompleted()) + " times.");
		}
		if (page > instance.maxPages()) {
			player.sendMessage(ChatColor.GRAY
					+ "Invalid page entered. There are only "
					+ instance.maxPages() + " pages.");
			return;
		}
		instance.process(page);
	}

	@CommandRequirements()
	@Command(
			aliases = "quest",
			usage = "status",
			desc = "view current quest status",
			modifiers = "status",
			min = 1,
			max = 1)
	@CommandPermissions("quester.use.quests.status")
	public static void viewCurrentQuestStatus(CommandContext args,
			Player player, HumanNPC npc) {
		PlayerProfile profile = PlayerProfile.getProfile(player.getName());
		if (!profile.hasQuest()) {
			player.sendMessage(ChatColor.GRAY
					+ "You don't have a quest at the moment.");
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "Currently in the middle of "
					+ StringUtils.wrap(profile.getProgress().getQuestName())
					+ ". You have been on this quest for "
					+ StringUtils.wrap(TimeUnit.MINUTES.convert(
							System.currentTimeMillis()
									- profile.getProgress().getStartTime(),
							TimeUnit.MILLISECONDS)) + " minutes.");
			if (profile.getProgress().isFullyCompleted()) {
				player.sendMessage(ChatColor.AQUA + "Quest is completed.");
			} else {
				player.sendMessage(ChatColor.GREEN + "-" + ChatColor.AQUA
						+ " Progress report " + ChatColor.GREEN + "-");
				for (ObjectiveProgress progress : profile.getProgress()
						.getProgress()) {
					if (progress == null)
						continue;
					try {
						Messaging.send(player, progress.getQuestUpdater()
								.getStatus(progress));
					} catch (QuestCancelException ex) {
						player.sendMessage(ChatColor.GRAY
								+ "Cancelling quest. Reason: " + ex.getReason());
						profile.setProgress(null);
					}
				}
			}
		}
	}

	@Override
	public void addPermissions() {
		PermissionManager.addPermission("quester.use.help");
		PermissionManager.addPermission("quester.modify.quests.assign");
		PermissionManager.addPermission("quester.modify.quests.remove");
		PermissionManager.addPermission("quester.use.quests.status");
		PermissionManager.addPermission("quester.use.quests.abort");
		PermissionManager.addPermission("quester.use.quests.view");
		PermissionManager.addPermission("quester.use.quests.help");
	}

	@Override
	public void sendHelpPage(CommandSender sender) {
		HelpUtils.header(sender, "Quester", 1, 1);
		HelpUtils.format(sender, "quest", "help",
				"see more commands for quests");
		HelpUtils.format(sender, "quester", "assign [quest]",
				"assign a quest to an NPC");
		HelpUtils.format(sender, "quester", "remove [quest]",
				"remove a quest from an NPC");
		HelpUtils.format(sender, "quester", "quests (page)",
				"view a quester's assigned quests");
		HelpUtils.footer(sender);
	}

	private static void sendQuestHelp(CommandSender sender) {
		HelpUtils.header(sender, "Quests", 1, 1);
		HelpUtils.format(sender, "quest", "abort", "abort your current quest");
		HelpUtils.format(sender, "quest", "add [player] [npcID] [quest] (-f)",
				"gives a quest to a player");
		HelpUtils.format(sender, "quest", "clear [player|*] [quest|*] (-c)",
				"clear in-progress/completed quests");
		HelpUtils.format(sender, "quest", "completed (page)",
				"view your completed quests");
		HelpUtils.format(sender, "quest", "status",
				"view your current quest status");
		HelpUtils.footer(sender);
	}
}