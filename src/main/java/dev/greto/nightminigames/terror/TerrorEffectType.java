package dev.greto.nightminigames.terror;

import java.util.Arrays;
import java.util.List;

public enum TerrorEffectType {
    SILUETA("silueta"), MOVIMIENTO("movimiento"), SOMBRA("sombra"), CABEZA("cabeza"), PRESENCIA("presencia"), FLOTANTE("flotante");
    public final String id;
    TerrorEffectType(String id) { this.id = id; }
    public static TerrorEffectType find(String id) { for (var type : values()) if (type.id.equals(id)) return type; return null; }
    public static List<String> ids() { return Arrays.stream(values()).map(type -> type.id).toList(); }
}
