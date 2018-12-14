package com.example.sample.mediaplayer;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CustomAdapter extends ArrayAdapter<AudioFile>  {
    private LayoutInflater inflater;
    private int layout;
    private List<AudioFile> audioFiles;

    public CustomAdapter(Context context, int resource, List<AudioFile> audioFiles) {
        super(context, resource, audioFiles);
        this.audioFiles = audioFiles;
        this.layout = resource;
        this.inflater = LayoutInflater.from(context);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View view=inflater.inflate(this.layout, parent, false);

        TextView nameView = (TextView) view.findViewById(R.id.name);
        TextView durationView = (TextView) view.findViewById(R.id.duration);

        AudioFile audioFile = audioFiles.get(position);

        nameView.setText(audioFile.getTitle());
        durationView.setText(audioFile.getArtist());

        return view;
    }
}
