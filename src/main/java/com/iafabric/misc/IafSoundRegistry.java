package com.iafabric.misc;

import com.iafabric.IafMod;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import static com.iafabric.IafMod.MOD_ID;

@SuppressWarnings("WeakerAccess")
public final class IafSoundRegistry {

    public static final SoundEvent BESTIARY_PAGE = createSoundEvent("bestiary_page");

    public static final SoundEvent EGG_HATCH = createSoundEvent("egg_hatch");

    public static final SoundEvent FIREDRAGON_BREATH = createSoundEvent("firedragon_breath");

    public static final SoundEvent ICEDRAGON_BREATH = createSoundEvent("icedragon_breath");

    public static final SoundEvent FIREDRAGON_CHILD_IDLE = createSoundEvent("firedragon_child_idle");

    public static final SoundEvent FIREDRAGON_CHILD_HURT = createSoundEvent("firedragon_child_hurt");

    public static final SoundEvent FIREDRAGON_CHILD_DEATH = createSoundEvent("firedragon_child_death");

    public static final SoundEvent FIREDRAGON_CHILD_ROAR = createSoundEvent("firedragon_child_roar");

    public static final SoundEvent FIREDRAGON_TEEN_ROAR = createSoundEvent("firedragon_teen_roar");

    public static final SoundEvent FIREDRAGON_TEEN_IDLE = createSoundEvent("firedragon_teen_idle");

    public static final SoundEvent FIREDRAGON_TEEN_DEATH = createSoundEvent("firedragon_teen_death");

    public static final SoundEvent FIREDRAGON_TEEN_HURT = createSoundEvent("firedragon_teen_hurt");

    public static final SoundEvent FIREDRAGON_ADULT_ROAR = createSoundEvent("firedragon_adult_roar");

    public static final SoundEvent FIREDRAGON_ADULT_IDLE = createSoundEvent("firedragon_adult_idle");

    public static final SoundEvent FIREDRAGON_ADULT_DEATH = createSoundEvent("firedragon_adult_death");

    public static final SoundEvent FIREDRAGON_ADULT_HURT = createSoundEvent("firedragon_adult_hurt");

    public static final SoundEvent ICEDRAGON_CHILD_IDLE = createSoundEvent("icedragon_child_idle");

    public static final SoundEvent ICEDRAGON_CHILD_HURT = createSoundEvent("icedragon_child_hurt");

    public static final SoundEvent ICEDRAGON_CHILD_DEATH = createSoundEvent("icedragon_child_death");

    public static final SoundEvent ICEDRAGON_CHILD_ROAR = createSoundEvent("icedragon_child_roar");

    public static final SoundEvent ICEDRAGON_TEEN_ROAR = createSoundEvent("icedragon_teen_roar");

    public static final SoundEvent ICEDRAGON_TEEN_IDLE = createSoundEvent("icedragon_teen_idle");

    public static final SoundEvent ICEDRAGON_TEEN_DEATH = createSoundEvent("icedragon_teen_death");

    public static final SoundEvent ICEDRAGON_TEEN_HURT = createSoundEvent("icedragon_teen_hurt");

    public static final SoundEvent ICEDRAGON_ADULT_ROAR = createSoundEvent("icedragon_adult_roar");

    public static final SoundEvent ICEDRAGON_ADULT_IDLE = createSoundEvent("icedragon_adult_idle");

    public static final SoundEvent ICEDRAGON_ADULT_DEATH = createSoundEvent("icedragon_adult_death");

    public static final SoundEvent ICEDRAGON_ADULT_HURT = createSoundEvent("icedragon_adult_hurt");

    public static final SoundEvent DRAGONFLUTE = createSoundEvent("dragonflute");

    public static final SoundEvent HIPPOGRYPH_IDLE = createSoundEvent("hippogryph_idle");

    public static final SoundEvent HIPPOGRYPH_HURT = createSoundEvent("hippogryph_hurt");

    public static final SoundEvent HIPPOGRYPH_DIE = createSoundEvent("hippogryph_die");

    public static final SoundEvent GORGON_IDLE = createSoundEvent("gorgon_idle");

    public static final SoundEvent GORGON_HURT = createSoundEvent("gorgon_hurt");

    public static final SoundEvent GORGON_DIE = createSoundEvent("gorgon_die");

