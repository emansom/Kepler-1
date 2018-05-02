#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <game/pathfinder/rotation.h>

#include "hashtable.h"
#include "list.h"
#include "deque.h"

#include "game/player/player.h"

#include "game/room/room.h"
#include "game/room/room_user.h"

#include "game/room/mapping/room_model.h"
#include "game/room/mapping/room_tile.h"
#include "game/room/mapping/room_map.h"
#include "game/room/pool/pool_handler.h"

#include "game/items/item.h"
#include "game/items/definition/item_definition.h"

#include "game/pathfinder/pathfinder.h"
#include "game/pathfinder/coord.h"

#include "util/stringbuilder.h"
#include "communication/messages/outgoing_message.h"

/**
 * Create room user by the given entity.
 *
 * @param player the entity for the room user
 * @return the room user struct to return
 */
room_user *room_user_create(session *player) {
    room_user *user = malloc(sizeof(room_user));
    user->player = player;
    user->position = create_coord(0, 0);
    user->goal = create_coord(0, 0);
    user->next = NULL;
    user->walk_list = NULL;
    hashtable_new(&user->statuses);
    room_user_reset(user);
    return user;
}

/**
 *  Called when a player either leaves a room, or disconnects.
 *
 * @param room_user
 */
void room_user_reset(room_user *room_user) {
    stop_walking(room_user, true);
    room_user_remove_status(room_user, "swim");
    room_user_remove_status(room_user, "sit");
    room_user_remove_status(room_user, "lay");
    room_user_remove_status(room_user, "flatctrl");

    // Carry items
    room_user_remove_status(room_user, "carryf");
    room_user_remove_status(room_user, "carryd");
    room_user_remove_status(room_user, "cri");

    room_user->is_walking = false;
    room_user->needs_update = false;
    room_user->walking_lock = false;
    room_user->authenticate_id = -1;
    room_user->room_id = 0;
    room_user->room = NULL;

    room_user->room_look_at_timer = -1;
    room_user->lido_vote = -1;
    room_user_reset_idle_timer(room_user);

    if (room_user->next != NULL) {
        free(room_user->next);
        room_user->next = NULL;
    }
}

/**
 * Called when a player disconnects.
 *
 * @param room_user
 */
void room_user_cleanup(room_user *room_user) {
    room_user_reset(room_user);

    if (room_user->position != NULL) {
        free(room_user->position);
        room_user->position = NULL;
    }

    if (room_user->goal != NULL) {
        free(room_user->goal);
        room_user->goal = NULL;
    }

    if (room_user->statuses != NULL) {
        hashtable_destroy(room_user->statuses);
        room_user->statuses = NULL;
    }

    room_user->room = NULL;
    free(room_user);
}


/**
 * Clear the walk list, called by the server automatically.
 *
 * @param room_user the room user to clear
 */
void room_user_clear_walk_list(room_user *room_user) {
    if (room_user->walk_list != NULL) {
        for (size_t i = 0; i < (int)deque_size(room_user->walk_list); i++) {
            coord *coord;
            deque_get_at(room_user->walk_list, i, (void*)&coord);
            free(coord);
        }

        deque_destroy(room_user->walk_list);
        room_user->walk_list = NULL;
        room_user->next = NULL;
    }
}

/**
 * Send walk request to room.
 *
 * @param room_user the room user that wants to walk
 * @param x the x coord
 * @param y the y corord
 */
void walk_to(room_user *room_user, int x, int y) {
    if (room_user->room == NULL) {
        return;
    }

    //log_debug("User requested path %i, %i from path %i, %i in rooms %i.", x, y, room_user->position->x, room_user->position->y, room_user->room_id);

    if (!room_tile_is_walkable((room *) room_user->room, room_user, x, y)) {
        return;
    }

    room_tile *tile = room_user->room->room_map->map[x][y];

    if (tile != NULL && tile->highest_item != NULL) {
        item *item = tile->highest_item;

        if (strcmp(item->definition->sprite, "queue_tile2") == 0 && room_user->player->player_data->tickets == 0) {
            outgoing_message *om = om_create(73); // "AI"
            player_send(room_user->player, om);
            om_cleanup(om);
            return;
        }
    }

    if (room_user->next != NULL) {
        room_user->position->x = room_user->next->x;
        room_user->position->y = room_user->next->y;
        room_user->needs_update = true;

        free(room_user->next);
        room_user->next = NULL;
    }

    room_user->goal->x = x;
    room_user->goal->y = y;

    Deque *path = create_path(room_user);

    if (path != NULL && deque_size(path) > 0) {
        room_user_clear_walk_list(room_user);
        room_user->walk_list = path;
        room_user->is_walking = true;
    }
}

