package net.corda.bn.flows.demo

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import javax.transaction.Transactional

@InitiatingFlow
@StartableByRPC
class RevokeMembership(private val id: Long) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        serviceHub.withEntityManager {
            val membership = find(BNMembership::class.java, id)
            remove(membership)

            membership
        }.also {
            val membership = Membership.from(it)
            val session = initiateFlow(membership.identity.cordaIdentity)
            serviceHub.sendSigned(session, it, ourIdentity.owningKey)
        }
    }
}

@InitiatedBy(RevokeMembership::class)
class RevokeMembershipResponder(private val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    @Transactional
    override fun call() {
        val membership = session.receiveSigned<BNMembership>()
        serviceHub.withEntityManager {
            createQuery("delete from BNMembership where network_id = :network_id")
                    .setParameter("network_id", membership.networkId)
                    .executeUpdate()
        }
    }
}