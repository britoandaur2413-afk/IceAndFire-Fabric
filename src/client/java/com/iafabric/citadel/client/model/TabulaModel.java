package com.iafabric.citadel.client.model;

import com.iafabric.citadel.client.model.basic.BasicModelPart;
import com.iafabric.citadel.client.model.container.TabulaCubeContainer;
import com.iafabric.citadel.client.model.container.TabulaCubeGroupContainer;
import com.iafabric.citadel.client.model.container.TabulaModelContainer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A model built from a Tabula .tbl file (replaces Citadel's TabulaModel). */
public class TabulaModel extends AdvancedEntityModel<Entity> {
	protected final Map<String, AdvancedModelBox> cubes = new LinkedHashMap<>();
	protected final Map<String, AdvancedModelBox> identifierMap = new HashMap<>();
	protected final List<AdvancedModelBox> rootBoxes = new ArrayList<>();
	protected final ITabulaModelAnimator<?> tabulaAnimator;
	public final ModelAnimator llibAnimator;
	protected final double[] scale;

	public TabulaModel(TabulaModelContainer container, ITabulaModelAnimator<?> tabulaAnimator) {
		this.texWidth = container.getTextureWidth();
		this.texHeight = container.getTextureHeight();
		this.tabulaAnimator = tabulaAnimator;
		this.llibAnimator = ModelAnimator.create();
		this.scale = container.scale;
		for (TabulaCubeContainer cube : container.getCubes()) {
			parseCube(cube, null);
		}
		for (TabulaCubeGroupContainer group : container.getCubeGroups()) {
			parseGroup(group);
		}
		updateDefaultPose();
	}

	public TabulaModel(TabulaModelContainer container) {
		this(container, null);
	}

	private void parseGroup(TabulaCubeGroupContainer group) {
		for (TabulaCubeContainer cube : group.getCubes()) {
			parseCube(cube, null);
		}
		for (TabulaCubeGroupContainer child : group.getCubeGroups()) {
			parseGroup(child);
		}
	}

	private void parseCube(TabulaCubeContainer cube, AdvancedModelBox parent) {
		AdvancedModelBox box = createBox(cube);
		this.cubes.put(cube.getName(), box);
		if (cube.getIdentifier() != null) {
			this.identifierMap.put(cube.getIdentifier(), box);
		}
		if (parent != null) {
			parent.addChild(box);
		} else {
			this.rootBoxes.add(box);
		}
		for (TabulaCubeContainer child : cube.getChildren()) {
			parseCube(child, box);
		}
	}

	protected AdvancedModelBox createBox(TabulaCubeContainer cube) {
		int[] textureOffset = cube.getTextureOffset();
		double[] position = cube.getPosition();
		double[] rotation = cube.getRotation();
		double[] offset = cube.getOffset();
		int[] dimensions = cube.getDimensions();
		AdvancedModelBox box = new AdvancedModelBox(this, cube.getName(), textureOffset[0], textureOffset[1]);
		box.mirror = cube.isTextureMirrorEnabled();
		box.setRotationPoint((float) position[0], (float) position[1], (float) position[2]);
		box.addBox((float) offset[0], (float) offset[1], (float) offset[2], dimensions[0], dimensions[1], dimensions[2], (float) cube.getMcScale());
		box.rotateAngleX = (float) Math.toRadians(rotation[0]);
		box.rotateAngleY = (float) Math.toRadians(rotation[1]);
		box.rotateAngleZ = (float) Math.toRadians(rotation[2]);
		double[] cubeScale = cube.getScale();
		if (cubeScale != null) {
			box.setScale((float) cubeScale[0], (float) cubeScale[1], (float) cubeScale[2]);
		}
		box.showModel = !cube.isHidden();
		return box;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		resetToDefaultPose();
		if (this.tabulaAnimator != null) {
			((ITabulaModelAnimator) this.tabulaAnimator).setRotationAngles(this, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 1.0F);
		}
	}

	public AdvancedModelBox getCube(String name) {
		return this.cubes.get(name);
	}

	public AdvancedModelBox getCubeByIdentifier(String identifier) {
		return this.identifierMap.get(identifier);
	}

	public Map<String, AdvancedModelBox> getCubes() {
		return this.cubes;
	}

	public List<AdvancedModelBox> getRootBoxes() {
		return this.rootBoxes;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Iterable<BasicModelPart> parts() {
		return (List) this.rootBoxes;
	}

	@Override
	public Iterable<AdvancedModelBox> getAllParts() {
		return this.cubes.values();
	}
}
