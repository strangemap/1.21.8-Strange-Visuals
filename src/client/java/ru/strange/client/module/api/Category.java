package ru.strange.client.module.api;

public enum Category {
    Player("На игроке"),
    World("В мире"),
    Utilities("Утилиты"),
    Other("Остальное"),
    Interface("Интерфейс"),
    Theme("Темы");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}