/**
 * Forcibly stops the user from walking, will clear/update statuses and auto manage memory.
 *
 * @param room_user the room user
 */
void stop_walking(room_user *room_user, bool is_silent) {
    if (room_user->next != NULL) {
        free(room_user->next);
        room_user->next = NULL;
    }

    room_user_remove_status(room_user, "mv");
    room_user_clear_walk_list(room_user);
    room_user->is_walking = false;

    if (!is_silent) {
        room_user_invoke_item(room_user);
    }
}

void room_user_reset_idle_timer(room_user *room_user) {
    room_user->room_idle_timer = (int) (time(NULL) + 300); // Give the user 5 minutes to idle or they'll be kicked.
}

/**
 * Animates the users mouth when speaking and detects any gestures.
 *
 * @param room_user the room user to animate for
 * @param text the text to read for any gestures and to find animation times
 */
void room_user_show_chat(room_user *room_user, char *text, bool is_shout) {
    int talk_duration = 1;

    if (strlen(text) > 1) {
        if (strlen(text) >= 10) {
            talk_duration = 5;
        } else {
            talk_duration = (int) (strlen(text) / 2);
        }
    }

    bool found_gesture = false;
    char gesture[5];

    if (strstr(text, ":)") != NULL
        || strstr(text, ":-)") != NULL
        || strstr(text, ":p") != NULL
        || strstr(text, ":d") != NULL
        || strstr(text, ":D") != NULL
        || strstr(text, ";)") != NULL
        || strstr(text, ";-)") != NULL) {
        strcpy(gesture, " sml");
        found_gesture = true;
    }

    if (!found_gesture &&
        strstr(text, ":s") != NULL
        || strstr(text, ":(") != NULL
        || strstr(text, ":-(") != NULL
        || strstr(text, ":'(") != NULL) {
        strcpy(gesture, " sad");
        found_gesture = true;
    }

    if (!found_gesture &&
        strstr(text, ":o") != NULL
        || strstr(text, ":O") != NULL) {
        strcpy(gesture, " srp");
        found_gesture = true;
    }


    if (!found_gesture &&
        strstr(text, ":@") != NULL
        || strstr(text, ">:(") != NULL) {
        strcpy(gesture, " agr");
        found_gesture = true;
    }

    if (found_gesture) {
        room_user_add_status(room_user, "gest", gesture, 5, "", -1, -1);
    }

    room_user_add_status(room_user, "talk", "", talk_duration, "", -1, -1);

    List *players;

    if (is_shout) {
        players = room_user->room->users;
    } else {
        players = room_nearby_players(room_user->room, room_user, room_user->position, 10);
    }

    for (size_t i = 0; i < list_size(players); i++) {
        session *player;
        list_get_at(players, i, (void *) &player);

        // Look at player talking
        room_user_look(player->room_user, room_user->position);
    }

    room_user->needs_update = true;

    if (!is_shout) {
        list_destroy(players);
    }
}

void room_user_look(room_user *room_user, coord *towards) {
    if (room_user->is_walking) {
        return;
    }

    int diff = room_user->position->rotation - calculate_human_direction(room_user->position->x, room_user->position->y, towards->x, towards->y);


    if ((room_user->position->rotation % 2) == 0) {

        if (diff > 0) {
            room_user->position->head_rotation = (room_user->position->rotation - 1);
        } else if (diff < 0) {
            room_user->position->head_rotation = (room_user->position->rotation + 1);
        } else {
            room_user->position->head_rotation = (room_user->position->rotation);
        }
    }

    room_user->needs_update = true;
    room_user->room_look_at_timer = (int) (time(NULL) + 6); // head reset back in 6 seconds
}

/**
 * Triggers the current item that the player on top of.
 *
 * @param room_user the room user to trigger for
 */
void room_user_invoke_item(room_user *room_user) {
    bool needs_update = false;

    double height = room_user->room->room_map->map[room_user->position->x][room_user->position->y]->tile_height;

    if (height != room_user->position->z) {
        room_user->position->z = height;
        needs_update = true;
    }

    item *item = NULL;
    room_tile *tile = room_user->room->room_map->map[room_user->position->x][room_user->position->y];

    if (tile != NULL) {
        if (tile->highest_item != NULL) {
            item = tile->highest_item;
        }
    }

    if (item == NULL || (!item->definition->behaviour->can_sit_on_top && !item->definition->behaviour->can_lay_on_top)) {
        if (room_user_has_status(room_user, "sit") || room_user_has_status(room_user, "lay")) {
            room_user_remove_status(room_user, "sit");
            room_user_remove_status(room_user, "lay");
            needs_update = true;
        }
    }

    if (item != NULL) {
        if (item->definition->behaviour->can_sit_on_top) {
            char sit_height[11];
            sprintf(sit_height, " %1.f", item->definition->top_height);

            room_user_add_status(room_user, "sit", sit_height, -1, "", -1, -1);
            coord_set_rotation(room_user->position, item->position->rotation ,item->position->rotation);
            needs_update = true;
        }

        pool_item_walk_on(room_user->player, item);
    }

    room_user->needs_update = needs_update;
}

