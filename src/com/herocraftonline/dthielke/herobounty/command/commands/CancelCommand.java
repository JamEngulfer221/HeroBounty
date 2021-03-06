package com.herocraftonline.dthielke.herobounty.command.commands;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.herocraftonline.dthielke.herobounty.Bounty;
import com.herocraftonline.dthielke.herobounty.HeroBounty;
import com.herocraftonline.dthielke.herobounty.bounties.BountyManager;
import com.herocraftonline.dthielke.herobounty.command.BaseCommand;
import com.herocraftonline.dthielke.herobounty.util.Messaging;
import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Method.MethodAccount;

public class CancelCommand extends BaseCommand {

    public CancelCommand(HeroBounty plugin) {
        super(plugin);
        name = "Cancel";
        description = "Cancels a previously posted bounty";
        usage = "§e/bounty cancel §9<id#>";
        minArgs = 1;
        maxArgs = 1;
        identifiers.add("bounty cancel");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player owner = (Player) sender;
            String ownerName = owner.getName();
            List<Bounty> bounties = plugin.getBountyManager().getBounties();
            int id = BountyManager.parseBountyId(args[0], bounties);
            if (id != -1) {
                Bounty bounty = bounties.get(id);
                int value = bounty.getValue();
                if (bounty.getOwner().equals(ownerName)) {
                    bounties.remove(bounty);
                    Collections.sort(bounties);

                    Method register = plugin.getRegister();
                    MethodAccount ownerAccount = register.getAccount(ownerName);
                    ownerAccount.add(value);
                    Messaging.send(plugin, owner, "You have been reimbursed $1 for your bounty.", register.format(value));

                    List<String> hunters = bounty.getHunters();
                    if (!hunters.isEmpty()) {
                        int inconvenience = (int) Math.floor((double) bounty.getPostingFee() / hunters.size());
                        for (String hunterName : bounty.getHunters()) {
                            MethodAccount hunterAccount = register.getAccount(hunterName);
                            hunterAccount.add(bounty.getContractFee());
                            if (plugin.getBountyManager().shouldPayInconvenience()) {
                                hunterAccount.add(inconvenience);
                            }

                            Player hunter = plugin.getServer().getPlayer(hunterName);
                            if (hunter != null) {
                                Messaging.send(plugin, hunter, "The bounty on $1 has been cancelled.", bounty.getTargetDisplayName());
                                Messaging.send(plugin, hunter, "Your contract fee has been refunded.");
                                if (plugin.getBountyManager().shouldPayInconvenience() && inconvenience > 0) {
                                    Messaging.send(plugin, hunter, "You have received $1 for the inconvenience.", register.format(inconvenience));
                                }
                            }

                        }
                    }
                    plugin.saveData();
                } else {
                    Messaging.send(plugin, owner, "You can only cancel bounties you created.");
                }
            } else {
                Messaging.send(plugin, owner, "Bounty not found.");
            }
        }
    }

}
