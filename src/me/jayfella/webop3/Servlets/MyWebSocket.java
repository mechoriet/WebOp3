package me.jayfella.webop3.Servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.core.MessagePriority;
import me.jayfella.webop3.core.WebOpMessage;
import me.jayfella.webop3.core.WebOpUser;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket()
public class MyWebSocket
{
    private void sendMessage(Session session, String message)
    {
        if (session.isOpen() == false)
            return;
        
        try
        {
            session.getRemote().sendString(message);
        }
        catch (Exception ex)
        {
            WebOp3Plugin.PluginContext.getPlugin().getLogger().log(Level.WARNING, "WebSocket Error:", ex);
        }
    }
    
    @OnWebSocketConnect
    public void onConnect(Session session) 
    {
        WebOp3Plugin.PluginContext.getSessionManager().addSession(session);
    }

    @OnWebSocketMessage
    public void onMessage(final Session session, String message) 
    {
        // ignore anything that doesnt send along valid authentication data
        if (!WebOp3Plugin.PluginContext.getSessionManager().isValidWebSocketConnection(session)) return;
        if (!WebOp3Plugin.PluginContext.getSessionManager().isValidWebSocketConnection(message)) return;
        
        Map<String, String> map = WebOp3Plugin.PluginContext.getSessionManager().parseWebSocketRequest(message);
        
        String socketUser = map.get("webop_user");
        String socketSession = map.get("webop_session");
        
        for (int i = 0; i < WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers().size(); i++)
        {
            WebOpUser user = WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers().get(i);
            
            if (user.getName().equals(socketUser) && user.getSession().equals(socketSession))
            {
                user.setWebSocketSession(session);
            }
        }
        
        String socketCase = map.get("case");
        
        if (socketCase == null || socketCase.isEmpty())
            return;
        
        switch(socketCase)
        {
            case "serverUtilization":
            {
                WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTask(WebOp3Plugin.PluginContext.getPlugin(), new Runnable()
                {
                   @Override
                   public void run()
                   {
                        int allChunksCount = 0;
                        int allEntitiesCount = 0;
                
                        for (World world : WebOp3Plugin.PluginContext.getPlugin().getServer().getWorlds())
                        {
                            if (world.getLoadedChunks() != null)
                                allChunksCount += world.getLoadedChunks().length;
                            
                            if (world.getEntities() != null)
                                allEntitiesCount += world.getEntities().size();
                        }
                
                        String response = new StringBuilder()
                                .append("case=serverUtilization;")
                                .append("CPU=").append(WebOp3Plugin.PluginContext.getUtilizationMonitor().getCpuLoadPercent()).append(";")
                                .append("MEM=").append(WebOp3Plugin.PluginContext.getUtilizationMonitor().getUsedMemoryPercent()).append(";")
                                .append("TPS=").append(WebOp3Plugin.PluginContext.getUtilizationMonitor().getCurrentTPS()).append(";")
                                .append("CHUNKS=").append(allChunksCount).append(";")
                                .append("ENTITIES=").append(allEntitiesCount)
                                .toString();
                
                        sendMessage(session, response.toString());
                   }
                });
                
                break;
            }
            case "subscribeAllPlayersData":
            {
                WebOp3Plugin.PluginContext.getPlayerMonitor().addSubscriber(socketUser);
                sendMessage(session, WebOp3Plugin.PluginContext.getPlayerMonitor().generatePlayerString());
                
                break;
            }
            case "subscribeConsole":
            {
                if (!WebOp3Plugin.PluginContext.getSessionManager().canViewConsole(socketUser))
                    return;
                
                WebOp3Plugin.PluginContext.getConsoleMonitor().addSubscriber(socketUser);
                
                break;
            }
            case "chat":
            {
                String sanitizedMsg = message
                        .replace("&webop_user=" + socketUser, "")
                        .replace("webop_user=" + socketUser, "")
                        .replace("&webop_session=" + socketSession, "")
                        .replace("webop_session=" + socketSession, "")
                        .replace("&case=" + socketCase, "")
                        .replace("case=" + socketCase, "")
                        .replace("&msg=", "")
                        .replace("msg=", "")
                        .replace(" ", "%20");
                
                if (sanitizedMsg.length() > 256)
                    sanitizedMsg = sanitizedMsg.substring(0, 255);
                
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                String time = df.format(new Date(System.currentTimeMillis()));
                
                try 
                { 
                    sanitizedMsg = URLEncoder.encode(sanitizedMsg, "UTF-8"); 
                }
                catch (IOException ex) { }
                
                String response = "case=chatMessage;user=" + socketUser + ";time=" + time + ";msg=" + sanitizedMsg;
                
                for (WebOpUser user : WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers())
                {
                    user.sendSocketMessage(response);
                }
                
                break;
            }
            case "message":
            {
                String action = map.get("action");
                
                if (action == null || action.isEmpty())
                    return;
                
                switch (action)
                {
                    case "create":
                    {
                        String socketMsg = map.get("msg");
                        String socketPriority = map.get("priority");
                        
                        if (socketMsg == null || socketMsg.isEmpty()) return;
                        if (socketPriority == null || socketPriority.isEmpty()) return;
                        
                        String sanitizedMsg = message
                                .replace("&webop_user=" + socketUser, "")
                                .replace("webop_user=" + socketUser, "")
                                .replace("&webop_session=" + socketSession, "")
                                .replace("webop_session=" + socketSession, "")
                                .replace("&case=" + socketCase, "")
                                .replace("case=" + socketCase, "")
                                .replace("&action=" + action, "")
                                .replace("action=" + action, "")
                                .replace("&priority=" + socketPriority, "")
                                .replace("priority=" + socketPriority, "")
                                .replace("&msg=", "")
                                .replace("msg=", "")
                                .replace(" ", "%20");
                        
                        try 
                        { 
                            sanitizedMsg = URLEncoder.encode(sanitizedMsg, "UTF-8"); 
                        }
                        catch (IOException ex) { }
                        
                        MessagePriority msgPriority = MessagePriority.valueOf(socketPriority);
                        
                        WebOpMessage newMessage = WebOp3Plugin.PluginContext.getMessageHandler().createMessage(socketUser, msgPriority, sanitizedMsg);
                        
                        String response = "case=message;action=new;" + WebOp3Plugin.PluginContext.getMessageHandler().createWebSocketString(newMessage);
                        
                        for (WebOpUser user : WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers())
                        {
                            user.sendSocketMessage(response);
                        }
                        
                        break;
                    }
                    case "delete":
                    {
                        String socketMsgId = map.get("msgId");
                        
                        if (socketMsgId == null || socketMsgId.isEmpty())
                            return;
                        
                        int msgId = Integer.valueOf(socketMsgId);
                        
                        WebOp3Plugin.PluginContext.getMessageHandler().deleteMessage(msgId);
                        
                        String response = "case=message;action=delete;msgId=" + msgId;
                        
                        for (WebOpUser user : WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers())
                        {
                            user.sendSocketMessage(response);
                        }
                        
                        break;
                    }
                    case "retrieve":
                    {
                        for (WebOpMessage msg : WebOp3Plugin.PluginContext.getMessageHandler().getMessages())
                        {
                            String response = "case=message;action=new;" + WebOp3Plugin.PluginContext.getMessageHandler().createWebSocketString(msg);
                            sendMessage(session, response);
                        }
                        
                        break;
                    }
                    
                    default: break;
                }
                
                break;                
            }
            case "consoleCommand":
            {
                String socketCommand = map.get("command");
                String socketAsConsole = map.get("asConsole");
                
                if (socketCommand == null || socketCommand.isEmpty()) return;
                if (socketAsConsole == null || socketAsConsole.isEmpty()) return;
                
                boolean asConsole = Boolean.valueOf(socketAsConsole);
                
                String sanitizedCommand = message
                        .replace("&webop_user=" + socketUser, "")
                        .replace("webop_user=" + socketUser, "")
                        .replace("&webop_session=" + socketSession, "")
                        .replace("webop_session=" + socketSession, "")
                        .replace("&case=" + socketCase, "")
                        .replace("case=" + socketCase, "")
                        .replace("&asConsole=" + socketAsConsole, "")
                        .replace("asConsole=" + socketAsConsole, "")
                        .replace("&command=", "")
                        .replace("command=", "");
                
                WebOp3Plugin.PluginContext.getConsoleMonitor().executeCommand(sanitizedCommand, asConsole, socketUser);
                break;
            }
            case "teleport":
            {
                String socketAction = map.get("action");
                
                if (socketAction == null || socketAction.isEmpty())
                    return;
                
                switch(socketAction)
                {
                    case "player":
                    {
                        String socketToPlayer = map.get("to");
                        
                        if (socketToPlayer == null || socketToPlayer.isEmpty())
                            return;
                        
                        final Player playerToTeleport = WebOp3Plugin.PluginContext.getPlugin().getServer().getPlayer(socketUser);
                        final Player playerDestination = WebOp3Plugin.PluginContext.getPlugin().getServer().getPlayer(socketToPlayer);
                        
                        if (playerToTeleport == null || playerDestination == null)
                            return;
                        
                        WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTask(WebOp3Plugin.PluginContext.getPlugin(), new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                playerToTeleport.teleport(playerDestination.getLocation());
                            }
                        });
                        
                        break;
                    }
                    case "coord":
                    {
                        String socketX = map.get("x");
                        String socketY = map.get("y");
                        String socketZ = map.get("z");
                        String socketW = map.get("w");
                        
                        if (socketX == null || socketX.isEmpty() || socketY == null || socketY.isEmpty() || socketZ == null || socketZ.isEmpty() || socketW == null || socketW.isEmpty())
                            return;
                        
                        final Player playerToTeleport = WebOp3Plugin.PluginContext.getPlugin().getServer().getPlayer(socketUser);
                        
                        if (playerToTeleport == null)
                            return;
                        
                        int x, y, z;
                        
                        try
                        {
                            x = Integer.parseInt(socketX);
                            y = Integer.parseInt(socketY);
                            z = Integer.parseInt(socketZ);
                        }
                        catch(NumberFormatException ex)
                        {
                            return;
                        }

                        World world = WebOp3Plugin.PluginContext.getPlugin().getServer().getWorld(socketW);
                        if (world == null) return;

                        final Location teleportLocation = new Location(world, x, y, z);
                        
                        WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTask(WebOp3Plugin.PluginContext.getPlugin(), new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                playerToTeleport.teleport(teleportLocation);
                            }
                        });
                        
                        break;
                    }
                        
