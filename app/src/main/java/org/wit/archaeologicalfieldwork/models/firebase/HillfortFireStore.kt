package org.wit.archaeologicalfieldwork.models.firebase

import android.content.Context
import android.graphics.Bitmap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.wit.archaeologicalfieldwork.helpers.readImageFromPath
import org.wit.archaeologicalfieldwork.models.HillfortModel
import org.wit.archaeologicalfieldwork.models.HillfortStore
import java.io.ByteArrayOutputStream
import java.io.File

class HillfortFireStore(val context: Context) : HillfortStore, AnkoLogger {

    val hillforts = ArrayList<HillfortModel>()
    val searchedHillforts = ArrayList<HillfortModel>()
    val favoriteList = ArrayList<String>()
    val favoriteHillforts = ArrayList<HillfortModel>()
    lateinit var userId: String
    lateinit var db: DatabaseReference
    lateinit var st: StorageReference

    override fun findSearchedHillforts(): ArrayList<HillfortModel> {
        return searchedHillforts
    }

    override fun clearSearch(){
        searchedHillforts.clear()
    }

    override fun findHillfortName(name: String): ArrayList<HillfortModel> {
        val foundHillforts: ArrayList<HillfortModel> = arrayListOf()
        hillforts.forEach {
            if (it.name.toLowerCase().contains(name.toLowerCase())) {
                foundHillforts?.add(it)
            }
        }
        return foundHillforts
    }

    fun findHillforts(name: String){
        searchedHillforts.clear()
        val query = db.child("users").child(userId).child("hillforts")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    if(it.child("name").toString().contains(name)){
                        searchedHillforts.add(it.getValue<HillfortModel>(HillfortModel::class.java)!!)
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun findAllHillforts(): List<HillfortModel> {
        return hillforts
    }

    override fun findAllFavorites(): List<HillfortModel> {
        hillforts.forEach {
            if (it.favorite) {
                favoriteHillforts.add(it)
            }
        }
        return  favoriteHillforts
    }

    override fun findHillfortById(id: Long): HillfortModel? {
        val foundHillfort: HillfortModel? = hillforts.find { p -> p.id == id }
        return foundHillfort
    }

    override fun createHillfort(hillfort: HillfortModel) {
        val key = db.child("users").child(userId).child("hillforts").push().key
        key?.let {
            hillfort.fbId = key
            hillforts.add(hillfort)
            db.child("users").child(userId).child("hillforts").child(key).setValue(hillfort)
            updateImage(hillfort)
        }
    }

    override fun updateHillfort(hillfort: HillfortModel) {
        var foundHillfort: HillfortModel? = hillforts.find { p -> p.fbId == hillfort.fbId }
        if (foundHillfort != null) {
            foundHillfort.name = hillfort.name
            foundHillfort.description = hillfort.description
            foundHillfort.image = hillfort.image
            foundHillfort.location = hillfort.location
            foundHillfort.notes = hillfort.notes
            foundHillfort.date = hillfort.date
            foundHillfort.visited = hillfort.visited
            foundHillfort.rating = hillfort.rating
            foundHillfort.favorite = hillfort.favorite
        }

        db.child("users").child(userId).child("hillforts").child(hillfort.fbId).setValue(hillfort)
        if ((hillfort.image.length) > 0 && (hillfort.image[0] != 'h')) {
            updateImage(hillfort)
        }
    }

    override fun addFavorite(hillfort: HillfortModel) {
        if (!hillfort.favorite) {
            db.child("users").child(userId).child("favoritehillforts").child(hillfort.fbId)
                .removeValue()
        } else {
            val key = db.child("users").child(userId).child("favoritehillforts").push()
            key.let {
                db.child("users").child(userId).child("favoritehillforts").child(hillfort.fbId)
                    .setValue(hillfort.name)
            }
        }
    }

    override fun deleteHillfort(hillfort: HillfortModel) {
        db.child("users").child(userId).child("hillforts").child(hillfort.fbId).removeValue()
        hillforts.remove(hillfort)
    }

    override fun clear() {
        hillforts.clear()
    }

    fun fetchHillforts(hillfortsReady: () -> Unit) {
        val valueEventListener = object : ValueEventListener {
            override fun onCancelled(dataSnapshot: DatabaseError) {
            }
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot!!.children.mapNotNullTo(hillforts) { it.getValue<HillfortModel>(HillfortModel::class.java) }
                hillfortsReady()
            }
        }
        userId = FirebaseAuth.getInstance().currentUser!!.uid
        db = FirebaseDatabase.getInstance().reference
        st = FirebaseStorage.getInstance().reference
        hillforts.clear()
        db.child("users").child(userId).child("hillforts").addListenerForSingleValueEvent(valueEventListener)
    }

    fun updateImage(hillfort: HillfortModel) {
        if (hillfort.image != "") {
            val fileName = File(hillfort.image)
            val imageName = fileName.getName()

            var imageRef = st.child(userId + '/' + imageName)
            val baos = ByteArrayOutputStream()
            val bitmap = readImageFromPath(context, hillfort.image)

            bitmap?.let {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()
                val uploadTask = imageRef.putBytes(data)
                uploadTask.addOnFailureListener {
                    println(it.message)
                }.addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                        hillfort.image = it.toString()
                        db.child("users").child(userId).child("hillforts").child(hillfort.fbId).setValue(hillfort)
                    }
                }
            }
        }
    }
}