package net.corda.bn.flows.demo

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.states.BNRole
import net.corda.bn.states.MembershipIdentity
import net.corda.bn.states.MembershipStatus
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize

@CordaSerializable
data class Membership(
        val id: Long,
        val identity: MembershipIdentity,
        val networkId: String,
        val status: MembershipStatus,
        val roles: Set<BNRole>
) {
    companion object {
        fun from(bnMembership: BNMembership): Membership = bnMembership.run {
            Membership(
                    id = id,
                    identity = identity.deserialize(),
                    networkId = networkId,
                    status = status,
                    roles = roles.deserialize()
            )
        }
    }

    fun toBNMembership(): BNMembership = BNMembership(
            id = id,
            identity = identity.serialize().bytes,
            networkId = networkId,
            status = status,
            roles = roles.serialize().bytes
    )
}

@StartableByRPC
class GetMembershipList(private val networkId: String) : FlowLogic<List<Membership>>() {

    @Suspendable
    override fun call(): List<Membership> = serviceHub.withEntityManager {
        createQuery("select m from BNMembership m where m.networkId = :network_id", BNMembership::class.java)
                .setParameter("network_id", networkId)
                .resultList
    }.map {
        Membership.from(it)
    }
}