package net.citizensnpcs.commands;

import java.util.ArrayDeque;

import net.citizensnpcs.Citizens;
import net.citizensnpcs.SettingsManager;
import net.citizensnpcs.api.event.citizens.CitizensReloadEvent;
import net.citizensnpcs.api.event.npc.NPCCreateEvent.NPCCreateReason;
import net.citizensnpcs.api.event.npc.NPCRemoveEvent.NPCRemoveReason;
import net.citizensnpcs.economy.EconomyManager;
import net.citizensnpcs.npcdata.NPCDataManager;
import net.citizensnpcs.npctypes.CitizensNPC;
import net.citizensnpcs.permissions.PermissionManager;
import net.citizensnpcs.properties.PropertyManager;
import net.citizensnpcs.properties.properties.UtilityProperties;
import net.citizensnpcs.resources.npclib.CraftNPC;
import net.citizensnpcs.resources.npclib.HumanNPC;
import net.citizensnpcs.resources.npclib.NPCManager;
import net.citizensnpcs.resources.npclib.creatures.CreatureNPC;
import net.citizensnpcs.resources.sk89q.Command;
import net.citizensnpcs.resources.sk89q.CommandContext;
import net.citizensnpcs.resources.sk89q.CommandPermissions;
import net.citizensnpcs.resources.sk89q.CommandRequirements;
import net.citizensnpcs.resources.sk89q.ServerCommand;
import net.citizensnpcs.utils.HelpUtils;
import net.citizensnpcs.utils.MessageUtils;
import net.citizensnpcs.utils.Messaging;
import net.citizensnpcs.utils.ServerUtils;
import net.citizensnpcs.utils.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@CommandRequirements(requireSelected = true, requireOwnership = true)
public class BasicCommands extends CommandHandler {

	@Command(
			aliases = "npc",
			usage = "add [text]",
			desc = "add text to an NPC",
			modifiers = "add",
			min = 2)
	@CommandPermissions("basic.modify.addtext")
	public static void add(CommandContext args, Player player, HumanNPC npc) {
		String text = args.getJoinedStrings(1);
		NPCDataManager.addText(npc.getUID(), text);
		player.sendMessage(StringUtils.wrap(text) + " was added to "
				+ StringUtils.wrap(npc.getStrippedName() + "'s") + " text.");
	}

	@Command(aliases = "citizens", desc = "view Citizens info", max = 0)
	@ServerCommand()
	@CommandPermissions("admin.info")
	@CommandRequirements()
	public static void citizens(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		sender.sendMessage(ChatColor.GREEN
				+ StringUtils.listify(StringUtils.wrap("Citizens")));
		sender.sendMessage(ChatColor.GREEN + "  Version: "
				+ StringUtils.wrap(Citizens.localVersion()));
		sender.sendMessage(ChatColor.GREEN + "  Authors: ");
		sender.sendMessage(ChatColor.YELLOW + "      - fullwall");
		sender.sendMessage(ChatColor.YELLOW + "      - aPunch");
	}

	@Command(
			aliases = "citizens",
			usage = "help (page)",
			desc = "view the Citizens help page",
			modifiers = "help",
			min = 1,
			max = 2)
	@CommandPermissions("basic.use.help")
	@CommandRequirements()
	@ServerCommand()
	public static void citizensHelp(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		int page = 1;
		if (args.argsLength() == 2) {
			page = Integer.parseInt(args.getString(1));
		}
		HelpUtils.sendHelpPage(sender, page);
	}

	@Command(
			aliases = "citizens",
			desc = "view Citizens info",
			modifiers = "clean",
			max = 1)
	@ServerCommand()
	@CommandPermissions("admin.clean")
	@CommandRequirements()
	public static void clean(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		sender.sendMessage(ChatColor.GRAY + "Cleaning up...");
		int count = 0;
		for (World world : Bukkit.getServer().getWorlds()) {
			for (Entity entity : world.getEntities()) {
				net.minecraft.server.Entity mcEntity = ((CraftEntity) entity)
						.getHandle();
				if (mcEntity instanceof CraftNPC
						&& !(mcEntity instanceof CreatureNPC)) {
					HumanNPC found = ((CraftNPC) mcEntity).npc;
					if (NPCManager.getList().get(found.getUID()) != found) {
						found.getPlayer().remove();
						++count;
					}
				}
			}
		}
		sender.sendMessage(ChatColor.GREEN + "Done. Removed "
				+ StringUtils.wrap(count) + " orphaned NPCs.");
	}

