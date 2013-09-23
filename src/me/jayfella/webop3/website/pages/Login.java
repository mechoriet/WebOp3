package me.jayfella.webop3.website.pages;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.core.MojangValidator;
import me.jayfella.webop3.core.WebOpUser;
import me.jayfella.webop3.website.WebPage;

public class Login extends WebPage
{
    public Login()
    {
        this.setResponseCode(200);
        this.setContentType("text/html; charset=utf-8");
    }

    @Override
    public byte[] get(HttpServletRequest req, HttpServletResponse resp)
    {
        if (WebOp3Plugin.PluginContext.getSessionManager().isValidCookie(req))
        {
            try { resp.sendRedirect("index.php"); }
            catch (IOException ex) { }
            
            return new byte[0];
        }
        
        String page = this.loadResource("me/jayfella/webop3/website/html/login.html");
        page = addSiteTemplate(page, "[WebOp] Login", req);
        return page.getBytes();
    }

    @Override
    public byte[] post(HttpServletRequest req, HttpServletResponse resp)
    {
        return attemptLogin(req, resp);
    }
    
    private byte[] attemptLogin(HttpServletRequest req, HttpServletResponse resp)
    {
        String minecraftName = "";

        try
        {
            String username = req.getParameter("user");
            String password = req.getParameter("password");
            
            if (username == null || password == null)
                return "".getBytes();
            
            username = URLDecoder.decode(username, "UTF-8");
            password = URLDecoder.decode(password, "UTF-8");

            minecraftName = MojangValidator.isValidAccount(username, password);
        }
        catch (UnsupportedEncodingException ex)
        {
            return "Invalid username or password.".getBytes();
        }

        if (minecraftName.isEmpty())
        {
            return "Invalid username or password.".getBytes();
        }
        else
        {
            if (!WebOp3Plugin.PluginContext.getSessionManager().isWhitelisted(minecraftName))
            {
                return "Username is not whitelisted.".getBytes();
            }

            if (WebOp3Plugin.PluginContext.getSessionManager().isLoggedIn(minecraftName))
            {
                WebOp3Plugin.PluginContext.getSessionManager().logUserOut(minecraftName);
            }

            WebOpUser user = new WebOpUser(minecraftName);

            WebOp3Plugin.PluginContext.getSessionManager().logUserIn(user);

            Cookie userCookie = new Cookie("webop_user", user.getName());
            Cookie sessCookie = new Cookie("webop_session", user.getSession());
            
            resp.addCookie(userCookie);
            resp.addCookie(sessCookie);
            
            try { resp.sendRedirect("index.php"); }
            catch(IOException ex) { }
            
            return new byte[0];
        }
    }
    
}
