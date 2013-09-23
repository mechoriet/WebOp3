
package me.jayfella.webop3.website.pages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.website.WebPage;

public class Error404 extends WebPage
{
    public Error404()
    {
        this.setResponseCode(404);
        this.setContentType("text/html; charset=utf-8");
    }

    @Override
    public byte[] get(HttpServletRequest req, HttpServletResponse resp)
    {
        String page = this.loadResource("me/jayfella/webop3/website/html/404.html");
        page = addSiteTemplate(page, "[WebOp] Error", req);
        return page.getBytes();
    }

    @Override
    public byte[] post(HttpServletRequest req, HttpServletResponse resp)
    {
        String page = this.loadResource("me/jayfella/webop3/website/html/404.html");
        page = addSiteTemplate(page, "[WebOp] Error", req);
        return page.getBytes();
    }
    
}