	@Command(
			aliases = "npc",
			usage = "color [color-code]",
			desc = "set the name color of an NPC",
			modifiers = "color",
			min = 2,
			max = 2)
	@CommandPermissions("basic.modify.color")
	public static void color(CommandContext args, Player player, HumanNPC npc) {
		if (!args.getString(1).substring(0, 1).equals("&")) {
			player.sendMessage(ChatColor.RED + "Use an & to specify color.");
		} else if (args.getString(1).length() != 2) {
			player.sendMessage(ChatColor.GRAY
					+ "Use the format &(code). Example - &f = white.");
		} else {
			int colour = 0xf;
			try {
				colour = Integer.parseInt(args.getString(1).substring(1, 2));
			} catch (NumberFormatException ex) {
				try {
					colour = Integer.parseInt(
							args.getString(1).substring(1, 2), 16);
				} catch (NumberFormatException e) {
					player.sendMessage(ChatColor.RED + "Invalid color code.");
					return;
				}
			}
			if (ChatColor.getByCode(colour) == null) {
				player.sendMessage(ChatColor.RED + "Color code not recognised.");
				return;
			}
			npc.getNPCData().setColour(ChatColor.getByCode(colour));
			NPCManager.setColour(npc.getUID(), npc.getOwner());
			player.sendMessage(StringUtils.wrapFull("{" + npc.getStrippedName()
					+ "}'s name color is now "
					+ args.getString(1).replace("&", "\u00A7") + "this}."));
		}
	}

	@Command(
			aliases = "npc",
			usage = "copy",
			desc = "copy an NPC",
			modifiers = "copy",
			min = 1,
			max = 1)
	@CommandPermissions("basic.modify.copy")
	public static void copy(CommandContext args, Player player, HumanNPC npc) {
		if (!PermissionManager.canCreate(player)) {
			player.sendMessage(MessageUtils.reachedNPCLimitMessage);
			return;
		}
		PropertyManager.save(npc);
		int newUID = NPCManager.register(npc.getName(), player.getLocation(),
				player.getName(), NPCCreateReason.COMMAND);
		HumanNPC newNPC = NPCManager.get(newUID);
		newNPC.teleport(player.getLocation());
		PropertyManager.copyNPCs(npc.getUID(), newUID);
		PropertyManager.save(newNPC);
		PropertyManager.load(newNPC);
		newNPC.getNPCData().setLocation(player.getLocation());
		PropertyManager.save(newNPC);
		player.sendMessage(StringUtils.wrap(npc.getStrippedName())
				+ " has been copied at your location.");
	}

	@CommandRequirements()
	@Command(
			aliases = "npc",
			usage = "create [name] (text)",
			desc = "create an NPC",
			modifiers = "create",
			min = 2)
	@CommandPermissions("basic.create")
	public static void create(CommandContext args, Player player, HumanNPC npc) {
		if (!PermissionManager.canCreate(player)) {
			player.sendMessage(MessageUtils.reachedNPCLimitMessage);
			return;
		}
		ArrayDeque<String> texts = new ArrayDeque<String>();
		String firstArg = args.getString(1);
		if (args.argsLength() >= 3) {
			texts.add(args.getJoinedStrings(2));
		}
		if (firstArg.length() > 16) {
			player.sendMessage(ChatColor.RED
					+ "The name of this NPC will be truncated - max name length is 16.");
			firstArg = args.getString(1).substring(0, 16);
		}
		if (EconomyManager.useEconPlugin()) {
			if (EconomyManager.hasEnough(player,
					UtilityProperties.getPrice("basic.creation"))) {
				double paid = EconomyManager.pay(player,
						UtilityProperties.getPrice("basic.creation"));
				if (paid > 0) {
					player.sendMessage(MessageUtils.getPaidMessage(player,
							"basic", "basic.creation",
							firstArg.replace("/", " "), false));
				}
			} else {
				player.sendMessage(MessageUtils.getNoMoneyMessage(player,
						"basic.creation"));
				return;
			}
		}
		int UID = NPCManager.register(firstArg, player.getLocation(),
				player.getName(), NPCCreateReason.COMMAND);
		NPCDataManager.setText(UID, texts);

		HumanNPC created = NPCManager.get(UID);
		created.getNPCData().setOwner(player.getName());
		Messaging.send(player, created,
				SettingsManager.getString("CreationMessage"));

		NPCDataManager.selectNPC(player, NPCManager.get(UID));
		Messaging.send(player, created,
				SettingsManager.getString("SelectionMessage"));
	}

