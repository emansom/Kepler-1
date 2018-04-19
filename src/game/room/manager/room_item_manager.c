#include "list.h"

#include "room_item_manager.h"

#include "game/items/item.h"
#include "game/room/room.h"

#include "database/queries/item_query.h"

void room_item_manager_load(room *room) {
    List *items = item_query_get_room_items(room->room_id);

    for (size_t i = 0; i < list_size(items); i++) {
        item *item;
        list_get_at(items, i, (void*)&item);
        list_add(room->items, item);
    }

    list_destroy(items);
}

void room_item_manager_dispose(room *room) {
    for (size_t i = 0; i < list_size(room->items); i++) {
        item *item;
        list_get_at(room->items, i, (void*)&item);
        item_dispose(item);
    }

    list_remove_all(room->items);
}