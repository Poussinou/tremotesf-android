/*
 * Copyright (C) 2017 Alexey Rochev <equeim@gmail.com>
 *
 * This file is part of Tremotesf.
 *
 * Tremotesf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tremotesf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.equeim.tremotesf.torrentpropertiesactivity

import java.text.Collator
import java.text.DecimalFormat
import java.util.Comparator

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.support.v7.widget.RecyclerView

import com.google.gson.JsonObject
import com.amjjd.alphanum.AlphanumericComparator

import org.equeim.tremotesf.R
import org.equeim.tremotesf.Torrent

import org.equeim.tremotesf.utils.Utils


private class Peer(peerJson: JsonObject) {
    val address = peerJson["address"].asString

    var downloadSpeed = 0L
        private set(value) {
            if (value != field) {
                field = value
                changed = true
            }
        }

    var uploadSpeed = 0L
        private set(value) {
            if (value != field) {
                field = value
                changed = true
            }
        }

    var progress = 0.0
        private set(value) {
            if (value != field) {
                field = value
                changed = true
            }
        }

    /*var flags = String()
        private set(value) {
            if (value != field) {
                field = value
                changed = true
            }
        }*/

    var client = String()
        private set(value) {
            if (value != field) {
                field = value
                changed = true
            }
        }

    var changed = false
        private set

    init {
        update(peerJson)
    }

    fun update(peerJson: JsonObject) {
        downloadSpeed = peerJson["rateToClient"].asLong
        uploadSpeed = peerJson["rateToPeer"].asLong
        progress = peerJson["progress"].asDouble
        client = peerJson["clientName"].asString
    }
}

class PeersAdapter(private val activity: TorrentPropertiesActivity) : RecyclerView.Adapter<PeersAdapter.ViewHolder>() {
    private var torrent: Torrent? = null
    private val peers = mutableListOf<Peer>()

    private val comparator = object : Comparator<Peer> {
        private val stringComparator = AlphanumericComparator(Collator.getInstance())
        override fun compare(o1: Peer, o2: Peer) = stringComparator.compare(o1.address, o2.address)
    }

    override fun getItemCount() = peers.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(R.layout.peer_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peer = peers[position]

        holder.addressTextView.text = peer.address
        holder.downloadSpeedTextView.text = activity.getString(R.string.download_speed_string, Utils.formatByteSpeed(activity, peer.downloadSpeed))
        holder.uploadSpeedTextView.text = activity.getString(R.string.upload_speed_string, Utils.formatByteSpeed(activity, peer.uploadSpeed))
        holder.progressTextView.text = activity.getString(R.string.progress_string, DecimalFormat("0.#").format(peer.progress * 100))
        holder.clientTextView.text = peer.client
    }

    fun update() {
        val torrent = activity.torrent

        if (torrent == null) {
            if (this.torrent == null) {
                return
            }
            this.torrent = null
            val count = itemCount
            peers.clear()
            notifyItemRangeRemoved(0, count)
            return
        }

        this.torrent = torrent

        val peerJsons = torrent.peers
        if (peerJsons == null) {
            return
        }

        val newPeers = mutableListOf<Peer>()
        for (jsonElement in peerJsons) {
            val peerJson = jsonElement.asJsonObject
            val address = peerJson["address"].asString
            var peer = peers.find { it.address == address }
            if (peer == null) {
                peer = Peer(peerJson)
            } else {
                peer.update(peerJson)
            }
            newPeers.add(peer)
        }

        run {
            var i = 0
            while (i < peers.size) {
                if (newPeers.contains(peers[i])) {
                    i++
                } else {
                    peers.removeAt(i)
                    notifyItemRemoved(i)
                }
            }
        }

        for ((i, peer) in newPeers.sortedWith(comparator).withIndex()) {
            if (peers.getOrNull(i) === peer) {
                if (peer.changed) {
                    notifyItemChanged(i)
                }
            } else {
                val index = peers.indexOf(peer)
                if (index == -1) {
                    peers.add(i, peer)
                    notifyItemInserted(i)
                } else {
                    peers.removeAt(index)
                    peers.add(i, peer)
                    notifyItemMoved(index, i)
                    if (peer.changed) {
                        notifyItemChanged(i)
                    }
                }
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val addressTextView = itemView.findViewById(R.id.address_text_view) as TextView
        val downloadSpeedTextView = itemView.findViewById(R.id.download_speed_text_view) as TextView
        val uploadSpeedTextView = itemView.findViewById(R.id.upload_speed_text_view) as TextView
        val progressTextView = itemView.findViewById(R.id.progress_text_view) as TextView
        val clientTextView = itemView.findViewById(R.id.client_text_view) as TextView
    }
}