	@CommandRequirements()
	@Command(
			aliases = "citizens",
			usage = "debug",
			desc = "toggle debug mode for Citizens",
			modifiers = "debug",
			min = 1,
			max = 1)
	@ServerCommand()
	@CommandPermissions("admin.debug")
	public static void debug(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		boolean debug = SettingsManager.getBoolean("DebugMode");
		UtilityProperties.getSettings().setRaw("general.debug-mode", !debug);
		debug = !debug;
		if (debug) {
			Messaging.log("Debug mode is now on.");
			if (sender instanceof Player) {
				Messaging.send(sender, npc, "Debug mode is now "
						+ ChatColor.GREEN + "on");
			}
		} else {
			Messaging.log("Debug mode is now off.");
			if (sender instanceof Player) {
				Messaging.send(sender, npc, "Debug mode is now "
						+ ChatColor.RED + "off");
			}
		}
	}

	@Command(
			aliases = "npc",
			usage = "equip",
			desc = "toggle equip mode",
			modifiers = "equip",
			min = 1,
			max = 1)
	@CommandPermissions("basic.modify.equip")
	public static void equip(CommandContext args, Player player, HumanNPC npc) {
		if (NPCDataManager.pathEditors.containsKey(player.getName())) {
			Messaging.sendError(player,
					"You can only be in one editor at a time.");
			return;
		}
		Integer editing = NPCDataManager.equipmentEditors.get(player);
		int UID = npc.getUID();
		if (editing == null) {
			player.sendMessage(ChatColor.GREEN
					+ StringUtils.listify(StringUtils.wrap("Now Editing "
							+ npc.getStrippedName() + "'s Items")));
			player.sendMessage(StringUtils.wrap("Right-click")
					+ " to set an NPC's equipment.");
			player.sendMessage(ChatColor.GREEN
					+ "Hold nothing in your hand to remove "
					+ StringUtils.wrap("all") + " items.");
			player.sendMessage(StringUtils.wrap("Sneak")
					+ " to set the item-in-hand to armor.");
			player.sendMessage(StringUtils.wrap("Repeat")
					+ " the command to exit equipment-edit mode.");
			editing = UID;
		} else if (editing == UID) {
			player.sendMessage(StringUtils.wrap("Exited")
					+ " equipment-edit mode.");
			NPCDataManager.equipmentEditors.remove(player);
			editing = null;
			return;
		} else if (editing != UID) {
			player.sendMessage(ChatColor.GRAY + "Now editing "
					+ StringUtils.wrap(npc.getStrippedName()) + "'s equipment.");
			editing = UID;
		}
		NPCDataManager.equipmentEditors.put(player, editing);
	}

	@CommandRequirements()
	@Command(
			aliases = "npc",
			usage = "list (name) (page)",
			desc = "view a list of NPCs for a player",
			modifiers = "list",
			min = 1,
			max = 3)
	@CommandPermissions("basic.use.list")
	public static void list(CommandContext args, Player player, HumanNPC npc) {
		switch (args.argsLength()) {
		case 1:
			MessageUtils.displayNPCList(player, player, npc, "1");
			break;
		case 2:
			if (StringUtils.isNumber(args.getString(1))) {
				MessageUtils.displayNPCList(player, player, npc,
						args.getString(1));
			} else {
				if (ServerUtils.matchPlayer(args.getString(1)) != null) {
					MessageUtils.displayNPCList(player,
							ServerUtils.matchPlayer(args.getString(1)), npc,
							"1");
				} else {
					player.sendMessage(ChatColor.RED
							+ "Could not match player.");
				}
			}
			break;
		case 3:
			if (ServerUtils.matchPlayer(args.getString(1)) != null) {
				MessageUtils.displayNPCList(player,
						ServerUtils.matchPlayer(args.getString(1)), npc,
						args.getString(2));
			} else {
				player.sendMessage(ChatColor.RED + "Could not match player.");
			}
			break;
		}
	}

