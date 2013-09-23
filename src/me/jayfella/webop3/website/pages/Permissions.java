package me.jayfella.webop3.website.pages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.core.SessionManager.ListType;
import me.jayfella.webop3.website.WebPage;

public class Permissions extends WebPage
{
    public Permissions()
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
        
        String page = this.loadResource("me/jayfella/webop3/website/html/permissions.html");

        page = page.replace("{accessWhitelist_users}", generateAccessListHtml());
        page = page.replace("{consoleViewWhitelist_users}", generateConsoleViewListHtml());
        page = page.replace("{consoleAsOpWhitelist_users}", generateConsoleUseListHtml());
        
        page = this.addSiteTemplate(page, "[WebOp] Permissions", req);
        return page.getBytes();
    }

    @Override
    public byte[] post(HttpServletRequest req, HttpServletResponse resp)
    {
        if (!WebOp3Plugin.PluginContext.getSessionManager().isValidCookie(req))
        {
            return new byte[0];
        }
        
        // only ops can modify lists
        String httpUser = WebOp3Plugin.PluginContext.getSessionManager().getUsername(req);
        
        if (httpUser.isEmpty() || WebOp3Plugin.PluginContext.getPlugin().getServer().getOfflinePlayer(httpUser).isOp() == false)
            return "OP permission required".getBytes();
        
        String caseParam = req.getParameter("case");
        
        if (caseParam == null || caseParam.isEmpty())
            return "insufficient data".getBytes();
        
        switch (caseParam)
        {
            case "webopaccess":
            {
                String postAction = req.getParameter("action");
                String postPlayers = req.getParameter("players");
                
                if (postAction == null || postAction.isEmpty() || postPlayers == null || postPlayers.isEmpty())
                    return "insufficient data".getBytes();
                
                switch (postAction)
                {
                    case "add": processList(postPlayers, ListType.Whitelist, true); break;
                    case "remove": processList(postPlayers, ListType.Whitelist, false); break;
                    default: return new byte[0];
                }
                
                return generateAccessListHtml().getBytes();
            }
            case "consoleView":
            {
                String postAction = req.getParameter("action");
                String postPlayers = req.getParameter("players");
                
                if (postAction == null || postAction.isEmpty() || postPlayers == null || postPlayers.isEmpty())
                    return "insufficient data".getBytes();
                
                switch (postAction)
                {
                    case "add": processList(postPlayers, ListType.ConsoleView, true); break;
                    case "remove": processList(postPlayers, ListType.ConsoleView, false); break;
                    default: return new byte[0];
                }
                
                return generateConsoleViewListHtml().getBytes();
            }
            case "consoleAsOp":
            {
                String postAction = req.getParameter("action");
                String postPlayers = req.getParameter("players");
                
                if (postAction == null || postAction.isEmpty() || postPlayers == null || postPlayers.isEmpty())
                    return "insufficient data".getBytes();
                
                switch (postAction)
                {
                    case "add": processList(postPlayers, ListType.ConsoleAsOp, true); break;
                    case "remove": processList(postPlayers, ListType.ConsoleAsOp, false); break;
                    default: return new byte[0];
                }
                
                return generateConsoleUseListHtml().getBytes();
            }
                
            default: return new byte[0];
        }
    }
    
    private void processList(String playernames, ListType type, boolean add)
    {
        List<String> playersToProcess = new ArrayList<>();
        String[] dirtyPlayers = playernames.split(",");

        for (int i = 0; i < dirtyPlayers.length; i++)
        {
            dirtyPlayers[i] = dirtyPlayers[i].trim();
            if (dirtyPlayers[i].isEmpty()) continue;
            playersToProcess.add(dirtyPlayers[i]);
        }
        
        switch (type)
        {
            case Whitelist:
            {
                for (String player : playersToProcess)
                {
                    if (add)
                        WebOp3Plugin.PluginContext.getSessionManager().addToWhitelist(player);
                    else
                        WebOp3Plugin.PluginContext.getSessionManager().removeFromWhitelist(player);
                }
                
                break;
            }
            case ConsoleView:
            {
                for (String player : playersToProcess)
                {
                    if (add)
                        WebOp3Plugin.PluginContext.getSessionManager().addToConsoleViewWhitelist(player);
                    else
                        WebOp3Plugin.PluginContext.getSessionManager().removeFromConsoleViewWhitelist(player);
                }
                
                break;
            }
            case ConsoleAsOp:
            {
                for (String player : playersToProcess)
                {
                    if (add)
                        WebOp3Plugin.PluginContext.getSessionManager().addToConsoleOpWhitelist(player);
                    else
                        WebOp3Plugin.PluginContext.getSessionManager().removeFromConsoleOpWhitelist(player);
                }
                
                break;
            }
            default: break;
        }
    }
    
    private String generateAccessListHtml()
    {
        StringBuilder whitelistUsers = new StringBuilder();
        for (String user : WebOp3Plugin.PluginContext.getSessionManager().getWhitelist())
        {
            whitelistUsers
                    .append("<li class='ui-widget-content'>")
                    .append(user)
                    .append("</li>")
                    .append("\n");
        }
        
        return whitelistUsers.toString();
    }
    
    private String generateConsoleViewListHtml()
    {
        StringBuilder consoleViewUsers = new StringBuilder();
        for (String user : WebOp3Plugin.PluginContext.getSessionManager().getConsoleViewList())
        {
            consoleViewUsers
                    .append("<li class='ui-widget-content'>")
                    .append(user)
                    .append("</li>")
                    .append("\n");
        }
        
        return consoleViewUsers.toString();
    }
    
    private String generateConsoleUseListHtml()
    {
        StringBuilder consoleAsOpUsers = new StringBuilder();
        for (String user : WebOp3Plugin.PluginContext.getSessionManager().getConsoleUseList())
        {
            consoleAsOpUsers
                    .append("<li class='ui-widget-content'>")
                    .append(user)
                    .append("</li>")
                    .append("\n");
        }
        
        return consoleAsOpUsers.toString();
    }
    
}
