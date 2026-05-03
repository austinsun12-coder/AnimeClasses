package com.animeclasses;

import org.bukkit.Material;

public enum AnimeClass {
    GOJO        ("Satoru Gojo",    "Jujutsu Kaisen",      Material.GLASS,          "§b"),
    ITADORI     ("Yuji Itadori",   "Jujutsu Kaisen",      Material.IRON_SWORD,     "§c"),
    SAITAMA     ("Saitama",        "One Punch Man",        Material.YELLOW_WOOL,    "§e"),
    GOKU        ("Goku",           "Dragon Ball",          Material.ORANGE_WOOL,    "§6"),
    LUFFY       ("Monkey D. Luffy","One Piece",            Material.STRAW_BALE,     "§d"),
    EREN        ("Eren Yeager",    "Attack on Titan",      Material.BONE_BLOCK,     "§7"),
    WUKONG      ("Sun Wukong",     "Journey to the West",  Material.STICK,          "§a"),
    L           ("L",              "Death Note",           Material.COOKIE,         "§f"),
    LIGHT       ("Light Yagami",   "Death Note",           Material.BOOK,           "§4"),
    NARUTO      ("Naruto",         "Naruto",               Material.ORANGE_DYE,     "§6"),
    ASH         ("Ash Ketchum",    "Pokémon",              Material.BONE,           "§9"),
    LEBRON      ("LeBron James",   "Real Life / Basketball",Material.ORANGE_WOOL,  "§6");

    private final String displayName;
    private final String series;
    private final Material icon;
    private final String color;

    AnimeClass(String displayName, String series, Material icon, String color) {
        this.displayName = displayName;
        this.series = series;
        this.icon = icon;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public String getSeries() { return series; }
    public Material getIcon() { return icon; }
    public String getColor() { return color; }
    public String getColoredName() { return color + displayName; }
}
