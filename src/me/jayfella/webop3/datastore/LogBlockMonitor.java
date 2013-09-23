package me.jayfella.webop3.datastore;

import de.diddiz.LogBlock.BlockChange;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.QueryParams;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import me.jayfella.webop3.WebOp3Plugin;
import org.bukkit.Material;
import org.bukkit.World;

public class LogBlockMonitor
{
    public LogBlockMonitor()
    {
        
    }
    
    public List<String> LookupGeneralDestroyedOre(String player, String block, String sinceMins)
    {
        List<String> results = new ArrayList<>();
        LogBlock lbPlugin = (LogBlock)WebOp3Plugin.PluginContext.getPlugin().getServer().getPluginManager().getPlugin("LogBlock");
        QueryParams lbQuery = new QueryParams(lbPlugin);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        int sinceVal = Integer.valueOf(sinceMins);
        
        if (!player.isEmpty()) lbQuery.setPlayer(player);
        
        lbQuery.needPlayer = true;
        lbQuery.bct = QueryParams.BlockChangeType.DESTROYED;
        lbQuery.types = Arrays.asList(new de.diddiz.util.Block[] { new de.diddiz.util.Block(Material.getMaterial(block).getId(), 0) });
        lbQuery.limit = -1;
        lbQuery.needDate = true;
        lbQuery.needType = true;
        lbQuery.needData = true;
        lbQuery.needCoords = true;
        lbQuery.since = sinceVal;
        
        List<String> loggedWorlds = lbPlugin.getConfig().getStringList("loggedWorlds");
        
        for (World world : WebOp3Plugin.PluginContext.getPlugin().getServer().getWorlds())
        {
            if (!loggedWorlds.contains(world.getName()))
                continue;
            
            lbQuery.world = world;
            
            try
            {
                List<BlockChange> lbResults = lbPlugin.getBlockChanges(lbQuery);

                for (BlockChange change : lbResults)
                {
                    StringBuilder result = new StringBuilder()
                            .append("<button class=\"btn-green teleportButton\" style=\"padding: 1px 4px !important\">Teleport</button>&nbsp;")
                            .append(change.playerName)
                            .append(" destroyed ")
                            .append(Material.getMaterial(change.replaced).name())
                            .append( " on ")
                            .append(df.format(new Date(change.date)))
                            .append(" X:<span class='xCoord'>")
                            .append(change.loc.getBlockX())
                            .append("</span> Y:<span class='yCoord'>")
                            .append(change.loc.getBlockY())
                            .append("</span> Z:<span class='zCoord'>")
                            .append(change.loc.getBlockZ())
                            .append("</span> in world:<span class='wCoord'>")
                            .append(change.loc.getWorld().getName())
                            .append("</span><br/>");
                    
                    results.add(result.toString());
                }

            } 
            catch (SQLException ex) { }
        }
        
        return results;
    }
    
}
