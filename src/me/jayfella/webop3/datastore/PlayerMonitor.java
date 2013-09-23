package me.jayfella.webop3.datastore;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import me.jayfella.webop3.PluginContext;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.core.SocketSubscription;
import me.jayfella.webop3.core.WebOpUser;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerMonitor extends SocketSubscription
{
    private boolean essentialsExists;
    
    public PlayerMonitor(PluginContext context)
    {
        context.getPlugin().getServer().getPluginManager().registerEvents(new PlayerMonitorEvents(), context.getPlugin());
        this.essentialsExists = (context.getPlugin().getServer().getPluginManager().getPlugin("Essentials") != null);
    }
    
    public boolean essentialsExists()
    {
        return this.essentialsExists;
    }
    
    public String generatePlayerString()
    {
        int alltimeCount = WebOp3Plugin.PluginContext.getPlugin().getServer().getOfflinePlayers().length;
        Player[] onlinePlayers = WebOp3Plugin.PluginContext.getPlugin().getServer().getOnlinePlayers();

        StringBuilder sb = new StringBuilder()
                .append("case=allPlayersData;")
                .append("ALLTIME=").append(NumberFormat.getIntegerInstance().format(alltimeCount)).append(";")
                .append("ONLINENOW=").append(NumberFormat.getIntegerInstance().format(onlinePlayers.length)).append(";")
                .append("MAXIMUM=").append(WebOp3Plugin.PluginContext.getPlugin().getServer().getMaxPlayers()).append(";")
                .append("PLAYERS=");

        for (int i = 0; i < onlinePlayers.length; i++)
        {
            sb.append(onlinePlayers[i].getName());

            if (i < onlinePlayers.length -1)
            {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public String findPlayers(String partialName)
    {
        StringBuilder results = new StringBuilder();

        int resultCount = 0;

        for (OfflinePlayer offlinePlayer : WebOp3Plugin.PluginContext.getPlugin().getServer().getOfflinePlayers())
        {
            String playerName = offlinePlayer.getName().toLowerCase();

            if (playerName.contains(partialName))
            {
                
                results
                        .append(offlinePlayer.isBanned() ? "<span style='color: darkred';>": "<span style='color: darkgreen';>")
                        .append(offlinePlayer.getName())
                        .append("</span>")
                        .append(",");
                
                resultCount++;
            }

            if (resultCount == 50)
                break;
        }

        return results.toString();
    }
    
    public String generateEssentialsPlayerDataString(String playername)
    {
        if (!essentialsExists) return "";
        
        Essentials ess = (Essentials)WebOp3Plugin.PluginContext.getPlugin().getServer().getPluginManager().getPlugin("Essentials");
        User essUser = ess.getOfflineUser(playername);
        
        SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");
        DecimalFormat balanceFormatter = new DecimalFormat("###,###.##");
        
        StringBuilder response = new StringBuilder()
                .append("<span style='color: darkorange; font-weight: bold;'>").append(playername).append("</span><br/><br/>")
                .append("<strong>Balance:</strong> ").append(balanceFormatter.format(essUser.getMoney().setScale(2, RoundingMode.HALF_EVEN))).append("<br/>")
                .append("<strong>First Login:</strong> ").append(df.format(new Date(essUser.getFirstPlayed()))).append("<br/>")
                .append("<strong>Last Login:</strong> ").append(df.format(new Date(essUser.getLastPlayed()))).append("<br/>")
                .append("<br/>")
                .append("<Strong>Flying:</strong> ").append(essUser.isFlying() ? "Yes" : "No").append("<br/>")
                .append("<strong>At Y:</strong> ").append(essUser.getLocation().getBlockY()).append("<br/>")
                .append("<strong>In World:</strong> ").append(essUser.getLocation().getWorld().getName()).append("<br/>");
        
        if (essUser.isJailed() | essUser.isMuted())
            response.append("<br/>");
        
        if (essUser.isJailed())
            response.append("<strong>Jailed Until:</strong> ").append(df.format(new Date(essUser.getJailTimeout()))).append("<br/>");
        
        if (essUser.isMuted())
            response.append("<strong>Muted Until:</strong> ").append(df.format(new Date(essUser.getMuteTimeout()))).append("<br/>");
        
        return response.toString();
    }
    
    private class PlayerMonitorEvents implements Listener
    {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event)
        {
            WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(WebOp3Plugin.PluginContext.getPlugin(), 
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (WebOpUser user : WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers())
                        {
                            if (isSubscriber(user.getName()))
                            {
                                if (user.getWebSocketSession() == null || user.getWebSocketSession().isOpen() == false)
                                    continue;

                                try { user.getWebSocketSession().getRemote().sendString(generatePlayerString()); }
                                catch (IOException ex) { }
                            }
                        }
                    }
                }, 10L);
        }
        
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event)
        {
            WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(WebOp3Plugin.PluginContext.getPlugin(), 
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (WebOpUser user : WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers())
                        {
                            if (isSubscriber(user.getName()))
                            {
                                if (user.getWebSocketSession() == null || user.getWebSocketSession().isOpen() == false)
                                    continue;

                                try { user.getWebSocketSession().getRemote().sendString(generatePlayerString()); }
                                catch (IOException ex) { }
                            }
                        }
                    }
                }, 10L);
        }
        
    }
}
