package me.jayfella.webop3.datastore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import me.jayfella.webop3.WebOp3Plugin;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class EntityMonitor
{
    
    public String findAllHighestEntityCountInChunks(int amount)
    {
        StringBuilder response = new StringBuilder();

        for (World world : WebOp3Plugin.PluginContext.getPlugin().getServer().getWorlds())
        {
            Chunk[] chunksArray = world.getLoadedChunks().clone();

            List<Chunk> chunks = Arrays.asList(chunksArray);
            Collections.sort(chunks, new ChunkComparator());

            response
                    .append("<div class='containerHead'><h2>World: ")
                    .append("<span style='color: darkblue; font-weight: bold;'>")
                    .append(world.getName())
                    .append("</span></h2></div>");

            int maxChunks = 50;

            for (int i = 0; i < chunks.size(); i++)
            {
                if (i >= maxChunks)
                    break;

                Chunk thisChunk = chunks.get(i);

                if (thisChunk.getEntities().length < amount)
                    continue;

                response
                        .append("<div style='background: #DFDFDF; padding: 4px; border-radius: 3px;'>")
                        .append("<button id='").append("'").append("class='btn-green teleportButton' style='margin-right: 5px; padding: 1px !important;'>Teleport</button>")
                        .append("Chunk: ")
                        .append("(X: ").append("<span class='xCoord'>").append(thisChunk.getX() << 4).append("</span>")
                        .append(" - ")
                        .append("Z: ").append("<span class='zCoord'>").append(thisChunk.getZ() << 4).append("</span>")
                        .append(")")
                        .append("<span class='wCoord' style='display: none;'>").append(world.getName()).append("</span>")
                        .append(" Total Entities: ")
                        .append(thisChunk.getEntities().length)
                        .append("<br/>")
                        .append("</div>");

                Map<EntityType, Integer> entityMap = new HashMap<>();

                for (Entity entity : thisChunk.getEntities())
                {
                    EntityType entityType = entity.getType();

                    Integer count = entityMap.get(entityType);

                    if (count == null) count = 0;

                    count++;

                    entityMap.put(entityType, count);
                }

                for (Map.Entry<EntityType, Integer> entry : entityMap.entrySet())
                {
                    response
                            .append(entry.getKey().name())
                            .append(" : ")
                            .append(entry.getValue())
                            .append("<br/>");
                }

                response.append("<br/>");
            }
        }

        return response.toString();
    }
    
    public String findCertainHighestEntityCountInChunks(String entityTypes, int amount)
    {
        StringBuilder response = new StringBuilder();

        if (entityTypes.isEmpty())
        {
            response.append("You must specify at least one entity type!");
            return response.toString();
        }

        EntityType[] parsedEntityTypes = parseGivenEntityTypes(entityTypes);

        for (World world : WebOp3Plugin.PluginContext.getPlugin().getServer().getWorlds())
        {
            Chunk[] chunksArray = world.getLoadedChunks().clone();
            List<Chunk> chunks = Arrays.asList(chunksArray);

            Iterator<Chunk> iterator = chunks.iterator();

            List<Chunk> validChunks = new ArrayList<>();

            while (iterator.hasNext())
            {
                Chunk chunk = iterator.next();
                Map<EntityType, Integer> entityMap = new HashMap<>();

                for (Entity entity : chunk.getEntities())
                {
                    EntityType entityType = entity.getType();

                    Integer count = entityMap.get(entityType);

                    if (count == null) count = 0;

                    count++;

                    entityMap.put(entityType, count);
                }

                boolean isValidChunk = true;

                for (EntityType entity : parsedEntityTypes)
                {
                    Integer entityCount = entityMap.get(entity);

                    if (entityCount == null || entityCount < amount)
                    {
                        isValidChunk = false;
                    }
                }

                if (isValidChunk)
                    validChunks.add(chunk);
            }

            Collections.sort(validChunks, new ChunkComparator());

            response
                    .append("<div class='containerHead'><h2>World: ")
                    .append("<span style='color: darkblue; font-weight: bold;'>")
                    .append(world.getName())
                    .append("</span></h2></div>");

            int maxChunks = 50;

            for (int i = 0; i < validChunks.size(); i++)
            {
                if (i >= maxChunks)
                    break;

                Chunk thisChunk = validChunks.get(i);

                if (thisChunk.getEntities().length < amount)
                    continue;

                response
                        .append("<div style='background: #DFDFDF; padding: 4px; border-radius: 3px;'>")
                        .append("<button id='").append("'").append("class='btn-green teleportButton' style='margin-right: 5px; padding: 1px !important;'>Teleport</button>")
                        .append("Chunk: ")
                        .append("(X: ").append("<span class='X'>").append(thisChunk.getX() << 4).append("</span>")
                        .append(" - ")
                        .append("Z: ").append("<span class='Z'>").append(thisChunk.getZ() << 4).append("</span>")
                        .append(")")
                        .append("<span class='world' style='hidden'>").append(world.getName()).append("</span>")
                        .append(" Total Entities: ")
                        .append(thisChunk.getEntities().length)
                        .append("<br/>")
                        .append("</div>");

                Map<EntityType, Integer> entityMap = new HashMap<>();

                for (Entity entity : thisChunk.getEntities())
                {
                    EntityType entityType = entity.getType();

                    Integer count = entityMap.get(entityType);

                    if (count == null) count = 0;

                    count++;

                    entityMap.put(entityType, count);
                }

                for (Map.Entry<EntityType, Integer> entry : entityMap.entrySet())
                {
                    response
                            .append(entry.getKey().name())
                            .append(" : ")
                            .append(entry.getValue())
                            .append("<br/>");
                }

                response.append("<br/>");
            }
        }

        return response.toString();
    }

    private EntityType getEntityType(String type)
    {
        String correctType = type.toUpperCase().trim().replace(" ", "_");

        for (EntityType c : EntityType.values())
        {
            if (c.name().equals(correctType))
            {
                return c;
            }
        }

        return null;
    }
    
    private EntityType[] parseGivenEntityTypes(String entityTypes)
    {
        String[] givenMobTypes = entityTypes.split(",");
        List<EntityType> parsedEntityTypes = new ArrayList<>();

        for (String str : givenMobTypes)
        {
            if (str.isEmpty())
                continue;

            EntityType type = getEntityType(str);

            if (type != null)
                parsedEntityTypes.add(type);
        }

        return parsedEntityTypes.toArray(new EntityType[parsedEntityTypes.size()]);
    }
    
    private class ChunkComparator implements Comparator<Chunk>
    {
        @Override
        public int compare(Chunk a, Chunk b)
        {
            return a.getEntities().length < b.getEntities().length ? 1 : a.getEntities().length == b.getEntities().length ? 0 : -1;
        }
    }

}
