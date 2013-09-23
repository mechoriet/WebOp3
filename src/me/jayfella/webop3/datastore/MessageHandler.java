package me.jayfella.webop3.datastore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import me.jayfella.webop3.PluginContext;
import me.jayfella.webop3.core.MessagePriority;
import me.jayfella.webop3.core.WebOpMessage;

public class MessageHandler
{
    private final PluginContext context;
    private final File messageFolder;
    private final List<WebOpMessage> messages;
    
    private int biggestMessageId = 0;
    
    public MessageHandler(PluginContext context)
    {
        this.context = context;
        
        this.messageFolder = new File(this.context.getPlugin().getDataFolder() + File.separator + "/messages");
        if (!messageFolder.isDirectory()) messageFolder.mkdirs();
        
        this.messages = loadMessages();
    }
    
    private List<WebOpMessage> loadMessages()
    {
        List<WebOpMessage> loadedMessages = new ArrayList<>();

        for (String file : messageFolder.list())
        {
            if (!file.endsWith(".txt"))
                continue;

            File msgFile = new File(messageFolder + File.separator + file);

            List<String> lines = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(msgFile.getAbsoluteFile())))
            {
                String line;

                while ((line = br.readLine()) != null)
                {
                    lines.add(line);
                }
            }
            catch(IOException ex)
            {
                this.context.getPlugin().getLogger().log(Level.WARNING, "Unable to open message {0}", file);
                this.context.getPlugin().getLogger().log(Level.WARNING, ex.getMessage());
                continue;
            }

            int msgId = Integer.valueOf(file.replace(".txt", ""));

            if (msgId > biggestMessageId)
                biggestMessageId = msgId;

            String user = lines.get(0);
            String timeStamp = lines.get(1);
            MessagePriority priority = MessagePriority.valueOf(lines.get(2));

            StringBuilder message = new StringBuilder();

            for (int i = 3; i < lines.size(); i++)
            {
                message.append(lines.get(i));
            }

            String parsedMessage = message.toString().replace("\n", " ");

            WebOpMessage newMessage = new WebOpMessage(msgId, user, timeStamp, priority, parsedMessage);
            loadedMessages.add(newMessage);
        }

        return loadedMessages;
    }
    
    public boolean deleteMessage(int messageId)
    {
        Iterator<WebOpMessage> iterator = this.messages.iterator();

        while (iterator.hasNext())
        {
            WebOpMessage msg = iterator.next();

            if (msg.getId() == messageId)
            {
                new File(messageFolder + File.separator + msg.getId() + ".txt").delete();
                iterator.remove();

                return true;
            }
        }

        return false;
    }

    public WebOpMessage createMessage(String user, MessagePriority priority, String message)
    {
        biggestMessageId++;

        int newId = biggestMessageId;
        String currentStamp = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss").format(new Date());

        File msgFile = new File(messageFolder + File.separator + newId + ".txt");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(msgFile, true)))
        {
            bw.write(user);
            bw.newLine();

            bw.write(currentStamp);
            bw.newLine();

            bw.write(priority.toString());
            bw.newLine();

            bw.write(message);

            bw.close();
        }
        catch (IOException ex)
        {

        }

        WebOpMessage newMessage = new WebOpMessage(newId, user, currentStamp, priority, message);
        messages.add(newMessage);
        
        return newMessage;
    }

    public WebOpMessage getMessage(int id)
    {
        for (int i = 0; i < messages.size(); i++)
        {
            if (messages.get(i).getId() == id)
                return messages.get(i);
        }

        return null;
    }

    public List<WebOpMessage> getMessages() 
    { 
        return this.messages; 
    }

    public String createWebSocketString(WebOpMessage message)
    {
        StringBuilder response = new StringBuilder()
                .append("id=").append(message.getId()).append(";")
                .append("user=").append(message.getUser()).append(";")
                .append("time=").append(message.getTimeStamp()).append(";")
                .append("priority=").append(message.getPriority().name()).append(";")
                .append("message=").append(message.getMessage());
        
        return response.toString();
    }
    
}
