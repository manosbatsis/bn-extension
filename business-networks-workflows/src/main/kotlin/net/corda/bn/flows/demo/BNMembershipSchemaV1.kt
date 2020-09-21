package net.corda.bn.flows.demo

import net.corda.bn.states.MembershipStatus
import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.CordaSerializable
import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

object BNMembershipSchema
object BNMembershipSchemaV1 : MappedSchema(schemaFamily = BNMembershipSchema::class.java, version = 1, mappedTypes = listOf(BNMembership::class.java)) {
    override val migrationResource: String? get() = "bn-membership-schema-v1.changelog-master"
}

@CordaSerializable
@Entity(name = "BNMembership")
@Table(name = "bn_membership")
data class BNMembership(
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @Column(name = "id", nullable = false)
        val id: Long = 0,

        @Column(name = "identity")
        val identity: ByteArray,

        @Column(name = "network_id")
        val networkId: String,

        @Column(name = "status")
        val status: MembershipStatus,

        @Column(name = "roles")
        val roles: ByteArray
) : Serializable