package net.corda.bn.flows.demo

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SignedData
import net.corda.core.flows.FlowSession
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.serialize
import net.corda.core.utilities.unwrap
import java.security.PublicKey

@Suspendable
fun ServiceHub.sendSigned(session: FlowSession, payload: Any, key: PublicKey) {
    val serialized = payload.serialize()
    val signedData = SignedData(serialized, keyManagementService.sign(serialized.bytes, key))
    session.send(signedData)
}

@Suspendable
inline fun <reified R : Any> FlowSession.receiveSigned(): R {
    val signedData = receive<SignedData<R>>().unwrap { it }
    return signedData.verified()
}