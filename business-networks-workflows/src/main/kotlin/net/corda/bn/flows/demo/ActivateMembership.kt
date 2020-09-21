package net.corda.bn.flows.demo

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.states.MembershipStatus
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

@InitiatingFlow
@StartableByRPC
class ActivateMembership(private val id: Long) : FlowLogic<Membership>() {

    @Suspendable
    override fun call(): Membership = serviceHub.withEntityManager {
        val membership = find(BNMembership::class.java, id)
        merge(membership.copy(status = MembershipStatus.ACTIVE))
    }.let {
        val membership = Membership.from(it)
        val session = initiateFlow(membership.identity.cordaIdentity)
        serviceHub.sendSigned(session, it, ourIdentity.owningKey)

        membership
    }
}

@InitiatedBy(ActivateMembership::class)
class ActivateMembershipResponder(private val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val membership = session.receiveSigned<BNMembership>()
        serviceHub.withEntityManager {
            merge(membership)
        }
    }
}