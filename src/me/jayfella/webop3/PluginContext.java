package me.jayfella.webop3;

import me.jayfella.webop3.Servlets.MyHttpServlet;
import me.jayfella.webop3.Servlets.MyWebSocket;
import me.jayfella.webop3.core.ServerProfiler;
import me.jayfella.webop3.core.SessionManager;
import me.jayfella.webop3.datastore.ConsoleMonitor;
import me.jayfella.webop3.datastore.EntityMonitor;
import me.jayfella.webop3.datastore.LogBlockMonitor;
import me.jayfella.webop3.datastore.MessageHandler;
import me.jayfella.webop3.datastore.PlayerMonitor;
import me.jayfella.webop3.datastore.UtilizationMonitor;
import me.jayfella.webop3.datastore.WorldMonitor;
import org.bukkit.World;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public final class PluginContext
{
    private final WebOp3Plugin plugin;
    private final PluginSettings pluginSettings;
    private final SessionManager sessionManager;
    
    private final UtilizationMonitor utilizationMonitor;
    private final PlayerMonitor playerMonitor;
    private final ConsoleMonitor consoleMonitor;
    private final MessageHandler messageHandler;
    private final LogBlockMonitor logblockMonitor;
    private final EntityMonitor entityMonitor;
    private final WorldMonitor worldMonitor;
    
    private final ServerProfiler serverProfiler;
    
    public PluginContext(WebOp3Plugin plugin)
    {
        this.plugin = plugin;
        this.pluginSettings = new PluginSettings(this);
        this.sessionManager = new SessionManager(this);
        
        this.utilizationMonitor = new UtilizationMonitor(this);
        this.playerMonitor = new PlayerMonitor(this);
        this.consoleMonitor = new ConsoleMonitor(this);
        this.messageHandler = new MessageHandler(this);
        this.logblockMonitor = new LogBlockMonitor();
        this.entityMonitor = new EntityMonitor();
        this.worldMonitor = new WorldMonitor(this);
        
        this.serverProfiler = new ServerProfiler(this);
        
        initJetty();
        
        // autosave each world every 5 minutes.
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, new AutoSaver(this), 6000L, 6000L);
    }
    
    private void initJetty()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                Server server = new Server(Integer.valueOf(pluginSettings.getHttpPort()));
                
                HandlerCollection handlerCollection = new HandlerCollection();
                server.setHandler(handlerCollection);

                WebSocketHandler wsHandler = new WebSocketHandler()
                {
                    @Override
                    public void configure(WebSocketServletFactory factory)
                    {
                        factory.register(MyWebSocket.class);
                    }
                };

                wsHandler.setServer(server);

                ContextHandler wsContext = new ContextHandler();
                wsContext.setContextPath("/socket");
                wsContext.setHandler(wsHandler);
                handlerCollection.addHandler(wsContext);

                ServletContextHandler httpContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
                httpContext.addServlet(new ServletHolder(new MyHttpServlet()), "/*");
                handlerCollection.addHandler(httpContext);

                try
                {
                    server.start();
                    server.join();
                }
                catch(Exception ex)
                {

                }
            }
        }.start();
    }
    
    public WebOp3Plugin getPlugin() { return this.plugin; }
    public PluginSettings getPluginSettings() { return this.pluginSettings; }
    public SessionManager getSessionManager() { return this.sessionManager; }
    
    public UtilizationMonitor getUtilizationMonitor() { return this.utilizationMonitor; }
    public PlayerMonitor getPlayerMonitor() { return this.playerMonitor; }
    public ConsoleMonitor getConsoleMonitor() { return this.consoleMonitor; }
    public MessageHandler getMessageHandler() { return this.messageHandler; }
    public LogBlockMonitor getLogBlockMonitor() { return this.logblockMonitor; }
    public EntityMonitor getEntityMonitor() { return this.entityMonitor; }
    public WorldMonitor getWorldMonitor() { return this.worldMonitor; }
    
    public ServerProfiler getServerProfiler() { return this.serverProfiler; }
    
    private class AutoSaver implements Runnable
    {
        private final PluginContext context;
        
        public AutoSaver(PluginContext context)
        {
            this.context = context;
        }
        
        @Override
        public void run()
        {
            for (World world : context.getPlugin().getServer().getWorlds())
            {
                world.save();
            }
        }
    }
}
