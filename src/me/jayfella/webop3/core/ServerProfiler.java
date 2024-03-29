package me.jayfella.webop3.core;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import me.jayfella.webop3.PluginContext;
import me.jayfella.webop3.serverprofiler.WebOpPluginManager;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;

public class ServerProfiler
{
    private final PluginContext context;
    private SimplePluginManager simplePluginManager;
    private WebOpPluginManager pluginManager;

    private PluginManager oldPluginManager;

    public ServerProfiler(PluginContext context)
    {
        this.context = context;
        this.oldPluginManager = this.context.getPlugin().getServer().getPluginManager();
    }

    private void initializeEventProfiler() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException
    {
        simplePluginManager = (SimplePluginManager)this.context.getPlugin().getServer().getPluginManager();
        Field cM = SimplePluginManager.class.getDeclaredField("commandMap");
        cM.setAccessible(true);
        this.pluginManager = new WebOpPluginManager(this.context.getPlugin().getServer(), (SimpleCommandMap)cM.get(simplePluginManager));

        for (Field f : simplePluginManager.getClass().getDeclaredFields())
        {
            boolean orig_simp_ia = f.isAccessible();
            f.setAccessible(true);

            Field smF = this.pluginManager.getClass().getField(f.getName());
            boolean orig_smart_ia = smF.isAccessible();
            smF.setAccessible(true);

            smF.set(this.pluginManager, f.get(simplePluginManager));

            f.setAccessible(orig_simp_ia);
            f.setAccessible(orig_smart_ia);
        }

        Field plugManager = this.context.getPlugin().getServer().getClass().getDeclaredField("pluginManager");
        plugManager.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(plugManager, plugManager.getModifiers() & 0xFFFFFFEF);

        plugManager.set(this.context.getPlugin().getServer(), this.pluginManager);
    }
    
    private void restoreEventProfiler() throws NoSuchFieldException, IllegalAccessException
    {
        Field plugManager = this.context.getPlugin().getServer().getClass().getDeclaredField("pluginManager");
        plugManager.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(plugManager, plugManager.getModifiers() & 0xFFFFFFEF);

        plugManager.set(this.context.getPlugin().getServer(), this.oldPluginManager);
    }

    public boolean isProfiling()
    {
        if (pluginManager == null)
            return false;
        
        return pluginManager.isProfiling();
    }
    
