package gigaherz.eyes;

import com.google.common.collect.Sets;
import gigaherz.eyes.entity.EntityEyes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
@Mod(modid = EyesInTheDarkness.MODID, version = EyesInTheDarkness.VERSION)
public class EyesInTheDarkness
{
    public static final String MODID = "eyesinthedarkness";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @GameRegistry.ObjectHolder(MODID + ":eyes_laugh")
    public static SoundEvent eyes_laugh;

    @GameRegistry.ObjectHolder(MODID + ":eyes_disappear")
    public static SoundEvent eyes_disappear;

    @GameRegistry.ObjectHolder(MODID + ":eyes_jumpscare")
    public static SoundEvent eyes_jumpscare;

    private static final String CHANNEL=MODID;
    public static SimpleNetworkWrapper channel;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        registerNetwork();
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event)
    {
        event.getRegistry().registerAll(
                new SoundEvent(location("mob.eyes.laugh")).setRegistryName(location("eyes_laugh")),
                new SoundEvent(location("mob.eyes.disappear")).setRegistryName(location("eyes_disappear")),
                new SoundEvent(location("mob.eyes.jumpscare")).setRegistryName(location("eyes_jumpscare"))
        );
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event)
    {
        int entityId = 1;

        EntityEntryBuilder<Entity> builder = EntityEntryBuilder.create().name("eyes")
                .id(location("eyes"), entityId++)
                .entity(EntityEyes.class).factory(EntityEyes::new)
                .tracker(80, 3, true)
                .egg(0x000000, 0x7F0000);

        if(ConfigData.EnableNaturalSpawn)
        {
            int currentWeight = ConfigData.OverrideWeight;

            if(currentWeight < 0)
            {
                int daysBefore = getDaysUntilNextHalloween();

                int weightMin = 15;
                int weightMax = 150;

                currentWeight = weightMin + ((weightMax - weightMin) * (30 - daysBefore)) / 30;
            }

            if (currentWeight > 0)
            {
                Collection<Biome> biomes = ForgeRegistries.BIOMES.getValuesCollection();

                if (ConfigData.BiomeWhitelist != null && ConfigData.BiomeWhitelist.length > 0)
                {
                    Set<String> whitelist = Sets.newHashSet(ConfigData.BiomeWhitelist);
                    biomes = biomes.stream().filter(b -> whitelist.contains(b.getRegistryName().toString())).collect(Collectors.toList());
                }
                else if (ConfigData.BiomeBlacklist != null && ConfigData.BiomeBlacklist.length > 0)
                {
                    Set<String> blacklist = Sets.newHashSet(ConfigData.BiomeBlacklist);
                    biomes = biomes.stream().filter(b -> !blacklist.contains(b.getRegistryName().toString())).collect(Collectors.toList());
                }

                builder = builder.spawn(EnumCreatureType.MONSTER, currentWeight,
                        ConfigData.MinimumPackSize, ConfigData.MaximumPackSize,
                        biomes);
            }
        }

        event.getRegistry().registerAll(
                builder
                        .build()
        );
        LOGGER.debug("Next entity id: " + entityId);

        // TODO: Evil eyes: [03:26] ProxyNeko: Add red eyes to your spooky mod and make those ones attack you if you don't look at them for more than 5 or so seconds @gigaherz xD
    }

    private static int getDaysUntilNextHalloween()
    {
        Calendar now = Calendar.getInstance();
        Calendar nextHalloween = new Calendar.Builder()
                .setDate(now.get(Calendar.YEAR), 9, 31)
                .setTimeOfDay(23,59,59,999).build();
        if (now.after(nextHalloween))
        {
            nextHalloween.add(Calendar.YEAR, 1);
        }
        return (int)Math.min(ChronoUnit.DAYS.between(now.toInstant(), nextHalloween.toInstant()), 30);
    }

    private void registerNetwork()
    {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);

        int messageNumber = 0;
        channel.registerMessage(InitiateJumpscare.Handler.class, InitiateJumpscare.class, messageNumber++, Side.CLIENT);
        LOGGER.debug("Final message number: " + messageNumber);
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void entityInit(EntityJoinWorldEvent event)
    {
        Entity e = event.getEntity();
        if (e instanceof EntityWolf)
        {
            EntityWolf wolf = (EntityWolf)e;
            wolf.targetTasks.addTask(5,
                    new EntityAINearestAttackableTarget(wolf, EntityEyes.class, false));
        }
        if (e instanceof EntityOcelot)
        {
            EntityOcelot cat = (EntityOcelot)e;
            cat.tasks.addTask(3, new EntityAIAvoidEntity(cat, EntityEyes.class, 6.0F, 1.0D, 1.2D));
        }
    }

    public static ResourceLocation location(String location)
    {
        return new ResourceLocation(MODID, location);
    }
}
