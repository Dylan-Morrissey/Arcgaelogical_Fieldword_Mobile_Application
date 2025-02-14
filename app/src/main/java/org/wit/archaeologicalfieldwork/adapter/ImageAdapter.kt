package org.wit.archaeologicalfieldwork.adapter

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import org.wit.archaeologicalfieldwork.helpers.readImageFromPath

class ImageAdapter(private val mContext: Context,private val  imageList: ArrayList<String>) : PagerAdapter() {

    override fun getCount(): Int {
        return imageList.size
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view === obj
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imageView = ImageView(mContext)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageBitmap(readImageFromPath(mContext, imageList[position]))
        container.addView(imageView)
        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as ImageView)
    }
}