    public static final SoundEvent GORGON_ATTACK = createSoundEvent("gorgon_attack");

    public static final SoundEvent GORGON_PETRIFY = createSoundEvent("gorgon_petrify");

    public static final SoundEvent TURN_STONE = createSoundEvent("turn_stone");

    public static final SoundEvent PIXIE_IDLE = createSoundEvent("pixie_idle");

    public static final SoundEvent PIXIE_HURT = createSoundEvent("pixie_hurt");

    public static final SoundEvent PIXIE_DIE = createSoundEvent("pixie_die");

    public static final SoundEvent PIXIE_TAUNT = createSoundEvent("pixie_taunt");

    public static final SoundEvent GOLD_PILE_STEP = createSoundEvent("gold_pile_step");

    public static final SoundEvent GOLD_PILE_BREAK = createSoundEvent("gold_pile_break");

    public static final SoundEvent DRAGON_FLIGHT = createSoundEvent("dragon_flight");

    public static final SoundEvent CYCLOPS_IDLE = createSoundEvent("cyclops_idle");

    public static final SoundEvent CYCLOPS_HURT = createSoundEvent("cyclops_hurt");

    public static final SoundEvent CYCLOPS_DIE = createSoundEvent("cyclops_die");

    public static final SoundEvent CYCLOPS_BITE = createSoundEvent("cyclops_bite");

    public static final SoundEvent CYCLOPS_BLINDED = createSoundEvent("cyclops_blinded");

    public static final SoundEvent HIPPOCAMPUS_IDLE = createSoundEvent("hippocampus_idle");

    public static final SoundEvent HIPPOCAMPUS_HURT = createSoundEvent("hippocampus_hurt");

    public static final SoundEvent HIPPOCAMPUS_DIE = createSoundEvent("hippocampus_die");

    public static final SoundEvent DEATHWORM_IDLE = createSoundEvent("deathworm_idle");

    public static final SoundEvent DEATHWORM_ATTACK = createSoundEvent("deathworm_attack");

    public static final SoundEvent DEATHWORM_HURT = createSoundEvent("deathworm_hurt");

    public static final SoundEvent DEATHWORM_DIE = createSoundEvent("deathworm_die");

    public static final SoundEvent DEATHWORM_GIANT_IDLE = createSoundEvent("deathworm_giant_idle");

    public static final SoundEvent DEATHWORM_GIANT_ATTACK = createSoundEvent("deathworm_giant_attack");

    public static final SoundEvent DEATHWORM_GIANT_HURT = createSoundEvent("deathworm_giant_hurt");

    public static final SoundEvent DEATHWORM_GIANT_DIE = createSoundEvent("deathworm_giant_die");

    public static final SoundEvent NAGA_IDLE = createSoundEvent("naga_idle");

    public static final SoundEvent NAGA_ATTACK = createSoundEvent("naga_attack");

    public static final SoundEvent NAGA_HURT = createSoundEvent("naga_hurt");

    public static final SoundEvent NAGA_DIE = createSoundEvent("naga_die");

    public static final SoundEvent MERMAID_IDLE = createSoundEvent("mermaid_idle");

    public static final SoundEvent MERMAID_HURT = createSoundEvent("mermaid_hurt");

    public static final SoundEvent MERMAID_DIE = createSoundEvent("mermaid_die");

    public static final SoundEvent SIREN_SONG = createSoundEvent("siren_song");

    public static final SoundEvent TROLL_DIE = createSoundEvent("troll_die");

    public static final SoundEvent TROLL_IDLE = createSoundEvent("troll_idle");

    public static final SoundEvent TROLL_HURT = createSoundEvent("troll_hurt");

    public static final SoundEvent TROLL_ROAR = createSoundEvent("troll_roar");

    public static final SoundEvent COCKATRICE_DIE = createSoundEvent("cockatrice_die");

    public static final SoundEvent COCKATRICE_IDLE = createSoundEvent("cockatrice_idle");

    public static final SoundEvent COCKATRICE_HURT = createSoundEvent("cockatrice_hurt");

    public static final SoundEvent COCKATRICE_CRY = createSoundEvent("cockatrice_cry");

    public static final SoundEvent STYMPHALIAN_BIRD_DIE = createSoundEvent("stymphalian_bird_die");

    public static final SoundEvent STYMPHALIAN_BIRD_IDLE = createSoundEvent("stymphalian_bird_idle");

