package com.thuo_ng.swift_chat_android.domain.model

fun String.normalizeNotificationType(): String {
    return replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .replace('-', '_')
        .lowercase()
}

fun String.isFriendRequestReceivedType(): Boolean {
    val normalized = normalizeNotificationType()
    return normalized == "friend_request_received" ||
        normalized == "friend_request" ||
        normalized == "friend_request_new" ||
        normalized == "friend_request_created"
}

fun String.isFriendRequestAcceptedType(): Boolean {
    val normalized = normalizeNotificationType()
    return normalized == "friend_request_accepted" ||
        normalized == "friend_request_accept" ||
        normalized == "friend_request_accepted_by_other"
}

fun String.isAddedToGroupType(): Boolean {
    val normalized = normalizeNotificationType()
    return normalized == "added_to_group" ||
        normalized == "group_member_added" ||
        normalized == "group_you_added"
}

fun String.isRemovedFromGroupType(): Boolean {
    val normalized = normalizeNotificationType()
    return normalized == "removed_from_group" ||
        normalized == "group_member_removed" ||
        normalized == "group_disbanded"
}

fun String.isGroupRoleChangedType(): Boolean {
    val normalized = normalizeNotificationType()
    return normalized == "group_role_changed"
}

fun String.isNewMessageType(): Boolean {
    val normalized = normalizeNotificationType()
    return normalized == "new_message" ||
        normalized == "message" ||
        normalized == "chat_message" ||
        normalized == "message_received" ||
        normalized == "message_created" ||
        normalized == "message_new" ||
        normalized == "chat_new_message"
}
