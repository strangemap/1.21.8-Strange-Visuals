package ru.strange.client.manager.friend;

import net.minecraft.client.MinecraftClient;
import ru.strange.client.Strange;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FriendManager {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static final List<Friend> friends = new ArrayList<>();
    public static final File file =  new File(Strange.get.root + "\\configs", "friend.cfg");;

    public static void init() {
        try {
            if (!file.exists()) {
                file.createNewFile();
            } else {
                readFriends();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void add(String name) {
        friends.add(new Friend(name));
        updateFile();
    }

    public Friend getFriend(String friend) {
        return friends.stream().filter(isFriend -> isFriend.getName().equals(friend)).findFirst().get();
    }

    public boolean isFriend(String friend) {
        return friends.stream().anyMatch(isFriend -> isFriend.getName().equals(friend));
    }

    public void remove(String name) {
        friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
        updateFile();
    }

    public void clearFriend() {
        friends.clear();
        updateFile();
    }

    public static List<Friend> getFriends() {
        return friends;
    }
    public static boolean getNearFriends(String name) {

        return mc.world.getPlayers().stream().anyMatch(player -> player.getName().getString().equals(name));

    }

    public void updateFile() {
        try {
            StringBuilder builder = new StringBuilder();
            friends.forEach(friend -> builder.append(friend.getName()).append("\n"));
            Files.write(file.toPath(), builder.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void readFriends() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file.getAbsolutePath()))));
            String line;
            while ((line = reader.readLine()) != null) {
                friends.add(new Friend(line));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

