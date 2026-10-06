package com.rutamotor.inventory.domain.model;

public record Buyer(String alias) {
  public Buyer {
    if (alias == null
        || !alias.matches("[A-Za-z0-9][A-Za-z0-9 _-]{1,39}")
        || !alias.equals(alias.strip()))
      throw new IllegalArgumentException(
          "Alias: 2 a 40 caracteres ASCII, sin espacios en los extremos");
  }
}
