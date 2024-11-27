package hundeklemmen.superawesome;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SignClickListener implements Listener {
/*
    sign.setLine(0, "§4§lSAJulekalender");
    sign.setLine(1, "§cTryk her");
    sign.setLine(2, "§cfor at gennemføre");
    sign.setLine(3, "§cLåge " + args[1]+".");
 */
    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getState() instanceof Sign) {
                Sign sign = (Sign) event.getClickedBlock().getState();
                if (!sign.getLine(0).equals("§4§lSAJulekalender") && !sign.getLine(1).equals("§cTryk her") && !sign.getLine(2).equals("§cfor at gennemføre") && !sign.getLine(3).startsWith("§cLåge")) {
                    return;
                }
                String args = sign.getLine(3).replace("§cLåge ", "").replace(".", "").trim();
                if (args.isEmpty()) {
                    return;
                }
                //if arg is not 1-24
                if (!args.matches("[1-9]|1[0-9]|2[0-4]")) {
                    return;
                }

                Player player = event.getPlayer();

                Julekalender.runAsync(() -> {
                    JSONObject postObj = new JSONObject();
                    // { world, x, y, z, playerUUID }
                    postObj.put("world", event.getClickedBlock().getWorld().getName());
                    postObj.put("x", event.getClickedBlock().getX());
                    postObj.put("y", event.getClickedBlock().getY());
                    postObj.put("z", event.getClickedBlock().getZ());
                    postObj.put("playerUUID", player.getUniqueId().toString());

                    String svar = Utils.put("https://api.superawesome.dk/storeapi/jul/" + args, postObj.toString(), Julekalender.authorization);

                    if (svar != null) {
                        //parse as json
                        JSONParser gemparser = new JSONParser();
                        JSONObject gemjson = null;
                        try {
                            gemjson = (JSONObject) gemparser.parse(svar);
                        } catch (ParseException e) {
                            System.out.println("Error parsing json");
                            e.printStackTrace();
                        }

                        if (gemjson != null) {
                            if (gemjson.get("error") != null) {
                                player.sendMessage("§c" + gemjson.get("error"));
                                return;
                            }

                            player.sendMessage("§a" + gemjson.get("message"));
                        } else {
                            player.sendMessage("§cNoget gik galt, prøv igen senere!");
                        }
                    } else {
                        player.sendMessage("§cNoget gik galt, prøv igen senere!");
                    }
                });
            }
        }
    }
}
