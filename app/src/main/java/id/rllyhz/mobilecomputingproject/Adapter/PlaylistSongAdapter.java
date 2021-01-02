package id.rllyhz.mobilecomputingproject.Adapter;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity;
import id.rllyhz.mobilecomputingproject.Helper.MusicHelper;
import id.rllyhz.mobilecomputingproject.Helper.StorageUtil;
import id.rllyhz.mobilecomputingproject.Model.Audio;
import id.rllyhz.mobilecomputingproject.R;
import id.rllyhz.mobilecomputingproject.Service.MediaPlayerService;

import static id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity.Broadcast_PLAY_NEW_AUDIO;
import static id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity.PLAYLIST_SONGS_INTENT;
import static id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity.whichIntentIsCalling;

public class PlaylistSongAdapter extends RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Audio> songsList;

    public PlaylistSongAdapter(Context context, ArrayList<Audio> songsList) {
        this.context = context;

        if (songsList.size() > 0) {
            this.songsList = songsList;
        } else {
            this.songsList = new ArrayList<>();
            this.songsList.add(new Audio("", "", "", null, "", "", false));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View songItemLayout = LayoutInflater.from(context).inflate(R.layout.row_songs_item, parent, false);
        return new ViewHolder(songItemLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.songTitle.setText(songsList.get(position).getTitle());
        holder.songArtist.setText(songsList.get(position).getArtist());
        holder.songDuration.setText(songsList.get(position).getDuration());

        if (songsList.get(position).getAlbumArt() == null) {
            holder.songAlbumArt.setImageResource(R.drawable.default_cover_art);
        } else {
            holder.songAlbumArt.setImageBitmap(songsList.get(position).getAlbumArt());
        }

        long durationInMillis = Long.parseLong(songsList.get(position).getDuration());
        String durationFixFormat = MusicHelper.Converters.milliSecondsToTimer(durationInMillis);
        holder.songDuration.setText(durationFixFormat);

        // set onclick  listener
        holder.rowSongsItemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save index to sharedPrefferences
                StorageUtil storage = new StorageUtil(context);
                int currentPosition = storage.loadAudioIndex();

                // check if clicked song is currently on activeAudio
                if (position == currentPosition) {
                    // just go directly to the musicPlayer activity
                    Intent musicPlayerIntent = new Intent(context, SmartMusicPlayerActivity.class);
                    musicPlayerIntent.putExtra(whichIntentIsCalling, PLAYLIST_SONGS_INTENT);
                    context.startActivity(musicPlayerIntent);
                    return;
                }

                // else
                // change audioIndex and activeAudio in MusicHelper to the new one
                MusicHelper.audioIndex = position;
                MusicHelper.activeAudio = songsList.get(position);

                // also store to the sharedPrefferences
                storage.storeAudioIndex(position);
                storage.storeAudio(songsList);


                // and then, check the app before playing the new one
                // is the first time to play?
                if (MusicHelper.isFirstTimeToPlay) {
                    Intent playerService = new Intent(context, MediaPlayerService.class);
                    context.startService(playerService);

                    MusicHelper.isFirstTimeToPlay = false;
                } else {
                    // send broadcast to the service so that it'll play the song that have been stored.
                    Intent broadCastPlayNewAudio = new Intent(Broadcast_PLAY_NEW_AUDIO);
                    context.sendBroadcast(broadCastPlayNewAudio);
                }

                // and go to the musicPlayer activity
                Intent musicPlayerIntent = new Intent(context, SmartMusicPlayerActivity.class);
                musicPlayerIntent.putExtra(whichIntentIsCalling, PLAYLIST_SONGS_INTENT);
                context.startActivity(musicPlayerIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songsList.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void insert(Audio song) {
        songsList.add(song);
        notifyItemInserted(getItemCount() - 1);
    }

    public void remove(Audio song) {
        int position = songsList.indexOf(song);
        songsList.remove(position);
        notifyItemRemoved(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView rowSongsItemLayout;
        TextView songTitle, songArtist, songDuration;
        CircleImageView songAlbumArt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            rowSongsItemLayout = itemView.findViewById(R.id.rowSongsItemLayout);
            songAlbumArt = itemView.findViewById(R.id.rowItem_songAlbumArt);
            songTitle = itemView.findViewById(R.id.rowItem_songtitle);
            songArtist = itemView.findViewById(R.id.rowItem_songArtist);
            songDuration = itemView.findViewById(R.id.rowItem_songDuration);
        }
    }
}
