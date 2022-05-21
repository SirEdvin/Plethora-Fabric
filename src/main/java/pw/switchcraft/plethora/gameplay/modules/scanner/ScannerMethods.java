package pw.switchcraft.plethora.gameplay.modules.scanner;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import pw.switchcraft.plethora.api.IWorldLocation;
import pw.switchcraft.plethora.api.WorldLocation;
import pw.switchcraft.plethora.api.method.*;
import pw.switchcraft.plethora.api.module.IModuleContainer;
import pw.switchcraft.plethora.api.module.SubtargetedModuleMethod;
import pw.switchcraft.plethora.api.reference.BlockReference;
import pw.switchcraft.plethora.gameplay.modules.RangeInfo;
import pw.switchcraft.plethora.integration.vanilla.meta.block.BlockStateMeta;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScannerMethods {
    private static final Identifier MODULE_ID = ScannerModuleItem.MODULE_ID;

    public static final SubtargetedModuleMethod<IWorldLocation> SCAN = SubtargetedModuleMethod.of(
        "scan", MODULE_ID, IWorldLocation.class,
        "function():table -- Scan all blocks in the vicinity",
        ScannerMethods::scan
    );
    private static FutureMethodResult scan(@Nonnull IUnbakedContext<IModuleContainer> unbaked,
                                           @Nonnull IArguments args) throws LuaException {
        ScannerMethodContext ctx = getContext(unbaked);
        World world = ctx.loc.getWorld();
        BlockPos pos = ctx.loc.getPos();
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        return ctx.context.getCostHandler().await(ctx.range.getBulkCost(), () ->
            FutureMethodResult.result(scan(world, x, y, z, ctx.range.getRange())));
    }

    private static List<Map<String, ?>> scan(World world, int x, int y, int z, int radius) {
        List<Map<String, ?>> result = new ArrayList<>();
        for (int oX = x - radius; oX <= x + radius; oX++) {
            for (int oY = y - radius; oY <= y + radius; oY++) {
                for (int oZ = z - radius; oZ <= z + radius; oZ++) {
                    BlockPos subPos = new BlockPos(oX, oY, oZ);
                    BlockState block = world.getBlockState(subPos);

                    HashMap<String, Object> data = new HashMap<>(6);
                    data.put("x", oX - x);
                    data.put("y", oY - y);
                    data.put("z", oZ - z);

                    Identifier name = Registry.BLOCK.getId(block.getBlock());
                    data.put("name", name.toString());

                    BlockStateMeta.fillBasicMeta(data, block);

                    result.add(data);
                }
            }
        }

        return result;
    }

    public static final SubtargetedModuleMethod<IWorldLocation> GET_BLOCK_META = SubtargetedModuleMethod.of(
        "getBlockMeta", MODULE_ID, IWorldLocation.class,
        "function(x:integer, y:integer, z:integer):table|nil -- -- Get metadata about a nearby block",
        ScannerMethods::getBlockMeta
    );
    private static FutureMethodResult getBlockMeta(@Nonnull IUnbakedContext<IModuleContainer> unbaked,
                                                   @Nonnull IArguments args) throws LuaException {
        ScannerMethodContext ctx = getContext(unbaked);
        int radius = ctx.range.getRange();

        int x = args.getInt(0);
        int y = args.getInt(1);
        int z = args.getInt(2);
        ArgumentHelper.assertBetween(x, -radius, radius, "X coordinate out of bounds (%s)");
        ArgumentHelper.assertBetween(y, -radius, radius, "Y coordinate out of bounds (%s)");
        ArgumentHelper.assertBetween(z, -radius, radius, "Z coordinate out of bounds (%s)");

        return FutureMethodResult.result(ctx.context
            .makeChild(new BlockReference(new WorldLocation(ctx.loc.getWorld(), ctx.loc.getPos().add(x, y, z))))
            .getMeta());
    }

    private record ScannerMethodContext(IContext<IModuleContainer> context, IWorldLocation loc, RangeInfo range) {}
    private static ScannerMethodContext getContext(@Nonnull IUnbakedContext<IModuleContainer> unbaked) throws LuaException {
        IContext<IModuleContainer> context = unbaked.bake();
        IWorldLocation location = context.getContext(ContextKeys.ORIGIN, IWorldLocation.class);
        RangeInfo range = context.getContext(ScannerModuleItem.MODULE_ID, RangeInfo.class);
        return new ScannerMethodContext(context, location, range);
    }
}
