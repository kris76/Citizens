package com.fullwall.Citizens.Traders;

import java.util.HashMap;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.InventoryPlayer;
import net.minecraft.server.Packet103SetSlot;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.fullwall.Citizens.Citizens;
import com.fullwall.Citizens.Economy.EconomyHandler;
import com.fullwall.Citizens.Economy.ServerEconomyInterface;
import com.fullwall.Citizens.Economy.Payment;
import com.fullwall.Citizens.Traders.TraderInterface.Mode;
import com.fullwall.Citizens.Utils.MessageUtils;
import com.fullwall.Citizens.Utils.StringUtils;
import com.fullwall.resources.redecouverte.NPClib.HumanNPC;

public class TraderTask implements Runnable {
	private HumanNPC npc;
	private CraftPlayer player;
	private int taskID;
	private int previousTraderClickedSlot = -1;
	private int previousPlayerClickedSlot = -1;
	private Citizens plugin;
	private PlayerInventory previousTraderInv;
	private PlayerInventory previousPlayerInv;
	private Mode mode;
	private EntityPlayer eplayer;
	private boolean stop;

	/**
	 * Gets run every tick, checks the inventory for changes.
	 * 
	 * @param npc
	 * @param player
	 * @param plugin
	 * @param mode
	 */
	public TraderTask(HumanNPC npc, Player player, Citizens plugin, Mode mode) {
		this.npc = npc;
		this.player = (CraftPlayer) player;
		this.eplayer = this.player.getHandle();
		this.plugin = plugin;
		// Create the inventory objects
		this.previousTraderInv = new CraftInventoryPlayer(new InventoryPlayer(
				null));
		this.previousPlayerInv = new CraftInventoryPlayer(new InventoryPlayer(
				null));
		// clone the items to the newly created inventory objects
		clonePlayerInventory(npc.getInventory(), this.previousTraderInv);
		clonePlayerInventory(player.getInventory(), this.previousPlayerInv);

		this.mode = mode;
		sendJoinMessage();
	}

	@Override
	public void run() {
		if (stop)
			return;
		if (npc == null || player == null
				|| eplayer.activeContainer == eplayer.defaultContainer) {
			kill();
			return;
		}
		if (mode == Mode.STOCK)
			return;
		// If the player cursor is empty (no itemstack in it).

		stop = true;
		int count = 0;

		boolean found = false;
		for (ItemStack i : npc.getInventory().getContents()) {
			if (!previousTraderInv.getItem(count).equals(i)
					&& player.getHandle().inventory.j() == null) {
				restorePreviousState();
				break;
			}
			if (!previousTraderInv.getItem(count).equals(i)
					&& previousTraderInv.getItem(count).getTypeId() == eplayer.inventory
							.j().id) {
				found = true;
				handleTraderItemClicked(count, npc.getInventory());
				break;
			}
			count += 1;
		}

		count = 0;
		if (!found) {
			for (ItemStack i : player.getInventory().getContents()) {
				if (!previousPlayerInv.getItem(count).equals(i)
						&& player.getHandle().inventory.j() == null) {
					restorePreviousState();
					break;
				}
				if (!previousPlayerInv.getItem(count).equals(i)
						&& previousPlayerInv.getItem(count).getTypeId() == eplayer.inventory
								.j().id) {
					handlePlayerItemClicked(count, player.getInventory());
					break;
				}
				count += 1;
			}
		}

		clonePlayerInventory(npc.getInventory(), this.previousTraderInv);
		clonePlayerInventory(player.getInventory(), this.previousPlayerInv);

		// Set the itemstack in the player's cursor to null.
		eplayer.inventory.b((net.minecraft.server.ItemStack) null);
		// Get rid of the picture on the cursor.
		Packet103SetSlot packet = new Packet103SetSlot(-1, -1, null);
		eplayer.netServerHandler.sendPacket(packet);
		stop = false;
	}

