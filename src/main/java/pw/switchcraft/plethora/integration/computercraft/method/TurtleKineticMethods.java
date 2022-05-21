package pw.switchcraft.plethora.integration.computercraft.method;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import pw.switchcraft.plethora.api.IPlayerOwnable;
import pw.switchcraft.plethora.api.method.ContextKeys;
import pw.switchcraft.plethora.api.method.FutureMethodResult;
import pw.switchcraft.plethora.api.method.IContext;
import pw.switchcraft.plethora.api.method.IUnbakedContext;
import pw.switchcraft.plethora.api.module.IModuleContainer;
import pw.switchcraft.plethora.api.module.SubtargetedModuleMethod;
import pw.switchcraft.plethora.gameplay.PlethoraFakePlayer;
import pw.switchcraft.plethora.gameplay.modules.kinetic.KineticModuleItem;
import pw.switchcraft.plethora.integration.PlayerInteractionHelpers;
import pw.switchcraft.plethora.integration.computercraft.TurtleFakePlayerProvider;
import pw.switchcraft.plethora.util.PlayerHelpers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TurtleKineticMethods {
    public static final SubtargetedModuleMethod<ITurtleAccess> USE = SubtargetedModuleMethod.of(
        "use", KineticModuleItem.MODULE_ID, ITurtleAccess.class,
        "function([duration:integer]):boolean, string|nil -- Right click with this item. The duration is in ticks, " +
        "or 1/20th of a second.",
        TurtleKineticMethods::use
    );
    private static FutureMethodResult use(@Nonnull IUnbakedContext<IModuleContainer> unbaked,
                                          @Nonnull IArguments args) throws LuaException {
        TurtleKineticMethodContext ctx = getContext(unbaked);
        ITurtleAccess turtle = ctx.turtle();
        IPlayerOwnable ownable = ctx.ownable();
        PlethoraFakePlayer fakePlayer = TurtleFakePlayerProvider.getPlayer(turtle, ownable);

        int duration = args.optInt(0, 0);

        // Sync the turtle's inventory with the fake player
        TurtleFakePlayerProvider.load(fakePlayer, turtle, turtle.getDirection());

        try {
            HitResult hit = PlayerHelpers.raycast(fakePlayer, 1.5f);
            return PlayerInteractionHelpers.use(fakePlayer, hit, Hand.MAIN_HAND, duration);
        } finally {
            TurtleFakePlayerProvider.unload(fakePlayer, turtle);
            fakePlayer.updateCooldown();
        }
    }

    public static final SubtargetedModuleMethod<ITurtleAccess> SWING = SubtargetedModuleMethod.of(
        "swing", KineticModuleItem.MODULE_ID, ITurtleAccess.class,
        "function():boolean, string|nil -- Left click with this item. Returns the action taken.",
        TurtleKineticMethods::swing
    );
    private static FutureMethodResult swing(@Nonnull IUnbakedContext<IModuleContainer> unbaked,
                                            @Nonnull IArguments args) throws LuaException {
        TurtleKineticMethodContext ctx = getContext(unbaked);
        ITurtleAccess turtle = ctx.turtle();
        IPlayerOwnable ownable = ctx.ownable();
        PlethoraFakePlayer fakePlayer = TurtleFakePlayerProvider.getPlayer(turtle, ownable);

        // Sync the turtle's inventory with the fake player
        TurtleFakePlayerProvider.load(fakePlayer, turtle, turtle.getDirection());

        try {
            HitResult baseHit = PlayerHelpers.raycast(fakePlayer, 1.5f);

            switch (baseHit.getType()) {
                case ENTITY -> {
                    EntityHitResult hit = (EntityHitResult) baseHit;
                    Pair<Boolean, String> result = PlayerInteractionHelpers.attack(fakePlayer, hit.getEntity());
                    return FutureMethodResult.result(result.getLeft(), result.getRight());
                }
                case BLOCK -> {
                    BlockHitResult hit = (BlockHitResult) baseHit;
                    Pair<Boolean, String> result = fakePlayer.dig(hit.getBlockPos(), hit.getSide());
                    return FutureMethodResult.result(result.getLeft(), result.getRight());
                }
                default -> {
                    return FutureMethodResult.result(false, "Nothing to do here");
                }
            }
        } finally {
            fakePlayer.clearActiveItem();

            TurtleFakePlayerProvider.unload(fakePlayer, turtle);
            fakePlayer.updateCooldown();
        }
    }

    public record TurtleKineticMethodContext(IContext<IModuleContainer> context, ITurtleAccess turtle,
                                             @Nullable IPlayerOwnable ownable) {}
    public static TurtleKineticMethodContext getContext(@Nonnull IUnbakedContext<IModuleContainer> unbaked) throws LuaException {
        IContext<IModuleContainer> context = unbaked.bake();
        ITurtleAccess turtle = context.getContext(ContextKeys.ORIGIN, ITurtleAccess.class);
        IPlayerOwnable ownable = context.getContext(ContextKeys.ORIGIN, IPlayerOwnable.class);
        return new TurtleKineticMethodContext(context, turtle, ownable);
    }
}