    public static final SoundEvent STYMPHALIAN_BIRD_HURT = createSoundEvent("stymphalian_bird_hurt");

    public static final SoundEvent STYMPHALIAN_BIRD_ATTACK = createSoundEvent("stymphalian_bird_attack");

    public static final SoundEvent MYRMEX_DIE = createSoundEvent("myrmex_die");

    public static final SoundEvent MYRMEX_IDLE = createSoundEvent("myrmex_idle");

    public static final SoundEvent MYRMEX_HURT = createSoundEvent("myrmex_hurt");

    public static final SoundEvent MYRMEX_WALK = createSoundEvent("myrmex_walk");

    public static final SoundEvent MYRMEX_BITE = createSoundEvent("myrmex_bite");

    public static final SoundEvent MYRMEX_STING = createSoundEvent("myrmex_sting");

    public static final SoundEvent AMPHITHERE_DIE = createSoundEvent("amphithere_die");

    public static final SoundEvent AMPHITHERE_IDLE = createSoundEvent("amphithere_idle");

    public static final SoundEvent AMPHITHERE_HURT = createSoundEvent("amphithere_hurt");

    public static final SoundEvent AMPHITHERE_BITE = createSoundEvent("amphithere_bite");

    public static final SoundEvent AMPHITHERE_GUST = createSoundEvent("amphithere_gust");

    public static final SoundEvent SEA_SERPENT_DIE = createSoundEvent("sea_serpent_die");

    public static final SoundEvent SEA_SERPENT_IDLE = createSoundEvent("sea_serpent_idle");

    public static final SoundEvent SEA_SERPENT_HURT = createSoundEvent("sea_serpent_hurt");

    public static final SoundEvent SEA_SERPENT_BITE = createSoundEvent("sea_serpent_bite");

    public static final SoundEvent SEA_SERPENT_ROAR = createSoundEvent("sea_serpent_roar");

    public static final SoundEvent SEA_SERPENT_BREATH = createSoundEvent("sea_serpent_breath");

    public static final SoundEvent SEA_SERPENT_SPLASH = createSoundEvent("sea_serpent_splash");

    public static final SoundEvent HYDRA_DIE = createSoundEvent("hydra_die");

    public static final SoundEvent HYDRA_IDLE = createSoundEvent("hydra_idle");

    public static final SoundEvent HYDRA_HURT = createSoundEvent("hydra_hurt");

    public static final SoundEvent HYDRA_SPIT = createSoundEvent("hydra_spit");

    public static final SoundEvent HYDRA_REGEN_HEAD = createSoundEvent("hydra_regen_head");

    public static final SoundEvent PIXIE_WAND = createSoundEvent("pixie_wand");

    public static final SoundEvent DREAD_LICH_SUMMON = createSoundEvent("dread_lich_summon");

    public static final SoundEvent DREAD_GHOUL_IDLE = createSoundEvent("dread_ghoul_idle");

    public static final SoundEvent LIGHTNINGDRAGON_CHILD_IDLE = createSoundEvent("lightningdragon_child_idle");

    public static final SoundEvent LIGHTNINGDRAGON_CHILD_HURT = createSoundEvent("lightningdragon_child_hurt");

    public static final SoundEvent LIGHTNINGDRAGON_CHILD_DEATH = createSoundEvent("lightningdragon_child_death");

    public static final SoundEvent LIGHTNINGDRAGON_CHILD_ROAR = createSoundEvent("lightningdragon_child_roar");

    public static final SoundEvent LIGHTNINGDRAGON_TEEN_ROAR = createSoundEvent("lightningdragon_teen_roar");

    public static final SoundEvent LIGHTNINGDRAGON_TEEN_IDLE = createSoundEvent("lightningdragon_teen_idle");

    public static final SoundEvent LIGHTNINGDRAGON_TEEN_DEATH = createSoundEvent("lightningdragon_teen_death");

    public static final SoundEvent LIGHTNINGDRAGON_TEEN_HURT = createSoundEvent("lightningdragon_teen_hurt");

    public static final SoundEvent LIGHTNINGDRAGON_ADULT_ROAR = createSoundEvent("lightningdragon_adult_roar");

    public static final SoundEvent LIGHTNINGDRAGON_ADULT_IDLE = createSoundEvent("lightningdragon_adult_idle");