	@SuppressWarnings("deprecation")
	private void handleTraderItemClicked(int slot, PlayerInventory npcInv) {
		npcInv.setItem(slot, previousTraderInv.getItem(slot));
		ItemStack i = npcInv.getItem(slot);
		Stockable stockable = getStockable(i, "sold", false);
		if (stockable == null)
			return;
		if (previousTraderClickedSlot != slot) {
			previousTraderClickedSlot = slot;
			sendStockableMessage("Buying ", stockable);
			return;
		}
		previousTraderClickedSlot = slot;
		previousPlayerClickedSlot = -1;
		if (checkMiscellaneous(npcInv, stockable, true))
			return;
		ItemStack buying = stockable.getStocking();
		EconomyHandler.pay(new Payment(stockable.getPrice()), player, -1);
		if (mode != Mode.INFINITE)
			EconomyHandler.pay(new Payment(buying, false), npc, slot);
		HashMap<Integer, ItemStack> unbought = player.getInventory().addItem(
				buying);
		if (unbought.size() >= 1) {
			restorePreviousState();
			player.sendMessage(ChatColor.RED
					+ "Not enough room in your inventory to add "
					+ MessageUtils.getStackToString(buying, ChatColor.RED)
					+ ".");
			return;
		}
		if (!stockable.isiConomy() && mode != Mode.INFINITE) {
			unbought = npc.getInventory().addItem(
					stockable.getPrice().getItemStack());
			if (unbought.size() >= 1) {
				restorePreviousState();
				player.sendMessage(ChatColor.RED
						+ "Not enough room in the npc's inventory to add "
						+ stockable.getString(ChatColor.RED) + ".");
				return;
			}
		} else {
			npc.setBalance(npc.getBalance() + stockable.getPrice().getPrice());
		}
		npc.getPlayer().updateInventory();
		player.updateInventory();
		player.sendMessage(ChatColor.GREEN + "Transaction successful.");
	}

	@SuppressWarnings("deprecation")
	private void handlePlayerItemClicked(int slot, PlayerInventory playerInv) {
		playerInv.setItem(slot, previousPlayerInv.getItem(slot));
		ItemStack i = playerInv.getItem(slot);
		Stockable stockable = getStockable(i, "bought", true);
		if (stockable == null)
			return;
		if (previousPlayerClickedSlot != slot) {
			previousPlayerClickedSlot = slot;
			sendStockableMessage("Selling ", stockable);
			return;
		}
		previousPlayerClickedSlot = slot;
		previousTraderClickedSlot = -1;
		if (checkMiscellaneous(playerInv, stockable, false))
			return;
		ItemStack selling = stockable.getStocking();
		if (mode != Mode.INFINITE)
			EconomyHandler.pay(new Payment(stockable.getPrice()), npc, -1);
		EconomyHandler.pay(new Payment(stockable.getStocking(), false), player,
				slot);
		HashMap<Integer, ItemStack> unsold = new HashMap<Integer, ItemStack>();
		if (mode != Mode.INFINITE) {
			unsold = npc.getInventory().addItem(stockable.getStocking());
		}
		if (unsold.size() >= 1) {
			restorePreviousState();
			player.sendMessage(ChatColor.RED
					+ "Not enough room available to add "
					+ MessageUtils.getStackToString(selling, ChatColor.RED)
					+ " to the trader's stock.");
			return;
		}
		if (!stockable.isiConomy()) {
			unsold = player.getInventory().addItem(
					stockable.getPrice().getItemStack());
			if (unsold.size() >= 1) {
				restorePreviousState();
				player.sendMessage(ChatColor.RED
						+ "Not enough room in your inventory to add "
						+ stockable.getString(ChatColor.RED) + ".");
				return;
			}
		} else {
			ServerEconomyInterface.add(player.getName(), stockable.getPrice()
					.getPrice());
		}
		npc.getPlayer().updateInventory();
		player.updateInventory();
		player.sendMessage(ChatColor.GREEN + "Transaction successful.");
	}

	private void restorePreviousState() {
		player.getInventory().setContents(previousPlayerInv.getContents());
		npc.getInventory().setContents(previousTraderInv.getContents());
	}

