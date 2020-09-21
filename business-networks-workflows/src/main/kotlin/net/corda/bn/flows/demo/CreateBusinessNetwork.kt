package net.corda.bn.flows.demo

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.states.BNIdentity
import net.corda.bn.states.BNORole
import net.corda.bn.states.MembershipIdentity
import net.corda.bn.states.MembershipStatus
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.serialization.serialize

@StartableByRPC
class CreateBusinessNetwork(private val businessIdentity: BNIdentity? = null) : FlowLogic<Membership>() {

    @Suspendable
    override fun call(): Membership = BNMembership(
            identity = MembershipIdentity(ourIdentity, businessIdentity).serialize().bytes,
            networkId = UniqueIdentifier().toString(),
            status = MembershipStatus.ACTIVE,
            roles = setOf(BNORole()).serialize().bytes
    ).let {
        serviceHub.withEntityManager {
            persist(it)
            flush()
        }

        Membership.from(it)
    }
}