void room_user_carry_item(room_user *room_user, int carry_id) {
    enum drink_type {
        DRINK,
        EAT,
        ITEM
    };

    enum drink_type drinks[26] = { DRINK };
    drinks[1] = DRINK;  // Tea
    drinks[2] = DRINK;  // Juice
    drinks[3] = EAT;    // Carrot
    drinks[4] = EAT;    // Ice-cream
    drinks[5] = DRINK;  // Milk
    drinks[6] = DRINK;  // Blackcurrant
    drinks[7] = DRINK;  // Water
    drinks[8] = DRINK;  // Regular
    drinks[9] = DRINK;  // Decaff
    drinks[10] = DRINK; // Latte
    drinks[11] = DRINK; // Mocha
    drinks[12] = DRINK; // Macchiato
    drinks[13] = DRINK; // Espresso
    drinks[14] = DRINK; // Filter
    drinks[15] = DRINK; // Iced
    drinks[16] = DRINK; // Cappuccino
    drinks[17] = DRINK; // Java
    drinks[18] = DRINK; // Tap
    drinks[19] = DRINK; // H*bbo Cola
    drinks[20] = ITEM;  // Camera
    drinks[21] = EAT;   // Hamburger
    drinks[22] = DRINK; // Lime H*bbo Soda
    drinks[23] = DRINK; // Beetroot H*bbo Soda
    drinks[24] = DRINK; // Bubble juice from 1999
    drinks[25] = DRINK; // Lovejuice

    if (carry_id >= 0 && carry_id <= 25) {
        char *carry_status[8];
        char *use_status[8];

        char drink_as_string[11];
        sprintf(drink_as_string, " %i", carry_id);

        enum drink_type type = drinks[carry_id];

        if (type == DRINK) {
            strcpy((char *) carry_status, "carryd");
            strcpy((char *) use_status, "drink");
        }

        if (type == EAT) {
            strcpy((char *) carry_status, "carryf");
            strcpy((char *) use_status, "eat");
        }

        if (type == ITEM) {
            strcpy((char *) carry_status, "cri");
            strcpy((char *) use_status, "usei");
        }

        room_user_remove_status(room_user, "cri");
        room_user_remove_status(room_user, "carryf");
        room_user_remove_status(room_user, "carryd");

        room_user_add_status(room_user, strdup((char*) carry_status), drink_as_string, 120, (char*) use_status, 12, 1);
        room_user->needs_update = true;
    }
}

/**
 * Adds a status to the room user, will handle switching actions automatically in the status task.
 * Will automatically remove and free the previous status.
 *
 * @param room_user the room user
 * @param key the first part to the status
 * @param value the second part to the status
 * @param sec_lifetime seconds until the status expires, -1 for permanent
 * @param action the action to switch to
 * @param sec_action_switch the amount of seconds needed until the action gets switched
 * @param sec_action_length the amount of seconds needed for the action to stay until the action switches back
 */
void room_user_add_status(room_user *room_user, char *key, char *value, int sec_lifetime, char *action, int sec_action_switch, int sec_switch_lifetime) {
    room_user_remove_status(room_user, key);

    room_user_status *status = malloc(sizeof(room_user_status));
    status->key = key;
    status->value = strdup(value);
    status->action = strdup(action);

    status->sec_lifetime = sec_lifetime;
    status->sec_action_switch = sec_action_switch;
    status->sec_switch_lifetime = sec_switch_lifetime;

    status->lifetime_countdown = sec_lifetime;
    status->action_countdown = sec_action_switch;
    status->action_switch_countdown = -1;

    hashtable_add(room_user->statuses, key, status);
}

/**
 * Removes a status of the room user by status key. Will
 * automatically remove and free the previous status.
 *
 * @param room_user the room user
 * @param key the key to remove
 */
void room_user_remove_status(room_user *room_user, char *key) {
    if (hashtable_contains_key(room_user->statuses, key)) {
        room_user_status *cleanup;
        hashtable_remove(room_user->statuses, key, (void*)&cleanup);
        free(cleanup->value);
        free(cleanup->action);
        free(cleanup);
    }
}

/**
 * Returns if the user currently has a status by its key.
 *
 * @param room_user the room user
 * @param key the key to remove
 * @return true, if successful
 */
int room_user_has_status(room_user *room_user, char *key) {
    return hashtable_contains_key(room_user->statuses, key);
}