    public void startProfiling()
    {
        if (isProfiling())
            return;
        
        try
        {
            initializeEventProfiler();
            this.pluginManager.startProfiling(600000); // max time, ten minutes
        }
        catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException ex)
        {
            context.getPlugin().getLogger().log(Level.SEVERE, "Server Profiling Error (start): ", ex);
        }
        
    }

    public void stopProfiling()
    {
        if (!isProfiling())
            return;
        
        try
        {
            this.pluginManager.stopProfiling();
            this.restoreEventProfiler();
        }
        catch (NoSuchFieldException | IllegalAccessException ex)
        {
            context.getPlugin().getLogger().log(Level.SEVERE, "Server Profiling Error (stop): ", ex);
        }
    }

    public String buildEventProfileResultRaw()
    {
        // Class<? extends Event>[] ranEvents = (Class<? extends Event>[])pluginManager.getEventDuration().keySet().toArray();
        
        List<Class<? extends Event>> ranEvents = new ArrayList<>();
        
        for (Class<? extends Event> key : pluginManager.getEventDuration().keySet())
            ranEvents.add(key);
        
        if (ranEvents.isEmpty())
        {
            return "NO_EVENTS";
        }
                
        List<String> eventData = new ArrayList<>();
        List<Long> countData = new ArrayList<>();
        List<Long> durationData = new ArrayList<>();
        
        for (int i = 0; i < ranEvents.size(); i++)
        {
            Class<? extends Event> event = ranEvents.get(i);
            
            String eventName = event.getName();
            long runCount = pluginManager.getEventRunCount().get(event);
            long runDuration = pluginManager.getEventDuration().get(event);
            
            eventData.add(eventName);
            countData.add(runCount);
            durationData.add(runDuration);
        }

        StringBuilder response = new StringBuilder();
        
        response.append("eventNames=");
        for (int i = 0; i < eventData.size(); i++)
        {
            response.append(eventData.get(i));
            
            if (i < eventData.size() - 1)
                response.append(",");
            else
                response.append("\n");
        }
            
        response.append("eventCounts=");
        for (int i = 0; i < countData.size(); i++)
        {
            response.append(countData.get(i));
            
            if (i < countData.size() - 1)
                response.append(",");
            else
                response.append("\n");
        }
        
        response.append("eventDurations=");
        for (int i = 0; i < durationData.size(); i++)
        {
            BigDecimal num1 = new BigDecimal(durationData.get(i));
            BigDecimal num2 = new BigDecimal(1000000L);

            String timeRunningMillis = new DecimalFormat("0.00").format(num1.divide(num2, 100, RoundingMode.HALF_UP));
            
            response.append(timeRunningMillis);
            
            if (i < durationData.size() - 1)
                response.append(",");
            else
                response.append("\n");
        }
        
        return response.toString();
    }

    /* public String buildEventProfileResultRaw()
    {
        StringBuilder sb = new StringBuilder();

        // List<EventProfile> profiles = new CopyOnWriteArrayList(pluginManager.getEventProfiles());

        Map<Class<? extends Event>, Long> durations = new HashMap<>(pluginManager.getEventDuration());
        Map<Class<? extends Event>, Long> counters = new HashMap<>(pluginManager.getEventRunCount());


        for (Map.Entry<Class<? extends Event>, Long> entry : durations.entrySet())
        {


            sb.append(entry.getKey().getName())
                    .append(">").append(counters.get(entry.getKey()))
                    .append(">").append(entry.getValue())
                    .append(";");
        }

        return sb.toString();
    }

    public String buildEventProfileResultHtml()
    {
        StringBuilder sb = new StringBuilder();

        // List<EventProfile> profiles = new CopyOnWriteArrayList(pluginManager.getEventProfiles());

        Map<Class<? extends Event>, Long> durations = new HashMap<>(pluginManager.getEventDuration());
        Map<Class<? extends Event>, Long> counters = new HashMap<>(pluginManager.getEventRunCount());


        for (Map.Entry<Class<? extends Event>, Long> entry : durations.entrySet())
        {
            sb.append("<span style='font-weight: bold;'>").append(entry.getKey().getName()).append(":</span>&nbsp;");

            String timeColor;

            long averageTime = entry.getValue();

            if (averageTime < 10000000)
                timeColor = "darkgreen";
            else if (averageTime < 20000000)
                timeColor = "darkorange";
            else
                timeColor = "darkred";

            BigDecimal num1 = new BigDecimal(averageTime);
            BigDecimal num2 = new BigDecimal(1000000L);

            String timeRunningMillis = new DecimalFormat("0.00").format(num1.divide(num2, 100, RoundingMode.HALF_UP));

            sb.append("took an average of ").append("<span style='font-weight: bold; color:").append(timeColor).append("'>").append(timeRunningMillis).append("ms</span>&nbsp;");

            sb.append("over ").append(counters.get(entry.getKey())).append(" call(s).");
            sb.append("<br/>");
            /* sb.append(entry.getKey().getName())
                    .append(">").append(counters.get(entry.getKey()))
                    .append(">").append(entry.getValue())
                    .append(";");
        }

        if (sb.toString().length() < 1)
        {
            return "No events are taking longer than 0.01 milliseconds. Nothing to report!";
        }

        return sb.toString();
    }*/

    public String buildRegisteredListenerResult()
    {
        StringBuilder sb = new StringBuilder();

        // List<EventProfile> profiles = new CopyOnWriteArrayList(pluginManager.getEventProfiles());

        Map<RegisteredListener, Long> durations = new HashMap<>(pluginManager.getRegisteredListenerDuration());
        Map<RegisteredListener, Long> counters = new HashMap<>(pluginManager.getRegisteredListenerRunCount());


        // Iterator<Class<? extends Event>, Long> iterator = profiles.entrySet().iterator();

        // for (EventProfile profile : profiles)

        for (Map.Entry<RegisteredListener, Long> entry : durations.entrySet())
        {
            sb.append(entry.getKey().getClass().getName())
                    .append(">").append(counters.get(entry.getKey()))
                    .append(">").append(entry.getValue())
                    .append(";");
        }

        return sb.toString();
    }

    public void clearProfilingResults() { this.pluginManager.clearProfilingResults();};

    /* public List<RunningTask> getScheduledTasks()
    {
        List<RunningTask> res = new ArrayList<>();

        try
        {
            Field ctList = CraftScheduler.class.getDeclaredField("head");
            ctList.setAccessible(true);

            Object headObject = ctList.get((CraftScheduler) this.context.getPlugin().getServer().getScheduler());
            Class headObjectClass = headObject.getClass();

            Field runners = headObjectClass.getDeclaredField("runners");
            runners.setAccessible(true);

            Map<Integer, Object> output = (ConcurrentHashMap<Integer, Object>)runners.get(headObject);

            for (Map.Entry pairs : output.entrySet())
            {
                Object outputVal = pairs.getValue();

                Class valueClass = outputVal.getClass();
                Class valueSuperClass = valueClass.getSuperclass();

                Field objPluginNameField;
                Field objClassNameField;
                Field objTaskIdField;
                Field objRecurringPeriodField;

                boolean isAsync = false;

                try // async tasks
                {
                    objPluginNameField = valueSuperClass.getDeclaredField("plugin");
                    objClassNameField = valueSuperClass.getDeclaredField("task");
                    objTaskIdField = valueSuperClass.getDeclaredField("id");
                    objRecurringPeriodField = valueSuperClass.getDeclaredField("period");

                    isAsync = true;
                }
                catch (NoSuchFieldException | SecurityException ex) // regular tasks
                {
                    objPluginNameField = valueClass.getDeclaredField("plugin");
                    objClassNameField = valueClass.getDeclaredField("task");
                    objTaskIdField = valueClass.getDeclaredField("id");
                    objRecurringPeriodField = valueClass.getDeclaredField("period");
                }

                objPluginNameField.setAccessible(true);
                objClassNameField.setAccessible(true);
                objTaskIdField.setAccessible(true);
                objRecurringPeriodField.setAccessible(true);

                Object objPluginName = objPluginNameField.get(outputVal);
                String pluginName = ((Plugin)objPluginName).getName();

                Object objClassName = objClassNameField.get(outputVal);
                String className = objClassName.toString().substring(0, objClassName.toString().indexOf("@"));

                Object objTaskId = objTaskIdField.get(outputVal);
                String taskId = objTaskId.toString();

                Object objRecurringPeriod = objRecurringPeriodField.get(outputVal);
                String recurringPeriod = objRecurringPeriod.toString();

                res.add(new RunningTask(pluginName, className, Integer.valueOf(taskId), Long.valueOf(recurringPeriod), isAsync));

            }
        }
        catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex)
        {

        }

        Collections.sort(res);
        return res;
    }*/

    
}