    public static final SoundEvent LIGHTNINGDRAGON_ADULT_DEATH = createSoundEvent("lightningdragon_adult_death");

    public static final SoundEvent LIGHTNINGDRAGON_ADULT_HURT = createSoundEvent("lightningdragon_adult_hurt");

    public static final SoundEvent LIGHTNINGDRAGON_BREATH = createSoundEvent("lightningdragon_breath");

    public static final SoundEvent LIGHTNINGDRAGON_BREATH_CRACKLE = createSoundEvent("lightningdragon_breath_crackle");

    public static final SoundEvent GHOST_IDLE = createSoundEvent("ghost_idle");

    public static final SoundEvent GHOST_HURT = createSoundEvent("ghost_hurt");

    public static final SoundEvent GHOST_DIE = createSoundEvent("ghost_die");

    public static final SoundEvent GHOST_ATTACK = createSoundEvent("ghost_attack");

    public static final SoundEvent GHOST_JUMPSCARE = createSoundEvent("ghost_jumpscare");

    /** Cryona's voice, a line for each moment of her fight (boss/CryonaVoice). */
    public static final SoundEvent CRYONA_ENTRY = createSoundEvent("cryona_entry");

    public static final SoundEvent CRYONA_WAKE = createSoundEvent("cryona_wake");

    public static final SoundEvent CRYONA_CAST1 = createSoundEvent("cryona_cast1");

    public static final SoundEvent CRYONA_CAST2 = createSoundEvent("cryona_cast2");

    public static final SoundEvent CRYONA_CAST3 = createSoundEvent("cryona_cast3");

    public static final SoundEvent CRYONA_ZERO = createSoundEvent("cryona_zero");

    public static final SoundEvent CRYONA_LAMENT = createSoundEvent("cryona_lament");

    public static final SoundEvent CRYONA_VENGEANCE = createSoundEvent("cryona_vengeance");

    public static final SoundEvent CRYONA_FALTER = createSoundEvent("cryona_falter");

    public static final SoundEvent CRYONA_DEATH = createSoundEvent("cryona_death");

    public static final SoundEvent CRYONA_LOST = createSoundEvent("cryona_lost");

    public static final SoundEvent CRYONA_TAUNT = createSoundEvent("cryona_taunt");

    /** Her grief after Black Frost falls: she sobs, then her anger grows to fury (boss/CryonaFight). */
    public static final SoundEvent CRYONA_SOB1 = createSoundEvent("cryona_sob1");

    public static final SoundEvent CRYONA_SOB2 = createSoundEvent("cryona_sob2");

    public static final SoundEvent CRYONA_ANGER = createSoundEvent("cryona_anger");

    public static final SoundEvent CRYONA_FURY = createSoundEvent("cryona_fury");

    /** Her ice shield rising and breaking; her rays of ice falling from the sky. */
    public static final SoundEvent CRYONA_SHIELD_UP = createSoundEvent("cryona_shield_up");

    public static final SoundEvent CRYONA_SHIELD_BREAK = createSoundEvent("cryona_shield_break");

    public static final SoundEvent CRYONA_ICE_RAY = createSoundEvent("cryona_ice_ray");

    /** Black Frost's breaths: its great breath, its frost breath (a charge, the blast, its echo: as long as each lasts). */
    public static final SoundEvent BLACK_FROST_GREAT_BREATH = createSoundEvent("black_frost_great_breath");

    public static final SoundEvent BLACK_FROST_BREATH = createSoundEvent("black_frost_breath");

    /** A quill writing by magic while a memory's subtitles are written out (client/sound/Writing). */
    public static final SoundEvent MEMORY_WRITING = createSoundEvent("memory_writing");

    /** The soundtracks (boss/Soundtracks): Cryona's duel, the exiled pixie's memories. */
    public static final SoundEvent MUSIC_CRYONA_DUEL = createSoundEvent("music.cryona_duel");

    public static final SoundEvent MUSIC_FAIRY_MEMORIES = createSoundEvent("music.fairy_memories");

    private static SoundEvent createSoundEvent(final String soundName) {
        final Identifier soundID = Identifier.fromNamespaceAndPath(MOD_ID, soundName);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, soundID, SoundEvent.createVariableRangeEvent(soundID));
    }

    /** Loads the class, which registers every sound. */
    public static void init() {
    }
}
