package me.jayfella.webop3.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpCookie;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import me.jayfella.webop3.PluginContext;
import me.jayfella.webop3.WebOp3Plugin;
import org.eclipse.jetty.websocket.api.Session;

public class SessionManager
{
    private final PluginContext context;
    
    private final List<Session> websocketSessions;
    private final List<WebOpUser> users;
    
    private final List<String> whitelist;
    private final List<String> consoleViewPermission;
    private final List<String> consoleUsePermission;
    
    public SessionManager(PluginContext context)
    {
        this.context = context;
        
        this.websocketSessions = new ArrayList<>();
        this.users = new ArrayList<>();
        
        this.whitelist = populateWhitelist("whitelist");
        this.consoleViewPermission = populateWhitelist("consoleView");
        this.consoleUsePermission = populateWhitelist("consoleOp");
        
        sessionRoundRobin();
    }
    
    public enum ListType { Whitelist, ConsoleView, ConsoleAsOp };
    
    private void sessionRoundRobin()
    {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                List<String> names = new ArrayList<>();
                
                for (WebOpUser user : users)
                    names.add(user.getName());
                
                for (String name : names)
                {
                    WebOpUser user = getUser(name);
                    
                    if (user == null)
                        continue;
                    
                    if (user.isSessionExpired())
                        logUserOut(name);
                }
                
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    private List<String> populateWhitelist(String node)
    {
        List<String> list = new ArrayList<>();
        list.addAll(context.getPluginSettings().getFileConfiguration().getStringList(node));
        return list;
    }
    
    
    public List<String> getWhitelist()
    {
        return this.whitelist;
    }
    
    public boolean isWhitelisted(String username)
    {
        if (WebOp3Plugin.PluginContext.getPlugin().getServer().getOfflinePlayer(username).isOp())
            return true;
        
        return whitelist.contains(username);
    }
    
    public void addToWhitelist(String name)
    {
        if (!isWhitelisted(name))
            this.whitelist.add(name);

        context.getPluginSettings().getFileConfiguration().set("whitelist", whitelist);
        context.getPlugin().saveConfig();
    }
    
    public void removeFromWhitelist(String name)
    {
        if (isWhitelisted(name))
            this.whitelist.remove(name);

        if (isLoggedIn(name))
            logUserOut(name);

        context.getPluginSettings().getFileConfiguration().set("whitelist", whitelist);
        context.getPlugin().saveConfig();
    }
    
    
    
    public List<String> getConsoleViewList()
    {
        return this.consoleViewPermission;
    }
    
    public boolean canViewConsole(String username) 
    { 
        if (WebOp3Plugin.PluginContext.getPlugin().getServer().getOfflinePlayer(username).isOp())
            return true;
        
        return (consoleViewPermission.contains(username)); 
    }
    
    public void addToConsoleViewWhitelist(String name)
    {
        if (!canViewConsole(name))
            this.consoleViewPermission.add(name);

        context.getPluginSettings().getFileConfiguration().set("consoleView", consoleViewPermission);
        context.getPlugin().saveConfig();
    }
    
    public void removeFromConsoleViewWhitelist(String name)
    {
        if (canViewConsole(name))
            this.consoleViewPermission.remove(name);

        context.getPluginSettings().getFileConfiguration().set("consoleView", consoleViewPermission);
        context.getPlugin().saveConfig();
    }
    
    
    
    public List<String> getConsoleUseList()
    {
        return this.consoleUsePermission;
    }
    
    public boolean canExecuteConsoleOpCommands(String username) 
    { 
        if (WebOp3Plugin.PluginContext.getPlugin().getServer().getOfflinePlayer(username).isOp())
            return true;
        
        return (consoleUsePermission.contains(username)); 
    }
    
    public void addToConsoleOpWhitelist(String name)
    {
        if (!canExecuteConsoleOpCommands(name))
            this.consoleUsePermission.add(name);

        context.getPluginSettings().getFileConfiguration().set("consoleOp", consoleUsePermission);
        context.getPlugin().saveConfig();
    }
    
    public void removeFromConsoleOpWhitelist(String name)
    {
        if (canExecuteConsoleOpCommands(name))
            this.consoleUsePermission.remove(name);

        context.getPluginSettings().getFileConfiguration().set("consoleOp", consoleUsePermission);
        context.getPlugin().saveConfig();
    }
    
    
    
    private boolean isValidUserAndSession(String username, String session)
    {
        if (username.isEmpty() || session.isEmpty())
            return false;
        
        for (WebOpUser user : users)
        {
            if (user.getName().equals(username) && user.getSession().equals(session))
            {
                if (user.isSessionExpired())
                    return false;
                
                user.updateLastActivity();
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isValidCookie(HttpServletRequest req)
    {
        Cookie[] cookies = req.getCookies();
        
        String username = "";
        String session = "";
        
        for (Cookie cookie : cookies)
        {
            switch (cookie.getName())
            {
                case "webop_user":
                    username = cookie.getValue();
                    break;
                case "webop_session":
                    session = cookie.getValue();
                    break;
            }
        }
        
        return isValidUserAndSession(username, session);
    }
    
    public String getUsername(HttpServletRequest req)
    {
        Cookie[] cookies = req.getCookies();
        
        for (Cookie cookie : cookies)
        {
            if (cookie.getName().equals("webop_user"))
            {
                    return cookie.getValue();
            }
        }
        
        return "";
    }
    
    public boolean isValidWebSocketConnection(String message)
    {
        Map<String, String> params = parseWebSocketRequest(message);
        
        String userParam = params.get("webop_user");
        String sessionParam = params.get("webop_session");
        
        return isValidUserAndSession(userParam, sessionParam);
    }
    
    public boolean isValidWebSocketConnection(Session session)
    {
        List<HttpCookie> cookies = session.getUpgradeRequest().getCookies();
        
        String socketUser = "";
        String socketSession = "";
        
        for (HttpCookie cookie : cookies)
        {
            switch (cookie.getName())
            {
                case "webop_user":
                    socketUser = cookie.getValue();
                    break;
                case "webop_session":
                    socketSession = cookie.getValue();
                    break;
            }
        }
        
        return isValidUserAndSession(socketUser, socketSession);
    }
    
    public Map<String, String> parseWebSocketRequest(String query)
    {
        Map<String, String> results = new HashMap<>();

        String[] pairs = query.split("&");

        for (String pair : pairs)
        {
            String[] param = pair.split("=");

            if (param.length == 2)
            {
                try
                {
                    results.put(param[0], URLDecoder.decode(param[1], "UTF-8"));
                }
                catch (UnsupportedEncodingException ex)
                {
                    results.put(param[0], "");
                }
            }
            else
            {
                results.put(param[0], "");
            }
        }

        return results;
    }
    
    public void addSession(Session session)
    {
        if (!websocketSessions.contains(session))
            websocketSessions.add(session);
    }
    
    public void removeSession(Session session)
    {
        if (websocketSessions.contains(session))
            websocketSessions.remove(session);
    }
    
    public List<Session> getSessions()
    {
        return this.websocketSessions;
    }
 
    public WebOpUser getUser(String name)
    {
        for (int i = 0; i < users.size(); i++)
        {
            WebOpUser user = users.get(i);
            
            if (user.getName().equals(name))
                return user;
        }
        
        return null;
    }
    
    public List<WebOpUser> getLoggedInUsers()
    { 
        return this.users; 
    }
    
    public void logUserIn(WebOpUser user)
    {
        if (!users.contains(user))
            users.add(user);
        
        for (WebOpUser person : users)
        {
            if (person.getWebSocketSession() == null || person.getWebSocketSession().isOpen() == false)
                continue;
            
            try { person.getWebSocketSession().getRemote().sendString("case=activityNotification;action=login;user=" + user.getName()); }
            catch (IOException ex) { }
        }
    }
    
    public boolean isLoggedIn(String username) 
    { 
        for (int i = 0; i < users.size(); i++)
        {
            WebOpUser user = users.get(i);
            
            if (user.getName().equals(username))
            {
                return true;
            }
        }
        
        return false;
    }
    
    public void logUserOut(String username)
    {
        Iterator<WebOpUser> iterator = users.iterator();
        
        while (iterator.hasNext())
        {
            WebOpUser user = iterator.next();
            
            if (user.getName().equals(username))
            {
                if (user.getWebSocketSession() != null)
                {
                    websocketSessions.remove(user.getWebSocketSession());
                    
                    try
                    {
                        user.getWebSocketSession().close();
                    }
                    catch (IOException ex) { }
                }
                
                for (WebOpUser person : users)
                {
                    if (person.getWebSocketSession() == null || person.getWebSocketSession().isOpen() == false)
                        continue;

                    try { person.getWebSocketSession().getRemote().sendString("case=activityNotification;action=logout;user=" + user.getName()); }
                    catch (IOException ex) { }
                }
                
                
                iterator.remove();
                return;
            }
        }
    }
    
    private String generateSalt()
    {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }
    
    private String hexEncode( byte[] aInput)
    {
        StringBuilder result = new StringBuilder();
        char[] digits = {'0', '1', '2', '3', '4','5','6','7','8','9','a','b','c','d','e','f'};
        for (int idx = 0; idx < aInput.length; ++idx)
        {
            byte b = aInput[idx];
            result.append( digits[ (b&0xf0) >> 4 ] );
            result.append( digits[ b&0x0f] );
        }

        return result.toString();
    }

    public String generateSession(String str)
    {
        try
        {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");

            String hash = str + generateSalt();

            byte[] hashOne = sha.digest(hash.getBytes());
            return hexEncode(hashOne);
        }
        catch (NoSuchAlgorithmException ex)
        {
            Logger.getLogger(SessionManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
}
