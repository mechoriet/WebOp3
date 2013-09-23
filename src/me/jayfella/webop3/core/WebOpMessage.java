package me.jayfella.webop3.core;

public final class WebOpMessage
{
    private final int id;
    private final String user;
    private final String timeStamp;
    private final MessagePriority priority;
    private final String message;

    public WebOpMessage(int id, String user, String timeStamp, MessagePriority priority, String message)
    {
        this.id = id;
        this.user = user;
        this.timeStamp = timeStamp;
        this.priority = priority;
        this.message = message;
    }

    public int getId() { return this.id; }
    public String getUser() { return this.user; }
    public String getTimeStamp () { return this.timeStamp; }
    public MessagePriority getPriority() { return this.priority; }
    public String getMessage() { return this.message; }

}