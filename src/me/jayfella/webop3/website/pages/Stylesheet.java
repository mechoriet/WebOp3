package me.jayfella.webop3.website.pages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.website.WebPage;


public class Stylesheet extends WebPage
{
    public Stylesheet()
    {
        this.setResponseCode(200);
        this.setContentType("text/css; charset=utf-8");
    }

    @Override
    public byte[] get(HttpServletRequest req, HttpServletResponse resp)
    {
        String requestedFile = req.getParameter("file");
        
        if (requestedFile == null || requestedFile.isEmpty())
        {
            this.setResponseCode(404);
            return new byte[0];
        }
        
        String stylesheet = this.loadResource("me/jayfella/webop3/website/css/" + requestedFile);
        
        if (!stylesheet.isEmpty())
            return stylesheet.getBytes();

        this.setResponseCode(404);
        return new byte[0];
    }

    @Override public byte[] post(HttpServletRequest req, HttpServletResponse resp) { return new byte[0]; }
}