	@Command(
			aliases = "npc",
			usage = "lookat",
			desc = "set an NPC's look-when-close setting",
			modifiers = "lookat",
			min = 1,
			max = 1)
	@CommandPermissions("basic.modify.lookat")
	public static void lookAt(CommandContext args, Player player, HumanNPC npc) {
		npc.getNPCData().setLookClose(!npc.getNPCData().isLookClose());
		if (npc.getNPCData().isLookClose()) {
			player.sendMessage(StringUtils.wrap(npc.getStrippedName())
					+ " will now look at players.");
		} else {
			player.sendMessage(StringUtils.wrap(npc.getStrippedName())
					+ " will stop looking at players.");
		}
	}

	@Command(
			aliases = "npc",
			usage = "move",
			desc = "move an NPC",
			modifiers = "move",
			min = 1,
			max = 1)
	@CommandPermissions("basic.modify.move")
	public static void move(CommandContext args, Player player, HumanNPC npc) {
		if (npc.getWorld() != player.getWorld()
				&& !PermissionManager.hasPermission(player,
						"citizens.basic.modify.move.multiworld")) {
			player.sendMessage(ChatColor.GRAY
					+ "You don't have permission to move NPCs between worlds.");
			return;
		}
		player.sendMessage(StringUtils.wrap(npc.getStrippedName())
				+ " is en route to your location!");
		npc.teleport(player.getLocation());
		npc.getNPCData().setLocation(player.getLocation());
	}

	@Command(
			aliases = "npc",
			usage = "moveto [x y z] (world pitch yaw)",
			desc = "move an NPC to a location",
			modifiers = "moveto",
			min = 4,
			max = 7)
	@CommandPermissions("basic.modify.moveto")
	public static void moveTo(CommandContext args, Player player, HumanNPC npc) {
		int index = 1;
		double x = 0, y = 0, z = 0;
		float yaw = npc.getLocation().getYaw();
		float pitch = npc.getLocation().getPitch();
		String world = npc.getWorld().getName();
		switch (args.argsLength()) {
		case 4:
			x = Double.parseDouble(args.getString(index));
			index++;
			y = Double.parseDouble(args.getString(index));
			index++;
			z = Double.parseDouble(args.getString(index));
			break;
		case 5:
			x = Double.parseDouble(args.getString(index));
			index++;
			y = Double.parseDouble(args.getString(index));
			index++;
			z = Double.parseDouble(args.getString(index));
			index++;
			world = args.getString(index);
			if (Bukkit.getServer().getWorld(world) == null) {
				Messaging.sendError(player, "Invalid world.");
				return;
			}
			break;
		case 6:
			x = Double.parseDouble(args.getString(index));
			index++;
			y = Double.parseDouble(args.getString(index));
			index++;
			z = Double.parseDouble(args.getString(index));
			index++;
			world = args.getString(index);
			if (Bukkit.getServer().getWorld(world) == null) {
				Messaging.sendError(player, "Invalid world.");
				return;
			}
			index++;
			pitch = Float.parseFloat(args.getString(index));
			break;
		case 7:
			x = Double.parseDouble(args.getString(index));
			index++;
			y = Double.parseDouble(args.getString(index));
			index++;
			z = Double.parseDouble(args.getString(index));
			index++;
			world = args.getString(index);
			if (Bukkit.getServer().getWorld(world) == null) {
				Messaging.sendError(player, "Invalid world.");
				return;
			}
			index++;
			pitch = Float.parseFloat(args.getString(index));
			index++;
			yaw = Float.parseFloat(args.getString(index));
		}
		Location loc = new Location(Bukkit.getServer().getWorld(world), x, y,
				z, pitch, yaw);
		npc.teleport(loc);
		npc.getNPCData().setLocation(loc);
		player.sendMessage(StringUtils.wrap(npc.getStrippedName())
				+ " moved to the coordinates " + StringUtils.wrap(x) + ", "
				+ StringUtils.wrap(y) + ", " + StringUtils.wrap(z)
				+ " in the world " + StringUtils.wrap(world) + ".");
	}

