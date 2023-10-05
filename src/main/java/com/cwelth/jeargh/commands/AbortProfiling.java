package com.cwelth.jeargh.commands;

import com.cwelth.jeargh.JEARGH;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class AbortProfiling {
    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("abort")
            .executes( cs -> {
                JEARGH.profiler.abort();
                return 0;
            });
    }
}
