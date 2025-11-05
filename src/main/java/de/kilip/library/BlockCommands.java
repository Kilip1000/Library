package de.kilip.library;

import java.math.BigInteger;
import java.util.*;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import static de.kilip.library.Library.playerPositionMap;
import static net.minecraft.server.command.CommandManager.*;
import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

import static de.kilip.library.Library.LOGGER;

public class BlockCommands {


    public static List<BlockState> allStates = getAllBlockStates();
    public static final int cubeSize = 16;

    public static List<BlockState> getAllBlockStates() {
        List<BlockState> allStates = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            allStates.addAll(block.getStateManager().getStates());
        }
        return allStates;
    }


    public static void place(BigInteger seed, BlockPos targetPos, ServerWorld world, ServerCommandSource source){
        place(seed, targetPos.getX(), targetPos.getY(), targetPos.getZ(), world, source);
    }


    public static void place(BigInteger seed, int px, int py, int pz, ServerWorld world, ServerCommandSource source) {
        int blockCount = allStates.size();
        int placed = 0;

        for (int dx = 0; dx < cubeSize; dx++) {
            for (int dz = 0; dz < cubeSize; dz++) {
                for (int dy = 0; dy < cubeSize; dy++) {
                    int idx = seed.mod(BigInteger.valueOf(blockCount)).intValue();
                    BlockState state = allStates.get(idx);

                    int finalX = px + dx;
                    int finalY = py - dy;
                    int finalZ = pz + dz;

                    int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS; //like in strict fill command

                    world.setBlockState(new BlockPos(finalX, finalY, finalZ), state, flags);
                    placed++;
                    seed = seed.divide(BigInteger.valueOf(blockCount));
                }
            }
        }

        int finalPlaced = placed;
        source.sendFeedback(() ->
                        Text.of("Placed " + finalPlaced + " blocks at " + px + "," + py + "," + pz),
                false
        );

    }
    public static BigInteger reconstructSeed(ServerWorld world,
                                      int px1, int py1, int pz1,
                                      int px2, int py2, int pz2) {
        BigInteger seed = BigInteger.ZERO;
        BigInteger multiplier = BigInteger.ONE;
        int blockCount = allStates.size();

        //any corner order
        int minX = Math.min(px1, px2);
        int maxX = Math.max(px1, px2);
        int minY = Math.min(py1, py2);
        int maxY = Math.max(py1, py2);
        int minZ = Math.min(pz1, pz2);
        int maxZ = Math.max(pz1, pz2);


        for (int x = maxX; x >= minX; x--) {
            for (int z = maxZ; z >= minZ; z--) {
                for (int y = maxY; y >= minY; y--) {

                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    int idx = allStates.indexOf(state);
                    if (idx < 0) continue;

                    seed = seed.add(multiplier.multiply(BigInteger.valueOf(idx)));
                    multiplier = multiplier.multiply(BigInteger.valueOf(blockCount));

                }
            }
        }

        return seed;
    }




    public static void registerPpfind(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("pfind")
                            .then(argument("pos1", blockPos())
                            .then(argument("pos2", blockPos())
                            .then(argument("player", EntityArgumentType.player())
                            .then(argument("index", integer())
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                ServerPlayerEntity player = source.getPlayerOrThrow();
                                ServerWorld world = player.getEntityWorld();

                                ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                BlockPos pos1 = getBlockPos(context, "pos1");
                                BlockPos pos2 = getBlockPos(context, "pos2");

                                int index = getInteger(context,"index");

                                int px1 = pos1.getX();
                                int py1 = pos1.getY();
                                int pz1 = pos1.getZ();

                                int px2 = pos2.getX();
                                int py2 = pos2.getY();
                                int pz2 = pos2.getZ();

                                BigInteger reconstructed = reconstructSeed(world, px1, py1, pz1, px2, py2, pz2);

                                source.sendFeedback(() -> Text.of("Reconstructed seed"), false);


                                Library.PlayerPosKey key = new Library.PlayerPosKey(targetPlayer.getUuid(), index);
                                playerPositionMap.put(key, reconstructed);

                                return 1;
                            })
                    )))));
        });
    }
    public static void registerPplace(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("pplace")
                            .then(argument("player", EntityArgumentType.player()) // require a player
                                    .then(argument("index", integer())
                                            .then(argument("dimension", DimensionArgumentType.dimension())
                                            .then(argument("target pos", blockPos())
                                                    .executes(context -> {
                                                        ServerCommandSource source = context.getSource();

                                                        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                                        ServerWorld world = DimensionArgumentType.getDimensionArgument(context, "dimension");

                                                        int index = getInteger(context, "index");
                                                        BlockPos target_pos = getBlockPos(context, "target pos");

                                                        Library.PlayerPosKey key = new Library.PlayerPosKey(targetPlayer.getUuid(), index);

                                                        BigInteger seed = playerPositionMap.get(key);
                                                        if (seed == null) {
                                                            source.sendError(Text.of("No stored seed found for player " + targetPlayer.getName() + " at map position " + index));
                                                            return 0;
                                                        }

                                                        try {
                                                            place(seed, target_pos, world, source);
                                                        } catch (Exception e) {
                                                            LOGGER.error("Error while placing seed for pplace", e);
                                                            source.sendError(Text.of("An error occurred while placing the seed: " + e.getMessage()));
                                                            return 0;
                                                        }

                                                        return 1;
                                                    })))))
            );
        });
    }


    public static void registerCheckstate(){

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("checkstate")
                            .then(argument("pos", blockPos())
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        ServerPlayerEntity player = source.getPlayerOrThrow();
                                        ServerWorld world = player.getEntityWorld();

                                        BlockPos pos = getBlockPos(context, "pos");

                                        source.sendFeedback(() -> Text.of(world.getBlockState(pos).toString()), false);

                                        return 1;
                                    })
                            )
            );
        });
    }

    public static void registerCommands(){
        registerPpfind();
        registerPplace();
        registerCheckstate();
    }

}
