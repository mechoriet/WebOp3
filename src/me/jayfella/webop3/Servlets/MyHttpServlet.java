package me.jayfella.webop3.Servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.website.PageHandler;
import me.jayfella.webop3.website.WebPage;

public class MyHttpServlet extends HttpServlet
{
    private final PageHandler pageHandler;
    
    public MyHttpServlet()
    {
        this.pageHandler = new PageHandler();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String requestedPage = req.getRequestURI().replace("/", "");
        
        WebPage page;
        
        // .htaccess
        if (requestedPage.length() < 1)
        {
            if (WebOp3Plugin.PluginContext.getSessionManager().isValidCookie(req))
            {
                page = this.pageHandler.getWebPage("index.php");
            }
            else
            {
                page = this.pageHandler.getWebPage("login.php");
            }
        }
        else
        {
            page = this.pageHandler.getWebPage(requestedPage);
            
        }
        
        try
        {
            byte[] content = gzip(page.get(req, resp));

            resp.setHeader("Content-Encoding", "gzip");
            resp.setContentType(page.getContentType());
            resp.setStatus(page.getResponseCode());
            resp.setContentLength(content.length);
            
            resp.getOutputStream().write(content);
        }
        catch(Exception ex) { }        
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String requestedPage = req.getRequestURI().replace("/", "");
        WebPage page = this.pageHandler.getWebPage(requestedPage);

        try
        {
            byte[] content = gzip(page.post(req, resp));

            resp.setHeader("Content-Encoding", "gzip");
            resp.setContentType(page.getContentType());
            resp.setStatus(page.getResponseCode());
            resp.setContentLength(content.length);
            
            resp.getOutputStream().write(content);
        }
        catch(Exception ex) { }
    }
    
    private byte[] gzip(byte[] data) throws IOException
    {
        try(ByteArrayOutputStream bytes = new ByteArrayOutputStream())
        {
            try (GZIPOutputStream out = new GZIPOutputStream(bytes))
            {
                out.write(data);
            }

            return bytes.toByteArray();
        }
    }
    
}
