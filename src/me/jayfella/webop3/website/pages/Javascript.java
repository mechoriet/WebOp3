package me.jayfella.webop3.website.pages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.website.WebPage;

public class Javascript extends WebPage
{
    public Javascript()
    {
        this.setResponseCode(200);
        this.setContentType("text/javascript; charset=utf-8");
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
        
        // me/jayfella/webop3/website/javascript/
        // me/jayfella/webop3/website/javascript/
        // me/jayfella/webop3/website/css/
        String script = this.loadResource("me/jayfella/webop3/website/javascript/" + requestedFile);
        
        if (!script.isEmpty())
            return script.getBytes();

        this.setResponseCode(404);
        return new byte[0];
    }

    @Override public byte[] post(HttpServletRequest req, HttpServletResponse resp) { return new byte[0]; }
    
}
