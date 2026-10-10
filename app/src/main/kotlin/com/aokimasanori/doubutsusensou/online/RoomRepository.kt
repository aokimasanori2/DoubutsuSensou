package com.aokimasanori.doubutsusensou.online

import android.content.Context
import com.aokimasanori.doubutsusensou.game.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface RoomRepository {
    val uid: String?
    var savedRoom: String?
    suspend fun create(): Pair<String, OnlineRoom>
    suspend fun join(code: String): OnlineRoom
    suspend fun resume(code: String): OnlineRoom
    fun watch(code: String, listener: (OnlineRoom?, Boolean, Throwable?) -> Unit): () -> Unit
    suspend fun prepare(code: String, positions: Map<Int, Cell>)
    suspend fun move(code: String, revision: Long, id: Int, target: Cell)
    suspend fun pass(code: String, revision: Long)
    suspend fun leave(code: String)
}

class FirebaseRoomRepository(context: Context) : RoomRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()
    private val preferences = context.getSharedPreferences("online_room", Context.MODE_PRIVATE)
    private val random = SecureRandom()
    override val uid get() = auth.currentUser?.uid
    override var savedRoom: String?
        get() = preferences.getString("room", null)
        set(value) { preferences.edit().putString("room", value).apply() }

    private suspend fun signIn(): String = uid ?: auth.signInAnonymously().waitForResult().user!!.uid
    private fun reference(code: String): DocumentReference {
        if (!code.matches(Regex("[0-9]{8}"))) throw RoomProblem("code")
        return database.collection("rooms").document(code)
    }
    private fun decode(snapshot: DocumentSnapshot): OnlineRoom =
        snapshot.data?.let(RoomCodec::decode) ?: throw RoomProblem("not_found")

    override suspend fun create(): Pair<String, OnlineRoom> {
        val player = signIn()
        repeat(5) {
            val code = (10_000_000 + random.nextInt(90_000_000)).toString()
            val room = OnlineRoom(player, expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
            val ref = reference(code)
            val created = database.runTransaction { transaction ->
                if (transaction.get(ref).exists()) false
                else { transaction.set(ref, RoomCodec.encode(room)); true }
            }.waitForResult()
            if (created) return code to room
        }
        throw RoomProblem("retry")
    }

    override suspend fun join(code: String): OnlineRoom {
        val player = signIn()
        val ref = reference(code)
        return database.runTransaction { transaction ->
            val current = decode(transaction.get(ref))
            val next = RoomEngine.join(current, player, System.currentTimeMillis())
            if (next != current) transaction.set(ref, RoomCodec.encode(next))
            next
        }.waitForResult()
    }

    override suspend fun resume(code: String): OnlineRoom {
        val player = signIn()
        val room = decode(reference(code).get(Source.SERVER).waitForResult())
        if (room.player(player) == null) throw RoomProblem("not_member")
        return room
    }

    override fun watch(code: String, listener: (OnlineRoom?, Boolean, Throwable?) -> Unit): () -> Unit {
        val registration = reference(code).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) listener(null, false, error)
            else if (snapshot != null) {
                val online = !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()
                // A missing cache entry is not proof that the server room was deleted.
                if (snapshot.exists() || online) {
                    try { listener(decode(snapshot), online, null) }
                    catch (e: Exception) { listener(null, online, e) }
                }
            }
        }
        return { registration.remove() }
    }

    private suspend fun change(code: String, action: (OnlineRoom, String, Long) -> OnlineRoom) {
        val player = signIn()
        val ref = reference(code)
        database.runTransaction { transaction ->
            val current = decode(transaction.get(ref))
            val next = action(current, player, System.currentTimeMillis())
            if (next != current) transaction.set(ref, RoomCodec.encode(next))
        }.waitForResult()
    }
    override suspend fun prepare(code: String, positions: Map<Int, Cell>) =
        change(code) { room, uid, now -> RoomEngine.prepare(room, uid, positions, now) }
    override suspend fun move(code: String, revision: Long, id: Int, target: Cell) =
        change(code) { room, uid, now -> RoomEngine.move(room, uid, revision, id, target, now) }
    override suspend fun pass(code: String, revision: Long) =
        change(code) { room, uid, now -> RoomEngine.pass(room, uid, revision, now) }
    override suspend fun leave(code: String) =
        change(code) { room, uid, now -> RoomEngine.leave(room, uid, now) }
}

private suspend fun <T> Task<T>.waitForResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
