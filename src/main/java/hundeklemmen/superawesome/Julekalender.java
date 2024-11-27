package hundeklemmen.superawesome;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.schematic.SchematicFormat;
import hundeklemmen.superawesome.Commands.SuperAwesomeJulekalender;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Logger;

public class Julekalender extends JavaPlugin {

    @Getter
    public static Julekalender instance;
    public static String prefix = "&8[&aSuperAwesome Store&8]&r";
    public static File configFile;
    public static FileConfiguration config;
    public static long lastJulekalenderFetch = 0;

    public static String authorization = "";




    public static Logger log;


    @Override
    public void onEnable(){
        instance = this;
        log = getLogger();
        getCommand("julekalendersa").setExecutor(new SuperAwesomeJulekalender());

        //register event listeners
        getServer().getPluginManager().registerEvents(new SignClickListener(), this);

        createConfig();

        if(config.get("serverkey") != null) {
            overwrite();
        }
        if(Bukkit.getPluginManager().getPlugin("WorldEdit") != null && !config.getString("apikey").equalsIgnoreCase("")) {
            authorization = config.getString("apikey");
            System.out.println("Auth: " + authorization);
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateDays();
                }
            }.runTaskTimer(Julekalender.instance, 20L, 20L * 60*5);
        }
    }

    public WorldEditPlugin getWorldedit() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin instanceof WorldEditPlugin) {
            return (WorldEditPlugin) plugin;
        }
        return null;
    }

    @Override
    public void onDisable(){

    }

    public static void createConfig() {
        configFile = new File(Julekalender.instance.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            Julekalender.instance.saveResource("config.yml", false);
        }
        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        if(config.contains("apikey") && config.getString("apikey").toString().length() != 0) {
            authorization = config.getString("apikey");
        }
    }

    public static void overwrite() {
        configFile = new File(Julekalender.instance.getDataFolder(), "config.yml");
        Julekalender.instance.saveResource("config.yml", true);
    }


    public static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(getInstance(), runnable);
    }

    public static void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(getInstance(), runnable);
    }

    public static void updateDays() {
        Julekalender.runAsync(() -> {
            String svar = Utils.get("https://api.superawesome.dk/storeapi/jul/days", Julekalender.authorization);
            System.out.println("result" + svar);
            //array of objects

            JSONParser parser = new JSONParser();
            try {
                // Parse the JSON string
                Object parsed = parser.parse(svar);

                // Ensure the parsed object is a JSONArray
                if (!(parsed instanceof JSONArray)) {
                    System.out.println("Unexpected response format: " + parsed);
                    return;
                }

                JSONArray jsonArray = (JSONArray) parsed;

                // Iterate over the array
                for (Object obj : jsonArray) {
                    if (!(obj instanceof JSONObject)) {
                        System.out.println("Unexpected object format: " + obj);
                        continue;
                    }

                    JSONObject jsonObject = (JSONObject) obj;

                    String world = (String) jsonObject.get("world");
                    int x = ((Long) jsonObject.get("x")).intValue(); // Convert Long to int
                    int y = ((Long) jsonObject.get("y")).intValue(); // Convert Long to int
                    int z = ((Long) jsonObject.get("z")).intValue(); // Convert Long to int

                    // Safely handle the optional "activeNow" field
                    int activeNow = jsonObject.containsKey("activeNow")
                            ? ((Long) jsonObject.get("activeNow")).intValue()
                            : 0;


                    //Get location to if there exists a sign
                    World w = Bukkit.getWorld(world);
                    if(w == null) {
                        System.out.println("World not found: " + world);
                        continue;
                    }

                    if(activeNow == 1) {
                        if(!w.getBlockAt(x, y, z).getType().name().contains("SIGN")) {
                            String _schemData = (String) jsonObject.get("afterSchem");
                            pasteSchem(_schemData, world);
                        }
                    } else {
                        if(w.getBlockAt(x, y, z).getType().name().contains("SIGN")) {
                            String _schemData = (String) jsonObject.get("beforeSchem");
                            pasteSchem(_schemData, world);
                        }
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
    }
    private static void pasteSchem(String schematic, String _world) {
        Julekalender.runSync(() -> {

            // Convert the Base64 string to a byte array
            byte[] schematicData = Base64.getDecoder().decode(schematic);
            long currentTime = System.currentTimeMillis();

            // Save the byte array to a temporary file
            try {
                File tempFile = File.createTempFile(currentTime + "", ".schematic");
                Utils.writeBytesToFile(schematicData, tempFile);
                org.bukkit.World world = Julekalender.getInstance().getServer().getWorld(_world);
                if (world == null) {
                    System.out.println("§cInvalid world for this schematic.");
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
            } catch (Exception e) {
                System.out.println("§cAn error occurred while loading the schematic.");
                e.printStackTrace();
            }
        });
    }
}
