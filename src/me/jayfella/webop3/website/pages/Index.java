
package me.jayfella.webop3.website.pages;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.core.WebOpUser;
import me.jayfella.webop3.website.WebPage;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class Index extends WebPage
{
    public Index()
    {
        this.setResponseCode(200);
        this.setContentType("text/html; charset=utf-8");
    }

    @Override
    public byte[] get(HttpServletRequest req, HttpServletResponse resp)
    {
        if (!WebOp3Plugin.PluginContext.getSessionManager().isValidCookie(req))
        {
            try { resp.sendRedirect("login.php"); }
            catch(IOException ex) { }
            
            return new byte[0];
        }
        
        String page = this.loadResource("me/jayfella/webop3/website/html/index.html");
        page = this.addSiteTemplate(page, "[WebOp] Index", req);
        
        int pluginCount = WebOp3Plugin.PluginContext.getPlugin().getServer().getPluginManager().getPlugins().length;
        
        StringBuilder pluginsSb = new StringBuilder();
        
        for (int i = 0; i < pluginCount; i++)
        {
            Plugin pl = WebOp3Plugin.PluginContext.getPlugin().getServer().getPluginManager().getPlugins()[i];

            pluginsSb
                    .append((pl.isEnabled()) ? "<span style='color: #0d8022' " : "<span style='color: #800d0d' ")
                    .append("title='Version: ").append(pl.getDescription().getVersion()).append("'>")
                    .append(pl.getName())
                    .append("</span>");

            if (i != pluginCount - 1)
            {
                pluginsSb.append(", ");
            }
        }
        
        String arch = System.getProperty("os.arch").contains("64") ? "64bit" : "2bit";
        
        page = page
                .replace("{java_version}", System.getProperty("java.version") + " " + arch + " on " + System.getProperty("os.name"))
                .replace("{bukkit_version}", WebOp3Plugin.PluginContext.getPlugin().getServer().getVersion())
                .replace("{plugin_count}", Integer.toString(pluginCount))
                .replace("{plugin_list}", pluginsSb.toString());
        
        page = (WebOp3Plugin.PluginContext.getPlugin().getServer().getPluginManager().getPlugin("LogBlock") != null)
            ? page.replace("{logblock_plugin}", this.loadResource("me/jayfella/webop3/website/html/logblock.html"))
            : page.replace("{logblock_plugin}", "");
        
        String httpUser = WebOp3Plugin.PluginContext.getSessionManager().getUsername(req);
        boolean isOp = WebOp3Plugin.PluginContext.getPlugin().getServer().getOfflinePlayer(httpUser).isOp();
        
        page = (isOp)
                ? page.replace("{server_profiler}", this.loadResource("me/jayfella/webop3/website/html/serverprofiler.html"))
                : page.replace("{server_profiler}", "");
        
        StringBuilder worldsData = new StringBuilder();
        World[] worlds = WebOp3Plugin.PluginContext.getPlugin().getServer().getWorlds().toArray(new World[WebOp3Plugin.PluginContext.getPlugin().getServer().getWorlds().size()]);
        for (int i = 0; i < worlds.length; i++)
        {
            worldsData.append("<span class='worldData' id='").append(worlds[i].getName()).append("'>").append(worlds[i].getName()).append("</span>");
            
            if (i < worlds.length - 1)
                worldsData.append(", ");
        }
        
        page = page.replace("{world_data}", worldsData.toString()).replace("{world_count}", Integer.toString(worlds.length));
        
        page = (WebOp3Plugin.PluginContext.getPlayerMonitor().essentialsExists())
                ? page.replace("{essentials_playerdata}", "<div id='essPlayerData' style='display: none; border-radius: 3px; background: #ffffff; border: 1px solid black; padding: 10px; box-shadow: 0px 0px 7px black; position: fixed; top: 10px; right: 10px;'></div>")
                : page.replace("{essentials_playerdata}", "");
        
        return page.getBytes();
    }

    @Override
    public byte[] post(HttpServletRequest req, HttpServletResponse resp)
    {
        return new byte[0];
    }
    
}
