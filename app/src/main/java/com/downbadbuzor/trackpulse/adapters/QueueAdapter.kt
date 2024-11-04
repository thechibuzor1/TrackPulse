package com.downbadbuzor.trackpulse.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.downbadbuzor.trackpulse.AudioItemModal
import com.downbadbuzor.trackpulse.R
import com.downbadbuzor.trackpulse.databinding.AudioItemBinding
import com.downbadbuzor.trackpulse.model.AudioModel

class QueueAdapter(
    private val queue: List<AudioModel>,
    private val supportFragmentManager: FragmentManager,
    private val usingQueue: Boolean
):
RecyclerView.Adapter<QueueAdapter.QueueViewHolder>(

)
{



    inner class QueueViewHolder(private val binding: AudioItemBinding)
        : RecyclerView.ViewHolder(binding.root){

        fun bind(audio: AudioModel) {

            binding.audioTitle.text = audio.title
            binding.audioArtist.text = audio.artist


            Glide.with(binding.albumArt)
                .load(audio.albumArtUri) // Use albumArtUri from AudioModel
                .placeholder(R.drawable.vinyl) // Placeholder image
                .error(R.drawable.vinyl) // Error image
                .into(binding.albumArt)

            binding.options.setOnClickListener {
                val bottomSheetFragment = AudioItemModal(audio, usingQueue)
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

            }

        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QueueAdapter.QueueViewHolder {
       val binding = AudioItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueueAdapter.QueueViewHolder, position: Int) {
        holder.bind(queue[position])
        holder.itemView.setOnLongClickListener {

            val bottomSheetFragment = AudioItemModal(queue[position], usingQueue)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

            true
        }
    }

    override fun getItemCount(): Int {
        return queue.size
    }
}




