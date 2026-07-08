package com.thuo_ng.swift_chat_android

import com.thuo_ng.swift_chat_android.data.mapper.normalizedAccountId
import com.thuo_ng.swift_chat_android.data.mapper.toDomain
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationMemberDto
import com.thuo_ng.swift_chat_android.domain.model.MuteDuration
import com.thuo_ng.swift_chat_android.ui.conversationinfo.ConversationMemberAction
import com.thuo_ng.swift_chat_android.ui.conversationinfo.availableMemberActions
import com.thuo_ng.swift_chat_android.ui.conversationinfo.canDisbandGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationInfoHelpersTest {

    @Test
    fun memberActions_followBackendRoles() {
        val leaderActions = availableMemberActions(
            currentRole = "leader",
            targetRole = "member",
            currentAccountId = "me",
            targetAccountId = "member-1"
        )

        assertTrue(ConversationMemberAction.Kick in leaderActions)
        assertTrue(ConversationMemberAction.PromoteToDeputy in leaderActions)
        assertTrue(ConversationMemberAction.TransferLeadership in leaderActions)

        val deputyActions = availableMemberActions(
            currentRole = "deputy",
            targetRole = "member",
            currentAccountId = "me",
            targetAccountId = "member-2"
        )
        assertEquals(setOf(ConversationMemberAction.Kick), deputyActions)

        val memberActions = availableMemberActions(
            currentRole = "member",
            targetRole = "member",
            currentAccountId = "me",
            targetAccountId = "member-3"
        )
        assertTrue(memberActions.isEmpty())
    }

    @Test
    fun memberActions_doNotAllowSelfManagementOrLeaderTarget() {
        assertTrue(
            availableMemberActions(
                currentRole = "leader",
                targetRole = "deputy",
                currentAccountId = "me",
                targetAccountId = "me"
            ).isEmpty()
        )

        assertTrue(
            availableMemberActions(
                currentRole = "leader",
                targetRole = "leader",
                currentAccountId = "me",
                targetAccountId = "leader-2"
            ).isEmpty()
        )

        assertTrue(canDisbandGroup("leader"))
        assertFalse(canDisbandGroup("deputy"))
    }

    @Test
    fun conversationMemberDto_normalizesIdAndFallbackDisplayFields() {
        val dto = ConversationMemberDto(
            id = "participant-id",
            accountId = null,
            userId = "user-id",
            role = null,
            joinAt = null,
            joinedAt = "2026-07-08T00:00:00Z",
            handle = "alex",
            displayName = null,
            avatarUrl = null
        )

        assertEquals("participant-id", dto.normalizedAccountId())

        val member = dto.toDomain()
        assertEquals("participant-id", member.accountId)
        assertEquals("member", member.role)
        assertEquals("alex", member.displayName)
        assertEquals("2026-07-08T00:00:00Z", member.joinAt)
    }

    @Test
    fun muteDuration_usesBackendValues() {
        assertEquals("1h", MuteDuration.OneHour.apiValue)
        assertEquals("8h", MuteDuration.EightHours.apiValue)
        assertEquals("24h", MuteDuration.OneDay.apiValue)
        assertEquals("forever", MuteDuration.Forever.apiValue)
    }
}
