package me.jayfella.webop3.website.pages;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.datastore.LogReader;
import me.jayfella.webop3.website.WebPage;

public class Data extends WebPage
{
    public Data() 
    { 
        this.setResponseCode(200); 
    }
    @Override public byte[] get(HttpServletRequest req, HttpServletResponse resp)
    {
        return new byte[0]; 
    }

    @Override
    public byte[] post(HttpServletRequest req, HttpServletResponse resp)
    {
        if (!WebOp3Plugin.PluginContext.getSessionManager().isValidCookie(req))
            return new byte[0];
        
        String caseParam = req.getParameter("case");
        
        if (caseParam == null || caseParam.isEmpty())
            return new byte[0];
        
        switch(caseParam)
        {
            case "playerNameSearch":
            {
                String partialParam = req.getParameter("partialName");
                
                if (partialParam == null || partialParam.isEmpty())
                    return new byte[0];
                
                String response = WebOp3Plugin.PluginContext.getPlayerMonitor().findPlayers(partialParam);
                return response.getBytes();
            }
            case "logSearch":
            {
                String searchTermParam = req.getParameter("term");
                String timeFrameParam = req.getParameter("timeframe");
                
                if (searchTermParam == null)
                    return new byte[0];
                
                if (timeFrameParam == null)
                    timeFrameParam = "";
                
                LogReader logReader = new LogReader();
                
                return logReader.searchLog(searchTermParam, timeFrameParam).getBytes();
            }
            case "logblock":
            {
                String postOre = req.getParameter("ore");
                String postSince = req.getParameter("since");
                String postPlayer = req.getParameter("player");
                
                if (postOre == null || postOre.isEmpty()) return new byte[0];
                if (postSince == null || postSince.isEmpty()) return new byte[0];
                if (postPlayer == null) postPlayer = "";
                
                List<String> results = WebOp3Plugin.PluginContext.getLogBlockMonitor().LookupGeneralDestroyedOre(postPlayer, postOre, postSince);
                
                StringBuilder response = new StringBuilder();
                
                for (String result : results)
                {
                    response
                            .append("<div class=\"lbResult\">")
                            .append(result)
                            .append("</div>");
                }
                
                return response.toString().getBytes();
            }
            case "findEntities":
            {
                String postEntityCount = req.getParameter("count");
                String postEntities = req.getParameter("types");
                
                if (postEntityCount == null || postEntityCount.isEmpty()) return new byte[0];
                if (postEntities == null) postEntities = "";
                
                int amount = Integer.parseInt(postEntityCount);
                
                String response = (postEntities.isEmpty())
                        ? WebOp3Plugin.PluginContext.getEntityMonitor().findAllHighestEntityCountInChunks(amount)
                        : WebOp3Plugin.PluginContext.getEntityMonitor().findCertainHighestEntityCountInChunks(postEntities, amount);
                
                return response.getBytes();
            }
            case "essentials":
            {
                String postAction = req.getParameter("action");
                
                if (postAction == null || postAction.isEmpty())
                    return new byte[0];
                
                switch(postAction)
                {
                    case "playerData":
                    {
                        String postPlayer = req.getParameter("player");
                        
                        if (postPlayer == null || postPlayer.isEmpty())
                            return new byte[0];
                        
                        String response = WebOp3Plugin.PluginContext.getPlayerMonitor().generateEssentialsPlayerDataString(postPlayer);
                        
                        return response.getBytes();
                    }
                    
                    default: return new byte[0];
                }
                
            }
            case "serverprofile":
            {
                String postAction = req.getParameter("action");
                
                if (postAction == null || postAction.isEmpty())
                    return new byte[0];
                
                switch(postAction)
                {
                    case "start":
                    {
                        if (WebOp3Plugin.PluginContext.getServerProfiler().isProfiling())
                            return "Server is already profiling.".getBytes();
                        
                        WebOp3Plugin.PluginContext.getServerProfiler().startProfiling();
                        return "OK".getBytes();
                    }
                    case "stop":
                    {
                        if (WebOp3Plugin.PluginContext.getServerProfiler().isProfiling() == false)
                            return "NOT_PROFILING".getBytes();
                        
                        WebOp3Plugin.PluginContext.getServerProfiler().stopProfiling();
                        
                        String response = WebOp3Plugin.PluginContext.getServerProfiler().buildEventProfileResultRaw();
                        return response.getBytes();
                    }
                        
                    default: return new byte[0];
                }
                
            }
                
            default: return new byte[0];
        }
    }
    
}
