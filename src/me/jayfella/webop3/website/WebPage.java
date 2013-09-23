
package me.jayfella.webop3.website;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.WebOp3Plugin;

public abstract class WebPage
{
    private int responseCode;
    private String contentType;
    
    public abstract byte[] get(HttpServletRequest req, HttpServletResponse resp);
    public abstract byte[] post(HttpServletRequest req, HttpServletResponse resp);
    
    public int getResponseCode() { return this.responseCode; }
    public void setResponseCode(int value) { this.responseCode = value; }
    
    public String getContentType() { return this.contentType; }
    public void setContentType(String value) { this.contentType = value; }
    
    public String addSiteTemplate(String content, String title, HttpServletRequest req)
    {
        String result = loadResource("me/jayfella/webop3/website/html/overall_layout.html")
                .replace("{page_body}", content)
                .replace("{title}", title);
        
        if (WebOp3Plugin.PluginContext.getSessionManager().isValidCookie(req))
        {
            String username = "";
            
            for (Cookie cookie : req.getCookies())
            {
                if (cookie.getName().equals("webop_user"))
                {
                    username = cookie.getValue();
                    break;
                }
            }
            
            result = result.replace("{main_menu}", loadResource("me/jayfella/webop3/website/html/mainmenu.html"));
            result = result.replace("{username}", username);
        }
        else
        {
            result = result.replace("{main_menu}", "");
        }
        
        return result;
    }
    
    
    public String loadResource(String path)
    {
        String output = "";

        try(InputStream inp = getClass().getClassLoader().getResourceAsStream(path))
        {
            try(BufferedReader rd = new BufferedReader(new InputStreamReader(inp)))
            {
                String s;

                while (null != (s = rd.readLine()))
                {
                    output += s + "\n";
                }
            }
        }
        catch (Exception ex)
        {
            // WebOp3Plugin.PluginContext.getPlugin().getLogger().log(Level.INFO, "unable to locate javascript file: {0}", path);
            return "";
        }

        return output;
    }
    
}
