package com.thuo_ng.swift_chat_android.ui.conversationinfo

enum class ConversationMemberAction {
    Kick,
    PromoteToDeputy,
    DemoteToMember,
    TransferLeadership
}

fun availableMemberActions(
    currentRole: String?,
    targetRole: String?,
    currentAccountId: String?,
    targetAccountId: String
): Set<ConversationMemberAction> {
    if (currentAccountId.isNullOrBlank() || currentAccountId == targetAccountId) return emptySet()

    val actorRole = currentRole.normalizedRole()
    val memberRole = targetRole.normalizedRole()

    return when (actorRole) {
        ROLE_LEADER -> leaderActionsFor(memberRole)
        ROLE_DEPUTY -> if (memberRole == ROLE_MEMBER) setOf(ConversationMemberAction.Kick) else emptySet()
        else -> emptySet()
    }
}

fun canDisbandGroup(currentRole: String?): Boolean =
    currentRole.normalizedRole() == ROLE_LEADER

fun canAddGroupMembers(conversationType: String?): Boolean =
    conversationType.equals("group", ignoreCase = true)

fun canLeaveGroup(currentRole: String?): Boolean =
    currentRole.normalizedRole() != ROLE_LEADER

fun String?.normalizedRole(): String =
    this?.lowercase()?.takeIf { it.isNotBlank() } ?: ROLE_MEMBER

private fun leaderActionsFor(targetRole: String): Set<ConversationMemberAction> {
    if (targetRole == ROLE_LEADER) return emptySet()

    return buildSet {
        add(ConversationMemberAction.Kick)
        add(ConversationMemberAction.TransferLeadership)
        if (targetRole == ROLE_DEPUTY) {
            add(ConversationMemberAction.DemoteToMember)
        } else {
            add(ConversationMemberAction.PromoteToDeputy)
        }
    }
}

private const val ROLE_LEADER = "leader"
private const val ROLE_DEPUTY = "deputy"
private const val ROLE_MEMBER = "member"
