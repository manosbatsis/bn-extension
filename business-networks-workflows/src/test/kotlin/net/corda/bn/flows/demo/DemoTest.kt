package net.corda.bn.flows.demo

import net.corda.bn.flows.MembershipManagementFlowTest
import net.corda.bn.flows.identity
import net.corda.bn.states.MembershipStatus
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.StartedMockNode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoTest : MembershipManagementFlowTest(numberOfAuthorisedMembers = 1, numberOfRegularMembers = 1) {

    private fun createBusinessNetwork(initiator: StartedMockNode): Membership {
        val future = initiator.startFlow(CreateBusinessNetwork())
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    private fun onboardMembership(initiator: StartedMockNode, networkId: String, party: Party): Membership {
        val future = initiator.startFlow(OnboardMembership(networkId, party))
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    private fun activateMembership(initiator: StartedMockNode, id: Long): Membership {
        val future = initiator.startFlow(ActivateMembership(id))
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    private fun suspendMembership(initiator: StartedMockNode, id: Long): Membership {
        val future = initiator.startFlow(SuspendMembership(id))
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    private fun revokeMembership(initiator: StartedMockNode, id: Long) {
        val future = initiator.startFlow(RevokeMembership(id))
        mockNetwork.runNetwork()
        future.getOrThrow()
    }

    private fun getMembershipList(initiator: StartedMockNode, networkId: String): List<Membership> {
        val future = initiator.startFlow(GetMembershipList(networkId))
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    @Test(timeout = 300_000)
    fun `simple test`() {
        val authorisedMember = authorisedMembers.first()
        val regularMember = regularMembers.first()

        val networkId = createBusinessNetwork(authorisedMember).networkId
        val id = onboardMembership(authorisedMember, networkId, regularMember.identity()).id
        listOf(authorisedMember, regularMember).forEach { member ->
            getMembershipList(member, networkId).apply {
                assertEquals(2, size)
                assertTrue(all { it.status == MembershipStatus.ACTIVE })
            }
        }

        suspendMembership(authorisedMember, id)
        listOf(authorisedMember, regularMember).forEach { member ->
            getMembershipList(member, networkId).apply {
                assertEquals(2, size)
                assertTrue(count { it.status == MembershipStatus.ACTIVE } == 1)
                assertTrue(count { it.status == MembershipStatus.SUSPENDED } == 1)
            }
        }

        activateMembership(authorisedMember, id)
        listOf(authorisedMember, regularMember).forEach { member ->
            getMembershipList(member, networkId).apply {
                assertEquals(2, size)
                assertTrue(all { it.status == MembershipStatus.ACTIVE })
            }
        }

        revokeMembership(authorisedMember, id)
        assertEquals(1, getMembershipList(authorisedMember, networkId).size)
        assertTrue(getMembershipList(regularMember, networkId).isEmpty())
    }
}