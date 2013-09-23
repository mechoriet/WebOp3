package me.jayfella.webop3.datastore;

import java.io.IOException;
import java.util.concurrent.Future;
import me.jayfella.webop3.PluginContext;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.core.SocketSubscription;
import me.jayfella.webop3.core.WebOpUser;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WorldMonitor extends SocketSubscription
{
    private final PluginContext context;
    
    public WorldMonitor(PluginContext context)
    {
        this.context = context;
        context.getPlugin().getServer().getPluginManager().registerEvents(new WorldEventMonitor(), context.getPlugin());
    }
    
    public String getWorldDetails(World world)
    {
        StringBuilder response = new StringBuilder()
                .append("name=").append(world.getName()).append(";")
                .append("playercount=").append(world.getPlayers().size()).append(";")
                .append("type=").append(world.getEnvironment().name()).append(";")
                .append("difficulty=").append(world.getDifficulty().name()).append(";")
                .append("israining=").append(world.hasStorm()).append(";")
                .append("isthundering=").append(world.isThundering());
        
        return response.toString();
    }
    
    private class WorldEventMonitor implements Listener
    {
        private void updateSubscribers(World world)
        {
            for (String player : getSubscribers())
            {
                WebOpUser user = WebOp3Plugin.PluginContext.getSessionManager().getUser(player);
                
                if (user == null)
                    continue;
                
                if (user.getWebSocketSession() == null || user.getWebSocketSession().isOpen() == false)
                    continue;
                
                try { user.getWebSocketSession().getRemote().sendString("case=worldData;" + getWorldDetails(world)); }
                catch (IOException ex) { }
            }
        }
        
        @EventHandler
        public void onWeatherChange(final WeatherChangeEvent event)
        {
            WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(WebOp3Plugin.PluginContext.getPlugin(), 
            new Runnable()
            {
                @Override
                public void run()
                {
                    updateSubscribers(event.getWorld());
                }
            }
                , 20L
            );
        }
        
        @EventHandler
        public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
        {
            updateSubscribers(event.getFrom());
            updateSubscribers(event.getPlayer().getWorld());
        }
        
        @EventHandler
        public void onPlayerJoin(final PlayerJoinEvent event)
        {
            WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(WebOp3Plugin.PluginContext.getPlugin(), 
            new Runnable()
            {
                @Override
                public void run()
                {
                    updateSubscribers(event.getPlayer().getWorld());
                }
            }
                , 20L
            );
        }
        
        @EventHandler
        public void onPlayerQuit(final PlayerQuitEvent event)
        {
            final World world = event.getPlayer().getWorld();
            
            WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(WebOp3Plugin.PluginContext.getPlugin(), 
            new Runnable()
            {
                @Override
                public void run()
                {
                    updateSubscribers(world);
                }
            }
                , 20L
            );
        }
    }
}
