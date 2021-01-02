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
import id.rllyhz.mobilecomputingproject.Model.FavSong;
import id.rllyhz.mobilecomputingproject.R;
import id.rllyhz.mobilecomputingproject.Service.MediaPlayerService;

import static id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity.Broadcast_PLAY_NEW_AUDIO;
import static id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity.PLAYLIST_FAV_SONGS_INTENT;
import static id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity.PLAYLIST_SONGS_INTENT;
import static id.rllyhz.mobilecomputingproject.Activity.SmartMusicPlayerActivity.whichIntentIsCalling;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.audioIndex;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.favSongsIndex;
import static id.rllyhz.mobilecomputingproject.Helper.MusicHelper.songs;

public class PlaylistFavSongAdapter extends RecyclerView.Adapter<PlaylistFavSongAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Audio> songsList;

    public PlaylistFavSongAdapter(Context context, ArrayList<Audio> favSongsList) {
        this.context = context;
        songsList = new ArrayList<>();

        if (!favSongsList.isEmpty()) {
            songsList = favSongsList;
        } else {
            this.songsList.add(new Audio("", "-1", "", null, "", "", false));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View favSongItem = LayoutInflater.from(context).inflate(R.layout.row_fav_songs_item, parent, false);
        return new ViewHolder(favSongItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

        final int index = position;
        // set onclick  listener
        holder.rowFavSongsItemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // simpan index kedalam sharedPrefferences
                StorageUtil storage = new StorageUtil(context);
                int currentPosition = storage.loadAudioIndex();

                // cek jika lagu yang di klik sedang aktif (diputar atau dalam media session yang aktif / activeAudio)
                if (index == currentPosition) {
                    // langsung buka activity musicPlayer tanpa melakukan apapun
                    Intent musicPlayerIntent = new Intent(context, SmartMusicPlayerActivity.class);
                    musicPlayerIntent.putExtra(whichIntentIsCalling, PLAYLIST_SONGS_INTENT);
                    context.startActivity(musicPlayerIntent);
                    return;
                }

                // else
                // ubah audioIndex dan activeAudio di MusicHelper menjadi yang baru
                MusicHelper.audioIndex = index;
                MusicHelper.activeAudio = songsList.get(index);

                // juga simpan ke dalam sharedPrefferences
                storage.storeAudioIndex(index);
                storage.storeAudio(songsList);


                // dan setelah itu, cek lagi sebelum memutar lagu yang baru
                // apakah pertama kali di putar?
                if (MusicHelper.isFirstTimeToPlay) {
                    Intent playerService = new Intent(context, MediaPlayerService.class);
                    context.startService(playerService);

                    MusicHelper.isFirstTimeToPlay = false;
                } else {
                    // kalau tidak, kirim broadcast ke Service agar memainkan lagu yang sudah di simpan.
                    Intent broadCastPlayNewAudio = new Intent(Broadcast_PLAY_NEW_AUDIO);
                    context.sendBroadcast(broadCastPlayNewAudio);
                }

                // dan redirect ke musicPlayer activity
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


    class ViewHolder extends RecyclerView.ViewHolder {
        CardView rowFavSongsItemLayout;
        TextView songTitle, songArtist, songDuration;
        CircleImageView songAlbumArt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            songAlbumArt = itemView.findViewById(R.id.rowItem_favSongAlbumArt);
            songTitle = itemView.findViewById(R.id.rowItem_favSongtitle);
            songArtist = itemView.findViewById(R.id.rowItem_favSongArtist);
            songDuration = itemView.findViewById(R.id.rowItem_favSongDuration);
            rowFavSongsItemLayout = itemView.findViewById(R.id.rowFavSongsItemLayout);
        }
    }
}
