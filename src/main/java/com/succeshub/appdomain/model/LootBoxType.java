package com.succeshub.appdomain.model;

/**
 * The three V1 loot box types. The box type determines the rarity weighting of
 * each roll, not the item pool — every box draws from the same reward catalog.
 *
 * <p>Weights are percentages and sum to 100 per box.</p>
 */
public enum LootBoxType {

    ARCANE_ORB(
            "Arcane Orb",
            "Daily streak qualifier",
            "Common, frequent",
            "A glowing sphere of accumulated focus. Earned every day you keep the streak alive.",
            "blur_on",
            70, 27, 3),

    IRON_CHEST(
            "Iron Chest",
            "Streak milestones (7d, 30d) + achievement unlock",
            "Uncommon, satisfying",
            "An armored cache that snaps open with a satisfying clunk. Reserved for real milestones.",
            "inventory_2",
            50, 44, 6),

    SOVEREIGN_VAULT(
            "Sovereign Vault",
            "100d streak, weekly reset, rare achievement",
            "Rare, cinematic",
            "An ornate dark vault that opens only for the truly relentless. The odds bend in your favor here.",
            "diamond",
            25, 60, 15);

    private final String displayName;
    private final String source;
    private final String feel;
    private final String blurb;
    private final String icon;
    private final int commonWeight;
    private final int rareWeight;
    private final int legendaryWeight;

    LootBoxType(String displayName, String source, String feel, String blurb, String icon,
                int commonWeight, int rareWeight, int legendaryWeight) {
        this.displayName = displayName;
        this.source = source;
        this.feel = feel;
        this.blurb = blurb;
        this.icon = icon;
        this.commonWeight = commonWeight;
        this.rareWeight = rareWeight;
        this.legendaryWeight = legendaryWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSource() {
        return source;
    }

    public String getFeel() {
        return feel;
    }

    public String getBlurb() {
        return blurb;
    }

    public String getIcon() {
        return icon;
    }

    public int getCommonWeight() {
        return commonWeight;
    }

    public int getRareWeight() {
        return rareWeight;
    }

    public int getLegendaryWeight() {
        return legendaryWeight;
    }

    /** Returns the weight (percentage) for a given rarity. */
    public int weightFor(RewardDefinition.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> commonWeight;
            case RARE -> rareWeight;
            case LEGENDARY -> legendaryWeight;
        };
    }
}