	@CommandRequirements(requireSelected = true)
	@Command(aliases = "npc", desc = "view information for an NPC", max = 0)
	@CommandPermissions("basic.use.info")
	public static void npc(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		sender.sendMessage(ChatColor.GREEN
				+ StringUtils.listify(StringUtils.wrap(npc.getStrippedName())));
		sender.sendMessage(ChatColor.GREEN + "ID: "
				+ StringUtils.wrap(npc.getUID()));
		sender.sendMessage(ChatColor.GREEN + "Owner: "
				+ StringUtils.wrap(npc.getOwner()));
		sender.sendMessage(ChatColor.GREEN + "Types:");
		if (npc.types().size() == 0) {
			sender.sendMessage(ChatColor.RED + "    None");
			return;
		}
		for (CitizensNPC type : npc.types()) {
			sender.sendMessage(ChatColor.GRAY + "    - "
					+ StringUtils.wrap(type.getType().getName()));
		}
	}

	@CommandRequirements()
	@Command(
			aliases = "npc",
			usage = "help (page)",
			desc = "view the Basic NPC help page",
			modifiers = "help",
			min = 1,
			max = 2)
	@CommandPermissions("basic.use.help")
	@ServerCommand()
	public static void npcHelp(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		int page = 1;
		if (args.argsLength() == 2) {
			page = Integer.parseInt(args.getString(1));
		}
		HelpUtils.sendBasicHelpPage(sender, page);
	}

