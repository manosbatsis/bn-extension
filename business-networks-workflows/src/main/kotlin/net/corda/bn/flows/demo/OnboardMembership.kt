package net.corda.bn.flows.demo

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.states.BNIdentity
import net.corda.bn.states.MemberRole
import net.corda.bn.states.MembershipIdentity
import net.corda.bn.states.MembershipStatus
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.serialization.serialize

@InitiatingFlow
@StartableByRPC
class OnboardMembership(
        private val networkId: String,
        private val party: Party,
        private val businessIdentity: BNIdentity? = null
) : FlowLogic<Membership>() {

    @Suspendable
    override fun call(): Membership = BNMembership(
            identity = MembershipIdentity(party, businessIdentity).serialize().bytes,
            networkId = networkId,
            status = MembershipStatus.ACTIVE,
            roles = setOf(MemberRole()).serialize().bytes
    ).let { membership ->
        serviceHub.withEntityManager {
            persist(membership)
            flush()
        }

        val session = initiateFlow(party)
        val membershipList = subFlow(GetMembershipList(networkId)).map { it.toBNMembership() }
        serviceHub.sendSigned(session, membershipList, ourIdentity.owningKey)

        Membership.from(membership)
    }
}

@InitiatedBy(OnboardMembership::class)
class OnboardMembershipResponder(private val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val membershipList = session.receiveSigned<List<BNMembership>>()
        serviceHub.withEntityManager {
            membershipList.forEach {
                merge(it)
            }
            flush()
        }
    }
}