package com.iafabric.client.model;

import com.iafabric.citadel.animation.Animation;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.citadel.client.model.ModelAnimator;
import com.iafabric.client.model.util.HideableModelRenderer;
import com.iafabric.entity.EntityDreadQueen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

/** Cryona's model: herself (EntityDreadQueen) and her sign asleep on her throne (EntityCryonaAsleep). */
public class ModelDreadQueen<T extends LivingEntity & IAnimatedEntity> extends ModelDreadBase<T> {
    public HideableModelRenderer chestplate;
    public HideableModelRenderer cloak;
    public HideableModelRenderer necklace;
    public HideableModelRenderer sleeveRight;
    public HideableModelRenderer robeLowerRight;
    public HideableModelRenderer sleeveLeft;
    public HideableModelRenderer robeLowerLeft;

    public ModelDreadQueen(float modelScale) {
        this.texWidth = 128;
        this.texHeight = 64;
        this.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        this.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        this.body = new HideableModelRenderer(this, 16, 16);
        this.body.setPos(0.0F, 0.0F, 0.0F);
        this.body.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelScale);
        this.headware = new HideableModelRenderer(this, 58, 0);
        this.headware.setPos(0.0F, 1.0F, 0.0F);
        this.headware.addBox(-4.5F, -12.5F, -4.5F, 9, 13, 9, modelScale);
        this.legRight = new HideableModelRenderer(this, 0, 16);
        this.legRight.setPos(-1.9F, 12.0F, 0.1F);
        this.legRight.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelScale);
        this.sleeveLeft = new HideableModelRenderer(this, 36, 33);
        this.sleeveLeft.mirror = true;
        this.sleeveLeft.setPos(-0.5F, -0.1F, 0.0F);
        this.sleeveLeft.addBox(-0.5F, -2.1F, -2.4F, 5, 6, 5, modelScale);
        this.robeLowerLeft = new HideableModelRenderer(this, 58, 33);
        this.robeLowerLeft.setPos(0.0F, -0.2F, 0.0F);
        this.robeLowerLeft.addBox(-1.9F, 0.0F, -2.5F, 4, 7, 5, modelScale);
        this.chestplate = new HideableModelRenderer(this, 1, 32);
        this.chestplate.setPos(0.0F, 0.1F, 0.0F);
        this.chestplate.addBox(-4.5F, -0.3F, -2.5F, 9, 12, 5, modelScale);
        this.robeLowerRight = new HideableModelRenderer(this, 58, 33);
        this.robeLowerRight.mirror = true;
        this.robeLowerRight.setPos(0.0F, -0.2F, 0.0F);
        this.robeLowerRight.addBox(-2.1F, 0.0F, -2.5F, 4, 7, 5, modelScale);
        this.sleeveRight = new HideableModelRenderer(this, 36, 33);
        this.sleeveRight.setPos(0.5F, -0.1F, 0.0F);
        this.sleeveRight.addBox(-4.5F, -2.1F, -2.4F, 5, 6, 5, modelScale);
        this.cloak = new HideableModelRenderer(this, 81, 37);
        this.cloak.setPos(0.0F, 0.1F, 0.0F);
        this.cloak.addBox(-4.5F, 0.0F, 2.3F, 9, 21, 1, modelScale);
        this.setRotateAngle(cloak, 0.045553093477052F, 0.0F, 0.0F);
        this.armRight = new HideableModelRenderer(this, 40, 16);
        this.armRight.setPos(-5.0F, 2.0F, 0.0F);
        this.armRight.addBox(-2.5F, -2.0F, -2.0F, 3, 12, 4, modelScale);
        this.setRotateAngle(armRight, 0.0F, 0.0F, 0.10000736613927509F);
        this.necklace = new HideableModelRenderer(this, 1, 52);
        this.necklace.setPos(0.0F, 0.1F, 0.0F);
        this.necklace.addBox(-4.5F, -0.3F, -2.8F, 9, 7, 5, modelScale);
        this.setRotateAngle(necklace, 0.032114058236695664F, 0.0F, 0.0F);
        this.armLeft = new HideableModelRenderer(this, 40, 16);
        this.armLeft.mirror = true;
        this.armLeft.setPos(5.0F, 2.0F, -0.0F);
        this.armLeft.addBox(-0.5F, -2.0F, -2.0F, 3, 12, 4, modelScale);
        this.setRotateAngle(armLeft, -0.0F, 0.0F, -0.10000736613927509F);
        this.head = new HideableModelRenderer(this, 0, 0);
        this.head.setPos(0.0F, 0.0F, 0.0F);
        this.head.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, modelScale);
        this.legLeft = new HideableModelRenderer(this, 0, 16);
        this.legLeft.mirror = true;
        this.legLeft.setPos(1.9F, 12.0F, 0.1F);
        this.legLeft.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelScale);
        this.head.addChild(this.headware);
        this.body.addChild(this.legRight);
        this.armLeft.addChild(this.sleeveLeft);
        this.legLeft.addChild(this.robeLowerLeft);
        this.body.addChild(this.chestplate);
        this.legRight.addChild(this.robeLowerRight);
        this.armRight.addChild(this.sleeveRight);
        this.body.addChild(this.cloak);
        this.body.addChild(this.armRight);
        this.chestplate.addChild(this.necklace);
        this.body.addChild(this.armLeft);
        this.body.addChild(this.head);
        this.body.addChild(this.legLeft);
        this.updateDefaultPose();
        animator = ModelAnimator.create();
    }

    @Override
    public void prepareMobModel(T LivingEntityIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        this.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        this.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        super.prepareMobModel(LivingEntityIn, limbSwing, limbSwingAmount, partialTickTime);
    }

    @Override
    public Animation getSpawnAnimation() {
        return EntityDreadQueen.ANIMATION_SPAWN;
    }

    /** Her blows (CryonaFight): the staff raised for a spell, a cut, a thrust, a leap and its blow down. */
    @Override
    public void animate(T entity, float f, float f1, float f2, float f3, float f4, float f5) {
        super.animate(entity, f, f1, f2, f3, f4, f5);
        if (animator.setAnimation(EntityDreadQueen.ANIMATION_CAST)) {
            animator.startKeyframe(6);
            rotate(animator, this.armLeft, -165, 0, -12);
            rotate(animator, this.armRight, -25, 0, 25);
            rotate(animator, this.head, -15, 0, 0);
            animator.endKeyframe();
            animator.setStaticKeyframe(12);
            animator.resetKeyframe(6);
        }
        if (animator.setAnimation(EntityDreadQueen.ANIMATION_SLASH)) {
            // the blade drawn back high on her right, then across in front of her (the blow at tick 9)
            animator.startKeyframe(7);
            rotate(animator, this.armRight, -125, 55, 0);
            rotate(animator, this.body, 0, 30, 0);
            animator.endKeyframe();
            animator.setStaticKeyframe(1);
            animator.startKeyframe(2);
            rotate(animator, this.armRight, -85, -70, 0);
            rotate(animator, this.body, 0, -30, 0);
            animator.endKeyframe();
            animator.setStaticKeyframe(2);
            animator.resetKeyframe(4);
        }
        if (animator.setAnimation(EntityDreadQueen.ANIMATION_THRUST)) {
            // drawn back, then driven straight ahead, leaning into it (the blow at tick 9)
            animator.startKeyframe(7);
            rotate(animator, this.armRight, -30, 0, 15);
            rotate(animator, this.body, -8, 25, 0);
            animator.endKeyframe();
            animator.setStaticKeyframe(1);
            animator.startKeyframe(2);
            rotate(animator, this.armRight, -95, -10, 0);
            rotate(animator, this.body, 18, -10, 0);
            animator.endKeyframe();
            animator.setStaticKeyframe(3);
            animator.resetKeyframe(5);
        }
        if (animator.setAnimation(EntityDreadQueen.ANIMATION_SLAM)) {
            // she crouches, leaps with her blade high over her head and brings it down as she lands (tick 28)
            animator.startKeyframe(10);
            animator.move(this.body, 0, 4, 0);
            rotate(animator, this.body, 20, 0, 0);
            rotate(animator, this.armRight, -170, 0, 10);
            rotate(animator, this.armLeft, -170, 0, -10);
            rotate(animator, this.legRight, -40, 0, 0);
            rotate(animator, this.legLeft, 30, 0, 0);
            animator.endKeyframe();
            animator.startKeyframe(6);
            rotate(animator, this.body, -10, 0, 0);
            rotate(animator, this.armRight, -175, 0, 10);
            rotate(animator, this.armLeft, -175, 0, -10);
            rotate(animator, this.legRight, -30, 0, 0);
            rotate(animator, this.legLeft, 20, 0, 0);
            animator.endKeyframe();
            animator.setStaticKeyframe(10);
            animator.startKeyframe(2);
            animator.move(this.body, 0, 3, 0);
            rotate(animator, this.body, 30, 0, 0);
            rotate(animator, this.armRight, -40, 0, 10);
            rotate(animator, this.armLeft, -40, 0, -10);
            animator.endKeyframe();
            animator.setStaticKeyframe(3);
            animator.resetKeyframe(5);
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (!(entity instanceof EntityDreadQueen queen)) {
            return;
        }
        float partial = ageInTicks - queen.tickCount;
        float kneel = net.minecraft.util.Mth.lerp(partial, queen.prevKneelProgress, queen.kneelProgress) / 20F;
        if (kneel <= 0F) {
            return;
        }
        // down on one knee, her blade planted before her, her head bowed (mourning, spent, at her end)
        this.body.rotationPointY += 6F * kneel;
        this.body.rotateAngleX += 0.2F * kneel;
        this.legRight.rotateAngleX = net.minecraft.util.Mth.lerp(kneel, this.legRight.rotateAngleX, -0.95F);
        this.legLeft.rotateAngleX = net.minecraft.util.Mth.lerp(kneel, this.legLeft.rotateAngleX, 1.0F);
        this.legRight.rotateAngleY = net.minecraft.util.Mth.lerp(kneel, this.legRight.rotateAngleY, 0.15F);
        this.legLeft.rotateAngleY = net.minecraft.util.Mth.lerp(kneel, this.legLeft.rotateAngleY, -0.1F);
        this.armRight.rotateAngleX = net.minecraft.util.Mth.lerp(kneel, this.armRight.rotateAngleX, -0.55F);
        this.armLeft.rotateAngleX = net.minecraft.util.Mth.lerp(kneel, this.armLeft.rotateAngleX, -0.15F);
        this.head.rotateAngleX = net.minecraft.util.Mth.lerp(kneel, this.head.rotateAngleX, 0.55F);
    }

}
