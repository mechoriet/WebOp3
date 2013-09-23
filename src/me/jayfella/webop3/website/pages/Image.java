package me.jayfella.webop3.website.pages;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import me.jayfella.webop3.website.WebPage;

public class Image extends WebPage
{
    public Image()
    {
        this.setResponseCode(200);
        this.setContentType("image/png; charset=utf-8");
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
        
        InputStream inp = getClass().getClassLoader().getResourceAsStream("me/jayfella/webop3/website/images/" + requestedFile);
        
        if (inp == null)
        {
            this.setResponseCode(404);
            return new byte[0];
        }
        
        int bytesRead;
        byte[] buffer = new byte[8192];
        byte[] data;

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream())
        {
            while ((bytesRead = inp.read(buffer)) != -1)
            {
                bytes.write(buffer, 0, bytesRead);
            }

            data = bytes.toByteArray();
        }
        catch (Exception ex)
        {
            this.setResponseCode(404);
            return new byte[0];
        }

        return data;
    }

    @Override public byte[] post(HttpServletRequest req, HttpServletResponse resp) { return new byte[0]; }
}
