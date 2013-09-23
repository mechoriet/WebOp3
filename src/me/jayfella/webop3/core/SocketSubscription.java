package me.jayfella.webop3.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class SocketSubscription
{
    private Set<String> subscribers = new HashSet<>();
    
    public void addSubscriber(String playername)
    {
        if (!this.subscribers.contains(playername))
            this.subscribers.add(playername);
    }
    
    public void removeSubscriber(String playername)
    {
        this.subscribers.remove(playername);
    }
    
    public String[] getSubscribers()
    {
        List<String> names = new ArrayList<>();
        
        Iterator<String> iterator = this.subscribers.iterator();
        
        while (iterator.hasNext())
        {
            String name = iterator.next();
            names.add(name);
        }
        
        return names.toArray(new String[names.size()]);
    }
    
    public boolean isSubscriber(String playername)
    {
        return this.subscribers.contains(playername);
    }
    
}