	private boolean checkMiscellaneous(PlayerInventory inv,
			Stockable stockable, boolean buying) {
		ItemStack stocking = stockable.getStocking();
		if (buying) {
			if (!EconomyHandler.canBuy(new Payment(stocking, false), npc)) {
				sendNoMoneyMessage(stocking, "sell", "The trader doesn't");
				return true;
			}
			if (!EconomyHandler.canBuy(new Payment(stockable.getPrice()),
					player)) {
				sendNoMoneyMessage(stocking, "buy", "You don't");
				return true;
			}
		} else {
			if (!EconomyHandler.canBuy(new Payment(stocking, false), player)) {
				sendNoMoneyMessage(stocking, "sell", "You don't");
				return true;
			}
			if (!EconomyHandler.canBuy(new Payment(stockable.getPrice()), npc)) {
				sendNoMoneyMessage(stocking, "buy", "The trader doesn't");
				return true;
			}
		}
		return false;
	}

	private void sendNoMoneyMessage(ItemStack stocking, String keyword,
			String start) {
		player.sendMessage(ChatColor.RED
				+ start
				+ " have enough money available to "
				+ keyword
				+ " "
				+ StringUtils.yellowify(stocking.getAmount() + " "
						+ stocking.getType().name(), ChatColor.RED) + "(s).");
	}

	private void sendStockableMessage(String keyword, Stockable stockable) {
		if (stockable.isSelling())
			player.sendMessage(ChatColor.AQUA
					+ keyword
					+ MessageUtils.getReverseStockableMessage(stockable,
							ChatColor.AQUA) + ".");
		else
			player.sendMessage(ChatColor.AQUA
					+ keyword
					+ MessageUtils.getStockableMessage(stockable,
							ChatColor.AQUA) + ".");
	}

	private Stockable getStockable(ItemStack i, String keyword, boolean selling) {
		if (!(npc.getTrader().isStocked(i.getTypeId(), selling))) {
			player.sendMessage(StringUtils.yellowify(i.getType().name(),
					ChatColor.RED) + " isn't being " + keyword + " here.");
			return null;
		}
		return npc.getTrader().getStockable(i.getTypeId(), selling);
	}

	public void addID(int ID) {
		this.taskID = ID;
	}

	public void kill() {
		stop = true;
		this.npc.getTrader().setFree(true);
		sendLeaveMessage();
		int index = TraderInterface.tasks.indexOf(taskID);
		if (index != -1)
			TraderInterface.tasks.remove(TraderInterface.tasks.indexOf(taskID));
		plugin.getServer().getScheduler().cancelTask(taskID);
	}

	private void sendJoinMessage() {
		switch (mode) {
		case INFINITE:
		case NORMAL:
			player.sendMessage(ChatColor.GREEN + "Transaction log");
			player.sendMessage(ChatColor.GOLD
					+ "-------------------------------");
			break;
		case STOCK:
			player.sendMessage(ChatColor.GOLD
					+ "Stocking of "
					+ StringUtils.yellowify(npc.getStrippedName(),
							ChatColor.GOLD) + " started.");
			break;
		}
	}

	private void sendLeaveMessage() {
		switch (mode) {
		case INFINITE:
		case NORMAL:
			player.sendMessage(ChatColor.GOLD
					+ "-------------------------------");
			break;
		case STOCK:
			player.sendMessage(ChatColor.GOLD
					+ "Stocking of "
					+ StringUtils.yellowify(npc.getStrippedName(),
							ChatColor.GOLD) + " finished.");
			break;
		}
	}

	private ItemStack cloneItemStack(ItemStack source) {
		if (source == null) // sanity check
			return null;
		ItemStack clone = new ItemStack(source.getType(), source.getAmount(),
				source.getDurability(), (source.getData() != null ? source
						.getData().getData() : null));
		return clone;
	}

	// Clones the first passed PlayerInventory object to the second one.
	private void clonePlayerInventory(PlayerInventory source,
			PlayerInventory target) {
		ItemStack[] contents = new ItemStack[source.getContents().length];
		System.arraycopy(source.getContents(), 0, contents, 0, contents.length);
		target.setContents(contents);

		target.setHelmet(cloneItemStack(source.getHelmet()));
		target.setChestplate(cloneItemStack(source.getChestplate()));
		target.setLeggings(cloneItemStack(source.getLeggings()));
		target.setBoots(cloneItemStack(source.getBoots()));
	}
}
