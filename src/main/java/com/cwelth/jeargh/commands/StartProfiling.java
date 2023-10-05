package com.cwelth.jeargh.commands;

import com.cwelth.jeargh.JEARGH;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.sun.jdi.connect.Connector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class StartProfiling {
    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("start")
                .then(Commands.argument("radius", IntegerArgumentType.integer(5))
                    .then(Commands.literal("merge")
                        .executes( cs -> {
                            JEARGH.profiler.start(cs.getSource().getPlayer(), IntegerArgumentType.getInteger(cs, "radius"), true);
                            return 0;
                        }))
                    .executes( cs -> {
                        JEARGH.profiler.start(cs.getSource().getPlayer(), IntegerArgumentType.getInteger(cs, "radius"), false);
                        return 0;
                    }))
                ;

    }
}
