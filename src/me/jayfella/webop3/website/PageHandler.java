package me.jayfella.webop3.website;

import me.jayfella.webop3.website.pages.*;

public class PageHandler
{
    // static content
    private final Stylesheet stylesheet = new Stylesheet();
    private final Javascript javascript = new Javascript();
    private final Image image = new Image();
    
    public WebPage getWebPage(String pageName)
    {
        switch(pageName)
        {
            case "stylesheet.php": return stylesheet;
            case "jscript.php": return javascript;
            case "image.php": return image;
            
            case "login.php": return new Login();
            case "index.php": return new Index();
            case "data.php": return new Data();
            case "permissions.php": return new Permissions();
                
            default: return new Error404();
        }
    }
}