                    default: break;
                }
                
                break;
            }
            case "subscribeWorldData":
            {
                WebOp3Plugin.PluginContext.getWorldMonitor().addSubscriber(socketUser);
                
                for (World world : WebOp3Plugin.PluginContext.getPlugin().getServer().getWorlds())
                {
                    String response = "case=worldData;" + WebOp3Plugin.PluginContext.getWorldMonitor().getWorldDetails(world);
                    sendMessage(session, response);
                }
                
                break;
            }
                
            default: break;
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) 
    {
        Iterator<Session> iterator = WebOp3Plugin.PluginContext.getSessionManager().getSessions().iterator();
        
        WebOpUser closedUser = null;
        
        while (iterator.hasNext())
        {
            Session sess = iterator.next();
            
            if (!sess.isOpen())
            {
                for (int i = 0; i < WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers().size(); i++)
                {
                    WebOpUser user = WebOp3Plugin.PluginContext.getSessionManager().getLoggedInUsers().get(i);
                    
                    if (user.getWebSocketSession().equals(sess))
                    {
                        user.setWebSocketSession(null);
                        closedUser = user;
                        
                        // remove any subscriptions that the player has
                        WebOp3Plugin.PluginContext.getConsoleMonitor().removeSubscriber(user.getName());
                        WebOp3Plugin.PluginContext.getPlayerMonitor().removeSubscriber(user.getName());
                        WebOp3Plugin.PluginContext.getWorldMonitor().removeSubscriber(user.getName());
                        
                        break;
                    }
                }
                
                iterator.remove();
            }
        }
        
        StringBuilder errorMessage = new StringBuilder().append("WebSocket Closed. Code: ").append(statusCode);
        
        if (!(reason == null || reason.isEmpty()))
            errorMessage.append(", Reason: ").append(reason);
        else
            errorMessage.append(", Reason: None");
                
        
        if (closedUser != null)
        {
            WebOp3Plugin.PluginContext.getPlugin().getLogger().log(Level.INFO, closedUser.getName() + "'s " + errorMessage.toString());
        }
        else
        {
            WebOp3Plugin.PluginContext.getPlugin().getLogger().log(Level.INFO, errorMessage.toString());
        }
        
    }
    
    @OnWebSocketError
    public void onError(Session session, Throwable throwable)
    {
        
    }
}