	@CommandRequirements()
	@Command(
			aliases = "citizens",
			usage = "reload",
			desc = "reload Citizens",
			modifiers = "reload",
			min = 1,
			max = 1)
	@CommandPermissions("admin.reload")
	@ServerCommand()
	public static void reload(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		Messaging.log("Reloading configuration settings....");
		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.GREEN + "["
					+ StringUtils.wrap("Citizens") + "] Reloading....");
		}

		UtilityProperties.initialize();
		PropertyManager.saveState();
		PropertyManager.loadAll();
		SettingsManager.setupVariables();

		Bukkit.getServer().getPluginManager()
				.callEvent(new CitizensReloadEvent());

		Messaging.log("Reloaded.");
		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.GREEN + "["
					+ StringUtils.wrap("Citizens") + "] Reloaded.");
		}
	}

	@CommandRequirements()
	@Command(
			aliases = "npc",
			usage = "remove (all)",
			desc = "remove NPCs",
			modifiers = "remove",
			min = 1,
			max = 2)
	public static void remove(CommandContext args, Player player, HumanNPC npc) {
		if (args.argsLength() == 2 && args.getString(1).equalsIgnoreCase("all")) {
			if (PermissionManager.hasPermission(player,
					"citizens.basic.modify.remove.all")) {
				if (NPCManager.GlobalUIDs.size() == 0) {
					Messaging.sendError(player, "There are no NPCs to remove.");
					return;
				}
				NPCManager.removeAll(NPCRemoveReason.COMMAND);
				NPCDataManager.deselectNPC(player);
				player.sendMessage(ChatColor.GRAY + "The NPC(s) disappeared.");
			} else {
				Messaging.sendError(player, MessageUtils.noPermissionsMessage);
			}
			return;
		}
		if (npc == null) {
			player.sendMessage(MessageUtils.mustHaveNPCSelectedMessage);
			return;
		}
		if ((!NPCManager.validateOwnership(player, npc.getUID()) && PermissionManager
				.hasPermission(player, "citizens.admin.override.remove"))
				|| (NPCManager.validateOwnership(player, npc.getUID()) && PermissionManager
						.hasPermission(player, "citizens.basic.modify.remove"))) {
			NPCManager.remove(npc.getUID(), NPCRemoveReason.COMMAND);
			NPCDataManager.deselectNPC(player);
			player.sendMessage(StringUtils.wrap(npc.getStrippedName(),
					ChatColor.GRAY) + " disappeared.");
			return;
		}
		Messaging.sendError(player, MessageUtils.noPermissionsMessage);
	}

	@Command(
			aliases = "npc",
			usage = "rename [name]",
			desc = "rename an NPC",
			modifiers = "rename",
			min = 2,
			max = 2)
	@CommandPermissions("basic.modify.rename")
	public static void rename(CommandContext args, Player player, HumanNPC npc) {
		String name = args.getString(1);
		if (name.length() > 16) {
			player.sendMessage(ChatColor.RED
					+ "Max name length is 16 - NPC name length will be truncated.");
			name = name.substring(0, 16);
		}
		NPCManager.rename(npc.getUID(), name, npc.getOwner());
		player.sendMessage(ChatColor.GREEN + StringUtils.wrap(npc.getName())
				+ "'s name was set to " + StringUtils.wrap(name) + ".");
	}

	@Command(
			aliases = "npc",
			usage = "reset",
			desc = "reset the text of an NPC",
			modifiers = "reset",
			min = 1,
			max = 1)
	@CommandPermissions("basic.modify.resettext")
	public static void reset(CommandContext args, Player player, HumanNPC npc) {
		NPCDataManager.resetText(npc.getUID());
		player.sendMessage(StringUtils.wrap(npc.getStrippedName() + "'s")
				+ " text was reset!");
	}

	@CommandRequirements()
	@Command(
			aliases = "citizens",
			usage = "save",
			desc = "force a save of Citizens files",
			modifiers = "save",
			min = 1,
			max = 1)
	@ServerCommand()
	@CommandPermissions("admin.save")
	public static void save(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		if (sender instanceof Player) {
			Messaging.log("Saving...");
		}
		sender.sendMessage(ChatColor.GREEN + "[" + StringUtils.wrap("Citizens")
				+ "] Saving...");

		PropertyManager.saveState();

		if (sender instanceof Player) {
			Messaging.log("Saved.");
		}
		sender.sendMessage(ChatColor.GREEN + "[" + StringUtils.wrap("Citizens")
				+ "] Saved.");
	}

	@Command(
			aliases = "npc",
			usage = "select [id]",
			desc = "select an NPC by its ID",
			modifiers = "select",
			min = 2,
			max = 2)
	@CommandPermissions("basic.use.select")
	@CommandRequirements()
	public static void select(CommandContext args, Player player, HumanNPC npc) {
		npc = NPCManager.get(args.getInteger(1));
		if (npc == null) {
			player.sendMessage(ChatColor.RED + "No NPC with ID "
					+ StringUtils.wrap(args.getString(1), ChatColor.RED)
					+ " exists.");
		} else {
			NPCDataManager.selectNPC(player, npc);
			Messaging.send(player, npc,
					SettingsManager.getString("SelectionMessage"));
		}
	}

	@Command(
			aliases = "npc",
			usage = "set [text]",
			desc = "set the text of an NPC",
			modifiers = "set",
			min = 2)
	@CommandPermissions("basic.modify.settext")
	public static void set(CommandContext args, Player player, HumanNPC npc) {
		String text = args.getJoinedStrings(1);
		ArrayDeque<String> texts = new ArrayDeque<String>();
		texts.add(text);
		NPCDataManager.setText(npc.getUID(), texts);
		player.sendMessage(StringUtils.wrapFull("{" + npc.getStrippedName()
				+ "}'s text was set to {" + text + "}."));
	}

	@CommandRequirements()
	@Command(
			aliases = "npc",
			usage = "setowner [name]",
			desc = "set the owner of an NPC",
			modifiers = "setowner",
			min = 2,
			max = 2)
	public static void setOwner(CommandContext args, Player player, HumanNPC npc) {
		if ((!NPCManager.validateOwnership(player, npc.getUID()) && PermissionManager
				.hasPermission(player, "citizens.admin.override.setowner"))
				|| (NPCManager.validateOwnership(player, npc.getUID()) && PermissionManager
						.hasPermission(player, "citizens.basic.modify.setowner"))) {
			player.sendMessage(ChatColor.GREEN + "The owner of "
					+ StringUtils.wrap(npc.getStrippedName()) + " is now "
					+ StringUtils.wrap(args.getString(1)) + ".");
			npc.getNPCData().setOwner(args.getString(1));
			return;
		}
		Messaging.sendError(player, MessageUtils.noPermissionsMessage);
	}

	@Command(
			aliases = "npc",
			usage = "talkclose",
			desc = "toggle an NPC's talk-when-close setting",
			modifiers = "talkclose",
			min = 1,
			max = 1)
	@CommandPermissions("basic.modify.talkclose")
	public static void talkClose(CommandContext args, Player player,
			HumanNPC npc) {
		npc.getNPCData().setTalkClose(!npc.getNPCData().isTalkClose());
		if (npc.getNPCData().isTalkClose()) {
			player.sendMessage(StringUtils.wrap(npc.getStrippedName())
					+ " will now talk to nearby players.");
		} else {
			player.sendMessage(StringUtils.wrap(npc.getStrippedName())
					+ " will stop talking to nearby players.");
		}
	}

	@Command(
			aliases = "npc",
			usage = "tp",
			desc = "teleport to an NPC",
			modifiers = { "tp", "teleport" },
			min = 1,
			max = 1)
	@CommandPermissions("basic.use.teleport")
	public static void teleport(CommandContext args, Player player, HumanNPC npc) {
		player.teleport(npc.getNPCData().getLocation());
		player.sendMessage(ChatColor.GREEN + "Teleported you to "
				+ StringUtils.wrap(npc.getStrippedName()) + ". Enjoy!");
	}

	@Command(
			aliases = "npc",
			usage = "[path|waypoints] (reset)",
			desc = "toggle waypoint editing",
			modifiers = { "path", "waypoints" },
			min = 1,
			max = 2)
	@CommandPermissions("waypoints.edit")
	public static void waypoints(CommandContext args, Player player,
			HumanNPC npc) {
		if (args.length() == 2) {
			if (NPCDataManager.equipmentEditors.containsKey(player)) {
				Messaging.sendError(player,
						"You can only be in one editor at a time.");
				return;
			}
			Integer editing = NPCDataManager.pathEditors.get(player.getName());
			int UID = npc.getUID();
			if (editing == null) {
				player.sendMessage(ChatColor.AQUA
						+ StringUtils.listify("Waypoint Editing Controls"));
				player.sendMessage(StringUtils.wrap("Left")
						+ " click adds a waypoint, while "
						+ StringUtils.wrap("right") + " click acts as an undo.");
				player.sendMessage(StringUtils.wrap("Repeat")
						+ " this command to finish.");
				editing = UID;
				npc.setPaused(true);
			} else if (editing == UID) {
				player.sendMessage(StringUtils.wrap("Finished")
						+ " editing waypoints.");
				NPCDataManager.pathEditors.remove(player.getName());
				editing = null;
				npc.setPaused(false);
				return;
			} else if (editing != UID) {
				player.sendMessage(ChatColor.GRAY + "Now editing "
						+ StringUtils.wrap(npc.getStrippedName())
						+ "'s waypoints.");
				editing = UID;
			}
			NPCDataManager.pathEditors.put(player.getName(), editing);
		} else if (args.length() >= 3 && args.getString(1).equals("reset")) {
			npc.getWaypoints().resetWaypoints();
			player.sendMessage(ChatColor.GREEN + "Waypoints "
					+ StringUtils.wrap("reset") + ".");
		}
	}

	@Override
	public void addPermissions() {
		PermissionManager.addPermission("admin.info");
		PermissionManager.addPermission("admin.clean");
		PermissionManager.addPermission("basic.use.help");
		PermissionManager.addPermission("admin.save");
		PermissionManager.addPermission("admin.debug");
		PermissionManager.addPermission("admin.reload");
		PermissionManager.addPermission("basic.modify.move");
		PermissionManager.addPermission("basic.create");
		PermissionManager.addPermission("basic.use.info");
		PermissionManager.addPermission("basic.modify.moveto");
		PermissionManager.addPermission("basic.modify.copy");
		PermissionManager.addPermission("basic.modify.remove");
		PermissionManager.addPermission("basic.modify.remove.all");
		PermissionManager.addPermission("basic.modify.rename");
		PermissionManager.addPermission("basic.modify.color");
		PermissionManager.addPermission("basic.modify.addtext");
		PermissionManager.addPermission("basic.modify.resettext");
		PermissionManager.addPermission("basic.modify.settext");
		PermissionManager.addPermission("basic.modify.equip");
		PermissionManager.addPermission("basic.use.teleport");
		PermissionManager.addPermission("basic.modify.talkclose");
		PermissionManager.addPermission("basic.modify.lookat");
		PermissionManager.addPermission("basic.use.select");
		PermissionManager.addPermission("basic.modify.setowner");
		PermissionManager.addPermission("waypoints.edit");
		PermissionManager.addPermission("basic.use.list");
		PermissionManager.addPermission("admin.override.setowner");
		PermissionManager.addPermission("admin.override.remove");
	}
}