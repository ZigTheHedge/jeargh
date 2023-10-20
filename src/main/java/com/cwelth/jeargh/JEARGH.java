package com.cwelth.jeargh;

import com.cwelth.jeargh.commands.AbortProfiling;
import com.cwelth.jeargh.commands.FixHeights;
import com.cwelth.jeargh.commands.StartProfiling;
import com.cwelth.jeargh.utils.JEARGHProfiler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(JEARGH.MODID)
public class JEARGH {
    public static final String MODID = "jeargh";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final JEARGHProfiler profiler = new JEARGHProfiler();
    public JEARGH()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

    }

    private void setup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandlers());
    }

    public class ForgeEventHandlers {
        @SubscribeEvent
        public void registerCommands(RegisterCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
            LiteralCommandNode<CommandSourceStack> cmdsJEARGH = dispatcher.register(
                    Commands.literal("jeargh")
                            .then(StartProfiling.register(dispatcher))
                            .then(AbortProfiling.register(dispatcher))
                            .then(FixHeights.register(dispatcher))
            );

            dispatcher.register(Commands.literal(JEARGH.MODID).redirect(cmdsJEARGH));
        }
    }
}
