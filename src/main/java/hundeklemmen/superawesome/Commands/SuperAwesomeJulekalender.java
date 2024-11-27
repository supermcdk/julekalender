package hundeklemmen.superawesome.Commands;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.World;
import hundeklemmen.superawesome.Julekalender;
import hundeklemmen.superawesome.Utils;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.selections.Selection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;

public class SuperAwesomeJulekalender implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        //Only allow OP or uuid 6d7d19a5-bc8b-48e3-9d01-de01f16d905f or 81e072a9-df97-4e7d-9422-b880e988c793

        Player player = (Player) sender;
        if (player.getUniqueId().toString().equalsIgnoreCase("6d7d19a5-bc8b-48e3-9d01-de01f16d905f") == false && player.getUniqueId().toString().equalsIgnoreCase("81e072a9-df97-4e7d-9422-b880e988c793") == false && player.isOp() == false) {
            player.sendMessage("§cDu har ikke adgang til denne kommando.");
            return true;
        }

        if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("info")) {
            if (args.length == 1) {
                player.sendMessage("§cBrug /sajulekalender info <låge> - Informationer om julekalender låge");
                return true;
            }
            //if args[1] is not a number
            if (!Utils.isInteger(args[1])) {
                player.sendMessage("§cLåge skal være et tal.");
                return true;
            }

            Julekalender.runAsync(() -> {
                String svar = Utils.get("https://api.superawesome.dk/storeapi/jul/" + args[1], Julekalender.authorization);
                if (svar != null) {
                    //parse as json
                    JSONParser parser = new JSONParser();
                    JSONObject json = null;
                    try {
                        json = (JSONObject) parser.parse(svar);
                    } catch (ParseException e) {
                        System.out.println("Error parsing json");
                        e.printStackTrace();
                    }

                    if (json != null) {
                        if(json.get("error") != null) {
                            player.sendMessage("§c" + json.get("error"));
                            return;
                        }
                        /*
                            id: day.id,
                            enabled: day.enabled,
                            world: day.world,
                            x: day.x,
                            y: day.y,
                            z: day.z,
                            beforeSchem: day.beforeSchem,
                            afterSchem: day.afterSchem,
                            description: day.description
                         */
                        player.sendMessage("§eInformationer om låge §6" + args[1]);
                        player.sendMessage("§eNavn: §6" + json.get("id"));
                        player.sendMessage("§eGåde: §6" + json.get("description"));
                        player.sendMessage("§eEnabled: §6" + json.get("enabled"));
                        player.sendMessage("§eWorld: §6" + json.get("world"));
                        player.sendMessage("§eX: §6" + json.get("x"));
                        player.sendMessage("§eY: §6" + json.get("y"));
                        player.sendMessage("§eZ: §6" + json.get("z"));
                        player.sendMessage("§eDefault Schematic: §6" + (json.get("beforeSchem") != null ? "Ja" : "Nej"));
                        player.sendMessage("§eJul Schematic: §6" + (json.get("afterSchem") != null ? "Ja" : "Nej"));
                    }
                }
            });

        } else if ((args.length == 1 || args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("gem")) {
            if (args.length == 1 || args.length == 2) {
                player.sendMessage("§cBrug /sajulekalender gem <låge> default/jul - for at gemme julekalender schema");
                return true;
            }
            if(args[2].equalsIgnoreCase("default") == false && args[2].equalsIgnoreCase("jul") == false) {
                player.sendMessage("§cBrug /sajulekalender gem <låge> default/jul - for at gemme julekalender schema");
                return true;
            }
            //check if args[1] is a number
            if (!Utils.isInteger(args[1])) {
                player.sendMessage("§cLåge skal være et tal.");
                return true;
            }

            int playerX = player.getLocation().getBlockX();
            int playerY = player.getLocation().getBlockY();
            int playerZ = player.getLocation().getBlockZ();

            Julekalender.runAsync(() -> {
                String svar = Utils.get("https://api.superawesome.dk/storeapi/jul/" + args[1], Julekalender.authorization);
                if (svar != null) {
                    //parse as json
                    JSONParser parser = new JSONParser();
                    JSONObject json = null;
                    try {
                        json = (JSONObject) parser.parse(svar);
                    } catch (ParseException e) {
                        System.out.println("Error parsing json");
                        e.printStackTrace();
                    }

                    if (json != null) {
                        if (json.get("error") != null) {
                            player.sendMessage("§c" + json.get("error"));
                            return;
                        }

                        String world = (String) json.get("world");
                        int x = 0;
                        int y = 0;
                        int z = 0;
                        if(json.get("x") != null) {
                            x = Integer.parseInt(json.get("x").toString());
                        }
                        if(json.get("y") != null) {
                            y = Integer.parseInt(json.get("y").toString());
                        }
                        if(json.get("z") != null) {
                            z = Integer.parseInt(json.get("z").toString());
                        }

                        int finalX = x;
                        int finalY = y;
                        int finalZ = z;
                        Julekalender.runSync(() -> {
                            if (Julekalender.getInstance() == null || Julekalender.getInstance().getWorldedit() == null) {
                                player.sendMessage("§cWorldEdit plugin is not available!");
                                return;
                            }

                            Selection selection = Julekalender.getInstance().getWorldedit().getSelection(player);
                            if (selection == null) {
                                player.sendMessage("§c⚠ Du skal markere et område med WorldEdit");
                                return;
                            }
                            if (world != null) {
                                if (world.equalsIgnoreCase(player.getWorld().getName()) == false) {
                                    player.sendMessage("§cDu skal være i verden " + world + " for at gemme dette schematic.");
                                    return;
                                }
                                //if x, y, z is not the same as player location
                                if (finalX != playerX || finalY != playerY || finalZ != playerZ) {
                                    player.sendMessage("§cDu skal være på koordinaterne " + finalX + ", " + finalY + ", " + finalZ + " for at gemme dette schematic.");
                                    return;
                                }
                            }

                            if(args[2].equalsIgnoreCase("jul")) {
                                //Check if player location has a sign
                                if(player.getLocation().getBlock().getType().name().contains("SIGN") == false) {
                                    player.sendMessage("§cDu skal stå på et skilt for at gemme dette schematic.");
                                    return;
                                }

                                //Check if sign has text
                                if(player.getLocation().getBlock().getState() instanceof Sign) {
                                    Sign sign = (Sign) player.getLocation().getBlock().getState();
                                    sign.setLine(0, "§4§lSAJulekalender");
                                    sign.setLine(1, "§cTryk her");
                                    sign.setLine(2, "§cfor at gennemføre");
                                    sign.setLine(3, "§cLåge " + args[1]+".");
                                    sign.update();
                                }
                            } else if(args[2].equalsIgnoreCase("default")) {
                                //Check if player location has a sign
                                if (player.getLocation().getBlock().getType().name().contains("SIGN") == true) {
                                    player.sendMessage("§cDer må ikke være et skilt på denne lokation for at gemme dette schematic.");
                                    return;
                                }
                            }

                            String schematicName = args[1];
                            File schematicFile = new File(Julekalender.getInstance().getDataFolder(), schematicName + ".schematic");

                            if (schematicFile.exists()) {
                                schematicFile.delete();
                            }

                            com.sk89q.worldedit.world.World worldEditWorld = BukkitUtil.getLocalWorld(player.getWorld());
                            try {
                                EditSession editSession = Julekalender.getInstance().getWorldedit().getWorldEdit().getEditSessionFactory()
                                        .getEditSession(worldEditWorld, -1);

                                Vector min = selection.getNativeMinimumPoint();
                                Vector max = selection.getNativeMaximumPoint();
                                CuboidClipboard clipboard = new CuboidClipboard(max.subtract(min).add(1, 1, 1), min);

                                clipboard.copy(editSession);
                                SchematicFormat schematicFormat = SchematicFormat.MCEDIT;

                                Long currentTime = System.currentTimeMillis();
                                File tempFile = File.createTempFile(currentTime.toString(), ".schematic");
                                schematicFormat.save(clipboard, tempFile);

                                // Read the temporary file into a ByteArrayOutputStream
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                                    byte[] buffer = new byte[1024];
                                    int bytesRead;
                                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                                    }
                                }

                                // Convert the byte array to a Base64 string
                                String base64Schematic = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());

                                // Save the Base64 string to the database (replace this with your database logic)
                                System.out.println("Base64 Schematic: " + base64Schematic);

                                Julekalender.runAsync(() -> {
                                    JSONObject postObj = new JSONObject();
                                    //world, x, y, z, schem, schematic
                                    postObj.put("world", player.getWorld().getName());
                                    postObj.put("x", playerX);
                                    postObj.put("y", playerY);
                                    postObj.put("z", playerZ);
                                    postObj.put("schem", args[2].equalsIgnoreCase("default") ? "before" : "after");
                                    postObj.put("schematic", base64Schematic);

                                    String gemsvar = Utils.post("https://api.superawesome.dk/storeapi/jul/" + args[1], postObj.toString(), Julekalender.authorization);

                                    if (gemsvar != null) {
                                        //parse as json
                                        JSONParser gemparser = new JSONParser();
                                        JSONObject gemjson = null;
                                        try {
                                            gemjson = (JSONObject) gemparser.parse(gemsvar);
                                        } catch (ParseException e) {
                                            System.out.println("Error parsing json");
                                            e.printStackTrace();
                                        }

                                        if (gemjson != null) {
                                            if (gemjson.get("error") != null) {
                                                player.sendMessage("§c" + gemjson.get("error"));
                                                return;
                                            }
                                            player.sendMessage("§eSchematic låge §6" + args[1] + "§e, tema §6" + args[2] + " §e gemt i SuperAwesome's database.");
                                        }
                                    }

                                });
                            } catch (Exception e) {
                                player.sendMessage("§cAn error occurred while saving the schematic.");
                                e.printStackTrace();
                            }
                        });
                    }

                }
            });
            return true;
        } else if ((args.length == 1 || args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("load")) {
            if (args.length == 1 || args.length == 2) {
                player.sendMessage("§cBrug /sajulekalender load <låge> <tema> - for at loade julekalender schema");
                return true;
            }

            //check if args[1] is a number
            if (!Utils.isInteger(args[1])) {
                player.sendMessage("§cLåge skal være et tal.");
                return true;
            }
            //if args[2] is not default or jul
            if (args[2].equalsIgnoreCase("default") == false && args[2].equalsIgnoreCase("jul") == false) {
                player.sendMessage("§cBrug /sajulekalender load <låge> <tema> - for at loade julekalender schema");
                return true;
            }


            Julekalender.runAsync(() -> {
                String svar = Utils.get("https://api.superawesome.dk/storeapi/jul/" + args[1], Julekalender.authorization);
                if (svar != null) {
                    //parse as json
                    JSONParser parser = new JSONParser();
                    JSONObject json = null;
                    try {
                        json = (JSONObject) parser.parse(svar);
                    } catch (ParseException e) {
                        System.out.println("Error parsing json");
                        e.printStackTrace();
                    }

                    if (json != null) {
                        if (json.get("error") != null) {
                            player.sendMessage("§c" + json.get("error"));
                            return;
                        }

                        JSONObject finalJson = json;
                        Julekalender.runSync(() -> {
                            String realSchemName = args[2].equalsIgnoreCase("default") ? "beforeSchem" : "afterSchem";
                            if (finalJson.get(realSchemName) == null) {
                                player.sendMessage("§cDer er ikke noget schematic for dette tema.");
                                return;
                            }

                            String schematic = (String) finalJson.get(realSchemName);

                            // Convert the Base64 string to a byte array
                            byte[] schematicData = Base64.getDecoder().decode(schematic);
                            long currentTime = System.currentTimeMillis();

                            // Save the byte array to a temporary file
                            try {
                                File tempFile = File.createTempFile(currentTime + "", ".schematic");
                                Utils.writeBytesToFile(schematicData, tempFile);
                                org.bukkit.World world = Julekalender.getInstance().getServer().getWorld((String) finalJson.get("world"));
                                if (world == null) {
                                    player.sendMessage("§cInvalid world for this schematic.");
                                    return;
                                }

                                Vector offset = new Vector(0, 0, 0);

                                EditSession editSession = Julekalender.getInstance().getWorldedit().getWorldEdit().getEditSessionFactory()
                                        .getEditSession(BukkitUtil.getLocalWorld(world), -1);
                                SchematicFormat schematicFormat = SchematicFormat.MCEDIT;
                                CuboidClipboard clipboard = schematicFormat.load(tempFile);

                                // Calculate the paste location
                                Vector min = clipboard.getOrigin();
                                Vector pasteLocation = min.add(offset);

                                clipboard.paste(editSession, pasteLocation, false);
                                tempFile.delete();

                                player.sendMessage("§eSchematic låge §6" + args[1] + "§e, tema §6" + args[2] + " §e loaded fra SuperAwesome's database.");
                            } catch (Exception e) {
                                player.sendMessage("§cAn error occurred while loading the schematic.");
                                e.printStackTrace();
                            }
                        });
                    }
                }
            });




            /*try {
                org.bukkit.World world = player.getServer().getWorld(player.getWorld().getName());
                if (world == null) {
                    player.sendMessage("§cInvalid world for this schematic.");
                    return true;
                }

                Vector offset = new Vector(0, 0, 0);

                EditSession editSession = Julekalender.getInstance().getWorldedit().getWorldEdit().getEditSessionFactory()
                        .getEditSession(BukkitUtil.getLocalWorld(world), -1);
                SchematicFormat schematicFormat = SchematicFormat.MCEDIT;
                CuboidClipboard clipboard = schematicFormat.load(schematicFile);

                // Calculate the paste location
                Vector min = clipboard.getOrigin();
                Vector pasteLocation = min.add(offset);

                clipboard.paste(editSession, pasteLocation, false);

                player.sendMessage("§aSchematic loaded from " + schematicFile.getName());
            } catch (Exception e) {
                player.sendMessage("§cAn error occurred while loading the schematic.");
                e.printStackTrace();
            }*/
        } else if ((args.length == 1 || args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("slet")) {
            if (args.length == 1 || args.length == 2) {
                player.sendMessage("§cBrug /sajulekalender slet <låge> <tema> - for at slette julekalender schema");
                return true;
            }

            //check if args[1] is a number
            if (!Utils.isInteger(args[1])) {
                player.sendMessage("§cLåge skal være et tal.");
                return true;
            }
            //if args[2] is not default or jul
            if (args[2].equalsIgnoreCase("default") == false && args[2].equalsIgnoreCase("jul") == false) {
                player.sendMessage("§cBrug /sajulekalender slet <låge> <tema> - for at slette julekalender schema");
                return true;
            }

            Julekalender.runAsync(() -> {
                String svar = Utils.get("https://api.superawesome.dk/storeapi/jul/" + args[1], Julekalender.authorization);
                if (svar != null) {
                    //parse as json
                    JSONParser parser = new JSONParser();
                    JSONObject json = null;
                    try {
                        json = (JSONObject) parser.parse(svar);
                    } catch (ParseException e) {
                        System.out.println("Error parsing json info ");
                        System.out.println(svar);
                        e.printStackTrace();
                    }

                    if (json != null) {
                        if (json.get("error") != null) {
                            player.sendMessage("§c" + json.get("error"));
                            return;
                        }

                        String realSchemName = args[2].equalsIgnoreCase("default") ? "before" : "after";
                        if (json.get(realSchemName + "Schem") == null) {
                            player.sendMessage("§cDer er ikke noget schematic for dette tema.");
                            return;
                        }

                        String _deleteSvar = Utils.delete("https://api.superawesome.dk/storeapi/jul/" + args[1] + "?schem=" + realSchemName, Julekalender.authorization);

                        if (_deleteSvar != null) {
                            //parse as json
                            JSONParser gemparser = new JSONParser();
                            JSONObject gemjson = null;
                            try {
                                gemjson = (JSONObject) gemparser.parse(_deleteSvar);
                            } catch (ParseException e) {
                                System.out.println("Error parsing json delete tema");
                                System.out.println(_deleteSvar);
                                e.printStackTrace();
                            }

                            if (gemjson != null) {
                                if (gemjson.get("error") != null) {
                                    player.sendMessage("§c" + gemjson.get("error"));
                                    return;
                                }
                                player.sendMessage("§eSchematic låge §6" + args[1] + "§e, tema §6" + args[2] + " §e slettet fra SuperAwesome's database.");
                            }
                        }
                    }
                }
            });
        } else if(args.length == 1 && args[0].equalsIgnoreCase("tjek")) {
            player.sendMessage("§eTjekker julekalender låger...");
            Julekalender.updateDays();
            player.sendMessage("§eJulekalender låger tjekket.");
        } else {
            player.sendMessage("§eBrug §6/sajulekalender info <låge> §8- §7Informationer om julekalender låge");
            player.sendMessage("§eBrug §6/sajulekalender gem <låge> default/jul §8- §7for at gemme julekalender schema");
            player.sendMessage("§eBrug §6/sajulekalender load <låge> default/jul §8- §7for at loade julekalender schema");
            player.sendMessage("§eBrug §6/sajulekalender slet <låge> default/jul §8- §7for at slette julekalender schema");
            player.sendMessage("§eBrug §6/sajulekalender tjek §8- §7for at tjekke julekalender låger");
        }
        return true;
    }
}