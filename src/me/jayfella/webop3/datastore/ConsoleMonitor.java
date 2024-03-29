package me.jayfella.webop3.datastore;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import me.jayfella.webop3.PluginContext;
import me.jayfella.webop3.WebOp3Plugin;
import me.jayfella.webop3.core.SocketSubscription;
import me.jayfella.webop3.core.WebOpUser;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ConsoleMonitor extends SocketSubscription
{
    private final PluginContext context;
    
    public ConsoleMonitor(PluginContext context)
    {
        this.context = context;
        
        Logger log = Logger.getLogger("Minecraft-Server");
        log.addHandler(new ConsoleLogHandler());
    }
    
    public void executeCommand(String command, boolean asConsole, String username)
    {
        if (asConsole)
        {
            boolean isOp = context.getPlugin().getServer().getOfflinePlayer(username).isOp();

            if (isOp | context.getSessionManager().canExecuteConsoleOpCommands(username))
            {
                String resp = new StringBuilder()
                        .append(ChatColor.DARK_GREEN).append("Player ")
                        .append(ChatColor.GREEN).append(username)
                        .append(ChatColor.DARK_GREEN).append(" issued console command: ")
                        .append(ChatColor.GOLD).append(command)
                        .toString();

                context.getPlugin().getLogger().log(Level.INFO, resp);
                context.getPlugin().getServer().dispatchCommand(context.getPlugin().getServer().getConsoleSender(), command);
            }
            else
            {
                String errorString = new StringBuilder()
                        .append(ChatColor.RED).append("Player ")
                        .append(ChatColor.GREEN).append(username)
                        .append(ChatColor.RED).append(" tried to execute a command as console without permission: ")
                        .append(ChatColor.GOLD).append(command)
                        .toString();

                context.getPlugin().getLogger().log(Level.WARNING, errorString);
            }
        }
        else
        {
            Player player = context.getPlugin().getServer().getPlayerExact(username);

            if (player == null)
            {
                String errorString = new StringBuilder()
                        .append(ChatColor.RED).append("Player ")
                        .append(ChatColor.GREEN).append(username)
                        .append(ChatColor.RED).append(" tried to issue a command whilst not logged in: ")
                        .append(ChatColor.GOLD).append(command)
                        .toString();

                context.getPlugin().getLogger().log(Level.WARNING, errorString);
            }
            else
            {
                player.chat(command);
            }
        }
    }

    
    private  class ConsoleLogHandler extends Handler
    {
        @Override public synchronized void publish(LogRecord record)
        {
            Object[] params = record.getParameters();
            String output = record.getMessage();

            if ((!(params == null)) && params.length > 0)
            {
                for (int i = 0; i <  params.length; i++)
                {
                    output = output.replace("{" + i + "}", params[i].toString());
                }
            }
            
            output = output.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            output = output.replace("\r\n", "<br/>");
            output = output.replace("\r", "<br/>");
            output = output.replace("\n", "<br/>");
            
            output = StringEscapeUtils.escapeJava(output);
            output = "[" + record.getLevel().getName() + "] " + output;
            output = parseMcColors(output);

            output = output.trim();
            
            for (final WebOpUser user : context.getSessionManager().getLoggedInUsers())
            {
                if (isSubscriber(user.getName()))
                {
                    if (user.getWebSocketSession() == null || user.getWebSocketSession().isOpen() == false)
                        continue;
                    
                    final String response = "case=consoleData;" + "<span class='consoleLine'>" + output +  "<br/></span>";
                    
                    WebOp3Plugin.PluginContext.getPlugin().getServer().getScheduler().runTask(WebOp3Plugin.PluginContext.getPlugin(), 
                    new Runnable()
                    {
                       @Override
                       public void run()
                       {
                           if (user.getWebSocketSession() == null || user.getWebSocketSession().isOpen() == false)
                               return;
                           
                           try { user.getWebSocketSession().getRemote().sendString(response); }
                            catch (IOException ex) {}
                       }
                    });                    
                }
            }
        }

        @Override public void flush() { }
        @Override public void close() throws SecurityException { }

        private String parseMcColors(String string)
        {

            // colors
            string = string

                    .replace("u00A70", "<span style='color: #c0c0c0'>") // &0
                    .replace("u00A71", "<span style='color: #0000aa'>") // &1
                    .replace("u00A72", "<span style='color: #00aa00'>") // &2
                    .replace("u00A73", "<span style='color: #00aaaa'>") // &3
                    .replace("u00A74", "<span style='color: #aa0000'>") // &4
                    .replace("u00A75", "<span style='color: #aa00aa'>") // &5
                    .replace("u00A76", "<span style='color: #ffaa00'>") // &6
                    .replace("u00A77", "<span style='color: #aaaaaa'>") // &7
                    .replace("u00A78", "<span style='color: #555555'>") // &8
                    .replace("u00A79", "<span style='color: #5555ff'>") // &9
                    .replace("u00A7a", "<span style='color: #55ff55'>") // &A
                    .replace("u00A7A", "<span style='color: #55ff55'>") // &A
                    .replace("u00A7b", "<span style='color: #55ffff'>") // &b
                    .replace("u00A7B", "<span style='color: #55ffff'>") // &b
                    .replace("u00A7c", "<span style='color: #ff5555'>") // &c
                    .replace("u00A7C", "<span style='color: #ff5555'>") // &c
                    .replace("u00A7d", "<span style='color: #ff55ff'>") // &d
                    .replace("u00A7D", "<span style='color: #ff55ff'>") // &d
                    .replace("u00A7e", "<span style='color: #ffff55'>") // &e
                    .replace("u00A7E", "<span style='color: #ffff55'>") // &e
                    .replace("u00A7f", "<span style='color: #ffffff'>") // &f
                    .replace("u00A7F", "<span style='color: #ffffff'>") // &f
                    .replace("u00A7", "<span style='color: #ffffff'>") // reset

                    .replace("\\u001B[m", "<span style='color: #c0c0c0'>") // &0
                    .replace("\\u001B[0;34;22m", "<span style='color: #0000aa'>") // &1
                    .replace("\\u001B[0;32;22m", "<span style='color: #00aa00'>") // &2
                    .replace("\\u001B[0;36;22m", "<span style='color: #00aaaa'>") // &3
                    .replace("\\u001B[0;31;22m", "<span style='color: #aa0000'>") // &4
                    .replace("\\u001B[0;35;22m", "<span style='color: #aa00aa'>") // &5
                    .replace("\\u001B[0;33;22m", "<span style='color: #ffaa00'>") // &6
                    .replace("\\u001B[0;37;22m", "<span style='color: #aaaaaa'>") // &7
                    .replace("\\u001B[0;30;1m", "<span style='color: #555555'>") // &8
                    .replace("\\u001B[0;34;1m", "<span style='color: #5555ff'>") // &9
                    .replace("\\u001B[0;32;1m", "<span style='color: #55ff55'>") // &A
                    .replace("\\u001B[0;36;1m", "<span style='color: #55ffff'>") // &b
                    .replace("\\u001B[0;31;1m", "<span style='color: #ff5555'>") // &c
                    .replace("\\u001B[0;35;1m", "<span style='color: #ff55ff'>") // &d
                    .replace("\\u001B[0;33;1m", "<span style='color: #ffff55'>") // &e
                    .replace("\\u001B[0;37;1m", "<span style='color: #ffffff'>") // &f
                    .replace("\\", ""); // escaped strings

            

            // close all spans.
            int count = 0;
            String term = "<span style=";
            int result = string.indexOf(term);

            while(result !=-1)
            {
                result = string.indexOf(term, result+1);
                count++;
            }

            for (int i = 0; i < count; i++)
            {
                string = string + "</span>";
            }

            return string;
        }
    }
}
