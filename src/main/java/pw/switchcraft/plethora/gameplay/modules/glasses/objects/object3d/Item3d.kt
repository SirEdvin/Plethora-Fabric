package pw.switchcraft.plethora.gameplay.modules.glasses.objects.object3d

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE
import net.minecraft.client.render.OverlayTexture.DEFAULT_UV
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.model.json.ModelTransformation.Mode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3f
import net.minecraft.util.registry.Registry
import pw.switchcraft.plethora.gameplay.modules.glasses.canvas.CanvasClient
import pw.switchcraft.plethora.gameplay.modules.glasses.objects.BaseObject
import pw.switchcraft.plethora.gameplay.modules.glasses.objects.ItemObject
import pw.switchcraft.plethora.gameplay.modules.glasses.objects.ObjectRegistry.ITEM_3D
import pw.switchcraft.plethora.gameplay.modules.glasses.objects.Scalable
import pw.switchcraft.plethora.util.ByteBufUtils

class Item3d(
  id: Int,
  parent: Int
) : BaseObject(id, parent, ITEM_3D), Scalable, Positionable3d, DepthTestable, ItemObject, Rotatable3d {
  override var position: Vec3d = Vec3d.ZERO
    set(value) { if (field != value) field = value.also { setDirty() } }
  override var rotation: Vec3d? = Vec3d.ZERO
    set(value) { if (field != value) field = value.also { setDirty() } }

  // TODO: Turn these into property overrides when the superclasses are converted to Kotlin
  private var scale: Float = 1f
  override fun getScale(): Float = scale
  override fun setScale(scale: Float) {
    if (this.scale != scale) this.scale = scale.also { setDirty() }
  }

  private var stack: ItemStack? = null
  private var item: Item = Items.STONE

  override fun getItem(): Item = item
  override fun setItem(item: Item) {
    if (this.item != item) {
      this.item = item
      stack = null
      setDirty()
    }
  }

  // TODO: Damage?

  override var hasDepthTest = true
    set(value) { if (field != value) field = value.also { setDirty() } }

  override fun readInitial(buf: PacketByteBuf) {
    position = ByteBufUtils.readVec3d(buf)
    rotation = ByteBufUtils.readOptVec3d(buf)
    scale = buf.readFloat()

    val name = Identifier(buf.readString())
    item = Registry.ITEM[name]

    hasDepthTest = buf.readBoolean()
  }

  override fun writeInitial(buf: PacketByteBuf) {
    ByteBufUtils.writeVec3d(buf, position)
    ByteBufUtils.writeOptVec3d(buf, rotation)
    buf.writeFloat(scale)
    buf.writeString(Registry.ITEM.getId(item).toString())
    buf.writeBoolean(hasDepthTest)
  }

  override fun draw(
    canvas: CanvasClient,
    matrices: MatrixStack,
    consumers: VertexConsumerProvider?
  ) {
    val mc = MinecraftClient.getInstance()
    val itemRenderer = mc.itemRenderer

    matrices.push()

    matrices.translate(position.x, position.y, position.z)
    matrices.scale(scale, scale, scale)

    val rot = rotation
    if (rot == null) {
      val cam = mc.gameRenderer.camera
      matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180 - cam.yaw))
      matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-cam.pitch))
    } else {
      matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rot.x.toFloat()))
      matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rot.y.toFloat()))
      matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rot.z.toFloat()))
    }

    RenderSystem.enableTexture()

    if (hasDepthTest) {
      RenderSystem.enableDepthTest()
    } else {
      RenderSystem.disableDepthTest()
    }

    val stack = stack ?: ItemStack(item).also { stack = it }
    itemRenderer.renderItem(stack, Mode.NONE, MAX_LIGHT_COORDINATE, DEFAULT_UV, matrices, consumers, 0)

    matrices.pop()
  }
}