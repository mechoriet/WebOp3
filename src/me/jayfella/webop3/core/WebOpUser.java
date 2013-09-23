package me.jayfella.webop3.core;

import java.util.Date;
import java.util.logging.Level;
import me.jayfella.webop3.WebOp3Plugin;
import org.eclipse.jetty.websocket.api.Session;

public class WebOpUser
{
    private final String username;
    private final String hash;
    
    private long lastActivity;
    
    private Session websocketSession;
    
    public WebOpUser(String username)
    {
        this.username = username;
        this.hash = WebOp3Plugin.PluginContext.getSessionManager().generateSession(username);
        
        this.lastActivity = System.currentTimeMillis();
    }
    
    public String getName() { return this.username; }
    public String getSession() { return this.hash; }
    
    public Session getWebSocketSession() { return this.websocketSession; }
    public void setWebSocketSession(Session websocketSession) { this.websocketSession = websocketSession; }
    
    public void updateLastActivity() { this.lastActivity = System.currentTimeMillis(); }

    public boolean isSessionExpired() 
    {
        Date currentDate = new Date(System.currentTimeMillis());
        Date lastActive = new Date(this.lastActivity);
        
        return (currentDate.getTime() - lastActive.getTime() > 15 * 60 * 1000);
    }
    
    public boolean isWebSocketReady()
    {
        return (!(websocketSession == null || websocketSession.isOpen() == false));
    }
    
    public void sendSocketMessage(String message)
    {
        if (!isWebSocketReady())
            return;
        
        try
        {
            websocketSession.getRemote().sendString(message);
        }
        catch (Exception ex)
        {
            WebOp3Plugin.PluginContext.getPlugin().getLogger().log(Level.WARNING, "WebSocket Error:", ex);
        }
    }
    
}
