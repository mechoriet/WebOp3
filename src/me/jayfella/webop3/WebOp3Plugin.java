package me.jayfella.webop3;

import org.bukkit.plugin.java.JavaPlugin;


public class WebOp3Plugin extends JavaPlugin
{
    public static PluginContext PluginContext;
    
    @Override
    public void onEnable()
    {
        PluginContext = new PluginContext(this);
    }
    
    @Override
    public void onDisable()
    {
        
    }
}
