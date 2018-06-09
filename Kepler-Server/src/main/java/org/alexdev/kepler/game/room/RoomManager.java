package org.alexdev.kepler.game.room;

import org.alexdev.kepler.dao.mysql.RoomDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static RoomManager instance = null;

    private ConcurrentHashMap<Integer, Room> roomMap;

    public RoomManager() {
        this.roomMap = new ConcurrentHashMap<>();
        RoomDao.resetVisitors();
    }

    /**
     * Find a room by its model.
     *
     * @param model the model to find the room by
     * @return the room found, else null
     */
    public Room getRoomByModel(String model) {
        int roomId = RoomDao.getRoomIdByModel(model);
        return getRoomById(roomId);
    }

    /**
     * Find a room by room id.
     *
     * @param roomId the id of the room to find
     * @return the loaded room instance, if successful, else query the db
     */
    public Room getRoomById(int roomId) {
        if (this.roomMap.containsKey(roomId)) {
            return this.roomMap.get(roomId);
        }

        return RoomDao.getRoomById(roomId);
    }

    /**
     * Check whether the room is active.
     *
     * @param roomId the room id to check
     * @return true, is successful
     */
    public boolean hasRoom(int roomId) {
        return this.roomMap.containsKey(roomId);
    }

    /**
     * Removes a room from the map by room id as key.
     *
     * @param roomId the id of the room to remove
     */
    public void removeRoom(int roomId) {
        this.roomMap.remove(roomId);
    }

    /**
     * Add a room instance to the map.
     *
     * @param room the instance of the room
     */
    public void addRoom(Room room) {
        if (room == null) {
            return;
        }

        if (this.roomMap.containsKey(room.getId())) {
            return;
        }

        this.roomMap.put(room.getData().getId(), room);
    }

    /**
     * Will sort a list of rooms returned by MySQL query and
     * replace any with loaded rooms that it finds.
     *
     * @param queryRooms the list of rooms returned by query
     * @return a possible list of actual loaded rooms
     */
    public List<Room> replaceQueryRooms(List<Room> queryRooms) {
        List<Room> roomList = new ArrayList<>();

        for (Room room : queryRooms) {
            if (this.roomMap.containsKey(room.getId())) {
                roomList.add(this.getRoomById(room.getData().getId()));
            } else {
                roomList.add(room);
            }
        }

        return roomList;
    }

    /**
     * Sort the list of rooms by higher populated rooms appearing first.
     *
     * @param roomList the list of rooms to sort
     */
    public void sortRooms(List<Room> roomList) {
        roomList.sort(Comparator.comparingDouble((Room room) -> room.getData().getTotalVisitorsNow()).reversed());
    }

    /**
     * Get the entire list of rooms.
     *
     * @return the collection of rooms
     */
    public Collection<Room> getRooms() {
        return this.roomMap.values();
    }

    /**
     * Get the instance of {@link RoomManager}
     *
     * @return the instance
     */
    public static RoomManager getInstance() {
        if (instance == null) {
            instance = new RoomManager();
        }

        return instance;
    }
}
