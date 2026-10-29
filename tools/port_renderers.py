"""Mechanical part of porting the 1.18 entity renderers and layers onto IafMobRenderer / IafRenderLayer.

usage: python -I tools/port_renderers.py <file.java>...
"""
import re
import sys
import pathlib

IMPORTS = {
    "IafMobRenderer": "com.iafabric.client.render.entity.IafMobRenderer",
    "IafRenderLayer": "com.iafabric.client.render.entity.IafRenderLayer",
    "BufferSource": "com.iafabric.client.render.BufferSource",
    "RenderTypes": "net.minecraft.client.renderer.rendertype.RenderTypes",
    "Axis": "com.mojang.math.Axis",
    "IafItemRender": "com.iafabric.client.render.IafItemRender",
    "ItemDisplayContext": "net.minecraft.world.item.ItemDisplayContext",
}


def add_import(s, imp):
    if "import " + imp + ";" in s:
        return s
    m = re.search(r"^package [^;]+;\n", s, re.M)
    return s[:m.end()] + "\nimport " + imp + ";" + s[m.end():]


def vertices(s):
    """The 1.18 vertex chain (vertex/color/uv/overlayCoords/uv2/normal/endVertex) on the 26.x names."""
    s = re.sub(r"\.vertex\((\w+), ", r".addVertex(\1, ", s)
    s = re.sub(r"\)\s*\.color\(", ").setColor(", s)
    s = re.sub(r"\)\s*\.uv\(", ").setUv(", s)
    s = re.sub(r"\)\s*\.overlayCoords\(", ").setOverlay(", s)
    s = re.sub(r"\)\s*\.uv2\(", ").setLight(", s)
    s = re.sub(r"\)\s*\.normal\(\w+, ", ").setNormal(", s)
    s = re.sub(r"\)\s*\.endVertex\(\)", ")", s)
    return s


ITEM_CONTEXTS = {"FIXED", "GROUND", "NONE", "GUI", "HEAD", "THIRD_PERSON_RIGHT_HAND", "THIRD_PERSON_LEFT_HAND", "FIRST_PERSON_RIGHT_HAND", "FIRST_PERSON_LEFT_HAND"}


def items(s):
    s = re.sub(r"Minecraft\.getInstance\(\)\.getItemRenderer\(\)\.renderStatic\(((?:[^,()]|\([^()]*\))+), ItemTransforms\.TransformType\.(\w+), (\w+), [^,]+, (\w+), (\w+), \w+\)",
               lambda m: "IafItemRender.render(%s, ItemDisplayContext.%s, %s, %s, %s)" % (m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)), s)
    s = s.replace("ItemTransforms.TransformType", "ItemDisplayContext")
    return s


def port(s):
    s = vertices(s)
    s = items(s)
    s = s.replace("import net.minecraft.client.renderer.BufferSource;" + chr(10), "")
    s = re.sub(r"extends MobRenderer<", "extends IafMobRenderer<", s)
    s = re.sub(r"extends RenderLayer<", "extends IafRenderLayer<", s)
    s = re.sub(r"\bRenderLayerParent<([^>]*(?:<[^>]*>)?[^>]*)>", r"IafMobRenderer<\1>", s)
    s = re.sub(r"\bMultiBufferSource\b", "BufferSource", s)
    s = re.sub(r"RenderType\.entityCutoutNoCull\(([^,()]+(?:\([^()]*\))?)(?:, (?:true|false))?\)", r"RenderTypes.entityCutout(\1)", s)
    s = re.sub(r"\bRenderType\.(\w+)\(", r"RenderTypes.\1(", s)
    s = re.sub(r"new Quaternionf\(Vector3f\.([XYZ])P, ([^;]*?), true\)", r"Axis.\1P.rotationDegrees(\2)", s)
    s = re.sub(r"new Quaternionf\(Vector3f\.([XYZ])N, ([^;]*?), true\)", r"Axis.\1N.rotationDegrees(\2)", s)
    s = re.sub(r"(\w+)\.mulPose\(Axis\.", r"\1.rotate(Axis.", s)
    s = re.sub(r"\.rotate\(Axis\.([XYZ][PN])\.rotationDegrees\(", r".rotateDegrees(Axis.\1, (", s)
    # the renderer's render(): what it draws after super.render becomes renderExtra
    m = re.search(r"(@Override\s*)?public void render\((?:@NotNull\s+)?(\w+(?:<[^>]*>)?) (\w+), float \w+, float (\w+), (?:@NotNull\s+)?PoseStack (\w+), (?:@NotNull\s+)?BufferSource (\w+), int (\w+)\) \{\s*super\.render\([^;]*\);", s)
    if m:
        s = s[:m.start()] + "@Override\n    protected void renderExtra(%s %s, float %s, PoseStack %s, BufferSource %s, int %s) {" % (
            m.group(2), m.group(3), m.group(4), m.group(5), m.group(6), m.group(7)) + s[m.end():]
    s = re.sub(r"protected Identifier getTextureLocation\(", "public Identifier getTextureLocation(", s)
    s = re.sub(r"(@Override\s*)public (@NotNull )?Identifier getTextureLocation\(@NotNull ", r"\1public Identifier getTextureLocation(", s)
    for simple, imp in IMPORTS.items():
        if re.search(r"\b" + simple + r"\b", s):
            s = add_import(s, imp)
    for gone in ["net.minecraft.client.renderer.MultiBufferSource", "net.minecraft.client.renderer.entity.MobRenderer",
                 "net.minecraft.client.renderer.entity.layers.RenderLayer", "net.minecraft.client.renderer.entity.RenderLayerParent",
                 "org.joml.Vector3f", "org.joml.Quaternionf"]:
        if not re.search(r"\b" + gone.rsplit(".", 1)[1] + r"\b", s.replace("import " + gone + ";", "")):
            s = s.replace("import " + gone + ";\n", "")
    return s


for name in sys.argv[1:]:
    p = pathlib.Path(name)
    s = p.read_text(encoding="utf-8")
    t = port(s)
    if t != s:
        p.write_text(t, encoding="utf-8", newline="\n")
