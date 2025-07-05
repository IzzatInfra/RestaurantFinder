package com.izzat.restaurantfinder.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.izzat.restaurantfinder.R
import com.izzat.restaurantfinder.model.Restaurant

class RestaurantAdapter(
    private val onClick: (Restaurant) -> Unit
) : RecyclerView.Adapter<RestaurantAdapter.ViewHolder>() {

    private val restaurants = mutableListOf<Restaurant>()

    fun submitList(data: List<Restaurant>) {
        restaurants.clear()
        restaurants.addAll(data)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.textRestaurantName)
        val btnNavigate: Button = view.findViewById(R.id.btnNavigate)

        fun bind(restaurant: Restaurant) {
            nameText.text = restaurant.name

            nameText.setOnClickListener {
                onClick(restaurant) // Pass to ScheduleActivity
            }

            btnNavigate.setOnClickListener {
                val uri = Uri.parse("google.navigation:q=${restaurant.latLng.latitude},${restaurant.latLng.longitude}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                btnNavigate.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = restaurants.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(restaurants[position])
    }
}
